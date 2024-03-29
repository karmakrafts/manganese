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

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.parser.TypeParser;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.vanadium.FerrousParser;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
@API(status = Status.INTERNAL)
public final class Types {
    private static final HashMap<String, Type> CACHE = new HashMap<>();

    // @formatter:off
    private Types() {}
    // @formatter:on

    public static void invalidateCache() {
        CACHE.clear();
    }

    @SuppressWarnings("unchecked")
    static <T extends Type> T cached(final T type) {
        final var key = type.toString();
        final var result = CACHE.get(key);
        if (result != null) {
            return (T) result;
        }
        CACHE.put(key, type);
        return type;
    }

    public static IntType integer(final int width, final boolean isUnsigned,
                                  final Function<IntType, IntType> callback) {
        return cached(callback.apply(new IntType(width, isUnsigned)));
    }

    public static FunctionType function(final Type returnType, final List<Type> paramTypes, final boolean isVarArg,
                                        final Function<FunctionType, FunctionType> callback,
                                        final TokenSlice tokenSlice) {
        return cached(callback.apply(new FunctionType(returnType, isVarArg, tokenSlice, paramTypes)));
    }

    public static NamedFunctionType namedFunction(final Identifier name, final Type returnType,
                                                  final List<Type> paramTypes, final boolean isVarArg,
                                                  final Function<NamedFunctionType, NamedFunctionType> callback,
                                                  final TokenSlice tokenSlice) {
        return cached(callback.apply(new NamedFunctionType(name, returnType, isVarArg, tokenSlice, paramTypes)));
    }

    public static StructureType structure(final Identifier name, final boolean isPacked,
                                          final Function<StructureType, StructureType> callback,
                                          final List<GenericParameter> genericParams, final TokenSlice tokenSlice,
                                          final List<Type> fieldTypes) {
        return cached(callback.apply(new StructureType(name, isPacked, genericParams, tokenSlice, fieldTypes)));
    }

    public static StructureType structure(final Identifier name, final Function<StructureType, StructureType> callback,
                                          final List<GenericParameter> genericParams, final TokenSlice tokenSlice,
                                          final List<Type> fieldTypes) {
        return structure(name, false, callback, genericParams, tokenSlice, fieldTypes);
    }

    public static AliasedType aliased(final Identifier name, final Type backingType,
                                      final Function<AliasedType, AliasedType> callback, final TokenSlice tokenSlice,
                                      final List<GenericParameter> genericParams) {
        return cached(callback.apply(new AliasedType(name, backingType, tokenSlice, genericParams)));
    }

    public static TupleType tuple(final Function<TupleType, TupleType> callback, final TokenSlice tokenSlice,
                                  final List<Type> types) {
        return cached(callback.apply(new TupleType(tokenSlice, types)));
    }

    public static VectorType vector(final Type type, final int elementCount,
                                    final Function<VectorType, VectorType> callback, final TokenSlice tokenSlice,
                                    final List<GenericParameter> genericParameters) {
        return cached(callback.apply(new VectorType(type, elementCount, tokenSlice, genericParameters)));
    }

    public static IncompleteType incomplete(final Identifier name,
                                            final Function<IncompleteType, IncompleteType> callback,
                                            final TokenSlice tokenSlice) {
        return cached(callback.apply(new IncompleteType(name, tokenSlice)));
    }

    public static @Nullable Type findCommonType(final TargetMachine targetMachine, final Type... types) {
        return findCommonType(targetMachine, Arrays.asList(types));
    }

    public static @Nullable Type findCommonType(final TargetMachine targetMachine, final List<Type> types) {
        final var numTypes = types.size();
        return switch(numTypes) { // @formatter:off
            case 0  -> null;
            case 1  -> types.getFirst();
            default -> {
                Type result = null;
                for(var i = 0; i < numTypes; i++) outer: {
                    final var baseType = types.get(i);
                    for(var j = 0; j < numTypes; j++) {
                        if(i == j) {
                            continue; // If we are at the same index, skip this iteration
                        }
                        final var type = types.get(j);
                        if(!baseType.canAccept(targetMachine, type)) {
                            result = null; // Reset result if kind was not compatible
                            continue;
                        }
                        result = baseType;
                        break outer;
                    }
                }
                yield result;
            }
        }; // @formatter:on
    }

    public static List<Type> parse(final CompileContext compileContext, final ScopeStack scopeStack,
                                   final @Nullable FerrousParser.TypeListContext context) {
        if (context == null) {
            return Collections.emptyList();
        }
        // @formatter:off
        return context.type().stream()
            .map(ctx -> parse(compileContext, scopeStack, ctx))
            .filter(Objects::nonNull)
            .toList();
        // @formatter:on
    }

    public static @Nullable Type parse(final CompileContext compileContext, final ScopeStack scopeStack,
                                       final @Nullable FerrousParser.TypeContext context) {
        if (context == null) {
            return null;
        }
        final TypeParser parser = new TypeParser(compileContext, scopeStack);
        ParseTreeWalker.DEFAULT.walk(parser, context);
        return parser.getType();
    }
}
