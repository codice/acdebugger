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
import com.sun.jdi.StackFrame
import org.codice.acdebugger.ReflectionSpecification
import org.codice.acdebugger.impl.Backdoor
import spock.lang.Shared
import spock.lang.Unroll

class BundleUtilSpec extends ReflectionSpecification {
  static def BUNDLE_NAME = 'bundle.name'

  @Shared
  def BUNDLE_CLASS = MockClassType('BUNDLE_CLASS', 'Lorg/osgi/framework/Bundle;')
  @Shared
  def MODULE_WIRING_CLASS = MockClassType('MODULE_WIRING_CLASS', 'Lorg/eclipse/osgi/container/ModuleWiring;')
  @Shared
  def BUNDLE_LOADER_CLASS = MockClassType('BUNDLE_LOADER_CLASS', 'Lorg/eclipse/osgi/internal/loader/BundleLoader;')
  @Shared
  def CLASSLOADER_CLASS = MockClassType('CLASSLOADER_CLASS', 'Ljava/lang/ClassLoader;')
  @Shared
  def EQUINOX_CLASSLOADER_CLASS = MockClassType('EQUINOX_CLASSLOADER_CLASS', 'Lorg/eclipse/osgi/internal/loader/EquinoxClassLoader;')
  @Shared
  def BUNDLE_CLASSLOADER_CLASS = MockClassType('BUNDLE_CLASSLOADER_CLASS', 'Lorg/apache/felix/framework/BundleWiringImpl/BundleClassLoader;', superclass: CLASSLOADER_CLASS)
  @Shared
  def PROXY_CLASSLOADER_CLASS = MockClassType('PROXY_CLASSLOADER_CLASS', 'Lorg/apache/aries/proxy/impl/interfaces/ProxyClassLoader;', superclass: CLASSLOADER_CLASS)
  @Shared
  def BUNDLE_PERMISSIONS_CLASS = MockClassType('BUNDLE_PERMISSIONS_CLASS', 'Lorg/eclipse/osgi/internal/permadmin/BundlePermissions;')
  @Shared
  def PROTECTION_DOMAIN_CLASS = MockClassType('PROTECTION_DOMAIN_CLASS', 'Ljava/security/ProtectionDomain;')
  @Shared
  def BUNDLE_CONTEXT_CLASS = MockClassType('BUNDLE_CONTEXT_CLASS', 'Lorg/osgi/framework/BundleContext;')
  @Shared
  def GET_BUNDLE_CONTEXT_CLASS = MockClassType('GET_BUNDLE_CONTEXT_CLASS', 'Lsome/class/with/GetBundleContextMethod;')
  @Shared
  def CM_PROTECTION_DOMAIN_CLASS = MockClassType('CM_PROTECTION_DOMAIN_CLASS', 'Lorg/apache/felix/cm/impl/helper/BaseTracker$CMProtectionDomain;', superclass: PROTECTION_DOMAIN_CLASS)
  @Shared
  def BLUEPRINT_PROTECTION_DOMAIN_CLASS = MockClassType('BLUEPRINT_PROTECTION_DOMAIN_CLASS', 'Lorg/apache/aries/blueprint/container/BlueprintProtectionDomain;', superclass: PROTECTION_DOMAIN_CLASS)
  @Shared
  def FILTERED_SERVICE_LISTENER_CLASS = MockClassType('FILTERED_SERVICE_LISTENER_CLASS', 'Lorg/eclipse/osgi/internal/serviceregistry/FilteredServiceListener;')
  @Shared
  def DELEGATING_PROTECTION_DOMAIN_CLASS = MockClassType('DELEGATING_PROTECTION_DOMAIN_CLASS', 'Lorg/apache/karaf/util/jaas/JaasHelper$DelegatingProtectionDomain;', superclass: PROTECTION_DOMAIN_CLASS)
  @Shared
  def ABSTRACT_SERVICE_REFERENCE_RECIPE_CLASS = MockClassType('ABSTRACT_SERVICE_REFERENCE_RECIPE_CLASS', 'Lorg/apache/aries/blueprint/container/AbstractServiceReferenceRecipe;')
  @Shared
  def ABSTRACT_SERVICE_REFERENCE_RECIPE$2_CLASS = MockClassType('ABSTRACT_SERVICE_REFERENCE_RECIPE$2_CLASS', 'Lorg/apache/aries/blueprint/container/AbstractServiceReferenceRecipe$2;')
  @Shared
  def ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1_CLASS = MockClassType('ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1_CLASS', 'Lorg/apache/aries/blueprint/container/AbstractServiceReferenceRecipe$2$1;', superclass: PROTECTION_DOMAIN_CLASS)

