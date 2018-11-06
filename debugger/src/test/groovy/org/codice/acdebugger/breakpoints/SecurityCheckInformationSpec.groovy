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

import com.sun.jdi.ObjectReference
import com.sun.jdi.Type
import org.codice.acdebugger.ReflectionSpecification
import org.codice.acdebugger.api.Debug
import org.codice.acdebugger.api.SecuritySolution
import org.codice.acdebugger.api.StackFrameInformation
import spock.lang.Shared
import spock.lang.Unroll

class SecurityCheckInformationSpec extends ReflectionSpecification {
  static def PERMISSION_INFO = 'java.io.FilePermission "${/}etc${/}some-dir", "read"'
  static def BUNDLE1 = 'bundle.name1'
  static def BUNDLE2 = 'bundle.name2'
  static def BUNDLE3 = 'bundle.name3'
  static def BUNDLE4 = 'bundle.name4'
  static def BUNDLE5 = 'bundle.name5'
  static def DOMAIN2_TYPE = 'domain.type2'
  static def PROXY_BUNDLE = 'proxy.bundle'
  static def COMBINED_BUNDLE = 'combined.bundle.name'
  static def ACCEPTABLE_BUNDLE = 'acceptable.bundle.name'

  @Shared
  def PERMISSION = Stub(ObjectReference)

  @Shared
  def PERMISSION_INFOS = [PERMISSION_INFO] as Set<String>

  @Shared
  def BOOT_DOMAIN = Stub(ObjectReference)
  @Shared
  def DOMAIN1 = Stub(ObjectReference)
  @Shared
  def DOMAIN2 = Stub(ObjectReference) {
    type() >> Mock(Type) {
      name() >> DOMAIN2_TYPE
    }
  }
  @Shared
  def DOMAIN3 = Stub(ObjectReference)
  @Shared
  def DOMAIN4 = Stub(ObjectReference)
  @Shared
  def DOMAIN5 = Stub(ObjectReference)
  @Shared
  def PROXY_DOMAIN = Stub(ObjectReference)
  @Shared
  def COMBINED_DOMAIN = Stub(ObjectReference)
  @Shared
  def ACCEPTABLE_DOMAIN = Stub(ObjectReference)

  @Shared
  def AC_CLASS = MockClassType('AC_CLASS', 'Ljava/security/AccessController;')
  @Shared
  def ACC_CLASS = MockClassType('ACC_CLASS', 'Ljava/security/AccessControlContext;')
  @Shared
  def SUBJECT_CLASS = MockClassType('SUBJECT_CLASS', 'Ljavax/security/auth/Subject;')
  @Shared
  def PROXY_INTERFACE = MockInterfaceType('PROXY_INTERFACE', 'Ljava/lang/reflect/Proxy;')
  @Shared
  def PROXY_CLASS = MockClassType('PROXY_CLASS', 'Lsome/ProxyClass$123;', interfaces: PROXY_INTERFACE)
  @Shared
  def ACCEPTABLE_CLASS = MockClassType('ACCEPTABLE_CLASS', 'Lorg/apache/pdfbox/pdmodel/font/FileSystemFontProvider;')
  @Shared
  def CLASS1 = MockClassType('CLASS1', 'Lsome/Class1;')
  @Shared
  def CLASS2 = MockClassType('CLASS2', 'Lsome/Class2;')
  @Shared
  def CLASS3 = MockClassType('CLASS3', 'Lsome/Class3;')
  @Shared
  def CLASS4 = MockClassType('CLASS4', 'Lsome/Class4;')
  @Shared
  def CLASS5 = MockClassType('CLASS5', 'Lsome/Class5;')

  @Shared
  def ACC_OBJ = MockObjectReference('ACC_OBJ', ACC_CLASS)
  @Shared
  def PROXY_OBJ = MockObjectReference('PROXY_OBJ', PROXY_CLASS)
  @Shared
  def OBJ1 = MockObjectReference('OBJ1', CLASS1)
  @Shared
  def OBJ2 = MockObjectReference('OBJ2', CLASS2)
  @Shared
  def OBJ3 = MockObjectReference('OBJ3', CLASS3)

