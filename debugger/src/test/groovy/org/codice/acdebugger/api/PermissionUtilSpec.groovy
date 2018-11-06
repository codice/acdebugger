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

import com.sun.jdi.ObjectReference
import org.codice.acdebugger.ReflectionSpecification
import org.codice.acdebugger.common.ServicePermissionInfo
import org.codice.acdebugger.impl.Backdoor
import org.codice.acdebugger.impl.DebugContext
import org.codice.acdebugger.impl.SystemProperties
import spock.lang.Shared
import spock.lang.Unroll

class PermissionUtilSpec extends ReflectionSpecification {
  static def LOCATION = 'http://somewhere:1234/projects/home/base/system/SomeLocation.jar'
  static def BUNDLE = 'some.Bundle'
  static def ACTIONS = 'register, listen'
  static def FILENAME = 'file:/projects/home/base/system/SomeLocation.jar'
  static def OBJ_CLASS = ['service1', 'service2'] as String[]
  static def SERVICE_PERMISSION_CLASSNAME = 'org.osgi.framework.ServicePermission'
  static def PERMISSION_NAME = 'for.this'
  static def PERMISSION_INFO = "java.security.Permission \"$PERMISSION_NAME\", \"$ACTIONS\"" as String
  static def FILE_PERMISSION_INFO = "java.io.FilePermission \"$FILENAME\", \"$ACTIONS\"" as String
  static def SERVICE_PERMISSION_INFO = "$SERVICE_PERMISSION_CLASSNAME \"$PERMISSION_NAME\", \"$ACTIONS\"" as String
  static def SERVICE_PERMISSION_INFOS = ["$SERVICE_PERMISSION_CLASSNAME \"service1\", \"$ACTIONS\"" as String, "$SERVICE_PERMISSION_CLASSNAME \"service2\", \"$ACTIONS\"" as String] as Set<String>
  static def SERVICE_PERMISSION_GET_INFO1 = "$SERVICE_PERMISSION_CLASSNAME \"service1\", \"get\"" as String
  static def SERVICE_PERMISSION_GET_INFO2 = "$SERVICE_PERMISSION_CLASSNAME \"service2\", \"get\"" as String
  static def SERVICE_PERMISSION_ALL_GET_INFO = "$SERVICE_PERMISSION_CLASSNAME \"*\", \"get\"" as String
  static def SERVICE_PERMISSION_GET_INFOS = [SERVICE_PERMISSION_GET_INFO1, SERVICE_PERMISSION_GET_INFO2] as Set<String>
  static def SERVICE_PERMISSION_ALL_GET_INFOS = [SERVICE_PERMISSION_ALL_GET_INFO] as Set<String>
  static def PERMISSION_INFOS = [PERMISSION_INFO, FILE_PERMISSION_INFO] as Set<String>

  @Shared
  def DOMAIN_CLASS = MockClassType('DOMAIN_CLASS', 'Ljava/security/ProtectionDomain;')
  @Shared
  def SERVICE_CLASS = MockClassType('SERVICE_CLASS', 'Lorg/osgi/framework/ServiceReference;')
  @Shared
  def PERMISSION_CLASS = MockClassType('PERMISSION_CLASS', 'Ljava/security/Permission;')
  @Shared
  def FILE_PERMISSION_CLASS = MockClassType('FILE_PERMISSION_CLASS', 'Ljava/io/FilePermission;', superclass: PERMISSION_CLASS)
  @Shared
  def SERVICE_PERMISSION_CLASS = MockClassType('SERVICE_PERMISSION_CLASS', 'Lorg/osgi/framework/ServicePermission;', superclass: PERMISSION_CLASS)
  @Shared
  def SERVICE_EVENT_CLASS = MockClassType('SERVICE_EVENT', 'Lorg/osgi/framework/ServiceEvent;')
  @Shared
  def SERVICE_REFERENCE_CLASS = MockClassType('SERVICE_REFERENCE_CLASS', 'Lorg/osgi/framework/ServiceReference;')

