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
import io.karma.ferrous.manganese.ocm.Mangleable;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.scope.Scoped;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.ocm.type.FunctionType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.Mangler;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public class Function implements Scoped, Mangleable {
    protected final Identifier name;
    protected final CallingConvention callConv;
    protected final boolean isExtern;
    protected final boolean shouldMangle;
    protected final List<Parameter> parameters;
    protected final List<GenericParameter> genericParams;
    protected final TokenSlice tokenSlice;
    protected final FunctionType type;
    protected FunctionBody body;
    protected Scope enclosingScope;
    protected long materializedPrototype;

    public Function(final Identifier name, final CallingConvention callConv, final FunctionType type,
                    final boolean isExtern, final boolean shouldMangle, final TokenSlice tokenSlice,
                    final List<Parameter> params, final List<GenericParameter> genericParams) {
        this.name = name;
        this.callConv = callConv;
        this.isExtern = isExtern;
        this.shouldMangle = shouldMangle;
        this.type = type;
        this.tokenSlice = tokenSlice;
        this.parameters = params;
        this.genericParams = genericParams;
    }

    public void createBody(final Statement... statements) {
        if (body != null) {
            throw new IllegalStateException("Body already exists for this function");
        }
        body = new FunctionBody(this, statements);
    }

    public CallingConvention getCallConv() {
        return callConv;
    }

    public boolean isExtern() {
        return isExtern;
    }

    public boolean shouldMangle() {
        return shouldMangle;
    }

    public boolean isMonomorphic() {
        return genericParams.isEmpty();
    }

    public @Nullable FunctionBody getBody() {
        return body;
    }

    public FunctionType getType() {
        return type;
    }

    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public List<GenericParameter> getGenericParameters() {
        return genericParams;
    }

    public long materialize(final Module module, final TargetMachine targetMachine) {
        if (materializedPrototype != NULL) {
            return materializedPrototype;
        }
        final var address = LLVMAddFunction(module.getAddress(), getMangledName(), type.materialize(targetMachine));
        final var numParams = parameters.size();
        for (var i = 0; i < numParams; i++) {
            LLVMSetValueName2(LLVMGetParam(address, i), parameters.get(i).getName().toString());
        }
        LLVMSetLinkage(address, isExtern ? LLVMExternalLinkage : 0);
        LLVMSetFunctionCallConv(address, callConv.getLLVMValue(targetMachine));
        return materializedPrototype = address;
    }

    public long emit(final CompileContext compileContext, final Module module, final TargetMachine targetMachine) {
        if (body != null) {
            body.append(compileContext, module, targetMachine);
            return materializedPrototype; // This won't be NULL at this point
        }
        return materialize(module, targetMachine);
    }

    public void delete() {
        if (materializedPrototype == NULL) {
            return;
        }
        LLVMDeleteFunction(materializedPrototype);
        materializedPrototype = NULL;
    }

    public MonomorphizedFunction monomorphize(final List<Type> genericTypes) {
        return new MonomorphizedFunction(this, genericTypes);
    }

    // Mangleable

    @Override
    public String getMangledName() {
        return String.format("%s(%s)",
            getQualifiedName().toInternalName(),
            Mangler.mangleSequence(parameters.stream().map(Parameter::getType).toList()));
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
        return String.format("%s %s(%s)", type, name, parameters);
    }
}
