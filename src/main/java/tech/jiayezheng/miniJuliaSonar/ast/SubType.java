package tech.jiayezheng.miniJuliaSonar.ast;

public class SubType extends Node {
    public Symbol subType;
    public Symbol baseType;
    public final Op op = Op.SubType;

    public SubType(Symbol subType, Symbol baseType, int start, int end, String file) {
        super(NodeType.SubType, start, end, file);
        this.subType = subType;
        this.baseType = baseType;
        addChildren(subType, baseType);
    }

    @Override
    public String toString() {
        return String.format("(<: %s %s)", this.subType, this.baseType);
    }
}
