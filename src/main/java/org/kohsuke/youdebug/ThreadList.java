package org.kohsuke.youdebug;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Maintains a list of {@link ThreadReference}s that are in the target VM.
 *
 * @author Kohsuke Kawaguchi
 */
public class ThreadList extends AbstractSet<ThreadReference> {
    private final Set<ThreadReference> threads = new CopyOnWriteArraySet<ThreadReference>();
    private final VirtualMachine vm;

    /*package*/ ThreadList(VirtualMachine vm) {
        this.vm = vm;
        threads.addAll(vm.allThreads());
    }

    /*package*/ void onThreadStart(ThreadStartEvent e) {
        threads.add(e.thread());
    }

    /*package*/ void onThreadEnd(ThreadDeathEvent e) {
        threads.remove(e.thread());
    }

    public int size() {
        return threads.size();
    }

    public boolean isEmpty() {
        return threads.isEmpty();
    }

    public Iterator<ThreadReference> iterator() {
        return threads.iterator();
    }

    /**
     * Gets a thread by its {@link Thread#getName()}.
     */
    public ThreadReference byName(String name) {
        for (ThreadReference t : threads)
            if (t.name().equals(name))
                return t;
        return null;
    }

    /**
     * {@code threads[name]} for Groovy
     */
    public ThreadReference getAt(String name) {
        return byName(name);
    }
}
