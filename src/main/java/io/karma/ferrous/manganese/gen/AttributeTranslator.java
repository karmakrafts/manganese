package io.karma.ferrous.manganese.gen;

import io.karma.ferrous.fir.codec.IFIRCodec;
import io.karma.ferrous.fir.tree.IFIRElement;
import io.karma.ferrous.fir.tree.block.impl.FIRAttributeBlock;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;

public final class AttributeTranslator implements IElementTranslator {
    @SuppressWarnings("unchecked")
    @Override
    public <E extends IFIRElement<E, C>, C extends IFIRCodec<E>> @NotNull E translate(final @NotNull ITranslationContext ctx, final @NotNull ParseTree node) {
        final var block = new FIRAttributeBlock();

        return (E)block;
    }
}
