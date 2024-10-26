package com.ail.core.urlhandler;

import static org.junit.Assert.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import com.ail.core.Functions;

/**
 * The core provides a number of URL handlers to simplify access to resources. The tests
 * defined here test those handlers. 
 */
public class TestUrlHandlers {

    @Before
    public void setUp() throws Exception {
        System.setProperty("java.protocol.handler.pkgs", "com.ail.core.urlhandler");
    }

    /**
     * Test raw access to the alfresco repository. This test uses the same method of access
     * as the alfresco content URL handler uses.
     * @throws Exception
     */
    @Test
    public void testProdctUrlAccess() throws Exception {
        URL url = null;
        try {
            url = new URL("product://localhost:8080/Demo/Demo/ContentThatDoesNotExist.html");
            Functions.loadUrlContentAsString(url);
            fail("got content which doesn't exist!");
        } catch (FileNotFoundException e) {
        } catch (Throwable t) {
            fail("Caught unexpected " + t.getClass().getName());
        }
        url = new URL("product://localhost:8080/Demo/Demo/Welcome");
        Functions.loadUrlContentAsString(url);
    }

    /**
     * This test checks that a URL pointing at an existing resource correctly opens that
     * resource.
     * <ul>
     * <li>Open a stream to the URL "classpath://TestUrlContent.xml"</li>
     * <li>Count the number of bytes in available</li>
     * <li>If the count is anything by 14, fail</li>
     * <li>Fail if any exceptions are thrown</li>
     * </ul>
     * @throws Exception
     */
    @Test
    public void testClasspathUrlHandlerGoodUrl() throws Exception {
        URL url = null;
        char[] buf = new char[18];
        url = new URL("classpath://com.ail.core.urlhandler/TestUrlContent.xml");
        InputStream in = url.openStream();
        InputStreamReader isr = new InputStreamReader(in);
        isr.read(buf, 0, 18);
        assertEquals("<root>hello</root>", new String(buf));
    }

    /**
     * This test checks that a URL pointing at an existing resource correctly opens that
     * resource.
     * <ul>
     * <li>Open a stream to the URL "classpath://TestUrlContent.xml"</li>
     * <li>Count the number of bytes in available</li>
     * <li>If the count is anything by 14, fail</li>
     * <li>Fail if any exceptions are thrown</li>
     * </ul>
     * @throws Exception
     */
    @Test
    public void testClasspathUrlHandlerBadUrl() throws Exception {
        URL url = null;
        url = new URL("classpath://com.ail.core/TestUrlContentThatDoesNotExist.xml");
        try {
            url.openStream();
            fail("Open a resource that doesn't exist!");
        } catch (FileNotFoundException e) {
        }
    }

    /**
     * Test raw access to the alfresco repository. This test uses the same method of access
     * as the alfresco content URL handler uses.
     * @throws Exception
     */
    @Test
    public void testAlfrescoUrlAccessWithoutATicket() throws Exception {
        URL url = null;
        try {
            url = new URL("http://localhost:8080/alfresco/download/direct?path=/Company%20Home/Data%20Dictionary/Email%20Templates/invite_user_email.ftl");
            String p = Functions.loadUrlContentAsString(url);
            System.out.println("res:" + p);
            fail("got content even though we didn't pass a vaid ticket in");
        } catch (IOException e) {
            assertTrue("expected error to contain: 'HTTP response code: 401', but message was: '" + e.getMessage() + "'", e.getMessage().contains("HTTP response code: 401"));
        }
    }

    @Test
    public void testAlfrescoUrlAccessWithATicket() throws Exception {
        URL url = null;
        url = new URL("http://localhost:8080/alfresco/service/api/login?u=admin&pw=admin");
        String rawTicketResponse = Functions.loadUrlContentAsString(url);
        assertTrue(rawTicketResponse.indexOf("<ticket>") > 0);
        String ticket = rawTicketResponse.substring(rawTicketResponse.indexOf("<ticket>") + 8, rawTicketResponse.lastIndexOf("<"));
        url = new URL("http://localhost:8080/alfresco/download/direct?path=/Company%20Home/Data%20Dictionary/Email%20Templates/invite_user_email.ftl&ticket=" + ticket);
        Functions.loadUrlContentAsString(url);
    }

    /**
     * Test raw access to the alfresco repository with and without defining the language. This test uses the same method of access
     * as the alfresco content URL handler uses.
     * @throws Exception
     */
    @Test
    public void testAlfrescoUrlAccessWithLocale() throws Exception {
        URL url = null;
        url = new URL("http://localhost:8080/alfresco/service/api/login?u=admin&pw=admin");
        String rawTicketResponse = Functions.loadUrlContentAsString(url);
        assertTrue(rawTicketResponse.indexOf("<ticket>") > 0);
        String ticket = rawTicketResponse.substring(rawTicketResponse.indexOf("<ticket>") + 8, rawTicketResponse.lastIndexOf("<"));
        url = new URL("http://localhost:8080/alfresco/download/direct?path=/Company%20Home/Product/Demo/Demo/Welcome&ticket=" + ticket);
        assertEquals("   Hello World!", Functions.loadUrlContentAsString(url));
        url = new URL("http://localhost:8080/alfresco/download/direct?path=/Company%20Home/Product/Demo/Demo/Welcome&ticket=" + ticket + "&language=de");
        assertEquals("Hallo Welt!", Functions.loadUrlContentAsString(url));
        url = new URL("http://localhost:8080/alfresco/download/direct?path=/Company%20Home/Product/Demo/Demo/Welcome&ticket=" + ticket + "&language=fr");
        assertEquals("   Hello World!", Functions.loadUrlContentAsString(url));
    }

    /**
     * Test product access to the alfresco repository with and without defining the language.
     * @throws Exception
     */
    @Test
    public void testProductUrlAccessWithLocale() throws Exception {
        URL url = null;
        com.ail.core.ThreadLocale.setThreadLocale(Locale.ENGLISH);
        url = new URL("product://localhost:8080/Demo/Demo/Welcome");
        assertEquals("   Hello World!", Functions.loadUrlContentAsString(url));
        com.ail.core.ThreadLocale.setThreadLocale(Locale.GERMAN);
        url = new URL("product://localhost:8080/Demo/Demo/Welcome");
        assertEquals("Hallo Welt!", Functions.loadUrlContentAsString(url));
        com.ail.core.ThreadLocale.setThreadLocale(Locale.FRENCH);
        url = new URL("product://localhost:8080/Demo/Demo/Welcome");
        assertEquals("   Hello World!", Functions.loadUrlContentAsString(url));
    }
}
