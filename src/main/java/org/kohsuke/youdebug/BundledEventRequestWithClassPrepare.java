package org.kohsuke.youdebug;

import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ClassPrepareRequest;

import java.util.List;

/**
 * {@link BundledEventRequest} with an associated {@link ClassPrepareRequest} on the side.
 * <p>
 * This is used for breakpoints that refer a class name by String, to detect additional loading of classes
 * of the given name.
 *
 * @author Kohsuke Kawaguchi
 */
class BundledEventRequestWithClassPrepare<E extends EventRequest> extends BundledEventRequest<E> {
    private final ClassPrepareRequest req;

    BundledEventRequestWithClassPrepare(ClassPrepareRequest req, List<E> requests) {
        super(requests);
        this.req = req;
    }

    @Override
    public void setEnabled(boolean val) {
        super.setEnabled(val);
        if (req!=null)
            req.setEnabled(val);
    }
    
    /*package*/ void add(E e) {
        requests.add(e);
    }

    @Override
    public void delete() {
        super.delete();
        JDICategory.delete(req);
    }
}
