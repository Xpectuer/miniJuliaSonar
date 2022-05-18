package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

// (let [x y] body...+)
public class Do extends Node {
    public List<Node> binder;
    public Node value;
    public Block body;

    public Do(Node value, List<Node> target, Block body, int start, int end, String file) {
        super(NodeType.Do, start, end, file);
        this.binder = target;
        this.value = value;
        this.body = body;
        addChildren(target);
        addChildren(value, body);
    }

    @Override
    public String toString() {
        return String.format("(Do [%s %s]  body...)", binder,value);
    }
}
