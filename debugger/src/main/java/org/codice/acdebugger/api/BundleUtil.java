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

import com.google.common.annotations.VisibleForTesting;
import com.sun.jdi.ClassObjectReference; // NOSONAR
import com.sun.jdi.ObjectReference; // NOSONAR
import com.sun.jdi.ReferenceType; // NOSONAR
import com.sun.jdi.StackFrame; // NOSONAR
import com.sun.jdi.Value; // NOSONAR
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

/** Provides bundle utility functionality. */
public class BundleUtil implements LocationUtil {
  /** Internal key where bundle names for specific objects are cached. */
  private static final String BUNDLE_INFO_CACHE = "debug.bundle.info.cache";

  /** Constant used in the bundle cache when a given object is known to belong to bundle-0. */
  @VisibleForTesting static final String NULL_BUNDLE = "NULL-BUNDLE";

  private final Debug debug;

  /**
   * Creates a new bundle utility instance.
   *
   * @param debug the current debug session
   */
  BundleUtil(Debug debug) {
    this.debug = debug;
  }

  @Nullable
  @Override
  public String get(@Nullable Value domain) {
    return get((Object) domain);
  }

  @Override
  @Nullable
  public String get(StackFrame frame) {
    return get((Object) frame);
  }

  /**
   * Gets a bundle location for the given object. The object can be a {@link StackFrame} or an
   * {@link ObjectReference} representing a bundle, a protection domain, a bundle context, or even a
   * classloader. This methods makes all attempts possible to figure out the corresponding bundle
   * based on implementation details.
   *
   * @param obj the object for which to find the corresponding bundle.
   * @return the name/location of the corresponding bundle or <code>null</code> if unable to find it
   */
  @Nullable
  public String get(@Nullable Object obj) {
    if (obj == null) {
      return null;
    }
    final Map<Object, String> cache =
        debug.computeIfAbsent(BundleUtil.BUNDLE_INFO_CACHE, ConcurrentHashMap::new);
    final String bundle = get0(debug.reflection(), obj, cache);

    return (bundle != BundleUtil.NULL_BUNDLE) ? bundle : null; // identity check here
  }

