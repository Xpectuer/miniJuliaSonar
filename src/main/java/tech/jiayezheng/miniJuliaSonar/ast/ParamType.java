package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

public class ParamType extends Node {
    public Symbol type;
    public List<Node> params;

    public ParamType(Symbol type, List<Node> params, int start, int end, String file) {
        super(NodeType.ParamType, start, end, file);
        this.type = type;
        this.params = params;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append("ParamType ");
        sb.append(type).append(" [");
        for (Node n : params) {
            sb.append(" " + n);
        }
        sb.append("])");
        return sb.toString();
    }
}
