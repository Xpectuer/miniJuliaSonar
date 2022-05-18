package tech.jiayezheng.miniJuliaSonar.visitor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.jiayezheng.miniJuliaSonar.*;
import tech.jiayezheng.miniJuliaSonar.ast.*;
import tech.jiayezheng.miniJuliaSonar.type.*;


import java.util.*;


import static tech.jiayezheng.miniJuliaSonar.Binding.Kind.*;


public class TypeInferencer implements Visitor1<Type, State> {
    @Override
    public Type visit(Root node, State s) {
        // mark global
        for (Node n : node.args) {
            // global type declare is not supported
            if (n instanceof Global) {
                for (Node e : ((Global) n).names) {
                    if (e instanceof Symbol) {
                        Symbol symbol = (Symbol) e;
                        s.addGlobalSymbol(symbol.name);
                        Set<Binding> nb = s.lookup(symbol.name);
                        if (nb != null) {
                            Analyzer.self.putRef(symbol, nb);
                        }
                    }
                }
            }
        }

        boolean returned = false;
        Type retType = Types.UNKNOWN;
        for (Node n : node.args) {
            Type t = visit(n, s);

            if (!returned) {
                retType = UnionType.union(retType, t);
                if (!UnionType.contains(t, Types.CONT)) {
                    returned = true;
                    retType = UnionType.remove(retType, Types.CONT);
                }
            }
        }
        return retType;
    }

    @Override
    public Type visit(KeyWord node, State s) {
        return Types.CONT;
    }

    @Override
    public Type visit(Assign node, State s) {
        Type valueType = visit(node.value, s);
        bind(s, node.target, valueType);
        return Types.CONT;
    }

    @Override
    public Type visit(Symbol node, State s) {
        Set<Binding> b = s.lookup(node.name);
        if (b != null) {
            Analyzer.self.putRef(node, b);
            Analyzer.self.resolved.add(node);
            Analyzer.self.unresolved.remove(node);
            return State.makeUnion(b);
        } else {
            addWarningToNode(node, "unbound variable " + node.name);
            Analyzer.self.unresolved.add(node);
            Type t = Types.UNKNOWN;
            t.table.setPath(s.extendPath(node.name));
            return t;
        }
    }

    @Override
    public Type visit(Block node, State s) {
        if (node.args != null) {
            // mark global
            for (Node n : node.args) {
                // global type declare is not supported
                if (n instanceof Global) {
                    for (Node e : ((Global) n).names) {
                        if (e instanceof Symbol) {
                            Symbol symbol = (Symbol) e;
                            s.addGlobalSymbol(symbol.name);
                            Set<Binding> nb = s.lookup(symbol.name);
                            if (nb != null) {
                                Analyzer.self.putRef(symbol, nb);
                            }
                        }
                    }
                }
            }

        }


        boolean returned = false;
        Type returnType = Types.UNKNOWN;

        if (node.args != null) {
            for (Node n : node.args) {
                Type t = visit(n, s);
                if (!returned) {
                    returnType = UnionType.union(returnType, t);
                    if (!UnionType.contains(t, Types.CONT)) {
                        returned = true;
                        returnType = UnionType.remove(returnType, Types.CONT);
                    }
                }
            }
        }

        return returnType;
    }

    @Override
    public Type visit(Operator node, State s) {
        $.die("Invalid Node: " + node + " please check parser.");
        return null;
    }

    @Override
    public Type visit(FuncCombineCall node, State s) {
        $.die("Invalid Node: " + node + " please check parser.");
        return null;
    }

    @Override
    public Type visit(Call node, State s) {
        Type nameType = visit(node.name, s);

        // Infer positional argument types
        List<Type> positional = visit(node.args, s);

        // Infer keyword argument type
        Map<String, Type> kwTypes = new HashMap<>();
        if (node.keywords != null) {
            for (KW k : node.keywords) {
                kwTypes.put(k.key.name, visit(k.value, s));
            }
        }


//        Type kwArg = node.kwargs == null ? null : visit(node.kwargs, s);
//        Type starArg = node.starargs == null ? null : visit(node.starargs, s);

        if (nameType instanceof UnionType) {
            Set<Type> types = ((UnionType) nameType).types;
            Type resultType = Types.UNKNOWN;
            for (Type funType : types) {
                Type retureType = resolveCall(funType, positional, kwTypes, node);
                resultType = UnionType.union(resultType, retureType);
            }
            return resultType;
        } else {
            return resolveCall(nameType, positional, kwTypes, node);
        }


    }


