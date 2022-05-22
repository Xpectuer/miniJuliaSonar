package tech.jiayezheng.miniJuliaSonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.jiayezheng.miniJuliaSonar.ast.*;
import tech.jiayezheng.miniJuliaSonar.type.ModuleType;
import tech.jiayezheng.miniJuliaSonar.type.Type;
import tech.jiayezheng.miniJuliaSonar.type.UnionType;

import java.util.LinkedHashSet;
import java.util.Set;

public class Binding implements Comparable<Object> {



    public enum Kind {
        ATTRIBUTE,    // attr accessed with "." on some other object
        DATATYPE,        // struct definition
        STRUCT,
        FUNCTION,     // plain function
        METHOD,       // assignment
        MODULE,       // file
        PARAMETER,    // function param
        SCOPE,        // top-level variable ("scope" means we assume it can have attrs)
        VARIABLE      // local variable
    }

    private boolean isBuiltin = false;
    private boolean isSynthetic = false;
    @NotNull
    public String name; // unqualified name

    @NotNull
    public Node node;
    @NotNull
    public String qname; // qualified name
    public Type type;   // inferred type
    public Kind kind;   // scope level of name usage context


    public Set<Node> refs = new LinkedHashSet<>(1);

    // fields from ast node
    public int start = -1;
    public int end = -1;
    public int bodyStart = -1;
    public int bodyEnd = -1;

    @Nullable
    public String fileOrUrl;

    public Binding(@NotNull String id, @NotNull Node node, @NotNull Type type, @NotNull Kind kind) {
        this.name = id;
        this.qname = type.table.path;
        this.type = type;
        this.kind = kind;
        this.node = node;

        if (node instanceof Url) {
            String url = ((Url) node).url;
            if (url.startsWith("file://")) {
                fileOrUrl = url.substring("file://".length());
            } else {
                fileOrUrl = url;
            }
        } else {
            fileOrUrl = node.file;
            if (node instanceof Symbol) {
                name = ((Symbol) node).name;
            }
        }

        initLocationInfo(node);
        Analyzer.self.registerBinding(this);
    }

    private void initLocationInfo(Node node) {
        start = node.start;
        end = node.end;

        Node parent = node.parent;
        if ((parent instanceof FuncDef && ((FuncDef) parent).name == node) ||
                (parent instanceof StructDef && ((StructDef) parent).name == node)) {
            bodyStart = parent.start;
            bodyEnd = parent.end;
        } else if (node instanceof JuliaModule) {
            name = ((JuliaModule) node).name.toString();
            start = 0;
            end = 0;
            bodyStart = node.start;
            bodyEnd = node.end;
        } else {
            bodyStart = node.start;
            bodyEnd = node.end;
        }
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (start == ((Binding) o).start) {
            return end - ((Binding) o).end;
        } else
        {
            return start - ((Binding) o).start;
        }
    }

    public void setQname(@NotNull String qname) {
        this.qname = qname;
    }

    public void addRef(Node node) {
        refs.add(node);
    }


    public void addType(Type t) {
        type = UnionType.union(type, t);
    }

    @Nullable
    public String getFile() {
        return isURL() ? null : fileOrUrl;
    }

    public boolean isURL() {
        return fileOrUrl != null && fileOrUrl.startsWith("http://");
    }


    public boolean isBuiltin() {
        return isBuiltin;
    }

    public boolean isSynthetic() {
        return isSynthetic;
    }


    public String  getFirstFile() {
        Type bt = type;
        if (bt instanceof ModuleType) {
            String file = bt.asModuleType().file;
            return file != null ? file : "<built-in module>";
        }

        String file = getFile();
        if (file != null) {
            return file;
        }

        return "<built-in module>";
    }


    @Nullable
    public String getURL() {
        return isURL() ? fileOrUrl : null;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(binding:");
        sb.append(":kind=").append(kind);
        sb.append(":node=").append(node);
        sb.append(":type=").append(type);
        sb.append(":qname=").append(qname);
        sb.append(":refs=");
        sb.append(":refs=");
        if (refs.size() > 10) {
            sb.append("[");
            sb.append(refs.iterator().next());
            sb.append(", ...(");
            sb.append(refs.size() - 1);
            sb.append(" more)]");
        } else {
            sb.append(refs);
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }
}
