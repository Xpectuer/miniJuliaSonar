package tech.jiayezheng.miniJuliaSonar.ast;

public class AbstractType extends Node {
    public Symbol name;
    public Symbol base;

    public AbstractType(Symbol name, Symbol base, int start, int end, String file) {
        super(NodeType.AbstractType, start, end, file);
        this.name = name;
        this.base = base;
    }

    @Override
    public String toString() {
        return "(AbstractType" +
                " " + name +
                ')';
    }
}
