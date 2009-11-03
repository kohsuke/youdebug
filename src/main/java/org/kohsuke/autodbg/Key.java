package org.kohsuke.autodbg;

import com.sun.jdi.request.EventRequest;
import com.sun.jdi.event.Event;

/**
 * @author Kohsuke Kawaguchi
 */
public class Key<T> {
    private final Class<T> type;

    public Key(Class<T> type) {
        this.type = type;
    }

    public T get(EventRequest e) {
        return type.cast(e.getProperty(this));
    }

    public T get(Event e) {
        EventRequest r = e.request();
        return r!=null ? get(r) : null;
    }

    public void put(EventRequest e, T value) {
        e.putProperty(this,value);
    }
}
