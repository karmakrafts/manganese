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

package io.karma.ferrous.manganese.ocm;

import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.util.Target2LongFunction;
import io.karma.ferrous.manganese.util.TokenUtils;
import org.lwjgl.system.MemoryUtil;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public final class BuiltinType implements Type {
    private final String name;
    private final Target2LongFunction typeProvider;
    private long materializedType = MemoryUtil.NULL;

    BuiltinType(String name, Target2LongFunction typeProvider) {
        this.name = name;
        this.typeProvider = typeProvider;
    }

    BuiltinType(final int token, final Target2LongFunction typeProvider) {
        this(TokenUtils.getLiteral(token), typeProvider);
    }

    public String getName() {
        return name;
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    @Override
    public long materialize(final Target target) {
        if (materializedType != MemoryUtil.NULL) {
            return materializedType;
        }
        return materializedType = typeProvider.getAddress(target);
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return new TypeAttribute[0];
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BuiltinType type) {
            return name.equals(type.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }
}
