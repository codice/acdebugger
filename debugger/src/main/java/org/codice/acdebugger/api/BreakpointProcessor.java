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

// NOSONAR - squid:S1191 - Using the Java debugger API

import com.sun.jdi.request.EventRequest; // NOSONAR
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.codice.acdebugger.impl.BreakpointInfo;
import org.codice.acdebugger.impl.BreakpointLocation;

/**
 * Interface for all breakpoint processors. Processors provides information about locations they
 * want to be called for and are then consulted to create requests with the debugger and finally
 * consulted to process the debugging callback.
 */
public interface BreakpointProcessor {
  /**
   * Creates a location for anywhere in a given class.
   *
   * @param signature the signature of the class
   * @return a newly created location
   */
  public static BreakpointLocation createLocationFor(String signature) {
    return new BreakpointLocation(signature, null, -1);
  }

  /**
   * Creates a location for a specific method of a given class.
   *
   * @param signature the signature of the class
   * @param method the name of the method
   * @return a newly created location
   */
  public static BreakpointLocation createLocationFor(String signature, String method) {
    return new BreakpointLocation(signature, method, -1);
  }

  /**
   * Creates a location for a specific line in a given class.
   *
   * @param signature the signature of the class
   * @param lineNum the line number in the class file
   * @return a newly created location
   */
  public static BreakpointLocation createLocationFor(String signature, int lineNum) {
    return new BreakpointLocation(signature, null, lineNum);
  }

  /**
   * Gets the locations where to create debug requests for this processor.
   *
   * @return a stream of locations where to create debug requests
   */
  public Stream<BreakpointLocation> locations();

  /**
   * Called to create a debug request at the specified location.
   *
   * @param debug the debug information
   * @param location the location where to create a debug request (will be one of the location
   *     object provided by {@link #locations()} with the class reference properly loaded and
   *     initialized
   * @return a newly created debug request or <code>null</code> if none needed
   * @throws Exception if an error occurs while creating the debug request
   */
  @SuppressWarnings("squid:S00112" /* Forced to by the Java debugger API */)
  @Nullable
  public EventRequest createRequest(Debug debug, BreakpointLocation location) throws Exception;

  /**
   * Called to process a specific breakpoint/debug request callback.
   *
   * @param info the information about the breakpoint
   * @param debug the debug information
   * @throws Exception if an error occurs while processing the breakpoint callback
   */
  @SuppressWarnings("squid:S00112" /* Forced to by the Java debugger API */)
  public void process(BreakpointInfo info, Debug debug) throws Exception;
}
