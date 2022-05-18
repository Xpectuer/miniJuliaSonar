package tech.jiayezheng.miniJuliaSonar.ast;

import tech.jiayezheng.miniJuliaSonar.$;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * assume: a = 2
 */
public class Assign extends Node {

    public Node target;
    public Node value;
    public boolean nonLocal;

    public Assign(Node target, boolean nonLocal, Node value, int start, int end, String file) {
        super(NodeType.Assign, start, end, file);

        this.target = target;
        this.value = value;
        addChildren(target);
        addChildren(value);
    }

    public List<Node> unpackTargetsSymbols() {
        if (target instanceof Symbol) {
            return Collections.singletonList((Symbol) target);
        } else if (target instanceof Tuple) {
            List<Node> elements = ((Tuple) target).elts;
            return elements.stream().filter(n -> n.nodeType == NodeType.Symbol).collect(Collectors.toList());
        }
        $.die("unexpected target");
        return null;
    }

    @Override
    public String toString() {
        return String.format("(assign %s %s)", target, value);
    }
}
