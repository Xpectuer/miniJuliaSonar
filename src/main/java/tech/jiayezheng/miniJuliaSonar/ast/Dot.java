package tech.jiayezheng.miniJuliaSonar.ast;


public class Dot extends Node {
    public Node target;
    public Symbol attr;

    public Dot(Node target, Symbol attr, int start, int end, String file) {
        super(NodeType.Dot, start, end, file);
        this.target = target;
        this.attr = attr;
    }

    @Override
    public String toString() {
        return String.format("(. %s %s)", target, attr);
    }
}
