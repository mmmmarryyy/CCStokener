package up2p.peer.jtella;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import protocol.com.dan.jtella.ConnectedHostsListener;
import protocol.com.dan.jtella.HostsChangedEvent;
import protocol.com.kenmccrary.jtella.Connection;
import protocol.com.kenmccrary.jtella.ConnectionData;
import protocol.com.kenmccrary.jtella.GNUTellaConnection;
import protocol.com.kenmccrary.jtella.GUID;
import protocol.com.kenmccrary.jtella.Host;
import protocol.com.kenmccrary.jtella.HostCache;
import protocol.com.kenmccrary.jtella.MessageReceiver;
import protocol.com.kenmccrary.jtella.NodeConnection;
import protocol.com.kenmccrary.jtella.PushMessage;
import protocol.com.kenmccrary.jtella.RelayMessage;
import protocol.com.kenmccrary.jtella.SearchMessage;
import protocol.com.kenmccrary.jtella.SearchReplyMessage;
import protocol.com.kenmccrary.jtella.SearchSession;
import protocol.com.kenmccrary.jtella.SubNetGNUTellaConnection;
import protocol.com.kenmccrary.jtella.SubNetRouter;
import up2p.core.BasePeerNetworkAdapter;
import up2p.core.Core2Network;
import up2p.core.LocationEntry;
import up2p.core.NetworkAdapterException;
import up2p.core.ResourceNotFoundException;
import up2p.core.WebAdapter;
import up2p.repository.ResourceEntry;
import up2p.repository.ResourceList;
import up2p.search.SearchQuery;
import up2p.search.SearchResponse;
import up2p.search.SearchResponseListener;
import up2p.servlet.HttpParams;
import up2p.util.Config;
import up2p.xml.TransformerHelper;

/**
 * Implements a Network Adapter using a Gnutella peer-to-peer protocol.
 * 
 * @author Michael Yartsev (anijap@gmail.com)
 * @author alan
 * @author Alexander Craig
 * @version 1.2
 */
public class JTellaAdapter extends BasePeerNetworkAdapter implements MessageReceiver, SearchResponseListener, ConnectedHostsListener {

    public static final String HOST_CACHE_FILE = "data" + File.separator + "HostCache.xml";

    /** The number of milliseconds that search sessions should remain active for */
    public static final int SEARCH_TIMEOUT_MILLIS = 60000;

    /**the number of wanted connections: when the "autoconnect"is disable, this is just a limit*/
    public static int OUTGOING_CONNECTIONS_WANTED = 100;

    public static int INCOMING_CONNECTIONS_WANTED = 20;

    /** Local port where Gnutella listens for incoming connections. */
    public static final String DEFAULT_LOCAL_PORT = "6346";

    /** Name of Logger used by this adapter. */
    public static final String LOGGER = "up2p.peer.jtella";

    /** Logger used by this adapter. */
    private static Logger LOG = Logger.getLogger(LOGGER);

    private String serventId;

    /**
     * GNUTellaConnection object (of the JTella library) that implements the Gnutella protocol
     */
    private static GNUTellaConnection c;

    /**
     * When a search is received with a duplicate query ID but new search criteria, a new
     * query ID is generated locally to ensure old results are not used. This map stores
     * the mapping of randomly generated query ID's to the original query ID that prompted
     * the search. Entries should be removed once a response has been sent.
     * 
     * Map<randomlyGeneratedQueryID, responseQueryID)
     */
    private Map<String, String> duplicateQueryIds;

    /**
     * May used to cache resource lists to reduce overall database
     * access.
     * 
     * Map<community, resourceList>
     */
    private Map<String, List<String>> hostedResListCache;

    /** 
     * A singleton instance of HostCacheParser used to manage the static
     * host cache. This should always be accessed through the singleton
     * accessor "getHostCacheParser()"
     */
    private static HostCacheParser hostCacheParser;

    /**
     * A map of query ID's of received search messages keyed by community ID.
     * When a search is received, the query ID should be added to this map until
     * a hosted resource list is sent through the connection, at which point the
     * mapping should be removed.
     */
    private Map<String, List<String>> requestedResourceLists;

    /** 
     * Stores the messages from the network indexed by their query Id.
     * This way, once the answer comes from the system, we can find the original query message.
     * Note: Multiple requests can be associated with the same query id. In this case, all messages
     * must have the same originating connection or they will be discarded. Only the most recently
     * received request is stored in the messageTable.
     **/
    private Map<String, SearchMessage> messageTable;

    /**
     * A map of peer ID's (IP address : port / urlPrefix) and client identifiers
     * (Gnutella GUIDs). This is updated every time a search reply message is received,
     * and the mapping is used to generate PUSH messages when a direct file
     * transfers fails.
     * 
     * Map < peer ID (IP:Port/urlPrefix) , Client Identifier (GUID) >
     */
    private Map<String, GUID> peerIdToClientGuid;

    /**
     * A list of all currently active search session threads (primarily used for
     * orderly termination)
     */
    private List<Thread> openSearchSessions;

