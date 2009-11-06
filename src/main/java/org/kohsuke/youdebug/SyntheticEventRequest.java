package org.kohsuke.youdebug;

import com.sun.jdi.request.EventRequest;

/**
 * {@link EventRequest} that are defined in YouDebug, not by JDI.
 *
 * @author Kohsuke Kawaguchi
 */
interface SyntheticEventRequest extends EventRequest {
    void delete();
}
