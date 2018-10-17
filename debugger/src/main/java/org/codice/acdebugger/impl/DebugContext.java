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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.codice.acdebugger.ACDebugger;
import org.codice.acdebugger.api.SecurityFailure;
import org.codice.acdebugger.api.SecuritySolution;

/** This class is used to maintain context information for a given debug session. */
public class DebugContext {
  private final Backdoor backdoor = new Backdoor();

  private final SystemProperties systemProperties = new SystemProperties();

  private final Map<String, Set<String>> permissions = new ConcurrentHashMap<>();

  private final Map<String, Object> map = new ConcurrentHashMap<>();

  private final List<SecurityFailure> failures = new ArrayList<>();

  private int count = 0;

  private volatile boolean osgi = true;

  private volatile boolean continuous = false;

  private volatile boolean granting = false;

  private volatile boolean failing = false;

  private volatile boolean monitoring = false;

  private volatile boolean debug = false;

  private volatile boolean doPrivileged = true;

  private volatile boolean run = true;

  /**
   * Accesses the backdoor utility.
   *
   * @return the backdoor utility
   */
  public Backdoor getBackdoor() {
    return backdoor;
  }

  /**
   * Accesses system properties util functionality.
   *
   * @return a system properties utility for the attached VM
   */
  public SystemProperties properties() {
    return systemProperties;
  }

  /**
   * Checks if a domain has or was temporarily granted a given permission.
   *
   * @param domain the bundle name or domain location to check for
   * @param permission the permission to check for
   * @return <code>true</code> if the domain has or was granted the specified permission; <code>
   *     false</code> if not
   */
  public boolean hasPermission(String domain, String permission) {
    if (domain == null) { // bundle-0 always has all permissions
      return true;
    }
    return permissions
        .computeIfAbsent(domain, d -> new ConcurrentSkipListSet<>())
        .contains(permission);
  }

  /**
   * Checks if a domain is or was temporarily granted the given permissions.
   *
   * @param domain the bundle name or domain location to check for
   * @param permissions the permission strings to check for
   * @return <code>true</code> if the bundle is or was granted the specified permissions; <code>
   *     false</code> if not
   */
  public boolean hasPermissions(String domain, Set<String> permissions) {
    if (domain == null) { // bundle-0 always has all permissions
      return true;
    }
    return this.permissions
        .computeIfAbsent(domain, d -> new ConcurrentSkipListSet<>())
        .containsAll(permissions);
  }

  /**
   * Temporarily grants a domain a given permission if not already granted.
   *
   * @param domain the bundle name or domain location to grant the permission for
   * @param permission the permission to be granted
   * @return <code>true</code> if the permission was granted to the specified domain; <code>false
   *     </code> if it was already granted
   */
  public boolean grantPermission(String domain, String permission) {
    if (domain == null) { // bundle-0 always has all permissions
      return false;
    }
    return permissions.computeIfAbsent(domain, b -> new ConcurrentSkipListSet<>()).add(permission);
  }

  /**
   * Temporarily grants a domain a set of permissions if not already granted.
   *
   * @param domain the bundle name or domain location to grant the permissions to
   * @param permissions the permissions to be granted
   * @return <code>true</code> if the permissions were granted to the specified domain; <code>false
   *     </code> if at least one was already granted
   */
  public boolean grantPermissions(String domain, Set<String> permissions) {
    if (domain == null) {
      return false;
    }
    final Set<String> cache =
        this.permissions.computeIfAbsent(domain, d -> new ConcurrentSkipListSet<>());

    return permissions.stream().map(cache::add).reduce(true, Boolean::logicalAnd);
  }

  /**
   * Retrieves a value given a key from the current debug context's cache.
   *
   * @param <T> the type of the value to retrieve
   * @param key the key for the value to retrieve
   * @return the value currently associated with the given key or <code>null</code> if none
   *     associated
   */
  @Nullable
  public <T> T get(String key) {
    return (T) map.get(key);
  }

  /**
   * If the specified key is not already associated with a value (or is mapped to <code>null</code>
   * ), attempts to compute its value using the given supplier and enters it into this map unless
   * <code>null</code>.
   *
   * @param <T> the type of the value to retrieve
   * @param key the key for the value to retrieve
   * @param supplier the supplier used to generate the first value if none already associated
   * @return the value currently associated with the given key or <code>null</code> if none
   *     associated
   */
  public <T> T computeIfAbsent(String key, Supplier<T> supplier) {
    return (T) map.computeIfAbsent(key, k -> supplier.get());
  }

