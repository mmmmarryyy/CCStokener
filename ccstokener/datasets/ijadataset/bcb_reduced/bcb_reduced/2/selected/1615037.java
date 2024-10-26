package org.apache.commons.vfs.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * A proxy for URLs that are supported by the standard stream handler factory.
 *
 * @author <a href="mailto:brian@mmmanager.org">Brian Olsen</a>
 * @version $Revision: 480428 $ $Date: 2006-11-28 22:15:24 -0800 (Tue, 28 Nov 2006) $
 */
class URLStreamHandlerProxy extends URLStreamHandler {

    protected URLConnection openConnection(final URL url) throws IOException {
        final URL proxyURL = new URL(url.toExternalForm());
        return proxyURL.openConnection();
    }

    protected void parseURL(final URL u, final String spec, final int start, final int limit) {
        try {
            final URL url = new URL(u, spec);
            setURL(u, url.getProtocol(), url.getHost(), url.getPort(), url.getAuthority(), url.getUserInfo(), url.getFile(), url.getQuery(), url.getRef());
        } catch (MalformedURLException mue) {
            throw new RuntimeException(mue.getMessage());
        }
    }
}
