import org.junit.jupiter.api.Test;
import tech.jiayezheng.miniJuliaSonar.$;
import tech.jiayezheng.miniJuliaSonar.ast.*;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class OperatorParserTest {

    private String file = "test";
    private int start = 1;
    private int end = 1;

    /**
     * WARNING: This test case only prints operator '+' and '-'
     * other unexpected operators will
     */
    @Test
    public void test() {
        // +-+-+ 1 + -+-+-1
        test1();
        test2();
        test3();
    }


    //    ==================================
    //    ||         Test Cases           ||
    //    ==================================
    /**
     * Fixme: this test do not parse the expression in Julia Style
     * @param expression
     */
    private void doTest(String expression) {
        List<Node> children = initExpression(expression);
        dumpChildren(children);
        // before: +-+-+ 1 + -+-+-1
        children = simplifyOperators(children);
        // after:  1 + (-1)
        dumpChildren(children);
        // before: -~-a * -~-b + -~-c
        children = extractUnary(children);
        // after: U * U + U
        dumpChildren(children);
    }

    private void test3() {
        System.out.println("==============test3================");
        doTest("+~+-~+ 1 + ~+-~+~-1*-1");
        System.out.println("==============test3 end================\n\n");
    }

    private void test2() {
        System.out.println("==============test2================");
        doTest("+~+-~+ 1 + ~+-~+~-1");
        System.out.println("==============test2 end================\n\n");
    }

    private void test1() {
        System.out.println("==============test1================");
        doTest("+-+-+ 1 + -+-+-1");
        System.out.println("==============test1 end================\n\n");
    }

    private void dumpChildren(List<Node> children) {
        System.out.print("\n[ ");
        for (Node child : children) {
            if (child instanceof Operator) {
                System.out.printf(" %s ", ((Operator) child).op.getRep());
            }

            if (child instanceof Symbol) {
                System.out.printf(" %s ", ((Symbol) child).name);
            }
            if (child instanceof UnaryOp) {

                System.out.printf(" %s ", ((UnaryOp) child));
            }
        }
        System.out.print(" ]\n");
    }

    // test case here
    private List<Node> initExpression(String expression) {
        List<Node> ret = new LinkedList<>();

        for (char c : expression.toCharArray()) {
            switch (c) {
                case '~':
                    ret.add(new Operator(Op.BwNot,start,end,file));
                    break;
                case '+':
                    ret.add(new Operator(Op.Add, start, end, file));
                    break;
                case '-':
                    ret.add(new Operator(Op.Sub, start, end, file));
                    break;
                case '*':
                    ret.add(new Operator(Op.Mul, start, end, file));
                    break;
                case '/':
                    ret.add(new Operator(Op.Div, start, end, file));
                    break;
                case '%':
                    ret.add(new Operator(Op.Mod, start, end, file));
                    break;
                case ' ':
                    break;
                default:
                    ret.add(new Symbol(String.valueOf(c), start, end, file));
            }
        }


        return ret;
    }



    //    ==================================
    //    ||         Methods Tested       ||
    //    ==================================
    //    Class: tech.jiayezheng.miniJuliaSonar.Parser
    private List<Node> extractUnary(List<Node> children) {
        Deque<Node> stack = new LinkedList<>();

        // operator as a dummy node here
        stack.addLast(new Operator(Op.Unsupported, -1, -1, file));

        List<Node> unaryBuffer = new LinkedList<>();
        for (Node child : children) {
            Node top = stack.getLast();
            if (top.nodeType != NodeType.Operator) {
                // push binary operator
                replaceTopWithUnary(stack, unaryBuffer);
            } else {
                // push operand and unary operator
                unaryBuffer.add(child);
            }
            stack.addLast(child);
        }
        if (!unaryBuffer.isEmpty()) {
            replaceTopWithUnary(stack, unaryBuffer);
        }
        // pop dummy
        stack.pollFirst();
        return new LinkedList<>(stack);
    }

    private void replaceTopWithUnary(Deque<Node> stack, List<Node> unaryBuffer) {


        for (int i = 0; i < unaryBuffer.size(); i++) {
            Node top = stack.pollLast();
            assert !(top instanceof UnaryOp);
        }

        stack.addLast(flushUnary(unaryBuffer));

    }

    private Node flushUnary(List<Node> unaryBuffer) {

        if (unaryBuffer.size() == 0) {
            $.die("Contract Violated: unaryBuffer ought have at least 1 element.");
        }

        if (unaryBuffer.size() == 1) {
            Symbol ret = (Symbol) unaryBuffer.get(0);
            unaryBuffer.clear();
            return ret;
        }

        // unaryBuffer.add(0, new Operator(Op.Add, -1, -1, file));

        Node operand = unaryBuffer.get(unaryBuffer.size() - 1);
        Operator operator = (Operator) unaryBuffer.get(unaryBuffer.size() - 2);
        Op lastOp = operator.op;

        UnaryOp lastUnary = new UnaryOp(lastOp, operand, operator.start, operand.end, file);

        // +-+-1
        // U(+,U(-,U(+,U(-,1)))))
        for (int i = unaryBuffer.size() - 3; i >= 0; i--) {
            Op op = ((Operator) unaryBuffer.get(i)).op;
            lastUnary = new UnaryOp(op, lastUnary, lastUnary.start, lastUnary.end, file);
        }
        unaryBuffer.clear();
        return lastUnary;
    }


    private List<Node> simplifyOperators(List<Node> children) {
        Deque<Node> stack = new LinkedList<>();
        stack.addLast(new Operator(Op.Add, -1, -1, file));


        for (Node child : children) {
            Node top = stack.getLast();

            // =====================

            if ((top.nodeType != NodeType.Operator) || (child.nodeType != NodeType.Operator)) {
                // push operands and binary operators
                stack.addLast(child);
            } else {
                // top.nodeType == NodeType.Operator && child.nodeType == NodeType.Operator
                Operator operatorChild = (Operator) (child);
                Operator operatorTop = (Operator) (top);

                if (operatorChild.op != Op.Add) {
                    if (isCancelable(operatorChild.op, operatorTop.op)) {
                        stack.pollLast();
                    } else {
                        stack.addLast(child);
                    }
                }

            }

            // =====================
        }
        // pop root
        stack.pollFirst();
        return new LinkedList<>(stack);
    }

    private boolean isCancelable(Op opChld, Op opTop) {
        return (opChld == Op.Sub && opTop == Op.Sub) || (opChld == Op.BwNot && opTop == Op.BwNot) || (opChld == Op.Not && opTop == Op.Not);
    }

}
