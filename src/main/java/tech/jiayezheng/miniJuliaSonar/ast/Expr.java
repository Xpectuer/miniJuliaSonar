package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

public class Expr extends Node {
    public List<Node> args;


    public Expr(List<Node> args, int start, int end, String file) {
        super(NodeType.Block, start, end, file);
        this.args = args;
        addChildren(args);
    }

    @Override
    public String toString() {
        return "<Expr: " + args + '>';
    }
}
