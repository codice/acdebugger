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

import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.MethodExitRequest;
import java.util.stream.Stream;
import org.codice.acdebugger.api.BreakpointProcessor;
import org.codice.acdebugger.api.Debug;
import org.codice.acdebugger.impl.Backdoor;
import org.codice.acdebugger.impl.BreakpointInfo;
import org.codice.acdebugger.impl.BreakpointLocation;

/** Breakpoint processor capable of connecting to the backdoor bundle in the attached VM. */
@SuppressWarnings("squid:S1191" /* Using the Java debugger API */)
public class BackdoorProcessor implements BreakpointProcessor {
  @Override
  public Stream<BreakpointLocation> locations() {
    return Stream.of(BreakpointProcessor.createLocationFor(Backdoor.CLASS_SIGNATURE, "start"));
  }

  @Override
  public EventRequest createRequest(Debug debug, BreakpointLocation location) {
    if (debug.backdoor().init(debug)) { // we are initialized or we managed to initialize
      return null;
    }
    final MethodExitRequest request = debug.getEventRequestManager().createMethodExitRequest();

    request.addClassFilter(location.getClassReference());
    return request;
  }

  @Override
  public void process(BreakpointInfo info, Debug debug) throws Exception {
    final MethodExitEvent event = (MethodExitEvent) debug.getEvent();
    final ThreadReference thread = event.thread();

    debug.backdoor().init(debug, thread.frame(0).thisObject());
    // no longer need this breakpoint since we found the reference we needed
    event.request().disable();
  }
}
