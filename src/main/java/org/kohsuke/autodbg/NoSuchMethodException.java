package org.kohsuke.autodbg;

/**
 * @author Kohsuke Kawaguchi
 */
public class NoSuchMethodException extends RuntimeException {
    public NoSuchMethodException(String message) {
        super(message);
    }
}
