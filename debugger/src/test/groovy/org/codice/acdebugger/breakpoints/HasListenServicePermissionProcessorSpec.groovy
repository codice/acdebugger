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

import com.sun.jdi.BooleanValue
import com.sun.jdi.EnhancedStackFrame
import com.sun.jdi.LocalVariable
import com.sun.jdi.Location
import com.sun.jdi.ObjectReference
import com.sun.jdi.ThreadReference
import com.sun.jdi.VirtualMachine
import com.sun.jdi.request.BreakpointRequest
import com.sun.jdi.request.EventRequestManager
import org.codice.acdebugger.api.BundleUtil
import org.codice.acdebugger.api.Debug
import org.codice.acdebugger.api.PermissionUtil
import org.codice.acdebugger.api.ReflectionUtil
import org.codice.acdebugger.impl.BreakpointInfo
import org.codice.acdebugger.impl.BreakpointLocation
import org.codice.acdebugger.impl.DebugContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class HasListenServicePermissionProcessorSpec extends Specification {
  static def BUNDLE = 'bundle'
  static def EMPTY_PERMISSIONS = [] as Set<String>
  static def PERMISSIONS = ['p1', 'p2'] as Set<String>

  @Shared
  def DOMAIN = Stub(ObjectReference)
  @Shared
  def SERVICE_EVENT = Stub(ObjectReference)

  @Shared
  def TRUE = Mock(BooleanValue)

  def "test locations() is within the ServiceRegistry class"() {
    when:
      def locations = new HasListenServicePermissionProcessor().locations().toArray()

    then:
      locations.length == 1
      locations[0].className == 'org.eclipse.osgi.internal.serviceregistry.ServiceRegistry'
      locations[0].classSignature == 'Lorg/eclipse/osgi/internal/serviceregistry/ServiceRegistry;'
      locations[0].method == null
      locations[0].lineNumber == 1096
  }

  def "test createRequest() creates a breakpoint request at the provided location"() {
    given:
      def location = Stub(Location)
      def breakpointLocation = Mock(BreakpointLocation) {
        getLocation() >> location
      }
      def breakpointRequest = Stub(BreakpointRequest)
      def erm = Mock(EventRequestManager)
      def debug = Mock(Debug) {
        eventRequestManager() >> erm
      }

    when:
      def req = new HasListenServicePermissionProcessor().createRequest(debug, breakpointLocation)

    then:
      req == breakpointRequest

    and:
      1 * erm.createBreakpointRequest(location) >> breakpointRequest
  }

  @Unroll
  def "test process() with #when_what"() {
    given:
      def thread = Mock(ThreadReference)
      def context = Stub(ObjectReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def bundles = Mock(BundleUtil)
      def permissions = Mock(PermissionUtil)
      def debug = Mock(Debug)

    when:
      new HasListenServicePermissionProcessor().process(Stub(BreakpointInfo), debug)

    then:
      interaction {
        stub(thread, context)
        stub(bundles, context)
        stub(reflection)
        stub(debug, thread, reflection, bundles, permissions, failing, continuous)
      }

    and:
      1 * permissions.findMissingServicePermissionStrings(BUNDLE, DOMAIN, SERVICE_EVENT) >> permissionInfos
      record_count * debug.record({
        (it.getGrantedDomains() == ([BUNDLE] as Set<String>)) && (it.getPermissions() == permissionInfos)
      })
      0 * debug.record(_)
      early_count * thread.forceEarlyReturn(TRUE)

    where:
      when_what                                                                                             || permissionInfos   || failing | continuous || record_count | early_count
      'no missing permissions are detected and the debugger is letting things fail in continuous mode'      || EMPTY_PERMISSIONS || true    | true       || 0            | 0
      'no missing permissions are detected and the debugger is letting things fail after one detection'     || EMPTY_PERMISSIONS || true    | false      || 0            | 0
      'no missing permissions are detected and the debugger is not letting things fail in continuous mode'  || EMPTY_PERMISSIONS || false   | true       || 0            | 1
      'no missing permissions are detected and the debugger is not letting things fail after one detection' || EMPTY_PERMISSIONS || false   | false      || 0            | 0
      'missing permissions are detected and the debugger is letting things fail in continuous mode'         || PERMISSIONS       || true    | true       || 1            | 0
      'missing permissions are detected and the debugger is letting things fail after one detection'        || PERMISSIONS       || true    | false      || 1            | 0
      'missing permissions are detected and the debugger is not letting things fail in continuous mode'     || PERMISSIONS       || false   | true       || 1            | 1
      'missing permissions are detected and the debugger is not letting things fail after one detection'    || PERMISSIONS       || false   | false      || 1            | 0
  }

  private def stub(def debug, def threadReference, def reflectionUtil, def bundleUtil, def permissionUtil, def failing, def continuous) {
    with(debug) {
      thread() >> threadReference
      reflection() >> reflectionUtil
      bundles() >> bundleUtil
      permissions() >> permissionUtil
      isFailing() >> failing
      isContinuous() >> continuous
    }
  }

  private def stub(BundleUtil bundles, ObjectReference context) {
    bundles.get(context) >> BUNDLE
  }

  private def stub(ReflectionUtil reflection) {
    reflection.toMirror(true) >> TRUE
  }

  private def stub(ThreadReference thread, ObjectReference context) {
    def domain = Stub(LocalVariable)

    with(thread) {
      frame(0) >> Mock(EnhancedStackFrame) {
        getArgumentValues() >> Mock(List) {
          get(1) >> context
          get(0) >> SERVICE_EVENT
        }
        visibleVariableByName('domain') >> domain
        getValue(domain) >> DOMAIN
      }
    }
  }
}
