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

import com.sun.jdi.request.EventRequest
import org.codice.acdebugger.api.BreakpointProcessor
import org.codice.acdebugger.api.Debug
import spock.lang.Specification

class BreakpointInfoSpec extends Specification {
  static def SIGNATURE = 'Ljava/lang/String;'
  static def METHOD = 'toString'
  static def LINE_NUM = 1234
  static def LOCATION = new BreakpointLocation(SIGNATURE, METHOD, LINE_NUM)

  def "test constructor"() {
    when:
      def info = new BreakpointInfo(Stub(EventRequest), Stub(BreakpointProcessor), LOCATION)

    then:
      info.getLocation() == LOCATION
  }

  def "test process()"() {
    given:
      def debug = Stub(Debug)
      def processor = Mock(BreakpointProcessor)
      def info = new BreakpointInfo(Stub(EventRequest), processor, LOCATION)

    when:
      info.process(debug)

    then:
      1 * processor.process(info, debug)
  }

  def "test toString()"() {
    given:
      def info = new BreakpointInfo(Stub(EventRequest), Stub(BreakpointProcessor), LOCATION)

    expect:
      info.toString() == LOCATION.toString()
  }
}
