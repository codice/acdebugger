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

import com.sun.jdi.ObjectReference
import org.codice.acdebugger.api.Debug
import org.codice.acdebugger.api.LocationUtil
import org.codice.acdebugger.api.PermissionUtil
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class AccessControlContextInfoSpec extends Specification {
  static def PERMISSION_INFO = 'java.io.FilePermission "${/}etc${/}some-dir", "read"'
  static def PERMISSION_INFO2 = 'permission java.lang.RuntimePermission "createClassLoader"'
  static def BUNDLE = 'bundle.name'
  static def BUNDLE2 = 'bundle.name2'
  static def BUNDLE3 = 'bundle.name3'
  static def UNKNOWN_TOSTRING = 'ref'

  @Shared
  def PERMISSION = Stub(ObjectReference)

  @Shared
  def PERMISSION_INFOS = [PERMISSION_INFO, PERMISSION_INFO2] as Set<String>

  @Shared
  def UNKNOWN_DOMAIN = Stub(ObjectReference) {
    toString() >> UNKNOWN_TOSTRING
  }
  @Shared
  def BOOT_DOMAIN = Stub(ObjectReference)
  @Shared
  def DOMAIN = Stub(ObjectReference)
  @Shared
  def DOMAIN2 = Stub(ObjectReference)
  @Shared
  def DOMAIN3 = Stub(ObjectReference)

  @Unroll
  def "test constructor when #when_what"() {
    given:
      def permissions = Mock(PermissionUtil)
      def locations = Mock(LocationUtil)
      def debug = Mock(Debug)

    when:
      def info = new AccessControlContextInfo(debug, context, local_i, PERMISSION)

      info.dumpTroubleshootingInfo(true)

    then:
      info.permissions == PERMISSION_INFOS
      info.domains == domains
      info.currentDomain == current_domain
      info.currentDomainReference == current_ref
      info.currentDomainIndex == local_i
      info.privilegedDomains == privileged as Set<String>

    and:
      interaction {
        stub(debug, permissions, locations)
        stub(locations)
        stub(permissions)
      }

    and:
      if (domain_implies) {
        domains.eachWithIndex { domain, i ->
          domain_implies_count[i] * permissions.implies(domain, PERMISSION_INFOS) >> domain_implies[i]
        }
      }
      0 * permissions.implies(_, PERMISSION_INFOS)
      if (ref_implies) {
        context.eachWithIndex { ref, i ->
          ref_implies_count[i] * permissions.implies(ref, PERMISSION) >> ref_implies[i]
        }
      }
      0 * permissions.implies(_, PERMISSION_INFOS)
      if (granted_domains) {
        granted_domains.each {
          1 * permissions.grant({ it in granted_domains }, PERMISSION_INFOS)
        }
      }
      0 * permissions.grant(*_)

    where:
      when_what                                                                                                                                || context                                         | local_i || domains                                        | privileged               | current_domain | current_ref | domain_implies_count | domain_implies      | ref_implies_count | ref_implies       || granted_domains
      'only one domain is in the context'                                                                                                      || [DOMAIN]                                        | 0       || [BUNDLE]                                       | [null]                   | BUNDLE         | DOMAIN      | [0]                  | [_]                 | [0]               | [_]               || []
      'multiple domains are in the context and failing on the last one'                                                                        || [BOOT_DOMAIN, DOMAIN, DOMAIN2]                  | 2       || [null, BUNDLE, BUNDLE2]                        | [null, BUNDLE]           | BUNDLE2        | DOMAIN2     | [0, 0, 0]            | [_, _, _]           | [0, 0, 0]         | [_, _, _]         || [null, BUNDLE]
      'duplicate domains are in the context'                                                                                                   || [BOOT_DOMAIN, DOMAIN, DOMAIN2, DOMAIN, DOMAIN3] | 4       || [null, BUNDLE, BUNDLE2, BUNDLE, BUNDLE3]       | [null, BUNDLE, BUNDLE2]  | BUNDLE3        | DOMAIN3     | [0, 0, 0, 0, 0]      | [_, _, _, _, _]     | [0, 0, 0, 0, 0]   | [_, _, _, _, _]   || [null, BUNDLE, BUNDLE2, BUNDLE]
      'all unknown domains were already granted in the cache'                                                                                  || [BOOT_DOMAIN, DOMAIN, DOMAIN2]                  | 0       || [null, BUNDLE, BUNDLE2]                        | [null, BUNDLE, BUNDLE2]  | null           | BOOT_DOMAIN | [0, 1, 1]            | [_, true, true]     | [0, 0, 0]         | [_, _, _]         || []
      'all unknown domains were not already granted in the cache and none were granted in the VM'                                              || [DOMAIN, DOMAIN2]                               | 0       || [BUNDLE, BUNDLE2]                              | [null]                   | BUNDLE         | DOMAIN      | [0, 1]               | [_, false]          | [0, 1]            | [_, false]        || []
      'all unknown domains were not already granted in the cache and none were granted in the VM and unable to find location for some domains' || [DOMAIN, UNKNOWN_DOMAIN, DOMAIN2]               | 0       || [BUNDLE, "unknown-$UNKNOWN_TOSTRING", BUNDLE2] | [null]                   | BUNDLE         | DOMAIN      | [0, 0, 1]            | [_, _, false]       | [0, 1, 1]         | [_, false, false] || []
      'all unknown domains were not already granted in the cache and none were granted in the VM'                                              || [BOOT_DOMAIN, DOMAIN, DOMAIN2]                  | 0       || [null, BUNDLE, BUNDLE2]                        | [null, BUNDLE2]          | null           | BOOT_DOMAIN | [0, 1, 1]            | [_, false, false]   | [0, 1, 1]         | [_, false, true]  || [BUNDLE2]
      'some unknown domains were not already granted in the cache and some were granted in the VM'                                             || [BOOT_DOMAIN, DOMAIN, DOMAIN2, DOMAIN3]         | 1       || [null, BUNDLE, BUNDLE2, BUNDLE3]               | [null, BUNDLE2, BUNDLE3] | BUNDLE         | DOMAIN      | [0, 0, 1, 1]         | [_, _, true, false] | [0, 0, 0, 1]      | [_, _, _, true]   || [null, BUNDLE3]
  }

  @Unroll
  def "test isPrivileged() when domain #is_what granted the permission"() {
    given:
      def permissions = Mock(PermissionUtil)
      def locations = Mock(LocationUtil)
      def debug = Mock(Debug)

    when:
      def result = new AccessControlContextInfo(debug, [BOOT_DOMAIN, DOMAIN, DOMAIN2], 2, PERMISSION).isPrivileged(domain)

    then:
      result == granted

    and:
      interaction {
        stub(debug, permissions, locations)
        stub(locations)
        stub(permissions)
      }

    where:
      is_what  || domain  || granted
      'is'     || BUNDLE  || true
      'is not' || BUNDLE2 || false
  }

  def "test grant() does add the domain as a privileged one only in the newly created info and all the rest of the info is the same"() {
    given:
      def permissions = Mock(PermissionUtil)
      def locations = Mock(LocationUtil)
      def debug = Mock(Debug)

    when:
      def info = new AccessControlContextInfo(debug, [BOOT_DOMAIN, DOMAIN, DOMAIN2], 2, PERMISSION);
      def newInfo = info.grant(BUNDLE2)

    then:
      info.permissions == PERMISSION_INFOS
      newInfo.permissions == PERMISSION_INFOS
      info.domains == [null, BUNDLE, BUNDLE2]
      newInfo.domains == [null, BUNDLE, BUNDLE2]
      info.currentDomain == BUNDLE2
      newInfo.currentDomain == BUNDLE2
      info.currentDomainReference == DOMAIN2
      newInfo.currentDomainReference == DOMAIN2
      info.currentDomainIndex == 2
      newInfo.currentDomainIndex == 2
      info.privilegedDomains == [null, BUNDLE] as Set<String>
      newInfo.privilegedDomains == [null, BUNDLE, BUNDLE2] as Set<String>

    and:
      interaction {
        stub(debug, permissions, locations)
        stub(locations)
        stub(permissions)
      }
  }

  private def stub(Debug debug, PermissionUtil permissionUtil, LocationUtil locationUtil) {
    with(debug) {
      permissions() >> permissionUtil
      locations() >> locationUtil
    }
  }

  private def stub(PermissionUtil permissions) {
    permissions.getPermissionStrings(PERMISSION) >> PERMISSION_INFOS
  }

  private def stub(LocationUtil locations) {
    locations.get(BOOT_DOMAIN) >> null
    locations.get(DOMAIN) >> BUNDLE
    locations.get(DOMAIN2) >> BUNDLE2
    locations.get(DOMAIN3) >> BUNDLE3
  }
}
