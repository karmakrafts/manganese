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

package io.karma.ferrous.manganese.ocm.generic;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.vanadium.FerrousParser.GenericExprContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * @author Alexander Hinze
 * @since 29/10/2023
 */
@FunctionalInterface
public interface GenericConstraint extends Predicate<Type> {
    GenericConstraint TRUE = type -> true;
    GenericConstraint FALSE = type -> false;

    static GenericConstraint parse(final CompileContext compileContext, final GenericExprContext context) {
        return GenericConstraint.TRUE; // TODO: implement this
    }

    default @NotNull GenericConstraint or(final GenericConstraint other) {
        return type -> test(type) || other.test(type);
    }

    default @NotNull GenericConstraint and(final GenericConstraint other) {
        return type -> test(type) && other.test(type);
    }

    default @NotNull GenericConstraint negate() {
        return type -> !test(type);
    }
}
