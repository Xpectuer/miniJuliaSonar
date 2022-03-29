package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public abstract class Node implements java.io.Serializable, Comparable<Object> {
    public NodeType nodeType;
    public int line;
    // Note: NOT the char col; but the position of lex of its line.
    public int col;
    public int start;
    public int end;

    public String name;
    public String file;
    public Node parent = null;

    public Node() {

    }


    public Node(NodeType nodeType, int line, int col, int start, int end,String file ,String name, Node parent) {
        this.nodeType = nodeType;
        this.line = line;
        this.col = col;
        this.start = start;
        this.end = end;
        this.name = name;
        this.parent = parent;
        this.file = file;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public Node getASTRoot() {
        if(parent == null) {
            return this;
        }
        return parent.getASTRoot();
    }

    public void addChildren(Node... nodes) {
        if(nodes != null) {
            for(Node node : nodes) {
                if(node != null) {
                    node.setParent(this);
                }
            }
        }
    }

    public void addChildren(Collection<? extends Node> nodes) {
        if(nodes != null) {
            for(Node node : nodes) {
                if(node != null) {
                    node.setParent(this);
                }
            }
        }
    }

    public int length() {
        return end - start;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return start == node.start && end == node.end && Objects.equals(file, node.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, file);
    }

    @Override
    public int compareTo(Object o) {
        if(o instanceof Node) {
            return start - ((Node) o).start;
        } else {
            return -1;
        }
    }

    @Override
    public String toString() {
        return "Node{"+
                ", name='" + name+
                ", file='" + file + '\'' +
                "start=" + start +  '\'' +
                '}';
    }
}
