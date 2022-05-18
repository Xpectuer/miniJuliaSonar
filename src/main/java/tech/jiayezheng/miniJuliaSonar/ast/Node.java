package tech.jiayezheng.miniJuliaSonar.ast;

import java.util.Collection;
import java.util.Objects;

public abstract class Node implements java.io.Serializable, Comparable<Object> {
    public NodeType nodeType;
    public int start;
    public int end;

    public String file;
    public Node parent = null;
    public String name;

    public Node() {

    }


    public Node(NodeType nodeType, int start, int end, String file) {
        this.nodeType = nodeType;
        this.start = start;
        this.end = end;
        this.file = file;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public Node getASTRoot() {
        if (parent == null) {
            return this;
        }
        return parent.getASTRoot();
    }

    public void addChildren(Node... nodes) {
        if (nodes != null) {
            for (Node node : nodes) {
                if (node != null) {
                    node.setParent(this);
                }
            }
        }
    }

    public void addChildren(Collection<? extends Node> nodes) {
        if (nodes != null) {
            for (Node node : nodes) {
                if (node != null) {
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
        return (file + ":" + start + ":" + end).hashCode();
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Node) {
            return start - ((Node) o).start;
        } else {
            return -1;
        }
    }

    @Override
    public String toString() {
        return "(Node" +
                " file:'" + file + '\'' +
                " start:'" + start + '\'' +
                ')';
    }

    public  String toDisplay() {return "";}
}
