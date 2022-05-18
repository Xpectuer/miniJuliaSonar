package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;


public class Try extends Node {
    public Block body;
    public List<Catch> catches;
    public Block finallyBody;

    public Try(Block tryBody, List<Catch> catches, Block finallyBody, int start, int end, String file) {
        super(NodeType.Try, start, end, file);
        this.body = tryBody;
        this.catches = catches;
        this.finallyBody = finallyBody;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(try ");
        sb.append(body);
        sb.append(" ");
        catches.forEach((s) -> sb.append(String.format(" [%s]", s)));
        sb.append(" ");
        sb.append(String.format("(finally %s)", finallyBody));
        sb.append(")");
        return sb.toString();
    }
}
