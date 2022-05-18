package tech.jiayezheng.miniJuliaSonar.type;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.jiayezheng.miniJuliaSonar.$;
import tech.jiayezheng.miniJuliaSonar.Binding;
import tech.jiayezheng.miniJuliaSonar.State;
import tech.jiayezheng.miniJuliaSonar.TypeStack;

import java.util.*;


public abstract class Type {

    @NotNull
    public State table = new State(null, State.StateType.GLOBAL);
    public String file = null;


    @NotNull
    protected static TypeStack typeStack = new TypeStack();

    private static Set<State> looked = new HashSet<>();


    public Type() {
    }

    @Override
    public boolean equals(Object other) {
        return typeEquals(other);
    }

    public abstract boolean typeEquals(Object other);


    public void setTable(@NotNull State table) {
        this.table = table;
    }


    public void setFile(String file) {
        this.file = file;
    }


    public boolean isNumType() {
        return this == Types.Int64Instance || this == Types.Float64Instance;
    }


    public boolean isUnknownType() {
        return this == Types.UNKNOWN;
    }


    @NotNull
    public ModuleType asModuleType() {
        if (this instanceof UnionType) {
            for (Type t : ((UnionType) this).types) {
                if (t instanceof ModuleType) {
                    return t.asModuleType();
                }
            }
            $.die("Not containing a ModuleType");
            // can't get here, just to make the @NotNull annotation happy
            return new ModuleType(null, null, null);
        } else if (this instanceof ModuleType) {
            return (ModuleType) this;
        } else {
            $.die("Not a ModuleType");
            // can't get here, just to make the @NotNull annotation happy
            return new ModuleType(null, null, null);
        }
    }



    /**
     * Internal class to support printing in the presence of type-graph cycles.
     */
    protected class CyclicTypeRecorder {
        int count = 0;
        @NotNull
        private Map<Type, Integer> elements = new HashMap<>();
        @NotNull
        private Set<Type> used = new HashSet<>();


        public Integer push(Type t) {
            count += 1;
            elements.put(t, count);
            return count;
        }


        public void pop(Type t) {
            elements.remove(t);
            used.remove(t);
        }


        public Integer visit(Type t) {
            Integer i = elements.get(t);
            if (i != null) {
                used.add(t);
            }
            return i;
        }


        public boolean isUsed(Type t) {
            return used.contains(t);
        }
    }


    /**
     * make key for types to get instances.
     * @param t
     * @param typeArgs
     * @return
     */
    public String makeKey(Type t, List<Type> typeArgs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<Instance :").append(t.toString()).append(" :");
        sb.append("{");
        for (Type type : typeArgs) {
            sb.append(" ").append(type.toString());
        }
        sb.append("}");
        return sb.toString();
    }


    protected abstract String printType(CyclicTypeRecorder ctr);


    @NotNull
    @Override
    public String toString() {
        return printType(new CyclicTypeRecorder());
    }

}
