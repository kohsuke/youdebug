package org.kohsuke.youdebug;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class StreamCopyThread extends Thread {
    private final InputStream in;
    private final OutputStream out;

    public StreamCopyThread(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
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
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
