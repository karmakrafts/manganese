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

import io.karma.ferrous.manganese.ocm.BlockBuilder;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.NullType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.TypeAttribute;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.lwjgl.llvm.LLVMCore;

/**
 * @author Alexander Hinze
 * @since 24/10/2023
 */
public final class NullConstant implements Constant {
    public static final NullConstant INSTANCE = new NullConstant();

    // @formatter:off
    private NullConstant() {}
    // @formatter:on

    @Override
    public Type getType() {
        return NullType.INSTANCE;
    }

    @Override
    public long materialize(final TargetMachine targetMachine, final BlockBuilder builder) {
        return LLVMCore.LLVMConstPointerNull(BuiltinType.VOID.derive(TypeAttribute.POINTER).materialize(targetMachine));
    }

    @Override
    public void emit(final TargetMachine targetMachine, final BlockBuilder builder) {

    }
}
