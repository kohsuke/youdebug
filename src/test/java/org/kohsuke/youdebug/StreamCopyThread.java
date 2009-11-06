package org.kohsuke.youdebug;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class StreamCopyThread extends Thread {
    private final InputStream in;
    private final OutputStream out;
    private final boolean closeOut;

    public StreamCopyThread(InputStream in, OutputStream out, boolean closeOut) {
        this.in = in;
        this.out = out;
        this.closeOut = closeOut;
    }

    public StreamCopyThread(InputStream in, OutputStream out) {
        this(in,out,false);
    }

    @Override
    public void run() {
        try {
            try {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0)
                    out.write(buf, 0, len);
            } finally {
                // it doesn't make sense not to close InputStream that's already EOF-ed,
                // so there's no 'closeIn' flag.
                in.close();
                if(closeOut)
                    out.close();
            }
        } catch (IOException e) {
            // TODO: what to do?
        }
    }
}
