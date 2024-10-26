package org.exist.protocolhandler.xmldb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.exist.protocolhandler.eXistURLStreamHandlerFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * jUnit test for the eXist eXistURLStreamHandlerFactory class.
 * 
 * 
 * @author Dannes Wessels
 */
public class XmldbURLStreamHandlerFactoryTest {

    private static Logger LOG = Logger.getLogger(XmldbURLStreamHandlerFactoryTest.class);

    private static String XMLDB_URL_1 = "xmldb:exist://guest:guest@localhost:8080/exist/xmlrpc" + "/db/build.xml";

    @BeforeClass
    public static void start() throws Exception {
        URL.setURLStreamHandlerFactory(new eXistURLStreamHandlerFactory());
        PropertyConfigurator.configure("log4j.conf");
    }

    @Test
    public void testInit() {
        System.out.println("testInit");
        eXistURLStreamHandlerFactory.init();
        eXistURLStreamHandlerFactory.init();
    }

    /**
     * Test of eXistURLStreamHandlerFactory.
     */
    @Test
    public void testXMLDBURLStreamHandler() {
        System.out.println("testXMLDBURLStreamHandler");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            URL url = new URL(XMLDB_URL_1);
            InputStream is = url.openStream();
            copyDocument(is, baos);
            is.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error(ex);
            fail(ex.getMessage());
        }
    }

    private void copyDocument(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
    }
}
