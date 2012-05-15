/**
 * @author Kohsuke Kawaguchi
 */
public class ThisAccess {
    public volatile boolean end;

    public static void main(String[] args) throws Exception {
   	    new ThisAccess().run();
   	}

    public void run() throws Exception {
        System.out.println("Started");
        while (!end) {
            Thread.sleep(100);
        }
    }

    public String getSomething() {
        return "something";
    }
}
