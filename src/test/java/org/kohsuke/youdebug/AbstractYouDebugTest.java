package org.kohsuke.youdebug;

import junit.framework.TestCase;

import java.net.URL;
import java.io.File;
import java.util.Collections;

import groovy.lang.GroovyCodeSource;

/**
 * Base class for unit tests.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractYouDebugTest extends TestCase {
    protected void join(VM vm) throws InterruptedException {
        assertEquals(0,vm.getVirtualMachine().process().waitFor());
    }

    protected VM fork(String cmdLine) throws Exception {
        URL url = getClass().getClassLoader().getResource("SubStringTest.class");
        File path = new File(url.getPath());

        VM vm = VMFactory.launch("-cp "+path.getParent()+" "+ cmdLine);
        Process p = vm.getVirtualMachine().process();
        new StreamCopyThread(p.getInputStream(),System.out).start();
        new StreamCopyThread(p.getErrorStream(),System.err).start();
        p.getOutputStream().close();

        return vm;
    }

    /**
     * Executes the given script and returns the hit counter.
     */
    protected int exec(VM vm, String script) throws Exception {
        Counter counter = new Counter();
        vm.execute(
            new GroovyCodeSource(getClass().getClassLoader().getResource(script)),
            Collections.singletonMap("hit",counter));
        return counter.n;
    }

    public static class Counter {
        int n;

        public Object next() {
            n++;
            return this;
        }
    }
}
