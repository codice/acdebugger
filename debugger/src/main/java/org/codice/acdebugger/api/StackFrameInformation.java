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

import com.sun.jdi.Location; // NOSONAR
import com.sun.jdi.ObjectReference; // NOSONAR
import com.sun.jdi.ReferenceType; // NOSONAR
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.codice.acdebugger.common.Resources;

/** Information about a particular frame in a calling stack. */
public class StackFrameInformation implements Comparable<StackFrameInformation> {
  /** Constant for how to represent bundle 0 to users. */
  public static final String BUNDLE0 = "bundle-0";

  /** Constant for how to represent the boot domain location to users. */
  public static final String BOOT_DOMAIN = "boot:";

  /**
   * Fake stack frame information for a <code>doPrivileged()</code> call to the access controller.
   */
  public static final StackFrameInformation DO_PRIVILEGED =
      new StackFrameInformation(
          null,
          "java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)",
          null,
          "java.security.AccessController",
          null);

  /**
   * Set of 3rd party prefixes for bundles and/or package names. These are used as indicator of
   * where we are shouldn't provide solutions that consists in extending privileges using <code>
   * doPrivileged()</code> blocks.
   */
  private static final List<String> THIRD_PARTY_PREFIXES =
      Resources.readLines(StackFrameInformation.class, "thirdparty-prefixes.txt");

  /**
   * Set of 3rd party patterns for domain locations. These are used as indicator of where we are
   * shouldn't provide solutions that consists in extending privileges using <code>
   * doPrivileged()</code> blocks.
   */
  private static final List<Pattern> THIRD_PARTY_PATTERNS =
      Resources.readLines(StackFrameInformation.class, "thirdparty-patterns.txt", Pattern::compile);

  /**
   * Set of signatures for proxy classes. These are used as indicator of * where we are shouldn't
   * provide solutions that consists in extending privileges using <code>doPrivileged()</code>
   * blocks.
   */
  private static final List<String> PROXIES =
      Resources.readLines(StackFrameInformation.class, "proxies.txt");

  /**
   * The bundle name or domain location corresponding to this stack frame or <code>null</code> if it
   * corresponds to <code>bundle-0</code> or the boot domain or if unknown.
   */
  @Nullable private final String domain;

  /** String representation of the location in the code for this stack frame. */
  private final String location;

  /** Class at above location. */
  @Nullable private final ReferenceType locationClass;

  /** Class name at above location. */
  private final String locationClassName;

  /**
   * Reference to the <code>this</code> at that location or <code>null</code> if it is a static
   * method call.
   */
  @Nullable private final ObjectReference thisObject;

  /** Class (if a static method call) or instance (if not) at the corresponding location. */
  private final String classOrInstanceAtLocation;

  private StackFrameInformation(
      String domain,
      String location,
      @Nullable ReferenceType locationClass,
      String locationClassName,
      @Nullable ObjectReference thisObject,
      String classOrInstanceAtLocation) {
    this.domain = domain;
    this.location = location;
    this.locationClass = locationClass;
    this.locationClassName = locationClassName;
    this.thisObject = thisObject;
    this.classOrInstanceAtLocation = classOrInstanceAtLocation;
  }

  private StackFrameInformation(
      String domain,
      String location,
      @Nullable ReferenceType locationClass,
      String locationClassName,
      @Nullable ObjectReference thisObject) {
    this(
        domain,
        location,
        locationClass,
        locationClassName,
        thisObject,
        (thisObject != null) ? thisObject.toString() : "class of " + locationClassName);
  }

  private StackFrameInformation(
      String domain,
      String location,
      ReferenceType locationClass,
      @Nullable ObjectReference thisObject) {
    this(domain, location, locationClass, locationClass.name(), thisObject);
  }

  /**
   * Constructs a stack frame location.
   *
   * @param domain the bundle name or domain location corresponding to that location or <code>null
   *     </code> if it is <code>bundle-0</code> or boot domain or if unknown
   * @param location the string representation of the location
   * @param thisObject the reference to the <code>this</code> object at that location or <code>null
   *     </code> if unknown
   */
  public StackFrameInformation(
      @Nullable String domain, Location location, @Nullable ObjectReference thisObject) {
    this(domain, location.toString(), location.declaringType(), thisObject);
  }

  /**
   * Gets the name of the bundle or the domain location corresponding to this stack frame or <code>
   * null</code> if it corresponds to <code>bundle-0</code> or the boot domain or if unknown.
   *
   * @return the name of the bundle or domain location at that location or <code>null</code> if it
   *     corresponds to <code>bundle-0</code> or the boot domain or if unknown
   */
  @Nullable
  public String getDomain() {
    return domain;
  }

  /**
   * Gets a string representation of the location in the code for this stack frame.
   *
   * @return a string representation of the location in the code for this stack frame
   */
  public String getLocation() {
    return location;
  }

  /**
   * Checks if we can put a <code>doPrivileged()</code> block at this location in the code.
   *
   * @param debug the current debug session
   * @return <code>true</code> if the code could be modified to include a <code>doPrivileged()
   *     </code> block at this location; <code>false</code> if it cannot
   */
  public boolean canDoPrivilegedBlocks(Debug debug) {
    return !(isThirdPartyDomain(debug) || isThirdPartyClass() || isProxyClass(debug));
  }

