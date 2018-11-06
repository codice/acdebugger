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
package org.codice.acdebugger.common

import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import spock.lang.Specification
import spock.lang.Unroll

class JsonUtilsSpec extends Specification {
  static def EXCEPTION = new RuntimeException('testing')
  static def ERROR = new Error('testing')

  @Unroll
  def "test simple conversion to Json with #with_what"() {
    expect:
      JsonUtils.toJson(object) == json

    where:
      with_what       || object               || json
      'simple string' || 'abcd'               || '"abcd"'
      'simple number' || 1234                 || '1234'
      'simple list'   || ['abcd', 1234]       || '["abcd",1234]'
      'simple map'    || [a: 'abcd', b: 1234] || '{"a":"abcd","b":1234}'
      'null'          || null                 || 'null'
  }

  @Unroll
  def "test simple conversion from Json with class #with_what"() {
    expect:
      JsonUtils.fromJson(json, clazz) == object

    where:
      with_what       || json                    | clazz   || object
      'simple string' || '"abcd"'                | String  || 'abcd'
      'simple number' || '1234'                  | Integer || 1234
      'simple list'   || '["abcd",1234]'         | List    || ['abcd', 1234]
      'simple map'    || '{"a":"abcd","b":1234}' | Map     || [a: 'abcd', b: 1234]
      'null'          || 'null'                  | Object  || null
  }

  @Unroll
  def "test simple conversion from Json with type #with_what"() {
    expect:
      JsonUtils.fromJson(json, type) == object

    where:
      with_what                  || json                      | type || object
      'simple string'            || '"abcd"'                  | new TypeToken<String>() {
      }.type                                                         || 'abcd'
      'simple number'            || '1234'                    | new TypeToken<Integer>() {
      }.type                                                         || 1234
      'list of strings'          || '["abcd","1234"]'         | new TypeToken<List<String>>() {
      }.type                                                         || ['abcd', '1234']
      'map of enums and strings' || '{"A":"abcd","B":"1234"}' | new TypeToken<Map<TestEnum, String>>() {
      }.type                                                         || [(TestEnum.A): 'abcd', (TestEnum.B): '1234']
      'null'                     || 'null'                    | new TypeToken<Object>() {
      }.type                                                         || null
  }

  @Unroll
  def "test invalid conversion from Json with #exception.simpleName"() {
    when:
      JsonUtils.fromJson(json, clazz)

    then:
      thrown(exception)

    where:
      json                | clazz     || exception
      '{3'                | Object    || JsonSyntaxException
      '{"m":{"C":"123"}}' | TestClass || NullPointerException
  }
}

enum TestEnum {
  A, B
}

class TestClass {
  Hashtable<TestEnum, String> m;
}
