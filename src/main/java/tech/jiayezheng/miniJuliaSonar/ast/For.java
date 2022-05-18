package tech.jiayezheng.miniJuliaSonar.ast;


public class For extends Node {
    public Node target;
    public Node iter;
    public Block body;

    public For(Node target, Node iter, Block body, int start, int end, String file) {
        super(NodeType.For, start, end, file);
        this.target = target;
        this.iter = iter;
        addChildren(target, iter);
        addChildren(body);
    }

    @Override
    public String toString() {
        return String.format("(for %s in %s %s)", target, iter, body);
    }
}
