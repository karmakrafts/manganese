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

package io.karma.ferrous.manganese.ocm.statement;

import io.karma.ferrous.manganese.ocm.Named;
import io.karma.ferrous.manganese.ocm.ValueStorage;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.field.FieldStorage;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.UserDefinedType;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.kommons.tuple.Pair;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static org.lwjgl.llvm.LLVMCore.LLVMSetValueName2;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 08/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class LetStatement implements Statement, Named, ValueStorage {
    private final Identifier name;
    private final Type type;
    private final boolean isMutable;
    private final TokenSlice tokenSlice;
    private final Map<Identifier, FieldStorage> fieldStorages;
    private Expression value;
    private boolean isInitialized;
    private Scope enclosingScope;
    private long immutableAddress;
    private long mutableAddress;
    private boolean hasChanged;

    public LetStatement(final Identifier name, final Type type, final @Nullable Expression value,
                        final boolean isMutable, final TokenSlice tokenSlice) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.isMutable = isMutable;
        this.isInitialized = value != null;
        this.tokenSlice = tokenSlice;
        if (type instanceof UserDefinedType udt) { // @formatter:off
            fieldStorages = udt.fields()
                .stream()
                .map(f -> Pair.of(f.getName(), new FieldStorage(f, this)))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        } // @formatter:off
        else {
            fieldStorages = Collections.emptyMap();
        }
    }

    // ValueCarrier

    @Override
    public void setInitialized() {
        isInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    // ValueStorage

    @Override
    public @Nullable ValueStorage getField(final Identifier name) {
        return fieldStorages.get(name);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public long getAddress(final TargetMachine targetMachine, final IRContext irContext) {
        if (!isMutable && mutableAddress == NULL) {
            // Allocate value on stack as needed so we can take its address, cache it
            final var builder = irContext.getCurrentOrCreate();
            mutableAddress = builder.alloca(getType().materialize(targetMachine));
            builder.store(value.emit(targetMachine, irContext), mutableAddress);
            LLVMSetValueName2(mutableAddress, STR."\{name.toInternalName()}.immaddr");
        }
        return mutableAddress;
    }

    @Override
    public long load(final TargetMachine targetMachine, final IRContext irContext) {
        if (!isMutable) {
            return immutableAddress; // Load from GPR directly
        }
        return ValueStorage.super.load(targetMachine, irContext);
    }

    @Override
    public void notifyMutation() {
        hasChanged = true;
    }

    @Override
    public Expression getValue() {
        return value;
    }

    @Override
    public void setValue(final @Nullable Expression value) {
        this.value = value;
    }

    @Override
    public boolean isRootMutable() {
        return isMutable;
    }

    @Override
    public boolean isMutable() {
        return isMutable;
    }

    @Override
    public boolean isMutated() {
        return hasChanged;
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

    // Statement

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        final var internalName = name.toInternalName();
        final var hasDefaultValue = value != null;
        if (hasDefaultValue) {
            immutableAddress = value.emit(targetMachine, irContext);
        }
        if (isMutable) {
            final var builder = irContext.getCurrentOrCreate();
            final var typeAddress = type.materialize(targetMachine);
            mutableAddress = builder.alloca(typeAddress);
            LLVMSetValueName2(mutableAddress, internalName);
            if (hasDefaultValue) {
                builder.store(immutableAddress, mutableAddress); // Store default value immediately after alloc
            }
        }
        else if (!irContext.isParameter(immutableAddress)) {
            LLVMSetValueName2(immutableAddress, internalName);
        }
        return NULL;
    }
}
