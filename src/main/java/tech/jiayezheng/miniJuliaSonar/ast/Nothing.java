package tech.jiayezheng.miniJuliaSonar.ast;

public class Nothing extends Node {
    public Nothing(int start, int end, String file) {
        super(NodeType.Nothing, start, end, file);
    }

    @Override
    public String toString() {
        return "nothing";
    }
}
