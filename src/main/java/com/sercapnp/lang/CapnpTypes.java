package com.sercapnp.lang;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;

/**
 * Created by striky on 6/26/17.
 */
public interface CapnpTypes {
    IElementType KEYWORD = new CapnpTokenType("KEYWORD");
    IElementType TYPE = new CapnpTokenType("TYPE");
    IElementType SEPARATOR = new CapnpTokenType("SEPARATOR");
    IElementType COMMENT = new CapnpTokenType("COMMENT");
    IElementType IDENTIFIER = new CapnpTokenType("IDENTIFIER");
    IElementType LEFT_BRACE = new CapnpTokenType("LEFT_BRACE");
    IElementType RIGHT_BRACE = new CapnpTokenType("RIGHT_BRACE");
    IElementType LEFT_BRACKET = new CapnpTokenType("LEFT_BRACKET");
    IElementType RIGHT_BRACKET = new CapnpTokenType("RIGHT_BRACKET");
    IElementType LEFT_PAREN = new CapnpTokenType("LEFT_PAREN");
    IElementType RIGHT_PAREN = new CapnpTokenType("RIGHT_PAREN");
    IElementType COMMA = new CapnpTokenType("COMMA");
}





