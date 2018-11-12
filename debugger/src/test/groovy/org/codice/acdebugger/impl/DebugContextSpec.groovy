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

import org.codice.acdebugger.api.SecurityFailure
import org.codice.acdebugger.api.SecuritySolution
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.Supplier

class DebugContextSpec extends Specification {
  static def DOMAIN = 'domain'
  static def DOMAIN2 = 'domain2'
  static def DOMAINS = [DOMAIN, DOMAIN2] as Set<String>
  static def PERMISSION = 'some.permission "do"'
  static def PERMISSION2 = 'some.permission.2 "do"'
  static def PERMISSIONS = [PERMISSION, PERMISSION2] as Set<String>
  static def PERMISSIONS2 = [PERMISSION2] as Set<String>

  static KEY = 'key'
  static VALUE = 123456

  @Shared
  def STACK = Stub(List)
  @Shared
  def STACK2 = Stub(List)
  @Shared
  def SOLUTION = Spy(SecuritySolution, constructorArgs: [STACK, PERMISSIONS, DOMAINS, []])
  @Shared
  def SOLUTION2 = Spy(SecuritySolution, constructorArgs: [PERMISSIONS2, DOMAINS])
  @Shared
  def SOLUTIONS = [SOLUTION, SOLUTION2]
  @Shared
  def SOLUTIONS2 = [SOLUTION2]

  @Shared
  def IDENTICAL_ACCEPTABLE_FAILURE = Mock(SecurityFailure) {
    isAcceptable() >> true
    getAcceptablePermissions() >> PERMISSIONS
    getStack() >> STACK
  }
  @Shared
  def ACCEPTABLE_FAILURE_WITH_SAME_PERMISSIONS = Mock(SecurityFailure) {
    isAcceptable() >> true
    getAcceptablePermissions() >> PERMISSIONS
    getStack() >> STACK2
  }
  @Shared
  def ACCEPTABLE_FAILURE_WITH_SAME_STACK = Mock(SecurityFailure) {
    isAcceptable() >> true
    getAcceptablePermissions() >> PERMISSIONS2
    getStack() >> STACK
  }
  @Shared
  def DIFFERENT_ACCEPTABLE_FAILURE = Mock(SecurityFailure) {
    isAcceptable() >> true
    getAcceptablePermissions() >> PERMISSIONS2
    getStack() >> STACK2
  }
  @Shared
  def IDENTICAL_FAILURE = Mock(SecurityFailure) {
    isAcceptable() >> false
    analyze() >> SOLUTIONS
  }
  @Shared
  def DIFFERENT_FAILURE = Mock(SecurityFailure) {
    isAcceptable() >> false
    analyze() >> SOLUTIONS2
  }
  @Shared
  def FAILURES = [DIFFERENT_ACCEPTABLE_FAILURE, DIFFERENT_FAILURE, ACCEPTABLE_FAILURE_WITH_SAME_PERMISSIONS, ACCEPTABLE_FAILURE_WITH_SAME_STACK, IDENTICAL_ACCEPTABLE_FAILURE, IDENTICAL_FAILURE]

  def CONTEXT = new DebugContext()

  def "test hasPermission() if not granted"() {
    expect:
      !CONTEXT.hasPermission(DOMAIN, PERMISSION)
  }

  @Unroll
  def "test hasPermission() if granted"() {
    given:
      CONTEXT.grantPermission(DOMAIN, PERMISSION)

    expect:
      CONTEXT.hasPermission(DOMAIN, PERMISSION)
  }

  def "test hasPermission() with the boot domain"() {
    expect:
      CONTEXT.hasPermission(null, PERMISSION)
  }

  def "test hasPermissions() if none are granted"() {
    expect:
      !CONTEXT.hasPermissions(DOMAIN, PERMISSIONS)
  }

  def "test hasPermissions() if only one was granted"() {
    given:
      CONTEXT.grantPermission(DOMAIN, PERMISSION)

    expect:
      !CONTEXT.hasPermissions(DOMAIN, PERMISSIONS)
  }

  def "test hasPermissions() if all were granted"() {
    given:
      CONTEXT.grantPermissions(DOMAIN, PERMISSIONS)

    expect:
      CONTEXT.hasPermissions(DOMAIN, PERMISSIONS)
  }

  def "test hasPermissions() if all were granted individually"() {
    given:
      CONTEXT.grantPermission(DOMAIN, PERMISSION)
      CONTEXT.grantPermission(DOMAIN, PERMISSION2)

    expect:
      CONTEXT.hasPermissions(DOMAIN, PERMISSIONS)
  }

