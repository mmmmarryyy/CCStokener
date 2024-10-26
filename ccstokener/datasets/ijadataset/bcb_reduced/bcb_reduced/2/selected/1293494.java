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
 * A text case for tariff information of German custom
 * 
 * @author Joerg Wurzer
 * 
 */
public class CustomTariffContentProviderTest extends TestCase {

    /** Content provider to test */
    private HTMLContentProvider provider = null;

    protected void setUp() throws Exception {
        super.setUp();
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/html-plugin/src/test/res/log4j.properties");
        Properties initParams = new Properties();
        initParams.setProperty("database", "localhost/crawler");
        initParams.setProperty("username", "root");
        initParams.setProperty("password", "master");
        initParams.setProperty("item-node-filter", "html,*,*,*");
        initParams.setProperty("attribute-node-filter", "title,*,*,*;meta,*,*,*");
        initParams.setProperty("key-attributes", "[TARIC 10011000] [Description] [Keywords]");
        initParams.setProperty("TITLE", "Name");
        initParams.setProperty("description", "Description");
        initParams.setProperty("keywords", "Keywords");
        provider = new HTMLContentProvider();
        provider.setId("com.iqser.plugin.web.custom");
        provider.setType("Custom Tariff");
        provider.setInitParams(initParams);
        provider.init();
    }

    protected void tearDown() throws Exception {
        provider.destroy();
        super.tearDown();
    }

    public void testGetContentString() {
        Content c = provider.getContent("http://www.zolltarifnummern.de/2010_de/10011000.html");
        assertNotNull(c);
        assertEquals("Custom Tariff", c.getType());
        assertEquals("com.iqser.plugin.web.custom", c.getProvider());
        assertTrue(c.getContentUrl().endsWith(".html") || c.getContentUrl().endsWith(".htm"));
        assertEquals(5, c.getAttributes().size());
        assertEquals("TARIC 10011000", c.getAttributes().iterator().next().getName());
        assertTrue(c.getAttributeByName("TARIC 10011000").isKey());
    }

    public void testGetContentInputStream() {
        try {
            URL url = new URL("http://www.zolltarifnummern.de/2010_de/10011000.html");
            InputStream in = url.openStream();
            Content c = provider.getContent(in);
            assertNotNull(c);
            assertEquals("Custom Tariff", c.getType());
            assertEquals("com.iqser.plugin.web.custom", c.getProvider());
            assertEquals(5, c.getAttributes().size());
            assertEquals("TARIC 10011000", c.getAttributes().iterator().next().getName());
        } catch (MalformedURLException e) {
            fail("Malformed URL - " + e.getMessage());
        } catch (IOException e) {
            fail("Couldn't read source - " + e.getMessage());
        }
    }

    public void testGetBinaryData() {
        Content c = new Content();
        c.setContentUrl("http://www.zolltarifnummern.de/2010_de/10011000.html");
        byte[] byteArr = provider.getBinaryData(c);
        assertNotNull(byteArr);
        String page = new String(byteArr);
        assertTrue(page.startsWith("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"de\" lang=\"de\" >\n" + "<head>\n" + "  <title>TARIC 10011000: Hartweizen</title>"));
    }
}
