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

import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.Scoped;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

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

    TokenSlice getTokenSlice();

    GenericParameter[] getGenericParams();

    default @Nullable GenericParameter getGenericParam(final String name) {
        final var params = getGenericParams();
        for (final GenericParameter param : params) {
            if (!param.getName().equals(name)) {
                continue;
            }
            return param;
        }
        return null;
    }

    default int getSize(final TargetMachine targetMachine) {
        return targetMachine.getTypeSize(materialize(targetMachine));
    }

    default int getAlignment(final TargetMachine targetMachine) {
        return targetMachine.getTypeAlignment(materialize(targetMachine));
    }

    /**
     * @param type The kind to check against.
     * @return True if the given kind may be assigned or
     * implicitly casted to this kind. False otherwise.
     */
    default boolean canAccept(final Type type) {
        return false;
    }

    /**
     * @return True if this kind is imaginary and cannot be materialized
     * into a runtime structure.
     */
    default boolean isImaginary() {
        return false;
    }

    /**
     * @return True if this kind is aliased and refers to another
     * kind or alias.
     */
    default boolean isAliased() {
        return false;
    }

    /**
     * @return True if this is a builtin kind.
     */
    default boolean isBuiltin() {
        return getBaseType().isBuiltin();
    }

    /**
     * @return True if this is a complete kind.
     * False if this kind is incomplete and missing and associated data layout.
     */
    default boolean isComplete() {
        return getBaseType().isComplete();
    }

    default Type deriveGeneric(final Expression... values) {
        final var type = new DerivedType(this);
        final var params = type.getGenericParams();
        final var numParams = params.length;
        if (values.length > numParams) {
            throw new IllegalArgumentException("Invalid number of values");
        }
        for (var i = 0; i < numParams; i++) {
            params[i].setValue(values[i]);
        }
        return Types.cached(type);
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