  /**
   * Stores a value in the debug context for a given key.
   *
   * @param <T> the type of the value to store
   * @param key the key for the value to store
   * @param obj the value to be stored
   */
  public <T> void put(String key, T obj) {
    map.put(key, obj);
  }

  /**
   * Checks if we are debugging an OSGi system.
   *
   * @return <code>true</code> if we are debugging an OSGi system; <code>false</code> if not
   */
  public boolean isOSGi() {
    return osgi;
  }

  /**
   * Sets wether or not we are debugging an OSGi system.
   *
   * @param osgi <code>true</code> if we are debugging an OSGi system; <code>false</code> if not
   */
  public void setOSGi(boolean osgi) {
    this.osgi = osgi;
  }

  /**
   * Checks if the debugger is running in continuous mode where it will not fail any security
   * exceptions detected and accumulate and report on all occurrences or if it will stop when the
   * first security failure is detected.
   *
   * @return <code>true</code> if debugging should be continuous and report all failures; <code>
   *     false</code> if it should stop at the first detected security failure
   */
  public boolean isContinuous() {
    return continuous;
  }

  /**
   * Sets the continuous mode for the debugger in which case it will not fail any security
   * exceptions detected and accumulate and report on all occurrences or if it will stop when the
   * first security failure is detected.
   *
   * @param continuous <code>true</code> if the debugger should process all breakpoint callbacks and
   *     continue as if no errors had occurred; <code>false</code> if i should stop after the first
   *     detected security failure
   */
  public void setContinuous(boolean continuous) {
    this.continuous = continuous;
  }

  /**
   * Checks if the debug mode has been enabled.
   *
   * @return <code>true</code> if the debugger will output additional security failure information
   *     as they are detected; <code>false</code> if only solutions are presented on the console
   */
  public boolean isDebug() {
    return debug;
  }

  /**
   * Sets the debug mode where information about security failures detected is dumped to the console
   * in addition to all the solutions.
   *
   * @param debug <code>true</code> to enable debug output of security failure information as they
   *     are detected; <code>false</code> to only preset solutions
   */
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  /**
   * Checks if we are automatically grant missing permissions when a single solution monitor is
   * found for a given service failure.
   *
   * @return <code>true</code> if we are automatically grating missing permissions; <code>false
   *     </code> if not
   */
  public boolean isGranting() {
    return granting;
  }

  /**
   * Sets whether or not to automatically grant missing permissions when a single solution monitor
   * is found for a given service failure.
   *
   * @param granting <code>true</code> to automatically grant missing permissions; <code>false
   *     </code> not to
   */
  public void setGranting(boolean granting) {
    this.granting = granting;
  }

  /**
   * Checks if security exceptions should be left to fail as they would have normally.
   *
   * @return <code>true</code> if security exceptions should be left to fail normally; <code>false
   *     </code> if we should let the VM think there was no error
   */
  public boolean isFailing() {
    return failing;
  }

  /**
   * Sets whether or not to let security exceptions fail.
   *
   * @param failing <code>true</code> to automatically let security exceptions fail; <code>false
   *     </code> to let the VM think there was no error
   */
  public void setFailing(boolean failing) {
    this.failing = failing;
  }

  /**
   * Checks if we are monitoring service event permission checks.
   *
   * @return <code>true</code> if we are monitoring service event permission checks; <code>false
   *     </code> if not
   */
  public boolean isMonitoringService() {
    return monitoring;
  }

  /**
   * Sets whether or not to monitor service event permission checks.
   *
   * @param monitoring <code>true</code> to monitor service event permission checks; <code>false
   *     </code> not to
   */
  public void setMonitoringService(boolean monitoring) {
    this.monitoring = monitoring;
  }

  /**
   * Checks whether or not to consider <code>doPrivileged()</code> blocks when analysing security
   * failures.
   *
   * @return <code>true</code> to consider <code>doPrivileged()</code> blocks for possible
   *     solutions; <code>false</code> not to
   */
  public boolean canDoPrivilegedBlocks() {
    return doPrivileged;
  }

