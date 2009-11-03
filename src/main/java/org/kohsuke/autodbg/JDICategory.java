package org.kohsuke.autodbg;

import com.sun.jdi.request.EventRequest;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Field;
import com.sun.jdi.Value;
import com.sun.jdi.ReferenceType;
import groovy.lang.MissingPropertyException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

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
}
