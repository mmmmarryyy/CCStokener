package com.manning.sdmia.ch04.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import junit.framework.Assert;
import org.osgi.framework.ServiceReference;
import org.springframework.context.ApplicationContext;
import com.manning.sdmia.ch04.AbstractTest;

/**
 * Share services and consume them from a webapp.
 * @author acogoluegnes
 */
public class ShareServicesTest extends AbstractTest {

    public void testShareServices() throws Exception {
        Thread.sleep(3 * 1000);
        ServiceReference[] refs = bundleContext.getAllServiceReferences(ApplicationContext.class.getName(), null);
        Collection<String> expectedNames = Arrays.asList("repository");
        boolean found = false;
        for (ServiceReference ref : refs) {
            ApplicationContext context = (ApplicationContext) bundleContext.getService(ref);
            if (context.getDisplayName().contains("com.manning.sdmia.ch04-web-repositorybundle")) {
                String[] names = context.getBeanDefinitionNames();
                if (Arrays.asList(names).containsAll(expectedNames)) {
                    found = true;
                }
            }
        }
        Assert.assertTrue("Could not find all the expected bean in the repository context", found);
        testConnection("http://localhost:8080/webapp/index.html");
        Assert.assertEquals("<html><head><title>The answer to life, the universe and everything</title></head>" + "<body><h1>The answer to life, the universe and everything is:</h1><p>42</p></body></html>", getTextResponse("http://localhost:8080/webapp/answerServlet"));
    }

    @Override
    protected String[] getTestBundlesNames() {
        return new String[] { "org.springframework, org.springframework.web, " + getSpringVersion(), "org.springframework.osgi, spring-osgi-web," + getSpringDMVersion(), "org.springframework.osgi, spring-osgi-web-extender," + getSpringDMVersion(), "javax.servlet, com.springsource.javax.servlet, 2.4.0", "org.springframework.osgi, catalina.osgi, 5.5.23-SNAPSHOT", "org.springframework.osgi, catalina.start.osgi, 1.0.0", "org.springframework.osgi, jsp-api.osgi, 2.0-SNAPSHOT", "org.springframework.osgi, jasper.osgi, 5.5.23-SNAPSHOT", "com.manning.sdmia, ch04-web-repositorybundle, 1.0-SNAPSHOT", "com.manning.sdmia, ch04-web-webapp, 1.0-SNAPSHOT" };
    }

    private void testConnection(String address) throws Exception {
        URL url = new URL(address);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setUseCaches(false);
        try {
            con.connect();
            assertEquals(HttpURLConnection.HTTP_OK, con.getResponseCode());
        } finally {
            con.disconnect();
        }
    }

    private String getTextResponse(String address) throws Exception {
        URL url = new URL(address);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setUseCaches(false);
        BufferedReader in = null;
        try {
            con.connect();
            assertEquals(HttpURLConnection.HTTP_OK, con.getResponseCode());
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String inputLine = null;
            while ((inputLine = in.readLine()) != null) {
                builder.append(inputLine);
            }
            return builder.toString();
        } finally {
            if (in != null) {
                in.close();
            }
            con.disconnect();
        }
    }
}
