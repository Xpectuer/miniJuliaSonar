package tech.jiayezheng.miniJuliaSonar.type;

/** Generated Code, DO NOT MODIFY!!!
 * This file is generated by gen_julia_types.py
 * target = java
 */
public class Types {


    public static PrimType AnyType = new PrimType("Any", null);



    public static PrimType NothingType = new PrimType("Nothing", null, AnyType);
    public static Type NothingInstance = NothingType.getInstance();

    public static PrimType MissingType = new PrimType("Missing", null, AnyType);
    public static Type MissingInstance = MissingType.getInstance();

    public static PrimType NumberType = new PrimType("Number", null, AnyType);
    public static PrimType AbstractCharType = new PrimType("AbstractChar", null, AnyType);
    public static PrimType RealType = new PrimType("Real", null, NumberType);
    public static PrimType AbstractFloatType = new PrimType("AbstractFloat", null, RealType);
    public static PrimType IntegerType = new PrimType("Integer", null, RealType);
    public static PrimType SignedType = new PrimType("Signed", null, IntegerType);
    public static PrimType UnsignedType = new PrimType("Unsigned", null, IntegerType);


    public static PrimType Float16Type = new PrimType("Float16", null, AbstractFloatType);
    public static Type Float16Instance = Float16Type.getInstance();


    public static PrimType Float32Type = new PrimType("Float32", null, AbstractFloatType);
    public static Type Float32Instance = Float32Type.getInstance();


    public static PrimType Float64Type = new PrimType("Float64", null, AbstractFloatType);
    public static Type Float64Instance = Float64Type.getInstance();


    public static PrimType BoolType = new PrimType("Bool", null, IntegerType);
    public static Type BoolInstance = BoolType.getInstance();


    public static PrimType CharType = new PrimType("Char", null, AbstractCharType);
    public static Type CharInstance = CharType.getInstance();


    public static PrimType Int8Type = new PrimType("Int8", null, SignedType);
    public static Type Int8Instance = Int8Type.getInstance();


    public static PrimType UInt8Type = new PrimType("UInt8", null, UnsignedType);
    public static Type UInt8Instance = UInt8Type.getInstance();


    public static PrimType Int16Type = new PrimType("Int16", null, SignedType);
    public static Type Int16Instance = Int16Type.getInstance();


    public static PrimType UInt16Type = new PrimType("UInt16", null, UnsignedType);
    public static Type UInt16Instance = UInt16Type.getInstance();


    public static PrimType Int32Type = new PrimType("Int32", null, SignedType);
    public static Type Int32Instance = Int32Type.getInstance();


    public static PrimType UInt32Type = new PrimType("UInt32", null, UnsignedType);
    public static Type UInt32Instance = UInt32Type.getInstance();


    public static PrimType Int64Type = new PrimType("Int64", null, SignedType);
    public static Type Int64Instance = Int64Type.getInstance();


    public static PrimType UInt64Type = new PrimType("UInt64", null, UnsignedType);
    public static Type UInt64Instance = UInt64Type.getInstance();


    public static PrimType Int128Type = new PrimType("Int128", null, SignedType);
    public static Type Int128Instance = Int128Type.getInstance();


    public static PrimType UInt128Type = new PrimType("UInt128", null, UnsignedType);
    public static Type UInt128Instance = UInt128Type.getInstance();



    //  Synthetic types used only for inference purposes
    //  They don't exist in Julia

    public static Type UNKNOWN = new InstanceType(new PrimType("?", null));
    public static PrimType DataTypeType = new PrimType("DataType", null, null);
    public static Type DataTypeInstance = DataTypeType.getInstance();

    // Virtual Node for reachable Node detection
    public static Type CONT = new InstanceType(new PrimType("None", null));
    public static PrimType BaseDict = new PrimType("dict", null);

    public static PrimType BaseVector = new PrimType("vector", null);
    public static Type BaseVectorInst = BaseVector.getInstance();

    public static PrimType BaseTuple = new PrimType("tuple", null);
    public static Type BaseTupleInst = BaseTuple.getInstance();

    // Extended Synthetic Types
    public  static  Type StrInstance = new StrType(null);
    public static UnionType UnionAll = new UnionType();
}


/**
 * abstract type Any end
 * primitive type Nothing end
 * primitive type Missing end
 * abstract type Number end
 * abstract type AbstractChar end
 * abstract type Real <: Number end
 * primitive type AbstractFloat <: Real end
 * primitive type Integer <: Real end
 * primitive type Signed <: Integer end
 * primitive type Unsigned <: Integer end
 *
 *
 * primitive type Float16 <: AbstractFloat 16 end
 * primitive type Float32 <: AbstractFloat 32 end
 * primitive type Float64 <: AbstractFloat 64 end
 * primitive type Bool <: Integer 8 end
 * primitive type Char <: AbstractChar 32 end
 * primitive type Int8 <: Signed 8 end
 * primitive type UInt8 <: Unsigned 8 end
 * primitive type Int16 <: Signed 16 end
 * primitive type UInt16 <: Unsigned 16 end
 * primitive type Int32 <: Signed 32 end
 * primitive type UInt32 <: Unsigned 32 end
 * primitive type Int64 <: Signed 64 end
 * primitive type UInt64 <: Unsigned 64 end
 * primitive type Int128 <: Signed 128 end
 * primitive type UInt128 <: Unsigned 128 end
 *
 *
 */
