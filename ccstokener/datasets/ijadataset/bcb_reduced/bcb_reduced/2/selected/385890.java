package util;

import static util.LogUtil.begin;
import static util.LogUtil.end;
import static util.LogUtil.endWarn;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Klasse enth�lt Methoden zum Verwenden von HTTP Daten
 * 
 * @author Goetz Epperlein
 */
public class HTTPUtil {

    /**
     * Log-Objekt f&uuml;r Commons Logging
     */
    private static final Log LOG = LogFactory.getLog(HTTPUtil.class);

    private static final boolean DEBUG = LOG.isDebugEnabled();

    private static final boolean WARN = LOG.isWarnEnabled();

    /**
     * Download any data from URL and save it to specified path
     * 
     * @param url
     *            URL for data to fetching
     * @param savePath
     *            Path for file to save (with filename includet!)
     * @throws MalformedURLException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void getURLData(String url, String savePath) throws MalformedURLException, FileNotFoundException, IOException {
        if (DEBUG) begin(LOG, url, savePath);
        InputStream inputSream = null;
        InputStream bufferedInputStrem = null;
        OutputStream fileOutputStream = null;
        try {
            URL urlObj = new URL(url);
            inputSream = urlObj.openStream();
            bufferedInputStrem = new BufferedInputStream(inputSream);
            File file = new File(savePath);
            fileOutputStream = new FileOutputStream(file);
            byte[] buffer = new byte[0xFFFF];
            for (int len; (len = bufferedInputStrem.read(buffer)) != -1; ) {
                fileOutputStream.write(buffer, 0, len);
            }
        } finally {
            try {
                if (fileOutputStream != null) fileOutputStream.close();
                if (bufferedInputStrem != null) bufferedInputStrem.close();
                if (inputSream != null) inputSream.close();
            } catch (Exception e) {
                if (WARN) endWarn(LOG, e);
                e.printStackTrace();
            }
        }
        if (DEBUG) end(LOG);
    }
}
