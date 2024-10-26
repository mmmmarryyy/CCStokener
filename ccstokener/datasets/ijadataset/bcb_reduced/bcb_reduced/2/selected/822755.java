package net.sf.iqser.plugin.web.html;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import com.iqser.core.model.Content;
import junit.framework.TestCase;

public class AdminChContentProviderTest extends TestCase {

    /** Content provider to test */
    private HTMLContentProvider provider = null;

    protected void setUp() throws Exception {
        super.setUp();
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/html-plugin/src/test/res/log4j.properties");
        Properties initParams = new Properties();
        initParams.setProperty("database", "localhost/crawler");
        initParams.setProperty("username", "root");
        initParams.setProperty("password", "master");
        initParams.setProperty("item-node-filter", "*,id,webInnerContentSmall,*");
        initParams.setProperty("attribute-node-filter", "h1,*,*,*;h2,*,*,*");
        initParams.setProperty("key-attributes", "[Title] [Subtitle]");
        initParams.setProperty("webTitle", "Title");
        initParams.setProperty("webTitleH2", "Subtitle");
        initParams.setProperty("webTitle ", "Title");
        initParams.setProperty("charset", "UTF-8");
        provider = new HTMLContentProvider();
        provider.setId("ch.admin.topics");
        provider.setType("Web Content");
        provider.setInitParams(initParams);
        provider.init();
    }

    protected void tearDown() throws Exception {
        provider.destroy();
        super.tearDown();
    }

    public void testGetContentString() {
        Content c = provider.getContent("http://www.bk.admin.ch/themen/sprachen/00083/index.html?lang=en");
        assertNotNull(c);
        assertEquals("Web Content", c.getType());
        assertEquals("ch.admin.topics", c.getProvider());
        assertTrue(c.getContentUrl().endsWith("lang=en"));
        assertEquals(4, c.getAttributes().size());
        assertEquals("Title", c.getAttributes().iterator().next().getName());
        assertTrue(c.getAttributeByName("Title").isKey());
        assertNotNull(c.getFulltext());
    }

    public void testGetContentInputStream() {
        try {
            URL url = new URL("http://www.bk.admin.ch/themen/sprachen/00083/index.html?lang=en");
            InputStream in = url.openStream();
            Content c = provider.getContent(in);
            assertNotNull(c);
            assertEquals("Web Content", c.getType());
            assertEquals("ch.admin.topics", c.getProvider());
            assertEquals(4, c.getAttributes().size());
            assertEquals("Title", c.getAttributes().iterator().next().getName());
        } catch (MalformedURLException e) {
            fail("Malformed URL - " + e.getMessage());
        } catch (IOException e) {
            fail("Couldn't read source - " + e.getMessage());
        }
    }

    public void testGetBinaryData() {
        Content c = new Content();
        c.setContentUrl("http://www.admin.ch/org/polit/00054/index.html?lang=de");
        byte[] byteArr = provider.getBinaryData(c);
        assertNotNull(byteArr);
        String page = new String(byteArr);
        assertTrue(page.contains("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\""));
    }
}
