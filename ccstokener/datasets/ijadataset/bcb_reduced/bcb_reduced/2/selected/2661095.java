package net.sf.iqser.plugin.web.html;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import com.iqser.core.model.Content;
import junit.framework.TestCase;

public class McKinleyContentProviderTest extends TestCase {

    /** Content provider to test */
    private HTMLContentProvider provider = null;

    protected void setUp() throws Exception {
        super.setUp();
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/html-plugin/src/test/res/log4j.properties");
        Properties initParams = new Properties();
        initParams.setProperty("database", "localhost/crawler");
        initParams.setProperty("username", "root");
        initParams.setProperty("password", "master");
        initParams.setProperty("item-node-filter", "div,class,section,*");
        initParams.setProperty("attribute-node-filter", "li,*,*,*;h1,class,title,*");
        initParams.setProperty("key-attributes", "[Title] [Metadata.1] [Metadata.1] [Metadata.2] [Metadata.3] [Metadata.4]");
        initParams.setProperty("title", "Title");
        initParams.setProperty("LI", "Metadata");
        initParams.setProperty("charset", "UTF-8");
        provider = new HTMLContentProvider();
        provider.setId("ie.morganmckinley.offerings");
        provider.setType("Job Offering");
        provider.setInitParams(initParams);
        provider.init();
    }

    protected void tearDown() throws Exception {
        provider.destroy();
        super.tearDown();
    }

    public void testGetContentString() {
        Content c = provider.getContent("http://www.morganmckinley.ie/job/897859/finance-manager-cork");
        assertNotNull(c);
        assertEquals("Job Offering", c.getType());
        assertEquals("ie.morganmckinley.offerings", c.getProvider());
        assertTrue(c.getContentUrl().endsWith("finance-manager-cork"));
        assertEquals(41, c.getAttributes().size());
        assertEquals("Title", c.getAttributes().iterator().next().getName());
        assertTrue(c.getAttributeByName("Title").isKey());
    }

    public void testGetContentInputStream() {
        try {
            URL url = new URL("http://www.morganmckinley.ie/job/897859/finance-manager-cork");
            InputStream in = url.openStream();
            Content c = provider.getContent(in);
            assertNotNull(c);
            assertEquals("Job Offering", c.getType());
            assertEquals("ie.morganmckinley.offerings", c.getProvider());
            assertEquals(43, c.getAttributes().size());
            assertEquals("Title", c.getAttributes().iterator().next().getName());
        } catch (MalformedURLException e) {
            fail("Malformed URL - " + e.getMessage());
        } catch (IOException e) {
            fail("Couldn't read source - " + e.getMessage());
        }
    }

    public void testGetBinaryData() {
        Content c = new Content();
        c.setContentUrl("http://www.morganmckinley.ie/job/897859/finance-manager-cork");
        byte[] byteArr = provider.getBinaryData(c);
        assertNotNull(byteArr);
        String page = new String(byteArr);
        assertTrue(page.contains("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "));
    }
}