    @Override
    public Type visit(BinOp node, State s) {
        Type ltype = visit(node.left, s);
        Type rtype = visit(node.right, s);
        Op op = node.op;
        if (operatorOverride(ltype, op.getRep())) {
            // TODO: Override : dependencies: FuncDef
        } else if (Op.isBoolean(op)) {
            return Types.BoolInstance;
        } else if (ltype == Types.UNKNOWN) {
            return rtype;
        } else if (rtype == Types.UNKNOWN) {
            return ltype;
        } else if (ltype.typeEquals(rtype)) {
            return ltype;
        } else if (op == Op.Or) {
            if (rtype == Types.NothingInstance || rtype == Types.MissingInstance) {
                return ltype;
            } else if (ltype == Types.NothingInstance || ltype == Types.MissingInstance) {
                return rtype;
            }
        } else if (op == Op.And) {
            if (ltype == Types.NothingInstance || rtype == Types.NothingType) {
                return Types.NothingType;
            } else if (ltype == Types.MissingInstance || rtype == Types.MissingType) {
                return Types.MissingType;
            }

        }
        addWarningToNode(node,
                "Cannot apply binary operator " + node.op.getRep() + " to type " + ltype + " and " + rtype);
        return Types.UNKNOWN;
    }


    @Override
    public Type visit(Char node, State s) {
        return Types.CharInstance;
    }

    @Override
    public Type visit(LPAREN node, State s) {
        $.die("unexpected node");
        return null;
    }

    @Override
    public Type visit(RPAREN node, State s) {
        $.die("unexpected node");
        return null;
    }

    @Override
    public Type visit(End node, State s) {
        return Types.CONT;
    }

    @Override
    public Type visit(Str node, State s) {
        return new StrType(node.s);
    }

    @Override
    public Type visit(FuncDef node, State s) {
        State env = s.getForwarding();
        FuncType fun = new FuncType(node, env);
        fun.table.setParent(s);
        fun.table.setPath(s.extendPath(node.name.name));
        fun.setDefaultTypes(visit(node.defaults, s));
        Analyzer.self.addUncalled(fun);
        Binding.Kind funkind;

        if (node.isLambda) {
            return fun;
        } else {
            funkind = FUNCTION;
            bind(s, node.name, fun, funkind);
            return Types.CONT;
        }
    }

    @Override
    public Type visit(JuliaInt node, State s) {
        return Types.Int64Instance;
    }

    @Override
    public Type visit(Comma node, State s) {
        $.die("unexpected node please check the parser !!!");
        return null;
    }

    @Override
    public Type visit(If node, State s) {
        Type type1, type2;
        State s1 = s.copy();
        State s2 = s.copy();

        visit(node.cond, s);
        inferInstance(node.cond, s, s1);

        if (node.body != null) {
            type1 = visit(node.body, s1);
        } else {
            type1 = Types.CONT;
        }

        if (node.orElse != null) {
            type2 = visit(node.orElse, s2);
        } else {
            type2 = Types.CONT;
        }

        boolean cont1 = UnionType.contains(type1, Types.CONT);
        boolean cont2 = UnionType.contains(type2, Types.CONT);

        // decide which branch affects the downstream state
        if (cont1 && cont2) {
            s1.merge(s2);
            s.overwrite(s1);
        } else if (cont1) {
            s.overwrite(s1);
        } else if (cont2) {
            s.overwrite(s2);
        }

        return UnionType.union(type1, type2);
    }


    @Override
    public Type visit(JuliaBool node, State param) {
        return Types.BoolInstance;
    }

    @Override
    public Type visit(Global node, State param) {
        return Types.CONT;
    }

    @Override
    public Type visit(Url node, State param) {
        return Types.StrInstance;
    }

    @Override
    public Type visit(Return node, State s) {
        if (node.value == null) {
            return Types.NothingInstance;
        } else {
            Type result = visit(node.value, s);
            CallStackEntry entry = Analyzer.self.callStack.top();
            if (entry != null) {
                entry.fun.addMapping(entry.from, result);
            }
            return result;
        }
    }

    @Override
    public Type visit(Continue node, State s) {
        return Types.CONT;
    }

