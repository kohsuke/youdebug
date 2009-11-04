package org.kohsuke.youdebug;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

public class YouDebug {
    public static void main(String[] args) throws Exception {
        VM vm = VMFactory.connectRemote("localhost",5005);
        vm.resume();
        vm.execute(new FileInputStream(args[0]));
    }
}
