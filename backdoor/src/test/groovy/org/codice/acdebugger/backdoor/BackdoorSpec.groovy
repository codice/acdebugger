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
package org.codice.acdebugger.backdoor

import org.codice.acdebugger.PermissionService
import org.codice.acdebugger.common.PropertiesUtil
import org.codice.junit.DeFinalize
import org.codice.junit.DeFinalizer
import org.codice.spock.Supplemental
import org.eclipse.osgi.internal.permadmin.BundlePermissions
import org.junit.runner.RunWith
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.BundlePermission
import org.osgi.framework.BundleReference
import org.osgi.framework.ServiceEvent
import org.osgi.framework.ServicePermission
import org.osgi.framework.ServiceReference
import org.osgi.framework.Version
import org.osgi.framework.wiring.BundleWiring
import org.osgi.util.tracker.ServiceTracker
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import java.security.CodeSource
import java.security.Permission
import java.security.PermissionCollection
import java.security.ProtectionDomain
import java.security.cert.Certificate

@RestoreSystemProperties
@Supplemental
@RunWith(DeFinalizer)
@DeFinalize(ServicePermission)
class BackdoorSpec extends Specification {
  static def DDF_HOME_PERM = '/root/'
  static def COMPRESSED_LOCATION = 'file:${ddf.home.perm}location${/}here'
  static def LOCATION = 'file:/root/location/here'
  static def VERSION = '2.3.4'
  static def EXCEPTION = new Exception('testing')
  static def RUNTIME_EXCEPTION = new NullPointerException('testing')
  static def VIRTUAL_MACHINE_ERROR = new OutOfMemoryError('testing')
  static def SERVICE_ID = 223344
  static def SERVICE_NAME_1 = 'name1'
  static def SERVICE_NAME_2 = 'name2'
  static def SERVICE_NAMES = [SERVICE_NAME_1, SERVICE_NAME_2] as String[]
  static def LOCATION_URL = new URL(LOCATION)
  static def CODESOURCE = new CodeSource(LOCATION_URL, (Certificate[]) null)
  static def CODESOURCE_NULL_URL = new CodeSource(null, (Certificate[]) null)
  static def PATH = new File('path').getCanonicalPath()
  static def PATH_WILDCARD = new File('path').getCanonicalPath() + File.separatorChar + '*'
  static def PATH_RECURSIVE = new File('path').getCanonicalPath() + File.separatorChar + '-'
  static def PATH_CHILD_NOTHING_COMPRESSED = new File('path/child').getCanonicalPath()
  static def PATH_CHILD_CHILD2_SLASH_COMPRESSED = new File('path/child/child2').getCanonicalPath().replace('/', '${/}')
  static def PATH_CHILD_BACKSLASH_COMPRESSED = new File('path\\child').getCanonicalPath().replace('\\', '${/}')
  static def PATH_CHILD_CHILDREN_CHILD_COMPRESSED = new File('path/child/children').getCanonicalPath().replace('child', '${/}')

  @Shared
  def BUNDLE = Mock(Bundle) {
    getSymbolicName() >> LOCATION
    getVersion() >> Mock(Version) {
      toString() >> VERSION
    }
  }
  @Shared
  def BUNDLE_REF = Mock(BundleReference) {
    getBundle() >> BUNDLE
  }
  @Shared
  def BUNDLE_WIRING = Mock(BundleWiring) {
    getBundle() >> BUNDLE
  }
  @Shared
  def BUNDLE_CONTEXT = Mock(BundleContext) {
    getBundle() >> BUNDLE
  }
  @Shared
  def BUNDLE_PERMISSION = new BundlePermission(LOCATION, 'provide')
  @Shared
  def BUNDLE_PERMISSIONS = new BundlePermissions(BUNDLE, null, null, null)
  @Shared
  def BUNDLE_DOMAIN = new ProtectionDomain(CODESOURCE, BUNDLE_PERMISSIONS)
  @Shared
  def SERVICE = Mock(ServiceReference) {
    getProperty('service.id') >> SERVICE_ID
  }
  @Shared
  def SERVICE_WITH_OBJCLASS = Mock(ServiceReference) {
    getProperty('service.id') >> SERVICE_ID
    getProperty('objectClass') >> SERVICE_NAMES
  }
  @Shared
  def SERVICE_EVENT = new ServiceEvent(ServiceEvent.REGISTERED, SERVICE_WITH_OBJCLASS)

