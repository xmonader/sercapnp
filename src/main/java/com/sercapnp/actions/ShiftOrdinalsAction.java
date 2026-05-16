package com.sercapnp.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shifts ordinals down (increments) to create a gap for inserting a new field.
 *
 * Usage:
 *   1. Place cursor on the line with @N (the field AFTER which you want to insert)
 *   2. Invoke via Ctrl+Alt+A, S (or menu: Tools → Shift Ordinals Down)
 *   3. All ordinals > N within the same struct scope are incremented by 1
 *   4. @(N+1) is now free — type your new field with that ordinal
 *
 * Scope rules (per Cap'n Proto spec):
 *   - union {} and group {} share the parent struct's ordinal space → INCLUDED
 *   - Nested struct/enum/interface have their OWN ordinal space → SKIPPED
 */
public class ShiftOrdinalsAction extends AnAction {

    // Matches @N where N is a decimal number (not @0x... hex IDs)
    private static final Pattern ORDINAL_PATTERN = Pattern.compile("@(\\d+)");

    // Matches the keyword before a { to determine block type
    private static final Pattern BLOCK_KEYWORD_PATTERN = Pattern.compile(
            "(struct|enum|interface|union|group)\\s+\\w*\\s*(?:\\([^)]*\\))?\\s*(?:@0x[a-fA-F0-9]+)?\\s*$"
    );

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getData(CommonDataKeys.PROJECT);
        if (editor == null || project == null) return;

        Document document = editor.getDocument();
        String text = document.getText();
        int caretOffset = editor.getCaretModel().getOffset();

        // 1. Find the ordinal on the current line
        int lineNumber = document.getLineNumber(caretOffset);
        int lineStart = document.getLineStartOffset(lineNumber);
        int lineEnd = document.getLineEndOffset(lineNumber);
        String lineText = text.substring(lineStart, lineEnd);

        Matcher lineMatcher = ORDINAL_PATTERN.matcher(lineText);
        if (!lineMatcher.find()) {
            Messages.showInfoMessage(project,
                    "No ordinal (@N) found on the current line.\n" +
                    "Place your cursor on the line with the ordinal after which you want to insert.",
                    "Shift Ordinals");
            return;
        }

        int threshold = Integer.parseInt(lineMatcher.group(1));

        // 2. Find the enclosing struct scope
        int[] scope = findEnclosingStructScope(text, caretOffset);
        if (scope == null) {
            Messages.showInfoMessage(project,
                    "Could not find an enclosing struct scope.",
                    "Shift Ordinals");
            return;
        }

        int scopeStart = scope[0]; // Position of '{'
        int scopeEnd = scope[1];   // Position of '}'

        // 3. Collect all ordinal positions within scope that need shifting
        //    Skip ordinals inside nested struct/enum/interface blocks
        List<OrdinalHit> hits = collectOrdinalsInScope(text, scopeStart, scopeEnd, threshold);

        if (hits.isEmpty()) {
            Messages.showInfoMessage(project,
                    "No ordinals found after @" + threshold + " to shift.",
                    "Shift Ordinals");
            return;
        }

        // 4. Apply replacements from end to start (preserves earlier offsets)
        Collections.sort(hits, (a, b) -> Integer.compare(b.offset, a.offset));

        com.intellij.openapi.vfs.VirtualFile vFile =
                com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(document);

        WriteCommandAction.runWriteCommandAction(project, "Shift Ordinals Down", null, () -> {
            for (OrdinalHit hit : hits) {
                String replacement = "@" + (hit.value + 1);
                document.replaceString(hit.offset, hit.offset + hit.length, replacement);
            }
        }, vFile != null ?
                com.intellij.psi.PsiManager.getInstance(project).findFile(vFile) :
                null);

        // Show result
        com.intellij.openapi.editor.EditorModificationUtil.scrollToCaret(editor);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable in .capnp files
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        boolean enabled = false;

        if (editor != null) {
            com.intellij.openapi.vfs.VirtualFile vFile =
                    com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
                            .getFile(editor.getDocument());
            if (vFile != null) {
                enabled = vFile.getName().endsWith(".capnp");
            }
        }

        e.getPresentation().setEnabledAndVisible(enabled);
    }

    // ── Scope detection ─────────────────────────────────────────────────────────

    /**
     * Find the enclosing struct's { } boundaries.
     *
     * Walks backwards from caretOffset, tracking brace depth.
     * When hitting a '{' at depth 0, checks if the preceding keyword is:
     *   - struct → found it, return [bracePos, matchingClose]
     *   - union/group → continue walking (these share parent struct's ordinals)
     *   - enum/interface → also valid top-level scope, return it
     */
    private int[] findEnclosingStructScope(String text, int caretOffset) {
        int depth = 0;
        boolean inString = false;
        boolean inComment = false;

        for (int i = caretOffset - 1; i >= 0; i--) {
            char c = text.charAt(i);

            // Reverse comment detection: if we hit \n, clear comment state
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

            // Check if this position is inside a comment
            // (walk forward to check if there's a # before us on this line)
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
                    // This '{' is our potential scope opener
                    String blockType = getBlockType(text, i);

                    if ("struct".equals(blockType) || "enum".equals(blockType)
                            || "interface".equals(blockType)) {
                        // Found our scope — find the matching '}'
                        int closePos = findMatchingClose(text, i);
                        if (closePos >= 0) {
                            return new int[]{i, closePos};
                        }
                        return null;
                    }

                    // union/group → keep going, ordinals belong to the parent struct
                    // (don't change depth — we're looking for the PARENT's '{')
                    continue;
                }
                depth--;
            }
        }

        return null;
    }

    /**
     * Determine the keyword (struct/enum/interface/union/group) before a '{'.
     */
    private String getBlockType(String text, int bracePos) {
        String before = text.substring(Math.max(0, bracePos - 200), bracePos).trim();
        Matcher m = BLOCK_KEYWORD_PATTERN.matcher(before);
        if (m.find()) {
            return m.group(1);
        }

        // Fallback: check for anonymous union (just "union {")
        if (before.endsWith("union")) return "union";
        if (before.endsWith("group")) return "group";

        // Check for simple patterns without identifiers
        String trimmed = before.replaceAll("\\s+", " ").trim();
        if (trimmed.endsWith("union")) return "union";

        return null;
    }

    /**
     * Find the matching '}' for a '{' at the given position.
     */
    private int findMatchingClose(String text, int openPos) {
        int depth = 0;
        boolean inString = false;
        boolean inComment = false;

        for (int i = openPos; i < text.length(); i++) {
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
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    // ── Ordinal collection ──────────────────────────────────────────────────────

    /**
     * Collect all @N ordinals within [scopeStart, scopeEnd] where N > threshold.
     *
     * Skips ordinals inside nested struct/enum/interface blocks (they have
     * independent ordinal spaces). Includes ordinals inside union/group
     * (they share the parent struct's ordinal space).
     */
    private List<OrdinalHit> collectOrdinalsInScope(String text, int scopeStart,
                                                     int scopeEnd, int threshold) {
        List<OrdinalHit> hits = new ArrayList<>();

        // Track which ranges to skip (nested struct/enum/interface bodies)
        List<int[]> skipRanges = findNestedIndependentBlocks(text, scopeStart, scopeEnd);

        // Scan for all @N in the scope
        Matcher m = ORDINAL_PATTERN.matcher(text);
        int searchStart = scopeStart + 1; // Skip the opening '{'

        while (m.find(searchStart)) {
            if (m.start() >= scopeEnd) break;

            int ordinalValue = Integer.parseInt(m.group(1));
            int matchStart = m.start();

            // Skip if inside a nested struct/enum/interface
            if (isInSkipRange(matchStart, skipRanges)) {
                searchStart = m.end();
                continue;
            }

            // Check this isn't a hex ID (@0x...)
            // ORDINAL_PATTERN only matches @digits, but @0 followed by x would match @0
            // The regex already handles this — @0x would match @0 then 'x' is after
            // Double-check: if the character after the match is 'x' or 'X', skip
            int afterMatch = m.end();
            if (afterMatch < text.length() &&
                    (text.charAt(afterMatch) == 'x' || text.charAt(afterMatch) == 'X')) {
                searchStart = m.end();
                continue;
            }

            if (ordinalValue > threshold) {
                hits.add(new OrdinalHit(matchStart, m.end() - matchStart, ordinalValue));
            }

            searchStart = m.end();
        }

        return hits;
    }

    /**
     * Find all nested struct/enum/interface block ranges that have independent
     * ordinal spaces. These ranges will be skipped during ordinal shifting.
     *
     * union {} and group {} are NOT included — they share the parent's ordinals.
     */
    private List<int[]> findNestedIndependentBlocks(String text, int scopeStart, int scopeEnd) {
        List<int[]> ranges = new ArrayList<>();

        // We need to find nested struct/enum/interface definitions
        // Pattern: (struct|enum|interface) Name ... {
        Pattern nestedDef = Pattern.compile(
                "\\b(struct|enum|interface)\\s+[A-Z][A-Za-z0-9]*\\s*(?:\\([^)]*\\))?\\s*(?:@0x[a-fA-F0-9]+)?\\s*\\{"
        );

        int depth = 0;
        boolean inString = false;
        boolean inComment = false;

        // First pass: find the depth-1 struct/enum/interface blocks
        // (depth 0 = the scope itself, depth 1 = direct children)
        Matcher m = nestedDef.matcher(text);
        int searchFrom = scopeStart + 1;

        while (m.find(searchFrom)) {
            int defStart = m.start();
            if (defStart >= scopeEnd) break;

            // Verify this isn't inside a string or comment
            if (isInStringOrComment(text, scopeStart, defStart)) {
                searchFrom = m.end();
                continue;
            }

            // Find the '{' in this match
            int bracePos = text.indexOf('{', m.start());
            if (bracePos < 0 || bracePos >= scopeEnd) {
                searchFrom = m.end();
                continue;
            }

            // Find its matching '}'
            int closePos = findMatchingClose(text, bracePos);
            if (closePos > 0 && closePos <= scopeEnd) {
                ranges.add(new int[]{bracePos, closePos});
                searchFrom = closePos + 1;
            } else {
                searchFrom = m.end();
            }
        }

        return ranges;
    }

    /**
     * Check if a position falls within any of the skip ranges.
     */
    private boolean isInSkipRange(int pos, List<int[]> ranges) {
        for (int[] range : ranges) {
            if (pos > range[0] && pos < range[1]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Simple check if a position is inside a string or comment.
     */
    private boolean isInStringOrComment(String text, int from, int pos) {
        boolean inString = false;
        boolean inComment = false;

        for (int i = from; i < pos && i < text.length(); i++) {
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
            if (c == '#') inComment = true;
            else if (c == '"') inString = true;
        }

        return inString || inComment;
    }

    // ── Data class ──────────────────────────────────────────────────────────────

    private static class OrdinalHit {
        final int offset;   // Position of '@' in the document
        final int length;   // Length of "@N" text
        final int value;    // The ordinal number N

        OrdinalHit(int offset, int length, int value) {
            this.offset = offset;
            this.length = length;
            this.value = value;
        }
    }
}
