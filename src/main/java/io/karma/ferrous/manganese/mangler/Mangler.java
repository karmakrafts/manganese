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

package io.karma.ferrous.manganese.mangler;

import io.karma.ferrous.manganese.ocm.type.Type;
import org.apiguardian.api.API;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Alexander Hinze
 * @since 27/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class Mangler {
    private static final Pattern SEQUENCE_PATTERN = Pattern.compile("['@]");

    // @formatter:off
    private Mangler() {}
    // @formatter:on

    public static String mangleSequence(final Collection<? extends Type> types) throws ManglerException {
        final var builder = new StringBuilder();
        for (final var type : types) {
            if (!type.isComplete()) {
                throw new ManglerException(STR."Type \{type} could not be mangled as it is incomplete");
            }
            builder.append(type.getMangledSequencePrefix()).append(type.getMangledName());
        }
        return builder.toString();
    }

    public static String mangleSequence(final Type... types) throws ManglerException {
        return mangleSequence(Arrays.asList(types));
    }

    public static List<Type> demangleSequence(final String sequence) throws ManglerException {
        final var chunks = SEQUENCE_PATTERN.split(sequence);
        if (chunks.length == 0) {
            return Collections.emptyList();
        }
        final var types = new ArrayList<Type>();
        for (final var chunk : chunks) {
            if (chunk.startsWith("'")) {

            }
            else if (chunk.startsWith("@")) {

            }
        }
        return types;
    }
}
