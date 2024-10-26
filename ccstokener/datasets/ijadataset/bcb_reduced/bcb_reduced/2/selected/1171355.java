package jdc.lib;

import org.apache.log4j.Logger;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Hashtable;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.StringBuffer;
import org.apache.tools.bzip2.CBZip2InputStream;

public class ConnectionHandler {

    /** get the logger for this package */
    protected static Logger libLogger = LoggerContainer.getLogger(ConnectionHandler.class);

    /** The one and only instance of this class. */
    private static ConnectionHandler _instance = null;

    /** Maps from an hub address(ip-number or whatever to a HubConnection. */
    private Hashtable _hub_connections = new Hashtable();

    private Hashtable _ip_to_hostname = new Hashtable();

    private Hashtable _hostname_to_ip = new Hashtable();

    private static Object _public_hublist_fetch_mutex = new Object();

    /**
   * Private constructor. Can only be called by the instance method.
   */
    private ConnectionHandler() {
        UserConnectionHandler.instance();
    }

    /**
   * Returns the instance of this class.
   *
   * @return The instance
   */
    public static ConnectionHandler instance() {
        if (_instance == null) {
            _instance = new ConnectionHandler();
        }
        return _instance;
    }

    /**
   * Fetch a list of the current public hubs.
   *
   * @return List of all known root-hubs
   */
    public LinkedList getPublicHubs() {
        synchronized (_public_hublist_fetch_mutex) {
            LinkedList hub_list = null;
            Iterator itr = Configuration.instance().getHubListStorage().getIterator();
            boolean done = false;
            while (!done && itr.hasNext()) {
                URL current_host = (URL) itr.next();
                libLogger.debug("Fetching data from: " + current_host.toString());
                try {
                    hub_list = _readHubList(current_host);
                    done = true;
                } catch (Throwable e) {
                    libLogger.error("Exception: ", e);
                }
            }
            return hub_list;
        }
    }

    /**
   * Fetch a hubconnection.
   *
   * @param the_hub Specification of the hub.
   *
   * @return The hubconnection or null if not found.
   */
    public HubConnection getHubConnection(Hub the_hub) {
        return getHubConnectionByHost(the_hub.getHost());
    }

    /**
   * Fetch a hubconnection.
   *
   * @param host IP/Hostname of the hub.
   *
   * @return The hubconnection or null if not found.
   */
    public synchronized HubConnection getHubConnectionByHost(String host) {
        HubConnection hub_con = (HubConnection) _hub_connections.get(host);
        if (hub_con == null) {
            String host2 = (String) _ip_to_hostname.get(host);
            if (host2 != null) hub_con = (HubConnection) _hub_connections.get(host2);
        }
        return hub_con;
    }

    public synchronized int nrOfHubsConnected() {
        int ret_val = 0;
        Iterator itr = _hub_connections.entrySet().iterator();
        while (itr.hasNext()) {
            HubConnection hub_c = (HubConnection) ((java.util.Map.Entry) itr.next()).getValue();
            if (hub_c.isConnected() && !hub_c.registeredUserHub()) ret_val++;
        }
        return ret_val;
    }

    /**
   * Create a new hub connection and store the result.
   *
   * @param the_hub Hub to connect to.
   *
   * @return The resulting hub connection.
   */
    public synchronized HubConnection createHubConnection(Hub the_hub) {
        HubConnection hc = new HubConnection(the_hub);
        libLogger.debug("Creating hub connection to host: " + the_hub.getHost());
        _hub_connections.put(the_hub.getHost(), hc);
        try {
            java.net.InetAddress inet_addr = java.net.InetAddress.getByName(the_hub.getHost());
            String ip = inet_addr.getHostAddress();
            _ip_to_hostname.put(ip, the_hub.getHost());
            _hostname_to_ip.put(the_hub.getHost(), ip);
        } catch (java.net.UnknownHostException ex) {
            libLogger.error("Failed to look up hub host", ex);
        }
        return hc;
    }

