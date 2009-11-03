def i=0;

vm.breakpoint("Test",9) {
    // access local variables. Either use 'vars' to disambiguate, or just use the variable name directly for convenience
    println "Hit ${vars.flag} : ${flag.class}";

    if (++i>2) {
        // making a method call 
        println "o.answer: ${o.answer()}";
        vars.flag = false;
    }
}
