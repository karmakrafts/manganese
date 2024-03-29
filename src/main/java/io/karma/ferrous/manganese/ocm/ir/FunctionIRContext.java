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

package io.karma.ferrous.manganese.ocm.ir;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.ocm.function.ParameterStorage;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.llvm.LLVMCore;

import java.util.HashMap;
import java.util.Stack;

import static org.lwjgl.llvm.LLVMCore.LLVMAppendBasicBlockInContext;
import static org.lwjgl.llvm.LLVMCore.LLVMDeleteBasicBlock;

/**
 * @author Alexander Hinze
 * @since 10/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class FunctionIRContext implements IRContext {
    private final CompileContext compileContext;
    private final Module module;
    private final TargetMachine targetMachine;
    private final Function function;
    private final HashMap<String, IRBuilder> builders = new HashMap<>();
    private final Stack<IRBuilder> builderStack = new Stack<>();
    private boolean isDropped;

    public FunctionIRContext(final CompileContext compileContext, final Module module,
                             final TargetMachine targetMachine, final Function function) {
        this.compileContext = compileContext;
        this.module = module;
        this.targetMachine = targetMachine;
        this.function = function;
    }

    public void dispose() {
        builders.values().forEach(IRBuilder::dispose);
    }

    @Override
    public void close() {
        dispose();
    }

    @Override
    public @Nullable ParameterStorage getParameter(final Identifier name) {
        return function.getParamStorage(name);
    }

    @Override
    public boolean isParameter(final long value) {
        final var params = function.getParameters();
        final var numParams = params.size();
        final var address = function.materialize(module, targetMachine);
        for (var i = 0; i < numParams; i++) {
            final var param = LLVMCore.LLVMGetParam(address, i);
            if (param != value) {
                continue;
            }
            return true;
        }
        return false;
    }

    @Override
    public CompileContext getCompileContext() {
        return compileContext;
    }

    @Override
    public Module getModule() {
        return module;
    }

    @Override
    public @Nullable IRBuilder getCurrent() {
        if (builderStack.isEmpty()) {
            return null;
        }
        return builderStack.peek();
    }

    @Override
    public void pushCurrent(final IRBuilder builder) {
        builderStack.push(builder);
    }

    @Override
    public @Nullable IRBuilder popCurrent() {
        if (builderStack.isEmpty()) {
            return null;
        }
        return builderStack.pop();
    }

    @Override
    public IRBuilder get(final String name) {
        return builders.computeIfAbsent(name, n -> {
            final var fnAddress = function.materialize(module, targetMachine);
            final var context = module.getContext();
            final var blockAddress = LLVMAppendBasicBlockInContext(context, fnAddress, STR.".\{name}");
            return new IRBuilder(this, targetMachine, blockAddress, context);
        });
    }

    @Override
    public IRBuilder getAndPush(final String name) {
        final var builder = get(name);
        builderStack.push(builder);
        return builder;
    }

    @Override
    public void drop() {
        if (isDropped) {
            return;
        }
        for (final var builder : builders.values()) {
            LLVMDeleteBasicBlock(builder.getBlockAddress());
        }
        isDropped = true;
    }

    @Override
    public void reset() {
        builderStack.clear();
        getAndPush(DEFAULT_BLOCK); // Restore default block
    }
}
