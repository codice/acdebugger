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

import com.sun.jdi.ArrayReference; // NOSONAR
import com.sun.jdi.BooleanValue; // NOSONAR
import com.sun.jdi.ByteValue; // NOSONAR
import com.sun.jdi.CharValue; // NOSONAR
import com.sun.jdi.ClassObjectReference; // NOSONAR
import com.sun.jdi.ClassType; // NOSONAR
import com.sun.jdi.DoubleValue; // NOSONAR
import com.sun.jdi.Field; // NOSONAR
import com.sun.jdi.FloatValue; // NOSONAR
import com.sun.jdi.IntegerValue; // NOSONAR
import com.sun.jdi.LongValue; // NOSONAR
import com.sun.jdi.Method; // NOSONAR
import com.sun.jdi.ObjectCollectedException; // NOSONAR
import com.sun.jdi.ObjectReference; // NOSONAR
import com.sun.jdi.ReferenceType; // NOSONAR
import com.sun.jdi.ShortValue; // NOSONAR
import com.sun.jdi.StringReference; // NOSONAR
import com.sun.jdi.ThreadReference; // NOSONAR
import com.sun.jdi.Type; // NOSONAR
import com.sun.jdi.Value; // NOSONAR
import com.sun.jdi.VirtualMachine; // NOSONAR
import com.sun.jdi.VoidValue; // NOSONAR
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.codice.acdebugger.impl.DebugContext;

/** Provides reflection-style functionality via the debugger's interface. */
public class ReflectionUtil {
  static final String METHOD_SIGNATURE_NO_ARGS_STRING_RESULT = "()Ljava/lang/String;";

  /** Internal key where information about whether a class is an instance of another is cached. */
  private static final String ASSIGNABLE_FROM_CACHE = "debug.assignable.from.cache";

  private static final int SANE_TRY_LIMIT = 15;

  /** The debug context. */
  private final DebugContext context;

  /** The virtual machine to which the debugger is attached. */
  private final VirtualMachine vm;

  /**
   * The current thread associated with this utility or <code>null</code> if it is not associated
   * with a thread.
   */
  @Nullable private final ThreadReference thread;

  /**
   * Creates a new reflection utility instance.
   *
   * @param context the debug context for the current debug session
   * @param vm the virtual machine to which the debugger is attached
   * @param thread the thread associated with this utility or <code>null</code> if none
   */
  ReflectionUtil(DebugContext context, VirtualMachine vm, @Nullable ThreadReference thread) {
    this.context = context;
    this.vm = vm;
    this.thread = thread;
  }

  /**
   * Gets the virtual machine to which this debugger is attached.
   *
   * @return the virtual machine to which this debugger is attached
   */
  public VirtualMachine getVirtualMachine() {
    return vm;
  }

  /**
   * Gets the current thread associated with this debug instance.
   *
   * @return the current thread associated with this debug instance
   * @throws IllegalStateException if currently not associated with a thread
   */
  public ThreadReference getThread() {
    if (thread == null) {
      throw new IllegalStateException("missing thread reference");
    }
    return thread;
  }

  /**
   * Checks if there is a current thread associates with this debug instance.
   *
   * @return <code>false</code> if it is not currently associated with a thread; <code>true</code>
   *     otherwise
   */
  public boolean hasThread() {
    return thread != null;
  }

