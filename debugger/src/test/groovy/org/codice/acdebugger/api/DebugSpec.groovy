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

import com.sun.jdi.Location
import com.sun.jdi.ObjectReference
import com.sun.jdi.ReferenceType
import com.sun.jdi.StackFrame
import com.sun.jdi.ThreadReference
import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.Event
import com.sun.jdi.event.LocatableEvent
import com.sun.jdi.request.EventRequestManager
import org.codice.acdebugger.impl.DebugContext
import org.codice.spock.Supplemental
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@Supplemental
class DebugSpec extends Specification {
  static def BUNDLE = 'my.bundle'
  static def BUNDLE2 = null
  static def LOCATION_STR = 'location(4)'
  static def LOCATION_STR2 = 'other/location(12)'
  static def CLASS_NAME = 'some.ClassName'
  static def CLASS_NAME2 = 'some.other.ClassName'

  @Shared
  def CONTEXT = Mock(DebugContext)

  @Shared
  def VM = Mock(VirtualMachine)

  @Shared
  def THREAD = Mock(ThreadReference)

  @Shared
  def EVENT = Mock(Event) {
    virtualMachine() >> VM
  }

  @Shared
  def LOCATABLE_EVENT = Mock(LocatableEvent) {
    virtualMachine() >> VM
    thread() >> THREAD
  }

  @Shared
  def CLASS = Mock(ReferenceType) {
    name() >> CLASS_NAME
  }
  @Shared
  def CLASS2 = Mock(ReferenceType) {
    name() >> CLASS_NAME2
  }

  @Shared
  def OBJECT = Mock(ObjectReference) {
    toString() >> "instance of $CLASS_NAME"
  }

  @Shared
  def FRAME = Mock(StackFrame) {
    location() >> Mock(Location) {
      toString() >> LOCATION_STR
      declaringType() >> CLASS
    }
    thisObject() >> OBJECT
  }
  @Shared
  def FRAME2 = Mock(StackFrame) {
    location() >> Mock(Location) {
      toString() >> LOCATION_STR2
      declaringType() >> CLASS2
    }
    thisObject() >> null
  }

  @Shared
  def INFO = new StackFrameInformation(BUNDLE, LOCATION_STR, CLASS, CLASS_NAME, OBJECT, "instance of $CLASS_NAME")
  @Shared
  def INFO2 = new StackFrameInformation(BUNDLE2, LOCATION_STR2, CLASS2, CLASS_NAME2, null, "class of $CLASS_NAME")

  def "test constructor with a context and a locatable event"() {
    when:
      def debug = Spy(Debug, constructorArgs: [CONTEXT, LOCATABLE_EVENT])

    then:
      debug.context() == CONTEXT
      debug.event() == LOCATABLE_EVENT
      debug.virtualMachine() == VM
      debug.thread() == THREAD
  }

  def "test constructor with a context and a non-locatable event"() {
    when:
      def debug = Spy(Debug, constructorArgs: [CONTEXT, EVENT])

    then:
      debug.context() == CONTEXT
      debug.event() == EVENT
      debug.virtualMachine() == VM

    when:
      debug.thread()

    then:
      thrown(IllegalStateException)
  }

  def "test constructor with a context and a vm"() {
    when:
      def debug = Spy(Debug, constructorArgs: [CONTEXT, VM])

    then:
      debug.context() == CONTEXT
      debug.virtualMachine() == VM

    when:
      debug.event() == EVENT

    then:
      thrown(IllegalStateException)

    when:
      debug.thread()

    then:
      thrown(IllegalStateException)
  }

  @Unroll
  def "test #method.simplePrototype is delegated to the context"() {
    given:
      def context = Mock(DebugContext)
      def debug = Spy(Debug, constructorArgs: [context, LOCATABLE_EVENT])

    and:
      def parms = Dummies(method.parameterTypes)
      def result = Dummy(method.returnType)

    when:
      def returnedResult = debug."$method.name"(*parms)

    then:
      returnedResult == result

    and:
      1 * context."$method.name"(*_) >> {
        method.verifyInvocation(delegate, *parms)
        result
      }

    where:
      method << Debug.proxyableMethods.findAll {
        !(it.name in [
            'virtualMachine', 'thread', 'threadStack', 'reflection', 'permissions', 'locations',
            'bundles', 'domains', 'eventRequestManager', 'event', 'add'
        ])
      }
  }

  def "test locations() when debugging OSGi containers"() {
    given:
      def bundles = Mock(BundleUtil)
      def context = Mock(DebugContext)
      def debug = Spy(Debug, constructorArgs: [context, LOCATABLE_EVENT])

    when:
      def result = debug.locations()

    then:
      result == bundles

    and:
      1 * context.isOSGi() >> true
      1 * debug.bundles() >> bundles
  }

  def "test locations() when debugging non-OSGi VMs"() {
    given:
      def domains = Mock(DomainUtil)
      def context = Mock(DebugContext)
      def debug = Spy(Debug, constructorArgs: [context, LOCATABLE_EVENT])

    when:
      def result = debug.locations()

    then:
      result == domains

    and:
      1 * context.isOSGi() >> false
      1 * debug.domains() >> domains
  }

  def "test eventRequestManager() is delegated to the virtual machine"() {
    given:
      def erm = Mock(EventRequestManager)
      def vm = Mock(VirtualMachine)
      def event = Mock(Event) {
        virtualMachine() >> vm
      }
      def debug = Spy(Debug, constructorArgs: [CONTEXT, event])

    when:
      def result = debug.eventRequestManager()

    then:
      result == erm

    and:
      1 * vm.eventRequestManager() >> erm
  }

  def "test threadStack() with no thread defined"() {
    given:
      def debug = Spy(Debug, constructorArgs: [CONTEXT, EVENT])

    when:
      debug.threadStack()

    then:
      thrown(IllegalStateException)
  }

  @Unroll
  def "test threadStack() with #with_what"() {
    given:
      def debug = Spy(Debug, constructorArgs: [CONTEXT, EVENT])

    when:
      def info = debug.threadStack()

    then:
      info == result

    and:
      debug.thread() >> Mock(ThreadReference) {
        frameCount() >> frames.size()
        frame(_) >> { frames[it[0]] }
      }
      debug.locations() >> Mock(LocationUtil) {
        get(FRAME) >> BUNDLE
        get(FRAME2) >> BUNDLE2
      }

    where:
      with_what             || frames          || result
      'no frames'           || []              || []
      'one frame'           || [FRAME]         || [INFO]
      'more than one frame' || [FRAME2, FRAME] || [INFO2, INFO]
  }
}
