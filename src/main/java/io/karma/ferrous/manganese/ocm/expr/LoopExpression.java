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
import io.karma.ferrous.manganese.ocm.scope.ScopeType;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.ocm.type.VoidType;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;

/**
 * @author Cedric Hammes, Cach30verfl0w
 * @since 25/12/2023
 */
@API(status = API.Status.INTERNAL)
public final class LoopExpression extends AbstractScopeExpression {

    public LoopExpression(final String scopeName, final TokenSlice tokenSlice) {
        super(scopeName, tokenSlice);
    }

    public LoopExpression(final TokenSlice tokenSlice) {
        super(tokenSlice);
    }

    // Expression

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        irContext.getCurrentOrCreate().br(this.scopeName.toInternalName());
        final var loopBuilder = irContext.getAndPush(this.scopeName.toInternalName());
        for (final var statement : this.statements) {
            statement.emit(targetMachine, irContext);
        }
        loopBuilder.br(this.scopeName.toInternalName());
        irContext.popCurrent();
        return 0L;
    }

    // Scope

    @Override
    public ScopeType getScopeType() {
        return ScopeType.LOOP;
    }

}
