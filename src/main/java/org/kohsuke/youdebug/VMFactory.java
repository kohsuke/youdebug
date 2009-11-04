package org.kohsuke.youdebug;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector.Argument;

import java.io.IOException;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class VMFactory {
    /**
     * Connects via a socket
     */
    public static VM connectRemote(String host, int port) throws IllegalConnectorArgumentsException, IOException {
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        AttachingConnector a = findConnector(vmm, "com.sun.jdi.SocketAttach");
        Map<String,Argument> args = a.defaultArguments();
        args.get("hostname").setValue(host);
        args.get("port").setValue(String.valueOf(port));
        return new VM(a.attach(args));
    }

    public static VM connectLocal(int pid) throws IllegalConnectorArgumentsException, IOException {
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        AttachingConnector a = findConnector(vmm, "com.sun.jdi.ProcessAttach");
        Map<String,Argument> args = a.defaultArguments();
        args.get("pid").setValue(String.valueOf(pid));
        return new VM(a.attach(args));
    }

    private static AttachingConnector findConnector(VirtualMachineManager vmm, String name) {
        for (AttachingConnector ac : vmm.attachingConnectors())
            if (ac.name().equals(name))
                return ac;
        throw new IllegalArgumentException("Unable to find a connector named "+name);
    }
}
