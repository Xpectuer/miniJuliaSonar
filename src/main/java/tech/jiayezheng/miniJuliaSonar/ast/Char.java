package tech.jiayezheng.miniJuliaSonar.ast;

public class Char extends Node {
    public String value;

    public Char(String value, int start, int end, String file) {
        super(NodeType.Char, start, end, file);
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("'%s'", value);
    }
}
