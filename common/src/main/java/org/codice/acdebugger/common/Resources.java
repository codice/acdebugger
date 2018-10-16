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
package org.codice.acdebugger.common;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/** Utility classes for managing resources. */
public class Resources {
  private Resources() {
    throw new UnsupportedOperationException();
  }

  /**
   * Reads a list of lines from the specified resource relative to the given class.
   *
   * @param clazz the class relative to where the resource should be loaded
   * @param name the name of the resource to load
   * @return an unmodifiable list of all trimmed lines read from the resource
   * @throws IOError if an I/O exception occurs
   */
  public static List<String> readLines(Class<?> clazz, String name) {
    return Resources.readLines(clazz, name, Function.identity());
  }

  /**
   * Reads a list of lines from the specified resource relative to the given class.
   *
   * @param clazz the class relative to where the resource should be loaded
   * @param name the name of the resource to load
   * @param transformer a function to convert each trimmed lines into the required object
   * @return an unmodifiable list of all transformed lines read from the resource
   * @throws IOError if an I/O exception occurs
   */
  public static <T> List<T> readLines(
      Class<?> clazz, String name, Function<String, T> transformer) {
    final List<T> lines = new ArrayList<>();

    try (final InputStream is = clazz.getResourceAsStream('/' + name);
        final Reader r = new InputStreamReader(is);
        final BufferedReader br = new BufferedReader(r)) {
      String line;

      while ((line = br.readLine()) != null) {
        final String trimmed = line.trim();

        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
          lines.add(transformer.apply(trimmed));
        }
      }
      return Collections.unmodifiableList(lines);
    } catch (IOException e) {
      throw new IOError(e);
    }
  }
}
