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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.codice.acdebugger.ACDebugger;
import org.codice.acdebugger.api.SecurityFailure;
import org.codice.acdebugger.api.SecuritySolution;
import org.codice.acdebugger.api.StackFrameInformation;

/**
 * This class serves 2 purposes. It is first a representation of a detected security service access
 * failure and also the solution itself.
 */
class SecurityServicePermissionImpliedInformation extends SecuritySolution
    implements SecurityFailure {
  /** The name of the bundle which doesn't have the corresponding service permission. */
  private final String bundle;

  /**
   * Creates a new service permission failure.
   *
   * @param bundle the bundle which doesn't have the corresponding service permission
   * @param permissionInfos the string representations of the missing service permission
   */
  SecurityServicePermissionImpliedInformation(String bundle, Set<String> permissionInfos) {
    super(permissionInfos, Collections.singleton(bundle), Collections.emptyList());
    this.bundle = bundle;
  }

  @Override
  public List<StackFrameInformation> getStack() {
    return Collections.emptyList();
  }

  @Override
  public boolean isAcceptable() {
    return false; // never acceptable
  }

  @Nullable
  @Override
  public String getAcceptablePermissions() {
    return null; // never acceptable
  }

  @Override
  public List<SecuritySolution> analyze() {
    return Collections.singletonList(this);
  }

  @SuppressWarnings("squid:S106" /* this is a console application */)
  @Override
  public void dump(boolean osgi, String prefix) {
    final String first = prefix + "IMPLIED PERMISSION FAILURE";

    System.out.println(ACDebugger.PREFIX);
    System.out.println(ACDebugger.PREFIX + first);
    System.out.println(
        ACDebugger.PREFIX
            + IntStream.range(0, first.length())
                .mapToObj(i -> "=")
                .collect(Collectors.joining("")));
    final String s = (permissionInfos.size() == 1) ? "" : "s";

    System.out.println(ACDebugger.PREFIX + "Permission" + s + ":");
    permissionInfos.forEach(p -> System.out.println(ACDebugger.PREFIX + "    " + p));
    System.out.println(ACDebugger.PREFIX + "Granting permission" + s + " to bundle:");
    System.out.println(ACDebugger.PREFIX + "    " + bundle);
    System.out.println(ACDebugger.PREFIX);
    System.out.println(ACDebugger.PREFIX + "SOLUTIONS");
    System.out.println(ACDebugger.PREFIX + "---------");
    print(osgi);
  }

  @Override
  public String toString() {
    if (permissionInfos.size() == 1) {
      return "Implied permission failure for " + bundle + ": " + permissionInfos.iterator().next();
    }
    return "Implied permissions failure for " + bundle + ": " + permissionInfos;
  }
}
