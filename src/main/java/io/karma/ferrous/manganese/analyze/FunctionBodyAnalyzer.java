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

package io.karma.ferrous.manganese.analyze;

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.statement.ReturnStatement;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.util.ExpressionUtils;
import io.karma.ferrous.vanadium.FerrousParser.ReturnStatementContext;
import org.apiguardian.api.API;

import java.util.ArrayList;

/**
 * @author Alexander Hinze
 * @since 05/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class FunctionBodyAnalyzer extends ParseAdapter {
    private final ArrayList<Statement> statements = new ArrayList<>();

    public FunctionBodyAnalyzer(final Compiler compiler, final CompileContext compileContext) {
        super(compiler, compileContext);
    }

    @Override
    public void enterReturnStatement(final ReturnStatementContext context) {
        final var exprContext = context.expr();
        if (exprContext != null) {
            final var expr = ExpressionUtils.parseExpression(compiler, compileContext, exprContext);
            if (expr == null) {
                return;
            }
            statements.add(new ReturnStatement(expr));
            super.enterReturnStatement(context);
            return;
        }
        statements.add(new ReturnStatement());
        super.enterReturnStatement(context);
    }

    public ArrayList<Statement> getStatements() {
        return statements;
    }
}
