package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

public class JuliaVector extends Node {
    public List<Node> elts;

    public JuliaVector(List<Node> elts, int start, int end, String file) {
        super(NodeType.JuliaVector, start, end, file);
        this.elts = elts;
        addChildren(this.elts);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(vector (");
        elts.forEach((s)->sb.append(String.format(" %s",s)));
        sb.append("))");

        return sb.toString();
    }
}
