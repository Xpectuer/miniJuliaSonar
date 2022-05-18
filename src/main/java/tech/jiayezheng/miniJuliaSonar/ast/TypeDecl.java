package tech.jiayezheng.miniJuliaSonar.ast;

public class TypeDecl extends Node {
    public Symbol name;
    // Where | Symbol
    public Node type;

    public TypeDecl(Symbol name, Node type, int start, int end, String file) {
        super(NodeType.TypeDecl, start, end, file);
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("(:: %s %s)", name, type);
    }
}
