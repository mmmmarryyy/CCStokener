package net.mufly.server;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public final class FileProperties {

    private URL url;

    private Properties properties;

    public FileProperties() {
    }

    public URL getUrl() {
        return this.url;
    }

    public void setUrl(final URL newUrl) {
        this.url = newUrl;
        this.properties = new Properties();
        try {
            final InputStream stream = this.url.openStream();
            this.properties.load(stream);
            stream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getProperty(final String name) {
        return this.properties.getProperty(name);
    }
}
