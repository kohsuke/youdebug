/**
 * @author Kohsuke Kawaguchi
 */
public class SubStringTest {
    public static void main(String[] args) {
        String s = someLengthComputationOfString();
        System.out.println(s.substring(5));
    }

    private static String someLengthComputationOfString() {
        return "test";
    }
}
