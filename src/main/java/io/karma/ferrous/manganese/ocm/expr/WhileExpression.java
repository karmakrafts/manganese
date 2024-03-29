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
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.ocm.statement.YieldStatement;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.VoidType;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Cedric Hammes, Cach30verfl0w
 * @since 23/12/2023
 */
@API(status = API.Status.INTERNAL)
public final class WhileExpression implements Expression {

    private final List<Statement> body;
    private final Expression condition;
    private final TokenSlice tokenSlice;
    private final Identifier loopLabel;
    private final boolean isDoWhile;
    private Scope enclosingScope;

    public WhileExpression(final List<Statement> body, final Expression condition, final TokenSlice tokenSlice,
                           final @Nullable Identifier loopLabel, final boolean doWhile) {
        this.body = body;
        this.condition = condition;
        this.tokenSlice = tokenSlice;
        this.isDoWhile = doWhile;
        this.loopLabel = loopLabel != null ? loopLabel : new Identifier(STR."loop\{UUID.randomUUID()}");
    }

    // Scoped

    @Override
    public @Nullable Scope getEnclosingScope() {
        return this.enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    // Expression

    @Override
    public TokenSlice getTokenSlice() {
        return this.tokenSlice;
    }

    @Override
    public Type getType(final TargetMachine targetMachine) {
        for (final var statement : this.body) {
            if (statement instanceof YieldStatement yieldStatement) {
                return yieldStatement.getValue().getType(targetMachine);
            }
        }
        return VoidType.INSTANCE;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        final var labelName = this.loopLabel.toInternalName();
        final var mainBuilder = irContext.getCurrentOrCreate();

        // Create jump trampoline
        final var trampolineBuilder = irContext.getAndPush(STR."trampoline_\{labelName}");
        trampolineBuilder.condBr(this.condition.emit(targetMachine, irContext), labelName, STR."end_\{labelName}");
        irContext.popCurrent();

        // Conditional jump if not do while, otherwise jump
        if (this.isDoWhile) {
            mainBuilder.br(labelName);
        } else {
            mainBuilder.br(STR."trampoline_\{labelName}");
        }

        // Create loop block
        final var loopBlockBuilder = irContext.getAndPush(labelName);
        for (final var statement : this.body) {
            statement.emit(targetMachine, irContext);
        }
        loopBlockBuilder.br(STR."trampoline_\{labelName}");
        irContext.popCurrent();

        irContext.getAndPush(STR."end_\{labelName}");
        return NULL; // Return value ref to result register
    }

}
