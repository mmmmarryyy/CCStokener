package org.exist.protocolhandler.xmlrpc;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.exist.protocolhandler.eXistURLStreamHandlerFactory;
import org.exist.protocolhandler.xmldb.XmldbURL;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *  jUnit tests for XmlrpcOutputStream class.
 *
 * @author Dannes Wessels.
 */
public class XmlrpcInOutputStreamTest {

    private static Logger LOG = Logger.getLogger(XmlrpcInOutputStreamTest.class);

    private String TESTCASENAME = getClass().getName();

    @BeforeClass
    public static void start() throws Exception {
        URL.setURLStreamHandlerFactory(new eXistURLStreamHandlerFactory());
        PropertyConfigurator.configure("log4j.conf");
    }

    private void sendDocument(XmldbURL uri, InputStream is) throws IOException {
        XmlrpcOutputStream xos = new XmlrpcOutputStream(uri);
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) > 0) {
            xos.write(buf, 0, len);
        }
        xos.close();
    }

    private void getDocument(XmldbURL uri, OutputStream os) throws IOException {
        InputStream xis = new XmlrpcInputStream(uri);
        byte[] buf = new byte[4096];
        int len;
        while ((len = xis.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
        xis.close();
    }

    @Test
    public void testCreateCollection() {
        try {
            URL url = new URL("http://localhost:8080/exist/rest/db?_query=" + "xmldb:create-collection(%22/db/%22,%22" + TESTCASENAME + "%22)");
            url.openStream();
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error(ex);
            fail(ex.getMessage());
        }
    }

    @Test
    public void toDB() {
        System.out.println("toDB");
        try {
            FileInputStream fis = new FileInputStream("conf.xml");
            String url = "xmldb:exist://localhost:8080/exist/xmlrpc/db/" + TESTCASENAME + "/conf_toDB.xml";
            XmldbURL xmldbUri = new XmldbURL(url);
            sendDocument(xmldbUri, fis);
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error(ex);
            fail(ex.getMessage());
        }
    }

    @Test
    public void fromDB() {
        System.out.println("fromDB");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String url = "xmldb:exist://localhost:8080/exist/xmlrpc/db/" + TESTCASENAME + "/conf_toDB.xml";
        try {
            XmldbURL xmldbUri = new XmldbURL(url);
            getDocument(xmldbUri, baos);
            assertTrue(baos.size() > 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error(ex);
            fail(ex.getMessage());
        }
    }

    @Test
    public void toDB_NotExistingCollection() {
        System.out.println("toDB_NotExistingCollection");
        try {
            FileInputStream fis = new FileInputStream("conf.xml");
            String url = "xmldb:exist://localhost:8080/exist/xmlrpc/db/" + TESTCASENAME + "/foobar/conf_toDB.xml";
            XmldbURL xmldbUri = new XmldbURL(url);
            sendDocument(xmldbUri, fis);
            fis.close();
            fail("not existing collection: Expected exception");
        } catch (Exception ex) {
            if (!ex.getCause().getMessage().matches(".*Collection /db/.* not found.*")) {
                ex.printStackTrace();
                LOG.error(ex);
                fail(ex.getMessage());
            }
        }
    }

    @Test
    public void fromDB_NotExistingCollection() {
        System.out.println("fromDB_NotExistingCollection");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String url = "xmldb:exist://localhost:8080/exist/xmlrpc/db/" + TESTCASENAME + "/foobar/conf_toDB.xml";
        try {
            XmldbURL xmldbUri = new XmldbURL(url);
            getDocument(xmldbUri, baos);
            fail("Exception expected, not existing collection.");
        } catch (Exception ex) {
            if (!ex.getCause().getMessage().matches(".*Collection /db/.* not found.*")) {
                ex.printStackTrace();
                LOG.error(ex);
                fail(ex.getMessage());
            }
        }
    }

    @Test
    public void toDB_NotExistingUser() {
        System.out.println("toDB_NotExistingUser");
        try {
            FileInputStream fis = new FileInputStream("conf.xml");
            String url = "xmldb:exist://foo:bar@localhost:8080/exist/xmlrpc/db/" + TESTCASENAME + "/conf_toDB.xml";
            XmldbURL xmldbUri = new XmldbURL(url);
            sendDocument(xmldbUri, fis);
            fis.close();
            fail("not existing user: Expected exception");
        } catch (Exception ex) {
            if (!ex.getCause().getMessage().matches(".*User foo unknown.*")) {
                ex.printStackTrace();
                LOG.error(ex);
                fail(ex.getMessage());
            }
        }
    }

    @Test
    public void fromDB_NotExistingUser() {
        System.out.println("fromDB_NotExistingUser");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String url = "xmldb:exist://foo:bar@localhost:8080/exist/xmlrpc/db/" + TESTCASENAME + "/conf_toDB.xml";
        try {
            XmldbURL xmldbUri = new XmldbURL(url);
            getDocument(xmldbUri, baos);
            fail("Exception expected, not existing collection.");
        } catch (Exception ex) {
            if (!ex.getCause().getMessage().matches(".*User foo unknown")) {
                ex.printStackTrace();
                LOG.error(ex);
                fail(ex.getMessage());
            }
        }
    }

    @Test
    public void toDB_NotAuthorized() {
        System.out.println("toDB_NotAuthorized");
        try {
            FileInputStream fis = new FileInputStream("build.xml");
            String url = "xmldb:exist://guest:guest@localhost:8080" + "/exist/xmlrpc/db/system/users.xml";
            XmldbURL xmldbUri = new XmldbURL(url);
            sendDocument(xmldbUri, fis);
            fis.close();
            fail("User not authorized: Expected exception");
        } catch (Exception ex) {
            if (!ex.getCause().getMessage().matches(".*Document exists and update is not allowed for the collection.*")) {
                ex.printStackTrace();
                LOG.error(ex);
                fail(ex.getMessage());
            }
        }
    }

    @Test
    public void fromDB_NotAuthorized() {
        System.out.println("fromDB_NotAuthorized");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String url = "xmldb:exist://guest:guest@localhost:8080" + "/exist/xmlrpc/db/system/users.xml";
        try {
            XmldbURL xmldbUri = new XmldbURL(url);
            getDocument(xmldbUri, baos);
            fail("Exception expected, not existing collection.");
        } catch (Exception ex) {
            if (!ex.getCause().getMessage().matches(".*Insufficient privileges to read resource")) {
                ex.printStackTrace();
                LOG.error(ex);
                fail(ex.getMessage());
            }
        }
    }

    @Test
    public void toDB_binaryDoc() {
        System.out.println("toDB_binaryDoc");
        try {
            FileInputStream fis = new FileInputStream("manifest.mf");
            String url = "xmldb:exist://localhost:8080/exist/xmlrpc/db/" + TESTCASENAME + "/manifest.txt";
            XmldbURL xmldbUri = new XmldbURL(url);
            sendDocument(xmldbUri, fis);
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error(ex);
            fail(ex.getMessage());
        }
    }

    @Test
    public void fromDB_binaryDoc() {
        System.out.println("fromDB_binaryDoc");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String url = "xmldb:exist://localhost:8080/exist/xmlrpc/db/" + TESTCASENAME + "/manifest.txt";
        try {
            XmldbURL xmldbUri = new XmldbURL(url);
            getDocument(xmldbUri, baos);
            assertTrue("Filesize must be greater than 0", baos.size() > 0);
            assertEquals(85, baos.size());
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error(ex);
            fail(ex.getMessage());
        }
    }
}
