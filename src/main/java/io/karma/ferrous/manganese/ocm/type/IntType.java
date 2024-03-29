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

import io.karma.ferrous.manganese.ocm.Mangleable;
import io.karma.ferrous.manganese.ocm.constant.BigIntConstant;
import io.karma.ferrous.manganese.ocm.constant.IntConstant;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.DefaultScope;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.kommons.function.Functions;
import org.apiguardian.api.API;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryUtil;

import java.math.BigInteger;

/**
 * @author Alexander Hinze
 * @since 03/12/2023
 */
@API(status = API.Status.INTERNAL)
public final class IntType implements Type, Mangleable {
    public static final char SEQUENCE_PREFIX = '\'';
    public static final IntType I1 = Types.integer(1, false, Functions.castingIdentity());
    public static final IntType I8 = Types.integer(8, false, Functions.castingIdentity());
    public static final IntType I16 = Types.integer(16, false, Functions.castingIdentity());
    public static final IntType I32 = Types.integer(32, false, Functions.castingIdentity());
    public static final IntType I64 = Types.integer(64, false, Functions.castingIdentity());
    public static final IntType U8 = Types.integer(8, true, Functions.castingIdentity());
    public static final IntType U16 = Types.integer(16, true, Functions.castingIdentity());
    public static final IntType U32 = Types.integer(32, true, Functions.castingIdentity());
    public static final IntType U64 = Types.integer(64, true, Functions.castingIdentity());

    private final int width;
    private final boolean isUnsigned;
    private final Identifier name;
    private final String mangledName;
    private long materializedType;

    public IntType(final int width, final boolean isUnsigned) {
        this.width = width;
        this.isUnsigned = isUnsigned;
        name = new Identifier(STR."\{isUnsigned ? 'u' : 'i'}\{width}");
        mangledName = STR."\{isUnsigned ? 'U' : 'S'}\{width}";
    }

    public int getWidth() {
        return width;
    }

    public boolean isUnsigned() {
        return isUnsigned;
    }

    public long sizeCast(final IntType type, final long value, final TargetMachine targetMachine,
                         final IRContext irContext) {
        final var builder = irContext.getCurrentOrCreate();
        final var typeAddress = type.materialize(targetMachine);
        final var otherWidth = type.getWidth();
        if (otherWidth == width) {
            return value; // If the width is the same, we don't do anything to it
        }
        if (otherWidth < width) {
            return builder.trunc(typeAddress, value);
        }
        if (type.isUnsigned) {
            return builder.zext(typeAddress, value);
        }
        return builder.sext(typeAddress, value);
    }

    @Override
    public long cast(final Type type, final long value, final TargetMachine targetMachine, final IRContext irContext) {
        final var builder = irContext.getCurrentOrCreate();
        final var typeAddress = type.materialize(targetMachine);
        return switch (type) {
            case BoolType ignored -> sizeCast(IntType.I1, value, targetMachine, irContext);
            case CharType ignored -> sizeCast(IntType.I8, value, targetMachine, irContext);
            case IntType intType -> sizeCast(intType, value, targetMachine, irContext);
            case RealType realType -> {
                final var realWidth = realType.getWidth();
                final var intType = Types.integer(realWidth, isUnsigned, Functions.castingIdentity());
                // @formatter:off
                final var intValue = isUnsigned
                    ? builder.floatToUint(typeAddress, value)
                    : builder.floatToSint(typeAddress, value);
                // @formatter:on
                yield sizeCast(intType, intValue, targetMachine, irContext);
            }
            default -> value;
        };
    }

    @Override
    public boolean canBeCastFrom(final TargetMachine targetMachine, final Type type) {
        if (type.isPtr()) { // Ints can be cast from pointers
            return true;
        }
        final var kind = type.getKind();
        return kind == TypeKind.BOOL || kind == TypeKind.CHAR || kind == TypeKind.REAL || kind == TypeKind.INT;
    }

    @Override
    public boolean canAccept(final TargetMachine targetMachine, final Type type) {
        return switch(type) { // @formatter:off
            case IntType intType -> intType.getWidth() <= width;
            default              -> false;
        }; // @formatter:on
    }

    @Override
    public char getMangledSequencePrefix() {
        return SEQUENCE_PREFIX;
    }

    @Override
    public String getMangledName() {
        return mangledName;
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public TypeKind getKind() {
        return isUnsigned ? TypeKind.UINT : TypeKind.INT;
    }

    @Override
    public Identifier getName() {
        return name;
    }

    @Override
    public Scope getEnclosingScope() {
        return DefaultScope.GLOBAL;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
    }

    @Override
    public long materialize(final TargetMachine machine) {
        if (materializedType == MemoryUtil.NULL) {
            materializedType = LLVMCore.LLVMIntType(width);
        }
        return materializedType;
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    @Override
    public Expression makeDefaultValue(final TargetMachine targetMachine) {
        if (width <= 64) {
            return new IntConstant(this, 0, TokenSlice.EMPTY);
        }
        return new BigIntConstant(this, BigInteger.ZERO, TokenSlice.EMPTY);
    }

    @Override
    public String toString() {
        return getName().toString();
    }
}
