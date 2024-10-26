package openvend.net;

import java.io.File;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Collections;
import junit.framework.Test;
import junit.framework.TestSuite;
import openvend.main.OvLog;
import openvend.net.OvX509TrustManager;
import openvend.net.OvX509Utils;
import openvend.test.A_OvTestCase;
import org.apache.commons.logging.Log;

public class OvX509TrustManagerTestCase extends A_OvTestCase {

    private static Log log = OvLog.getLog(OvX509TrustManagerTestCase.class);

    /**
     * Default JUnit constructor.<p>
     *
     * @param arg0 JUnit parameters
     */
    public OvX509TrustManagerTestCase(String arg0) {
        super(arg0);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.setName(OvX509TrustManagerTestCase.class.getName());
        suite.addTest(new OvX509TrustManagerTestCase("testX509TrustManager"));
        return suite;
    }

    public void testX509TrustManager() {
        try {
            echo("Test to open a SLL connection with the X509 SSL trust manager...");
            File certFile = new File(getTestDataPath(), "cert" + File.separator + "www.amazon.de.crt");
            X509Certificate cert = OvX509Utils.loadX509Certificate(certFile);
            assertTrue(cert != null);
            OvX509Utils.installHttpsURLConnectionTrustManager(new OvX509TrustManager(Collections.singletonList(cert)));
            URL url = new URL("https://www.amazon.de");
            Object content = url.openConnection().getContent();
            assertTrue(content != null);
        } catch (Throwable t) {
            fail(t.getMessage());
            if (log.isErrorEnabled()) {
                log.error(t.getMessage(), t);
            }
        }
    }
}
