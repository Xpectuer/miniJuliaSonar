package tech.jiayezheng.miniJuliaSonar.ast;

public class Char extends Node {
    public Character value;
    public Char(Character value, int start, int end, String file) {
        super(NodeType.Char, start,end,file);
        this.value = value;
    }
}
