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
import io.karma.ferrous.manganese.target.Architecture;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * @author Alexander Hinze
 * @since 28/10/2023
 */
@API(status = API.Status.INTERNAL)
public final class WASMLinker extends AbstractLinker {
    WASMLinker() {
        super(EnumSet.of(Architecture.WASM32, Architecture.WASM64));
    }

    @Override
    protected void buildCommand(final ArrayList<String> buffer, final String command, final Path outFile,
                                final Path objectFile, final LinkModel linkModel, final TargetMachine targetMachine,
                                final CompileContext compileContext, final LinkTargetType targetType) {
        buffer.add(command);
        buffer.addAll(options);
        buffer.add("-o");
        buffer.add(outFile.toAbsolutePath().normalize().toString());
        buffer.add(objectFile.toAbsolutePath().normalize().toString());
    }

    @Override
    public LinkerType getType() {
        return LinkerType.WASM;
    }
}
