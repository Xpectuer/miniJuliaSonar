package tech.jiayezheng.miniJuliaSonar.ast;

public class KW extends Node {
    public Symbol key;
    public Node value;

    public KW(Symbol key, Node value, int start, int end, String file) {
        super(NodeType.KW, start, end, file);
        this.key = key;
        this.value = value;
    }
}
