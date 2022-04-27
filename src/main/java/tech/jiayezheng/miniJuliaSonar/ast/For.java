package tech.jiayezheng.miniJuliaSonar.ast;

public class For extends Node {
    public Assign assign;
    public Block body;

    public For(Assign assign, Block body, int start, int end, String file) {
        super(NodeType.For, start, end, file);
        this.assign = assign;
        this.body = body;
    }

    @Override
    public String toString() {
        return String.format("(for %s %s)", assign, body);
    }
}
