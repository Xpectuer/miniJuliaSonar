package tech.jiayezheng.miniJuliaSonar.ast;

public class QuoteNode extends Node {
    public Symbol name;

    public QuoteNode(Symbol name,int start, int end, String file) {
        super(NodeType.QuoteNode, start, end, file);
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("'%s",name);
    }
}
