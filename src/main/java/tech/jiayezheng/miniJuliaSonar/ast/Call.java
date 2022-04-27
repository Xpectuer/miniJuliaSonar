package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

public class Call extends Node {
    public Symbol name;
    public List<Node> args;

    public Call(Symbol name, List<Node> args, int start, int end, String file) {
        super(NodeType.Call, start, end, file);
        this.name = name;
        this.args = args;
    }
}
