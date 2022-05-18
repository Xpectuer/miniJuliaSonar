package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

public class Catch extends Node {
    public List<Node> binders;
    public Block body;

    public Catch(List<Node> binders, Block body, int start, int end, String file) {
        super(NodeType.Catch, start, end, file);
        this.binders = binders;
        this.body = body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(catch ");
        sb.append(" [");
        binders.forEach((s) -> sb.append(String.format(" %s", s)));
        sb.append("] ");
        sb.append(body);
        sb.append(")");
        return sb.toString();
    }
}
