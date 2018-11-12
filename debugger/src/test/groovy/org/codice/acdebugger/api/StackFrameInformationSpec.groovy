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
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class StackFrameInformationSpec extends Specification {
  static def DOMAIN = 'file:/root/location/here'
  static def BUNDLE = 'my.bundle'
  static def LOCATION_STR = 'location(4)'
  static def CLASS_NAME = 'class.name'

  @Shared
  def CLASS = Mock(ReferenceType) {
    name() >> CLASS_NAME
  }

  @Shared
  def LOCATION = Mock(Location) {
    toString() >> LOCATION_STR
    declaringType() >> CLASS
  }

  @Shared
  def OBJECT = Mock(ObjectReference) {
    toString() >> "instance of $CLASS_NAME"
  }

  @Shared
  def INFO = new StackFrameInformation(DOMAIN, LOCATION, OBJECT)

  @Shared
  def OSGI_DEBUG = Mock(Debug) {
    isOSGi() >> true
    reflection() >> Mock(ReflectionUtil) {
      isInstance(_, _) >> false
    }
  }

  @Shared
  def NON_OSGI_DEBUG = Mock(Debug) {
    isOSGi() >> false
    reflection() >> Mock(ReflectionUtil) {
      isInstance(_, _) >> false
    }
  }

  @Shared
  def FRAME1 = Stub(StackFrameInformation)
  @Shared
  def FRAME2 = Stub(StackFrameInformation)
  @Shared
  def FRAME3 = Stub(StackFrameInformation)
  @Shared
  def FRAME0 = Stub(StackFrameInformation)

  @Unroll
  def "test constructor with a location, #with_what"() {
    when:
      def info = new StackFrameInformation(domain, LOCATION, obj)

    then:
      info.getDomain() == domain
      info.getLocation() == LOCATION_STR
      info.getThisObject() == obj
      info.getLocationClass() == CLASS
      info.getLocationClassName() == CLASS_NAME
      info.getClassOrInstanceAtLocation() == classOrInstance

    where:
      with_what                            || domain | obj    || classOrInstance
      'a domain, and an object reference'  || DOMAIN | OBJECT || "instance of $CLASS_NAME"
      'no domain, and an object reference' || null   | OBJECT || "instance of $CLASS_NAME"
      'a domain, and no object reference'  || DOMAIN | null   || "class of $CLASS_NAME"
      'no domain, and no object reference' || null   | null   || "class of $CLASS_NAME"
  }

  @Unroll
  def "test canDoPrivilegedBlocks() with 3rd party bundle: #bundle"() {
    expect:
      !new StackFrameInformation(bundle, LOCATION, OBJECT).canDoPrivilegedBlocks(OSGI_DEBUG)

    where:
      bundle << getClass().getResource('/thirdparty-prefixes.txt').readLines().findAll {
        !(it.isEmpty() || it =~ /#.*/)
      }.collect {
        it + '.some.bundle'
      } + null
  }

  @Unroll
  def "test canDoPrivilegedBlocks() with 3rd party domain: #domain"() {
    expect:
      !new StackFrameInformation(domain, LOCATION, OBJECT).canDoPrivilegedBlocks(NON_OSGI_DEBUG)

    where:
      domain << getClass().getResource('/thirdparty-patterns.txt').readLines().findAll {
        !(it.isEmpty() || it =~ /#.*/)
      }.collect {
        it.replace('.*\\.', '1.3.4.').replace('.*', 'file:/some/location/')
      } + null
  }

  def "test canDoPrivilegedBlocks() with a non 3rd party bundle"() {
    expect:
      new StackFrameInformation('com.mycode', LOCATION, OBJECT).canDoPrivilegedBlocks(OSGI_DEBUG)
  }

  def "test canDoPrivilegedBlocks() with a non 3rd party domain"() {
    expect:
      new StackFrameInformation('file:/some/location/some-1234.jar', LOCATION, OBJECT).canDoPrivilegedBlocks(NON_OSGI_DEBUG)
  }

  @Unroll
  def "test canDoPrivilegedBlocks() when debuging OSGI containers with 3rd party class: #clazz"() {
    given:
      def location = Mock(Location) {
        toString() >> clazz + '(234)'
        declaringType() >> Mock(ReferenceType) {
          name() >> clazz
        }
      }

    expect:
      !new StackFrameInformation(BUNDLE, location, OBJECT).canDoPrivilegedBlocks(OSGI_DEBUG)

    where:
      clazz << getClass().getResource('/thirdparty-prefixes.txt').readLines().findAll {
        !(it.isEmpty() || it =~ /#.*/)
      }.collect {
        it + '.some.ClassName'
      }
  }

  @Unroll
  def "test canDoPrivilegedBlocks() when debuging non-OSGI VMs with 3rd party class: #clazz"() {
    given:
      def location = Mock(Location) {
        toString() >> clazz + '(234)'
        declaringType() >> Mock(ReferenceType) {
          name() >> clazz
        }
      }

    expect:
      !new StackFrameInformation(DOMAIN, location, OBJECT).canDoPrivilegedBlocks(NON_OSGI_DEBUG)

    where:
      clazz << getClass().getResource('/thirdparty-prefixes.txt').readLines().findAll {
        !(it.isEmpty() || it =~ /#.*/)
      }.collect {
        it + '.some.ClassName'
      }
  }

  @Unroll
  def "test canDoPrivilegedBlocks() when debugging #debugging_what with a non 3rd party class"() {
    given:
      def location = Mock(Location) {
        toString() >> 'com.mycode.MyClass(1234)'
        declaringType() >> Mock(ReferenceType) {
          name() >> 'com.mycode.MyClass'
        }
      }

    expect:
      new StackFrameInformation(domain, location, OBJECT).canDoPrivilegedBlocks(debug)

    where:
      debugging_what    || domain | debug
      'OSGI containers' || BUNDLE | OSGI_DEBUG
      'non-OSGI VMs'    || DOMAIN | NON_OSGI_DEBUG
  }

  @Unroll
  def "test canDoPrivilegedBlocks() when debugging #debugging_what with #with_what object reference"() {
    given:
      def reflectionUtil = Mock(ReflectionUtil)
      def debug = Mock(Debug) {
        isOSGi() >> osgi
        reflection() >> reflectionUtil
      }

    when:
      def result = new StackFrameInformation(domain, LOCATION, OBJECT).canDoPrivilegedBlocks(debug)

    then:
      result != proxy

    and:
      (1.._) * reflectionUtil.isInstance(_, OBJECT) >> proxy

    where:
      debugging_what    | with_what     || domain | osgi  | proxy
      'OSGI containers' | 'a proxy'     || BUNDLE | true  | true
      'non-OSGI VMs'    | 'a proxy'     || DOMAIN | false | true
      'OSGI containers' | 'a non proxy' || BUNDLE | true  | false
      'non-OSGI VMs'    | 'a non proxy' || DOMAIN | false | false
  }

  @Unroll
  def "test isDoPrivilegedBlock() with #with_what"() {
    given:
      def location = Mock(Location) {
        toString() >> location_str
        declaringType() >> CLASS
      }

    expect:
      new StackFrameInformation(domain, location, OBJECT).isDoPrivilegedBlock() == result

    where:
      with_what                              || domain | location_str                                       || result
      'no domain and no doPrivileged() call' || null   | LOCATION_STR                                       || false
      'domain and no doPrivileged() calls'   || DOMAIN | LOCATION_STR                                       || false
      'no domain and a doPrivileged() call'  || null   | 'java.security.AccessController.doPrivileged(452)' || true
  }

  @Unroll
  def "test isCallingDoPrivilegedBlockOnBehalfOfCaller() from #location_str in the boot domain"() {
    given:
      def location = Mock(Location) {
        toString() >> location_str
        declaringType() >> CLASS
      }

    expect:
      new StackFrameInformation(null, location, OBJECT).isCallingDoPrivilegedBlockOnBehalfOfCaller()

    where:
      location_str << getClass().getResource('/do-privileged-on-behalf-patterns.txt').readLines().findAll {
        !(it.isEmpty() || it =~ /#.*/)
      }.collect {
        it.replace('\\.', '.')
      }
  }

  @Unroll
  def "test isCallingDoPrivilegedBlockOnBehalfOfCaller() from #location_str not in the boot domain"() {
    given:
      def location = Mock(Location) {
        toString() >> location_str
        declaringType() >> CLASS
      }

    expect:
      !new StackFrameInformation(DOMAIN, location, OBJECT).isCallingDoPrivilegedBlockOnBehalfOfCaller()

    where:
      location_str << getClass().getResource('/do-privileged-on-behalf-patterns.txt').readLines().findAll {
        !(it.isEmpty() || it =~ /#.*/)
      }.collect {
        it.replace('\\.', '.')
      } + '"a.new.class:234'
  }

  @Unroll
  def "test isPrivileged() with #with_what"() {
    expect:
      new StackFrameInformation(domain, LOCATION, OBJECT).isPrivileged(domains as Set<String>) == result

    where:
      with_what                 || domain | domains             || result
      'boot domain'             || null   | []                  || true
      'a privileged domain'     || DOMAIN | [DOMAIN, 'another'] || true
      'a non privileged domain' || DOMAIN | ['another']         || false
      'null privileged domains' || DOMAIN | null                || true
  }

  @Unroll
  def "test hashCode() when #when_what"() {
    given:
      def location1 = Mock(Location) {
        toString() >> location_str1
        declaringType() >> CLASS
      }
      def location2 = Mock(Location) {
        toString() >> location_str2
        declaringType() >> CLASS
      }
      def info1 = new StackFrameInformation(domain1, location1, OBJECT)
      def info2 = new StackFrameInformation(domain2, location2, OBJECT)

    expect:
      (info1.hashCode() == info2.hashCode()) == result

    where:
      when_what                    || domain1 | domain2   | location_str1 | location_str2 || result
      'equals'                     || DOMAIN  | DOMAIN    | LOCATION_STR  | LOCATION_STR  || true
      'both domains are null'      || null    | null      | LOCATION_STR  | LOCATION_STR  || true
      'both locations are null'    || DOMAIN  | DOMAIN    | null          | null          || true
      'domains are different'      || DOMAIN  | 'domain2' | LOCATION_STR  | LOCATION_STR  || false
      'one domain is null'         || null    | DOMAIN    | LOCATION_STR  | LOCATION_STR  || false
      'the other domain is null'   || DOMAIN  | null      | LOCATION_STR  | LOCATION_STR  || false
      'locations are different'    || DOMAIN  | DOMAIN    | LOCATION_STR  | 'location2'   || false
      'one location is null'       || DOMAIN  | DOMAIN    | null          | LOCATION_STR  || false
      'the other location is null' || DOMAIN  | DOMAIN    | LOCATION_STR  | null          || false
      'everything is different'    || DOMAIN  | 'domain2' | LOCATION_STR  | 'location2'   || false
  }

  @Unroll
  def "test equals() when #when_what"() {
    given:
      def location1 = Mock(Location) {
        toString() >> location_str1
        declaringType() >> CLASS
      }
      def location2 = Mock(Location) {
        toString() >> location_str2
        declaringType() >> CLASS
      }
      def info1 = new StackFrameInformation(domain1, location1, OBJECT)
      def info2 = new StackFrameInformation(domain2, location2, OBJECT)

    expect:
      info1.equals(info2) == result

    where:
      when_what                    || domain1 | domain2   | location_str1 | location_str2 || result
      'equals'                     || DOMAIN  | DOMAIN    | LOCATION_STR  | LOCATION_STR  || true
      'both domains are null'      || null    | null      | LOCATION_STR  | LOCATION_STR  || true
      'both locations are null'    || DOMAIN  | DOMAIN    | null          | null          || true
      'domains are different'      || DOMAIN  | 'domain2' | LOCATION_STR  | LOCATION_STR  || false
      'one domain is null'         || null    | DOMAIN    | LOCATION_STR  | LOCATION_STR  || false
      'the other domain is null'   || DOMAIN  | null      | LOCATION_STR  | LOCATION_STR  || false
      'locations are different'    || DOMAIN  | DOMAIN    | LOCATION_STR  | 'location2'   || false
      'one location is null'       || DOMAIN  | DOMAIN    | null          | LOCATION_STR  || false
      'the other location is null' || DOMAIN  | DOMAIN    | LOCATION_STR  | null          || false
      'everything is different'    || DOMAIN  | 'domain2' | LOCATION_STR  | 'location2'   || false
  }

  @Unroll
  def "test equals() when the other is #is_what"() {
    expect:
      info1.equals(info2) == result

    where:
      is_what                       || info1 | info2 || result
      'identical'                   || INFO  | INFO  || true
      'null'                        || INFO  | null  || false
      'not a StackFrameInformation' || INFO  | 'abc' || false
  }

  @Unroll
  def "test toString() when debugging #debugging_what with #with_what"() {
    expect:
      new StackFrameInformation(domain, location, obj).toString(osgi, domains as Set<String>) == result

    where:
      debugging_what    | with_what                                         || osgi  | domain | location | obj    | domains   || result
      'OSGi containers' | 'bundle-0 and instance'                           || true  | null   | LOCATION | OBJECT | [BUNDLE]  || "bundle-0($LOCATION_STR) <instance of $CLASS_NAME>"
      'OSGi containers' | 'privileged bundle and instance'                  || true  | BUNDLE | LOCATION | OBJECT | [BUNDLE]  || "$BUNDLE($LOCATION_STR) <instance of $CLASS_NAME>"
      'OSGi containers' | 'bundle and instance and null privileged bundles' || true  | BUNDLE | LOCATION | OBJECT | null      || "$BUNDLE($LOCATION_STR) <instance of $CLASS_NAME>"
      'OSGi containers' | 'non privileged bundle and instance'              || true  | BUNDLE | LOCATION | OBJECT | ['other'] || "*$BUNDLE($LOCATION_STR) <instance of $CLASS_NAME>"
      'OSGi containers' | 'bundle-0 and class'                              || true  | null   | LOCATION | null   | [BUNDLE]  || "bundle-0($LOCATION_STR) <class of $CLASS_NAME>"
      'OSGi containers' | 'privileged bundle and class'                     || true  | BUNDLE | LOCATION | null   | [BUNDLE]  || "$BUNDLE($LOCATION_STR) <class of $CLASS_NAME>"
      'OSGi containers' | 'bundle and class and null privileged bundles'    || true  | BUNDLE | LOCATION | null   | null      || "$BUNDLE($LOCATION_STR) <class of $CLASS_NAME>"
      'OSGi containers' | 'non privileged bundle and class'                 || true  | BUNDLE | LOCATION | null   | ['other'] || "*$BUNDLE($LOCATION_STR) <class of $CLASS_NAME>"
      'non-OSGi VMs'    | 'boot domain and instance'                        || false | null   | LOCATION | OBJECT | [DOMAIN]  || "boot:($LOCATION_STR) <instance of $CLASS_NAME>"
      'non-OSGi VMs'    | 'privileged domain and instance'                  || false | DOMAIN | LOCATION | OBJECT | [DOMAIN]  || "$DOMAIN($LOCATION_STR) <instance of $CLASS_NAME>"
      'non-OSGi VMs'    | 'domain and instance and null privileged domains' || false | DOMAIN | LOCATION | OBJECT | null      || "$DOMAIN($LOCATION_STR) <instance of $CLASS_NAME>"
      'non-OSGi VMs'    | 'non privileged domain and instance'              || false | DOMAIN | LOCATION | OBJECT | ['other'] || "*$DOMAIN($LOCATION_STR) <instance of $CLASS_NAME>"
      'non-OSGi VMs'    | 'boot domain and class'                           || false | null   | LOCATION | null   | [DOMAIN]  || "boot:($LOCATION_STR) <class of $CLASS_NAME>"
      'non-OSGi VMs'    | 'privileged domain and class'                     || false | DOMAIN | LOCATION | null   | [DOMAIN]  || "$DOMAIN($LOCATION_STR) <class of $CLASS_NAME>"
      'non-OSGi VMs'    | 'domain and class and null privileged domains'    || false | DOMAIN | LOCATION | null   | null      || "$DOMAIN($LOCATION_STR) <class of $CLASS_NAME>"
      'non-OSGi VMs'    | 'non privileged domain and class'                 || false | DOMAIN | LOCATION | null   | ['other'] || "*$DOMAIN($LOCATION_STR) <class of $CLASS_NAME>"
  }

  @Unroll
  def "test toString() with #with_what"() {
    expect:
      new StackFrameInformation(domain, location, obj).toString() == result

    where:
      debugging_what    | with_what                                         || domain | location | obj    | result
      'OSGi containers' | 'bundle and instance and null privileged bundles' || BUNDLE | LOCATION | OBJECT | "$BUNDLE($LOCATION_STR) <instance of $CLASS_NAME>"
      'OSGi containers' | 'bundle and class and null privileged bundles'    || BUNDLE | LOCATION | null   | "$BUNDLE($LOCATION_STR) <class of $CLASS_NAME>"
  }

  @Unroll
  def "test doPrivilegedAt() with #with_what"() {
    expect:
      StackFrameInformation.doPrivilegedAt(stack, index) == result

    where:
      with_what                                             || stack                                    | index || result
      'with 1 frame'                                        || [FRAME0]                                 | 0     || [StackFrameInformation.DO_PRIVILEGED, FRAME0]
      'with 2 frames and inserting at the top of the stack' || [FRAME0, FRAME1]                         | 0     || [StackFrameInformation.DO_PRIVILEGED, FRAME0, FRAME1]
      'with 2 frames and inserting at the end of the stack' || [FRAME0, FRAME1]                         | 1     || [FRAME0, StackFrameInformation.DO_PRIVILEGED, FRAME1]
      'with multiple frames and inserting in the middle'    || [FRAME0, FRAME1, FRAME2, FRAME2, FRAME3] | 2     || [FRAME0, FRAME1, StackFrameInformation.DO_PRIVILEGED, FRAME2, FRAME2, FRAME3]
  }
}