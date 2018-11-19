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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;
import java.io.IOError;
import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.codice.acdebugger.ACDebugger;
import org.codice.acdebugger.api.Debug;
import org.codice.acdebugger.api.SecurityFailure;
import org.codice.acdebugger.api.SecuritySolution;
import org.codice.acdebugger.api.StackFrameInformation;

/**
 * This class serves 2 purposes. It is first a representation of a detected security check failure.
 * During analysis, it is also used to compute and report possible security solutions to the
 * security failure.
 */
class SecurityCheckInformation extends SecuritySolution implements SecurityFailure {
  private static final String DOUBLE_LINES =
      "=======================================================================";

  /**
   * List of patterns to match security check information that should be considered acceptable
   * failures and skipped.
   */
  private static final List<Pattern> ACCEPTABLE_PATTERNS;

  static {
    try {
      ACCEPTABLE_PATTERNS =
          Resources.readLines(
              Resources.getResource("acceptable-security-check-failures.txt"),
              Charsets.UTF_8,
              new PatternProcessor());
    } catch (IOException e) {
      throw new IOError(e);
    }
  }

  /** The associated debug session. */
  private final Debug debug;

  /** The context information retrieved from the AccessControlContext class at line 472. */
  private final AccessControlContextInfo context;

  /**
   * list of protection domains (i.e. bundle name/domain location) in the security context as
   * recomputed here. This list will not contain nulls or duplicates.
   */
  private final List<String> domains;

  /**
   * the index in the stack for the frame that doesn't have the failed permission, -1 if no failures
   */
  private int failedStackIndex = -1;

  /**
   * the stack index of the last place (i.e. lowest index) a domain extended its privileges or -1 if
   * none found. Everything between 0 til this index is of interest for the security manager
   */
  private int privilegedStackIndex = -1;

  /**
   * index in the domains list where we recomputed the reported security exception to be generated
   * for or <code>-1</code> if no failures.
   */
  private int failedDomainIndex = -1;

  /**
   * index in the domains list where we estimate the security manager started combining additional
   * domains not coming from the stack. Below this mark are pure domains extracted from the current
   * stack. At this mark and above are domains that were combined. Will be <code>-1</code> if no
   * combined domains were added.
   */
  private int combinedDomainsStartIndex = -1;

  private final boolean invalid;

  /** First pattern that was matched indicating the failure was acceptable. */
  @Nullable private Pattern acceptablePattern = null;

  @Nullable private List<SecuritySolution> analysis = null;

  /**
   * Creates a security check failure from a given access control context.
   *
   * @param debug the current debug session
   * @param context the context information to be analyzed
   * @throws Exception if unable to create a new security check failure
   */
  @SuppressWarnings("squid:S00112" /* Forced to by the Java debugger API */)
  SecurityCheckInformation(Debug debug, AccessControlContextInfo context) throws Exception {
    super(
        debug.threadStack(),
        context.getPermissions(),
        Collections.emptySet(),
        Collections.emptyList());
    this.debug = debug;
    this.context = context;
    this.domains = new ArrayList<>(context.getDomains().size());
    if (context.getCurrentDomain() == null) {
      // since bundle-0/boot domain always has all permissions, we cannot received null as the
      // current domain where the failure occurred
      dumpTroubleshootingInfo(
          "AN ERROR OCCURRED WHILE ATTEMPTING TO FIND THE LOCATION FOR A DOMAIN");
      throw new Error(
          "unable to find location for domain: "
              + context.getCurrentDomainReference().type().name());
    }
    this.invalid = !recompute();
    analyze0();
  }

  /**
   * Creates a possible solution as if we granted the missing permission to the failed domain to be
   * analyzed for the given security check failure.
   *
   * @param failure the security check failure for which to create a possible solution
   */
  private SecurityCheckInformation(SecurityCheckInformation failure) {
    super(failure);
    this.debug = failure.debug;
    // add the failed domain from the specified failure as a privileged and as a granted one
    super.grantedDomains.add(failure.getFailedDomain());
    this.context = failure.context.grant(failure.getFailedDomain());
    // the combined domain start index is something fixed provided to us when the error is detected
    // this won't change because we are simply granting permissions to domains as this wouldn't
    // change the stack or the access control context as seen originally
    this.combinedDomainsStartIndex = failure.combinedDomainsStartIndex;
    this.domains = new ArrayList<>(failure.domains.size());
    this.invalid = !recompute();
  }

