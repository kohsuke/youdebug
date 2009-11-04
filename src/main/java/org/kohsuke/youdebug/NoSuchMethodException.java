package org.kohsuke.youdebug;

/**
 * @author Kohsuke Kawaguchi
 */
public class NoSuchMethodException extends RuntimeException {
    public NoSuchMethodException(String message) {
        super(message);
    }
}
