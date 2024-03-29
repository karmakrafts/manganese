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

package io.karma.ferrous.manganese.compiler.pass;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.module.Module;
import org.apiguardian.api.API;

import java.util.concurrent.ExecutorService;

/**
 * @author Alexander Hinze
 * @since 16/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class EmitPass implements CompilePass {
    @Override
    public void run(final CompileContext compileContext, final Module module, final ExecutorService executor) {
        final var compiler = compileContext.getCompiler();
        final var profiler = compiler.getProfiler();
        profiler.push();
        final var moduleData = compileContext.getOrCreateModuleData();
        final var overloadSets = moduleData.getFunctions().values();
        for (final var overloadSet : overloadSets) {
            final var functions = overloadSet.values();
            for (final var function : functions) {
                if (!function.isMonomorphic()) {
                    continue;
                }
                function.emit(compileContext, module, compiler.getTargetMachine());
            }
        }
        profiler.pop();
    }
}
