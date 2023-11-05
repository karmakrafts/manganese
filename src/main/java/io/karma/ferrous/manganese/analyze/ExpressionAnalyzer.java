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
import io.karma.ferrous.manganese.ocm.constant.BoolConstant;
import io.karma.ferrous.manganese.ocm.constant.IntConstant;
import io.karma.ferrous.manganese.ocm.constant.NullConstant;
import io.karma.ferrous.manganese.ocm.constant.RealConstant;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.vanadium.FerrousParser.*;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 05/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class ExpressionAnalyzer extends ParseAdapter {
    private Expression expression;

    public ExpressionAnalyzer(final Compiler compiler, final CompileContext compileContext) {
        super(compiler, compileContext);
    }

    private static IntConstant parseIntConstant(final BuiltinType type, final TerminalNode node) {
        final var suffix = type.getName().toString();
        var text = node.getText();
        if (text.endsWith(suffix)) {
            text = text.substring(0, text.length() - suffix.length());
        }
        return new IntConstant(type, Long.parseLong(text));
    }

    private static RealConstant parseRealConstant(final BuiltinType type, final TerminalNode node) {
        final var suffix = type.getName().toString();
        var text = node.getText();
        if (text.endsWith(suffix)) {
            text = text.substring(0, text.length() - suffix.length());
        }
        return new RealConstant(type, Double.parseDouble(text));
    }

    @Override
    public void enterSintLiteral(final SintLiteralContext context) {
        if (expression != null) {
            return;
        }
        var node = context.LITERAL_I8();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.I8, node);
            return;
        }
        node = context.LITERAL_I16();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.I16, node);
            return;
        }
        node = context.LITERAL_I32();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.I32, node);
            return;
        }
        node = context.LITERAL_I64();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.I64, node);
            return;
        }
        node = context.LITERAL_ISIZE();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.ISIZE, node);
        }
        super.enterSintLiteral(context);
    }

    @Override
    public void enterUintLiteral(final UintLiteralContext context) {
        if (expression != null) {
            return;
        }
        var node = context.LITERAL_U8();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.U8, node);
            return;
        }
        node = context.LITERAL_U16();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.U16, node);
            return;
        }
        node = context.LITERAL_U32();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.U32, node);
            return;
        }
        node = context.LITERAL_U64();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.U64, node);
            return;
        }
        node = context.LITERAL_USIZE();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.USIZE, node);
        }
        super.enterUintLiteral(context);
    }

    @Override
    public void enterFloatLiteral(final FloatLiteralContext context) {
        if (expression != null) {
            return;
        }
        var node = context.LITERAL_F32();
        if (node != null) {
            expression = parseRealConstant(BuiltinType.F32, node);
            return;
        }
        node = context.LITERAL_F64();
        if (node != null) {
            expression = parseRealConstant(BuiltinType.F64, node);
        }
        super.enterFloatLiteral(context);
    }

    @Override
    public void enterBoolLiteral(final BoolLiteralContext context) {
        if (expression != null) {
            return;
        }
        expression = new BoolConstant(context.KW_TRUE() != null);
        super.enterBoolLiteral(context);
    }

    @Override
    public void enterLiteral(final LiteralContext context) {
        if (expression != null) {
            return;
        }
        if (context.KW_NULL() != null) {
            expression = NullConstant.INSTANCE;
        }
        super.enterLiteral(context);
    }

    public @Nullable Expression getExpression() {
        return expression;
    }
}
