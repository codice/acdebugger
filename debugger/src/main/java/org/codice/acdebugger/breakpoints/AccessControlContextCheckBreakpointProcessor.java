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

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.EventRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.codice.acdebugger.api.BreakpointProcessor;
import org.codice.acdebugger.api.Debug;
import org.codice.acdebugger.api.SecuritySolution;
import org.codice.acdebugger.impl.BreakpointInfo;
import org.codice.acdebugger.impl.BreakpointLocation;

/**
 * Defines a breakpoint processor capable of intercepting security exceptions in the access
 * controller before they get thrown out.
 */
@SuppressWarnings("squid:S1191" /* Using the Java debugger API */)
public class AccessControlContextCheckBreakpointProcessor implements BreakpointProcessor {
  @Override
  public Stream<BreakpointLocation> locations() {
    return Stream.of(
        BreakpointProcessor.createLocationFor("Ljava/security/AccessControlContext;", 472));
  }

  @Override
  public EventRequest createRequest(Debug debug, BreakpointLocation l) throws Exception {
    return debug.getEventRequestManager().createBreakpointRequest(l.getLocation());
  }

  @Override
  public void process(BreakpointInfo info, Debug debug) throws Exception {
    final ThreadReference thread = debug.thread();
    final ArrayReference context =
        debug
            .reflection()
            .get(thread.frame(0).thisObject(), "context", "[Ljava/security/ProtectionDomain;");
    final int local_i = ReadDebugIndex.getIndex(thread);
    final ObjectReference permission = (ObjectReference) thread.frame(0).getArgumentValues().get(0);
    final SecurityCheckInformation security =
        new SecurityCheckInformation(debug, context, local_i, permission);

    if (security.getFailedBundle() != null) {
      if (!security.isAcceptable()) {
        // check if we have only one solution and that solution is to only grant permission(s)
        // (no privileged blocks) in which case we shall cache them to avoid going through all
        // of this again
        final List<SecuritySolution> solutions = security.analyze();

        if (solutions.size() == 1) {
          final SecuritySolution solution = solutions.get(0);
          final Set<String> grantedBundles = solution.getGrantedBundles();

          if (!grantedBundles.isEmpty() && solution.getDoPrivilegedLocations().isEmpty()) {
            solution
                .getGrantedBundles()
                .forEach(b -> debug.permissions().grant(b, solution.getPermissions()));
          }
        }
      }
      debug.record(security);
    } // else - could be caused because we already processed something and artificially granted a
    //          bundle the missing permissions we use in the ctor to determine if bundles have
    //          permissions so even if the VM tells us there was an exception, we skip over it right
    //          away
    if (debug.isContinuous() && !security.isAcceptable()) {
      // force early return as if no exception is thrown, that way we simulate no security
      // exceptions; allowing us to record what is missing while continuing to run
      thread.forceEarlyReturn(debug.reflection().getVoid());
    } // else - let it fail as intended since we aren't continuing or again it is an acceptable
    //          failure
  }
}
