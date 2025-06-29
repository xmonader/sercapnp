package com.sercapnp.lang;

/**
 * Created by meetzli on 6/29/25.
 */

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class CapnpColorSettingsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
        new AttributesDescriptor("Keyword", CapnpSyntaxHighlighter.KEY),
        new AttributesDescriptor("Type", CapnpSyntaxHighlighter.TYPE),
        new AttributesDescriptor("Comment", CapnpSyntaxHighlighter.COMMENT),
        new AttributesDescriptor("Identifier", CapnpSyntaxHighlighter.IDENTIFIER),
        new AttributesDescriptor("Separator", CapnpSyntaxHighlighter.SEPARATOR),
    };

    @Nullable
    @Override
    public Icon getIcon() {
        return CapnpFileType.INSTANCE.getIcon();
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new CapnpSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return "# Cap'n Proto example\n" +
               "@0x85150b117366d14b;\n" +
               "\n" +
               "struct Person {\n" +
               "  id @0 :UInt32;\n" +
               "  name @1 :Text;\n" +
               "  email @2 :Text;\n" +
               "}";
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Cap'n Proto";
    }
}
