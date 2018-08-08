/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.acdebugger.impl;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.codice.acdebugger.api.BreakpointProcessor;
import org.codice.acdebugger.api.Debug;

/** This class provides the main implementation for processing breakpoint requests/callbacks. */
@SuppressWarnings("squid:S1191" /* Using the Java debugger API */)
public class Debugger {
  private static final String INFO_KEY = "info";

  private static final boolean SUSPEND_ALL = false;

  private static final String PORT_KEY = "port";

  private static final String HOST_KEY = "hostname";

  private final ExecutorService executor = Executors.newCachedThreadPool();

  private final String transport;

  private final String port;

  private final String host;

  private final DebugContext context;

  private final AtomicLong sequence = new AtomicLong();

  private Debug debug = null;

  /**
   * Creates a new debugger.
   *
   * @param transport the transport to use for the debugger
   * @param host the host where to attach to
   * @param port the port to connect to
   */
  public Debugger(String transport, String host, String port) {
    this.transport = transport;
    this.port = port;
    this.host = host;
    this.context = new DebugContext();
  }

  /**
   * Sets the continuous mode for the debugger in which case it will not fail any security
   * exceptions detected and accumulate and report on all occurrences or if it will stop when the
   * first security failure is detected.
   *
   * @param continuous <code>true</code> if the debugger should process all breakpoint callbacks and
   *     continue as if no errors had occurred; <code>false</code> if i should stop after the first
   *     detected security failure
   */
  public void setContinuous(boolean continuous) {
    context.setContinuousMode(continuous);
  }

  /**
   * Sets the dump mode where information about security failures detected is dumped to the console
   * in addition to all the solutions.
   *
   * @param dumping <code>true</code> to enable dumping of security failure information as they are
   *     detected; <code>false</code> to only preset solutions
   */
  public void setDumping(boolean dumping) {
    context.setDumping(dumping);
  }

  /**
   * Sets whether or not to automatically grant missing permissions when a single solution monitor
   * is found for a given service failure.
   *
   * @param granting <code>true</code> to automatically grant missing permissions; <code>false
   *     </code> not to
   */
  public void setGranting(boolean granting) {
    context.setGranting(granting);
  }

  /**
   * Sets whether or not to monitor service event permission checks.
   *
   * @param monitoring <code>true</code> to monitor service event permission checks; <code>false
   *     </code> to not to
   */
  public void setMonitoringService(boolean monitoring) {
    context.setMonitoringService(monitoring);
  }

  /**
   * Sets whether or not to consider <code>doPrivileged()</code> blocks when analysing security
   * failures.
   *
   * @param doPrivileged <code>true</code> to consider <code>doPrivileged()</code> blocks for
   *     possible solutions; <code>false</code> not to
   */
  public void setDoPrivilegedBlocks(boolean doPrivileged) {
    context.setDoPrivilegedBlocks(doPrivileged);
  }

  /** Attaches this debugger to the VM. */
  @SuppressWarnings("squid:S106" /* this is a console application */)
  public Debugger attach() throws IOException, IllegalConnectorArgumentsException {
    System.out.println("AC Debugger: Attaching to " + host + ":" + port + " ...");
    final VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
    final List<AttachingConnector> connectors = vmm.attachingConnectors();
    final AttachingConnector connector =
        connectors
            .stream()
            .filter(c -> c.transport().name().equals(transport))
            .findFirst()
            .orElseThrow(
                () -> new IOException(String.format("Failed to find transport %s", transport)));
    final Map<String, Argument> map = connector.defaultArguments();
    final Argument portArg = map.get(PORT_KEY);
    final Argument hostArg = map.get(HOST_KEY);

    portArg.setValue(port);
    hostArg.setValue(host);
    map.put(PORT_KEY, portArg);
    map.put(HOST_KEY, hostArg);
    this.debug = new DebugImpl(context, connector.attach(map));
    debug.virtualMachine().setDebugTraceMode(VirtualMachine.TRACE_NONE);
    System.out.printf("AC Debugger: Connected to:%n%s%n%n", debug.virtualMachine().description());
    return this;
  }

  /**
   * Adds a breakpoint to this debugger.
   *
   * @param processor the breakpoint processor to be added
   * @throws Exception if unable to add and register the breakpoint
   */
  public void add(BreakpointProcessor processor) throws Exception {
    for (final Iterator<BreakpointLocation> i = processor.locations().iterator(); i.hasNext(); ) {
      add(processor, i.next());
    }
  }

  /**
   * Performs the main loop processing all breakpoint callbacks.
   *
   * @throws Exception if a failure occurs while processing all breakpoint callbacks
   */
  @SuppressWarnings({
    "squid:S106", /* this is a console application */
    "squid:S00112" /* Forced to by the Java debugger API */
  })
  public void loop() throws Exception {
    final EventQueue evtQueue = debug.virtualMachine().eventQueue();
    String line = "AC Debugger: Monitoring security exceptions";

    if (debug.isMonitoringService()) {
      line += " and service events";
    }
    if (debug.isContinuous()) {
      line += " in continuous mode";
    }
    if (debug.canDoPrivilegedBlocks()) {
      line += " while analyzing missing permissions";
      line += " and doPrivileged() blocks";
      if (debug.isGranting()) {
        line +=
            " and automatically granting missing permissions (whenever it is the only possible solution)";
      }
    } else {
      line += " while analyzing";
      if (debug.isGranting()) {
        line += " and automatically granting";
      }
      line += " missing permissions";
    }
    System.out.println(line);
    System.out.println();
    while (context.isRunning()) {
      final EventSet eventSet = evtQueue.remove();
      final EventIterator i = eventSet.eventIterator();
      final AtomicBoolean resume = new AtomicBoolean(true); // unless otherwise specified

      try {
        while (i.hasNext()) {
          handleEventSet(eventSet, i, resume);
        }
      } finally {
        if (resume.get()) {
          eventSet.resume();
        }
      }
    }
    executor.shutdown();
    executor.awaitTermination(1L, TimeUnit.MINUTES);
  }

