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

package io.karma.ferrous.manganese.ocm.expr;

import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 10/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class CastExpression implements Expression {
    private final Type castType;
    private final Expression value;
    private final boolean isImplicit;
    private final TokenSlice tokenSlice;
    private Scope enclosingScope;

    public CastExpression(final Type castType, final Expression value, final boolean isImplicit,
                          final TokenSlice tokenSlice) {
        this.castType = castType;
        this.value = value;
        this.isImplicit = isImplicit;
        this.tokenSlice = tokenSlice;
    }

    public boolean isImplicit() {
        return isImplicit;
    }

    // Scoped

    @Override
    public @Nullable Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    // Expression

    @Override
    public Type getType(final TargetMachine targetMachine) {
        return castType;
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        return 0;
    }
}
