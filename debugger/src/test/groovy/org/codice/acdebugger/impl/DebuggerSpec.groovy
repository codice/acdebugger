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
import com.sun.jdi.Method
import com.sun.jdi.StackFrame
import com.sun.jdi.ThreadReference
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VirtualMachineManager
import com.sun.jdi.connect.AttachingConnector
import com.sun.jdi.connect.Connector.Argument
import com.sun.jdi.connect.Transport
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.event.Event
import com.sun.jdi.event.EventIterator
import com.sun.jdi.event.EventQueue
import com.sun.jdi.event.EventSet
import com.sun.jdi.event.LocatableEvent
import com.sun.jdi.event.MethodExitEvent
import com.sun.jdi.event.VMDisconnectEvent
import com.sun.jdi.request.BreakpointRequest
import com.sun.jdi.request.ClassPrepareRequest
import com.sun.jdi.request.EventRequest
import com.sun.jdi.request.EventRequestManager
import com.sun.jdi.request.MethodExitRequest
import org.codice.acdebugger.api.BreakpointProcessor
import org.codice.acdebugger.api.Debug
import org.codice.acdebugger.api.ReflectionUtil
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.ExecutorService
import java.util.stream.Stream

class DebuggerSpec extends Specification {
  static def TRANSPORT = 'transport'
  static def HOST = 'host'
  static def PORT = '2345'
  static def SIGNATURE = 'Ljava/lang/String;'
  static def CLASS_NAME = String.class.name
  static def METHOD_NAME = 'method'

  @Shared
  def CLASS = Mock(ClassType)
  @Shared
  def PENDING_PROCESSOR = Stub(BreakpointProcessor)
  @Shared
  def PENDING_LOCATION = Stub(BreakpointLocation)
  @Shared
  def THREAD = Stub(ThreadReference)
  @Shared
  def THREAD2 = Stub(ThreadReference)
  @Shared
  def VM = Stub(VirtualMachine)
  @Shared
  def VM2 = Stub(VirtualMachine)
  @Shared
  def VM_DISCONNECTED_EVENT = Stub(VMDisconnectEvent)
  @Shared
  def EXPECTED_METHOD_EXIT_EVENT = Mock(MethodExitEvent) {
    request() >> Mock(MethodExitRequest) {
      getProperty(Debugger.INFO_KEY) >> Mock(BreakpointInfo) {
        getLocation() >> Mock(BreakpointLocation) {
          getMethod() >> METHOD_NAME
        }
      }
    }
    thread() >> Mock(ThreadReference) {
      frame(0) >> Mock(StackFrame) {
        location() >> Mock(Location) {
          method() >> Mock(Method) {
            name() >> METHOD_NAME
          }
        }
      }
    }
  }
  @Shared
  def UNEXPECTED_METHOD_EXIT_EVENT = Mock(MethodExitEvent) {
    request() >> Mock(MethodExitRequest) {
      getProperty(Debugger.INFO_KEY) >> Mock(BreakpointInfo) {
        getLocation() >> Mock(BreakpointLocation) {
          getMethod() >> METHOD_NAME
        }
      }
    }
    thread() >> Mock(ThreadReference) {
      frame(0) >> Mock(StackFrame) {
        location() >> Mock(Location) {
          method() >> Mock(Method) {
            name() >> 'other.method'
          }
        }
      }
    }
  }
  @Shared
  def EXPECTING_METHOD_EXIT_EVENT_WITH_NO_METHOD = Mock(MethodExitEvent) {
    request() >> Mock(MethodExitRequest) {
      getProperty(Debugger.INFO_KEY) >> Mock(BreakpointInfo) {
        getLocation() >> Mock(BreakpointLocation) {
          getMethod() >> METHOD_NAME
        }
      }
    }
    thread() >> Mock(ThreadReference) {
      frame(0) >> Mock(StackFrame) {
        location() >> Mock(Location) {
          method() >> null
        }
      }
    }
  }
  @Shared
  def BREAKPOINT_EVENT = Mock(BreakpointEvent) {
    request() >> Mock(BreakpointRequest) {
      getProperty(Debugger.INFO_KEY) >> Mock(BreakpointInfo) {
        getLocation() >> Mock(BreakpointLocation) {
          getMethod() >> null
        }
      }
    }
    thread() >> Mock(ThreadReference) {
      frame(0) >> Mock(StackFrame) {
        location() >> Mock(Location) {
          method() >> METHOD_NAME
        }
      }
    }
  }
  @Shared
  def BREAKPOINT_EVENT_WITH_NO_INFO = Mock(BreakpointEvent) {
    request() >> Mock(BreakpointRequest) {
      getProperty(Debugger.INFO_KEY) >> null
    }
    thread() >> Mock(ThreadReference) {
      frame(0) >> Mock(StackFrame) {
        location() >> Mock(Location) {
          method() >> METHOD_NAME
        }
      }
    }
  }

