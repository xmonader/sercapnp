package com.sercapnp.lang;

/**
 * Created by striky on 6/26/17.
 * Updated by meetzli on 6/29/25 for Intellij Platform 2025.1 compatibility
 */

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;



public class CapnpSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey SEPARATOR =
            createTextAttributesKey("CAPNP_SEPARATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey KEY =
            createTextAttributesKey("CAPNP_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey TYPE =
            createTextAttributesKey("CAPNP_TYPE", DefaultLanguageHighlighterColors.CLASS_NAME);
    public static final TextAttributesKey IDENTIFIER =
            createTextAttributesKey("CAPNP_IDENTIFIER", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    public static final TextAttributesKey COMMENT =
            createTextAttributesKey("CAPNP_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey STRING =
            createTextAttributesKey("CAPNP_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey NUMBER =
            createTextAttributesKey("CAPNP_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey CONSTANT =
            createTextAttributesKey("CAPNP_CONSTANT", DefaultLanguageHighlighterColors.CONSTANT);
    public static final TextAttributesKey CAPNPID =
            createTextAttributesKey("CAPNP_UNIQUE_ID", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey POSITION =
            createTextAttributesKey("CAPNP_ORDINAL", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey BRACES =
            createTextAttributesKey("CAPNP_BRACES", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey BRACKETS =
            createTextAttributesKey("CAPNP_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
    public static final TextAttributesKey PARENS =
            createTextAttributesKey("CAPNP_PARENS", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey BAD_CHARACTER =
            createTextAttributesKey("CAPNP_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

    private static final TextAttributesKey[] BAD_CHAR_KEYS = new TextAttributesKey[]{BAD_CHARACTER};
    private static final TextAttributesKey[] SEPARATOR_KEYS = new TextAttributesKey[]{SEPARATOR};
    private static final TextAttributesKey[] KEY_KEYS = new TextAttributesKey[]{KEY};
    private static final TextAttributesKey[] TYPE_KEYS = new TextAttributesKey[]{TYPE};
    private static final TextAttributesKey[] IDENTIFIER_KEYS = new TextAttributesKey[]{IDENTIFIER};
    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
    private static final TextAttributesKey[] STRING_KEYS = new TextAttributesKey[]{STRING};
    private static final TextAttributesKey[] NUMBER_KEYS = new TextAttributesKey[]{NUMBER};
    private static final TextAttributesKey[] CONSTANT_KEYS = new TextAttributesKey[]{CONSTANT};
    private static final TextAttributesKey[] CAPNPID_KEYS = new TextAttributesKey[]{CAPNPID};
    private static final TextAttributesKey[] POSITION_KEYS = new TextAttributesKey[]{POSITION};
    private static final TextAttributesKey[] BRACES_KEYS = new TextAttributesKey[]{BRACES};
    private static final TextAttributesKey[] BRACKETS_KEYS = new TextAttributesKey[]{BRACKETS};
    private static final TextAttributesKey[] PARENS_KEYS = new TextAttributesKey[]{PARENS};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new CapnpLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(CapnpTypes.SEPARATOR)) {
            return SEPARATOR_KEYS;
        } else if (tokenType.equals(CapnpTypes.IDENTIFIER)) {
            return IDENTIFIER_KEYS;
        } else if (tokenType.equals(CapnpTypes.KEYWORD)) {
            return KEY_KEYS;
        } else if (tokenType.equals(CapnpTypes.TYPE)) {
            return TYPE_KEYS;
        } else if (tokenType.equals(CapnpTypes.COMMENT)) {
            return COMMENT_KEYS;
        } else if (tokenType.equals(CapnpTypes.STRING)) {
            return STRING_KEYS;
        } else if (tokenType.equals(CapnpTypes.NUMBER)) {
            return NUMBER_KEYS;
        } else if (tokenType.equals(CapnpTypes.CONSTANT)) {
            return CONSTANT_KEYS;
        } else if (tokenType.equals(CapnpTypes.CAPNPID)) {
            return CAPNPID_KEYS;
        } else if (tokenType.equals(CapnpTypes.POSITION)) {
            return POSITION_KEYS;
        } else if (tokenType.equals(CapnpTypes.LEFT_BRACE) || tokenType.equals(CapnpTypes.RIGHT_BRACE)) {
            return BRACES_KEYS;
        } else if (tokenType.equals(CapnpTypes.LEFT_BRACKET) || tokenType.equals(CapnpTypes.RIGHT_BRACKET)) {
            return BRACKETS_KEYS;
        } else if (tokenType.equals(CapnpTypes.LEFT_PAREN) || tokenType.equals(CapnpTypes.RIGHT_PAREN)) {
            return PARENS_KEYS;
        } else if (tokenType.equals(TokenType.BAD_CHARACTER)) {
            return BAD_CHAR_KEYS;
        } else {
            return EMPTY_KEYS;
        }
    }
}