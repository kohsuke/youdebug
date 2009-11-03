package org.kohsuke.autodbg;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;
import groovy.lang.Closure;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.InputStream;
import java.io.Closeable;

import org.codehaus.groovy.control.CompilerConfiguration;

/**
 * @author Kohsuke Kawaguchi
 */
public class VM implements Closeable {
    private final VirtualMachine vm;
    private final EventQueue q;
    private final EventRequestManager req;
    private final ThreadList threads;

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
     * Dispatches events received from the target JVM until the connection is {@linkplain #close() closed},
     * the remote JVM exits, or the thread ges interrupted.
     */
    public void dispatchEvents() throws InterruptedException {
        try {
            while (true) {
                EventSet es = q.remove();
                for (Event e : es) {
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
                es.resume();
            }
        } catch (VMDisconnectedException e) {
            LOGGER.log(Level.INFO, VM.this.vm.name()+" disconnected");
        }
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
    public void breakpoint(String className, int line, final Closure c) throws AbsentInformationException {
        ReferenceType math = $(className);
        for (Location loc : math.locationsOfLine(line)) {
            BreakpointRequest bp = req.createBreakpointRequest(loc);
            HANDLER.put(bp,new EventHandler<BreakpointEvent>() {
                public void on(BreakpointEvent e) {
                    c.call();
                    vm.resume();
                }
            });
            bp.enable();
        }
    }

    public ReferenceType $(String className) {
        for (ReferenceType r : vm.allClasses()) {
            if (r.name().equals(className))
                return r;
        }
        return null;
    }

    /**
     * Shuts down the connection.
     */
    public void close() {
        vm.dispose();
    }

    public void execute(InputStream script) throws InterruptedException {
        CompilerConfiguration cc = new CompilerConfiguration();
        // cc.setScriptBaseClass();

        Binding binding = new Binding();
        binding.setVariable("vm",this);

        GroovyShell groovy = new GroovyShell(binding,cc);

        groovy.parse(script).run();
        dispatchEvents();
    }

    private static final Logger LOGGER = Logger.getLogger(VM.class.getName());

    public static final Key<EventHandler> HANDLER = new Key<EventHandler>(EventHandler.class);
}
