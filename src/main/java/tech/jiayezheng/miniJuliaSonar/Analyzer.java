package tech.jiayezheng.miniJuliaSonar;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.jiayezheng.miniJuliaSonar.ast.Node;
import tech.jiayezheng.miniJuliaSonar.ast.Symbol;
import tech.jiayezheng.miniJuliaSonar.ast.Url;
import tech.jiayezheng.miniJuliaSonar.type.*;
import tech.jiayezheng.miniJuliaSonar.visitor.TypeInferencer;

import java.io.File;
import java.net.URL;
import java.util.*;


public class Analyzer {
    // global static
    public static Analyzer self;
    public final Builtins builtins;

    public String cacheDir;
    public Stats stats = new Stats();
    public String cwd = null;

    public String sid = $.newSessionId();
    public Map<String, Object> options;
    public Set<Symbol> resolved = new HashSet<>();
    public Set<Symbol> unresolved = new HashSet<>();
    public boolean multilineFunType = false;
    private AstCache astCache;
    public Set<String> failedToParse = new HashSet<>();
    public List<String> loadedFiles = new ArrayList<>();
    public List<Binding> allBindings = new ArrayList<>();

    public State moduleTable = new State(null, State.StateType.GLOBAL);
    public State globaltable = new State(null, State.StateType.GLOBAL);
    private String modelDir;
    public String projectDir;
    public int nCalled = 0;
    public List<String> path = new ArrayList<>();
    private Progress loadingProgress = null;
    public TypeInferencer inferencer = new TypeInferencer();
    public ListMultimap<Node, Binding> references = ArrayListMultimap.create();
    public ListMultimap<String, Diagnostic> semanticErrors = ArrayListMultimap.create();

    public Stack<CallStackEntry> callStack = new Stack<>();
    private Set<FuncType> uncalled = new HashSet<>();


    public Analyzer() {
        this(null);
    }

    public Analyzer(Map<String, Object> options) {
        self = this;
        if (options != null) {
            this.options = options;
        } else {
            this.options = new HashMap<>();
        }

        this.stats.putInt("startTime", System.currentTimeMillis());

        this.builtins = new Builtins();

        this.cacheDir = createCacheDir();
        this.astCache = new AstCache();
        addJuliaPath();
        // copyModels();


    }

    private String createCacheDir() {
        String dir = $.getTempFile("ast_cache");
        File f = new File(dir);
        $.msg("AST cache is at: " + dir);

        if (!f.exists()) {
            if (!f.mkdirs()) {
                $.die("Failed to create tmp directory: " + dir + ". Please check permissions");
            }
        }
        return dir;
    }


    public void setOption(String option) {
        options.put(option, true);
    }


    public boolean hasOption(String option) {
        Object op = options.get(option);
        if (op != null && op.equals(true)) {
            return true;
        } else {
            return false;
        }
    }


    private void addJuliaPath() {
        String path = System.getenv("JuliaPATH");
        if (path != null) {
            String[] segments = path.split(":");
            for (String p : segments) {
                addPath(p);
            }
        }
    }

    private void copyModels() {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(Globals.MODEL_LOCATION);
        String dest = $.getTempFile("models");
        this.modelDir = dest;

        try {
            $.copyResourcesRecursively(resource, new File(dest));
            $.msg("copied models to: " + modelDir);
        } catch (Exception e) {
            $.die("Failed to copy models. Please check permissions of writing to: " + dest);
        }
        addPath(dest);
    }

    public void addPaths(@NotNull List<String> p) {
        for (String s : p) {
            addPath(s);
        }
    }

    public void addPath(String p) {
        path.add($.unifyPath(p));
    }

    // main entry to the analyzer
    public void analyze(String path) {
        String upath = $.unifyPath(path);
        File f = new File(upath);
        projectDir = f.isDirectory() ? f.getPath() : f.getParent();
        loadFileRecursive(upath);
    }

    /**
     * Load all Julia source files recursively if the given fullname is a
     * directory; otherwise just load a file.  Looks at file extension to
     * determine whether to load a given file.
     */
    public void loadFileRecursive(String fullname) {
        int count = countFileRecursive(fullname);
        if (loadingProgress == null) {
            loadingProgress = new Progress(count, 50);
        }

        File file_or_dir = new File(fullname);

        if (file_or_dir.isDirectory()) {
            for (File file : file_or_dir.listFiles()) {
                loadFileRecursive(file.getPath());
            }
        } else {
            if (file_or_dir.getPath().endsWith(Globals.FILE_SUFFIX)) {
                loadFile(file_or_dir.getPath());
            }
        }
    }



    @Nullable
    public Type loadFile(String path) {
        path = $.unifyPath(path);
        File f = new File(path);

        if (!f.canRead()) {
            return null;
        }

        Type module = getCachedModule(path);
        if(module != null) {
            return module;
        }
        // set new CWD and save the old one on stack
        // current work dir 当前工作路径
        String oldcwd = cwd;
        setCWD(f.getParent());

        // pushImportStack(path);
        Type type = parseAndResolve(path);
        // popImportStack(path);

        // restore old CWD
        setCWD(oldcwd);

        return type;
    }

    @Nullable
    private Type getCachedModule(String file) {
        Type t = moduleTable.lookupType($.moduleQname(file));
        if(t == null) {
            return null;
        } else if(t instanceof UnionType) {
            for (Type tt : ((UnionType) t).types) {
                if (tt instanceof ModuleType) {
                    return (ModuleType) tt;
                }
            }
            return  null;
        } else if (t instanceof ModuleType) {
            return (ModuleType) t;
        } else {
            return null;
        }
    }

