package tech.jiayezheng.miniJuliaSonar.ast;


public class JuliaFloat extends Node {
    public String value;

    public JuliaFloat( String value, int start, int end, String file) {
        super(NodeType.JuliaFloat, start, end, file);
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
