package org.kohsuke.youdebug;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;

/**
 * Variables visible at the given stack frame, exposed in a Groovy friendly fashion.
 *
 * @author Kohsuke Kawaguchi
 */
public class StackFrameVariables extends GroovyObjectSupport {
    private final StackFrame frame;

    public StackFrameVariables(StackFrame frame) {
        this.frame = frame;
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
        if (name.equals("_this"))
            return frame.thisObject();
        throw new MissingPropertyException(name,ThreadReference.class);
    }

    public void setProperty(String name, Object o) {
        Variable v = getVariable(name);
        if (v!=null)    v.set(o);
        else            throw new MissingPropertyException(name,getClass());
    }

    public Variable getVariable(String name) {
        try {
            if (name.startsWith("ref"))
                return Variable.fromMethodArgument(frame,Integer.parseInt(name.substring(1)));
        } catch (NumberFormatException e) {
            // fall through
        } catch (LinkageError e) {
            // fall through
        }

        // local variable?
        try {
            LocalVariable v = frame.visibleVariableByName(name);
            if (v!=null)
                return Variable.fromLocalVariable(frame,v);
        } catch (AbsentInformationException e) {
            // fall through
//                f.location().method().
        }

        // instance field?
        ObjectReference $this = frame.thisObject();
        if ($this!=null) {
            Field fi = $this.referenceType().fieldByName(name);
            if (fi!=null)
                return Variable.fromField($this,fi);
        }

        // static field?
        ReferenceType t = frame.location().declaringType();
        Field fi = t.fieldByName(name);
        if (fi!=null && fi.isStatic())
            return Variable.fromField(t,fi);

        return null;
    }
}