  def "test hasPermissions() with the boot domain"() {
    expect:
      CONTEXT.hasPermissions(null, PERMISSIONS)
  }

  def "test grantPermission() if not already granted"() {
    expect:
      CONTEXT.grantPermission(DOMAIN, PERMISSION)
  }

  def "test grantPermission() if already granted"() {
    given:
      CONTEXT.grantPermission(DOMAIN, PERMISSION)

    expect:
      !CONTEXT.grantPermission(DOMAIN, PERMISSION)
  }

  def "test grantPermission() with boot domain"() {
    expect:
      !CONTEXT.grantPermission(null, PERMISSION)
  }

  def "test grantPermissions() if none already granted"() {
    expect:
      CONTEXT.grantPermissions(DOMAIN, PERMISSIONS)
  }

  def "test grantPermissions() if only one was already granted"() {
    given:
      CONTEXT.grantPermission(DOMAIN, PERMISSION)

    expect:
      !CONTEXT.grantPermissions(DOMAIN, PERMISSIONS)
  }

  def "test grantPermissions() if all were already granted"() {
    given:
      CONTEXT.grantPermissions(DOMAIN, PERMISSIONS)

    expect:
      !CONTEXT.grantPermissions(DOMAIN, PERMISSIONS)
  }

  def "test grantPermissions() if all were already granted individually"() {
    given:
      CONTEXT.grantPermission(DOMAIN, PERMISSION)
      CONTEXT.grantPermission(DOMAIN, PERMISSION2)

    expect:
      !CONTEXT.grantPermissions(DOMAIN, PERMISSIONS)
  }

  def "test grantPermissions() with boot domain"() {
    expect:
      !CONTEXT.grantPermissions(null, PERMISSIONS)
  }

  def "test get() if nothing stored"() {
    expect:
      CONTEXT.get(KEY) == null
  }

  def "test get() if something stored"() {
    given:
      CONTEXT.put(KEY, VALUE)

    expect:
      CONTEXT.get(KEY) == VALUE
  }

  def "test computeIfAbsent() if nothing stored"() {
    given:
      def supplier = Mock(Supplier)

    when:
      def result = CONTEXT.computeIfAbsent(KEY, supplier)

    then:
      result == VALUE

    and:
      1 * supplier.get() >> VALUE
  }

  def "test computeIfAbsent() if something stored"() {
    given:
      def supplier = Mock(Supplier)

      CONTEXT.put(KEY, VALUE)

    when:
      def result = CONTEXT.computeIfAbsent(KEY, supplier)

    then:
      result == VALUE

    and:
      0 * supplier.get()
  }

  def "test put() if nothing stored already"() {
    when:
      CONTEXT.put(KEY, VALUE)

    then:
      CONTEXT.get(KEY) == VALUE
  }

  def "test put() if something already already"() {
    given:
      CONTEXT.put(KEY, 'abc')

    when:
      CONTEXT.put(KEY, VALUE)

    then:
      CONTEXT.get(KEY) == VALUE
  }

  def "test isOSGi() if not set"() {
    expect:
      CONTEXT.isOSGi()
  }

  @Unroll
  def "test isOSGi() if set to #value"() {
    given:
      CONTEXT.setOSGi(value)

    expect:
      CONTEXT.isOSGi() == value

    where:
      value << [true, false]
  }

  def "test isContinuous() if not set"() {
    expect:
      !CONTEXT.isContinuous()
  }

  @Unroll
  def "test isContinuous() if set to #value"() {
    given:
      CONTEXT.setContinuous(value)

    expect:
      CONTEXT.isContinuous() == value

    where:
      value << [true, false]
  }

  def "test isDebug() if not set"() {
    expect:
      !CONTEXT.isDebug()
  }

  @Unroll
  def "test isDebug() if set to #value"() {
    given:
      CONTEXT.setDebug(value)

    expect:
      CONTEXT.isDebug() == value

    where:
      value << [true, false]
  }

  def "test isGranting() if not set"() {
    expect:
      !CONTEXT.isGranting()
  }

  @Unroll
  def "test isGranting() if set to #value"() {
    given:
      CONTEXT.setGranting(value)

    expect:
      CONTEXT.isGranting() == value

    where:
      value << [true, false]
  }

  def "test isFailing() if not set"() {
    expect:
      !CONTEXT.isFailing()
  }

