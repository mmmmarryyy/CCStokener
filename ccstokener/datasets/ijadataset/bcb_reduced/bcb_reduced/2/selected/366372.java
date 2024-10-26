package net.laubenberger.bogatyr.service.localizer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import net.laubenberger.bogatyr.helper.HelperLog;
import net.laubenberger.bogatyr.misc.Constants;
import net.laubenberger.bogatyr.misc.exception.RuntimeExceptionIsNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encoding control for the {@link LocalizerFile}.
 * 
 * @author Stefan Laubenberger
 * @version 0.9.5 (20110123)
 * @since 0.9.5
 */
public class EncodingControl extends Control {

    private static final Logger log = LoggerFactory.getLogger(EncodingControl.class);

    private final String encoding;

    public EncodingControl() {
        this(Constants.ENCODING_DEFAULT);
        if (log.isTraceEnabled()) log.trace(HelperLog.constructor());
    }

    public EncodingControl(final String encoding) {
        super();
        if (log.isTraceEnabled()) log.trace(HelperLog.constructor(encoding));
        if (null == encoding) {
            throw new RuntimeExceptionIsNull("encoding");
        }
        this.encoding = encoding;
    }

    @Override
    public ResourceBundle newBundle(final String baseName, final Locale locale, final String format, final ClassLoader loader, final boolean reload) throws IllegalAccessException, InstantiationException, IOException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(baseName, locale, format, loader, reload));
        final String bundleName = toBundleName(baseName, locale);
        final String resourceName = toResourceName(bundleName, "properties");
        ResourceBundle result = null;
        InputStream stream = null;
        if (reload) {
            final URL url = loader.getResource(resourceName);
            if (null != url) {
                final URLConnection connection = url.openConnection();
                if (null != connection) {
                    connection.setUseCaches(false);
                    stream = connection.getInputStream();
                }
            }
        } else {
            stream = loader.getResourceAsStream(resourceName);
        }
        if (null != stream) {
            try {
                result = new PropertyResourceBundle(new InputStreamReader(stream, encoding));
            } finally {
                stream.close();
            }
        }
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit(result));
        return result;
    }
}