  @Shared
  def SERVICE_REFERENCE = MockObjectReference('SERVICE_REFERENCE', SERVICE_EVENT_CLASS, 'getProperty()': [args: ['objectClass'], result: OBJ_CLASS])
  @Shared
  def SERVICE_EVENT = MockObjectReference('SERVICE_EVENT', SERVICE_REFERENCE_CLASS, getServiceReference: SERVICE_REFERENCE)
  @Shared
  def PERMISSION = MockObjectReference('PERMISSION', PERMISSION_CLASS, getActions: ACTIONS, getName: PERMISSION_NAME)
  @Shared
  def FILE_PERMISSION = MockObjectReference('FILE_PERMISSION', FILE_PERMISSION_CLASS, getActions: ACTIONS, getName: FILENAME)
  @Shared
  def SERVICE_PERMISSION = MockObjectReference('SERVICE_PERMISSION', SERVICE_PERMISSION_CLASS, newInstance: [SERVICE_REFERENCE, ACTIONS], getActions: ACTIONS, objectClass: OBJ_CLASS, getName: PERMISSION_NAME)
  @Shared
  def SERVICE_PERMISSION_WITH_NO_OBJ_CLASS = MockObjectReference('SERVICE_PERMISSION_WITH_NO_OBJ_CLASS', SERVICE_PERMISSION_CLASS, getActions: ACTIONS, objectClass: null, getName: PERMISSION_NAME)

  @Shared
  def DOMAIN = MockObjectReference('DOMAIN1', DOMAIN_CLASS)
  @Shared
  def DOMAIN_WITH_PERMISSION = MockObjectReference('DOMAIN_WITH_PERMISSION', DOMAIN_CLASS, 'implies()': [args: [PERMISSION], result: true])
  @Shared
  def DOMAIN_WITHOUT_PERMISSION = MockObjectReference('DOMAIN_WITHOUT_PERMISSION', DOMAIN_CLASS, 'implies()': [args: [PERMISSION], result: false])

  @Shared
  def SERVICE = MockObjectReference('SERVICE', SERVICE_CLASS)

  @Unroll
  def "test implies() with #with_what where the domain is #is_what"() {
    given:
      def context = Mock(DebugContext)
      def debug = Mock(Debug)

    when:
      def result = new PermissionUtil(debug).implies(LOCATION, permissions)

    then:
      result == implied

    and:
      1 * debug.context() >> context
      if (permissions instanceof Set) {
        1 * context.hasPermissions(LOCATION, permissions) >> implied
      } else {
        1 * context.hasPermission(LOCATION, permissions) >> implied
      }

    where:
      with_what            | is_what                              || permissions      | implied
      'a permission'       | 'granted the permission'             || PERMISSION_INFO  | true
      'a permission'       | 'not granted the permission'         || PERMISSION_INFO  | false
      'set of permissions' | 'granted one of the permissions'     || PERMISSION_INFOS | true
      'set of permissions' | 'not granted any of the permissions' || PERMISSION_INFOS | false
  }

  @Unroll
  def "test implies() with no backdoor where the domain is #is_what"() {
    given:
      def backdoor = Mock(Backdoor)
      def debug = Mock(Debug)

    when:
      def result = new PermissionUtil(debug).implies(domain, PERMISSION)

    then:
      result == implied

    and:
      1 * debug.backdoor() >> backdoor
      1 * backdoor.hasPermission(*_) >> { throw new IllegalStateException() }
      1 * debug.reflection() >> REFLECTION

    where:
      is_what                      || domain                    || implied
      'granted the permission'     || DOMAIN_WITH_PERMISSION    || true
      'not granted the permission' || DOMAIN_WITHOUT_PERMISSION || false
  }

