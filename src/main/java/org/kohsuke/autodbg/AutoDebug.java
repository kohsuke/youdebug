package org.kohsuke.autodbg;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

import java.io.IOException;
import java.io.FileInputStream;
import java.util.Map;

public class AutoDebug {
    public static void main(String[] args) throws Exception {
        VirtualMachine vm = connectRemote("localhost",5005);
        vm.resume();
        new VM(vm).execute(new FileInputStream(args[0]));
    }


    /**
     * Connects via a socket
     */
    public static VirtualMachine connectRemote(String host, int port) throws IllegalConnectorArgumentsException, IOException {
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        AttachingConnector a = findConnector(vmm, "com.sun.jdi.SocketAttach");
        Map<String,Argument> args = a.defaultArguments();
        args.get("hostname").setValue(host);
        args.get("port").setValue(String.valueOf(port));
        return a.attach(args);
    }

    public static VirtualMachine connectLocal(int pid) throws IllegalConnectorArgumentsException, IOException {
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        AttachingConnector a = findConnector(vmm, "com.sun.jdi.ProcessAttach");
        Map<String,Argument> args = a.defaultArguments();
        args.get("pid").setValue(String.valueOf(pid));
        return a.attach(args);
    }

    private static AttachingConnector findConnector(VirtualMachineManager vmm, String name) {
        for (AttachingConnector ac : vmm.attachingConnectors())
            if (ac.name().equals(name))
                return ac;
        throw new IllegalArgumentException("Unable to find a connector named "+name);
    }
}
