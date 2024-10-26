package com.entelience.test.test13servlet;

import org.junit.*;
import static org.junit.Assert.*;
import com.entelience.soap.soapBaseHelper;
import com.entelience.util.Config;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

public class test01XmlLang extends com.entelience.test.OurDbTestCase {

    private static String localServer = null;

    private static int localPort = 0;

    private static String baseUrl = null;

    private static Integer userId = null;

    private static String xmlLangURLs = null;

    @Test
    public void test00_setup() throws Exception {
        localServer = Config.getProperty(db, "com.entelience.esis.web.localServerAddress", "localhost");
        localPort = Config.getProperty(db, "com.entelience.esis.web.localServerPort", 8080);
        baseUrl = "http://" + localServer + ":" + Integer.toString(localPort);
        System.out.println("Using : " + baseUrl);
        xmlLangURLs = baseUrl + "/esis/html/langxml?entity=";
    }

    /**
	 * Just checks that the language files are there
	 */
    @Test
    public void test01_lang_default() throws Exception {
        String[] entities = { "admin", "audits", "asset", "esis", "explorer", "identity", "riskregister", "risks", "vulnerabilities" };
        for (String entity : entities) {
            DefaultHttpClient client = new DefaultHttpClient();
            try {
                HttpGet get = new HttpGet(xmlLangURLs + entity);
                HttpResponse response = client.execute(get);
                assertEquals("failed code for " + entity, 200, response.getStatusLine().getStatusCode());
                assertNotNull("page empty for " + entity, response.getEntity());
            } finally {
                client.getConnectionManager().shutdown();
            }
        }
    }

    @Test
    public void test02_lang_set() throws Exception {
        String[] entities = { "admin", "audits", "asset", "esis", "explorer", "identity", "riskregister", "risks", "vulnerabilities" };
        for (String entity : entities) {
            DefaultHttpClient client = new DefaultHttpClient();
            try {
                HttpGet get = new HttpGet(xmlLangURLs + entity + "&company=def&lang=fr");
                HttpResponse response = client.execute(get);
                assertEquals("failed code for " + entity, 200, response.getStatusLine().getStatusCode());
                assertNotNull("page empty for " + entity, response.getEntity());
            } finally {
                client.getConnectionManager().shutdown();
            }
        }
    }

    /**
	 * empty file
	 */
    @Test
    public void test03_fail_bad_entity() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(xmlLangURLs + "foobar");
        HttpResponse response = client.execute(get);
        try {
            assertNotNull("page empty", response.getEntity());
            String content = EntityUtils.toString(response.getEntity());
            assertTrue(content.contains("error"));
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
	 * empty file
	 */
    @Test
    public void test04_fail_bad_company() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(xmlLangURLs + "admin&company=foobar");
        HttpResponse response = client.execute(get);
        try {
            assertNotNull("page empty", response.getEntity());
            String content = EntityUtils.toString(response.getEntity());
            assertTrue(content.contains("error"));
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}
