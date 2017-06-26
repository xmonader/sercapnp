package com.sercapnp;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;

/**
 * Created by striky on 6/26/17.
 */
    public interface CapnpTypes {
        IElementType WHITE_SPACE = new IElementType("WHITE_SPACE", Language.ANY);
        IElementType KEYWORD = new IElementType("KEYWORD", Language.ANY);
        IElementType TYPE = new IElementType("TYPE", Language.ANY);
        IElementType SEPARATOR = new IElementType("SEPARATOR", Language.ANY);
        IElementType COMMENT = new IFileElementType("COMMENT", Language.ANY);
    }





