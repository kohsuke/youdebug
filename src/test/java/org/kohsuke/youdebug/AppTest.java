package org.kohsuke.youdebug;

public class AppTest extends AbstractYouDebugTest {
    public void testApp() throws Exception {
        VM vm = fork("SubStringTest");
        assertEquals(2,exec(vm, "SubStringTest_.groovy"));
        join(vm);
    }

    public void testDynamicUpdate() throws Exception {
        VM vm = fork("DynamicUpdate");
        assertEquals(3,exec(vm, "DynamicUpdate_.groovy"));
        join(vm);
    }
}
