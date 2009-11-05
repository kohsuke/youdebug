package org.kohsuke.youdebug

import com.sun.jdi.request.ExceptionRequest
import com.sun.jdi.request.ClassPrepareRequest
import com.sun.jdi.ReferenceType
import com.sun.jdi.ThreadReference
import com.sun.jdi.ObjectReference

/**
 *
 *
 * @author Kohsuke Kawaguchi
 */
public class BundledExceptionRequest extends BundledEventRequestWithClassPrepare<ExceptionRequest> implements ExceptionRequest {

    /*package*/ BundledExceptionRequest(ClassPrepareRequest req, List<ExceptionRequest> requests) {
        super(req, requests);
    }

    /*package*/ BundledExceptionRequest(List<ExceptionRequest> requests) {
        this(null,requests);
    }

    public ReferenceType exception() {
        return null;
    }

    public List<ReferenceType> exceptions() {
        List<ReferenceType> r = new ArrayList<ReferenceType>();
        for (ExceptionRequest request : requests)
            r.add(request.exception());
        return r;
    }

    public boolean notifyCaught() {
        return one().notifyCaught();
    }

    public boolean notifyUncaught() {
        return one().notifyUncaught();
    }

    public void addThreadFilter(ThreadReference thread) {
        requests*.addThreadFilter(thread);
    }

    public void addClassFilter(ReferenceType refType) {
        requests*.addClassFilter(refType);
    }

    public void addClassFilter(String classPattern) {
        requests*.addClassFilter(classPattern);
    }

    public void addClassExclusionFilter(String classPattern) {
        requests*.addClassExclusionFilter(classPattern);
    }

    public void addInstanceFilter(ObjectReference instance) {
        requests*.addInstanceFilter(instance);
    }
}
