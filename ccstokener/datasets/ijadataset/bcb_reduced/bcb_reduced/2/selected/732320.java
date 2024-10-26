package net.sf.jannot.source;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import net.sf.jannot.parser.Parser;

/**
 * 
 * @author Thomas Abeel
 *
 */
public class URLSource extends AbstractStreamDataSource {

    protected URL url;

    protected URLSource(URL url, Object x) throws IOException {
        super(new Locator(url.toString()));
        this.url = url;
    }

    private void init() throws MalformedURLException, IOException {
        super.setParser(Parser.detectParser(url.openStream(), url));
        super.setIos(url.openStream());
    }

    public URLSource(URL url) throws IOException {
        this(url, null);
        SSL.certify(url);
        init();
    }

    public URL getURL() {
        return url;
    }

    @Override
    public String toString() {
        return url.toString();
    }

    @Override
    public boolean isIndexed() {
        return false;
    }

    private long cachedSize = -2;

    @Override
    public long size() {
        if (cachedSize == -2) try {
            cachedSize = url.openConnection().getContentLength();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cachedSize;
    }
}
