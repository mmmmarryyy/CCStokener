package com.manning.sdmia.web.myfaces;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;

/**
 * @author Thierry Templier
 */
public class MyFacesTest extends AbstractConfigurableBundleCreatorTests {

    public void testWebapp() throws Exception {
        Thread.sleep(4000);
        assertTrue(getTextResponse("http://localhost:8080/myfaces/jsp/contacts.faces").contains("Cogoluegnes"));
    }

    @Override
    protected String[] getTestBundlesNames() {
        return new String[] { "net.sourceforge.cglib, com.springsource.net.sf.cglib, 2.1.3", "org.springframework, org.springframework.context.support, 3.0.0.RC1", "org.springframework, org.springframework.web, 3.0.0.RC1", "org.springframework, org.springframework.web.servlet, 3.0.0.RC1", "org.apache.log4j, com.springsource.org.apache.log4j, 1.2.15", "org.aspectj, com.springsource.org.aspectj.runtime, 1.6.1", "org.aspectj, com.springsource.org.aspectj.weaver, 1.6.1", "javax.activation, com.springsource.javax.activation, 1.1.1", "javax.annotation, com.springsource.javax.annotation, 1.0.0", "javax.ejb, com.springsource.javax.ejb, 3.0.0", "javax.el, com.springsource.javax.el, 1.0.0", "javax.mail, com.springsource.javax.mail, 1.4.1", "javax.persistence, com.springsource.javax.persistence, 1.99.0", "javax.servlet, com.springsource.javax.servlet, 2.5.0", "javax.servlet, com.springsource.javax.servlet.jsp, 2.1.0", "javax.servlet, com.springsource.javax.servlet.jsp.jstl, 1.2.0", "javax.xml.bind, com.springsource.javax.xml.bind, 2.1.7", "javax.xml.rpc, com.springsource.javax.xml.rpc, 1.1.0", "javax.xml.soap, com.springsource.javax.xml.soap, 1.3.0", "javax.xml.stream, com.springsource.javax.xml.stream, 1.0.1", "javax.xml.ws, com.springsource.javax.xml.ws, 2.1.1", "org.apache.catalina.springsource, com.springsource.org.apache.catalina.springsource, 6.0.20.S2-r5956", "org.apache.commons, com.springsource.org.apache.commons.beanutils, 1.8.0", "org.apache.commons, com.springsource.org.apache.commons.codec, 1.4.0", "org.apache.commons, com.springsource.org.apache.commons.collections, 3.2.1", "org.apache.commons, com.springsource.org.apache.commons.digester, 1.8.1", "org.apache.commons, com.springsource.org.apache.commons.logging, 1.1.1", "org.apache.commons, com.springsource.org.apache.commons.discovery, 0.4.0", "org.apache.coyote.springsource, com.springsource.org.apache.coyote.springsource, 6.0.20.S2-r5956", "org.apache.el.springsource, com.springsource.org.apache.el.springsource, 6.0.20.S2-r5956", "org.apache.jasper, com.springsource.org.apache.jasper.org.eclipse.jdt, 6.0.16", "org.apache.jasper.springsource, com.springsource.org.apache.jasper.org.eclipse.jdt.springsource, 6.0.20.S2-r5956", "org.apache.jasper.springsource, com.springsource.org.apache.jasper.springsource, 6.0.20.S2-r5956", "org.apache.juli.springsource, com.springsource.org.apache.juli.extras.springsource, 6.0.20.S2-r5956", "org.apache.xmlcommons, com.springsource.org.apache.xmlcommons, 1.3.4", "org.springframework.osgi, catalina.start.osgi, 1.0-SNAPSHOT", "org.springframework.osgi, spring-osgi-web, 2.0.0.M1", "org.springframework.osgi, spring-osgi-web-extender, 2.0.0.M1", "org.apache.myfaces, myfaces-api, 1.2.2", "org.apache.myfaces, myfaces-impl, 1.2.2", "com.manning.sdmia.ch08, ch08-tomcat6-default-config, 1.0-SNAPSHOT", "com.manning.sdmia.ch08, ch08-directory, 1.0-SNAPSHOT", "com.manning.sdmia.ch08, ch08-jsf-myfaces, 1.0-SNAPSHOT" };
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
