package tech.jiayezheng.miniJuliaSonar.ast;

public class Operator extends Node{
    public Op op;

    public Operator(Op op, int start, int end, String file) {
        super(NodeType.Operator, start, end, file);
        this.op = op;
    }

    @Override
    public String toString() {
        return "(Operator:" + op.getRep() + this.start+')';
    }
}
