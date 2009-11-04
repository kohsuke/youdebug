package org.kohsuke.youdebug;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequest;

import java.util.List;

/**
 * Multiple {@link EventRequest} bundled into one for single point of control.
 *
 * @author Kohsuke Kawaguchi
 */
public class BundledEventRequest<E extends EventRequest> implements EventRequest {

    protected final List<E> requests;

    /*package*/ BundledEventRequest(List<E> requests) {
        this.requests = requests;
    }

    public boolean isEnabled() {
        return one().isEnabled();
    }

    public void setEnabled(boolean val) {
        for (E r : requests)
            r.setEnabled(val);
    }

    public void enable() {
        setEnabled(true);
    }

    public void disable() {
        setEnabled(false);
    }

    public void addCountFilter(int count) {
        for (E r : requests)
            r.addCountFilter(count);
    }

    public void setSuspendPolicy(int policy) {
        for (E r : requests)
            r.setSuspendPolicy(policy);
    }

    public int suspendPolicy() {
        return one().suspendPolicy();
    }

    public void putProperty(Object key, Object value) {
        one().putProperty(key,value);
    }

    public Object getProperty(Object key) {
        return one().getProperty(key);
    }

    public VirtualMachine virtualMachine() {
        return one().virtualMachine();
    }

    protected E one() {
        return requests.get(0);
    }
}
