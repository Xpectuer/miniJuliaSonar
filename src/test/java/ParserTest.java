import org.junit.jupiter.api.Test;
import tech.jiayezheng.miniJuliaSonar.Analyzer;
import tech.jiayezheng.miniJuliaSonar.Parser;
import tech.jiayezheng.miniJuliaSonar.ast.Node;
import tech.jiayezheng.miniJuliaSonar.ast.Symbol;

/**
 *  This test is to test julia script to be normally lauched
 */
public class ParserTest {
    @Test
    public void json_dump() {
        Analyzer analyzer = new Analyzer();
        // Analyzer.self.setOption("debug");
        test("/Users/zhengjiaye/projects/java_proj/miniJuliaSonar/src/main/resources/tech/jiayezheng/miniJuliaSonar/julia/test1.jl");
        test("/Users/zhengjiaye/projects/java_proj/miniJuliaSonar/src/main/resources/tech/jiayezheng/miniJuliaSonar/julia/test_short.jl");
    }

    public void test(String path) {
        Node root = new Parser().parseFile(path);
        System.out.println("test1:\n"+root);
    }
}
