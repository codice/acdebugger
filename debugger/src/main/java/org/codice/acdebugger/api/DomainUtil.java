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
import com.sun.jdi.ArrayReference; // NOSONAR
import com.sun.jdi.ClassObjectReference; // NOSONAR
import com.sun.jdi.ObjectReference; // NOSONAR
import com.sun.jdi.ReferenceType; // NOSONAR
import com.sun.jdi.StackFrame; // NOSONAR
import com.sun.jdi.Value; // NOSONAR
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.codice.acdebugger.common.DomainInfo;

/** Provides domain utility functionality. */
public class DomainUtil implements LocationUtil {
  /** Internal key where locations for specific domains are cached. */
  private static final String DOMAIN_LOCATION_CACHE = "debug.domains.location.cache";

  /** Constant used in the domain cache when a given object is associated with a null domain. */
  @VisibleForTesting static final String NULL_DOMAIN = "NULL-DOMAIN";

  private final Debug debug;

  /**
   * Creates a new domain utility instance.
   *
   * @param debug the current debug session
   */
  DomainUtil(Debug debug) {
    this.debug = debug;
  }

  @Override
  @Nullable
  public String get(@Nullable Value domain) {
    return get((Object) domain);
  }

  @Override
  @Nullable
  public String get(StackFrame frame) {
    return get((Object) frame);
  }

  /**
   * Gets a domain location for the given object. The object can be a {@link StackFrame}, a {@link
   * ClassObjectReference}, a {@link ReferenceType}, or an {@link ObjectReference} representing a
   * protection domain.
   *
   * @param obj the object for which to find the corresponding domain
   * @return the corresponding domain location or <code>null</code> if none exist
   */
  @Nullable
  public String get(@Nullable Object obj) {
    if (obj == null) {
      return null;
    }
    final Map<Object, String> cache =
        debug.computeIfAbsent(DomainUtil.DOMAIN_LOCATION_CACHE, ConcurrentHashMap::new);
    final String location = get0(debug.reflection(), obj, cache);

    return (location != DomainUtil.NULL_DOMAIN) ? location : null; // identity check here
  }

  /**
   * Gets domain information for the given domains and permission.
   *
   * @param domainsRef the reference to the array of domains to find the corresponding information
   * @param domains the list of domains to find the corresponding information
   * @param permission the permission for which to verify if the domains are granted it
   * @param permissionInfos the corresponding permissions info
   * @param firstDomainWithoutPermission the index for the first domain in the array which is known
   *     to not have permissions; after that it is unknown wether a domain has or doesn't have the
   *     permission
   * @return a corresponding list of domain information
   */
  public List<DomainInfo> get(
      ArrayReference domainsRef,
      List<Value> domains,
      ObjectReference permission,
      Set<String> permissionInfos,
      int firstDomainWithoutPermission) {
    final Map<Object, String> cache =
        debug.computeIfAbsent(DomainUtil.DOMAIN_LOCATION_CACHE, ConcurrentHashMap::new);
    List<DomainInfo> info = getFromBackdoor(domainsRef, domains, permission, cache);

    if (info != null) {
      return info;
    }
    final PermissionUtil permissions = debug.permissions();
    final ReflectionUtil reflection = debug.reflection();

    info = new ArrayList<>(domains.size());
    for (int i = 0; i < domains.size(); i++) {
      final ObjectReference domain = (ObjectReference) domains.get(i);
      String location = get0(reflection, domain, cache);
      boolean implies;

      location = (location != DomainUtil.NULL_DOMAIN) ? location : null; // identity check here
      if ((i < firstDomainWithoutPermission)
          || (domain == null)
          || (location == null)) { // boot domain always have permissions
        implies = true;
      } else {
        // check cache based on its location
        implies = permissions.implies(location, permissionInfos);
        if (!implies && (i > firstDomainWithoutPermission)) { // check attached VM
          // we know the first one doesn't so no need to check again
          implies = permissions.implies(domain, permission);
        }
      }
      info.add(new DomainInfo(location, implies));
    }
    return info;
  }

  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* this is a console application */
  })
  @Nullable
  private String getFromBackdoor(ObjectReference obj, Map<Object, String> cache) {
    try {
      String location = debug.backdoor().getDomain(debug, obj);

      if (location == null) {
        location = DomainUtil.NULL_DOMAIN;
      }
      cache.put(obj, location);
      return location;
    } catch (VirtualMachineError e) {
      throw e;
    } catch (IllegalStateException e) { // ignore and continue the long way
    } catch (Throwable t) {
      // ignore and continue the long way which might require more calls to the process
      t.printStackTrace();
    }
    return null;
  }

  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* this is a console application */
  })
  @Nullable
  private List<DomainInfo> getFromBackdoor(
      ArrayReference domainsRef,
      List<Value> domains,
      ObjectReference permission,
      Map<Object, String> cache) {
    try {
      final List<DomainInfo> info = debug.backdoor().getDomainInfo(debug, domainsRef, permission);

      for (int i = 0; i < info.size(); i++) {
        final String location = info.get(i).getLocationString();

        cache.put(domains.get(i), (location != null) ? location : DomainUtil.NULL_DOMAIN);
      }
      return info;
    } catch (VirtualMachineError e) {
      throw e;
    } catch (IllegalStateException e) { // ignore and continue the long way
    } catch (Throwable t) {
      // ignore and continue the long way which might require more calls to the process
      t.printStackTrace();
    }
    return null;
  }

  @Nullable
  private String get0(ReflectionUtil reflection, @Nullable Object obj, Map<Object, String> cache) {
    if (obj == null) {
      return null;
    }
    String location = cache.get(obj);

    if (location != null) {
      return location;
    } else if (obj instanceof StackFrame) {
      // cannot cache stack frames as calling a single method invalidates it including its hashCode
      return get0(reflection, ((StackFrame) obj).location().declaringType().classObject(), cache);
    } else if (obj instanceof ReferenceType) {
      return get0(reflection, ((ReferenceType) obj).classObject(), cache);
    } else if (!(obj instanceof ObjectReference)) {
      return null;
    }
    location = getFromBackdoor((ObjectReference) obj, cache);
    if (location != null) {
      return location;
    } else if (obj instanceof ClassObjectReference) {
      return getFromAssociatedProtectionDomain(reflection, (ClassObjectReference) obj, cache);
    }
    // if we get here then it has to be a protection domain as we do not support anything else
    location =
        reflection.toString(
            reflection.invokeAndReturnNullIfNotFound(
                reflection.invoke(
                    (ObjectReference) obj, "getCodeSource", "()Ljava/security/CodeSource;"),
                "getLocation",
                "()Ljava/net/URL;"));
    if (location != null) {
      if (location.regionMatches(true, 0, "file:/", 0, 6)) {
        location = debug.properties().compress(debug, location);
      }
      cache.put(obj, location);
    } else {
      cache.put(obj, DomainUtil.NULL_DOMAIN);
    }
    return location;
  }

  @Nullable
  private String getFromAssociatedProtectionDomain(
      ReflectionUtil reflection, ClassObjectReference clazz, Map<Object, String> cache) {
    // getProtectionDomain0() is private in class Class and avoids the security manager check which
    // would create a recursion which we don't want to handle here. In addition, it returns a fake
    // domain if none is associated with the class instead of null as we prefer
    final String location =
        get0(
            reflection,
            (ObjectReference)
                reflection.invoke(
                    clazz, "getProtectionDomain0", "()Ljava/security/ProtectionDomain;"),
            cache);

    cache.put(clazz, (location != null) ? location : DomainUtil.NULL_DOMAIN);
    return location;
  }
}
