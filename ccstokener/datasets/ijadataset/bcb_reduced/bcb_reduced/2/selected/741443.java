package com.entelience.test.test13servlet;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import com.entelience.soap.soapBaseHelper;
import com.entelience.util.Config;
import com.entelience.directory.Company;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

public class test03CompanyLogo extends com.entelience.test.OurDbTestCase {

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
        xlsURL = baseUrl + "/esis/html/companylogo";
        db.begin();
        Company.removeLogo(db);
        db.commit();
    }

    /**
	 * Just checks that the servlet is there
	 */
    @Test
    public void test01_ok_failed_500_no_logo() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(xlsURL);
            HttpResponse response = client.execute(post);
            assertEquals("failed code for ", 500, response.getStatusLine().getStatusCode());
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    @Test
    public void test02_set_logo() throws Exception {
        db.begin();
        File file = new File("/opt/ESIS/share/ESIS/icon.gif");
        Company.setLogo(db, file);
        db.commit();
    }

    @Test
    public void test02_ok_200_logo() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(xlsURL);
            HttpResponse response = client.execute(post);
            assertEquals("failed code for ", 200, response.getStatusLine().getStatusCode());
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}
