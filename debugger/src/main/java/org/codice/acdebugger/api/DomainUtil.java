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
  private static final String NULL_DOMAIN = "NULL-DOMAIN";

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
    if (!(domain instanceof ObjectReference)) {
      return null;
    }
    return get((ObjectReference) domain);
  }

  @Override
  @Nullable
  public String get(StackFrame frame) {
    final ObjectReference thisObject = frame.thisObject();

    return (thisObject != null) ? get(thisObject.referenceType()) : null;
  }

  /**
   * Gets domain information for the given domain.
   *
   * @param domain the domain to find the corresponding location
   * @return the corresponding location or <code>null</code> if none exist
   */
  @Nullable
  public String get(@Nullable ObjectReference domain) {
    if (domain == null) {
      return null;
    }
    final Map<Object, String> cache =
        debug.computeIfAbsent(DomainUtil.DOMAIN_LOCATION_CACHE, ConcurrentHashMap::new);
    String location = cache.get(domain);

    if (location == null) {
      location = getFromBackdoor(domain, cache);
    }
    if (location != null) {
      return (location != DomainUtil.NULL_DOMAIN) ? location : null;
    }
    final ReflectionUtil reflection = debug.reflection();

    location =
        reflection.toString(
            reflection.invokeAndReturnNullIfNotFound(
                reflection.invoke(
                    (ObjectReference) domain, "getCodeSource", "()Ljava/security/CodeSource;"),
                "getLocation",
                "()Ljava/net/URL;"));
    if (location != null) {
      if (location.regionMatches(true, 0, "file:/", 0, 6)) {
        location = debug.properties().compress(debug, location);
      }
      cache.put(domain, location);
    } else {
      cache.put(domain, DomainUtil.NULL_DOMAIN);
    }
    return location;
  }

  /**
   * Gets domain information for the given class.
   *
   * @param clazz the class to find the corresponding domain location
   * @return the corresponding domain location or <code>null</code> if none exist
   */
  @Nullable
  public String get(@Nullable ReferenceType clazz) {
    if (clazz == null) {
      return null;
    }
    return get(clazz.classObject());
  }

  /**
   * Gets domain information for the given class.
   *
   * @param clazz the class to find the corresponding domain location
   * @return the corresponding domain location or <code>null</code> if none exist
   */
  @Nullable
  public String get(@Nullable ClassObjectReference clazz) {
    if (clazz == null) {
      return null;
    }
    final Map<Object, String> cache =
        debug.computeIfAbsent(DomainUtil.DOMAIN_LOCATION_CACHE, ConcurrentHashMap::new);
    String location = cache.get(clazz);

    if (location != null) {
      return (location != DomainUtil.NULL_DOMAIN) ? location : null;
    }
    // getProtectionDomain0() is private in class Class and avoids the security manager check which
    // would create a recursion which we don't want to handle here. In addition, it returns a fake
    // domain if none is associated with the class
    location =
        get(
            (ObjectReference)
                debug
                    .reflection()
                    .invoke(clazz, "getProtectionDomain0", "()Ljava/security/ProtectionDomain;"));
    cache.put(clazz, (location != null) ? location : DomainUtil.NULL_DOMAIN);
    return location;
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
    info = new ArrayList<>(domains.size());
    for (int i = 0; i < domains.size(); i++) {
      final ObjectReference domain = (ObjectReference) domains.get(i);
      final String location = get(domain);
      boolean implies;

      if (i < firstDomainWithoutPermission) {
        implies = true;
      } else {
        implies = debug.permissions().implies(location, permissionInfos); // check cache
        if (!implies && (i > firstDomainWithoutPermission)) { // check attached VM
          // we now the first one doesn't so no need to check again
          implies = debug.permissions().implies(domain, permission);
        }
      }
      info.add(new DomainInfo(location, implies));
    }
    return info;
  }

  @Nullable
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* this is a console application */
  })
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
  @SuppressWarnings({
    "squid:S1181", /* letting VirtualMachineErrors bubble out directly, so ok to catch Throwable */
    "squid:S1148" /* this is a console application */
  })
  private String getFromBackdoor(ObjectReference domain, Map<Object, String> cache) {
    try {
      String location = debug.backdoor().getDomain(debug, domain);

      if (location == null) {
        location = DomainUtil.NULL_DOMAIN;
      }
      cache.put(domain, location);
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
}
