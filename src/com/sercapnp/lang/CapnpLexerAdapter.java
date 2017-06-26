package com.sercapnp.lang;

import com.intellij.lexer.FlexAdapter;

import java.io.Reader;

/**
 * Created by striky on 6/26/17.
 */
public class CapnpLexerAdapter extends FlexAdapter {

    public CapnpLexerAdapter() {
        super(new CapnpLexer((Reader) null));
    }
}

