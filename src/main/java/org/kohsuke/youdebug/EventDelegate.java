package org.kohsuke.youdebug;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ThreadReference;
import groovy.lang.GroovyObjectSupport;

/**
 * Used as the delegate of the breakpoint callback, instead of passing in {@link ThreadReference} directly.
 *
 * This prevents exposure of the protected fields of {@link ThreadReference} implementation classes, such
 * as 'vm', 'ref', and so on.
 *
 * @author Kohsuke Kawaguchi
 */
final class EventDelegate extends GroovyObjectSupport {
    private final ThreadReference thread;

    EventDelegate(ThreadReference thread) {
        this.thread = thread;
    }

    /**
     * Returns the current thread.
     */
    public ThreadReference getThread() {
        return thread;
    }

    /**
     * Returns the 'this' object in the current stack frame of the target JVM.
     */
    public Object getSelf() {
        return getProperty("this");
    }

    public Object getProperty(String property) {
        try {
            if (property.equals("thread"))   return thread;
            if (property.equals("self"))     return getSelf();

            return JDICategory.propertyMissing(thread.frame(0),property);
        } catch (IncompatibleThreadStateException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setProperty(String property, Object newValue) {
        try {
            JDICategory.propertyMissing(thread.frame(0),property,newValue);
        } catch (IncompatibleThreadStateException e) {
            throw new IllegalStateException(e);
        }
    }
}
