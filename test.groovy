def i=0;

vm.breakpoint("Test",9) {
    vars.o.test();
    println "Hit ${vars.flag} : ${vars.flag.class}";
    if (++i>3)
        vars.flag = false;
}
