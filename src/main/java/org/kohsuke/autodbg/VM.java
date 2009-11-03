package org.kohsuke.autodbg;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.GroovyCategorySupport;

import java.io.Closeable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class VM implements Closeable {
    private final VirtualMachine vm;
    private final EventQueue q;
    private final EventRequestManager req;
    private final ThreadList threads;
    private Event currentEvent;

    public VM(VirtualMachine vm) {
        this.vm = vm;
        q = vm.eventQueue();
        req = vm.eventRequestManager();
        threads = new ThreadList(vm);
    }

    /**
     * Gets the list of all threads.
     *
     * @return
     *      this list is live and concurrecy safe.
     */
    public ThreadList getThreads() {
        return threads;
    }

    /**
     * Returns the thread that raised the current event.
     * For example, if the caller is a closure for a break point, this method
     * returns the thread that hit the breakpoint.
     */
    public ThreadReference getCurrentThread() {
        if (currentEvent instanceof LocatableEvent)
            return ((LocatableEvent) currentEvent).thread();
        return null;
    }

    /**
     * Returns the current debugger event that we are dispatching.
     */
    public Event getCurrentEvent() {
        return currentEvent;
    }

    /**
     * Dispatches events received from the target JVM until the connection is {@linkplain #close() closed},
     * the remote JVM exits, or the thread ges interrupted.
     */
    public void dispatchEvents() throws InterruptedException {
        final VM old = CURRENT.get();
        CURRENT.set(this);
        try {
            while (true) {
                EventSet es = q.remove();
                for (Event e : es) {
                    currentEvent = e;
                    EventHandler h = HANDLER.get(e);
                    if (h!=null) {
                        h.on(e);
                        continue;
                    }

                    if (e instanceof ThreadStartEvent) {
                        handleThreadStart((ThreadStartEvent) e);
                        continue;
                    }
                    if (e instanceof ThreadDeathEvent) {
                        handleThreadDeath((ThreadDeathEvent) e);
                        continue;
                    }
                    if (e instanceof VMStartEvent) {
                        LOGGER.info("Application started");
                        continue;
                    }
                    if (e instanceof VMDeathEvent) {
                        LOGGER.info("Application exited");
                        return; // peacefully terminate the execution
                    }
                    if (e instanceof VMDisconnectEvent) {
                        LOGGER.info("Debug session has disconnected");
                        return; // peacefully terminate the execution
                    }

                    LOGGER.info("Unhandled event type: "+e);
                }
                currentEvent = null;
                es.resume();
            }
        } catch (VMDisconnectedException e) {
            LOGGER.log(Level.INFO, VM.this.vm.name()+" disconnected");
        } finally {
            CURRENT.set(old);
        }
    }

    public ClassPrepareRequest classPrepare(String name, final Closure c) {
        ClassPrepareRequest r = req.createClassPrepareRequest();
        r.addClassFilter(name);
        HANDLER.put(r,new EventHandler<ClassPrepareEvent>() {
            public void on(ClassPrepareEvent event) {
                c.setDelegate(event.thread());
                c.call(event.referenceType());
            }
        });
        r.enable();
        return r;
    }

    public ClassPrepareRequest classPrepare(Class clazz, final Closure c) {
        return classPrepare(clazz.getName(),c);
    }

    private void handleThreadStart(ThreadStartEvent tse) {
        threads.onThreadStart(tse);
    }

    private void handleThreadDeath(ThreadDeathEvent tde) {
        threads.onThreadEnd(tde);
    }
    
    /**
     * Sets a break point at the specified line in the specified class, and if it hits,
     * invoke the closure.
     */
    public BundledBreakpointRequest breakpoint(String className, int line, final Closure c) throws AbsentInformationException {
        ReferenceType math = ref(className);
        List<BreakpointRequest> bps = new ArrayList<BreakpointRequest>();
        for (Location loc : math.locationsOfLine(line)) {
            BreakpointRequest bp = req.createBreakpointRequest(loc);
            HANDLER.put(bp, new EventHandler<BreakpointEvent>() {
                public void on(BreakpointEvent e) {
                    c.setDelegate(new BreakpointDelegate(e));
                    c.call();
                }
            });
            bp.enable();
            bps.add(bp);
        }
        return new BundledBreakpointRequest(bps);
    }

    /**
     * Sets a break point that hits upon an exception.
     *
     * <pre>
     * exceptionBreakpoint(className) { e ->
     *    // e references the exception that is thrown
     * }
     * </pre>
     */
    public ExceptionRequest exceptionBreakpoint(ReferenceType exceptionClass, Collection<ExceptionBreakpointModifier> modifiers, final Closure c) {
        // default to all situations, since "none" isn't useful
        if (modifiers.isEmpty())    modifiers = EnumSet.allOf(ExceptionBreakpointModifier.class);

        ExceptionRequest q = req.createExceptionRequest(exceptionClass, modifiers.contains(CAUGHT), modifiers.contains(UNCAUGHT));
        HANDLER.put(q,new EventHandler<ExceptionEvent>() {
            public void on(ExceptionEvent e) {
                c.setDelegate(e.thread());
                c.call(e.exception());
            }
        });
        q.enable();
        return q;
    }

    public ExceptionRequest exceptionBreakpoint(String exceptionClassName, Collection<ExceptionBreakpointModifier> modifiers, final Closure c) {
        return exceptionBreakpoint(ref(exceptionClassName),modifiers,c);
    }

    public ExceptionRequest exceptionBreakpoint(Class exceptionClass, Collection<ExceptionBreakpointModifier> modifiers, final Closure c) {
        return exceptionBreakpoint(ref(exceptionClass),modifiers,c);
    }

    public ExceptionRequest exceptionBreakpoint(Class exceptionClass, final Closure c) {
        return exceptionBreakpoint(exceptionClass,EnumSet.allOf(ExceptionBreakpointModifier.class),c);
    }

    public ExceptionRequest exceptionBreakpoint(ReferenceType exceptionClass, final Closure c) {
        return exceptionBreakpoint(exceptionClass,EnumSet.allOf(ExceptionBreakpointModifier.class),c);
    }

    public ExceptionRequest exceptionBreakpoint(String exceptionClass, final Closure c) {
        return exceptionBreakpoint(exceptionClass,EnumSet.allOf(ExceptionBreakpointModifier.class),c);
    }

    /**
     * Resolves a class by the name.
     *
     * TODO: use the current thread and classloader to try to disambiguate in a multi-classloader situation
     */
    public ReferenceType ref(String className) {
        List<ReferenceType> r = vm.classesByName(className);
//        getCurrentThread().frame(0).location().declaringType().classLoader().
        if (r.isEmpty())        throw new IllegalArgumentException("No such class: "+className);
        return r.get(0);
    }

    /**
     * Resolves the same class on the target JVM.
     */
    public ReferenceType ref(Class c) {
        return ref(c.getName());
    }

    /**
     * Shuts down the connection.
     */
    public void close() {
        vm.dispose();
    }

    public void execute(final InputStream script) throws InterruptedException {
        try {
            // make JDICategory available by default
            GroovyCategorySupport.use(JDICategory.class, new Closure(this) {
                public Object call() {
                    try {
                        CompilerConfiguration cc = new CompilerConfiguration();
                        // cc.setScriptBaseClass();

                        Binding binding = new Binding();
                        binding.setVariable("vm",this);

                        GroovyShell groovy = new GroovyShell(binding,cc);
//                        InputStream res = getClass().getClassLoader().getResourceAsStream("register.groovy");
//                        groovy.parse(res).run();

                        groovy.parse(script).run();
                        dispatchEvents();
                        return null;
                    } catch (InterruptedException e) {
                        throw new TunnelException(e);
                    }
                }
            });
        } catch (TunnelException e) {
            throw (InterruptedException)e.getCause();
        }
    }

    /**
     * Can be called during event dispatching to obtain the current {@link VM} instance.
     */
    public static VM current() {
        return CURRENT.get();
    }

    private static final class TunnelException extends RuntimeException {
        private TunnelException(Throwable cause) {
            super(cause);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(VM.class.getName());

    /*package*/ static final Key<EventHandler> HANDLER = new Key<EventHandler>(EventHandler.class);

    public static final ExceptionBreakpointModifier CAUGHT = ExceptionBreakpointModifier.CAUGHT;
    public static final ExceptionBreakpointModifier UNCAUGHT = ExceptionBreakpointModifier.UNCAUGHT;

    private static final ThreadLocal<VM> CURRENT = new ThreadLocal<VM>();
}
