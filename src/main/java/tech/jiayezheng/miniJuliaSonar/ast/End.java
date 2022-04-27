package tech.jiayezheng.miniJuliaSonar.ast;

public class End extends Node {
    public End(int start, int end, String file) {
        super(NodeType.End, start,end,file);
    }
}
