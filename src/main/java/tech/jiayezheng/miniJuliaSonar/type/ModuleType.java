package tech.jiayezheng.miniJuliaSonar.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.jiayezheng.miniJuliaSonar.$;
import tech.jiayezheng.miniJuliaSonar.Analyzer;
import tech.jiayezheng.miniJuliaSonar.State;

public class ModuleType extends Type {


    @NotNull
    public String name;
    @Nullable
    public String qname;



    public ModuleType(@NotNull String name, @Nullable String file, @NotNull State parent) {
        this.name = name;
        this.file = file;  // null for builtin modules

        if (file != null) {
            // This will return null iff specified file is not prefixed by
            // any path in the module search path -- i.e., the caller asked
            // the analyzer to load a file not in the search path.
            qname = $.moduleQname(file);
        }
        if (qname == null) {
            qname = name;
        }
        setTable(new State(parent, State.StateType.MODULE));
        table.setPath(qname);
        table.setType(this);


        if (Analyzer.self.builtins != null) {
            table.addSuper(Analyzer.self.builtins.BaseModule.table);
        }
    }

    public void setTable( State table) {
        this.table = table;
    }

    @Override
    public boolean typeEquals(Object other) {
        if (other instanceof ModuleType) {
            ModuleType co = (ModuleType) other;
            if (file != null) {
                return file.equals(co.file);
            }
        }
        return this == other;
    }

    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return name;
    }
}