  @Unroll
  def "test implies() with no backdoor where the domain failed with Error"() {
    given:
      def backdoor = Mock(Backdoor)
      def reflection = Mock(ReflectionUtil)
      def debug = Mock(Debug)

    when:
      def result = new PermissionUtil(debug).implies(DOMAIN_WITH_PERMISSION, PERMISSION)

    then:
      !result

    and:
      1 * debug.backdoor() >> backdoor
      1 * backdoor.hasPermission(*_) >> { throw new IllegalStateException() }
      1 * debug.reflection() >> reflection
      1 * reflection.invoke(*_) >> { throw new Error() }
  }

  def "test implies() with no backdoor where the domain failed with OutOfMemoryError"() {
    given:
      def backdoor = Mock(Backdoor)
      def reflection = Mock(ReflectionUtil)
      def debug = Mock(Debug)

    when:
      new PermissionUtil(debug).implies(DOMAIN_WITH_PERMISSION, PERMISSION)

    then:
      thrown(OutOfMemoryError)

    and:
      1 * debug.backdoor() >> backdoor
      1 * backdoor.hasPermission(*_) >> { throw new IllegalStateException() }
      1 * debug.reflection() >> reflection
      1 * reflection.invoke(*_) >> { throw new OutOfMemoryError() }
  }

  @Unroll
  def "test implies() when provided by backdoor where the domain is #is_what"() {
    given:
      def backdoor = Mock(Backdoor)
      def debug = Mock(Debug)

    when:
      def result = new PermissionUtil(debug).implies(DOMAIN_WITH_PERMISSION, PERMISSION)

    then:
      result == implied

    and:
      1 * debug.backdoor() >> backdoor
      1 * backdoor.hasPermission(debug, DOMAIN_WITH_PERMISSION, PERMISSION) >> implied
      0 * debug.reflection()

    where:
      is_what                      || implied
      'granted the permission'     || true
      'not granted the permission' || false
  }

  @Unroll
  def "test implies() when backdoor failed with #exception.class.simpleName"() {
    given:
      def backdoor = Mock(Backdoor)
      def debug = Mock(Debug)

    when:
      def result = new PermissionUtil(debug).implies(DOMAIN_WITH_PERMISSION, PERMISSION)

    then:
      result

    and:
      1 * debug.backdoor() >> backdoor
      1 * backdoor.hasPermission(debug, DOMAIN_WITH_PERMISSION, PERMISSION) >> { throw exception }
      1 * debug.reflection() >> REFLECTION

    where:
      exception << [new NullPointerException(), new Exception(), new Error()]
  }

  def "test implies() when backdoor failed with OutOfMemoryError"() {
    given:
      def backdoor = Mock(Backdoor)
      def debug = Mock(Debug)
      def exception = new OutOfMemoryError()

    when:
      new PermissionUtil(debug).implies(DOMAIN_WITH_PERMISSION, PERMISSION)

    then:
      def e = thrown(OutOfMemoryError)

    and:
      e.is(exception)

    and:
      1 * debug.backdoor() >> backdoor
      1 * backdoor.hasPermission(debug, DOMAIN_WITH_PERMISSION, PERMISSION) >> { throw exception }
      0 * debug.reflection()
  }

  def "test createServicePermission()"() {
    given:
      def debug = Mock(Debug)

    when:
      def permission = new PermissionUtil(debug).createServicePermission(SERVICE_REFERENCE, ACTIONS)

    then:
      permission == SERVICE_PERMISSION

    and:
      1 * debug.reflection() >> REFLECTION
  }

