package tech.jiayezheng.miniJuliaSonar.ast;

public class Break extends Node{
    public Break( int start, int end, String file) {
        super(NodeType.Break, start, end, file);
    }
}
