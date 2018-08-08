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

import java.util.List;
import java.util.Set;

/** Represents a security failure detected in the attached VM. */
public interface SecurityFailure {
  /**
   * Gets the permissions associated with this security failure.
   *
   * @return the permissions associated with this security failure
   */
  public Set<String> getPermissions();

  /**
   * Analyze this security failure in order to find solutions.
   *
   * @return a list of possible solutions ordered based on priority (returns an empty list if there
   *     is no possible solutions or if this security failure shows no issues)
   */
  public List<SecuritySolution> analyze();

  /**
   * Dumps info about this security failure along with its solutions.
   *
   * @param prefix a prefix string to dump in fron of the first line
   */
  public void dump(String prefix);
}