  @Unroll
  def "test grant() with no backdoor and a set of permissions when #when_what"() {
    given:
      def context = Mock(DebugContext)
      def debug = Mock(Debug)

    when:
      def returnedResult = new PermissionUtil(debug).grant(LOCATION, PERMISSION_INFOS)

    then:
      returnedResult == result

    and:
      2 * debug.context() >> context
      PERMISSION_INFOS.eachWithIndex { permission, i ->
        1 * context.grantPermission(LOCATION, permission) >> granted[i]
      }
      debug.backdoor() >> { throw new IllegalStateException() }

    where:
      when_what                                                    || granted        || result
      'all permissions are granted'                                || [true, true]   || true
      'first permission is granted and second was already granted' || [true, false]  || false
      'first permission is already granted and second is granted'  || [false, true]  || false
      'both permissions are already granted'                       || [false, false] || false
  }

  def "test grant() with a set of permissions when the domain is null"() {
    given:
      def context = Mock(DebugContext)
      def debug = Mock(Debug)

    when:
      def result = new PermissionUtil(debug).grant(null, PERMISSION_INFOS)

    then:
      !result

    and:
      0 * debug.context()
      0 * context.grantPermission(*_)
      0 * debug.backdoor()
  }

  @Unroll
  def "test grant() with no backdoor and a permission when #when_what"() {
    given:
      def context = Mock(DebugContext)
      def debug = Mock(Debug)

    when:
      def result = new PermissionUtil(debug).grant(LOCATION, PERMISSION_INFO)

    then:
      result == granted

    and:
      1 * debug.context() >> context
      1 * context.grantPermission(LOCATION, PERMISSION_INFO) >> granted
      if (granted) {
        1 * debug.isGranting() >> true
        1 * debug.isContinuous() >> true
        1 * debug.backdoor() >> { throw new IllegalStateException() }
      } else {
        0 * debug.backdoor()
      }

    where:
      when_what         || granted
      'granted'         || true
      'already granted' || false
  }

  @Unroll
  def "test grant() when granted, and #and_what, and backdoor is #notified"() {
    given:
      def context = Mock(DebugContext)
      def backdoor = Mock(Backdoor)
      def debug = Mock(Debug)

    when:
      def result = new PermissionUtil(debug).grant(LOCATION, PERMISSION_INFO)

    then:
      result

    and:
      1 * debug.context() >> context
      1 * context.grantPermission(LOCATION, PERMISSION_INFO) >> true
      debug.isGranting() >> granting
      debug.isContinuous() >> continuous
      back_count * debug.backdoor() >> backdoor
      back_count * backdoor.grantPermission(debug, LOCATION, PERMISSION_INFO)

    where:
      and_what                                         || granting | continuous || back_count
      'granting and continuous modes are on'           || true     | true       || 1
      'granting mode is on but continuous mode is off' || true     | false      || 0
      'granting mode is off but continuous mode is on' || false    | true       || 0
      'granting and continuous modes are off'          || false    | false      || 0
  }

  @Unroll
  def "test grant() when backdoor failed with #exception.class.simpleName"() {
    given:
      def context = Mock(DebugContext)
      def backdoor = Mock(Backdoor)
      def debug = Mock(Debug)

    when:
      def result = new PermissionUtil(debug).grant(LOCATION, PERMISSION_INFO)

    then:
      result

    and:
      1 * debug.context() >> context
      1 * context.grantPermission(LOCATION, PERMISSION_INFO) >> true
      1 * debug.isGranting() >> true
      1 * debug.isContinuous() >> true
      1 * debug.backdoor() >> backdoor
      1 * backdoor.grantPermission(debug, LOCATION, PERMISSION_INFO) >> { throw exception }

    where:
      exception << [new NullPointerException(), new Exception(), new Error()]
  }

  def "test grant() when backdoor failed with OutOfMemoryError"() {
    given:
      def context = Mock(DebugContext)
      def backdoor = Mock(Backdoor)
      def debug = Mock(Debug)
      def exception = new OutOfMemoryError()

    when:
      new PermissionUtil(debug).grant(LOCATION, PERMISSION_INFO)

    then:
      def e = thrown(OutOfMemoryError)

    and:
      e.is(exception)

    and:
      1 * debug.context() >> context
      1 * context.grantPermission(LOCATION, PERMISSION_INFO) >> true
      1 * debug.isGranting() >> true
      1 * debug.isContinuous() >> true
      1 * debug.backdoor() >> backdoor
      1 * backdoor.grantPermission(debug, LOCATION, PERMISSION_INFO) >> { throw exception }
  }

