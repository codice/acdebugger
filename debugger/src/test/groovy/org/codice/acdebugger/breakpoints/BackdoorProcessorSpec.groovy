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

import com.sun.jdi.Location
import com.sun.jdi.ObjectReference
import com.sun.jdi.StackFrame
import com.sun.jdi.ThreadReference
import com.sun.jdi.event.MethodExitEvent
import com.sun.jdi.request.EventRequestManager
import com.sun.jdi.request.MethodExitRequest
import org.codice.acdebugger.api.Debug
import org.codice.acdebugger.impl.Backdoor
import org.codice.acdebugger.impl.BreakpointInfo
import org.codice.acdebugger.impl.BreakpointLocation
import spock.lang.Specification
import spock.lang.Unroll

class BackdoorProcessorSpec extends Specification {
  def "test locations() is within the Backdoor.start() method"() {
    when:
      def locations = new BackdoorProcessor().locations().toArray()

    then:
      locations.length == 1
      locations[0].className == org.codice.acdebugger.backdoor.Backdoor.class.getName()
      locations[0].classSignature == 'Lorg/codice/acdebugger/backdoor/Backdoor;'
      locations[0].method == 'start'
      locations[0].lineNumber == -1
  }

  @Unroll
  def "test createRequest() #creates a method exit request at the provided location #if_what"() {
    given:
      def location = Stub(Location)
      def breakpointLocation = Mock(BreakpointLocation) {
        getLocation() >> location
      }
      def methodExitRequest = Stub(MethodExitRequest)
      def erm = Mock(EventRequestManager)
      def backdoorUtil = Mock(Backdoor)
      def debug = Mock(Debug) {
        eventRequestManager() >> erm
        backdoor() >> backdoorUtil
      }

    when:
      def req = new BackdoorProcessor().createRequest(debug, breakpointLocation)

    then:
      req == (create_count ? methodExitRequest : null)

    and:
      1 * backdoorUtil.init(debug) >> initialized
      create_count * erm.createMethodExitRequest() >> methodExitRequest

    where:
      creates           | if_what                                      || initialized || create_count
      'creates'         | 'if unable to initialize the backdoor'       || false       || 1
      'does not create' | 'if the backdoor is initialized sucessfully' || true         | 0
  }

  def "test process() will initialize the backdoor and disable the request"() {
    given:
      def obj = Stub(ObjectReference)
      def methodExitRequest = Mock(MethodExitRequest)
      def backdoorUtil = Mock(Backdoor)
      def debug = Mock(Debug) {
        event() >> Mock(MethodExitEvent) {
          thread() >> Mock(ThreadReference) {
            frame(0) >> Mock(StackFrame) {
              thisObject() >> obj
            }
          }
          request() >> methodExitRequest
        }
        backdoor() >> backdoorUtil
      }

    when:
      new BackdoorProcessor().process(Stub(BreakpointInfo), debug)

    then:
      1 * backdoorUtil.init(debug, obj)
      1 * methodExitRequest.disable()
  }
}
