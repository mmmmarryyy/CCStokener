package org.jasen.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.jasen.error.JasenException;
import org.jasen.interfaces.URLReader;
import org.jasen.io.NonBlockingStreamReader;

/**
 * <P>
 * 	Extracts the content from a remote web server for the purposes of analysis.
 * </P>
 * @author Jason Polites
 */
public class StandardURLReader implements URLReader {

    private int readBufferSize = 2048;

    private long readTimeout = 5000L;

    public String readURL(URL url) throws JasenException {
        OutputStream out = new ByteArrayOutputStream();
        InputStream in = null;
        String html = null;
        NonBlockingStreamReader reader = null;
        try {
            in = url.openStream();
            reader = new NonBlockingStreamReader();
            reader.read(in, out, readBufferSize, readTimeout, null);
            html = new String(((ByteArrayOutputStream) out).toByteArray());
        } catch (IOException e) {
            throw new JasenException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
        }
        return html;
    }

    /**
	 * @return Returns the size (in bytes) of the buffer used when reading url data.
	 */
    public int getReadBufferSize() {
        return readBufferSize;
    }

    /**
	 * @param readBufferSize The size (in bytes) of the buffer used when reading url data.
	 */
    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    /**
	 * @return Returns the time (in milliseconds) to wait for data from the url stream until reading is abnormally aborted.
	 */
    public long getReadTimeout() {
        return readTimeout;
    }

    /**
	 * @param readTimeout The time (in milliseconds) to wait for data from the url stream until reading is abnormally aborted.
	 */
    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }
}
