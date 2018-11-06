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
package com.sun.tools.jdi; // NOSONAR

import com.sun.jdi.EnhancedStackFrame; // NOSONAR
import com.sun.jdi.InternalException;
import com.sun.jdi.InvalidSlotException;
import com.sun.jdi.InvalidStackFrameException; // NOSONAR
import com.sun.jdi.Location; // NOSONAR
import com.sun.jdi.Value; // NOSONAR
import com.sun.jdi.VirtualMachine; // NOSONAR
import java.lang.reflect.Field;

/** Implementation of the {@Link EnhancedStackFrame} interface. */
@SuppressWarnings("squid:S2160" /* only designed to add methods; super.equals() is still valid */)
public class EnhancedStackFrameImpl extends StackFrameImpl implements EnhancedStackFrame {
  private static final Field ID_FIELD;

  static {
    try {
      final Field field = StackFrameImpl.class.getDeclaredField("id");

      field.setAccessible(true);
      ID_FIELD = field;
    } catch (Exception e) {
      throw new Error(e); // NOSONAR - Not meant to be catchable so keeping it generic
    }
  }

  private final ThreadReferenceImpl thread;
  private final long id;

  /**
   * Creates an enhanced version of the specified stack frame.
   *
   * @param frame the frame to create an enhanced version of
   */
  public EnhancedStackFrameImpl(StackFrameImpl frame) {
    this(
        frame.virtualMachine(),
        (ThreadReferenceImpl) frame.thread(),
        EnhancedStackFrameImpl.getId(frame),
        frame.location());
  }

  private EnhancedStackFrameImpl(
      VirtualMachine vm, ThreadReferenceImpl thread, long id, Location location) {
    super(vm, thread, id, location);
    this.thread = thread;
    this.id = id;
  }

  // the logic here is modeled after StackFrameImpl.getValues()
  @Override
  public Value getValue(int slot, String signature) {
    validateStackFrame();
    final JDWP.StackFrame.GetValues.SlotInfo[] slots =
        new JDWP.StackFrame.GetValues.SlotInfo[] {
          new JDWP.StackFrame.GetValues.SlotInfo(slot, (byte) signature.charAt(0))
        };
    final PacketStream ps;

    /* protect against defunct frame id */
    synchronized (vm.state()) {
      validateStackFrame();
      ps = JDWP.StackFrame.GetValues.enqueueCommand(vm, thread, id, slots);
    }
    /* actually get it, now that order is guaranteed */
    final ValueImpl[] values;

    try {
      values = JDWP.StackFrame.GetValues.waitForReply(vm, ps).values;
    } catch (JDWPException exc) {
      switch (exc.errorCode()) {
        case JDWP.Error.INVALID_FRAMEID:
        case JDWP.Error.THREAD_NOT_SUSPENDED:
        case JDWP.Error.INVALID_THREAD:
          throw new InvalidStackFrameException();
        case JDWP.Error.INVALID_SLOT:
          throw new InvalidSlotException();
        default:
          throw exc.toJDIException();
      }
    }
    if (values.length != 1) {
      throw new InternalException("Wrong number of values returned from target VM");
    }
    return values[0];
  }

  @SuppressWarnings("squid:S00112" /* Not meant to be catchable so keeping it generic */)
  private static long getId(StackFrameImpl frame) {
    try {
      return EnhancedStackFrameImpl.ID_FIELD.getLong(frame);
    } catch (Exception e) {
      throw new Error(e);
    }
  }
}
