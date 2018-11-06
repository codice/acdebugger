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

import com.sun.jdi.ArrayReference
import com.sun.jdi.EnhancedStackFrame
import com.sun.jdi.IntegerValue
import com.sun.jdi.Location
import com.sun.jdi.ObjectReference
import com.sun.jdi.ThreadReference
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VoidValue
import com.sun.jdi.request.BreakpointRequest
import com.sun.jdi.request.EventRequestManager
import org.codice.acdebugger.api.Debug
import org.codice.acdebugger.api.PermissionUtil
import org.codice.acdebugger.api.ReflectionUtil
import org.codice.acdebugger.api.SecuritySolution
import org.codice.acdebugger.api.StackFrameInformation
import org.codice.acdebugger.impl.BreakpointInfo
import org.codice.acdebugger.impl.BreakpointLocation
import org.codice.acdebugger.impl.DebugContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.security.AccessControlContext

class AccessControlContextCheckBreakpointProcessorSpec extends Specification {
  static def LOCAL_I = 1

  static def DOMAINS = ['d1', 'd2'] as Set<String>
  static def PERMISSIONS = ['p1', 'p2', 'p3'] as Set<String>

  @Shared
  def STACK = [Stub(StackFrameInformation), Stub(StackFrameInformation), Stub(StackFrameInformation), Stub(StackFrameInformation)]

  @Shared
  def SOLUTION = new SecuritySolution(STACK, [], PERMISSIONS, DOMAINS, [Stub(StackFrameInformation), Stub(StackFrameInformation)])
  @Shared
  def SOLUTION_WITH_NO_DOMAINS = new SecuritySolution(STACK, PERMISSIONS, [] as Set<String>, [Stub(StackFrameInformation)])
  @Shared
  def SOLUTION_WITH_NO_DOMAINS_AND_NO_DO_PRIVILEGED = new SecuritySolution(STACK, PERMISSIONS, [] as Set<String>, [])
  @Shared
  def SOLUTION_WITH_NO_DO_PRIVILEGED = new SecuritySolution(STACK, PERMISSIONS, DOMAINS, [])

  @Shared
  def PERMISSION = Mock(ObjectReference)
  @Shared
  def CONTEXT = Mock(ArrayReference)
  @Shared
  def ACC = Mock(ObjectReference)

  @Shared
  def VOID = Mock(VoidValue)

  def "test locations() is within the AccessControllerContext class"() {
    when:
      def locations = new AccessControlContextCheckProcessor().locations().toArray()

    then:
      locations.length == 1
      locations[0].className == AccessControlContext.name
      locations[0].classSignature == 'Ljava/security/AccessControlContext;'
      locations[0].method == null
      locations[0].lineNumber == 472
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
      def req = new AccessControlContextCheckProcessor().createRequest(debug, breakpointLocation)

    then:
      req == breakpointRequest

    and:
      1 * erm.createBreakpointRequest(location) >> breakpointRequest
  }

