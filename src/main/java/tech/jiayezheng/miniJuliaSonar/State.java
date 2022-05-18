package tech.jiayezheng.miniJuliaSonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.jiayezheng.miniJuliaSonar.ast.Node;
import tech.jiayezheng.miniJuliaSonar.type.ModuleType;
import tech.jiayezheng.miniJuliaSonar.type.Type;
import tech.jiayezheng.miniJuliaSonar.type.Types;
import tech.jiayezheng.miniJuliaSonar.type.UnionType;

import java.util.*;

/**
 * Indentify Scope of the on-fly analysis
 * Stores Intermediate Results of the analysis
 * Lattice Product Value: symbol::String ↦ Set<Binding>
 */
public class State {


    @Nullable
    public Set<String> globalNames;


    public enum StateType {
        STRUCT,
        ATTRIBUTE,
        INSTANCE,
        FUNCTION,
        MODULE,
        GLOBAL,
        SCOPE
    }


    @NotNull
    public Map<String, Set<Binding>> table = new HashMap<>(0);


    @Nullable
    public State parent;       // not null except global table

    public void setParent(State parent) {
        this.parent = parent;
    }


    @Nullable
    public State forwarding; // decide where to go when function is over

    public State getForwarding() {
        if (forwarding != null) {
            return forwarding;
        } else {
            return this;
        }
    }

    @Nullable
    public List<State> supers;

    @Nullable
    public Set<String> globalSymbols;

    public StateType stateType;

    public Type type;

    public void setType(Type type) {
        this.type = type;
    }

    @NotNull
    public String path = "";

    public void setPath(String path) {
        this.path = path;
    }

    public String extendPath(String name) {
        name = $.moduleName(name);
        if (path.equals("")) {
            return name;
        }
        return path + "." + name;
    }


    public State(@Nullable State parent, StateType type) {
        this.parent = parent;
        this.stateType = type;
        this.forwarding = this;
    }

    public State(@NotNull State s) {
        this.table = new HashMap<>();
        this.table.putAll(s.table);
        this.parent = s.parent;
        this.parent = s.parent;
        this.stateType = s.stateType;
        this.forwarding = s.forwarding;
        this.supers = s.supers;
        this.globalSymbols = s.globalSymbols;
        this.type = s.type;
        this.path = s.path;
    }

    // erase and overwrite this to s's contents
    public void overwrite(@NotNull State s) {
        this.table = s.table;
        this.parent = s.parent;
        this.stateType = s.stateType;
        this.forwarding = s.forwarding;
        this.supers = s.supers;
        this.globalSymbols = s.globalSymbols;
        this.type = s.type;
        this.path = s.path;
    }

    @NotNull
    public State copy() {
        return new State(this);
    }

    public void merge(State other) {
        for (Map.Entry<String, Set<Binding>> e2 : other.table.entrySet()) {
            Set<Binding> b1 = table.get(e2.getKey());
            Set<Binding> b2 = e2.getValue();

            if (b1 != null && b2 != null) {
                b1.addAll(b2);
            } else if (b1 == null && b2 != null) {
                table.put(e2.getKey(), b2);
            }
        }
    }

    public static State merge(State state1, State state2) {
        State ret = state1.copy();
        ret.merge(state2);
        return ret;
    }

    public void addSuper(State sup) {
        if (supers == null) {
            supers = new ArrayList<>();
        }
        supers.add(sup);
    }

    public void setStateType(StateType type) {
        this.stateType = type;
    }

    public void addGlobalSymbol(@NotNull String symbol) {
        if (globalSymbols == null) {
            globalSymbols = new HashSet<>(1);
        }

        globalSymbols.add(symbol);
    }

    public boolean isGlobalSymbol(@NotNull String symbol) {
        if (globalSymbols != null) {
            return globalSymbols.contains(symbol);
        } else if (parent != null) {
            return parent.isGlobalSymbol(symbol);
        } else {
            return false;
        }
    }

    public void remove(String id) {
        table.remove(id);
    }

    // create new binding and insert
    public void insert(String id, @NotNull Node node, @NotNull Type type, Binding.Kind kind) {
        Binding b = new Binding(id, node, type, kind);
        if (type instanceof ModuleType) {
            b.setQname(type.asModuleType().qname);
        } else {
            b.setQname(extendPath(id));
        }
        update(id,b);
    }

    @NotNull
    public Set<Binding> update(String id, @NotNull Binding b) {
        Set<Binding> bs = new HashSet<>(1);
        bs.add(b);
        table.put(id, bs);
        return bs;
    }

    public Set<Binding> lookup(String name) {
        Set<Binding> b = getModuleBindingIfGlobal(name);
        if (b != null) {
            return b;
        } else {
            Set<Binding> ent = lookupLocal(name);
            if (ent != null) {
                return ent;
            } else {
                if (parent != null) {
                    return parent.lookup(name);
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Look up a name in the current symbol table only. Don't recurse on the
     * parent table.
     */
    @Nullable
    public Set<Binding> lookupLocal(String name) {
        return table.get(name);
    }

    private Set<Binding> getModuleBindingIfGlobal(String name) {
        if (isGlobalName(name)) {
            State module = getGlobalTable();
            if (module != this) {
                return module.lookupLocal(name);
            }
        }
        return null;
    }


    public void addGlobalName(@NotNull String name) {
        if (globalNames == null) {
            globalNames = new HashSet<>(1);
        }
        globalNames.add(name);
    }


    private State getGlobalTable() {
        State result = getStateOfType(StateType.MODULE);
        if (result != null) {
            return result;
        } else {
            $.die("Couldn't find global table. Shouldn't happen");
            return this;
        }
    }

    private State getStateOfType(StateType type) {
        if (stateType == type) {
            return this;
        } else if (parent == null) {
            return null;
        } else {
            return parent.getStateOfType(type);
        }
    }

    public boolean isGlobalName(String name) {
        if (globalNames != null) {
            return globalNames.contains(name);
        } else if (parent != null) {
            return parent.isGlobalName(name);
        } else {
            return false;
        }
    }


    @NotNull
    private static Set<State> looked = new HashSet<State>();

    public Set<Binding> lookupAttr(String attr) {
        if (looked.contains(this)) {
            return null;
        } else {
            Set<Binding> b = lookupLocal(attr);
            if (b != null) {
                return b;
            } else {
                if (supers != null && !supers.isEmpty()) {
                    looked.add(this);
                    for (State p : supers) {
                        b = p.lookupAttr(attr);
                        if (b != null) {
                            looked.remove(this);
                            return b;
                        }
                    }
                    looked.remove(this);
                    return null;
                } else {
                    return null;
                }
            }
        }
    }

    public static Type makeUnion(Set<Binding> bs) {
        Type t = Types.UNKNOWN;
        for (Binding b : bs) {
            t = UnionType.union(t, b.type);
        }
        return t;
    }


}