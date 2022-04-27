package tech.jiayezheng.miniJuliaSonar.ast;

public class While extends Node {
    public Expr cond;
    public Node body;

    public While(Expr cond, Block body, int start, int end, String file) {
        super(NodeType.While, start, end, file);
        this.cond = cond;
        this.body = body;

        addChildren(cond, body);
    }
}
