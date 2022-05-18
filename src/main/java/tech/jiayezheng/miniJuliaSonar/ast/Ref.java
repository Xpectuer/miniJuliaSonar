package tech.jiayezheng.miniJuliaSonar.ast;

public class Ref extends Node {
    public Node name;
    // set/dict ref & list ref
    public Node index;

    public Ref(Node name, Node index, int start, int end, String file) {
        super(NodeType.Ref, start, end, file);
        this.name = name;
        this.index = index;
    }

    @Override
    public String toString() {
        return String.format("(Ref %s %s)",name, index);
    }
}
