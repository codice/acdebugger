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
package org.codice.acdebugger.api

import com.sun.jdi.ArrayReference
import com.sun.jdi.Location
import com.sun.jdi.ObjectReference
import com.sun.jdi.StackFrame
import org.codice.acdebugger.ReflectionSpecification
import org.codice.acdebugger.common.DomainInfo
import org.codice.acdebugger.impl.Backdoor
import org.codice.acdebugger.impl.SystemProperties
import spock.lang.Shared
import spock.lang.Unroll

class DomainUtilSpec extends ReflectionSpecification {
  static def LOCATION = 'http://somewhere:1234/projects/home/base/system/SomeLocation.jar'
  static def FILE_LOCATION = 'file:/projects/home/base/system/SomeLocation.jar'
  static def COMPRESSED_LOCATION = 'file:${home}${/}base${/}system${/}SomeLocation.jar'

  @Shared
  def URL_CLASS = MockClassType('URL_CLASS', 'Ljava/net/URL;')
  @Shared
  def CODE_SOURCE_CLASS = MockClassType('CODE_SOURCE_CLASS', 'Ljava/security/CodeSource;')
  @Shared
  def DOMAIN_CLASS = MockClassType('DOMAIN_CLASS', 'Ljava/security/ProtectionDomain;')

  @Shared
  def URL = MockObjectReference('URL', URL_CLASS, toString: LOCATION)
  @Shared
  def FILE_URL = MockObjectReference('FILE_URL', URL_CLASS, toString: FILE_LOCATION)
  @Shared
  def CODE_SOURCE = MockObjectReference('CODE_SOURCE', CODE_SOURCE_CLASS, getLocation: URL)
  @Shared
  def FILE_CODE_SOURCE = MockObjectReference('FILE_CODE_SOURCE', CODE_SOURCE_CLASS, getLocation: FILE_URL)
  @Shared
  def CODE_SOURCE_WITH_NO_LOCATION = MockObjectReference('CODE_SOURCE', CODE_SOURCE_CLASS)
  @Shared
  def DOMAIN = MockObjectReference('DOMAIN1', DOMAIN_CLASS, getCodeSource: CODE_SOURCE)
  @Shared
  def FILE_DOMAIN = MockObjectReference('FILE_DOMAIN', DOMAIN_CLASS, getCodeSource: FILE_CODE_SOURCE)
  @Shared
  def DOMAIN_WITH_NO_LOCATION = MockObjectReference('DOMAIN1', DOMAIN_CLASS, getCodeSource: CODE_SOURCE_WITH_NO_LOCATION)
  @Shared
  def DOMAIN_WITH_NO_CODE_SOURCE = MockObjectReference('DOMAIN1', DOMAIN_CLASS)

  @Shared
  def ARRAY = Mock(ArrayReference)
  @Shared
  def PERMISSION = Mock(ObjectReference)
  @Shared
  def PERMISSION_INFOS = Mock(Set)

  @Shared
  def DOMAINS = [DOMAIN_WITH_NO_LOCATION, DOMAIN, FILE_DOMAIN, DOMAIN_WITH_NO_CODE_SOURCE]
  @Shared
  def LOCATIONS = [null, LOCATION, FILE_LOCATION, null]
  @Shared
  def IMPLIES = [true, true, false, true]

  @Shared
  def CLASS_OBJ = MockClassObjectReference('CLASS_OBJ', getProtectionDomain0: DOMAIN)
  @Shared
  def CLASS = MockClassType('CLASS', 'Lsome/class/ClassName;', classObject: CLASS_OBJ)
  @Shared
  def STACKFRAME = Mock(StackFrame, name: 'STACKFRAME') {
    location() >> Mock(Location) {
      declaringType() >> CLASS
    }
  }

