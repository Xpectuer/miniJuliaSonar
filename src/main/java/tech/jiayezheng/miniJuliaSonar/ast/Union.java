package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

public class Union extends Node {
    public Symbol name;
    public List<Node> types;

    public Union(List<Node> unions, int start, int end, String file) {
        super(NodeType.UnionType, start, end, file);
        this.types = unions;
        name = makeUnionSymbol(types);
    }

    private Symbol makeUnionSymbol(List<Node> unions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Union").append("{");
        unions.forEach(n -> sb.append(n).append(','));
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return new Symbol(sb.toString(), start, end, file);
    }

}
