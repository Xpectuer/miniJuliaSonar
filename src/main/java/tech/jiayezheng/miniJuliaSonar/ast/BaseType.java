package tech.jiayezheng.miniJuliaSonar.ast;

public class BaseType extends Node{
    public Symbol baseType;
    public Symbol subType;
    public final Op op = Op.BaseType;

    public BaseType(Symbol baseType, Symbol subType, int start, int end, String file) {
        super(NodeType.BaseType, start, end, file);
        this.baseType = baseType;
        this.subType = subType;
        addChildren(baseType,subType);
    }
}

