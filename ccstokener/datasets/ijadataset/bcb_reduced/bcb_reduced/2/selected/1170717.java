package org.eclipse.core.internal.boot;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.*;
import java.util.Hashtable;
import org.eclipse.core.internal.runtime.Messages;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.url.AbstractURLStreamHandlerService;

/**
 * URL handler for the "platform" protocol
 */
public class PlatformURLHandler extends AbstractURLStreamHandlerService {

    private static Hashtable connectionType = new Hashtable();

    public static final String PROTOCOL = "platform";

    public static final String FILE = "file";

    public static final String JAR = "jar";

    public static final String BUNDLE = "bundle";

    public static final String JAR_SEPARATOR = "!/";

    public static final String PROTOCOL_SEPARATOR = ":";

    public PlatformURLHandler() {
        super();
    }

    public URLConnection openConnection(URL url) throws IOException {
        String spec = url.getFile().trim();
        if (spec.startsWith("/")) spec = spec.substring(1);
        int ix = spec.indexOf("/");
        if (ix == -1) throw new MalformedURLException(NLS.bind(Messages.url_invalidURL, url.toExternalForm()));
        String type = spec.substring(0, ix);
        Constructor construct = (Constructor) connectionType.get(type);
        if (construct == null) throw new MalformedURLException(NLS.bind(Messages.url_badVariant, type));
        PlatformURLConnection connection = null;
        try {
            connection = (PlatformURLConnection) construct.newInstance(new Object[] { url });
        } catch (Exception e) {
            throw new IOException(NLS.bind(Messages.url_createConnection, e.getMessage()));
        }
        connection.setResolvedURL(connection.resolve());
        return connection;
    }

    public static void register(String type, Class connectionClass) {
        try {
            Constructor c = connectionClass.getConstructor(new Class[] { URL.class });
            connectionType.put(type, c);
        } catch (NoSuchMethodException e) {
        }
    }

    public static void unregister(String type) {
        connectionType.remove(type);
    }
}
