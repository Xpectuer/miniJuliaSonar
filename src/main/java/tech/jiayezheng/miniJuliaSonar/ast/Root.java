package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

public class Root extends Node{
    public List<Node> args;

    public Root( List<Node> args, String file) {
        super(NodeType.Root, 0, 0, file);
        this.args = args;
        addChildren(args);
    }

    @Override
    public String toString() {
        return "(ROOT "+args+")";

    }
}
