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

import com.sun.jdi.ClassType
import com.sun.jdi.Location
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class BreakpointLocationSpec extends Specification {
  static def SIGNATURE = 'Ljava/lang/String;'
  static def METHOD = 'toString'
  static def LINE_NUM = 1234

  @Shared
  def LOCATION = Stub(Location)

  @Unroll
  def "test constructor with #with_what"() {
    when:
      def info = new BreakpointLocation(SIGNATURE, method, LINE_NUM)

    then:
      info.classSignature == SIGNATURE
      info.method == method
      info.lineNumber == LINE_NUM

    where:
      with_what   || method
      'a method'  || METHOD
      'no method' || null
  }

  def "test getClassName()"() {
    given:
      def info = new BreakpointLocation(SIGNATURE, METHOD, LINE_NUM)

    expect:
      info.className == String.name
  }

  def "test getClassReference() if not set yet"() {
    given:
      def info = new BreakpointLocation(SIGNATURE, METHOD, LINE_NUM)

    when:
      info.classReference

    then:
      def e = thrown(IllegalStateException)

      e.message.contains('not available')
  }

  def "test getClassReference() if set"() {
    given:
      def clazz = Stub(ClassType)
      def info = new BreakpointLocation(SIGNATURE, METHOD, LINE_NUM)

      info.classReference = clazz

    expect:
      info.classReference == clazz
  }

  @Unroll
  def "test getLocation() with #with_what"() {
    given:
      def clazz = Mock(ClassType)
      def info = new BreakpointLocation(SIGNATURE, METHOD, line_num)

      info.classReference = clazz

    when:
      def result = info.location

    then:
      result == location

    and:
      locations_count * clazz.locationsOfLine(line_num) >> [location, Stub(Location)]

    where:
      with_what             || line_num || locations_count || location
      'with a line number'  || LINE_NUM || 1               || LOCATION
      'with no line number' || -1       || 0               || null
  }

  @Unroll
  def "test getLocation() if class reference not set yet"() {
    given:
      def info = new BreakpointLocation(SIGNATURE, METHOD, LINE_NUM)

    expect:
      info.location == null
  }

  @Unroll
  def "test toString() with #with_what"() {
    given:
      def info = new BreakpointLocation(SIGNATURE, method, line_num)

    expect:
      info.toString() == result

    where:
      with_what                      || method | line_num || result
      'a method and a line number'   || METHOD | LINE_NUM || 'java.lang.String.toString():1234'
      'a method and no line number'  || METHOD | -1       || 'java.lang.String.toString()'
      'a line number and no method'  || null   | LINE_NUM || 'java.lang.String:1234'
      'no method and no line number' || null   | -1       || 'java.lang.String'
  }
}
