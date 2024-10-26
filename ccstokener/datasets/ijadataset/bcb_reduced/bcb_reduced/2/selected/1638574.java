package org.pandcorps.core;

import java.io.*;
import java.net.*;

public final class Iotil {

    private Iotil() {
        throw new Error();
    }

    public static final InputStream getInputStream(final String location) {
        final File f = new File(location);
        if (f.exists()) {
            try {
                return new FileInputStream(f);
            } catch (final FileNotFoundException e) {
                throw new Error(e);
            }
        }
        URL url = Iotil.class.getClassLoader().getResource(location);
        try {
            if (url == null) {
                url = new URL(location);
            }
            return url.openStream();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final void close(final InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (final Exception e) {
                handleCloseException(e);
            }
        }
    }

    public static final void close(final OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (final Exception e) {
                handleCloseException(e);
            }
        }
    }

    public static final void close(final Reader in) {
        if (in != null) {
            try {
                in.close();
            } catch (final Exception e) {
                handleCloseException(e);
            }
        }
    }

    public static final void close(final Writer out) {
        if (out != null) {
            try {
                out.close();
            } catch (final Exception e) {
                handleCloseException(e);
            }
        }
    }

    private static final void handleCloseException(final Exception e) {
        throw Pantil.toRuntimeException(e);
    }

    public static final BufferedInputStream getBufferedInputStream(final InputStream in) {
        return in instanceof BufferedInputStream ? (BufferedInputStream) in : new BufferedInputStream(in);
    }

    public static final BufferedReader getBufferedReader(final Reader in) {
        return in instanceof BufferedReader ? (BufferedReader) in : new BufferedReader(in);
    }

    public static final void copy(final InputStream in, final OutputStream out) throws IOException {
        final int size = 1024;
        final byte[] buf = new byte[size];
        while (true) {
            final int len = in.read(buf);
            if (len < 0) {
                break;
            }
            out.write(buf, 0, len);
        }
    }
}
