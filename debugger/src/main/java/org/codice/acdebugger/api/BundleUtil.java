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

import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

/** Provides bundle utility functionality. */
@SuppressWarnings("squid:S1191" /* Using the Java debugger API */)
public class BundleUtil {
  /** Internal key where bundle names for specific objects are cached. */
  private static final String BUNDLE_INFO_CACHE = "debug.bundle.info.cache";

  /** Constant used in the bundle cache when a given object is known to belong to bundle-0. */
  private static final String NULL_BUNDLE = "NULL-BUNDLE";

  private final Debug debug;

  /**
   * Creates a new bundle utility instance.
   *
   * @param debug the current debug session
   */
  BundleUtil(Debug debug) {
    this.debug = debug;
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
    final String bundle = get0(obj);

    return (bundle != BundleUtil.NULL_BUNDLE) ? bundle : null; // identity check here
  }

  @Nullable
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* this is a console application */
  })
  private String getFromBackdoor(Object obj, Map<Object, String> cache) {
    try {
      final String bundle = debug.backdoor().getBundle(debug, obj);

      cache.put(obj, (bundle != null) ? bundle : BundleUtil.NULL_BUNDLE);
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
   * classloader. This methods makes all attempts possible to figure out the corresponding bundle
   * based on implementation details.
   *
   * @param obj the object for which to find the corresponding bundle.
   * @return the name/location of the corresponding bundle, {@link #NULL_BUNDLE} if the
   *     corresponding bundle could not be found in previous attempts (could be because it is
   *     bundle-0), or <code>null</code> if unable to find it
   */
  @Nullable
  @SuppressWarnings({
    "squid:S3776", /* Recursive logic and simple enough to not warrant decomposing more */
  })
  private String get0(@Nullable Object obj) {
    if (obj == null) {
      return null;
    }
    // start by checking our cache
    final Map<Object, String> cache =
        debug.computeIfAbsent(BundleUtil.BUNDLE_INFO_CACHE, ConcurrentHashMap::new);
    String bundle = cache.get(obj);

    if (bundle != null) {
      return bundle;
    } else if (obj instanceof StackFrame) {
      // cannot cache stack frames as calling a single method invalidates it including its hashCode
      return get0(((StackFrame) obj).location().declaringType().classLoader());
    } else if (!(obj instanceof ObjectReference)) {
      return null;
    }
    bundle = getFromBackdoor(obj, cache); // first try via the backdoor
    if (bundle != null) {
      return bundle;
    }
    final ObjectReference ref = (ObjectReference) obj;
    final ReferenceType type = ref.referenceType();

    if (debug.reflection().isAssignableFrom("Lorg/osgi/framework/Bundle;", type)) {
      bundle =
          debug
              .reflection()
              .invoke(
                  ref, "getSymbolicName", ReflectionUtil.METHOD_SIGNATURE_NO_ARGS_STRING_RESULT);
    } else if (debug
        .reflection()
        .isAssignableFrom("Lorg/eclipse/osgi/internal/loader/EquinoxClassLoader;", type)) {
      bundle =
          get0(
              debug
                  .reflection()
                  .invoke(
                      debug
                          .reflection()
                          .invoke(
                              ref,
                              "getBundleLoader",
                              "()Lorg/eclipse/osgi/internal/loader/BundleLoader;"),
                      "getWiring",
                      "()Lorg/eclipse/osgi/container/ModuleWiring;"));
    }
    if (bundle == null) { // check if we have a getBundle() method
      // useful for org.eclipse.osgi.internal.loader.ModuleClassLoader$GenerationProtectionDomain,
      // org.eclipse.osgi.container.ModuleWiring
      // which exposes the bundle via a method
      bundle =
          get0(invokeAndReturnNullIfNotFound(ref, "getBundle", "()Lorg/osgi/framework/Bundle;"));
    }
    if (bundle == null) { // check if we have a getBundleContext() method
      bundle =
          get0(
              invokeAndReturnNullIfNotFound(
                  ref, "getBundleContext", "()Lorg/osgi/framework/BundleContext;"));
    }
    if (bundle == null) { // check if we have a bundle field
      // useful for org.apache.felix.cm.impl.helper.BaseTracker$CMProtectionDomain,
      // which does not expose the bundle in any other ways
      bundle = get0(debug.reflection().get(ref, "bundle", "Lorg/osgi/framework/Bundle;"));
    }
    if (bundle == null) { // check if we have a bundle context field
      // useful for org.apache.aries.blueprint.container.BlueprintProtectionDomain
      // which does not expose the bundle in any other ways
      bundle =
          get0(debug.reflection().get(ref, "bundleContext", "Lorg/osgi/framework/BundleContext;"));
    }
    if (bundle == null) { // check if we have a context field
      // useful for org.eclipse.osgi.internal.serviceregistry.FilteredServiceListener
      bundle = get0(debug.reflection().get(ref, "context", "Lorg/osgi/framework/BundleContext;"));
    }
    if (bundle == null) { // check if it has a delegate protection domain via a delegate field
      // useful for org.apache.karaf.util.jaas.JaasHelper$DelegatingProtectionDomain
      bundle = get0(debug.reflection().get(ref, "delegate", "Ljava/security/ProtectionDomain;"));
    }
    if (bundle == null) {
      // for org.apache.aries.blueprint.container.AbstractServiceReferenceRecipe
      // we look for getBundleContextForServiceLookup()
      bundle =
          get0(
              invokeAndReturnNullIfNotFound(
                  ref, "getBundleContextForServiceLookup", "()Lorg/osgi/framework/BundleContext;"));
    }
    if (bundle == null) {
      // for org.apache.aries.blueprint.container.AbstractServiceReferenceRecipe$2$1
      // or org.apache.aries.blueprint.container.AbstractServiceReferenceRecipe$2
      // we shall look at the container class and figure it out from there
      bundle = get0(debug.reflection().getContainerThis(ref));
    }
    if (bundle == null) { // check if it has a classloader via a getClassLoader() method
      // useful if it is a straight java.security.ProtectionDomain
      bundle =
          get0(invokeAndReturnNullIfNotFound(ref, "getClassLoader", "()Ljava/lang/ClassLoader;"));
    }
    if (bundle == null) {
      // for classloaders, check their parent classloader via a getParent() method
      // useful for org.apache.aries.proxy.impl.interfaces.ProxyClassLoader
      bundle = get0(invokeAndReturnNullIfNotFound(ref, "getParent", "()Ljava/lang/ClassLoader;"));
    }
    cache.put(obj, (bundle != null) ? bundle : BundleUtil.NULL_BUNDLE);
    return bundle;
  }

  @Nullable
  private <T> T invokeAndReturnNullIfNotFound(
      @Nullable ObjectReference obj, String name, String signature, Object... args) {
    if (obj == null) {
      return null;
    }
    final ReferenceType type =
        (obj instanceof ClassObjectReference)
            ? ((ClassObjectReference) obj).reflectedType()
            : obj.referenceType();
    final Method method = debug.reflection().findMethod(type, name, signature);

    return (method != null) ? debug.reflection().invoke(obj, method, args) : null;
  }
}
