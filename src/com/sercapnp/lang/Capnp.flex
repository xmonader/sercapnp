package com.sercapnp.lang;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import com.sercapnp.lang.CapnpTypes;

%%
%class CapnpLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType


WHITE_SPACE=[\ \n\t]
END_OF_LINE_COMMENT="#"[^\r\n]*
SEPARATOR=["="|";"|":"|"->"]
LB = ["{" | "["]
RB = ["}" | "]"]
CAPNPID = "@0x"[a-z0-9]+
POSITION = "@"[0-9]+

IDENTIFIER = [A-Za-z_][A-Za-z_0-9]*

%%
    {END_OF_LINE_COMMENT}                           { yybegin(YYINITIAL);  return CapnpTypes.COMMENT; }

    "using"                                         { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }
    "import"                                        { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }
    "annotation"                                    { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }
    "struct"                                        { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }
    "union"                                         { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }
    "enum"                                          { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }
    "const"                                         { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }
    "interface"                                     { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }
    "extends  "                                     { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }

    {POSITION}                                      { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }
    {CAPNPID}                                       { yybegin(YYINITIAL);  return CapnpTypes.KEYWORD; }


    "Void"                                          { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "Bool"                                          { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "Int8"                                          { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "Int16"                                         { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "Int32"                                         { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "Int64"                                         { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "UInt8"                                         { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "UInt16"                                        { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "UInt32"                                        { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "UInt64"                                        { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "Float32"                                       { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "Float64"                                       { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "Data"                                          { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "Text"                                          { yybegin(YYINITIAL); return CapnpTypes.TYPE; }
    "List"                                          { yybegin(YYINITIAL); return CapnpTypes.TYPE; }

    "group"                                         { yybegin(YYINITIAL); return CapnpTypes.TYPE; }

    {IDENTIFIER}                                    { yybegin(YYINITIAL);  return CapnpTypes.IDENTIFIER; }


    {SEPARATOR}                                     { yybegin(YYINITIAL);  return CapnpTypes.SEPARATOR; }

    {LB}                                            { yybegin(YYINITIAL);  return TokenType.DUMMY_HOLDER; }
    {RB}                                            { yybegin(YYINITIAL);  return TokenType.DUMMY_HOLDER; }

    ({WHITE_SPACE})+                                { yybegin(YYINITIAL);  return TokenType.WHITE_SPACE; }
    [^]                                             { return TokenType.DUMMY_HOLDER; }

