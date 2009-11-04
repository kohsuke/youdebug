import org.kohsuke.youdebug.VM

def i=0;

VM v = vm;

bp = v.breakpoint("Test",9) {
    // access local variables. Either use 'vars' to disambiguate, or just use the variable name directly for convenience
    println "Hit ${vars.flag} : ${flag.class}";

    // making a method call
    println "o.answer=${vars.o.answer(1)}";
    // if you need to call ambiguous methods like equals/hashCode that the local proxy object implements on its own,
    // use the '@' notation like this
    println "o.toString()=${o.'@toString'()}";
    println "o.toString()=${o.toString()}";

    // making a static method call
    v.ref(System.class).out.'@println'("Hello from debugger");

    if (++i>0) {
        vars.flag = false;
        bp.disable();
        v.dumpAllThreads();
    }
}

v.classPrepare(IllegalArgumentException.class) { t ->
    println "Loaded the exception breakpoint";
    v.exceptionBreakpoint(t) { e ->
        println "Caught ${e}";
        e.dumpStackTrace(System.err)
        v.dumpHeap();
    }
}