  @Shared
  def DO_PRIVILEGED_FRAME = new StackFrameInformation(
      null,
      'java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)',
      AC_CLASS,
      'java.security.AccessController',
      null,
      'class of java.security.AccessController'
  )
  @Shared
  def ACC_CHECK_FRAME = new StackFrameInformation(
      null,
      'java.security.AccessControllerContext.checkPermission(472)',
      ACC_CLASS,
      'jeva.security.AccessControllerContext',
      ACC_OBJ,
      'instance of java.security.AccessControllerContext'
  )
  @Shared
  def DO_AS_FRAME = new StackFrameInformation(
      null,
      'javax.security.auth.Subject:422',
      SUBJECT_CLASS,
      'javax.security.auth.Subject',
      null,
      'class of javax.security.auth.Subject'
  )
  @Shared
  def PROXY_FRAME = new StackFrameInformation(
      PROXY_BUNDLE,
      'some.ProxyClass$123.delegate(java.lang.Object:12)',
      PROXY_CLASS,
      'some.ProxyClass$123',
      PROXY_OBJ,
      'instance of some.ProxyClass$123'
  )
  @Shared
  def ACCEPTABLE_FRAME = new StackFrameInformation(
      ACCEPTABLE_BUNDLE,
      'org.apache.pdfbox.pdmodel.font.FileSystemFontProvider.<init>(:214)',
      ACCEPTABLE_CLASS,
      'org.apache.pdfbox.pdmodel.font.FileSystemFontProvider',
      null,
      'class of org.apache.pdfbox.pdmodel.font.FileSystemFontProvider'
  )
  @Shared
  def FRAME1A = new StackFrameInformation(
      BUNDLE1,
      'some.Class1.openFile(frame1a:123)',
      CLASS1,
      'some.Class1',
      OBJ1,
      'instance of some.Class1'
  )
  @Shared
  def FRAME1B = new StackFrameInformation(
      BUNDLE1,
      'some.Class1.walkDir(frame1b:123)',
      CLASS1,
      'some.Class1',
      OBJ1,
      'instance of some.Class1'
  )
  @Shared
  def FRAME2A = new StackFrameInformation(
      BUNDLE2,
      'some.Class2.doThat0(frame2a:155)',
      CLASS2,
      'some.Class2',
      OBJ2,
      'instance of some.Class2'
  )
  @Shared
  def FRAME2B = new StackFrameInformation(
      BUNDLE2,
      'some.Class2.doThat(frame2b:623)',
      CLASS2,
      'some.Class2',
      OBJ2,
      'instance of some.Class2'
  )
  @Shared
  def FRAME3 = new StackFrameInformation(
      BUNDLE3,
      'some.Class3.rollback(frame3:45)',
      CLASS3,
      'some.Class3',
      OBJ3,
      'instance of some.Class3'
  )
  @Shared
  def FRAME4 = new StackFrameInformation(
      BUNDLE4,
      'some.Class4.login(frame4:2222)',
      CLASS4,
      'some.Class4',
      null,
      'class of some.Class4'
  )
  @Shared
  def FRAME5 = new StackFrameInformation(
      BUNDLE5,
      'some.Class5.login(frame5:72)',
      CLASS5,
      'some.Class5',
      null,
      'class of some.Class5'
  )

