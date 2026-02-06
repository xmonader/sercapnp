package com.sercapnp.lang;

/**
 * Created by meetzli on 6/29/25.
 * Patched by lilyslvr on 2/5/26 for Intellij Platform v2 Compatability
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
        return """
                # Cap'n Proto example
                @0x85150b117366d14b;
                
                struct Person {
                  id @0 :UInt32;
                  name @1 :Text;
                  email @2 :Text;
                }""";
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @NotNull
    @Override
    public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor @NotNull [] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Cap'n Proto";
    }
}
