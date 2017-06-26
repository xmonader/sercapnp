package com.sercapnp;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

%%
%class CapnpLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType


WHITE_SPACE=[\ \n\t]
END_OF_LINE_COMMENT="#"[^\r\n]*
SEPARATOR="="|";"

%%



    {END_OF_LINE_COMMENT}                           { yybegin(YYINITIAL);  return CapnpTypes.COMMENT; }

    "struct"                                        { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }
    "union"                                         { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }
    "enum"                                          { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }

//    "Void"                                          { return CapnpTypes.TYPE; }
//    "Bool"                                          { return CapnpTypes.TYPE; }
//    "Int16"                                         { return CapnpTypes.TYPE; }
//    "Int32"                                         { return CapnpTypes.TYPE; }
//    "Int64"                                         { return CapnpTypes.TYPE; }
//    "Data"                                          { return CapnpTypes.TYPE; }
//    "Text"                                          { return CapnpTypes.TYPE; }
//    "List"                                          { return CapnpTypes.TYPE; }
//    "Float32"                                       { return CapnpTypes.TYPE; }
//    "Float64"                                       { return CapnpTypes.TYPE; }





    {SEPARATOR}                                     { yybegin(YYINITIAL);  return CapnpTypes.SEPARATOR; }


    ({WHITE_SPACE})+                                { yybegin(YYINITIAL);  return TokenType.WHITE_SPACE; }
    //[^]                                      { return TokenType.BAD_CHARACTER; }
    [^]                              { throw new Error("Illegal character <"+
                                                        yytext()+">"); }
