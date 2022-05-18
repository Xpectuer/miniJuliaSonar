package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

public class Block extends Node {
    public List<Node> args;

    public Block(List<Node> args, int start, int end, String file) {
        super(NodeType.Block, start, end, file);
        this.args = args;
        addChildren(args);
    }

    @Override
    public String toString() {
        return "(Block " +
                  args +
                ')';
    }
}
