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

package io.karma.ferrous.manganese.translate;

import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.vanadium.FerrousParser.FunctionIdentContext;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public final class FunctionTranslationUnit extends AbstractTranslationUnit {
    private final long function = NULL;

    public FunctionTranslationUnit(Compiler compiler) {
        super(compiler);
    }

    @Override
    public void enterFunctionIdent(FunctionIdentContext context) {

    }

    public long getFunction() {
        return function;
    }
}
