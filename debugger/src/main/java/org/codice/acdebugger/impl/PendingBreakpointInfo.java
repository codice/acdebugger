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

import com.sun.jdi.request.ClassPrepareRequest;
import org.codice.acdebugger.api.BreakpointProcessor;

/**
 * Information about a pending breakpoint to register which is awaiting its corresponding class to
 * be loaded.
 */
@SuppressWarnings("squid:S1191" /* Using the Java debugger API */)
public class PendingBreakpointInfo {
  private final ClassPrepareRequest request;
  private final BreakpointProcessor processor;
  private final BreakpointLocation location;

  PendingBreakpointInfo(
      ClassPrepareRequest request, BreakpointProcessor processor, BreakpointLocation location) {
    this.request = request;
    this.processor = processor;
    this.location = location;
  }

  public BreakpointLocation getLocation() {
    return location;
  }

  public BreakpointProcessor getProcessor() {
    return processor;
  }
}
