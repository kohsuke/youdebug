/**
 * @author Kohsuke Kawaguchi
 */
public class DynamicUpdate {
    public static void test1() {
        activate();
        interrupt();
        deactivate();
        interrupt();
    }

//-------------------

    private static void activate() {
    }
    
    private static void interrupt() {
        System.out.println("--interrupt");
    }

    private static void deactivate() {
    }

    public static void main(String[] args) {
        test1();
    }
}
