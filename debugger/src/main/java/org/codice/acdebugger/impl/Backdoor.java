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

import com.google.gson.reflect.TypeToken;
import com.sun.jdi.ArrayReference; // NOSONAR
import com.sun.jdi.ClassType; // NOSONAR
import com.sun.jdi.Method; // NOSONAR
import com.sun.jdi.ObjectReference; // NOSONAR
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.codice.acdebugger.ACDebugger;
import org.codice.acdebugger.api.Debug;
import org.codice.acdebugger.api.ReflectionUtil;
import org.codice.acdebugger.breakpoints.HasListenServicePermissionBreakpointProcessor;
import org.codice.acdebugger.common.DomainInfo;
import org.codice.acdebugger.common.JsonUtils;
import org.codice.acdebugger.common.ServicePermissionInfo;

/** This class provides access to the backdoor class running inside the attached VM. */
public class Backdoor {
  public static final String CLASS_SIGNATURE = "Lorg/codice/acdebugger/backdoor/Backdoor;";

  private static final String METHOD_SIGNATURE_OBJ_ARG_STRING_RESULT =
      "(Ljava/lang/Object;)Ljava/lang/String;";

  private ObjectReference backdoorReference;

  private Method getBundle;

  private Method getBundleVersion;

  private Method getDomain;

  private Method getDomainInfo;

  private Method getPermissionStrings;

  private Method grantPermission;

  private Method hasPermission;

  private Method getServicePermissionInfoAndGrant;

  private boolean initializing = false;

