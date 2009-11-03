package org.kohsuke.autodbg;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.event.BreakpointEvent;

/**
 * Object passed to the breakpoint closure as a delegate.
 *
 * <p>
 * This mostly mimicks a {@link ThreadReference} but also exposes some additional stuff.
 *
 * @author Kohsuke Kawaguchi
 */
public class BreakpointDelegate extends ThreadReferenceFilter {
    BreakpointDelegate(BreakpointEvent e) {
        super(e.thread());
    }

    // evaluating a method invocation will render the frame unusable,
    // so we can't really cache this
    public StackFrameVariables getVars() {
        try {
            return new StackFrameVariables(ref.frame(0));
        } catch (IncompatibleThreadStateException x) {
            throw new IllegalStateException(x);
        }
    }

    public Object propertyMissing(String name) {
        return getVars().getProperty(name);
    }

    public void propertyMissing(String name, Object value) {
        getVars().setProperty(name,value);
    }
}
