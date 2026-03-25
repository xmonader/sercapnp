package com.sercapnp.lang;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CapnpBraceMatcher implements PairedBraceMatcher {
    private static final BracePair[] PAIRS = new BracePair[]{
        new BracePair(CapnpTypes.LEFT_BRACE, CapnpTypes.RIGHT_BRACE, true),
        new BracePair(CapnpTypes.LEFT_BRACKET, CapnpTypes.RIGHT_BRACKET, false),
        new BracePair(CapnpTypes.LEFT_PAREN, CapnpTypes.RIGHT_PAREN, false),
    };

    @Override
    public BracePair @NotNull [] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
        return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
