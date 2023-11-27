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

package io.karma.ferrous.manganese.util;

import io.karma.ferrous.vanadium.FerrousParser;
import org.apiguardian.api.API;

import java.util.EnumSet;
import java.util.List;

/**
 * @author Alexander Hinze
 * @since 08/11/2023
 */
@API(status = API.Status.INTERNAL)
public enum StorageMod {
    CONST, TLS;

    public static EnumSet<StorageMod> parse(final List<FerrousParser.StorageModContext> contexts) {
        final var mods = EnumSet.noneOf(StorageMod.class);
        for (final var context : contexts) {
            if (context.KW_CONST() != null) {
                mods.add(CONST);
            }
            if (context.KW_TLS() != null) {
                mods.add(TLS);
            }
        }
        return mods;
    }
}