  /**
   * Sets whether or not to consider <code>doPrivileged()</code> blocks when analysing security
   * failures.
   *
   * @param doPrivileged <code>true</code> to consider <code>doPrivileged()</code> blocks for
   *     possible solutions; <code>false</code> not to
   */
  public void setDoPrivilegedBlocks(boolean doPrivileged) {
    this.doPrivileged = doPrivileged;
  }

  /**
   * Checks if the debugger is still running. The debugger shall stop after processing the current
   * set of breakpoints once requested to do so.
   *
   * @return <code>true</code> if the debugger is still running; <code>false</code> if it was
   *     requested to stop
   */
  public boolean isRunning() {
    return run;
  }

  /** Notifies to stop debugging after processing the current breakpoint callback. */
  public void stop() {
    this.run = false;
  }

  /**
   * Records information about a security failure.
   *
   * @param failure the security failure to record
   */
  public void record(SecurityFailure failure) {
    synchronized (failures) {
      failures.add(failure);
      if (failure.isAcceptable()) {
        recordAcceptableFailure(failure);
      } else {
        recordUnacceptableFailure(failure);
      }
    }
  }

  @SuppressWarnings("squid:S106" /* this is a console application */)
  private void recordAcceptableFailure(SecurityFailure failure) {
    // check to see if we have another recorded failure with the exact same reason for being
    // acceptable in which case we do not want to trace it again
    if (failures
        .stream()
        .filter(i -> i != failure)
        .filter(SecurityFailure::isAcceptable)
        .anyMatch(
            f ->
                f.getAcceptablePermissions().equals(failure.getAcceptablePermissions())
                    && f.getStack().equals(failure.getStack()))) {
      return;
    }
    if (debug) {
      failure.dump(osgi, continuous ? String.format("%04d - ", (++count)) : "");
    } else {
      if (continuous) {
        System.out.printf("%s%04d - %s {%n", ACDebugger.PREFIX, (++count), failure);
      } else {
        System.out.printf("%s%s {%n", ACDebugger.PREFIX, failure);
      }
      System.out.println(ACDebugger.PREFIX + "}");
    }
  }

  @SuppressWarnings("squid:S106" /* this is a console application */)
  private void recordUnacceptableFailure(SecurityFailure failure) {
    final List<SecuritySolution> solutions = failure.analyze();

    if (solutions.isEmpty()) { // no solutions so ignore
      return;
    }
    // check to see if we have another recorded failure with the exact same set of solutions
    if (failures
        .stream()
        .filter(i -> i != failure)
        .filter(((Predicate<SecurityFailure>) SecurityFailure::isAcceptable).negate())
        .map(SecurityFailure::analyze)
        .anyMatch(solutions::equals)) {
      return;
    }
    if (!continuous) { // stop processing
      this.run = false;
    }
    // check if we have only one solution and that solution is to only grant permission(s)
    // (no privileged blocks) in which case we shall cache them to avoid going through all
    // of this again (if not in continuous mode, then we don't really care about that)
    grantMissingPermissionsIfPossible(solutions);
    if (debug) {
      failure.dump(osgi, continuous ? String.format("%04d - ", (++count)) : "");
    } else {
      if (continuous) {
        System.out.printf("%s%04d - %s {%n", ACDebugger.PREFIX, (++count), failure);
      } else {
        System.out.printf("%s%s {%n", ACDebugger.PREFIX, failure);
      }
      if (solutions.size() > 1) {
        System.out.println(
            ACDebugger.PREFIX
                + "    Analyze the following "
                + solutions.size()
                + " solutions and choose the best:");
      } else {
        System.out.println(ACDebugger.PREFIX + "    Solution:");
      }
      solutions.forEach(s -> s.print(osgi, "    "));
      System.out.println(ACDebugger.PREFIX + "}");
      if (!continuous) {
        this.run = false;
      }
    }
  }

  private void grantMissingPermissionsIfPossible(List<SecuritySolution> solutions) {
    // check if we have only one solution and that solution is to only grant permission(s)
    // (no privileged blocks) in which case we shall cache them to avoid going through all
    // of this again (if not in continuous mode, then we don't really care about that)
    if (continuous && (solutions.size() == 1)) {
      final SecuritySolution solution = solutions.get(0);
      final Set<String> grantedDomains = solution.getGrantedDomains();

      if (!grantedDomains.isEmpty() && solution.getDoPrivilegedLocations().isEmpty()) {
        solution.getGrantedDomains().forEach(d -> grantPermissions(d, solution.getPermissions()));
      }
    }
  }
}
