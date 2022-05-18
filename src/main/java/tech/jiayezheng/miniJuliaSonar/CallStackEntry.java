package tech.jiayezheng.miniJuliaSonar;

import tech.jiayezheng.miniJuliaSonar.type.FuncType;
import tech.jiayezheng.miniJuliaSonar.type.Type;

public class CallStackEntry {
    public FuncType fun;
    public Type from;

    public CallStackEntry(FuncType fun, Type from) {
        this.fun = fun;
        this.from = from;
    }
}