  @Unroll
  def "test get() when not found in cache and with no backdoor and #and_what"() {
    given:
      def properties = Mock(SystemProperties)
      def debug = Mock(Debug)
      def cache = Mock(Map)
      def backdoor = Mock(Backdoor)

    when:
      def result = new DomainUtil(debug).get(obj)

    then:
      result == finalLocation

    and:
      debug.reflection() >> REFLECTION
      cache_access * debug.computeIfAbsent(*_) >> cache
      cache_get * cache.get(_) >> null
      back_count * debug.backdoor() >> backdoor
      back_count * backdoor.getDomain(*_) >> { throw new IllegalStateException() }
      compressed * debug.properties() >> properties
      compressed * properties.compress(debug, location) >> finalLocation
      cached * cache.put(_, {
        (finalLocation != null) ? it.is(finalLocation) : it.is(DomainUtil.NULL_DOMAIN)
      }) >> null

    where:
      and_what                       || obj                        | compressed | cached | cache_access | cache_get | back_count || location      | finalLocation
      'a domain'                     || DOMAIN                     | 0          | 1      | 1            | 1         | 1          || LOCATION      | LOCATION
      'a file domain'                || FILE_DOMAIN                | 1          | 1      | 1            | 1         | 1          || FILE_LOCATION | COMPRESSED_LOCATION
      'a domain with no location'    || DOMAIN_WITH_NO_LOCATION    | 0          | 1      | 1            | 1         | 1          || null          | null
      'a domain with no code source' || DOMAIN_WITH_NO_CODE_SOURCE | 0          | 1      | 1            | 1         | 1          || null          | null
      'a class object'               || CLASS_OBJ                  | 0          | 2      | 1            | 2         | 2          || LOCATION      | LOCATION
      'a class reference'            || CLASS                      | 0          | 2      | 1            | 3         | 2          || LOCATION      | LOCATION
      'a stack frame'                || STACKFRAME                 | 0          | 2      | 1            | 3         | 2          || LOCATION      | LOCATION
      'null'                         || null                       | 0          | 0      | 0            | 0         | 0          || null          | null
      'something unsupported'        || 'abc'                      | 0          | 0      | 1            | 1         | 0          || null          | null
  }

  @Unroll
  def "test get() with #with_what when found in cache as #as_what"() {
    given:
      def debug = Mock(Debug)
      def cache = Mock(Map)

    when:
      def result = new DomainUtil(debug).get(obj)

    then:
      result == location

    and:
      1 * debug.reflection()
      0 * debug.properties()
      1 * debug.computeIfAbsent(*_) >> cache
      1 * cache.get(obj) >> cached
      0 * debug.backdoor()
      0 * cache.put(*_)

    where:
      with_what        | as_what             || obj       | cached                   | location
      'a domain'       | 'a domain location' || DOMAIN    | 'file://domain/location' | 'file://domain/location'
      'a domain'       | 'null'              || DOMAIN    | DomainUtil.NULL_DOMAIN   | null
      'a class object' | 'a domain location' || CLASS_OBJ | 'file://domain/location' | 'file://domain/location'
      'a class object' | 'null'              || CLASS_OBJ | DomainUtil.NULL_DOMAIN   | null
  }

  @Unroll
  def "test get() when not found in cache and provided by backdoor as #as_what"() {
    given:
      def debug = Mock(Debug)
      def cache = Mock(Map)
      def backdoor = Mock(Backdoor)

    when:
      def result = new DomainUtil(debug).get(DOMAIN)

    then:
      result == location

    and:
      1 * debug.reflection()
      0 * debug.properties()
      1 * debug.computeIfAbsent(*_) >> cache
      1 * cache.get(DOMAIN) >> null
      1 * debug.backdoor() >> backdoor
      1 * backdoor.getDomain(*_) >> location
      1 * cache.put(DOMAIN, {
        (location != null) ? it.is(location) : it.is(DomainUtil.NULL_DOMAIN)
      }) >> null

    where:
      as_what             || location
      'a domain location' || 'file://domain/location'
      'null'              || null
  }

