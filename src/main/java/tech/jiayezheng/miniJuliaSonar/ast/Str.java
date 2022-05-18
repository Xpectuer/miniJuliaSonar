package tech.jiayezheng.miniJuliaSonar.ast;

public class Str extends Node {
    public String s;
    public Str(String s, int start, int end, String file) {
        super(NodeType.Str, start, end, file);
        this.s = s;
    }

    @Override
    public String toString() {
        return String.format("\"%s\"",s);
    }
}
