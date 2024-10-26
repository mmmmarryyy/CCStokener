package org.dcm4chee.xds.repository.mbean;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.UIDDictionary;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DimseRSP;
import org.dcm4che2.net.DimseRSPHandler;
import org.dcm4che2.net.Executor;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.NoPresentationContextException;
import org.dcm4che2.net.PDVOutputStream;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.UserIdentity;
import org.dcm4che2.net.service.StorageCommitmentService;
import org.dcm4che2.util.StringUtils;
import org.dcm4che2.util.UIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentSendCfg extends StorageCommitmentService {

    private static char[] SECRET = { 's', 'e', 'c', 'r', 'e', 't' };

    private Executor executor = new NewThreadExecutor("XDS_STORE");

    private NetworkApplicationEntity remoteAE = new NetworkApplicationEntity();

    private NetworkConnection remoteConn = new NetworkConnection();

    private Device device = new Device("XDS_STORE");

    private NetworkApplicationEntity ae = new NetworkApplicationEntity();

    private NetworkConnection conn = new NetworkConnection();

    private HashMap as2ts = new HashMap();

    private Association assoc;

    private int priority = 0;

    private long shutdownDelay = 1000L;

    private boolean useTLS;

    private String keyStoreURL = "resource:tls/test_sys_1.p12";

    private char[] keyStorePassword = SECRET;

    private char[] keyPassword;

    private String trustStoreURL = "resource:tls/mesa_certs.jks";

    private char[] trustStorePassword = SECRET;

    private String username;

    private String passcode;

    private boolean uidnegrsp;

    private static Logger log = LoggerFactory.getLogger(DocumentSendCfg.class);

    public DocumentSendCfg() {
        remoteAE.setInstalled(true);
        remoteAE.setAssociationAcceptor(true);
        remoteAE.setNetworkConnection(new NetworkConnection[] { remoteConn });
        device.setNetworkApplicationEntity(ae);
        device.setNetworkConnection(conn);
        ae.setNetworkConnection(conn);
        ae.setAssociationInitiator(true);
        ae.setAssociationAcceptor(true);
        ae.register(this);
    }

    public NetworkApplicationEntity getAE() {
        return ae;
    }

    public NetworkConnection getConn() {
        return conn;
    }

    public final void setCalledAET(String called) {
        remoteAE.setAETitle(called);
    }

    public String getCalledAET() {
        return remoteAE.getAETitle();
    }

    public final void setRemoteHost(String hostname) {
        remoteConn.setHostname(hostname);
    }

    public String getRemoteHost() {
        return remoteConn.getHostname();
    }

    public final void setRemotePort(int port) {
        remoteConn.setPort(port);
    }

    public int getRemotePort() {
        return remoteConn.getPort();
    }

    public final void setCallingAET(String calling) {
        ae.setAETitle(calling);
    }

    public String getCallingAET() {
        return ae.getAETitle();
    }

    public final void setLocalHost(String hostname) {
        conn.setHostname(hostname);
    }

    public String getLocalHost() {
        return conn.getHostname();
    }

    public final void setLocalPort(int port) {
        conn.setPort(port);
    }

    public int getLocalPort() {
        return conn.getPort();
    }

    public void setUseTLS(boolean useTLS) {
        this.useTLS = useTLS;
    }

    public boolean isUseTLS() {
        return useTLS;
    }

    public final void setKeyStoreURL(String url) {
        keyStoreURL = url;
    }

    public final String getKeyStoreURL() {
        return keyStoreURL;
    }

    public final void setKeyStorePassword(String pw) {
        keyStorePassword = pw.toCharArray();
    }

    public final String getKeyStorePassword() {
        return new String(keyStorePassword);
    }

    public final void setKeyPassword(String pw) {
        keyPassword = pw.toCharArray();
    }

    public final String getKeyPassword() {
        return new String(keyPassword);
    }

    public final void setTrustStorePassword(String pw) {
        trustStorePassword = pw.toCharArray();
    }

    public final String getTrustStorePassword() {
        return new String(trustStorePassword);
    }

    public final void setTrustStoreURL(String url) {
        trustStoreURL = url;
    }

    public final String getTrustStoreURL() {
        return trustStoreURL;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setPasscode(String passcode) {
        this.passcode = passcode;
    }

    public String getPasscode() {
        return passcode;
    }

    public void setUidnegrsp(boolean uidnegrsp) {
        this.uidnegrsp = uidnegrsp;
    }

    public boolean isUidnegrsp() {
        return uidnegrsp;
    }

    public final void setTlsWithoutEncyrption() {
        conn.setTlsWithoutEncyrption();
        remoteConn.setTlsWithoutEncyrption();
    }

    public final void setTls3DES_EDE_CBC() {
        conn.setTls3DES_EDE_CBC();
        remoteConn.setTls3DES_EDE_CBC();
    }

    public final void setTlsAES_128_CBC() {
        conn.setTlsAES_128_CBC();
        remoteConn.setTlsAES_128_CBC();
    }

    public final void disableSSLv2Hello() {
        conn.disableSSLv2Hello();
    }

    public final void setTlsNeedClientAuth(boolean needClientAuth) {
        conn.setTlsNeedClientAuth(needClientAuth);
    }

    public final void setUserIdentity(UserIdentity userIdentity) {
        ae.setUserIdentity(userIdentity);
    }

    public final void setShutdownDelay(long shutdownDelay) {
        this.shutdownDelay = shutdownDelay;
    }

    public long getShutdownDelay() {
        return shutdownDelay;
    }

    public final void setConnectTimeout(int connectTimeout) {
        conn.setConnectTimeout(connectTimeout);
    }

    public int getConnectTimeout() {
        return conn.getConnectTimeout();
    }

    public final void setMaxPDULengthReceive(int maxPDULength) {
        ae.setMaxPDULengthReceive(maxPDULength);
    }

    public int getMaxPDULengthReceive() {
        return ae.getMaxPDULengthReceive();
    }

    public final void setMaxOpsInvoked(int maxOpsInvoked) {
        ae.setMaxOpsInvoked(maxOpsInvoked);
    }

    public int getMaxOpsInvoked() {
        return ae.getMaxOpsInvoked();
    }

    public final void setPackPDV(boolean packPDV) {
        ae.setPackPDV(packPDV);
    }

    public boolean isPackPDV() {
        return ae.isPackPDV();
    }

    public final void setDimseRspTimeout(int timeout) {
        ae.setDimseRspTimeout(timeout);
    }

    public int getDimseRspTimeout() {
        return ae.getDimseRspTimeout();
    }

    public final void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public final void setTcpNoDelay(boolean tcpNoDelay) {
        conn.setTcpNoDelay(tcpNoDelay);
    }

    public boolean isTcpNoDelay() {
        return conn.isTcpNoDelay();
    }

    public final void setAcceptTimeout(int timeout) {
        conn.setAcceptTimeout(timeout);
    }

    public int getAcceptTimeout() {
        return conn.getAcceptTimeout();
    }

    public final void setReleaseTimeout(int timeout) {
        conn.setReleaseTimeout(timeout);
    }

    public int getReleaseTimeout() {
        return conn.getReleaseTimeout();
    }

    public final void setSocketCloseDelay(int timeout) {
        conn.setSocketCloseDelay(timeout);
    }

    public int getSocketCloseDelay() {
        return conn.getSocketCloseDelay();
    }

    public final void setMaxPDULengthSend(int maxPDULength) {
        ae.setMaxPDULengthSend(maxPDULength);
    }

    public int getMaxPDULengthSend() {
        return ae.getMaxPDULengthSend();
    }

    public final void setReceiveBufferSize(int bufferSize) {
        conn.setReceiveBufferSize(bufferSize);
    }

    public int getReceiveBufferSize() {
        return conn.getReceiveBufferSize();
    }

    public final void setSendBufferSize(int bufferSize) {
        conn.setSendBufferSize(bufferSize);
    }

    public int getSendBufferSize() {
        return conn.getSendBufferSize();
    }

    public void initTLS() throws GeneralSecurityException, IOException {
        KeyStore keyStore = loadKeyStore(keyStoreURL, keyStorePassword);
        KeyStore trustStore = loadKeyStore(trustStoreURL, trustStorePassword);
        device.initTLS(keyStore, keyPassword != null ? keyPassword : keyStorePassword, trustStore);
    }

    private static KeyStore loadKeyStore(String url, char[] password) throws GeneralSecurityException, IOException {
        KeyStore key = KeyStore.getInstance(toKeyStoreType(url));
        InputStream in = openFileOrURL(url);
        try {
            key.load(in, password);
        } finally {
            in.close();
        }
        return key;
    }

    public Association connect() throws IOException, ConfigurationException, InterruptedException {
        return ae.connect(remoteAE, executor);
    }

    private static InputStream openFileOrURL(String url) throws IOException {
        if (url.startsWith("resource:")) {
            return DocumentSendCfg.class.getClassLoader().getResourceAsStream(url.substring(9));
        }
        try {
            return new URL(url).openStream();
        } catch (MalformedURLException e) {
            return new FileInputStream(url);
        }
    }

    private static String toKeyStoreType(String fname) {
        return fname.endsWith(".p12") || fname.endsWith(".P12") ? "PKCS12" : "JKS";
    }
}
