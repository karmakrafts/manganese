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

import io.karma.ferrous.manganese.ocm.BlockContext;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.llvm.LLVMCore;

/**
 * @author Alexander Hinze
 * @since 24/10/2023
 */
@API(status = Status.INTERNAL)
public record BoolConstant(boolean value, TokenSlice tokenSlice) implements Constant {
    public static final BoolConstant TRUE = new BoolConstant(true, TokenSlice.EMPTY);
    public static final BoolConstant FALSE = new BoolConstant(false, TokenSlice.EMPTY);

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public Type getType() {
        return BuiltinType.BOOL;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final BlockContext blockContext) {
        return LLVMCore.LLVMConstInt(getType().materialize(targetMachine), value ? 1 : 0, false);
    }
}
