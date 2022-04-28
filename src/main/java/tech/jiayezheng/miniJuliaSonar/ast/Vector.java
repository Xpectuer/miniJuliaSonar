package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

public class Vector extends Node {
    public List<Node> args;

    public Vector(List<Node> args, int start, int end, String file) {
        super(NodeType.Vector, start, end, file);
        this.args = args;
    }
}
