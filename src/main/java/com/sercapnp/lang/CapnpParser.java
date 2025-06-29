package com.sercapnp.lang;

/**
 * Created by meetzli on 6/29/25.
 */

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import com.intellij.psi.TokenType;

public class CapnpParser implements PsiParser {
    @NotNull
    @Override
    public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        PsiBuilder.Marker rootMarker = builder.mark();


        while (!builder.eof()) {
            IElementType tokenType = builder.getTokenType();

            if (tokenType == TokenType.DUMMY_HOLDER) {
                builder.error("Unexpected character");
            }

            builder.advanceLexer();
        }


        rootMarker.done(root);
        return builder.getTreeBuilt();
    }
}