  def DEBUGGER = new Debugger(TRANSPORT, HOST, PORT)

  def "test isOSGi() if not set"() {
    expect:
      DEBUGGER.context.isOSGi()
  }

  @Unroll
  def "test isOSGi() if set to #value"() {
    given:
      DEBUGGER.setOSGi(value)

    expect:
      DEBUGGER.context.isOSGi() == value

    where:
      value << [true, false]
  }

  def "test isContinuous() if not set"() {
    expect:
      !DEBUGGER.context.isContinuous()
  }

  @Unroll
  def "test isContinuous() if set to #value"() {
    given:
      DEBUGGER.setContinuous(value)

    expect:
      DEBUGGER.context.isContinuous() == value

    where:
      value << [true, false]
  }

  def "test isDebug() if not set"() {
    expect:
      !DEBUGGER.context.isDebug()
  }

  @Unroll
  def "test isDebug() if set to #value"() {
    given:
      DEBUGGER.setDebug(value)

    expect:
      DEBUGGER.context.isDebug() == value

    where:
      value << [true, false]
  }

  def "test isGranting() if not set"() {
    expect:
      !DEBUGGER.context.isGranting()
  }

  @Unroll
  def "test isGranting() if set to #value"() {
    given:
      DEBUGGER.setGranting(value)

    expect:
      DEBUGGER.context.isGranting() == value

    where:
      value << [true, false]
  }

  def "test isFailing() if not set"() {
    expect:
      !DEBUGGER.context.isFailing()
  }

  @Unroll
  def "test isFailing() if set to #value"() {
    given:
      DEBUGGER.setFailing(value)

    expect:
      DEBUGGER.context.isFailing() == value

    where:
      value << [true, false]
  }

  def "test isMonitoringService() if not set"() {
    expect:
      !DEBUGGER.context.isMonitoringService()
  }

  @Unroll
  def "test isMonitoringService() if set to #value"() {
    given:
      DEBUGGER.setMonitoringService(value)

    expect:
      DEBUGGER.context.isMonitoringService() == value

    where:
      value << [true, false]
  }

  def "test canDoPrivilegedBlocks() if not set"() {
    expect:
      DEBUGGER.context.canDoPrivilegedBlocks()
  }

  @Unroll
  def "test canDoPrivilegedBlocks() if set to #value"() {
    given:
      DEBUGGER.setDoPrivilegedBlocks(value)

    expect:
      DEBUGGER.context.canDoPrivilegedBlocks() == value

    where:
      value << [true, false]
  }

