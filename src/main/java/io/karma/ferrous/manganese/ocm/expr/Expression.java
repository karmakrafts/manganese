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

package io.karma.ferrous.manganese.ocm.expr;

import io.karma.ferrous.manganese.ocm.constant.Constant;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @since 16/10/2023
 */
@API(status = Status.INTERNAL)
public interface Expression extends Statement {
    Type getType(final TargetMachine targetMachine);

    default boolean isConst() {
        return false;
    }

    default boolean isResultDiscarded() {
        return false;
    }

    default void setResultDiscarded(boolean isResultDiscarded) {
    }

    default boolean isConstant() {
        return false;
    }

    default Constant asConstant() {
        if (!isConstant()) {
            throw new RuntimeException("Not a constant");
        }
        return (Constant) this;
    }
}
