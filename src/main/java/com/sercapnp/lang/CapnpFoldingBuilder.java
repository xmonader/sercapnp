package com.sercapnp.lang;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CapnpFoldingBuilder extends FoldingBuilderEx {
    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        collectFoldRegions(root.getNode(), document, descriptors);
        return descriptors.toArray(new FoldingDescriptor[0]);
    }

    private void collectFoldRegions(@NotNull ASTNode node, @NotNull Document document, @NotNull List<FoldingDescriptor> descriptors) {
        if (node.getElementType() == CapnpTypes.LEFT_BRACE) {
            ASTNode parent = node.getTreeParent();
            if (parent != null) {
                // Find the matching RIGHT_BRACE within the same parent
                ASTNode sibling = node.getTreeNext();
                while (sibling != null) {
                    if (sibling.getElementType() == CapnpTypes.RIGHT_BRACE) {
                        int startOffset = node.getStartOffset();
                        int endOffset = sibling.getStartOffset() + sibling.getTextLength();
                        if (endOffset > startOffset + 1) {
                            TextRange range = new TextRange(startOffset, endOffset);
                            int startLine = document.getLineNumber(startOffset);
                            int endLine = document.getLineNumber(endOffset);
                            if (endLine > startLine) {
                                descriptors.add(new FoldingDescriptor(node, range));
                            }
                        }
                        break;
                    }
                    sibling = sibling.getTreeNext();
                }
            }
        }

        ASTNode child = node.getFirstChildNode();
        while (child != null) {
            collectFoldRegions(child, document, descriptors);
            child = child.getTreeNext();
        }
    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        return "{...}";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }
}
