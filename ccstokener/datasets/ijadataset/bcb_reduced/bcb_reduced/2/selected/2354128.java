package net.sourceforge.x360mediaserve.plugins.audioTranscoder.impl.streamers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles files that are already in the correct format
 * 
 * @author tom
 * 
 */
public class StreamNative implements StreamStreamer {

    Logger logger = LoggerFactory.getLogger(StreamNative.class);

    /**
	 * Copies a given file to the OutputStream
	 * 
	 * @param file
	 * @param os
	 */
    public void writeToStream(String urlString, OutputStream os) {
        BufferedInputStream input = null;
        try {
            URL url = new URL(urlString);
            logger.debug("Opening stream:" + url.toString());
            input = new BufferedInputStream(url.openStream(), 4 * 1024 * 1024);
            byte[] data = new byte[102400];
            int read;
            while ((read = input.read(data)) != -1) {
                os.write(data, 0, read);
            }
        } catch (Exception e) {
            logger.debug("Error writing to stream:", e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.debug("Error closing stream:", e);
                }
            }
        }
    }
}
