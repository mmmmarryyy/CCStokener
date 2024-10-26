package conga.io;

import conga.CongaRuntimeException;
import conga.io.format.ConfigFormat;
import conga.io.format.ConfigFormats;
import conga.param.tree.Tree;
import org.apache.commons.lang.Validate;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/** @author Justin Caballero */
public class UrlRepo extends AbstractRepository implements FormattedSource {

    private final URL url;

    private final AbstractFormattedSource source;

    public UrlRepo(String url) {
        this(makeUrl(url));
    }

    public UrlRepo(String url, ConfigFormat format) {
        this(makeUrl(url), format);
    }

    public UrlRepo(URL url) {
        this(url, ConfigFormats.getDefault());
    }

    public UrlRepo(URL url, ConfigFormat format) {
        Validate.notNull(url, "URL cannot be null");
        this.url = url;
        source = new AbstractFormattedSource(format) {

            {
                setParent(UrlRepo.this);
            }

            public BufferedReader newReader() {
                try {
                    return buffer(UrlRepo.this.url.openStream());
                } catch (IOException e) {
                    throw new CongaRuntimeException(e);
                }
            }
        };
    }

    public URL getUrl() {
        return url;
    }

    public BufferedReader newReader() {
        return source.newReader();
    }

    public ConfigFormat getFormat() {
        return source.getFormat();
    }

    protected Source getSource() {
        return source;
    }

    protected Sink getSink() {
        return new AbstractSink() {

            protected void doSink(final Tree tree) {
                try {
                    URLConnection cxn = url.openConnection();
                    cxn.setDoOutput(true);
                    PrintWriter w = new PrintWriter(cxn.getOutputStream());
                    getFormat().write(tree, w);
                    w.close();
                } catch (IOException e) {
                    throw new CongaRuntimeException(url.toString(), e);
                }
            }
        };
    }

    public String toString() {
        return url.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (source == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final UrlRepo that = (UrlRepo) o;
        return url.equals(that.url);
    }

    public int hashCode() {
        return url.hashCode();
    }

    private static URL makeUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
