package org.kohsuke.youdebug

import org.codehaus.groovy.control.CompilerConfiguration
import com.sun.jdi.IncompatibleThreadStateException
import com.sun.jdi.request.EventRequestManager
import com.sun.jdi.event.LocatableEvent
import com.sun.jdi.event.ThreadStartEvent
import com.sun.jdi.event.ThreadDeathEvent
import com.sun.jdi.event.VMStartEvent
import com.sun.jdi.event.VMDeathEvent
import com.sun.jdi.event.VMDisconnectEvent
import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.EventQueue
import com.sun.jdi.event.Event
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.ThreadReference
import com.sun.jdi.request.ClassPrepareRequest
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.event.EventSet
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.request.BreakpointRequest
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.request.ExceptionRequest
import com.sun.jdi.event.ExceptionEvent
import com.sun.jdi.ReferenceType
import com.sun.jdi.Location
import java.util.logging.Logger
import java.lang.management.ManagementFactory
import com.sun.management.HotSpotDiagnosticMXBean
import com.sun.jdi.request.EventRequest
import java.util.logging.Level
import com.sun.jdi.request.MethodEntryRequest
import com.sun.jdi.event.MethodEntryEvent
import com.sun.jdi.request.MethodExitRequest
import com.sun.jdi.event.MethodExitEvent