  @Shared
  def BUNDLE = MockObjectReference('BUNDLE1', BUNDLE_CLASS, getSymbolicName: BUNDLE_NAME)
  @Shared
  def MODULE_WIRING = MockObjectReference('MODULE_WIRING', MODULE_WIRING_CLASS, getBundle: BUNDLE)
  @Shared
  def BUNDLE_LOADER = MockObjectReference('BUNDLE_LOADER', BUNDLE_LOADER_CLASS, getWiring: MODULE_WIRING)
  @Shared
  def CLASSLOADER = MockClassLoaderReference('CLASSLOADER', CLASSLOADER_CLASS)
  @Shared
  def EQUINOX_CLASSLOADER = MockObjectReference('EQUINOX_CLASSLOADER', EQUINOX_CLASSLOADER_CLASS, getBundleLoader: BUNDLE_LOADER)
  @Shared
  def BUNDLE_CLASSLOADER = MockClassLoaderReference('BUNDLE_CLASSLOADER', BUNDLE_CLASSLOADER_CLASS, getBundle: BUNDLE)
  @Shared
  def PROXY_CLASSLOADER = MockClassLoaderReference('PROXY_CLASSLOADER', PROXY_CLASSLOADER_CLASS, getParent: BUNDLE_CLASSLOADER)
  @Shared
  def BUNDLE_PERMISSIONS = MockObjectReference('BUNDLE_PERMISSIONS', BUNDLE_PERMISSIONS_CLASS, getBundle: BUNDLE)
  @Shared
  def PROTECTION_DOMAIN = MockObjectReference('PROTECTION_DOMAIN', PROTECTION_DOMAIN_CLASS, getClassLoader: PROXY_CLASSLOADER)
  @Shared
  def PROTECTION_DOMAIN_WITH_BUNDLE_PERMISSIONS = MockObjectReference('PROTECTION_DOMAIN_WITH_BUNDLE_PERMISSIONS', PROTECTION_DOMAIN_CLASS, getPermissions: BUNDLE_PERMISSIONS)
  @Shared
  def BUNDLE_CONTEXT = MockObjectReference('BUNDLE_CONTEXT', BUNDLE_CONTEXT_CLASS, getBundle: BUNDLE)
  @Shared
  def GET_BUNDLE_CONTEXT = MockObjectReference('GET_BUNDLE_CONTEXT', GET_BUNDLE_CONTEXT_CLASS, getBundleContext: BUNDLE_CONTEXT)
  @Shared
  def CM_PROTECTION_DOMAIN = MockObjectReference('CM_PROTECTION_DOMAIN', CM_PROTECTION_DOMAIN_CLASS, bundle: BUNDLE)
  @Shared
  def BLUEPRINT_PROTECTION_DOMAIN = MockObjectReference('BLUEPRINT_PROTECTION_DOMAIN', BLUEPRINT_PROTECTION_DOMAIN_CLASS, bundleContext: BUNDLE_CONTEXT)
  @Shared
  def FILTERED_SERVICE_LISTENER = MockObjectReference('FILTERED_SERVICE_LISTENER', FILTERED_SERVICE_LISTENER_CLASS, context: BUNDLE_CONTEXT)
  @Shared
  def DELEGATING_PROTECTION_DOMAIN = MockObjectReference('DELEGATING_PROTECTION_DOMAIN', DELEGATING_PROTECTION_DOMAIN_CLASS, delegate: PROTECTION_DOMAIN_WITH_BUNDLE_PERMISSIONS)
  @Shared
  def ABSTRACT_SERVICE_REFERENCE_RECIPE = MockObjectReference('ABSTRACT_SERVICE_REFERENCE_RECIPE', ABSTRACT_SERVICE_REFERENCE_RECIPE_CLASS, getBundleContextForServiceLookup: BUNDLE_CONTEXT)
  @Shared
  def ABSTRACT_SERVICE_REFERENCE_RECIPE$2 = MockObjectReference('ABSTRACT_SERVICE_REFERENCE_RECIPE$2', ABSTRACT_SERVICE_REFERENCE_RECIPE$2_CLASS, getContainerThis: ABSTRACT_SERVICE_REFERENCE_RECIPE)
  @Shared
  def ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1 = MockObjectReference('ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1', ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1_CLASS, getContainerThis: ABSTRACT_SERVICE_REFERENCE_RECIPE$2)

