package tech.jiayezheng.miniJuliaSonar.ast;

import org.jetbrains.annotations.NotNull;

public class Return extends Node {
    public Node value;
    public Return(Node n,  int start, int end, String file) {
        super(NodeType.Return, start, end,file);
        this.value = n;
        addChildren(n);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Return:" + value + ">";
    }
}
