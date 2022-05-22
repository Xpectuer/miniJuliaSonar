package tech.jiayezheng.miniJuliaSonar;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.io.FileUtils;
import org.checkerframework.checker.units.qual.C;
import tech.jiayezheng.miniJuliaSonar.ast.*;
import tech.jiayezheng.miniJuliaSonar.ast.JuliaInt;
import tech.jiayezheng.miniJuliaSonar.ast.JuliaVector;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;


import static tech.jiayezheng.miniJuliaSonar.ast.NodeType.*;

// parser -> dump.json -> parser.toAst
public class Parser {
    private Process juliaProcess;
    private String jsonizer;
    private String parserLog;
    private String exchangeFile;
    private String endMark;
    private String file;
    private String content;
    private final Set<String> typeTable = new HashSet<>();

    private static final Set<String> keywordTypes = new HashSet<>();
    private static final int TIMEOUT = 30000;
    private static final String dumpJuliaResource = "tech/jiayezheng/miniJuliaSonar/julia/dump_julia.jl";
    private static final String JULIA_EXE = "julia";


    static {
        keywordTypes.add("MODULE");
        keywordTypes.add("BEGIN");
        keywordTypes.add("FUNCTION");
        // may cause bug
        // keywordTypes.add("END");
        keywordTypes.add("IF");
        keywordTypes.add("ELSEIF");
        keywordTypes.add("ELSE");

        keywordTypes.add("WHILE");
        keywordTypes.add("DO");
        keywordTypes.add("FOR");
        keywordTypes.add("SCALL");
        keywordTypes.add("CCALL");
        keywordTypes.add("ABSTRACT");
        keywordTypes.add("PRIMITIVE");
        keywordTypes.add("TYPE");
        keywordTypes.add("STRUCT");
        keywordTypes.add("MUTABLE");
        keywordTypes.add("TRY");
        keywordTypes.add("CATCH");
        keywordTypes.add("FINALLY");
        keywordTypes.add("GLOBAL");
        keywordTypes.add("RETURN");
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

        if (!Analyzer.self.hasOption("debug")) {
            new File(exchangeFile).delete();
            new File(endMark).delete();
            new File(jsonizer).delete();
            new File(parserLog).delete();
        }
    }


    // parse file to AST
    public Node parseFile(String filename) {
        file = filename;
        content = $.readFile(filename);
        if (juliaProcess != null) {
            Node node = parseFileInner(filename, juliaProcess);
            if (node == null) {
                Analyzer.self.failedToParse.add(filename);
                return null;
            } else {
                return node;
            }
        } else {
            Analyzer.self.failedToParse.add(filename);
            return null;
        }
    }


