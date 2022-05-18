package tech.jiayezheng.miniJuliaSonar.ast;



public class StructDef extends Node {
    public boolean mutable;
    public Symbol name;
    public Node baseType;
    public Block body;

    public StructDef(boolean mutable, Symbol name, Node baseType, Block body, int start, int end, String file) {
        super(NodeType.StructDef, start, end, file);
        this.mutable = mutable;
        this.name = name;
        this.body = body;
        this.baseType = baseType;
        addChildren(name, baseType);
        addChildren(body);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("(JuliaStruct %s [", name));
        sb.append(body);
        sb.append("])");
        return sb.toString();
    }
}
