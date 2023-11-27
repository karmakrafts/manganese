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

import io.karma.ferrous.manganese.ocm.constant.NullConstant;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class DerivedType implements Type {
    private final Type baseType;
    private final TypeAttribute attribute;
    private final TokenSlice tokenSlice;
    private long materializedType = MemoryUtil.NULL;

    DerivedType(final Type baseType, TypeAttribute attribute) {
        this.baseType = baseType;
        this.attribute = attribute;
        tokenSlice = baseType.getTokenSlice();
    }

    // NameProvider

    @Override
    public Identifier getName() {
        return baseType.getName();
    }

    // Scoped

    @Override
    public Scope getEnclosingScope() {
        return baseType.getEnclosingScope();
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
    }

    // Type

    @Override
    public boolean isDerived() {
        return true;
    }

    @Override
    public String getMangledName() {
        return String.format("%s%s", baseType.getMangledName(), attribute.getText());
    }

    @Override
    public boolean canAccept(final Type type) {
        if (type instanceof DerivedType derivedType) {
            return baseType == derivedType.baseType && getAttributes().equals(derivedType.getAttributes());
        }
        return Type.super.canAccept(type);
    }

    @Override
    public Expression makeDefaultValue() {
        if (isPointer()) {
            final var value = new NullConstant(TokenSlice.EMPTY);
            value.setContextualType(baseType);
            return value;
        }
        return baseType.makeDefaultValue();
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public List<GenericParameter> getGenericParams() {
        return baseType.getGenericParams();
    }

    @Override
    public Type getBaseType() {
        return baseType;
    }

    @Override
    public long materialize(final TargetMachine machine) {
        if (materializedType != MemoryUtil.NULL) {
            return materializedType;
        }
        return materializedType = LLVMCore.LLVMPointerType(baseType.materialize(machine), 0);
    }

    @Override
    public List<TypeAttribute> getAttributes() {
        final var attribs = new ArrayList<>(baseType.getAttributes());
        attribs.add(attribute);
        return attribs;
    }

    // Object

    @Override
    public int hashCode() {
        return Objects.hash(baseType, attribute);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof DerivedType type) { // @formatter:off
            return baseType.equals(type.baseType)
                && attribute == type.attribute;
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s%s", attribute.getText(), baseType.toString());
    }
}
