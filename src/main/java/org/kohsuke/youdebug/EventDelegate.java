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

    public Object getProperty(String property) {
        try {
            if (property.equals("thread"))   return thread;
            
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
