package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;
import java.util.Map;

public class Global extends Node {
    public List<Node> names;

    public Global(List<Node> value, int start, int end, String file) {
        super(NodeType.Global, start, end, file);
        this.names = names;
    }


}
