package tech.jiayezheng.miniJuliaSonar.ast;

public class If extends Node {
    public Expr cond;
    public Node body;
    public Node orElse;

    public If(Expr cond, Block body, Node orelse, int start, int end, String file) {
        super(NodeType.If, start, end, file);
        this.cond = cond;
        this.body = body;
        this.orElse = orelse;
        addChildren(cond,body, orElse);
    }
}
