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

import java.security.Permission;
import javax.annotation.Nullable;

/** Provides permission-specific functionality. */
public class PermissionUtil {
  /**
   * Gets a permission string compatible with the policy file format based on the given policy
   * class, name and actions.
   *
   * @param clazz the policy class
   * @param name the policy name
   * @param actions the policy actions or <code>null</code> if none
   * @return the corresponding policy string
   */
  public static String getPermissionString(
      Class<? extends Permission> clazz, String name, @Nullable String actions) {
    return PermissionUtil.getPermissionString(clazz.getName(), name, actions);
  }

  /**
   * Gets a permission string compatible with the policy file format based on the given policy
   * class, name and actions.
   *
   * @param clazz the policy class
   * @param name the policy name
   * @param actions the policy actions or <code>null</code> if none
   * @return the corresponding policy string
   */
  public static String getPermissionString(String clazz, String name, @Nullable String actions) {
    final StringBuilder sb = new StringBuilder();

    sb.append(clazz);
    sb.append(" \"");
    sb.append(PermissionUtil.escape(name));
    if ((actions != null) && !actions.isEmpty()) {
      sb.append("\", \"").append(PermissionUtil.escape(actions));
    }
    sb.append("\"");
    return sb.toString();
  }

  /**
   * Escape quoted strings for permissions such that it would be compatible would the policy file
   * format.
   *
   * @param str the string to be quoted
   * @return the corresponding quoted string
   */
  private static String escape(String str) {
    final StringBuilder sb = new StringBuilder(str.length() * 3 / 2); // add 50% to start with

    for (final char c : str.toCharArray()) {
      switch (c) {
        case 0x7:
          sb.append("\\a");
          break;
        case '\b':
          sb.append("\\b");
          break;
        case 0xc:
          sb.append("\\f");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        case 0xB:
          sb.append("\\v");
          break;
        case '\\':
          sb.append("\\\\");
          break;
        default:
          if ((c >= 0x20) && (c < 0x7F)) {
            sb.append(c);
          } else {
            sb.append("\\").append(Integer.toOctalString(c));
          }
      }
    }
    return sb.toString();
  }
}