  @Unroll
  def "test getServicePermissionStrings() with #with_what"() {
    given:
      def debug = Stub(Debug)

    expect:
      new PermissionUtil(debug).getServicePermissionStrings(service_classes as String[], ACTIONS) == result as Set

    where:
      with_what              || service_classes || result
      'null service classes' || null            || ["$SERVICE_PERMISSION_CLASSNAME \"*\", \"$ACTIONS\""]
      'one service class'    || ['a']           || ["$SERVICE_PERMISSION_CLASSNAME \"a\", \"$ACTIONS\""]
      'two service classes'  || ['a', 'b']      || ["$SERVICE_PERMISSION_CLASSNAME \"a\", \"$ACTIONS\"", "$SERVICE_PERMISSION_CLASSNAME \"b\", \"$ACTIONS\""]
  }

  @Unroll
  def "test getPermissionStrings() with no backdoor and with #with_what"() {
    given:
      def properties = Mock(SystemProperties)
      def debug = Mock(Debug)

    when:
      def returnedResult = new PermissionUtil(debug).getPermissionStrings(permission)

    then:
      returnedResult == result as Set<String>

    and:
      1 * debug.backdoor() >> { throw new IllegalStateException() }
      1 * debug.reflection() >> REFLECTION
      props_count * debug.properties() >> properties
      props_count * properties.compress(debug, _) >> { it[1] }

    where:
      with_what                                       || permission                           || props_count || result
      'a permission'                                  || PERMISSION                           || 0           || [PERMISSION_INFO]
      'a file permission'                             || FILE_PERMISSION                      || 1           || [FILE_PERMISSION_INFO]
      'a service permission that has an object class' || SERVICE_PERMISSION                   || 0           || SERVICE_PERMISSION_INFOS
      'a service permission that has no object class' || SERVICE_PERMISSION_WITH_NO_OBJ_CLASS || 0           || [SERVICE_PERMISSION_INFO]
  }

  def "test getPermissionStrings() when provided by backdoor"() {
    given:
      def backdoor = Mock(Backdoor)
      def debug = Mock(Debug)

    when:
      def result = new PermissionUtil(debug).getPermissionStrings(SERVICE_PERMISSION)

    then:
      result == SERVICE_PERMISSION_INFOS

    and:
      1 * debug.backdoor() >> backdoor
      1 * backdoor.getPermissionStrings(debug, SERVICE_PERMISSION) >> SERVICE_PERMISSION_INFOS
      0 * debug.reflection()
      0 * debug.properties()
  }

  @Unroll
  def "test getPermissionStrings() when backdoor failed with #exception.class.simpleName"() {
    given:
      def backdoor = Mock(Backdoor)
      def debug = Mock(Debug)

    when:
      def result = new PermissionUtil(debug).getPermissionStrings(PERMISSION)

    then:
      result == [PERMISSION_INFO] as Set<String>

    and:
      1 * debug.backdoor() >> backdoor
      1 * backdoor.getPermissionStrings(debug, PERMISSION) >> { throw exception }
      1 * debug.reflection() >> REFLECTION
      0 * debug.properties()

    where:
      exception << [new NullPointerException(), new Exception(), new Error()]
  }

  def "test getPermissionStrings() when backdoor failed with OutOfMemoryError"() {
    given:
      def backdoor = Mock(Backdoor)
      def debug = Mock(Debug)
      def exception = new OutOfMemoryError()

    when:
      new PermissionUtil(debug).getPermissionStrings(PERMISSION)

    then:
      def e = thrown(OutOfMemoryError)

    and:
      e.is(exception)

    and:
      1 * debug.backdoor() >> backdoor
      1 * backdoor.getPermissionStrings(debug, PERMISSION) >> { throw exception }
      0 * debug.reflection()
      0 * debug.properties()
  }

