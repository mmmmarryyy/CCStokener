package vavi.net.xmpp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Poller.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 040117 nsano initial version <br>
 */
class Poller extends Thread {

    public JabberConnection jc;

    private boolean keepPolling = true;

    private int pollingInterval = 2500;

    private final int minPollingInterval = 2500;

    private final int maxPollingInterval = 30000;

    private URL pollingURL;

    private String sessionCookie;

    /** */
    public Poller(JabberConnection jc, URL pollingURL) {
        this.jc = jc;
        this.pollingURL = pollingURL;
    }

    /**
     * send a polling request to the server and send data to it
     * @param send data to be sent
     */
    synchronized void poll(String send) throws IOException {
        URLConnection urlTemp = pollingURL.openConnection();
        if (!(urlTemp instanceof HttpURLConnection)) {
            System.out.println("URL ist not a HTTP URL");
            disconnect();
            return;
        }
        HttpURLConnection urlConn = (HttpURLConnection) urlTemp;
        urlConn.setRequestMethod("POST");
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setAllowUserInteraction(true);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream out = urlConn.getOutputStream();
        if (sessionCookie == null) {
            out.write("0,".getBytes("UTF8"));
        } else {
            out.write((sessionCookie + ",").getBytes("UTF8"));
        }
        if (send != null) {
            out.write(send.getBytes("UTF8"));
        }
        out.flush();
        out.close();
        InputStream in = urlConn.getInputStream();
        sessionCookie = urlConn.getHeaderField("Set-Cookie");
        if (sessionCookie != null) {
            if (sessionCookie.substring(0, 3).equals("ID=")) {
                sessionCookie = sessionCookie.substring(3);
            }
            int index = sessionCookie.indexOf(';');
            if (index > 0) {
                sessionCookie = sessionCookie.substring(0, index - 1);
            }
        }
        if (urlConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            System.out.println("HTTP response code is: " + (new Integer(urlConn.getResponseCode())).toString());
            disconnect();
        }
        if (sessionCookie.endsWith(":0")) {
            System.out.println("Got error cookie: " + sessionCookie);
            disconnect();
        } else {
            System.out.println("Got session cookie: " + sessionCookie);
        }
        long bytesReceived = 0;
        int count;
        byte[] b = new byte[1024];
        while (-1 != (count = in.read(b, 0, 1024))) {
            if (count > 0) {
                String resp = new String(b, 0, count, "UTF8");
                bytesReceived += count;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (Exception ie) {
                }
            }
        }
        in.close();
        int tenthOfInterval = pollingInterval / 10;
        if (bytesReceived > 2) {
            pollingInterval -= tenthOfInterval;
        } else {
            pollingInterval += tenthOfInterval;
        }
        if (pollingInterval > maxPollingInterval) {
            pollingInterval = maxPollingInterval;
        }
        if (pollingInterval < minPollingInterval) {
            pollingInterval = minPollingInterval;
        }
    }

    /** send a polling request to the server */
    void poll() {
        try {
            poll(null);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            disconnect();
        }
    }

    /** Look for new data packets from Server. */
    public void run() {
        while (keepPolling) {
            System.out.println("PollingLoop");
            poll();
            try {
                Thread.sleep(pollingInterval);
            } catch (Exception e) {
            }
        }
    }

    /**
     * Sends data packets to Server
     * @param str text to send to server
     * @return true if it was able to send data, false otherwise
     */
    public boolean send(String str) throws IOException {
        poll(str);
        return true;
    }

    /** Disconnect logical connection to the server */
    public void disconnect() {
        System.out.println("Poller.disconnect() called");
    }

    /**
     * Prepare the instance of Poller to be removed.
     * This will stop the polling loop.
     */
    public void cleanup() {
        disconnect();
        keepPolling = false;
    }
}
