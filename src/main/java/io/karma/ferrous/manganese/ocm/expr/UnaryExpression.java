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

import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.value.Value;
import io.karma.ferrous.manganese.util.Operator;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 22/10/2023
 */
public final class UnaryExpression implements Expression {
    private final Operator op;
    private final Value value;

    public UnaryExpression(final Operator op, final Value value) {
        if (!op.isUnary()) {
            throw new IllegalArgumentException(String.format("%s is not a unary operator", op));
        }
        this.op = op;
        this.value = value;
    }

    public Operator getOp() {
        return op;
    }

    public Value getValue() {
        return value;
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UnaryExpression expr) {
            return op == expr.op && value.equals(expr.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s%s", op, value);
    }
}
