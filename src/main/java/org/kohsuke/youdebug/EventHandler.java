package org.kohsuke.youdebug;

import com.sun.jdi.event.Event;

/**
 * @author Kohsuke Kawaguchi
 */
public interface EventHandler<T extends Event> {
    void on(T event);
}
