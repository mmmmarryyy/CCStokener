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
 * A test case for a German vendor search engine
 * 
 * @author Joerg Wurzer
 *
 */
public class WlWContentProviderTest extends TestCase {

    /** Content provider to test */
    private HTMLContentProvider provider = null;

    protected void setUp() throws Exception {
        super.setUp();
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/html-plugin/src/test/res/log4j.properties");
        Properties initParams = new Properties();
        initParams.setProperty("database", "localhost/crawler");
        initParams.setProperty("username", "root");
        initParams.setProperty("password", "master");
        initParams.setProperty("item-node-filter", "*,class,subcontentFirmeninformation,*");
        initParams.setProperty("attribute-node-filter", "*,class,firmierung,*;tr,*,*,*;*,class,ansprechpartner angebot,*");
        initParams.setProperty("key-attributes", "[Name] [Description] [Contact]");
        initParams.setProperty("firmierung", "Name");
        initParams.setProperty("ansprechpartner", "Description");
        initParams.setProperty("TR", "Address");
        initParams.setProperty("charset", "UTF-8");
        provider = new HTMLContentProvider();
        provider.setId("com.iqser.plugin.web.wlw");
        provider.setType("Vendor");
        provider.setInitParams(initParams);
        provider.init();
    }

    protected void tearDown() throws Exception {
        provider.destroy();
        super.tearDown();
    }

    public void testGetContentString() {
        Content c = provider.getContent("http://www.wlw.de/sse/MainServlet?anzeige=vollanzeige&land=DE&sprache=de&firmaid=278527&klobjid=85340&ccode=310131180163&suchbegriff=Maschinen");
        assertNotNull(c);
        assertEquals("Vendor", c.getType());
        assertEquals("com.iqser.plugin.web.wlw", c.getProvider());
        assertTrue(c.getContentUrl().endsWith("Maschinen"));
        assertEquals(13, c.getAttributes().size());
        assertEquals("Name", c.getAttributes().iterator().next().getName());
        assertTrue(c.getAttributeByName("Name").isKey());
    }

    public void testGetContentInputStream() {
        try {
            URL url = new URL("http://www.wlw.de/sse/MainServlet?anzeige=vollanzeige&land=DE&sprache=de&firmaid=278527&klobjid=85340&ccode=310131180163&suchbegriff=Maschinen");
            InputStream in = url.openStream();
            Content c = provider.getContent(in);
            assertNotNull(c);
            assertEquals("Vendor", c.getType());
            assertEquals("com.iqser.plugin.web.wlw", c.getProvider());
            assertEquals(13, c.getAttributes().size());
            assertEquals("Name", c.getAttributes().iterator().next().getName());
        } catch (MalformedURLException e) {
            fail("Malformed URL - " + e.getMessage());
        } catch (IOException e) {
            fail("Couldn't read source - " + e.getMessage());
        }
    }

    public void testGetBinaryData() {
        Content c = new Content();
        c.setContentUrl("http://www.wlw.de/sse/MainServlet?anzeige=vollanzeige&land=DE&sprache=de&firmaid=278527&klobjid=85340&ccode=310131180163&suchbegriff=Maschinen");
        byte[] byteArr = provider.getBinaryData(c);
        assertNotNull(byteArr);
        String page = new String(byteArr);
        assertTrue(page.startsWith("\n" + "      <!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" + "    <html><head><title>&#8222;Wer liefert was?&#8220; 2011 Firmeninformation</title>"));
    }
}
