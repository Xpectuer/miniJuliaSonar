package tech.jiayezheng.miniJuliaSonar.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.jiayezheng.miniJuliaSonar.State;

import java.util.*;


public class StructType extends Type {
    private Map<String, InstanceType> instances;
    public String name;
    public List<Type> baseType;

    public StructType(@NotNull String name, @Nullable State parent) {
        this.name = name;
        this.setTable(new State(parent, State.StateType.GLOBAL));

        table.setType(this);
        if (parent != null) {
            table.setPath(parent.extendPath(name));
        } else {
            table.setPath(name);
        }
    }

    public StructType(@NotNull String name, @Nullable State parent, @Nullable Type baseType) {
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

    public Map<String, InstanceType> getInstances() {
        if (instances == null) {
            instances = new HashMap<>();
        }
        return instances;
    }

    public InstanceType getInstance(List<Type> typeArgs) {
        String key = makeKey(this, typeArgs);
        return instances.get(key);
    }


    public void addInstance(@NotNull InstanceType instance) {
        if (this.instances == null) {
            this.instances = new HashMap<>();
        }
        this.instances.put(instance.toString(), instance);
    }

    @Override
    public boolean typeEquals(Object other) {
        return this == other;
    }

    @Override
    protected String printType(Type.CyclicTypeRecorder ctr) {
        return "<struct :" + name + ">";
    }

}
