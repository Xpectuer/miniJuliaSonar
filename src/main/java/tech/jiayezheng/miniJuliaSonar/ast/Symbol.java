package tech.jiayezheng.miniJuliaSonar.ast;

public class Symbol extends Node {
    public String name;

    public Symbol(String name, int start, int end, String file) {
        super(NodeType.Symbol, start, end, file);
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
