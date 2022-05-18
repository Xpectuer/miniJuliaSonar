package tech.jiayezheng.miniJuliaSonar.ast;

public class VarArg extends Node {
    public Symbol name;

    public VarArg(Symbol name, int start, int end, String file ) {
        super(NodeType.VarArg, start, end, file);
        this.name = name;
        addChildren(name);
    }
}
