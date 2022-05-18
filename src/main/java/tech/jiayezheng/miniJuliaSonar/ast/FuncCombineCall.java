package tech.jiayezheng.miniJuliaSonar.ast;


import java.util.List;

public class FuncCombineCall extends Node{
    // (f∘g∘h)
    public Block pipes;
    public List<Node> args;
    public FuncCombineCall(Block pipes, List<Node> args, int start, int end, String file) {
        super(NodeType.FuncCombine, start, end, file);
        this.pipes = pipes;
        this.args = args;
        addChildren(pipes);
        addChildren(args);

    }
}
