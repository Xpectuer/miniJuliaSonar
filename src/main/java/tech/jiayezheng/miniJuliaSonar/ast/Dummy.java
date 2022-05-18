package tech.jiayezheng.miniJuliaSonar.ast;

/**
 * dummy node for locating purposes only
 * rarely used
 */
public class Dummy extends Node {

    public Dummy( int start, int end,String file) {
        super(NodeType.DUMMY, start, end, file);
    }

}

