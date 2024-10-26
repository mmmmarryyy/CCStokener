package org.exist.protocolhandler.embedded;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.apache.log4j.Logger;
import org.exist.protocolhandler.GenericTestcase;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dannes Wessels
 */
public class EmbeddedURLsTest extends GenericTestcase {

    private static Logger LOG = Logger.getLogger(EmbeddedURLsTest.class);

    private void sendToURL(String URL, String file) throws Exception {
        URL url = new URL(URL);
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        OutputStream os = url.openConnection().getOutputStream();
        copyDocument(is, os);
        is.close();
        os.close();
    }

    private void getFromURL(String URL, OutputStream os) throws Exception {
        URL url = new URL(URL);
        InputStream is = url.openConnection().getInputStream();
        copyDocument(is, os);
        is.close();
        os.close();
    }

    private void copyDocument(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
    }

    @Test
    public void toDB() {
        System.out.println("toDB");
        try {
            sendToURL("xmldb:exist:///db/build_toDB.xml", "build.xml");
        } catch (Exception ex) {
            LOG.error(ex);
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }

    @Test
    public void fromDB() {
        System.out.println("fromDB");
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            getFromURL("xmldb:exist:///db/build_toDB.xml", baos);
            baos.close();
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
            sendToURL("xmldb:exist:///db/foo/bar.xml", "build.xml");
            fail("Not existing collection: Exception expected");
        } catch (Exception ex) {
            if (!ex.getCause().getMessage().matches(".*Resource /db/foo is not a collection.*")) {
                ex.printStackTrace();
                fail(ex.getMessage());
                LOG.error(ex);
            }
        }
    }

    @Test
    public void fromDB_NotExistingCollection() {
        System.out.println("fromDB_NotExistingCollection");
        try {
            OutputStream os = new ByteArrayOutputStream();
            getFromURL("xmldb:exist:///db/foo.bar", os);
            os.close();
            fail("Not existing collection: Exception expected");
        } catch (Exception ex) {
            if (!ex.getCause().getMessage().matches("Resource .* not found.")) {
                ex.printStackTrace();
                fail(ex.getMessage());
                LOG.error(ex);
            }
        }
    }

    @Test
    public void toDB_NotExistingUser() {
        System.out.println("toDB_NotExistingUser");
        try {
            sendToURL("xmldb:exist://foo:bar@/db/toDB_NotExistingUser.xml", "build.xml");
            fail("Not existing user: Exception expected");
        } catch (Exception ex) {
            if (!ex.getCause().getMessage().matches(".*Unauthorized user.*")) {
                ex.printStackTrace();
                LOG.error(ex);
                fail(ex.getMessage());
            }
        }
    }

    @Test
    public void fromDB_NotExistingUser() {
        System.out.println("fromDB_NotExistingUser()");
        try {
            OutputStream os = new ByteArrayOutputStream();
            getFromURL("xmldb:exist://foo:bar@/db/fromDB_NotExistingUser.xml", os);
            os.close();
            fail("Not existing user: Exception expected");
        } catch (Exception ex) {
            if (!ex.getCause().getMessage().matches(".*Unauthorized user.*")) {
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
            sendToURL("xmldb:exist://guest:guest@/db/system/toDB_NotAuthorized.xml", "build.xml");
            fail("Not authorized: Exception expected");
        } catch (Exception ex) {
            if (!ex.getCause().getMessage().matches(".*not allowed to write to collection.*")) {
                ex.printStackTrace();
                LOG.error(ex);
                fail(ex.getMessage());
            }
        }
    }

    @Test
    public void fromDB_NotAuthorized() {
        System.out.println("fromDB_NotAuthorized");
        try {
            OutputStream os = new ByteArrayOutputStream();
            getFromURL("xmldb:exist://guest:guest@/db/system/toDB_NotAuthorized.xml", os);
            os.close();
            fail("Not authorized: Exception expected");
        } catch (Exception ex) {
            if (!ex.getCause().getMessage().matches(".*Permission denied to read collection.*")) {
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
            sendToURL("xmldb:exist:///db/manifest.txt", "manifest.mf");
        } catch (Exception ex) {
            LOG.error(ex);
            fail(ex.getMessage());
        }
    }

    @Test
    public void fromDB_binaryDoc() {
        System.out.println("fromDB_binaryDoc");
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            getFromURL("xmldb:exist:///db/manifest.txt", baos);
            baos.close();
            assertTrue(baos.size() > 0);
            assertEquals(85, baos.size());
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error(ex);
            fail(ex.getMessage());
        }
    }
}
