package tech.jiayezheng.miniJuliaSonar.ast;

public class RPAREN extends Node {
    public RPAREN(int start, int end, String file) {
        super(NodeType.RPAREN, start,end,file);
    }
}
