package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

public class Tuple extends Node {
    public List<Node> args;

    public Tuple(List<Node> args, int start, int end, String file) {
        super(NodeType.Tuple, start, end, file);
        this.args = args;
    }
}