  @Unroll
  def "test getServiceProperty() when #when_what"() {
    given:
      def debug = Mock(Debug)
      def cache = Mock(Map)

    when:
      def result = new PermissionUtil(debug).getServiceProperty(SERVICE_REFERENCE, 'objectClass')

    then:
      result == value

    and:
      1 * debug.reflection() >> REFLECTION
      1 * debug.computeIfAbsent(PermissionUtil.SERVICE_PROPERTY_CACHE, _) >> cache
      1 * cache.computeIfAbsent(SERVICE_REFERENCE, _) >> map

    where:
      when_what                              || map                                      || value
      'already cached with property'         || [objectClass: ['something'] as String[]] || ['something'] as String[]
      'a map is cached without the property' || [name: 'something']                      || OBJ_CLASS
      'no map is cached'                     || [:]                                      || OBJ_CLASS
  }

  @Unroll
  def "test findMissingServicePermissionStrings() with no backdoor and #and_what"() {
    given:
      def permission = Mock(ObjectReference)
      def debug = Mock(Debug)
      def cache = [:]
      def util = Spy(PermissionUtil, constructorArgs: [debug])

    when:
      def returnedResult = util.findMissingServicePermissionStrings(BUNDLE, DOMAIN, SERVICE_EVENT)

    then:
      returnedResult == result as Set<String>

    and:
      debug.reflection() >> REFLECTION
      debug.computeIfAbsent(PermissionUtil.SERVICE_PROPERTY_CACHE, _) >> cache
      1 * debug.backdoor() >> { throw new IllegalStateException() }
      1 * util.implies(BUNDLE, SERVICE_PERMISSION_ALL_GET_INFOS) >> implies_all
      if (implies_all) {
        0 * util.implies(*_)
        0 * util.createServicePermission(*_)
        0 * util.grant(*_)
      } else {
        1 * util.implies(BUNDLE, SERVICE_PERMISSION_GET_INFOS) >> implies_infos
        if (implies_infos) {
          0 * util.createServicePermission(*_)
          0 * util.implies(*_)
          0 * util.grant(*_)
        } else {
          1 * util.createServicePermission(SERVICE_REFERENCE, 'get') >> permission
          1 * permission.enableCollection()
          // verify with the VM if the DOMAIN1 object has the re-created service permission
          1 * util.implies(DOMAIN, permission) >> implies_ref
          if (implies_obj) {
            // only report as granted some of the permissions as configured in implies_obj
            (SERVICE_PERMISSION_GET_INFOS.size()) * util.implies(BUNDLE, _) >> {
              it[1] != implies_obj
            }
          }
          // we should only be granting the permissions that were missing and that we are about to return
          1 * util.grant(BUNDLE, result) >> true
        }
      }

    where:
      and_what                                                                                      || implies_all | implies_infos | implies_ref | implies_obj                  || result
      'bundle was already granted * service permission'                                             || true        | true          | true        | null                         || []
      'bundle was not granted * but was already granted all services permissions'                   || false       | true          | true        | null                         || []
      'bundle was not granted * and any of the services permissions'                                || false       | false         | true        | null                         || SERVICE_PERMISSION_GET_INFOS
      'bundle was not granted * but does have all the services permissions where some were granted' || false       | false         | false       | SERVICE_PERMISSION_GET_INFO1 || SERVICE_PERMISSION_GET_INFOS - SERVICE_PERMISSION_GET_INFO1
  }

