package tech.jiayezheng.miniJuliaSonar.type;

import org.jetbrains.annotations.NotNull;
import tech.jiayezheng.miniJuliaSonar.State;

import java.util.ArrayList;
import java.util.List;



public class InstanceType extends Type {
    public Type ctype;
    // C{T...}
    public List<Type> params;

    public InstanceType(@NotNull Type c) {
        table.setStateType(State.StateType.INSTANCE);
        table.addSuper(c.table);
        table.setPath(c.table.path);
        ctype = c;
        params = new ArrayList<>();
    }

    public InstanceType(@NotNull Type c, List<Type> typeParams) {
        this(c);
        params.addAll(typeParams);
    }

    @Override
    public int hashCode() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.ctype.toString());
        for(Type param : params) {
            sb.append(param);
        }
        return sb.toString().hashCode();
    }

    @Override
    public boolean typeEquals(Object other) {
        if (other instanceof InstanceType) {
            return this.hashCode() == other.hashCode();
        }
        return false;
    }

    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return ctype.toString();
    }



}
