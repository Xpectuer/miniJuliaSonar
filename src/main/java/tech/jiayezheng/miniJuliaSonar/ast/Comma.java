package tech.jiayezheng.miniJuliaSonar.ast;

public class Comma extends Node {
    public Comma(int start, int end, String file) {
        super(NodeType.Comma, start, end, file);
    }
}
