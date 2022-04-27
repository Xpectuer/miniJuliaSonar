package tech.jiayezheng.miniJuliaSonar.ast;

public class KeyWord extends Node {
    public String name;

    public KeyWord(String name, int start, int end, String file) {
        super(NodeType.KeyWord, start, end, file);
        this.name = name;
    }

    @Override
    public String toString() {
        return "(keyword:" + name + ")";

    }
}
