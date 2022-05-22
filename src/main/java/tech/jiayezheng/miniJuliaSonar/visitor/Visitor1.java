package tech.jiayezheng.miniJuliaSonar.visitor;

import org.jetbrains.annotations.NotNull;


import tech.jiayezheng.miniJuliaSonar.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Generated Source File: Do not modify !!!
 */
public interface Visitor1<T, P> {

    default T visit(@NotNull Node node, P param) {
        switch (node.nodeType) {
            case KeyWord:
                return visit((KeyWord) node, param);
            case Assign:
                return visit((Assign) node, param);
            case Symbol:
                return visit((Symbol) node, param);
            case Block:
                return visit((Block) node, param);
            case Operator:
                return visit((Operator) node, param);
            case FuncCombine:
                return visit((FuncCombineCall) node, param);
            case Call:
                return visit((Call) node, param);
            case BinOp:
                return visit((BinOp) node, param);
            case Char:
                return visit((Char) node, param);
            case LPAREN:
                return visit((LPAREN) node, param);
            case RPAREN:
                return visit((RPAREN) node, param);
            case End:
                return visit((End) node, param);
            case Str:
                return visit((Str) node, param);
            case FuncDef:
                return visit((FuncDef) node, param);
            case JuliaInt:
                return visit((JuliaInt) node, param);
            case Comma:
                return visit((Comma) node, param);
            case Continue:
                return visit((Continue) node, param);
            case Break:
                return visit((Break) node, param);
            case Tuple:
                return visit((Tuple) node, param);
            case KW:
                return visit((KW) node, param);
            case VarArg:
                return visit((VarArg) node, param);
            case LSQUARE:
                return visit((LSQUARE) node, param);
            case RSQUARE:
                return visit((RSQUARE) node, param);
            case JuliaVector:
                return visit((JuliaVector) node, param);
            case While:
                return visit((While) node, param);
            case For:
                return visit((For) node, param);
            case Do:
                return visit((Do) node, param);
            case UnaryOp:
                return visit((UnaryOp) node, param);
            case JuliaModule:
                return visit((JuliaModule) node, param);
            case StructDef:
                return visit((StructDef) node, param);
            case SubType:
                return visit((SubType) node, param);
            case BaseType:
                return visit((BaseType) node, param);
            case Ref:
                return visit((Ref) node, param);
            case PrimitiveType:
                return visit((PrimitiveType) node, param);
            case TypeDecl:
                return visit((TypeDecl) node, param);
            case QuoteNode:
                return visit((QuoteNode) node, param);
            case Dot:
                return visit((Dot) node, param);
            case LBRACE:
                return visit((LBRACE) node, param);
            case RBRACE:
                return visit((RBRACE) node, param);
            case ParamType:
                return visit((ParamType) node, param);
            case UnionType:
                return visit((Union) node, param);
            case Where:
                return visit((Where) node, param);
            case Catch:
                return visit((Catch) node, param);
            case Try:
                return visit((Try) node, param);
            case AbstractType:
                return visit((AbstractType) node, param);
            case JuliaFloat:
                return visit((JuliaFloat) node, param);
            case Complex:
                return visit((Complex) node, param);
            case Missing:
                return visit((Missing) node, param);
            case Nothing:
                return visit((Nothing) node, param);
            case If:
                return visit((If) node, param);
            case JuliaBool:
                return visit((JuliaBool) node, param);
            case Global:
                return visit((Global) node, param);
            case Return:
                return visit((Return) node, param);
            default:
                throw new RuntimeException("unexpected node");
        }
    }

    default <N extends Node, O extends T> List<O> visit(List<N> list, P param) {
        List<O> result = new ArrayList<>();
        for (N element : list) {
            result.add((O) visit(element, param));
        }
        return result;
    }

    T visit(KeyWord node, P param);

    T visit(Assign node, P param);

    T visit(Symbol node, P param);

    T visit(Block node, P param);

    T visit(Operator node, P param);

    T visit(FuncCombineCall node, P param);

    T visit(Call node, P param);

    T visit(BinOp node, P param);

    T visit(Char node, P param);

    T visit(LPAREN node, P param);

    T visit(RPAREN node, P param);

    T visit(End node, P param);

    T visit(Str node, P param);

    T visit(FuncDef node, P param);

    T visit(JuliaInt node, P param);

    T visit(Comma node, P param);

    T visit(Continue node, P param);

    T visit(Break node, P param);

    T visit(Tuple node, P param);

    T visit(KW node, P param);

    T visit(VarArg node, P param);

    T visit(LSQUARE node, P param);

    T visit(RSQUARE node, P param);

    T visit(JuliaVector node, P param);

    T visit(While node, P param);

    T visit(For node, P param);

    T visit(Do node, P param);

    T visit(UnaryOp node, P param);

    T visit(JuliaModule node, P param);

    T visit(StructDef node, P param);

    T visit(SubType node, P param);

    T visit(BaseType node, P param);

    T visit(Ref node, P param);

    T visit(PrimitiveType node, P param);

    T visit(TypeDecl node, P param);

    T visit(QuoteNode node, P param);

    T visit(Dot node, P param);

    T visit(LBRACE node, P param);

    T visit(RBRACE node, P param);

    T visit(ParamType node, P param);

    T visit(Union node, P param);

    T visit(Where node, P param);

    T visit(Catch node, P param);

    T visit(Try node, P param);

    T visit(AbstractType node, P param);

    T visit(JuliaFloat node, P param);

    T visit(Complex node, P param);

    T visit(Missing node, P param);

    T visit(Nothing node, P param);

    T visit(If node, P param);

    T visit(JuliaBool node, P param);

    T visit(Global node, P param);

    T visit(Url node, P param);

    T visit(Return node, P param);
}

