import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import tech.jiayezheng.miniJuliaSonar.$;
import tech.jiayezheng.miniJuliaSonar.ast.Node;
import tech.jiayezheng.miniJuliaSonar.ast.NodeType;
import tech.jiayezheng.miniJuliaSonar.ast.Op;
import tech.jiayezheng.miniJuliaSonar.ast.Root;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class MiscsTest {

    @Test
    public void test() {
        testJsonBool();
    }

    private void genConvertOp() {
        String format =  "if(value.equals(\"%1$s\")) {\n" +
                "\t\treturn Op.%2$s;\n" +
                "}\n\n";
        String format1 =  "if(value.equals(\"\\%1$s\")) {\n" +
                "\t\treturn Op.%2$s;\n" +
                "}\n\n";


        for(Op op : Op.values()) {
            if(op.getRep().equals("\\")) {
                System.out.printf((format1) + "%n",op.getRep(),op.name());

            } else {
                System.out.printf((format) + "%n",op.getRep(),op.name());
            }

        }
    }

    private void testJsonBool() {
        String json = "{\"key\":false}";
        JSONObject jobj= JSON.parseObject(json);

        Boolean b = (Boolean) jobj.get("key");
        System.out.println(b);
    }


}
