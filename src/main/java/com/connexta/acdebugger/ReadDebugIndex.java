package com.connexta.acdebugger;

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

public class ReadDebugIndex {

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

  private static <T> T getValue(Class clazz, Object instance, String fieldName, Class<T> fieldType)
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

  static int getIndex(ThreadReference threadRef)
      throws IncompatibleThreadStateException, NoSuchFieldException, NoSuchMethodException,
          InvocationTargetException, IllegalAccessException, ClassNotFoundException,
          InstantiationException {
    StackFrame frame = threadRef.frame(0);
    VirtualMachine vm = frame.virtualMachine();
    long frameId = getValue(frame.getClass(), frame, "id", long.class);

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
      Value[] values = getValue(reply.getClass(), reply, "values", Value[].class);

      //      Field replyField = reply.getClass().getDeclaredField("values");
      //      replyField.setAccessible(true);
      //      Value[] values = (Value[]) replyField.get(reply);

      return ((IntegerValue) values[0]).value();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return 0;
  }
}