  /**
   * Creates a possible solution as if we were extending privileges of the domain at the specified
   * stack index to be analyzed for the given security check failure.
   *
   * @param failure the security check failure for which to create a possible solution
   * @param index the index in the stack where to extend privileges
   */
  private SecurityCheckInformation(SecurityCheckInformation failure, int index) {
    super(failure, index);
    this.debug = failure.debug;
    // because we are extending privileges which would be before any combined domains gets added to
    // the access control context, we can safely account for the fact that these combined domains
    // won't be present anymore in the new and revised access control context, as such we should
    // simply clear whatever start index would normally be inherited from the provided failure
    // we will also make sure that while recomputing the resulting failure, we ignore combined
    // domains provided by the original access control context
    this.context = failure.context;
    this.combinedDomainsStartIndex = -1;
    this.domains = new ArrayList<>(failure.domains.size());
    this.invalid = !recompute();
  }

  /**
   * Gets the domain where the failure was detected.
   *
   * @return the domain where the failure was detected or <code>null</code> if no failure is
   *     recomputed from the solution
   */
  @Nullable
  public String getFailedDomain() {
    return (failedDomainIndex != -1) ? domains.get(failedDomainIndex) : null;
  }

  @Override
  public boolean isAcceptable() {
    return acceptablePattern != null;
  }

  @Override
  @Nullable
  public String getAcceptablePermissions() {
    return isAcceptable() ? "REGEX: " + acceptablePattern.getPermissionInfos() : null;
  }

  @Override
  public List<SecuritySolution> analyze() {
    return analysis;
  }

  @SuppressWarnings("squid:S106" /* this is a console application */)
  @Override
  public void dump(boolean osgi, String prefix) {
    final String first =
        prefix + (isAcceptable() ? "ACCEPTABLE " : "") + "ACCESS CONTROL PERMISSION FAILURE";

    System.out.println(ACDebugger.PREFIX);
    System.out.println(ACDebugger.PREFIX + first);
    System.out.println(
        ACDebugger.PREFIX
            + IntStream.range(0, first.length())
                .mapToObj(i -> "=")
                .collect(Collectors.joining("")));
    dump0(osgi);
    for (int i = 0; i < analysis.size(); i++) {
      final SecuritySolution info = analysis.get(i);

      System.out.println(ACDebugger.PREFIX);
      System.out.println(ACDebugger.PREFIX + "OPTION " + (i + 1));
      System.out.println(ACDebugger.PREFIX + "--------");
      ((SecurityCheckInformation) info).dump0(osgi);
    }
    if (!analysis.isEmpty()) {
      System.out.println(ACDebugger.PREFIX);
      System.out.println(ACDebugger.PREFIX + "SOLUTIONS");
      System.out.println(ACDebugger.PREFIX + "---------");
      analysis.forEach(s -> s.print(osgi));
    }
  }

  @Override
  public String toString() {
    if (!grantedDomains.isEmpty() || !doPrivileged.isEmpty()) { // we have a solution
      return super.toString();
    }
    final String currentDomain = context.getCurrentDomain();

    if (currentDomain == null) {
      return "";
    }
    if (isAcceptable()) {
      return "Acceptable check permissions failure for "
          + currentDomain
          + ": "
          + getAcceptablePermissions();
    } else {
      if (permissionInfos.size() == 1) {
        return "Check permission failure for "
            + currentDomain
            + ": "
            + permissionInfos.iterator().next();
      }
      return "Check permissions failure for " + currentDomain + ": " + permissionInfos;
    }
  }

  /**
   * Gets the associated access control context information.
   *
   * @return the associated access control context information
   */
  @VisibleForTesting
  AccessControlContextInfo getContext() {
    return context;
  }

