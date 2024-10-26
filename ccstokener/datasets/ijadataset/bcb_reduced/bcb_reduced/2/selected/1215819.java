package com.ail.coretest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import com.ail.core.Functions;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * The core provides a number of URL handlers to simplify access to resources. The tests
 * defined here test those handlers. 
 * @version $Revision: 1.2 $
 * @state $State: Exp $
 * @date $Date: 2005/07/16 10:23:24 $
 * @source $Source:
 *         /home/bob/CVSRepository/projects/core/test/com/ail/coretest/TestTypeXpath.java,v $
 */
public class TestUrlHandlers extends TestCase {

    /** Constructs a test case with the given name. */
    public TestUrlHandlers(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestUrlHandlers.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    protected void setUp() throws Exception {
        System.setProperty("java.protocol.handler.pkgs", "com.ail.core.urlhandler");
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
    public void testClasspathUrlHandlerGoodUrl() throws Exception {
        URL url = null;
        char[] buf = new char[18];
        url = new URL("classpath://com.ail.coretest/TestUrlContent.xml");
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
    public void testClasspathUrlHandlerBadUrl() throws Exception {
        URL url = null;
        url = new URL("classpath://com.ail.coretest/TestUrlContentThatDoesNotExist.xml");
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
    public void testAlfrescoUrlAccess() throws Exception {
        URL url = null;
        try {
            url = new URL("http://localhost:8080/alfresco/download/direct?path=/Company%20Home/Data%20Dictionary/Email%20Templates/invite_user_email.ftl");
            Functions.loadUrlContentAsString(url);
            fail("got content even though we didn't pass a vaid ticket in");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("response code: 500"));
        }
        url = new URL("http://localhost:8080/alfresco/service/api/login?u=admin&pw=admin");
        String rawTicketResponse = Functions.loadUrlContentAsString(url);
        assertTrue(rawTicketResponse.indexOf("<ticket>") > 0);
        String ticket = rawTicketResponse.substring(rawTicketResponse.indexOf("<ticket>") + 8, rawTicketResponse.lastIndexOf("<"));
        url = new URL("http://localhost:8080/alfresco/download/direct?path=/Company%20Home/Data%20Dictionary/Email%20Templates/invite_user_email.ftl&ticket=" + ticket);
        Functions.loadUrlContentAsString(url);
    }

    /**
     * Test raw access to the alfresco repository. This test uses the same method of access
     * as the alfresco content URL handler uses.
     * @throws Exception
     */
    public void testProdctUrlAccess() throws Exception {
        URL url = null;
        try {
            url = new URL("product://localhost:8080/Demo/Demo/ContentThatDoesNotExist.html");
            Functions.loadUrlContentAsString(url);
            fail("got content which doesn't exist!");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("response code: 500"));
        }
        url = new URL("product://localhost:8080/Demo/Demo/Welcome.html");
        Functions.loadUrlContentAsString(url);
    }
}
