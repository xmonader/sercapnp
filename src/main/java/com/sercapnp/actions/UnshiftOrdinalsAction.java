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
 * Shifts ordinals up (decrements) to close a gap left by a removed field.
 *
 * Usage:
 *   1. Remove the field (e.g., delete the line with @5)
 *   2. Place cursor on the first line AFTER the gap (the line with @6)
 *   3. Invoke via Ctrl+Alt+A, U (or menu: Tools → Shift Ordinals Up)
 *   4. All ordinals >= 6 within the same struct scope are decremented by 1
 *   5. Result: @6→@5, @7→@6, ..., @45→@44
 *
 * Scope rules (per Cap'n Proto spec):
 *   - union {} and group {} share the parent struct's ordinal space → INCLUDED
 *   - Nested struct/enum/interface have their OWN ordinal space → SKIPPED
 */
public class UnshiftOrdinalsAction extends AnAction {

    private static final Pattern ORDINAL_PATTERN = Pattern.compile("@(\\d+)");

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
                    "Place your cursor on the first line after the removed field.",
                    "Shift Ordinals Up");
            return;
        }

        int threshold = Integer.parseInt(lineMatcher.group(1));

        if (threshold == 0) {
            Messages.showInfoMessage(project,
                    "Cannot shift @0 — it is already the first ordinal.",
                    "Shift Ordinals Up");
            return;
        }

        // 2. Find the enclosing struct scope
        int[] scope = findEnclosingStructScope(text, caretOffset);
        if (scope == null) {
            Messages.showInfoMessage(project,
                    "Could not find an enclosing struct scope.",
                    "Shift Ordinals Up");
            return;
        }

        int scopeStart = scope[0];
        int scopeEnd = scope[1];

        // 3. Collect ordinals >= threshold that need shifting
        List<OrdinalHit> hits = collectOrdinalsInScope(text, scopeStart, scopeEnd, threshold);

        if (hits.isEmpty()) {
            Messages.showInfoMessage(project,
                    "No ordinals found at or after @" + threshold + " to shift.",
                    "Shift Ordinals Up");
            return;
        }

        // 4. Apply replacements from end to start
        Collections.sort(hits, (a, b) -> Integer.compare(b.offset, a.offset));

        com.intellij.openapi.vfs.VirtualFile vFile =
                com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(document);

        WriteCommandAction.runWriteCommandAction(project, "Shift Ordinals Up", null, () -> {
            for (OrdinalHit hit : hits) {
                String replacement = "@" + (hit.value - 1);
                document.replaceString(hit.offset, hit.offset + hit.length, replacement);
            }
        }, vFile != null ?
                com.intellij.psi.PsiManager.getInstance(project).findFile(vFile) :
                null);

        com.intellij.openapi.editor.EditorModificationUtil.scrollToCaret(editor);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
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

    // ── Scope detection (same logic as ShiftOrdinalsAction) ──────────────────────

    private int[] findEnclosingStructScope(String text, int caretOffset) {
        int depth = 0;
        boolean inString = false;
        boolean inComment = false;

        for (int i = caretOffset - 1; i >= 0; i--) {
            char c = text.charAt(i);

            if (c == '\n') { inComment = false; continue; }
            if (inString) {
                if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) inString = false;
                continue;
            }
            if (c == '#') { inComment = true; continue; }
            if (inComment) continue;
            if (c == '"') { inString = true; continue; }

            if (c == '}') {
                depth++;
            } else if (c == '{') {
                if (depth == 0) {
                    String blockType = getBlockType(text, i);
                    if ("struct".equals(blockType) || "enum".equals(blockType)
                            || "interface".equals(blockType)) {
                        int closePos = findMatchingClose(text, i);
                        if (closePos >= 0) return new int[]{i, closePos};
                        return null;
                    }
                    continue; // union/group — keep going up
                }
                depth--;
            }
        }
        return null;
    }

    private String getBlockType(String text, int bracePos) {
        String before = text.substring(Math.max(0, bracePos - 200), bracePos).trim();
        Matcher m = BLOCK_KEYWORD_PATTERN.matcher(before);
        if (m.find()) return m.group(1);
        if (before.endsWith("union")) return "union";
        if (before.endsWith("group")) return "group";
        return null;
    }

    private int findMatchingClose(String text, int openPos) {
        int depth = 0;
        boolean inString = false;
        boolean inComment = false;

        for (int i = openPos; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inComment) { if (c == '\n') inComment = false; continue; }
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
                continue;
            }
            if (c == '#') { inComment = true; continue; }
            if (c == '"') { inString = true; continue; }
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    // ── Ordinal collection ──────────────────────────────────────────────────────

    /**
     * Collect all @N ordinals within scope where N >= threshold.
     * Skips nested struct/enum/interface blocks.
     */
    private List<OrdinalHit> collectOrdinalsInScope(String text, int scopeStart,
                                                     int scopeEnd, int threshold) {
        List<OrdinalHit> hits = new ArrayList<>();
        List<int[]> skipRanges = findNestedIndependentBlocks(text, scopeStart, scopeEnd);

        Matcher m = ORDINAL_PATTERN.matcher(text);
        int searchStart = scopeStart + 1;

        while (m.find(searchStart)) {
            if (m.start() >= scopeEnd) break;

            int ordinalValue = Integer.parseInt(m.group(1));
            int matchStart = m.start();

            if (isInSkipRange(matchStart, skipRanges)) {
                searchStart = m.end();
                continue;
            }

            // Skip hex IDs
            int afterMatch = m.end();
            if (afterMatch < text.length() &&
                    (text.charAt(afterMatch) == 'x' || text.charAt(afterMatch) == 'X')) {
                searchStart = m.end();
                continue;
            }

            if (ordinalValue >= threshold) {
                hits.add(new OrdinalHit(matchStart, m.end() - matchStart, ordinalValue));
            }

            searchStart = m.end();
        }
        return hits;
    }

    private List<int[]> findNestedIndependentBlocks(String text, int scopeStart, int scopeEnd) {
        List<int[]> ranges = new ArrayList<>();
        Pattern nestedDef = Pattern.compile(
                "\\b(struct|enum|interface)\\s+[A-Z][A-Za-z0-9]*\\s*(?:\\([^)]*\\))?\\s*(?:@0x[a-fA-F0-9]+)?\\s*\\{"
        );
        Matcher m = nestedDef.matcher(text);
        int searchFrom = scopeStart + 1;

        while (m.find(searchFrom)) {
            int defStart = m.start();
            if (defStart >= scopeEnd) break;
            if (isInStringOrComment(text, scopeStart, defStart)) {
                searchFrom = m.end();
                continue;
            }
            int bracePos = text.indexOf('{', m.start());
            if (bracePos < 0 || bracePos >= scopeEnd) { searchFrom = m.end(); continue; }
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

    private boolean isInSkipRange(int pos, List<int[]> ranges) {
        for (int[] range : ranges) {
            if (pos > range[0] && pos < range[1]) return true;
        }
        return false;
    }

    private boolean isInStringOrComment(String text, int from, int pos) {
        boolean inString = false;
        boolean inComment = false;
        for (int i = from; i < pos && i < text.length(); i++) {
            char c = text.charAt(i);
            if (inComment) { if (c == '\n') inComment = false; continue; }
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

    private static class OrdinalHit {
        final int offset;
        final int length;
        final int value;
        OrdinalHit(int offset, int length, int value) {
            this.offset = offset;
            this.length = length;
            this.value = value;
        }
    }
}