  @Shared
  def STACK = [
      ACC_CHECK_FRAME,
      FRAME1A,
      FRAME2A,
      FRAME3,
      FRAME1B,
      FRAME4,
      FRAME5
  ]
  @Shared
  def DOMAINS = [BOOT_DOMAIN, DOMAIN1, DOMAIN2, DOMAIN3, DOMAIN1, DOMAIN4, DOMAIN5]
  @Shared
  def BUNDLES = [null, BUNDLE1, BUNDLE2, BUNDLE3, BUNDLE1, BUNDLE4, BUNDLE5]
  @Shared
  def ACC1 = new AccessControlContextInfo(DOMAINS, BUNDLES, 2, PERMISSION, PERMISSION_INFOS, [null, BUNDLE1, BUNDLE2, BUNDLE3] as Set<String>)
  @Shared
  def ACC2 = new AccessControlContextInfo(DOMAINS, BUNDLES, 2, PERMISSION, PERMISSION_INFOS, [null, BUNDLE1, BUNDLE2, BUNDLE3, BUNDLE4] as Set<String>)
  @Shared
  def ACC_WITH_COMBINED_DOMAIN = new AccessControlContextInfo(DOMAINS + COMBINED_DOMAIN, BUNDLES + COMBINED_BUNDLE, 2, PERMISSION, PERMISSION_INFOS, [null, BUNDLE1, BUNDLE2, BUNDLE3, COMBINED_BUNDLE] as Set<String>)
  @Shared
  def ACC2_WITH_COMBINED_DOMAIN = new AccessControlContextInfo(DOMAINS + COMBINED_DOMAIN, BUNDLES + COMBINED_BUNDLE, 2, PERMISSION, PERMISSION_INFOS, [null, BUNDLE1, BUNDLE2, BUNDLE3] as Set<String>)
  @Shared
  def SOLUTION1 = new SecuritySolution(STACK, StackFrameInformation.doPrivilegedAt(STACK, 1), PERMISSION_INFOS, [] as Set<String>, [FRAME1A])
  @Shared
  def SOLUTION2 = new SecuritySolution(STACK, StackFrameInformation.doPrivilegedAt(STACK, 2), PERMISSION_INFOS, [] as Set<String>, [FRAME2A])
  @Shared
  def SOLUTION3 = new SecuritySolution(STACK, StackFrameInformation.doPrivilegedAt(STACK, 3), PERMISSION_INFOS, [] as Set<String>, [FRAME3])
  @Shared
  def SOLUTION4 = new SecuritySolution(STACK, StackFrameInformation.doPrivilegedAt(STACK, 4), PERMISSION_INFOS, [] as Set<String>, [FRAME1B])
  @Shared
  def SOLUTION5 = new SecuritySolution(STACK, StackFrameInformation.doPrivilegedAt(STACK, 5), PERMISSION_INFOS, [BUNDLE4] as Set<String>, [FRAME4])
  @Shared
  def SOLUTION6 = new SecuritySolution(STACK, PERMISSION_INFOS, [BUNDLE4, BUNDLE5] as Set<String>, [])
  @Shared
  def SOLUTION7 = new SecuritySolution(STACK, StackFrameInformation.doPrivilegedAt(STACK, 5), PERMISSION_INFOS, [] as Set<String>, [FRAME4])
  @Shared
  def SOLUTION8 = new SecuritySolution(STACK, PERMISSION_INFOS, [BUNDLE5] as Set<String>, [])
  @Shared
  def SOLUTION9 = new SecuritySolution(STACK, PERMISSION_INFOS, [BUNDLE4, BUNDLE5, COMBINED_BUNDLE] as Set<String>, [])

