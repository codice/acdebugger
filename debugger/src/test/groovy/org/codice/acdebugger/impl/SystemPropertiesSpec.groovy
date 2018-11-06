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
import org.codice.acdebugger.common.PropertiesUtil
import spock.lang.Shared

import java.util.stream.Collectors
import java.util.stream.Stream

class SystemPropertiesSpec extends ReflectionSpecification {
  static def PROPERTIES = PropertiesUtil.propertiesNames().collect(Collectors.toList())

  @Shared
  def SYSTEM_CLASS = MockClassType('SYSTEM_CLASS', SystemProperties.CLASS_SIGNATURE)
  @Shared
  def SYSTEM_CLASS0 = MockClassType('SYSTEM_CLASS0', SystemProperties.CLASS_SIGNATURE)
  @Shared
  def SYSTEM_OBJ = MockObjectReference('SYSTEM_OBJ', SYSTEM_CLASS)
  @Shared
  def GET_PROPERTY = Mock(Method)

  def "test init() with a system reference"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def system = new SystemProperties()

    when:
      system.init(Mock(Debug) {
        reflection() >> reflectionUtil
      }, SYSTEM_OBJ)

    then:
      1 * reflectionUtil.findMethod(SYSTEM_CLASS, 'getProperty', SystemProperties.METHOD_SIGNATURE_STRING_ARG_STRING_RESULT) >> GET_PROPERTY
      PROPERTIES.each {
        1 * reflectionUtil.invoke(SYSTEM_OBJ, GET_PROPERTY, it) >> 'value-' + it
      }
  }

  def "test init() with no system reference and a system discovered"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def system = new SystemProperties()

    when:
      def initialized = system.init(Mock(Debug) {
        reflection() >> reflectionUtil
      })

    then:
      initialized

    and:
      1 * reflectionUtil.classes(SystemProperties.CLASS_SIGNATURE) >> Stream.of(SYSTEM_CLASS0, SYSTEM_CLASS)
      1 * reflectionUtil.invokeStatic(SYSTEM_CLASS0, 'getProperties', '()Ljava/util/Properties;') >> null
      1 * reflectionUtil.invokeStatic(SYSTEM_CLASS, 'getProperties', '()Ljava/util/Properties;') >> SYSTEM_OBJ
      1 * reflectionUtil.findMethod(SYSTEM_CLASS, 'getProperty', SystemProperties.METHOD_SIGNATURE_STRING_ARG_STRING_RESULT) >> GET_PROPERTY
      PROPERTIES.each {
        1 * reflectionUtil.invoke(SYSTEM_OBJ, GET_PROPERTY, it) >> 'value-' + it
      }
  }

  def "test init() with no system reference and no system discovered"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def system = new SystemProperties()

    when:
      def initialized = system.init(Mock(Debug) {
        reflection() >> reflectionUtil
      })

    then:
      !initialized

    and:
      1 * reflectionUtil.classes(SystemProperties.CLASS_SIGNATURE) >> Stream.of(SYSTEM_CLASS0, SYSTEM_CLASS)
      1 * reflectionUtil.invokeStatic(SYSTEM_CLASS0, 'getProperties', '()Ljava/util/Properties;') >> null
      1 * reflectionUtil.invokeStatic(SYSTEM_CLASS, 'getProperties', '()Ljava/util/Properties;') >> null
      0 * reflectionUtil.findMethod(SYSTEM_CLASS, 'getProperty', SystemProperties.METHOD_SIGNATURE_STRING_ARG_STRING_RESULT)
      0 * reflectionUtil.invoke(_, GET_PROPERTY, _)
  }

  def "test init() with no system reference and unable to get system"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def system = new SystemProperties()

    when:
      def initialized = system.init(Mock(Debug) {
        reflection() >> reflectionUtil
      })

    then:
      !initialized

    and:
      1 * reflectionUtil.classes(SystemProperties.CLASS_SIGNATURE) >> Stream.of(SYSTEM_CLASS)
      1 * reflectionUtil.invokeStatic(SYSTEM_CLASS, 'getProperties', '()Ljava/util/Properties;') >> {
        throw new Exception('testing')
      }
      0 * reflectionUtil.findMethod(SYSTEM_CLASS, 'getProperty', SystemProperties.METHOD_SIGNATURE_STRING_ARG_STRING_RESULT)
      0 * reflectionUtil.invoke(_, GET_PROPERTY, _)
  }

  def "test compress() when already initialized"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        reflection() >> reflectionUtil
      }
      def system = new SystemProperties()

    when:
      system.init(debug, SYSTEM_OBJ)

    then:
      1 * reflectionUtil.findMethod(SYSTEM_CLASS, 'getProperty', SystemProperties.METHOD_SIGNATURE_STRING_ARG_STRING_RESULT) >> GET_PROPERTY
      PROPERTIES.each {
        1 * reflectionUtil.invoke(SYSTEM_OBJ, GET_PROPERTY, it) >> 'value-' + it
      }

    when:
      def returnedResult = system.compress(debug, 'value-/ value-java.home value-/')

    then:
      returnedResult == '${/} ${java.home} ${/}'
  }

  def "test compress() when not initialized"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        reflection() >> reflectionUtil
      }
      def system = new SystemProperties()

    when:
      def returnedResult = system.compress(debug, 'value-/ value-java.home value-/')

    then:
      returnedResult == '${/} ${java.home} ${/}'

    and:
      1 * reflectionUtil.classes(SystemProperties.CLASS_SIGNATURE) >> Stream.of(SYSTEM_CLASS0, SYSTEM_CLASS)
      1 * reflectionUtil.invokeStatic(SYSTEM_CLASS0, 'getProperties', '()Ljava/util/Properties;') >> null
      1 * reflectionUtil.invokeStatic(SYSTEM_CLASS, 'getProperties', '()Ljava/util/Properties;') >> SYSTEM_OBJ
      1 * reflectionUtil.findMethod(SYSTEM_CLASS, 'getProperty', SystemProperties.METHOD_SIGNATURE_STRING_ARG_STRING_RESULT) >> GET_PROPERTY
      PROPERTIES.each {
        1 * reflectionUtil.invoke(SYSTEM_OBJ, GET_PROPERTY, it) >> 'value-' + it
      }
  }

  def "test compress() when not initialized and failing to find a system"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        reflection() >> reflectionUtil
      }
      def system = new SystemProperties()

    when:
      system.compress(debug, 'value-/ value-java.home value-/')

    then:
      def e = thrown(IllegalStateException)

      e.message.contains('system properties is not initialized yet')

    and:
      1 * reflectionUtil.classes(SystemProperties.CLASS_SIGNATURE) >> Stream.of(SYSTEM_CLASS)
      1 * reflectionUtil.invokeStatic(SYSTEM_CLASS0, 'getProperties', '()Ljava/util/Properties;') >> null
      0 * reflectionUtil.findMethod(SYSTEM_CLASS, *_)
      0 * reflectionUtil.invoke(_, GET_PROPERTY, _)
  }

  def "test compress() when not initialized and failing to get the system"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        reflection() >> reflectionUtil
      }
      def system = new SystemProperties()

    when:
      system.compress(debug, 'value-/ value-java.home value-/')

    then:
      def e = thrown(IllegalStateException)

      e.message.contains('system properties is not initialized yet')

    and:
      1 * reflectionUtil.classes(SystemProperties.CLASS_SIGNATURE) >> Stream.of(SYSTEM_CLASS)
      1 * reflectionUtil.invokeStatic(SYSTEM_CLASS0, 'getProperties', '()Ljava/util/Properties;') >> {
        throw new NullPointerException('testing')
      }
      0 * reflectionUtil.findMethod(SYSTEM_CLASS, *_)
      0 * reflectionUtil.invoke(_, GET_PROPERTY, _)
  }
}
