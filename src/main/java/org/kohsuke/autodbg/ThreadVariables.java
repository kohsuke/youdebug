package org.kohsuke.autodbg;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.IncompatibleThreadStateException;
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
    public Value getProperty(String name) {
        if (name.startsWith("_")) {
            try {
                name = name.substring(1);
                StackFrame f = thread.frame(0);

                try {
                    return f.getArgumentValues().get(Integer.parseInt(name));
                } catch (NumberFormatException e) {
                    // fall through
                } catch (LinkageError e) {
                    // fall through
                }

                // local variable?
                try {
                    LocalVariable v = f.visibleVariableByName(name);
                    if (v!=null)    return f.getValue(v);
                } catch (AbsentInformationException e) {
                    // fall through
    //                f.location().method().
                }

                // instance field?
                ObjectReference $this = f.thisObject();
                if ($this!=null) {
                    Field fi = $this.referenceType().fieldByName(name);
                    if (fi!=null)
                        return $this.referenceType().getValue(fi);
                }

                // static field?
                ReferenceType t = f.location().declaringType();
                Field fi = t.fieldByName(name);
                if (fi!=null && fi.isStatic())
                    return t.getValue(fi);
            } catch (IncompatibleThreadStateException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new MissingPropertyException(name,ThreadReference.class);
    }
}
