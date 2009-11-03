package org.kohsuke.autodbg;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Method;
import com.sun.jdi.Value;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.request.EventRequest;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

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
        LOGGER.info("Invoking "+name+" on "+ref+" with "+ Arrays.asList(args));
        
        Method m = chooseMethod(ref,name,args);
        if (m==null)    throw new NoSuchMethodException("No such method "+name+" on "+ref.referenceType()+" that takes "+Arrays.asList(args));

        List<Value> arguments = new ArrayList<Value>();
        for (Object a : args)
            arguments.add(Variable.wrap(ref.virtualMachine(),a));

        try {
            return Variable.unwrap(ref.invokeMethod( VM.current().getCurrentThread(), m, arguments, 0));
        } catch (InvalidTypeException e) {
            throw new org.kohsuke.autodbg.InvocationException(e);
        } catch (ClassNotLoadedException e) {
            throw new org.kohsuke.autodbg.InvocationException(e);
        } catch (IncompatibleThreadStateException e) {
            throw new org.kohsuke.autodbg.InvocationException(e);
        } catch (InvocationException e) {
            throw new org.kohsuke.autodbg.InvocationException(e);
        }
    }

    // TODO: proper selection
    private static Method chooseMethod(ObjectReference ref, String name, Object[] args) {
        return ref.referenceType().methodsByName(name).get(0);
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

    private static final Logger LOGGER = Logger.getLogger(JDICategory.class.getName());
}
