package com.bitgate.server.proxy;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import com.bitgate.util.constants.Constants;

public class Roundrobin implements ProxyRotatorInterface {

    private ArrayList servers;

    private HashMap serverStatus;

    private HashMap serverRequests;

    private int position;

    private static final int DEFAULT_TIMEOUT = 10;

    public Roundrobin() {
        servers = new ArrayList();
        serverStatus = new HashMap();
        serverRequests = new HashMap();
        position = 0;
    }

    public void addForward(String server) {
        servers.add(server);
        serverStatus.put(server, "0");
    }

    public void addRequest(String server, String url, String match) {
        serverRequests.put(server, url + "\t" + match);
    }

    private String getNext() {
        String serverName = null;
        if (position >= servers.size()) {
            position = 0;
        }
        serverName = (String) servers.get(position++);
        if (((String) serverStatus.get(serverName)).equals("0")) {
            return null;
        }
        return serverName;
    }

    public ProxyRotatorObject getNextAvailable() {
        for (int i = 0; i < servers.size(); i++) {
            String serverName = getNext();
            if (serverName != null) {
                String hostString = serverName.substring(0, serverName.indexOf(":"));
                int port = Integer.parseInt(serverName.substring(serverName.indexOf(":") + 1));
                return new ProxyRotatorObject(hostString, port);
            }
        }
        System.err.println("*** Warning: Roundrobin cannot locate an active server!");
        return null;
    }

    private void verifyAvailability() {
        for (int i = 0; i < servers.size(); i++) {
            String hostEntry = (String) servers.get(i);
            String hostString = hostEntry.substring(0, hostEntry.indexOf(":"));
            String portString = hostEntry.substring(hostEntry.indexOf(":") + 1);
            String urlLocation = "http://" + hostString + ":" + portString + "/";
            String urlData = null;
            String urlMatch = null;
            URL url = null;
            HttpURLConnection conn = null;
            InputStream istream = null;
            if (serverRequests.get(hostEntry) != null) {
                String requestData = (String) serverRequests.get(hostEntry);
                urlData = requestData.substring(0, requestData.indexOf("\t"));
                try {
                    urlMatch = requestData.substring(requestData.indexOf("\t") + 1);
                } catch (Exception e) {
                    urlMatch = null;
                }
                urlLocation = "http://" + hostString + ":" + portString + "/" + urlData;
            }
            try {
                url = new URL(urlLocation);
                conn = (HttpURLConnection) url.openConnection();
            } catch (Exception e) {
                System.err.println("*** Warning: Unable to contact host '" + hostEntry + "': " + e.getMessage());
                continue;
            }
            try {
                istream = conn.getInputStream();
            } catch (Exception e) {
                try {
                    if (conn.getResponseCode() != 401) {
                        System.err.println("*** Warning: Unable to contact host '" + hostEntry + "': " + e);
                        continue;
                    }
                } catch (Exception ee) {
                    System.err.println("*** Warning: Unable to contact host '" + hostEntry + "': " + e);
                    continue;
                }
            }
            int response = 501;
            try {
                response = conn.getResponseCode();
                if (response != 200 && response != 401) {
                    System.err.println("*** Warning: Connection to host '" + hostEntry + "' returns response: " + response);
                    continue;
                }
            } catch (Exception e) {
                System.err.println("*** Warning: Unable to contact host '" + hostString + "' on port '" + portString + "'");
                continue;
            }
            if (response != 401) {
                int contentLength = conn.getContentLength();
                if (contentLength == -1) {
                    contentLength = 4096;
                }
                byte data[] = new byte[contentLength];
                int curPos = 0;
                try {
                    int dataRead = 0;
                    while ((dataRead = istream.read(data, curPos, contentLength - curPos)) != -1) {
                        if (dataRead == 0) {
                            break;
                        }
                        curPos += dataRead;
                    }
                } catch (Exception e) {
                    System.err.println("*** Warning: Unable to contact host '" + hostEntry + "': Cannot read response from site.");
                    continue;
                }
                if (urlMatch != null) {
                    String urlContents = new String(data);
                    data = null;
                    if (urlContents.indexOf(urlMatch) == -1) {
                        System.err.println("*** Warning: Host '" + hostEntry + "' does not match search string.  Reports '" + urlContents + "'");
                        try {
                            istream.close();
                            conn.disconnect();
                        } catch (Exception e) {
                        }
                        continue;
                    }
                }
            }
            try {
                istream.close();
                conn.disconnect();
            } catch (Exception e) {
            }
            serverStatus.put(hostEntry, "1");
        }
    }

    public void process() {
        String timeout = Constants.getDefault().getProperties().get("proxy.thread.timeout");
        int timerTimeout = DEFAULT_TIMEOUT;
        if (timeout != null) {
            timerTimeout = Integer.parseInt(timeout);
        }
        verifyAvailability();
        try {
            Thread.sleep(timerTimeout * 1000);
        } catch (Exception e) {
        }
    }
}