  /**
   * Determines if the class or interface represented by <code>signature</code> is either the same
   * as, or is a superclass or superinterface of, the class or interface represented by the
   * specified {@code Type} parameter. It returns <code>true</code> if so; otherwise it returns
   * <code>false</code>. If the specified {@code Type} parameter is exactly the same class as the
   * signature; it returns <code>true</code>; otherwise it returns <code>false</code>
   *
   * <p>Specifically, this method tests whether the type represented by the specified {@code Type}
   * parameter can be converted to the type represented by the signature via an identity conversion
   * or via a widening reference conversion.
   *
   * @param signature the signature for the class to check if <code>type</code> can be converted to
   * @param type the type to check if it can be converted to the class represented by <code>
   *     signature</code>
   * @return <code>true</code> if <code>type</code> an be converted to <code>signature</code>;
   *     <code>false</code> otherwise or if <code>type</code> is <code>null</code>
   */
  public boolean isAssignableFrom(String signature, @Nullable Type type) {
    if (type == null) {
      return false;
    } else if (signature.equals(type.signature())) {
      return true;
    } else if (type instanceof ClassType) {
      final ClassType ctype = (ClassType) type;
      // start by checking our cache
      final Map<String, Map<ClassType, Boolean>> signatureCache =
          context.computeIfAbsent(ReflectionUtil.ASSIGNABLE_FROM_CACHE, ConcurrentHashMap::new);
      final Map<ClassType, Boolean> cache =
          signatureCache.computeIfAbsent(signature, s -> new ConcurrentHashMap());
      final Boolean is = cache.get(ctype);

      if (is != null) {
        return is;
      }
      if (ctype
          .allInterfaces()
          .stream()
          .map(ReferenceType::signature)
          .anyMatch(signature::equals)) {
        cache.put(ctype, true);
        return true;
      }
      final boolean result = isAssignableFrom(signature, ctype.superclass());

      cache.put(ctype, result);
      return result;
    }
    return false;
  }

  /**
   * Determines if the specified {@code Value} is assignment-compatible with the class represented
   * by the signature. This method is the dynamic equivalent of the Java language {@code instanceof}
   * operator. The method returns <code>true</code> if the specified {@code Value} argument is non-
   * <code>null</code> and can be cast to the reference type represented by the signature without
   * raising a {@code ClassCastException.} It returns <code>false</code> otherwise.
   *
   * @param signature the signature for the class to check if <code>obj</code> is an instance of
   * @param obj the object to check
   * @return <code>true</code> if <code>obj</code> is an instanceof of the class represented by
   *     <code>signature</code>; <code>false</code> otherwise or if it is <code>null</code>
   */
  public boolean isInstance(String signature, @Nullable Value obj) {
    if (obj == null) {
      return false;
    }
    return isAssignableFrom(signature, obj.type());
  }

  /**
   * Finds the first loaded class given its signature.
   *
   * @param signature the signature of the class to load
   * @return the corresponding class or <code>null</code> if none found
   */
  @Nullable
  public ClassType getClass(String signature) {
    return classes(signature).findFirst().orElse(null);
  }

  /**
   * Finds all loaded classes for a given signature.
   *
   * @param signature the signature of the classes to load
   * @return a stream of all corresponding classes (may be empty if none loaded)
   */
  public Stream<ClassType> classes(String signature) {
    return vm.classesByName(signature.substring(1, signature.length() - 1).replace('/', '.'))
        .stream()
        .map(ClassType.class::cast);
  }

  /**
   * Return a reference to the container object where the given object is defined (if defined as an
   * inner class or an anonymous one.
   *
   * @param obj the object for which to find its container object
   * @return the corresponding container object or <code>null</code> if <code>obj</code> is not
   *     defined inside another object
   */
  @Nullable
  public ObjectReference getContainerThis(@Nullable ObjectReference obj) {
    if (obj == null) {
      return null;
    }
    // reverse order to get this$2 before this$1 and this$0
    final Map<String, Field> fields = new TreeMap<>(Comparator.reverseOrder());

    for (final Field f : obj.referenceType().fields()) {
      if (f.name().startsWith("this$")) {
        fields.put(f.name(), f);
      }
    }
    return !fields.isEmpty() ? get(obj, fields.values().iterator().next(), null) : null;
  }

  /**
   * Finds a constructor for a given class given its signature (e.g. <code>(Ljava/lang/String;)V
   * </code>.
   *
   * @param type the class for which to find the constructor
   * @param signature the signature of the constructor to find
   * @return the corresponding constructor method or <code>null</code> if none found
   */
  @Nullable
  public Method findConstructor(ClassType type, String signature) {
    return findMethod(type, "<init>", signature);
  }

