/*
    Makes sure that breakpoints can be dynamically disabled.
 */
vm.breakpoint("DynamicUpdate",6) {
    hit++;

    def bp = vm.breakpoint("DynamicUpdate",18) {
        hit++;
        println "break" // this should only happen once, instead of twice
    }

    def sbp;
    sbp = vm.breakpoint("DynamicUpdate",8) {
        println "disabled"
        bp.disable()
        sbp.delete()
        hit++;
    }
}