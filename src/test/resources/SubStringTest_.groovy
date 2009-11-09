import junit.framework.Assert

vm.breakpoint("SubStringTest",8) {
    println "s="+s;
    Assert.assertEquals(s,"test")
    Assert.assertEquals(thread.frame(0).s,"test")
    hit++;

    vm.exceptionBreakpoint(StringIndexOutOfBoundsException) { e ->
        e.dumpStackTrace();

        // evaluate getMessage()
        Assert.assertEquals("String index out of range: -1",e.getMessage());
        // local hashCode vs remote hashCode
        Assert.assertTrue(e.hashCode()!=e.'@hashCode'());

        // reference to a static field
        vm._(System).out.println("Hello from debugger");

        // static method invocation
        Assert.assertEquals(
            System.getProperty("java.home"),
            vm.ref("java.lang.System").getProperty("java.home"));

        // new instance
        def sw = vm.loadClass(StringWriter.class)."@new"();
        def pw = vm.loadClass(PrintWriter.class)."@new"(sw);
        e.printStackTrace(pw);
        String dump = sw.toString();
        System.out.println(dump);
        Assert.assertTrue(dump.contains(StringIndexOutOfBoundsException.class.name))

        thread.dumpThread();   // should dump the current thread
        vm.threads*.dumpThread();   // all threads

        // local variable access
        Assert.assertEquals(delegate."@0",5);
        Assert.assertEquals(delegate."@1",4);

        Assert.assertEquals(thread.frame(1)."@0",5);

        hit++;
    }
}
