package tech.jiayezheng.miniJuliaSonar.ast;

public class BinOp extends Node {
    public Op op;
    public Node left;
    public Node right;

    public BinOp(Op op, Node left, Node right, int start, int end, String file) {
        super(NodeType.BinOp, start, end, file);
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return String.format("(%s %s %s)", this.op.getRep(), left, right);
    }
}
