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

package io.karma.ferrous.manganese.target;

import io.karma.ferrous.manganese.util.Logger;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.Objects;

import static org.lwjgl.llvm.LLVMTarget.*;
import static org.lwjgl.llvm.LLVMTargetMachine.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 18/10/2023
 */
@API(status = Status.STABLE)
public final class TargetMachine {
    private final Target target;
    private final String features;
    private final OptimizationLevel level;
    private final Relocation relocation;
    private final CodeModel codeModel;
    private final FileType fileType;
    private final long address;
    private final long dataAddress;
    private boolean isDisposed = false;

    @API(status = Status.INTERNAL)
    public TargetMachine(final Target target, final String features, final OptimizationLevel level,
                         final Relocation reloc, final CodeModel model, final FileType fileType) {
        this.target = target;
        this.features = features;
        this.level = level;
        this.relocation = reloc;
        this.codeModel = model;
        this.fileType = fileType;

        address = LLVMCreateTargetMachine(target.getAddress(), target.toString(), "", features, level.getLlvmValue(),
                                          reloc.getLlvmValue(), model.getLlvmValue());
        if (address == NULL) {
            throw new RuntimeException("Could not create target machine");
        }
        Logger.INSTANCE.debugln("Allocated target machine %s at 0x%08X", toString(), address);

        dataAddress = LLVMCreateTargetDataLayout(address);
        if (dataAddress == NULL) {
            throw new RuntimeException("Could not allocate target machine data");
        }
        Logger.INSTANCE.debugln("Allocated target machine data at 0x%08X", toString(), dataAddress);
    }

    public int getPointerSize() {
        return LLVMPointerSize(dataAddress);
    }

    public int getTypeAlignment(final long type) {
        return LLVMPreferredAlignmentOfType(dataAddress, type);
    }

    public int getGlobalAlignment(final long global) {
        return LLVMPreferredAlignmentOfGlobal(dataAddress, global);
    }

    public long getDataAddress() {
        return dataAddress;
    }

    public FileType getFileType() {
        return fileType;
    }

    public Target getTarget() {
        return target;
    }

    public String getFeatures() {
        return features;
    }

    public OptimizationLevel getLevel() {
        return level;
    }

    public Relocation getRelocation() {
        return relocation;
    }

    public CodeModel getCodeModel() {
        return codeModel;
    }

    public long getAddress() {
        return address;
    }

    public void dispose() {
        if (isDisposed) {
            return;
        }
        LLVMDisposeTargetData(dataAddress);
        Logger.INSTANCE.debugln("Disposed target machine data at 0x%08X", dataAddress);
        LLVMDisposeTargetMachine(address);
        Logger.INSTANCE.debugln("Disposed target machine %s at 0x%08X", toString(), address);
        isDisposed = true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, features, level, relocation, codeModel, fileType);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TargetMachine machine) { // @formatter:off
            return target.equals(machine.target)
                && features.equals(machine.features)
                && level == machine.level
                && relocation == machine.relocation
                && codeModel == machine.codeModel
                && fileType == machine.fileType;
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s (%s/%s/%s/%s/%s)", target, features, level, relocation, codeModel, fileType);
    }
}