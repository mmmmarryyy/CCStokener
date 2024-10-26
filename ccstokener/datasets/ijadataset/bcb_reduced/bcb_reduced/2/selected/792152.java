package org.exist.protocolhandler.shared;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.apache.log4j.Logger;

/**
 *
 * @author Dannes Wessels
 */
public class PutThread extends Thread {

    private static Logger LOG = Logger.getLogger(PutThread.class);

    private File file;

    private URL url;

    private Exception exception;

    public Exception getException() {
        return exception;
    }

    /**
     * Creates a new instance of PutThread
     */
    public PutThread(File file, URL url) {
        this.file = file;
        this.url = url;
    }

    public void run() {
        try {
            LOG.info("thread started");
            OutputStream os = url.openConnection().getOutputStream();
            InputStream is = new FileInputStream(file);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            is.close();
            os.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            LOG.error(ex);
            exception = ex;
        } finally {
            LOG.info("thread stopped");
        }
    }
}