  @Shared
  def CLASS_OBJ = MockClassObjectReference('CLASS_OBJ', getProtectionDomain0: PROTECTION_DOMAIN_WITH_BUNDLE_PERMISSIONS)
  @Shared
  def CLASS = MockClassType('CLASS', 'Lsome/class/ClassName;', classObject: CLASS_OBJ)
  @Shared
  def STACKFRAME = Mock(StackFrame, name: 'STACKFRAME') {
    location() >> Mock(Location) {
      declaringType() >> CLASS
    }
  }

  @Unroll
  def "test get() when not found in cache and with no backdoor and #and_what"() {
    given:
      def debug = Mock(Debug)
      def cache = Mock(Map)
      def backdoor = Mock(Backdoor)

    when:
      def result = new BundleUtil(debug).get(obj)

    then:
      result == bundle

    and:
      debug.reflection() >> REFLECTION
      (cache_min..cache_max) * debug.computeIfAbsent(*_) >> cache
      (cache_min..cache_max) * cache.get(obj) >> null
      (back_min..back_max) * debug.backdoor() >> backdoor
      (back_min..back_max) * backdoor.getBundle(*_) >> { throw new IllegalStateException() }
      cached * cache.put(_, {
        (bundle != null) ? it.is(bundle) : it.is(BundleUtil.NULL_BUNDLE)
      }) >> null

    where:
      and_what                                                     || obj                                       | cached | cache_min | cache_max | back_min | back_max || bundle
      'a bundle'                                                   || BUNDLE                                    | 1      | 1         | _         | 1        | _        || BUNDLE_NAME
      'an equinox classloader'                                     || EQUINOX_CLASSLOADER                       | 3      | 1         | _         | 1        | _        || BUNDLE_NAME
      'a protection domain with bundle permissions'                || PROTECTION_DOMAIN_WITH_BUNDLE_PERMISSIONS | 3      | 1         | _         | 1        | _        || BUNDLE_NAME
      'a class that has a getBundle() method'                      || BUNDLE_CONTEXT                            | 2      | 1         | _         | 1        | _        || BUNDLE_NAME
      'a class that has a getBundleContext() method'               || GET_BUNDLE_CONTEXT                        | 3      | 1         | _         | 1        | _        || BUNDLE_NAME
      'a class that has a bundle field'                            || CM_PROTECTION_DOMAIN                      | 2      | 1         | _         | 1        | _        || BUNDLE_NAME
      'a class that has a bundleContext field'                     || BLUEPRINT_PROTECTION_DOMAIN               | 3      | 1         | _         | 1        | _        || BUNDLE_NAME
      'a class that has a context field'                           || FILTERED_SERVICE_LISTENER                 | 3      | 1         | _         | 1        | _        || BUNDLE_NAME
      'a protection domain that has a delegate field'              || DELEGATING_PROTECTION_DOMAIN              | 4      | 1         | _         | 1        | _        || BUNDLE_NAME
      'a class that has a getBundleContextForServiceLookup method' || ABSTRACT_SERVICE_REFERENCE_RECIPE         | 3      | 1         | _         | 1        | _        || BUNDLE_NAME
      'an innerclass'                                              || ABSTRACT_SERVICE_REFERENCE_RECIPE$2       | 4      | 1         | _         | 1        | _        || BUNDLE_NAME
      'an innerclass inside another innerclass'                    || ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1     | 5      | 1         | _         | 1        | _        || BUNDLE_NAME
      'a standard protection domain'                               || PROTECTION_DOMAIN                         | 4      | 1         | _         | 1        | _        || BUNDLE_NAME
      'a classloader that has a parent'                            || PROXY_CLASSLOADER                         | 3      | 1         | _         | 1        | _        || BUNDLE_NAME
      'a standard classloader'                                     || CLASSLOADER                               | 1      | 1         | _         | 1        | _        || null
      'a class object'                                             || CLASS_OBJ                                 | 4      | 1         | _         | 1        | _        || BUNDLE_NAME
      'a class reference'                                          || CLASS                                     | 4      | 1         | _         | 1        | _        || BUNDLE_NAME
      'a stack frame'                                              || STACKFRAME                                | 4      | 1         | _         | 1        | _        || BUNDLE_NAME
      'null'                                                       || null                                      | 0      | 0         | 0         | 0        | 0        || null
      'something unsupported'                                      || 'abc'                                     | 0      | 1         | _         | 0        | 0        || null
  }

