package com.sercapnp.lang;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

public class CapnpLanguage extends Language {
    public static final CapnpLanguage INSTANCE = new CapnpLanguage();

    private CapnpLanguage() {
        super("Capnp");
    }

    @Override
    public @NotNull String getDisplayName() {
        return super.getDisplayName();
    }
}
