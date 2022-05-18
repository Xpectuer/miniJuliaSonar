package tech.jiayezheng.miniJuliaSonar.type;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.jiayezheng.miniJuliaSonar.State;

import java.util.ArrayList;
import java.util.List;


// abstract, primitive or composite type
public class PrimType extends Type {
    private InstanceType instance;
    public String name;
    public List<Type> baseType;

    public PrimType(@NotNull String name, @Nullable State parent) {
        this.name = name;
        this.setTable(new State(parent, State.StateType.GLOBAL));

        table.setType(this);
        if (parent != null) {
            table.setPath(parent.extendPath(name));
        } else {
            table.setPath(name);
        }
    }

    public PrimType(@NotNull String name, @Nullable State parent, @Nullable Type baseType) {
        this(name, parent);
        if (baseType != null) {
            addBase(baseType);
        }
    }

    public void addBase(@NotNull Type baseType) {
        if(this.baseType == null) {
            this.baseType = new ArrayList<>(1);
        }
        this.baseType.add(baseType);
        table.addSuper(baseType.table);
    }

    public InstanceType getInstance() {
        if (instance == null) {
            instance = new InstanceType(this);
        }
        return instance;
    }


    public void setInstance(InstanceType instance) {
        this.instance = instance;
    }

    @Override
    public boolean typeEquals(Object other) {
        return this == other;
    }

    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return "<" + name + ">";
    }


}