  @Shared
  def STACK_WITH_PROXY = [
      ACC_CHECK_FRAME,
      FRAME1A,
      PROXY_FRAME,
      FRAME2A
  ]
  @Shared
  def DOMAINS_WITH_PROXY = [BOOT_DOMAIN, DOMAIN1, PROXY_DOMAIN, DOMAIN2]
  @Shared
  def BUNDLES_WITH_PROXY = [null, BUNDLE1, PROXY_BUNDLE, BUNDLE2]
  @Shared
  def ACC1_WITH_PROXY = new AccessControlContextInfo(DOMAINS_WITH_PROXY, BUNDLES_WITH_PROXY, 1, PERMISSION, PERMISSION_INFOS, [null, BUNDLE2] as Set<String>)
  @Shared
  def ACC2_WITH_PROXY = new AccessControlContextInfo(DOMAINS_WITH_PROXY, BUNDLES_WITH_PROXY, 2, PERMISSION, PERMISSION_INFOS, [null, BUNDLE1] as Set<String>)
  @Shared
  def ACC3_WITH_PROXY = new AccessControlContextInfo(DOMAINS_WITH_PROXY, BUNDLES_WITH_PROXY, 3, PERMISSION, PERMISSION_INFOS, [null, BUNDLE1, PROXY_BUNDLE] as Set<String>)
  @Shared
  def SOLUTION1_WITH_PROXY = new SecuritySolution(STACK_WITH_PROXY, StackFrameInformation.doPrivilegedAt(STACK_WITH_PROXY, 1), PERMISSION_INFOS, [BUNDLE1] as Set<String>, [FRAME1A])
  @Shared
  def SOLUTION2_WITH_PROXY = new SecuritySolution(STACK_WITH_PROXY, PERMISSION_INFOS, [BUNDLE1, PROXY_BUNDLE] as Set<String>, [])
  @Shared
  def SOLUTION3_WITH_PROXY = new SecuritySolution(STACK_WITH_PROXY, StackFrameInformation.doPrivilegedAt(STACK_WITH_PROXY, 1), PERMISSION_INFOS, [] as Set<String>, [FRAME1A])
  @Shared
  def SOLUTION4_WITH_PROXY = new SecuritySolution(STACK_WITH_PROXY, PERMISSION_INFOS, [PROXY_BUNDLE, BUNDLE2] as Set<String>, [])
  @Shared
  def SOLUTION5_WITH_PROXY = new SecuritySolution(STACK_WITH_PROXY, PERMISSION_INFOS, [BUNDLE2] as Set<String>, [])

  @Shared
  def STACK_WITH_DO_PRIVILEGED = [
      ACC_CHECK_FRAME,
      FRAME1A,
      FRAME2A,
      FRAME2B,
      FRAME3,
      PROXY_FRAME,
      FRAME1B,
      DO_PRIVILEGED_FRAME,
      FRAME4,
      FRAME5
  ]
  @Shared
  def DOMAINS_WITH_DO_PRIVILEGED = [BOOT_DOMAIN, DOMAIN1, DOMAIN2, DOMAIN2, DOMAIN3, PROXY_DOMAIN, DOMAIN1, COMBINED_DOMAIN]
  @Shared
  def BUNDLES_WITH_DO_PRIVILEGED = [null, BUNDLE1, BUNDLE2, BUNDLE2, BUNDLE3, PROXY_BUNDLE, BUNDLE1, COMBINED_BUNDLE]
  @Shared
  def ACC_WITH_DO_PRIVILEGED = new AccessControlContextInfo(DOMAINS_WITH_DO_PRIVILEGED, BUNDLES_WITH_DO_PRIVILEGED, 2, PERMISSION, PERMISSION_INFOS, [null, BUNDLE1] as Set<String>)
  @Shared
  def SOLUTION1_WITH_DO_PRIVILEGED = new SecuritySolution(STACK_WITH_DO_PRIVILEGED, StackFrameInformation.doPrivilegedAt(STACK_WITH_DO_PRIVILEGED, 1), PERMISSION_INFOS, [] as Set<String>, [FRAME1A])
  @Shared
  def SOLUTION2_WITH_DO_PRIVILEGED = new SecuritySolution(STACK_WITH_DO_PRIVILEGED, StackFrameInformation.doPrivilegedAt(STACK_WITH_DO_PRIVILEGED, 2), PERMISSION_INFOS, [BUNDLE2] as Set<String>, [FRAME2A])
  @Shared
  def SOLUTION3_WITH_DO_PRIVILEGED = new SecuritySolution(STACK_WITH_DO_PRIVILEGED, StackFrameInformation.doPrivilegedAt(STACK_WITH_DO_PRIVILEGED, 3), PERMISSION_INFOS, [BUNDLE2] as Set<String>, [FRAME2B])
  @Shared
  def SOLUTION4_WITH_DO_PRIVILEGED = new SecuritySolution(STACK_WITH_DO_PRIVILEGED, StackFrameInformation.doPrivilegedAt(STACK_WITH_DO_PRIVILEGED, 4), PERMISSION_INFOS, [BUNDLE2, BUNDLE3] as Set<String>, [FRAME3])
  @Shared
  def SOLUTION5_WITH_DO_PRIVILEGED = new SecuritySolution(STACK_WITH_DO_PRIVILEGED, StackFrameInformation.doPrivilegedAt(STACK_WITH_DO_PRIVILEGED, 6), PERMISSION_INFOS, [BUNDLE2, BUNDLE3, PROXY_BUNDLE] as Set<String>, [FRAME1B])
  @Shared
  def SOLUTION6_WITH_DO_PRIVILEGED = new SecuritySolution(STACK_WITH_DO_PRIVILEGED, PERMISSION_INFOS, [BUNDLE2, BUNDLE3, PROXY_BUNDLE, BUNDLE4, COMBINED_BUNDLE] as Set<String>, [])