    @Override
    public Type visit(Break node, State s) {
        return Types.NothingInstance;
    }

    @Override
    public Type visit(Tuple node, State s) {
        TupleType t = new TupleType();
        for (Node e : node.elts) {
            t.add(visit(e, s));
        }
        return t;
    }

    @Override
    public Type visit(KW node, State s) {
        Type value = visit(node.value, s);
        bind(s, node.key, value);
        return Types.CONT;
    }

    @Override
    public Type visit(VarArg node, State s) {
        $.die("unexpected node");
        return null;
    }

    @Override
    public Type visit(LSQUARE node, State s) {
        $.die("unexpected node");
        return null;
    }

    @Override
    public Type visit(RSQUARE node, State s) {
        $.die("unexpected node");
        return null;
    }

    @Override
    public Type visit(JuliaVector node, State s) {
        if (node.elts.size() == 0) {
            return new VectorType();
        }

        VectorType vectorType = new VectorType();
        for (Node elt : node.elts) {
            vectorType.add(visit(elt, s));
            if (elt instanceof Str) {
                vectorType.addValue(((Str) elt).s);
            }
        }

        return vectorType;
    }

    @Override
    public Type visit(While node, State s) {
        visit(node.cond, s);
        Type t = Types.UNKNOWN;

        State s1 = s.copy();

        if (node.body != null) {
            t = visit(node.body, s1);
            s.merge(s1);
        }

        return t;
    }

    @Override
    public Type visit(For node, State s) {
        bindIter(s, node.target, node.iter, SCOPE);
        Type t = Types.UNKNOWN;

        State s1 = s.copy();

        if (node.body != null) {
            t = visit(node.body, s1);
            s.merge(s1);
        }

        return t;
    }


    @Override
    public Type visit(Do node, State s) {
        Type ctxt = visit(node.value, s);
        if (ctxt instanceof TupleType) {
            TupleType tt = ((TupleType) ctxt);
            if (tt.eltTypes.size() == node.binder.size()) {
                for (int i = 0; i < node.binder.size(); i++) {
                    bind(s, node.binder.get(i), tt.eltTypes.get(i), VARIABLE);
                }
            } else {
                addWarningToNode(node, "Invalid number of return values");
            }
        } else {
            bind(s, node.binder, ctxt, VARIABLE);
        }

        return visit(node.body, s);
    }

    @Override
    public Type visit(UnaryOp node, State s) {
        return visit(node.operand, s);
    }

    @Override
    public Type visit(JuliaModule node, State s) {
        ModuleType mt = new ModuleType(node.name.name, node.file, Analyzer.self.globaltable);
        s.insert($.moduleQname(node.file), node, mt, Binding.Kind.MODULE);
        if (node.body != null) {
            visit(node.body, mt.table);
        }
        return mt;
    }

    @Override
    public Type visit(StructDef node, State s) {
        StructType structType = new StructType(node.name.name, s);
        List<Type> baseType = new ArrayList<>();
        Type base = visit(node.baseType, s);
        if (base instanceof StructType) {
            structType.addBase(base);
        } else if (base instanceof UnionType) {
            for (Type parent : ((UnionType) baseType).types) {
                structType.addBase(parent);
            }
        } else {
            addWarningToNode(node.baseType, base + " is not a class");
        }
        baseType.add(base);

        bind(s, node.name, structType, DATATYPE);
        if (node.body != null) {
            visit(node.body, structType.table);
        }
        return Types.CONT;
    }

    @Override
    public Type visit(SubType node, State s) {
        return Types.BoolInstance;
    }

    @Override
    public Type visit(BaseType node, State s) {
        return Types.BoolInstance;
    }


    @Override
    public Type visit(Ref node, State s) {
        Type vt = visit(node.name, s);
        Type st = node.index == null ? null : visit(node.index, s);

        if (st instanceof UnionType) {
            Type retType = Types.UNKNOWN;
            for (Type t : ((UnionType) st).types) {
                retType = UnionType.union(retType, getRefIndex(node, t, st, s));
            }
            return retType;
        } else {
            return getRefIndex(node, vt, st, s);
        }

    }


