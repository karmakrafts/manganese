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

package io.karma.ferrous.manganese.ocm.constant;

import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.NullType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import static org.lwjgl.llvm.LLVMCore.LLVMConstNull;

/**
 * @author Alexander Hinze
 * @since 24/10/2023
 */
@API(status = API.Status.INTERNAL)
public final class NullConstant implements Constant {
    private final TokenSlice tokenSlice;
    private Scope enclosingScope;
    private Type contextualType;

    public NullConstant(final TokenSlice tokenSlice) {
        this.tokenSlice = tokenSlice;
    }

    public Type getContextualType() {
        return contextualType;
    }

    public void setContextualType(final Type contextualType) {
        this.contextualType = contextualType;
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

    // Expressions

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public Type getType(final TargetMachine targetMachine) {
        return NullType.INSTANCE;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        return LLVMConstNull(contextualType.materialize(targetMachine));
    }
}
