package tech.jiayezheng.miniJuliaSonar.ast;

public class Complex extends Node {
    Node real;
    Node imaginary;

    public Complex(Node real, Node imaginary, NodeType nodeType, int start, int end, String file) {
        super(NodeType.Complex, start, end, file);
        this.real = real;
        this.imaginary = imaginary;
    }
}
