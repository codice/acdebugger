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
package org.codice.acdebugger;

/** Service used for dynamically granting permissions. */
public interface PermissionService {
  /**
   * Grants the specified permission to the specified bundle.
   *
   * @param bundle the bundle name/location to whom the permission should be granted
   * @param permission the permission to be granted in a standard policy format string
   *     representation
   * @throws Exception if a failure occurs while granting the given permission
   */
  @SuppressWarnings("squid:S00112" /* Interface used during debugging and meant to be generic */)
  public void grantPermission(String bundle, String permission) throws Exception;
}
