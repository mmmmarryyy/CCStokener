package uk.co.caprica.vlcj.radio.service.icecast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.simpleframework.xml.core.Persister;
import uk.co.caprica.vlcj.radio.model.Directory;
import uk.co.caprica.vlcj.radio.service.DirectoryService;

/**
 * Implementation of a streaming media station directory service that gets the
 * IceCast server directory from xiph.org.
 */
public class IcecastDirectoryService implements DirectoryService {

    /**
   * Remote directory URL. 
   */
    private static final String DIRECTORY_URL = "http://dir.xiph.org/yp.xml";

    /**
   * XML binding parser.
   */
    private final Persister persister;

    /**
   * Create a new directory service component.
   */
    public IcecastDirectoryService() {
        this.persister = new Persister();
    }

    @Override
    public Directory directory() {
        HttpURLConnection urlConnection = null;
        InputStream in = null;
        try {
            URL url = new URL(DIRECTORY_URL);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            String encoding = urlConnection.getContentEncoding();
            if ("gzip".equalsIgnoreCase(encoding)) {
                in = new GZIPInputStream(urlConnection.getInputStream());
            } else if ("deflate".equalsIgnoreCase(encoding)) {
                in = new InflaterInputStream(urlConnection.getInputStream(), new Inflater(true));
            } else {
                in = urlConnection.getInputStream();
            }
            return persister.read(IcecastDirectory.class, in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get directory", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
