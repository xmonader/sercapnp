package com.sercapnp.lang;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Auto-completion for Cap'n Proto schema files.
 *
 * Context detection (by scanning backwards from caret):
 *   ':' → built-in types + user/imported types
 *   '=' → constant values
 *   '$' → annotation names
 *   top-level → declaration keywords
 *   inside {} → body-level keywords
 *   extends( → interface types
 *   annotation name( → target list
 *   List( / generic( → types
 */
public class CapnpCompletionContributor extends CompletionContributor {

    private static final String[] BUILTIN_TYPES = {
            "Void", "Bool",
            "Int8", "Int16", "Int32", "Int64",
            "UInt8", "UInt16", "UInt32", "UInt64",
            "Float32", "Float64",
            "Text", "Data",
            "List", "AnyPointer"
    };

    private static final String[] TOP_LEVEL_KEYWORDS = {
            "struct", "enum", "interface", "const", "using", "import", "annotation"
    };

    private static final String[] BODY_KEYWORDS = {
            "union", "group", "struct", "enum", "interface", "const", "using"
    };

    private static final String[] ANNOTATION_TARGETS = {
            "struct", "field", "union", "group", "enum", "enumerant",
            "interface", "method", "param", "annotation", "const", "file", "*"
    };

    private static final String[] CONSTANT_VALUES = {
            "true", "false", "void", "inf", "nan"
    };

    public CapnpCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(CapnpLanguage.INSTANCE),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        doAddCompletions(parameters, result);
                    }
                }
        );
    }

    private void doAddCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull CompletionResultSet result) {
        PsiFile file = parameters.getOriginalFile();
        int offset = parameters.getOffset();
        String fileText = file.getText();

        ContextKind ctx = analyzeContext(fileText, offset);

        switch (ctx) {
            case TYPE_POSITION:
                addBuiltinTypes(result);
                addUserDefinedTypes(result, file);
                break;
            case TOP_LEVEL:
                addKeywords(result, TOP_LEVEL_KEYWORDS, "keyword");
                break;
            case BODY_LEVEL:
                addKeywords(result, BODY_KEYWORDS, "keyword");
                break;
            case ANNOTATION_TARGET:
                addKeywords(result, ANNOTATION_TARGETS, "target");
                break;
            case VALUE_POSITION:
                addKeywords(result, CONSTANT_VALUES, "constant");
                addUserDefinedConstants(result, file);
                break;
            case ANNOTATION_REF:
                addAnnotationNames(result, file);
                break;
            case EXTENDS_TYPE:
                addBuiltinTypes(result);
                addUserDefinedTypes(result, file);
                break;
            default:
                addBuiltinTypes(result);
                addKeywords(result, TOP_LEVEL_KEYWORDS, "keyword");
                addUserDefinedTypes(result, file);
                break;
        }
    }

    // ── Context detection ───────────────────────────────────────────────────────

    private enum ContextKind {
        TYPE_POSITION, TOP_LEVEL, BODY_LEVEL, ANNOTATION_TARGET,
        VALUE_POSITION, ANNOTATION_REF, EXTENDS_TYPE, GENERIC
    }

    private ContextKind analyzeContext(String text, int offset) {
        int pos = Math.min(offset, text.length()) - 1;

        // Skip whitespace and partial identifier backwards
        // First skip the partial identifier the user is currently typing
        while (pos >= 0 && (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '_')) {
            pos--;
        }
        // Then skip whitespace
        while (pos >= 0 && Character.isWhitespace(text.charAt(pos))) {
            pos--;
        }

        if (pos < 0) return ContextKind.TOP_LEVEL;

        char prevChar = text.charAt(pos);

        // After ':' → type position
        if (prevChar == ':') return ContextKind.TYPE_POSITION;

        // After '=' → value position
        if (prevChar == '=') return ContextKind.VALUE_POSITION;

        // After '$' → annotation reference
        if (prevChar == '$') return ContextKind.ANNOTATION_REF;

        // After '(' or ',' → check what kind of context
        if (prevChar == '(' || prevChar == ',') {
            String wordBefore = getWordBefore(text, pos - 1);
            if ("extends".equals(wordBefore)) return ContextKind.EXTENDS_TYPE;
            if ("List".equals(wordBefore)) return ContextKind.TYPE_POSITION;

            // Check if inside annotation target list
            if (prevChar == '(' && isAfterAnnotationDecl(text, pos)) {
                return ContextKind.ANNOTATION_TARGET;
            }
            if (prevChar == ',' && isInAnnotationTargetList(text, pos)) {
                return ContextKind.ANNOTATION_TARGET;
            }

            // If after '(' preceded by a capitalized name → likely generic param → type
            if (wordBefore.length() > 0 && Character.isUpperCase(wordBefore.charAt(0))) {
                return ContextKind.TYPE_POSITION;
            }
        }

        int braceDepth = countBraceDepth(text, offset);

        // After ';', '{', '}' → depends on depth
        if (prevChar == ';' || prevChar == '{' || prevChar == '}') {
            return braceDepth == 0 ? ContextKind.TOP_LEVEL : ContextKind.BODY_LEVEL;
        }

        // Line start
        if (isAtLineStart(text, pos)) {
            return braceDepth == 0 ? ContextKind.TOP_LEVEL : ContextKind.BODY_LEVEL;
        }

        // If inside braces, check if there's a preceding ':'
        if (braceDepth > 0 && hasPrecedingColonOnStatement(text, pos)) {
            return ContextKind.TYPE_POSITION;
        }

        return braceDepth > 0 ? ContextKind.BODY_LEVEL : ContextKind.GENERIC;
    }

    // ── Completion providers ────────────────────────────────────────────────────

    private void addBuiltinTypes(@NotNull CompletionResultSet result) {
        for (String type : BUILTIN_TYPES) {
            LookupElementBuilder el = LookupElementBuilder.create(type)
                    .withTypeText("built-in", true)
                    .withBoldness(true);

            if ("List".equals(type)) {
                el = el.withTailText("(T)", true)
                        .withInsertHandler((ctx, item) -> {
                            ctx.getEditor().getDocument().insertString(ctx.getTailOffset(), "()");
                            ctx.getEditor().getCaretModel().moveToOffset(ctx.getTailOffset() - 1);
                        });
            }

            result.addElement(PrioritizedLookupElement.withPriority(el, 100));
        }
    }

    private void addUserDefinedTypes(@NotNull CompletionResultSet result, PsiFile file) {
        Set<String> types = CapnpSchemaScanner.collectVisibleTypes(file);
        for (String type : types) {
            // Skip built-in type names to avoid duplicates
            boolean isBuiltin = false;
            for (String bt : BUILTIN_TYPES) {
                if (bt.equals(type)) { isBuiltin = true; break; }
            }
            if (isBuiltin) continue;

            LookupElementBuilder el = LookupElementBuilder.create(type)
                    .withTypeText("type", true);
            result.addElement(PrioritizedLookupElement.withPriority(el, 80));
        }
    }

    private void addKeywords(@NotNull CompletionResultSet result, String[] keywords, String typeText) {
        for (String kw : keywords) {
            LookupElementBuilder el = LookupElementBuilder.create(kw)
                    .withTypeText(typeText, true)
                    .withBoldness(true);

            if ("struct".equals(kw) || "enum".equals(kw) || "interface".equals(kw)) {
                el = el.withInsertHandler((ctx, item) -> {
                    ctx.getEditor().getDocument().insertString(ctx.getTailOffset(), " ");
                    ctx.getEditor().getCaretModel().moveToOffset(ctx.getTailOffset());
                });
            } else if ("import".equals(kw)) {
                el = el.withInsertHandler((ctx, item) -> {
                    ctx.getEditor().getDocument().insertString(ctx.getTailOffset(), " \"\";");
                    ctx.getEditor().getCaretModel().moveToOffset(ctx.getTailOffset() - 2);
                });
            } else if ("const".equals(kw)) {
                el = el.withInsertHandler((ctx, item) -> {
                    ctx.getEditor().getDocument().insertString(ctx.getTailOffset(), " ");
                    ctx.getEditor().getCaretModel().moveToOffset(ctx.getTailOffset());
                });
            }

            result.addElement(PrioritizedLookupElement.withPriority(el, 90));
        }
    }

    private void addUserDefinedConstants(@NotNull CompletionResultSet result, PsiFile file) {
        CapnpSchemaScanner.ScanResult scan = CapnpSchemaScanner.scan(file.getText());
        for (String c : scan.constants) {
            LookupElementBuilder el = LookupElementBuilder.create("." + c)
                    .withPresentableText("." + c)
                    .withTypeText("const", true);
            result.addElement(PrioritizedLookupElement.withPriority(el, 70));
        }
    }

    private void addAnnotationNames(@NotNull CompletionResultSet result, PsiFile file) {
        Set<String> annots = CapnpSchemaScanner.collectVisibleAnnotations(file);
        for (String name : annots) {
            LookupElementBuilder el = LookupElementBuilder.create(name)
                    .withTypeText("annotation", true);
            result.addElement(PrioritizedLookupElement.withPriority(el, 85));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

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

    private String getWordBefore(String text, int pos) {
        while (pos >= 0 && Character.isWhitespace(text.charAt(pos))) pos--;
        if (pos < 0) return "";
        int end = pos + 1;
        while (pos >= 0 && Character.isLetterOrDigit(text.charAt(pos))) pos--;
        return text.substring(pos + 1, end);
    }

    private boolean isAtLineStart(String text, int pos) {
        while (pos >= 0 && (text.charAt(pos) == ' ' || text.charAt(pos) == '\t')) pos--;
        return pos < 0 || text.charAt(pos) == '\n';
    }

    /**
     * Check if position is right after 'annotation Name (' pattern.
     */
    private boolean isAfterAnnotationDecl(String text, int parenPos) {
        // Go back from '(' to find the pattern: annotation <name> <optional-typeId>
        String before = "";
        int lineStart = text.lastIndexOf('\n', parenPos - 1) + 1;
        if (parenPos > lineStart) {
            before = text.substring(lineStart, parenPos).trim();
        }
        return before.matches(".*annotation\\s+[a-zA-Z][a-zA-Z0-9]*\\s*(?:@0x[a-fA-F0-9]+\\s*)?");
    }

    /**
     * Check if we're inside an annotation target list by finding unmatched '('.
     */
    private boolean isInAnnotationTargetList(String text, int pos) {
        int parenDepth = 0;
        for (int i = pos; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == ')') parenDepth++;
            else if (c == '(') {
                if (parenDepth == 0) return isAfterAnnotationDecl(text, i);
                parenDepth--;
            }
            else if (c == '{' || c == '}') break;
        }
        return false;
    }

    /**
     * Check if there's a ':' on the current statement (between last ';'/'{' and pos).
     */
    private boolean hasPrecedingColonOnStatement(String text, int pos) {
        for (int i = pos; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == ':') return true;
            if (c == ';' || c == '{' || c == '}') return false;
        }
        return false;
    }
}
