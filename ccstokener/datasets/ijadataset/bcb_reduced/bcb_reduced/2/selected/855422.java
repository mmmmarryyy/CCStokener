package org.peaseplate.utils.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Properties;
import org.peaseplate.utils.message.Messages;

public class URLBasedLocatorEntry extends AbstractLocatorEntry {

    private final URL url;

    private final Locale locale;

    public URLBasedLocatorEntry(final URL url, final Locale locale) {
        super();
        this.url = url;
        this.locale = locale;
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public boolean gotUpdated() {
        return false;
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void upToDate() {
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public Reader openReader(final Charset charset) throws LocatorException {
        try {
            if (charset != null) {
                return new InputStreamReader(url.openStream(), charset);
            }
            return new InputStreamReader(url.openStream());
        } catch (final IOException e) {
            throw new LocatorException("Failed to read from URL: " + url, e);
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void readMessages(final Messages messages) throws LocatorException {
        try {
            final InputStream in = url.openStream();
            try {
                final Properties properties = new Properties();
                properties.load(in);
                messages.add(locale, properties);
            } finally {
                in.close();
            }
        } catch (final IOException e) {
            throw new LocatorException("Failed to read messages from URL: " + url, e);
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public String toString() {
        return url.toString();
    }
}