    @Override
    public Type visit(PrimitiveType node, State s) {
        PrimType primType = new PrimType(node.name.name, s);
        List<Type> baseTypes = new ArrayList<>();
        Type base = visit(node.base, s);
        if (base instanceof PrimType) {
            primType.addBase(base);
        } else if (base instanceof UnionType) {
            for (Type parent : ((UnionType) base).types) {
                primType.addBase(parent);
            }
        } else {
            addWarningToNode(node.base, base + "is not a datatype");
        }

        baseTypes.add(base);
        bind(s, node.name, primType, DATATYPE);
        return Types.CONT;

    }

    @Override
    public Type visit(TypeDecl node, State s) {
        Type type = visit(node.type, s);
        bind(s, node.name, type);
        return Types.CONT;
    }

    @Override
    public Type visit(QuoteNode node, State s) {
        $.die("Unexpected Node");
        return null;
    }


    @Override
    public Type visit(Dot node, State s) {
        Type targetType = visit(node.target, s);
        if (targetType instanceof UnionType) {
            Set<Type> types = ((UnionType) targetType).types;
            Type retType = Types.UNKNOWN;
            for (Type tt : types) {
                retType = UnionType.union(retType, getAttrType(node, tt));
            }
            return retType;

        } else {
            return getAttrType(node, targetType);
        }
    }


    @Override
    public Type visit(LBRACE node, State s) {
        $.die("unexpected node");
        return null;
    }

    @Override
    public Type visit(RBRACE node, State s) {
        $.die("unexpected node");
        return null;
    }

    @Override
    public Type visit(ParamType node, State s) {
        Type type = visit(node.type, s);
        List<Type> params = new ArrayList<>();
        for (Node n : node.params) {
            Type t = visit(n, s);
            params.add(t);
        }
        Type paramType = new InstanceType(type, params);

        bind(s, node.type, paramType, DATATYPE);
        return paramType;
    }

    @Override
    public Type visit(Union node, State s) {
        List<Type> types = new LinkedList<>();

        for (Node n : node.types) {
            Type t = visit(n, s);
            types.add(t);
        }

        Type unionT = UnionType.newUnion(types);
        bind(s, node.name, unionT, DATATYPE);

        return unionT;
    }

    @Override
    public Type visit(Where node, State s) {
        return Types.CONT;
    }


    /**
     * Bad control flow of exception handling
     *
     * @param node catch
     * @param s    state
     * @return type
     */
    @Override
    public Type visit(Catch node, State s) {
        Type typeval = Types.UNKNOWN;
        if (node.body != null) {
            return visit(node.body, s);
        } else {
            return Types.UNKNOWN;
        }
    }

    @Override
    public Type visit(Try node, State s) {
        Type tp1 = Types.UNKNOWN;
        // Type tp2 = Types.UNKNOWN;
        Type tpc = Types.UNKNOWN;
        Type tpFinal = Types.UNKNOWN;

        if (node.catches != null) {
            for (Catch h : node.catches) {
                tpc = UnionType.union(tpc, visit(h, s));
            }
        }

        if (node.body != null) {
            tp1 = visit(node.body, s);
        }


        if (node.finallyBody != null) {
            tpFinal = visit(node.finallyBody, s);
        }

        return new UnionType(tp1, tpc, tpFinal);

    }

    @Override
    public Type visit(AbstractType node, State s) {
        PrimType primType = new PrimType(node.name.name, s);
        List<Type> baseTypes = new ArrayList<>();
        Type base = visit(node.base, s);
        if (base instanceof PrimType) {
            primType.addBase(base);
        } else if (base instanceof UnionType) {
            for (Type parent : ((UnionType) base).types) {
                primType.addBase(parent);
            }
        } else {
            addWarningToNode(node.base, base + " is not a class");
        }
        baseTypes.add(base);

        bind(s, node.name, primType, DATATYPE);
        return Types.CONT;

    }

    @Override
    public Type visit(JuliaFloat node, State s) {
        return Types.Float64Instance;
    }

    @Override
    public Type visit(Complex node, State s) {
        $.die("not implemented yet");
        return null;
    }


    @Override
    public Type visit(Missing node, State s) {
        return Types.MissingInstance;
    }

    @Override
    public Type visit(Nothing node, State s) {
        return Types.NothingInstance;
    }

    @NotNull
    private Type resolveUnion(@NotNull Collection<? extends Node> nodes, State s) {
        Type result = Types.UNKNOWN;
        for (Node node : nodes) {
            Type nodeType = visit(node, s);
            result = UnionType.union(result, nodeType);
        }
        return result;
    }


