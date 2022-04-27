package tech.jiayezheng.miniJuliaSonar;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import tech.jiayezheng.miniJuliaSonar.ast.*;
import tech.jiayezheng.miniJuliaSonar.ast.Int;
import tech.jiayezheng.miniJuliaSonar.ast.Vector;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static tech.jiayezheng.miniJuliaSonar.ast.NodeType.*;

// parser -> dump.json -> parser.toAst
public class Parser {
    private static final String dumpJuliaResource = "tech/jiayezheng/miniJuliaSonar/julia/dump_julia.jl";
    private Process juliaProcess;
    private String jsonizer;
    private static final String JULIA_EXE = "julia";
    private String parserLog;
    private String exchangeFile;
    private String endMark;
    private String file;
    private String content;


    private static final Set<String> keywordTypes = new HashSet<>();
    private static final int TIMEOUT = 30000;


    static {
        keywordTypes.add("MODULE");
        keywordTypes.add("BEGIN");
        keywordTypes.add("FUNCTION");
        // may cause bug
        // keywordTypes.add("END");
        keywordTypes.add("IF");
        keywordTypes.add("ELSEIF");
        keywordTypes.add("WHILE");
        keywordTypes.add("DO");
        keywordTypes.add("FOR");
        // break and continue relates to control flow
    }


    public Parser() {
        exchangeFile = $.getTempFile("json");
        endMark = $.getTempFile("end");
        jsonizer = $.getTempFile("dump_julia");
        parserLog = $.getTempFile("parser_log");

        startJuliaProcess();
    }

    private void startJuliaProcess() {
        if (juliaProcess != null) {
            juliaProcess.destroy();
        }

        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(dumpJuliaResource);
            FileUtils.copyURLToFile(url, new File(jsonizer));
        } catch (IOException ioe) {
            $.die("Failed to copy URL to File:" + dumpJuliaResource);
        }

