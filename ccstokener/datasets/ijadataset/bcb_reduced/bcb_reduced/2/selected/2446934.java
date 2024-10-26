package iwork.manager.core.monitor;

import iwork.manager.core.ApplicationMonitor;
import java.net.*;
import java.io.*;

/**
* <p>An ApplicationMonitor that monitors a webserver by sending requests for
 * index.html to localhost.</p>
 *
 * <p>The port is assumed to be 8080, but can be set via the MONITOR_PARAMS
 * setting in the configuration file.</p>
 *
 * @author Ulf Ochsenfahrt (ulfjack@stanford.edu)
 */
public class HTTPServerMonitor extends ApplicationMonitor {

    /**
    * Check interval in milliseconds.
     */
    public static final int INTERVAL = 3 * 1000;

    public static final String PREFIX = "[HTTPServerMonitor] ";

    private int port = 8080;

    public void run() {
        System.err.println(PREFIX + "running");
        while (waitForInterval(INTERVAL)) {
            try {
                URLConnection connection;
                URL url = new URL("http://localhost:" + port + "/index.html");
                connection = url.openConnection();
                connection.connect();
                InputStream in = connection.getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int bytesRead;
                byte[] buffer = new byte[500];
                while ((bytesRead = in.read(buffer)) >= 0) out.write(buffer, 0, bytesRead);
                String data = new String(out.toByteArray());
                notifyAlive();
            } catch (ConnectException ignored) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void init(String[] params) {
        if (params.length > 0) {
            try {
                port = Integer.parseInt(params[0]);
            } catch (Exception e) {
                System.err.println("Takes an int value (port) as first parameter!");
            }
        }
    }

    public String getPrefix() {
        return PREFIX;
    }

    public HTTPServerMonitor() {
        super(System.err, INTERVAL);
    }
}
