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

// NOSONAR - squid:S1191 - Using the Java debugger API

import com.google.common.annotations.VisibleForTesting;
import com.sun.jdi.ArrayReference; // NOSONAR
import com.sun.jdi.EnhancedStackFrame; // NOSONAR
import com.sun.jdi.ObjectReference; // NOSONAR
import com.sun.jdi.StackFrame; // NOSONAR
import com.sun.jdi.ThreadReference; // NOSONAR
import com.sun.jdi.request.EventRequest; // NOSONAR
import java.security.Permission;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.codice.acdebugger.api.BreakpointProcessor;
import org.codice.acdebugger.api.Debug;
import org.codice.acdebugger.api.ReflectionUtil;
import org.codice.acdebugger.api.SecuritySolution;
import org.codice.acdebugger.impl.BreakpointInfo;
import org.codice.acdebugger.impl.BreakpointLocation;

/**
 * Defines a breakpoint processor capable of intercepting security exceptions in the access
 * controller before they get thrown out.
 */
public class AccessControlContextCheckProcessor implements BreakpointProcessor {
  /**
   * Slot index in the {@link java.security.AccessControlContext#checkPermission(Permission)} method
   * corresponding to the local i loop variable used when the security manager is iterating all
   * domains in the context. We are forced to use a slot index and not the variable name because
   * that class is never compiled with debug information.
   */
  @VisibleForTesting static final int LOCAL_I_SLOT_INDEX = 3;

  @Override
  public Stream<BreakpointLocation> locations() {
    return Stream.of(
        BreakpointProcessor.createLocationFor("Ljava/security/AccessControlContext;", 472));
  }

  @Override
  public EventRequest createRequest(Debug debug, BreakpointLocation l) throws Exception {
    return debug.eventRequestManager().createBreakpointRequest(l.getLocation());
  }

  @Override
  public void process(BreakpointInfo info, Debug debug) throws Exception {
    final ThreadReference thread = debug.thread();
    final ReflectionUtil reflection = debug.reflection();
    final ArrayReference context =
        reflection.get(
            thread.frame(0).thisObject(), "context", "[Ljava/security/ProtectionDomain;");
    final EnhancedStackFrame frame = enhance(thread.frame(0));
    final int local_i =
        reflection.fromMirror(
            frame.getValue(AccessControlContextCheckProcessor.LOCAL_I_SLOT_INDEX, "I"));
    final ObjectReference permission = (ObjectReference) frame.getArgumentValues().get(0);
    final SecurityCheckInformation security = process(debug, context, local_i, permission);

    if (security.getFailedDomain() != null) {
      // check if we have only one solution and that solution is to only grant permission(s)
      // (no privileged blocks) in which case we shall cache them to avoid going through all
      // of this again
      final List<SecuritySolution> solutions = security.analyze();

      if (solutions.size() == 1) {
        final SecuritySolution solution = solutions.get(0);
        final Set<String> grantedDomains = solution.getGrantedDomains();

        if (!grantedDomains.isEmpty() && solution.getDoPrivilegedLocations().isEmpty()) {
          grantedDomains.forEach(d -> debug.permissions().grant(d, solution.getPermissions()));
        }
      }
      debug.record(security);
    } // else - could be caused because we already processed something and artificially granted a
    //          domain the missing permissions we use in the ctor to determine if domains have
    //          permissions so even if the VM tells us there was an exception, we skip over it right
    //          away
    if (!debug.isFailing() && debug.isContinuous() && !security.isAcceptable()) {
      // force early return as if no exception is thrown, that way we simulate no security
      // exceptions; allowing us to record what is missing while continuing to run
      thread.forceEarlyReturn(reflection.getVoid());
    } // else - let it fail as intended since we failing or we aren't continuing or again it is
    //          an acceptable failure
  }

  @VisibleForTesting
  EnhancedStackFrame enhance(StackFrame frame) {
    return EnhancedStackFrame.of(frame);
  }

  @VisibleForTesting
  @SuppressWarnings({
    "squid:S00117", /* name is clearer that way */
    "squid:S00112" /* Forced to by the Java debugger API */
  })
  SecurityCheckInformation process(
      Debug debug, ArrayReference context, int local_i, ObjectReference permission)
      throws Exception {
    return new SecurityCheckInformation(
        debug,
        new AccessControlContextInfo( // domains in the context can only be object references
            debug, (List<ObjectReference>) (List) context.getValues(), local_i, permission));
  }
}
