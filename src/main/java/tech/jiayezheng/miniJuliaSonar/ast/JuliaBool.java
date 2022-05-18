package tech.jiayezheng.miniJuliaSonar.ast;

public class JuliaBool extends Node {
    public enum BoolValue {
        TRUE,
        FALSE,
        UNKNOWN
    }

    BoolValue value;

    public JuliaBool(String value, int start, int end, String file) {
        super(NodeType.JuliaBool, start, end, file);
        this.value = asBoolValue(value);
    }

    public static BoolValue asBoolValue(String value) {
        if (value.equals("true")) {
            return BoolValue.TRUE;
        } else if (value.equals("false")) {
            return BoolValue.FALSE;
        } else {
            return BoolValue.UNKNOWN;
        }
    }

    @Override
    public String toString() {
        return value.name();
    }
}
