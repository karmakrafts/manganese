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

package io.karma.ferrous.manganese.ocm.access;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.Named;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.scope.Scoped;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @author Cedric Hammes
 * @since 17/10/2023
 */
@API(status = Status.INTERNAL)
public record ScopedAccess(TokenSlice tokenSlice, Type... types) implements Access {
    @Override
    public AccessKind getKind() {
        return AccessKind.SCOPED;
    }

    @Override
    public <T extends Scoped & Named> boolean hasAccess(final Compiler compiler, final CompileContext compileContext,
                                                        final ScopeStack scopeStack, final T target) {
        var type = compileContext.getOrCreateModuleData().findType(target.getQualifiedName(), target.getScopeName());
        for (Type allowedType : types) {
            if (type == allowedType) {
                return true;
            }
        }
        compileContext.reportError(CompileErrorCode.E4001);
        return false;
    }
}
