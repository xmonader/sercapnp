package com.sercapnp.lang;

/**
 * Created by striky on 6/26/17.
 */


import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CapnpFile extends PsiFileBase {
    public CapnpFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, CapnpLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return CapnpFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Simple File";
    }

    @Override
    public Icon getIcon(int flags) {
        return super.getIcon(flags);
    }

}
