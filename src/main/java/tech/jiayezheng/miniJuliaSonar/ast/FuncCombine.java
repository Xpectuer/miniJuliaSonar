package tech.jiayezheng.miniJuliaSonar.ast;

public class FuncCombine extends Node{
    public FuncCombine(int start, int end, String file) {
        super(NodeType.FuncCombine, start, end, file);
    }
}
