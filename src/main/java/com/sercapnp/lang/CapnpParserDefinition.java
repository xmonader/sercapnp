package com.sercapnp.lang;

/**
 * Created by striky on 6/26/17.
 * Updated by meetzli on 6/29/25 for Intellij Platform 2025.1 compatibility
 */
import com.intellij.lang.*;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.psi.tree.*;

import org.jetbrains.annotations.NotNull;

public class CapnpParserDefinition implements ParserDefinition {
    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    public static final TokenSet COMMENTS = TokenSet.create(CapnpTypes.COMMENT);
    public static final TokenSet BAD_CHARACTERS = TokenSet.create(TokenType.BAD_CHARACTER);

    public static final IFileElementType FILE = new IFileElementType(CapnpLanguage.INSTANCE);

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new CapnpLexerAdapter();
    }

    @NotNull
    public TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }

    @NotNull
    public TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @NotNull
    public TokenSet getStringLiteralElements() {
        return TokenSet.EMPTY;
    }

    @NotNull
    public PsiParser createParser(final Project project) {
        return new CapnpParser();

    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    public PsiFile createFile(FileViewProvider viewProvider) {
        return new CapnpFile(viewProvider);
    }

    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }

    @NotNull
    public PsiElement createElement(ASTNode node) {
        return new ASTWrapperPsiElement(node);
    }
}