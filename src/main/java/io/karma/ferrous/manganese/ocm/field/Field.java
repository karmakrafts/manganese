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

package io.karma.ferrous.manganese.ocm.field;

import io.karma.ferrous.manganese.ocm.Named;
import io.karma.ferrous.manganese.ocm.access.Access;
import io.karma.ferrous.manganese.ocm.access.AccessProvider;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.scope.Scoped;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
@API(status = Status.INTERNAL)
public final class Field implements Named, AccessProvider, Scoped {
    private final int index;
    private final Identifier name;
    private final Type type;
    private final Access access;
    private final TokenSlice tokenSlice;
    private final boolean isMutable;
    private final boolean isStatic;
    private final boolean isGlobal;
    private Scope enclosingScope;

    public Field(final int index, final Identifier name, final Type type, final Access access, final boolean isMutable,
                 final boolean isStatic, final boolean isGlobal, final TokenSlice tokenSlice) {
        this.index = index;
        this.name = name;
        this.type = type;
        this.access = access;
        this.isMutable = isMutable;
        this.isStatic = isStatic;
        this.isGlobal = isGlobal;
        this.tokenSlice = tokenSlice;
    }

    public int getIndex() {
        return index;
    }

    public Type getType() {
        return type;
    }

    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isMutable() {
        return isMutable;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    // AccessProvider

    @Override
    public Access getAccess() {
        return access;
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

    // NameProvider

    @Override
    public Identifier getName() {
        return name;
    }

    // Object

    @Override
    public String toString() {
        return STR."\{name}: \{type}";
    }
}
