package net.sourceforge.retriever.fetcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A fetcher receives an URL and produces an object holding information
 * about the resource.
 * </p>
 * 
 * <p>
 * For HTTP files, for instance, the fetcher makes a HTTP connection to
 * the resource pointed by the URL, extracting from it all useful information
 * and storing them into a <code>Resource</code> object for consumption. After,
 * it closes the underlying data stream to the resource.
 * </p>
 * 
 * <p>
 * The fetcher is intended to work with a range of different protocols. For
 * each protocol, a <code>Fetcher</code> subclass must be implemented. After implementing
 * the specific fetcher class, registering it with the 
 * <code>register(String protocol, Class class)</code> method will make it 
 * available to the crawler.
 * </p>
 * 
 * @see net.sourceforge.retriever.fetcher.resource.Resource
 * @see net.sourceforge.retriever.fetcher.Fetcher#register(String, Class)
 */
public abstract class Fetcher {

    private volatile boolean charsetJarPresent = true;

    private static Map<String, Class<? extends Fetcher>> fetcherTypes;

    static {
        if (fetcherTypes == null) {
            synchronized (Fetcher.class) {
                if (fetcherTypes == null) {
                    fetcherTypes = new HashMap<String, Class<? extends Fetcher>>();
                    fetcherTypes.put("http", HTTPFetcher.class);
                    fetcherTypes.put("https", HTTPFetcher.class);
                    fetcherTypes.put("file", FileFetcher.class);
                    fetcherTypes.put("smb", SMBFetcher.class);
                }
            }
        }
    }

    private static int connectTimeout = 60000;

    private static int fetchTimeout = 60000 * 5;

    private final Resource fetchedResource;

    public abstract boolean canCrawl();

    protected abstract List<String> getInnerURLs(String charset);

    protected abstract void treatFetchException(final URLConnection urlConnection, final Exception e);

    protected abstract void close(URLConnection urlConnection);

    protected abstract void lastFetch(URLConnection urlConnection);

    /**
	 * Creates a <code>Fetcher</code> object for a given URL.
	 * 
	 * @param url The URL containing the content to be used in the fetch process.
	 */
    public Fetcher(final URL url) {
        this.fetchedResource = new Resource(url);
    }

    /**
	 * Creates a <code>Fetcher</code> object for a given URL.
	 * 
	 * @param url The URL containing the content to be used in the fetch process.
	 */
    public static Fetcher create(final URL url) throws Exception {
        final Class<? extends Fetcher> resourceClass = Fetcher.fetcherTypes.get(url.getProtocol().toLowerCase());
        if (resourceClass == null) return new NullFetcher(url);
        return resourceClass.getConstructor(URL.class).newInstance(url);
    }

    /**
	 * Fetches the <code>Resource</code>.
	 * 
	 * @throws Exception If something goes wrong while the <code>Resource</code> is
	 *         being fetched.
	 */
    public Resource fetch() throws Exception {
        return this.fetch(false);
    }

    /**
	 * TODO Write javadoc.
	 */
    public Resource fetch(final boolean lastFetch) throws Exception {
        if (!this.canCrawl()) {
            this.fetchedResource.addInnerURLs(this.getInnerURLs(null));
            return this.fetchedResource;
        }
        URLConnection urlConnection = null;
        try {
            urlConnection = this.fetchedResource.getURL().openConnection();
            if (lastFetch) this.lastFetch(urlConnection);
            urlConnection.setConnectTimeout(Fetcher.connectTimeout);
            urlConnection.setReadTimeout(Fetcher.fetchTimeout);
            urlConnection.connect();
            this.correctURL(urlConnection);
            final InputStream copiedInputStream = copyInputStream(urlConnection.getInputStream());
            final String charset = this.getCharset(copiedInputStream);
            this.fetchedResource.setInputStream(copiedInputStream);
            this.fetchedResource.setCharset(charset);
            this.fetchedResource.addInnerURLs(this.getInnerURLs(charset));
            this.fetchedResource.setContentType(urlConnection.getContentType());
            return this.fetchedResource;
        } catch (final Exception e) {
            this.treatFetchException(urlConnection, e);
            throw e;
        } finally {
            this.closeConnectionToResource(urlConnection);
        }
    }

    /**
	 * Resets the <code>InputStream</code>, so one can reread it.
	 */
    public void resetInputStream() {
        if (this.fetchedResource.getInputStream() != null) {
            try {
                this.fetchedResource.getInputStream().reset();
            } catch (final IOException e) {
            }
        }
    }