    public Type getRefIndex(Ref node, @NotNull Type vt, @Nullable Type st, State s) {
        if (vt.isUnknownType()) {
            return Types.UNKNOWN;
        } else {
            if (vt instanceof VectorType) {
                return getVecIndex(node, vt, st, s);
            } else if (vt instanceof TupleType) {
                return getVecIndex(node, ((TupleType) vt).toVectorType(), st, s);
            } else if (vt instanceof DictType) {
                DictType dt = (DictType) vt;
                if (!dt.keyType.equals(st)) {
                    addWarningToNode(node, "Possible KeyError (wrong type for subscript)");
                }
                return dt.valueType;
            } else if (vt == Types.StrInstance) {
                if (st != null && (st instanceof VectorType || st.isNumType())) {
                    return vt;
                } else {
                    addWarningToNode(node, "Possible KeyError (wrong type for subscript)");
                    return Types.UNKNOWN;
                }
            } else {
                return Types.UNKNOWN;
            }
        }

    }

    private Type getVecIndex(Ref node, Type vt, Type st, State s) {
        if (vt instanceof VectorType) {
            if (st != null && st instanceof VectorType) {
                return vt;
            } else if (st == null || st.isNumType()) {
                return ((VectorType) vt).eltType;
            } else {
                addError(node, "The type can't be sliced: " + vt);
                return Types.UNKNOWN;
            }
        } else {
            return Types.UNKNOWN;
        }
    }



    private boolean operatorOverride(Type ltype, String funcName) {
        return false;
    }

    private void setAttr(Dot node, State s, @NotNull Type v) {
        Type targetType = visit(node.target, s);
        if (targetType instanceof UnionType) {
            Set<Type> types = ((UnionType) targetType).types;
            for (Type t : types) {
                setAttrType(node, t, v);
            }
        } else {
            setAttrType(node, targetType, v);
        }
    }


    /**
     * Helper for branch inference for 'isinstance'
     */
    private void inferInstance(Node test, State s, State s1) {
        if (test instanceof Call) {
            Call testCall = (Call) test;
            if (testCall.name instanceof Symbol) {
                Symbol testFunc = (Symbol) testCall.name;
                if (testFunc.name.equals("isa")) {
                    if (testCall.args.size() >= 2) {
                        Node name = testCall.args.get(0);
                        if (name instanceof Symbol) {
                            Node typeExp = testCall.args.get(1);
                            Type type = visit(typeExp, s);
                            s1.insert(((Symbol) name).name, name, type, VARIABLE);
                        }
                    }
                }

                if (testCall.args.size() != 2) {
                    addWarningToNode(test, "Incorrect number of arguments for isinstance");
                }

            }
        }
    }

    private void setAttrType(Dot node, @NotNull Type targetType, @NotNull Type v) {
        if (targetType.isUnknownType()) {
            addWarningToNode(node, "Can't set attribute for UnknownType");
            return;
        }

        Set<Binding> bs = targetType.table.lookupAttr(node.attr.name);
        if (bs != null) {
            for (Binding b : bs) {
                b.addType(v);
                Analyzer.self.putRef(node.attr, b);
            }
        } else {
            targetType.table.insert(node.attr.name, node.attr, v, ATTRIBUTE);
        }
    }


    public Type getAttrType(Dot node, Type targetType) {
        Set<Binding> bs = targetType.table.lookupAttr(node.attr.name);
        if (bs == null) {
            addWarningToNode(node.attr, "attribute not found in type: " + targetType);
            Type t = Types.UNKNOWN;
            t.table.setPath(targetType.table.extendPath(node.attr.name));
            return t;
        } else {
            for (Binding b : bs) {
                Analyzer.self.putRef(node.attr, b);
            }
            return State.makeUnion(bs);
        }
    }

    private Type resolveCall(@NotNull Type fun,
                             @Nullable List<Type> positional,
                             @NotNull Map<String, Type> kwTypes,
                             @NotNull Call node) {
        if (fun instanceof FuncType) {
            return apply((FuncType) fun, positional, kwTypes, node);
        } else {
            addWarningToNode(node, "calling non-function and non-class: " + fun);
            return Types.UNKNOWN;
        }
    }