  @Shared
  def STACK_WITH_DO_AS = [
      ACC_CHECK_FRAME,
      FRAME1A,
      FRAME2A, // <--
      FRAME2B,
      DO_PRIVILEGED_FRAME, // this stack break is ignored because the doAs() on next frame
      DO_AS_FRAME,
      FRAME3,
      FRAME1B,
      DO_PRIVILEGED_FRAME,
      FRAME4 // ----------------
  ]
  @Shared
  def DOMAINS_WITH_DO_AS = [BOOT_DOMAIN, DOMAIN1, DOMAIN2, DOMAIN2, BOOT_DOMAIN, BOOT_DOMAIN, DOMAIN3, DOMAIN1, COMBINED_DOMAIN]
  @Shared
  def BUNDLES_WITH_DO_AS = [null, BUNDLE1, BUNDLE2, BUNDLE2, null, null, BUNDLE3, BUNDLE1, COMBINED_BUNDLE]
  @Shared
  def ACC_WITH_DO_AS = new AccessControlContextInfo(DOMAINS_WITH_DO_AS, BUNDLES_WITH_DO_AS, 2, PERMISSION, PERMISSION_INFOS, [null, BUNDLE1, BUNDLE4] as Set<String>)
  @Shared
  def SOLUTION1_WITH_DO_AS = new SecuritySolution(STACK_WITH_DO_AS, StackFrameInformation.doPrivilegedAt(STACK_WITH_DO_AS, 1), PERMISSION_INFOS, [] as Set<String>, [FRAME1A])
  @Shared
  def SOLUTION2_WITH_DO_AS = new SecuritySolution(STACK_WITH_DO_AS, StackFrameInformation.doPrivilegedAt(STACK_WITH_DO_AS, 2), PERMISSION_INFOS, [BUNDLE2] as Set<String>, [FRAME2A])
  @Shared
  def SOLUTION3_WITH_DO_AS = new SecuritySolution(STACK_WITH_DO_AS, StackFrameInformation.doPrivilegedAt(STACK_WITH_DO_AS, 3), PERMISSION_INFOS, [BUNDLE2] as Set<String>, [FRAME2B])
  @Shared
  def SOLUTION4_WITH_DO_AS = new SecuritySolution(STACK_WITH_DO_AS, PERMISSION_INFOS, [BUNDLE2, BUNDLE3, COMBINED_BUNDLE] as Set<String>, [])

