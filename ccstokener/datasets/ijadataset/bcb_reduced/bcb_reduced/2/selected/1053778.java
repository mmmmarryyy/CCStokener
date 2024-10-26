package net.mlw.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Matthew L. Wilson
 * @version $Revision: 1.4 $ $Date: 2004/03/17 14:20:21 $
 */
public final class NetUtils {

    /** Commons Logger */
    public static final Log LOGGER = LogFactory.getFactory().getInstance(NetUtils.class);

    /** Protect singleton. */
    private NetUtils() {
    }

    /** Retrieves a file.
    * 
    * @param url The location of the file to retrieve.
    * @param local The location to store the file locally.
    * @throws IOException Thrown if thre is an issue saving the file.
    */
    public static void copyFile(URL url, File local) throws IOException {
        InputStream in = null;
        FileWriter writer = null;
        try {
            writer = new FileWriter(local);
            in = url.openStream();
            int c;
            while ((c = in.read()) != -1) {
                writer.write(c);
            }
        } finally {
            try {
                writer.flush();
                writer.close();
                in.close();
            } catch (Exception ignore) {
                LOGGER.error(ignore);
            }
        }
    }
}
