package edu.ucdavis.genomics.metabolomics.util.io.source;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.apache.log4j.Logger;
import edu.ucdavis.genomics.metabolomics.exception.ConfigurationException;

public class URLSource implements Source {

    private URL url;

    private Logger logger = Logger.getLogger(URLSource.class);

    public URLSource() {
    }

    public URLSource(URL url) {
        super();
        this.url = url;
    }

    public URLSource(String url) throws MalformedURLException {
        super();
        this.url = new URL(url);
    }

    public InputStream getStream() throws IOException {
        return url.openStream();
    }

    public String getSourceName() {
        return url.toString();
    }

    public void setIdentifier(Object o) throws ConfigurationException {
        if (o instanceof URL) {
            url = (URL) o;
        } else {
            try {
                url = new URL(o.toString());
            } catch (MalformedURLException e) {
                throw new ConfigurationException(e);
            }
        }
    }

    public void configure(Map<?, ?> p) throws ConfigurationException {
    }

    public boolean exist() {
        return url != null;
    }

    public long getVersion() {
        try {
            return url.openConnection().getDate();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return -1;
        }
    }
}