  def tracker = Mock(ServiceTracker)
  def backdoor = Spy(Backdoor) {
    newServiceTracker(_) >> tracker
  }
  def context = Mock(BundleContext)

  def cleanup() {
    Backdoor.instance = null
  }

  def "test singleton not set after creation"() {
    expect:
      Backdoor.instance == null
  }

  def "test start() will track permission services"() {
    given:
      def backdoor = new Backdoor()

    when:
      backdoor.start(context)

    then:
      Backdoor.instance.is(backdoor)

    and: "a filter will be registered for the permission service"
      1 * context.createFilter('(objectClass=org.codice.acdebugger.PermissionService)')
  }

  def "test start() will create and open a tracker and register the singleton"() {
    when:
      backdoor.start(context)

    then:
      Backdoor.instance.is(backdoor)

    and:
      1 * tracker.open()
  }

  def "test stop() will close the tracker and clear the singleton"() {
    given:
      backdoor.start(context)

    when:
      backdoor.stop(context)

    then:
      Backdoor.instance == null

    and:
      1 * tracker.close()
  }

  @Unroll
  def "test getBundle() with #with_what"() {
    given:
      backdoor.start(context)

    expect:
      backdoor.getBundle(obj) == bundle

    where:
      with_what                                                        || obj                                                                   || bundle
      'null'                                                           || null                                                                  || null
      'something unrelated to bundles'                                 || 'abc'                                                                 || null
      'a bundle'                                                       || BUNDLE                                                                || LOCATION
      'a bundle reference'                                             || BUNDLE_REF                                                            || LOCATION
      'a bundle wiring'                                                || BUNDLE_WIRING                                                         || LOCATION
      'a bundle context'                                               || BUNDLE_CONTEXT                                                        || LOCATION
      'bundle permissions'                                             || BUNDLE_PERMISSIONS                                                    || LOCATION
      'a protection domain that contains bundle permissions'           || BUNDLE_DOMAIN                                                         || LOCATION
      'a protection domain that does not contain bundle permissions'   || Mock(ProtectionDomain)                                                || null
      'a class that has a getBundle() method'                          || new GetBundle(BUNDLE)                                                 || LOCATION
      'a class that has a getBundleContext() method'                   || new GetBundleContext(BUNDLE_CONTEXT)                                  || LOCATION
      'a class that has a bundle field'                                || new BundleField(BUNDLE)                                               || LOCATION
      'a class that has a bundleContext field'                         || new BundleContextField(BUNDLE_CONTEXT)                                || LOCATION
      'a class that has a context field'                               || new ContextField(BUNDLE_CONTEXT)                                      || LOCATION
      'a class that has a delegate field'                              || new DelegateProtectionDomain(BUNDLE_DOMAIN)                           || LOCATION
      'a class that has a getBundleContextForServiceLookup() method'   || new GetBundleContextForServiceLookup(BUNDLE_CONTEXT)                  || LOCATION
      'an anonymous class where parent has a bundle field'             || new GetBundle(BUNDLE).child                                           || LOCATION
      'an anonymous class where grandparent has a bundle field'        || new GetBundle(BUNDLE).grandChild.child                                || LOCATION
      'a protection domain with a classloader that has a bundle field' || new ProtectionDomain(null, null, new BundleClassLoader(BUNDLE), null) || LOCATION
      'a classloader that has a parent that has a bundle field'        || new ClassLoaderWithParentBundleClassLoader(BUNDLE)                    || LOCATION
      'a class that has a getBundle() method that fails'               || new GetBundleFailing()                                                || null
      'a class object loaded from the boot classloader'                || String                                                                || null
  }