  @Nullable
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* this is a console application */
  })
  private String getFromBackdoor(Object obj, Map<Object, String> cache) {
    try {
      String bundle = debug.backdoor().getBundle(debug, obj);

      if (bundle == null) {
        bundle = BundleUtil.NULL_BUNDLE;
      }
      cache.put(obj, bundle);
      return bundle;
    } catch (VirtualMachineError e) {
      throw e;
    } catch (IllegalStateException e) { // ignore and continue the long way
    } catch (Throwable t) {
      // ignore and continue the long way which might require more calls to the process
      t.printStackTrace();
    }
    return null;
  }

  /**
   * Gets a bundle location for the given object. The object can be a {@link StackFrame} or an
   * {@link ObjectReference} representing a bundle, a protection domain, a bundle context, or even a
   * class or a classloader. This methods makes all attempts possible to figure out the
   * corresponding bundle based on implementation details.
   *
   * @param reflection the reflection utility
   * @param obj the object for which to find the corresponding bundle.
   * @param cache the cache to look into for a bundle name or to cache new values
   * @return the name/location of the corresponding bundle, {@link #NULL_BUNDLE} if the
   *     corresponding bundle could not be found in previous attempts (could be because it is
   *     bundle-0), or <code>null</code> if unable to find it
   */
  @Nullable
  @SuppressWarnings({
    "squid:S3776", /* Recursive logic and simple enough to not warrant decomposing more */
  })
  private String get0(ReflectionUtil reflection, @Nullable Object obj, Map<Object, String> cache) {
    // NOTE: The logic here should be kept in sync with the logic in
    // org.codice.acdebugger.backdoor.Backdoor
    if (obj == null) {
      return null;
    }
    // start by checking our cache
    String bundle = cache.get(obj);

    if (bundle != null) {
      return bundle;
    } else if (obj instanceof StackFrame) {
      // cannot cache stack frames as calling a single method invalidates it including its hashCode
      return get0(reflection, ((StackFrame) obj).location().declaringType().classObject(), cache);
    } else if (obj instanceof ReferenceType) {
      return get0(reflection, ((ReferenceType) obj).classObject(), cache);
    } else if (!(obj instanceof ObjectReference)) {
      return null;
    }
    bundle = getFromBackdoor(obj, cache); // first try via the backdoor
    if (bundle != null) {
      return bundle;
    } else if (obj instanceof ClassObjectReference) {
      return getFromAssociatedProtectionDomain(reflection, (ClassObjectReference) obj, cache);
    }
    final ObjectReference ref = (ObjectReference) obj;
    final ReferenceType type = ref.referenceType();

    if (reflection.isAssignableFrom("Lorg/osgi/framework/Bundle;", type)) {
      bundle =
          reflection.invoke(
              ref, "getSymbolicName", ReflectionUtil.METHOD_SIGNATURE_NO_ARGS_STRING_RESULT);
    } else if (reflection.isAssignableFrom(
        "Lorg/eclipse/osgi/internal/loader/EquinoxClassLoader;", type)) {
      bundle =
          get0(
              reflection,
              reflection.invoke(
                  reflection.invoke(
                      ref, "getBundleLoader", "()Lorg/eclipse/osgi/internal/loader/BundleLoader;"),
                  "getWiring",
                  "()Lorg/eclipse/osgi/container/ModuleWiring;"),
              cache);
    } else if (reflection.isAssignableFrom("Ljava/security/ProtectionDomain;", type)) {
      // check if we have a protection domain with Eclipse's permissions
      // must be done checked before the getBundle() check because otherwise in the case of a
      // org.eclipse.osgi.internal.loader.ModuleClassLoader.GenerationProtectionDomain, we would
      // end up referencing the bundle host and not the bundle fragment
      final ObjectReference permissions =
          reflection.invokeAndReturnNullIfNotFound(
              ref, "getPermissions", "()Ljava/security/PermissionCollection;");

      if (reflection.isInstance(
          "Lorg/eclipse/osgi/internal/permadmin/BundlePermissions;", permissions)) {
        bundle = get0(reflection, permissions, cache);
      }
    } // no else here
    if (bundle == null) { // check if we have a getBundle() method
      // useful for org.eclipse.osgi.internal.loader.ModuleClassLoader$GenerationProtectionDomain,
      // org.eclipse.osgi.container.ModuleWiring which exposes the bundle via a method
      bundle =
          get0(
              reflection,
              reflection.invokeAndReturnNullIfNotFound(
                  ref, "getBundle", "()Lorg/osgi/framework/Bundle;"),
              cache);
    }
    if (bundle == null) { // check if we have a getBundleContext() method
      bundle =
          get0(
              reflection,
              reflection.invokeAndReturnNullIfNotFound(
                  ref, "getBundleContext", "()Lorg/osgi/framework/BundleContext;"),
              cache);
    }
    if (bundle == null) { // check if we have a bundle field
      // useful for org.apache.felix.cm.impl.helper.BaseTracker$CMProtectionDomain,
      // which does not expose the bundle in any other ways
      bundle =
          get0(reflection, reflection.get(ref, "bundle", "Lorg/osgi/framework/Bundle;"), cache);
    }
    if (bundle == null) { // check if we have a bundle context field
      // useful for org.apache.aries.blueprint.container.BlueprintProtectionDomain
      // which does not expose the bundle in any other ways
      bundle =
          get0(
              reflection,
              reflection.get(ref, "bundleContext", "Lorg/osgi/framework/BundleContext;"),
              cache);
    }
    if (bundle == null) { // check if we have a context field
      // useful for org.eclipse.osgi.internal.serviceregistry.FilteredServiceListener
      bundle =
          get0(
              reflection,
              reflection.get(ref, "context", "Lorg/osgi/framework/BundleContext;"),
              cache);
    }
    if (bundle == null) { // check if it has a delegate protection domain via a delegate field
      // useful for org.apache.karaf.util.jaas.JaasHelper$DelegatingProtectionDomain
      bundle =
          get0(
              reflection,
              reflection.get(ref, "delegate", "Ljava/security/ProtectionDomain;"),
              cache);
    }
    if (bundle == null) {
      // for org.apache.aries.blueprint.container.AbstractServiceReferenceRecipe
      // we look for getBundleContextForServiceLookup()
      bundle =
          get0(
              reflection,
              reflection.invokeAndReturnNullIfNotFound(
                  ref, "getBundleContextForServiceLookup", "()Lorg/osgi/framework/BundleContext;"),
              cache);
    }
    if (bundle == null) {
      // for org.apache.aries.blueprint.container.AbstractServiceReferenceRecipe$2$1
      // or org.apache.aries.blueprint.container.AbstractServiceReferenceRecipe$2
      // we shall look at the container class and figure it out from there
      bundle = get0(reflection, reflection.getContainerThis(ref), cache);
    }
    if (bundle == null) { // check if it has a classloader via a getClassLoader() method
      // useful if it is a straight java.security.ProtectionDomain
      bundle =
          get0(
              reflection,
              reflection.invokeAndReturnNullIfNotFound(
                  ref, "getClassLoader", "()Ljava/lang/ClassLoader;"),
              cache);
    }
    if (bundle == null) {
      // for classloaders, check their parent classloader via a getParent() method
      // useful for org.apache.aries.proxy.impl.interfaces.ProxyClassLoader
      bundle =
          get0(
              reflection,
              reflection.invokeAndReturnNullIfNotFound(
                  ref, "getParent", "()Ljava/lang/ClassLoader;"),
              cache);
    }
    cache.put(obj, (bundle != null) ? bundle : BundleUtil.NULL_BUNDLE);
    return bundle;
  }

  private String getFromAssociatedProtectionDomain(
      ReflectionUtil reflection, ClassObjectReference clazz, Map<Object, String> cache) {
    // getProtectionDomain0() is private in class Class and avoids the security manager check which
    // would create a recursion which we don't want to handle here. In addition, it returns a fake
    // domain if none is associated with the class instead of null as we prefer
    final String bundle =
        get0(
            reflection,
            (ObjectReference)
                reflection.invoke(
                    clazz, "getProtectionDomain0", "()Ljava/security/ProtectionDomain;"),
            cache);

    cache.put(clazz, (bundle != null) ? bundle : BundleUtil.NULL_BUNDLE);
    return bundle;
  }
}
