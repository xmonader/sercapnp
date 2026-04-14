package com.sercapnp.lang;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans .capnp files to extract type definitions and resolve imports.
 * Works without PSI tree — uses regex on file text.
 */
public class CapnpSchemaScanner {

    private static final Logger LOG = Logger.getInstance(CapnpSchemaScanner.class);

    // ── Patterns ────────────────────────────────────────────────────────────────

    // Top-level type defs (0-4 spaces indent)
    private static final Pattern TYPE_DEF_PATTERN = Pattern.compile(
            "^\\s{0,4}(struct|enum|interface)\\s+([A-Z][A-Za-z0-9]*)\\b",
            Pattern.MULTILINE
    );

    // Nested type defs (2+ spaces or tab indent)
    private static final Pattern NESTED_TYPE_PATTERN = Pattern.compile(
            "^(?:\\s{2,}|\\t+)(struct|enum|interface)\\s+([A-Z][A-Za-z0-9]*)\\b",
            Pattern.MULTILINE
    );

    private static final Pattern CONST_DEF_PATTERN = Pattern.compile(
            "^\\s*const\\s+([a-zA-Z][A-Za-z0-9]*)\\s*:",
            Pattern.MULTILINE
    );

    private static final Pattern ANNOTATION_DEF_PATTERN = Pattern.compile(
            "^\\s*annotation\\s+([a-zA-Z][A-Za-z0-9]*)\\s",
            Pattern.MULTILINE
    );

