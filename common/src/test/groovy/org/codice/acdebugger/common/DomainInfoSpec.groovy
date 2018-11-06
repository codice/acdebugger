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

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.security.CodeSource
import java.security.Permission
import java.security.PermissionCollection
import java.security.ProtectionDomain
import java.security.cert.Certificate

class DomainInfoSpec extends Specification {
  static def LOCATION = 'file:/location'
  static def LOCATION_URL = new URL(LOCATION)
  static def CODESOURCE = new CodeSource(LOCATION_URL, (Certificate[]) null)
  static def CODESOURCE_NULL_URL = new CodeSource(null, (Certificate[]) null)
  static def INFO = new DomainInfo(LOCATION, true)

  @Shared
  def PERMISSION = Stub(Permission)

  def "test default constructor"() {
    when:
      def info = new DomainInfo()

    then:
      info.locationString == null
      !info.implies()
  }

  @Unroll
  def "test domain constructor with #with_what"() {
    given:
      def permissions = Mock(PermissionCollection)
      def domain = new ProtectionDomain(codesource, permissions)

    when:
      def info = new DomainInfo(domain, PERMISSION)

      info.toString()

    then:
      1 * permissions.implies(PERMISSION) >> implies

    and:
      info.locationString == location
      info.implies() == implies

    where:
      with_what                                            || codesource          | implies || location
      'a codesource and url and granted permission'        || CODESOURCE          | true    || LOCATION
      'a codesource and url and not granted permission'    || CODESOURCE          | false   || LOCATION
      'a codesource and no url and granted permission'     || CODESOURCE_NULL_URL | true    || null
      'a codesource and no url and not granted permission' || CODESOURCE_NULL_URL | false   || null
      'no codesource and granted permission'               || null                | true    || null
      'no codesource and not granted permission'           || null                | false   || null
  }

  @Unroll
  def "test string constructor with #implies"() {
    when:
      def info = new DomainInfo(LOCATION, implies)

    then:
      info.locationString == LOCATION
      info.implies() == implies

    where:
      implies << [true, false]
  }

  @Unroll
  def "test json serialization with #with_what"() {
    given:
      def info = new DomainInfo(domain, implies)

    when:
      def result = JsonUtils.toJson(info)

    then:
      result == json

    where:
      with_what                              || domain   | implies || json
      'a domain and granted permission'      || LOCATION | true    || '{"locationString":"file:/location","implies":true}'
      'a domain and not granted permission'  || LOCATION | false   || '{"locationString":"file:/location","implies":false}'
      'no domain and granted permission'     || null     | true    || '{"locationString":null,"implies":true}'
      'no domain and not granted permission' || null     | false   || '{"locationString":null,"implies":false}'
  }

  @Unroll
  def "test json deserialization with #with_what"() {
    when:
      def info = JsonUtils.fromJson(json, DomainInfo)

    then:
      info.locationString == domain
      info.implies == implies

    where:
      with_what                                || json                                                  || domain   | implies
      'a domain and granted permission'        || '{"locationString":"file:/location","implies":true}'  || LOCATION | true
      'a domain and not granted permission'    || '{"locationString":"file:/location","implies":false}' || LOCATION | false
      'null domain and granted permission'     || '{"locationString":null,"implies":true}'              || null     | true
      'null domain and not granted permission' || '{"locationString":null,"implies":false}'             || null     | false
      'missing domain and granted permission'  || '{"implies":true}'                                    || null     | true
      'a domain and missing permission'        || '{"locationString":"file:/location"}'                 || LOCATION | false
      'missing domain and permission'          || '{}'                                                  || null     | false
  }

  @Unroll
  def "test hashCode() when #when_what"() {
    expect:
      (info1.hashCode() == info2.hashCode()) == result

    where:
      when_what                    || info1                           | info2                                    || result
      'equals'                     || new DomainInfo(LOCATION, true)  | new DomainInfo(LOCATION, true)           || true
      'identical'                  || INFO                            | INFO                                     || true
      'both locations are null'    || new DomainInfo(null, true)      | new DomainInfo(null, true)               || true
      'locations are different'    || new DomainInfo(LOCATION, true)  | new DomainInfo('file://location2', true) || false
      'one location is null'       || new DomainInfo(null, true)      | new DomainInfo(LOCATION, true)           || false
      'the other location is null' || new DomainInfo(LOCATION, true)  | new DomainInfo(null, true)               || false
      'implies are different'      || new DomainInfo(LOCATION, false) | new DomainInfo(LOCATION, true)           || false
      'everything different'       || new DomainInfo(LOCATION, false) | new DomainInfo(null, true)               || false
  }

  @Unroll
  def "test equals() when #when_what"() {
    expect:
      info1.equals(info2) == result

    where:
      when_what                       || info1                           | info2                                    || result
      'equals'                        || new DomainInfo(LOCATION, true)  | new DomainInfo(LOCATION, true)           || true
      'identical'                     || INFO                            | INFO                                     || true
      'both locations are null'       || new DomainInfo(null, true)      | new DomainInfo(null, true)               || true
      'locations are different'       || new DomainInfo(LOCATION, true)  | new DomainInfo('file://location2', true) || false
      'one location is null'          || new DomainInfo(null, true)      | new DomainInfo(LOCATION, true)           || false
      'the other location is null'    || new DomainInfo(LOCATION, true)  | new DomainInfo(null, true)               || false
      'implies are different'         || new DomainInfo(LOCATION, false) | new DomainInfo(LOCATION, true)           || false
      'everything different'          || new DomainInfo(LOCATION, false) | new DomainInfo(null, true)               || false
      'the other is null'             || new DomainInfo(LOCATION, false) | null                                     || false
      'the other is not a DomainInfo' || new DomainInfo(LOCATION, false) | 'abc'                                    || false
  }
}
