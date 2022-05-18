package tech.jiayezheng.miniJuliaSonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.jiayezheng.miniJuliaSonar.ast.BaseType;
import tech.jiayezheng.miniJuliaSonar.ast.Url;
import tech.jiayezheng.miniJuliaSonar.type.*;

import java.util.HashMap;

import java.util.Map;

import static tech.jiayezheng.miniJuliaSonar.Binding.Kind.*;
import static tech.jiayezheng.miniJuliaSonar.type.Types.AnyType;

public class Builtins {


    public static final String LIBRARY_URL = "https://docs.julialang.org/en/v1/";
    public static final String BASE_URL = "https://docs.julialang.org/en/v1/base/";

    @NotNull
    public static State table = new State(null, State.StateType.SCOPE);
    @NotNull
    private Map<String, NativeModule> modules = new HashMap<>();


    public PrimType BaseModule;
    public PrimType BaseVector;
    public InstanceType BaseVectorInst;
    public PrimType BaseDict;
    public PrimType BaseTuple;
    public PrimType BaseStruct;
    public PrimType BaseFunction;
    private Type BasePair;

    public Builtins() {
        buildTypes();
    }


    public ModuleType Builtin;

    private void buildTypes() {
        new CoreModule();
        State bt = Builtin.table;


        BaseVector = newDataType("Vector", bt, AnyType);
        BaseVectorInst = new InstanceType(BaseVector);
        BaseTuple = newDataType("Tuple", bt, AnyType);
        BaseModule = newDataType("module", bt);
        BaseStruct = newDataType("struct", bt, AnyType);
        BaseFunction = newDataType("function", bt, AnyType);
        BasePair = newDataType("Pair", bt, AnyType);

    }


    @Nullable
    ModuleType newModule(String name) {
        return new ModuleType(name, null, Analyzer.self.globaltable);
    }

    @NotNull
    PrimType newDataType(@NotNull String name, State table) {
        return newDataType(name, table, null);
    }


    @NotNull
    PrimType newDataType(@NotNull String name, State table,
                         PrimType superClass, @NotNull PrimType... moreSupers) {
        PrimType t = new PrimType(name, table, superClass);
        for (PrimType c : moreSupers) {
            t.addBase(c);
        }
        return t;
    }

    @Nullable
    FuncType newFunc(@Nullable Type type) {
        if (type == null) {
            type = Types.UNKNOWN;
        }
        return new FuncType(Types.UNKNOWN, type);
    }

    @NotNull
    public static Url newLibUrl(@NotNull String path) {
        if (!path.contains("#") && !path.endsWith(".html")) {
            path += ".html";
        }
        return new Url(LIBRARY_URL + path);
    }

    @NotNull
    public static Url newLibUrl(String module, String name) {
        return newLibUrl(module + ".html#" + name);
    }

    @NotNull
    public static Url newCoreUrl(@NotNull String path) {
        return new Url(BASE_URL + path);
    }

    @NotNull
    public static Url newCoreUrl(String module, String name) {
        return newCoreUrl(module + "/#Core." + name);
    }

    @NotNull
    public static Url newBaseUrl(@NotNull String path) {
        return new Url(BASE_URL + path);
    }

    @NotNull
    public static Url newBaseUrl(String module, String name) {
        return newCoreUrl(module + "/#Base." + name);
    }


    private abstract class NativeModule {
        protected String name;
        @Nullable
        protected ModuleType module;
        @Nullable
        protected State table;

        NativeModule(String name) {
            this.name = name;
            modules.put(name, this);
        }

        /**
         * Lazily load the module.
         */
        @Nullable
        ModuleType getModule() {
            if (module == null) {
                createModuleType();
                initBindings();
            }
            return module;
        }

        @Nullable
        protected void addDataType(String name, Url url, Type type) {
            assert table != null;
            table.insert(name, url, type, DATATYPE);
        }

        @Nullable
        protected void addDataType(PrimType type) {
            table.insert(type.name, liburl(type.name), type, DATATYPE);
        }

        @Nullable
        protected void addFunction(String name, Url url, Type type) {
            table.insert(name, url, newFunc(type), FUNCTION);
        }

        protected abstract void initBindings();

        protected void createModuleType() {
            if (module == null) {
                module = newModule(name);
                table = module.table;
                Analyzer.self.moduleTable.insert(name, liburl(), module, MODULE);
            }
        }


        @NotNull
        protected Url liburl() {
            return newLibUrl(name);
        }

        @NotNull
        protected Url liburl(String anchor) {
            return newLibUrl(name, anchor);
        }

        @NotNull
        @Override
        public String toString() {
            return module == null
                    ? "<Non-loaded builtin module '" + name + "'>"
                    : "<NativeModule:" + module + ">";


        }
    }

    class CoreModule extends NativeModule {
        public CoreModule() {
            super("Base");
            Builtin = module = newModule(name);
            table = module.table;
        }


        @Override
        protected void initBindings() {
            Analyzer.self.moduleTable.insert(name, liburl(), module, MODULE);


            table.addSuper(BaseModule.table);
            addDataType("Any", newCoreUrl("base", "Any"), AnyType);
            addFunction("typeof", newCoreUrl("base", "typeof"), Types.DataTypeInstance);

            addDataType("Bool", newCoreUrl("number", "Bool"), Types.BoolInstance);
            addDataType("Int64", newCoreUrl("number", "Int64"), Types.Int64Instance);
            addDataType("Float64", newCoreUrl("numbers", "Float64"), Types.Float64Instance);
            addDataType("Char", newCoreUrl("strings", "Char"), Types.CharInstance);

            addDataType("Nothing", newCoreUrl("base", "Nothing"), Types.NothingInstance);
            addDataType("Missing", newCoreUrl("base", "Missing"), Types.MissingType);


            addFunction("vec", newBaseUrl("arrays", "vect"), new InstanceType(BaseVector));
            addFunction("Dict", newBaseUrl("collections", "Dict"), new InstanceType(BaseDict));
            addFunction("Pair", newBaseUrl("collections", "Pair"), new InstanceType(BasePair));

        }
    }

}