  /**
   * Gets the index in the stack for the frame that doesn't have the failed permission.
   *
   * @return the index in the stack for the frame that doesn't have the failed permission, <code>-1
   *     </code> if no failures
   */
  @VisibleForTesting
  int getFailedStackIndex() {
    return failedStackIndex;
  }

  /**
   * Gets the index in the stack of the last place (i.e. lowest index) a domain extended its
   * privileges. Everything between 0 til this index is of interest for the security manager. Frames
   * deemed to do that on behalf of their caller are ignored.
   *
   * @return the stack index of the last place a domain extended its privileges or <code>-1</code>
   *     if none found
   */
  @VisibleForTesting
  int getPrivilegedStackIndex() {
    return privilegedStackIndex;
  }

  /**
   * Gets the protection domains (i.e. bundle name/domain location) in the security context as
   * recomputed here.
   *
   * @return the list of protection domains (i.e. bundle name/domain location) in the security
   *     context as recomputed here
   */
  @VisibleForTesting
  List<String> getComputedDomains() {
    return domains;
  }

  /**
   * Gets the index in the recomputed domains list where the reported security exception is to be
   * generated for.
   *
   * @return the index for the failed domain or <code>-1</code> if no failures exist
   */
  @VisibleForTesting
  int getFailedDomainIndex() {
    return failedDomainIndex;
  }