  @Unroll
  def "test getBundle() failing with #exception.class.simpleName"() {
    given:
      def bundle = Mock(Bundle)

      backdoor.start(context)

    when:
      backdoor.getBundle(bundle)

    then:
      def e = thrown(Throwable)

      e.is(exception)

    and:
      1 * bundle.getSymbolicName() >> { throw exception }

    where:
      exception << [RUNTIME_EXCEPTION, VIRTUAL_MACHINE_ERROR]
  }

  @Unroll
  def "test getBundleVersion() with #with_what"() {
    given:
      backdoor.start(context)

    expect:
      backdoor.getBundleVersion(obj) == version

    where:
      with_what                                                        || obj                                                                   || version
      'null'                                                           || null                                                                  || null
      'something unrelated to bundles'                                 || 'abc'                                                                 || null
      'a bundle'                                                       || BUNDLE                                                                || VERSION
      'a bundle reference'                                             || BUNDLE_REF                                                            || VERSION
      'a bundle wiring'                                                || BUNDLE_WIRING                                                         || VERSION
      'a bundle context'                                               || BUNDLE_CONTEXT                                                        || VERSION
      'bundle permissions'                                             || BUNDLE_PERMISSIONS                                                    || VERSION
      'a protection domain that contains bundle permissions'           || BUNDLE_DOMAIN                                                         || VERSION
      'a protection domain that does not contain bundle permissions'   || Mock(ProtectionDomain)                                                || null
      'a class that has a getBundle() method'                          || new GetBundle(BUNDLE)                                                 || VERSION
      'a class that has a getBundleContext() method'                   || new GetBundleContext(BUNDLE_CONTEXT)                                  || VERSION
      'a class that has a bundle field'                                || new BundleField(BUNDLE)                                               || VERSION
      'a class that has a bundleContext field'                         || new BundleContextField(BUNDLE_CONTEXT)                                || VERSION
      'a class that has a context field'                               || new ContextField(BUNDLE_CONTEXT)                                      || VERSION
      'a class that has a delegate field'                              || new DelegateProtectionDomain(BUNDLE_DOMAIN)                           || VERSION
      'a class that has a getBundleContextForServiceLookup() method'   || new GetBundleContextForServiceLookup(BUNDLE_CONTEXT)                  || VERSION
      'an anonymous class where parent has a bundle field'             || new GetBundle(BUNDLE).child                                           || VERSION
      'an anonymous class where grandparent has a bundle field'        || new GetBundle(BUNDLE).grandChild.child                                || VERSION
      'a protection domain with a classloader that has a bundle field' || new ProtectionDomain(null, null, new BundleClassLoader(BUNDLE), null) || VERSION
      'a classloader that has a parent that has a bundle field'        || new ClassLoaderWithParentBundleClassLoader(BUNDLE)                    || VERSION
      'a class that has a getBundle() method that fails'               || new GetBundleFailing()                                                || null
  }

  @Unroll
  def "test getBundleVersion() failing with #exception.class.simpleName"() {
    given:
      def bundle = Mock(Bundle)

      backdoor.start(context)

    when:
      backdoor.getBundleVersion(bundle)

    then:
      def e = thrown(Throwable)

      e.is(exception)

    and:
      1 * bundle.getVersion() >> { throw exception }

    where:
      exception << [RUNTIME_EXCEPTION, VIRTUAL_MACHINE_ERROR]
  }

