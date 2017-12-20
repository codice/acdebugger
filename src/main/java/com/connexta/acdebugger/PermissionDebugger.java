package com.connexta.acdebugger;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.Field;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PermissionDebugger {

  private static final String PORT_KEY = "port";

  private final String transport;

  private final String port;

  private VirtualMachine vm;

  private SortedSetMultimap<String, String> missingPerms = TreeMultimap.create();

  public PermissionDebugger(String transport, String port) {
    this.transport = transport;
    this.port = port;
  }

  public PermissionDebugger attach() throws IOException, IllegalConnectorArgumentsException {
    VirtualMachineManager vmm = Bootstrap.virtualMachineManager();

    List<AttachingConnector> connectors = vmm.attachingConnectors();
    AttachingConnector connector =
        connectors
            .stream()
            .filter(c -> c.transport().name().equals(transport))
            .findFirst()
            .orElseThrow(
                () -> new IOException(String.format("Failed to find transport %s", transport)));

    Map<String, Argument> map = connector.defaultArguments();
    Argument portArg = map.get(PORT_KEY);
    portArg.setValue(port);
    map.put(PORT_KEY, portArg);
    vm = connector.attach(map);

    return this;
  }

  public void process(String accessControlClass, int lineNum) throws Exception {
    EventRequestManager erm = vm.eventRequestManager();
    Optional<ReferenceType> classRef = getFirstClassReference(accessControlClass);
    if (!classRef.isPresent()) {
      throw new ClassNotFoundException(String.format("Class %s not found", accessControlClass));
    }

    ReferenceType accRef = classRef.get();
    Field contextField = accRef.fieldByName("context");

    Location location = accRef.locationsOfLine(lineNum).get(0);

    BreakpointRequest br = erm.createBreakpointRequest(location);
    loop(br, contextField);
  }

  private List<ReferenceType> getClassReferences(String name) {
    return vm.classesByName(name);
  }

  private Optional<ReferenceType> getFirstClassReference(String name) {
    return getClassReferences(name).stream().findFirst();
  }

  private void loop(BreakpointRequest br, Field contextField) throws Exception {
    int lineNumber = br.location().lineNumber();

    br.setSuspendPolicy(BreakpointRequest.SUSPEND_EVENT_THREAD);
    br.enable();
    EventQueue evtQueue = vm.eventQueue();
    boolean run = true;
    while (run) {
      EventSet evtSet = evtQueue.remove();
      EventIterator evtIter = evtSet.eventIterator();
      while (evtIter.hasNext()) {
        try {
          Event evt = evtIter.next();
          EventRequest evtReq = evt.request();
          if (evtReq instanceof BreakpointRequest
              && ((BreakpointRequest) evtReq).location().lineNumber() == lineNumber) {
            new BreakpointProcessor(missingPerms)
                .processBreakpoint(contextField, (BreakpointEvent) evt);

            // TODO: 12/20/17 Remove when full loop processing is restored
            // run = false;
            System.out.println(missingPerms);
            missingPerms.clear();
          }
        } finally {
          evtSet.resume();
        }
      }
    }

    //    System.out.println(missingPerms);
    //    printPerms(missingPerms);
  }

  //  private void printPerms(
  //      SortedSetMultimap<String, String> missingPerms) {
  //    Map<String, Collection<String>> stringCollectionMap = missingPerms.asMap();
  //    for (String key : stringCollectionMap.keySet()) {
  //      sout
  //    }
  //  }
}
