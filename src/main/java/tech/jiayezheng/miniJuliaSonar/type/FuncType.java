package tech.jiayezheng.miniJuliaSonar.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.jiayezheng.miniJuliaSonar.Analyzer;
import tech.jiayezheng.miniJuliaSonar.State;
import tech.jiayezheng.miniJuliaSonar.TypeStack;
import tech.jiayezheng.miniJuliaSonar.ast.FuncDef;

import java.util.*;

public class FuncType extends Type{

    private static final int MAX_ARROWS = 10;

    public Map<Type, Type> arrows = new HashMap<>();
    public FuncDef func;

    public State env;
    public List<Type> defaultTypes;       // types for default parameters (evaluated at def time)


    public FuncType() {
    }

    @Override
    public boolean typeEquals(Object other) {
        if(other instanceof FuncType) {
            FuncType fo = (FuncType) other;
            return  fo.table.path.equals(table.path) || this == other;
        } else {
            return false;
        }
    }

    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        if (arrows.isEmpty()) {
            return "? -> ?";
        }

        StringBuilder sb = new StringBuilder();

        Integer num = ctr.visit(this);
        if (num != null) {
            sb.append("#").append(num);
        } else {
            int newNum = ctr.push(this);

            int i = 0;
            Set<String> seen = new HashSet<>();

            for (Map.Entry<Type, Type> e : arrows.entrySet()) {
                Type from = e.getKey();
                String as = from.printType(ctr) + " -> " + e.getValue().printType(ctr);

                if (!seen.contains(as)) {
                    if (i != 0) {
                        if (Analyzer.self.multilineFunType) {
                            sb.append("\n/ ");
                        } else {
                            sb.append(" / ");
                        }
                    }

                    sb.append(as);
                    seen.add(as);
                }

                i++;
            }

            if (ctr.isUsed(this)) {
                sb.append("=#").append(newNum).append(": ");
            }
            ctr.pop(this);
        }
        return sb.toString();
    }


    public FuncType(FuncDef func, State env) {
        this.func = func;
        this.env = env;
    }

    public FuncType(Type from, Type to) {
        addMapping(from, to);
    }


    public void addMapping(Type from, Type to) {
        if (arrows.size() < MAX_ARROWS) {
            arrows.put(from, to);
        }
    }

    public void removeMapping(Type from)
    {
        arrows.remove(from);
    }

    @Nullable
    public Type getMapping(@NotNull Type from) {
        return arrows.get(from);
    }

    private Map<Type, Type> compressArrows(Map<Type, Type> arrows) {
        Map<Type, Type> ret = new HashMap<>();

        for (Map.Entry<Type, Type> e1 : arrows.entrySet()) {
            boolean subsumed = false;

            for (Map.Entry<Type, Type> e2 : arrows.entrySet()) {
                if (e1 != e2 && subsumed(e1.getKey(), e2.getKey())) {
                    subsumed = true;
                    break;
                }
            }

            if (!subsumed) {
                ret.put(e1.getKey(), e1.getValue());
            }
        }

        return ret;
    }

    private boolean subsumed(Type type1, Type type2) {
        return subsumedInner(type1, type2, new TypeStack());
    }

    public void setDefaultTypes(List<Type> defaultTypes) {
        this.defaultTypes = defaultTypes;
    }

    private boolean subsumedInner(Type type1, Type type2, TypeStack typeStack) {
        if (typeStack.contains(type1, type2)) {
            return true;
        }
//
//        if (type1.isUnknownType() || type1 == Type.NIL || type1.equals(type2)) {
//            return true;
//        }

//        if (type1 instanceof TupleType && type2 instanceof TupleType) {
//            List<Type> elems1 = ((TupleType) type1).eltTypes;
//            List<Type> elems2 = ((TupleType) type2).eltTypes;
//
//            if (elems1.size() == elems2.size()) {
//                typeStack.push(type1, type2);
//                for (int i = 0; i < elems1.size(); i++) {
//                    if (!subsumedInner(elems1.get(i), elems2.get(i), typeStack)) {
//                        typeStack.pop(type1, type2);
//                        return false;
//                    }
//                }
//            }
//
//            return true;
//        }
//
//        if (type1 instanceof ListType && type2 instanceof ListType) {
//            return subsumedInner(((ListType) type1).toTupleType(), ((ListType) type2).toTupleType(), typeStack);
//        }

        return false;
    }

    public Type getReturnType() {
        if (!arrows.isEmpty()) {
            return arrows.values().iterator().next();
        } else {
            return Types.UNKNOWN;
        }
    }

    public boolean oversized() {
        return arrows.size() >= MAX_ARROWS;
    }
}
