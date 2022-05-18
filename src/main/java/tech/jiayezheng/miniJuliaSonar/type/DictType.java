package tech.jiayezheng.miniJuliaSonar.type;



public class DictType extends Type {
    public Type keyType;
    public Type valueType;

    public DictType(Type key0, Type val0) {
        keyType = key0;
        valueType = val0;
        table.addSuper(Types.BaseDict.table);
        table.setPath(Types.BaseDict.table.path);
    }

    @Override
    public boolean typeEquals(Object other) {
        return false;
    }

    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return null;
    }

    public void setKeyType(Type keyType) {
        this.keyType = keyType;
    }

    public void setValueType(Type valueType) {
        this.valueType = valueType;
    }

    public Type toTupleType(int n) {
        TupleType ret = new TupleType();
        for(int i = 0; i < n;i ++) {
            ret.add(keyType);
        }
        return ret;
    }
}
