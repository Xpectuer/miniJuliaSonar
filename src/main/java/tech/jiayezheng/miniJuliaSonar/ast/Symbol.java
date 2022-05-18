package tech.jiayezheng.miniJuliaSonar.ast;


import org.jetbrains.annotations.NotNull;

public class Symbol extends Node {
    // public String name;
    public SymbolKind type;

    public Symbol(String name, int start, int end, String file) {
        super(NodeType.Symbol, start, end, file);
        this.name = name;
        this.type = SymbolKind.LOCAL;
    }


    public boolean isAttribute() {
        return parent instanceof Dot && ((Dot) parent).attr == this;
    }


    @NotNull
    @Override
    public String toString() {
        return name;
    }

    @NotNull
    @Override
    public String toDisplay() {
        return name;
    }

}
