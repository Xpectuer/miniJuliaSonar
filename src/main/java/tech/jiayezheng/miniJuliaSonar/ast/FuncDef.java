package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class FuncDef extends Node {
    public Symbol name;
    public List<Node> params;
    // optional positional arguments must occur at end
    //  f️(a,b=1) ☑️
    //  f(a=1,b) ❌
    public List<Node> defaults;
    public Symbol vararg;
    public Symbol kwarg;

    public Node body;
    public End end;
    public boolean called = false;
    public boolean isLambda = false;


    public FuncDef(Symbol name, List<Node> params, List<Node> defaults, Node body, int start, int end, String file) {
        super(NodeType.FuncDef, start, end, file);
        if (name != null) {
            this.name = name;
        } else {
            isLambda = true;
            String fn = genLambdaName();
            this.name = new Symbol(fn, start, end, file);
        }


        this.params = params;
        this.defaults = defaults;
        this.body = body;
        addChildren(name, body);
        addChildren(params);
        addChildren(defaults);
    }

    public void setVararg(Symbol vararg) {
        this.vararg = vararg;
        addChildren(vararg);
    }

    public void setKWarg(Symbol kwarg) {
        this.kwarg = kwarg;
        addChildren(kwarg);
    }

    private static int lambdaCounter = 0;

    private static String genLambdaName() {
        String ret = "lambda@" + lambdaCounter;
        lambdaCounter = lambdaCounter + 1;
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("(define (");
        sb.append(this.name);
        sb.append(" ");

        Deque<Node> stack_kw = new LinkedList<>();

        assert defaults.size() <= params.size();

        //  |s|
        //  |d|
        int j = params.size() - 1;
        for (int i = defaults.size() - 1; i >= 0; i--, j--) {
            stack_kw.addLast(defaults.get(i));
            stack_kw.addLast(params.get(j));
        }

        // |s|
        Deque<Node> stack_symbol = new LinkedList<>();
        for (; j >= 0; j--) {
            stack_symbol.addLast(params.get(j));
        }

        while (!stack_symbol.isEmpty()) {
            sb.append(stack_symbol.pollLast());
            sb.append(" ");
        }
        while (!stack_kw.isEmpty()) {
            sb.append(stack_kw.pollLast());
            sb.append("=");
            sb.append(stack_kw.pollLast());
            sb.append(" ");
        }

        sb.append(") body...)");
        return sb.toString();
    }
}
