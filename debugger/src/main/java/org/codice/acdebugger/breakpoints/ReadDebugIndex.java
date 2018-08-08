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

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Class used to read the current index in the access controller were the security exception is
 * being thrown.
 */
@SuppressWarnings({
  "squid:S1191", /* Using the Java debugger API */
  "squid:S1148" /* this is a console application */
})
class ReadDebugIndex {
  private static final byte INT_BYTE_SIG = (byte) 73;
  private static Class<?> slotInfoClass;
  private static Constructor<?> slotCtor;
  private static Method enqueueCommand;
  private static Method waitForReply;

  static {
    try {
      String getValuesClassName = "com.sun.tools.jdi.JDWP$StackFrame$GetValues";
      slotInfoClass = Class.forName(getValuesClassName + "$SlotInfo");
      slotCtor = slotInfoClass.getDeclaredConstructor(int.class, byte.class);
      slotCtor.setAccessible(true);

      Class<?> ourGetValuesClass = Class.forName(getValuesClassName);
      enqueueCommand = getDeclaredMethodByName(ourGetValuesClass, "enqueueCommand");
      waitForReply = getDeclaredMethodByName(ourGetValuesClass, "waitForReply");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static Method getDeclaredMethodByName(Class aClass, String methodName)
      throws NoSuchMethodException {
    for (Method method : aClass.getDeclaredMethods()) {
      if (methodName.equals(method.getName())) {
        method.setAccessible(true);
        return method;
      }
    }
    throw new NoSuchMethodException(aClass.getName() + "." + methodName);
  }

  private static <T> T getValue(Class clazz, Object instance, String fieldName)
      throws NoSuchFieldException, IllegalAccessException {
    Field declaredField = clazz.getDeclaredField(fieldName);
    declaredField.setAccessible(true);
    return (T) declaredField.get(instance);
  }

  private static Object getSlotArray()
      throws IllegalAccessException, InvocationTargetException, InstantiationException {
    Object slotArray = Array.newInstance(slotInfoClass, 1);
    Object slot = slotCtor.newInstance(3, INT_BYTE_SIG);
    Array.set(slotArray, 0, slot);

    return slotArray;
  }

  @SuppressWarnings("squid:CommentedOutCodeLine" /* alternative way */)
  static int getIndex(ThreadReference threadRef)
      throws IncompatibleThreadStateException, NoSuchFieldException, NoSuchMethodException,
          InvocationTargetException, IllegalAccessException, InstantiationException {
    StackFrame frame = threadRef.frame(0);
    VirtualMachine vm = frame.virtualMachine();
    long frameId = getValue(frame.getClass(), frame, "id");

    Method state = vm.getClass().getDeclaredMethod("state");
    state.setAccessible(true);
    final Object vmSemaphore = state.invoke(vm);
    Object ps;
    Object slotArray = getSlotArray();

    try {
      synchronized (vmSemaphore) {
        ps = enqueueCommand.invoke(null, vm, frame.thread(), frameId, slotArray);
      }

      final Object reply = waitForReply.invoke(null, vm, ps);
      Value[] values = getValue(reply.getClass(), reply, "values");

      //      Field replyField = reply.getClass().getDeclaredField("values");
      //      replyField.setAccessible(true);
      //      Value[] values = (Value[]) replyField.get(reply);

      return ((IntegerValue) values[0]).value();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return 0;
  }

  private ReadDebugIndex() {
    throw new UnsupportedOperationException();
  }
}
