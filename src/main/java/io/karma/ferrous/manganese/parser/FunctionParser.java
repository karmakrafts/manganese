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

package io.karma.ferrous.manganese.parser;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.ocm.statement.ReturnStatement;
import io.karma.ferrous.manganese.ocm.type.VoidType;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.vanadium.FerrousParser.FunctionBodyContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class FunctionParser extends ParseAdapter {
    private final Function function;

    public FunctionParser(final CompileContext compileContext, final Function function) {
        super(compileContext);
        this.function = function;
    }

    @Override
    public void enterFunctionBody(final FunctionBodyContext context) {
        if (function.getBody() != null) {
            super.enterFunctionBody(context);
            return;
        }
        final var scopeStack = function.rebuildScopeStack(); // Reconstruct scope stack from prototype
        final var type = function.getType();
        final var parser = new StatementParser(compileContext, type.getReturnType(), scopeStack, function);
        ParseTreeWalker.DEFAULT.walk(parser, context);
        final var statements = parser.getStatements();
        function.createBody(statements);
        for (final var statement : statements) {
            statement.setEnclosingScope(function.getBody());
        }
        super.enterFunctionBody(context);
    }

    @Override
    public void exitFunctionBody(final FunctionBodyContext context) {
        final var body = function.getBody();
        if (body == null) {
            super.exitFunctionBody(context);
            return;
        }
        final var statements = body.getStatements();
        if (statements.isEmpty() || !statements.getLast().terminatesScope()) {
            if (function.getType().getReturnType() == VoidType.INSTANCE) {
                statements.addLast(new ReturnStatement(TokenSlice.from(compileContext,
                    context))); // Implicitly return from void functions at the end of scope
            }
            else {
                if (statements.isEmpty()) {
                    compileContext.reportError(context.start, CompileErrorCode.E4005);
                }
                else {
                    compileContext.reportError(statements.getLast().getTokenSlice().getFirstToken(),
                        CompileErrorCode.E4005);
                }
            }
        }
        super.exitFunctionBody(context);
    }

    public Function getFunction() {
        return function;
    }
}
