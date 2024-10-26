package com.entelience.test.test13servlet;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.List;
import java.util.ArrayList;
import com.entelience.soap.soapBaseHelper;
import com.entelience.util.Config;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

public class test02ExcelServlet extends com.entelience.test.OurDbTestCase {

    private static String localServer = null;

    private static int localPort = 0;

    private static String baseUrl = null;

    private static String xlsURL = null;

    @Test
    public void test00_setup() throws Exception {
        localServer = Config.getProperty(db, "com.entelience.esis.web.localServerAddress", "localhost");
        localPort = Config.getProperty(db, "com.entelience.esis.web.localServerPort", 8080);
        baseUrl = "http://" + localServer + ":" + Integer.toString(localPort);
        System.out.println("Using : " + baseUrl);
        xlsURL = baseUrl + "/esis/html/portal/xls/Portal.xls";
    }

    /**
	 * Just checks that the servlet is there
	 */
    @Test
    public void test01_ok_failed_500() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(xlsURL);
            HttpResponse response = client.execute(post);
            assertEquals("failed code for ", 500, response.getStatusLine().getStatusCode());
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
	 * Just checks that the servlet is there
	 */
    @Test
    public void test02_ok_simple() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            HttpPost httpost = new HttpPost(xlsURL);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("wsName", "getGlobalVulnCountEvolution"));
            nvps.add(new BasicNameValuePair("p1", "chart"));
            nvps.add(new BasicNameValuePair("p2", "week"));
            nvps.add(new BasicNameValuePair("p3", "all"));
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = client.execute(httpost);
            assertEquals("failed code for ", 200, response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            assertNotNull("page empty for ", entity);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}