    /**
     * Entry For inference
     */
    @Nullable
    private Type parseAndResolve(String file) {
        loadingProgress.tick();
        Node ast = getAstForFile(file);

        if (ast == null) {
            failedToParse.add(file);
            return null;
        } else {
            Type type = inferencer.visit(ast, moduleTable);
            loadedFiles.add(file);
            return type;
        }
    }

    private Node getAstForFile(String file) {
        return astCache.getAST(file);
    }

    public void setCWD(String cd) {
        if (cd != null) {
            cwd = $.unifyPath(cd);
        }
    }


    // count number of .py files
    public int countFileRecursive(String fullname) {
        File file_or_dir = new File(fullname);
        int sum = 0;

        if (file_or_dir.isDirectory()) {
            for (File file : file_or_dir.listFiles()) {
                sum += countFileRecursive(file.getPath());
            }
        } else {
            if (file_or_dir.getPath().endsWith(Globals.FILE_SUFFIX)) {
                sum += 1;
            }
        }
        return sum;
    }

    public void finish() {
        $.msg("\nFinished loading files. " + nCalled + " functions were called.");
        $.msg("Analyzing uncalled functions");
        applyUncalled();

        // mark unused variables
        for (List<Binding> bset : $.correlateBindings(allBindings)) {
            if (unusedBindingSet(bset)) {
                Binding first = bset.get(0);
                putProblem(first.node, "Unused variable: " + first.name);
            }
        }


        $.msg(getAnalysisSummary());
        close();
    }

    @NotNull
    public String getAnalysisSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n" + $.banner("analysis summary"));

        String duration = $.formatTime(System.currentTimeMillis() - stats.getInt("startTime"));
        sb.append("\n- total time: " + duration);
        sb.append("\n- modules loaded: " + loadedFiles.size());
        sb.append("\n- semantic problems: " + semanticErrors.size());
        sb.append("\n- failed to parse: " + failedToParse.size());

        // calculate number of defs, refs, xrefs
        int nDef = 0, nXRef = 0;
        for (Binding b : getAllBindings()) {
            nDef += 1;
            nXRef += b.refs.size();
        }

        sb.append("\n- number of definitions: " + nDef);
        sb.append("\n- number of cross references: " + nXRef);
        sb.append("\n- number of references: " + references.size());

        long nResolved = resolved.size();
        long nUnresolved = unresolved.size();
        sb.append("\n- resolved names: " + nResolved);
        sb.append("\n- unresolved names: " + nUnresolved);
        sb.append("\n- name resolve rate: " + $.percent(nResolved, nResolved + nUnresolved));
        sb.append("\n" + $.getGCStats());

        return sb.toString();
    }

    public List<Binding> getAllBindings() {
        return allBindings;
    }

    private boolean unusedBindingSet(List<Binding> bindings) {
        for (Binding binding : bindings) {
            if (!unused(binding)) {
                return false;
            }
        }
        return true;
    }


    private boolean unused(Binding binding) {
        return (!(binding.type instanceof PrimType) &&
                !(binding.type instanceof FuncType) &&
                !(binding.type instanceof ModuleType)
                && binding.refs.isEmpty());
    }

    public void close() {

        astCache.close();
        $.sleep(10);
        if (!$.deleteDirectory($.getTempDir())) {
            $.msg("Failed to delete temp dir: " + $.getTempDir());
        }
    }

    public void applyUncalled() {
        Progress progress = new Progress(this.uncalled.size(), 50);

        while (!uncalled.isEmpty()) {
            List<FuncType> uncalledDup = new ArrayList<>(uncalled);

            for (FuncType cl : uncalledDup) {
                progress.tick();
                inferencer.apply(cl, null, null, null);
            }
        }
    }

    @NotNull
    public List<String> getLoadedFiles() {
        List<String> files = new ArrayList<>();
        for (String file : loadedFiles) {
            if (file.endsWith(Globals.FILE_SUFFIX)) {
                files.add(file);
            }
        }
        return files;
    }

    public void registerBinding(Binding binding) {
        allBindings.add(binding);
    }

    public void putRef(Node node, Collection<Binding> bs) {
        if (!(node instanceof Url)) {
            List<Binding> bindings = references.get(node);
            for (Binding b : bs) {
                if (!bindings.contains(b)) {
                    bindings.add(b);
                }
                b.addRef(node);
            }
        }
    }

    public void putRef(@NotNull Node node, @NotNull Binding b) {
        List<Binding> bs = new ArrayList<>();
        bs.add(b);
        putRef(node, bs);
    }


    public void putProblem(@NotNull Node loc, String msg) {
        String file = loc.file;
        if (file != null) {
            addFileErr(file, loc.start, loc.end, msg);
        }
    }

    // for situations without a Node
    public void putProblem(@Nullable String file, int begin, int end, String msg) {
        if (file != null) {
            addFileErr(file, begin, end, msg);
        }
    }


    void addFileErr(String file, int begin, int end, String msg) {
        Diagnostic d = new Diagnostic(file, Diagnostic.Category.ERROR, begin, end, msg);
        semanticErrors.put(file, d);
    }

    // Assume that same input(types) same output
    public void addUncalled(@NotNull FuncType cl) {
        if (!cl.func.called) {
            uncalled.add(cl);
        }
    }


    public void removeUncalled(FuncType f) {
        uncalled.remove(f);
    }


}