    /**
	 * Sets the read timeout to a specified timeout, in milliseconds. A non-zero value 
	 * specifies the timeout when reading from Input stream when a connection is established 
	 * to a resource. If the timeout expires before there is data available for read, 
	 * an exception is raised. A timeout of zero is interpreted as an infinite timeout. 
	 * 
	 * @param milliseconds The time in milliseconds.
	 */
    public static void setFetchTimeout(final int milliseconds) {
        Fetcher.fetchTimeout = milliseconds;
    }

    /**
	 * The connect timeout.
	 * 
	 * @return The time in milliseconds.
	 * @see Fetcher#setFetchTimeout(int)
	 */
    public static int getFetchTimeout() {
        return Fetcher.fetchTimeout;
    }

    /**
	 * Sets a specified timeout value, in milliseconds, to be used when opening a 
	 * communications link to the resource. If the timeout expires before the connection 
	 * can be established, an exception is raised. A timeout of zero is interpreted as an 
	 * infinite timeout. 
	 * 
	 * @param milliseconds The time in milliseconds.
	 */
    public static void setConnectTimeout(final int milliseconds) {
        Fetcher.connectTimeout = milliseconds;
    }

    /**
	 * The connect timeout.
	 * 
	 * @return The time in milliseconds.
	 * @see Fetcher#setConnectTimeout(int)
	 */
    public static int getConnectTimeout() {
        return Fetcher.connectTimeout;
    }

    /**
	 * Dynamically registers a resource type and associates it with a protocol.
	 * 
	 * @param protocol The protocol.
	 * @param resource The resource class that handles a specific protocol.
	 */
    public static void register(final String protocol, final Class<? extends Fetcher> resource) {
        Fetcher.fetcherTypes.put(protocol.toLowerCase(), resource);
    }

    protected URL getURL() {
        return this.fetchedResource.getURL();
    }

    protected InputStream getInputStream() {
        return this.fetchedResource.getInputStream();
    }

    private void closeConnectionToResource(URLConnection urlConnection) {
        try {
            if (urlConnection != null) {
                this.close(urlConnection);
                if (urlConnection.getInputStream() != null) {
                    urlConnection.getInputStream().close();
                }
            }
        } catch (final Throwable t) {
        }
    }

    private InputStream copyInputStream(final InputStream input) throws IOException {
        final int BUFFER_SIZE = 2048;
        ByteArrayOutputStream fos = null;
        try {
            fos = new ByteArrayOutputStream();
            final byte[] buffer = new byte[BUFFER_SIZE];
            int length = input.read(buffer, 0, BUFFER_SIZE);
            while (length > -1) {
                fos.write(buffer, 0, length);
                length = input.read(buffer, 0, BUFFER_SIZE);
            }
            return new ByteArrayInputStream(fos.toByteArray());
        } finally {
            try {
                if (input != null) input.close();
            } catch (final IOException e) {
            }
            try {
                if (fos != null) fos.close();
            } catch (final IOException e) {
            }
        }
    }

    private void correctURL(URLConnection urlConnection) {
        final URL url = urlConnection.getURL();
        this.fetchedResource.setURL(url);
        this.addEndSlashIfNecessary(url);
    }

    private void addEndSlashIfNecessary(final URL url) {
        final String urlAsString = url.toExternalForm();
        if (urlAsString.toLowerCase().startsWith("http")) {
            if (urlAsString.endsWith(url.getHost())) {
                try {
                    this.fetchedResource.setURL(new URL(urlAsString + "/"));
                } catch (final MalformedURLException e) {
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String getCharset(final InputStream inputStream) {
        if (!this.charsetJarPresent) return null;
        try {
            final Class charsetDetectorClass = Class.forName("com.ibm.icu.text.CharsetDetector");
            final Object charsetDetectorObject = charsetDetectorClass.newInstance();
            final Method setText = charsetDetectorClass.getDeclaredMethod("setText", InputStream.class);
            inputStream.reset();
            setText.invoke(charsetDetectorObject, inputStream);
            final Method detect = charsetDetectorClass.getMethod("detect");
            final Object charsetMatch = detect.invoke(charsetDetectorObject);
            final Method getName = charsetMatch.getClass().getMethod("getName");
            return (String) getName.invoke(charsetMatch);
        } catch (final Exception e) {
            this.charsetJarPresent = false;
            return null;
        } finally {
            try {
                inputStream.reset();
            } catch (final IOException e) {
            }
        }
    }
}
