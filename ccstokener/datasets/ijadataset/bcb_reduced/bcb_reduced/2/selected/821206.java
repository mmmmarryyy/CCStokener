package eulergui.parser.service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;

/**
 * 
 * @author luc peuvrier
 * 
 */
public final class NetReaderFactory {

    private static final NetReaderFactory INSTANCE = new NetReaderFactory();

    /** last url used by create method */
    private transient URI url;

    private NetReaderFactory() {
        super();
    }

    /**
	 * 
	 * @return the uniq instance
	 */
    public static NetReaderFactory getInstance() {
        return INSTANCE;
    }

    /**
	 * 
	 * @param strUrl
	 *            url to read, string form
	 * @return a reader on url ( opened for read )
	 * @throws IOException
	 *             MalformedURLException If the string specifies an unknown
	 *             protocol.<br>
	 *             IOException if an I/O exception occurs while opening
	 *             connection.<br>
	 * @throws URISyntaxException 
	 * 
	 */
    public Reader create(final String strUrl) throws IOException, URISyntaxException {
        final URI url = new URI(strUrl);
        return create(url);
    }

    /**
	 * 
	 * @param url
	 *            url to read
	 * @return a reader on url ( opened for read )
	 * @throws IOException
	 *             if an I/O exception occurs while opening connection.<br>
	 *             if an I/O error occurs while creating the input stream.<br>
	 */
    public Reader create(final URI url) throws IOException {
        this.url = url;
        if (!url.isAbsolute()) {
            return new FileReader(new File(url.toString()));
        }
        URLConnection connection = url.toURL().openConnection();
        connection.setDoInput(true);
        final InputStream inputStream = connection.getInputStream();
        return new InputStreamReader(inputStream);
    }

    public URI getUrl() {
        return url;
    }
}
