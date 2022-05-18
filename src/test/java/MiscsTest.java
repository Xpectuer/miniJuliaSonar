import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import tech.jiayezheng.miniJuliaSonar.$;


public class MiscsTest {

    private String json_path = "/Users/zhengjiaye/projects/java_proj/miniJuliaSonar/src/main/resources/tech/jiayezheng/miniJuliaSonar/julia/test1.json";


    @Test
    public void test() {
        // testJsonFromFile(json_path);
        //  Generator.genConvertOp();
         Generator.genVisitors();
        testSysProperty();
    }

    private void testJsonFromFile(String path) {
        String json = $.readFile(path);
        JSONObject jsonObject = (JSONObject) JSON.parse(json);
        System.out.println(jsonObject);
    }
    private void testSysProperty() {
        System.out.println(System.getProperty("os.arch"));
    }
}
