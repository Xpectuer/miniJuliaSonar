package tech.jiayezheng.miniJuliaSonar.demo;

import tech.jiayezheng.miniJuliaSonar.$;
import tech.jiayezheng.miniJuliaSonar.Analyzer;
import tech.jiayezheng.miniJuliaSonar.Options;
import tech.jiayezheng.miniJuliaSonar.Progress;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Demo {

    private static File OUTPUT_DIR;

    private static final String CSS = $.readResource("tech/jiayezheng/miniJuliaSonar/css/demo.css");
    private static final String JS = $.readResource("tech/jiayezheng/miniJuliaSonar/css/highlight.js");
    private static final String JS_DEBUG = $.readResource("tech/jiayezheng/miniJuliaSonar/css/highlight-debug.js");

    private Analyzer analyzer;
    private String rootPath;
    private Linker linker;

    private void makeOutputDir() {
        if (!OUTPUT_DIR.exists()) {
            OUTPUT_DIR.mkdirs();
            $.msg("Created directory: " + OUTPUT_DIR.getAbsolutePath());
        }
    }



    private void start(String fileOrDir, Map<String, Object> options) {
        File f = new File(fileOrDir);
        File rootDir = f.isFile() ? f.getParentFile() : f;
        try
        {
            rootPath = $.unifyPath(rootDir);
        }
        catch (Exception e)
        {
            $.die("File not found: " + f);
        }

        analyzer = new Analyzer(options);
        $.msg("Loading and analyzing files");
        try
        {
            analyzer.analyze(f.getPath());
        }
        finally
        {
            analyzer.finish();
        }

        generateHtml();

    }

    private void generateHtml() {

        $.msg("\nGenerating HTML");
        makeOutputDir();

//        linker = new Linker(rootPath, OUTPUT_DIR);
//        linker.findLinks(analyzer);

        int rootLength = rootPath.length();

        int total = 0;
        for (String path : analyzer.getLoadedFiles()) {
            if (path.startsWith(rootPath)) {
                total++;
            }
        }

        Progress progress = new Progress(total, 50);

        for (String path : analyzer.getLoadedFiles()) {
            if (path.startsWith(rootPath)) {
                progress.tick();
                File destFile = $.joinPath(OUTPUT_DIR, path.substring(rootLength));
                destFile.getParentFile().mkdirs();
                String destPath = destFile.getAbsolutePath() + ".html";
                String html = markup(path);
                try {
                    $.writeFile(destPath, html);
                } catch (Exception e) {
                    $.msg("Failed to write: " + destPath);
                }
            }
        }

        $.msg("\nWrote " + analyzer.getLoadedFiles().size() + " files to " + OUTPUT_DIR);
    }

    private String markup(String path) {
        String source;

        try {
            source = $.readFile(path);
        } catch(Exception e) {
            $.die("Failed to read file: "+ path);
            return "";
        }


        List<Style> styles = new ArrayList<>();
        return "";
    }


    // entry
    public static void main(String[] args) {
        Options options = new Options(args);

        List<String> argsList = options.getArgs();
        String fileOrDir = argsList.get(0);
        OUTPUT_DIR = new File(argsList.get(1));

        new Demo().start(fileOrDir, options.getOptionsMap());
        $.msg($.getGCStats());
    }
}
