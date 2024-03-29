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

package io.karma.ferrous.manganese.ocm.statement;

import io.karma.ferrous.manganese.ocm.ir.IREmitter;
import io.karma.ferrous.manganese.ocm.scope.Scoped;
import io.karma.ferrous.manganese.util.TokenSlice;

/**
 * @author Alexander Hinze
 * @since 22/10/2023
 */
public interface Statement extends IREmitter, Scoped {
    TokenSlice getTokenSlice();

    default boolean isUnsafe() {
        return false;
    }

    default boolean returnsExecution() { // False for loop, while(true) etc.
        return true;
    }

    default boolean terminatesScope() {
        return false;
    }

    default boolean terminatesBlock() {
        return terminatesScope();
    }
}
