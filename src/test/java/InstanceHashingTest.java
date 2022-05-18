import org.junit.jupiter.api.Test;
import tech.jiayezheng.miniJuliaSonar.type.*;

import java.util.Collections;
import java.util.List;

public class InstanceHashingTest {

    @Test
    public void test() {

        StructType ctype = new StructType("testVector",null);
        List<Type> parameter = Collections.singletonList(Types.Int64Instance);
        InstanceType instance = new InstanceType(ctype, parameter);
        ctype.addInstance(instance);
        System.out.println(ctype.getInstance(Collections.singletonList(Types.Int64Instance))) ;

    }
}