  def "test attach()"() {
    given:
      def portArg = Mock(Argument)
      def hostArg = Mock(Argument)
      def args = [(Debugger.PORT_KEY): portArg, (Debugger.HOST_KEY): hostArg]
      def connector = Mock(AttachingConnector) {
        transport() >> Mock(Transport) {
          name() >> TRANSPORT
        }
        defaultArguments() >> args
      }
      def connector2 = Mock(AttachingConnector) {
        transport() >> Mock(Transport) {
          name() >> 'transport-2'
        }
      }
      def vmm = Mock(VirtualMachineManager) {
        attachingConnectors() >> [connector2, connector]
      }
      def vm = Mock(VirtualMachine)
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT])

    when:
      def d = debugger.attach()

    then:
      d.is(debugger)
      with(d.debug) {
        virtualMachine() == vm
      }
      d.debug.context().is(d.context)
      with(d.context) {
        !isDebug()
        isOSGi()
        !isContinuous()
        !isGranting()
        !isFailing()
        !isMonitoringService()
        canDoPrivilegedBlocks()
        isRunning()
      }

    and:
      1 * debugger.virtualMachineManager() >> vmm
      1 * portArg.setValue(PORT)
      1 * hostArg.setValue(HOST)
      1 * connector.attach(args) >> vm
      1 * vm.setDebugTraceMode(_)
      1 * vm.description() >> "description\n2nd line"

    when:
      d.debug.thread()

    then:
      thrown(IllegalStateException)
  }

  def "test attach() when no matching connector found"() {
    given:
      def connector2 = Mock(AttachingConnector) {
        transport() >> Mock(Transport) {
          name() >> 'transport-2'
        }
      }
      def vmm = Mock(VirtualMachineManager) {
        attachingConnectors() >> [connector2]
      }
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT])

    when:
      def d = debugger.attach()

    then:
      def e = thrown(IOException)

      e.message.contains('find transport')

    and:
      1 * debugger.virtualMachineManager() >> vmm
  }

  @Unroll
  def "test add() with a location that has its referenced class already loaded with #with_what"() {
    given:
      def location = Mock(BreakpointLocation) {
        getClassSignature() >> SIGNATURE
        getClassName() >> CLASS_NAME
      }
      def processor = Mock(BreakpointProcessor) {
        locations() >> Stream.of(location)
      }
      def request = (has_request ? Mock(EventRequest) : null)
      def debug = Mock(Debug) {
        reflection() >> Mock(ReflectionUtil) {
          getClass(SIGNATURE) >> CLASS
        }
      }
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT, debug, null])

    when:
      debugger.add(processor)

    then:
      1 * location.setClassReference(CLASS)
      1 * processor.createRequest(debug, location) >> request
      if (has_request) {
        1 * request.putProperty(Debugger.INFO_KEY, {
          (it.request == request) && (it.processor == processor) && (it.location == location)
        })
        1 * request.setSuspendPolicy(BreakpointRequest.SUSPEND_EVENT_THREAD)
        1 * request.enable()
      }

    where:
      with_what                               || has_request
      'a created request from the processor'  || true
      'no created request from the processor' || false
  }

  def "test add() with a location that with a referenced class not loaded yet"() {
    given:
      def location = Mock(BreakpointLocation) {
        getClassSignature() >> SIGNATURE
        getClassName() >> CLASS_NAME
      }
      def processor = Mock(BreakpointProcessor) {
        locations() >> Stream.of(location)
      }
      def request = Mock(ClassPrepareRequest)
      def erm = Mock(EventRequestManager)
      def debug = Mock(Debug) {
        reflection() >> Mock(ReflectionUtil) {
          getClass(SIGNATURE) >> null
        }
        eventRequestManager() >> erm
      }
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT, debug, null])

    when:
      debugger.add(processor)

    then:
      1 * erm.createClassPrepareRequest() >> request
      1 * request.putProperty(Debugger.INFO_KEY, {
        (it instanceof PendingBreakpointInfo) && (it.request == request) && (it.processor == processor) && (it.location == location)
      })
      1 * request.addClassFilter(CLASS_NAME)
      1 * request.enable()
  }

  @Unroll
  def "test loop() with a disconnect event while monitoring=#is_monitoring, continuous=#is_continuous, doPrivileged=#do_privileged_blocks, and granting=#granting"() {
    given:
      def set = Mock(EventSet) {
        eventIterator() >> Mock(EventIterator) {
          hasNext() >>> [true, false]
          next() >>> [VM_DISCONNECTED_EVENT]
        }
      }
      def erm = Mock(EventRequestManager)
      def debug = Mock(Debug) {
        virtualMachine() >> Mock(VirtualMachine) {
          eventQueue() >> Mock(EventQueue) {
            remove() >> set
          }
        }
        eventRequestManager() >> erm
        isMonitoringService() >> is_monitoring
        isContinuous() >> is_continuous
        canDoPrivilegedBlocks() >> do_privileged_blocks
        isGranting() >> granting
      }
      def executor = Mock(ExecutorService)
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT, debug, executor])

    when:
      debugger.loop()

    then:
      !debugger.context.running

    and:
      1 * set.resume()
      1 * executor.shutdown()
      1 * executor.awaitTermination(*_)

    where:
      is_monitoring | is_continuous | do_privileged_blocks | granting
      true          | true          | true                 | true
      false         | true          | true                 | true
      true          | false         | true                 | true
      true          | true          | false                | true
      true          | true          | true                 | false
      true          | true          | false                | false
  }

  def "test loop() with a pending breakpoint event"() {
    given:
      def pendingRequest = Mock(EventRequest) {
        getProperty(Debugger.INFO_KEY) >> Mock(PendingBreakpointInfo) {
          getProcessor() >> PENDING_PROCESSOR
          getLocation() >> PENDING_LOCATION
        }
      }
      def pendingEvent = Mock(Event) {
        request() >> pendingRequest
      }
      def set = Mock(EventSet) {
        eventIterator() >> Mock(EventIterator) {
          hasNext() >>> [true, true, false]
          next() >>> [pendingEvent, VM_DISCONNECTED_EVENT]
        }
      }
      def erm = Mock(EventRequestManager)
      def debug = Mock(Debug) {
        virtualMachine() >> Mock(VirtualMachine) {
          eventQueue() >> Mock(EventQueue) {
            remove() >> set
          }
        }
        eventRequestManager() >> erm
        isMonitoringService() >> true
        isContinuous() >> true
        canDoPrivilegedBlocks() >> true
        isGranting() >> true
      }
      def executor = Mock(ExecutorService)
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT, debug, executor])

    when:
      debugger.loop()

    then:
      !debugger.context.running

    and:
      1 * pendingRequest.disable()
      1 * erm.deleteEventRequest(pendingRequest)
      1 * debugger.add(PENDING_PROCESSOR, PENDING_LOCATION) >> null
      1 * set.resume()
      1 * executor.shutdown()
      1 * executor.awaitTermination(*_)
  }

  def "test loop() with an unknown event followed by another set with a VM disconnect event"() {
    given:
      def event = Mock(Event) {
        request() >> null
      }
      def set = Mock(EventSet) {
        eventIterator() >> Mock(EventIterator) {
          hasNext() >>> [true, false]
          next() >>> [event]
        }
      }
      def set2 = Mock(EventSet) {
        eventIterator() >> Mock(EventIterator) {
          hasNext() >>> [true, false]
          next() >>> [VM_DISCONNECTED_EVENT]
        }
      }
      def erm = Mock(EventRequestManager)
      def debug = Mock(Debug) {
        virtualMachine() >> Mock(VirtualMachine) {
          eventQueue() >> Mock(EventQueue) {
            remove() >>> [set, set2]
          }
        }
        eventRequestManager() >> erm
        isMonitoringService() >> true
        isContinuous() >> true
        canDoPrivilegedBlocks() >> true
        isGranting() >> true
      }
      def executor = Mock(ExecutorService)
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT, debug, executor])

    when:
      debugger.loop()

    then:
      !debugger.context.running

    and:
      1 * set.resume()
      1 * set2.resume()
      1 * executor.shutdown()
      1 * executor.awaitTermination(*_)
  }

  @Unroll
  def "test loop() with #with_what"() {
    given:
      def iterator = Mock(EventIterator) {
        hasNext() >>> [true, true, false]
        next() >>> [event, VM_DISCONNECTED_EVENT]
      }
      def set = Mock(EventSet) {
        eventIterator() >> iterator
      }
      def debug = Mock(Debug) {
        virtualMachine() >> Mock(VirtualMachine) {
          eventQueue() >> Mock(EventQueue) {
            remove() >> set
          }
        }
        isMonitoringService() >> true
        isContinuous() >> true
        canDoPrivilegedBlocks() >> true
        isGranting() >> true
      }
      def executor = Mock(ExecutorService)
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT, debug, executor])

    when:
      debugger.loop()

    then:
      !debugger.context.running

    and:
      resume_count * set.resume()
      execute_count * executor.execute({
        it.eventSet == set
        it.i == iterator
        it.initialEvent == event
      })
      1 * executor.shutdown()
      1 * executor.awaitTermination(*_)

    where:
      with_what                                        || event                                      || resume_count | execute_count
      'a method exit event for an expected method'     || EXPECTED_METHOD_EXIT_EVENT                 || 0            | 1
      'a method exit event for an unexpected method'   || UNEXPECTED_METHOD_EXIT_EVENT               || 1            | 0
      'a method exit event missing a method'           || EXPECTING_METHOD_EXIT_EVENT_WITH_NO_METHOD || 1            | 0
      'a breakpoint event'                             || BREAKPOINT_EVENT                           || 0            | 1
      'a breakpoint event that has no breakpoint info' || BREAKPOINT_EVENT_WITH_NO_INFO              || 1            | 0
  }

  @Unroll
  def "test loop() failing with #exception.class.simpleName"() {
    given:
      def set = Mock(EventSet) {
        eventIterator() >> Mock(EventIterator) {
          hasNext() >>> [true, true, false]
          next() >>> [BREAKPOINT_EVENT, VM_DISCONNECTED_EVENT]
        }
      }
      def debug = Mock(Debug) {
        virtualMachine() >> Mock(VirtualMachine) {
          eventQueue() >> Mock(EventQueue) {
            remove() >> set
          }
        }
        isMonitoringService() >> true
        isContinuous() >> true
        canDoPrivilegedBlocks() >> true
        isGranting() >> true
      }
      def executor = Mock(ExecutorService)
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT, debug, executor])

    when:
      debugger.loop()

    then:
      !debugger.context.running

    and:
      1 * set.resume()
      1 * executor.execute(_) >> {
        throw exception
      }
      1 * executor.shutdown()
      1 * executor.awaitTermination(*_)

    where:
      exception << [new NullPointerException('testing'), new Error('testing')]
  }

  @Unroll
  def "test loop() failing with OutOfMemoryError"() {
    given:
      def exception = new OutOfMemoryError('testing')
      def set = Mock(EventSet) {
        eventIterator() >> Mock(EventIterator) {
          hasNext() >>> [true, true, false]
          next() >>> [BREAKPOINT_EVENT, VM_DISCONNECTED_EVENT]
        }
      }
      def debug = Mock(Debug) {
        virtualMachine() >> Mock(VirtualMachine) {
          eventQueue() >> Mock(EventQueue) {
            remove() >> set
          }
        }
        isMonitoringService() >> true
        isContinuous() >> true
        canDoPrivilegedBlocks() >> true
        isGranting() >> true
      }
      def executor = Mock(ExecutorService)
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT, debug, executor])

    when:
      debugger.loop()

    then:
      def e = thrown(VirtualMachineError)

      e.is(exception)

    and:
      1 * set.resume()
      1 * executor.execute(_) >> {
        throw exception
      }
      0 * executor.shutdown()
      0 * executor.awaitTermination(*_)
  }

  def "test EventSetThread.run()"() {
    given:
      def initialBreakpointInfo = Mock(BreakpointInfo)
      def initialEvent = Mock(LocatableEvent) {
        request() >> Mock(EventRequest) {
          getProperty(Debugger.INFO_KEY) >> initialBreakpointInfo
        }
        virtualMachine() >> VM
        thread() >> THREAD
      }
      def breakpointInfo = Mock(BreakpointInfo)
      def event = Mock(LocatableEvent) {
        request() >> Mock(EventRequest) {
          getProperty(Debugger.INFO_KEY) >> breakpointInfo
        }
        virtualMachine() >> VM2
        thread() >> THREAD2
      }
      def eventSet = Mock(EventSet)
      def eventIterator = Mock(EventIterator) {
        hasNext() >>> [true, false]
        next() >> event
      }
      def executor = Mock(ExecutorService)
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT, null, executor])

    when:
      new Debugger.EventSetThread(debugger, eventSet, eventIterator, initialEvent).run()

    then:
      1 * initialBreakpointInfo.process({
        (it.context() == debugger.context) && (it.event() == initialEvent) && (it.virtualMachine() == VM) && (it.thread() == THREAD)
      })
      1 * breakpointInfo.process({
        (it.context() == debugger.context) && (it.event() == event) && (it.virtualMachine() == VM2) && (it.thread() == THREAD2)
      })
      1 * eventSet.resume()
  }

  def "test EventSetThread.run() when no longer running from the start"() {
    given:
      def initialBreakpointInfo = Mock(BreakpointInfo)
      def initialEvent = Mock(LocatableEvent) {
        request() >> Mock(EventRequest) {
          getProperty(Debugger.INFO_KEY) >> initialBreakpointInfo
        }
        virtualMachine() >> VM
        thread() >> THREAD
      }
      def eventSet = Mock(EventSet)
      def eventIterator = Mock(EventIterator) {
        hasNext() >> false
      }
      def executor = Mock(ExecutorService)
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT, null, executor])

      debugger.context.stop()

    when:
      new Debugger.EventSetThread(debugger, eventSet, eventIterator, initialEvent).run()

    then:
      0 * initialBreakpointInfo.process(_)
      1 * eventSet.resume()
  }

  def "test EventSetThread.run() when no longer running after first event"() {
    given:
      def initialBreakpointInfo = Mock(BreakpointInfo)
      def initialEvent = Mock(LocatableEvent) {
        request() >> Mock(EventRequest) {
          getProperty(Debugger.INFO_KEY) >> initialBreakpointInfo
        }
        virtualMachine() >> VM
        thread() >> THREAD
      }
      def breakpointInfo = Mock(BreakpointInfo)
      def event = Mock(LocatableEvent) {
        request() >> Mock(EventRequest) {
          getProperty(Debugger.INFO_KEY) >> breakpointInfo
        }
        virtualMachine() >> VM2
        thread() >> THREAD2
      }
      def eventSet = Mock(EventSet)
      def eventIterator = Mock(EventIterator) {
        hasNext() >>> [true, false]
        next() >> event
      }
      def executor = Mock(ExecutorService)
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT, null, executor])

    when:
      new Debugger.EventSetThread(debugger, eventSet, eventIterator, initialEvent).run()

    then:
      1 * initialBreakpointInfo.process({
        (it.context() == debugger.context) && (it.event() == initialEvent) && (it.virtualMachine() == VM) && (it.thread() == THREAD)
      }) >> { debugger.context.stop() }
      0 * breakpointInfo.process({
        (it.context() == debugger.context) && (it.event() == event) && (it.virtualMachine() == VM2) && (it.thread() == THREAD2)
      })
      1 * eventSet.resume()
  }

  @Unroll
  def "test EventSetThread.run() when failing with #exception.class.simpleName"() {
    given:
      def initialBreakpointInfo = Mock(BreakpointInfo)
      def initialEvent = Mock(LocatableEvent) {
        request() >> Mock(EventRequest) {
          getProperty(Debugger.INFO_KEY) >> initialBreakpointInfo
        }
        virtualMachine() >> VM
        thread() >> THREAD
      }
      def breakpointInfo = Mock(BreakpointInfo)
      def event = Mock(LocatableEvent) {
        request() >> Mock(EventRequest) {
          getProperty(Debugger.INFO_KEY) >> breakpointInfo
        }
        virtualMachine() >> VM2
        thread() >> THREAD2
      }
      def eventSet = Mock(EventSet)
      def eventIterator = Mock(EventIterator) {
        hasNext() >>> [true, false]
        next() >> event
      }
      def executor = Mock(ExecutorService)
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT, null, executor])

    when:
      new Debugger.EventSetThread(debugger, eventSet, eventIterator, initialEvent).run()

    then:
      1 * initialBreakpointInfo.process({
        (it.context() == debugger.context) && (it.event() == initialEvent) && (it.virtualMachine() == VM) && (it.thread() == THREAD)
      }) >> { throw exception }
      1 * breakpointInfo.process({
        (it.context() == debugger.context) && (it.event() == event) && (it.virtualMachine() == VM2) && (it.thread() == THREAD2)
      })
      1 * eventSet.resume()

    where:
      exception << [new NullPointerException('testing'), new Error('testing')]
  }

  def "test EventSetThread.run() when failing with OutOfMemoryError"() {
    given:
      def exception = new OutOfMemoryError('testing')
      def initialBreakpointInfo = Mock(BreakpointInfo)
      def initialEvent = Mock(LocatableEvent) {
        request() >> Mock(EventRequest) {
          getProperty(Debugger.INFO_KEY) >> initialBreakpointInfo
        }
        virtualMachine() >> VM
        thread() >> THREAD
      }
      def breakpointInfo = Mock(BreakpointInfo)
      def event = Mock(LocatableEvent) {
        request() >> Mock(EventRequest) {
          getProperty(Debugger.INFO_KEY) >> breakpointInfo
        }
        virtualMachine() >> VM2
        thread() >> THREAD2
      }
      def eventSet = Mock(EventSet)
      def eventIterator = Mock(EventIterator) {
        hasNext() >>> [true, false]
        next() >> event
      }
      def executor = Mock(ExecutorService)
      def debugger = Spy(Debugger, constructorArgs: [TRANSPORT, HOST, PORT, null, executor])

    when:
      new Debugger.EventSetThread(debugger, eventSet, eventIterator, initialEvent).run()

    then:
      def e = thrown(VirtualMachineError)

      e.is(exception)

    and:
      1 * initialBreakpointInfo.process({
        (it.context() == debugger.context) && (it.event() == initialEvent) && (it.virtualMachine() == VM) && (it.thread() == THREAD)
      }) >> { throw exception }
      0 * breakpointInfo.process({
        (it.context() == debugger.context) && (it.event() == event) && (it.virtualMachine() == VM2) && (it.thread() == THREAD2)
      })
      1 * eventSet.resume()
  }
}
