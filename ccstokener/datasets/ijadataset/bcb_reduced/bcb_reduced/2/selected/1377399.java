package neembuu.http;

import java.util.logging.Level;
import java.util.logging.Logger;
import neembuu.util.logging.LoggerUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 *
 * @author Shashank Tulsyan
 */
public class FileSizeFinder {

    private static final Logger LOGGER = LoggerUtil.getLogger();

    private static final FileSizeFinder SINGLETON = new FileSizeFinder();

    FileSizeFinder() {
    }

    public long getSize(String url) {
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            long length = response.getEntity().getContentLength();
            LOGGER.log(Level.INFO, "File size found = {0}", length);
            if (length < 0) {
                LOGGER.info("length < 0 , not setting");
            } else {
                return length;
            }
            request.abort();
        } catch (Exception any) {
            LOGGER.log(Level.INFO, "", any);
        }
        return -1;
    }

    public static FileSizeFinder getFileSizeFinder() {
        return SINGLETON;
    }
}
