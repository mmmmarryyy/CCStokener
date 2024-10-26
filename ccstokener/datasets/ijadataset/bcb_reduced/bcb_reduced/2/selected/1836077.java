package edu.harvard.iq.safe.saasystem.etl;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import edu.harvard.iq.safe.saasystem.etl.dao.DaemonStatusDatabaseUtil;
import edu.harvard.iq.safe.saasystem.etl.dao.MySQLArchivalUnitStatusDAO;
import edu.harvard.iq.safe.saasystem.etl.dao.MySQLAuOverviewDAO;
import edu.harvard.iq.safe.saasystem.etl.dao.MySQLCrawlStatusDAO;
import edu.harvard.iq.safe.saasystem.etl.dao.MySQLDAOFactory;
import edu.harvard.iq.safe.saasystem.etl.dao.MySQLLockssBoxDAO;
import edu.harvard.iq.safe.saasystem.etl.dao.MySQLPollsDAO;
import edu.harvard.iq.safe.saasystem.etl.dao.MySQLRepositorySpaceDAO;
import edu.harvard.iq.safe.saasystem.etl.dao.MySQLSuccessfulPollsDAO;
import edu.harvard.iq.safe.saasystem.etl.dao.MySQLSuccessfulReplicaIpDAO;
import edu.harvard.iq.safe.saasystem.etl.http.HttpClientDAO;
import edu.harvard.iq.safe.saasystem.etl.http.HttpClientPlatformStatusDAO;
import edu.harvard.iq.safe.saasystem.etl.http.HttpClientV3PollerDetailTableDAO;
import edu.harvard.iq.safe.saasystem.etl.util.lockss.LOCKSSDaemonStatusTableTO;
import edu.harvard.iq.safe.saasystem.etl.util.sql.DAOFactory;
import edu.harvard.iq.safe.saasystem.util.ConfigFile;
import edu.harvard.iq.safe.saasystem.util.SAASConfigurationRegistryBean;
import edu.harvard.iq.safe.saasystem.util.SAASEJBConstants;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author asone
 */
public class ETLFacade {

    static final Logger logger = Logger.getLogger(ETLFacade.class.getName());

    static String SAAS_EJB_JNDI_MODULE_NAME = SAASEJBConstants.SAAS_EJB_JNDI_MODULE_NAME_DEFAULT;

    static {
        if (StringUtils.isNotBlank(System.getProperty(SAASEJBConstants.SAAS_EJB_JNDI_MODULE_NAME_KEY))) {
            SAAS_EJB_JNDI_MODULE_NAME = System.getProperty(SAASEJBConstants.SAAS_EJB_JNDI_MODULE_NAME_KEY);
            logger.log(Level.INFO, "user-defined saasEjbJndiModuleName is available:{0}", SAAS_EJB_JNDI_MODULE_NAME);
        } else {
            logger.log(Level.INFO, "user-defined saasEjbJndiModuleName is not available; use the default one:{0}", SAAS_EJB_JNDI_MODULE_NAME);
        }
    }

    SAASConfigurationRegistryBean saasConfigRegistry = null;

    ConfigFile configFile = null;

    InitialContext ic = null;

    String daemonStatusPort;

    String daemonStatusAccessAccount;

    String daemonStatusAccessPassword;

    String saasTimeZone;

    ConcurrentHashMap<String, String> ipAddressToTimezoneOffsetMap = new ConcurrentHashMap<String, String>();

    XStream xstream = new XStream(new JsonHierarchicalStreamDriver());

    private long boxId;

    int etlHttprequestTimeout;

    {
        try {
            ic = new InitialContext();
            logger.log(Level.INFO, "JNDI MODULE NAME={0}", SAAS_EJB_JNDI_MODULE_NAME);
            logger.log(Level.INFO, "SAASConfigurationRegistryBean.class.getSimpleName()={0}", SAASConfigurationRegistryBean.class.getSimpleName());
            logger.log(Level.INFO, "ConfigFile.class.getSimpleName()={0}", ConfigFile.class.getSimpleName());
            saasConfigRegistry = (SAASConfigurationRegistryBean) ic.lookup(SAAS_EJB_JNDI_MODULE_NAME + SAASConfigurationRegistryBean.class.getSimpleName());
            configFile = (ConfigFile) ic.lookup(SAAS_EJB_JNDI_MODULE_NAME + ConfigFile.class.getSimpleName());
        } catch (NamingException ex) {
            logger.log(Level.SEVERE, "Class {0} - JNDI lookup failed:", ETLFacade.class);
        }
    }

