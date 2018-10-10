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

import java.util.List;
import java.util.Properties;

/**
 * Utility classes for compressing strings based on system property values with special <code>
 * ${property.name}</code> strings.
 */
public class PropertiesUtil {
  public static final List<String> PROPERTIES =
      Resources.readLines(PropertiesUtil.class, "properties.txt");

  private final Properties properties;

  /**
   * Builds a property utility with the given set of system property mappings.
   *
   * @param properties the set of property names/values to use when contracting
   */
  public PropertiesUtil(Properties properties) {
    this.properties = properties;
  }

  /**
   * Compresses the specified strings by replacing occurrences of configured system properties.
   *
   * @param s the string to compress
   * @return the corresponding compressed string
   */
  public String compress(String s) {
    for (final String property : PropertiesUtil.PROPERTIES) {
      s = replaceWithProperty(s, property);
    }
    return s;
  }

  private String replaceWithProperty(String s, String property) {
    final String value = properties.getProperty(property);

    if ((value != null) && !value.isEmpty()) {
      return s.replace(value, "${" + property + "}");
    }
    return s;
  }
}
