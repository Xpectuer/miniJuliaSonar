package tech.jiayezheng.miniJuliaSonar.ast;

public class VarArg extends Node {
    Symbol name;

    public VarArg(Symbol name, int start, int end, String file ) {
        super(NodeType.VarArg, start, end, file);
        this.name = name;
    }
}
