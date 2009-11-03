package org.kohsuke.autodbg;

import com.sun.jdi.event.Event;

/**
 * @author Kohsuke Kawaguchi
 */
public interface EventHandler<T extends Event> {
    void on(T event);
}