  @Shared
  def STACK_WITH_ACCEPTABLE = [
      ACC_CHECK_FRAME,
      ACCEPTABLE_FRAME,
      FRAME2A
  ]
  @Shared
  def DOMAINS_WITH_ACCEPTABLE = [BOOT_DOMAIN, ACCEPTABLE_DOMAIN, DOMAIN2]
  @Shared
  def BUNDLES_WITH_ACCEPTABLE = [null, ACCEPTABLE_BUNDLE, BUNDLE2]
  @Shared
  def ACC_WITH_ACCEPTABLE = new AccessControlContextInfo(DOMAINS_WITH_ACCEPTABLE, BUNDLES_WITH_ACCEPTABLE, 1, PERMISSION, PERMISSION_INFOS, [null, BUNDLE2] as Set<String>)

  @Unroll
  def "test when #when_what while#analyzing_or_not analyzing doPrivileged() blocks"() {
    given:
      def debug = Mock(Debug) {
        threadStack() >> stack
        canDoPrivilegedBlocks() >> can_do_privileged_blocks
        reflection() >> REFLECTION
      }

    when:
      def info = new SecurityCheckInformation(debug, acc)
      def analysis = info.analyze()

      info.dump(true, '')
      info.toString()
      analysis.each { it.toString() }

    then:
      info.permissions == PERMISSION_INFOS
      info.stack == stack
      info.acceptable == acceptable
      if (acceptable) {
        info.acceptablePermissions.startsWith("REGEX: ")
      } else {
        info.acceptablePermissions == null
      }
      info.computedDomains == computed_domains
      info.failedDomain == failed_bundle
      info.failedStackIndex == failed_stack_index
      info.privilegedStackIndex == privileged_stack_index
      info.failedDomainIndex == failed_domain_index
      info.combinedDomainsStartIndex == combined_index
      info.context.is(acc)
      analysis == solutions
      if (analysis) {
        analysis.eachWithIndex { a, i ->
          assert a.stack == solutions[i].stack
          assert a.originalStack == solutions[i].originalStack
          assert a.grantedDomains == solutions[i].grantedDomains
          assert a.doPrivilegedLocations == solutions[i].doPrivilegedLocations
        }
      }

    where:
      when_what                                                                                               | analyzing_or_not || stack                    | acc                       | can_do_privileged_blocks || acceptable | computed_domains                                                    | failed_bundle     | failed_stack_index | privileged_stack_index | failed_domain_index | combined_index || solutions
      'no doPrivileged() calls, no proxies, and no combined domains and failing towards the end of the stack' | ''               || STACK                    | ACC1                      | true                     || false      | [BUNDLE1, BUNDLE2, BUNDLE3, BUNDLE4, BUNDLE5]                       | BUNDLE4           | 5                  | -1                     | 3                   | -1             || [SOLUTION1, SOLUTION2, SOLUTION3, SOLUTION4, SOLUTION5, SOLUTION6]
      'no doPrivileged() calls, no proxies, and no combined domains and failing towards the end of the stack' | ' not'           || STACK                    | ACC1                      | false                    || false      | [BUNDLE1, BUNDLE2, BUNDLE3, BUNDLE4, BUNDLE5]                       | BUNDLE4           | 5                  | -1                     | 3                   | -1             || [SOLUTION6]
      'no doPrivileged() calls, no proxies, and no combined domains and failing at the end of the stack'      | ''               || STACK                    | ACC2                      | true                     || false      | [BUNDLE1, BUNDLE2, BUNDLE3, BUNDLE4, BUNDLE5]                       | BUNDLE5           | 6                  | -1                     | 4                   | -1             || [SOLUTION1, SOLUTION2, SOLUTION3, SOLUTION4, SOLUTION7, SOLUTION8]
      'no doPrivileged() calls, no proxies, and no combined domains and failing at the end of the stack'      | ' not'           || STACK                    | ACC2                      | false                    || false      | [BUNDLE1, BUNDLE2, BUNDLE3, BUNDLE4, BUNDLE5]                       | BUNDLE5           | 6                  | -1                     | 4                   | -1             || [SOLUTION8]
      'a proxy class and failing after'                                                                       | ''               || STACK_WITH_PROXY         | ACC1_WITH_PROXY           | true                     || false      | [BUNDLE1, PROXY_BUNDLE, BUNDLE2]                                    | BUNDLE1           | 1                  | -1                     | 0                   | -1             || [SOLUTION1_WITH_PROXY, SOLUTION2_WITH_PROXY]
      'a proxy class and failing after'                                                                       | ' not'           || STACK_WITH_PROXY         | ACC1_WITH_PROXY           | false                    || false      | [BUNDLE1, PROXY_BUNDLE, BUNDLE2]                                    | BUNDLE1           | 1                  | -1                     | 0                   | -1             || [SOLUTION2_WITH_PROXY]
      'a proxy class and failing on the proxy'                                                                | ''               || STACK_WITH_PROXY         | ACC2_WITH_PROXY           | true                     || false      | [BUNDLE1, PROXY_BUNDLE, BUNDLE2]                                    | PROXY_BUNDLE      | 2                  | -1                     | 1                   | -1             || [SOLUTION3_WITH_PROXY, SOLUTION4_WITH_PROXY]
      'a proxy class and failing on the proxy'                                                                | ' not'           || STACK_WITH_PROXY         | ACC2_WITH_PROXY           | false                    || false      | [BUNDLE1, PROXY_BUNDLE, BUNDLE2]                                    | PROXY_BUNDLE      | 2                  | -1                     | 1                   | -1             || [SOLUTION4_WITH_PROXY]
      'a proxy class and failing before'                                                                      | ''               || STACK_WITH_PROXY         | ACC3_WITH_PROXY           | true                     || false      | [BUNDLE1, PROXY_BUNDLE, BUNDLE2]                                    | BUNDLE2           | 3                  | -1                     | 2                   | -1             || [SOLUTION3_WITH_PROXY, SOLUTION5_WITH_PROXY]
      'a proxy class and failing before'                                                                      | ' not'           || STACK_WITH_PROXY         | ACC3_WITH_PROXY           | false                    || false      | [BUNDLE1, PROXY_BUNDLE, BUNDLE2]                                    | BUNDLE2           | 3                  | -1                     | 2                   | -1             || [SOLUTION5_WITH_PROXY]
      'with a combined domain that has permissions'                                                           | ''               || STACK                    | ACC_WITH_COMBINED_DOMAIN  | true                     || false      | [BUNDLE1, BUNDLE2, BUNDLE3, BUNDLE4, BUNDLE5, COMBINED_BUNDLE]      | BUNDLE4           | 5                  | -1                     | 3                   | 5              || [SOLUTION1, SOLUTION2, SOLUTION3, SOLUTION4, SOLUTION5, SOLUTION6]
      'with a combined domain that does not have permissions'                                                 | ''               || STACK                    | ACC2_WITH_COMBINED_DOMAIN | true                     || false      | [BUNDLE1, BUNDLE2, BUNDLE3, BUNDLE4, BUNDLE5, COMBINED_BUNDLE]      | BUNDLE4           | 5                  | -1                     | 3                   | 5              || [SOLUTION1, SOLUTION2, SOLUTION3, SOLUTION4, SOLUTION5, SOLUTION9]
      'no doPrivileged() calls, no proxies, and no combined domains and failing towards the end of the stack' | ' not'           || STACK                    | ACC1                      | false                    || false      | [BUNDLE1, BUNDLE2, BUNDLE3, BUNDLE4, BUNDLE5]                       | BUNDLE4           | 5                  | -1                     | 3                   | -1             || [SOLUTION6]
      'a class calls doPrivileged()'                                                                          | ''               || STACK_WITH_DO_PRIVILEGED | ACC_WITH_DO_PRIVILEGED    | true                     || false      | [BUNDLE1, BUNDLE2, BUNDLE3, PROXY_BUNDLE, BUNDLE4, COMBINED_BUNDLE] | BUNDLE2           | 2                  | 8                      | 1                   | 5              || [SOLUTION1_WITH_DO_PRIVILEGED, SOLUTION2_WITH_DO_PRIVILEGED, SOLUTION3_WITH_DO_PRIVILEGED, SOLUTION4_WITH_DO_PRIVILEGED, SOLUTION5_WITH_DO_PRIVILEGED, SOLUTION6_WITH_DO_PRIVILEGED]
      'a class calls doPrivileged()'                                                                          | ' not'           || STACK_WITH_DO_PRIVILEGED | ACC_WITH_DO_PRIVILEGED    | false                    || false      | [BUNDLE1, BUNDLE2, BUNDLE3, PROXY_BUNDLE, BUNDLE4, COMBINED_BUNDLE] | BUNDLE2           | 2                  | 8                      | 1                   | 5              || [SOLUTION6_WITH_DO_PRIVILEGED]
      'a class calls Subject.doAs()'                                                                          | ''               || STACK_WITH_DO_AS         | ACC_WITH_DO_AS            | true                     || false      | [BUNDLE1, BUNDLE2, BUNDLE3, BUNDLE4, COMBINED_BUNDLE]               | BUNDLE2           | 2                  | 9                      | 1                   | 4              || [SOLUTION1_WITH_DO_AS, SOLUTION2_WITH_DO_AS, SOLUTION3_WITH_DO_AS, SOLUTION4_WITH_DO_AS]
      'a class calls Subject.doAs()'                                                                          | ' not'           || STACK_WITH_DO_AS         | ACC_WITH_DO_AS            | false                    || false      | [BUNDLE1, BUNDLE2, BUNDLE3, BUNDLE4, COMBINED_BUNDLE]               | BUNDLE2           | 2                  | 9                      | 1                   | 4              || [SOLUTION4_WITH_DO_AS]
      'a class calls something marked acceptable'                                                             | ''               || STACK_WITH_ACCEPTABLE    | ACC_WITH_ACCEPTABLE       | true                     || true       | [ACCEPTABLE_BUNDLE, BUNDLE2]                                        | ACCEPTABLE_BUNDLE | 1                  | -1                     | 0                   | -1             || []
  }