  @Unroll
  def "test findMissingServicePermissionStrings() when provided by backdoor and #and_what"() {
    given:
      def backdoor = Mock(Backdoor)
      def context = Mock(DebugContext)
      def debug = Mock(Debug)
      def util = Spy(PermissionUtil, constructorArgs: [debug])

    when:
      def returnedResult = util.findMissingServicePermissionStrings(BUNDLE, DOMAIN, SERVICE_EVENT)

    then:
      returnedResult == result as Set<String>

    and:
      debug.isContinuous() >> continuous
      debug.isGranting() >> granting
      debug.context() >> context
      1 * debug.backdoor() >> backdoor
      1 * backdoor.getServicePermissionInfoAndGrant(debug, BUNDLE, DOMAIN, SERVICE_EVENT, granting) >> new ServicePermissionInfo(permission_infos.clone(), implies, implied as Set<String>)
      // we might or might not be caching new granted permissions, we don't care which one as long as they are associated with bundle
      // that being said, if we are granting then we should make sure that all permission_infos are granted
      if (granting && continuous) {
        permission_infos.each {
          1 * context.grantPermission(BUNDLE, it) >> true
        }
      }
      // this is for anything else returned in 'implied'
      (0.._) * context.grantPermission(BUNDLE, _) >> true
      0 * context.grantPermission(_, _)
      0 * util.implies(*_)

    where:
      and_what                                || continuous | granting | permission_infos             | implies | implied                                                        || result
      'we have all permissions'               || false      | false    | SERVICE_PERMISSION_GET_INFOS | true    | SERVICE_PERMISSION_GET_INFOS + SERVICE_PERMISSION_ALL_GET_INFO || []
      'we have no permissions'                || false      | true     | SERVICE_PERMISSION_GET_INFOS | false   | []                                                             || SERVICE_PERMISSION_GET_INFOS
      'we are missing some permissions'       || true       | true     | SERVICE_PERMISSION_GET_INFOS | false   | [SERVICE_PERMISSION_GET_INFO1]                                 || [SERVICE_PERMISSION_GET_INFO2]
      'we are missing some other permissions' || false      | false    | SERVICE_PERMISSION_GET_INFOS | false   | [SERVICE_PERMISSION_GET_INFO2]                                 || [SERVICE_PERMISSION_GET_INFO1]
  }

  @Unroll
  def "test findMissingServicePermissionStrings() when backdoor failed with #exception.class.simpleName"() {
    given:
      def backdoor = Mock(Backdoor)
      def debug = Mock(Debug)
      def util = Spy(PermissionUtil, constructorArgs: [debug])

    when:
      def result = util.findMissingServicePermissionStrings(BUNDLE, DOMAIN, SERVICE_EVENT)

    then:
      result.isEmpty()

    and:
      debug.isContinuous() >> true
      debug.isGranting() >> true
      debug.backdoor() >> backdoor
      1 * backdoor.getServicePermissionInfoAndGrant(debug, BUNDLE, DOMAIN, SERVICE_EVENT, true) >> {
        throw exception
      }
      1 * util.implies(BUNDLE, SERVICE_PERMISSION_ALL_GET_INFOS) >> true

    where:
      exception << [new NullPointerException(), new Exception(), new Error()]
  }

  def "test findMissingServicePermissionStrings() when backdoor failed with OutOfMemoryError"() {
    given:
      def backdoor = Mock(Backdoor)
      def debug = Mock(Debug)
      def util = Spy(PermissionUtil, constructorArgs: [debug])
      def exception = new OutOfMemoryError()

    when:
      util.findMissingServicePermissionStrings(BUNDLE, DOMAIN, SERVICE_EVENT)

    then:
      def e = thrown(OutOfMemoryError)

    and:
      e.is(exception)

    and:
      debug.isContinuous() >> true
      debug.isGranting() >> true
      debug.backdoor() >> backdoor
      1 * backdoor.getServicePermissionInfoAndGrant(debug, BUNDLE, DOMAIN, SERVICE_EVENT, true) >> {
        throw exception
      }
      0 * util.implies(*_)
  }
}
