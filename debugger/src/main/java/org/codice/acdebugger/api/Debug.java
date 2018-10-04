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
package org.codice.acdebugger.api;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.EventRequestManager;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.codice.acdebugger.impl.Backdoor;
import org.codice.acdebugger.impl.DebugContext;

/** This class keeps information about the current debugging session/callback. */
@SuppressWarnings("squid:S1191" /* Using the Java debugger API */)
public abstract class Debug {
  /** The debug context for the current debug session. */
  private final DebugContext context;

  private final ReflectionUtil reflection;

  /**
   * The current debugging event or <code>null</code> if this debug instance is not associated with
   * a debugging callback.
   */
  @Nullable private final Event event;

  /**
   * Creates a debug object representing a specific debug event/callback.
   *
   * @param context the debug context for the current debug session
   * @param event the event corresponding to the breakpoint callback
   */
  public Debug(DebugContext context, Event event) {
    this.context = context;
    this.reflection =
        new ReflectionUtil(
            context,
            event.virtualMachine(),
            (event instanceof LocatableEvent) ? ((LocatableEvent) event).thread() : null);
    this.event = event;
  }

  /**
   * Creates a debug object representing the session outside of normal breakpoint callbacks.
   *
   * @param context the debug context for the current debug session
   * @param vm the virtual machine to which the debugger is attached
   */
  public Debug(DebugContext context, VirtualMachine vm) {
    this.context = context;
    this.reflection = new ReflectionUtil(context, vm, null);
    this.event = null;
  }

  /**
   * Gets the virtual machine to which this debugger is attached.
   *
   * @return the virtual machine to which this debugger is attached
   */
  public VirtualMachine virtualMachine() {
    return reflection.getVirtualMachine();
  }

  /**
   * Gets the current thread associated with this debug instance.
   *
   * @return the current thread associated with this debug instance
   * @throws IllegalStateException if currently not associated with a thread
   */
  public ThreadReference thread() {
    return reflection.getThread();
  }

  /**
   * Accesses the backdoor utility.
   *
   * @return the backdoor utility
   */
  public Backdoor backdoor() {
    return context.getBackdoor();
  }

  /**
   * Accesses reflection-like functionality.
   *
   * @return a reflection instance onto which reflection-type methods can be invoked
   */
  public ReflectionUtil reflection() {
    return reflection;
  }

  /**
   * Accesses permission-specific functionality.
   *
   * @return a permission utility instance onto which permission-specific methods can be invoked
   */
  public PermissionUtil permissions() {
    return new PermissionUtil(this);
  }

  /**
   * Accesses bundle-specific functionality.
   *
   * @return a bundle utility instance onto which bundle-specific methods can be invoked
   */
  public BundleUtil bundles() {
    return new BundleUtil(this);
  }

  /**
   * Gets the event request manager associated with the debugging session.
   *
   * @return the event request manager associated with the debugging session
   */
  public EventRequestManager getEventRequestManager() {
    return virtualMachine().eventRequestManager();
  }

  /**
   * Gets the current event associated with this debug instance.
   *
   * @return the current event associated with this debug instance
   * @throws IllegalStateException if currently not associated with an event
   */
  public Event getEvent() {
    if (event == null) {
      throw new IllegalStateException("missing event");
    }
    return event;
  }

  /**
   * Retrieves a value given a key from the current debug context's cache.
   *
   * @param <T> the type of the value to retrieve
   * @param key the key for the value to retrieve
   * @return the value currently associated with the given key or <code>null</code> if none
   *     associated
   */
  @Nullable
  public <T> T get(String key) {
    return context.get(key);
  }

  /**
   * If the specified key is not already associated with a value (or is mapped to <code>null</code>
   * ), attempts to compute its value using the given supplier and enters it into this map unless
   * <code>null</code>.
   *
   * @param <T> the type of the value to retrieve
   * @param key the key for the value to retrieve
   * @param supplier the supplier used to generate the first value if none already associated
   * @return the value currently associated with the given key or <code>null</code> if none
   *     associated
   */
  public <T> T computeIfAbsent(String key, Supplier<T> supplier) {
    return context.computeIfAbsent(key, supplier);
  }

  /**
   * Stores a value in the debug context for a given key.
   *
   * @param <T> the type of the value to store
   * @param key the key for the value to store
   * @param obj the value to be stored
   */
  public <T> void put(String key, T obj) {
    context.put(key, obj);
  }

  /**
   * Checks if the debugger is running in continuous mode where it will not fail any security
   * exceptions detected and accumulate and report on all occurrences or if it will stop when the
   * first security failure is detected.
   *
   * @return <code>true</code> if debugging should be continuous and report all failures; <code>
   *     false</code> if it should stop at the first detected security failure
   */
  public boolean isContinuous() {
    return context.isContinuous();
  }

  /**
   * Checks if the debugger is still running. The debugger shall stop after processing the current
   * set of breakpoints once requested to do so.
   *
   * @return <code>true</code> if the debugger is still running; <code>false</code> if it was
   *     requested to stop
   */
  public boolean isRunning() {
    return context.isRunning();
  }

  /**
   * Checks if we are monitoring service event permission checks.
   *
   * @return <code>true</code> if we are monitoring service event permission checks; <code>false
   *     </code> if not
   */
  public boolean isMonitoringService() {
    return context.isMonitoringService();
  }

  /**
   * Checks if we are automatically grant missing permissions when a single solution monitor is
   * found for a given service failure.
   *
   * @return <code>true</code> if we are automatically grating missing permissions; <code>false
   *     </code> if not
   */
  public boolean isGranting() {
    return context.isGranting();
  }

  /**
   * Checks if security exceptions should be left to fail as they would have normally.
   *
   * @return <code>true</code> if security exceptions should be left to fail normally; <code>false
   *     </code> if we should let the VM think there was no error
   */
  public boolean isFailing() {
    return context.isFailing();
  }

  /**
   * Checks whether or not to consider <code>doPrivileged()</code> blocks when analysing security
   * failures.
   *
   * @return <code>true</code> to consider <code>doPrivileged()</code> blocks for possible
   *     solutions; <code>false</code> not to
   */
  public boolean canDoPrivilegedBlocks() {
    return context.canDoPrivilegedBlocks();
  }

  /**
   * Adds a breakpoint to this debugger.
   *
   * @param processor the breakpoint processor to be added
   * @throws Exception if unable to add and register the breakpoint
   */
  @SuppressWarnings("squid:S00112" /* Forced to by the Java debugger API */)
  public abstract void add(BreakpointProcessor processor) throws Exception;

  /**
   * Records a security failure.
   *
   * @param failure the security failure to record
   */
  public void record(SecurityFailure failure) {
    context.record(failure);
  }

  DebugContext getContext() {
    return context;
  }
}
