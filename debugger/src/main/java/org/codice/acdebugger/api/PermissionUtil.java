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

import com.sun.jdi.ClassType;
import com.sun.jdi.ObjectReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.codice.acdebugger.common.ServicePermissionInfo;

/** Provides permission-specific functionality. */
@SuppressWarnings("squid:S1191" /* Using the Java debugger API */)
public class PermissionUtil {
  /** Internal key where information about service properties is cached. */
  private static final String SERVICE_PROPERTY_CACHE = "debug.service.property.cache";

  private static final String SERVICE_PERMISSION_FORMAT =
      "org.osgi.framework.ServicePermission \"%s\", \"%s\"";

  private static final String GET = "get";

  private final Debug debug;

  /**
   * Creates a new permission utility instance.
   *
   * @param debug the current debug session
   */
  PermissionUtil(Debug debug) {
    this.debug = debug;
  }

  /**
   * Checks if a bundle is or was temporarily granted a given permission (as long as the information
   * was previously cached using {@link #grant(String, String)}) or {@link #grant(String, Set)}.
   *
   * <p><i>Note:</i> This method only looks at the local cache.
   *
   * @param bundle the bundle to check for
   * @param permission the permission string to check for
   * @return <code>true</code> if the bundle is or was granted the specified permission; <code>
   *     false</code> if not
   */
  public boolean implies(String bundle, String permission) {
    return debug.getContext().hasPermission(bundle, permission);
  }

  /**
   * Checks if a bundle is or was temporarily granted the given permissions (as long as the
   * information was previously cached using {@link #grant(String, String)}) or {@link
   * #grant(String, Set)}.
   *
   * <p><i>Note:</i> This method only looks at the local cache.
   *
   * @param bundle the bundle to check for
   * @param permissions the permission strings to check for
   * @return <code>true</code> if the bundle is or was granted the specified permissions; <code>
   *     false</code> if not
   */
  public boolean implies(String bundle, Set<String> permissions) {
    return debug.getContext().hasPermissions(bundle, permissions);
  }

