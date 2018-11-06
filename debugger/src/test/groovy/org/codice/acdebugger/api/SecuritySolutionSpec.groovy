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

import org.codice.acdebugger.ReflectionSpecification
import spock.lang.Shared
import spock.lang.Unroll

class SecuritySolutionSpec extends ReflectionSpecification {
  static def PERMISSIONS = ['p1', 'p2', 'p3'] as Set<String>

  static def DOMAINS = ['d1', 'd2'] as Set<String>
  static def NO_DOMAINS = [] as Set<String>

  @Shared
  def SOME_CLASS = MockClassType('SOME_CLASS', 'Lpackage/SomeClass;')

  @Shared
  def SOME_OBJ = MockObjectReference('SOME_OBJ', SOME_CLASS)

  @Shared
  def FRAME1 = new StackFrameInformation('d1', 'file:1', SOME_CLASS, 'package.SomeClass', SOME_OBJ, 'instance of package.SomeClass')
  @Shared
  def FRAME2 = new StackFrameInformation('d2', 'file:2', SOME_CLASS, 'package.SomeClass', null, 'class of package.SomeClass')
  @Shared
  def FRAME3 = new StackFrameInformation('d3', 'file:3', SOME_CLASS, 'package.SomeClass', SOME_OBJ, 'instance of package.SomeClass')
  @Shared
  def FRAME0 = new StackFrameInformation('d0', 'file:0', SOME_CLASS, 'package.SomeClass', null, 'class of package.SomeClass')

  @Shared
  def STACK = [FRAME0, FRAME1, FRAME2, FRAME3]
  @Shared
  def STACK_WITH_DO_PRIVILEGED = [FRAME0, StackFrameInformation.DO_PRIVILEGED, FRAME1, FRAME2, FRAME3]

  @Shared
  def DO_PRIVILEGED = [FRAME2]

  @Shared
  def SOLUTION = new SecuritySolution(STACK, PERMISSIONS, DOMAINS, DO_PRIVILEGED)

  def "test constructor with a stack"() {
    expect:
      SOLUTION.originalStack == STACK
      SOLUTION.stack == STACK
      SOLUTION.permissions == PERMISSIONS
      SOLUTION.grantedDomains == DOMAINS
      SOLUTION.doPrivilegedLocations == DO_PRIVILEGED
  }

  def "test constructor with no stack and granted domains"() {
    when:
      def solution = new SecuritySolution(PERMISSIONS, DOMAINS)

    then:
      solution.print(false, '')
      solution.originalStack.isEmpty()
      solution.stack.isEmpty()
      solution.permissions == PERMISSIONS
      solution.grantedDomains == DOMAINS
      solution.doPrivilegedLocations.isEmpty()
  }

  def "test constructor with no stack and no granted domains"() {
    when:
      def solution = new SecuritySolution(PERMISSIONS, NO_DOMAINS)

    then:
      solution.print(false, '')
      solution.originalStack.isEmpty()
      solution.stack.isEmpty()
      solution.permissions == PERMISSIONS
      solution.grantedDomains == NO_DOMAINS
      solution.doPrivilegedLocations.isEmpty()
  }

  def "test copy constructor"() {
    when:
      def solution = new SecuritySolution(SOLUTION)


    then:
      solution.print(true, '')
      solution.originalStack == STACK
      solution.stack == STACK
      solution.permissions == PERMISSIONS
      solution.grantedDomains == DOMAINS
      solution.doPrivilegedLocations == DO_PRIVILEGED
  }

  def "test constructor that extends privileges"() {
    when:
      def solution = new SecuritySolution(SOLUTION, 1)

    then:
      solution.print(true, '')
      solution.originalStack == STACK
      solution.stack == STACK_WITH_DO_PRIVILEGED
      solution.permissions == PERMISSIONS
      solution.grantedDomains == DOMAINS
      solution.doPrivilegedLocations == [FRAME2, FRAME1]
  }

  def "test getPermissions() returns an unmodifiable set"() {
    when:
      SOLUTION.getPermissions().add('permission')

    then:
      thrown(UnsupportedOperationException)
  }

  def "test getGrantedDomains() returns an unmodifiable set"() {
    when:
      SOLUTION.getGrantedDomains().add('domain')

    then:
      thrown(UnsupportedOperationException)
  }

  def "test getDoPrivilegedLocations() returns an unmodifiable list"() {
    when:
      SOLUTION.getDoPrivilegedLocations().add(FRAME0)

    then:
      thrown(UnsupportedOperationException)
  }

