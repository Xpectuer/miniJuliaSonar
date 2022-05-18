package tech.jiayezheng.miniJuliaSonar.ast;

public class PrimitiveType extends Node {
    public Symbol name;
    public Symbol base;
    public int size;

    public PrimitiveType(Symbol name, Symbol base, int size, int start, int end, String file) {
        super(NodeType.PrimitiveType, start, end, file);
        this.name = name;
        this.base = base;
        this.size = size;
    }

    @Override
    public String toString() {
        if (base != null) {
            return String.format("(PrimType %s %d)", name, size);
        }
        return String.format("(PrimType %s <: %s %d)", name, base, size);
    }
}