    public Node parseFileInner(String filename, Process juliaProcess) {

        File endMarker = new File(endMark);
        cleanTemp();

        String s1 = $.escapeWindowsPath(filename);
        String s2 = $.escapeWindowsPath(exchangeFile);
        String s3 = $.escapeWindowsPath(endMark);
        String dumpCommand = String.format("dump_json(\"%s\",\"%s\",\"%s\")", s1, s2, s3);


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
            JSONObject map = prettyAndParse(json);
            return convert(map);
        } else {
            return null;
        }

    }


    private JSONObject prettyAndParse(String json) {
        return (JSONObject) JSON.parse(json);
    }

    // TODO: convert()
    private Node convert(Object o) {
        if (!(o instanceof Map)) {
            return null;
        }

        JSONObject jsonObj = (JSONObject) o;

        String type = (String) jsonObj.get("julia_sonar_node_type");
        Integer startDouble = (Integer) jsonObj.get("_start");
        Integer endDouble = (Integer) jsonObj.get("_end");
        Boolean hasArgs = (Boolean) jsonObj.get("hasArgs");
        Boolean isNonLocal = (Boolean) jsonObj.get("nonlocal");

        int start = startDouble == null ? 0 : startDouble.intValue();
        int end = endDouble == null ? 0 : endDouble.intValue();

        if (type.equals("ROOT")) {
            List<Node> args = convertNodeList(jsonObj.get("args"));
            Symbol symbol = new Symbol((String) jsonObj.get("value"), -1, -1, file);
            Block block = new Block(args, -1, -1, file);
            return new JuliaModule(symbol, block, start, end, file);
        }

        if (type.equals("block")) {
            List<Node> body = convertNodeList(jsonObj.get("args"));
            return new Block(body, start, end, file);
        }


        if (type.equals("Assign")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertAssign(children, isNonLocal, start, end, file);
        }


        if (type.equals("Arithmetic")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertArithmetic(children, start, end, file);
        }

        if (type.equals("SubType") || type.equals("BaseType")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertTypeComp(children, start, end, file);
        }

        if (type.equals("where")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertWhere(children, start, end, file);
        }


        if (type.equals("Dot")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertDot(children, start, end, file);
        }


        if (type.equals("module")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertModule(children, start, end, file);
        }

        if (type.equals("call")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertCall(children, start, end, file);
        }


        if (type.equals("comparison")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertComparison(children, start, end, file);

        }

        if (type.equals("tuple")) {
            List<Node> elements = convertNodeList(jsonObj.get("args"));
            return new Tuple(elements, start, end, file);
        }

        if (type.equals("if") || type.equals("elseif")) {
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

        if (type.equals("return")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertReturn(children, start, end, file);
        }

        if (type.equals("parameters")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return new Tuple(children, start, end, file);
        }

        if (type.equals("vect")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return new JuliaVector(children, start, end, file);
        }

        if (type.equals("struct")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertStruct(children, start, end, file);
        }

        if (type.equals("ref")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertRef(children, start, end, file);
        }

        if (type.equals("abstract") || type.equals("primitive")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertTypeDef(children, start, end, file);
        }

        if (type.equals("TypeDecl")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertTypeDecl(children, start, end, file);
        }

        if (type.equals("try")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertExceptHandle(children, start, end, file);
        }


        if (type.equals("IDENTIFIER")) {
            String name = (String) jsonObj.get("value");
            return new Symbol(name, start, end, file);
        }

        if (type.equals("quotenode")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return new QuoteNode((Symbol) children.get(0), start, end, file);
        }

        if (type.equals("OPERATOR")) {
            Op op = convertOp(jsonObj);
            return new Operator(op, start, end, file);
        }

        // includes param type & union type
        if (type.equals("ParamType")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertParamType(children, start, end, file);
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

        if (type.equals("LBRACE")) {
            return new LBRACE(start, end, file);
        }
        if (type.equals("RBRACE")) {
            return new RBRACE(start, end, file);
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

        if (type.equals("global")) {
            List<Node> children = convertNodeList(jsonObj.get("args"));
            return convertGlobal(children, start, end, file);
        }


        if (type.equals("STRING") || type.equals("TRIPLESTRING")) {
            String s = (String) jsonObj.get("value");
            return new Str(s, start, end, file);
        }

        if (type.equals("CHAR")) {
            String value = (String) jsonObj.get("value");
            return new Char(value, start, end, file);
        }


        // todo: complex float ...
        if (type.equals("INTEGER")) {
            String i = (String) jsonObj.get("value");
            return new JuliaInt(i, start, end, file);
        }


        if (type.equals("FLOAT")) {
            String f = (String) jsonObj.get("value");
            return new JuliaFloat(f, start, end, file);
        }

        if (type.equals("TRUE") || type.equals("FALSE")) {
            return new JuliaBool((String) jsonObj.get("value"), start, end, file);
        }

        if (type.equals("NOTHING")) {
            return new Nothing(start, end, file);
        }

        if (type.equals("MISSING")) {
            return new Missing(start, end, file);
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


        if (type.equals("VarArg")) {
            List<Node> args = convertNodeList(jsonObj.get("args"));
            Symbol symbol = (Symbol) args.get(0);
            return new VarArg(symbol, start, end, file);
        }


        if (keywordTypes.contains(type)) {
            String name = (String) jsonObj.get("value");
            return new KeyWord(name, start, end, file);
        }


        $.debugf("\n[DEBUG]unsupported node: (type: %s, value: %s) \n",
                jsonObj.get("julia_sonar_node_type"),
                jsonObj.get("value"));

        return null;
    }

    private Node convertReturn(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();
        KeyWord kret = (KeyWord) it.next();
        assert kret.name.equals("return");
        Node value = it.hasNext() ? it.next() : null;
        return new Return(value, start, end, file);
    }

    private Node convertGlobal(List<Node> children, int start, int end, String file) {
        List<Node> list = new LinkedList<>();
        Iterator<Node> it = children.iterator();
        KeyWord kglo = (KeyWord) it.next();


        while (it.hasNext()) {
            Node next = it.next();
            if (next instanceof Symbol) {
                list.add(next);
            } else {
                $.die("JavaParser: Syntax Not Supported Yet");
            }
        }

        return new Global(list, start, end, file);
    }


    private Node convertExceptHandle(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();
        Block tryBody = null, finallyBody = null;

        List<Catch> catches = new LinkedList<>();


        while (it.hasNext()) {
            Node e = it.next();
            if (e.nodeType == NodeType.KeyWord) {
                KeyWord kw = (KeyWord) e;
                if (kw.name.equals("try")) {
                    tryBody = (Block) it.next();

                } else if (kw.name.equals("finally")) {
                    finallyBody = (Block) it.next();

                } else if (kw.name.equals("catch")) {

                    List<Node> exceptions = new LinkedList<>();
                    Node nodeAfterCatch = it.next();

                    while (nodeAfterCatch.nodeType == NodeType.Symbol) {
                        exceptions.add(nodeAfterCatch);
                        nodeAfterCatch = it.next();
                    }

                    while (nodeAfterCatch.nodeType == NodeType.JuliaBool) {
                        nodeAfterCatch = it.next();
                    }

                    Block body = (Block) nodeAfterCatch;
                    catches.add(new Catch(exceptions, body, start, end, file));
                }
            } else if (e.nodeType == NodeType.End) {
                return new Try(tryBody, catches, finallyBody, start, end, file);
            }
        }

        return new Try(tryBody, catches, finallyBody, start, end, file);
    }


    private Node convertParamType(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();
        Symbol type = (Symbol) it.next();

        List<Node> params = new LinkedList<>();
        {
            while (it.hasNext()) {
                Symbol e = (Symbol) it.next();
                params.add(e);
            }
        }

        if (type.name.equals("Union")) {
            return new Union(params, start, end, file);
        }

        return new ParamType(type, params, start, end, file);

    }

    private Node convertWhere(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();

        ParamType paramType = (ParamType) it.next();
        Operator operator = (Operator) it.next();
        assert operator.op == Op.Where;
        List<BinOp> constraints = new LinkedList<>();
        {
            while (it.hasNext()) {
                Node e = it.next();
                if (e.nodeType == NodeType.Symbol) {
                    Symbol any = new Symbol("Any", -1, -1, file);
                    constraints.add(new BinOp(Op.SubType, e, any, start, end, file));
                } else {
                    constraints.add((BinOp) e);
                }
            }
        }

        return new Where(paramType, constraints, start, end, file);
    }

    private Node convertDot(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();

        Node left = it.next();
        Operator op = (Operator) it.next();
        assert op.op == Op.Dot;
        QuoteNode right = (QuoteNode) it.next();

        return new Dot(left, right.name, start, end, file);
    }

    private Node convertTypeDecl(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();

        Symbol left = (Symbol) it.next();
        Operator op = (Operator) it.next();
        assert op.op == Op.TypeDecl;
        Node right = it.next();

        return new TypeDecl(left, right, start, end, file);
    }

    private Node convertTypeComp(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();

        Symbol left = (Symbol) it.next();
        Operator op = (Operator) it.next();
        Symbol right = (Symbol) it.next();


        if (op.op == Op.SubType) {
            return new SubType(left, right, start, end, file);
        }

        if (op.op == Op.BaseType) {
            return new BaseType(left, right, start, end, file);
        }

        $.die("convertTypeComp");
        return null;

    }

    private Node convertTypeDef(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();
        KeyWord k1 = (KeyWord) it.next();
        if (k1.name.equals("abstract")) {
            KeyWord ktype = (KeyWord) it.next();
            assert ktype.name.equals("type");

            Node n = it.next();
            Symbol name = null, base = null;
            if (n instanceof Symbol) {
                name = (Symbol) it.next();
            } else if (n instanceof BinOp) {
                BinOp bop = (BinOp) n;
                assert bop.op == Op.SubType;
                name = (Symbol) bop.left;
                base = (Symbol) bop.right;
            }
            return new AbstractType(name, base, start, end, file);
        }

        if (k1.name.equals("primitive")) {
            KeyWord ktype = (KeyWord) it.next();
            assert ktype.name.equals("type");

            Node n = it.next();
            Symbol name = null, base = null;
            if (n instanceof Symbol) {
                name = (Symbol) it.next();
            } else if (n instanceof BinOp) {
                BinOp bop = (BinOp) n;
                assert bop.op == Op.SubType;
                name = (Symbol) bop.left;
                base = (Symbol) bop.right;
            }

            JuliaInt i = (JuliaInt) it.next();
            int size = Integer.parseInt(i.value);
            return new PrimitiveType(name, base, size, start, end, file);
        }


        $.die("type define");
        return null;
    }

    private Node convertRef(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();
        Node name = it.next();
        Node index = it.next();
        assert !it.hasNext();

        return new Ref(name, index, start, end, file);
    }

    private Node convertStruct(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();

        boolean mutable = false;

        KeyWord k1 = (KeyWord) it.next();
        if (k1.name.equals("mutable")) {
            mutable = true;
            k1 = (KeyWord) it.next();
        }

        assert k1.name.equals("struct");

        Symbol name = null;
        Symbol base = null;
        Block body = null;

        while (it.hasNext()) {
            Node nodeAfterTRUE = it.next();
            if (nodeAfterTRUE instanceof BinOp) {
                BinOp subtypeOp = (BinOp) nodeAfterTRUE;
                name = (Symbol) subtypeOp.left;
                base = (Symbol) subtypeOp.right;
            } else if (nodeAfterTRUE instanceof Symbol) {
                name = (Symbol) nodeAfterTRUE;
            } else if (nodeAfterTRUE instanceof Block) {
                body = (Block) nodeAfterTRUE;
            }
        }

        if (name != null) {
            this.typeTable.add(name.name);
        }

        return new StructDef(mutable, name, base, body, start, end, file);
    }


    private Node convertCall(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();
        KeyWord head = (KeyWord) it.next();
        if (head.name.equals("scall")) {
            // single name call
            Symbol name = (Symbol) it.next();
            List<Node> args = new LinkedList<>();
            List<KW> kws = new LinkedList<>();
            while (it.hasNext()) {
                Node n = it.next();
                if (n instanceof KW) {
                    kws.add((KW) n);
                } else {
                    args.add(n);
                }
            }

            Call ret = new Call(name, args, kws, start, end, file);
            if (typeTable.contains(name.name)) {
                ret.markInit();
            }

            return ret;

        } else if (head.name.equals("ccall")) {
            // composite call
            Node pipes = it.next();
            List<Node> args = new LinkedList<>();
            while (it.hasNext()) {
                args.add(it.next());
            }

            //call.setArgs(args);
            return desugarCompositeCall(pipes, args, 0, start, end, file);
        }

        $.die("[ERROR]Unsupported Call\n");
        return null;
    }

    private Call desugarCompositeCall(Node node, List<Node> args, int level, int start, int end, String file) {
        if (node instanceof Block) {
            Block block = (Block) node;
            return desugarCompositeCall(block.args.get(0), args, level, start, end, file);

        } else if (node instanceof Symbol) {
            Symbol symbol = (Symbol) node;
            return new Call(symbol, args, symbol.start, symbol.end, file);

        } else if (node instanceof BinOp) {
            /*
             * The binop structure which is right recursive
             * (∘ f (∘ g h)) . (args)
             * h( g( f(args)))
             * */
            BinOp bop = (BinOp) node;
            Node left = desugarCompositeCall(bop.left, args, level + 1, start, end, file);
            List<Node> arg = Collections.singletonList(left);
            Call right = desugarCompositeCall(bop.right, arg, level + 1, start, end, file);
            return right;
        } else {
            $.die("Unexpected CCall structure !!!");
            return null;
        }

    }

    private Node convertModule(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();
        KeyWord kmod = (KeyWord) it.next();
        assert kmod.name.equals("module");

        Symbol name = (Symbol) it.next();
        Block block = (Block) it.next();

        return new JuliaModule(name, block, start, end, file);
    }

    private Node convertFuncDef(List<Node> children, int start, int end, String file) {

        Iterator<Node> it = children.iterator();

        KeyWord kfunc = (KeyWord) it.next();
        assert kfunc.name.equals("function");

        Call call = (Call) it.next();
        Node n = call.name;
        Symbol name = null;
        if (n instanceof Symbol) {
            name = (tech.jiayezheng.miniJuliaSonar.ast.Symbol) n;
        } else {
            $.die("Invalid Function definition: \" + call.name + \"Please check julia parser!!!");
            return null;
        }

        List<Node> params = new LinkedList<>(),
                defaults = new LinkedList<>(),
                varargs = new LinkedList<>(),
                kwargs = new LinkedList<>();
        Tuple tuple = new Tuple(call.args, -1, -1, file);
        handleParamList(tuple, params, defaults, varargs, kwargs);

        Block body = (Block) it.next();

        End _end = (End) it.next();
        assert !it.hasNext();

        FuncDef ret = new FuncDef(name, params, defaults, body, start, end, file);
        if (kwargs.size() == 1) {
            ret.setKWarg((Symbol) kwargs.get(0));
        }
        if (varargs.size() == 1) {
            ret.setKWarg((Symbol) varargs.get(0));
        }

        return ret;

    }

    private Node convertAugAssign(List<Node> children, int start, int end, String file) {
        Iterator<Node> it = children.iterator();
        Symbol target = (Symbol) it.next();
        Op op = ((Operator) it.next()).op;
        Node value = it.next();
        assert !it.hasNext();
        // Node operation = new BinOp(op, target, value, start, end, file);
        Node operation = convertBinOp(op, target, value, start, end, file);
        return new Assign(target, false, operation, start, end, file);
    }

    private Node convertAssign(List<Node> children, boolean isNonLocal, int start, int end, String file) {
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
            List<Node> operands = extractOperands(children);

            if (operands.size() == 1) {
                Node target = operands.get(0);
                return new Assign(target, isNonLocal, value, start, end, file);
            } else if (operands.size() > 1) {
                List<Node> assignments = new LinkedList<>();
                Node lastOperand = operands.get(operands.size() - 1);
                for (int i = operands.size() - 2; i >= 0; i -= 1) {
                    Node target = operands.get(i);
                    assignments.add(new Assign(target, isNonLocal, lastOperand, start, end, file));
                }

                return new Block(assignments, start, end, file);
            } else {
                $.die("Assign: Contract Violated: Invalid # of arguments");
                // dead code
                return null;
            }
        }

    }

    private Node convertArithmetic(List<Node> children, int start, int end, String file) {

        if (children.size() == 1 && isOpExpr(children.get(0))) {
            return children.get(0);
        }

        // List<Node> testchidren = new LinkedList<>(children);
        // before: +-+-+ 1 + -+-+-1
        children = simplifyOperators(children);
        // after:  1 + (-1)

        // before: -~-a * -~-b + -~-c
        children = extractUnary(children);
        // after: U * U + U

        List<Op> operators = extractOperators(children);
        extractOperators(children);
        List<Node> operands = extractOperands(children);


        if (operands.size() == 1) {
            // Unary Case
            return operands.get(0);
        }


        Op op = operators.get(0);
        Node left = operands.get(0);
        Node right = operands.get(1);
        Node ret = convertBinOp(op, left, right, start, end, file);

        for (int i = 1; i < operators.size(); i++) {
            op = operators.get(i);
            ret = convertBinOp(op, ret, operands.get(i + 1), start, end, file);
        }

        return ret;
    }

    private boolean isOpExpr(Node node) {
        return node.nodeType == NodeType.BinOp || node.nodeType == NodeType.UnaryOp;
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

        Node n = it.next();
        if (n instanceof Block) {
            n = ((Block) n).args.get(0);
        }
        Assign assign = (Assign) n;
        Block body = (Block) it.next();

        return new For(assign.target, assign.value, body, start, end, file);
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


        Node cond = null;
        if (!conds.isEmpty()) {
            cond = new Block(conds, conds.get(0).start, conds.get(conds.size() - 1).end, file);
        }


        assert body != null;

        Node orElse = null;
        while (it.hasNext()) {
            Node e = it.next();
            if (e instanceof End) {
                // pass
                $.debugf("If End Hit!  offset: %d\n", e.start);
            } else if (e instanceof If) {
                orElse = e;
            } else if (e instanceof KeyWord) {
                KeyWord kw = (KeyWord) e;
                assert kw.name.equals("else");

                Block b = (Block) it.next();
                orElse = b;
            } else {
                $.debugf("[DEBUG]node: %s k\n", e);
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

        List<Node> parameter = new LinkedList<>(),
                defaults = new LinkedList<>(),
                varargs = new LinkedList<>(),
                kwargs = new LinkedList<>();
        handleParamList(lhs, parameter, defaults, varargs, kwargs);


        return new FuncDef(null, parameter, defaults, body, start, end, file);
    }


    private void handleParamList(Node list, List<Node> parameter, List<Node> defaults, List<Node> varargs, List<Node> kwargs) {

        if (list.nodeType == NodeType.Tuple || list.nodeType == NodeType.Block) {
            List<Node> args = null;
            switch (list.nodeType) {
                case Tuple:
                    args = ((Tuple) list).elts;
                    break;
                case Block:
                    args = ((Block) list).args;
                    break;
                default:
                    $.die("Unexpected input for parameter list, please check AST");
            }

            if(args == null) {
                return;
            }

            for (Node e : args) {
                if (e.nodeType == NodeType.Symbol) {
                    parameter.add(e);
                    defaults.clear();
                } else if (e.nodeType == NodeType.VarArg) {
                    varargs.add(((VarArg) e).name);
                } else if (e.nodeType == NodeType.KW) {
                    // Only kw param after raw param allowed
                    Symbol key = ((KW) e).key;
                    Node value = ((KW) e).value;
                    parameter.add(key);
                    defaults.add(value);
                } else if (e.nodeType == NodeType.Tuple) {

                    List<Node> param = ((Tuple) list).elts;
                    for (Node ee : param) {
                        if (ee.nodeType == NodeType.Symbol) {
                            parameter.add(ee);
                        } else if (ee.nodeType == NodeType.VarArg) {
                            kwargs.add(((VarArg) ee).name);
                        } else if (ee.nodeType == NodeType.KW) {
                            // Only kw param after raw param allowed
                            Symbol key = ((KW) ee).key;
                            Node value = ((KW) ee).value;
                            parameter.add(key);
                            defaults.add(value);
                        }
                    }
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

        List<Node> params = new LinkedList<>(),
                defaults = new LinkedList<>(),
                varargs = new LinkedList<>(),
                kwargs = new LinkedList<>();

        handleParamList(tuple, params, defaults, varargs, kwargs);

        Block body = (Block) children.get(2);

        if (!(call.name instanceof Symbol)) {
            $.die("Invalid Function definition: " + call.name + "Please check julia parser!!!");
        }
        return new FuncDef((Symbol) call.name, params, defaults, body, start, end, file);
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
        // U + U
        for (int i = 0; i < unaryBuffer.size(); i++) {
            stack.pollLast();
            //assert !(top instanceof UnaryOp);
        }

        stack.addLast(flushUnary(unaryBuffer));

    }

    private Node flushUnary(List<Node> unaryBuffer) {

        if (unaryBuffer.size() == 0) {
            $.die("Contract Violated: unaryBuffer ought have at least 1 element.");
        }

        if (unaryBuffer.size() == 1) {
            Node ret = unaryBuffer.get(0);
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

        if (op == Op.FuncCombine) {
            return new BinOp(Op.FuncCombine, right, left, start, end, file);
        }

        if (op == Op.In1) {
            return new BinOp(Op.In, right, left, start, end, file);
        }

        return new BinOp(op, left, right, start, end, file);

    }


    private List<Node> extractOperands(List<Node> children) {
        return children.stream().filter(child -> child.nodeType != NodeType.Operator).collect(Collectors.toList());
    }

    private List<Op> extractOperators(List<Node> children) {
        List<Op> res = new LinkedList<>();
        List<Node> nodes = children.stream().filter(child -> child.nodeType == NodeType.Operator).collect(Collectors.toList());
        nodes.forEach(node -> res.add(((Operator) node).op));
        return res;
    }


    /**
     * Generated Source File: Do not modify !!!
     *
     * @param o jsonObject
     * @return Op Type
     */
    private Op convertOp(Object o) {
        if (!(o instanceof Map)) {
            $.die("Contract Violate: Invalid Argument");
            return Op.Unsupported;
        }

        String value = (String) ((JSONObject) o).get("value");
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

        if (value.equals("where")) {
            return Op.Where;
        }

        if (value.equals("<:")) {
            return Op.SubType;
        }

        if (value.equals(">:")) {
            return Op.BaseType;
        }

        if (value.equals(".")) {
            return Op.Dot;
        }

        if (value.equals("∘")) {
            return Op.FuncCombine;
        }

        if (value.equals("=")) {
            return Op.Assign;
        }

        if (value.equals("->")) {
            return Op.Lambda;
        }

        if (value.equals(":")) {
            return Op.Range;
        }

        if (value.equals("::")) {
            return Op.TypeDecl;
        }

        if (value.equals("=>")) {
            return Op.Mapsto;
        }

        if (value.equals("...")) {
            return Op.VarArg;
        }

        $.msg("[please report] unsupported operator: " + value + "\n");
        return Op.Unsupported;
    }

    private List<Node> extractSymbols(List<Node> args) {
        return args.stream().filter(e -> e.nodeType == Symbol).collect(Collectors.toList());
    }


    private List<Node> convertNodeList(Object o) {
        if (o == null) {
            return null;
        } else {
            List<JSONObject> in = (List<JSONObject>) o;
            List<Node> out = new LinkedList<>();
            for (JSONObject m : in) {
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
                n.nodeType == NodeType.Comma ||
                n.nodeType == LSQUARE ||
                n.nodeType == RSQUARE ||
                n.nodeType == LBRACE ||
                n.nodeType == RBRACE
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
        if (!Analyzer.self.hasOption("debug")) {
            new File(exchangeFile).delete();
            new File(endMark).delete();
        }
    }

}
