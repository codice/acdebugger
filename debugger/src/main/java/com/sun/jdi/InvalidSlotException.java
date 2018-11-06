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
// NOSONAR - squid:S1191 - Using the Java debugger API
package com.sun.jdi; // NOSONAR

/**
 * Thrown to indicate that the requested operation cannot be completed because the specified slot
 * number is invalid.
 */
public class InvalidSlotException extends RuntimeException {
  private static final long serialVersionUID = -191951000000007922L;

  public InvalidSlotException() {
    super();
  }

  public InvalidSlotException(String s) {
    super(s);
  }
}
