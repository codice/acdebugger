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

import com.sun.jdi.ArrayReference
import com.sun.jdi.ArrayType
import com.sun.jdi.BooleanValue
import com.sun.jdi.ByteValue
import com.sun.jdi.CharValue
import com.sun.jdi.ClassNotLoadedException
import com.sun.jdi.ClassType
import com.sun.jdi.DoubleValue
import com.sun.jdi.Field
import com.sun.jdi.FloatValue
import com.sun.jdi.IncompatibleThreadStateException
import com.sun.jdi.IntegerValue
import com.sun.jdi.InvalidStackFrameException
import com.sun.jdi.InvalidTypeException
import com.sun.jdi.InvocationException
import com.sun.jdi.LongValue
import com.sun.jdi.Method
import com.sun.jdi.ObjectCollectedException
import com.sun.jdi.ObjectReference
import com.sun.jdi.ReferenceType
import com.sun.jdi.ShortValue
import com.sun.jdi.StringReference
import com.sun.jdi.ThreadReference
import com.sun.jdi.VMCannotBeModifiedException
import com.sun.jdi.Value
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VoidValue
import org.codice.acdebugger.ReflectionSpecification
import org.codice.acdebugger.impl.DebugContext
import spock.lang.Shared
import spock.lang.Unroll

import java.util.concurrent.atomic.AtomicInteger

class ReflectionUtilSpec extends ReflectionSpecification {
  static def MAP_SIGNATURE = "Ljava/util/Map;"
  static def SORTED_MAP_SIGNATURE = "Ljava/util/SortedMap;"
  static def ABSTRACT_MAP_SIGNATURE = "Ljava/util/AbstractMap;"
  static def TREE_MAP_SIGNATURE = "Ljava/util/TreeMap;"

  @Shared
  def MAP_CLASS = MockInterfaceType('MAP_CLASS', MAP_SIGNATURE)
  @Shared
  def SORTED_MAP_CLASS = MockInterfaceType('SORTED_MAP_CLASS', SORTED_MAP_SIGNATURE, superinterfaces: [MAP_CLASS])
  @Shared
  def ABSTRACT_MAP_CLASS = MockClassType('ABSTRACT_MAP_CLASS', ABSTRACT_MAP_SIGNATURE, interfaces: [MAP_CLASS])
  @Shared
  def TREE_MAP_CLASS = MockClassType('TREE_MAP_CLASS', TREE_MAP_SIGNATURE, superclass: ABSTRACT_MAP_CLASS, interfaces: [MAP_CLASS, SORTED_MAP_CLASS])
  @Shared
  def TREE_MAP_CLASS2 = MockClassType('TREE_MAP_CLASS2', TREE_MAP_SIGNATURE, superclass: ABSTRACT_MAP_CLASS, interfaces: [MAP_CLASS, SORTED_MAP_CLASS])
  @Shared
  def PROTECTION_DOMAIN_CLASS = MockClassType('PROTECTION_DOMAIN_CLASS', 'Ljava/security/ProtectionDomain;')
  @Shared
  def ABSTRACT_SERVICE_REFERENCE_RECIPE_CLASS = MockClassType('ABSTRACT_SERVICE_REFERENCE_RECIPE_CLASS', 'Lorg/apache/aries/blueprint/container/AbstractServiceReferenceRecipe;')
  @Shared
  def ABSTRACT_SERVICE_REFERENCE_RECIPE$2_CLASS = MockClassType('ABSTRACT_SERVICE_REFERENCE_RECIPE$2_CLASS', 'Lorg/apache/aries/blueprint/container/AbstractServiceReferenceRecipe$2;', container: ABSTRACT_SERVICE_REFERENCE_RECIPE_CLASS)
  @Shared
  def ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1_CLASS = MockClassType('ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1_CLASS', 'Lorg/apache/aries/blueprint/container/AbstractServiceReferenceRecipe$2$1;', superclass: PROTECTION_DOMAIN_CLASS, container: ABSTRACT_SERVICE_REFERENCE_RECIPE$2_CLASS)

  @Shared
  def FLOAT_TYPE = MockPrimitiveType('FLOAT_TYPE', 'F')

  @Shared
  def TREE_MAP = MockObjectReference('TREE_MAP', TREE_MAP_CLASS)
  @Shared
  def TREE_MAP2 = MockObjectReference('TREE_MAP2', TREE_MAP_CLASS)
  @Shared
  def ABSTRACT_SERVICE_REFERENCE_RECIPE = MockObjectReference('ABSTRACT_SERVICE_REFERENCE_RECIPE', ABSTRACT_SERVICE_REFERENCE_RECIPE_CLASS)
  @Shared
  def ABSTRACT_SERVICE_REFERENCE_RECIPE$2 = MockObjectReference('ABSTRACT_SERVICE_REFERENCE_RECIPE$2', ABSTRACT_SERVICE_REFERENCE_RECIPE$2_CLASS)
  @Shared
  def ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1 = MockObjectReference('ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1', ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1_CLASS)

  @Shared
  def FLOAT = MockPrimitiveValue('FLOAT', FLOAT_TYPE)
  @Shared
  def INT = Stub(IntegerValue)
  @Shared
  def BOOLEAN = Stub(BooleanValue)
  @Shared
  def STRING = Stub(StringReference)

  @Shared
  def METHOD = Mock(Method) {
    isAbstract() >> false
  }
  @Shared
  def METHOD2 = Mock(Method) {
    isAbstract() >> false
  }
  @Shared
  def ABSTRACT_METHOD = Mock(Method) {
    isAbstract() >> true
  }
  @Shared
  def FIELD = Stub(Field)

  def "test constructor with a thread"() {
    given:
      def thread = Stub(ThreadReference)
      def vm = Stub(VirtualMachine)

    when:
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, thread)