  def "test when unable to find the location for a domain"() {
    given:
      def bundles = [null, BUNDLE1, null, BUNDLE3, BUNDLE1, BUNDLE4, BUNDLE5]
      def debug = Mock(Debug) {
        threadStack() >> STACK
        canDoPrivilegedBlocks() >> true
        reflection() >> REFLECTION
      }
      def acc = new AccessControlContextInfo(DOMAINS, bundles, 2, PERMISSION, PERMISSION_INFOS, [null, BUNDLE1, BUNDLE3] as Set<String>)

    when:
      new SecurityCheckInformation(debug, acc)

    then:
      def e = thrown(Error)

      e.message.contains('unable to find location')
      e.message.contains(DOMAIN2_TYPE)
  }

  def "test when recomputing cannot find a domain computed from the stack"() {
    given:
      def stack = [
          ACC_CHECK_FRAME,
          FRAME1A,
          new StackFrameInformation(
              'other-name-found-by-error-for-bundle-2',
              'some.Class2.doThat0(frame2a:155)',
              CLASS2,
              'some.Class2',
              OBJ2,
              'instance of some.Class2'
          ),
          FRAME3,
          FRAME1B,
          FRAME4,
          FRAME5
      ]
      def debug = Mock(Debug) {
        threadStack() >> stack
        canDoPrivilegedBlocks() >> true
        reflection() >> REFLECTION
      }
      def acc = new AccessControlContextInfo(DOMAINS, BUNDLES, 2, PERMISSION, PERMISSION_INFOS, [null, BUNDLE1] as Set<String>)

    when:
      new SecurityCheckInformation(debug, acc)

    then:
      def e = thrown(Error)

      e.message.contains('unable to find a domain computed from the stack')
  }
}
