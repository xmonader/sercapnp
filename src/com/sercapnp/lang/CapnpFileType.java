package com.sercapnp.lang;

/**
 * Created by striky on 6/26/17.
 */

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.*;

import javax.swing.*;


public class CapnpFileType extends LanguageFileType {
    public static final CapnpFileType INSTANCE = new CapnpFileType();

    private CapnpFileType() {
        super(CapnpLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Capnp file";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Capnp language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "Capnp";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return null;
        //return CapnpIcons.FILE;
    }
}

