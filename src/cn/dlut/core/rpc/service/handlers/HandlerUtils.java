/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package cn.dlut.core.rpc.service.handlers;


import java.util.Map;
import cn.dlut.exceptions.MissingRequiredField;

/**
 * Utility class that implements various checks
 * to validate API input.
 */
public final class HandlerUtils {

    /**
     * Implements no-op private constructor.
     * Needed for checkstyle.
     */
    private HandlerUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T fetchField(final String fieldName,
            final Map<String, Object> map,
            /* Class<T> type, */final boolean required, final T def)
                    throws ClassCastException, MissingRequiredField {
        final Object field = map.get(fieldName);
        if (field == null) {
            if (required) {
                throw new MissingRequiredField(fieldName);
            } else {
                return def;
            }
        }
        /*
         * if (field.getClass().isAssignableFrom()) return type.cast(field);
         */
        return (T) field;
        // throw new UnknownFieldType(fieldName, type.getName());

    }

}
