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

import com.google.common.base.Charsets;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import java.io.IOError;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/** Information about a particular frame in a calling stack. */
@SuppressWarnings("squid:S1191" /* Using the Java debugger API */)
public class StackFrameInformation implements Comparable<StackFrameInformation> {
  /** Constant for how to represent bundle-0 to users. */
  public static final String BUNDLE0 = "bundle-0";

  /**
   * Fake stack frame information for a <code>doPrivileged()</code> call to the access controller.
   */
  public static final StackFrameInformation DO_PRIVILEGED =
      new StackFrameInformation(
          null,
          "java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)");

  /**
   * Sets of 3rd party prefixes for bundles and/or package names. These are used as indicator of
   * where we are shouldn't provide solutions that consists in extending privileges using <code>
   * doPrivileged()</code> blocks.
   */
  private static final Set<String> THIRD_PARTY_PREFIXES;

  /**
   * Sets of signatures for proxy classes. These are used as indicator of * where we are shouldn't
   * provide solutions that consists in extending privileges using <code>doPrivileged()</code>
   * blocks.
   */
  private static final Set<String> PROXIES;

  static {
    try {
      THIRD_PARTY_PREFIXES =
          Resources.readLines(
              Resources.getResource("thirdparty-prefixes.txt"), Charsets.UTF_8, new SetProcessor());
      PROXIES =
          Resources.readLines(
              Resources.getResource("proxies.txt"), Charsets.UTF_8, new SetProcessor());
    } catch (IOException e) {
      throw new IOError(e);
    }
  }

  /**
   * The bundle corresponding to this stack frame or <code>null</code> if it corresponds to <code>
   * bundle-0</code>.
   */
  private final String bundle;

  /** String representation of the location in the code for this stack frame. */
  private final String location;

  /** Reference to the <code>this</code> at that location. */
  @Nullable private final ObjectReference thisObject;

  private StackFrameInformation(String bundle, String location, ObjectReference thisObject) {
    this.bundle = bundle;
    this.location = location;
    this.thisObject = thisObject;
  }

  /**
   * Constructs a stack frame location.
   *
   * <p><i>Note:</i> This flavor of the constructor provides no reference to the <code>this</code>
   * object
   *
   * @param bundle the bundle corresponding to that location or <code>null</code> if it is <code>
   *     bundle-0</code>
   * @param location the string representation of the location
   */
  public StackFrameInformation(@Nullable String bundle, String location) {
    this(bundle, location, null);
  }

  /**
   * Constructs a stack frame location.
   *
   * <p><i>Note:</i> This flavor of the constructor provides no reference to the <code>this</code>
   * object
   *
   * @param bundle the bundle corresponding to that location or <code>null</code> if it is <code>
   *     bundle-0</code>
   * @param location the location
   */
  public StackFrameInformation(@Nullable String bundle, Location location) {
    this(bundle, location.toString(), null);
  }

  /**
   * Constructs a stack frame location.
   *
   * @param bundle the bundle corresponding to that location or <code>null</code> if it is <code>
   *     bundle-0</code>
   * @param location the string representation of the location
   * @param thisObject the reference to the <code>this</code> object at that location or <code>null
   *     </code> if unknown
   */
  public StackFrameInformation(
      @Nullable String bundle, Location location, @Nullable ObjectReference thisObject) {
    this(bundle, location.toString(), thisObject);
  }

  /**
   * Gets the name of the bundle corresponding to this stack frame or <code>null</code> if it
   * corresponds to <code>bundle-0</code>.
   *
   * @return the name of the bundle at that location or <code>null</code> if it corresponds to
   *     <code>bundle-0</code>
   */
  @Nullable
  public String getBundle() {
    return bundle;
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
    return !(isThirdPartyBundle() || isThirdPartyClass() || isProxyClass(debug));
  }

  /**
   * Checks if this location corresponds to a <code>doPrivileged()</code> block in the code.
   *
   * @return <code>true</code> if this location corresponds to a <code>doPrivileged()</code> block;
   *     <code>false</code> if not
   */
  public boolean isDoPrivilegedBlock() {
    return ((bundle == null) && location.startsWith("java.security.AccessController.doPrivileged"));
  }

  /**
   * Checks if this location has permissions based on the specified set of privileged bundles.
   *
   * @param privilegedBundles the set of privileged bundles to checked against
   * @return <code>true</code> if this location corresponds to <code>bundle-0</code> or if it's
   *     corresponding bundle is included in the provided set of privileged bundles or if <code>
   *     privilegedBundles</code> is <code>null</code>; <code>false</code> otherwise
   */
  public boolean isPrivileged(@Nullable Set<String> privilegedBundles) {
    if (privilegedBundles == null) {
      return true;
    }
    // bundle=0 always has all permissions
    return (bundle == null) || privilegedBundles.contains(bundle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bundle, location);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof StackFrameInformation) {
      final StackFrameInformation f = (StackFrameInformation) obj;

      return Objects.equals(bundle, f.bundle) && Objects.equals(location, f.location);
    }
    return false;
  }

  @Override
  public int compareTo(StackFrameInformation f) {
    if (f == this) {
      return 0;
    }
    if (bundle != f.bundle) {
      final int d = (bundle == null) ? -1 : bundle.compareTo(f.bundle);

      if (d != 0) {
        return d;
      }
    }
    return location.compareTo(f.location);
  }

  /**
   * Provides a string representation for this location based on the given set of privileged
   * bundles.
   *
   * <p>The returned string will be prefixed with <code>*</code> if the corresponding bundle doesn't
   * have privileges.
   *
   * @param privilegedBundles the set of privileged bundles to checked against
   * @return a corresponding string representation with prefixed privileged information
   */
  public String toString(@Nullable Set<String> privilegedBundles) {
    final String p = (isPrivileged(privilegedBundles) ? "" : "*");

    if (bundle != null) {
      return p + bundle + "(" + location + ")";
    } else {
      return p + StackFrameInformation.BUNDLE0 + "(" + location + ")";
    }
  }

  @Override
  public String toString() {
    return toString(null);
  }

  private boolean isThirdPartyBundle() {
    return (bundle == null)
        || StackFrameInformation.THIRD_PARTY_PREFIXES.stream().anyMatch(bundle::startsWith);
  }

  private boolean isThirdPartyClass() {
    return (location != null)
        && StackFrameInformation.THIRD_PARTY_PREFIXES.stream().anyMatch(location::startsWith);
  }

  private boolean isProxyClass(Debug debug) {
    return StackFrameInformation.PROXIES
        .stream()
        .anyMatch(p -> debug.reflection().isInstance(p, thisObject));
  }

  /** Line processors for returning set of strings while trimming and ignoring comment lines. */
  private static class SetProcessor implements LineProcessor<Set<String>> {
    private final Set<String> result = new HashSet<>();

    @Override
    public boolean processLine(String line) throws IOException {
      final String trimmed = line.trim();

      if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
        result.add(trimmed);
      }
      return true;
    }

    @Override
    public Set<String> getResult() {
      return Collections.unmodifiableSet(result);
    }
  }
}
