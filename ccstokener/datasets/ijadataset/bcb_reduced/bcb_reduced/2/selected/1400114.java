package org.netbeans.server.uihandler.bugs;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import junit.framework.TestCase;

/**
 *
 * @author Jindrich Sedek
 */
public abstract class BugzillaTestCase extends TestCase {

    private String disableMessage = null;

    @Override
    protected void setUp() throws Exception {
        if (disableMessage == null) {
            checkServerAccess();
        }
    }

    private void checkServerAccess() throws IOException {
        URL url = new URL("https://testnetbeans.org/bugzilla/index.cgi");
        try {
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
        } catch (IOException exc) {
            disableMessage = "Bugzilla is not accessible";
        }
        url = new URL(BugzillaConnector.SERVER_URL);
        try {
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
        } catch (IOException exc) {
            disableMessage = "Bugzilla Service is not accessible";
        }
    }

    @Override
    protected void runTest() throws Throwable {
        if (disableMessage != null) {
            System.err.println(disableMessage);
            return;
        }
        super.runTest();
    }
}