    /**
     * The url prefix that should be included with outgoing SearchResponseMessages
     * and PushMessages.
     */
    private String urlPrefix;

    /**
     * The URL that should be advertised as a peer relay if a connection can not
     * be established for a PUSH file transfer.
     */
    private String relayPeerUrl;

    /**
     * Creates a JTella Adapter
     * @param incomingPort	The port to listen for Gnutella connections on.
     * 						Uses the default value if null is passed.
     * @param relayUrl	The relay URL (hostname:port/urlPrefix) that should be advertised as
     * 					a peer relay if a PUSH connection fails
     * @param peerDiscovery	Determines whether automatic peer discovery will be enabled
     * @param urlPrefix		The URL prefix for this U-P2P instance
     * @param adapter		The adapter to use to communicate with the tuple space
     */
    public JTellaAdapter(Config config, String urlPrefix, Core2Network adapter) {
        this.urlPrefix = urlPrefix;
        this.relayPeerUrl = config.getProperty("networkAdapter.relayPeer", "");
        this.serventId = config.getProperty("up2p.gnutella.serventId");
        System.out.println("JTELLA:: gnutella servent Id:" + serventId);
        short[] serventIdasArray = new short[16];
        int i = 0;
        for (String k : serventId.split("\\.")) {
            serventIdasArray[i] = Short.parseShort(k);
            i++;
        }
        GNUTellaConnection.setServantIdentifier(serventIdasArray);
        setWebAdapter(adapter);
        initialize(config.getProperty("up2p.gnutella.incoming", "6346"), Boolean.parseBoolean(config.getProperty("up2p.gnutella.peerdiscovery", "false")));
        initializeHostCache();
        messageTable = new HashMap<String, SearchMessage>();
        peerIdToClientGuid = new HashMap<String, GUID>();
        openSearchSessions = new ArrayList<Thread>();
        requestedResourceLists = new HashMap<String, List<String>>();
        hostedResListCache = new HashMap<String, List<String>>();
        duplicateQueryIds = new HashMap<String, String>();
        LOG.info("JTella adapter initialized.");
    }

    public void setProperty(String propertyName, String propertyValue) {
        LOG.info("setProperty method was called in JTellaAdapter.");
    }

    /**
     * Initializes the adapter
     * @param incomingPort	The port to listen for Gnutella connections on.
     * 						Uses the default value if null is passed.
     * @param peerDiscovery	Determines whether automatic peer discovery will be enabled
     */
    private void initialize(String incomingPort, boolean peerDiscovery) {
        try {
            ConnectionData connData = new ConnectionData();
            connData.setIncommingConnectionCount(INCOMING_CONNECTIONS_WANTED);
            connData.setOutgoingConnectionCount(OUTGOING_CONNECTIONS_WANTED);
            connData.setUltrapeer(true);
            connData.setIncomingPort(Integer.valueOf(DEFAULT_LOCAL_PORT).intValue());
            connData.setAgentHeader("up2p");
            connData.setUrlPrefix(getUrlPrefix());
            connData.setFileTransferPort(adapter.getPort());
            connData.setPeerLookupEnabled(peerDiscovery);
            if (incomingPort == null) {
                incomingPort = DEFAULT_LOCAL_PORT;
            }
            try {
                connData.setIncomingPort(Integer.parseInt(incomingPort));
            } catch (NumberFormatException e) {
                LOG.error("Invalid Gnutella port specified (\"" + incomingPort + "\"), using default.");
                incomingPort = DEFAULT_LOCAL_PORT;
                connData.setIncomingPort(Integer.parseInt(incomingPort));
            }
            LOG.info("Listening for incoming connections on port: " + incomingPort);
            LOG.info("Peer discovery enabled: " + peerDiscovery);
            System.out.println("Incoming Gnutella port: " + incomingPort);
            c = new GNUTellaConnection(connData);
            c.getSearchMonitorSession(this);
            c.createFileServerSession(this);
            c.addListener(this);
            LOG.info("JTellaAdapter:: init: about to start the GnutellaConnection");
            c.start();
            LOG.info("JTellaAdapter:: init: GnutellaConnection started");
            LOG.info("JTellaAdapter::  Servent identifier GUID: " + new GUID(GNUTellaConnection.getServantIdentifier()));
        } catch (NumberFormatException e) {
            LOG.debug("NumberFormatException while initializing JTella adapter: " + e.getMessage());
        } catch (UnknownHostException e) {
            LOG.error("UnknownHostException while initializing JTellaAdapter: " + e.getMessage());
        } catch (IOException e) {
            LOG.error("IOException while initializing JTellaAdapter: " + e.getMessage());
        }
    }

    /**
     * Initializes the Host Cache
     */
    private void initializeHostCache() {
        LOG.info("== Initializing Host Cache ==");
        HostCacheParser hostCacheParser = getHostCacheParser();
        Host[] hosts = hostCacheParser.getHosts();
        if (hosts != null) {
            HostCache hostCache = c.getHostCache();
            for (int i = 0; i < hosts.length; i++) {
                Host thehost = new Host(hosts[i].getIPAddress(), hosts[i].getPort(), 1, 1);
                thehost.setManual(true);
                hostCache.addHost(thehost);
            }
        }
        LOG.info("== Finished initializing Host Cache ==");
    }

