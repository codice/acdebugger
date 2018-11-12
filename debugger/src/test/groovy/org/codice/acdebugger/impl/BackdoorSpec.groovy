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
package org.codice.acdebugger.impl

import com.sun.jdi.Method
import org.codice.acdebugger.ReflectionSpecification
import org.codice.acdebugger.api.Debug
import org.codice.acdebugger.api.ReflectionUtil
import org.codice.acdebugger.breakpoints.HasListenServicePermissionProcessor
import org.codice.acdebugger.common.DomainInfo
import org.codice.acdebugger.common.ServicePermissionInfo
import org.codice.spock.Supplemental
import spock.lang.Shared
import spock.lang.Unroll

import java.util.stream.Stream

@Supplemental
class BackdoorSpec extends ReflectionSpecification {
  static def METHOD_NAMES = ['getBundle', 'getBundleVersion', 'getDomain', 'getDomainInfo', 'getPermissionStrings', 'grantPermission', 'hasPermission', 'getServicePermissionInfoAndGrant']
  static def METHOD_SIGNATURES = [
      getBundle: Backdoor.METHOD_SIGNATURE_OBJ_ARG_STRING_RESULT,
      getBundleVersion: Backdoor.METHOD_SIGNATURE_OBJ_ARG_STRING_RESULT,
      getDomain: Backdoor.METHOD_SIGNATURE_OBJ_ARG_STRING_RESULT,
      getDomainInfo: '(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/String;',
      getPermissionStrings: Backdoor.METHOD_SIGNATURE_OBJ_ARG_STRING_RESULT,
      grantPermission: '(Ljava/lang/String;Ljava/lang/String;)V',
      hasPermission: '(Ljava/lang/Object;Ljava/lang/Object;)Z',
      getServicePermissionInfoAndGrant: '(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Z)Ljava/lang/String;'
  ]

  @Shared
  def METHODS = METHOD_NAMES.collectEntries {
    [(it): Stub(Method, name: it + 'MethodStub')]
  }
  @Shared
  def RESULTS = [
      getBundle: [invoke: 'bundle-name', method: 'bundle-name'],
      getBundleVersion: [invoke: 'bundle-version', method: 'bundle-version'],
      getDomain: [invoke: 'bundle-location', method: 'bundle-location'],
      getDomainInfo: [invoke: '[{"locationString":"file:/location","implies":true}, {"implies":false}]', method: [new DomainInfo('file:/location', true), new DomainInfo(null, false)]],
      getPermissionStrings: [invoke: '["a", "b", "a"]', method: ['a', 'b'] as Set<String>],
      grantPermission: [invoke: null, method: null],
      hasPermission: [invoke: true, method: true],
      getServicePermissionInfoAndGrant: [invoke: '{"permissionStrings":["permission.1"],"implies":true,"implied":["permission.1","permission.2"]}', method: new ServicePermissionInfo(['permission.1'] as Set<String>, true, ['permission.1', 'permission.2'] as Set<String>)]
  ]

  @Shared
  def BACKDOOR_CLASS = MockClassType('BACKDOOR_CLASS', Backdoor.CLASS_SIGNATURE)
  @Shared
  def BACKDOOR_CLASS0 = MockClassType('BACKDOOR_CLASS0', Backdoor.CLASS_SIGNATURE)
  @Shared
  def BACKDOOR_OBJ = MockObjectReference('BACKDOOR_OBJ', BACKDOOR_CLASS)

