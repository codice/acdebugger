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

import com.google.common.collect.Ordering;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.codice.acdebugger.api.Debug;
import org.codice.acdebugger.api.SecurityFailure;
import org.codice.acdebugger.api.SecuritySolution;
import org.codice.acdebugger.api.StackFrameInformation;

/**
 * This class serves 2 purposes. It is first a representation of a detected security check failure.
 * During analysis, it is also used to compute and report possible security solutions to the
 * security failure.
 */
@SuppressWarnings("squid:S1191" /* Using the Java debugger API */)
class SecurityCheckInformation extends SecuritySolution implements SecurityFailure {
  /** context array from AccessControlContext at line 472 */
  private final List<Value> context;

  /** domain where the exception is about to be generated for */
  private final Value currentDomain;

  /** bundle where the exception is about to be generated for */
  private final String currentBundle;

  /** list of protection domains (i.e. bundle name for the domain) in the security stack */
  private final List<String> domains;

  /* list of protection domains (i.e. bundle name for the domain) that are granted the failed permissions */
  private final Set<String> privilegedDomains;

  /**
   * index in the context where the security manager found the first domain that doesn't have the
   * failed permissions
   */
  private int failedDomainIndex = -1;

  /** the failed permission object */
  private final ObjectReference permission;

  /** the full stack trace at the point the security exception is about to be thrown */
  private final List<StackFrameInformation> stack;

  /**
   * the index in the stack for the code that doesn't have the failed permission, -1 if no failures
   */
  private int failedStackIndex = -1;

  /**
   * the stack index of that last place a bundle extended its privileges or -1 if none everything
   * between 0 til this index is of interest for the security manager
   */
  private int privilegedStackIndex = -1;

  private final boolean invalid;

  private List<SecuritySolution> analysis = null;

  /**
   * Creates a security check failure.
   *
   * @param debug the current debug session
   * @param context the context of permission domain at the point of imminent failure
   * @param local_i the current loop index through the domain context in the code where the
   *     breakpoint occurred
   * @param permission the permission being checked
   * @throws Exception if unable to create a new security check failure
   */
  @SuppressWarnings("squid:S00117" /* name is clearer that way */)
  SecurityCheckInformation(
      Debug debug, ArrayReference context, int local_i, ObjectReference permission)
      throws Exception {
    this(debug, context.getValues(), local_i, permission);
  }

  /**
   * Creates a security check failure.
   *
   * @param debug the current debug session
   * @param context the context of permission domain at the point of imminent failure
   * @param local_i the current loop index through the domain context in the code where the
   *     breakpoint occurred
   * @param permission the permission being checked
   * @throws Exception if unable to create a new security check failure
   */
  @SuppressWarnings({
    "squid:S00117", /* name is clearer that way */
    "squid:S00112" /* Forced to by the Java debugger API */
  })
  private SecurityCheckInformation(
      Debug debug, List<Value> context, int local_i, ObjectReference permission) throws Exception {
    super(
        debug.permissions().getPermissionStrings(permission),
        Collections.emptySet(),
        Collections.emptyList());
    this.context = context;
    this.currentDomain = context.get(local_i);
    this.currentBundle = debug.bundles().get(currentDomain);
    if (currentBundle == null) {
      // since bundle-0 always has all permissions, we cannot received null as the current bundle
      // where the failure occurred
      throw new Error("unable to find bundle for domain: " + currentDomain.type().name());
    }
    this.domains = new ArrayList<>(context.size());
    this.permission = permission;
    this.privilegedDomains = new HashSet<>(context.size() * 3 / 2);
    for (int i = 0; i < context.size(); i++) {
      if (i == local_i) { // we know we don't have privileges since we failed here
        continue;
      }
      final Value domain = context.get(i);
      final String bundle = (domain == currentDomain) ? currentBundle : debug.bundles().get(domain);

      if (i < local_i) { // we know we have privileges since we failed after `i`
        debug.permissions().grant(bundle, permissionInfos);
        privilegedDomains.add(bundle);
      } else if (domain instanceof ObjectReference) {
        if (debug.permissions().implies(bundle, permissionInfos)) { // check cache
          privilegedDomains.add(bundle);
        } else if (debug
            .permissions()
            .implies((ObjectReference) domain, permission)) { // check attached VM
          debug.permissions().grant(bundle, permissionInfos);
          privilegedDomains.add(bundle);
        }
      }
    }
    this.stack = new ArrayList<>(debug.thread().frameCount());
    // don't cache the set of stack as it will change every time we invoke()
    // something using the thread
    for (int i = 0; i < debug.thread().frameCount(); i++) {
      final StackFrame frame = debug.thread().frame(i);
      final Location location = frame.location(); // cache before we invoke anything on the thread
      final ObjectReference thisObject = frame.thisObject();
      final String bundle = debug.bundles().get(frame);
      final StackFrameInformation currentFrame =
          new StackFrameInformation(bundle, location, thisObject);

      stack.add(currentFrame);
    }
    this.invalid = !recompute();
    analyze0(debug);
  }

