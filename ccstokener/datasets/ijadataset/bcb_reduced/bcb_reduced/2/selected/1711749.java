package org.objectstyle.cayenne.remote.hessian;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.objectstyle.cayenne.remote.RemoteSession;

/**
 * A proxy factory that handles HTTP sessions.
 * 
 * @author Andrus Adamchik
 * @since 1.2
 */
class HessianProxyFactory extends com.caucho.hessian.client.HessianProxyFactory {

    static final String SESSION_COOKIE_NAME = "JSESSIONID";

    private HessianConnection clientConnection;

    HessianProxyFactory(HessianConnection clientConnection) {
        this.clientConnection = clientConnection;
    }

    protected URLConnection openConnection(URL url) throws IOException {
        URLConnection connection = super.openConnection(url);
        RemoteSession session = clientConnection.getSession();
        if (session != null && session.getSessionId() != null) {
            connection.setRequestProperty("Cookie", SESSION_COOKIE_NAME + "=" + session.getSessionId());
        }
        return connection;
    }
}
