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
import io.karma.ferrous.manganese.ocm.scope.DefaultScope;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import org.apiguardian.api.API;

/**
 * @author Alexander Hinze
 * @since 22/10/2023
 */
@API(status = API.Status.INTERNAL)
public final class NullType implements Type {
    public static final NullType INSTANCE = new NullType();

    // @formatter:off
    private NullType() {}
    // @formatter:on

    // NameProvider

    @Override
    public Identifier getName() {
        return new Identifier(TokenUtils.getLiteral(FerrousLexer.KW_NULL));
    }

    // Scoped

    @Override
    public Scope getEnclosingScope() {
        return DefaultScope.GLOBAL;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
    }

    // Type

    @Override
    public Expression makeDefaultValue(final TargetMachine targetMachine) {
        return new NullConstant(TokenSlice.EMPTY);
    }

    @Override
    public long materialize(final TargetMachine machine) {
        return VoidType.INSTANCE.asPtr().materialize(machine);
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    // Object

    @Override
    public String toString() {
        return "null";
    }
}
