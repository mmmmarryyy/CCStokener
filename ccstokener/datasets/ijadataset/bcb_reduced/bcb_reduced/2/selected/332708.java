package net.sf.iqser.plugin.web.html;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import net.sf.iqser.plugin.web.html.HTMLContentProvider;
import org.apache.log4j.PropertyConfigurator;
import com.iqser.core.model.Content;
import junit.framework.TestCase;

/**
 * A general test case for HTML Content Provider
 * 
 * @author Joerg Wurzer
 *
 */
public class HTMLContentProviderTest extends TestCase {

    /** Content provider to test */
    private HTMLContentProvider provider = null;

    protected void setUp() throws Exception {
        super.setUp();
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/html-plugin/src/test/res/log4j.properties");
        Properties initParams = new Properties();
        initParams.setProperty("database", "localhost/crawler");
        initParams.setProperty("username", "root");
        initParams.setProperty("password", "master");
        initParams.setProperty("item-node-filter", "TABLE,*,*,*");
        initParams.setProperty("attribute-node-filter", "LI,*,*,*;P,*,*,*");
        initParams.setProperty("P", "Name");
        initParams.setProperty("LI", "Description");
        provider = new HTMLContentProvider();
        provider.setId("net.sf.iqser.plugin.web.html");
        provider.setType("Web Page");
        provider.setInitParams(initParams);
        provider.init();
    }

    protected void tearDown() throws Exception {
        provider.destroy();
        super.tearDown();
    }

    public void testGetContentString() {
        Content c = provider.getContent("http://www.designerfashion.de/Seiten/r2-Felljacke.html");
        assertNotNull(c);
        assertEquals("Web Page", c.getType());
        assertEquals("net.sf.iqser.plugin.web.html", c.getProvider());
        assertTrue(c.getContentUrl().endsWith(".html") || c.getContentUrl().endsWith(".htm"));
        assertEquals(8, c.getAttributes().size());
        assertEquals("Name", c.getAttributes().iterator().next().getName());
    }

    public void testGetContentInputStream() {
        try {
            URL url = new URL("http://www.designerfashion.de/Seiten/r2-Felljacke.html");
            InputStream in = url.openStream();
            Content c = provider.getContent(in);
            assertNotNull(c);
            assertEquals("Web Page", c.getType());
            assertEquals("net.sf.iqser.plugin.web.html", c.getProvider());
            assertEquals(8, c.getAttributes().size());
            assertEquals("Name", c.getAttributes().iterator().next().getName());
        } catch (MalformedURLException e) {
            fail("Malformed URL - " + e.getMessage());
        } catch (IOException e) {
            fail("Couldn't read source - " + e.getMessage());
        }
    }

    public void testGetBinaryData() {
        Content c = new Content();
        c.setContentUrl("http://www.designerfashion.de/Seiten/r2-Felljacke.html");
        byte[] byteArr = provider.getBinaryData(c);
        assertNotNull(byteArr);
        String page = new String(byteArr);
        assertTrue(page.startsWith("<HTML>  \n<HEAD>\n  " + "<META NAME=\"GENERATOR\" CONTENT=\"Adobe PageMill 3.0 Macintosh\">\n  " + "<TITLE>+++ streetnightwear +++</TITLE>\n</HEAD>"));
    }
}
