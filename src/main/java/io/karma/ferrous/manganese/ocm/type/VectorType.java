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

import io.karma.ferrous.manganese.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.lwjgl.llvm.LLVMCore;

import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 21/10/2023
 */
public final class VectorType implements Type {
    private final Type type;
    private final int elementCount;
    private long materializedType = NULL;
    private Scope enclosingScope;

    public VectorType(final Type type, final int elementCount) {
        this.type = type;
        this.elementCount = elementCount;
    }

    public Type getType() {
        return type;
    }

    public int getElementCount() {
        return elementCount;
    }

    @Override
    public void setEnclosingScope(Scope scope) {
        enclosingScope = scope;
    }

    @Override
    public long materialize(final TargetMachine machine) {
        if (materializedType == NULL) {
            materializedType = LLVMCore.LLVMVectorType(type.materialize(machine), elementCount);
        }
        return materializedType;
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return new TypeAttribute[0];
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, elementCount, enclosingScope);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof VectorType vecType) {
            return type.equals(vecType.type)
                && elementCount == vecType.elementCount
                && (enclosingScope == null || enclosingScope.equals(vecType.enclosingScope));
        }
        return false;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