/**
 * Debugger view of a Virtual machine. 
 *
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
     * Access to the underlying JDI {@link VirtualMachine} object.
     * Use this method when you want to go down to JDI.
     *
     * @return never null.
     */
    public VirtualMachine getVirtualMachine() {
        return vm;
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
                    if (LOGGER.isLoggable(Level.FINER))
                        LOGGER.finer("Received event "+e);

                    currentEvent = e;
                    Closure h = HANDLER.get(e);
                    if (h!=null) {
                        h(e);
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
                        LOGGER.fine("Application started");
                        continue;
                    }
                    if (e instanceof VMDeathEvent) {
                        LOGGER.fine("Application exited");
                        return; // peacefully terminate the execution
                    }
                    if (e instanceof VMDisconnectEvent) {
                        LOGGER.fine("Debug session has disconnected");
                        return; // peacefully terminate the execution
                    }

                    LOGGER.info("Unhandled event type: "+e);
                }
                currentEvent = null;
                es.resume();
            }
        } catch (VMDisconnectedException e) {
            LOGGER.log(Level.INFO, "Target JVM  disconnected",e);
        } finally {
            CURRENT.set(old);
        }
    }

    /**
     * Sets a break point to be fired when a class of the specified name is loaded into the JVM.
     *
     * <p>
     * The closure will get the {@link ReferenceType} object as a parameter, which represents
     * a newly loaded class.
     *
     * <pre>
     * classPrepare("org.acme.FooBar") { t ->
     *   ...
     * }
     * </pre>
     */
    public ClassPrepareRequest classPrepare(String name, final Closure c) {
        ClassPrepareRequest r = req.createClassPrepareRequest();
        r.addClassFilter(name);
        registerHandler(r) { ClassPrepareEvent event ->
            c.setDelegate(event.thread());
            c.call(event.referenceType());
        };
        r.enable();
        return r;
    }

    /**
     * Same as {@link #classPrepare(String, Closure)} except you specify the class by using
     * a reference to the class in this JVM.
     */
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
    public BundledBreakpointRequest breakpoint(String className, int line, final Closure body) throws AbsentInformationException {
        List<BreakpointRequest> bpreqs = [];
        def r = forEachClass(className) { ReferenceType t ->
            bpreqs.add(breakpoint(t,line,body));
        }
        return new BundledBreakpointRequest(r,bpreqs);
    }

    /**
     * Same as {@link #breakpoint(String, int, Closure)} except you specify the class by using
     * a reference to the class in this JVM.
     */
    public BundledBreakpointRequest breakpoint(Class className, int line, final Closure c) throws AbsentInformationException {
        return breakpoint(className.name,line,c);
    }

    /**
     * Same as {@link #breakpoint(String, int, Closure)} except you specify the class by using
     * a remote reference {@link ReferenceType}.
     */
    public BundledBreakpointRequest breakpoint(ReferenceType type, int line, final Closure c) throws AbsentInformationException {
        List<BreakpointRequest> bps = [];
        for (Location loc : type.locationsOfLine(line)) {
            BreakpointRequest bp = req.createBreakpointRequest(loc);
            registerHandler(bp) { BreakpointEvent e ->
                c.setDelegate(e.thread());
                c.call();
            };
            bp.enable();
            bps.add(bp);
        }
        return new BundledBreakpointRequest(null,bps);
    }

    /**
     * Same as {@link #exceptionBreakpoint(String, Collection<org.kohsuke.youdebug.ExceptionBreakpointModifier>, Closure)}
     * except you specify the exception type by a remote reference of the type.
     */
    public ExceptionRequest exceptionBreakpoint(ReferenceType exceptionClass, Collection<ExceptionBreakpointModifier> modifiers, Closure c) {
        // default to all situations, since "none" isn't useful
        if (modifiers==null || modifiers.isEmpty())    modifiers = EnumSet.allOf(ExceptionBreakpointModifier.class);

        ExceptionRequest q = req.createExceptionRequest(exceptionClass, modifiers.contains(CAUGHT), modifiers.contains(UNCAUGHT));
        registerHandler(q) { ExceptionEvent e ->
            c.setDelegate(e.thread());
            c.call(e.exception());
        };
        q.enable();
        return q;
    }

    /**
     * Sets a break point that hits upon an exception.
     *
     * <pre>
     * exceptionBreakpoint(className) { e ->
     *    // e references the exception that is thrown
     * }
     * </pre>
     *
     * @param modifiers
     *      Optionally specify the kind of exceptions (caught or uncaught) that you want to catch.
     *      If this is null or empty, both kinds of exceptions are caught.
     */
    public ExceptionRequest exceptionBreakpoint(String className, Collection<ExceptionBreakpointModifier> modifiers, Closure c) {
        List<ExceptionRequest> bpreqs = [];
        def r = forEachClass(className) { ReferenceType t ->
            bpreqs.add(exceptionBreakpoint(t,modifiers,c));
        }
        return new BundledExceptionRequest(r,bpreqs);
    }

    /**
     * Same as {@link #exceptionBreakpoint(String, Collection<org.kohsuke.youdebug.ExceptionBreakpointModifier>, Closure)}
     * except you specify the exception type by a local class of the same name.
     */
    public ExceptionRequest exceptionBreakpoint(Class exceptionClass, Collection<ExceptionBreakpointModifier> modifiers, Closure c) {
        return exceptionBreakpoint(exceptionClass.name,modifiers,c);
    }

    /**
     * Short for {@code exceptionBreakpoint(exceptionClass,null,c)}, to get notified for both kinds of exceptions.
     */
    public ExceptionRequest exceptionBreakpoint(Class exceptionClass, Closure c) {
        return exceptionBreakpoint(exceptionClass,EnumSet.allOf(ExceptionBreakpointModifier.class),c);
    }

    /**
     * Short for {@code exceptionBreakpoint(exceptionClass,null,c)}, to get notified for both kinds of exceptions.
     */
    public ExceptionRequest exceptionBreakpoint(ReferenceType exceptionClass, Closure c) {
        return exceptionBreakpoint(exceptionClass,EnumSet.allOf(ExceptionBreakpointModifier.class),c);
    }

    /**
     * Short for {@code exceptionBreakpoint(exceptionClass,null,c)}, to get notified for both kinds of exceptions.
     */
    public ExceptionRequest exceptionBreakpoint(String exceptionClass, Closure c) {
        return exceptionBreakpoint(exceptionClass,EnumSet.allOf(ExceptionBreakpointModifier.class),c);
    }

    public MethodEntryRequest methodEntryBreakpoint(ReferenceType type, String methodName, Closure body) {
        MethodEntryRequest q = createMethodEntryRequest(methodName,body);
        q.addClassFilter(type);
        q.enable();
        return q;
    }

    public MethodEntryRequest methodEntryBreakpoint(String type, String methodName, Closure body) {
        MethodEntryRequest q = createMethodEntryRequest(methodName,body);
        q.addClassFilter(type);
        q.enable();
        return q;
    }

    private MethodEntryRequest createMethodEntryRequest(String methodName, Closure body) {
        MethodEntryRequest q = req.createMethodEntryRequest()
        registerHandler(q) { MethodEntryEvent e ->
            if (e.method().name() == methodName) {
                body.setDelegate(e.thread());
                body.call();
            }
        }
        return q;
    }

    public MethodExitRequest methodExitBreakpoint(ReferenceType type, String methodName, Closure body) {
        MethodExitRequest q = createMethodExitRequest(methodName,body);
        q.addClassFilter(type);
        q.enable();
        return q;
    }

    public MethodExitRequest methodExitBreakpoint(String type, String methodName, Closure body) {
        MethodExitRequest q = createMethodExitRequest(methodName,body);
        q.addClassFilter(type);
        q.enable();
        return q;
    }

    private MethodExitRequest createMethodExitRequest(String methodName, Closure body) {
        MethodExitRequest q = req.createMethodExitRequest()
        registerHandler(q) { MethodExitEvent e ->
            if (e.method().name() == methodName) {
                body.setDelegate(e.thread());
                body.call(e.returnValue());
            }
        }
        return q;
    }

    private void registerHandler(EventRequest q, Closure body) {
        HANDLER[q] = body;
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
     * Short hand for {@link #ref(Class)
     */
    public ReferenceType _(Class c) {
        return ref(c);
    }

    /**
     * Short hand for {@link #ref(String) 
     */
    public ReferenceType _(String c) {
        return ref(c);
    }

    public Object _new(Class c, Object... args) {
        return ref(c).methodMissing("new",args);
    }

    public Object _new(String c, Object... args) {
        return ref(c).methodMissing("new",args);
    }

    /**
     * Instead of just resolving an existing class, load the specified class in the target JVM
     * and returns its reference.
     */
    public ReferenceType loadClass(String c) {
        try {
            return ref(c)
        } catch (IllegalArgumentException e) {
            // force load
            def cl = currentThread.frame(0).location().declaringType().classLoader();
            if (cl==null)   cl=ref(ClassLoader).getSystemClassLoader();
            def clazz = cl.loadClass(c, true)
            clazz.getMethods(); // force preparation
            return clazz.reflectedType();
        };
    }

    public ReferenceType loadClass(Class c) {
        return loadClass(c.name);
    }

    /**
     * Executes the given closure for each class of the given name.
     *
     * <p>
     * The closure gets a {@link ReferenceType} object as a parameter.
     * It'll be invoked immediately for all the loaded class (of the given name),
     * and then whenever a new class of the given name is loaded, the closure will be re-invoked.
     *
     * @return
     *      A {@link EventRequest} that can be used to cancel future invcocations.
     */
    public ClassPrepareRequest forEachClass(String className, Closure body) {
        // for classes that are already loaded
        for (ReferenceType r : vm.classesByName(className))
            body.call(r);
        // for classes to be loaded in the future
        return classPrepare(className,body);
    }

    /**
     * Shuts down the connection.
     */
    public void close() {
        vm.dispose();
    }

    private VM _this() {
        return this;
    }

    public void execute(InputStream script) throws InterruptedException {
        execute(new GroovyCodeSource(script,null,null));
    }

    public void execute(File script) throws InterruptedException {
        execute(new GroovyCodeSource(script));
    }

    public void execute(URL script) throws InterruptedException {
        execute(new GroovyCodeSource(script));
    }

    public void execute(GroovyCodeSource script) throws InterruptedException {
        execute(script,Collections.emptyMap());
    }



    /**
    * Executes the given YouDebug script.
     *
     * @param script
     *      The script to execute.
     * @param variables
     *      Additional variables to be exposed to the script. This is useful for passing information
     *      between the script and the calling program.
     */
    public void execute(GroovyCodeSource script, Map<String,?> variables) throws InterruptedException {
        // this makes JDWP retrieves a global 'versionInfo' from the target JVM. Do this while the JVM is suspended,
        // or else when we do this later, the VM runs a bit, and we can miss events
        vm.version();

        // make JDICategory available by default
        use(JDICategory) {
            CompilerConfiguration cc = new CompilerConfiguration();
            // cc.setScriptBaseClass();

            Binding binding = new Binding();
            binding.setVariable("vm",_this());
            variables?.each { k,v -> binding.setVariable(k,v) }

            GroovyShell groovy = new GroovyShell(binding,cc);

            groovy.parse(script).run();
            dispatchEvents();
        }
    }

    public void dumpAllThreads() throws IncompatibleThreadStateException {
        dumpAllThreads(System.out);
    }

    public void dumpAllThreads(PrintStream out) throws IncompatibleThreadStateException {
        dumpAllThreads(new PrintWriter(out,true));
    }

    public void dumpAllThreads(PrintWriter out) throws IncompatibleThreadStateException {
        for (ThreadReference r : vm.allThreads()) {
            JDICategory.dumpThread(r,out);
        }

    }

    public void suspend() {
        vm.suspend();
    }

    public void resume() {
        vm.resume();
    }

    /**
     * Executes the given closure by suspending the VM. At the exit of the scope the VM will be resumed.
     */
    public void withFrozenWorld(Closure c) {
        vm.suspend();
        try {
            c.call();
        } finally {
            vm.resume();
        }
    }

    public void dumpHeap() {
        def mf = loadClass(ManagementFactory.class)
        def server = mf.getPlatformMBeanServer();
        def bean = mf.newPlatformMXBeanProxy(server,"com.sun.management:type=HotSpotDiagnostic", loadClass(HotSpotDiagnosticMXBean.class));
        new File("/tmp/heapdump").delete();
        bean.dumpHeap("/tmp/heapdump",true);
    }

    /**
     * Can be called during event dispatching to obtain the current {@link VM} instance.
     */
    public static VM current() {
        return CURRENT.get();
    }

    private static final Logger LOGGER = Logger.getLogger(VM.class.getName());

    /*package*/ static final Key<Closure> HANDLER = new Key<Closure>(Closure.class);

    public static final ExceptionBreakpointModifier CAUGHT = ExceptionBreakpointModifier.CAUGHT;
    public static final ExceptionBreakpointModifier UNCAUGHT = ExceptionBreakpointModifier.UNCAUGHT;

    private static final ThreadLocal<VM> CURRENT = new ThreadLocal<VM>();
}
