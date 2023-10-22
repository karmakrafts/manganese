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

package io.karma.ferrous.manganese.ocm.type;

import io.karma.ferrous.manganese.scope.Scoped;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.Arrays;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public interface Type extends Scoped {
    long materialize(final TargetMachine machine);

    TypeAttribute[] getAttributes();

    Type getBaseType();

    default boolean isAliased() {
        return false;
    }

    default boolean isBuiltin() {
        return getBaseType().isBuiltin();
    }

    default boolean isComplete() {
        return getBaseType().isComplete();
    }

    default Type derive(final TypeAttribute... attributes) {
        if (attributes.length == 0) {
            return this;
        }
        return Types.cached(new DerivedType(this, attributes));
    }

    default Type derivePointer(final int depth) {
        final var attribs = new TypeAttribute[depth];
        Arrays.fill(attribs, TypeAttribute.POINTER);
        return derive(attribs);
    }

    default Type deriveSlice(final int depth) {
        final var attribs = new TypeAttribute[depth];
        Arrays.fill(attribs, TypeAttribute.SLICE);
        return derive(attribs);
    }

    default Type deriveReference() {
        return derive(TypeAttribute.REFERENCE);
    }

    default boolean isReference() {
        final var attributes = getAttributes();
        if (attributes.length == 0) {
            return false;
        }
        return attributes[attributes.length - 1] == TypeAttribute.REFERENCE;
    }

    default boolean isPointer() {
        final var attributes = getAttributes();
        if (attributes.length == 0) {
            return false;
        }
        return attributes[attributes.length - 1] == TypeAttribute.POINTER;
    }

    default boolean isSlice() {
        final var attributes = getAttributes();
        if (attributes.length == 0) {
            return false;
        }
        return attributes[attributes.length - 1] == TypeAttribute.SLICE;
    }
}