    then:
      reflection.virtualMachine() == vm
      reflection.hasThread()
      reflection.thread() == thread
  }

  def "test constructor with no thread"() {
    given:
      def vm = Stub(VirtualMachine)

    when:
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, null)

    then:
      reflection.virtualMachine() == vm
      !reflection.hasThread()

    when:
      reflection.thread()

    then:
      thrown(IllegalStateException)
  }

  @Unroll
  def "test isAssignableFrom() with #with_what"() {
    given:
      def signatureCache = Mock(Map)
      def context = Mock(DebugContext)
      def reflection = new ReflectionUtil(context, Stub(VirtualMachine), null)

    when:
      def returnedResult = reflection.isAssignableFrom(signature, type)

    then:
      returnedResult == result
      cache == cached

    and:
      cache_count * context.computeIfAbsent(ReflectionUtil.ASSIGNABLE_FROM_CACHE, _) >> signatureCache
      cache_count * signatureCache.computeIfAbsent(signature, _) >> cache

    where:
      with_what                                                                                  || signature              | type             || cache_count | cache                       || cached                      | result
      'a null type'                                                                              || ''                     | null             || 0           | [:]                         || [:]                         | false
      'a class type that corresponds to the signature'                                           || TREE_MAP_SIGNATURE     | TREE_MAP_CLASS   || 0           | [:]                         || [:]                         | true
      'a class type which implements the signature and nothing cached'                           || SORTED_MAP_SIGNATURE   | TREE_MAP_CLASS   || 1           | [:]                         || [(TREE_MAP_CLASS): true]    | true
      'a class type which extends the signature and nothing cached'                              || ABSTRACT_MAP_SIGNATURE | TREE_MAP_CLASS   || 1           | [:]                         || [(TREE_MAP_CLASS): true]    | true
      'a class type and signature cached as true'                                                || MAP_SIGNATURE          | TREE_MAP_CLASS   || 1           | [(TREE_MAP_CLASS): true]    || [(TREE_MAP_CLASS): true]    | true
      'a class type and signature cached as false'                                               || MAP_SIGNATURE          | TREE_MAP_CLASS   || 1           | [(TREE_MAP_CLASS): false]   || [(TREE_MAP_CLASS): false]   | false
      'an interface type that corresponds to the signature'                                      || SORTED_MAP_SIGNATURE   | SORTED_MAP_CLASS || 0           | [:]                         || [:]                         | true
      'an interface type which extends the signature and nothing cached'                         || MAP_SIGNATURE          | SORTED_MAP_CLASS || 1           | [:]                         || [(SORTED_MAP_CLASS): true]  | true
      'an interface type and signature cached as true'                                           || MAP_SIGNATURE          | SORTED_MAP_CLASS || 1           | [(SORTED_MAP_CLASS): true]  || [(SORTED_MAP_CLASS): true]  | true
      'an interface type and signature cached as false'                                          || MAP_SIGNATURE          | SORTED_MAP_CLASS || 1           | [(SORTED_MAP_CLASS): false] || [(SORTED_MAP_CLASS): false] | false
      'a primitive type that corresponds to the signature'                                       || 'F'                    | FLOAT_TYPE       || 0           | [:]                         || [:]                         | true
      'a primitive type that does not correspond to the signature and nothing cached'            || 'B'                    | FLOAT_TYPE       || 1           | [:]                         || [(FLOAT_TYPE): false]       | false
      'a primitive type that does not correspond to the signature and signature cached as true'  || 'B'                    | FLOAT_TYPE       || 1           | [(FLOAT_TYPE): true]        || [(FLOAT_TYPE): true]        | true // should never happen!!!
      'a primitive type that does not correspond to the signature and signature cached as false' || 'B'                    | FLOAT_TYPE       || 1           | [(FLOAT_TYPE): false]       || [(FLOAT_TYPE): false]       | false
  }

  @Unroll
  def "test isInstance() with #with_what"() {
    given:
      def context = Mock(DebugContext)
      def reflection = new ReflectionUtil(context, Stub(VirtualMachine), null)

    when:
      def returnedResult = reflection.isInstance(signature, value)

    then:
      returnedResult == result

    and:
      context.computeIfAbsent(ReflectionUtil.ASSIGNABLE_FROM_CACHE, _) >> Mock(Map) {
        computeIfAbsent(signature, _) >> [:]
      }

    where:
      with_what                                          || signature              | value    || result
      'a null value'                                     || ''                     | null     || false
      'a reference value of the signature'               || TREE_MAP_SIGNATURE     | TREE_MAP || true
      'a reference value which implements the signature' || SORTED_MAP_SIGNATURE   | TREE_MAP || true
      'a reference value which extends the signature'    || ABSTRACT_MAP_SIGNATURE | TREE_MAP || true
      'a primitive value of the signature'               || 'F'                    | FLOAT    || true
      'a primitive value not of the signature'           || 'B'                    | FLOAT    || false
  }

  @Unroll
  def "test classes() when #when_what"() {
    given:
      def vm = Mock(VirtualMachine)
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, null)

    when:
      def returnedResult = reflection.classes(MAP_SIGNATURE)

    then:
      returnedResult.toArray() == result

    and:
      vm.classesByName('java.util.Map') >> classes

    where:
      when_what                          || classes                           || result
      'vm is returning interfaces'       || [MAP_CLASS, SORTED_MAP_CLASS]     || []
      'vm is returning one class'        || [TREE_MAP_CLASS]                  || [TREE_MAP_CLASS]
      'vm is returning multiple classes' || [TREE_MAP_CLASS, TREE_MAP_CLASS2] || [TREE_MAP_CLASS, TREE_MAP_CLASS2]
      'vm returning nothing'             || []                                || []
  }

  @Unroll
  def "test getClass() when #when_what"() {
    given:
      def vm = Mock(VirtualMachine)
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, null)

    when:
      def returnedResult = reflection.getClass(TREE_MAP_SIGNATURE)

    then:
      returnedResult == result

    and:
      vm.classesByName('java.util.TreeMap') >> classes

    where:
      when_what                          || classes                           || result
      'vm is returning multiple classes' || [TREE_MAP_CLASS, TREE_MAP_CLASS2] || TREE_MAP_CLASS
      'vm is returning one class'        || [TREE_MAP_CLASS2]                 || TREE_MAP_CLASS2
      'vm is returning nothing'          || []                                || null
  }

  @Unroll
  def "test getContainerThis() with #with_what"() {
    given:
      def vm = Mock(VirtualMachine)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), vm, null])

    when:
      def returnedResult = reflection.getContainerThis(obj)

    then:
      returnedResult == result

    and:
      reflection.get(ABSTRACT_SERVICE_REFERENCE_RECIPE$2, {
        it.name() == 'this$0'
      }, null) >> ABSTRACT_SERVICE_REFERENCE_RECIPE
      reflection.get(ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1, {
        it.name() == 'this$0'
      }, null) >> ABSTRACT_SERVICE_REFERENCE_RECIPE
      reflection.get(ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1, {
        it.name() == 'this$1'
      }, null) >> ABSTRACT_SERVICE_REFERENCE_RECIPE$2

    where:
      with_what                             || obj                                   || result
      'null'                                || null                                  || null
      'not an innerclass'                   || ABSTRACT_SERVICE_REFERENCE_RECIPE     || null
      'an innerclass'                       || ABSTRACT_SERVICE_REFERENCE_RECIPE$2   || ABSTRACT_SERVICE_REFERENCE_RECIPE
      'an innerclass of another innerclass' || ABSTRACT_SERVICE_REFERENCE_RECIPE$2$1 || ABSTRACT_SERVICE_REFERENCE_RECIPE$2
  }

  @Unroll
  def "test findMethod() with a class when #when_what"() {
    given:
      def reflection = new ReflectionUtil(Stub(DebugContext), Stub(VirtualMachine), null)
      def type = Mock(ClassType)

    when:
      def result = reflection.findMethod(type, 'name', 'signature')

    then:
      result == method

    and:
      1 * type.concreteMethodByName('name', 'signature') >> method

    where:
      when_what                 || method
      'the method is defined'   || METHOD
      'the method is not found' || null
  }

  @Unroll
  def "test findMethod() with a reference type when #when_what"() {
    given:
      def reflection = new ReflectionUtil(Stub(DebugContext), Stub(VirtualMachine), null)
      def type = Mock(ReferenceType)

    when:
      def result = reflection.findMethod(type, 'name', 'signature')

    then:
      result == method

    and:
      1 * type.methodsByName('name', 'signature') >> methods

    where:
      when_what                           || methods                   || method
      'the method is defined'             || [METHOD]                  || METHOD
      'more than one method is defined'   || [METHOD, METHOD2]         || METHOD
      'some defined methods are abstract' || [ABSTRACT_METHOD, METHOD] || METHOD
      'only abstract methods are defined' || [ABSTRACT_METHOD]         || null
      'the method is not found'           || []                        || null
  }

  @Unroll
  def "test findConstructor() when #when_what"() {
    given:
      def reflection = new ReflectionUtil(Stub(DebugContext), Stub(VirtualMachine), null)
      def type = Mock(ClassType)

    when:
      def result = reflection.findConstructor(type, 'signature')

    then:
      result == ctor

    and:
      1 * type.concreteMethodByName('<init>', 'signature') >> ctor

    where:
      when_what                      || ctor
      'the constructor is defined'   || METHOD
      'the constructor is not found' || null
  }

  @Unroll
  def "test get() with #with_what"() {
    given:
      def context = Mock(DebugContext) {
        computeIfAbsent(ReflectionUtil.ASSIGNABLE_FROM_CACHE, _) >> [:]
      }
      def reflection = Spy(ReflectionUtil, constructorArgs: [context, Stub(VirtualMachine), null])
      def type = MockClassType('type', TREE_MAP_SIGNATURE)
      def obj = null_obj ? null : MockObjectReference('obj', type)
      def fieldObj = (field instanceof String) ? FIELD : field

    when:
      def returnedResult = reflection.get(obj, field, signature)

    then:
      returnedResult == result

    and:
      obj_count * obj.getValue(fieldObj) >> value
      // result is converted from its mirror whenever it is not null
      ((result != null) ? 1 : 0) * reflection.fromMirror(value) >> value
      field_count * type.fieldByName(field) >> fieldObj

    where:
      with_what                                                                                    || null_obj | field  | signature     | obj_count | field_count | value    || result
      'a null field'                                                                               || false    | null   | 'Z'           | 0         | 0           | null     || null
      'a null object and a field name'                                                             || true     | 'name' | 'Z'           | 0         | 0           | null     || null
      'a null object and a field'                                                                  || true     | FIELD  | 'Z'           | 0         | 0           | null     || null
      'a field containing null'                                                                    || false    | FIELD  | 'Z'           | 1         | 0           | null     || null
      'a field containing something while no signature provided'                                   || false    | FIELD  | null          | 1         | 0           | TREE_MAP || TREE_MAP
      'a field containing something that is an instance of the provided signature'                 || false    | FIELD  | MAP_SIGNATURE | 1         | 0           | TREE_MAP || TREE_MAP
      'a field containing something that is not an instance of the provided signature'             || false    | FIELD  | 'Z'           | 1         | 0           | TREE_MAP || null
      'the name of a field containing null'                                                        || false    | 'name' | 'Z'           | 1         | 1           | null     || null
      'the name of a field containing something while no signature provided'                       || false    | 'name' | null          | 1         | 1           | TREE_MAP || TREE_MAP
      'the name of a field containing something that is an instance of the provided signature'     || false    | 'name' | MAP_SIGNATURE | 1         | 1           | TREE_MAP || TREE_MAP
      'the name of a field containing something that is not an instance of the provided signature' || false    | 'name' | 'Z'           | 1         | 1           | TREE_MAP || null
  }

  @Unroll
  def "test getStatic() with #with_what"() {
    given:
      def context = Mock(DebugContext) {
        computeIfAbsent(ReflectionUtil.ASSIGNABLE_FROM_CACHE, _) >> [:]
      }
      def reflection = Spy(ReflectionUtil, constructorArgs: [context, Stub(VirtualMachine), null])
      def clazz = null_class ? null : MockClassType('clazz', TREE_MAP_SIGNATURE)
      def fieldObj = (field instanceof String) ? FIELD : field

    when:
      def returnedResult = reflection.getStatic(clazz, field, signature)

    then:
      returnedResult == result

    and:
      class_count * clazz.getValue(fieldObj) >> value
      // result is converted from its mirror whenever it is not null
      ((result != null) ? 1 : 0) * reflection.fromMirror(value) >> value
      field_count * clazz.fieldByName(field) >> fieldObj

    where:
      with_what                                                                                    || null_class | field  | signature     | class_count | field_count | value    || result
      'a null field'                                                                               || false      | null   | 'Z'           | 0           | 0           | null     || null
      'a null class and a field name'                                                              || true       | 'name' | 'Z'           | 0           | 0           | null     || null
      'a null class and a field'                                                                   || true       | FIELD  | 'Z'           | 0           | 0           | null     || null
      'a field containing null'                                                                    || false      | FIELD  | 'Z'           | 1           | 0           | null     || null
      'a field containing something while no signature provided'                                   || false      | FIELD  | null          | 1           | 0           | TREE_MAP || TREE_MAP
      'a field containing something that is an instance of the provided signature'                 || false      | FIELD  | MAP_SIGNATURE | 1           | 0           | TREE_MAP || TREE_MAP
      'a field containing something that is not an instance of the provided signature'             || false      | FIELD  | 'Z'           | 1           | 0           | TREE_MAP || null
      'the name of a field containing null'                                                        || false      | 'name' | 'Z'           | 1           | 1           | null     || null
      'the name of a field containing something while no signature provided'                       || false      | 'name' | null          | 1           | 1           | TREE_MAP || TREE_MAP
      'the name of a field containing something that is an instance of the provided signature'     || false      | 'name' | MAP_SIGNATURE | 1           | 1           | TREE_MAP || TREE_MAP
      'the name of a field containing something that is not an instance of the provided signature' || false      | 'name' | 'Z'           | 1           | 1           | TREE_MAP || null
  }

  @Unroll
  def "test #what_call with #with_what and no arguments"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def obj_or_class = null_obj_or_class ? null : "$mock"('obj_or_class', mock_arg)

    when:
      def returnedResult = reflection."$what"(obj_or_class, method)

    then:
      returnedResult == result

    and:
      obj_count * obj_or_class.invokeMethod(thread, method, [], ObjectReference.INVOKE_SINGLE_THREADED) >> value
      class_count * obj_or_class.newInstance(thread, method, [], ObjectReference.INVOKE_SINGLE_THREADED | ObjectReference.INVOKE_NONVIRTUAL) >> value
      mirror_count * reflection.fromMirror(value) >> value

    where:
      what           | with_what                      | mock                  | mock_arg           || null_obj_or_class | method | obj_count | class_count | mirror_count | value    || result
      'invoke'       | 'a null method'                | 'MockObjectReference' | TREE_MAP_CLASS     || false             | null   | 0         | 0           | 0            | null     || null
      'invoke'       | 'a null object and a method'   | 'MockObjectReference' | TREE_MAP_CLASS     || true              | METHOD | 0         | 0           | 0            | null     || null
      'invoke'       | 'a method returning null'      | 'MockObjectReference' | TREE_MAP_CLASS     || false             | METHOD | 1         | 0           | 1            | null     || null
      'invoke'       | 'a method returning something' | 'MockObjectReference' | TREE_MAP_CLASS     || false             | METHOD | 1         | 0           | 1            | TREE_MAP || TREE_MAP
      'invokeStatic' | 'a null method'                | 'MockClassType'       | TREE_MAP_SIGNATURE || false             | null   | 0         | 0           | 0            | null     || null
      'invokeStatic' | 'a null object and a method'   | 'MockClassType'       | TREE_MAP_SIGNATURE || true              | METHOD | 0         | 0           | 0            | null     || null
      'invokeStatic' | 'a method returning null'      | 'MockClassType'       | TREE_MAP_SIGNATURE || false             | METHOD | 1         | 0           | 1            | null     || null
      'invokeStatic' | 'a method returning something' | 'MockClassType'       | TREE_MAP_SIGNATURE || false             | METHOD | 1         | 0           | 1            | TREE_MAP || TREE_MAP
      'newInstance'  | 'a null method'                | 'MockClassType'       | TREE_MAP_SIGNATURE || false             | null   | 0         | 0           | 0            | null     || null
      'newInstance'  | 'a null object and a method'   | 'MockClassType'       | TREE_MAP_SIGNATURE || true              | METHOD | 0         | 0           | 0            | null     || null
      'newInstance'  | 'a method returning something' | 'MockClassType'       | TREE_MAP_SIGNATURE || false             | METHOD | 0         | 1           | 1            | TREE_MAP || TREE_MAP

      what_call = what + '()'
  }

  @Unroll
  def "test #what_call with no strings when failing with #exception.class.simpleName"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def obj_or_class = "$mock"('obj_or_class', mock_arg)

    when:
      reflection."$what"(obj_or_class, METHOD)

    then:
      def e = thrown(Error)

    and:
      e.cause.is(exception)

    and:
      obj_count * obj_or_class.invokeMethod(thread, METHOD, [], ObjectReference.INVOKE_SINGLE_THREADED) >> {
        throw exception
      }
      class_count * obj_or_class.newInstance(thread, METHOD, [], ObjectReference.INVOKE_SINGLE_THREADED | ObjectReference.INVOKE_NONVIRTUAL) >> {
        throw exception
      }
      0 * reflection.fromMirror(_)

    where:
      what           || mock                  | mock_arg           || obj_count | class_count || exception
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS     || 1         | 0           || new ClassNotLoadedException('testing')
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS     || 1         | 0           || new IncompatibleThreadStateException()
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS     || 1         | 0           || new InvocationException()
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS     || 1         | 0           || new InvalidTypeException()
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS     || 1         | 0           || new VMCannotBeModifiedException()
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS     || 1         | 0           || new InvalidStackFrameException()
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE || 1         | 0           || new ClassNotLoadedException('testing')
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE || 1         | 0           || new IncompatibleThreadStateException()
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE || 1         | 0           || new InvocationException()
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE || 1         | 0           || new InvalidTypeException()
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE || 1         | 0           || new VMCannotBeModifiedException()
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE || 1         | 0           || new InvalidStackFrameException()
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE || 0         | 1           || new ClassNotLoadedException('testing')
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE || 0         | 1           || new IncompatibleThreadStateException()
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE || 0         | 1           || new InvocationException()
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE || 0         | 1           || new InvalidTypeException()
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE || 0         | 1           || new VMCannotBeModifiedException()
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE || 0         | 1           || new InvalidStackFrameException()

      what_call = what + '()'
  }

  @Unroll
  def "test #what_call with no strings amd with #with_what"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def obj_or_class = "$mock"('obj_or_class', mock_arg)
      def invoke_args = mirror_args.clone()

    when:
      def returnedResult = reflection."$what"(obj_or_class, METHOD, *args)

    then:
      returnedResult == TREE_MAP

    and:
      (args.size()) * reflection.toMirror({ it in args }) >> { mirror_args.remove(0) }
      obj_count * obj_or_class.invokeMethod(thread, METHOD, invoke_args, ObjectReference.INVOKE_SINGLE_THREADED) >> TREE_MAP
      class_count * obj_or_class.newInstance(thread, METHOD, invoke_args, ObjectReference.INVOKE_SINGLE_THREADED | ObjectReference.INVOKE_NONVIRTUAL) >> TREE_MAP
      1 * reflection.fromMirror(TREE_MAP) >> TREE_MAP

    where:
      what           | with_what                                        || mock                  | mock_arg           || args                                          || obj_count | class_count | mirror_args
      'invoke'       | 'no arguments'                                   || 'MockObjectReference' | TREE_MAP_CLASS     || []                                            || 1         | 0           | []
      'invoke'       | 'primitive arguments'                            || 'MockObjectReference' | TREE_MAP_CLASS     || [1234, false, FLOAT]                          || 1         | 0           | [INT, BOOLEAN, FLOAT]
      'invoke'       | 'null arguments'                                 || 'MockObjectReference' | TREE_MAP_CLASS     || [1234, null, FLOAT]                           || 1         | 0           | [INT, null, FLOAT]
      'invoke'       | 'a mixture of references, primitives, and nulls' || 'MockObjectReference' | TREE_MAP_CLASS     || [TREE_MAP2, STRING, false, FLOAT, 1234, null] || 1         | 0           | [TREE_MAP2, STRING, BOOLEAN, FLOAT, INT, null]
      'invokeStatic' | 'no arguments'                                   || 'MockClassType'       | TREE_MAP_SIGNATURE || []                                            || 1         | 0           | []
      'invokeStatic' | 'primitive arguments'                            || 'MockClassType'       | TREE_MAP_SIGNATURE || [1234, false, FLOAT]                          || 1         | 0           | [INT, BOOLEAN, FLOAT]
      'invokeStatic' | 'null arguments'                                 || 'MockClassType'       | TREE_MAP_SIGNATURE || [1234, null, FLOAT]                           || 1         | 0           | [INT, null, FLOAT]
      'invokeStatic' | 'a mixture of references, primitives, and nulls' || 'MockClassType'       | TREE_MAP_SIGNATURE || [TREE_MAP2, STRING, false, FLOAT, 1234, null] || 1         | 0           | [TREE_MAP2, STRING, BOOLEAN, FLOAT, INT, null]
      'newInstance'  | 'no arguments'                                   || 'MockClassType'       | TREE_MAP_SIGNATURE || []                                            || 0         | 1           | []
      'newInstance'  | 'primitive arguments'                            || 'MockClassType'       | TREE_MAP_SIGNATURE || [1234, false, FLOAT]                          || 0         | 1           | [INT, BOOLEAN, FLOAT]
      'newInstance'  | 'null arguments'                                 || 'MockClassType'       | TREE_MAP_SIGNATURE || [1234, null, FLOAT]                           || 0         | 1           | [INT, null, FLOAT]
      'newInstance'  | 'a mixture of references, primitives, and nulls' || 'MockClassType'       | TREE_MAP_SIGNATURE || [TREE_MAP2, STRING, false, FLOAT, 1234, null] || 0         | 1           | [TREE_MAP2, STRING, BOOLEAN, FLOAT, INT, null]

      what_call = what + '()'
  }

  @Unroll
  def "test #what_call with strings"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def obj_or_class = "$mock"('obj_or_class', mock_arg)
      def string2 = Mock(StringReference)
      def string3 = Mock(StringReference)
      def args = [STRING, 'abc', 123, 'def', false, null]
      def mirror_args = [STRING, string2, INT, string3, BOOLEAN, null]
      def invoke_args = mirror_args.clone()

    when:
      def returnedResult = reflection."$what"(obj_or_class, METHOD, *args)

    then:
      returnedResult == TREE_MAP

    and:
      2 * reflection.protect(_) >> {
        def mirror = it[0].get()

        if (!(mirror in [string2, string3])) {
          throw new AssertionError("unexpected mirror: $mirror")
        }
        mirror
      }
      (args.size()) * reflection.toMirror({ it in args }) >> { mirror_args.remove(0) }
      obj_count * obj_or_class.invokeMethod(thread, METHOD, invoke_args, ObjectReference.INVOKE_SINGLE_THREADED) >> TREE_MAP
      class_count * obj_or_class.newInstance(thread, METHOD, invoke_args, ObjectReference.INVOKE_SINGLE_THREADED | ObjectReference.INVOKE_NONVIRTUAL) >> TREE_MAP
      1 * reflection.fromMirror(TREE_MAP) >> TREE_MAP
      1 * string2.enableCollection()
      1 * string3.enableCollection()

    where:
      what           || mock                  | mock_arg           || obj_count | class_count
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS     || 1         | 0
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE || 1         | 0
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE || 0         | 1

      what_call = what + '()'
  }

  @Unroll
  def "test #what_call with strings and failing with #exception.class.simpleName"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def obj_or_class = "$mock"('obj_or_class', mock_arg)
      def string2 = Mock(StringReference)
      def string3 = Mock(StringReference)
      def args = [STRING, 'abc', 123, 'def', false, null]
      def mirror_args = [STRING, string2, INT, string3, BOOLEAN, null]
      def invoke_args = mirror_args.clone()

    when:
      reflection."$what"(obj_or_class, METHOD, *args)

    then:
      def e = thrown(Error)

    and:
      e.cause.is(exception)

    and:
      2 * reflection.protect(_) >> {
        def mirror = it[0].get()

        if (!(mirror in [string2, string3])) {
          throw new AssertionError("unexpected mirror: $mirror")
        }
        mirror
      }
      (args.size()) * reflection.toMirror({ it in args }) >> { mirror_args.remove(0) }
      obj_count * obj_or_class.invokeMethod(thread, METHOD, invoke_args, ObjectReference.INVOKE_SINGLE_THREADED) >> {
        throw exception
      }
      class_count * obj_or_class.newInstance(thread, METHOD, invoke_args, ObjectReference.INVOKE_SINGLE_THREADED | ObjectReference.INVOKE_NONVIRTUAL) >> {
        throw exception
      }
      0 * reflection.fromMirror(_)
      1 * string2.enableCollection()
      1 * string3.enableCollection()

    where:
      what           || mock                  | mock_arg           || obj_count | class_count || exception
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS     || 1         | 0           || new ClassNotLoadedException('testing')
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS     || 1         | 0           || new IncompatibleThreadStateException()
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS     || 1         | 0           || new InvocationException()
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS     || 1         | 0           || new InvalidTypeException()
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS     || 1         | 0           || new VMCannotBeModifiedException()
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS     || 1         | 0           || new InvalidStackFrameException()
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE || 1         | 0           || new ClassNotLoadedException('testing')
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE || 1         | 0           || new IncompatibleThreadStateException()
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE || 1         | 0           || new InvocationException()
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE || 1         | 0           || new InvalidTypeException()
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE || 1         | 0           || new VMCannotBeModifiedException()
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE || 1         | 0           || new InvalidStackFrameException()
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE || 0         | 1           || new ClassNotLoadedException('testing')
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE || 0         | 1           || new IncompatibleThreadStateException()
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE || 0         | 1           || new InvocationException()
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE || 0         | 1           || new InvalidTypeException()
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE || 0         | 1           || new VMCannotBeModifiedException()
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE || 0         | 1           || new InvalidStackFrameException()

      what_call = what + '()'
  }

  @Unroll
  def "test #what_call with strings when unable to protect the created references"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def obj_or_class = "$mock"('obj_or_class', mock_arg)
      def string2 = Mock(StringReference)
      def string3 = Mock(StringReference)
      def args = [STRING, 'abc', 123, 'def', false, null]
      def mirror_args = [STRING, string2, INT, string3]
      def exception = new ObjectCollectedException()

    when:
      reflection."$what"(obj_or_class, METHOD, *args)

    then:
      def e = thrown(Error)

    and:
      e.cause.is(exception)

    and:
      2 * reflection.protect(_) >> {
        def mirror = it[0].get()

        if (mirror == string2) {
          mirror
        } else if (mirror == string3) {
          throw exception
        } else {
          throw new AssertionError("unexpected mirror: $mirror")
        }
      }
      (mirror_args.size()) * reflection.toMirror({ it in args }) >> { mirror_args.remove(0) }
      0 * obj_or_class.invokeMethod(*_)
      0 * obj_or_class.newInstance(*_)
      0 * reflection.fromMirror(_)
      1 * string2.enableCollection()
      0 * string3.enableCollection()

    where:
      what           || mock                  | mock_arg
      'invoke'       || 'MockObjectReference' | TREE_MAP_CLASS
      'invokeStatic' || 'MockClassType'       | TREE_MAP_SIGNATURE
      'newInstance'  || 'MockClassType'       | TREE_MAP_SIGNATURE

      what_call = what + '()'
  }

  @Unroll
  def "test #what_call with method name and signature"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def args = [STRING, 'abc', 123, 'def', false, null]

    when:
      def result = reflection."$what"(obj_or_class, 'method', 'signature', *args)

    then:
      result == FLOAT

    and:
      1 * reflection.findMethod(TREE_MAP_CLASS, 'method', 'signature') >> METHOD
      1 * reflection."$what"(obj_or_class, METHOD, args) >> FLOAT

    where:
      what           || obj_or_class
      'invoke'       || TREE_MAP
      'invokeStatic' || TREE_MAP_CLASS

      what_call = what + '()'
  }

  def "test newInstance() with signature"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def args = [STRING, 'abc', 123, 'def', false, null]

    when:
      def result = reflection.newInstance(TREE_MAP_CLASS, 'signature', *args)

    then:
      result == TREE_MAP

    and:
      1 * reflection.findConstructor(TREE_MAP_CLASS, 'signature') >> METHOD
      1 * reflection.newInstance(TREE_MAP_CLASS, METHOD, args) >> TREE_MAP
  }

  @Unroll
  def "test #what_call with method name and signature and null #null_what"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def args = [STRING, 'abc', 123, 'def', false, null]

    when:
      def result = reflection."$what"(null, 'method', 'signature', *args)

    then:
      result == null

    and:
      0 * reflection.findMethod(*_)
      0 * reflection."$what"(_, { it instanceof Method }, _)

    where:
      what           | null_what
      'invoke'       | 'object'
      'invokeStatic' | 'class'

      what_call = what + '()'
  }

  def "test newInstance() with signature and null class"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def args = [STRING, 'abc', 123, 'def', false, null]

    when:
      def result = reflection.newInstance(null, 'signature', *args)

    then:
      result == null

    and:
      0 * reflection.findConstructor(*_)
      0 * reflection.newInstance(_, { it instanceof Method }, _)
  }

  @Unroll
  def "test #what_call with method name and signature when method is not found"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def args = [STRING, 'abc', 123, 'def', false, null]

    when:
      reflection."$what"(obj_or_class, 'method', 'signature', *args)

    then:
      def e = thrown(Error)

    and:
      e.getMessage().startsWith(message)

    and:
      1 * reflection.findMethod(TREE_MAP_CLASS, 'method', 'signature') >> null
      0 * reflection."$what"(_, _, _)

    where:
      what           || obj_or_class   || message
      'invoke'       || TREE_MAP       || 'could not find method'
      'invokeStatic' || TREE_MAP_CLASS || 'could not find static method'

      what_call = what + '()'
  }

  def "test newInstance() with signature when method is not found"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def args = [STRING, 'abc', 123, 'def', false, null]

    when:
      reflection.newInstance(TREE_MAP_CLASS, 'signature', *args)

    then:
      def e = thrown(Error)

    and:
      e.getMessage().startsWith('could not find constructor')

    and:
      1 * reflection.findConstructor(TREE_MAP_CLASS, 'signature') >> null
      0 * reflection.newInstance(_, { it instanceof Method }, _)
  }

  def "test invokeAndReturnNullIfNotFound() with method name and signature"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def args = [STRING, 'abc', 123, 'def', false, null]

    when:
      def result = reflection.invokeAndReturnNullIfNotFound(TREE_MAP, 'method', 'signature', *args)

    then:
      result == FLOAT

    and:
      1 * reflection.findMethod(TREE_MAP_CLASS, 'method', 'signature') >> METHOD
      1 * reflection.invoke(TREE_MAP, METHOD, args) >> FLOAT
  }

  @Unroll
  def "test invokeAndReturnNullIfNotFound() with #with_what"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def args = [STRING, 'abc', 123, 'def', false, null]

    when:
      def result = reflection.invokeAndReturnNullIfNotFound(obj, 'method', 'signature', *args)

    then:
      result == null

    and:
      method_count * reflection.findMethod(TREE_MAP_CLASS, 'method', 'signature') >> method
      0 * reflection.invoke(_, _, _)

    where:
      with_what                                            || obj     || method_count | method
      'method name and signature and null object'          || null    || 0            | METHOD
      'method name and signature when method is not found' || TREE_MAP | 1            | null
  }

  def "test newInstance() with class name"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def args = [STRING, 'abc', 123, 'def', false, null]

    when:
      def result = reflection.newInstance(TREE_MAP_SIGNATURE, 'signature', *args)

    then:
      result == TREE_MAP

    and:
      1 * reflection.getClass(TREE_MAP_SIGNATURE) >> TREE_MAP_CLASS
      1 * reflection.newInstance(TREE_MAP_CLASS, 'signature', args) >> TREE_MAP
  }

  def "test newInstance() with null class name"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def args = [STRING, 'abc', 123, 'def', false, null]

    when:
      def result = reflection.newInstance((String) null, 'signature', *args)

    then:
      result == null

    and:
      0 * reflection.getClass(_)
      0 * reflection.newInstance({ it instanceof ClassType }, *_)
  }

  def "test enumValue()"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])
      def enumClassObject = MockClassObjectReference('enumClassObject')
      def enumClass = MockClassType('enumClass', 'Lsome/EnumClass', classObject: enumClassObject)
      def enumObject = MockObjectReference('enumObject', enumClass)

    when:
      def result = reflection.enumValue(enumClass, 'value')

    then:
      result == enumObject

    and:
      1 * reflection.invoke(enumClassObject, 'valueOf', '(Ljava/lang/String;)Lsome/EnumClass', 'value') >> enumObject
  }


  def "test enumValue() with a null type"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])

    when:
      def result = reflection.enumValue(null, 'value')

    then:
      result == null

    and:
      0 * reflection.invoke(*_)
  }

  def "test toString()"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])

    when:
      def result = reflection.toString(TREE_MAP)

    then:
      result == 'some string'

    and:
      1 * reflection.invoke(TREE_MAP, 'toString', '()Ljava/lang/String;') >> 'some string'
  }


  def "test toString() with null"() {
    given:
      def thread = Stub(ThreadReference)
      def reflection = Spy(ReflectionUtil, constructorArgs: [Stub(DebugContext), Stub(VirtualMachine), thread])

    when:
      def result = reflection.toString(null)

    then:
      result == null

    and:
      0 * reflection.invoke(*_)
  }

  @Unroll
  def "test toMirror() with #with_what creating a mirror"() {
    given:
      def thread = Stub(ThreadReference)
      def vm = Mock(VirtualMachine)
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, thread)

    when:
      def result = reflection.toMirror(obj)

    then:
      result == mirror

    and:
      1 * vm.mirrorOf(obj) >> mirror

    where:
      with_what   || obj         || mirror
      'a boolean' || true        || Stub(BooleanValue)
      'a byte'    || 2 as byte   || Stub(ByteValue)
      'a char'    || 'a' as char || Stub(CharValue)
      'a double'  || 2d          || Stub(DoubleValue)
      'a float'   || 2f          || Stub(FloatValue)
      'an int'    || 2           || Stub(IntegerValue)
      'a long'    || 2l          || Stub(LongValue)
      'a short'   || 2 as short  || Stub(ShortValue)
      'a string'  || 'string'    || Stub(StringReference)
  }

  @Unroll
  def "test toMirror() with #with_what returning it as is"() {
    given:
      def thread = Stub(ThreadReference)
      def vm = Mock(VirtualMachine)
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, thread)

    when:
      def result = reflection.toMirror(obj)

    then:
      result == obj

    and:
      0 * vm.mirrorOf(_)

    where:
      with_what || obj
      'null'    || null
      'a value' || Stub(ByteValue)
  }

  def "test toMirror() with a void"() {
    given:
      def thread = Stub(ThreadReference)
      def vm = Mock(VirtualMachine)
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, thread)
      def mirror = Mock(VoidValue)

    when:
      def result = reflection.toMirror(GroovyStub(Void))

    then:
      result == mirror

    and:
      1 * vm.mirrorOfVoid() >> mirror
  }

  def "test toMirror() failing with an unknown object"() {
    given:
      def thread = Stub(ThreadReference)
      def vm = Mock(VirtualMachine)
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, thread)

    when:
      reflection.toMirror([:])

    then:
      thrown(Error)

    and:
      0 * vm.mirrorOf(_)
  }

  @Unroll
  def "test fromMirror() with #with_what"() {
    given:
      def thread = Stub(ThreadReference)
      def vm = Mock(VirtualMachine)
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, thread)
      def mirror = type ? Mock(type) : null

    when:
      def result = reflection.fromMirror((Value) mirror)

    then:
      result == ((obj != 'mirror') ? obj : mirror)

    and:
      if (method) {
        1 * mirror."$method"() >> obj
      }

    where:
      with_what             || type            | method         || obj
      'a boolean value'     || BooleanValue    | 'booleanValue' || true
      'a byte value'        || ByteValue       | 'byteValue'    || 2 as byte
      'a char value'        || CharValue       | 'charValue'    || 'a' as char
      'a double value'      || DoubleValue     | 'doubleValue'  || 2d
      'a float value'       || FloatValue      | 'floatValue'   || 2f
      'an integer value'    || IntegerValue    | 'intValue'     || 2
      'a long value'        || LongValue       | 'longValue'    || 2l
      'a short value'       || ShortValue      | 'shortValue'   || 2 as short
      'a void value'        || VoidValue       | null           || null
      'a string reference'  || StringReference | 'value'        || 'string'
      'null'                || null            | null           || null
      'an object reference' || ObjectReference | null           || 'mirror'
  }

  def "test getType() with a void signature"() {
    given:
      def thread = Stub(ThreadReference)
      def vm = Mock(VirtualMachine)
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, thread)

    expect:
      reflection.getType('V') == Void.TYPE
  }

  @Unroll
  def "test fromMirror() with an array of #of_what"() {
    given:
      def thread = Stub(ThreadReference)
      def vm = Mock(VirtualMachine)
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, thread)
      def array = Mock(ArrayReference) {
        referenceType() >> Mock(ArrayType) {
          componentSignature() >> signature
        }
      }
      def obj_values = objs as List
      def values = Mock(List) {
        size() >> objs.length
        get(_) >> {
          def v = obj_values[it[0]]
          def mv = Mock(type) {
            "$method"() >> v
          }
          obj_values[it[0]] = null
          mv
        }
      }

    when:
      def result = reflection.fromMirror((Value) array)

    then:
      result.class.isArray()
      result.class.componentType == ctype
      result == objs

    and:
      1 * array.getValues() >> values

    where:
      of_what             || signature            | type            | method         || ctype   | objs
      'boolean values'    || 'Z'                  | BooleanValue    | 'booleanValue' || boolean | [true, false, true, true] as boolean[]
      'byte values'       || 'B'                  | ByteValue       | 'byteValue'    || byte    | [2, 4, 7, 3] as byte[]
      'char values'       || 'C'                  | CharValue       | 'charValue'    || char    | ['a', 'G', '7', 'S', 'x'] as char[]
      'double values'     || 'D'                  | DoubleValue     | 'doubleValue'  || double  | [22d] as double[]
      'float values'      || 'F'                  | FloatValue      | 'floatValue'   || float   | [2f, 3f, 8f] as float[]
      'integer values'    || 'I'                  | IntegerValue    | 'intValue'     || int     | [66, 12] as int[]
      'long values'       || 'J'                  | LongValue       | 'longValue'    || long    | [2l, 0l, 88l, 45l] as long[]
      'short values'      || 'S'                  | ShortValue      | 'shortValue'   || short   | [5, 8, 22, 123, 2, 2, 0] as short[]
      'string references' || 'Ljava/lang/String;' | StringReference | 'value'        || String  | ['string', 'd', 'abc'] as String[]
  }

  def "test fromMirror() with an array of object references"() {
    given:
      def thread = Stub(ThreadReference)
      def vm = Mock(VirtualMachine)
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, thread)
      def array = Mock(ArrayReference) {
        referenceType() >> Mock(ArrayType) {
          componentSignature() >> TREE_MAP_SIGNATURE
        }
      }

    expect:
      reflection.fromMirror((Value) array) == array
  }

  @Unroll
  def "test protect() when succeeding after #attempts attempts"() {
    given:
      def thread = Stub(ThreadReference)
      def vm = Mock(VirtualMachine)
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, thread)
      def exception = new ObjectCollectedException()
      def obj = Mock(ObjectReference)
      def count = new AtomicInteger()

    when:
      def result = reflection.protect({ obj })

    then:
      result == obj

    and:
      attempts * obj.disableCollection() >> {
        if (count.incrementAndGet() < attempts) {
          throw exception
        }
      }

    where:
      attempts << (1..(ReflectionUtil.SANE_TRY_LIMIT))
  }

  @Unroll
  def "test protect() when failing after #max attempts"() {
    given:
      def thread = Stub(ThreadReference)
      def vm = Mock(VirtualMachine)
      def reflection = new ReflectionUtil(Stub(DebugContext), vm, thread)
      def exception = new ObjectCollectedException()
      def obj = Mock(ObjectReference)
      def count = new AtomicInteger()

    when:
      reflection.protect({ obj })

    then:
      def e = thrown(ObjectCollectedException)

    and:
      e.is(exception)

    and:
      max * obj.disableCollection() >> {
        throw (count.incrementAndGet() == max) ? exception : new ObjectCollectedException()
      }

    where:
      max << [ReflectionUtil.SANE_TRY_LIMIT]
  }
}