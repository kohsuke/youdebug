package org.kohsuke.autodbg;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;

/**
 * Variables visible at the top of the thread.
 *
 * @author Kohsuke Kawaguchi
 */
public class ThreadVariables extends GroovyObjectSupport {
    private final ThreadReference thread;

    public ThreadVariables(ThreadReference thread) {
        this.thread = thread;
    }

    /**
     * {@code t._name} refers to a variable of the given name visible in the thread.
     *
     * <p>
     * This searches all the local variables and arguments, instance fields and static fields.
     * As a special form, "_0", "_1", ... can be used to refer to method arguments by position.
     */
    public Object getProperty(String name) {
        Variable v = getVariable(name);
        if (v!=null)    return v.getUnwrapped();
        else            throw new MissingPropertyException(name,ThreadReference.class);
    }

    public void setProperty(String name, Object o) {
        Variable v = getVariable(name);
        if (v!=null)    v.set(o);
        else            throw new MissingPropertyException(name,getClass());
    }

    public Variable getVariable(String name) {
        try {
            StackFrame f = thread.frame(0);

            try {
                return Variable.fromMethodArgument(f,Integer.parseInt(name));
            } catch (NumberFormatException e) {
                // fall through
            } catch (LinkageError e) {
                // fall through
            }

            // local variable?
            try {
                LocalVariable v = f.visibleVariableByName(name);
                if (v!=null)
                    return Variable.fromLocalVariable(f,v);
            } catch (AbsentInformationException e) {
                // fall through
//                f.location().method().
            }

            // instance field?
            ObjectReference $this = f.thisObject();
            if ($this!=null) {
                Field fi = $this.referenceType().fieldByName(name);
                if (fi!=null)
                    return Variable.fromField($this,fi);
            }

            // static field?
            ReferenceType t = f.location().declaringType();
            Field fi = t.fieldByName(name);
            if (fi!=null && fi.isStatic())
                return Variable.fromField(t,fi);

            return null;
        } catch (IncompatibleThreadStateException e) {
            throw new IllegalStateException(e);
        }
    }
}
