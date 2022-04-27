package tech.jiayezheng.miniJuliaSonar.ast;

public class LPAREN extends Node {
    public LPAREN(int start, int end, String file) {
        super(NodeType.LPAREN, start, end, file);
    }
}