  @Unroll
  def "test isFailing() if set to #value"() {
    given:
      CONTEXT.setFailing(value)

    expect:
      CONTEXT.isFailing() == value

    where:
      value << [true, false]
  }

  def "test isMonitoringService() if not set"() {
    expect:
      !CONTEXT.isMonitoringService()
  }

  @Unroll
  def "test isMonitoringService() if set to #value"() {
    given:
      CONTEXT.setMonitoringService(value)

    expect:
      CONTEXT.isMonitoringService() == value

    where:
      value << [true, false]
  }

  def "test canDoPrivilegedBlocks() if not set"() {
    expect:
      CONTEXT.canDoPrivilegedBlocks()
  }

  @Unroll
  def "test canDoPrivilegedBlocks() if set to #value"() {
    given:
      CONTEXT.setDoPrivilegedBlocks(value)

    expect:
      CONTEXT.canDoPrivilegedBlocks() == value

    where:
      value << [true, false]
  }

  def "test isRunning() if not stopped"() {
    expect:
      CONTEXT.isRunning()
  }

  def "test isRunning() if stopped"() {
    given:
      CONTEXT.stop()

    expect:
      !CONTEXT.isRunning()
  }

  @Unroll
  def "test record() with an acceptable failure #recorded_how and #and_what in #in_what"() {
    given:
      def failure = Mock(SecurityFailure) {
        isAcceptable() >> true
        getAcceptablePermissions() >> PERMISSIONS
        getStack() >> STACK
      }

      CONTEXT.setDebug(debug)
      CONTEXT.setOSGi(osgi)
      CONTEXT.setContinuous(continuous)
      if (recorded) {
        CONTEXT.failures.addAll(FAILURES)
      }

    when:
      CONTEXT.record(failure)

    then:
      CONTEXT.failures == (recorded ? FAILURES + failure : [failure])
      CONTEXT.isRunning()

    and:
      dump_count * failure.dump(osgi, _)
      print_count * failure.toString() >> '<FAILURE>'

    where:
      recorded_how           | and_what                               | in_what       || recorded | debug | osgi  | continuous || dump_count | print_count
      'not already recorded' | 'an OSGi container in continuous mode' | 'debug mode'  || false    | true  | true  | true       || 1          | 0
      'not already recorded' | 'an OSGi container for one error'      | 'debug mode'  || false    | true  | true  | false      || 1          | 0
      'not already recorded' | 'a VM in continuous mode'              | 'debug mode'  || false    | true  | false | true       || 1          | 0
      'not already recorded' | 'a VM for one error'                   | 'debug mode'  || false    | true  | false | false      || 1          | 0
      'not already recorded' | 'an OSGi container in continuous mode' | 'normal mode' || false    | false | true  | true       || 0          | 1
      'not already recorded' | 'an OSGi container for one error'      | 'normal mode' || false    | false | true  | false      || 0          | 1
      'not already recorded' | 'a VM in continuous mode'              | 'normal mode' || false    | false | false | true       || 0          | 1
      'not already recorded' | 'a VM for one error'                   | 'normal mode' || false    | false | false | false      || 0          | 1

      'already recorded'     | 'an OSGi container in continuous mode' | 'debug mode'  || true     | true  | true  | true       || 0          | 0
      'already recorded'     | 'an OSGi container for one error'      | 'debug mode'  || true     | true  | true  | false      || 0          | 0
      'already recorded'     | 'a VM in continuous mode'              | 'debug mode'  || true     | true  | false | true       || 0          | 0
      'already recorded'     | 'a VM for one error'                   | 'debug mode'  || true     | true  | false | false      || 0          | 0
      'already recorded'     | 'an OSGi container in continuous mode' | 'normal mode' || true     | false | true  | true       || 0          | 0
      'already recorded'     | 'an OSGi container for one error'      | 'normal mode' || true     | false | true  | false      || 0          | 0
      'already recorded'     | 'a VM in continuous mode'              | 'normal mode' || true     | false | false | true       || 0          | 0
      'already recorded'     | 'a VM for one error'                   | 'normal mode' || true     | false | false | false      || 0          | 0
  }

