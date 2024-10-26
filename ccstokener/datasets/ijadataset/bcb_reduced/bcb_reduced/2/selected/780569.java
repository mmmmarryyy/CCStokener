package es.f2020.osseo.core.util;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.fileupload.util.Streams;
import es.f2020.osseo.core.OsseoFailure;

/**
 * Some networking utilities.
 */
public final class NetUtils {

    public static final int CONNECT_TIMEOUT = 30000;

    public static final int READ_TIMEOUT = 240000;

    public static final int NUMBER_OF_ATTEMPTS = 4;

    private static Log log = LogFactory.getLog(NetUtils.class);

    private NetUtils() {
    }

    public static synchronized String getPage(String urlString) {
        for (int i = 1; i <= NUMBER_OF_ATTEMPTS; i++) {
            try {
                URL url = new URL(urlString);
                URLConnection conn = url.openConnection();
                conn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; rv:1.8.1.12) Osseo 0.9.8");
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setDoOutput(true);
                conn.setUseCaches(true);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Streams.copy(conn.getInputStream(), out, true);
                String result = out.toString("UTF-8");
                if (result != null && result.length() > 0) {
                    return result;
                }
            } catch (MalformedURLException ex) {
                throw new OsseoFailure(ex);
            } catch (IOException ex) {
                if (i == NUMBER_OF_ATTEMPTS) {
                    if (log.isErrorEnabled()) {
                        log.error("Error searching for -" + urlString + "-");
                    }
                    ex.printStackTrace(System.out);
                    throw new OsseoFailure(ex);
                }
            }
        }
        if (log.isInfoEnabled()) {
            log.error("Cannot get -" + urlString + "- in " + NUMBER_OF_ATTEMPTS + " attempts.");
        }
        return "";
    }
}
