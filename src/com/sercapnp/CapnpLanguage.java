package com.sercapnp;


import com.intellij.lang.Language;

public class CapnpLanguage extends Language {
    public static final CapnpLanguage INSTANCE = new CapnpLanguage();

    private CapnpLanguage() {
        super("Capnp");
    }
}