  @Unroll
  def "test process() when no failures are detected #when_what"() {
    given:
      def thread = Mock(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def permissions = Mock(PermissionUtil)
      def debug = Mock(Debug)
      def security = Mock(SecurityCheckInformation)
      def processor = Spy(AccessControlContextCheckProcessor)

    when:
      processor.process(Stub(BreakpointInfo), debug)

    then:
      interaction {
        stub(thread)
        stub(reflection)
        stub(debug, thread, reflection, permissions, failing, continuous)
      }
      security.isAcceptable() >> acceptable

    and:
      1 * reflection.get(ACC, 'context', '[Ljava/security/ProtectionDomain;') >> CONTEXT
      1 * processor.process(debug, CONTEXT, LOCAL_I, PERMISSION) >> security
      1 * security.getFailedDomain() >> null
      0 * permissions.grant(*_)
      0 * debug.record(_)
      early_count * thread.forceEarlyReturn(VOID)

    where:
      when_what                                                                                  || acceptable | failing | continuous || early_count
      'because it is acceptable and the debugger is letting things fail in continuous mode'      || true       | true    | true       || 0
      'or acceptable and the debugger is letting things fail in continuous mode'                 || false      | true    | true       || 0
      'because it is acceptable and the debugger is letting things fail after one detection'     || true       | true    | false      || 0
      'or acceptable and the debugger is letting things fail after one detection'                || false      | true    | false      || 0
      'because it is acceptable and the debugger is not letting things fail in continuous mode'  || true       | false   | true       || 0
      'or acceptable and the debugger is not letting things fail in continuous mode'             || false      | false   | true       || 1
      'because it is acceptable and the debugger is not letting things fail after one detection' || true       | false   | false      || 0
      'or acceptable and the debugger is not letting things fail after one detection'            || false      | false   | false      || 0
  }

  @Unroll
  def "test process() when #when_what is detected"() {
    given:
      def thread = Mock(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def permissions = Mock(PermissionUtil)
      def debug = Mock(Debug)
      def security = Mock(SecurityCheckInformation)
      def processor = Spy(AccessControlContextCheckProcessor)

    when:
      processor.process(Stub(BreakpointInfo), debug)

    then:
      interaction {
        stub(thread)
        stub(reflection)
        stub(debug, thread, reflection, permissions, false, true)
      }
      security.isAcceptable() >> acceptable

    and:
      1 * reflection.get(ACC, 'context', '[Ljava/security/ProtectionDomain;') >> CONTEXT
      1 * processor.process(debug, CONTEXT, LOCAL_I, PERMISSION) >> security
      1 * security.getFailedDomain() >> 'd1'
      1 * security.analyze() >> solutions
      if (grant_domains) {
        grant_domains.each {
          1 * permissions.grant({ it in grant_domains }, grant_perms)
        }
      }
      0 * permissions.grant(*_)
      1 * debug.record(security)
      early_count * thread.forceEarlyReturn(VOID)

    where:
      when_what                                                                                        || acceptable | solutions                                       || grant_domains | grant_perms | early_count
      'a failure with more than 1 solutions'                                                           || false      | [SOLUTION, SOLUTION_WITH_NO_DOMAINS]            || []            | []          | 1
      'a failure with no solutions'                                                                    || false      | []                                              || []            | []          | 1
      'a failure with 1 solution that has no privileged blocks and grants to some domains'             || false      | [SOLUTION_WITH_NO_DO_PRIVILEGED]                || DOMAINS       | PERMISSIONS | 1
      'a failure with 1 solution that has privileged blocks and grants to some domains'                || false      | [SOLUTION]                                      || []            | []          | 1
      'a failure with 1 solution that has no privileged blocks and grants to no domains'               || false      | [SOLUTION_WITH_NO_DOMAINS_AND_NO_DO_PRIVILEGED] || []            | []          | 1
      'a failure with 1 solution that has privileged blocks and grants to no domains'                  || false      | [SOLUTION_WITH_NO_DOMAINS]                      || []            | []          | 1
      'an acceptable failure with more than 1 solutions'                                               || true       | [SOLUTION, SOLUTION_WITH_NO_DOMAINS]            || []            | []          | 0
      'an acceptable failure with no solutions'                                                        || true       | []                                              || []            | []          | 0
      'an acceptable failure with 1 solution that has no privileged blocks and grants to some domains' || true       | []                                              || []            | []          | 0
      'an acceptable failure with 1 solution that has privileged blocks and grants to some domains'    || true       | []                                              || []            | []          | 0
      'an acceptable failure with 1 solution that has no privileged blocks and grants to no domains'   || true       | []                                              || []            | []          | 0
      'an acceptable failure with 1 solution that has privileged blocks and grants to no domains'      || true       | []                                              || []            | []          | 0
  }

  private def stub(def debug, def threadReference, def reflectionUtil, def permissionUtil, def failing, def continuous) {
    with(debug) {
      thread() >> threadReference
      reflection() >> reflectionUtil
      permissions() >> permissionUtil
      isFailing() >> failing
      isContinuous() >> continuous
    }
  }

  private def stub(ReflectionUtil reflection) {
    reflection.getVoid() >> VOID
  }

  private def stub(ThreadReference thread) {
    with(thread) {
      frame(0) >> Mock(EnhancedStackFrame) {
        thisObject() >> ACC
        getArgumentValues() >> Mock(List) {
          get(0) >> PERMISSION
        }
        getValue(AccessControlContextCheckProcessor.LOCAL_I_SLOT_INDEX, 'I') >> Mock(IntegerValue) {
          intValue() >> LOCAL_I
        }
      }
    }
  }
}
