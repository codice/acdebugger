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

import com.sun.jdi.ObjectReference; // NOSONAR
import com.sun.jdi.ThreadReference; // NOSONAR
import com.sun.jdi.request.EventRequest; // NOSONAR
import java.util.Set;
import java.util.stream.Stream;
import org.codice.acdebugger.api.BreakpointProcessor;
import org.codice.acdebugger.api.Debug;
import org.codice.acdebugger.impl.BreakpointInfo;
import org.codice.acdebugger.impl.BreakpointLocation;

/**
 * Breakpoint processor specific to Eclipse's service registry which is used to detect all calls
 * that checks if a given bundle has permissions to receive service events.
 *
 * <p><i>Note:</i> Verified with org.eclipse.osgi 3.12.50.
 */
public class HasListenServicePermissionProcessor implements BreakpointProcessor {
  @Override
  public final Stream<BreakpointLocation> locations() {
    return Stream.of(
        BreakpointProcessor.createLocationFor(
            "Lorg/eclipse/osgi/internal/serviceregistry/ServiceRegistry;", 1096));
  }

  @Override
  public EventRequest createRequest(Debug debug, BreakpointLocation l) throws Exception {
    return debug.eventRequestManager().createBreakpointRequest(l.getLocation());
  }

  @Override
  public void process(BreakpointInfo info, Debug debug) throws Exception {
    final ThreadReference thread = debug.thread();
    final ObjectReference context = (ObjectReference) thread.frame(0).getArgumentValues().get(1);
    final ObjectReference serviceEvent =
        (ObjectReference) thread.frame(0).getArgumentValues().get(0);
    final ObjectReference domain =
        (ObjectReference) thread.frame(0).getValue(thread.frame(0).visibleVariableByName("domain"));
    final String bundle = debug.bundles().get(context);
    final Set<String> permissionStrings =
        debug.permissions().findMissingServicePermissionStrings(bundle, domain, serviceEvent);

    if (!permissionStrings.isEmpty()) {
      // only record if the domain didn't have the permission to start with
      debug.record(new SecurityServicePermissionImpliedInformation(bundle, permissionStrings));
    } // else - the bundle has or we already granted the permissions
    // force early return as if the domain had the permission since we either tested it above
    // and it was true or it is false and we are recording an error while still continuing as
    // if the domain had the permission
    if (!debug.isFailing() && debug.isContinuous()) {
      // only force in continuous mode and if we are not failing
      thread.forceEarlyReturn(debug.reflection().toMirror(true));
    } // else - let it go through the normal process and fail if need be
  }
}