  /**
   * Checks if this location corresponds to a <code>doPrivileged()</code> block in the code.
   *
   * @return <code>true</code> if this location corresponds to a <code>doPrivileged()</code> block;
   *     <code>false</code> if not
   */
  public boolean isDoPrivilegedBlock() {
    return ((domain == null) && location.startsWith("java.security.AccessController.doPrivileged"));
  }

  /**
   * Checks if this location corresponds to a code known to perform a <code>doPrivileged()</code>
   * block in the code on behalf of its caller by re-arranging the access control context.
   *
   * <p><i>Note:</i> These locations are confirmed to get the current context using <code>
   * AccessController.getContext()</code> and pass it along as is directly to a <code>
   * AccessController.doPrivileged()</code> method as opposed to getting it from somewhere else. The
   * difference is that the context being passed in is from the stack at that point and not from
   * something unrelated and as such, we can treat it as being part of the stack dcontext of domains
   * instead of combined domains.
   *
   * @return <code>true</code> if this location corresponds to a <code>doPrivileged()</code> block;
   *     <code>false</code> if not
   */
  public boolean isCallingDoPrivilegedBlockOnBehalfOfCaller() {
    return ((domain == null) && location.equals("javax.security.auth.Subject:422"));
  }

  /**
   * Checks if this location has permissions based on the specified set of privileged bundles.
   *
   * @param privilegedDomains the set of privileged bundle names or domain locations to checked
   *     against
   * @return <code>true</code> if this location corresponds to <code>bundle-0</code> or the boot
   *     domain or is unknown or if it's corresponding bundle name or domain location is included in
   *     the provided set of privileged domains or if <code>privilegedDomains</code> is <code>null
   *     </code>; <code>false</code> otherwise
   */
  public boolean isPrivileged(@Nullable Set<String> privilegedDomains) {
    if (privilegedDomains == null) {
      return true;
    }
    // bundle=0/boot domain always has all permissions
    return (domain == null) || privilegedDomains.contains(domain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domain, location);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof StackFrameInformation) {
      final StackFrameInformation f = (StackFrameInformation) obj;

      return Objects.equals(domain, f.domain) && Objects.equals(location, f.location);
    }
    return false;
  }

  @Override
  public int compareTo(StackFrameInformation f) {
    if (f == this) {
      return 0;
    }
    if (domain != f.domain) {
      final int d = (domain == null) ? -1 : domain.compareTo(f.domain);

      if (d != 0) {
        return d;
      }
    }
    return location.compareTo(f.location);
  }

  /**
   * Provides a string representation for this location based on the given set of privileged
   * domains.
   *
   * <p>The returned string will be prefixed with <code>*</code> if the corresponding domain doesn't
   * have privileges.
   *
   * @param osgi <code>true</code> if debugging an OSGi container; <code>false</code> otherwise
   * @param privilegedDomains the set of privileged bundle names or domain locations to checked
   *     against
   * @return a corresponding string representation with prefixed privileged information
   */
  public String toString(boolean osgi, @Nullable Set<String> privilegedDomains) {
    final String p = (isPrivileged(privilegedDomains) ? "" : "*");
    final String d;

    if (domain != null) {
      d = domain;
    } else {
      d = osgi ? StackFrameInformation.BUNDLE0 : StackFrameInformation.BOOT_DOMAIN;
    }
    return p + d + "(" + location + ") <" + classOrInstanceAtLocation + '>';
  }

  @Override
  public String toString() {
    return toString(false, null);
  }

  private boolean isThirdPartyDomain(Debug debug) {
    if (domain == null) {
      return true;
    } else if (debug.isOSGi()) {
      return StackFrameInformation.THIRD_PARTY_PREFIXES.stream().anyMatch(domain::startsWith);
    }
    return StackFrameInformation.THIRD_PARTY_PATTERNS
        .stream()
        .map(p -> p.matcher(domain))
        .anyMatch(Matcher::matches);
  }

  private boolean isThirdPartyClass() {
    // check the class at the location (i.e. the source class) as opposed to the instance class
    // (i.e. thisObject) as the later could be a non-3rd party class extending a 3rd-party class and
    // this stack frame location could be located inside that 3rd party base class
    return StackFrameInformation.THIRD_PARTY_PREFIXES
        .stream()
        .anyMatch(locationClassName::startsWith);
  }

  private boolean isProxyClass(Debug debug) {
    final ReflectionUtil reflection = debug.reflection();

    if ((thisObject != null)
        && StackFrameInformation.PROXIES
            .stream()
            .anyMatch(p -> reflection.isInstance(p, thisObject))) {
      return true;
    }
    if (locationClass != null) {
      // although unlikely that a normal class would extend a proxy generated class, let's still
      // check for it
      return StackFrameInformation.PROXIES
          .stream()
          .anyMatch(p -> reflection.isAssignableFrom(p, locationClass));
    }
    return false;
  }
}
