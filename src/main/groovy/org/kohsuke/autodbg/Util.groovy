package org.kohsuke.autodbg

import com.sun.management.HotSpotDiagnosticMXBean
import org.kohsuke.autodbg.VM
import java.lang.management.ManagementFactory
import com.sun.jdi.ReferenceType
import com.sun.jdi.ObjectReference

/**
 *
 *
 * @author Kohsuke Kawaguchi
 */

public class Util {
    /**
     * Dumps the exception
     */
    public static void dumpStackTrace (ObjectReference exp, out) {
        def frames = exp.getStackTrace();
        out.println(exp.toString());
        int len = frames.length();
        for ( int i=0; i<len; i++ ) {
            def f = frames[i];
            out.println("\tat ${f.getClassName()}.${f.getMethodName()}(${f.getFileName()}:${f.getLineNumber()})")
        }
    }

    public static ReferenceType loadClass(VM vm, Class c) {
        try {
            return vm.ref(c)
        } catch (IllegalArgumentException e) {
            // force load
            def cl = vm.currentThread.frame(0).location().declaringType().classLoader();
            def clazz = cl.loadClass(c.name, true)
            clazz.getMethods(); // force preparation
            return clazz.reflectedType();
        };
    }

    public static void dumpHeap(VM vm) {
        use (JDICategory) {
            def mf = loadClass(vm,ManagementFactory.class)
            def server = mf.getPlatformMBeanServer();
            def bean = mf.newPlatformMXBeanProxy(server,"com.sun.management:type=HotSpotDiagnostic", vm.ref(HotSpotDiagnosticMXBean.class));
            new File("/tmp/heapdump").delete();
            bean.dumpHeap("/tmp/heapdump",true);
        }
    }
}