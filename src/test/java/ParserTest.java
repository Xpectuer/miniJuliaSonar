import org.junit.jupiter.api.Test;
import tech.jiayezheng.miniJuliaSonar.Parser;

public class ParserTest {
    @Test
    public void test() {
        Parser parser = new Parser();
        parser.parseFile("/Users/zhengjiaye/projects/java_proj/miniJuliaSonar/src/main/resources/tech/jiayezheng/miniJuliaSonar/julia/test1.jl");

    }
}
