import junit.framework.Assert

def count = 0;

vm.breakpoint("ScopedAccess",10) {
    println "hit";

    a = 100;    // set local variable

    count++;    // this is 'owner.count', not 'delegate.count' unlike others
    if (count==2) {
        flag = false; // set static field in scope
        Assert.assertEquals(42,o.answer(0)) // access to instance field
        Assert.assertEquals("custom tostring",o.toString());
    }
}

vm.breakpoint("ScopedAccess",11) {
    Assert.assertEquals(100,i); // make sure the assignment to 'a' took effect
}
