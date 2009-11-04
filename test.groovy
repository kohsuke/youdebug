def i=0;

bp = vm.breakpoint("Test",9) {
    org.kohsuke.youdebug.Util.dumpHeap(vm);

    boolean b = flag;

    // access local variables. Either use 'vars' to disambiguate, or just use the variable name directly for convenience
    println "Hit ${b} : ${flag.class}";

    // making a method call
    println "o.answer=${vars.o.answer(1)}";
    // if you need to call ambiguous methods like equals/hashCode that the local proxy object implements on its own,
    // use the '@' notation like this
    println "o.toString()=${o.'@toString'()}";
    println "o.toString()=${o.toString()}";

    // making a static method call
    vm.ref(System.class).out.'@println'("Hello from debugger");

    if (++i>2) {
        vars.flag = false;
        bp.disable();
        vm.dumpAllThreads();
    }
}

vm.classPrepare(IllegalArgumentException.class) { t ->
    println "Loaded the exception breakpoint";
    vm.exceptionBreakpoint(t) { e ->
        println "Caught ${e}";
        dumpThread(System.err);
    }
}
