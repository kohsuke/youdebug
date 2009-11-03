package org.kohsuke.autodbg;

/**
 * Signals an error in invoking a method.
 *
 * @author Kohsuke Kawaguchi
 */
public class InvocationException extends RuntimeException {
    public InvocationException(String message) {
        super(message);
    }

    public InvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvocationException(Throwable cause) {
        super(cause);
    }
}