  @Unroll
  def "test getDomain() with a domain that has #has_what"() {
    given:
      def obj = new ProtectionDomain(codesource, BUNDLE_PERMISSIONS)

      backdoor.start(context)
      System.setPropertyIfNotNull('/', slash)
      System.setPropertyIfNotNull('ddf.home.perm', ddf_home_perm)

    expect:
      backdoor.getDomain(obj) == domain

    where:
      has_what                                      || codesource          | ddf_home_perm | slash || domain
      'a codesource and url'                        || CODESOURCE          | null          | null  || LOCATION
      'a codesource and no url'                     || CODESOURCE_NULL_URL | null          | null  || null
      'no codesource'                               || null                | null          | null  || null
      'a codesource and url and properties defined' || CODESOURCE          | DDF_HOME_PERM | '/'   || COMPRESSED_LOCATION
  }

  @Unroll
  def "test getDomain() with #with_what"() {
    given:
      backdoor.start(context)

    expect:
      backdoor.getDomain(domain) == result

    where:
      with_what                                    || domain || result
      'null'                                       || null   || null
      'an object loaded from the boot classloader' || 'abc'  || null
  }

  @Unroll
  def "test getDomain() failing with #exception.class.simpleName"() {
    given:
      def properties = Mock(PropertiesUtil)
      def backdoor = new Backdoor(properties)

      backdoor.start(context)

    when:
      backdoor.getDomain(BUNDLE_DOMAIN)

    then:
      def e = thrown(Throwable)

      e.is(exception)

    and:
      1 * properties.compress(_) >> { throw exception }

    where:
      exception << [RUNTIME_EXCEPTION, VIRTUAL_MACHINE_ERROR]
  }

  @Unroll
  def "test getDomainInfo() with a domain that has #has_what"() {
    given:
      def permissions = Mock(PermissionCollection)
      def domain = new ProtectionDomain(codesource, permissions)

      backdoor.start(context)

    when:
      def result = backdoor.getDomainInfo(domain, BUNDLE_PERMISSION)

    then:
      permissions.implies(BUNDLE_PERMISSION) >> implies

    and:
      result == json

    where:
      has_what                                          || codesource          | implies || json
      'a codesource and url and granted permission'     || CODESOURCE          | true    || "{\"locationString\":\"$LOCATION\",\"implies\":true}"
      'a codesource and url and not granted permission' || CODESOURCE          | false   || "{\"locationString\":\"$LOCATION\",\"implies\":false}"
      'a codesource and no url and granted permission'  || CODESOURCE_NULL_URL | true    || '{"locationString":null,"implies":true}'
      'no codesource and not granted permission'        || null                | false   || '{"locationString":null,"implies":false}'
  }

  def "test getDomainInfo() with an array of domains"() {
    given:
      def permissions = Mock(PermissionCollection)
      def permissions2 = Mock(PermissionCollection)
      def domain = new ProtectionDomain(CODESOURCE_NULL_URL, permissions)
      def domain2 = new ProtectionDomain(CODESOURCE, permissions2)

      backdoor.start(context)

    when:
      def result = backdoor.getDomainInfo([domain, domain2] as ProtectionDomain[], BUNDLE_PERMISSION)

    then:
      1 * permissions.implies(BUNDLE_PERMISSION) >> false
      1 * permissions2.implies(BUNDLE_PERMISSION) >> true

    and:
      result == "[{\"locationString\":null,\"implies\":false},{\"locationString\":\"$LOCATION\",\"implies\":true}]"
  }

  @Unroll
  def "test getDomainInfo() failing with #exception.class.simpleName"() {
    given:
      def properties = Mock(PropertiesUtil)
      def backdoor = new Backdoor(properties)

      backdoor.start(context)

    when:
      backdoor.getDomainInfo(BUNDLE_DOMAIN, BUNDLE_PERMISSION)

    then:
      def e = thrown(Throwable)

      e.is(exception)

    and:
      1 * properties.compress(_) >> { throw exception }

    where:
      exception << [RUNTIME_EXCEPTION, VIRTUAL_MACHINE_ERROR]
  }

