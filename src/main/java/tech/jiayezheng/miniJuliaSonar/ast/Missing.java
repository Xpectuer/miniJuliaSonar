package tech.jiayezheng.miniJuliaSonar.ast;

public class Missing extends Node {
    public Missing(int start, int end, String file) {
        super(NodeType.Missing, start, end, file);
    }

    @Override
    public String toString() {
        return "missing";
    }
}
