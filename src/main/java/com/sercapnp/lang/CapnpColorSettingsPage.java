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
        new AttributesDescriptor("String", CapnpSyntaxHighlighter.STRING),
        new AttributesDescriptor("Number", CapnpSyntaxHighlighter.NUMBER),
        new AttributesDescriptor("Constant", CapnpSyntaxHighlighter.CONSTANT),
        new AttributesDescriptor("Unique ID", CapnpSyntaxHighlighter.CAPNPID),
        new AttributesDescriptor("Ordinal", CapnpSyntaxHighlighter.POSITION),
        new AttributesDescriptor("Braces", CapnpSyntaxHighlighter.BRACES),
        new AttributesDescriptor("Brackets", CapnpSyntaxHighlighter.BRACKETS),
        new AttributesDescriptor("Parentheses", CapnpSyntaxHighlighter.PARENS),
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
               "using Import = import \"schema.capnp\";\n" +
               "\n" +
               "struct Person {\n" +
               "  id @0 :UInt32;\n" +
               "  name @1 :Text;\n" +
               "  email @2 :Text;\n" +
               "  phones @3 :List(PhoneNumber);\n" +
               "  employed @4 :Bool = true;\n" +
               "  salary @5 :Float64 = 0.0;\n" +
               "\n" +
               "  struct PhoneNumber {\n" +
               "    number @0 :Text;\n" +
               "    type @1 :PhoneType = mobile;\n" +
               "  }\n" +
               "\n" +
               "  enum PhoneType {\n" +
               "    mobile @0;\n" +
               "    home @1;\n" +
               "    work @2;\n" +
               "  }\n" +
               "}\n" +
               "\n" +
               "interface AddressBook {\n" +
               "  getPerson @0 (id :UInt32) -> (person :Person);\n" +
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
