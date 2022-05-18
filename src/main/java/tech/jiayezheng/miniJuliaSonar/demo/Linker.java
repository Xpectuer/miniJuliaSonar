package tech.jiayezheng.miniJuliaSonar.demo;

import org.jetbrains.annotations.NotNull;
import tech.jiayezheng.miniJuliaSonar.$;
import tech.jiayezheng.miniJuliaSonar.Analyzer;
import tech.jiayezheng.miniJuliaSonar.Progress;
import tech.jiayezheng.miniJuliaSonar.ast.Str;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class Linker {
    private static final Pattern CONSTANT = Pattern.compile("[A-Z_][A-Z0-9_]*");

    // Map of file-path to semantic styles & links for that path.
    @NotNull
    private Map<String, List<Style>> fileStyles = new HashMap<>();
    private File outDir;
    private String rootPath;

    Set<Object> seenRef = new HashSet<>();
    Set<Object> seenDef = new HashSet<>();

    public Linker(String root, File outputDir) {
        rootPath = root;
        outDir = outputDir;
    }

    public void findLinks(@NotNull Analyzer analyzer) {
        $.msg("Adding xref links");

//        Progress progress = new Progress(analyzer.getAllBindings().size(), 50);
//        List<Binding> linkBindings = new ArrayList<>();

    }

}
