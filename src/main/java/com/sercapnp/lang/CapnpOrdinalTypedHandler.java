package com.sercapnp.lang;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auto-inserts the next ordinal number when '@' is typed in a field/enumerant position.
 *
 * Behavior:
 *   - User types field name, then '@'
 *   - Handler detects we're inside a struct/enum/interface/union block
 *   - Finds the highest existing @N ordinal in the current scope
 *   - Replaces the typed '@' with '@(N+1)'
 *
 * Does NOT trigger when:
 *   - At line start (might be a file ID like @0xABCD)
 *   - After '=' or ':' (value/type context)
 *   - Outside of any block (brace depth 0)
 *   - Inside a comment or string
 */
public class CapnpOrdinalTypedHandler extends TypedHandlerDelegate {

    // Matches @N ordinals in text
    private static final Pattern ORDINAL_PATTERN = Pattern.compile("@(\\d+)");

    @NotNull
    @Override
    public Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (c != '@') return Result.CONTINUE;
        if (!(file instanceof CapnpFile)) return Result.CONTINUE;

        Document doc = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        String text = doc.getText();

        // The '@' has already been inserted at offset-1
        int atPos = offset - 1;
        if (atPos < 0) return Result.CONTINUE;

        // Check context: should we auto-insert ordinal?
        if (!shouldAutoInsert(text, atPos)) {
            return Result.CONTINUE;
        }

        // Find the enclosing block boundaries
        int blockStart = findBlockStart(text, atPos);
        if (blockStart < 0) return Result.CONTINUE;

        int blockEnd = findBlockEnd(text, atPos);
        if (blockEnd < 0) blockEnd = text.length();

        // Find highest ordinal in the current block
        int maxOrdinal = findMaxOrdinal(text, blockStart, blockEnd);
        int nextOrdinal = maxOrdinal + 1;

        // Insert the ordinal number right after '@'
        String ordinalStr = Integer.toString(nextOrdinal);
        doc.insertString(offset, ordinalStr);

        // Move caret after the inserted number
        editor.getCaretModel().moveToOffset(offset + ordinalStr.length());

        return Result.STOP;
    }

    /**
     * Determine if we should auto-insert an ordinal at this position.
     */
    private boolean shouldAutoInsert(String text, int atPos) {
        // Must be inside a block (brace depth > 0)
        int braceDepth = countBraceDepth(text, atPos);
        if (braceDepth <= 0) return false;

        // Check what's before the '@' (skip whitespace)
        int pos = atPos - 1;
        while (pos >= 0 && (text.charAt(pos) == ' ' || text.charAt(pos) == '\t')) {
            pos--;
        }

        if (pos < 0) return false;

        char prevChar = text.charAt(pos);

        // The '@' should come after an identifier (field name / enum value / method name)
        if (!Character.isLetterOrDigit(prevChar) && prevChar != '_') {
            return false;
        }

        // Walk back through the identifier
        while (pos >= 0 && (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '_')) {
            pos--;
        }

        // Now pos is at the character before the identifier.
        // Skip whitespace again
        while (pos >= 0 && (text.charAt(pos) == ' ' || text.charAt(pos) == '\t')) {
            pos--;
        }

        if (pos < 0) return false;

        char beforeIdent = text.charAt(pos);

        // Valid contexts for ordinal:
        // - After newline or '{' → field/enumerant at start of line (inside block)
        // - After ';' → next field
        // These indicate we're at a field/enumerant declaration position
        if (beforeIdent == '\n' || beforeIdent == '\r' || beforeIdent == '{' || beforeIdent == ';') {
            return true;
        }

        // Also check: if the identifier is preceded by ')' that could be method params
        // e.g., method name @N (...) -> (...)
        // But typically in capnp: methodName @N (params) -> (results)
        // so '@' comes right after the name, same as fields

        return false;
    }

    /**
     * Find the start of the enclosing block (the '{' that opens the current scope).
     */
    private int findBlockStart(String text, int pos) {
        int depth = 0;
        boolean inString = false;
        boolean inComment = false;

        for (int i = pos - 1; i >= 0; i--) {
            char c = text.charAt(i);

            // Handle reverse scanning through comments (tricky — skip to line start for #)
            if (c == '\n') {
                inComment = false;
                continue;
            }

            if (inString) {
                if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                    inString = false;
                }
                continue;
            }

            // Check if this line has a # comment — if so, skip if we're after #
            if (c == '#') {
                inComment = true;
                continue;
            }
            if (inComment) continue;

            if (c == '"') {
                inString = true;
                continue;
            }

            if (c == '}') {
                depth++;
            } else if (c == '{') {
                if (depth == 0) {
                    return i;
                }
                depth--;
            }
        }

        return -1;
    }

    /**
     * Find the end of the enclosing block (the '}' that closes the current scope).
     */
    private int findBlockEnd(String text, int pos) {
        int depth = 0;
        boolean inString = false;
        boolean inComment = false;

        for (int i = pos; i < text.length(); i++) {
            char c = text.charAt(i);

            if (inComment) {
                if (c == '\n') inComment = false;
                continue;
            }

            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
                continue;
            }

            if (c == '#') { inComment = true; continue; }
            if (c == '"') { inString = true; continue; }

            if (c == '{') {
                depth++;
            } else if (c == '}') {
                if (depth == 0) return i;
                depth--;
            }
        }

        return -1;
    }

    /**
     * Find the maximum ordinal @N within the given text range.
     * Only looks at the current block level (skips nested blocks).
     */
    private int findMaxOrdinal(String text, int start, int end) {
        int maxOrdinal = -1;
        int depth = 0;
        boolean inString = false;
        boolean inComment = false;

        // Start after the opening '{'
        int searchStart = start + 1;

        for (int i = searchStart; i < end && i < text.length(); i++) {
            char c = text.charAt(i);

            if (inComment) {
                if (c == '\n') inComment = false;
                continue;
            }

            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
                continue;
            }

            if (c == '#') { inComment = true; continue; }
            if (c == '"') { inString = true; continue; }

            if (c == '{') { depth++; continue; }
            if (c == '}') { depth--; continue; }

            // Only look at ordinals at the current block level (depth 0)
            // But ALSO look inside unions/groups at depth 1,
            // because capnp ordinals share the same numbering space within a struct
            if (depth <= 1 && c == '@' && i + 1 < end) {
                // Check if this is @N (ordinal) not @0x... (type ID)
                int numStart = i + 1;
                if (numStart < end && Character.isDigit(text.charAt(numStart))) {
                    int numEnd = numStart;
                    while (numEnd < end && Character.isDigit(text.charAt(numEnd))) {
                        numEnd++;
                    }
                    // Make sure it's not @0x... (hex ID)
                    if (numEnd < end && (text.charAt(numEnd) == 'x' || text.charAt(numEnd) == 'X')) {
                        continue; // This is a hex ID, skip
                    }
                    try {
                        int ordinal = Integer.parseInt(text.substring(numStart, numEnd));
                        if (ordinal > maxOrdinal) {
                            maxOrdinal = ordinal;
                        }
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        }

        return maxOrdinal;
    }

    private int countBraceDepth(String text, int offset) {
        int depth = 0;
        boolean inString = false;
        boolean inComment = false;

        for (int i = 0; i < offset && i < text.length(); i++) {
            char c = text.charAt(i);
            if (inComment) {
                if (c == '\n') inComment = false;
                continue;
            }
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
                continue;
            }
            if (c == '#') { inComment = true; continue; }
            if (c == '"') { inString = true; continue; }
            if (c == '{') depth++;
            if (c == '}') depth--;
        }
        return depth;
    }
}
