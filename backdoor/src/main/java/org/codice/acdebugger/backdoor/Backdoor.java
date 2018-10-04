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
package org.codice.acdebugger.backdoor;

import java.io.FilePermission;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.codice.acdebugger.PermissionService;
import org.codice.acdebugger.common.JsonUtils;
import org.codice.acdebugger.common.PermissionUtil;
import org.codice.acdebugger.common.ServicePermissionInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;

/** Provides a backdoor access point for the AC debugger. */
public class Backdoor implements BundleActivator {
  @SuppressWarnings({
    "squid:S1068" /* DO NOT CHANGE THIS NAME, the AC debugger is accessing it directly */
  })
  private static volatile Backdoor instance = null;

  private volatile ServiceTracker<PermissionService, PermissionService> permServiceTracker = null;

  @SuppressWarnings({
    "squid:S2696" /* singleton instance set when initialized so AC debugger can easily find it */
  })
  @Override
  public void start(BundleContext bundleContext) {
    this.permServiceTracker =
        new ServiceTracker<>(bundleContext, PermissionService.class.getName(), null);
    this.permServiceTracker.open();
    Backdoor.instance = this;
  }

  @SuppressWarnings({
    "squid:S2696" /* singleton instance cleared when stopped so AC debugger can easily find it */
  })
  @Override
  public void stop(BundleContext bundleContext) {
    Backdoor.instance = null;
    permServiceTracker.close();
  }