  private void add(BreakpointProcessor processor, BreakpointLocation l) throws Exception {
    final EventRequestManager erm = debug.getEventRequestManager();
    final ReferenceType clazz = debug.reflection().getClass(l.getClassSignature());

    if (clazz == null) {
      // class is either invalid or has not been loaded yet so let's wait for it before
      // adding a corresponding breakpoint
      final ClassPrepareRequest cpr = erm.createClassPrepareRequest();

      cpr.putProperty(Debugger.INFO_KEY, new PendingBreakpointInfo(cpr, processor, l));
      cpr.addClassFilter(l.getClassName());
      cpr.enable();
    } else {
      l.setClassReference(clazz);
      final EventRequest request = processor.createRequest(debug, l);

      if (request != null) {
        request.putProperty(Debugger.INFO_KEY, new BreakpointInfo(request, processor, l));
        request.setSuspendPolicy(
            Debugger.SUSPEND_ALL
                ? BreakpointRequest.SUSPEND_ALL
                : BreakpointRequest.SUSPEND_EVENT_THREAD);
        request.enable();
      }
    }
  }

  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* this is a console application */
  })
  private boolean handleEventSet(EventSet eventSet, EventIterator i, AtomicBoolean resume) {
    try {
      final Event event = i.next();

      if (event instanceof VMDisconnectEvent) {
        System.out.println("");
        System.out.println("AC Debugger: Attached VM has disconnected");
        context.stop();
        return true;
      }
      final EventRequest request = event.request();

      if (request == null) {
        return true;
      }
      final Object oinfo = request.getProperty(Debugger.INFO_KEY);

      if (oinfo instanceof PendingBreakpointInfo) {
        final PendingBreakpointInfo info = (PendingBreakpointInfo) oinfo;

        request.disable();
        debug.getEventRequestManager().deleteEventRequest(request);
        add(info.getProcessor(), info.getLocation());
      } else if (oinfo instanceof BreakpointInfo) {
        final String method = ((BreakpointInfo) oinfo).getLocation().getMethod();

        if ((method != null) && (event instanceof LocatableEvent)) {
          final Method currentMethod =
              ((LocatableEvent) event).thread().frame(0).location().method();

          if ((currentMethod == null) || !method.equals(currentMethod.name())) {
            // skip this event as its location doesn't match the expected method
            return true;
          }
        }
        // process the set on a separate thread an let the thread resume it when all done
        resume.set(false);
        executor.execute(new EventSetThread(eventSet, i, event));
        // break out of processing the event set as we will do that on the separate thread
        return false;
      }
    } catch (VirtualMachineError e) {
      resume.set(true);
      throw e;
    } catch (Throwable t) {
      resume.set(true);
      t.printStackTrace();
    }
    return true;
  }

  /** Used to process a single event set. */
  class EventSetThread implements Runnable {
    private final long sequence = Debugger.this.sequence.incrementAndGet();
    private final EventSet eventSet;
    private final EventIterator i;
    private final Event initialEvent;

    private EventSetThread(EventSet eventSet, EventIterator i, Event initialEvent) {
      this.eventSet = eventSet;
      this.i = i;
      this.initialEvent = initialEvent;
    }

    @SuppressWarnings({
      "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
      "squid:S106", /* this is a console application */
      "squid:S1148" /* this is a console application */
    })
    @Override
    public void run() {
      final Thread thread = Thread.currentThread();
      final String name = thread.getName();

      try {
        if (!context.isRunning()) { // we are done so ignore!
          return;
        }
        Event event = null;
        long seq = 0L;

        do {
          // start the first event we got above and then continue on with the other
          // events in the set
          event = (event == null) ? initialEvent : i.next();
          final EventRequest request = event.request();
          final BreakpointInfo info = (BreakpointInfo) request.getProperty(Debugger.INFO_KEY);

          thread.setName(info + "-" + sequence + "-" + (++seq));
          try {
            info.process(new DebugImpl(context, event));
          } catch (VirtualMachineError e) {
            throw e;
          } catch (Throwable t) {
            t.printStackTrace();
          }
        } while (i.hasNext());
      } finally {
        thread.setName(name);
        eventSet.resume();
      }
    }
  }

  /** Implementation of the debug interface providing support for adding new breakpoints. */
  class DebugImpl extends Debug {
    DebugImpl(DebugContext context, Event event) {
      super(context, event);
    }

    DebugImpl(DebugContext context, VirtualMachine vm) {
      super(context, vm);
    }

    @Override
    public void add(BreakpointProcessor processor) throws Exception {
      Debugger.this.add(processor);
    }
  }
}
