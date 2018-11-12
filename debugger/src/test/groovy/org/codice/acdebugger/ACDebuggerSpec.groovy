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
package org.codice.acdebugger

import org.codice.acdebugger.breakpoints.AccessControlContextCheckProcessor
import org.codice.acdebugger.breakpoints.BackdoorProcessor
import org.codice.acdebugger.impl.Debugger
import spock.lang.Unroll

import java.lang.reflect.Modifier
import java.security.Permission

class ACDebuggerSpec extends spock.lang.Specification {
  def "make sure no non-static fields are defined as final as they are updated directly by picocli"() {
    expect:
      ACDebugger.fields.find {
        !Modifier.isStatic(it.modifiers)
      }.every {
        !Modifier.isFinal(it.modifiers)
      }
  }

  @Unroll
  def "test call() when #when_what"() {
    given:
      def debugger = Mock(Debugger)
      def acd = Spy(ACDebugger)

      acd.init(admin, continuous, debug, granting, failing, service, null, null, null, false, 0L, reconnect, osgi)

    when:
      acd.call()

    then:
      loop_count * acd.attach() >> debugger
      loop_count * debugger.setOSGi(osgi)
      loop_count * debugger.setContinuous(continuous)
      loop_count * debugger.setDebug(debug)
      loop_count * debugger.setGranting(granting)
      loop_count * debugger.setFailing(failing)
      loop_count * debugger.setMonitoringService(service)
      loop_count * debugger.setDoPrivilegedBlocks(!admin)
      loop_count * debugger.add({ it instanceof BackdoorProcessor })
      loop_count * debugger.add({ it instanceof AccessControlContextCheckProcessor })
      loop_count * debugger.loop() >> null >> {
        // stop reconnecting such that the loop will exit
        acd.init(admin, continuous, debug, granting, failing, service, null, null, null, false, 0L, false, osgi)
      }

    where:
      when_what                  || admin | continuous | debug | granting | failing | service | reconnect | osgi  || loop_count
      'all options are enabled'  || true  | true       | true  | true     | true    | true    | true      | true  || 2
      'all options are disabled' || false | false      | false | false    | false   | false   | false     | false || 1
      'admin is enabled'         || true  | false      | false | false    | false   | false   | false     | false || 1
      'continuous is enabled'    || false | true       | false | false    | false   | false   | false     | false || 1
      'debug is enabled'         || false | false      | true  | false    | false   | false   | false     | false || 1
      'granting is enabled'      || false | false      | false | true     | false   | false   | false     | false || 1
      'failing is enabled'       || false | false      | false | false    | true    | false   | false     | false || 1
      'service is enabled'       || false | false      | false | false    | false   | true    | false     | false || 1
      'osgi is enabled'          || false | false      | false | false    | false   | false   | false     | true  || 1
  }

  @Unroll
  def "test call() when reconnect is enabled but not continuous"() {
    given:
      def exception = new SecurityException()
      def acd = Spy(ACDebugger)

      acd.init(false, false, false, false, false, false, null, null, null, false, 0L, true, false)

      System.setSecurityManager(new SecurityManager() {
        @Override
        public void checkExit(int status) {
          assert status == 2
          throw exception
        }

        @Override
        public void checkPermission(Permission perm) {}
      })

    when:
      acd.call()

    then:
      def e = thrown(SecurityException)

      e.is(exception)

    and:
      0 * acd.attach()

    cleanup:
      System.setSecurityManager0(null) // must be done through private method
  }

  def "test attach()"() {
    given:
      def debugger = Mock(Debugger)
      def acd = Spy(ACDebugger)

      acd.init(false, false, false, false, false, false, 'transport', 'host', 'port', false, 1000L, false, false)

    when:
      def d = acd.attach()

    then:
      d == debugger

    and:
      1 * acd.newDebugger() >> debugger
      1 * debugger.attach() >> debugger
  }

  def "test attach() when failing to connect on first try but not waiting"() {
    given:
      def exception = new SecurityException()
      def debugger = Mock(Debugger)
      def acd = Spy(ACDebugger)

      acd.init(false, false, false, false, false, false, 'transport', 'host', 'port', false, 1000L, false, false)

      System.setSecurityManager(new SecurityManager() {
        @Override
        public void checkExit(int status) {
          assert status == 1
          throw exception
        }

        @Override
        public void checkPermission(Permission perm) {}
      })

    when:
      acd.attach()

    then:
      def e = thrown(SecurityException)

      e.is(exception)

    and:
      1 * acd.newDebugger() >> debugger
      1 * debugger.attach() >> { throw new ConnectException('testing') }

    cleanup:
      System.setSecurityManager0(null) // must be done through private method
  }
  
  def "test attach() when failing to connect on first try and waiting"() {
    given:
      def debugger = Mock(Debugger)
      def debugger2 = Mock(Debugger)
      def acd = Spy(ACDebugger)

      acd.init(false, false, false, false, false, false, 'transport', 'host', 'port', true, 1000L, false, false)

    when:
      def d = acd.attach()

    then:
      d == debugger2

    and:
      2 * acd.newDebugger() >>> [debugger, debugger2]
      1 * debugger.attach() >> { throw new ConnectException('testing') }
      1 * acd.sleep() >> null
      1 * debugger2.attach() >> debugger2
  }
}