  /**
   * Creates a possible solution as if we granted the missing permission to the failed bundle to be
   * analyzed for the given security check failure.
   *
   * @param failure the security check failure for which to create a possible solution
   */
  private SecurityCheckInformation(SecurityCheckInformation failure) {
    super(failure);
    this.context = failure.context;
    this.currentDomain = failure.currentDomain;
    this.currentBundle = failure.currentBundle;
    this.domains = new ArrayList<>(failure.domains.size());
    this.privilegedDomains = new HashSet<>(failure.privilegedDomains);
    this.permission = failure.permission;
    this.stack = failure.stack;
    // add the specified bundle as a privileged one
    privilegedDomains.add(failure.getFailedBundle());
    grantedBundles.add(failure.getFailedBundle());
    this.invalid = !recompute();
  }

  /**
   * Creates a possible solution as if we were extending privileges of the bundle at the specified
   * stack index to be analyzed for the given security check failure.
   *
   * @param failure the security check failure for which to create a possible solution
   * @param index the index in the stack where to extend privileges
   */
  private SecurityCheckInformation(SecurityCheckInformation failure, int index) {
    super(failure);
    this.context = failure.context;
    this.currentDomain = failure.currentDomain;
    this.currentBundle = failure.currentBundle;
    this.domains = new ArrayList<>(failure.domains.size());
    this.privilegedDomains = failure.privilegedDomains;
    this.permission = failure.permission;
    this.stack = new ArrayList<>();
    final Set<String> newStackDomainsUpToDoPrivileged = new HashSet<>();

    // extend the stack by adding a fake doPrivileged() at the specified index and duplicating that
    // frame after it to show that it is still in the stack after the call to doPrivileged()
    for (int i = 0; i <= index; i++) {
      final StackFrameInformation frame = failure.stack.get(i);

      stack.add(frame);
      newStackDomainsUpToDoPrivileged.add(frame.getBundle());
    }
    stack.add(StackFrameInformation.DO_PRIVILEGED);
    doPrivileged.add(failure.stack.get(index));
    for (int i = index; i < failure.stack.size(); i++) {
      stack.add(failure.stack.get(i));
    }
    this.invalid = !recompute();
  }

  /**
   * Gets the name of the bundle where the failure was detected.
   *
   * @return the name of the bundle where the failure was detected or <code>null</code> if no
   *     failure is recomputed from the solution
   */
  @Nullable
  public String getFailedBundle() {
    return (failedDomainIndex != -1) ? domains.get(failedDomainIndex) : null;
  }

  @Override
  public Set<String> getPermissions() {
    return Collections.unmodifiableSet(permissionInfos);
  }

  @Override
  public Set<String> getGrantedBundles() {
    return Collections.unmodifiableSet(grantedBundles);
  }

  @Override
  public List<StackFrameInformation> getDoPrivilegedLocations() {
    return Collections.unmodifiableList(doPrivileged);
  }

  @Override
  public List<SecuritySolution> analyze() {
    return Ordering.natural().sortedCopy(analysis);
  }

  @SuppressWarnings("squid:S106" /* this is a console application */)
  @Override
  public void dump(String prefix) {
    final String first = prefix + "ACCESS CONTROL PERMISSION FAILURE";

    System.out.println(first);
    System.out.println(
        IntStream.range(1, first.length()).mapToObj(i -> "=").collect(Collectors.joining("")));
    dump0();
    analyze();
    for (int i = 0; i < analysis.size(); i++) {
      final SecuritySolution info = analysis.get(i);

      System.out.println("");
      System.out.println("OPTION " + (i + 1));
      System.out.println("--------");
      ((SecurityCheckInformation) info).dump0();
    }
    System.out.println("");
    System.out.println("SOLUTIONS");
    System.out.println("---------");
    analysis.forEach(SecuritySolution::print);
  }

  @Override
  public String toString() {
    if (currentBundle == null) {
      return "";
    } else if (permissionInfos.size() == 1) {
      return "Check permission failure for "
          + currentBundle
          + ": "
          + permissionInfos.iterator().next();
    }
    return "Check permissions failure for " + currentBundle + ": " + permissionInfos;
  }

