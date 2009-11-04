package org.kohsuke.youdebug;

/**
 * Indicates that the field assignment failed.
 *
 * @author Kohsuke Kawaguchi
 */
public class FailedAssignmentException extends RuntimeException {
    public FailedAssignmentException() {
    }

    public FailedAssignmentException(String message) {
        super(message);
    }

    public FailedAssignmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedAssignmentException(Throwable cause) {
        super(cause);
    }
}
