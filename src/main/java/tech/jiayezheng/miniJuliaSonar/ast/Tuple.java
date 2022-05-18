package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

public class Tuple extends Node {
    public List<Node> elts;

    public Tuple(List<Node> elts, int start, int end, String file) {
        super(NodeType.Tuple, start, end, file);
        this.elts = elts;
        addChildren(this.elts);
    }

    public List<Node> unPack() {
        return this.elts;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(tuple ");
        for(Node n: elts) {
            sb.append(" ").append(n);
        }
        return sb.append(")").toString();
    }
}
