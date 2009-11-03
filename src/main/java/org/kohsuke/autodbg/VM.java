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
    private final Thread eventThread;

    public VM(VirtualMachine vm) {
        this.vm = vm;
        q = vm.eventQueue();
        req = vm.eventRequestManager();

        eventThread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        EventSet es = q.remove();
                        for (Event e : es) {
                            EventHandler h = HANDLER.get(e);
                            if (h!=null) {
                                h.on(e);
                                continue;
                            }

                            if (e instanceof VMDisconnectEvent) {
                                return; // peacefully terminate the execution
                            }
                            
                            LOGGER.info("Unhandled event type: "+e);
                        }
                        es.resume();
                    }
                } catch (VMDisconnectedException e) {
                    LOGGER.log(Level.INFO, VM.this.vm.name()+" disconnected");
                } catch (InterruptedException e) {
                    LOGGER.log(Level.FINE, "Terminating the event dispatch thread",e);
                }
            }
        }, vm.name()+" JDI event dispatch thread");
        eventThread.start();
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
        eventThread.interrupt();
        vm.dispose();
    }

    public void execute(InputStream script) {
        CompilerConfiguration cc = new CompilerConfiguration();
        // cc.setScriptBaseClass();

        Binding binding = new Binding();
        binding.setVariable("vm",this);

        GroovyShell groovy = new GroovyShell(binding,cc);

        groovy.parse(script).run();
    }

    private static final Logger LOGGER = Logger.getLogger(VM.class.getName());

    public static final Key<EventHandler> HANDLER = new Key<EventHandler>(EventHandler.class);
}
