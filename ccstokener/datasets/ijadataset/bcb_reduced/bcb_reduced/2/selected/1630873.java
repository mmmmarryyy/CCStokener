package pl.rzarajczyk.utils.application;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.logging.Log;
import pl.rzarajczyk.utils.log.LazyLogFactory;

/**
 *
 * @author rafalz
 */
public class VersionCheck {

    private final Configuration configuration;

    private final URL url;

    private Log log = LazyLogFactory.getLog(getClass());

    public VersionCheck(Configuration configuration, String url) throws MalformedURLException {
        this.configuration = configuration;
        this.url = new URL(url);
    }

    public boolean hasNewerVersion() {
        try {
            InputStream input = url.openStream();
            try {
                byte[] bytes = ByteStreams.toByteArray(input);
                if (bytes == null || bytes.length == 0) {
                    throw new IOException("Downloaded empty contents");
                }
                Version current = new Version(configuration.getApplicationVersion());
                Version remote = new Version(new String(bytes));
                log.info("Version check: current = " + current + "; remote = " + remote);
                return remote.compareTo(current) > 0;
            } finally {
                input.close();
            }
        } catch (IOException e) {
            log.warn("Unable to check the new version", e);
            return false;
        } catch (NumberFormatException e) {
            log.warn("Unable to check the new version", e);
            return false;
        }
    }
}