    public synchronized void hostMoved(String former_host, String new_host) {
        HubConnection hub_con = (HubConnection) _hub_connections.remove(former_host);
        if (hub_con == null) {
            String host2 = (String) _ip_to_hostname.remove(former_host);
            if (host2 != null) {
                hub_con = (HubConnection) _hub_connections.remove(host2);
                _hostname_to_ip.remove(host2);
            }
        } else {
            _hostname_to_ip.remove(former_host);
        }
        if (hub_con != null) {
            _hub_connections.put(new_host, hub_con);
            try {
                java.net.InetAddress inet_addr = java.net.InetAddress.getByName(new_host);
                String ip = inet_addr.getHostAddress();
                _ip_to_hostname.put(ip, new_host);
                _hostname_to_ip.put(new_host, ip);
            } catch (java.net.UnknownHostException ex) {
                libLogger.error("Failed to look up hub host", ex);
            }
        }
    }

    /**
   * Removes a hubconnection from the internal map.
   *
   * @param the_hub The hub to remove.
   */
    public synchronized void removeHubConnection(Hub the_hub) {
        _hub_connections.remove(the_hub.getHost());
        String ip = (String) _hostname_to_ip.remove(the_hub.getHost());
        if (ip != null) _ip_to_hostname.remove(ip);
    }

    /**
   * Search all hubs for the specified data. All results are delivered
   * via the SEARCH_REPLY_CB from each hub.
   *
   * @param sr The searchrequest.
   */
    public synchronized void search(SearchRequest sr) {
        Iterator itr = _hub_connections.values().iterator();
        while (itr.hasNext()) {
            HubConnection hc = (HubConnection) itr.next();
            hc.search(sr);
        }
    }

    /**
   * Takes a search match and sends it the correcting depending on
   * if it's a active/passive search.
   *
   * @param sm The search match.
   */
    public synchronized void sendSearchMatch(ShareStorage.SearchMatch sm) {
        String sender = (sm.request.isPassiveSearch() ? sm.request.getSender() : null);
        String slots = "" + Configuration.instance().getAvailableSlots() + "/" + Configuration.instance().getSlots();
        SearchReply sr = new SearchReply(sm.file, slots, sm.request.getHub(), sender);
        if (sm.request.isPassiveSearch()) {
            HubConnection hc = (HubConnection) _hub_connections.get(sm.request.getHub().getHost());
            if (hc != null) {
                hc.sendSearchReply(sr);
            }
        } else {
            String host_ip = sm.request.getSender();
            int colon_pos = host_ip.indexOf(':');
            String host = host_ip.substring(0, colon_pos);
            int port = Integer.parseInt(host_ip.substring(colon_pos + 1, host_ip.length()));
            UDPConnection.sendCommand(host, port, "$SR", sr.getString());
        }
    }

    /**
   * Fetches all hubs located in the specified URL.
   *
   * @param an_url URL pointing to the HUB file
   *
   * @return LinkedList of Hub objects.
   */
    private LinkedList _readHubList(URL an_url) {
        LinkedList ret_val = new LinkedList();
        try {
            URLConnection connection = an_url.openConnection();
            InputStream connectionInputStream = connection.getInputStream();
            if (connectionInputStream != null) if (an_url.getFile().toUpperCase().endsWith(".BZ2")) {
                libLogger.debug("Fetching bzip2 compressed HubList");
                StringBuffer next_line = new StringBuffer();
                int next_char = connectionInputStream.read();
                if (next_char != 'B') {
                    throw new IOException("Invalid bz2 file." + an_url.toString());
                }
                next_char = connectionInputStream.read();
                if (next_char != 'Z') {
                    throw new IOException("Invalid bz2 file." + an_url.toString());
                }
                CBZip2InputStream bz2 = new CBZip2InputStream(connectionInputStream);
                while ((next_char = bz2.read()) != -1) {
                    next_line.append((char) next_char);
                    if (next_char == '\n') {
                        ret_val.addLast(new Hub(next_line.toString()));
                        next_line = new StringBuffer(50);
                    }
                }
                bz2.close();
            } else {
                libLogger.debug("Fetching Std. HubList");
                InputStreamReader isr = new InputStreamReader(connectionInputStream);
                BufferedReader reader = new BufferedReader(isr);
                String next_line = null;
                while ((next_line = reader.readLine()) != null) {
                    ret_val.addLast(new Hub(next_line));
                }
                reader.close();
                isr.close();
            }
            libLogger.debug("Got " + ret_val.size() + " entries.");
        } catch (IOException e) {
            Configuration.instance().executeExceptionCallback(e);
        }
        return ret_val;
    }
}