  @Unroll
  def "test getDomainInfo() failing when called with #with_what"() {
    given:
      backdoor.start(context)

    when:
      backdoor.getDomainInfo(domain, permission)

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains(message)

    where:
      with_what                                             || domain        | permission        || message
      'a null permission'                                   || BUNDLE_DOMAIN | null              || 'not a permission'
      'something else than a permission'                    || BUNDLE_DOMAIN | 'abc'             || 'not a permission'
      'a null domain or array of domains'                   || null          | BUNDLE_PERMISSION || 'not a domain or array of domains'
      'something else than a domain or an array of domains' || 'cde'         | BUNDLE_PERMISSION || 'not a domain or array of domains'
  }

  @Unroll
  def "test getPermissionStrings() failing when called with #with_what"() {
    given:
      backdoor.start(context)

    when:
      backdoor.getPermissionStrings(permission)

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains('not a permission')

    where:
      with_what                          || permission
      'null'                             || null
      'something else than a permission' || 'abc'
  }

  @Unroll
  def "test getPermissionStrings() when called with #with_what"() {
    given:
      backdoor.start(context)

    expect:
      backdoor.getPermissionStrings(permission) == result

    where:
      with_what                                       || permission                                                          || result
      'a file permission'                             || new FilePermission('path', 'read,write')                            || "[\"java.io.FilePermission \\\"$PATH\\\", \\\"read,write\\\"\"]"
      'a file permission with a wildcard path'        || new FilePermission('path' + File.separatorChar + '*', 'read,write') || "[\"java.io.FilePermission \\\"$PATH_WILDCARD\\\", \\\"read,write\\\"\"]"
      'a file permission with a recursive path'       || new FilePermission('path' + File.separatorChar + '-', 'read,write') || "[\"java.io.FilePermission \\\"$PATH_RECURSIVE\\\", \\\"read,write\\\"\"]"
      'a property permission'                         || new PropertyPermission('property', 'read')                          || '["java.util.PropertyPermission \\"property\\", \\"read\\""]'
      'a service permission with service name'        || new ServicePermission('name', 'register')                           || '["org.osgi.framework.ServicePermission \\"name\\", \\"register\\""]'
      'a service permission with service id'          || new ServicePermission(SERVICE, 'get')                               || "[\"org.osgi.framework.ServicePermission \\\"(service.id=$SERVICE_ID)\\\", \\\"get\\\"\"]"
      'a service permission with service objectClass' || new ServicePermission(SERVICE_WITH_OBJCLASS, 'get')                 || '["org.osgi.framework.ServicePermission \\"name1\\", \\"get\\"","org.osgi.framework.ServicePermission \\"name2\\", \\"get\\""]'
  }

  @Unroll
  def 'test getPermissionStrings() for file permissions when ${/} is #slash_is'() {
    given:
      backdoor.start(context)
      System.setPropertyIfNotNull('/', slash)

    expect:
      backdoor.getPermissionStrings(permission) == result

    where:
      slash_is           || slash   | permission                                        || result
      'not defined'      || null    | new FilePermission('path/child', 'read')          || "[\"java.io.FilePermission \\\"$PATH_CHILD_NOTHING_COMPRESSED\\\", \\\"read\\\"\"]"
      'defined as /'     || '/'     | new FilePermission('path/child/child2', 'read')   || "[\"java.io.FilePermission \\\"$PATH_CHILD_CHILD2_SLASH_COMPRESSED\\\", \\\"read\\\"\"]"
      'defined as \\'    || '\\'    | new FilePermission('path\\child', 'read')         || "[\"java.io.FilePermission \\\"$PATH_CHILD_BACKSLASH_COMPRESSED\\\", \\\"read\\\"\"]"
      'defined as child' || 'child' | new FilePermission('path/child/children', 'read') || "[\"java.io.FilePermission \\\"$PATH_CHILD_CHILDREN_CHILD_COMPRESSED\\\", \\\"read\\\"\"]"
  }

