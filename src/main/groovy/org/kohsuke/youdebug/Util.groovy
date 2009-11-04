package org.kohsuke.youdebug

import com.sun.jdi.ObjectReference

/**
 *
 *
 * @author Kohsuke Kawaguchi
 */

public class Util {
    /**
     * Dumps the exception
     */
    public static void dumpStackTrace (ObjectReference exp, out) {
        def frames = exp.getStackTrace();
        out.println(exp.toString());
        int len = frames.length();
        for ( int i=0; i<len; i++ ) {
            def f = frames[i];
            out.println("\tat ${f.getClassName()}.${f.getMethodName()}(${f.getFileName()}:${f.getLineNumber()})")
        }
    }

}