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
package org.codice.acdebugger

import com.sun.jdi.ClassLoaderReference
import com.sun.jdi.ClassObjectReference
import com.sun.jdi.ClassType
import com.sun.jdi.Field
import com.sun.jdi.InterfaceType
import com.sun.jdi.ObjectReference
import com.sun.jdi.PrimitiveType
import com.sun.jdi.PrimitiveValue
import com.sun.jdi.ReferenceType
import org.codice.acdebugger.api.ReflectionUtil
import spock.lang.Shared
import spock.lang.Specification

class ReflectionSpecification extends Specification {
  @Shared
  def REFLECTION = Mock(ReflectionUtil) {
    isInstance(_, _) >> {
      args -> (args[1] != null) && REFLECTION.isAssignableFrom(args[0], args[1].referenceType())
    }
  }

  @Shared
  def OBJECT_CLASS = MockClassType('OBJECT_CLASS', 'Ljava/lang/Object;', superclass: null)

  @Shared
  def CLASS_CLASS = MockClassType('CLASS_CLASS', 'Ljava/lang/Class;')

  /**
   * Creates a mock for a {@link ClassType}.
   *
   * @param options a map of additional options for the class type where <code>classObject</code> is
   *        an optional class object to return as the {@link ReferenceType#classObject} types,
   *        <code>superclass</code> is an optional base reference type, <code>interfaces</code>
   *        is an optional list of interfaces reference types, and <code>container</code> is an optional
   *        container class reference type
   *
   * @param mockName the name for the mock
   * @param s the class signature for the reference type
   * @return the corresponding mocked object
   */
  def MockClassType(def options = [:], def mockName, def s) {
    def obj = options['classObject']
    def sc = options.containsKey('superclass') ? options['superclass'] : OBJECT_CLASS
    def interfaces = options['interfaces'] ?: []
    def container = options['container']
    def fieldList = []

    if (container) {
      // get all this$ fields from its container if any and setup the next one to our direct container
      container.fields().findAll { it.name().startsWith('this$') }.each { fieldList.add(it) }
      def size = fieldList.size()

      fieldList.add(Mock(Field) {
        name() >> 'this$' + size
      })
    }
    def type = Mock(ClassType, name: mockName) {
      signature() >> s
      genericSignature() >> s
      name() >> s.substring(1, s.length() - 1).replace('/', '.')
      classObject() >> obj
      superclass() >> sc
      allInterfaces() >> interfaces
      fields() >> fieldList
    }

    REFLECTION.isAssignableFrom({
      (it == s) || ((sc != null) && (it == sc.signature())) || interfaces.find { i -> (it == i.signature()) }
    }, {
      it.is(type)
    }) >> true
    type
  }

  /**
   * Creates a mock for a {@link InterfaceType}.
   *
   * @param options a map of additional options for the class type where <code>classObject</code> is
   *        an optional class object to return as the {@link ReferenceType#classObject} types,
   *        <code>superclass</code> is an optional base reference type, and <code>interfaces</code>
   *        is an optional list of interfaces reference types
   * @param mockName the name for the mock
   * @param s the class signature for the interface type
   * @return the corresponding mocked object
   */
  def MockInterfaceType(def options = [:], def mockName, def s) {
    def obj = options['classObject']
    def interfaces = options['superinterfaces'] ?: []
    def type = Mock(InterfaceType, name: mockName) {
      signature() >> s
      genericSignature() >> s
      name() >> s.substring(1, s.length() - 1).replace('/', '.')
      classObject() >> obj
      superinterfaces() >> interfaces
    }

    REFLECTION.isAssignableFrom({
      (it == s) || interfaces.find { i -> (it == i.signature()) }
    }, {
      it.is(type)
    }) >> true
    type
  }

  /**
   * Creates a mock for a {@link PrimitiveType}.
   *
   * @param mockName the name for the mock
   * @param s the signature for the primitive type
   * @return the corresponding mocked object
   */
  def MockPrimitiveType(def mockName, def s) {
    Mock(PrimitiveType) {
      signature() >> s
      name() >> s
    }
  }

  /**
   * Creates a mock for an {@link ObjectReference}.
   *
   * @param mocks an optional map of methods or fields with their return value to add to the mock
   * @param mockName the name for the mock
   * @param clazz the reference type to create the mock for
   * @return the corresponding mocked object
   */
  def MockObjectReference(def mocks = [:], def mockName, def clazz) {
    MockReference(mocks, ObjectReference, mockName, clazz)
  }

  /**
   * Creates a mock for a {@link ClassLoaderReference}.
   *
   * @param mocks an optional map of methods or fields with their return value to add to the mock
   * @param mockName the name for the mock
   * @param clazz the reference type to create the mock for
   * @return the corresponding mocked object
   */
  def MockClassLoaderReference(def mocks = [:], def mockName, def clazz) {
    MockReference(mocks, ClassLoaderReference, mockName, clazz)
  }

  /**
   * Creates a mock for a {@link ClassObjectReference}.
   *
   * @param mocks an optional map of methods or fields with their return value to add to the mock
   * @param mockName the name for the mock
   * @return the corresponding mocked object
   */
  def MockClassObjectReference(def mocks = [:], def mockName) {
    MockReference(mocks, ClassObjectReference, mockName, CLASS_CLASS)
  }

