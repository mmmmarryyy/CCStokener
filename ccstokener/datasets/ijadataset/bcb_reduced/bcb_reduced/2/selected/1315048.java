package com.jaspersoft.jasperserver.api.engine.jasperreports.util.repo;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import com.jaspersoft.jasperserver.api.engine.jasperreports.util.RepositoryUtil;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 * @version $Id: Handler.java 2 2006-04-30 16:19:39Z sgwood $
 */
public class Handler extends URLStreamHandler {

    public static final String REPOSITORY_PROTOCOL = "repo";

    public static final String URL_PROTOCOL_PREFIX = REPOSITORY_PROTOCOL + ':';

    public Handler() {
        super();
    }

    protected void parseURL(URL u, String spec, int start, int limit) {
        spec = spec.trim();
        String protocol = null;
        String path;
        if (spec.startsWith(URL_PROTOCOL_PREFIX)) {
            protocol = REPOSITORY_PROTOCOL;
            path = spec.substring(URL_PROTOCOL_PREFIX.length());
        } else {
            path = spec;
        }
        setURL(u, protocol, null, -1, null, null, path, null, null);
    }

    protected URLConnection openConnection(URL url) throws IOException {
        return new RepositoryConnection(RepositoryUtil.getThreadRepositoryContext(), url);
    }
}
