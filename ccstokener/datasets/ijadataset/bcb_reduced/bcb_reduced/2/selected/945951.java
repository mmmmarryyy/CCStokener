package org.skuebeck.ooc.examples.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.skuebeck.ooc.ConcurrentObjectConfig;
import org.skuebeck.ooc.ConcurrentObjects;
import org.skuebeck.ooc.LoggingObserver;
import junit.framework.TestCase;

public class ServerTest extends TestCase {

    Server server;

    public void setUp() {
        ConcurrentObjectConfig config = new ConcurrentObjectConfig();
        config.setObserver(new LoggingObserver());
        ConcurrentObjects.setCommonConfig(config);
    }

    public void tearDown() {
        ConcurrentObjects.setCommonConfig(new ConcurrentObjectConfig());
    }

    public void testStartStop() throws IOException {
        startUp();
        shutDown();
    }

    private void shutDown() {
        server.shutDown();
        assertFalse(server.isRunning());
    }

    private void startUp() throws IOException {
        server = new Server(8080);
        assertTrue(server.isRunning());
    }

    public void testConnect() throws IOException {
        startUp();
        URL url = new URL("http://localhost:8080/test");
        assertEquals("*Test*", read(url));
        shutDown();
    }

    private String read(URL url) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer text = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null) {
                text.append(line);
            }
            return text.toString();
        } finally {
            in.close();
        }
    }
}