  /**
   * Creates a mock for a {@link com.sun.jdi.PrimitiveValue}.
   *
   * @param mockName the name for the mock
   * @param clazz the type to create the mock for
   * @return the corresponding mocked object
   */
  def MockPrimitiveValue(def mockName, def clazz) {
    Mock(PrimitiveValue, name: mockName) {
      type() >> clazz
    }
  }

  /**
   * Creates a mock reference.
   *
   * @param mocks an optional map of methods or fields with their return value to add to the mock
   * @param ref the type of reference to create
   * @param mockName the name for the mock
   * @param clazz the reference type to create the mock for
   * @return the corresponding mocked object
   */
  private def MockReference(def mocks = [:], def ref, def mockName, def clazz) {
    def obj = Mock(ref, name: mockName) {
      referenceType() >> clazz
      type() >> clazz
    }

    interactWith(mocks, 'toString', obj)
    interactWith(mocks, 'getContainerThis', obj)

    interactWith(mocks, 'invoke', obj, 'getSymbolicName', ReflectionUtil.METHOD_SIGNATURE_NO_ARGS_STRING_RESULT)
    interactWith(mocks, 'invoke', obj, 'getBundleLoader', '()Lorg/eclipse/osgi/internal/loader/BundleLoader;')
    interactWith(mocks, 'invoke', obj, 'getWiring', '()Lorg/eclipse/osgi/container/ModuleWiring;')
    interactWith(mocks, 'invoke', obj, 'getCodeSource', '()Ljava/security/CodeSource;')
    interactWith(mocks, 'invoke', obj, 'getProtectionDomain0', '()Ljava/security/ProtectionDomain;')
    interactWith(mocks, 'invoke', obj, 'implies()', '(Ljava/security/Permission;)Z')
    interactWith(mocks, 'invoke', obj, 'getActions', ReflectionUtil.METHOD_SIGNATURE_NO_ARGS_STRING_RESULT)
    interactWith(mocks, 'invoke', obj, 'getName', ReflectionUtil.METHOD_SIGNATURE_NO_ARGS_STRING_RESULT)
    interactWith(mocks, 'invoke', obj, 'getProperty()', '(Ljava/lang/String;)Ljava/lang/Object;')
    interactWith(mocks, 'invoke', obj, 'getServiceReference', '()Lorg/osgi/framework/ServiceReference;')

    interactWith(mocks, 'invokeAndReturnNullIfNotFound', obj, 'getBundle', '()Lorg/osgi/framework/Bundle;')
    interactWith(mocks, 'invokeAndReturnNullIfNotFound', obj, 'getBundleContext', '()Lorg/osgi/framework/BundleContext;')
    interactWith(mocks, 'invokeAndReturnNullIfNotFound', obj, 'getPermissions', '()Ljava/security/PermissionCollection;')
    interactWith(mocks, 'invokeAndReturnNullIfNotFound', obj, 'getBundleContextForServiceLookup', '()Lorg/osgi/framework/BundleContext;')
    interactWith(mocks, 'invokeAndReturnNullIfNotFound', obj, 'getClassLoader', '()Ljava/lang/ClassLoader;')
    interactWith(mocks, 'invokeAndReturnNullIfNotFound', obj, 'getParent', '()Ljava/lang/ClassLoader;')
    interactWith(mocks, 'invokeAndReturnNullIfNotFound', obj, 'getLocation', '()Ljava/net/URL;')

    interactWith(mocks, 'get', obj, 'bundle', 'Lorg/osgi/framework/Bundle;')
    interactWith(mocks, 'get', obj, 'bundleContext', 'Lorg/osgi/framework/BundleContext;')
    interactWith(mocks, 'get', obj, 'context', 'Lorg/osgi/framework/BundleContext;')
    interactWith(mocks, 'get', obj, 'delegate', 'Ljava/security/ProtectionDomain;')
    interactWith(mocks, 'get', obj, 'objectClass', '[Ljava/lang/String;')

    interactNew(mocks, obj, clazz.signature(), _)

    obj
  }

  private def interactNew(def mocks, def obj, def type, def signature) {
    if (mocks.containsKey('newInstance')) {
      def newInstance = mocks['newInstance']

      REFLECTION.newInstance(type, signature, newInstance) >> obj
    }
  }

  private def interactWith(def mocks, def what, def obj) {
    if (mocks.containsKey(what)) {
      REFLECTION."$what"({
        (it != null) && it.is(obj)
      }) >> mocks[what]
    }
  }

  private def interactWith(def mocks, def what, def obj, def method, def signature) {
    if (mocks.containsKey(method)) {
      if (method.endsWith('()')) {
        REFLECTION."$what"({
          (it != null) && it.is(obj)
        }, method[0..-3], signature, mocks[method]['args']) >> mocks[method]['result']
      } else {
        REFLECTION."$what"({
          (it != null) && it.is(obj)
        }, method, signature) >> mocks[method]
      }
    }
  }
}
