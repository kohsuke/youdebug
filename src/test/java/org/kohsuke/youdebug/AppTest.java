package org.kohsuke.youdebug;

import junit.framework.TestCase;

import java.net.URL;
import java.io.File;

public class AppTest extends TestCase {
    public void testApp() throws Exception {
        URL url = getClass().getClassLoader().getResource("SubStringTest.class");
        File path = new File(url.getPath());

        VM vm = VMFactory.launch("-cp "+path.getParent()+" SubStringTest");
        Process p = vm.getVirtualMachine().process();
        new StreamCopyThread(p.getInputStream(),System.out).start();
        new StreamCopyThread(p.getErrorStream(),System.err).start();
        p.getOutputStream().close();
        vm.execute(getClass().getClassLoader().getResourceAsStream("_SubStringTest.groovy"));
        assertEquals(0,p.waitFor());
    }

    
}
