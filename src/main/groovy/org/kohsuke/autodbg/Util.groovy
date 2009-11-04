package org.kohsuke.autodbg

import com.sun.management.HotSpotDiagnosticMXBean
import org.kohsuke.autodbg.VM
import java.lang.management.ManagementFactory

/**
 *
 *
 * @author Kohsuke Kawaguchi
 */

public class Util {
    public static void dumpHeap(VM vm) {
        def mf = vm.ref(ManagementFactory.class)
        def server = mf.getPlatformMBeanServer();
        def bean = mf.newPlatformMXBeanProxy(server,"com.sun.management:type=HotSpotDiagnostic", vm.ref(HotSpotDiagnosticMXBean.class));
        bean.dumpHeap("/tmp/heapdump",true);
    }
}