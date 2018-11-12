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

import java.util.stream.Collectors

class PropertiesUtilSpec extends Specification {
  def "test propertyNames() reports all configured properties in the same order"() {
    expect:
      PropertiesUtil.propertiesNames().collect(Collectors.toList()) == getClass().getResource('/properties.txt').readLines().findAll {
        !(it.isEmpty() || it =~ /#.*/)
      }
  }

  @Unroll
  def "test compress() with property #property when defined"() {
    given:
      def properties = new Properties()

      properties.put(property, 'value')
      def util = new PropertiesUtil(properties)

    when:
      def result = util.compress('a value/value/Value')

    then:
      result == "a \${$property}/\${$property}/Value"

    where:
      property << getClass().getResource('/properties.txt').readLines().findAll {
        !(it.isEmpty() || it =~ /#.*/)
      }
  }

  def "test compress() with properties not defined"() {
    given:
      def properties = new Properties()
      def util = new PropertiesUtil(properties)

    when:
      def result = util.compress('a value/value/Value')

    then:
      result == 'a value/value/Value'
  }

  def "test compress() with properties defined as empty"() {
    given:
      def properties = new Properties()

      getClass().getResource('/properties.txt').readLines().findAll {
        !(it.isEmpty() || it =~ /#.*/)
      }.forEach { properties.put(it, 'null') }
      def util = new PropertiesUtil(properties)

    when:
      def result = util.compress('a value/value/Value')

    then:
      result == 'a value/value/Value'
  }

  def "test compress() does it in the configured order"() {
    given:
      def properties = new Properties()

      properties.put('ddf.home', '/projects/ddf')
      properties.put('ddf.home.perm', '/projects/ddf/')
      properties.put('/', '/')
      def util = new PropertiesUtil(properties)

    when:
      def result = util.compress('/projects/ddf/etc/config.cfg')

    then:
      result == '${ddf.home.perm}etc${/}config.cfg'
  }
}
