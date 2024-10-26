package com.bitgate.server.proxy;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import com.bitgate.util.constants.Constants;
import com.bitgate.util.debug.Debug;

/**
 * This class describes how to handle a connection and send it off to the "Best Available" server for the request
 * at the time.  This class is used with the Proxy system, and is one of the algorithms for determining the best
 * route of the connection at the time.
 *
 * @author Kenji Shino &lt;kenji@nuklees.com&gt;
 * @version $Id: //depot/nuklees/serviceproxy/BestAvailable.java#2 $
 */
public class BestAvailable implements ProxyRotatorInterface {

    private ArrayList servers;

    private HashMap serverStatus;

    private HashMap serverRequests;

    private HashMap serverTimes;

    private static final int DEFAULT_TIMEOUT = 10;

    /**
     * Constructor
     */
    public BestAvailable() {
        servers = new ArrayList();
        serverStatus = new HashMap();
        serverRequests = new HashMap();
        serverTimes = new HashMap();
    }

    /**
     * This adds a forwarding server entry to the list of best available hosts.
     *
     * @param server The server (host:port format) to add.
     */
    public void addForward(String server) {
        servers.add(server);
        serverStatus.put(server, "0");
    }

    /**
     * This adds a forwarding server entry to the list of best available hosts, and adds a URL to hit on that
     * server, and a match to look for when requesting that URL to determine availability.
     *
     * @param server The server (host:port format) to add.
     * @param url The URL to request with.
     * @param match The matching string to look up.
     */
    public void addRequest(String server, String url, String match) {
        serverRequests.put(server, url + "\t" + match);
    }

    /**
     * This function determines the next best-available host.
     *
     * @return <code>String</code> containing the next best available host.
     */
    private String getNext() {
        String serverName = null;
        long bestTime = 0L;
        String lastServerName = null;
        Iterator serverTimesIterator = serverTimes.keySet().iterator();
        while (serverTimesIterator.hasNext()) {
            String servName = (String) serverTimesIterator.next();
            String servTime = (String) serverTimes.get(servName);
            long serverTime = Long.parseLong(servTime);
            if (!servTime.equals("0")) {
                if (bestTime == 0) {
                    bestTime = serverTime;
                    lastServerName = servName;
                } else if (bestTime > serverTime) {
                    bestTime = serverTime;
                    lastServerName = servName;
                }
            }
        }
        Debug.log("Best available server '" + lastServerName + "' reports latency of " + bestTime + " ms.");
        return lastServerName;
    }

    /**
     * This returns the next "Best Available" host.
     *
     * @return <code>ProxyRotatorObject</code> containing the host and port of the next available host.
     */
    public ProxyRotatorObject getNextAvailable() {
        String serverName = getNext();
        if (serverName != null) {
            String hostString = serverName.substring(0, serverName.indexOf(":"));
            int port = Integer.parseInt(serverName.substring(serverName.indexOf(":") + 1));
            return new ProxyRotatorObject(hostString, port);
        }
        System.err.println("*** Warning: BestAvailable cannot locate an active server!");
        return null;
    }

    /**
     * This function verifies availability through a threaded operation.
     */
    private void verifyAvailability() {
        for (int i = 0; i < servers.size(); i++) {
            String hostEntry = (String) servers.get(i);
            String hostString = hostEntry.substring(0, hostEntry.indexOf(":"));
            String portString = hostEntry.substring(hostEntry.indexOf(":") + 1);
            String urlLocation = "http://" + hostString + ":" + portString + "/";
            String urlData = null;
            String urlMatch = null;
            long startTime = System.currentTimeMillis();
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
                serverTimes.put(hostEntry, "0");
                continue;
            }
            try {
                istream = conn.getInputStream();
            } catch (Exception e) {
                try {
                    if (conn.getResponseCode() != 401) {
                        System.err.println("*** Warning: Unable to contact host '" + hostEntry + "': " + e);
                        serverTimes.put(hostEntry, "0");
                        continue;
                    }
                } catch (Exception ee) {
                    System.err.println("*** Warning: Unable to contact host '" + hostEntry + "': " + e);
                    serverTimes.put(hostEntry, "0");
                    continue;
                }
            }
            int response = 501;
            try {
                response = conn.getResponseCode();
                if (response != 200 && response != 401) {
                    System.err.println("*** Warning: Connection to host '" + hostEntry + "' returns response: " + response);
                    serverTimes.put(hostEntry, "0");
                    continue;
                }
            } catch (Exception e) {
                System.err.println("*** Warning: Unable to contact host '" + hostString + "' on port '" + portString + "'");
                serverTimes.put(hostEntry, "0");
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
                    serverTimes.put(hostEntry, "0");
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
                        serverTimes.put(hostEntry, "0");
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
            String timeResponse = Long.toString(System.currentTimeMillis() - startTime);
            Debug.log("Response time for '" + hostEntry + "' is " + timeResponse + " ms.");
            serverTimes.put(hostEntry, timeResponse);
        }
    }

    /**
     * Initiates the threaded process.
     */
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
