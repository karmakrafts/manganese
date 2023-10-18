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

import io.karma.ferrous.manganese.ocm.scope.EnclosingScopeProvider;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.scope.ScopeType;
import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.INTERNAL)
public class FunctionType implements Type {
    private final Type returnType;
    private final Type[] paramTypes;
    private final boolean isVarArg;
    private long materializedType = MemoryUtil.NULL;

    FunctionType(final Type returnType, final boolean isVarArg, final Type... paramTypes) {
        this.returnType = returnType;
        this.paramTypes = paramTypes;
        this.isVarArg = isVarArg;
    }

    @Override
    public ScopeType getScopeType() {
        return ScopeType.FUNCTION;
    }

    @Override
    public EnclosingScopeProvider getEnclosingScope() {
        return Scope.GLOBAL;
    }

    @Override
    public void setEnclosingScope(final EnclosingScopeProvider scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Identifier getName() {
        return new Identifier(toString()); // TODO: find a better solution for this..
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public boolean isComplete() {
        return false; // TOOD: ...
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    @Override
    public long materialize(final Target target) {
        if (materializedType != MemoryUtil.NULL) {
            return materializedType;
        }
        try (final var stack = MemoryStack.stackPush()) {
            final var returnType = this.returnType.materialize(target);
            final var paramTypes = Arrays.stream(this.paramTypes).mapToLong(type -> type.materialize(target)).toArray();
            return materializedType = LLVMCore.LLVMFunctionType(returnType, stack.pointers(paramTypes), isVarArg);
        }
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return new TypeAttribute[0];
    }

    public String toString(final @Nullable String name) {
        final var builder = new StringBuilder();
        builder.append(returnType);

        if (name != null) {
            builder.append(' ');
            builder.append(name);
        }

        builder.append('(');
        final var numParams = paramTypes.length;
        for (var i = 0; i < numParams; i++) {
            builder.append(paramTypes[i]);
            if (i < numParams - 1) {
                builder.append(", ");
            }
        }

        if (isVarArg) {
            builder.append(", ...");
        }
        builder.append(')');
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, Arrays.hashCode(paramTypes), isVarArg);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof FunctionType type) { // @formatter:off
            return returnType.equals(type.returnType)
                && Arrays.equals(paramTypes, type.paramTypes)
                && isVarArg == type.isVarArg;
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        return toString(null);
    }
}
