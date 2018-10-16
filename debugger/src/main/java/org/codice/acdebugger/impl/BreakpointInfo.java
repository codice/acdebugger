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

// NOSONAR - squid:S1191 - Using the Java debugger API

import com.sun.jdi.request.EventRequest; // NOSONAR
import org.codice.acdebugger.api.BreakpointProcessor;
import org.codice.acdebugger.api.Debug;

/** This class provides information about a current breakpoint registered with the debugger. */
public class BreakpointInfo {
  private final EventRequest request;
  private final BreakpointProcessor processor;
  private final BreakpointLocation location;

  /**
   * Constructs a new breapoint information.
   *
   * @param request the event request registered with the debugger
   * @param processor the breakpoint processor to use for processing the breakpoint callback
   * @param location the location registered for the breakpoint callback
   */
  public BreakpointInfo(
      EventRequest request, BreakpointProcessor processor, BreakpointLocation location) {
    this.request = request;
    this.processor = processor;
    this.location = location;
  }

  /**
   * Called by the debugger to process the current breakpoint callback.
   *
   * @param debug the current debug information for the callback
   * @throws Exception if any errors occurs while processing the breakpoint
   */
  public void process(Debug debug) throws Exception {
    processor.process(this, debug);
  }

  /**
   * Gets the location where the breakpoint is registered.
   *
   * @return the location where the breakpoint registered
   */
  public BreakpointLocation getLocation() {
    return location;
  }

  @Override
  public String toString() {
    return location.toString();
  }
}
