/*
 * Copyright 2023 Karma Krafts & associates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.karma.ferrous.manganese.util;

import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.manganese.type.Type;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import io.karma.ferrous.vanadium.FerrousParser.TypeContext;

import java.util.List;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public final class TypeUtils {
    // @formatter:off
    private TypeUtils() {}
    // @formatter:on

    public static List<Type> getParameterTypes(final Compiler compiler, final ProtoFunctionContext context) {
        // @formatter:off
        return context.functionParamList().children.stream()
            .filter(tok -> tok instanceof TypeContext)
            .map(tok -> Type.findType(compiler, (TypeContext) tok).orElseThrow())
            .toList();
        // @formatter:on
    }
}
