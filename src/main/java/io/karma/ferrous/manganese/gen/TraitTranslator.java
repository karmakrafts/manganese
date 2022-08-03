package io.karma.ferrous.manganese.gen;

import io.karma.ferrous.fir.tree.IFIRElement;
import io.karma.ferrous.fir.tree.block.impl.FIRTraitBlock;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;

public final class TraitTranslator implements IElementTranslator {
    @Override
    public @NotNull IFIRElement<?, ?> translate(final @NotNull ITranslationContext ctx, final @NotNull ParseTree node) {
        final var block = new FIRTraitBlock();

        return block;
    }
}