  /**
   * Gets a bundle location for the given object. The object can be a bundle, a protection domain, a
   * bundle context, or even a classloader. This methods makes all attempts possible to figure out
   * the corresponding bundle (in some case based on implementation details).
   *
   * @param obj the object for which to find the corresponding bundle
   * @return the name/location of the corresponding bundle or <code>null</code> if unable to find it
   */
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* don't have access to logger at this stage */
  })
  @Nullable
  public String getBundle(@Nullable Object obj) {
    try {
      final Bundle bundle =
          AccessController.doPrivileged((PrivilegedAction<Bundle>) () -> getBundle0(obj));

      return (bundle != null) ? bundle.getSymbolicName() : null;
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Throwable t) {
      t.printStackTrace(); // suppress checkstyle:RegexpSingleline|RegexpMultiline
      throw t;
    }
  }

  /**
   * Gets a bundle version for the given object. The object can be a bundle, a protection domain, a
   * bundle context, or even a classloader. This methods makes all attempts possible to figure out
   * the corresponding bundle (in some case based on implementation details).
   *
   * @param obj the object for which to find the corresponding bundle version
   * @return the version of the corresponding bundle or <code>null</code> if unable to find it
   */
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* don't have access to logger at this stage */
  })
  @Nullable
  public String getBundleVersion(@Nullable Object obj) {
    try {
      final Bundle bundle =
          AccessController.doPrivileged((PrivilegedAction<Bundle>) () -> getBundle0(obj));

      return Objects.toString((bundle != null) ? bundle.getVersion() : null, null);
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Throwable t) {
      t.printStackTrace(); // suppress checkstyle:RegexpSingleline|RegexpMultiline
      throw t;
    }
  }

  /**
   * Gets permission strings corresponding to the given permission object.
   *
   * <p>This method is purposely defined using Object to avoid having the classes not yet loaded
   * when the AC debugger attempts to invoke the method. Further more the result is encoded as a
   * JSON string to reduce the number of times the AC debugger will come back to get the values. If
   * we return anything else than a primitive or a string, the AC debugger is forced to retrieve the
   * content of all the objects.
   *
   * <p><i>Note:</i> Some permissions, like the service permission are more easily represented using
   * multiple permissions.
   *
   * @param permission the permission for which to get a set of permission strings
   * @return a JSON string for a set of multiple permission string representations for the
   *     corresponding permission
   * @throws IllegalArgumentException if <code>permission</code> is not a {@link Permission}
   */
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* don't have access to logger at this stage */
  })
  public String getPermissionStrings(Object permission) {
    try {
      if (!(permission instanceof Permission)) {
        throw new IllegalArgumentException("not a permission: " + permission);
      }
      return AccessController.doPrivileged(
          (PrivilegedAction<String>)
              () -> JsonUtils.toJson(getPermissionStrings((Permission) permission)));
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Throwable t) {
      t.printStackTrace(); // suppress checkstyle:RegexpSingleline|RegexpMultiline
      throw t;
    }
  }

  /**
   * Gets service permission information for a given bundle/domain and service event.
   *
   * <p>This method is called from the AC debugger and is purposely defined using Object to avoid
   * having the classes not yet loaded when the AC debugger attempts to invoke the method. Further
   * more the result is encoded in a string to reduce the number of times the AC debugger will come
   * back to get the values. If we return anything else than a primitive or a string, the AC
   * debugger is forced to retrieve the content of the object.
   *
   * <p>Some permissions, like the service permission are more easily represented using multiple
   * permissions.
   *
   * @param bundle the name of the bundle associated with the given domain
   * @param domain the domain to check service permissions against
   * @param serviceEvent the service event which we need to check permissions for
   * @param grant <code>true</code> to automatically grant the missing permissions; <code>false
   *     </code> not to
   * @return a Json string for the corresponding {@link ServicePermissionInfo}
   * @throws IllegalArgumentException if <code>domain</code> is not a {@link ProtectionDomain} or if
   *     <code>serviceEvent</code> is not a {@link ServiceEvent}
   */
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* don't have access to logger at this stage */
  })
  public String getServicePermissionInfoAndGrant(
      String bundle, Object domain, Object serviceEvent, boolean grant) throws Exception {
    try {
      if (!(domain instanceof ProtectionDomain)) {
        throw new IllegalArgumentException("not a domain: " + domain);
      }
      if (!(serviceEvent instanceof ServiceEvent)) {
        throw new IllegalArgumentException("not a service event: " + serviceEvent);
      }
      return AccessController.doPrivileged(
          (PrivilegedExceptionAction<String>)
              () ->
                  getServicePermissionInfoAndGrant(
                      bundle, (ProtectionDomain) domain, (ServiceEvent) serviceEvent, grant));
    } catch (PrivilegedActionException e) {
      e.getException().printStackTrace(); // suppress checkstyle:RegexpSingleline|RegexpMultiline
      throw e.getException();
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Throwable t) {
      t.printStackTrace(); // suppress checkstyle:RegexpSingleline|RegexpMultiline
      throw t;
    }
  }

  /**
   * Temporarily grants a bundle a permission.
   *
   * <p>This method is called from the AC debugger.
   *
   * @param bundle the bundle to grant the permissions to
   * @param permission the permission to be granted
   */
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* don't have access to logger at this stage */
  })
  public void grantPermission(String bundle, String permission) throws Exception {
    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Void>)
              () -> {
                synchronized (this) { // making sure only one thread at a time is granting
                  final PermissionService permissionService = permServiceTracker.getService();

                  if (permissionService != null) {
                    permissionService.grantPermission(bundle, permission);
                  }
                  return null;
                }
              });
    } catch (PrivilegedActionException e) {
      e.getException().printStackTrace(); // suppress checkstyle:RegexpSingleline|RegexpMultiline
      throw e.getException();
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Throwable t) {
      t.printStackTrace(); // suppress checkstyle:RegexpSingleline|RegexpMultiline
      throw t;
    }
  }

  /**
   * Checks if a domain has a given permission.
   *
   * <p>This method is called from the AC debugger and is purposely defined using Object to avoid
   * having the classes not yet loaded when the AC debugger attempts to invoke the method.
   *
   * @param domain the domain to check for
   * @param permission the permission to check for
   * @return <code>true</code> if the domain has the specified permission; <code>false</code> if not
   * @throws IllegalArgumentException if <code>domain</code> is not a {@link ProtectionDomain} or if
   *     <code>serviceEvent</code> is not a {@link ServiceEvent}
   */
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* don't have access to logger at this stage */
  })
  public boolean hasPermission(Object domain, Object permission) {
    try {
      if (!(domain instanceof ProtectionDomain)) {
        throw new IllegalArgumentException("not a protection domain: " + domain);
      }
      if (!(permission instanceof Permission)) {
        throw new IllegalArgumentException("not a permission: " + permission);
      }
      return AccessController.doPrivileged(
          (PrivilegedAction<Boolean>)
              () -> ((ProtectionDomain) domain).implies((Permission) permission));
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Throwable t) {
      t.printStackTrace(); // suppress checkstyle:RegexpSingleline|RegexpMultiline
      throw t;
    }
  }

  /**
   * Gets information from the bundle associated with the given object. The object can be a bundle,
   * a protection domain, a bundle context, or even a classloader. This methods makes all attempts
   * possible to figure out the corresponding bundle (in some case based on implementation details).
   *
   * @param obj the object for which to find the corresponding bundle
   * @return the bundle of the corresponding bundle or <code>null</code> if unable to find it
   */
  @Nullable
  @SuppressWarnings({
    "squid:S3776", /* Recursive logic and simple enough to not warrant decomposing more */
    "squid:S1872" /* cannot use instanceof as the class is not exported */
  })
  private Bundle getBundle0(@Nullable Object obj) {
    // NOTE: The logic here should be kept in sync with the logic in
    // org.codice.acdebugger.api.BundleUtil
    Bundle bundle = null;

    if (obj == null) {
      return null;
    } else if (obj instanceof Bundle) {
      return (Bundle) obj;
    } else if (obj instanceof BundleReference) {
      return ((BundleReference) obj).getBundle();
    } else if (obj instanceof BundleWiring) {
      return ((BundleWiring) obj).getBundle();
    } else if (obj instanceof BundleContext) {
      return ((BundleContext) obj).getBundle();
    } else if (obj instanceof ProtectionDomain) {
      // check if we have a protection domain with Eclipse's permissions
      final PermissionCollection permissions = ((ProtectionDomain) obj).getPermissions();

      // we cannot reference org.eclipse.osgi.internal.permadmin.BundlePermissions directly as it is
      // not exported by Eclipse
      if ((permissions != null)
          && permissions
              .getClass()
              .getName()
              .equals("org.eclipse.osgi.internal.permadmin.BundlePermissions")) {
        bundle = getBundle0(permissions);
      }
    } // no else here
    if (bundle == null) { // check if it has a getBundle() method
      bundle = getBundle0(invoke(obj, "getBundle", Bundle.class));
    }
    if (bundle == null) { // check if we have a getBundleContext() method
      bundle = getBundle0(invoke(obj, "getBundleContext", BundleContext.class));
    }
    if (bundle == null) { // check if we have a bundle field
      // useful for org.apache.felix.cm.impl.helper.BaseTracker$CMProtectionDomain,
      // which does not expose the bundle in any other ways
      bundle = getBundle0(get(obj, "bundle", Bundle.class));
    }
    if (bundle == null) { // check if we have a bundle context field
      // useful for org.apache.aries.blueprint.container.BlueprintProtectionDomain
      // which does not expose the bundle in any other ways
      bundle = getBundle0(get(obj, "bundleContext", BundleContext.class));
    }
    if (bundle == null) { // check if we have a context field
      // useful for org.eclipse.osgi.internal.serviceregistry.FilteredServiceListener
      bundle = getBundle0(get(obj, "context", BundleContext.class));
    }
    if (bundle == null) { // check if it has a delegate protection domain via a delegate field
      // useful for org.apache.karaf.util.jaas.JaasHelper$DelegatingProtectionDomain
      bundle = getBundle0(get(obj, "delegate", ProtectionDomain.class));
    }
    if (bundle == null) {
      // for org.apache.aries.blueprint.container.AbstractServiceReferenceRecipe
      // we look for getBundleContextForServiceLookup()
      bundle = getBundle0(invoke(obj, "getBundleContextForServiceLookup", BundleContext.class));
    }
    if (bundle == null) {
      // for org.apache.aries.blueprint.container.AbstractServiceReferenceRecipe$2$1
      // or org.apache.aries.blueprint.container.AbstractServiceReferenceRecipe$2
      // we shall look at the container class and figure it out from there
      bundle = getBundle0(getContainerThis(obj));
    }
    if (bundle == null) { // check if it has a classloader via a getClassLoader() method
      // useful if it is a straight java.security.ProtectionDomain
      bundle = getBundle0(invoke(obj, "getClassLoader", ClassLoader.class));
    }
    if (bundle == null) {
      // for classloaders, check their parent classloader via a getParent() method
      // useful for org.apache.aries.proxy.impl.interfaces.ProxyClassLoader
      bundle = getBundle0(invoke(obj, "getParent", ClassLoader.class));
    }
    return bundle;
  }

  private String getServicePermissionInfoAndGrant(
      String bundle, ProtectionDomain domain, ServiceEvent serviceEvent, boolean grant)
      throws Exception {
    synchronized (this) {
      final ServiceReference sr = serviceEvent.getServiceReference();
      final ServicePermission p = new ServicePermission(sr, ServicePermission.GET);
      final String[] objectClass = (String[]) sr.getProperty(Constants.OBJECTCLASS);
      final boolean implies = domain.implies(p);
      final Set<String> implied = new HashSet<>(12);

      if (domain.implies(new ServicePermission("*", ServicePermission.GET))) {
        implied.add(getPermissionString(ServicePermission.class, "*", ServicePermission.GET));
      }
      for (final String c : objectClass) {
        final String permissionString =
            getPermissionString(ServicePermission.class, c, ServicePermission.GET);

        if (domain.implies(new ServicePermission(c, ServicePermission.GET))) {
          implied.add(permissionString);
        } else if (grant) {
          final PermissionService permissionService = permServiceTracker.getService();

          if (permissionService != null) {
            permissionService.grantPermission(bundle, permissionString);
          }
        }
      }
      return JsonUtils.toJson(new ServicePermissionInfo(getPermissionStrings(p), implies, implied));
    }
  }

  @Nullable
  private Object invoke(Object obj, String name, Class<?> returnClass) {
    Class<?> c = obj.getClass();

    while (c != null) {
      try {
        final Method method = c.getDeclaredMethod(name);

        if (returnClass.isAssignableFrom(method.getReturnType())) {
          method.setAccessible(true);
          return invoke(obj, method);
        }
      } catch (NoSuchMethodException e) { // ignore and check superclass
      }
      c = c.getSuperclass();
    }
    return null;
  }

  @Nullable
  private Object invoke(Object obj, Method method) {
    try {
      return method.invoke(obj);
    } catch (IllegalAccessException | InvocationTargetException e) {
      return null;
    }
  }

  @Nullable
  private Object get(Object obj, String name, Class<?> fieldClass) {
    Class<?> c = obj.getClass();

    while (c != null) {
      try {
        final Field field = c.getDeclaredField(name);

        if (fieldClass.isAssignableFrom(field.getType())) {
          return get(obj, field);
        }
      } catch (NoSuchFieldException e) { // ignore and check superclass
      }
      c = c.getSuperclass();
    }
    return null;
  }

  @Nullable
  private Object get(Object obj, Field field) {
    try {
      field.setAccessible(true);
      return field.get(obj);
    } catch (IllegalAccessException e) { // ignore and return null
      return null;
    }
  }

  private Object getContainerThis(Object obj) {
    // reverse order to get this$2 before this$1 and this$0
    final Map<String, Field> fields = new TreeMap<>(Comparator.reverseOrder());

    for (final Field f : obj.getClass().getDeclaredFields()) {
      if (f.getName().startsWith("this$")) {
        fields.put(f.getName(), f);
      }
    }
    if (fields.isEmpty()) {
      return null;
    }
    return get(obj, fields.values().iterator().next());
  }

  /**
   * Gets permission strings corresponding to the given permission object.
   *
   * <p><i>Note:</i> Some permissions, like the service permission are more easily represented using
   * multiple permissions.
   *
   * @param permission the permission for which to get a set of permission strings
   * @return a set of multiple permission string representations for the corresponding permission
   */
  @SuppressWarnings(
      "squid:S1181" /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */)
  private Set<String> getPermissionStrings(Permission permission) {
    if (permission instanceof ServicePermission) {
      try {
        final Field f = ServicePermission.class.getDeclaredField("objectClass");

        f.setAccessible(true);
        final String[] objectClass = (String[]) f.get(permission);

        if (objectClass != null) {
          return Stream.of(objectClass)
              .map(n -> getPermissionString(permission.getClass(), n, permission.getActions()))
              .collect(Collectors.toCollection(LinkedHashSet::new));
        }
      } catch (VirtualMachineError e) {
        throw e;
      } catch (Throwable t) { // ignore and continue with standard string representation
      }
    }
    return Collections.singleton(
        getPermissionString(permission.getClass(), permission.getName(), permission.getActions()));
  }

  private String getPermissionString(
      Class<? extends Permission> clazz, String name, String actions) {
    if (FilePermission.class.isAssignableFrom(clazz)) {
      // special case to take advantage of the special slash system property if defined
      final String slash = System.getProperty("/");

      if ((slash != null) && !slash.isEmpty()) {
        name = name.replace(slash, "${/}");
      }
    }
    return PermissionUtil.getPermissionString(clazz, name, actions);
  }
}