        juliaProcess = startInterpreter(JULIA_EXE);
        if (juliaProcess != null) {
            $.msg("started: " + JULIA_EXE);
        } else {
            $.die("Please install Julia or set Julia in PATH.");
        }
    }


    private int logCount = 0;

    private Process startInterpreter(String juliaExec) {
        Process p;
        try {
            // TODO: run interactive ?
            ProcessBuilder builder = new ProcessBuilder(juliaExec, "-i", jsonizer);
            builder.redirectErrorStream(true);
            builder.redirectOutput(new File(parserLog + "-" + (logCount++)));
            builder.environment().remove("JULIA_HOME");
            p = builder.start();
        } catch (Exception e) {
            $.msg("Failed to start Julia process: " + juliaExec);
            return null;
        }
        return p;
    }

    public void close() {
        if (juliaProcess != null) {
            juliaProcess.destroy();
        }
        // TODO: activate
        //        if(!Analyzer.self.hasOption("debug")) {
        //            new File(exchangeFile).delete();
        //            new File(endMark).delete();
        //            new File(jsonizer).delete();
        //            new File(parserLog).delete();
        //        }
    }

    // parse file to AST
    public Node parseFile(String filename) {
        file = filename;
        content = $.readFile(filename);

        if (juliaProcess != null) {
            Node node = parseFileInner(filename, juliaProcess);
            if (node == null) {
                // TODO:
                //  Analyzer.self.failedToParse.add(filename);
                return null;
            } else {
                return node;
            }
        } else {
            // TODO:
            //  Analyzer.self.failedToParse.add(filename);
            return null;
        }
    }

    public Node parseFileInner(String filename, Process juliaProcess) {

        File endMarker = new File(endMark);
        cleanTemp();

        String s1 = $.escapeWindowsPath(filename);
        String s2 = $.escapeWindowsPath(exchangeFile);
        String s3 = $.escapeWindowsPath(endMark);
        String dumpCommand = "dump_json('" + s1 + "', '" + s2 + "', '" + s3 + "')";

        if (!sendCommand(dumpCommand, juliaProcess)) {
            cleanTemp();
            return null;
        }

        long waitStart = System.currentTimeMillis();
        while (!endMarker.exists()) {
            if (System.currentTimeMillis() - waitStart > TIMEOUT) {
                // time out
                cleanTemp();
                startJuliaProcess();
                return null;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException ie) {
                cleanTemp();
                return null;
            }
        }

        String json = $.readFile(exchangeFile);

        cleanTemp();
        if (json != null) {
            JSONObject jsonObject = JSON.parseObject(json);
            return convert(jsonObject);
        } else {
            return null;
        }

    }

    // TODO: convert()
    private Node convert(Object o) {
        if (!(o instanceof Map)) {
            return null;
        }

        JSONObject jsonObj = (JSONObject) o;

        String type = (String) jsonObj.get("julia_sonar_node_type");
        Double startDouble = (Double) jsonObj.get("_start");
        Double endDouble = (Double) jsonObj.get("_end");
        Boolean hasArgs = (Boolean) jsonObj.get("hasArgs");

        int start = startDouble == null ? 0 : startDouble.intValue();
        int end = endDouble == null ? 0 : endDouble.intValue();

        if (type.equals("ROOT")) {
            List<Node> args = convertNodeList(jsonObj.get("args"));
            return new Root(args, file);
        }

        if (type.equals("block")) {
            List<Node> body = convertNodeList(jsonObj.get("args"));
            return new Block(body, start, end, file);
        }


        if (type.equals("Assign")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertAssign(children, start, end, file);
        }


        if (type.equals("Arithmetic")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertArithmetic(children, start, end, file);
        }

        if (type.equals("call")) {
            // f call | expression
            List<Node> children = convertNodeList(jsonObj.get("args"));
            List<Node> operands = extractOperands(children);
            // desugar combination
            int nFuncCombines = extractFuncCombines(children).size();
            return convertFuncCall(nFuncCombines, operands, start, end, file);
        }



        if (type.equals("comparison")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertComparison(children, start, end, file);

        }

        if (type.equals("tuple")) {
            List<Node> elements = convertNodeList(jsonObj.get("args"));
            return new Tuple(elements, start, end, file);
        }

        if (type.equals("if") || type.equals("elseif") || type.equals("else")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertCondExpr(children, start, end, file);
        }


        if (type.equals("while")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertCondExpr(children, start, end, file);
        }

        if (type.equals("for")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertForExpr(children, start, end, file);
        }

        if (type.equals("do")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertDoExpr(children, start, end, file);
        }


        if (type.equals("vect")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return new Vector(children, start, end, file);
        }

        if (type.equals("IDENTIFIER")) {
            String name = (String) jsonObj.get("value");
            return new Symbol(name, start, end, file);
        }

        if (type.equals("OPERATOR")) {
            Op op = convertOp(jsonObj);
            return new Operator(op, start, end, file);
        }

        if (type.equals("LPAREN")) {
            return new LPAREN(start, end, file);
        }

        if (type.equals("RPAREN")) {
            return new RPAREN(start, end, file);
        }

        if (type.equals("LSQUARE")) {
            return new LSQUARE(start, end, file);
        }

        if (type.equals("RSQUARE")) {
            return new RSQUARE(start, end, file);

        }

        if (type.equals("COMMA")) {
            return new Comma(start, end, file);
        }

        if (type.equals("END")) {
            return new End(start, end, file);
        }


        if (type.equals("AugAssign")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertAugAssign(children, start, end, file);
        }


        if (type.equals("STRING") || type.equals("TRIPLESTRING")) {
            String s = (String) jsonObj.get("value");
            return new Str(s, start, end, file);
        }

        if (type.equals("CHAR")) {
            Character value = (Character) jsonObj.get("value");
            return new Char(value, start, end, file);
        }


        // todo: complex float ...
        if (type.equals("INTEGER")) {
            String i = (String) jsonObj.get("value");
            return new Int(i, start, end, file);
        }

        // function define
        if (type.equals("function")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertFuncDef(children, start, end, file);

        }


        if (type.equals("Lambda")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertLambda(children, start, end, file);
        }


        if (type.equals("FuncCombine")) {
            return new FuncCombine(start, end, file);
        }

        if (type.equals("VarArg")) {
            List<Node> args = convertNodeList(jsonObj.get("args"));
            Symbol symbol = (Symbol) args.get(0);
            return new VarArg(symbol, start, end, file);
        }

        if (keywordTypes.contains(type)) {
            String name = (String) jsonObj.get("value");
            return new KeyWord(name, start, end, file);
        }


        $.debugf("unsupported node: ", jsonObj);
        return null;
    }

    private Node convertFuncDef(List<Node> children, int start, int end, String file) {

        Iterator<Node> it = children.iterator();

        KeyWord kfunc = (KeyWord) it.next();
        assert kfunc.name.equals("function");

        Call call = (Call) it.next();
        Symbol name = call.name;

        List<Node> params = new LinkedList<>(), defaults = new LinkedList<>();
        Tuple tuple = new Tuple(call.args, -1, -1, file);
        handleParamList(tuple, params, defaults);

        Block body = (Block) it.next();

        End _end = (End) it.next();
        assert !it.hasNext();

        return new FuncDef(name, params, defaults, body, start, end, file);
    }

    private Node convertAugAssign(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();
        Node target = it.next();
        Op op = ((Operator) it.next()).op;
        Node value = it.next();
        assert !it.hasNext();
        // Node operation = new BinOp(op, target, value, start, end, file);
        Node operation = convertBinOp(op, target, value, start, end, file);
        return new Assign(target, operation, start, end, file);
    }

    private Node convertAssign(List<Node> children, int start, int end, String file) {
        if (isCompactFuncDef(children)) {
            // f(a,b) = {a+b};
            return convertCompact(children, start, end, file);
        } else {
            // handle chaining
            // assign could be x=y=z=1
            // turn it into one or more Assign nodes
            // z = 1; y = z; x = z
            Node value = children.get(children.size() - 1);
            // extract symbols to targets except last symbol
            List<Node> symbols = extractSymbols(children);

            if (symbols.size() == 1) {
                Symbol target = (Symbol) symbols.get(0);
                return new Assign(target, value, start, end, file);
            } else if (symbols.size() > 1) {
                List<Node> assignments = new LinkedList<>();
                Symbol lastSymbol = (Symbol) symbols.get(symbols.size() - 1);
                for (int i = symbols.size() - 2; i >= 0; i -= 1) {
                    Node target = symbols.get(i);
                    assignments.add(new Assign(target, lastSymbol, start, end, file));
                }

                return new Block(assignments, start, end, file);
            } else {
                $.die("Assign: Contract Violated: Invalid # of arguments");

                // dead code
                return new Block(null,-1,-1,file);
            }
        }

    }

    private Node convertArithmetic(List<Node> children, int start, int end, String file) {
        // before: +-+-+ 1 + -+-+-1
        children = simplifyOperators(children);
        // after:  1 + (-1)

        // before: -~-a * -~-b + -~-c
        children = extractUnary(children);
        // after: U * U + U

        List<Op> operators = extractOperators(children);
        extractOperators(children);
        List<Node> operands = extractOperands(children);


        assert operators.size() >= 1 && operands.size() >= 2;

        Op op = operators.get(0);
        Node left = operands.get(0);
        Node right = operands.get(1);
        Node ret = convertBinOp(op, left, right, start, end, file);


        for (int i = 1; i < operators.size(); i++) {
            op = operators.get(i);
            // ret = new BinOp(op, ret, operands.get(i + 1), start, end, file);
            ret = convertBinOp(op, ret, operands.get(i + 1), start, end, file);
        }

        return ret;
    }

    private Node convertComparison(List<Node> children, int start, int end, String file) {
        List<Op> operators = extractOperators(children);
        List<Node> operands = extractOperands(children);

        assert operators.size() >= 1 && operands.size() >= 2;


        Op op = operators.get(0);
        Node left = operands.get(0);
        Node right = operands.get(1);
        // Node ret = new BinOp(op, left, right, start, end, file);
        Node ret = convertBinOp(op, left, right, start, end, file);


        for (int i = 1; i < operators.size(); i++) {
            op = operators.get(i);
            ret = convertBinOp(op, ret, operands.get(i + 1), start, end, file);
        }
        return ret;
    }

    private Node convertDoExpr(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();
        Node value = it.next();

        KeyWord kDo = (KeyWord) it.next();
        assert kDo.name.equals("do");

        FuncDef lambda = (FuncDef) it.next();
        List<Node> target = lambda.params;
        Block body = (Block) lambda.body;

        return new Do(value, target, body, start, end, file);

    }

    private Node convertForExpr(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();
        KeyWord kfor = (KeyWord) it.next();
        assert kfor.name.equals("for");

        Assign assign = (Assign) it.next();
        Block body = (Block) it.next();


        return new For(assign, body, start, end, file);
    }

    private Node convertCondExpr(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();
        KeyWord kIf = (KeyWord) it.next();

        assert (kIf.name.equals("if") ||
                kIf.name.equals("elseif") ||
                kIf.name.equals("else") ||
                kIf.name.equals("while"));

        List<Node> conds = new LinkedList<>();
        Block body = null;
        while (it.hasNext()) {
            Node e = it.next();
            if (e instanceof Block) {
                body = (Block) e;
                break;
            }
            conds.add(e);
        }


        Expr cond = null;
        if (!conds.isEmpty()) {
            cond = new Expr(conds, conds.get(0).start, conds.get(conds.size() - 1).end, file);
        }


        assert body != null;

        Node orElse = null;
        while (it.hasNext()) {
            Node e = it.next();
            if (e instanceof End) {
                // pass
                $.debugf("If End Hit");
            } else if (e instanceof If) {
                orElse = e;
            } else {
                $.die("Unexpected If");
            }
        }


        if (kIf.name.equals("while")) {
            return new While(cond, body, start, end, file);
        } else {
            return new If(cond, body, orElse, start, end, file);
        }
    }

    private Node convertLambda(List<Node> children, int start, int end, String file) {
        assert children.size() == 2 || children.size() == 3;


        Iterator<Node> it = children.iterator();
        Node lhs = it.next();
        assert it.hasNext();
        if (children.size() == 3) {
            Operator op = (Operator) it.next();
            assert op.op == Op.Lambda && it.hasNext();
        }
        Block body = (Block) it.next();
        //assert !it.hasNext();

        List<Node> parameter = new LinkedList<>(), defaults = new LinkedList<>();
        handleParamList(lhs, parameter, defaults);

        return new FuncDef(null, parameter, defaults, body, start, end, file);
    }


    private void handleParamList(Node list, List<Node> parameter, List<Node> defaults) {

        if (list.nodeType == NodeType.Tuple) {
            List<Node> args = ((Tuple) list).args;
            for (Node e : args) {
                if (e.nodeType == NodeType.Symbol || e.nodeType == NodeType.VarArg) {
                    parameter.add(e);
                    defaults.clear();
                } else if (e.nodeType == NodeType.KW) {
                    Symbol key = ((KW) e).key;
                    Node value = ((KW) e).value;
                    parameter.add(key);
                    defaults.add(value);
                }
            }

        } else if (list.nodeType == NodeType.Symbol || list.nodeType == NodeType.VarArg) {
            parameter.add(list);
            defaults.clear();
        } else if (list.nodeType == NodeType.KW) {
            Symbol key = ((KW) list).key;
            Node value = ((KW) list).value;
            parameter.add(key);
            defaults.add(value);
        } else {
            $.debugf("error occured in %s | ", file);
            $.die("Unexpected input for parameter list, please check AST");
        }


    }

    private Node convertCompact(List<Node> children, int start, int end, String file) {
        assert children.size() == 3;
        Call call = (Call) children.get(0);
        Operator op = (Operator) children.get(1);
        assert op.op == Op.Assign;


        List<Node> list = call.args;
        Tuple tuple = new Tuple(list, call.name.end + 1, call.end, call.file);

        List<Node> params = new LinkedList<>(), defaults = new LinkedList<>();
        handleParamList(tuple, params, defaults);

        Block body = (Block) children.get(2);
        return new FuncDef(call.name, params, defaults, body, start, end, file);
    }

    private boolean isCompactFuncDef(List<Node> args) {
        assert args.size() >= 1;
        return args.get(0).nodeType == NodeType.Call;
    }

    private List<Node> extractBlocks(List<Node> children) {
        return children.stream().filter(node -> node.nodeType == NodeType.Block).collect(Collectors.toList());

    }

    private List<Node> extractCall(List<Node> children) {
        return children.stream().filter(node -> node.nodeType == NodeType.Call).collect(Collectors.toList());
    }

    /**
     * parse unary expressions in a LL1 style
     *
     * @return List of children with combined unary operator expressions
     */
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


    // desugar complex operators and LaTex
    private Node convertBinOp(Op op, Node left, Node right, int start, int end, String file) {

        if (op == Op.NotEq || op == Op.NotEqual) {
            // a!=b --> !(a == b)
            Node eq = new BinOp(Op.Eq, left, right, start, end, file);
            return new UnaryOp(Op.Not, eq, start, end, file);
        }

        if (op == Op.LtE || op == Op.LtEqual) {
            Node lt = new BinOp(Op.Lt, left, right, start, end, file);
            Node eq = new BinOp(Op.Eq, left, right, start, end, file);
            return new BinOp(Op.Or, lt, eq, start, end, file);
        }

        if (op == Op.GtE || op == Op.GtEqual) {
            Node gt = new BinOp(Op.Gt, left, right, start, end, file);
            Node eq = new BinOp(Op.Eq, left, right, start, end, file);
            return new BinOp(Op.Or, gt, eq, start, end, file);
        }

        if (op == Op.InverseDiv) {
            return new BinOp(Op.Div, right, left, start, end, file);
        }

        if (op == Op.In1) {
            return new BinOp(Op.In, right, left, start, end, file);
        }


        return new BinOp(op, left, right, start, end, file);

    }

    private Node convertFuncCall(int nFuncCombines, List<Node> symbols, int start, int end, String file) {
        if (nFuncCombines == 0) {
            // f(...)
            Symbol function = (Symbol) symbols.get(0);
            List<Node> args = new LinkedList<>();
            for (int i = 1; i < symbols.size(); i += 1) {
                args.add(symbols.get(i));
            }
            return new Call(function, args, start, end, file);
        } else if (nFuncCombines > 0) {
            // (f ∘ g)(a,b,c)
            // f(g(...))
            List<Symbol> functions = new LinkedList<>();
            int i = 0;
            for (; i < nFuncCombines + 1; i++) {
                functions.add((Symbol) symbols.get(i));
            }
            List<Node> args = new LinkedList<>();
            for (; i < symbols.size(); i++) {
                args.add(symbols.get(i));
            }

            Symbol lastFuncName = functions.get(functions.size() - 1);

            Call prevCall = new Call(lastFuncName, args, start, end, file);
            for (int j = functions.size() - 2; j >= 0; j--) {
                Symbol name = functions.get(i);
                prevCall = new Call(name, List.of(prevCall), start, end, file);
            }

            return prevCall;

        } else {
            $.die("call: Contract Violated: no function name provided, please check parser");
            // unreachable
            return null;
        }

    }


    private List<Node> extractOperands(List<Node> children) {
        return children.stream().filter(child -> child.nodeType != NodeType.Operator).collect(Collectors.toList());
    }

    private List<Node> extractFuncCombines(List<Node> children) {
        return children.stream().filter(child -> child.nodeType == FuncCombine).collect(Collectors.toList());
    }

    private List<Op> extractOperators(List<Node> children) {
        List<Op> res = new LinkedList<>();
        List<Node> nodes = children.stream().filter(child -> child.nodeType == NodeType.Operator).collect(Collectors.toList());
        nodes.forEach(node -> res.add(((Operator) node).op));
        return res;
    }

    private Op convertOp(Object o) {
        if (!(o instanceof Map)) {
            $.die("Contract Violate: Invalid Argument");
            return Op.Unsupported;
        }

        String value = (String) ((Map<String, Object>) o).get("value");


        if (value.equals("+") || value.equals("+=")) {
            return Op.Add;
        }


        if (value.equals("-") || value.equals("-=")) {
            return Op.Sub;
        }


        if (value.equals("*") || value.equals("*=")) {
            return Op.Mul;
        }


        if (value.equals("/") || value.equals("/=")) {
            return Op.Div;
        }


        if (value.equals("÷")) {
            return Op.IntDiv;
        }


        if (value.equals("\\")) {
            return Op.InverseDiv;
        }


        if (value.equals("^")) {
            return Op.Pow;
        }


        if (value.equals("%")) {
            return Op.Mod;
        }


        if (value.equals("==")) {
            return Op.Eq;
        }


        if (value.equals("!=")) {
            return Op.NotEq;
        }


        if (value.equals("≠")) {
            return Op.NotEqual;
        }


        if (value.equals("<")) {
            return Op.Lt;
        }


        if (value.equals("<=")) {
            return Op.LtE;
        }


        if (value.equals("≤")) {
            return Op.LtEqual;
        }


        if (value.equals(">")) {
            return Op.Gt;
        }


        if (value.equals(">=")) {
            return Op.GtE;
        }


        if (value.equals("≥")) {
            return Op.GtEqual;
        }


        if (value.equals("!")) {
            return Op.Not;
        }


        if (value.equals("&&")) {
            return Op.And;
        }


        if (value.equals("||")) {
            return Op.Or;
        }


        if (value.equals("in")) {
            return Op.In;
        }


        if (value.equals("∈")) {
            return Op.In1;
        }


        if (value.equals("~")) {
            return Op.BwNot;
        }


        if (value.equals("&")) {
            return Op.BwAnd;
        }


        if (value.equals("|")) {
            return Op.BwOr;
        }


        if (value.equals("⊻")) {
            return Op.BwXor;
        }


        if (value.equals("⊼")) {
            return Op.BwNand;
        }


        if (value.equals("⊽")) {
            return Op.BwNor;
        }


        if (value.equals(">>>")) {
            return Op.LogShfR;
        }


        if (value.equals(">>")) {
            return Op.AriShfR;
        }


        if (value.equals("<<")) {
            return Op.AriShfL;
        }


        if (value.equals(":")) {
            return Op.Range;
        }

        $.msg("[please report] unsupported operator: " + value);
        return Op.Unsupported;
    }

    private List<Node> extractSymbols(List<Node> args) {
        return args.stream().filter(e -> e.nodeType == Symbol).collect(Collectors.toList());
    }


    private List<Node> convertNodeList(Object o) {
        if (o == null) {
            return null;
        } else {
            List<Map<String, Object>> in = (List<Map<String, Object>>) o;
            List<Node> out = new LinkedList<>();

            for (Map<String, Object> m : in) {
                Node n = convert(m);
                if (n != null && notBanned(n)) {
                    out.add(n);
                }
            }

            if (out.isEmpty()) {
                return null;
            }

            return out;
        }
    }


    private static boolean notBanned(Node n) {
        return !(n.nodeType == NodeType.LPAREN ||
                n.nodeType == NodeType.RPAREN ||
                n.nodeType == NodeType.COMMA ||
                n.nodeType == LSQUARE ||
                n.nodeType == RSQUARE
        );
    }


    private boolean sendCommand(String cmd, Process juliaProcess) {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(juliaProcess.getOutputStream());
            writer.write(cmd);
            writer.write("\n");
            writer.flush();
            return true;
        } catch (IOException ioe) {
            $.msg("\n Failed to send command to interpreter: " + cmd);
            return false;
        }
    }

    private void cleanTemp() {
        new File(exchangeFile).delete();
        new File(endMark).delete();
    }

}