  /**
   * Finds a method for a given class given its name and signature (e.g. <code>(Ljava/lang/String;)V
   * </code>.
   *
   * @param type the class for which to find the method
   * @param name the name of the method to find
   * @param signature the signature of the method to find
   * @return the corresponding method or <code>null</code> if none found
   */
  @Nullable
  public Method findMethod(ReferenceType type, String name, String signature) {
    if (type instanceof ClassType) {
      return ((ClassType) type).concreteMethodByName(name, signature);
    }
    for (final Method m : type.methodsByName(name, signature)) {
      if (!m.isAbstract()) {
        return m;
      }
    }
    return null;
  }

  /**
   * Retrieves the value of a field for a given object given its name while unwrapping any primitive
   * values or strings; all others are returned as {@link ObjectReference} objects.
   *
   * @param <T> the type of object or object reference returned
   * @param obj the object where to retrieve a field value
   * @param name the name of the field to retrieve its value
   * @param signature the signature of the class the value must be an instance of or <code>null
   *     </code> to return the current value no matter what
   * @return the corresponding value or unwrapped object or <code>null</code> if none found, if not
   *     an instance of the given class signature, if <code>obj</code> is <code>null</code>, or if
   *     it the current value is <code>null</code> to start with
   */
  @Nullable
  public <T> T get(@Nullable ObjectReference obj, String name, @Nullable String signature) {
    if (obj == null) {
      return null;
    }
    final ReferenceType type = obj.referenceType();

    return get(obj, type.fieldByName(name), signature);
  }

  /**
   * Retrieves the value of a field for a given object while unwrapping any primitive values or
   * strings; all others are returned as {@link ObjectReference} objects.
   *
   * @param <T> the type of object or object reference returned
   * @param obj the object where to retrieve a field value
   * @param field the field to retrieve its value
   * @param signature the signature of the class the value must be an instance of or <code>null
   *     </code> to return the current value no matter what
   * @return the corresponding value or unwrapped object or <code>null</code> if none found, if not
   *     an instance of the given class signature, if <code>obj</code> or <code>field</code> is
   *     <code>null</code>, or if it the current value is <code>null</code> to start with
   */
  @Nullable
  public <T> T get(
      @Nullable ObjectReference obj, @Nullable Field field, @Nullable String signature) {
    if ((obj != null) && (field != null)) {
      final Value value =
          (obj instanceof ClassObjectReference)
              ? ((ClassObjectReference) obj).reflectedType().getValue(field)
              : obj.getValue(field);

      if ((value != null) && ((signature == null) || isInstance(signature, value))) {
        return (T) fromMirror(value);
      }
    }
    return null;
  }

  /**
   * Retrieves the value of a static field for a given class given its name while unwrapping any
   * primitive values or strings; all others are returned as {@link ObjectReference} objects.
   *
   * @param <T> the type of object or object reference returned
   * @param clazz the class where to retrieve a static field value
   * @param name the name of the field to retrieve its value
   * @param signature the signature of the class the value must be an instance of or <code>null
   *     </code> to return the current value no matter what
   * @return the corresponding value or unwrapped object or <code>null</code> if none found, if not
   *     an instance of the given class signature, if <code>clazz</code> is <code>null</code>, or if
   *     it the current value is <code>null</code> to start with
   */
  @Nullable
  public <T> T getStatic(@Nullable ClassType clazz, String name, @Nullable String signature) {
    if (clazz == null) {
      return null;
    }
    final ReferenceType type = clazz.classObject().reflectedType();

    return getStatic(clazz, type.fieldByName(name), signature);
  }

