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

%state STRING_STATE

WHITE_SPACE=[ \n\t\r]+
END_OF_LINE_COMMENT="#"[^\r\n]*
CAPNPID = "@0x"[a-fA-F0-9]+
POSITION = "@"[0-9]+
HEX_NUMBER = "0x"[a-fA-F0-9]+
FLOAT_NUMBER = [0-9]+"."[0-9]+([eE][+-]?[0-9]+)?
INT_NUMBER = [0-9]+
IDENTIFIER = [A-Za-z_][A-Za-z_0-9]*

%%

<STRING_STATE> {
    \"                                              { yybegin(YYINITIAL); return CapnpTypes.STRING; }
    \\[^\r\n]                                       { }
    [^\"\\\r\n]+                                    { }
    [\r\n]                                          { yybegin(YYINITIAL); return CapnpTypes.STRING; }
}

<YYINITIAL> {
    {END_OF_LINE_COMMENT}                           { return CapnpTypes.COMMENT; }

    \"                                              { yybegin(STRING_STATE); }

    "using"                                         { return CapnpTypes.KEYWORD; }
    "import"                                        { return CapnpTypes.KEYWORD; }
    "annotation"                                    { return CapnpTypes.KEYWORD; }
    "struct"                                        { return CapnpTypes.KEYWORD; }
    "union"                                         { return CapnpTypes.KEYWORD; }
    "enum"                                          { return CapnpTypes.KEYWORD; }
    "const"                                         { return CapnpTypes.KEYWORD; }
    "interface"                                     { return CapnpTypes.KEYWORD; }
    "extends"                                       { return CapnpTypes.KEYWORD; }

    "true"                                          { return CapnpTypes.CONSTANT; }
    "false"                                         { return CapnpTypes.CONSTANT; }
    "void"                                          { return CapnpTypes.CONSTANT; }
    "inf"                                           { return CapnpTypes.CONSTANT; }
    "nan"                                           { return CapnpTypes.CONSTANT; }

    {CAPNPID}                                       { return CapnpTypes.CAPNPID; }
    {POSITION}                                      { return CapnpTypes.POSITION; }

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
    "AnyPointer"                                    { return CapnpTypes.TYPE; }
    "group"                                         { return CapnpTypes.TYPE; }

    {IDENTIFIER}                                    { return CapnpTypes.IDENTIFIER; }

    {HEX_NUMBER}                                    { return CapnpTypes.NUMBER; }
    {FLOAT_NUMBER}                                  { return CapnpTypes.NUMBER; }
    {INT_NUMBER}                                    { return CapnpTypes.NUMBER; }

    ";"                                             { return CapnpTypes.SEPARATOR; }
    ":"                                             { return CapnpTypes.SEPARATOR; }
    "="                                             { return CapnpTypes.SEPARATOR; }
    "->"                                            { return CapnpTypes.SEPARATOR; }
    ","                                             { return CapnpTypes.SEPARATOR; }
    "."                                             { return CapnpTypes.SEPARATOR; }
    "$"                                             { return CapnpTypes.SEPARATOR; }

    "{"                                             { return CapnpTypes.LEFT_BRACE; }
    "}"                                             { return CapnpTypes.RIGHT_BRACE; }
    "["                                             { return CapnpTypes.LEFT_BRACKET; }
    "]"                                             { return CapnpTypes.RIGHT_BRACKET; }
    "("                                             { return CapnpTypes.LEFT_PAREN; }
    ")"                                             { return CapnpTypes.RIGHT_PAREN; }

    {WHITE_SPACE}                                   { return TokenType.WHITE_SPACE; }
    [^]                                             { return TokenType.BAD_CHARACTER; }
}
