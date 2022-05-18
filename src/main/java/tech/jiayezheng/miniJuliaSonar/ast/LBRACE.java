package tech.jiayezheng.miniJuliaSonar.ast;

public class LBRACE extends Node {
    public LBRACE(int start, int end, String file) {
        super(NodeType.LBRACE, start, end, file);
    }
}
