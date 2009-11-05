package org.kohsuke.youdebug;

import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Entry point.
 */
public class YouDebug {
    @Option(name="-pid",usage="Attaches to the local process of the given PID")
    public int pid = -1;

    @Option(name="-socket",usage="Attaches to the target process by a socket",metaVar="[HOST:]PORT")
    public String remote = null;

//    @Option(name="-force")
//    public boolean force = false;
//
    @Argument
    public File script;

    public static void main(String[] args) throws Exception {
        // locate tools.jar first
        String home = System.getProperty("java.home");
        File toolsJar = new File(new File(home), "../lib/tools.jar");
        if (!toolsJar.exists()) {
            System.err.println("This tool requires a JDK, but you are running Java from "+home);
            System.exit(1);
        }

        // shove tools.jar into the classpath
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        m.setAccessible(true);
        m.invoke(cl,toolsJar.toURL());


        YouDebug main = new YouDebug();
        CmdLineParser p = new CmdLineParser(main);
        try {
            p.parseArgument(args);
            System.exit(main.run());
        } catch (CmdLineException e) {
            System.out.println(e.getMessage());
            System.out.println("Usage: java -jar youdebug.jar [options...] [script file]");
            p.printUsage(System.out);
            System.exit(1);
        }
    }

    public int run() throws CmdLineException, IOException, IllegalConnectorArgumentsException, InterruptedException, AgentInitializationException, AgentLoadException, AttachNotSupportedException {
        VM vm = null;
        if (pid>=0) {
            vm = VMFactory.connectLocal(pid);
//            try {
//            } catch (IOException e) {
//                VirtualMachine avm = VirtualMachine.attach(String.valueOf(pid));
//                ServerSocket ss = new ServerSocket();
//                int port =ss.getLocalPort();
//                ss.close();
//                System.out.println("Trying x");
//                avm.loadAgentLibrary("jdwp","transport=dt_socket,server=y,suspend=n,address=9999");
//                avm.detach();
//                remote = "127.0.0.1:"+port;
//            }
        }
        if (remote!=null) {
            String[] tokens = remote.split(":");
            if (tokens.length==1)   tokens = new String[]{"localhost",tokens[0]};
            if (tokens.length!=2)   throw new CmdLineException("Invalid argument to the -socket option: "+remote);
            vm = VMFactory.connectRemote(tokens[0],Integer.valueOf(tokens[1]));
        }
        if (vm==null)
            throw new CmdLineException("Neither -pid nor -socket option was specified");

        vm.execute(script!=null ? new FileInputStream(script) : null);
        return 0;
    }
}