    /**
     * @return	The singleton instance of HostCacheParser used to manage the
     * 			JTellaAdapter's host cache.
     */
    public static HostCacheParser getHostCacheParser() {
        if (hostCacheParser == null) {
            hostCacheParser = new HostCacheParser(Core2Network.getRootPath() + File.separator + HOST_CACHE_FILE);
        }
        if (hostCacheParser.getDynamicHostCache() == null && c != null) {
            hostCacheParser.setDynamicHostCache(c.getHostCache());
        }
        return hostCacheParser;
    }

    public boolean isAsynchronous() {
        return true;
    }

    /**
     * Performs a search on the GNutella network and returns the results
     * TODO: deprecated: I just leave it to avoid having to clean up properly; tbd: remove and clean up
     */
    public SearchResponse[] searchNetwork(String communityId, SearchQuery query, long maxTimeout) throws NetworkAdapterException {
        return null;
    }

    /**
     * Performs a search on the GNutella network and returns the results
     */
    public void searchNetwork(String communityId, String query, String queryId) {
        LOG.info("User requesting to search the network. Query:: " + query);
        if (messageTable.containsKey(queryId)) {
            LOG.error("JTELLA Adapter error: outputting a search that was already from the network! [caught]");
            return;
        }
        SearchRequestMessage request = new SearchRequestMessage(queryId, communityId, query);
        StringWriter sw = new StringWriter();
        Node requestXML = request.serialize();
        try {
            TransformerHelper.encodedTransform((Element) requestXML, "UTF-8", sw, true);
        } catch (IOException e) {
            System.out.println("Failed sending up2p request.");
            e.printStackTrace();
        }
        final String searchCriteria = sw.toString().trim();
        final String subNetId = communityId;
        Thread searchSession = new Thread(new Runnable() {

            @Override
            public void run() {
                SearchSession search = null;
                if (c instanceof SubNetGNUTellaConnection) {
                    search = ((SubNetGNUTellaConnection) c).createSubNetSearchSession(searchCriteria, 0, subNetId, 10, 0, JTellaAdapter.this);
                } else {
                    search = c.createSearchSession(searchCriteria, 0, 10, 0, JTellaAdapter.this);
                }
                LOG.debug("Search initiated: Hash - " + search.hashCode());
                try {
                    Thread.sleep(SEARCH_TIMEOUT_MILLIS);
                } catch (InterruptedException e) {
                    LOG.error("Search session timeout thread interrupted, search session closing prematurely.");
                }
                LOG.debug("Search closed: Hash - " + search.hashCode());
                search.close();
                synchronized (openSearchSessions) {
                    openSearchSessions.remove(this);
                }
            }
        });
        synchronized (openSearchSessions) {
            openSearchSessions.add(searchSession);
        }
        searchSession.start();
        LOG.info("JTellaAdapter:exiting search method");
    }

    public void shutdown() {
        LOG.info("Stopping Gnutella connection");
        synchronized (openSearchSessions) {
            for (Thread session : openSearchSessions) {
                session.interrupt();
            }
        }
        c.stop();
    }

