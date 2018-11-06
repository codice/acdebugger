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
package org.codice.acdebugger.breakpoints

import spock.lang.Specification

class SecurityServicePermissionImpliedInformationSpec extends Specification {
  static def BUNDLE = 'bundle.name'
  static def PERMISSIONS = ['p1', 'p2'] as Set<String>

  def "test constructor"() {
    when:
      def info = new SecurityServicePermissionImpliedInformation(BUNDLE, PERMISSIONS)
      def analysis = info.analyze()

      info.dump(true, '')
      info.toString()
      analysis.each { it.toString() }
    then:
      info.permissions == PERMISSIONS
      info.stack.isEmpty()
      !info.acceptable
      info.acceptablePermissions == null
      info.bundle == BUNDLE

      analysis.eachWithIndex { a, i ->
        assert a.stack.isEmpty()
        assert a.grantedDomains == [BUNDLE] as Set<String>
        assert a.doPrivilegedLocations.isEmpty()
      }
  }
}
