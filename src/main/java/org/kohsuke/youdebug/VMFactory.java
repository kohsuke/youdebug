package org.kohsuke.youdebug;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;

import java.io.IOException;
import java.util.Map;
import java.util.List;

/**
 * Connects to {@link VM} in various ways.
 *
 * @author Kohsuke Kawaguchi
 */
public class VMFactory {
    /**
     * Connects via a socket
     */
    public static VM connectRemote(String host, int port) throws IllegalConnectorArgumentsException, IOException {
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        AttachingConnector a = findConnector("com.sun.jdi.SocketAttach", vmm.attachingConnectors());
        Map<String,Argument> args = a.defaultArguments();
        args.get("hostname").setValue(host);
        args.get("port").setValue(String.valueOf(port));
        return new VM(a.attach(args));
    }

    public static VM connectLocal(int pid) throws IllegalConnectorArgumentsException, IOException {
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        AttachingConnector a = findConnector("com.sun.jdi.ProcessAttach", vmm.attachingConnectors());
        Map<String,Argument> args = a.defaultArguments();
        args.get("pid").setValue(String.valueOf(pid));
        return new VM(a.attach(args));
    }

    public static VM launch(String commandLine) throws IllegalConnectorArgumentsException, IOException, VMStartException {
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        LaunchingConnector a = findConnector("com.sun.jdi.CommandLineLaunch", vmm.launchingConnectors());
        Map<String,Argument> args = a.defaultArguments();
        args.get("main").setValue(commandLine);
        return new VM(a.launch(args));
    }

    private static <T extends Connector> T findConnector(String name, List<T> connectors) {
        for (T c : connectors)
            if (c.name().equals(name))
                return c;
        throw new IllegalArgumentException("Unable to find a connector named "+name);
    }
}
