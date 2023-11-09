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

package io.karma.ferrous.manganese.ocm.function;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.NameProvider;
import io.karma.ferrous.manganese.ocm.Parameter;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.scope.Scoped;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.ocm.type.FunctionType;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.CallingConvention;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class Function implements NameProvider, Scoped {
    private final Identifier name;
    private final CallingConvention callConv;
    private final boolean isExtern;
    private final Parameter[] parameters;
    private final TokenSlice tokenSlice;
    private final FunctionType type;
    private FunctionBody body;
    private Scope enclosingScope;
    private long materializedPrototype;

    public Function(final Identifier name, final CallingConvention callConv, final FunctionType type,
                    final boolean isExtern, final TokenSlice tokenSlice, final Parameter... params) {
        this.name = name;
        this.callConv = callConv;
        this.isExtern = isExtern;
        this.type = type;
        this.tokenSlice = tokenSlice;
        this.parameters = params;
    }

    public void createBody(final Statement... statements) {
        if (body != null) {
            throw new IllegalStateException("Body already exists for this function");
        }
        body = new FunctionBody(statements);
    }

    public CallingConvention getCallConv() {
        return callConv;
    }

    public boolean isExtern() {
        return isExtern;
    }

    public @Nullable FunctionBody getBody() {
        return body;
    }

    public FunctionType getType() {
        return type;
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public long materializePrototype(final Module module, final TargetMachine targetMachine) {
        if (materializedPrototype != NULL) {
            return materializedPrototype;
        }
        final var address = LLVMAddFunction(module.getAddress(),
            name.toInternalName(),
            getType().materialize(targetMachine));
        final var numParams = parameters.length;
        for (var i = 0; i < numParams; i++) {
            LLVMSetValueName2(LLVMGetParam(address, i), parameters[i].getName().toString());
        }
        LLVMSetLinkage(address, isExtern ? LLVMExternalLinkage : 0);
        LLVMSetFunctionCallConv(address, callConv.getLLVMValue(targetMachine));
        return materializedPrototype = address;
    }

    public long materialize(final CompileContext compileContext, final Module module,
                            final TargetMachine targetMachine) {
        if (body != null) {
            body.append(compileContext, this, module, targetMachine);
            return materializedPrototype; // This won't be NULL at this point
        }
        return materializePrototype(module, targetMachine);
    }

    // NameProvider

    @Override
    public Identifier getName() {
        return name;
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

    // Object

    @Override
    public int hashCode() {
        return Objects.hash(name, callConv, isExtern, type, enclosingScope);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Function function) { // @formatter:off
            return name.equals(function.name)
                && callConv == function.callConv
                && isExtern == function.isExtern
                && type.equals(function.type)
                && Objects.equals(enclosingScope, function.enclosingScope);
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s %s(%s)", type, name, Arrays.toString(parameters));
    }
}