  /**
   * Retrieves the value of a static field for a given class while unwrapping any primitive values
   * or strings; all others are returned as {@link ObjectReference} objects.
   *
   * @param <T> the type of object or object reference returned
   * @param clazz the class where to retrieve a static field value
   * @param field the field to retrieve its value
   * @param signature the signature of the class the value must be an instance of or <code>null
   *     </code> to return the current value no matter what
   * @return the corresponding value or unwrapped object or <code>null</code> if none found, if not
   *     an instance of the given class signature, if <code>clazz</code> or <code>field</code> is
   *     <code>null</code>, or if it the current value is <code>null</code> to start with
   */
  @Nullable
  public <T> T getStatic(
      @Nullable ClassType clazz, @Nullable Field field, @Nullable String signature) {
    if ((clazz != null) && (field != null)) {
      final Value value = clazz.getValue(field);

      if ((value != null) && ((signature == null) || isInstance(signature, value))) {
        return (T) fromMirror(value);
      }
    }
    return null;
  }

  /**
   * Invokes a method on a given object given its name, signature and arguments and returns the
   * result while unwrapping any primitive values or strings or return <code>null</code> if the
   * method cannot be found.
   *
   * @param <T> the type of object or object reference returned
   * @param obj the object where to invoke the method
   * @param name the name of the method to invoke
   * @param signature the signature of the method to invoke
   * @param args the arguments for the method (primitives and strings are automatically wrapped; all
   *     others have to be {@link ObjectReference} objects)
   * @return the result value or unwrapped object or <code>null</code> if the result or <code>obj
   *     </code> is <code>null</code> or again if the method cannot be found
   * @throws Error if a failure occurs while invoking the method or if the method could not be
   *     located
   */
  @Nullable
  public <T> T invokeAndReturnNullIfNotFound(
      @Nullable ObjectReference obj, String name, String signature, Object... args) {
    if (obj == null) {
      return null;
    }
    final ReferenceType type = obj.referenceType();
    final Method method = findMethod(type, name, signature);

    return (method != null) ? invoke(obj, method, args) : null;
  }

  /**
   * Invokes a method on a given object given its name, signature and arguments and returns the
   * result while unwrapping any primitive values or strings.
   *
   * @param <T> the type of object or object reference returned
   * @param obj the object where to invoke the method
   * @param name the name of the method to invoke
   * @param signature the signature of the method to invoke
   * @param args the arguments for the method (primitives and strings are automatically wrapped; all
   *     others have to be {@link ObjectReference} objects)
   * @return the result value or unwrapped object or <code>null</code> if the result or <code>obj
   *     </code> is <code>null</code>
   * @throws Error if a failure occurs while invoking the method or if the method could not be
   *     located
   */
  @Nullable
  @SuppressWarnings("squid:S00112" /* Not meant to be catchable so keeping it generic */)
  public <T> T invoke(
      @Nullable ObjectReference obj, String name, String signature, Object... args) {
    if (obj == null) {
      return null;
    }
    final ReferenceType type = obj.referenceType();
    final Method method = findMethod(type, name, signature);

    if (method == null) {
      throw new Error(
          String.format("could not find method '%s[%s]' for: %s", name, signature, type.name()));
    }
    return invoke(obj, method, args);
  }

  /**
   * Invokes a method on a given object with the specified arguments and returns the result while
   * unwrapping any primitive values or strings.
   *
   * @param <T> the type of object or object reference returned
   * @param obj the object where to invoke the method
   * @param method the method to invoke
   * @param args the arguments for the method (primitives and strings are automatically wrapped; all
   *     others have to be {@link ObjectReference} objects)
   * @return the result value or unwrapped object or <code>null</code> if the result or <code>obj
   *     </code> or <code>method</code> is <code>null</code>
   * @throws Error if a failure occurs while invoking the method of if the method could not be
   *     located
   */
  @SuppressWarnings("squid:S00112" /* Not meant to be catchable so keeping it generic */)
  @Nullable
  public <T> T invoke(@Nullable ObjectReference obj, @Nullable Method method, Object... args) {
    if ((obj == null) || (method == null)) {
      return null;
    }
    final List<StringReference> prefs = new ArrayList<>(args.length);

    try {
      // if we are not using INVOKE_SINGLE_THREADED, then other threads starts which means we
      // could get another breakpoint event which will be processed in parallel and that means
      // that us invoking code here will resume all threads which will create
      // <sun.jdi.InvalidStackFrameException: Thread has been resumed> for other breakpoint
      // processors but on the other end, if we use INVOKE_SINGLE_THREADED, we might create a
      // deadlock if the code we are calling requires a lock that another suspended thread has but
      // again this means that if that thread is currently suspended because of a breakpoint, it
      // will now be resumed preventing us from being able to process it.
      final List<Value> values = new ArrayList<>(args.length);

      for (final Object arg : args) {
        if (arg instanceof String) {
          final StringReference pref = protect(() -> toMirror(arg));

          prefs.add(pref);
          values.add(pref);
        } else {
          values.add(toMirror(arg));
        }
      }
      return (T)
          fromMirror(
              obj.invokeMethod(
                  getThread(), method, values, ObjectReference.INVOKE_SINGLE_THREADED));
    } catch (Exception e) {
      throw new Error(e);
    } finally {
      prefs.forEach(StringReference::enableCollection);
    }
  }