    public Type apply(@NotNull FuncType func,
                      @Nullable List<Type> positional,
                      @Nullable Map<String, Type> kwTypes,
                      @Nullable Call call) {

        Analyzer.self.removeUncalled(func);

        if (func != null && !func.func.called) {
            Analyzer.self.nCalled++;
            func.func.called = true;
        }

        if (func.func == null) {
            // func without definition (possibly builtins)
            return func.getReturnType();
        }

        List<Type> argTypes = new ArrayList<>();

        // Put in positional arguments
        if (positional != null) {
            argTypes.addAll(positional);
        }

        State callState = new State(func.env, State.StateType.FUNCTION);

        if (func.table.parent != null) {
            callState.setPath(func.table.parent.extendPath(func.func.name.name));
        } else {
            callState.setPath(func.func.name.name);
        }

        Type fromType = bindParams(callState, func.func, argTypes, func.defaultTypes, kwTypes);
        Type cachedTo = func.getMapping(fromType);


        if (cachedTo != null) {
            return cachedTo;
        } else if (func.oversized()) {
            return Types.UNKNOWN;
        } else {
            func.addMapping(fromType, Types.UNKNOWN);
            Analyzer.self.callStack.push(new CallStackEntry(func, fromType));
            Type toType = visit(func.func.body, callState);
            Analyzer.self.callStack.pop();
            if (missingReturn(toType)) {
                addWarningToNode(func.func.name, "Function not always return a value");
                if (call != null) {
                    addWarningToNode(call, "Call not always return a value");
                }
            }

            toType = UnionType.remove(toType, Types.CONT);
            func.addMapping(fromType, toType);

            return toType;
        }


    }

    static boolean missingReturn(@NotNull Type toType) {
        boolean hasNone = false;
        boolean hasOther = false;

        // iff toType is a Union and exists Nothing or Cont
        if (toType instanceof UnionType) {
            for (Type t : ((UnionType) toType).types) {
                if (t == Types.NothingType || t == Types.CONT) {
                    hasNone = true;
                } else {
                    hasOther = true;
                }
            }
        }
        return hasNone && hasOther;
    }

    private Type bindParams(@NotNull State state,
                            @NotNull FuncDef func,
                            @Nullable List<Type> pTypes,
                            @Nullable List<Type> dTypes,
                            @Nullable Map<String, Type> hash) {
        List<Node> params = func.params;
        Symbol rest = func.vararg;
        Symbol restKw = func.kwarg;

        TupleType fromType = new TupleType();
        int pSize = params == null ? 0 : params.size();
        int aSize = pTypes == null ? 0 : pTypes.size();
        int dSize = dTypes == null ? 0 : dTypes.size();
        int nPos = pSize - dSize;


        for (int i = 0; i < pSize; i++) {
            Node arg = params.get(i);
            Type aType;
            if (i < aSize) {
                aType = pTypes.get(i);
            } else if (i - nPos >= 0 && i - nPos < dSize) {
                aType = dTypes.get(i - nPos);
            } else {
                if (hash != null &&
                        params.get(i) instanceof Symbol &&
                        hash.containsKey(((Symbol) params.get(i)).name)) {
                    aType = hash.get(((Symbol) params.get(i)).name);
                    hash.remove(((Symbol) params.get(i)).name);
                } else {
                    aType = Types.UNKNOWN;
                    addWarningToNode(params.get(i), "Unable to bind argument: " + params.get(i));
                }

            }
            bind(state, arg, aType, PARAMETER);
            fromType.add(aType);
        }

        if (restKw != null) {
            if (hash != null && !hash.isEmpty()) {
                Type hashType = UnionType.newUnion(hash.values());
                bind(state, restKw, new DictType(Types.StrInstance, hashType), PARAMETER);
            } else {
                bind(state, restKw, Types.UNKNOWN, PARAMETER);
            }
        }

        if (rest != null && pTypes != null) {
            if (pTypes.size() > pSize) {
                Type restType = new TupleType(pTypes.subList(pSize, pTypes.size()));
                bind(state, rest, restType, PARAMETER);
            } else {
                bind(state, rest, Types.UNKNOWN, PARAMETER);

            }
        }
        return fromType;
    }


    public void bind(@NotNull State s, Node target, @NotNull Type rvalue) {
        Binding.Kind kind;
        if (s.stateType == State.StateType.FUNCTION) {
            kind = VARIABLE;
        } else if (s.stateType == State.StateType.STRUCT || s.stateType == State.StateType.INSTANCE) {
            kind = ATTRIBUTE;
        } else {
            kind = SCOPE;
        }
        bind(s, target, rvalue, kind);
    }

