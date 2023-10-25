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

import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.NameProvider;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.scope.Scoped;
import io.karma.ferrous.manganese.ocm.type.Type;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @since 17/10/2023
 */
@API(status = Status.INTERNAL)
public record ScopedAccess(Type... types) implements Access {
    @Override
    public AccessKind getKind() {
        return AccessKind.SCOPED;
    }

    @Override
    public <T extends Scoped & NameProvider> boolean hasAccess(final Compiler compiler, final ScopeStack scopeStack,
                                                               final T target) {
        var compilerContext = compiler.getContext();
        var type = compiler.getContext().getAnalyzer()
                           .findTypeInScope(target.getQualifiedName(), target.getScopeName());
        for (Type allowedType : types) {
            if (type == allowedType) {
                return true;
            }
        }

        compilerContext.reportError(compilerContext.makeError(CompileErrorCode.E5001));
        return false;
    }
}