  /**
   * Invokes a static method on a given class given its name, signature and arguments and returns
   * the result while unwrapping any primitive values or strings.
   *
   * @param <T> the type of object or object reference returned
   * @param clazz the class where to invoke the static method
   * @param name the name of the method to invoke
   * @param signature the signature of the method to invoke
   * @param args the arguments for the method (primitives and strings are automatically wrapped; all
   *     others have to be {@link ObjectReference} objects)
   * @return the result value or unwrapped object or <code>null</code> if the result or <code>clazz
   *     </code> is <code>null</code>
   * @throws Error if a failure occurs while invoking the method or if the method could not be
   *     located
   */
  @Nullable
  @SuppressWarnings("squid:S00112" /* Not meant to be catchable so keeping it generic */)
  public <T> T invokeStatic(
      @Nullable ClassType clazz, String name, String signature, Object... args) {
    if (clazz == null) {
      return null;
    }
    final ReferenceType type = clazz.classObject().reflectedType();
    final Method method = findMethod(type, name, signature);

    if (method == null) {
      throw new Error(
          String.format(
              "could not find static method '%s[%s]' for: %s", name, signature, type.name()));
    }
    return invokeStatic(clazz, method, args);
  }

  /**
   * Invokes a static method on a given class with the specified arguments and returns the result
   * while unwrapping any primitive values or strings.
   *
   * @param <T> the type of object or object reference returned
   * @param clazz the class where to invoke the static method
   * @param method the method to invoke
   * @param args the arguments for the method (primitives and strings are automatically wrapped; all
   *     others have to be {@link ObjectReference} objects)
   * @return the result value or unwrapped object or <code>null</code> if the result or <code>clazz
   *     </code> or <code>method</code> is <code>null</code>
   * @throws Error if a failure occurs while invoking the method of if the method could not be
   *     located
   */
  @SuppressWarnings("squid:S00112" /* Not meant to be catchable so keeping it generic */)
  @Nullable
  public <T> T invokeStatic(@Nullable ClassType clazz, @Nullable Method method, Object... args) {
    if ((clazz == null) || (method == null)) {
      return null;
    }
    final List<StringReference> prefs = new ArrayList<>(args.length);

    try {
      // if we are not using INVOKE_SINGLE_THREADED, then other threads starts which means we
      // could get another breakpoint event which will be processed in parallel and that means
      // that us invoking code here will resume all threads which will create
      // <sun.jdi.InvalidStackFrameException: Thread has been resumed> for other breakpoint
      // processors but on the other end, if we use INVOKE_SINGLE_THREADED, we might create a
      // deadlock if the code we are calling requires a lock that another suspended thread has but
      // again this means that if that thread is currently suspended because of a breakpoint, it
      // will now be resumed preventing us from being able to process it.
      final List<Value> values = new ArrayList<>(args.length);

      for (final Object arg : args) {
        if (arg instanceof String) {
          final StringReference pref = protect(() -> toMirror(arg));

          prefs.add(pref);
          values.add(pref);
        } else {
          values.add(toMirror(arg));
        }
      }
      return (T)
          fromMirror(
              clazz.invokeMethod(
                  getThread(), method, values, ObjectReference.INVOKE_SINGLE_THREADED));
    } catch (Exception e) {
      throw new Error(e);
    } finally {
      prefs.forEach(StringReference::enableCollection);
    }
  }

