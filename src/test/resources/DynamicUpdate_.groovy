vm.breakpoint("DynamicUpdate",6) {
    hit++;

    def bp = vm.breakpoint("DynamicUpdate",18) {
        hit++;
        println "break"
    }

    vm.breakpoint("DynamicUpdate",8) {
        println "disabled"
        bp.disable()
        hit++;
    }
}