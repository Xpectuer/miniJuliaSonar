Compiled from "MiscsTest.java"
public class MiscsTest {
  public MiscsTest();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public void test();
    Code:
       0: ldc           #2                  // String nm$l
       2: astore_1
       3: ldc           #3                  // String nm$ldsaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
       5: astore_2
       6: getstatic     #4                  // Field java/lang/System.out:Ljava/io/PrintStream;
       9: aload_1
      10: aload_2
      11: if_acmpne     18
      14: iconst_1
      15: goto          19
      18: iconst_0
      19: invokevirtual #5                  // Method java/io/PrintStream.println:(Z)V
      22: return
}
