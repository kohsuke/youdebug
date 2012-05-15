import junit.framework.Assert

// test the access to 'this' object
vm.breakpoint("ThisAccess",14) {
    println "hit";

    def o = delegate."@this";
    Assert.assertEquals("something",o.getSomething())
    o.end = true;
}
