import junit.framework.Assert

vm.breakpoint("SubStringTest",8) {
    println "s="+s;
    Assert.assertEquals(s,"test")
    hit++;

    vm.exceptionBreakpoint(StringIndexOutOfBoundsException) { e ->
        e.dumpStackTrace();
        hit++;
    }
}
