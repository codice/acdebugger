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
package org.codice.acdebugger.common;

import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.ProtectionDomain;
import java.util.Objects;
import javax.annotation.Nullable;

/** Defines a Json object to represent domain information. */
public class DomainInfo {
  /**
   * The location from the codebase of the domain as a string (see {@link
   * ProtectionDomain#getCodeSource()} and {@link CodeSource#getLocation()}).
   */
  @Nullable private final String locationString;

  /**
   * The {@link java.security.ProtectionDomain#implies(Permission)} result from the requested domain
   * and permission.
   */
  private final boolean implies;

  public DomainInfo() {
    this.locationString = null;
    this.implies = false;
  }

  public DomainInfo(ProtectionDomain domain, Permission permission) {
    final CodeSource src = domain.getCodeSource();
    final URL url = (src != null) ? src.getLocation() : null;

    this.locationString = (url != null) ? url.toString() : null;
    this.implies = domain.implies(permission);
  }

  public DomainInfo(@Nullable String locationString, boolean implies) {
    this.locationString = locationString;
    this.implies = implies;
  }

  @Nullable
  public String getLocationString() {
    return locationString;
  }

  public boolean implies() {
    return implies;
  }

  @Override
  public int hashCode() {
    return Objects.hash(locationString, implies);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof DomainInfo) {
      final DomainInfo i = (DomainInfo) obj;

      return (implies == i.implies) && Objects.equals(locationString, i.locationString);
    }
    return false;
  }

  @Override
  public String toString() {
    return "DomainInfo[location=" + locationString + ", implies=" + implies + ']';
  }
}