  @Unroll
  def "test record() with an unacceptable failure #recorded_how and #and_what in #in_what"() {
    given:
      def solution = Spy(SecuritySolution, constructorArgs: [STACK, PERMISSIONS, DOMAINS, []])
      def solution2 = Spy(SecuritySolution, constructorArgs: [PERMISSIONS2, DOMAINS])
      def solutions = [[], [solution2], [solution, solution2]][solutions_count]
      def failure = Mock(SecurityFailure) {
        isAcceptable() >> false
        analyze() >> solutions
      }

      CONTEXT.setDebug(debug)
      CONTEXT.setOSGi(osgi)
      CONTEXT.setContinuous(continuous)
      if (recorded) {
        CONTEXT.failures.addAll(FAILURES)
      }

    when:
      CONTEXT.record(failure)

    then:
      CONTEXT.failures == (recorded ? FAILURES + failure : [failure])
      CONTEXT.isRunning() != stopped
      DOMAINS.every { domain ->
        solutions.every {
          CONTEXT.hasPermissions(domain, it.permissions) == granted
        }
      }

    and:
      dump_count * failure.dump(osgi, _)
      print_count * failure.toString() >> '<FAILURE>'
      if (solutions) {
        solutions.each {
          print_count * it.print(osgi, _)
        }
      }

    where:
      recorded_how                             | and_what                               | in_what       || recorded | solutions_count | debug | osgi  | continuous || dump_count | print_count | stopped | granted
      'not already recorded with no solutions' | 'an OSGi container in continuous mode' | 'debug mode'  || false    | 0               | true  | true  | true       || 0          | 0           | false   | false
      'not already recorded with no solutions' | 'an OSGi container for one error'      | 'debug mode'  || false    | 0               | true  | true  | false      || 0          | 0           | false   | false
      'not already recorded with no solutions' | 'a VM in continuous mode'              | 'debug mode'  || false    | 0               | true  | false | true       || 0          | 0           | false   | false
      'not already recorded with no solutions' | 'a VM for one error'                   | 'debug mode'  || false    | 0               | true  | false | false      || 0          | 0           | false   | false
      'not already recorded with no solutions' | 'an OSGi container in continuous mode' | 'normal mode' || false    | 0               | false | true  | true       || 0          | 0           | false   | false
      'not already recorded with no solutions' | 'an OSGi container for one error'      | 'normal mode' || false    | 0               | false | true  | false      || 0          | 0           | false   | false
      'not already recorded with no solutions' | 'a VM in continuous mode'              | 'normal mode' || false    | 0               | false | false | true       || 0          | 0           | false   | false
      'not already recorded with no solutions' | 'a VM for one error'                   | 'normal mode' || false    | 0               | false | false | false      || 0          | 0           | false   | false

      'already recorded with no solutions'     | 'an OSGi container in continuous mode' | 'debug mode'  || true     | 0               | true  | true  | true       || 0          | 0           | false   | false
      'already recorded with no solutions'     | 'an OSGi container for one error'      | 'debug mode'  || true     | 0               | true  | true  | false      || 0          | 0           | false   | false
      'already recorded with no solutions'     | 'a VM in continuous mode'              | 'debug mode'  || true     | 0               | true  | false | true       || 0          | 0           | false   | false
      'already recorded with no solutions'     | 'a VM for one error'                   | 'debug mode'  || true     | 0               | true  | false | false      || 0          | 0           | false   | false
      'already recorded with no solutions'     | 'an OSGi container in continuous mode' | 'normal mode' || true     | 0               | false | true  | true       || 0          | 0           | false   | false
      'already recorded with no solutions'     | 'an OSGi container for one error'      | 'normal mode' || true     | 0               | false | true  | false      || 0          | 0           | false   | false
      'already recorded with no solutions'     | 'a VM in continuous mode'              | 'normal mode' || true     | 0               | false | false | true       || 0          | 0           | false   | false
      'already recorded with no solutions'     | 'a VM for one error'                   | 'normal mode' || true     | 0               | false | false | false      || 0          | 0           | false   | false

      'not already recorded with solutions'    | 'an OSGi container in continuous mode' | 'debug mode'  || false    | 2               | true  | true  | true       || 1          | 0           | false   | false
      'not already recorded with solutions'    | 'an OSGi container for one error'      | 'debug mode'  || false    | 2               | true  | true  | false      || 1          | 0           | true    | false
      'not already recorded with solutions'    | 'a VM in continuous mode'              | 'debug mode'  || false    | 2               | true  | false | true       || 1          | 0           | false   | false
      'not already recorded with solutions'    | 'a VM for one error'                   | 'debug mode'  || false    | 2               | true  | false | false      || 1          | 0           | true    | false
      'not already recorded with solutions'    | 'an OSGi container in continuous mode' | 'normal mode' || false    | 2               | false | true  | true       || 0          | 1           | false   | false
      'not already recorded with solutions'    | 'an OSGi container for one error'      | 'normal mode' || false    | 2               | false | true  | false      || 0          | 1           | true    | false
      'not already recorded with solutions'    | 'a VM in continuous mode'              | 'normal mode' || false    | 2               | false | false | true       || 0          | 1           | false   | false
      'not already recorded with solutions'    | 'a VM for one error'                   | 'normal mode' || false    | 2               | false | false | false      || 0          | 1           | true    | false

      'already recorded with solutions'        | 'an OSGi container in continuous mode' | 'debug mode'  || true     | 2               | true  | true  | true       || 0          | 0           | false   | false
      'already recorded with solutions'        | 'an OSGi container for one error'      | 'debug mode'  || true     | 2               | true  | true  | false      || 0          | 0           | false   | false
      'already recorded with solutions'        | 'a VM in continuous mode'              | 'debug mode'  || true     | 2               | true  | false | true       || 0          | 0           | false   | false
      'already recorded with solutions'        | 'a VM for one error'                   | 'debug mode'  || true     | 2               | true  | false | false      || 0          | 0           | false   | false
      'already recorded with solutions'        | 'an OSGi container in continuous mode' | 'normal mode' || true     | 2               | false | true  | true       || 0          | 0           | false   | false
      'already recorded with solutions'        | 'an OSGi container for one error'      | 'normal mode' || true     | 2               | false | true  | false      || 0          | 0           | false   | false
      'already recorded with solutions'        | 'a VM in continuous mode'              | 'normal mode' || true     | 2               | false | false | true       || 0          | 0           | false   | false
      'already recorded with solutions'        | 'a VM for one error'                   | 'normal mode' || true     | 2               | false | false | false      || 0          | 0           | false   | false

      'not already recorded with one solution' | 'an OSGi container in continuous mode' | 'debug mode'  || false    | 1               | true  | true  | true       || 1          | 0           | false   | true
      'not already recorded with one solution' | 'an OSGi container for one error'      | 'debug mode'  || false    | 1               | true  | true  | false      || 1          | 0           | true    | false
      'not already recorded with one solution' | 'a VM in continuous mode'              | 'debug mode'  || false    | 1               | true  | false | true       || 1          | 0           | false   | true
      'not already recorded with one solution' | 'a VM for one error'                   | 'debug mode'  || false    | 1               | true  | false | false      || 1          | 0           | true    | false
      'not already recorded with one solution' | 'an OSGi container in continuous mode' | 'normal mode' || false    | 1               | false | true  | true       || 0          | 1           | false   | true
      'not already recorded with one solution' | 'an OSGi container for one error'      | 'normal mode' || false    | 1               | false | true  | false      || 0          | 1           | true    | false
      'not already recorded with one solution' | 'a VM in continuous mode'              | 'normal mode' || false    | 1               | false | false | true       || 0          | 1           | false   | true
      'not already recorded with one solution' | 'a VM for one error'                   | 'normal mode' || false    | 1               | false | false | false      || 0          | 1           | true    | false

      'already recorded with one solution'     | 'an OSGi container in continuous mode' | 'debug mode'  || true     | 1               | true  | true  | true       || 0          | 0           | false   | false
      'already recorded with one solution'     | 'an OSGi container for one error'      | 'debug mode'  || true     | 1               | true  | true  | false      || 0          | 0           | false   | false
      'already recorded with one solution'     | 'a VM in continuous mode'              | 'debug mode'  || true     | 1               | true  | false | true       || 0          | 0           | false   | false
      'already recorded with one solution'     | 'a VM for one error'                   | 'debug mode'  || true     | 1               | true  | false | false      || 0          | 0           | false   | false
      'already recorded with one solution'     | 'an OSGi container in continuous mode' | 'normal mode' || true     | 1               | false | true  | true       || 0          | 0           | false   | false
      'already recorded with one solution'     | 'an OSGi container for one error'      | 'normal mode' || true     | 1               | false | true  | false      || 0          | 0           | false   | false
      'already recorded with one solution'     | 'a VM in continuous mode'              | 'normal mode' || true     | 1               | false | false | true       || 0          | 0           | false   | false
      'already recorded with one solution'     | 'a VM for one error'                   | 'normal mode' || true     | 1               | false | false | false      || 0          | 0           | false   | false
  }
}