  /**
   * Instantiates in the attached VM a new object given its type by calling the specified
   * constructor with the given arguments.
   *
   * <p><i>Note:</i> The caller of this method must unprotect the returned reference manually.
   *
   * @param <T> the type of object reference returned
   * @param type the class to instantiate
   * @param ctor the constructor to call
   * @param args the arguments to the constructor
   * @return a reference to the newly created object with garbage collection disabled on it or
   *     <code>null</code> if <code>type</code> or <code>ctor</code> is <code>null</code>
   * @throws Error if a failure occurs while invoking the constructor
   */
  @SuppressWarnings("squid:S00112" /* Not meant to be catchable so keeping it generic */)
  @Nullable
  public <T extends ObjectReference> T newInstance(
      @Nullable ClassType type, @Nullable Method ctor, Object... args) {
    if (type == null) {
      return null;
    }
    final List<StringReference> prefs = new ArrayList<>(args.length);

    try {
      final List<Value> values = new ArrayList<>(args.length);

      for (final Object arg : args) {
        if (arg instanceof String) {
          final StringReference pref = protect(() -> toMirror(arg));

          prefs.add(pref);
          values.add(pref);
        } else {
          values.add(toMirror(arg));
        }
      }
      // because newly created refs can be garbage collected at any time, we need to protect them
      return (T)
          protect(
              () ->
                  type.newInstance(
                      getThread(),
                      ctor,
                      values,
                      ObjectReference.INVOKE_SINGLE_THREADED
                          // (Debugger.SUSPEND_ALL ? 0 : ObjectReference.INVOKE_SINGLE_THREADED)
                          | ObjectReference.INVOKE_NONVIRTUAL));
    } catch (Exception e) {
      throw new Error(e);
    } finally {
      prefs.forEach(StringReference::enableCollection);
    }
  }

  /**
   * Instantiates in the attached VM a new object given its type by calling the specified
   * constructor with the given arguments.
   *
   * <p><i>Note:</i> The caller of this method must unprotect the returned reference manually.
   *
   * @param <T> the type of object reference returned
   * @param type the class to instantiate
   * @param signature the signature of the constructor to call
   * @param args the arguments to the constructor
   * @return a reference to the newly created object with garbage collection disabled on it or
   *     <code>null</code> if <code>type</code> or <code>ctor</code> is <code>null</code>
   * @throws Error if a failure occurs while invoking the constructor of if the constructor could
   *     not be located
   */
  @SuppressWarnings("squid:S00112" /* Not meant to be catchable so keeping it generic */)
  @Nullable
  public <T extends ObjectReference> T newInstance(
      @Nullable ClassType type, String signature, Object... args) {
    if (type == null) {
      return null;
    }
    final Method ctor = findConstructor(type, signature);

    if (ctor == null) {
      throw new Error(
          String.format("could not find constructor '[%s]' for: %s", signature, type.name()));
    }
    return newInstance(type, ctor, args);
  }

  /**
   * Instantiates in the attached VM a new object given its type signature by calling the specified
   * constructor with the given arguments.
   *
   * <p><i>Note:</i> The caller of this method must unprotect the returned reference manually.
   *
   * @param <T> the type of object reference returned
   * @param type the signature of the class to instantiate
   * @param signature the signature of the constructor to call
   * @param args the arguments to the constructor
   * @return a reference to the newly created object with garbage collection disabled on it or
   *     <code>null</code> if <code>type</code> is <code>null</code>
   * @throws Error if a failure occurs while invoking the constructor of if the constructor could
   *     not be located
   */
  @Nullable
  public <T extends ObjectReference> T newInstance(String type, String signature, Object... args) {
    if (type == null) {
      return null;
    }
    return newInstance(getClass(type), signature, args);
  }

