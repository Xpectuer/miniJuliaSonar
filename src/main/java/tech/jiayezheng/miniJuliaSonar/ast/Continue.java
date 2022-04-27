package tech.jiayezheng.miniJuliaSonar.ast;

public class Continue extends Node{
    public Continue(int start, int end, String file) {
        super(NodeType.Continue, start, end, file);
    }
}
