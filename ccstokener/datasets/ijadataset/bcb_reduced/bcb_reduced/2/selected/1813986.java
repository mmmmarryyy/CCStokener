package uk.co.cocking.getinline2.pipeline.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import uk.co.cocking.getinline2.pipeline.transformers.AbstractTransformer;

public class UrlRetriever extends AbstractTransformer<String, String> {

    private String lineSeparator = System.getProperty("line.separator");

    private static Logger log = Logger.getLogger("uk.co.cocking.getinline2.io.UrlRetriever");

    @Override
    public List<String> transform(String urlString) {
        String result = "";
        InputStream inputStream = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-agent", "finance news monitor");
            connection.setRequestProperty("From", "romilly.cocking@gmail.com");
            connection.setInstanceFollowRedirects(true);
            inputStream = connection.getInputStream();
            result = StringUtils.join(IOUtils.readLines(inputStream).toArray(), lineSeparator);
        } catch (MalformedURLException e) {
            log.warn("Malformed url " + urlString);
        } catch (IOException e) {
            log.warn("error reading from url " + urlString, e);
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.warn("could not close url " + urlString, e);
            }
        }
        return enlist(result);
    }
}