  /**
   * Initializes the backdoor.
   *
   * @param debug the current debug information
   * @param backdoorReference the backdoor object reference
   */
  @SuppressWarnings({
    "squid:S106", /* this is a console application */
    "squid:S1148" /* this is a console application */
  })
  public synchronized void init(Debug debug, ObjectReference backdoorReference) {
    if (backdoorReference == null) {
      throw new IllegalStateException("unable to locate backdoor instance");
    }
    final ReflectionUtil reflection = debug.reflection();

    try {
      this.initializing = true;
      this.backdoorReference = backdoorReference;
      this.getBundle =
          reflection.findMethod(
              backdoorReference.referenceType(),
              "getBundle",
              Backdoor.METHOD_SIGNATURE_OBJ_ARG_STRING_RESULT);
      this.getBundleVersion =
          reflection.findMethod(
              backdoorReference.referenceType(),
              "getBundleVersion",
              Backdoor.METHOD_SIGNATURE_OBJ_ARG_STRING_RESULT);
      this.getDomain =
          reflection.findMethod(
              backdoorReference.referenceType(),
              "getDomain",
              Backdoor.METHOD_SIGNATURE_OBJ_ARG_STRING_RESULT);
      this.getDomainInfo =
          reflection.findMethod(
              backdoorReference.referenceType(),
              "getDomainInfo",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/String;");
      this.getPermissionStrings =
          reflection.findMethod(
              backdoorReference.referenceType(),
              "getPermissionStrings",
              Backdoor.METHOD_SIGNATURE_OBJ_ARG_STRING_RESULT);
      this.grantPermission =
          reflection.findMethod(
              backdoorReference.referenceType(),
              "grantPermission",
              "(Ljava/lang/String;Ljava/lang/String;)V");
      this.hasPermission =
          reflection.findMethod(
              backdoorReference.referenceType(),
              "hasPermission",
              "(Ljava/lang/Object;Ljava/lang/Object;)Z");
      this.getServicePermissionInfoAndGrant =
          reflection.findMethod(
              backdoorReference.referenceType(),
              "getServicePermissionInfoAndGrant",
              "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Z)Ljava/lang/String;");
      System.out.println(ACDebugger.PREFIX);
      System.out.println(ACDebugger.PREFIX + "Backdoor discovered");
    } finally {
      this.initializing = false;
    }
    // now register the Eclipse service permission breakpoint, now that we know the policy has been
    // loaded since the backdoor is initialized
    if (debug.isMonitoringService()) {
      try {
        debug.add(new HasListenServicePermissionBreakpointProcessor());
      } catch (Exception e) { // cannot register breakpoint so continue without it
        e.printStackTrace();
      }
    }
  }

  /**
   * Initializes the backdoor by attempting to find its instance in the attached VM.
   *
   * @param debug the current debug information
   * @return <code>true</code> if the backdoor is initialized; <code>false</code> if not
   */
  public synchronized boolean init(Debug debug) {
    if (initializing) {
      return false;
    } else if (backdoorReference == null) {
      final ObjectReference ref =
          debug
              .reflection()
              .classes(Backdoor.CLASS_SIGNATURE)
              .map(clazz -> getInstance(debug, clazz))
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
   * Gets a bundle location for the given object. The object can be a bundle, a protection domain, a
   * bundle context, or even a classloader. This methods makes all attempts possible to figure out
   * the corresponding bundle (in some case based on implementation details).
   *
   * @param debug the current debug information
   * @param obj the object for which to find the corresponding bundle
   * @return the name/location of the corresponding bundle or <code>null</code> if unable to find it
   * @throws IllegalStateException if the backdoor is initializing or doesn't support this method
   */
  @Nullable
  public synchronized String getBundle(Debug debug, Object obj) {
    findBackdoor(debug); // make sure the backdoor is enabled
    if (getBundle == null) {
      throw new IllegalStateException("getBundle() is not supported by the backdoor");
    }
    return debug.reflection().invoke(backdoorReference, getBundle, obj);
  }

  /**
   * Gets a bundle version for the given object. The object can be a bundle, a protection domain, a
   * bundle context, or even a classloader. This methods makes all attempts possible to figure out
   * the corresponding bundle (in some case based on implementation details).
   *
   * @param debug the current debug information
   * @param obj the object for which to find the corresponding bundle
   * @return the version of the corresponding bundle or <code>null</code> if unable to find it
   * @throws IllegalStateException if the backdoor is initializing or doesn't support this method
   */
  @Nullable
  public synchronized String getBundleVersion(Debug debug, Object obj) {
    findBackdoor(debug); // make sure the backdoor is enabled
    if (getBundleVersion == null) {
      throw new IllegalStateException("getBundleVersion() is not supported by the backdoor");
    }
    return debug.reflection().invoke(backdoorReference, getBundleVersion, obj);
  }

  /**
   * Gets a domain location for the given domain.
   *
   * @param debug the current debug information
   * @param domain the domain for which to find the corresponding location
   * @return the corresponding domain location or <code>null</code> if unable to find it or if none
   *     defined
   */
  @Nullable
  public synchronized String getDomain(Debug debug, ObjectReference domain) {
    findBackdoor(debug); // make sure the backdoor is enabled
    if (getDomain == null) {
      throw new IllegalStateException("getDomain() is not supported by the backdoor");
    }
    return debug.reflection().invoke(backdoorReference, getDomain, domain);
  }

  /**
   * Gets domain information for the given array of domains and permission.
   *
   * @param debug the current debug information
   * @param domains the array of domains for which to find the corresponding information
   * @param permission the permission for which to verify if the domains are granted it
   * @return a corresponding list of domain information
   */
  public synchronized List<DomainInfo> getDomainInfo(
      Debug debug, ArrayReference domains, Object permission) {
    findBackdoor(debug); // make sure the backdoor is enabled
    if (getDomainInfo == null) {
      throw new IllegalStateException("getDomainInfo() is not supported by the backdoor");
    }
    return JsonUtils.fromJson(
        debug.reflection().invoke(backdoorReference, getDomainInfo, domains, permission),
        new TypeToken<List<DomainInfo>>() {}.getType());
  }

  /**
   * Temporarily grants a domain a given permission if not already granted.
   *
   * @param debug the current debug information
   * @param domain the bundle name or domain location to grant the permission to
   * @param permission the permission to be granted
   * @throws IllegalStateException if the backdoor is initializing or doesn't support this method
   * @throws Error if an error occurred while invoking the backdoor's method
   */
  public synchronized void grantPermission(Debug debug, String domain, String permission) {
    findBackdoor(debug); // make sure the backdoor is enabled
    if (grantPermission == null) {
      throw new IllegalStateException("grantPermission() is not supported by the backdoor");
    }
    debug.reflection().invoke(backdoorReference, grantPermission, domain, permission);
  }

  /**
   * Gets a set of permissions strings corresponding to a given permission.
   *
   * @param debug the current debug information
   * @param permission the permission to get a permission strings for
   * @return a corresponding set of permissions
   * @throws IllegalStateException if the backdoor is initializing or doesn't support this method
   * @throws Error if an error occurred while invoking the backdoor's method
   */
  public synchronized Set<String> getPermissionStrings(Debug debug, ObjectReference permission) {
    findBackdoor(debug); // make sure the backdoor is enabled
    if (getPermissionStrings == null) {
      throw new IllegalStateException("getPermissionStrings() is not supported by the backdoor");
    }
    return new LinkedHashSet<>(
        JsonUtils.fromJson(
            debug.reflection().invoke(backdoorReference, getPermissionStrings, permission),
            List.class));
  }

  /**
   * Gets service permission information for a given bundle/domain and service event.
   *
   * <p>This method is called from the AC debugger and is purposely defined using Object to avoid
   * having the classes not yet loaded when the AC debugger attempts to invoke the method.
   *
   * <p><i>Note:</i> Some permissions, like the service permission are more easily represented using
   * multiple permissions.
   *
   * @param debug the current debug information
   * @param bundle the name of the bundle associated with the given domain
   * @param domain the domain to check service permissions against
   * @param serviceEvent the service event which we need to check permissions for
   * @param grant <code>true</code> to automatically grant the missing permissions; <code>false
   *     </code> not to
   * @return the corresponding service permission info
   * @throws IllegalStateException if the backdoor is initializing or doesn't support this method
   * @throws Error if an error occurred while invoking the backdoor's method
   */
  public synchronized ServicePermissionInfo getServicePermissionInfoAndGrant(
      Debug debug,
      String bundle,
      ObjectReference domain,
      ObjectReference serviceEvent,
      boolean grant) {
    findBackdoor(debug); // make sure the backdoor is enabled
    if (getServicePermissionInfoAndGrant == null) {
      throw new IllegalStateException(
          "getServicePermissionInfoAndGrant() is not supported by the backdoor");
    }
    return JsonUtils.fromJson(
        debug
            .reflection()
            .invoke(
                backdoorReference,
                getServicePermissionInfoAndGrant,
                bundle,
                domain,
                serviceEvent,
                grant),
        ServicePermissionInfo.class);
  }

  /**
   * Checks if a domain has a given permission.
   *
   * @param debug the current debug information
   * @param domain the domain to check for
   * @param permission the permission to check for
   * @return <code>true</code> if the domain has the specified permission; <code>false</code> if not
   */
  public synchronized boolean hasPermission(
      Debug debug, ObjectReference domain, ObjectReference permission) {
    findBackdoor(debug); // make sure the backdoor is enabled
    if (hasPermission == null) {
      throw new IllegalStateException("hasPermission() is not supported by the backdoor");
    }
    return debug.reflection().invoke(backdoorReference, hasPermission, domain, permission);
  }

  private synchronized void findBackdoor(Debug debug) {
    if (initializing) {
      throw new IllegalStateException("backdoor is initializing");
    }
    if (!init(debug)) {
      throw new IllegalStateException("backdoor is not initialized yet");
    }
  }

  private ObjectReference getInstance(Debug debug, ClassType clazz) {
    try {
      return debug.reflection().getStatic(clazz, "instance", Backdoor.CLASS_SIGNATURE);
    } catch (Exception e) {
      return null;
    }
  }
}