  /**
   * Gets the index in the recomputed domains list where we estimate the security manager started
   * combining additional domains not coming from the stack. Below this mark are pure domains
   * extracted from the current stack). At this mark and above are domains that were combined.
   *
   * @return the starting index for combined domains or <code>-1</code> if no combined domains were
   *     present
   */
  @VisibleForTesting
  int getCombinedDomainsStartIndex() {
    return combinedDomainsStartIndex;
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
   * <p>We shall also check the stack and the failed permission against all acceptable patterns and
   * if one matches, we will skip mark it as acceptable.
   *
   * @return <code>true</code> if all granted domains were required; <code>false</code> if we didn't
   *     need all of them which would mean this is an invalid option as we are granting more than we
   *     need
   */
  @SuppressWarnings("squid:CommentedOutCodeLine" /* no commented out code here */)
  private boolean recompute() {
    domains.clear();
    this.failedStackIndex = -1;
    this.privilegedStackIndex = -1;
    this.failedDomainIndex = -1;
    this.combinedDomainsStartIndex = -1;
    final Set<String> grantedDomains = new HashSet<>(this.grantedDomains);
    final boolean foundFailedDomain = recomputeFromStack(grantedDomains);

    if (doPrivileged.isEmpty()) {
      // make sure we account for all inherited/combined domains in the access control context. In
      // case where a combiner was used, it is possible that additional domains from an inherited
      // access control context be added to the list that the access controller is checking against.
      // If we have more than we need to remember as there will be no stack lines that will
      // correspond to these domains
      recomputeFromContext(grantedDomains, foundFailedDomain);
    } // else
    //     if we are extending privileges for a solution then we are modifying the stack and the
    //     access control context that would now result from the same exception we are trying to
    //     recompute. because we are extending the stack before any places where we were combining
    //     domains there is no way combined domains would leak into this solution
    //     we are guaranteed that we are doing this before because:
    //     1- we cannot analyze stack from combined domains as those are not from a stack execution;
    //        hence we cannot propose to extend privileges there
    //     2- to combined domains, one must extend privileges via the AccessController which means
    //        this would already be the last line in the stack we computed before and as such, the
    //        privileged block we are proposing would be before that particular line any way
    return grantedDomains.isEmpty();
  }

  private int findNextToFindNotAlreadyFound(int lastNextToFind, List<String> contextDomains) {
    // increase 'nextToFind' to find the next one we didn't already find
    final int size = contextDomains.size();
    int nextToFind = lastNextToFind;

    while (++nextToFind < size) {
      final String nextDomainToFind = contextDomains.get(nextToFind);

      if (nextDomainToFind == null) {
        // the boot domain/bundle-0 implicitly comes before any other domains so it is "implicitly"
        // already found so skip it
      } else if (contextDomains.indexOf(nextDomainToFind) > lastNextToFind) {
        break;
      }
    }
    return nextToFind;
  }

  private int getNextContextDomainIndexNotComputedFromStack(List<String> contextDomains) {
    // at this point, all domains we have in 'domains' are computed from the stack and should
    // correspond to the same in order we have in the context
    // 'context.domains' have duplicates and possibly null at the top (and elsewhere) whereas
    // 'domains' doesn't have nulls and doesn't have duplicates
    final int size = contextDomains.size();
    // skip null at the top since those are not added to 'domains'
    int nextToFind = findNextToFindNotAlreadyFound(0, contextDomains);

    for (int i = 0; i < domains.size(); i++) {
      final String domain = domains.get(i);
      // the computed domain should be found at or before 'next' otherwise, we just computed a
      // domain from a frame that doesn't match a domain we got from the access control context
      final int index = contextDomains.indexOf(domain);

      if (index == -1) {
        // this means that we computed a domain from the stack that we cannot find in the
        // access control context. this is a bug and we need to figure out what we missed
        dumpTroubleshootingInfo(
            "AN ERROR OCCURRED WHILE ATTEMPTING TO ANALYZE THE SECURITY EXCEPTION,",
            "A DOMAIN WE COMPUTED FROM THE STACK (INDEX: " + (i + 1) + ") CANNOT BE FOUND IN THE",
            "CURRENT ACCESS CONTROL CONTEXT (STARTING AT INDEX: " + nextToFind + ")");
        throw new InternalError(
            "unable to find a domain computed from the stack in the access control context: "
                + domain);
      } else if (index < nextToFind) { // we already found that domain so continue
      } else if (index == nextToFind) { // we found the next domain we were looking for
        nextToFind = findNextToFindNotAlreadyFound(nextToFind, contextDomains);
      } else {
        // this means that the next domain in the access control context we need to find in our
        // computed domain list cannot be found. this is a bug and we need to figure out what we
        // missed
        dumpTroubleshootingInfo(
            "AN ERROR OCCURRED WHILE ATTEMPTING TO ANALYZE THE SECURITY EXCEPTION,",
            "A DOMAIN IN THE CURRENT ACCESS CONTROL CONTEXT (INDEX: " + nextToFind + ") CANNOT",
            "BE CORRELATED TO ONE COMPUTED FROM THE STACK (INDEX: " + (i + 1) + ")");
        throw new InternalError(
            "unable to correlate a domain in the access control context with those computed from the stack : "
                + domain);
      }
    }
    return (nextToFind < size) ? nextToFind : -1;
  }

  private void recomputeFromContext(Set<String> grantedDomains, boolean foundFailedDomain) {
    final List<String> contextDomains = context.getDomains();
    final int nextToFind = getNextContextDomainIndexNotComputedFromStack(contextDomains);

    if (nextToFind != -1) {
      // at this point, all domains starting at 'nextToFind' in the context are deemed combined and
      // should be considered part of the context
      this.combinedDomainsStartIndex = domains.size();
      for (int i = nextToFind; i < contextDomains.size(); i++) {
        final String domain = contextDomains.get(i);

        domains.add(domain);
        if (!foundFailedDomain) {
          if (!context.isPrivileged(domain)) { // found the place it will fail!!!!
            foundFailedDomain = true;
            this.failedDomainIndex = domains.size() - 1;
          } else {
            // keep track of the fact that this granted domain helped if it was one
            // that we artificially granted the permission to
            grantedDomains.remove(domain);
          }
        }
      }
    }
  }

  private void recomputeAcceptablePattern(
      List<Pattern> stackPatterns, StackFrameInformation frame, int index) {
    if (!isAcceptable()) {
      final String location = frame.getLocation();

      this.acceptablePattern =
          stackPatterns
              .stream()
              .filter(p -> p.matchLocations(index, location))
              .filter(Pattern::wasAllMatched)
              .findFirst()
              .orElse(null);
    }
  }

  @SuppressWarnings("squid:S1066" /* keeping ifs separate actually increase readability here */)
  private int reduceLastFrameToCheckIfDoPrivilegedBlock(
      final StackFrameInformation frame, int index, int last) {
    if (frame.isDoPrivilegedBlock()) {
      int increment = 1;

      // note: there cannot be a call to doPrivileged() without another frame following that
      // as such, doing a blind (index+increment) is safe and will never exceed stack.size()
      if (stack.get(index + 1).isCallingDoPrivilegedBlockOnBehalfOfCaller()) {
        // we check if the frame following the call to doPrivileged() is calling it on behalf of its
        // own caller. this is a special case to handle situations like
        // javax.security.auth.Subject:422
        // we therefore advance the index one more since we want to account for the its caller as
        // part of the stack break
        // note: there cannot be a call to one those special cases without another frame following
        // that
        // as such, doing a blind increment++ is safe and will never exceed stack.size()
        increment++;
      }
      // found a stack break that we care about, we have to stop after including the next frame
      // as part of the stack since it is the one calling doPrivileged()
      if (privilegedStackIndex == -1) {
        this.privilegedStackIndex = index + increment;
      }
      return index + increment + 1; // stop after next
    }
    return last;
  }

  private boolean recomputeFromStack(Set<String> grantedDomains) {
    final List<Pattern> stackPatterns =
        SecurityCheckInformation.ACCEPTABLE_PATTERNS
            .stream()
            .filter(p -> p.matchAllPermissions(permissionInfos))
            .map(Pattern::new)
            .collect(Collectors.toList());
    boolean foundFailedDomain = false;
    int last = stack.size();

    for (int i = 0; i < last; i++) {
      final StackFrameInformation frame = stack.get(i);

      last = reduceLastFrameToCheckIfDoPrivilegedBlock(frame, i, last);
      recomputeAcceptablePattern(stackPatterns, frame, i);
      final String domain = frame.getDomain();

      if ((domain != null) && !domains.contains(domain)) {
        domains.add(domain);
      }
      if (!foundFailedDomain) {
        if (!frame.isPrivileged(
            context.getPrivilegedDomains())) { // found the place where it failed!
          foundFailedDomain = true;
          this.failedStackIndex = i;
          this.failedDomainIndex = domains.indexOf(domain);
        } else {
          // keep track of the fact that this granted domain helped if it was one
          // that we artificially granted the permission to
          grantedDomains.remove(frame.getDomain());
        }
      }
    }
    return foundFailedDomain;
  }

  private List<SecuritySolution> analyze0() {
    List<SecuritySolution> solutions = analysis;

    if (solutions == null) {
      if (invalid) { // if this is not a valid solution then the analysis should be empty
        solutions = Collections.emptyList();
        this.analysis = solutions;
      } else if (((failedStackIndex == -1) && (failedDomainIndex == -1)) || isAcceptable()) {
        // no issues here (i.e. good solution) or acceptable security exception so return self
        solutions = Collections.singletonList(this);
        this.analysis = Collections.emptyList();
      } else {
        solutions = new ArrayList<>();
        // first see what happens if we grant the missing permission to the failed domain
        solutions.addAll(new SecurityCheckInformation(this).analyze0());
        if (debug.canDoPrivilegedBlocks()) {
          analyzeDoPrivilegedBlocks(solutions);
        }
        Collections.sort(solutions); // sort the result
        this.analysis = solutions;
      }
    }
    return solutions;
  }

  private void analyzeDoPrivilegedBlocks(List<SecuritySolution> solutions) {
    // now check if we could extend the privileges of a domain that comes up
    // before which already has the permission
    for (int i = failedStackIndex - 1; i >= 0; i--) {
      final StackFrameInformation frame = stack.get(i);

      if (frame.isPrivileged(context.getPrivilegedDomains())
          && frame.canDoPrivilegedBlocks(debug)) {
        solutions.addAll(new SecurityCheckInformation(this, i).analyze0());
      }
    }
  }

  @SuppressWarnings("squid:S106" /* this is a console application */)
  private void dumpPermission() {
    if (isAcceptable()) {
      System.out.println(ACDebugger.PREFIX + "Acceptable permissions:");
      System.out.println(ACDebugger.PREFIX + "    " + getAcceptablePermissions());
    } else {
      final String s = (permissionInfos.size() == 1) ? "" : "s";

      System.out.println(ACDebugger.PREFIX + "Permission" + s + ":");
      permissionInfos.forEach(p -> System.out.println(ACDebugger.PREFIX + "    " + p));
    }
  }

  @SuppressWarnings("squid:S106" /* this is a console application */)
  private void dumpHowToFix(boolean osgi) {
    if (!grantedDomains.isEmpty()) {
      final String ds = (grantedDomains.size() == 1) ? "" : "s";
      final String ps = (permissionInfos.size() == 1) ? "" : "s";

      System.out.println(
          ACDebugger.PREFIX
              + "Granting permission"
              + ps
              + " to "
              + (osgi ? "bundle" : "domain")
              + ds
              + ":");
      grantedDomains.forEach(d -> System.out.println(ACDebugger.PREFIX + "    " + d));
    }
    if (!doPrivileged.isEmpty()) {
      System.out.println(ACDebugger.PREFIX + "Extending privileges at:");
      doPrivileged.forEach(f -> System.out.println(ACDebugger.PREFIX + "    " + f));
    }
  }

  @SuppressWarnings("squid:S106" /* this is a console application */)
  private void dumpContext(boolean osgi) {
    System.out.println(ACDebugger.PREFIX + "Context:");
    System.out.println(
        ACDebugger.PREFIX
            + "     "
            + (osgi ? StackFrameInformation.BUNDLE0 : StackFrameInformation.BOOT_DOMAIN));
    for (int i = 0; i < domains.size(); i++) {
      final String domain = domains.get(i);

      System.out.println(
          ACDebugger.PREFIX
              + " "
              + ((i == failedDomainIndex) ? "--> " : "    ")
              + (context.isPrivileged(domain) ? "" : "*")
              + domain
              + (((i >= combinedDomainsStartIndex) && (combinedDomainsStartIndex != -1))
                  ? " (combined)"
                  : ""));
    }
  }

  @SuppressWarnings("squid:S106" /* this is a console application */)
  private void dumpStack(boolean osgi) {
    System.out.println(ACDebugger.PREFIX + "Stack:");
    final int size = stack.size();

    for (int i = 0; i < size; i++) {
      System.out.println(
          ACDebugger.PREFIX
              + " "
              + ((i == failedStackIndex) ? "-->" : "   ")
              + " at "
              + (isAcceptable() && acceptablePattern.wasMatched(i) ? "#" : "")
              + stack.get(i).toString(osgi, context.getPrivilegedDomains()));
      if ((privilegedStackIndex != -1) && (i == privilegedStackIndex)) {
        System.out.println(
            ACDebugger.PREFIX + "    ----------------------------------------------------------");
      }
    }
  }

  private void dump0(boolean osgi) {
    dumpPermission();
    dumpHowToFix(osgi);
    dumpContext(osgi);
    dumpStack(osgi);
  }

  @SuppressWarnings("squid:S106" /* this is a console application */)
  private void dumpTroubleshootingInfo(String... msg) {
    System.err.println(ACDebugger.PREFIX);
    System.err.println(ACDebugger.PREFIX + SecurityCheckInformation.DOUBLE_LINES);
    Stream.of(msg).map(ACDebugger.PREFIX::concat).forEach(System.err::println);
    System.err.println(
        ACDebugger.PREFIX
            + "PLEASE REPORT AN ISSUE WITH THE FOLLOWING INFORMATION AND INSTRUCTIONS");
    System.err.println(ACDebugger.PREFIX + "ON HOW TO REPRODUCE IT");
    System.err.println(ACDebugger.PREFIX + SecurityCheckInformation.DOUBLE_LINES);
    System.err.println(
        ACDebugger.PREFIX + "PERMISSION" + ((permissionInfos.size() == 1) ? ":" : "S:"));
    permissionInfos.forEach(p -> System.err.println(ACDebugger.PREFIX + "    " + p));
    if (isAcceptable()) {
      System.err.println(ACDebugger.PREFIX + "ACCEPTABLE PERMISSIONS: ");
      System.err.println(ACDebugger.PREFIX + "    " + getAcceptablePermissions());
    }
    context.dumpTroubleshootingInfo(debug.isOSGi());
    System.err.println(ACDebugger.PREFIX + "COMPUTED CONTEXT:");
    System.err.println(
        ACDebugger.PREFIX
            + "  "
            + (debug.isOSGi() ? StackFrameInformation.BUNDLE0 : StackFrameInformation.BOOT_DOMAIN));
    for (int i = 0; i < domains.size(); i++) {
      final String domain = domains.get(i);

      System.err.println(
          ACDebugger.PREFIX
              + "  "
              + (context.isPrivileged(domain) ? "" : "*")
              + domain
              + (((i >= combinedDomainsStartIndex) && (combinedDomainsStartIndex != -1))
                  ? " (combined)"
                  : ""));
    }
    System.err.println(ACDebugger.PREFIX + "STACK:");
    final int size = stack.size();

    for (int i = 0; i < size; i++) {
      System.err.println(
          ACDebugger.PREFIX
              + "  at "
              + stack.get(i).toString(debug.isOSGi(), context.getPrivilegedDomains()));
      if ((privilegedStackIndex != -1) && (i == privilegedStackIndex)) {
        System.err.println(
            ACDebugger.PREFIX + "    ----------------------------------------------------------");
      }
    }
    System.err.println(ACDebugger.PREFIX + SecurityCheckInformation.DOUBLE_LINES);
  }

  /** Pattern class for matching specific permission and stack information. */
  private static class Pattern {
    private final java.util.regex.Pattern permissionPattern;
    private final List<java.util.regex.Pattern> stackPatterns;
    private final List<Integer> stackIndexes;

    private Pattern(String permissionPattern) {
      this.permissionPattern = java.util.regex.Pattern.compile(permissionPattern);
      this.stackPatterns = new ArrayList<>(8);
      this.stackIndexes = null;
    }

    public Pattern(Pattern pattern) {
      this.permissionPattern = pattern.permissionPattern;
      this.stackPatterns = new ArrayList<>(pattern.stackPatterns);
      this.stackIndexes = new ArrayList<>(stackPatterns.size());
    }

    private void addStack(String stackPattern) {
      stackPatterns.add(java.util.regex.Pattern.compile(stackPattern));
    }

    @SuppressWarnings("squid:S00112" /* Forced to by the Java debugger API */)
    private void validate() {
      if (stackPatterns.isEmpty()) {
        throw new Error(
            "missing stack frame information for [" + permissionPattern.pattern() + "]");
      }
    }

    public String getPermissionInfos() {
      return permissionPattern.pattern();
    }

    public boolean matchAllPermissions(Set<String> permissionInfos) {
      return permissionInfos.stream().map(permissionPattern::matcher).allMatch(Matcher::matches);
    }

    public boolean matchLocations(int index, String location) {
      if (!stackPatterns.isEmpty() && stackPatterns.get(0).matcher(location).matches()) {
        stackPatterns.remove(0);
        stackIndexes.add(index);
        return true;
      }
      return false;
    }

    public boolean wasAllMatched() {
      return stackPatterns.isEmpty();
    }

    public boolean wasMatched(int index) {
      return stackIndexes.contains(index);
    }
  }

  /** Line processors for returning a list of patterns while trimming and ignoring comment lines. */
  private static class PatternProcessor implements LineProcessor<List<Pattern>> {
    private final List<Pattern> result = new ArrayList<>();
    private Pattern current = null;

    @Override
    public boolean processLine(String line) throws IOException {
      final String trimmed = line.trim();

      if (trimmed.startsWith("#")) { // nothing to do, just skip that line and continues
      } else if (trimmed.isEmpty()) {
        if (current != null) {
          current.validate();
          this.current = null;
        }
      } else if (current == null) {
        this.current = new Pattern(trimmed);
        result.add(current);
      } else {
        current.addStack(trimmed);
      }
      return true;
    }

    @Override
    public List<Pattern> getResult() {
      if (current != null) {
        current.validate();
        this.current = null;
      }
      return Collections.unmodifiableList(result);
    }
  }
}