  @Unroll
  def "test getPermissionStrings() failing with #exception.class.simpleName"() {
    given:
      def permission = Mock(Permission)

      backdoor.start(context)

    when:
      backdoor.getPermissionStrings(permission)

    then:
      def e = thrown(Throwable)

      e.is(exception)

    and:
      1 * permission.actions >> { throw exception }

    where:
      exception << [RUNTIME_EXCEPTION, VIRTUAL_MACHINE_ERROR]
  }

  def "test getPermissionStrings() with service permission failing with exception falls back to standard string representation"() {
    given:
      def permission = Spy(ServicePermission, constructorArgs: [SERVICE_WITH_OBJCLASS, 'get'])

      backdoor.start(context)

    when:
      def result = backdoor.getPermissionStrings(permission)

    then:
      result ==~ /\["org\.osgi\.framework\.ServicePermission.* \\"\(service\.id=223344\)\\", \\"get\\"\"]/

    and:
      3 * permission.actions >> 'get' >> { throw RUNTIME_EXCEPTION } >> 'get'
  }

  def "test getPermissionStrings() with service permission failing with VirtualMachineError"() {
    given:
      def permission = Spy(ServicePermission, constructorArgs: [SERVICE_WITH_OBJCLASS, 'get'])

      backdoor.start(context)

    when:
      backdoor.getPermissionStrings(permission)

    then:
      def e = thrown(Throwable)

      e.is(VIRTUAL_MACHINE_ERROR)

    and:
      2 * permission.actions >> 'get' >> { throw VIRTUAL_MACHINE_ERROR }
  }

  @Unroll
  def "test getServicePermissionInfoAndGrant() failing when called with #with_what"() {
    given:
      backdoor.start(context)

    when:
      backdoor.getServicePermissionInfoAndGrant('bundle', domain, event, false)

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains(message)

    where:
      with_what                             || domain        | event         || message
      'a null domain'                       || null          | SERVICE_EVENT || 'not a domain'
      'something else than a domain'        || 'abc'         | SERVICE_EVENT || 'not a domain'
      'a null service event'                || BUNDLE_DOMAIN | null          || 'not a service event'
      'something else than a service event' || BUNDLE_DOMAIN | 'abc'         || 'not a service event'
  }

  @Unroll
  def "test getServicePermissionInfoAndGrant() when #when_what and granting is #grant"() {
    given:
      def domain = Mock(ProtectionDomain)
      def permissionService = Mock(PermissionService)
      def servicesArray = names as String[]
      def event = new ServiceEvent(ServiceEvent.REGISTERED, Mock(ServiceReference) {
        getProperty('service.id') >> SERVICE_ID
        getProperty('objectClass') >> servicesArray
      })

      backdoor.start(context)

    when:
      def result = backdoor.getServicePermissionInfoAndGrant('bundle', domain, event, grant)

    then:
      result == json

    and:
      1 * domain.implies({
        it.name == "(service.id=$SERVICE_ID)" && it.actions == 'get'
      }) >> serviceImplied
      1 * domain.implies({ it.name == '*' && it.actions == 'get' }) >> allImplied
      names.each { s ->
        1 * domain.implies({ it.name == s && it.actions == 'get' }) >> (s in namesImplied)
      }

    and:
      if (namesGranted.isEmpty()) {
        0 * tracker.getService()
      } else {
        namesGranted.each {
          1 * tracker.getService() >> permissionService
          1 * permissionService.grantPermission('bundle', "org.osgi.framework.ServicePermission \"$it\", \"get\"")
        }
      }

    where:
      when_what                                                       || grant | names      | serviceImplied | allImplied | namesImplied || namesGranted | json
      'service and * have permissions but not its names'              || true  | ['a', 'b'] | true           | true       | []           || ['a', 'b']   | '{"permissionStrings":["org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""],"implies":true,"implied":["org.osgi.framework.ServicePermission \\"*\\", \\"get\\""]}'
      'service and * have permissions but not its names'              || false | ['a', 'b'] | true           | true       | []           || []           | '{"permissionStrings":["org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""],"implies":true,"implied":["org.osgi.framework.ServicePermission \\"*\\", \\"get\\""]}'
      'service has permissions but not * and its names'               || false | ['a', 'b'] | true           | false      | []           || []           | '{"permissionStrings":["org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""],"implies":true,"implied":[]}'
      'service, *, and its names have permissions'                    || true  | ['a', 'b'] | true           | true       | ['a', 'b']   || []           | '{"permissionStrings":["org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""],"implies":true,"implied":["org.osgi.framework.ServicePermission \\"*\\", \\"get\\"","org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""]}'
      'service, *, and its names have permissions'                    || false | ['a', 'b'] | true           | true       | ['a', 'b']   || []           | '{"permissionStrings":["org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""],"implies":true,"implied":["org.osgi.framework.ServicePermission \\"*\\", \\"get\\"","org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""]}'
      'service and its names have permissions but not all'            || false | ['a', 'b'] | true           | false      | ['a', 'b']   || []           | '{"permissionStrings":["org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""],"implies":true,"implied":["org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""]}'
      'service, *, and some of its names have permissions'            || true  | ['a', 'b'] | true           | true       | ['a']        || ['b']        | '{"permissionStrings":["org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""],"implies":true,"implied":["org.osgi.framework.ServicePermission \\"*\\", \\"get\\"","org.osgi.framework.ServicePermission \\"a\\", \\"get\\""]}'
      'service, *, and some of its names have permissions'            || false | ['a', 'b'] | true           | true       | ['a']        || []           | '{"permissionStrings":["org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""],"implies":true,"implied":["org.osgi.framework.ServicePermission \\"*\\", \\"get\\"","org.osgi.framework.ServicePermission \\"a\\", \\"get\\""]}'
      'service and some of its names have permissions and all do not' || false | ['a', 'b'] | true           | false      | ['a']        || []           | '{"permissionStrings":["org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""],"implies":true,"implied":["org.osgi.framework.ServicePermission \\"a\\", \\"get\\""]}'
      'nobody has permissions'                                        || true  | ['a', 'b'] | false          | false      | []           || ['a', 'b']   | '{"permissionStrings":["org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""],"implies":false,"implied":[]}'
      'nobody has permissions'                                        || false | ['a', 'b'] | false          | false      | []           || []           | '{"permissionStrings":["org.osgi.framework.ServicePermission \\"a\\", \\"get\\"","org.osgi.framework.ServicePermission \\"b\\", \\"get\\""],"implies":false,"implied":[]}'
  }

  @Unroll
  def "test getServicePermissionInfoAndGrant() failing with #exception.class.simpleName"() {
    given:
      def domain = Stub(ProtectionDomain)
      def permissionService = Mock(PermissionService)

      backdoor.start(context)

    when:
      backdoor.getServicePermissionInfoAndGrant('bundle', domain, SERVICE_EVENT, true)

    then:
      def e = thrown(Throwable)

      e.is(exception)

    and:
      1 * tracker.getService() >> permissionService
      1 * permissionService.grantPermission('bundle', "org.osgi.framework.ServicePermission \"$SERVICE_NAME_1\", \"get\"") >> {
        throw exception
      }

    where:
      exception << [EXCEPTION, RUNTIME_EXCEPTION, VIRTUAL_MACHINE_ERROR]
  }

  def "test grantPermission()"() {
    given:
      def permissionService = Mock(PermissionService)

      backdoor.start(context)

    when:
      backdoor.grantPermission('bundle', 'permission')

    then:
      1 * tracker.getService() >> permissionService
      1 * permissionService.grantPermission('bundle', 'permission')
  }

  @Unroll
  def "test grantPermission() failing with #exception.class.simpleName"() {
    given:
      def permissionService = Mock(PermissionService)

      backdoor.start(context)

    when:
      backdoor.grantPermission('bundle', 'permission')

    then:
      def e = thrown(Throwable)

      e.is(exception)

    and:
      1 * tracker.getService() >> permissionService
      1 * permissionService.grantPermission('bundle', 'permission') >> {
        throw exception
      }

    where:
      exception << [EXCEPTION, RUNTIME_EXCEPTION, VIRTUAL_MACHINE_ERROR]
  }

  @Unroll
  def "test hasPermission() failing with #exception.class.simpleName"() {
    given:
      def domain = Mock(ProtectionDomain)
      def permission = Stub(Permission)

      backdoor.start(context)

    when:
      backdoor.hasPermission(domain, permission)

    then:
      def e = thrown(Throwable)

      e.is(exception)

    and:
      1 * domain.implies(permission) >> { throw exception }

    where:
      exception << [RUNTIME_EXCEPTION, VIRTUAL_MACHINE_ERROR]
  }

  @Unroll
  def "test hasPermission() failing when called with #with_what"() {
    given:
      backdoor.start(context)

    when:
      backdoor.hasPermission(domain, permission)

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains(message)

    where:
      with_what                          || domain        | permission        || message
      'a null permission'                || BUNDLE_DOMAIN | null              || 'not a permission'
      'something else than a permission' || BUNDLE_DOMAIN | 'abc'             || 'not a permission'
      'a null domain'                    || null          | BUNDLE_PERMISSION || 'not a domain'
      'something else than a domain'     || 'cde'         | BUNDLE_PERMISSION || 'not a domain'
  }

  @Unroll
  def "test hasPermission() when the domain #domain_what"() {
    given:
      def domain = Mock(ProtectionDomain)

      backdoor.start(context)

    when:
      def implied = backdoor.hasPermission(domain, BUNDLE_PERMISSION)

    then:
      implied == implies

    and:
      1 * domain.implies(BUNDLE_PERMISSION) >> implies

    where:
      domain_what                    || implies
      'has the permission'           || true
      'doesn\'t have the permission' || false
  }
}

class GetBundle {
  // don't call it bundle on purpose
  private final Bundle b

