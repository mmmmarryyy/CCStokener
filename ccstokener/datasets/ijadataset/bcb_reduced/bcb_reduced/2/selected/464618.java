package com.entelience.test.test12jsp;

import org.junit.*;
import static org.junit.Assert.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import com.entelience.directory.PeopleFactory;
import com.entelience.soap.soapBaseHelper;
import com.entelience.util.Config;
import com.entelience.util.HttpQuick;
import com.entelience.util.Logs;
import com.entelience.raci.module.RaciVrt;
import com.entelience.objects.raci.RACI;

public class test01JspBasic extends com.entelience.test.OurDbTestCase {

    private static String localServer = null;

    private static int localPort = 0;

    private static String baseUrl = null;

    private static Integer userId = null;

    @Test
    public void test00a_setup() throws Exception {
        Logs.logMethodName();
        localServer = Config.getProperty(db, "com.entelience.esis.web.localServerAddress", "localhost");
        localPort = Config.getProperty(db, "com.entelience.esis.web.localServerPort", 8080);
        baseUrl = "http://" + localServer + ":" + Integer.toString(localPort);
        System.out.println("Using : " + baseUrl);
    }

    /**
	 * Ensure esis user is C in vuln module
	 */
    @Test
    public void test00b_init() throws Exception {
        Logs.logMethodName();
        try {
            db.begin();
            userId = PeopleFactory.lookupUserName(db, "esis");
            RaciVrt raciVrt = new RaciVrt(db);
            if (!raciVrt.isRACI(db, userId.intValue())) {
                RACI raci = new RACI(userId.intValue(), raciVrt.getRaciObjectId(), false, false, true, false);
                raciVrt.addRaci(db, raci, PeopleFactory.anonymousId);
            }
            db.commit();
        } finally {
            db.safeRollback();
        }
    }

    /**
	 * Just checks that the index pages are there
	 */
    @Test
    public void test01_basic() throws Exception {
        Logs.logMethodName();
        String[] jspURLs = { baseUrl + "/esis/module.jsp", baseUrl + "/esis/html/vrt/index.jsp", baseUrl + "/esis/html/admin/index.jsp", baseUrl + "/esis/html/asset/index.jsp", baseUrl + "/esis/html/audit/index.jsp", baseUrl + "/esis/html/portal/index.jsp", baseUrl + "/esis/html/risk/index.jsp", baseUrl + "/esis/html/mim/index.jsp", baseUrl + "/esis/reports.jsp" };
        for (int i = 0; i < jspURLs.length; ++i) {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(jspURLs[i]);
            HttpResponse response = client.execute(get);
            try {
                assertEquals("failed code for " + jspURLs[i], 200, response.getStatusLine().getStatusCode());
                assertNotNull("page empty for " + jspURLs[i], response.getEntity());
                assertTrue("0 content length " + jspURLs[i], response.getEntity().getContentLength() > 0L);
            } finally {
                client.getConnectionManager().shutdown();
            }
        }
    }

    /**
	 * Checks that 404 works
	 */
    @Test
    public void test02_fail_bad_url() throws Exception {
        Logs.logMethodName();
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(baseUrl + "/esis/html/vrt/foobar.jsp");
        HttpResponse response = client.execute(get);
        try {
            assertEquals("failed code", 404, response.getStatusLine().getStatusCode());
            assertNotNull("page empty", response.getEntity());
            assertTrue("0 content length ", response.getEntity().getContentLength() > 0L);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
	 * Should failed if the authentication header is missing
	 */
    @Test
    public void test03_fail_session_id() throws Exception {
        Logs.logMethodName();
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(baseUrl + "/esis/html/vrt/vuln_email.jsp?e_vulnerability_id=1080");
        HttpResponse response = client.execute(get);
        try {
            assertEquals("failed code", 500, response.getStatusLine().getStatusCode());
            assertNotNull("page empty", response.getEntity());
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
	 * Should failed with a bad session id
	 */
    @Test
    public void test04_fail_bad_session_id() throws Exception {
        Logs.logMethodName();
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(baseUrl + "/esis/html/vrt/vuln_email.jsp?e_vulnerability_id=1080");
        get.addHeader("Session-Id", "ABCD");
        HttpResponse response = client.execute(get);
        try {
            assertEquals("failed code", 500, response.getStatusLine().getStatusCode());
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
	 * Should failed with a bad session id
	 */
    @Test
    public void test05_fail_bad_session_id() throws Exception {
        Logs.logMethodName();
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(baseUrl + "/esis/html/vrt/vuln_email.jsp?e_vulnerability_id=1080");
        get.addHeader("Session-Id", "");
        HttpResponse response = client.execute(get);
        try {
            assertEquals("failed code", 500, response.getStatusLine().getStatusCode());
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
	 * Should failed with a bad session id
	 */
    @Test
    public void test06_fail_bad_session_id() throws Exception {
        Logs.logMethodName();
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(baseUrl + "/esis/html/vrt/vuln_email.jsp?e_vulnerability_id=1080");
        get.addHeader("Session-Id", Long.toString(Long.MAX_VALUE) + "99999");
        HttpResponse response = client.execute(get);
        try {
            assertEquals("failed code", 500, response.getStatusLine().getStatusCode());
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
	 * Should failed with a bad session id
	 */
    @Test
    public void test07_fail_bad_session_id() throws Exception {
        Logs.logMethodName();
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(baseUrl + "/esis/html/vrt/vuln_email.jsp?e_vulnerability_id=1080");
        get.addHeader("Session-Id", Long.toString(Long.MAX_VALUE));
        HttpResponse response = client.execute(get);
        try {
            assertEquals("failed code", 500, response.getStatusLine().getStatusCode());
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    @Test
    public void test08_http_on_local_authenticated_jsp() throws Exception {
        Logs.logMethodName();
        assertNotNull(userId);
        String sessionId = null;
        try {
            String url = baseUrl + "/esis/html/vrt/new_vulnerabilities.jsp?page=1";
            sessionId = soapBaseHelper.login(db, userId.intValue());
            assertNotNull(sessionId);
            String urlContent = HttpQuick.downloadAuthenticatedJsp(db, url, sessionId);
            assertNotNull(urlContent);
            assertTrue(urlContent.length() > 0);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            soapBaseHelper.logout(db, sessionId);
        }
    }
}
