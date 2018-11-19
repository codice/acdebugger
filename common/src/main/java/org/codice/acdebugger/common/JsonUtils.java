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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;

/** Provides useful functions for dealing with Json. */
public class JsonUtils {
  private JsonUtils() {
    throw new UnsupportedOperationException();
  }

  private static final Gson GSON =
      new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

  public static String toJson(Object value) {
    return JsonUtils.GSON.toJson(value);
  }

  public static <T> T fromJson(String json, Class<T> clazz) {
    return JsonUtils.GSON.fromJson(json, clazz);
  }

  public static <T> T fromJson(String json, Type type) {
    return JsonUtils.GSON.fromJson(json, type);
  }
}