  /**
   * Retrieves a specific enum value.
   *
   * @param type the enum class for which to retrieve a value
   * @param value the specific value to retrieve
   * @return a reference to the specified enum value or <code>null</code> if <code>type</code> is
   *     <code>null</code>
   * @throws Error if a failure occurs while locating the enum value
   */
  @Nullable
  public ObjectReference enumValue(ReferenceType type, String value) {
    if (type == null) {
      return null;
    }
    return invoke(type.classObject(), "valueOf", "(Ljava/lang/String;)" + type.signature(), value);
  }

  /**
   * Invokes the {@link Object#toString()} on a given object living inside the attached virtual
   * machine.
   *
   * @param obj the reference to the object to gets its string representation
   * @return the corresponding string representation or <code>null</code> if <code>obj</code> is
   *     <code>null</code>
   * @throws Error if a failure occurs while invoking the <code>toString()</code> method
   */
  @SuppressWarnings("squid:S00112" /* Forced to by the Java debugger API */)
  @Nullable
  public String toString(@Nullable ObjectReference obj) {
    if (obj == null) {
      return null;
    }
    return invoke(obj, "toString", ReflectionUtil.METHOD_SIGNATURE_NO_ARGS_STRING_RESULT);
  }

  /**
   * Retrieves a mirror of the specified primitive object or strings defined in the attached VM.
   *
   * <p><i>Note:</i> The return value, if corresponding to a string, will not be protected against
   * garbage collection and can therefore get garbage collected at any point in time. It is
   * recommended to protect it.
   *
   * @param <T> the type of value mirroring the corresponding object in the attached VM
   * @param obj the object to be mirrored
   * @return a corresponding mirrored value for the object inside the attached VM or <code>null
   *     </code> if <code>obj</code> is <code>null</code>
   * @throws Error if the object is not a supported class of object that can be mirrored
   */
  @SuppressWarnings("squid:S00112" /* Not meant to be catchable so keeping it generic */)
  @Nullable
  public <T extends Value> T toMirror(@Nullable Object obj) {
    if (obj instanceof Boolean) {
      return (T) vm.mirrorOf(((Boolean) obj).booleanValue());
    } else if (obj instanceof Byte) {
      return (T) vm.mirrorOf(((Byte) obj).byteValue());
    } else if (obj instanceof Character) {
      return (T) vm.mirrorOf(((Character) obj).charValue());
    } else if (obj instanceof Double) {
      return (T) vm.mirrorOf(((Double) obj).doubleValue());
    } else if (obj instanceof Float) {
      return (T) vm.mirrorOf(((Float) obj).floatValue());
    } else if (obj instanceof Integer) {
      return (T) vm.mirrorOf(((Integer) obj).intValue());
    } else if (obj instanceof Long) {
      return (T) vm.mirrorOf(((Long) obj).longValue());
    } else if (obj instanceof Short) {
      return (T) vm.mirrorOf(((Short) obj).shortValue());
    } else if (obj instanceof String) {
      return (T) vm.mirrorOf((String) obj);
    } else if (obj instanceof Void) {
      return (T) vm.mirrorOfVoid();
    } else if (obj == null) {
      return null;
    } else if (obj instanceof Value) {
      return (T) obj;
    }
    throw new Error("unsupported type '" + obj.getClass().getName() + "' to convert from: " + obj);
  }

  /**
   * Gets a mirror reference for void.
   *
   * @return a mirror reference for void
   */
  public VoidValue getVoid() {
    return vm.mirrorOfVoid();
  }

