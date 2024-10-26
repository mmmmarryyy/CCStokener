package net.sourceforge.x360mediaserve.formats.impl.streamers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import net.sourceforge.x360mediaserve.api.formats.Streamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple streamer that opens a file and reads it to the outputstream
 * 
 * @author tom
 * 
 */
public class NativeURL implements Streamer {

    Logger logger = LoggerFactory.getLogger(NativeURL.class);

    /**
	 * Copies a given file to the OutputStream
	 * 
	 * @param file
	 * @param os
	 */
    URL url;

    long startPoint = 0;

    String mimeType;

    URLConnection connection;

    public NativeURL() {
    }

    public void setMimeType(String type) {
        this.mimeType = type;
    }

    public void setURL(URL url) {
        this.url = url;
    }

    public boolean setStartPoint(long index) {
        this.startPoint = index;
        return true;
    }

    public boolean writeToStream(OutputStream os) {
        BufferedInputStream is = null;
        boolean success = false;
        try {
            logger.debug("Playing:" + url + " from " + this.startPoint);
            if (connection == null) {
                openConnection();
            }
            is = new BufferedInputStream(connection.getInputStream());
            if (this.startPoint != 0) is.skip(this.startPoint);
            byte input[] = new byte[4096];
            int bytesread;
            long totalbytes = 0;
            while ((bytesread = is.read(input)) != -1) {
                os.write(input, 0, bytesread);
                totalbytes += bytesread;
            }
            os.flush();
            logger.debug("End of File Reached, wrote:" + totalbytes);
            logger.debug("End of File Reached, wrote:" + is.read());
            success = true;
        } catch (Exception e) {
            logger.debug(e.toString());
        } finally {
            if (is != null) try {
                is.close();
            } catch (Exception e) {
            }
        }
        return success;
    }

    public void cleanUp() {
    }

    private void openConnection() {
        try {
            connection = url.openConnection();
        } catch (IOException e) {
            logger.error("Error opening url", e);
        }
    }

    public void setSizeOfContent(long size) {
    }

    public Long getSizeOfContent() {
        if (connection == null) {
            openConnection();
        }
        int l = connection.getContentLength();
        if (l == -1) return null;
        return (long) l;
    }

    public boolean supportsRanges() {
        return true;
    }

    public String getContentType() {
        return this.mimeType;
    }
}
