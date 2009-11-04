import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @author Kohsuke Kawaguchi
 */
public class LongRunningProgram {
    public static void main(String[] args) throws Exception {
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }
}
