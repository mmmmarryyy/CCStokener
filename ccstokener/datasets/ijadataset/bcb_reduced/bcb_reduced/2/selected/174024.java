package org.exist.protocolhandler.shared;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;

/**
 *
 * @author wessels
 */
public class GetThread extends Thread {

    private static Logger LOG = Logger.getLogger(GetThread.class);

    private URL url;

    private int size = -1;

    private Exception exception;

    public Exception getException() {
        return exception;
    }

    public int getSize() {
        return size;
    }

    /**
     * Creates a new instance of PutThread
     */
    public GetThread(URL url) {
        this.url = url;
    }

    public void run() {
        try {
            LOG.info("thread started");
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            InputStream is = url.openConnection().getInputStream();
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            is.close();
            os.close();
            size = os.size();
        } catch (IOException ex) {
            LOG.error(ex);
            ex.printStackTrace();
            exception = ex;
        } finally {
            LOG.info("thread stopped");
        }
    }
}