  @Unroll
  def "test get() when found in cache as #as_what"() {
    given:
      def debug = Mock(Debug)
      def cache = Mock(Map)

    when:
      def result = new BundleUtil(debug).get(BUNDLE)

    then:
      result == bundle

    and:
      1 * debug.reflection() >> REFLECTION
      1 * debug.computeIfAbsent(*_) >> cache
      1 * cache.get(BUNDLE) >> cached
      0 * debug.backdoor()
      0 * cache.put(*_)

    where:
      as_what         || cached                 | bundle
      'a bundle name' || 'some.cached.bundle'   | 'some.cached.bundle'
      'null'          || BundleUtil.NULL_BUNDLE | null
  }

  @Unroll
  def "test get() when not found in cache and provided by backdoor as #as_what"() {
    given:
      def debug = Mock(Debug)
      def cache = Mock(Map)
      def backdoor = Mock(Backdoor)

    when:
      def result = new BundleUtil(debug).get(BUNDLE)

    then:
      result == bundle

    and:
      1 * debug.reflection() >> REFLECTION
      1 * debug.computeIfAbsent(*_) >> cache
      1 * cache.get(BUNDLE) >> null
      1 * debug.backdoor() >> backdoor
      1 * backdoor.getBundle(*_) >> bundle
      1 * cache.put(BUNDLE, {
        (bundle != null) ? it.is(bundle) : it.is(BundleUtil.NULL_BUNDLE)
      }) >> null

    where:
      as_what         || bundle
      'a bundle name' || 'some.backdoor.bundle'
      'null'          || null
  }

  @Unroll
  def "test get() when not found in cache and backdoor failed with #exception.class.simpleName"() {
    given:
      def debug = Mock(Debug)
      def cache = Mock(Map)
      def backdoor = Mock(Backdoor)

    when:
      def result = new BundleUtil(debug).get(BUNDLE)

    then:
      result == BUNDLE_NAME

    and:
      debug.reflection() >> REFLECTION
      1 * debug.computeIfAbsent(*_) >> cache
      1 * cache.get(BUNDLE) >> null
      1 * debug.backdoor() >> backdoor
      1 * backdoor.getBundle(*_) >> { throw exception }
      1 * cache.put(BUNDLE, BUNDLE_NAME) >> null

    where:
      exception << [new NullPointerException(), new Exception(), new Error()]
  }

  def "test get() when not found in cache and backdoor failed with OutOfMemoryError"() {
    given:
      def debug = Mock(Debug)
      def cache = Mock(Map)
      def backdoor = Mock(Backdoor)
      def exception = new OutOfMemoryError()

    when:
      new BundleUtil(debug).get(BUNDLE)

    then:
      def e = thrown(OutOfMemoryError)

    and:
      e.is(exception)

    and:
      debug.reflection() >> REFLECTION
      1 * debug.computeIfAbsent(*_) >> cache
      1 * cache.get(BUNDLE) >> null
      1 * debug.backdoor() >> backdoor
      1 * backdoor.getBundle(*_) >> { throw exception }
      0 * cache.put(BUNDLE, BUNDLE_NAME)
  }
}
