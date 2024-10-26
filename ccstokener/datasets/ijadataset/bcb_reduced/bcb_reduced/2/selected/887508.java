package com.android.unit_tests;

import android.content.ContentResolver;
import android.net.http.AndroidHttpClient;
import android.provider.Checkin;
import android.provider.Settings;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import com.google.android.collect.Lists;
import com.google.android.net.GoogleHttpClient;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import java.io.IOException;

/** Unit test for {@link GoogleHttpClient}. */
public class GoogleHttpClientTest extends AndroidTestCase {

    private TestHttpServer mServer;

    private String mServerUrl;

    protected void setUp() throws Exception {
        mServer = new TestHttpServer();
        mServer.registerHandler("*", new HttpRequestHandler() {

            public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                String uri = request.getRequestLine().getUri();
                response.setEntity(new StringEntity(uri));
            }
        });
        mServer.start();
        mServerUrl = "http://localhost:" + mServer.getPort() + "/";
    }

    protected void tearDown() throws Exception {
        if (mServer != null) mServer.shutdown();
    }

    @LargeTest
    public void testThreadCheck() throws Exception {
        ContentResolver resolver = getContext().getContentResolver();
        GoogleHttpClient client = new GoogleHttpClient(resolver, "Test", false);
        try {
            HttpGet method = new HttpGet(mServerUrl);
            AndroidHttpClient.setThreadBlocked(true);
            try {
                client.execute(method);
                fail("\"thread forbids HTTP requests\" exception expected");
            } catch (RuntimeException e) {
                if (!e.toString().contains("forbids HTTP requests")) throw e;
            } finally {
                AndroidHttpClient.setThreadBlocked(false);
            }
            HttpResponse response = client.execute(method);
            assertEquals("/", EntityUtils.toString(response.getEntity()));
        } finally {
            client.close();
        }
    }

    @MediumTest
    public void testUrlRewriteRules() throws Exception {
        ContentResolver resolver = getContext().getContentResolver();
        GoogleHttpClient client = new GoogleHttpClient(resolver, "Test", false);
        Settings.Gservices.putString(resolver, "url:test", "http://foo.bar/ rewrite " + mServerUrl + "new/");
        Settings.Gservices.putString(resolver, "digest", mServerUrl);
        try {
            HttpGet method = new HttpGet("http://foo.bar/path");
            HttpResponse response = client.execute(method);
            String body = EntityUtils.toString(response.getEntity());
            assertEquals("/new/path", body);
        } finally {
            client.close();
        }
    }
}
