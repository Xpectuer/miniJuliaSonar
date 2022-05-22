package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.List;

public class Call extends Node {
    public Node name;
    public List<Node> args;
    public List<KW> keywords;
    // struct initialization?
    public boolean isInit = false;

    public Call(Node name, List<Node> args, List<KW> keywords, int start, int end, String file) {
        this(name,args, start, end, file);
        this.keywords = keywords;
        addChildren(keywords);
    }


    public Call(Node name, List<Node> args, int start, int end, String file) {
        this(name, start, end, file);
        this.args = args;
        addChildren(args);

    }

    public Call(Node name, int start, int end, String file) {
        super(NodeType.Call, start, end, file);
        this.name = name;
        addChildren(name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(isInit) {
            sb.append(String.format("(init %s", name));
        } else {
            sb.append(String.format("(call %s", name));
        }

        if(args != null) {
            args.forEach((s) -> sb.append(String.format(" %s", s)));
        }
        sb.append(")");
        return sb.toString();
    }

    public void setArgs(List<Node> args) {
        this.args = args;
        addChildren(args);
    }

    public void markInit() {
        this.isInit = true;
    }
}
