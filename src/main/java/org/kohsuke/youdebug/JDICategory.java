package org.kohsuke.youdebug;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.ArrayType;
import com.sun.jdi.Field;
import com.sun.jdi.Location;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.ArrayReference;
import static com.sun.jdi.ThreadReference.*;
import com.sun.jdi.request.EventRequest;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.MissingPropertyException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
import static java.util.logging.Level.FINE;
import java.util.logging.Logger;
import java.lang.reflect.InvocationTargetException;
import java.io.PrintStream;
import java.io.PrintWriter;

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

    /**
     * Index access to array.
     */
    public static Value getAt(ArrayReference a, int index) {
        return a.getValue(index);
    }

    /**
     * Allow method invocation directly on the {@link ObjectReference} instance.
     */
    public static Object methodMissing(ObjectReference ref, String name, Object... args) throws InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        name = unescape(name);

        if(LOGGER.isLoggable(FINE))
            LOGGER.fine("Invoking "+name+" on "+ref+" with "+ Arrays.asList(args));

        List<Value> arguments = Variable.wrapList(ref.virtualMachine(), args);
        try {
            return Variable.unwrap(ref.invokeMethod( VM.current().getCurrentThread(),
                    chooseMethod(ref.referenceType(), name, arguments, false),
                    arguments, 0));
        } catch (InvocationException e) {
            Util.dumpStackTrace(e.exception(),System.out);
            throw e;
        }
    }

    /**
     * Instance field retrieval.
     */
    public static Object propertyMissing(ObjectReference ref, String name) throws InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        return lvalue(ref, name).getUnwrapped();
    }

    /**
     * Instance field assignment.
     */
    public static void propertyMissing(ObjectReference ref, String name, Object value) throws InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        lvalue(ref, name).set(value);
    }

    private static Variable lvalue(ObjectReference ref, String name) {
        name = unescape(name);
        Field f = ref.referenceType().fieldByName(name);
        if (f==null)        throw new MissingPropertyException("No such property '"+name+"' on "+ref.referenceType().name());
        return Variable.fromField(ref,f);
    }

    /**
     * Select the right method to invoke based on the arguments and their types.
     *
     * @param staticOnly
     *      If true, only search the static methods.
     */
    private static Method chooseMethod(ReferenceType type, String name, List<Value> args, boolean staticOnly) throws ClassNotLoadedException {
        OUTER:
        for (Method m : type.methodsByName(name)) {
            if (m.argumentTypeNames().size()!=args.size())
                continue;       // length doesn't match    TODO: handle var args?
            if (staticOnly && !m.isStatic())
                continue;       // we are only looking for a static method

            List<Type> types = m.argumentTypes();
            for (int i = 0; i < args.size(); i++) {
                Value v = args.get(i);
                if (v==null)        continue;   // null is assignable to anything
                if (!isAssignableFrom(types.get(i),v.type()))
                    continue OUTER; // not assignable

            }

            return m;
        }
        throw new NoSuchMethodException("No such method "+name+" on "+type+" that takes "+Arrays.asList(args));
    }

    /**
     * Is the given value an instance of the given type?
     */
    public static boolean isAssignableFrom(Type type, Type subtype) throws ClassNotLoadedException {
        // this handles all the primitives and other simple cases
        if (subtype.equals(type))      return true;

        // transparent boxing/unboxing
        if (type instanceof PrimitiveType && subtype instanceof ClassType)
            return subtype.name().equals(primitive2box.get(type.name()));
        if (subtype instanceof PrimitiveType && type instanceof ClassType)
            return type.name().equals(primitive2box.get(subtype.name()));

        if (subtype instanceof ClassType) {
            ClassType ct = (ClassType) subtype;
            if (type instanceof ClassType) {
                for (ClassType s=ct; s!=null; s=s.superclass())
                    if (s.equals(type))
                        return true;
            }
            if (type instanceof InterfaceType) {
                for (InterfaceType o : ct.allInterfaces()) {
                    if (o.equals(type))
                        return true;
                }
            }
        }
        if (subtype instanceof InterfaceType) {
            InterfaceType it = (InterfaceType) subtype;
            if (type instanceof InterfaceType) {
                for (InterfaceType o : allInterfaces(it)) {
                    if (o.equals(type))
                        return true;
                }
            }
            if (type.name().equals("java.lang.Object"))
                return true;    // only case where an interface is assignable to a class
        }
        if (subtype instanceof ArrayType) {
            if (type instanceof ArrayType)
                return isAssignableFrom(((ArrayType)type).componentType(), ((ArrayType)subtype).componentType());
            if (type.name().equals("java.lang.Object"))
                return true;    // only case where an array is assignable to a class
        }

        return false;
    }

    public static Set<InterfaceType> allInterfaces(InterfaceType it) {
        Set<InterfaceType> visited = new HashSet<InterfaceType>();
        Stack<InterfaceType> q = new Stack<InterfaceType>();
        q.push(it);
        while (!q.isEmpty()) {
            it = q.pop();
            if (visited.add(it))
                q.addAll(it.superinterfaces());
        }
        return visited;
    }

    /**
     * Static method invocation from {@link ClassType}.
     */
    public static Object methodMissing(ClassType c, String name, Object... args) throws InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        name = unescape(name);

        if(LOGGER.isLoggable(FINE))
            LOGGER.fine("Invoking "+name+" on "+c+" with "+ Arrays.asList(args));

        List<Value> arguments = Variable.wrapList(c.virtualMachine(), args);
        return Variable.unwrap(c.invokeMethod( VM.current().getCurrentThread(),
                chooseMethod(c,name,arguments,true), arguments, 0));
    }

    /**
     * Static field retrieval.
     */
    public static Object propertyMissing(ClassType c, String name) throws InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        return lvalue(c, name).getUnwrapped();
    }

    /**
     * Static field assignment.
     */
    public static void propertyMissing(ClassType c, String name, Object value) throws InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        lvalue(c,name).set(value);
    }

    private static Variable lvalue(ClassType c, String name) {
        name = unescape(name);
        Field f = c.fieldByName(name);
        if (f==null)        throw new MissingPropertyException("No such property '"+name+"' on "+c.name());
        return Variable.fromField(c,f);
    }

    /**
     * Decode escapes of the form '@name'.
     *
     * This escape allows invocation of methods like hashCode/equals that collide with what the proxy implements.
     */
    private static String unescape(String name) {
        if (name.startsWith("@"))       name=name.substring(1);
        return name;
    }

    /**
     * Override the default behavior of {@link ObjectReference#toString()} and instead call the remote
     * {@link Object#toString()} method.
     */
    public static String toString(ObjectReference ref) throws InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        return methodMissing(ref,"toString").toString();
    }

    public static void dumpThread(ThreadReference tr, PrintStream out) throws IncompatibleThreadStateException {
        dumpThread(tr,new PrintWriter(out,true));
    }

    public static String getStatusMessage(ThreadReference tr) {
        int st = tr.status();
        switch (st) {
        case THREAD_STATUS_UNKNOWN:     return "UNKNOWN";
        case THREAD_STATUS_ZOMBIE:      return "ZOMBIE";
        case THREAD_STATUS_RUNNING:     return "RUNNING";
        case THREAD_STATUS_SLEEPING:    return "SLEEPING";
        case THREAD_STATUS_MONITOR:     return "MONITOR";
        case THREAD_STATUS_WAIT:        return "WAIT";
        case THREAD_STATUS_NOT_STARTED: return "NOT_STARTED";
        default:                        return "STATE"+String.valueOf(st);
        }
    }

    /**
     * Dumps the current thread stack
     */
    public static void dumpThread(ThreadReference tr, PrintWriter out) throws IncompatibleThreadStateException {
        out.printf("\"%s\" %s\n", tr.name(), getStatusMessage(tr));
        int c = tr.frameCount();
        for (int i=0; i<c; i++) {
            Location l = tr.frame(i).location();
            out.printf("\tat %s.%s", l.declaringType().name(), l.method().name());
            try {
                String n = l.sourceName();
                int line = l.lineNumber();
                if (line<0)
                    out.println("(Native Method)");
                else
                    out.printf("(%s:%s)\n", n, line);
            } catch (AbsentInformationException e) {
                out.println();
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(JDICategory.class.getName());

    /**
     * Primitive type to boxed type mapping
     */
    private static final Map<String,String> primitive2box = new HashMap<String, String>();

    private static void primitive2box(Class primitive, Class box) {
        primitive2box.put(primitive.getName(),box.getName());
    }

    static {
        primitive2box(boolean.class,Boolean.class);
        primitive2box(char.class,Character.class);
        primitive2box(short.class,Short.class);
        primitive2box(int.class,Integer.class);
        primitive2box(long.class,Long.class);
        primitive2box(float.class,Float.class);
        primitive2box(double.class,Double.class);

        for (final java.lang.reflect.Method m : JDICategory.class.getMethods()) {
            if (!m.getName().equals("methodMissing") && !m.getName().equals("propertyMissing"))
                continue;

            Class<?>[] pt = m.getParameterTypes();
            Class target = pt[0];
            final Class[] cpt = new Class[pt.length-1];
            System.arraycopy(pt,1,cpt,0,pt.length-1);
            if (cpt.length==2)  cpt[1]=Object.class; // methodMissing needs to return [String,Object] not [String,Object[]]

            MetaClass mc = DefaultGroovyMethods.getMetaClass(target);
            GroovyObject mmc = (GroovyObject) mc;

            mmc.setProperty(m.getName(),new Closure(null) {
                public Object call(Object[] args) {
                    try {
                        if (args.length==2)
                            return m.invoke(null, getDelegate(),args[0],args[1]);
                        else
                            return m.invoke(null, getDelegate(),args[0]);
                    } catch (IllegalAccessException e) {
                        throw new IllegalAccessError(m.getName());
                    } catch (InvocationTargetException e) {
                        Throwable t = e.getTargetException();
                        if (t instanceof RuntimeException)
                            throw (RuntimeException) t;
                        if (t instanceof Error)
                            throw (Error) t;
                        throw new org.kohsuke.youdebug.InvocationException(t);
                    }
                }
                public Class[] getParameterTypes() {
                    return cpt;
                }
            });
        }
    }
}
