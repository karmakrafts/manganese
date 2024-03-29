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

package io.karma.ferrous.manganese.linker;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.target.Architecture;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.EnumSet;

/**
 * @author Alexander Hinze
 * @since 28/10/2023
 */
@API(status = API.Status.STABLE)
public interface Linker {
    void link(final Compiler compiler, final CompileContext compileContext, final Path outFile, final Path objectFile,
              final LinkModel linkModel, final TargetMachine target, final LinkTargetType targetType);

    @Nullable Path getDynamicLinkerPath();

    void setDynamicLinkerPath(final Path path);

    void addOption(final String option);

    void clearOptions();

    LinkerType getType();

    EnumSet<Architecture> getSupportedArchitectures();

    default void addRawOptions(final String options) {
        if (options.isBlank()) {
            return;
        }
        final var chunks = options.split(",");
        for (final var chunk : chunks) {
            addOption(chunk.trim());
        }
    }
}
