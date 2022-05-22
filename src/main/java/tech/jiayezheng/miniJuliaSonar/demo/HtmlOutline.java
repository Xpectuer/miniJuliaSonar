package tech.jiayezheng.miniJuliaSonar.demo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.jiayezheng.miniJuliaSonar.Analyzer;
import tech.jiayezheng.miniJuliaSonar.Outliner;

import java.util.List;


public class HtmlOutline {
    private Analyzer analyzer;

    @Nullable
    private StringBuilder buffer;

    public HtmlOutline(Analyzer idx) {
        this.analyzer = idx;
    }

    @NotNull
    public String generate(String path) {
        buffer = new StringBuilder(1024);
        List<Outliner.Entry> entries = generateOutline(analyzer, path);
        addOutline(entries);
        String html = buffer.toString();
        buffer = null;
        return html;
    }

    @NotNull
    public List<Outliner.Entry> generateOutline(Analyzer analyzer, @NotNull String file) {
        return new Outliner().generate(analyzer, file);
    }

    private void addOutline(@NotNull List<Outliner.Entry> entries) {
        add("<ul>\n");
        for (Outliner.Entry e : entries) {
            addEntry(e);
        }
        add("</ul>\n");
    }

    private void addEntry(@NotNull Outliner.Entry e) {
        add("<li>");

        String style = null;
        switch (e.kind) {
            case FUNCTION:
            case METHOD:
                style = "function";
                break;
            case STRUCT:
                style = "type-name";
                break;
            case PARAMETER:
                style = "parameter";
                break;
            case VARIABLE:
            case SCOPE:
                style = "identifier";
                break;
        }

        add("<a href='#");
        add(e.getQname());
        add("', xid='" + e.getQname() + "'>");
        add(e.getName());
        add("</a>");

        if (e.isBranch()) {
            addOutline(e.getChildren());
        }
        add("</li>");
    }



    private void add(String text) {
        buffer.append(text);
    }
}