  @Unroll
  def "test init() with a backdoor reference and #and_what"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        reflection() >> reflectionUtil
        isMonitoringService() >> monitoring
      }
      def backdoor = new Backdoor()

    when:
      backdoor.init(debug, BACKDOOR_OBJ)

    then:
      METHOD_NAMES.each {
        1 * reflectionUtil.findMethod(BACKDOOR_CLASS, it, METHOD_SIGNATURES[it]) >> METHODS[it]
      }
      add_count * debug.add({ it instanceof HasListenServicePermissionProcessor })

    where:
      and_what                             || monitoring || add_count
      'monitoring service permissions'     || true       || 1
      'not monitoring service permissions' || false      || 0
  }

  @Unroll
  def "test init() with no backdoor reference and a backdoor discovered and #and_what"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        reflection() >> reflectionUtil
        isMonitoringService() >> monitoring
      }
      def backdoor = new Backdoor()

    when:
      def initialized = backdoor.init(debug)

    then:
      initialized

    and:
      1 * reflectionUtil.classes(Backdoor.CLASS_SIGNATURE) >> Stream.of(BACKDOOR_CLASS0, BACKDOOR_CLASS)
      1 * reflectionUtil.getStatic(BACKDOOR_CLASS0, 'instance', Backdoor.CLASS_SIGNATURE) >> null
      1 * reflectionUtil.getStatic(BACKDOOR_CLASS, 'instance', Backdoor.CLASS_SIGNATURE) >> BACKDOOR_OBJ
      METHOD_NAMES.each {
        1 * reflectionUtil.findMethod(BACKDOOR_CLASS, it, METHOD_SIGNATURES[it]) >> METHODS[it]
      }
      add_count * debug.add({ it instanceof HasListenServicePermissionProcessor })

    where:
      and_what                             || monitoring || add_count
      'monitoring service permissions'     || true       || 1
      'not monitoring service permissions' || false      || 0
  }

  def "test init() with no backdoor reference and no backdoor discovered"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        reflection() >> reflectionUtil
      }
      def backdoor = new Backdoor()

    when:
      def initialized = backdoor.init(debug)

    then:
      !initialized

    and:
      1 * reflectionUtil.classes(Backdoor.CLASS_SIGNATURE) >> Stream.of(BACKDOOR_CLASS0, BACKDOOR_CLASS)
      1 * reflectionUtil.getStatic(BACKDOOR_CLASS0, 'instance', Backdoor.CLASS_SIGNATURE) >> null
      1 * reflectionUtil.getStatic(BACKDOOR_CLASS, 'instance', Backdoor.CLASS_SIGNATURE) >> null
      0 * reflectionUtil.findMethod(BACKDOOR_CLASS, *_)
  }

  def "test init() with no backdoor reference and unable to get backdoor"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        reflection() >> reflectionUtil
      }
      def backdoor = new Backdoor()

    when:
      def initialized = backdoor.init(debug)

    then:
      !initialized

    and:
      1 * reflectionUtil.classes(Backdoor.CLASS_SIGNATURE) >> Stream.of(BACKDOOR_CLASS)
      1 * reflectionUtil.getStatic(BACKDOOR_CLASS, 'instance', Backdoor.CLASS_SIGNATURE) >> {
        throw new Exception('testing')
      }
      0 * reflectionUtil.findMethod(BACKDOOR_CLASS, *_)
  }

  @Unroll
  def "test #method.simplePrototype when already initialized"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        reflection() >> reflectionUtil
        isMonitoringService() >> false
      }
      def backdoor = new Backdoor()
      def parms = Dummies(method.parameterTypes)

      parms[0] = debug // force our debug as the first parameter

    when:
      backdoor.init(debug, BACKDOOR_OBJ)

    then:
      1 * reflectionUtil.findMethod(BACKDOOR_CLASS, method.name, METHOD_SIGNATURES[method.name]) >> METHODS[method.name]

    when:
      def returnedResult = backdoor."$method.name"(*parms)

    then:
      returnedResult == RESULTS[method.name]['method']

    and:
      1 * reflectionUtil.invoke(BACKDOOR_OBJ, METHODS[method.name], *_) >> { obj, m, parameters ->
        parameters.eachWithIndex { p, i ->
          assert p.is(parms[i + 1]) // skip debug
        }
        RESULTS[method.name]['invoke']
      }
      0 * reflectionUtil.classes(Backdoor.CLASS_SIGNATURE)

    where:
      method << Backdoor.methods.findAll {
        it.name in METHOD_NAMES
      }
  }

  @Unroll
  def "test #method.simplePrototype when not initialized"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        reflection() >> reflectionUtil
        isMonitoringService() >> false
      }
      def backdoor = new Backdoor()
      def parms = Dummies(method.parameterTypes)

      parms[0] = debug // force our debug as the first parameter

    when:
      def returnedResult = backdoor."$method.name"(*parms)

    then:
      returnedResult == RESULTS[method.name]['method']

    and:
      1 * reflectionUtil.classes(Backdoor.CLASS_SIGNATURE) >> Stream.of(BACKDOOR_CLASS)
      1 * reflectionUtil.getStatic(BACKDOOR_CLASS, 'instance', Backdoor.CLASS_SIGNATURE) >> BACKDOOR_OBJ
      1 * reflectionUtil.findMethod(BACKDOOR_CLASS, method.name, METHOD_SIGNATURES[method.name]) >> METHODS[method.name]
      1 * reflectionUtil.invoke(BACKDOOR_OBJ, METHODS[method.name], *_) >> { obj, m, parameters ->
        parameters.eachWithIndex { p, i ->
          assert p.is(parms[i + 1]) // skip debug
        }
        RESULTS[method.name]['invoke']
      }

    where:
      method << Backdoor.methods.findAll {
        it.name in METHOD_NAMES
      }
  }

  @Unroll
  def "test #method.simplePrototype when not initialized and failing to find a backdoor"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        reflection() >> reflectionUtil
        isMonitoringService() >> false
      }
      def backdoor = new Backdoor()
      def parms = Dummies(method.parameterTypes)

      parms[0] = debug // force our debug as the first parameter

    when:
      backdoor."$method.name"(*parms)

    then:
      def e = thrown(IllegalStateException)

      e.message.contains('backdoor is not initialized yet')

    and:
      1 * reflectionUtil.classes(Backdoor.CLASS_SIGNATURE) >> Stream.of(BACKDOOR_CLASS)
      1 * reflectionUtil.getStatic(BACKDOOR_CLASS, 'instance', Backdoor.CLASS_SIGNATURE) >> null
      0 * reflectionUtil.findMethod(BACKDOOR_CLASS, *_)
      0 * reflectionUtil.invoke(BACKDOOR_OBJ, *_)

    where:
      method << Backdoor.methods.findAll {
        it.name in METHOD_NAMES
      }
  }

  @Unroll
  def "test #method.simplePrototype when not initialized and failing to get the backdoor"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        reflection() >> reflectionUtil
        isMonitoringService() >> false
      }
      def backdoor = new Backdoor()
      def parms = Dummies(method.parameterTypes)

      parms[0] = debug // force our debug as the first parameter

    when:
      backdoor."$method.name"(*parms)

    then:
      def e = thrown(IllegalStateException)

      e.message.contains('backdoor is not initialized yet')

    and:
      1 * reflectionUtil.classes(Backdoor.CLASS_SIGNATURE) >> Stream.of(BACKDOOR_CLASS)
      1 * reflectionUtil.getStatic(BACKDOOR_CLASS, 'instance', Backdoor.CLASS_SIGNATURE) >> {
        throw new NullPointerException('testing')
      }
      0 * reflectionUtil.findMethod(BACKDOOR_CLASS, *_)
      0 * reflectionUtil.invoke(BACKDOOR_OBJ, *_)

    where:
      method << Backdoor.methods.findAll {
        it.name in METHOD_NAMES
      }
  }

  @Unroll
  def "test #method.simplePrototype when not supported by backdoor"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        reflection() >> reflectionUtil
        isMonitoringService() >> false
      }
      def backdoor = new Backdoor()
      def parms = Dummies(method.parameterTypes)

      parms[0] = debug // force our debug as the first parameter

    when:
      backdoor."$method.name"(*parms)

    then:
      def e = thrown(IllegalStateException)

      e.message.contains(method.name)
      e.message.contains('not supported')

    and:
      1 * reflectionUtil.classes(Backdoor.CLASS_SIGNATURE) >> Stream.of(BACKDOOR_CLASS)
      1 * reflectionUtil.getStatic(BACKDOOR_CLASS, 'instance', Backdoor.CLASS_SIGNATURE) >> BACKDOOR_OBJ
      1 * reflectionUtil.findMethod(BACKDOOR_CLASS, method.name, METHOD_SIGNATURES[method.name]) >> null
      0 * reflectionUtil.invoke(BACKDOOR_OBJ, *_)

    where:
      method << Backdoor.methods.findAll {
        it.name in METHOD_NAMES
      }
  }
}