  /**
   * This method reproduces what the {@link
   * java.security.AccessControlContext#checkPermission(Permission)} does whenever it checks for
   * permissions. It goes through the stack and builds a list of domains based on each stack frame
   * encountered. If the domain is already known it moves on. if it encounters the doPrivileged()
   * block, it stops processing the stack. As it goes through it, it checks if the corresponding
   * domain implies() the permission it currently checks and if not, it would normally generate the
   * exception.
   *
   * <p>By re-implementing this logic, we can now see what would happen if we change permissions or
   * if we extend privileges at a specific location in our code. It actually allows us to verify if
   * there would be a line later that would create another problem.
   *
   * <p>When the breakpoint is invoked, we could extract that information from the loop in the
   * <code>AccessControlContext.checkPermission()</code> method. But instead of doing that, it is
   * simpler to keep the same logic to recompute. I kept the code around and left it in the private
   * constructor with the dummy parameter.
   *
   * @return <code>true</code> if all granted domains were required; <code>false</code> if we didn't
   *     need all of them which would mean this is an invalid option as we are granting more than we
   *     need
   */
  private boolean recompute() {
    domains.clear();
    this.privilegedStackIndex = -1;
    this.failedStackIndex = -1;
    final Set<String> grantedDomains = new HashSet<>(this.grantedBundles);
    String failedBundle = null;

    for (int i = 0; i < stack.size(); i++) {
      final StackFrameInformation frame = stack.get(i);

      if (frame.isDoPrivilegedBlock()) { // stop here
        this.privilegedStackIndex = i + 1;
        break;
      }
      final String bundle = frame.getBundle();

      if (failedBundle == null) {
        if ((bundle != null) && !domains.contains(bundle)) {
          domains.add(bundle);
        }
        if (!frame.isPrivileged(privilegedDomains)) { // found the place where it will fail!!!!
          failedBundle = bundle;
          this.failedStackIndex = i;
          this.failedDomainIndex = domains.indexOf(bundle);
        } else {
          // keep track of the fact that this granted domain helped if it was one
          // that we artificially granted the permission to
          grantedDomains.remove(frame.getBundle());
        }
      }
    }
    return grantedDomains.isEmpty();
  }

  private boolean isInvalidOrHasNoFailuresDetected() {
    return (failedStackIndex == -1) || invalid;
  }

  private List<SecuritySolution> analyze0(Debug debug) {
    if (analysis == null) {
      if (isInvalidOrHasNoFailuresDetected()) {
        // no issues here or invalid options so nothing to be done
        this.analysis = Collections.singletonList(this);
      } else {
        this.analysis = new ArrayList<>();
        // first see what happens if we grant the missing permission to the failed domain
        analysis.addAll(new SecurityCheckInformation(this).analyze0(debug));
        if (debug.canDoPrivilegedBlocks()) {
          analyzeDoPrivilegedBlocks(debug);
        }
      }
    }
    return Ordering.natural().sortedCopy(analysis);
  }

  private void analyzeDoPrivilegedBlocks(Debug debug) {
    // now check if we could extend the privileges of a bundle that comes up
    // before which already has the permission
    for (int i = failedStackIndex - 1; i >= 0; i--) {
      final StackFrameInformation frame = stack.get(i);

      if (frame.isPrivileged(privilegedDomains) && frame.canDoPrivilegedBlocks(debug)) {
        analysis.addAll(new SecurityCheckInformation(this, i).analyze0(debug));
      }
    }
  }

  @SuppressWarnings("squid:S106" /* this is a console application */)
  private void dump0() {
    final String s = (permissionInfos.size() == 1) ? "" : "s";

    System.out.println("Permission" + s + ":");
    permissionInfos.forEach(p -> System.out.println("    " + p));
    if (!grantedBundles.isEmpty()) {
      System.out.println("Granting permission to bundles:");
      grantedBundles.forEach(d -> System.out.println("    " + d));
    }
    if (!doPrivileged.isEmpty()) {
      System.out.println("Extending privileges at:");
      doPrivileged.forEach(f -> System.out.println("    " + f));
    }
    System.out.println("Context:");
    System.out.println("     " + StackFrameInformation.BUNDLE0);
    for (int i = 0; i < domains.size(); i++) {
      final String domain = domains.get(i);

      System.out.println(
          " "
              + ((i == failedDomainIndex) ? "--> " : "    ")
              + (privilegedDomains.contains(domain) ? "" : "*")
              + domain);
    }
    System.out.println("Stack:");
    for (int i = 0; i < stack.size(); i++) {
      System.out.println(
          " "
              + ((i == failedStackIndex) ? "-->" : "   ")
              + " at "
              + stack.get(i).toString(privilegedDomains));
      if ((privilegedStackIndex != -1) && (i == privilegedStackIndex)) {
        System.out.println("    ----------------------------------------------------------");
      }
    }
  }
}