    /**
     *
     * @param box
     */
    public void setBoxId(long box) {
        this.boxId = box;
    }

    /**
     *
     * @return
     */
    public long getBoxId() {
        return boxId;
    }

    private String ipAddress = null;

    /**
     *
     * @return
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     *
     * @param ip
     */
    public void setIpAddress(String ip) {
        this.ipAddress = ip;
    }

    String plnGroupName;

    public String getPlnGroupName() {
        return plnGroupName;
    }

    /**
     *
     */
    public ETLFacade() {
        initialize();
    }

    /**
     *
     * @param box
     * @param ip
     */
    public ETLFacade(long box, String ip) {
        this.boxId = box;
        this.ipAddress = ip;
        initialize();
    }

    private void initialize() {
        logger.log(Level.INFO, "++++++++++++++++++ ETLFacade: initialize(): start ++++++++++++++++++");
        daemonStatusAccessAccount = saasConfigRegistry.getDaemonStatusAccessAccount();
        logger.log(Level.INFO, "daemonStatusAccessAccount={0}", daemonStatusAccessAccount);
        daemonStatusAccessPassword = saasConfigRegistry.getDaemonStatusAccessPassword();
        logger.log(Level.FINEST, "daemonStatusAccessPassword={0}", daemonStatusAccessPassword);
        daemonStatusPort = saasConfigRegistry.getDaemonStatusPort();
        logger.log(Level.INFO, "daemonStatusPort={0}", daemonStatusPort);
        saasTimeZone = configFile.getSaastimezone();
        logger.log(Level.INFO, "saasTimeZone={0}", saasTimeZone);
        logger.log(Level.INFO, "++++++++++++++++++ ETLFacade: initialize(): end ++++++++++++++++++");
        etlHttprequestTimeout = saasConfigRegistry.getEtlHttprequestTimeout();
    }

