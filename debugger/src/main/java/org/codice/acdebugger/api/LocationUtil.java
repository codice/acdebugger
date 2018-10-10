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

import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import javax.annotation.Nullable;

/** Provides location utility functionality. */
@SuppressWarnings("squid:S1191" /* Using the Java debugger API */)
public interface LocationUtil {
  /**
   * Gets the location (i.e. bundle name or domain location) for the given domain.
   *
   * @param domain the domain to find the corresponding location
   * @return the corresponding location or <code>null</code> if none exist
   */
  @Nullable
  public String get(@Nullable Value domain);

  /**
   * Gets the location (i.e. bundle name or domain location) for the given stack frame.
   *
   * @param frame the stack frame to find the corresponding domain location
   * @return the corresponding location or <code>null</code> if none exist
   */
  @Nullable
  public String get(StackFrame frame);
}
