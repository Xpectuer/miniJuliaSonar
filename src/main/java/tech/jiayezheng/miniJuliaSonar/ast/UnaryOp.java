package tech.jiayezheng.miniJuliaSonar.ast;

public class UnaryOp extends Node {
    public Op op;
    public Node operand;

    public UnaryOp(Op op, Node operand, int start, int end, String file) {
        super(NodeType.UnaryOp, start, end, file);
        this.op = op;
        this.operand = operand;
        addChildren(operand);
    }

    @Override
    public String toString() {
        return String.format("(UnaryOp %s %s)", this.op.getRep(), this.operand);
    }
}
