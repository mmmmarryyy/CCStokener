package com.manning.sdmia.ch09;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.osgi.framework.Bundle;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;

/**
 * @author acogoluegnes
 *
 */
public abstract class AbstractOsgiTest extends AbstractConfigurableBundleCreatorTests {

    protected static final String SPRING_OSGI_GROUP = "org.springframework.osgi";

    protected static final String JETTY_VERSION = "6.1.19";

    protected Bundle findBundle(String symbolicName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().equals(symbolicName)) {
                return bundle;
            }
        }
        return null;
    }

    protected Collection<String> getJavaEe5WebArtifacts() {
        List<String> col = new ArrayList<String>();
        col.add("org.mortbay.jetty, servlet-api-2.5, 6.1.14");
        col.add("org.mortbay.jetty, jsp-api-2.1, 6.1.14");
        return col;
    }

    protected Collection<String> getJettyArtifacts() {
        List<String> col = new ArrayList<String>();
        col.add("org.mortbay.jetty, jetty, " + JETTY_VERSION);
        col.add("org.mortbay.jetty, jetty-util, " + JETTY_VERSION);
        col.add(SPRING_OSGI_GROUP + ", jetty.web.extender.fragment.osgi, 1.0.1");
        col.add(SPRING_OSGI_GROUP + ", jetty.start.osgi, 1.0.0");
        return col;
    }

    protected Collection<String> getTomcat5Artifacts() {
        List<String> col = new ArrayList<String>();
        col.add("org.springframework.osgi, catalina.osgi, 5.5.23-SNAPSHOT");
        col.add("org.springframework.osgi, catalina.start.osgi, 1.0.0");
        return col;
    }

    protected Collection<String> getTomcat6Artifacts() {
        List<String> col = new ArrayList<String>();
        col.add("org.springframework.osgi, catalina.osgi, 6.0.16-SNAPSHOT");
        col.add("org.springframework.osgi, catalina.start.osgi, 1.0.0");
        return col;
    }

    protected Collection<String> getSpringDmWebArtifacts() {
        List<String> col = new ArrayList<String>();
        col.add(SPRING_OSGI_GROUP + ", spring-osgi-web," + getSpringDMVersion());
        col.add(SPRING_OSGI_GROUP + ", spring-osgi-web-extender," + getSpringDMVersion());
        return col;
    }

    @Override
    protected Resource getTestingFrameworkBundlesConfiguration() {
        return new InputStreamResource(AbstractOsgiTest.class.getResourceAsStream("boot-bundles.properties"));
    }

    protected String getTextResponse(String address, boolean ignoreResponseCode) throws Exception {
        URL url = new URL(address);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setUseCaches(false);
        BufferedReader in = null;
        try {
            con.connect();
            if (!ignoreResponseCode) {
                assertEquals(HttpURLConnection.HTTP_OK, con.getResponseCode());
            }
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

    protected String getTextResponse(String address) throws Exception {
        return getTextResponse(address, false);
    }

    protected int getResponseCode(String address) throws Exception {
        URL url = new URL(address);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setUseCaches(false);
        try {
            con.connect();
            return con.getResponseCode();
        } finally {
            con.disconnect();
        }
    }

    protected void testConnection(String address) throws Exception {
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
}
