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

import io.karma.ferrous.manganese.ocm.expr.AllocExpression;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;

import java.util.List;
import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 21/10/2023
 */
@API(status = API.Status.INTERNAL)
public final class TupleType implements Type {
    private final TokenSlice tokenSlice;
    private final List<Type> types;
    private Scope enclosingScope;

    public TupleType(final TokenSlice tokenSlice, final List<Type> types) {
        this.tokenSlice = tokenSlice;
        this.types = types;
    }

    public List<Type> getTypes() {
        return types;
    }

    // Scoped

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope scope) {
        enclosingScope = scope;
    }

    // Type

    @Override
    public Identifier getName() {
        return Identifier.EMPTY; // TODO: fix this
    }

    @Override
    public Expression makeDefaultValue(final TargetMachine targetMachine) {
        return new AllocExpression(this, false, TokenSlice.EMPTY);
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public long materialize(final TargetMachine machine) {
        return NULL;
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    // Object

    @Override
    public int hashCode() {
        return Objects.hash(types, enclosingScope);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof TupleType type) { // @formatter:off
            return types.equals(type.types)
                && Objects.equals(enclosingScope, type.enclosingScope);
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        final var buffer = new StringBuilder();
        buffer.append('(');
        final var numTypes = types.size();
        for (var i = 0; i < numTypes; i++) {
            buffer.append(types.get(i));
            if (i < numTypes - 1) {
                buffer.append(", ");
            }
        }
        buffer.append(')');
        return buffer.toString();
    }
}
