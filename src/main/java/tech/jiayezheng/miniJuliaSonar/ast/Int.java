package tech.jiayezheng.miniJuliaSonar.ast;

public class Int extends Node{
    // Note: python has no integer size limit
    public String value;

    public Int(String value, int start, int end, String file) {
        super(NodeType.Integer, start, end, file);
        this.value = value;
    }
}
