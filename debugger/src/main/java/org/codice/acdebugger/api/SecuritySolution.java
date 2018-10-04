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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Represents a solution to a security failure that was detected while debugging. */
public class SecuritySolution implements Comparable<SecuritySolution> {
  /** The failed permissions info */
  protected final Set<String> permissionInfos;

  /** Bundles that are granted the permission */
  protected final Set<String> grantedBundles;

  /** List of locations where to add doPrivileged() blocks */
  protected final List<StackFrameInformation> doPrivileged;

  /**
   * Constructs a solution.
   *
   * @param permissionInfos the failed permissions info
   * @param grantedBundles bundles that are granted the permission
   * @param doPrivileged list of locations where to add doPrivileged() blocks
   */
  public SecuritySolution(
      Set<String> permissionInfos,
      Set<String> grantedBundles,
      List<StackFrameInformation> doPrivileged) {
    this.permissionInfos = permissionInfos;
    this.grantedBundles = grantedBundles;
    this.doPrivileged = doPrivileged;
  }

  /**
   * Constructs a solution.
   *
   * @param solution the original solution to clone
   */
  protected SecuritySolution(SecuritySolution solution) {
    this.permissionInfos = solution.permissionInfos;
    this.grantedBundles = new LinkedHashSet<>(solution.grantedBundles);
    this.doPrivileged = new ArrayList<>(solution.doPrivileged);
  }

  /**
   * Gets the permissions associated with this security solution.
   *
   * @return the permissions associated with this security solution
   */
  public Set<String> getPermissions() {
    return Collections.unmodifiableSet(permissionInfos);
  }

  /**
   * Gets the bundles that have to be given permissions for this solution.
   *
   * @return bundles requiring permissions for this solution
   */
  public Set<String> getGrantedBundles() {
    return Collections.unmodifiableSet(grantedBundles);
  }

  /**
   * Gets the locations where to introduce <code>doPrivileged()</code> blocks for this solution.
   *
   * @return the locations where to introduce <code>doPrivileged()</code> blocks for this solution
   */
  public List<StackFrameInformation> getDoPrivilegedLocations() {
    return Collections.unmodifiableList(doPrivileged);
  }

  /** Prints information about this solution. */
  public void print() {
    print("");
  }

  /**
   * Prints information about this solution.
   *
   * @param prefix a prefix string to prepend to each printed line
   */
  @SuppressWarnings("squid:S106" /* this is a console application */)
  public void print(String prefix) {
    System.out.println(prefix + "{");
    final String and;

    if (!grantedBundles.isEmpty()) {
      final String s = (permissionInfos.size() == 1) ? "" : "s";

      System.out.println(
          prefix + "    Add the following permission" + s + " to the appropriate policy file:");
      System.out.println(
          prefix
              + "        grant codeBase \"file:/"
              + grantedBundles.stream().sorted().collect(Collectors.joining("/"))
              + "\" {");
      permissionInfos
          .stream()
          .sorted()
          .forEach(p -> System.out.println(prefix + "            permission " + p + ";"));
      System.out.println(prefix + "        }");
      and = "and a";
    } else {
      and = "A";
    }
    if (!doPrivileged.isEmpty()) {
      System.out.println(
          prefix + "    " + and + "dd an AccessController.doPrivileged() block around:");
      doPrivileged.forEach(f -> System.out.println(prefix + "        " + f));
    }
    System.out.println(prefix + "}");
  }

  @Override
  public final int compareTo(SecuritySolution s) {
    if (s == this) {
      return 0;
    }
    // compare how many bundles we have to grant the permissions to
    // we favor options where we have to grant the least number of permissions
    int d = compareSizes(grantedBundles, s.grantedBundles);

    if (d != 0) {
      return d;
    }
    if (!grantedBundles.isEmpty()) {
      // compare how many permissions we have to grant and favor the least number of permissions
      d = compareSizes(permissionInfos, s.permissionInfos);
      if (d != 0) {
        return d;
      }
    }
    // compare the # of doPrivileged blocks
    d = compareSizes(doPrivileged, s.doPrivileged);
    if (d != 0) {
      return d;
    }
    // finally, compare each doPrivileged locations to find the best choice
    for (int i = 0; i < doPrivileged.size(); i++) {
      d = doPrivileged.get(i).compareTo(s.doPrivileged.get(i));
      if (d != 0) {
        return d;
      }
    }
    return 0;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(permissionInfos, grantedBundles, doPrivileged);
  }

  @Override
  public final boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof SecuritySolution) {
      final SecuritySolution s = (SecuritySolution) obj;

      return permissionInfos.equals(s.permissionInfos)
          && grantedBundles.equals(s.grantedBundles)
          && doPrivileged.equals(s.doPrivileged);
    }
    return false;
  }

  private int compareSizes(Collection<?> c1, Collection<?> c2) {
    if (c1.size() == c2.size()) {
      return 0;
    } else if (c1.size() > c2.size()) {
      return 1;
    }
    return -1;
  }
}
