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

import com.sun.tools.jdi.EnhancedStackFrameImpl; // NOSONAR
import com.sun.tools.jdi.StackFrameImpl; // NOSONAR

/**
 * Extension to the {@link StackFrame} class to provide additional support for classes not compiled
 * with debug information.
 */
public interface EnhancedStackFrame extends StackFrame {
  /**
   * Gets an enhanced version of a given stack frame.
   *
   * @param frame the frame to retrieve an enhanced version of
   * @return the corresponding enhanced version of the given stack frame
   * @throws IllegalArgumentException if the provided frame cannot be enhanced
   */
  public static EnhancedStackFrame of(StackFrame frame) {
    if (frame instanceof EnhancedStackFrame) {
      return (EnhancedStackFrame) frame;
    } else if (!(frame instanceof StackFrameImpl)) {
      throw new IllegalArgumentException("unexpected frame class: " + frame.getClass().getName());
    } else {
      return new EnhancedStackFrameImpl((StackFrameImpl) frame);
    }
  }

  /**
   * Gets the {@link Value} of a local variable in this frame. The variable must be valid for this
   * frame's method.
   *
   * @param slot the slot number of the local variable to be accessed
   * @param signature the signature of the variable to be accessed
   * @return the {@link Value} of the specified variable
   * @throws InvalidStackFrameException if this stack frame has become invalid. Once the frame's
   *     thread is resumed, the stack frame is no longer valid
   * @throws InvalidSlotException if the specified slot is invalid
   */
  public Value getValue(int slot, String signature);
}
