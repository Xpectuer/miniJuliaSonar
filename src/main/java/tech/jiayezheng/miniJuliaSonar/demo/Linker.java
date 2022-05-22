package tech.jiayezheng.miniJuliaSonar.demo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.jiayezheng.miniJuliaSonar.*;
import tech.jiayezheng.miniJuliaSonar.ast.Node;
import tech.jiayezheng.miniJuliaSonar.type.ModuleType;
import tech.jiayezheng.miniJuliaSonar.type.Type;
import tech.jiayezheng.miniJuliaSonar.type.UnionType;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Linker {
    private static final Pattern CONSTANT = Pattern.compile("[A-Z_][A-Z0-9_]*");

    // Map of file-path to semantic styles & links for that path.
    @NotNull
    private Map<String, List<Style>> fileStyles = new HashMap<>();

    private File outDir;
    private String rootPath;

    Set<Object> seenDef = new HashSet<>();
    Set<Object> seenRef = new HashSet<>();

    /**
     * @param root   input root work dir
     * @param outDir out dir
     */
    public Linker(String root, File outDir) {
        this.rootPath = root;
        this.outDir = outDir;
    }

    public void findLinks(@NotNull Analyzer analyzer) {
        $.msg("Add xref links");
        Progress progress = new Progress(analyzer.getAllBindings().size(), 50);
        List<Binding> linkBindings = new ArrayList<>();

        for (Binding binding : analyzer.getAllBindings()) {
            if (binding.kind != Binding.Kind.MODULE) {
                linkBindings.add(binding);
            }
        }

        for (List<Binding> bs : $.correlateBindings(linkBindings)) {
            progressDef(bs);
            progress.tick();
        }

        // hl defs
        $.msg("\nAdding ref links");
        progress = new Progress(analyzer.references.size(), 50);

        for (Node node : analyzer.references.keys()) {
            if (Analyzer.self.hasOption("debug")) {
                progressRefDebug(node, analyzer.references.get(node));
            } else {
                progressRef(node, analyzer.references.get(node));
            }
            progress.tick();
        }

        if(Analyzer.self.hasOption("report")) {
            for(Diagnostic d: analyzer.semanticErrors.values()) {
                processDiagnostic(d);
            }
        }

    }

    private void processDiagnostic(@NotNull Diagnostic d) {
        Style style = new Style(Style.Type.WARNING, d.start, d.end);
        style.message = d.msg;
        style.url = d.file;
        addFileStyle(d.file, style);
    }

    private void progressRef(Node ref, List<Binding> bindings) {
        String qname = bindings.iterator().next().qname;
        int hash = ref.hashCode();

        if (!seenRef.contains(hash)) {
            seenRef.add(hash);

            Style link = new Style(Style.Type.LINK, ref.start, ref.end);
            link.id = qname;

            List<Type> types = bindings.stream().map(b -> b.type).collect(Collectors.toList());
            link.message = UnionType.union(types).toString();


            // todo: hover menu.
            String path = ref.file;
            if (path != null) {
                for (Binding b : bindings) {
                    if (link.url == null) {
                        link.url = toURL(b, path);
                    }

                    if (link.url != null) {
                        addFileStyle(path, link);
                        break;
                    }
                }
            }

        }


    }

    private void progressRefDebug(Node ref, List<Binding> bindings) {
        int hash = ref.hashCode();

        if (!seenRef.contains(hash)) {
            seenRef.add(hash);

            Style link = new Style(Style.Type.LINK, ref.start, ref.end);
            link.id = Integer.toString(Math.abs(hash));

            List<String> typings = new ArrayList<>();
            for (Binding b : bindings) {
                typings.add(b.type.toString());
            }

            link.message = $.joinWithSep(typings, " | ", "{", "}");

            link.highlight = new ArrayList<>();
            for (Binding b : bindings) {
                link.highlight.add(Integer.toString(Math.abs(b.hashCode())));
            }

            // todo: hover menu
            String path = ref.file;
            if (path != null) {
                for (Binding b : bindings) {
                    if (link.url == null) {
                        link.url = toURL(b, path);
                    }
                }
            }

        }
    }

    private void progressDef(List<Binding> bindings) {
        Binding first = bindings.get(0);
        String qname = first.qname;

        if (first.isURL() || first.start < 0) {
            return;
        }

        List<Type> types = bindings.stream().map(b -> b.type).collect(Collectors.toList());
        Style style = new Style(Style.Type.ANCHOR, first.start, first.end);
        style.message = UnionType.union(types).toString();
        style.url = first.qname;
        style.id = qname;
        addFileStyle(first.getFile(), style);
    }

    private void processDefDebug(@NotNull Binding binding) {
        int hash = binding.hashCode();

        if (binding.isURL() || binding.start < 0 || seenDef.contains(hash)) {
            return;
        }

        seenDef.add(hash);
        Style style = new Style(Style.Type.ANCHOR, binding.start, binding.end);
        style.message = binding.type.toString();
        style.url = binding.qname;
        style.id = "" + Math.abs(binding.hashCode());

        Set<Node> refs = binding.refs;
        style.highlight = new ArrayList<>();


        for (Node r : refs) {
            style.highlight.add(Integer.toString(Math.abs(r.hashCode())));
        }
        addFileStyle(binding.getFile(), style);
    }


    private List<Style> stylesForFile(String path) {
        List<Style> styles = fileStyles.get(path);
        if (styles == null) {
            styles = new ArrayList<>();
            fileStyles.put(path, styles);
        }
        return styles;
    }

    private void addFileStyle(String path, Style style) {
        stylesForFile(path).add(style);
    }

    @Nullable
    private String toURL(@NotNull Binding binding, String filename) {

        if (binding.isBuiltin()) {
            return binding.getURL();
        }

        String destPath;
        if (binding.type instanceof ModuleType) {
            destPath = binding.type.asModuleType().file;
        } else {
            destPath = binding.getFile();
        }

        if (destPath == null) {
            return null;
        }

        String anchor = "#" + binding.qname;
        if (binding.getFirstFile().equals(filename)) {
            return anchor;
        }

        if (destPath.startsWith(rootPath)) {
            String relpath;
            if (filename != null) {
                relpath = $.relPath(filename, destPath);
            } else {
                relpath = destPath;
            }

            if (relpath != null) {
                return relpath + ".html" + anchor;
            } else {
                return anchor;
            }
        } else {
            return "file://" + destPath + anchor;
        }
    }


    public List<Style> getStyles(String path) {
        return stylesForFile(path);
    }
}
