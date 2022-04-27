package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

/**
 * assume: a = 2
 */
public class Assign extends Node {

    public List<Node> args;
    public Node target;
    public Node value;

    public Assign(Node target, Node value, int start, int end, String file) {
        super(NodeType.Assign, start, end, file);

        this.target = target;
        this.value = value;
        addChildren(target);
        addChildren(value);
    }
}