    /**
	 * Handles incoming search requests from the network.
	 * 
	 * New: Multiple requests with the same query id are now serviced if the query
	 * is deemed valid (see below), and all tuples are collected as part of a single result set.
	 * 
	 * A never before seen query id is automatically considered to be valid. If the query.
	 * id has already been serviced than the new request must have the same originating connection, and
	 * must have different search criteria than the last request serviced.
	 */
    public void receiveSearch(SearchMessage message) {
        LOG.info("Received a search message");
        String xmlMessage = message.getSearchCriteria().trim();
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = db.parse(new ByteArrayInputStream(xmlMessage.getBytes("UTF-8")));
            SearchRequestMessage searchMessage = SearchRequestMessage.parse(document.getDocumentElement());
            boolean validQuery = false;
            String queryId = searchMessage.getId();
            if (!messageTable.containsKey(searchMessage.getId())) {
                LOG.info("New unique query id: " + searchMessage.getId() + " received, processing request.");
                messageTable.put(searchMessage.getId(), message);
                validQuery = true;
            } else {
                SearchMessage lastMessage = messageTable.get(searchMessage.getId());
                if (lastMessage.getOriginatingConnection() == message.getOriginatingConnection() && !lastMessage.getSearchCriteria().trim().equals(xmlMessage)) {
                    LOG.info("New search criteria for query id: " + searchMessage.getId() + " received, processing request.");
                    messageTable.remove(searchMessage.getId());
                    messageTable.put(searchMessage.getId(), message);
                    queryId = Long.toString((Integer.parseInt(searchMessage.getId()) % 10000) + (new Date()).getTime());
                    synchronized (duplicateQueryIds) {
                        duplicateQueryIds.put(queryId, searchMessage.getId());
                    }
                    validQuery = true;
                } else {
                    LOG.info("Query id: " + searchMessage.getId() + " received duplicate search criteria, ignoring request.");
                }
            }
            if (validQuery) {
                LOG.info("Launching search with criteria:\n\t" + xmlMessage);
                adapter.searchLocal(searchMessage.getCommunityId(), searchMessage.getQuery(), queryId);
            } else {
                LOG.info("Ignoring search message with id: " + searchMessage.getId());
            }
        } catch (Exception e) {
        }
    }

    /**
	 *  implementation of ReceiveSearchReply in the JTella interface: receives responses from the network [QUERYHIT messages].
	 *  The message is parsed and the contents sent to the tuplespace
	 *  @param searchReplyMessage: the JTella message received from the network
	 */
    public void receiveSearchReply(SearchReplyMessage searchReplyMessage) {
        LOG.info("Received a search reply message from " + searchReplyMessage.getOriginatingConnection().getConnectedServant());
        String xmlMessage = searchReplyMessage.getXmlBlock();
        if (xmlMessage == null) {
            xmlMessage = searchReplyMessage.getFileRecord(0).getName().trim();
        }
        LOG.info("xmlMessage of the search reply: " + xmlMessage);
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = db.parse(new ByteArrayInputStream(xmlMessage.getBytes("UTF-8")));
            SearchResponseMessage responseMessage = SearchResponseMessage.parse(document.getDocumentElement());
            String peerIdentifier = searchReplyMessage.getIPAddress() + ":" + searchReplyMessage.getPort() + "/" + responseMessage.getUrlPrefix();
            if (peerIdToClientGuid.containsKey(peerIdentifier)) {
                peerIdToClientGuid.remove(peerIdentifier);
            }
            peerIdToClientGuid.put(peerIdentifier, searchReplyMessage.getClientIdentifier());
            LOG.debug("JTellaAdapter: Mapped \"" + peerIdentifier + "\" to GUID: " + searchReplyMessage.getClientIdentifier().toString());
            if (responseMessage.getHostedResIdList() != null) {
                fireNetworkResourceList(peerIdentifier, responseMessage.getCommunityId(), responseMessage.getHostedResIdList());
                LOG.debug("Fired network resource list.");
            }
            if (responseMessage.getResultSetSize() > 0) {
                SearchResponse[] resultSet = responseMessage.getResponses();
                for (SearchResponse response : resultSet) {
                    LocationEntry[] locations = new LocationEntry[1];
                    locations[0] = new LocationEntry(searchReplyMessage.getIPAddress(), searchReplyMessage.getPort(), responseMessage.getUrlPrefix());
                    response.setLocations(locations);
                }
                fireTrustMetric(peerIdentifier, responseMessage.getCommunityId(), "Network Distance", Integer.toString(searchReplyMessage.getHops() + 1));
                List<String> metricNames = responseMessage.getMetricNames();
                if (metricNames != null) {
                    for (String metricName : metricNames) {
                        fireTrustMetric(peerIdentifier, responseMessage.getCommunityId(), metricName, responseMessage.getTrustMetric(metricName));
                    }
                }
                fireSearchResponse(resultSet);
                LOG.debug("Fired search responses.");
            }
        } catch (ParserConfigurationException e) {
            LOG.error("ParserConfigurationException in JTellaSearchMonitor: " + e.getMessage());
        } catch (SAXException e) {
            LOG.error("SAXException in JTellaSearchMonitor: " + e.getMessage());
        } catch (IOException e) {
            LOG.error("IOException in JTellaSearchMonitor: " + e.getMessage());
        } catch (MalformedPeerMessageException e) {
            LOG.error("MalformedPeerMessageException in JTella Adapter: " + e.getMessage());
            LOG.error(e.getStackTrace().toString());
        }
    }

    /**
	 * Handles an incoming PUSH message. Initiates a connection to the remote nodes
	 * PushServlet, and receives transfer requests. Continually requests and pushes
	 * file transfers until an "OK" handshake is received.
	 */
    public void receivePush(PushMessage pushMsg) {
        LOG.debug("JTellaAdapter: Recieved PUSH message from: " + pushMsg.getIPAddress() + ":" + pushMsg.getPort() + " Url Prefix: " + pushMsg.getUrlPrefix());
        String localIp;
        try {
            localIp = InetAddress.getByName(pushMsg.getOriginatingConnection().getPublicIP()).getHostAddress();
        } catch (UnknownHostException e) {
            localIp = pushMsg.getOriginatingConnection().getPublicIP();
        }
        String urlString = "http://" + pushMsg.getIPAddress() + ":" + pushMsg.getPort() + "/" + pushMsg.getUrlPrefix() + "/push?" + HttpParams.UP2P_PEERID + "=" + localIp + ":" + adapter.getPort() + "/" + getUrlPrefix();
        LOG.debug("JTellaAdapter: Initiating HTTP connection to: " + urlString);
        HttpURLConnection pushConn = null;
        try {
            URL url = new URL(urlString);
            boolean pushComplete = false;
            while (!pushComplete) {
                pushConn = (HttpURLConnection) url.openConnection();
                pushConn.setDoInput(true);
                pushConn.setDoOutput(true);
                pushConn.setUseCaches(false);
                pushConn.setRequestMethod("GET");
                pushConn.setRequestProperty("Connection", "Keep-Alive");
                pushConn.setRequestProperty("User-Agent", "UP2P");
                pushConn.setRequestProperty("Accept", "[star]/[star]");
                BufferedReader inStream = new BufferedReader(new InputStreamReader(pushConn.getInputStream()));
                String serverResponse = inStream.readLine();
                inStream.close();
                LOG.debug("JTellaAdapter: Received from PUSH servlet: " + serverResponse);
                if (serverResponse.startsWith("OK")) {
                    LOG.debug("JTellaAdapter: PUSH transfers complete.");
                    pushComplete = true;
                } else if (serverResponse.startsWith("GIV")) {
                    serverResponse = serverResponse.substring(serverResponse.indexOf(" ") + 1);
                    String[] splitResponse = serverResponse.split("/");
                    LOG.debug("JTellaAdapter: Got PUSH request for ComId: " + splitResponse[0] + "   ResId: " + splitResponse[1]);
                    try {
                        List<String> filePathList = adapter.lookupFilepaths(splitResponse[0], splitResponse[1]);
                        String resourceFilePath = filePathList.remove(0);
                        pushResource(pushMsg.getIPAddress() + ":" + pushMsg.getPort() + "/" + pushMsg.getUrlPrefix(), splitResponse[0], resourceFilePath, filePathList);
                    } catch (ResourceNotFoundException e) {
                        LOG.error("JTellaAdapter: Could not find resource specified by PUSH message: " + splitResponse[0] + "/" + splitResponse[1]);
                    }
                }
                pushConn.disconnect();
            }
        } catch (IOException e) {
            LOG.error("JTellaAdapter: PUSH file transfer failed: " + e.getMessage());
            if (!this.relayPeerUrl.equals("")) {
                issueRelayMessage(pushMsg);
            }
        }
    }

    /**
	 * Handles an incoming RELAY message. Notifies the tuple space of the relay identifier
	 * and the relay URL so that download requests can be initiated.
	 */
    public void receiveRelay(RelayMessage relayMsg) {
        LOG.info("Got RelayMessage:\nTarget Identifier: " + relayMsg.getTargetIdentifier() + "\nRelay Identifier: " + relayMsg.getRelayIdentifier() + "\nPeer URL: " + relayMsg.getRelayUrl() + "\nSource URL: " + relayMsg.getSourcePeerId());
        adapter.notifyRelayReceived(relayMsg.getSourcePeerId(), relayMsg.getRelayUrl(), relayMsg.getRelayIdentifier());
    }

    /**
	 * Send a PUSH message to the specified remote node.
	 * @param peerId	The peer ID of the remote node (IP:port/urlPrefix)
	 */
    public void issuePushMessage(String peerId) {
        LOG.debug("JTellaAdapter: PUSH requested to remote node: " + peerId);
        if (peerIdToClientGuid.get(peerId) == null) {
            LOG.error("JTellaAdapter: PUSH message requested for unknown peer ID: " + peerId);
            return;
        }
        GUID clientIdentifier = peerIdToClientGuid.get(peerId);
        LOG.debug("JTellaAdapter: Issuing PUSH with advertised address: " + c.getRouter().getQueryHitSource(clientIdentifier).getPublicIP() + ":" + adapter.getPort() + "/" + getUrlPrefix());
        PushMessage pushRequest = new PushMessage(clientIdentifier, 0, c.getRouter().getQueryHitSource(clientIdentifier).getPublicIP(), (short) adapter.getPort(), getUrlPrefix());
        try {
            NodeConnection clientConnection = c.getRouter().getQueryHitSource(clientIdentifier);
            clientConnection.send(pushRequest);
            LOG.debug("JTellaAdapter: PUSH message sent to client: " + peerId + " (" + clientIdentifier.toString() + ")");
            LOG.debug("JTellaAdapter: PUSH sent on connection: " + clientConnection.getListenString());
        } catch (IOException e) {
            LOG.error("JTellaAdapter: Error sending PUSH message.");
            e.printStackTrace();
        }
    }

    /**
	 * Issues a relay message in response to the specified push message (this should
	 * only be called when a push file transfer fails)
	 * @param pushMsg	The push message which initiated the failed transfer.
	 */
    private void issueRelayMessage(PushMessage pushMsg) {
        Random rand = new Random();
        int relayId = rand.nextInt(Integer.MAX_VALUE - 1) + 1;
        LOG.info("JTellaAdapter: Sending a RELAY message (Relay ID: " + relayId + ").");
        String sourceIp = pushMsg.getOriginatingConnection().getPublicIP();
        try {
            sourceIp = InetAddress.getByName(sourceIp).getHostAddress();
        } catch (UnknownHostException e2) {
        }
        String sourceUrl = sourceIp + ":" + adapter.getPort() + "/" + adapter.getUrlPrefix();
        String relayRegistration;
        try {
            relayRegistration = "http://" + this.relayPeerUrl + "/relay?up2p:registerrelay=" + URIUtil.encodeQuery(sourceUrl, "UTF-8") + "&up2p:relayidentifier=" + relayId;
        } catch (URIException e1) {
            relayRegistration = "http://" + this.relayPeerUrl + "/relay?up2p:registerrelay=" + sourceUrl + "&up2p:relayidentifier=" + relayId;
        }
        LOG.info("JTellaAdapter: Registering relay pair with URL: " + relayRegistration);
        boolean relayRegSuccessful = false;
        HttpURLConnection relayRegConn = null;
        try {
            URL relayRegUrl = new URL(relayRegistration);
            relayRegConn = (HttpURLConnection) relayRegUrl.openConnection();
            relayRegConn.setRequestMethod("GET");
            relayRegConn.setRequestProperty("User-Agent", "UP2P");
            relayRegConn.setRequestProperty("Accept", "[star]/[star]");
            relayRegConn.connect();
            if (relayRegConn.getResponseCode() == 200) {
                LOG.info("JTellaAdapter: Relay peer registration was successful.");
                relayRegSuccessful = true;
            }
            relayRegConn.disconnect();
        } catch (IOException e2) {
            LOG.error("JTellaAdapter: Relay peer registration failed, abandoning relay attempt.");
        }
        if (relayRegSuccessful) {
            RelayMessage relayMessage = new RelayMessage(pushMsg, relayId, this.relayPeerUrl, pushMsg.getOriginatingConnection().getPublicIP(), adapter.getPort(), adapter.getUrlPrefix());
            try {
                NodeConnection clientConnection = c.getRouter().getPushSource(pushMsg.getSourceIdentifier());
                LOG.debug("JTellaAdapter: Got client connection for RELAY: " + clientConnection.getListenString());
                clientConnection.send(relayMessage);
                LOG.debug("JTellaAdapter: RELAY message sent to client: " + relayMessage.getTargetIdentifier());
            } catch (IOException ex) {
                LOG.error("JTellaAdapter: Error sending RELAY message.");
                ex.printStackTrace();
            }
        }
    }

    public static GNUTellaConnection getConnection() {
        return c;
    }

    /**
     * @return	The url prefix that should be attached to outgoing SearchResponse
     * 			and Push messages.
     */
    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void publish(ResourceEntry resourceEntry, boolean buffer) throws NetworkAdapterException {
    }

    public void publishAll(ResourceList resourceList) throws NetworkAdapterException {
    }

    public void publishFlush() throws NetworkAdapterException {
    }

    public void remove(ResourceEntry resourceEntry) throws NetworkAdapterException {
    }

    public void removeAll(ResourceList resourceList) throws NetworkAdapterException {
    }

    /**
     * build a list of searchresponse messages (in case we need to break up a large list)
     * from a set of search results
     * @param results
     * @return
     */
    private SearchResponseMessage buildResponseMessages(SearchResponse[] results) {
        String communityId = results[0].getCommunityId();
        String qid = results[0].getQueryId();
        SearchResponseMessage responseMessage = new SearchResponseMessage(qid, communityId, getUrlPrefix());
        int resultCount = 0;
        if (results.length > 0) {
            for (SearchResponse r : results) {
                responseMessage.addResult(r);
                resultCount++;
            }
        }
        LOG.info(" -Search returning " + resultCount + " search results for query in community " + communityId);
        return responseMessage;
    }

    /**
     * Receives local search responses from the repository, and outputs
     * search response messages to the network.
     */
    public void receiveSearchResponse(SearchResponse[] results) {
        LOG.debug(" -Local repository returned " + results.length + " results.");
        StringWriter sw2 = new StringWriter();
        SearchResponseMessage theresponse = buildResponseMessages(results);
        synchronized (duplicateQueryIds) {
            if (duplicateQueryIds.get(theresponse.getId()) != null) {
                theresponse.setId(duplicateQueryIds.remove(theresponse.getId()));
            }
        }
        synchronized (hostedResListCache) {
            if (hostedResListCache.get(theresponse.getCommunityId()) != null) {
                theresponse.setHostedResIdList(hostedResListCache.remove(theresponse.getCommunityId()));
                LOG.debug("JTellaAdapter: Found cached resource list for community: " + theresponse.getCommunityId());
            } else {
                synchronized (requestedResourceLists) {
                    if (requestedResourceLists.get(theresponse.getCommunityId()) == null) {
                        ArrayList<String> idList = new ArrayList<String>();
                        idList.add(theresponse.getId());
                        requestedResourceLists.put(theresponse.getCommunityId(), idList);
                    } else {
                        requestedResourceLists.get(theresponse.getCommunityId()).add(theresponse.getId());
                    }
                }
                LOG.debug("JTellaAdapter: Added mapping: " + theresponse.getCommunityId() + " -> " + theresponse.getId() + " to requested resource lists.");
                adapter.fetchHostedResourceList(theresponse.getCommunityId(), theresponse.getId());
            }
        }
        int neighbours = 0;
        for (NodeConnection connection : c.getConnectionList()) {
            if (connection.getType() == connection.CONNECTION_INCOMING) {
                neighbours++;
            }
        }
        theresponse.addTrustMetric("Network Neighbours", Integer.toString(neighbours));
        List<SearchResponseMessage> messagesplit = theresponse.split();
        for (SearchResponseMessage responseMessage : messagesplit) {
            responseMessage.addTrustMetric("Network Neighbours", Integer.toString(neighbours));
            StringWriter sw = new StringWriter();
            Node requestXML = responseMessage.serialize();
            try {
                TransformerHelper.encodedTransform((Element) requestXML, "UTF-8", sw, true);
            } catch (IOException e) {
                LOG.error("JTella Adapter:" + e);
                e.printStackTrace();
            }
            String qid = responseMessage.getId();
            sendSearchReplyMessage(sw.toString(), qid);
        }
    }

    private void sendSearchReplyMessage(String xmlmsg, String qid) {
        LOG.info(" -Sending the search response");
        LOG.debug("content=" + xmlmsg);
        LOG.debug("getting original msg with id:" + qid);
        SearchMessage message = messageTable.get(qid);
        SearchReplyMessage replyMessage;
        if (message != null && message.getOriginatingConnection().getPublicIP() != null) {
            replyMessage = new SearchReplyMessage(message, (short) adapter.getPort(), message.getOriginatingConnection().getPublicIP(), 0);
            replyMessage.addCompressedXmlBlock(xmlmsg);
            try {
                message.getOriginatingConnection().send(replyMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
            LOG.debug("Using public IP address: " + message.getOriginatingConnection().getPublicIP());
            LOG.info("JTellaAdapter: Finished sending search response");
        } else if (message == null) {
            LOG.error("Error: didn't find original message to reply to...");
            LOG.debug("The remembered messages are: ");
            Iterator<String> iter = messageTable.keySet().iterator();
            while (iter.hasNext()) {
                LOG.debug("Query identifier: " + iter.next());
            }
        } else {
            LOG.error("Error: unknown local IP address. Ignoring query response.");
        }
    }

    /**
	 * Updates the set of sub-network ID's that this node should
	 * serve (if sub-network support is enabled)
	 * @param subNetIds	A set of U-P2P community IDs (also used as sub-network IDs)
	 */
    public void updateSubnets(Set<String> subNetIds) {
        if (c instanceof SubNetGNUTellaConnection) {
            ((SubNetRouter) ((SubNetGNUTellaConnection) c).getRouter()).setServedSubNets(subNetIds);
        }
    }

    /**
	 * Pushes a resource to a remote peer by connection to upload.jsp and emulating
	 * a web browser upload request.
	 * @param peerId	The IP:port/urlPrefix of the remote peer to upload to
	 * @param communityId	The community Id of the resource being uploaded
	 * @param resourceFilePath	The absolute file path to the resource file
	 * @param attachmentFilePaths	A list of absolute file paths to any required resource files
	 */
    private void pushResource(String peerId, String communityId, String resourceFilePath, List<String> attachmentFilePaths) throws IOException {
        String urlString = "http://" + peerId + "/upload";
        HttpURLConnection uploadConnection = null;
        DataOutputStream connOutput = null;
        FileInputStream fileInput = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "232404jkg4220957934FW";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        try {
            File resourceFile = new File(resourceFilePath);
            if (!resourceFile.exists()) {
                LOG.error("JTellaAdapter: Resource file could not be found for push: " + resourceFilePath);
                return;
            }
            List<File> attachments = new ArrayList<File>();
            for (String attachmentPath : attachmentFilePaths) {
                File attachFile = new File(attachmentPath);
                if (!attachFile.exists()) {
                    LOG.error("JTellaAdapter: Attachment file could not be found for push: " + attachmentPath);
                    return;
                }
                attachments.add(attachFile);
            }
            LOG.debug("JTellaAdapter: Initiating push to: " + urlString);
            URL url = new URL(urlString);
            uploadConnection = (HttpURLConnection) url.openConnection();
            uploadConnection.setDoInput(true);
            uploadConnection.setDoOutput(true);
            uploadConnection.setUseCaches(false);
            uploadConnection.setRequestMethod("POST");
            uploadConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            uploadConnection.setRequestProperty("Connection", "Keep-Alive");
            uploadConnection.setRequestProperty("User-Agent", "UP2P");
            uploadConnection.setRequestProperty("Accept", "[star]/[star]");
            connOutput = new DataOutputStream(uploadConnection.getOutputStream());
            connOutput.writeBytes(twoHyphens + boundary + lineEnd);
            connOutput.writeBytes("Content-Disposition: form-data; name=\"up2p:community\"" + lineEnd + lineEnd);
            connOutput.writeBytes(communityId + lineEnd);
            connOutput.writeBytes(twoHyphens + boundary + lineEnd);
            connOutput.writeBytes("Content-Disposition: form-data; name=\"up2p:pushupload\"" + lineEnd + lineEnd + "true" + lineEnd);
            connOutput.writeBytes(twoHyphens + boundary + lineEnd);
            boolean fileWriteComplete = false;
            boolean resourceFileWritten = false;
            File nextFile = null;
            while (!fileWriteComplete) {
                if (!resourceFileWritten) {
                    nextFile = resourceFile;
                } else {
                    nextFile = attachments.remove(0);
                }
                LOG.debug("JTellaAdapter: PUSHing file: " + nextFile.getAbsolutePath());
                connOutput.writeBytes("Content-Disposition: form-data; name=\"up2p:filename\";" + " filename=\"" + nextFile.getName() + "\"" + lineEnd);
                connOutput.writeBytes(lineEnd);
                fileInput = new FileInputStream(nextFile);
                bytesAvailable = fileInput.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                bytesRead = fileInput.read(buffer, 0, bufferSize);
                while (bytesRead > 0) {
                    connOutput.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInput.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInput.read(buffer, 0, bufferSize);
                }
                connOutput.writeBytes(lineEnd);
                if (attachments.isEmpty()) {
                    connOutput.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                } else {
                    connOutput.writeBytes(twoHyphens + boundary + lineEnd);
                }
                resourceFileWritten = true;
                if (attachments.isEmpty()) {
                    fileWriteComplete = true;
                }
            }
            BufferedReader inStream = new BufferedReader(new InputStreamReader(uploadConnection.getInputStream()));
            while (inStream.readLine() != null) ;
            inStream.close();
            LOG.debug("JTellaAdapter: Push upload was succesful.");
        } catch (MalformedURLException ex) {
            LOG.error("JTellaAdapter: pushResource Malformed URL: " + ex);
            throw new IOException("pushResource failed for URL: " + urlString);
        } catch (IOException ioe) {
            LOG.error("JTellaAdapter: pushResource IOException: " + ioe);
            throw new IOException("pushResource failed for URL: " + urlString);
        } finally {
            try {
                if (fileInput != null) {
                    fileInput.close();
                }
                if (connOutput != null) {
                    connOutput.flush();
                }
                if (connOutput != null) {
                    connOutput.close();
                }
                if (uploadConnection != null) {
                    uploadConnection.disconnect();
                }
            } catch (IOException e) {
                LOG.error("JTellaAdapter: pushResource failed to close connection streams.");
            }
        }
    }

    /**
	 * Distributes a list of hosted resources for the specified community to
	 * in response to any search reply messages that have yet to be serviced.
	 * @param communityId	Community ID of all listed resources
	 * @param resourceIds	A list of all resource IDs served in the specified community
	 */
    public void updateResourceList(String queryId, String communityId, List<String> resourceIds) {
        LOG.debug("JTellaAdapter: Got resource list for community: " + communityId);
        synchronized (hostedResListCache) {
            hostedResListCache.put(communityId, resourceIds);
        }
        List<String> unservicedQueries = null;
        synchronized (requestedResourceLists) {
            if (requestedResourceLists.get(communityId) == null) {
                LOG.debug("JTellaAdapter: No outstanding requests for this resource list.");
                return;
            } else {
                unservicedQueries = requestedResourceLists.remove(communityId);
            }
        }
        LOG.debug("JTellaAdapter: Sending resource list in response to " + unservicedQueries.size() + " unserviced search reply messages.");
        for (String oldQueryId : unservicedQueries) {
            SearchResponseMessage responseMessage = new SearchResponseMessage(oldQueryId, communityId, getUrlPrefix());
            responseMessage.setHostedResIdList(resourceIds);
            StringWriter sw = new StringWriter();
            Node requestXML = responseMessage.serialize();
            try {
                TransformerHelper.encodedTransform((Element) requestXML, "UTF-8", sw, true);
            } catch (IOException e) {
                LOG.error("JTella Adapter:" + e);
                e.printStackTrace();
            }
            sendSearchReplyMessage(sw.toString(), oldQueryId);
        }
    }

    /**
	 * Removes the list of cached resources for a particular community.
	 * @param communityId	The community to clear cached resources for.
	 */
    public void invalidateCachedResourceList(String communityId) {
        LOG.debug("JTellaAdapter: Invalidating cached resource list for community: " + communityId);
        synchronized (hostedResListCache) {
            hostedResListCache.remove(communityId);
        }
    }

    @Override
    public void hostsChanged(HostsChangedEvent he) {
        if (he.getSource() instanceof Connection) {
            Connection c = (Connection) he.getSource();
            String connectionType;
            if (c.getType() == Connection.CONNECTION_OUTGOING) connectionType = "OUTGOING"; else connectionType = "INCOMING";
            String remoteservent = c.getRemoteServentId();
            if (remoteservent == null) {
                remoteservent = "[UnknownServentId]" + c.getConnectedServant();
            }
            int port = c.getConnectedServantPort();
            boolean opening = (c.getStatus() == Connection.STATUS_OK);
            adapter.notifyConnection(remoteservent, port, connectionType, opening);
        }
    }
}
