import tech.jiayezheng.miniJuliaSonar.$;
import tech.jiayezheng.miniJuliaSonar.ast.NodeType;
import tech.jiayezheng.miniJuliaSonar.ast.Op;
import tech.jiayezheng.miniJuliaSonar.ast.Str;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Generator {
    public static void genConvertOp() {
        StringBuilder buffer = new StringBuilder();
        Set<String> arith_set = new HashSet<>();
        {
            arith_set.add("+");
            arith_set.add("-");
            arith_set.add("*");
            arith_set.add("/");
        }


        String header = " /**\n" +
                "     * Generated Source File: Do not modify !!!\n" +
                "     *\n" +
                "     * @param o jsonObject\n" +
                "     * @return Op Type\n" +
                "     */\n";
        String header1 = "private Op convertOp(Object o) {\n" +
                "        if (!(o instanceof Map)) {\n" +
                "            $.die(\"Contract Violate: Invalid Argument\");\n" +
                "            return Op.Unsupported;\n" +
                "        }\n" +
                "\n" +
                "        String value = (String) ((JSONObject) o).get(\"value\");\n";

        buffer.append(header);
        buffer.append(header1);

        String format = "\t\tif(value.equals(\"%1$s\")) {\n" +
                "\t\t\treturn Op.%2$s;\n" +
                "\t\t}\n\n";
        String format1 = "\t\tif(value.equals(\"\\%1$s\")) {\n" +
                "\t\t\treturn Op.%2$s;\n" +
                "\t\t}\n\n";
        String formatArith = "\t\tif(value.equals(\"%1$s\") || value.equals(\"%1$s=\")) {\n" +
                "\t\t\treturn Op.%2$s;\n" +
                "\t\t}\n\n";

        for (Op op : Op.values()) {
            if(op != Op.Unsupported) {
                if (op.getRep().equals("\\")) {
                    String s = String.format(format1, op.getRep(), op.name());
                    buffer.append(s);

                } else if (arith_set.contains(op.getRep())) {
                    String s = String.format(formatArith, op.getRep(), op.name());
                    buffer.append(s);
                } else {
                    String s = String.format(format, op.getRep(), op.name());
                    buffer.append(s);

                }
            }
        }


        String foot = "        $.msg(\"[please report] unsupported operator: \" + value + \"\\n\");\n" +
                "        return Op.Unsupported;\n" +
                "    }";
        buffer.append(foot);


        $.writeFile("./gen/gen_op.txt", buffer.toString());

    }




    public static void genVisitors() {

        Set<NodeType> banned = new HashSet<>();
        banned.add(NodeType.Imaginary);

        StringBuilder buffer = new StringBuilder();

        String header = " /**\n" +
                "     * Generated Source File: Do not modify !!!\n" +
                "     *\n" +
                "     */\n";

        String header1 = "public interface Visitor1<T, P> {\n" +
                "\n" +
                "\tdefault T visit(@NotNull Node node, P param) {\n" +
                "\t\tswitch (node.nodeType) {\n";

        buffer.append(header);
        buffer.append(header1);


        for(NodeType nodeType : NodeType.values()) {
            if(!banned.contains(nodeType)) {
                String format = "\t\t\tcase %s:\n" +
                        " \t\t\t\treturn visit((%s)node, param);\n";
                String code = String.format(format, nodeType.toString(), nodeType);
                buffer.append(code);
            }

        }

        String sdefault = "\t\t\tdefault:\n" +
                "\t\t\t\tthrow new RuntimeException(\"unexpected node\");\n";

        buffer.append(sdefault);
        buffer.append("\t\t}\n\t}");

        String visitList = "    default <N extends Node, O extends T> List<O> visit(List<N> list, P param) {\n" +
                "        List<O> result = new ArrayList<>();\n" +
                "        for (N elem : list) {\n" +
                "            result.add((O) visit(elem, param));\n" +
                "        }\n" +
                "        return result;\n" +
                "    }\n";


        for(NodeType nodeType : NodeType.values()) {
            if(!banned.contains(nodeType)) {
                String format = "\tT visit(%s node, P param);\n";
                String code = String.format(format, nodeType.toString());
                buffer.append(code);
            }

        }

        buffer.append("}\n");

        $.writeFile("./gen/gen_visitor.txt", buffer.toString());
    }
}