  GetBundle(Bundle bundle) {
    this.b = bundle
  }

  private Bundle getBundle() {
    return b;
  }

  Object getChild() {
    return new Object() {}
  }

  Object getGrandChild() {
    return new Object() {
      Object getChild() {
        return new Object() {}
      }
    }
  }
}

class BundleField {
  private final Bundle bundle

  BundleField(Bundle bundle) {
    this.bundle = bundle
  }
}

class GetBundleContext {
  // don't call it bundleContext on purpose
  private final BundleContext bc

  GetBundleContext(BundleContext bundleContext) {
    this.bc = bundleContext
  }

  private BundleContext getBundleContext() {
    return bc
  }
}

class BundleContextField {
  private final BundleContext bundleContext

  BundleContextField(BundleContext bundleContext) {
    this.bundleContext = bundleContext
  }
}

class ContextField {
  private final BundleContext context

  ContextField(BundleContext bundleContext) {
    this.context = bundleContext
  }
}

class DelegateProtectionDomain {
  private final ProtectionDomain delegate

  DelegateProtectionDomain(ProtectionDomain domain) {
    this.delegate = domain
  }
}

class GetBundleContextForServiceLookup {
  // don't call it bundleContext on purpose
  private final BundleContext bc

  GetBundleContextForServiceLookup(BundleContext bundleContext) {
    this.bc = bundleContext
  }

  private BundleContext getBundleContextForServiceLookup() {
    return bc
  }
}

class BundleClassLoader extends ClassLoader {
  private final Bundle bundle

  BundleClassLoader(Bundle bundle) {
    this.bundle = bundle
  }
}

class ClassLoaderWithParentBundleClassLoader extends ClassLoader {
  ClassLoaderWithParentBundleClassLoader(Bundle bundle) {
    super(new BundleClassLoader(bundle))
  }
}

class GetBundleFailing {
  private Bundle getBundle() {
    throw new IllegalStateException()
  }
}
