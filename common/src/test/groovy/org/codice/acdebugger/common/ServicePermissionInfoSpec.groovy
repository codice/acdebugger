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

import spock.lang.Specification
import spock.lang.Unroll

class ServicePermissionInfoSpec extends Specification {
  static def PERMISSIONS = ['permission.1'] as Set
  static def IMPLIED = ['permission.1', 'permission.2'] as Set
  static def NO_PERMISSIONS = [] as Set
  static def INFO = new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)

  def "test default constructor"() {
    when:
      def info = new ServicePermissionInfo()

      info.toString()
    then:
      info.permissionStrings.isEmpty()
      !info.implies()
      info.impliedPermissionStrings.isEmpty()
  }

  @Unroll
  def "test constructor and getter with #with_permissions and implies is #implies"() {
    when:
      def info = new ServicePermissionInfo(permissions, implies, permissions)

    then:
      info.permissionStrings == permissions
      info.implies() == implies
      info.impliedPermissionStrings == permissions

    where:
      with_permissions     || permissions    | implies | implied
      'actual permissions' || PERMISSIONS    | true    | IMPLIED
      'actual permissions' || PERMISSIONS    | false   | IMPLIED
      'no permissions'     || NO_PERMISSIONS | true    | NO_PERMISSIONS
      'no permissions'     || NO_PERMISSIONS | false   | NO_PERMISSIONS
  }

  @Unroll
  def "test constructor and getter with null permissions and implies is #implies"() {
    when:
      def info = new ServicePermissionInfo(null, implies, null)

    then:
      info.permissionStrings.isEmpty()
      info.implies() == implies
      info.impliedPermissionStrings.isEmpty()

    where:
      implies << [true, false]
  }

  @Unroll
  def "test json serialization with #with_what"() {
    given:
      def info = new ServicePermissionInfo(permissions, implies, implied)

    when:
      def result = JsonUtils.toJson(info)

    then:
      result == json

    where:
      with_what                                                             || permissions    | implies | implied        || json
      'permissions and implied permissions and granted permissions'         || PERMISSIONS    | true    | IMPLIED        || '{"permissionStrings":["permission.1"],"implies":true,"implied":["permission.1","permission.2"]}'
      'permissions and implied permissions and not granted permissions'     || PERMISSIONS    | false   | IMPLIED        || '{"permissionStrings":["permission.1"],"implies":false,"implied":["permission.1","permission.2"]}'
      'null permissions and no implied permissions and granted permissions' || null           | true    | NO_PERMISSIONS || '{"permissionStrings":null,"implies":true,"implied":[]}'
      'no permissions and null implied permissions and granted permissions' || NO_PERMISSIONS | true    | null           || '{"permissionStrings":[],"implies":true,"implied":null}'
  }

  @Unroll
  def "test json deserialization with #with_what"() {
    when:
      def info = org.codice.acdebugger.common.JsonUtils.fromJson(json, ServicePermissionInfo)

    then:
      info.permissionStrings == permissions
      info.implies() == implies
      info.impliedPermissionStrings == implied

    where:
      with_what                                                                || json                                                                                               || permissions    | implies | implied
      'permissions and implied permissions and granted permissions'            || '{"permissionStrings":["permission.1"],"implies":true,"implied":["permission.1","permission.2"]}'  || PERMISSIONS    | true    | IMPLIED
      'permissions and implied permissions and not granted permissions'        || '{"permissionStrings":["permission.1"],"implies":false,"implied":["permission.1","permission.2"]}' || PERMISSIONS    | false   | IMPLIED
      'null permissions and no implied permissions and granted permissions'    || '{"permissionStrings":null,"implies":true,"implied":[]}'                                           || NO_PERMISSIONS | true    | NO_PERMISSIONS
      'no permissions and null implied permissions and granted permissions'    || '{"permissionStrings":[],"implies":true,"implied":null}'                                           || NO_PERMISSIONS | true    | NO_PERMISSIONS
      'missing permissions and no implied permissions and granted permissions' || '{"implies":true,"implied":[]}'                                                                    || NO_PERMISSIONS | true    | NO_PERMISSIONS
      'no permissions and missing implied permissions and granted permissions' || '{"permissionStrings":[],"implies":true}'                                                          || NO_PERMISSIONS | true    | NO_PERMISSIONS
  }

  @Unroll
  def "test hashCode() when #when_what"() {
    expect:
      (info1.hashCode() == info2.hashCode()) == result

    where:
      when_what                      || info1                                                  | info2                                                     || result
      'equals'                       || new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)  | new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)     || true
      'identical'                    || INFO                                                   | INFO                                                      || true
      'both permissions are null'    || new ServicePermissionInfo(null, true, IMPLIED)         | new ServicePermissionInfo(null, true, IMPLIED)            || true
      'both implied are null'        || new ServicePermissionInfo(PERMISSIONS, true, null)     | new ServicePermissionInfo(PERMISSIONS, true, null)        || true
      'permissions are different'    || new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)  | new ServicePermissionInfo(IMPLIED, true, IMPLIED)         || false
      'one permission is null'       || new ServicePermissionInfo(null, true, IMPLIED)         | new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)     || false
      'the other permission is null' || new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)  | new ServicePermissionInfo(null, true, IMPLIED)            || false
      'implies are different'        || new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)  | new ServicePermissionInfo(PERMISSIONS, false, IMPLIED)    || false
      'implied are different'        || new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)  | new ServicePermissionInfo(PERMISSIONS, true, PERMISSIONS) || false
      'one implied is null'          || new ServicePermissionInfo(PERMISSIONS, true, null)     | new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)     || false
      'the other implied is null'    || new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)  | new ServicePermissionInfo(PERMISSIONS, true, null)        || false
      'everything different'         || new ServicePermissionInfo(PERMISSIONS, false, IMPLIED) | new ServicePermissionInfo(null, true, PERMISSIONS)        || false
  }

  @Unroll
  def "test equals() when #when_what"() {
    expect:
      info1.equals(info2) == result

    where:
      when_what                                  || info1                                                  | info2                                                     || result
      'equals'                                   || new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)  | new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)     || true
      'identical'                                || INFO                                                   | INFO                                                      || true
      'both permissions are null'                || new ServicePermissionInfo(null, true, IMPLIED)         | new ServicePermissionInfo(null, true, IMPLIED)            || true
      'both implied are null'                    || new ServicePermissionInfo(PERMISSIONS, true, null)     | new ServicePermissionInfo(PERMISSIONS, true, null)        || true
      'permissions are different'                || new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)  | new ServicePermissionInfo(IMPLIED, true, IMPLIED)         || false
      'one permission is null'                   || new ServicePermissionInfo(null, true, IMPLIED)         | new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)     || false
      'the other permission is null'             || new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)  | new ServicePermissionInfo(null, true, IMPLIED)            || false
      'implies are different'                    || new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)  | new ServicePermissionInfo(PERMISSIONS, false, IMPLIED)    || false
      'implied are different'                    || new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)  | new ServicePermissionInfo(PERMISSIONS, true, PERMISSIONS) || false
      'one implied is null'                      || new ServicePermissionInfo(PERMISSIONS, true, null)     | new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)     || false
      'the other implied is null'                || new ServicePermissionInfo(PERMISSIONS, true, IMPLIED)  | new ServicePermissionInfo(PERMISSIONS, true, null)        || false
      'everything different'                     || new ServicePermissionInfo(PERMISSIONS, false, IMPLIED) | new ServicePermissionInfo(null, true, PERMISSIONS)        || false
      'the other is null'                        || INFO                                                   | null                                                      || false
      'the other is not a ServicePermissionInfo' || INFO                                                   | 'abc'                                                     || false
  }
}
