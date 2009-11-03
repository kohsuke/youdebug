package org.kohsuke.autodbg;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.EventRequest;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.Arrays;

/**
 * Method augmentation on top of JDI for Groovy
 *
 * @author Kohsuke Kawaguchi
 */
public class JDICategory {
    /**
     * Deletes this event request.
     */
    public static void delete(EventRequest req) {
        req.virtualMachine().eventRequestManager().deleteEventRequest(req);
    }

    /**
     * Gets the variables visible in this stack frame in a Groovy-friendly fashion.
     */
    public static StackFrameVariables getVars(StackFrame f) {
        return new StackFrameVariables(f);
    }

    /**
     * Gets the variables visible at that top of the stack frame of the given thread.
     */
    public static StackFrameVariables getVars(ThreadReference tr) throws IncompatibleThreadStateException {
        return new StackFrameVariables(tr.frame(0));
    }

    public static Object methodMissing(ObjectReference ref, String name, Object[] args) {
        System.out.println("Invoking "+name+" on "+ref+" with "+ Arrays.asList(args));
        return null;
    }

    static {
        MetaClass mc = DefaultGroovyMethods.getMetaClass(ObjectReference.class);
        GroovyObject mmc = (GroovyObject) mc;

        mmc.setProperty("methodMissing",new Closure(null) {
            public Object call(Object[] args) {
                return methodMissing((ObjectReference)getDelegate(),
                        (String)args[0],(Object[])args[1]);
            }
            public Class[] getParameterTypes() {
                // groovy runtime expects this exact array
                return new Class[]{String.class,Object.class};
            }
        });
    }
}
