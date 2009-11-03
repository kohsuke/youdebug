package org.kohsuke.autodbg;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Method;
import com.sun.jdi.MonitorInfo;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

import java.util.List;
import java.util.Map;

/**
 * Delegation to another {@link ThreadReference}.
 *
 * @author Kohsuke Kawaguchi
 */
class ThreadReferenceFilter implements ThreadReference {
    final ThreadReference ref;

    public ThreadReferenceFilter(ThreadReference ref) {
        this.ref = ref;
    }

    public String name() {
        return ref.name();
    }

    public void suspend() {
        ref.suspend();
    }

    public void resume() {
        ref.resume();
    }

    public int suspendCount() {
        return ref.suspendCount();
    }

    public void stop(ObjectReference throwable) throws InvalidTypeException {
        ref.stop(throwable);
    }

    public void interrupt() {
        ref.interrupt();
    }

    public int status() {
        return ref.status();
    }

    public boolean isSuspended() {
        return ref.isSuspended();
    }

    public boolean isAtBreakpoint() {
        return ref.isAtBreakpoint();
    }

    public ThreadGroupReference threadGroup() {
        return ref.threadGroup();
    }

    public int frameCount() throws IncompatibleThreadStateException {
        return ref.frameCount();
    }

    public List<StackFrame> frames() throws IncompatibleThreadStateException {
        return ref.frames();
    }

    public StackFrame frame(int index) throws IncompatibleThreadStateException {
        return ref.frame(index);
    }

    public List<StackFrame> frames(int start, int length) throws IncompatibleThreadStateException {
        return ref.frames(start, length);
    }

    public List<ObjectReference> ownedMonitors() throws IncompatibleThreadStateException {
        return ref.ownedMonitors();
    }

    public List<MonitorInfo> ownedMonitorsAndFrames() throws IncompatibleThreadStateException {
        return ref.ownedMonitorsAndFrames();
    }

    public ObjectReference currentContendedMonitor() throws IncompatibleThreadStateException {
        return ref.currentContendedMonitor();
    }

    public void popFrames(StackFrame frame) throws IncompatibleThreadStateException {
        ref.popFrames(frame);
    }

    public void forceEarlyReturn(Value value) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        ref.forceEarlyReturn(value);
    }

    public ReferenceType referenceType() {
        return ref.referenceType();
    }

    public Value getValue(Field sig) {
        return ref.getValue(sig);
    }

    public Map<Field, Value> getValues(List<? extends Field> fields) {
        return ref.getValues(fields);
    }

    public void setValue(Field field, Value value) throws InvalidTypeException, ClassNotLoadedException {
        ref.setValue(field, value);
    }

    public Value invokeMethod(ThreadReference thread, Method method, List<? extends Value> arguments, int options) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException {
        return ref.invokeMethod(thread, method, arguments, options);
    }

    public void disableCollection() {
        ref.disableCollection();
    }

    public void enableCollection() {
        ref.enableCollection();
    }

    public boolean isCollected() {
        return ref.isCollected();
    }

    public long uniqueID() {
        return ref.uniqueID();
    }

    public List<ThreadReference> waitingThreads() throws IncompatibleThreadStateException {
        return ref.waitingThreads();
    }

    public ThreadReference owningThread() throws IncompatibleThreadStateException {
        return ref.owningThread();
    }

    public int entryCount() throws IncompatibleThreadStateException {
        return ref.entryCount();
    }

    public List<ObjectReference> referringObjects(long maxReferrers) {
        return ref.referringObjects(maxReferrers);
    }

    public boolean equals(Object obj) {
        return ref.equals(obj);
    }

    public int hashCode() {
        return ref.hashCode();
    }

    public Type type() {
        return ref.type();
    }

    public VirtualMachine virtualMachine() {
        return ref.virtualMachine();
    }

    public String toString() {
        return ref.toString();
    }
}
