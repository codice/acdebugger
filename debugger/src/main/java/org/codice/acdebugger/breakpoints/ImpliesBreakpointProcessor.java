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
package org.codice.acdebugger.breakpoints;

import com.google.common.collect.ImmutableSet;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.EventRequest;
import java.security.Permission;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.codice.acdebugger.ACDebugger;
import org.codice.acdebugger.api.BreakpointProcessor;
import org.codice.acdebugger.api.Debug;
import org.codice.acdebugger.impl.BreakpointInfo;
import org.codice.acdebugger.impl.BreakpointLocation;

/**
 * Old version of a breakpoint processor that was attempting to intercept all calls to {@link
 * java.security.ProtectionDomain#implies(Permission)}.
 */
@SuppressWarnings("squid:S1191" /* Using the Java debugger API */)
public class ImpliesBreakpointProcessor implements BreakpointProcessor {
  private static final Set<BreakpointLocation> LOCATIONS =
      ImmutableSet.of(
          BreakpointProcessor.createLocationFor("Ljava/security/ProtectionDomain;", 290),
          BreakpointProcessor.createLocationFor("Ljava/security/Permissions;", 185),
          BreakpointProcessor.createLocationFor(
              "Lorg/eclipse/osgi/internal/permadmin/SecurityAdmin;", 131),
          BreakpointProcessor.createLocationFor(
              "Lorg/eclipse/osgi/internal/permadmin/SecurityAdmin;", 134) /*,
        BreakpointLocation.createLocationFor("Lorg/eclipse/osgi/internal/permadmin/BundlePermissions;", 86)*/);
  private static final Set<String> NESTED_LOCATIONS =
      Stream.concat(
              Stream.of("Ljava/security/AccessControlContext;"),
              ImpliesBreakpointProcessor.LOCATIONS
                  .stream()
                  .map(BreakpointLocation::getClassSignature)
                  .map(Object::toString))
          .map(s -> s + ':')
          .collect(Collectors.toSet());

  @Override
  public final Stream<BreakpointLocation> locations() {
    return ImpliesBreakpointProcessor.LOCATIONS.stream();
  }

  private boolean isNested(String location) {
    /*|| caller.startsWith("org.eclipse.osgi.internal.permadmin.BundlePermissions:")*/
    // do nothing when it is called from these locations
    return ImpliesBreakpointProcessor.NESTED_LOCATIONS.stream().anyMatch(location::startsWith);
  }

  @Override
  public EventRequest createRequest(Debug debug, BreakpointLocation l) throws Exception {
    return debug.getEventRequestManager().createBreakpointRequest(l.getLocation());
  }

  @SuppressWarnings({
    "squid:CommentedOutCodeLine", /* still under development */
    "squid:S106" /* still under development */
  })
  @Override
  public void process(BreakpointInfo info, Debug debug) throws Exception {
    final ThreadReference thread = debug.thread();

    if (thread
        .frames()
        .stream()
        .skip(1) // skip current frame
        .map(StackFrame::location)
        .map(Object::toString)
        .anyMatch(this::isNested)) {
      // do nothing when it is called from a nested location
      return;
    }
    final StackFrame frame = thread.frame(0);
    final ObjectReference permission = (ObjectReference) frame.getArgumentValues().get(0);

    System.out.println(ACDebugger.PREFIX);
    System.out.println(
        ACDebugger.PREFIX + "PERMISSIONS: " + debug.permissions().getPermissionStrings(permission));
    for (int i = 0; i < debug.thread().frameCount(); i++) {
      final StackFrame f = debug.thread().frame(i);
      final com.sun.jdi.Location location =
          f.location(); // cache before we invoke anything on the thread
      // final String b = debug.getBundle(f);
      // final StackFrameInformation currentFrame = new StackFrameInformation(b, location);

      System.out.println(ACDebugger.PREFIX + ">>>>>> " + location);
    }
    // force early return as if the domain had permission, that way we simulate no security manager;
    // allowing us to record what is missing while continuing to run
    thread.forceEarlyReturn(debug.reflection().toMirror(true));
  }
}
