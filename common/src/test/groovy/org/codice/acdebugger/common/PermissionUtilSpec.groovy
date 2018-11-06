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
package org.codice.acdebugger.common

import net.sourceforge.prograde.policyparser.Parser
import spock.lang.Specification
import spock.lang.Unroll

class PermissionUtilSpec extends Specification {
  @Unroll
  def "test getPermissionString() when called with #with_what"() {
    when:
      def encoded = PermissionUtil.getPermissionString(clazz, name, actions)

    then:
      encoded == result

    when:
      def permission = new Parser(false).parse(
          new StringReader(
              String.format("grant codebase \"file:/bundle\" { permission %s; }", encoded)
          )
      ).grantEntries.get(0).permissions.get(0)

    then:
      permission.permissionType == clazz.name
      permission.permissionName == name
      permission.actions == (((actions == null) || actions.empty) ? null : actions)

    where:
      with_what                                                 || clazz              | name                 | actions              || result
      "a file permission class"                                 || FilePermission     | "path"               | "read,write"         || "java.io.FilePermission \"path\", \"read,write\""
      "a property permission class"                             || PropertyPermission | "property"           | "read"               || "java.util.PropertyPermission \"property\", \"read\""
      "null as actions"                                         || FilePermission     | "C:\\path"           | null                 || "java.io.FilePermission \"C:\\\\path\""
      "empty actions"                                           || FilePermission     | "path/child"         | ""                   || "java.io.FilePermission \"path/child\""
      "character 0x7 in the name and actions"                   || FilePermission     | "path\u0007"         | "read\u0007"         || "java.io.FilePermission \"path\\a\", \"read\\a\""
      "character \\b in the name and actions"                   || FilePermission     | "path\b"             | "read\b"             || "java.io.FilePermission \"path\\b\", \"read\\b\""
      "character \\f in the name and actions"                   || FilePermission     | "path\f"             | "read\f"             || "java.io.FilePermission \"path\\f\", \"read\\f\""
      "character \\n in the name and actions"                   || FilePermission     | "path\n"             | "read\n"             || "java.io.FilePermission \"path\\n\", \"read\\n\""
      "character \\r in the name and actions"                   || FilePermission     | "path\r"             | "read\r"             || "java.io.FilePermission \"path\\r\", \"read\\r\""
      "character \\t in the name and actions"                   || FilePermission     | "path\t"             | "read\t"             || "java.io.FilePermission \"path\\t\", \"read\\t\""
      "character 0xB in the name and actions"                   || FilePermission     | "path\u000B"         | "read\u000b"         || "java.io.FilePermission \"path\\v\", \"read\\v\""
      "character \\ in the name and actions"                    || FilePermission     | "path\\"             | "read\\"             || "java.io.FilePermission \"path\\\\\", \"read\\\\\""
      "character outside and on the limit of the regular range" || FilePermission     | "path é\u0002\u007f" | "read é\u0002\u007F" || "java.io.FilePermission \"path \\351\\2\\177\", \"read \\351\\2\\177\""
  }
}
