/**
 * @author Kohsuke Kawaguchi
 */
public class SubStringTest {
    public static void main(String[] args) {
        try {
            String s = someLengthComputationOfString();
            System.out.println(s.substring(5));

            System.exit(1); // should throw an exception
        } catch (StringIndexOutOfBoundsException e) {
            System.out.println("caught");
        }
    }

    private static String someLengthComputationOfString() {
        return "test";
    }
}
