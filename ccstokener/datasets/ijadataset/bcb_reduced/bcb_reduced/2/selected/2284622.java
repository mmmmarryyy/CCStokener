package org.specrunner.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import org.specrunner.source.resource.ResourceException;

public final class UtilIO {

    public static final int BUFFER_SIZE = 1024;

    private UtilIO() {
    }

    public static void writeAllTo(List<URL> files, OutputStream out) throws ResourceException {
        InputStream[] ins = null;
        int i = 0;
        try {
            ins = getInputStreams(files);
            for (InputStream in : ins) {
                writeTo(in, out);
                in.close();
                i++;
            }
        } catch (IOException e) {
            if (ins != null) {
                for (int j = 0; j < i; j++) {
                    try {
                        if (UtilLog.LOG.isDebugEnabled()) {
                            UtilLog.LOG.debug("Closing " + ins[j]);
                        }
                        ins[j].close();
                    } catch (IOException e1) {
                        if (UtilLog.LOG.isDebugEnabled()) {
                            UtilLog.LOG.debug(e1.getMessage(), e1);
                        }
                    }
                }
            }
            if (UtilLog.LOG.isDebugEnabled()) {
                UtilLog.LOG.debug(e.getMessage(), e);
            }
        }
    }

    public static InputStream[] getInputStreams(List<URL> files) throws ResourceException {
        InputStream[] result = new InputStream[files.size()];
        int i = 0;
        try {
            for (URL url : files) {
                result[i++] = url.openStream();
            }
        } catch (IOException e) {
            for (int j = 0; j < i; j++) {
                try {
                    if (UtilLog.LOG.isDebugEnabled()) {
                        UtilLog.LOG.debug("Closing " + files.get(j));
                    }
                    result[j].close();
                } catch (IOException e1) {
                    if (UtilLog.LOG.isDebugEnabled()) {
                        UtilLog.LOG.debug(e1.getMessage(), e1);
                    }
                }
            }
            if (UtilLog.LOG.isDebugEnabled()) {
                UtilLog.LOG.debug(e.getMessage(), e);
            }
            throw new ResourceException(e);
        }
        return result;
    }

    public static void writeTo(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int size = 0;
        while ((size = in.read(buffer)) > 0) {
            out.write(buffer, 0, size);
        }
        out.flush();
    }

    public static void writeToClose(InputStream in, OutputStream out) throws IOException {
        try {
            writeTo(in, out);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    if (UtilLog.LOG.isDebugEnabled()) {
                        UtilLog.LOG.debug("Closing " + in);
                    }
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    if (UtilLog.LOG.isDebugEnabled()) {
                        UtilLog.LOG.debug("Closing " + out);
                    }
                }
            }
        }
    }
}
