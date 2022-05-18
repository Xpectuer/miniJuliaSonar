package tech.jiayezheng.miniJuliaSonar.ast;

public class JuliaInt extends Node{
    // Note: python has no integer size limit
    public String value;

    public JuliaInt(String value, int start, int end, String file) {
        super(NodeType.JuliaInt, start, end, file);
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