  @Unroll
  def "test get() when not found in cache and backdoor failed with #exception.class.simpleName"() {
    given:
      def debug = Mock(Debug)
      def cache = Mock(Map)
      def backdoor = Mock(Backdoor)

    when:
      def result = new DomainUtil(debug).get(DOMAIN)

    then:
      result == LOCATION

    and:
      debug.reflection() >> REFLECTION
      0 * debug.properties()
      1 * debug.computeIfAbsent(*_) >> cache
      1 * cache.get(DOMAIN) >> null
      1 * debug.backdoor() >> backdoor
      1 * backdoor.getDomain(*_) >> { throw exception }
      1 * cache.put(DOMAIN, LOCATION) >> null

    where:
      exception << [new NullPointerException(), new Exception(), new Error()]
  }

  def "test get() when not found in cache and backdoor failed with OutOfMemoryError"() {
    given:
      def debug = Mock(Debug)
      def cache = Mock(Map)
      def backdoor = Mock(Backdoor)
      def exception = new OutOfMemoryError()

    when:
      new DomainUtil(debug).get(DOMAIN)

    then:
      def e = thrown(OutOfMemoryError)

    and:
      e.is(exception)

    and:
      debug.reflection() >> REFLECTION
      1 * debug.computeIfAbsent(*_) >> cache
      1 * cache.get(DOMAIN) >> null
      1 * debug.backdoor() >> backdoor
      1 * backdoor.getDomain(*_) >> { throw exception }
      0 * cache.put(DOMAIN, LOCATION)
  }

  @Unroll
  def "test get() with an array and no backdoor where #where_what"() {
    given:
      def permissions = Mock(PermissionUtil)
      def debug = Mock(Debug) {
        properties() >> Mock(SystemProperties) {
          compress(_, _) >> { it[1] }
        }
      }
      def cache = Mock(Map) {
        get(_) >> null
      }
      def backdoor = Mock(Backdoor)
      def infos = new ArrayList<>()

      LOCATIONS.eachWithIndex { location, i ->
        infos.add(new DomainInfo(location, implies[i]))
      }

    when:
      def result = new DomainUtil(debug).get(ARRAY, domains, PERMISSION, PERMISSION_INFOS, first)

    then:
      result == infos

    and:
      debug.reflection() >> REFLECTION
      1 * debug.computeIfAbsent(*_) >> cache
      (back_min.._) * debug.backdoor() >> backdoor
      1 * backdoor.getDomainInfo(*_) >> { throw new IllegalStateException() }
      domain_count * backdoor.getDomain(*_) >> { throw new IllegalStateException() }
      1 * debug.permissions() >> permissions
      LOCATIONS.eachWithIndex { location, i ->
        def domain = domains[i]

        location_implies_count[i] * permissions.implies(location, PERMISSION_INFOS) >> location_implies[i]
        domain_implies_count[i] * permissions.implies(domain, PERMISSION) >> domain_implies[i]
        if (domain) {
          1 * cache.put(domain, {
            (location != null) ? it.is(location) : it.is(DomainUtil.NULL_DOMAIN)
          }) >> null
        }
      }

    where:
      where_what                                                                        || domains                                              | first || location_implies_count | location_implies     | domain_implies_count | domain_implies   | back_min | domain_count || implies
      'no permissions cached and only boot domains defined after first failure'         || DOMAINS                                              | 2     || [0, 0, 1, 0]           | [_, _, false, _]     | [0, 0, 0, 0]         | [_, _, _, _]     | 5        | 4            || IMPLIES
      'no permissions cached and domain without permission defined after first failure' || DOMAINS                                              | 1     || [0, 1, 1, 0]           | [_, false, false, _] | [0, 0, 1, 0]         | [_, _, false, _] | 5        | 4            || [true, false, false, true]
      'no permissions cached and domain with permission defined after first failure'    || DOMAINS                                              | 1     || [0, 1, 1, 0]           | [_, false, false, _] | [0, 0, 1, 0]         | [_, _, true, _]  | 5        | 4            || [true, false, true, true]
      'no permissions cached and null domain ref'                                       || [DOMAIN_WITH_NO_LOCATION, DOMAIN, FILE_DOMAIN, null] | 2     || [0, 0, 1, 0]           | [_, _, false, _]     | [0, 0, 0, 0]         | [_, _, _, _]     | 4        | 3            || IMPLIES
      'permissions cached and domain with permission defined after first failure'       || DOMAINS                                              | 1     || [0, 1, 1, 0]           | [_, false, true, _]  | [0, 0, 0, 0]         | [_, _, _, _]     | 5        | 4            || [true, false, true, true]
      'permissions cached and domain with permission cache at first failure'            || DOMAINS                                              | 1     || [0, 1, 1, 0]           | [_, true, true, _]   | [0, 0, 0, 0]         | [_, _, _, _]     | 5        | 4            || [true, true, true, true]
  }

