package com.simoncat.net;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.IOException;
import com.simoncat.vo.Server;
import com.simoncat.net.TestResult;
import java.util.Vector;

public class HttpTest {

    private String host;

    private int port;

    private String user;

    private String pwd;

    private String url;

    private String statusLine;

    private String answer;

    private InputStream content;

    private InputStreamReader isr;

    private LineNumberReader lnr;

    private int numAttempts;

    private long connTimeout;

    private Vector results = new Vector();

    public HttpTest(String host, int port, String user, String pwd, String url, int numAttempts, long connTimeout) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.pwd = pwd;
        this.url = url;
        this.numAttempts = numAttempts;
        this.connTimeout = connTimeout;
    }

    public HttpTest(Server server, int numAttempts, long connTimeout) {
        this.host = server.getAddress();
        port = new Integer(server.getPort()).intValue();
        user = server.getUserTomcat();
        pwd = server.getPasswordTomcat();
        this.numAttempts = numAttempts;
        this.connTimeout = connTimeout;
    }

    public boolean executeTest() {
        boolean[] r = new boolean[numAttempts];
        int i = 0;
        boolean R = false;
        while (i < numAttempts) {
            r[i] = false;
            Exception e = null;
            try {
                DefaultHttpClient httpclient = new DefaultHttpClient();
                httpclient.getCredentialsProvider().setCredentials(new AuthScope(host, port), new UsernamePasswordCredentials(user, pwd));
                HttpGet httpget = new HttpGet(url);
                System.out.println("executing request:" + httpget.getRequestLine());
                HttpResponse response = httpclient.execute(httpget);
                HttpEntity entity = response.getEntity();
                statusLine = response.getStatusLine().toString();
                System.out.println("statusLine:" + statusLine);
                if (entity != null) {
                    content = entity.getContent();
                    isr = new InputStreamReader(content);
                    lnr = new LineNumberReader(isr);
                    answer = lnr.readLine();
                    if (answer.startsWith("OK") && statusLine.trim().endsWith("OK")) {
                        R = R | true;
                        r[i] = true;
                    }
                }
            } catch (IOException io) {
                e = io;
                io.printStackTrace();
            } finally {
                TestResult tr = new TestResult(r[i], statusLine, e);
                results.add(tr);
            }
            i++;
        }
        return R;
    }

    public TestResult[] getResults() {
        TestResult tr[] = new TestResult[results.size()];
        results.toArray(tr);
        return tr;
    }
}
