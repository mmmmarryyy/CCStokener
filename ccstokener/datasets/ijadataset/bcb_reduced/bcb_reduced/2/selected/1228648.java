package net.sf.iqser.plugin.web.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import net.sf.iqser.plugin.web.pdf.PDFContentProvider;
import org.apache.log4j.PropertyConfigurator;
import com.iqser.core.model.Content;
import junit.framework.TestCase;

/**
 * A general test case for the PDF Content Provider
 * 
 * @author Joerg Wurzer
 *
 */
public class PDFContentProviderTest extends TestCase {

    /** Content provider to test */
    private PDFContentProvider provider = null;

    protected void setUp() throws Exception {
        super.setUp();
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/html-plugin/src/test/res/log4j.properties");
        Properties initParams = new Properties();
        initParams.setProperty("database", "localhost/crawler");
        initParams.setProperty("username", "root");
        initParams.setProperty("password", "master");
        provider = new PDFContentProvider();
        provider.setId("net.sf.iqser.plugin.web.pdf");
        provider.setType("PDF Document");
        provider.setInitParams(initParams);
        provider.init();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetContentString() {
        Content c = provider.getContent("http://www.wurzer.org/" + "Homepage/Publikationen/Eintrage/2009/10/7_Wissen_dynamisch_organisieren_files/" + "KnowTech%202009%20-%20Wissen%20dynamisch%20organisieren.pdf");
        assertNotNull(c);
        assertTrue(!c.getFulltext().isEmpty());
        assertTrue(c.getModificationDate() < System.currentTimeMillis());
        assertTrue(c.getAttributes().size() > 0);
        assertEquals("KnowTech 2009 - Wissen dynamisch organisieren", c.getAttributeByName("Title").getValue());
        assertEquals("Joerg Wurzer", c.getAttributeByName("Author").getValue());
        assertEquals("Pages", c.getAttributeByName("Creator").getValue());
        assertNull(c.getAttributeByName("Keywords"));
        assertTrue(c.getFulltext().startsWith("Wissen dynamisch organisieren"));
        assertTrue(c.getAttributeByName("Author").isKey());
        assertTrue(!c.getAttributeByName("Producer").isKey());
    }

    public void testGetContentInputStream() {
        URL url;
        try {
            url = new URL("http://www.wurzer.org/" + "Homepage/Publikationen/Eintrage/2009/10/7_Wissen_dynamisch_organisieren_files/" + "KnowTech%202009%20-%20Wissen%20dynamisch%20organisieren.pdf");
            InputStream in = url.openStream();
            Content c = provider.getContent(in);
            assertNotNull(c);
            assertTrue(!c.getFulltext().isEmpty());
            assertTrue(c.getModificationDate() < System.currentTimeMillis());
            assertTrue(c.getAttributes().size() > 0);
            assertEquals("KnowTech 2009 - Wissen dynamisch organisieren", c.getAttributeByName("Title").getValue());
            assertEquals("Joerg Wurzer", c.getAttributeByName("Author").getValue());
            assertEquals("Pages", c.getAttributeByName("Creator").getValue());
            assertNull(c.getAttributeByName("Keywords"));
            assertTrue(c.getFulltext().startsWith("Wissen dynamisch organisieren"));
            assertTrue(c.getAttributeByName("Author").isKey());
            assertTrue(!c.getAttributeByName("Producer").isKey());
        } catch (MalformedURLException e) {
            fail("Malformed url - " + e.getMessage());
        } catch (IOException e) {
            fail("Couldn't read file - " + e.getMessage());
        }
    }

    public void testGetBinaryData() {
        Content c = new Content();
        c.setContentUrl("http://www.wurzer.org/" + "Homepage/Publikationen/Eintrage/2009/10/7_Wissen_dynamisch_organisieren_files/" + "KnowTech%202009%20-%20Wissen%20dynamisch%20organisieren.pdf");
        byte[] byteArr = provider.getBinaryData(c);
        assertNotNull(byteArr);
    }
}
