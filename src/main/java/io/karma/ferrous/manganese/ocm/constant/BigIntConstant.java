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
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

import static org.lwjgl.llvm.LLVMCore.LLVMConstIntOfStringAndSize;

/**
 * @author Alexander Hinze
 * @since 02/12/2023
 */
@API(status = API.Status.INTERNAL)
public final class BigIntConstant implements Constant {
    private final Type type;
    private final BigInteger value;
    private final TokenSlice tokenSlice;
    private Scope enclosingScope;

    public BigIntConstant(final Type type, final BigInteger value, final TokenSlice tokenSlice) {
        this.type = type;
        this.value = value;
        this.tokenSlice = tokenSlice;
    }

    public BigInteger getValue() {
        return value;
    }

    @Override
    public Type getType(final TargetMachine targetMachine) {
        return type;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        final var typeAddress = this.type.materialize(targetMachine);
        return LLVMConstIntOfStringAndSize(typeAddress, value.toString(10), (byte) 10);
    }

    @Override
    public @Nullable Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }
}
