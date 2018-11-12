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

// NOSONAR - squid:S1191 - Using the Java debugger API

import com.sun.jdi.ClassType; // NOSONAR
import com.sun.jdi.Method; // NOSONAR
import com.sun.jdi.ObjectReference; // NOSONAR
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.codice.acdebugger.ACDebugger;
import org.codice.acdebugger.api.Debug;
import org.codice.acdebugger.api.ReflectionUtil;
import org.codice.acdebugger.common.PropertiesUtil;

/** Provides utility functions around system properties for the attached VM. */
public class SystemProperties {
  public static final String CLASS_SIGNATURE = "Ljava/lang/System;";

  private static final String METHOD_SIGNATURE_STRING_ARG_STRING_RESULT =
      "(Ljava/lang/String;)Ljava/lang/String;";

  private ObjectReference systemReference;

  private boolean initializing = false;

  private PropertiesUtil util;

  /**
   * Initializes the backdoor.
   *
   * @param debug the current debug information
   * @param systemReference the system properties reference
   */
  @SuppressWarnings("squid:S106" /* this is a console application */)
  public synchronized void init(Debug debug, ObjectReference systemReference) {
    if (systemReference == null) {
      throw new IllegalStateException("unable to locate system properties");
    }
    final ReflectionUtil reflection = debug.reflection();

    try {
      this.initializing = true;
      this.systemReference = systemReference;
      final Method getProperty =
          reflection.findMethod(
              systemReference.referenceType(),
              "getProperty",
              SystemProperties.METHOD_SIGNATURE_STRING_ARG_STRING_RESULT);
      final Map<String, String> map = new LinkedHashMap<>();

      PropertiesUtil.propertiesNames()
          .forEach(
              name -> {
                final String value = reflection.invoke(systemReference, getProperty, name);

                if (value != null) {
                  map.put(name, value);
                }
              });
      final Properties props = new Properties();

      props.putAll(map);
      this.util = new PropertiesUtil(props);
      System.out.println(ACDebugger.PREFIX);
      System.out.println(ACDebugger.PREFIX + "System properties are initialized");
      map.forEach(
          (p, v) -> System.out.println(ACDebugger.PREFIX + "    " + p + " = \"" + v + "\""));
    } finally {
      this.initializing = false;
    }
  }

  /**
   * Initializes the system properties by attempting to find its instance in the attached VM.
   *
   * @param debug the current debug information
   * @return <code>true</code> if the backdoor is initialized; <code>false</code> if not
   */
  public synchronized boolean init(Debug debug) {
    if (initializing) {
      return false;
    } else if (systemReference == null) {
      final ObjectReference ref =
          debug
              .reflection()
              .classes(SystemProperties.CLASS_SIGNATURE)
              .map(clazz -> getSystemProperties(debug, clazz))
              .filter(Objects::nonNull)
              .findFirst()
              .orElse(null);

      if (ref == null) {
        return false;
      }
      init(debug, ref);
    }
    return true;
  }

  /**
   * Compresses the specified strings by replacing occurrences of configured system properties.
   *
   * @param debug the current debug information
   * @param s the string to compress
   * @return the corresponding compressed string
   */
  public synchronized String compress(Debug debug, String s) {
    findSystemProperties(debug); // make sure the system properties are enabled
    return util.compress(s);
  }

  private synchronized void findSystemProperties(Debug debug) {
    if (initializing) {
      throw new IllegalStateException("system properties are initializing");
    }
    if (!init(debug)) {
      throw new IllegalStateException("system properties is not initialized yet");
    }
  }

  private ObjectReference getSystemProperties(Debug debug, ClassType clazz) {
    try {
      return debug.reflection().invokeStatic(clazz, "getProperties", "()Ljava/util/Properties;");
    } catch (Exception e) {
      return null;
    }
  }
}
