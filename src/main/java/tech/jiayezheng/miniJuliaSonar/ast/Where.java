package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

public class Where extends Node {
    public ParamType paramType;
    public List<BinOp> constraints;

    public Where(ParamType paramType, List<BinOp> constraints, int start, int end, String file) {
        super(NodeType.Where, start, end, file);
        this.paramType = paramType;
        this.constraints = constraints;
    }
}