    /**
     *
     */
    public void collectDataFromDaemonStatusTables(CopyOnWriteArraySet<String> rawPeerIpSet) {
        logger.log(Level.INFO, "++++++++ ETLFacade: collectDataFromDaemonStatusTables(): start ++++++++");
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("ETLFacade", ETLFacade.class);
        if (rawPeerIpSet == null) {
            logger.log(Level.WARNING, "peer Ip Set is null");
            plnGroupName = "NA";
            return;
        } else if (rawPeerIpSet.isEmpty()) {
            logger.log(Level.WARNING, "peer Ip Set is empty:");
            plnGroupName = "NA";
            return;
        } else {
            logger.log(Level.INFO, "raw peer ip Set: members={0}", rawPeerIpSet);
        }
        CopyOnWriteArraySet<String> peerIpSet = new CopyOnWriteArraySet<String>();
        for (String rawIp : rawPeerIpSet) {
            peerIpSet.add(rawIp.replaceAll("\\s", ""));
        }
        logger.log(Level.INFO, "sanitized peer ip Set: members={0}", peerIpSet.toString());
        DefaultHttpClient httpClient = null;
        long currentBoxId = 0L;
        String currentDaemonVersion = null;
        List<String> excludedIps = new ArrayList<String>();
        MySQLDAOFactory daof = null;
        boolean firstTimeInsert = true;
        Set<String> groupNameSet = new LinkedHashSet<String>();
        IP: for (String ip : peerIpSet) {
            currentBoxId++;
            currentDaemonVersion = null;
            setBoxId(currentBoxId);
            setIpAddress(ip);
            String hostname = null;
            boolean isProtocolHttp = true;
            httpClient = getTestHttpClient(Integer.parseInt(daemonStatusPort));
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, etlHttprequestTimeout);
            HttpConnectionParams.setSoTimeout(params, etlHttprequestTimeout);
            httpClient.setParams(params);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(ipAddress, Integer.parseInt(daemonStatusPort)), new UsernamePasswordCredentials(daemonStatusAccessAccount, daemonStatusAccessPassword));
            Set<String> targetPollIdSet = null;
            TABLE: for (TargetDaemonStatusTable targetTableName : TargetDaemonStatusTable.values()) {
                String tableId = targetTableName.getTableName();
                LOCKSSDaemonStatusTableTO ldstTO = null;
                if (!tableId.equals("V3PollerDetailTable") && !tableId.equals("PlatformStatus")) {
                    HttpClientDAO hcdao = isProtocolHttp ? new HttpClientDAO(httpClient, ipAddress, tableId) : new HttpClientDAO(httpClient, hostname, tableId, "https");
                    try {
                        ldstTO = isProtocolHttp ? hcdao.getDataFromDaemonStatusTable() : hcdao.getDataFromDaemonStatusTableByHttps();
                        ldstTO.setBoxId(boxId);
                        ldstTO.setIpAddress(ipAddress);
                        if (StringUtils.isNotBlank(currentDaemonVersion)) {
                            ldstTO.setDaemonVersion(currentDaemonVersion);
                        }
                        logger.log(Level.INFO, "timezone offset ={0}", ipAddressToTimezoneOffsetMap.get(ipAddress));
                        ldstTO.setTimezoneOffset(ipAddressToTimezoneOffsetMap.get(ipAddress));
                        logger.log(Level.FINE, "tableId={0}: ", tableId);
                        logger.log(Level.FINE, "returned contents:\n{0}", xstream.toXML(ldstTO));
                        if (!ldstTO.isPageReadable()) {
                            logger.log(Level.INFO, "ip={0}: table ={1} was excluded due to a page error", new Object[] { ip, tableId });
                            continue TABLE;
                        }
                        if (tableId.equals("V3PollerTable")) {
                            targetPollIdSet = ldstTO.getTargetPollIdSet();
                            if (targetPollIdSet != null) {
                                logger.log(Level.INFO, "The size of targetPollIdSdet={0}", targetPollIdSet.size());
                            } else if (targetPollIdSet == null) {
                                logger.log(Level.WARNING, "targetPollIdSet is still null");
                            }
                        }
                    } catch (HttpResponseException ex) {
                        logger.log(Level.WARNING, "http client connection failed when {0} table was called: for Ip={1}", new Object[] { tableId, ip });
                        continue TABLE;
                    }
                } else if (tableId.equals("PlatformStatus")) {
                    HttpClientDAO hcdao = new HttpClientPlatformStatusDAO(httpClient, ipAddress, tableId);
                    try {
                        ldstTO = hcdao.getDataFromDaemonStatusTable();
                        if (!ldstTO.isHttpProtocol()) {
                            isProtocolHttp = false;
                            try {
                                InetAddress address = InetAddress.getByName(ip);
                                hostname = address.getHostName();
                                logger.log(Level.INFO, "hostname={0}", hostname);
                            } catch (UnknownHostException ex) {
                                logger.log(Level.SEVERE, "unknown host", ex);
                                hostname = ipAddress;
                            }
                            String loginUrl = "https://" + hostname + ":" + hcdao.getPortNumber() + "/j_security_check";
                            logger.log(Level.INFO, "loginUrl={0}", loginUrl);
                            HttpPost httpost = new HttpPost(loginUrl);
                            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                            nvps.add(new BasicNameValuePair("j_username", daemonStatusAccessAccount));
                            nvps.add(new BasicNameValuePair("j_password", daemonStatusAccessPassword));
                            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
                            HttpResponse resp = httpClient.execute(httpost);
                            HttpEntity entity = resp.getEntity();
                            logger.log(Level.INFO, "Login form get: {}", resp.getStatusLine());
                            logger.log(Level.INFO, "Login form get: {}", resp.getStatusLine());
                            EntityUtils.consume(entity);
                            logger.log(Level.INFO, "Post logon cookies:");
                            List<Cookie> cookies = httpClient.getCookieStore().getCookies();
                            if (cookies.isEmpty()) {
                                logger.log(Level.INFO, "cookies: None");
                            } else {
                                if (logger.isLoggable(Level.INFO)) {
                                    for (int i = 0; i < cookies.size(); i++) {
                                        logger.log(Level.INFO, "{0}-th cookies={1}", new Object[] { i, cookies.get(i).toString() });
                                    }
                                }
                            }
                            hcdao = new HttpClientPlatformStatusDAO(httpClient, hostname, tableId, "https");
                            ldstTO = hcdao.getDataFromDaemonStatusTableByHttps();
                        }
                        if (!ldstTO.isBoxHttpStatusOK()) {
                            excludedIps.add(ip);
                            continue IP;
                        }
                        ldstTO.setBoxId(boxId);
                        ldstTO.setIpAddress(ipAddress);
                        logger.log(Level.FINE, "tableId={0}: ", tableId);
                        logger.log(Level.FINE, "returned contents:\n{0}", xstream.toXML(ldstTO));
                        if (ldstTO.getSummaryInfoMap().containsKey("Group")) {
                            String groupName = ldstTO.getSummaryInfoMap().get("Group");
                            if (StringUtils.isNotBlank(groupName)) {
                                logger.log(Level.INFO, "group name={0}", groupName);
                                groupNameSet.add(groupName);
                            } else {
                                logger.log(Level.WARNING, "group name is empty");
                            }
                        } else if (ldstTO.getSummaryInfoMap().containsKey("Groups")) {
                            String groupNames = ldstTO.getSummaryInfoMap().get("Groups");
                            if (StringUtils.isNotBlank(groupNames)) {
                                String[] gNames = groupNames.split(", ");
                                logger.log(Level.INFO, "how many group names={0}", gNames.length);
                                logger.log(Level.INFO, "1st group name={0}", gNames[0]);
                                String joineGroupName = StringUtils.join(gNames, ";");
                                ldstTO.getSummaryInfoMap().put("Group", joineGroupName);
                                logger.log(Level.INFO, "joined group name={0}", joineGroupName);
                                groupNameSet.addAll(Arrays.asList(gNames));
                            } else {
                                logger.log(Level.WARNING, "group name set is empty");
                            }
                        }
                        String daemonVersion = ldstTO.getBoxInfoMap().get("version");
                        if (StringUtils.isNotBlank(daemonVersion)) {
                            logger.log(Level.INFO, "daemon Version: {0}", daemonVersion);
                            currentDaemonVersion = daemonVersion;
                            ldstTO.setDaemonVersion(daemonVersion);
                        }
                        saasConfigRegistry.getIpToTimezoneOffsetMap().putIfAbsent(ipAddress, ldstTO.getTimezoneOffset());
                        ipAddressToTimezoneOffsetMap.putIfAbsent(ipAddress, ldstTO.getTimezoneOffset());
                        logger.log(Level.INFO, "timezone offset={0}", ldstTO.getTimezoneOffset());
                    } catch (HttpResponseException ex) {
                        logger.log(Level.WARNING, "http client connection failed when PlatformStatus table was called: for Ip={0}", ip);
                        excludedIps.add(ip);
                        continue IP;
                    } catch (UnsupportedEncodingException ex) {
                        logger.log(Level.SEVERE, "UnsupportedEncodingException", ex);
                        excludedIps.add(ip);
                        continue IP;
                    } catch (UnknownHostException ex) {
                        logger.log(Level.SEVERE, "UnknownHostException", ex);
                        excludedIps.add(ip);
                        continue IP;
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "IOException", ex);
                        excludedIps.add(ip);
                        continue IP;
                    }
                } else if (tableId.equals("V3PollerDetailTable")) {
                    HttpClientV3PollerDetailTableDAO hcdao = isProtocolHttp ? new HttpClientV3PollerDetailTableDAO(httpClient, ipAddress, tableId) : new HttpClientV3PollerDetailTableDAO(httpClient, hostname, tableId, "https");
                    logger.log(Level.INFO, "saved timezone offset ={0}", ipAddressToTimezoneOffsetMap.get(ipAddress));
                    hcdao.setTimezoneOffset(ipAddressToTimezoneOffsetMap.get(ipAddress));
                    logger.log(Level.INFO, "timezone offset ={0}", hcdao.getTimezoneOffset());
                    hcdao.setTargetPollIdSet(targetPollIdSet);
                    try {
                        ldstTO = isProtocolHttp ? hcdao.getDataFromDaemonStatusTable() : hcdao.getDataFromDaemonStatusTableByHttps();
                        ldstTO.setBoxId(boxId);
                        ldstTO.setIpAddress(ipAddress);
                        if (StringUtils.isNotBlank(currentDaemonVersion)) {
                            ldstTO.setDaemonVersion(currentDaemonVersion);
                        }
                        ldstTO.setTimezoneOffset(ipAddressToTimezoneOffsetMap.get(ipAddress));
                        logger.log(Level.FINE, "tableId={0}: ", tableId);
                        logger.log(Level.FINE, "returned contents:\n{0}", xstream.toXML(ldstTO));
                        if (!ldstTO.isPageReadable()) {
                            logger.log(Level.INFO, "ip={0}: table ={1} was excluded due to a page error", new Object[] { ip, targetTableName });
                            continue TABLE;
                        }
                    } catch (HttpResponseException ex) {
                        logger.log(Level.WARNING, "http client connection failed when V3PollerDetailTable was called: for Ip={0}", ip);
                        excludedIps.add(ip);
                        continue TABLE;
                    }
                }
                if (firstTimeInsert) {
                    DaemonStatusDatabaseUtil.truncateDaemonStatusTables();
                    firstTimeInsert = false;
                }
                logger.log(Level.INFO, "insert: tableId={0}", tableId);
                daof = (MySQLDAOFactory) DAOFactory.getDAOFactory(DAOFactory.DBvendor.MySQL);
                if (tableId.equals("PlatformStatus")) {
                    MySQLLockssBoxDAO mlbdao = (MySQLLockssBoxDAO) daof.getLockssBoxDAO();
                    mlbdao.setLOCKSSDaemonStatusTableTO(ldstTO);
                    mlbdao.createTable();
                    mlbdao.insertData();
                } else if (tableId.equals("ArchivalUnitStatusTable")) {
                    MySQLArchivalUnitStatusDAO mausdao = (MySQLArchivalUnitStatusDAO) daof.getArchivalUnitStatusDAO();
                    mausdao.setLOCKSSDaemonStatusTableTO(ldstTO);
                    mausdao.createTable();
                    mausdao.insertData();
                } else if (tableId.equals("RepositorySpace")) {
                    MySQLRepositorySpaceDAO mrsdao = (MySQLRepositorySpaceDAO) daof.getRepositorySpaceDAO();
                    mrsdao.setLOCKSSDaemonStatusTableTO(ldstTO);
                    mrsdao.createTable();
                    mrsdao.insertData();
                } else if (tableId.equals("V3PollerTable")) {
                    MySQLPollsDAO mpdao = (MySQLPollsDAO) daof.getPollsDAO();
                    mpdao.setLOCKSSDaemonStatusTableTO(ldstTO);
                    mpdao.createTable();
                    mpdao.insertData();
                } else if (tableId.equals("crawl_status_table")) {
                    MySQLCrawlStatusDAO mcsdao = (MySQLCrawlStatusDAO) daof.getCrawlStatusDAO();
                    mcsdao.setLOCKSSDaemonStatusTableTO(ldstTO);
                    mcsdao.createTable();
                    mcsdao.insertData();
                } else if (tableId.equals("V3PollerDetailTable")) {
                    MySQLSuccessfulPollsDAO mspdao = (MySQLSuccessfulPollsDAO) daof.getSuccessfulPollsDAO();
                    mspdao.setLOCKSSDaemonStatusTableTO(ldstTO);
                    mspdao.createTable();
                    mspdao.insertData();
                    logger.log(Level.FINE, "check new poll ip address data:{0}\n", xstream.toXML(ldstTO.getSuccessfulReplicaIpMap()));
                    MySQLSuccessfulReplicaIpDAO msri = (MySQLSuccessfulReplicaIpDAO) daof.getSuccessfulReplicaIpDAO();
                    msri.setLOCKSSDaemonStatusTableTO(ldstTO);
                    msri.createTable();
                    msri.insertData();
                }
            }
        }
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
        }
        if (excludedIps.size() > 0) {
            logger.log(Level.WARNING, "Some IPs were not reachable: they are:{0}", excludedIps);
            if (excludedIps.size() == peerIpSet.size()) {
                logger.log(Level.SEVERE, "All listed IPs were not reachable to get their daemon-status data");
                logger.log(Level.WARNING, "group name is not available");
                plnGroupName = "NA";
                return;
            }
        } else {
            logger.log(Level.INFO, "All Ips were reachable to get their daemon-status data");
        }
        MySQLAuOverviewDAO maodao = (MySQLAuOverviewDAO) daof.getAuOverviewDAO();
        if (!peerIpSet.isEmpty()) {
            logger.log(Level.INFO, "peerIpSet is not empty");
            ((MySQLLockssBoxDAO) daof.getLockssBoxDAO()).updateRepositorySpaceColumn();
            logger.log(Level.INFO, "filling up au_overview_table");
            maodao.insertData();
            if (maodao.isTableCreated()) {
                logger.log(Level.INFO, "au_overview_table exists");
            } else {
                logger.log(Level.WARNING, "au_overview_table does not exist; try to create it one more time");
                maodao.insertData();
                if (maodao.isTableCreated()) {
                    logger.log(Level.INFO, "au_overview_table exists after the retrial");
                } else {
                    logger.log(Level.WARNING, "au_overview_table still does not exist even after the retrial");
                    logger.log(Level.INFO, "create the empty au_overview_table anyway");
                    maodao.createTable();
                }
            }
        } else {
            logger.log(Level.INFO, "peerIpSet is empty; no data to be saved on the tables");
            logger.log(Level.INFO, "create the empty au_overview_table anyway");
            maodao.createTable();
        }
        if (groupNameSet.size() >= 1) {
            List<String> groupList = new LinkedList<String>();
            groupList.addAll(groupNameSet);
            plnGroupName = groupList.get(0);
            logger.log(Level.INFO, "group name(0)={0}", plnGroupName);
        } else {
            logger.log(Level.WARNING, "group name is not available");
            plnGroupName = "NA";
        }
        logger.log(Level.INFO, "group name = {0}", plnGroupName);
        saasConfigRegistry.getSaasConfigProperties().put("saas.daemonstatus.groupname", plnGroupName);
        logger.log(Level.INFO, "ipAddressToTimezoneOffsetMap={0}", ipAddressToTimezoneOffsetMap);
        logger.log(Level.INFO, "saasConfigRegistry:getIpToTimezoneOffsetMap()={0}", saasConfigRegistry.getIpToTimezoneOffsetMap());
        logger.log(Level.INFO, "++++++++ ETLFacade: collectDataFromDaemonStatusTables(): End ++++++++");
    }

    private DefaultHttpClient getTestHttpClient(int portNumber) {
        try {
            SSLSocketFactory sf = new SSLSocketFactory(new TrustSelfSignedStrategy());
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", portNumber, PlainSocketFactory.getSocketFactory()));
            registry.register(new Scheme("https", portNumber, sf));
            ClientConnectionManager ccm = new ThreadSafeClientConnManager(registry);
            return new DefaultHttpClient(ccm);
        } catch (NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, "NoSuchAlgorithmException", ex);
        } catch (KeyManagementException ex) {
            logger.log(Level.SEVERE, "KeyManagementException", ex);
        } catch (KeyStoreException ex) {
            logger.log(Level.SEVERE, "KeyStoreException", ex);
        } catch (UnrecoverableKeyException ex) {
            logger.log(Level.SEVERE, "UnrecoverableKeyException", ex);
        } finally {
            return new DefaultHttpClient();
        }
    }
}