  def "test get() with an array when provided by a backdoor"() {
    given:
      def debug = Mock(Debug)
      def cache = Mock(Map) {
        get(_) >> null
      }
      def backdoor = Mock(Backdoor)
      def infos = new ArrayList<>()

      LOCATIONS.eachWithIndex { location, i ->
        infos.add(new DomainInfo(location, IMPLIES[i]))
      }

    when:
      def result = new DomainUtil(debug).get(ARRAY, DOMAINS, PERMISSION, PERMISSION_INFOS, 2)

    then:
      result == infos

    and:
      debug.reflection() >> REFLECTION
      1 * debug.computeIfAbsent(*_) >> cache
      1 * debug.backdoor() >> backdoor
      1 * backdoor.getDomainInfo(*_) >> infos
      0 * backdoor.getDomain(*_)
      0 * debug.permissions()
      LOCATIONS.eachWithIndex { location, i ->
        1 * cache.put(DOMAINS[i], {
          (location != null) ? it.is(location) : it.is(DomainUtil.NULL_DOMAIN)
        }) >> null
      }
  }

  @Unroll
  def "test get() with an array and backdoor failed with #exception.class.simpleName"() {
    given:
      def permissions = Mock(PermissionUtil)
      def debug = Mock(Debug) {
        properties() >> Mock(SystemProperties) {
          compress(_, _) >> { it[1] }
        }
      }
      def cache = Mock(Map) {
        get(_) >> null
      }
      def backdoor = Mock(Backdoor)
      def location_implies_count = [0, 0, 1, 0]
      def location_implies = [_, _, false, _]
      def infos = new ArrayList<>()

      LOCATIONS.eachWithIndex { location, i ->
        infos.add(new DomainInfo(location, IMPLIES[i]))
      }

    when:
      def result = new DomainUtil(debug).get(ARRAY, DOMAINS, PERMISSION, PERMISSION_INFOS, 2)

    then:
      result == infos

    and:
      debug.reflection() >> REFLECTION
      1 * debug.computeIfAbsent(*_) >> cache
      (5.._) * debug.backdoor() >> backdoor
      1 * backdoor.getDomainInfo(*_) >> { throw exception }
      4 * backdoor.getDomain(*_) >> { throw new IllegalStateException() }
      1 * debug.permissions() >> permissions
      LOCATIONS.eachWithIndex { location, i ->
        location_implies_count[i] * permissions.implies(location, PERMISSION_INFOS) >> location_implies[i]
        0 * permissions.implies(DOMAINS[i], PERMISSION)
      }

    where:
      exception << [new NullPointerException(), new Exception(), new Error()]
  }

  def "test get() with an array and backdoor failed with OutOfMemoryError"() {
    given:
      def debug = Mock(Debug)
      def cache = Mock(Map)
      def backdoor = Mock(Backdoor)
      def exception = new OutOfMemoryError()

    when:
      new DomainUtil(debug).get(ARRAY, DOMAINS, PERMISSION, PERMISSION_INFOS, 2)

    then:
      def e = thrown(OutOfMemoryError)

    and:
      e.is(exception)

    and:
      debug.reflection() >> REFLECTION
      1 * debug.computeIfAbsent(*_) >> cache
      1 * debug.backdoor() >> backdoor
      1 * backdoor.getDomainInfo(*_) >> { throw exception }
      0 * debug.permissions()
      0 * cache.put(_, _)
  }
}
