package com.sercapnp.lang;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles Ctrl+Click (Go to Declaration) for Cap'n Proto type references.
 *
 * When the user Ctrl+Clicks on a type name like "PersonInfo" or "StatusCode",
 * this handler finds its struct/enum/interface definition in:
 *   1. The current file
 *   2. Imported .capnp files
 * and navigates the editor to it.
 */
public class CapnpGotoDeclarationHandler implements GotoDeclarationHandler {

    // Pattern to find a type definition: struct/enum/interface Name
    // Captures the entire line so we can get exact offset
    private static final Pattern TYPE_DEF_LINE = Pattern.compile(
            "^(\\s*)(struct|enum|interface)\\s+(%s)\\b",
            Pattern.MULTILINE
    );

    // Pattern to find: const name
    private static final Pattern CONST_DEF_LINE = Pattern.compile(
            "^(\\s*)const\\s+(%s)\\b",
            Pattern.MULTILINE
    );

    // Pattern to find: annotation name
    private static final Pattern ANNOTATION_DEF_LINE = Pattern.compile(
            "^(\\s*)annotation\\s+(%s)\\b",
            Pattern.MULTILINE
    );

    // Pattern to find enum enumerant: name @N
    private static final Pattern ENUMERANT_LINE = Pattern.compile(
            "^(\\s+)(%s)\\s+@\\d+",
            Pattern.MULTILINE
    );

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                              int offset,
                                                              Editor editor) {
        if (sourceElement == null) return null;

        PsiFile file = sourceElement.getContainingFile();
        if (file == null || !(file instanceof CapnpFile)) return null;

        // Get the identifier text under the cursor
        String name = getIdentifierAt(file.getText(), offset);
        if (name == null || name.isEmpty()) return null;

        // Skip built-in types — no definition to navigate to
        if (isBuiltinType(name)) return null;

        Project project = file.getProject();

        // 1. Search in current file
        PsiElement found = findDefinition(file, name);
        if (found != null) return new PsiElement[]{found};

        // 2. Search in imported files
        CapnpSchemaScanner.ScanResult scan = CapnpSchemaScanner.scan(file.getText());

        VirtualFile vFile = file.getVirtualFile();
        if (vFile == null && file.getOriginalFile() != null) {
            vFile = file.getOriginalFile().getVirtualFile();
        }

        for (CapnpSchemaScanner.ImportInfo imp : scan.imports) {
            VirtualFile imported = CapnpSchemaScanner.resolveImport(vFile, imp.path, project);
            if (imported == null) continue;

            PsiFile importedPsi = PsiManager.getInstance(project).findFile(imported);
            if (importedPsi == null) continue;

            // If the import has an alias and the name matches the alias,
            // and there's a specific type, resolve to that type
            if (imp.alias != null && imp.alias.equals(name) && imp.specificType != null) {
                PsiElement target = findDefinition(importedPsi, imp.specificType);
                if (target != null) return new PsiElement[]{target};
            }

            // Try finding the name directly in the imported file
            PsiElement target = findDefinition(importedPsi, name);
            if (target != null) return new PsiElement[]{target};
        }

        // 3. Fallback: search all .capnp files in project
        for (VirtualFile capnpFile : com.intellij.psi.search.FileTypeIndex.getFiles(
                CapnpFileType.INSTANCE,
                com.intellij.psi.search.GlobalSearchScope.allScope(project))) {

            if (capnpFile.equals(file.getVirtualFile())) continue;

            PsiFile psi = PsiManager.getInstance(project).findFile(capnpFile);
            if (psi == null) continue;

            PsiElement target = findDefinition(psi, name);
            if (target != null) return new PsiElement[]{target};
        }

        return null;
    }

    /**
     * Find the PsiElement at the definition of 'name' in the given file.
     * Returns the PsiElement at the name's offset, or null.
     */
    private PsiElement findDefinition(PsiFile file, String name) {
        String text = file.getText();
        String escaped = Pattern.quote(name);

        // Try struct/enum/interface
        int nameOffset = findNameOffset(text, TYPE_DEF_LINE, escaped, name);
        if (nameOffset >= 0) {
            return file.findElementAt(nameOffset);
        }

        // Try const
        nameOffset = findNameOffset(text, CONST_DEF_LINE, escaped, name);
        if (nameOffset >= 0) {
            return file.findElementAt(nameOffset);
        }

        // Try annotation
        nameOffset = findNameOffset(text, ANNOTATION_DEF_LINE, escaped, name);
        if (nameOffset >= 0) {
            return file.findElementAt(nameOffset);
        }

        // Try enum enumerant
        nameOffset = findNameOffset(text, ENUMERANT_LINE, escaped, name);
        if (nameOffset >= 0) {
            return file.findElementAt(nameOffset);
        }

        return null;
    }

    /**
     * Find the character offset of 'name' in a pattern match.
     *
     * @param text     File text
     * @param template Pattern template with %s placeholder for the name
     * @param escaped  Regex-escaped name
     * @param name     Raw name string
     * @return offset of the name, or -1
     */
    private int findNameOffset(String text, Pattern template, String escaped, String name) {
        // Build the actual pattern by formatting the name into the template
        Pattern pattern = Pattern.compile(
                String.format(template.pattern(), escaped),
                template.flags()
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            // Find the exact position of the name within the match
            int matchStart = m.start();
            int namePos = text.indexOf(name, matchStart);
            if (namePos >= matchStart && namePos <= m.end()) {
                return namePos;
            }
        }
        return -1;
    }

    /**
     * Extract the identifier at the given offset.
     */
    private String getIdentifierAt(String text, int offset) {
        if (offset < 0 || offset >= text.length()) return null;

        // Find word boundaries
        int start = offset;
        int end = offset;

        while (start > 0 && isIdentChar(text.charAt(start - 1))) start--;
        while (end < text.length() && isIdentChar(text.charAt(end))) end++;

        if (start == end) return null;

        String word = text.substring(start, end);

        // Must start with a letter
        if (word.isEmpty() || !Character.isLetter(word.charAt(0))) return null;

        return word;
    }

    private boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private boolean isBuiltinType(String name) {
        switch (name) {
            case "Void": case "Bool":
            case "Int8": case "Int16": case "Int32": case "Int64":
            case "UInt8": case "UInt16": case "UInt32": case "UInt64":
            case "Float32": case "Float64":
            case "Text": case "Data": case "List": case "AnyPointer":
            // Keywords
            case "struct": case "enum": case "interface": case "union":
            case "group": case "import": case "using": case "const":
            case "annotation": case "extends":
            case "true": case "false": case "void": case "inf": case "nan":
                return true;
            default:
                return false;
        }
    }
}
