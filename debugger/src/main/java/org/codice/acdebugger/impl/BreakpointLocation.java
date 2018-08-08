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

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import javax.annotation.Nullable;

/** This class keeps track of a location in the code where we want to debug. */
@SuppressWarnings("squid:S1191" /* Using the Java debugger API */)
public class BreakpointLocation {
  private final String classSignature;
  private final String method;
  private final int linenum;
  private volatile ReferenceType clazz = null;

  /**
   * Creates a location in a given class.
   *
   * @param signature the signature of the class
   * @param method the optional name of the method
   * @param linenum the optional line number in the class file (<code>-1</code> if unknown)
   */
  public BreakpointLocation(String signature, @Nullable String method, int linenum) {
    this.classSignature = signature;
    this.method = method;
    this.linenum = linenum;
  }

  /**
   * Gets the reference to the class associated with this location.
   *
   * @return the reference to the class associated with this location
   * @throws IllegalStateException if the reference has not been detected yet
   */
  public ReferenceType getClassReference() {
    if (clazz == null) {
      throw new IllegalStateException("class reference not available");
    }
    return clazz;
  }

  /**
   * Gets the signature for the class associated with this location.
   *
   * @return the signature for the class associated with this location
   */
  public String getClassSignature() {
    return classSignature;
  }

  /**
   * Gets the name for the class associated with this location.
   *
   * @return the name for the class associated with this location
   */
  public String getClassName() {
    return classSignature.substring(1, classSignature.length() - 1).replace('/', '.');
  }

  /**
   * Gets the method associated with this location.
   *
   * @return the method associated with this location or <code>null</code> if this location is not
   *     associated with a method
   */
  @Nullable
  public String getMethod() {
    return method;
  }

  /**
   * Gets the line number in the class associated with this location.
   *
   * @return the line number in the class associated with this location or <code>-1</code> if this
   *     location is not associated with a particular line number
   */
  public int getLineNumber() {
    return linenum;
  }

  /**
   * Gets a corresponding {@link Location} object.
   *
   * @return a corresponding {@link Location} object or <code>null</code> if the class reference has
   *     not been detected yet or if this location is not associated with a particular line number
   * @throws AbsentInformationException if unable to find the corresponding location information
   */
  public Location getLocation() throws AbsentInformationException {
    return ((clazz != null) && (linenum != -1)) ? clazz.locationsOfLine(linenum).get(0) : null;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    sb.append(getClassName());
    if (method != null) {
      sb.append('.').append(method).append("()");
    }
    if (linenum != -1) {
      sb.append(':').append(linenum);
    }
    return sb.toString();
  }

  /**
   * Sets the reference to the class associated with this location.
   *
   * @param clazz the reference to the class for this location
   */
  void setClassReference(ReferenceType clazz) {
    this.clazz = clazz;
  }
}