    // import "path.capnp"
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "import\\s+\"([^\"]+)\"",
            Pattern.MULTILINE
    );

    // using Alias = import "path.capnp".Type
    private static final Pattern USING_IMPORT_PATTERN = Pattern.compile(
            "^\\s*using\\s+([A-Z][A-Za-z0-9]*)\\s*=\\s*import\\s+\"([^\"]+)\"(?:\\s*\\.\\s*([A-Z][A-Za-z0-9]*))?",
            Pattern.MULTILINE
    );

    // using import "path.capnp".Type;
    private static final Pattern USING_IMPORT_DIRECT_PATTERN = Pattern.compile(
            "^\\s*using\\s+import\\s+\"([^\"]+)\"\\s*\\.\\s*([A-Z][A-Za-z0-9]*)",
            Pattern.MULTILINE
    );

    // using Alias = Scope.Type
    private static final Pattern USING_ALIAS_PATTERN = Pattern.compile(
            "^\\s*using\\s+([A-Z][A-Za-z0-9]*)\\s*=\\s*([A-Z][A-Za-z0-9.]+)",
            Pattern.MULTILINE
    );

    // ── Data classes ────────────────────────────────────────────────────────────

    public static class ScanResult {
        public final List<TypeInfo> types = new ArrayList<>();
        public final List<String> constants = new ArrayList<>();
        public final List<String> annotations = new ArrayList<>();
        public final List<ImportInfo> imports = new ArrayList<>();
        public final List<AliasInfo> aliases = new ArrayList<>();

        public List<String> allTypeNames() {
            List<String> names = new ArrayList<>();
            for (TypeInfo t : types) names.add(t.name);
            return names;
        }
    }

    public static class TypeInfo {
        public final String kind;
        public final String name;
        public final boolean nested;

        public TypeInfo(String kind, String name, boolean nested) {
            this.kind = kind;
            this.name = name;
            this.nested = nested;
        }
    }

    public static class ImportInfo {
        public final String path;
        public final String alias;
        public final String specificType;

        public ImportInfo(String path, String alias, String specificType) {
            this.path = path;
            this.alias = alias;
            this.specificType = specificType;
        }
    }

    public static class AliasInfo {
        public final String alias;
        public final String target;

        public AliasInfo(String alias, String target) {
            this.alias = alias;
            this.target = target;
        }
    }

    // ── Scanning ────────────────────────────────────────────────────────────────

    public static ScanResult scan(String text) {
        ScanResult result = new ScanResult();
        if (text == null || text.isEmpty()) return result;

        String stripped = stripComments(text);
        Set<String> seenNames = new HashSet<>();

        // Top-level types
        Matcher m = TYPE_DEF_PATTERN.matcher(stripped);
        while (m.find()) {
            String name = m.group(2);
            if (seenNames.add(name)) {
                result.types.add(new TypeInfo(m.group(1), name, false));
            }
        }

        // Nested types
        m = NESTED_TYPE_PATTERN.matcher(stripped);
        while (m.find()) {
            String name = m.group(2);
            if (seenNames.add(name)) {
                result.types.add(new TypeInfo(m.group(1), name, true));
            }
        }

        // Constants
        m = CONST_DEF_PATTERN.matcher(stripped);
        while (m.find()) result.constants.add(m.group(1));

        // Annotations
        m = ANNOTATION_DEF_PATTERN.matcher(stripped);
        while (m.find()) result.annotations.add(m.group(1));

        // === Imports ===
        Set<String> seenImportPaths = new HashSet<>();

        // using Alias = import "path".Type
        m = USING_IMPORT_PATTERN.matcher(stripped);
        while (m.find()) {
            String path = m.group(2);
            seenImportPaths.add(path);
            result.imports.add(new ImportInfo(path, m.group(1), m.group(3)));
        }

        // using import "path".Type
        m = USING_IMPORT_DIRECT_PATTERN.matcher(stripped);
        while (m.find()) {
            String path = m.group(1);
            String typeName = m.group(2);
            if (seenImportPaths.add(path + "." + typeName)) {
                result.imports.add(new ImportInfo(path, typeName, typeName));
                result.aliases.add(new AliasInfo(typeName, typeName));
            }
        }

        // Plain import "path"
        m = IMPORT_PATTERN.matcher(stripped);
        while (m.find()) {
            String path = m.group(1);
            if (seenImportPaths.add(path)) {
                result.imports.add(new ImportInfo(path, null, null));
            }
        }

        // using Alias = Scope.Type (non-import)
        m = USING_ALIAS_PATTERN.matcher(stripped);
        while (m.find()) {
            String target = m.group(2);
            if (!target.contains("import")) {
                result.aliases.add(new AliasInfo(m.group(1), target));
            }
        }

        return result;
    }

    // ── Import Resolution (multi-strategy) ──────────────────────────────────────

    /**
     * Resolve an import path. Tries multiple strategies:
     * 1. Relative to the importing file's directory
     * 2. Relative to project content roots
     * 3. Relative to module source roots
     * 4. Filename-based search across all project .capnp files
     */
    public static VirtualFile resolveImport(VirtualFile importingFile, String importPath, Project project) {
        if (importPath == null || importPath.isEmpty() || project == null) return null;

        // Strategy 1: Relative to current file's directory
        if (importingFile != null) {
            VirtualFile dir = importingFile.getParent();
            if (dir != null) {
                VirtualFile found = dir.findFileByRelativePath(importPath);
                if (found != null && found.isValid()) return found;
            }
        }

        // Normalize path for remaining strategies
        String searchPath = importPath.startsWith("/") ? importPath.substring(1) : importPath;

        // Strategy 2: Relative to project content roots
        for (VirtualFile root : ProjectRootManager.getInstance(project).getContentRoots()) {
            VirtualFile found = root.findFileByRelativePath(searchPath);
            if (found != null && found.isValid()) return found;
        }

        // Strategy 3: Relative to module source roots
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            for (VirtualFile sourceRoot : ModuleRootManager.getInstance(module).getSourceRoots()) {
                VirtualFile found = sourceRoot.findFileByRelativePath(searchPath);
                if (found != null && found.isValid()) return found;
            }
        }

        // Strategy 4: Search all .capnp files by filename / path suffix
        String targetFilename = importPath;
        int lastSlash = targetFilename.lastIndexOf('/');
        if (lastSlash >= 0) targetFilename = targetFilename.substring(lastSlash + 1);

        Collection<VirtualFile> allFiles = FileTypeIndex.getFiles(
                CapnpFileType.INSTANCE,
                GlobalSearchScope.allScope(project)  // allScope includes libraries
        );

        // Try path suffix match first (more precise)
        for (VirtualFile file : allFiles) {
            if (file.getPath().endsWith(searchPath)) return file;
        }

        // Try filename match (less precise but catches more)
        String finalTarget = targetFilename;
        for (VirtualFile file : allFiles) {
            if (file.getName().equals(finalTarget)) return file;
        }

        LOG.debug("Could not resolve capnp import: " + importPath +
                " from " + (importingFile != null ? importingFile.getPath() : "null"));
        return null;
    }

    // ── High-level collection ───────────────────────────────────────────────────

    public static ScanResult scanFile(VirtualFile file, Project project) {
        if (file == null || !file.isValid() || project == null) return null;
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) return null;
        return scan(psiFile.getText());
    }

    /**
     * Collect all type names visible from the given file.
     * Resolves imports, follows one level deep.
     * Falls back to project-wide search if no imports resolve.
     */
    public static Set<String> collectVisibleTypes(PsiFile currentFile) {
        Set<String> types = new LinkedHashSet<>();
        if (currentFile == null) return types;

        Project project = currentFile.getProject();
        ScanResult current = scan(currentFile.getText());

        // 1. Current file types
        for (TypeInfo t : current.types) types.add(t.name);

        // 2. Aliases
        for (AliasInfo a : current.aliases) types.add(a.alias);

        // 3. Resolve imports
        VirtualFile vFile = currentFile.getVirtualFile();
        if (vFile == null && currentFile.getOriginalFile() != null) {
            vFile = currentFile.getOriginalFile().getVirtualFile();
        }

        boolean anyImportResolved = false;

        for (ImportInfo imp : current.imports) {
            VirtualFile imported = resolveImport(vFile, imp.path, project);
            if (imported == null) continue;

            anyImportResolved = true;
            ScanResult importedScan = scanFile(imported, project);
            if (importedScan == null) continue;

            if (imp.specificType != null) {
                // using import "...".SpecificType — only that type
                for (TypeInfo t : importedScan.types) {
                    if (t.name.equals(imp.specificType)) {
                        types.add(imp.alias != null ? imp.alias : t.name);
                        break;
                    }
                }
            } else {
                // Whole-file import — add all top-level types
                for (TypeInfo t : importedScan.types) {
                    if (!t.nested) types.add(t.name);
                }
                // If there's a module alias, add it too
                if (imp.alias != null) types.add(imp.alias);
            }
        }

        // 4. Fallback: if imports exist but none resolved → project-wide
        if (!current.imports.isEmpty() && !anyImportResolved) {
            types.addAll(collectProjectTypes(project));
        }

        return types;
    }

    /**
     * Collect all type names from ALL .capnp files in the project.
     */
    public static Set<String> collectProjectTypes(Project project) {
        Set<String> types = new LinkedHashSet<>();
        Collection<VirtualFile> files = FileTypeIndex.getFiles(
                CapnpFileType.INSTANCE,
                GlobalSearchScope.allScope(project)
        );
        for (VirtualFile file : files) {
            ScanResult scan = scanFile(file, project);
            if (scan != null) {
                for (TypeInfo t : scan.types) {
                    if (!t.nested) types.add(t.name);
                }
            }
        }
        return types;
    }

    /**
     * Collect all annotation names visible from the given file.
     */
    public static Set<String> collectVisibleAnnotations(PsiFile currentFile) {
        Set<String> annots = new LinkedHashSet<>();
        if (currentFile == null) return annots;

        Project project = currentFile.getProject();
        ScanResult current = scan(currentFile.getText());
        annots.addAll(current.annotations);

        VirtualFile vFile = currentFile.getVirtualFile();
        if (vFile == null && currentFile.getOriginalFile() != null) {
            vFile = currentFile.getOriginalFile().getVirtualFile();
        }

        for (ImportInfo imp : current.imports) {
            VirtualFile imported = resolveImport(vFile, imp.path, project);
            ScanResult importedScan = scanFile(imported, project);
            if (importedScan != null) annots.addAll(importedScan.annotations);
        }

        return annots;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private static String stripComments(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        boolean inString = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                sb.append(c);
                if (c == '\\' && i + 1 < text.length()) {
                    sb.append(text.charAt(++i));
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                sb.append(c);
            } else if (c == '#') {
                while (i < text.length() && text.charAt(i) != '\n') i++;
                if (i < text.length()) sb.append('\n');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static List<String> getEnumValues(String text, String enumName) {
        List<String> values = new ArrayList<>();
        Pattern p = Pattern.compile(
                "enum\\s+" + Pattern.quote(enumName) + "\\s*(?:@0x[a-fA-F0-9]+)?\\s*\\{([^}]*)\\}",
                Pattern.DOTALL);
        Matcher m = p.matcher(text);
        if (m.find()) {
            Matcher em = Pattern.compile("([a-zA-Z][a-zA-Z0-9]*)\\s+@\\d+").matcher(m.group(1));
            while (em.find()) values.add(em.group(1));
        }
        return values;
    }

    public static List<String> getStructFields(String text, String structName) {
        List<String> fields = new ArrayList<>();
        Pattern p = Pattern.compile(
                "struct\\s+" + Pattern.quote(structName) + "\\s*(?:\\([^)]*\\))?\\s*(?:@0x[a-fA-F0-9]+)?\\s*\\{([^}]*)\\}",
                Pattern.DOTALL);
        Matcher m = p.matcher(text);
        if (m.find()) {
            Matcher fm = Pattern.compile("([a-z][a-zA-Z0-9]*)\\s+@\\d+\\s*:").matcher(m.group(1));
            while (fm.find()) fields.add(fm.group(1));
        }
        return fields;
    }
}
