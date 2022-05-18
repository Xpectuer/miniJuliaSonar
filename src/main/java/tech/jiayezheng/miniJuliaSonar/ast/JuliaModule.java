package tech.jiayezheng.miniJuliaSonar.ast;

public class JuliaModule extends Node {
    public Symbol name;
    public Block body;

    public JuliaModule(Symbol name, Block block, int start, int end, String file) {
        super(NodeType.JuliaModule, start, end, file);
        this.name = name;
        this.body = block;
        addChildren(block);
    }

    @Override
    public String toString() {
        return String.format("(module %s %s)",name, body);
    }
}
