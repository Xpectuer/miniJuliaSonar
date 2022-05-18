package tech.jiayezheng.miniJuliaSonar.ast;

public enum Op {
    // Arithmetic Operators
    Add("+"),
    Sub("-"),

    /**
     *       When used in multiplication, false acts as a strong zero:
     *     > NaN * false
     *       0.0
     *     > false * Inf
     *       0.0
     *      This is useful for preventing the propagation of NaN values in quantities that
     *      are known to be zero.
     */

    Mul("*"),
    Div("/"),



    IntDiv("÷"),
    InverseDiv("\\"),
    Pow("^"),
    Mod("%"),

    // Numeric Comparisons Operators
    Eq("=="),
    NotEq("!="),
    NotEqual("≠"),
    Lt("<"),
    LtE("<="),
    LtEqual("≤"),
    Gt(">"),
    GtE(">="),
    GtEqual("≥"),

    // Boolean Operators
    Not("!"),
    And("&&"),
    Or("||"),
    In("in"),
    In1("∈"),


    // Bitwise Operators
    BwNot("~"),
    BwAnd("&"),
    BwOr("|"),
    BwXor("⊻"),
    BwNand("⊼"),
    BwNor("⊽"),
    LogShfR(">>>"),
    AriShfR(">>"),
    AriShfL("<<"),

    // TODO: Vectorized Operators
    // https://docs.julialang.org/en/v1/manual/mathematical-operations/#man-dot-operators



    // miscs
    VarArg("..."),
    Mapsto("=>"),
    Where("where"),
    SubType("<:"),
    BaseType(">:"),
    Dot("."),
    FuncCombine("∘"),
    Assign("="),
    Lambda("->"),
    Range(":"),
    TypeDecl("::"),
    Unsupported(";)");

    //  ============================================================

    // Updating Operators are considered as sugar
    // +=  -=  *=  /=  \=  ÷=  %=  ^=  &=  |=  ⊻=  >>>=  >>=  <<=
    // a += 1 |-> a = a + 1

    // Chained Com


    private final String rep;
    Op(String rep) {
        this.rep = rep;
    }

    public String getRep() {return rep;}

    public static boolean isBoolean(Op op) {
        return op == Eq ||
                op == NotEq ||
                op == NotEqual ||
                op == Lt ||
                op == LtE ||
                op == LtEqual ||
                op == Gt ||
                op == GtE ||
                op == GtEqual ||
                op == In ||
                op == In1;
    }

    @Override
    public String toString() {
        return "(Op:" + rep  + ')';
    }
}