  /**
   * Checks if a domain has a given permission by communicating with the attached VM to verify the
   * permission with the given domain.
   *
   * <p><i>Note:</i> The result is not locally cached.
   *
   * @param domain the domain to check for
   * @param permission the permission to check for
   * @return <code>true</code> if the domain has the specifieds permission; <code>false</code> if
   *     not
   */
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* this is a console application */
  })
  public boolean implies(ObjectReference domain, ObjectReference permission) {
    try { // first try via the backdoor
      return debug.backdoor().hasPermission(debug, domain, permission);
    } catch (VirtualMachineError e) {
      throw e;
    } catch (IllegalStateException e) { // ignore and continue
    } catch (Throwable t) { // ignore and continue the long way
      t.printStackTrace();
    }
    try {
      return debug
          .reflection()
          .invoke(domain, "implies", "(Ljava/security/Permission;)Z", permission);
    } catch (Error e) { // since we cannot tell, we shall assume the worst
      // that sometimes happen if some classes have not yet been loaded by JVM
      return false;
    }
  }

  /**
   * Creates a new service permission.
   *
   * <p><i>Note:</i> The caller of this method must unprotect the returned reference manually.
   *
   * @param serviceReference the OSGI service reference for the service to create a permission for
   * @return a newly created reference to a service permission object created in the attached VM
   */
  public ObjectReference createServicePermission(ObjectReference serviceReference, String actions) {
    // this would be more optimal if we extended the PermissionActivator and let it create it for us
    // and only fallback to this is we don't have access to it
    return debug
        .reflection()
        .newInstance(
            "Lorg/osgi/framework/ServicePermission;",
            "(Lorg/osgi/framework/ServiceReference;Ljava/lang/String;)V",
            serviceReference,
            actions);
  }

  /**
   * Temporarily grants a bundle a set of permissions if not already granted.
   *
   * <p><i>Note:</i> This method will automatically grant the permissions in the attached VM if the
   * debugger is running in continuous mode and granting was enabled and if attached to VM running
   * the backdoor bundle.
   *
   * @param bundle the bundle to grant the permissions to
   * @param permissions the permissions to be granted
   * @return <code>true</code> if the permissions were granted to the specified bundle; <code>false
   *     </code> if at least one was already granted
   */
  public boolean grant(String bundle, Set<String> permissions) {
    if (bundle == null) {
      return false;
    }
    return permissions.stream().map(p -> grant(bundle, p)).reduce(true, Boolean::logicalAnd);
  }

  /**
   * Temporarily grants a bundle a given permission if not already granted.
   *
   * <p><i>Note:</i> This method will automatically grant the permission in the attached VM if the
   * debugger is running in continuous mode and granting was enabled and if attached to VM running
   * the backdoor bundle.
   *
   * @param bundle the bundle to grant the permission to
   * @param permission the permission to be granted
   * @return <code>true</code> if the permission was granted to the specified bundle; <code>false
   *     </code> if it was already granted
   */
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* this is a console application */
  })
  public boolean grant(String bundle, String permission) {
    final boolean granted = debug.getContext().grantPermission(bundle, permission);

    if (granted && debug.isGranting() && debug.isContinuous()) {
      // try to grant the permission in the VM via the backdoor
      try {
        debug.backdoor().grantPermission(debug, bundle, permission);
      } catch (VirtualMachineError e) {
        throw e;
      } catch (IllegalStateException e) { // ignore and continue
      } catch (Throwable t) { // ignore and continue
        t.printStackTrace();
      }
    }
    return granted;
  }

  /**
   * Gets a set of service permissions strings for the specified services.
   *
   * @param serviceClasses the service classes to get service permission strings for or <code>null
   *     </code> to get one for all services
   * @param actions the actions to get the service permissions for
   * @return a corresponding set of service permissions
   */
  public Set<String> getServicePermissionStrings(
      @Nullable String[] serviceClasses, String actions) {
    if (serviceClasses == null) {
      return Collections.singleton(
          String.format(PermissionUtil.SERVICE_PERMISSION_FORMAT, "*", actions));
    }
    return Stream.of(serviceClasses)
        .sorted()
        .map(s -> String.format(PermissionUtil.SERVICE_PERMISSION_FORMAT, s, actions))
        .collect(Collectors.toSet());
  }

  /**
   * Gets a set of permissions strings corresponding to a given permission.
   *
   * @param permission the permission to get a permission strings for
   * @return a corresponding set of permissions
   */
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* this is a console application */
  })
  public Set<String> getPermissionStrings(ObjectReference permission) {
    try { // first try via the backdoor
      return debug.backdoor().getPermissionStrings(debug, permission);
    } catch (VirtualMachineError e) {
      throw e;
    } catch (IllegalStateException e) { // ignore and continue the long way
    } catch (Throwable t) {
      // ignore and continue the long way which might require more calls to the process
      t.printStackTrace();
    }
    final ClassType obj = (ClassType) permission.referenceType();
    final String actions =
        debug
            .reflection()
            .invoke(
                permission, "getActions", ReflectionUtil.METHOD_SIGNATURE_NO_ARGS_STRING_RESULT);

    if (debug.reflection().isInstance("Lorg/osgi/framework/ServicePermission;", permission)) {
      // for ServicePermission, we typically get something like "(service.id=14)"
      // for the permission name which is not useful; so we first try to get the set
      // of object classes representing the services and if we get a set then we can
      // create permissions for each names. If we get null then we typically can rely
      // on the permission name
      final String[] objectClass =
          debug.reflection().get(permission, "objectClass", "[Ljava/lang/String;");

      if (objectClass != null) {
        return Stream.of(objectClass)
            .map(n -> getPermissionString(obj.name(), n, actions))
            .collect(Collectors.toSet());
      }
    }
    return Collections.singleton(
        getPermissionString(
            obj.name(),
            debug
                .reflection()
                .invoke(
                    permission, "getName", ReflectionUtil.METHOD_SIGNATURE_NO_ARGS_STRING_RESULT),
            actions));
  }

  /**
   * Finds missing service permissions for the given bundle/domain that corresponds to a 'GET'
   * service permission referenced in the given service event.
   *
   * @param bundle the name of the bundle associated with the given domain
   * @param domain the domain to check service permissions against
   * @param serviceEvent the service event which we need to check permissions for
   * @return a set of permission strings for all missing service permissions
   */
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* this is a console application */
  })
  public Set<String> findMissingServicePermissionStrings(
      String bundle, ObjectReference domain, ObjectReference serviceEvent) {
    try { // first try via the backdoor
      final ServicePermissionInfo info =
          debug
              .backdoor() // only grant in continuous mode and if granting
              .getServicePermissionInfoAndGrant(
                  debug, bundle, domain, serviceEvent, debug.isContinuous() && debug.isGranting());

      final Set<String> permissionStrings = info.getPermissionStrings();

      if (debug.isContinuous()) {
        // first make sure we cache all permissions since we granted them if they were missing
        permissionStrings.forEach(p -> debug.getContext().grantPermission(bundle, p));
      }
      // next cache all info we have about pre-existed implied permissions
      // and remove them from the complete list of permissions since they were not missing
      info.getImpliedPermissionStrings()
          .stream()
          .peek(permissionStrings::remove)
          .forEach(p -> debug.getContext().grantPermission(bundle, p));
      if (!info.implies()) { // we didn't have all permissions
        return permissionStrings;
      }
      return Collections.emptySet();
    } catch (VirtualMachineError e) {
      throw e;
    } catch (IllegalStateException e) { // ignore and continue the long way
    } catch (Throwable t) {
      // ignore and continue the long way which might require more calls to the process
      t.printStackTrace();
    }
    return findMissingServicePermissionStrings0(bundle, domain, serviceEvent);
  }

  /**
   * Gets a service property for a given reference.
   *
   * @param <T> the type for the service property to retrieve
   * @param serviceReference the service reference for which to retrieve a service property
   * @param key the key for the service property to retrieve
   * @return the corresponding service property value
   */
  @Nullable
  private <T> T getServiceProperty(ObjectReference serviceReference, String key) {
    final Map<ObjectReference, Map<String, Object>> cache =
        debug.computeIfAbsent(PermissionUtil.SERVICE_PROPERTY_CACHE, ConcurrentHashMap::new);
    final Map<String, Object> props =
        cache.computeIfAbsent(serviceReference, s -> new ConcurrentHashMap<>());

    if (props.containsKey(key)) {
      return (T) props.get(key);
    }
    final T value =
        debug
            .reflection()
            .invoke(serviceReference, "getProperty", "(Ljava/lang/String;)Ljava/lang/Object;", key);

    props.put(key, value);
    return value;
  }

  private Set<String> findMissingServicePermissionStrings0(
      String bundle, ObjectReference domain, ObjectReference serviceEvent) {
    // check if we cached the permission for ALL: '*'
    if (!implies(bundle, getServicePermissionStrings(null, PermissionUtil.GET))) {
      final ObjectReference serviceReference =
          debug
              .reflection()
              .invoke(
                  serviceEvent, "getServiceReference", "()Lorg/osgi/framework/ServiceReference;");
      final Set<String> permissionStrings =
          getServicePermissionStrings(
              getServiceProperty(serviceReference, "objectClass"), PermissionUtil.GET);

      // check if we cached the permission for the specific service
      if (!implies(bundle, permissionStrings)) {
        final ObjectReference permission =
            createServicePermission(serviceReference, PermissionUtil.GET);

        try {
          // test the domain as done by the normal code where we put the breakpoint
          if (!implies(domain, permission)) {
            // remove the permissions we already had from the set as they were not missing
            for (Iterator<String> i = permissionStrings.iterator(); i.hasNext(); ) {
              final String p = i.next();

              if (!implies(bundle, p)) {
                i.remove();
              }
            }
            // fall-through and grant all missing permissions
          } // else - grant all permissions since the domain has them
        } finally {
          permission.enableCollection();
        }
        grant(bundle, permissionStrings); // grant missing permissions
        return permissionStrings;
      } // else - already cached so the bundle either has the permission or we already detected it
    } // else - the bundle has all service "*" permissions
    return Collections.emptySet();
  }

  private String getPermissionString(String clazz, String name, @Nullable String actions) {
    final StringBuilder sb = new StringBuilder();

    sb.append(clazz);
    sb.append(" \"");
    sb.append(name);
    if ((actions != null) && !actions.isEmpty()) {
      sb.append("\", \"").append(actions);
    }
    sb.append("\"");
    return sb.toString();
  }
}
