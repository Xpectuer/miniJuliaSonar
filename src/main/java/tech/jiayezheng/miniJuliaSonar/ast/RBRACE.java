package tech.jiayezheng.miniJuliaSonar.ast;

public class RBRACE extends Node {
    public RBRACE(int start, int end, String file) {
        super(NodeType.RBRACE, start, end, file);
    }
}
