package com.sercapnp.lang;

import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
/**
 * Created by striky on 6/26/17.
 * Updated by meetzli on 6/29/25 for Intellij Platform 2025.1 compatibility
 */


public class CapnpSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    @NotNull
    @Override
    public SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile virtualFile) {
        return new CapnpSyntaxHighlighter();
    }
}

