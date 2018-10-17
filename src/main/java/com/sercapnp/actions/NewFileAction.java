package com.sercapnp.actions;

import com.intellij.ide.actions.CreateFileAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbAware;
import com.sercapnp.lang.CapnpFileType;

public class NewFileAction extends CreateFileAction implements DumbAware {
    static final FileType FILE_TYPE = CapnpFileType.INSTANCE;
    static final String SUFFIX = "." + FILE_TYPE.getDefaultExtension().toLowerCase();

    @Override
    protected String getFileName(String newName) {
        return newName.endsWith(SUFFIX) ? newName : newName + SUFFIX;
    }

    public NewFileAction() {
        super(FILE_TYPE.getName(), "", null);
    }
}
