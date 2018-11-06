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
package org.codice.acdebugger.breakpoints;

// NOSONAR - squid:S1191 - Using the Java debugger API

import com.google.common.annotations.VisibleForTesting;
import com.sun.jdi.ObjectReference; // NOSONAR
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.codice.acdebugger.api.Debug;
import org.codice.acdebugger.api.LocationUtil;
import org.codice.acdebugger.api.PermissionUtil;

/** Class used to hold access control context information */
public class AccessControlContextInfo {
  private final ObjectReference permission;
  private final Set<String> permissionInfos;

  /** list of protection domains (i.e. bundle name/domain location) in the context */
  private final List<ObjectReference> domainReferences;

  /** list of protection domains (i.e. bundle name/domain location) in the context */
  private final List<String> domains;

  /**
   * The index of the first domain in the list of domains that is reporting not being granted the
   * permission.
   */
  private final int currentDomainIndex;

  /** Domain where the exception is being reported. */
  private final ObjectReference currentDomainReference;

  /**
   * Domain location/bundle name where the exception is being reported or <code>null</code> if
   * unable to determine the domain location or bundle name.
   */
  @Nullable private final String currentDomain;

  /* list of protection domains (i.e. bundle name/domain location) that are granted the failed permissions */
  private final Set<String> privilegedDomains;

  /**
   * Creates a new access control context information.
   *
   * @param debug the current debug session
   * @param domainReferences the list of domains on the stack for which to get information
   * @param currentDomainIndex the index of the first domain (a.k.a. the current domain) in the
   *     above list that reported not being granted the specified permission
   * @param permission the permission being checked
   */
  public AccessControlContextInfo(
      Debug debug,
      List<ObjectReference> domainReferences,
      int currentDomainIndex,
      ObjectReference permission) {
    final PermissionUtil permissions = debug.permissions();

    this.permission = permission;
    this.permissionInfos = permissions.getPermissionStrings(permission);
    this.currentDomainIndex = currentDomainIndex;
    this.domainReferences = domainReferences;
    this.domains = new ArrayList<>(domainReferences.size());
    this.privilegedDomains = new HashSet<>(domainReferences.size() * 3 / 2);
    computeDomainInfo(debug.locations(), permissions);
    this.currentDomainReference = domainReferences.get(currentDomainIndex);
    this.currentDomain = domains.get(currentDomainIndex);
  }

  @VisibleForTesting
  AccessControlContextInfo(
      List<ObjectReference> domainReferences,
      List<String> domains,
      int currentDomainIndex,
      ObjectReference permission,
      Set<String> permissionInfos,
      Set<String> privilegedDomains) {
    this.permission = permission;
    this.permissionInfos = permissionInfos;
    this.currentDomainIndex = currentDomainIndex;
    this.domainReferences = domainReferences;
    this.domains = domains;
    this.currentDomainReference = domainReferences.get(currentDomainIndex);
    this.currentDomain = domains.get(currentDomainIndex);
    this.privilegedDomains = privilegedDomains;
  }

  /**
   * Creates a new access control context information from another one corresponding corresponding
   * to a scenario where the specified domain would have been granted the permission.
   *
   * @param info the access control context information being cloned
   * @param domain the domain that would have been granted the permission
   * @return a corresponding context info
   */
  private AccessControlContextInfo(AccessControlContextInfo info, String domain) {
    this(
        info.domainReferences,
        info.domains,
        info.currentDomainIndex,
        info.permission,
        info.permissionInfos,
        // add the specified domain as a privileged one
        AccessControlContextInfo.copyAndAdd(info.privilegedDomains, domain));
  }

  /**
   * Gets the permissions associated with this access control context information.
   *
   * @return the permissions associated with this context info
   */
  public Set<String> getPermissions() {
    return permissionInfos;
  }

  /**
   * Gets the list of protection domains (i.e. bundle name/domain location) in the context.
   *
   * @return the list of protection domains (i.e. bundle name/domain location) in the context
   */
  public List<String> getDomains() {
    return domains;
  }

  /**
   * Gets the domain location/bundle name where the exception is being reported.
   *
   * @return the domain location/bundle name where the exception is being reported or <code>null
   *     </code> if unable to determine the domain location or bundle name
   */
  @Nullable
  public String getCurrentDomain() {
    return currentDomain;
  }

  /**
   * Gets the reference to the domain where the exception is being reported.
   *
   * @return the domain reference where the exception is being reported
   */
  public ObjectReference getCurrentDomainReference() {
    return currentDomainReference;
  }

  /**
   * Gets the index of the first domain in the list of domains that is reporting not being granted
   * the permission.
   *
   * @return the index of the first domain in the list of domains that is reporting not being
   *     granted the permission
   */
  public int getCurrentDomainIndex() {
    return currentDomainIndex;
  }

  /**
   * Checks if the specified domain is privileged.
   *
   * @param domain the domain to check
   * @return <code>true</code> if the specified domain is granted the permission; <code>false</code>
   *     otherwise
   */
  public boolean isPrivileged(String domain) {
    return privilegedDomains.contains(domain);
  }

  /**
   * Gets the set of privileged domains.
   *
   * @return the set of domains which are granted the permission
   */
  public Set<String> getPrivilegedDomains() {
    return privilegedDomains;
  }

  /**
   * Creates a new access control context information corresponding corresponding to a scenario
   * where the specified domain would have been granted the permission.
   *
   * @param domain the domain that would have been granted the permission
   * @return a corresponding context info
   */
  public AccessControlContextInfo grant(String domain) {
    return new AccessControlContextInfo(this, domain);
  }

  @SuppressWarnings("squid:S1871" /* order of each "if"s is important so we cannot combine them */)
  private void computeDomainInfo(LocationUtil locations, PermissionUtil permissions) {
    for (int i = 0; i < domainReferences.size(); i++) {
      final ObjectReference domainReference = domainReferences.get(i);
      final String domain = locations.get(domainReference);

      domains.add(domain);
      if (i == currentDomainIndex) {
        // we know we don't have privileges since we failed here so continue but only after
        // having added the current domain to the context list
      } else if (i < currentDomainIndex) { // we know we have privileges since we failed after `i`
        permissions.grant(domain, permissionInfos);
        privilegedDomains.add(domain);
      } else if (permissions.implies(domain, permissionInfos)) { // check cache
        privilegedDomains.add(domain);
      } else if (permissions.implies(domainReference, permission)) { // check attached VM
        permissions.grant(domain, permissionInfos);
        privilegedDomains.add(domain);
      }
    }
  }

  private static Set<String> copyAndAdd(Set<String> set, String element) {
    final Set<String> copy = new HashSet<>(set);

    copy.add(element);
    return copy;
  }
}