  @Unroll
  def "test compareTo() when #when_what"() {
    when:
      def comparison = solution1.compareTo(solution2)

    then:
      (comparison > 0) ? 1 : ((comparison < 0) ? -1 : 0) == result

    where:
      when_what                                                        || solution1                                                                        | solution2                                                                                || result
      'identical'                                                      || SOLUTION                                                                         | SOLUTION                                                                                 || 0
      'equals'                                                         || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, DOMAINS, DO_PRIVILEGED)                         || 0
      'solution1 has more domains'                                     || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, ['a'] as Set<String>, DO_PRIVILEGED)            || 1
      'solution2 has less domains'                                     || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, ['a', 'b', 'c'] as Set<String>, DO_PRIVILEGED)  || -1
      'solution1 has more permissions'                                 || SOLUTION                                                                         | new SecuritySolution(STACK, ['a'] as Set<String>, DOMAINS, DO_PRIVILEGED)                || 1
      'solution2 has less permissions'                                 || SOLUTION                                                                         | new SecuritySolution(STACK, ['a', 'b', 'c', 'd'] as Set<String>, DOMAINS, DO_PRIVILEGED) || -1
      'solution1 has more frames'                                      || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, DOMAINS, [])                                    || 1
      'solution2 has less frames'                                      || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, DOMAINS, [FRAME1, FRAME2, FRAME3])              || -1
      'solutions have no domains and permissions and have same frames' || new SecuritySolution(STACK, [] as Set<String>, [] as Set<String>, DO_PRIVILEGED) | new SecuritySolution(STACK, [] as Set<String>, [] as Set<String>, DO_PRIVILEGED)         || 0
      'solution1 has later frame'                                      || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, DOMAINS, [FRAME0])                              || 1
      'solution1 has earlier frame'                                    || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, DOMAINS, [FRAME3])                              || -1
  }

  @Unroll
  def "test hashCode() when #when_what"() {
    expect:
      (solution1.hashCode() == solution2.hashCode()) == result

    where:
      when_what                  || solution1 | solution2                                                                      || result
      'equals'                   || SOLUTION  | SOLUTION                                                                       || true
      'permissions are different' | SOLUTION  | new SecuritySolution(STACK, ['a'] as Set<String>, DOMAINS, DO_PRIVILEGED)      || false
      'domains are different'     | SOLUTION  | new SecuritySolution(STACK, PERMISSIONS, ['a'] as Set<String>, DO_PRIVILEGED)  || false
      'frames are different'      | SOLUTION  | new SecuritySolution(STACK, PERMISSIONS, DOMAINS, ['a'])                       || false
      'everything is different'   | SOLUTION  | new SecuritySolution(STACK, ['a'] as Set<String>, ['a'] as Set<String>, ['a']) || false
  }

  @Unroll
  def "test equals() when #when_what"() {
    expect:
      solution1.equals(solution2) == result

    where:
      when_what                                                        || solution1                                                                        | solution2                                                                                || result
      'identical'                                                      || SOLUTION                                                                         | SOLUTION                                                                                 || true
      'equals'                                                         || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, DOMAINS, DO_PRIVILEGED)                         || true
      'the other is null'                                              || SOLUTION                                                                         | null                                                                                     || false
      'the other is not a SecuritySolution'                            || SOLUTION                                                                         | 'abc'                                                                                    || false
      'solution1 has more domains'                                     || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, ['a'] as Set<String>, DO_PRIVILEGED)            || false
      'solution2 has less domains'                                     || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, ['a', 'b', 'c'] as Set<String>, DO_PRIVILEGED)  || false
      'solution1 has more permissions'                                 || SOLUTION                                                                         | new SecuritySolution(STACK, ['a'] as Set<String>, DOMAINS, DO_PRIVILEGED)                || false
      'solution2 has less permissions'                                 || SOLUTION                                                                         | new SecuritySolution(STACK, ['a', 'b', 'c', 'd'] as Set<String>, DOMAINS, DO_PRIVILEGED) || false
      'solution1 has more frames'                                      || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, DOMAINS, [])                                    || false
      'solution2 has less frames'                                      || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, DOMAINS, [FRAME1, FRAME2, FRAME3])              || false
      'solutions have no domains and permissions and have same frames' || new SecuritySolution(STACK, [] as Set<String>, [] as Set<String>, DO_PRIVILEGED) | new SecuritySolution(STACK, [] as Set<String>, [] as Set<String>, DO_PRIVILEGED)         || true
      'solution1 has earlier frame'                                    || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, DOMAINS, [FRAME3])                              || false
      'solution1 has later frame'                                      || SOLUTION                                                                         | new SecuritySolution(STACK, PERMISSIONS, DOMAINS, [FRAME0])                              || false
  }
}