    public void bind(@NotNull State s, Node target, @NotNull Type rvalue, Binding.Kind kind) {
        if (target instanceof Symbol) {
            bind(s, (Symbol) target, rvalue, kind);
        } else if (target instanceof Tuple) {
            bind(s, ((Tuple) target).elts, rvalue, kind);
        } else if (target instanceof JuliaVector) {
            bind(s, ((JuliaVector) target).elts, rvalue, kind);
        } else if (target instanceof Dot) {
            setAttr((Dot) target, s, rvalue);
        } else if (target instanceof Ref) {
            Ref sub = (Ref) target;
            Type indexType = sub.index == null ? null : visit(sub.index, s);
            Type nameType = visit(sub.name, s);
            if (nameType instanceof VectorType) {
                VectorType t = (VectorType) nameType;
                t.setElementType(UnionType.union(t.eltType, rvalue));
            } else if (nameType instanceof DictType) {
                DictType t = (DictType) nameType;
                if (indexType != null) {
                    t.setKeyType(UnionType.union(t.keyType, indexType));
                }
                t.setValueType(UnionType.union(t.valueType, rvalue));
            }
        } else if (target != null) {
            addWarningToNode(target, "invalid location for assignment");
        }
    }


    public static void bind(@NotNull State s, @NotNull Symbol symbol, @NotNull Type rvalue, Binding.Kind kind) {
        if (s.isGlobalName(symbol.name)) {
            Set<Binding> bs = s.lookup(symbol.name);
            if (bs != null) {
                for (Binding b : bs) {
                    b.addType(rvalue);
                    Analyzer.self.putRef(symbol, b);
                }
            }
        } else {
            s.insert(symbol.name, symbol, rvalue, kind);
        }
    }

    public void bind(@NotNull State s, @NotNull List<Node> xs, @NotNull Type rvalue, Binding.Kind kind) {
        if (rvalue instanceof TupleType) {
            List<Type> vs = ((TupleType) rvalue).eltTypes;
            if (xs.size() != vs.size()) {
                reportUnpackMismatch(xs, vs.size());
            } else {
                for (int i = 0; i < xs.size(); i++) {
                    bind(s, xs.get(i), vs.get(i), kind);
                }
            }
        } else if (rvalue instanceof VectorType) {
            bind(s, xs, ((VectorType) rvalue).toTupleType(xs.size()), kind);
        } else if (rvalue instanceof DictType) {
            bind(s, xs, ((DictType) rvalue).toTupleType(xs.size()), kind);
        } else if (xs.size() > 0) {
            for (Node x : xs) {
                bind(s, x, Types.UNKNOWN, kind);
            }
            addWarningToFile(xs.get(0).file, xs.get(0).start, xs.get(xs.size() - 1).end, "unpacking non-iterable: " + rvalue);
        }
    }

    private void bindIter(@NotNull State s, Node target, @NotNull Node iter, Binding.Kind kind) {
        Type iterType = visit(iter, s);
        if (iterType instanceof VectorType) {
            bind(s, target, ((VectorType) iterType).eltType, kind);
        } else if (iterType instanceof TupleType) {
            bind(s, target, ((TupleType) iterType).toVectorType().eltType, kind);
        } else {
            // todo: more iterable
            bind(s, target, Types.UNKNOWN, kind);
        }
    }

    private void reportUnpackMismatch(List<Node> xs, int vsize) {
        int xsize = xs.size();
        int begin = xs.get(0).start;
        int end = xs.get(0).end;
        int diff = xsize - vsize;
        String msg;
        if (diff > 0) {
            msg = "ValueError: need more than \" + vsize + \" values to unpack";
        } else {
            msg = "ValueError: too many values to unpack";
        }
        addWarningToFile(xs.get(0).file, begin, end, msg);

    }

    private void addWarningToFile(String file, int begin, int end, String msg) {
        Analyzer.self.putProblem(file, begin, end, msg);
    }


    public static void addWarningToNode(Node node, String msg) {
        Analyzer.self.putProblem(node, msg);
    }

    public void addError(Node node, String msg) {
        Analyzer.self.putProblem(node, msg);
    }

}
