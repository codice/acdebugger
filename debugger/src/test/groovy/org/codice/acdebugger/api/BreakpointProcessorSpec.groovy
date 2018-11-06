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

import spock.lang.Specification
import spock.lang.Unroll

class BreakpointProcessorSpec extends Specification {
  static def SIGNATURE = 'Lsome/class;'
  static def METHOD = 'someMethod'
  static def LINE_NUM = 1234

  @Unroll
  def "test createLocationFor() with #with_what"() {
    when:
      def location = BreakpointProcessor.createLocationFor(*args)

    then:
      location.classSignature == signature
      location.method == method
      location.lineNumber == lineNum

    where:
      with_what                       || args                  || signature | method | lineNum
      'just a signature'              || [SIGNATURE]           || SIGNATURE | null   | -1
      'a signature and a method'      || [SIGNATURE, METHOD]   || SIGNATURE | METHOD | -1
      'a signature and a line number' || [SIGNATURE, LINE_NUM] || SIGNATURE | null   | LINE_NUM
  }
}
