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
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.*;
import java.lang.NoSuchMethodException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point.
 */
public class YouDebug {
    @Option(name="-pid",usage="Attaches to the local process of the given PID")
    public int pid = -1;

    @Option(name="-socket",usage="Attaches to the target process by a socket",metaVar="[HOST:]PORT")
    public String remote = null;

    @Option(name="-toolsJar",usage="Specify the location of the tools.jar, if it's in a non-standard location",metaVar="JAR")
    public File toolsJar = null;

//    @Option(name="-force")
//    public boolean force = false;
//
    @Argument
    public File script;

    public int debugLevel=0;

    @Option(name="-debug",usage="Increase the debug output level. Specify multiple times to get more detailed logging")
    public void setDebugLevel(boolean b) {
        debugLevel++;
    }

    public static void main(String[] args) throws Exception {
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

    public int run() throws Exception {
        ensureJDILoaded();

        if (debugLevel>0) {
            ConsoleHandler h = new ConsoleHandler();
            h.setLevel(Level.ALL);
            Logger logger = Logger.getLogger("org.kohsuke.youdebug");
            Level lv;
            switch (debugLevel) {
            case 1:     lv = Level.FINE; break;
            case 2:     lv = Level.FINER; break;
            default:    lv = Level.FINEST; break;
            }
            logger.setLevel(lv);
            logger.addHandler(h);
        }

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

        vm.execute(script!=null ? script : null);
        return 0;
    }

    /**
     * If JDI is not already loadable, make sure it's loadable by adding <tt>tools.jar</tt> to the classpath.
     */
    private void ensureJDILoaded() throws CmdLineException, MalformedURLException {
        try {
            getClass().getClassLoader().loadClass("com.sun.jdi.ThreadReference");
            // if JDK is already loaded, we don't need to resolve tools.jar.
        } catch (ClassNotFoundException _) {
            // resolve tools.jar
            if (toolsJar==null) {
                // locate tools.jar via java.home if not specified
                String home = System.getProperty("java.home");
                toolsJar = new File(new File(home), "../lib/tools.jar");
                if (!toolsJar.exists()) {
                    toolsJar = new File(new File(home), "../Classes/classes.jar");
                    if (!toolsJar.exists())
                        throw new CmdLineException("Specify the location of tools.jar via -toolsJar, or run this tool with JDK (your java home is at "+home+')');
                }
            }
            if (!toolsJar.exists())
                throw new CmdLineException("No such file: "+toolsJar);

            // shove tools.jar into the classpath
            try {
                ClassLoader cl = ClassLoader.getSystemClassLoader();
                Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                m.setAccessible(true);
                m.invoke(cl,toolsJar.toURL());
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                throw new AssertionError(e);
            }
        }
    }
}