  /**
   * Unwraps a given mirror by returning its corresponding primitive value or string.
   *
   * <p>This method also supports arrays of primitives or strings in where they are returned as
   * arrays of primitives or strings. All others are returned as references.
   *
   * @param obj the value to be un-mirrored
   * @return a corresponding un-mirrored object or <code>obj</code> is it doesn't correspond to a
   *     primitive, string, array of primitive or strings
   */
  @Nullable
  public Object fromMirror(@Nullable Value obj) {
    if (obj instanceof BooleanValue) {
      return ((BooleanValue) obj).booleanValue();
    } else if (obj instanceof ByteValue) {
      return ((ByteValue) obj).byteValue();
    } else if (obj instanceof CharValue) {
      return ((CharValue) obj).charValue();
    } else if (obj instanceof DoubleValue) {
      return ((DoubleValue) obj).doubleValue();
    } else if (obj instanceof FloatValue) {
      return ((FloatValue) obj).floatValue();
    } else if (obj instanceof IntegerValue) {
      return ((IntegerValue) obj).intValue();
    } else if (obj instanceof LongValue) {
      return ((LongValue) obj).longValue();
    } else if (obj instanceof ShortValue) {
      return ((ShortValue) obj).shortValue();
    } else if (obj instanceof StringReference) {
      return ((StringReference) obj).value();
    } else if (obj instanceof VoidValue) {
      return null;
    } else if (obj == null) {
      return null;
    } else if (obj instanceof ArrayReference) {
      return fromMirror((ArrayReference) obj);
    }
    return obj; // must be an ObjectReference so keep it as is
  }

  /**
   * Gets a class corresponding to the given signature.
   *
   * @param signature the signature for which to return a corresponding class
   * @return the corresponding class for primitive or strings; {@linl Value} for all others
   */
  public Class<?> getType(String signature) {
    switch (signature) {
      case "B":
        return Byte.TYPE;
      case "C":
        return Character.TYPE;
      case "D":
        return Double.TYPE;
      case "F":
        return Float.TYPE;
      case "I":
        return Integer.TYPE;
      case "J":
        return Long.TYPE;
      case "S":
        return Short.TYPE;
      case "V":
        return Void.TYPE;
      case "Z":
        return Boolean.TYPE;
      case "Ljava/lang/String;":
        return String.class;
      default:
        return Value.class;
    }
  }

  private Object fromMirror(ArrayReference ref) {
    final Class<?> type = getType(ref.referenceType().signature().substring(1));

    if (Value.class.equals(type)) {
      return ref;
    }
    final List<Value> list = ref.getValues();
    final int length = list.size();
    final Object array = Array.newInstance(type, length);

    for (int i = 0; i < length; i++) {
      Array.set(array, i, fromMirror(list.get(i)));
    }
    return array;
  }

  /**
   * Called repeatedly the provided supplier until garbage collection can be successfully disabled
   * on its returned object reference.
   *
   * <p><i>Note:</i> This method will attempt to retrieve a new object reference from the supplier a
   * maximum of {@link #SANE_TRY_LIMIT} times.
   *
   * @param <E> the exception that can be thrown out of the supplier
   * @param <T> the type of object reference returned from the supplier
   * @param supplier the supplier consulted to provide a new object reference each time a failure
   *     occurs to disable garbage collection on it
   * @return an object reference with garbage collection disabled
   * @throws E if the supplier fails to provide a new object reference
   * @throws ObjectCollectedException if unable to protect the returned object reference after too
   *     many attempts
   */
  @SuppressWarnings({"squid:S2259" /* error cannot be null after the loop */})
  protected <E extends Exception, T extends ObjectReference> T protect(
      ThrowingSupplier<T, E> supplier) throws E {
    ObjectCollectedException error = null;

    for (int i = 0; i < ReflectionUtil.SANE_TRY_LIMIT; i++) {
      try {
        final T t = supplier.get();

        t.disableCollection();
        return t;
      } catch (ObjectCollectedException e) {
        error = e;
      }
    }
    throw error;
  }

  /**
   * Functional interface for suppliers for the {@link #protect} method.
   *
   * @param <E> the exception that can be thrown out of the supplier
   * @param <T> the type of object reference returned from the supplier
   */
  @FunctionalInterface
  private interface ThrowingSupplier<T, E extends Exception> {
    /**
     * Gets a result.
     *
     * @return a result
     * @throws E if unable to provide the result
     */
    T get() throws E;
  }
}
