package tech.jiayezheng.miniJuliaSonar.type;

import org.jetbrains.annotations.NotNull;
import tech.jiayezheng.miniJuliaSonar.ast.Node;

import java.util.*;
import java.util.stream.Collectors;

public class UnionType extends Type {
    public Set<Type> types;

    public UnionType() {
        this.types = new HashSet<>();
    }

    public UnionType(@NotNull Type... initialTypes) {
        this();
        for (Type nt : initialTypes) {
            addType(nt);
        }
    }

    private void addType(@NotNull Type t) {
        if (t instanceof UnionType) {
            types.addAll(((UnionType) t).types);
        } else {
            types.add(t);
        }
    }

    /**
     * Returns true if t1 == t2 or t1 is a union type that contains t2.
     */
    static public boolean contains(Type t1, Type t2) {
        if (t1 instanceof UnionType) {
            return ((UnionType) t1).contains(t2);
        } else {
            return t1.equals(t2);
        }
    }

    public boolean contains(Type t) {
        return types.contains(t);
    }

    static public Type remove(Type t1, Type t2) {
        if (t1 instanceof UnionType) {
            Set<Type> types = new HashSet<>(((UnionType) t1).types);
            types.remove(t2);
            return UnionType.newUnion(types);
        } else if (t1 != Types.CONT && t1 == t2) {
            return Types.UNKNOWN;
        } else {
            return t1;
        }
    }

    static public Type newUnion(@NotNull Collection<Type> types) {
        Type t = Types.UNKNOWN;
        for (Type nt : types) {
            t = union(t, nt);
        }
        return t;
    }

    // take a union of two types
    // with preference: other > None > Cont > unknown
    @NotNull
    public static Type union(@NotNull Type u, @NotNull Type v) {
        if (u.equals(v)) {
            return u;
        } else if (u != Types.UNKNOWN && v == Types.UNKNOWN) {
            return u;
        } else if (v != Types.UNKNOWN && u == Types.UNKNOWN) {
            return v;
        } else if (u != Types.NothingInstance && v == Types.NothingInstance) {
            return u;
        } else if (v != Types.NothingInstance && u == Types.NothingInstance) {
            return v;
        } else if (v != Types.UnionAll && u == Types.UnionAll) {
            return v;
        } else if (u != Types.UnionAll && v == Types.UnionAll) {
            return u;
        } else if (u != Types.MissingInstance && v == Types.MissingInstance) {
            return u;
        } else if (v != Types.MissingInstance && u == Types.MissingInstance) {
            return v;
        } else if (u instanceof TupleType && v instanceof TupleType &&
                ((TupleType) u).size() == ((TupleType) v).size()) {
            return union((TupleType) u, (TupleType) v);
        } else {
            return new UnionType(u, v);
        }

    }

    public static Type union(Collection<Type> types) {
        Type result = Types.UNKNOWN;
        for (Type type: types) {
            result = UnionType.union(result, type);
        }
        return result;
    }


    @Override
    public boolean typeEquals(Object other) {
        if (typeStack.contains(this, other)) {
            return true;
        } else if (other instanceof UnionType) {
            Set<Type> types1 = types;
            Set<Type> types2 = ((UnionType) other).types;
            if (types1.size() != types2.size()) {
                return false;
            } else {
                for (Type t : types2) {
                    if (!types1.contains(t)) {
                        return false;
                    }
                }
                for (Type t : types1) {
                    if (!types2.contains(t)) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }


    @Override
    public int hashCode() {
        return "UnionType".hashCode();
    }

    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        StringBuilder sb = new StringBuilder();

        Integer num = ctr.visit(this);
        if (num != null) {
            sb.append("#").append(num);
        } else {
            int newNum = ctr.push(this);
            List<String> typeStrings = types.stream().map(x->x.printType(ctr)).collect(Collectors.toList());
            Collections.sort(typeStrings);
            sb.append("{");
            sb.append(String.join(" | ", typeStrings));

            if (ctr.isUsed(this)) {
                sb.append("=#").append(newNum).append(":");
            }

            sb.append("}");
            ctr.pop(this);
        }

        return sb.toString();
    }
}
