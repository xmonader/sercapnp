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
CAPNPID = "@0x"[a-z0-9]+
POSITION = "@"[0-9]+
IDENTIFIER = [A-Za-z_][A-Za-z_0-9]*

%%
    {END_OF_LINE_COMMENT}                           { return CapnpTypes.COMMENT; }

    "using"                                         { return CapnpTypes.KEYWORD; }
    "import"                                        { return CapnpTypes.KEYWORD; }
    "annotation"                                    { return CapnpTypes.KEYWORD; }
    "struct"                                        { return CapnpTypes.KEYWORD; }
    "union"                                         { return CapnpTypes.KEYWORD; }
    "enum"                                          { return CapnpTypes.KEYWORD; }
    "const"                                         { return CapnpTypes.KEYWORD; }
    "interface"                                     { return CapnpTypes.KEYWORD; }
    "extends"                                       { return CapnpTypes.KEYWORD; }

    {POSITION}                                      { return CapnpTypes.KEYWORD; }
    {CAPNPID}                                       { return CapnpTypes.KEYWORD; }

    "Void"                                          { return CapnpTypes.TYPE; }
    "Bool"                                          { return CapnpTypes.TYPE; }
    "Int8"                                          { return CapnpTypes.TYPE; }
    "Int16"                                         { return CapnpTypes.TYPE; }
    "Int32"                                         { return CapnpTypes.TYPE; }
    "Int64"                                         { return CapnpTypes.TYPE; }
    "UInt8"                                         { return CapnpTypes.TYPE; }
    "UInt16"                                        { return CapnpTypes.TYPE; }
    "UInt32"                                        { return CapnpTypes.TYPE; }
    "UInt64"                                        { return CapnpTypes.TYPE; }
    "Float32"                                       { return CapnpTypes.TYPE; }
    "Float64"                                       { return CapnpTypes.TYPE; }
    "Data"                                          { return CapnpTypes.TYPE; }
    "Text"                                          { return CapnpTypes.TYPE; }
    "List"                                          { return CapnpTypes.TYPE; }
    "group"                                         { return CapnpTypes.TYPE; }

    {IDENTIFIER}                                    { return CapnpTypes.IDENTIFIER; }
    {SEPARATOR}                                     { return CapnpTypes.SEPARATOR; }

    "{"                                             { return CapnpTypes.LEFT_BRACE; }
    "}"                                             { return CapnpTypes.RIGHT_BRACE; }
    "["                                             { return CapnpTypes.LEFT_BRACKET; }
    "]"                                             { return CapnpTypes.RIGHT_BRACKET; }

    "("                                             { return CapnpTypes.LEFT_PAREN; }
    ")"                                             { return CapnpTypes.RIGHT_PAREN; }
    ","                                             { return CapnpTypes.COMMA; }


    ({WHITE_SPACE})+                                { return TokenType.WHITE_SPACE; }
    [^]                                             { return TokenType.BAD_CHARACTER; }
