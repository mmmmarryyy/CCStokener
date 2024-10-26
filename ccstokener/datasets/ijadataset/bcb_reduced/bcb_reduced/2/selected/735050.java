package com.googlecode.mycontainer.web;

import static org.junit.Assert.assertEquals;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.googlecode.mycontainer.kernel.ShutdownCommand;
import com.googlecode.mycontainer.web.WebServerDeployer;
import com.googlecode.mycontainer.web.jetty.JettyServerDeployer;

public class WebServerDeployerTest {

    @Before
    public void boot() throws NamingException {
        InitialContext ic = new InitialContext();
        WebServerDeployer server = new JettyServerDeployer();
        server.setContext(ic);
        server.setName("WebServer");
        server.bindPort(8380);
        server.deploy();
    }

    @Test
    public void basicTest() throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://localhost:8380/any.html");
            conn = (HttpURLConnection) url.openConnection();
            int code = conn.getResponseCode();
            assertEquals(HttpURLConnection.HTTP_NOT_FOUND, code);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @After
    public void shutdown() throws Exception {
        ShutdownCommand shutdown = new ShutdownCommand();
        shutdown.setContext(new InitialContext());
        shutdown.shutdown();
    }
}
