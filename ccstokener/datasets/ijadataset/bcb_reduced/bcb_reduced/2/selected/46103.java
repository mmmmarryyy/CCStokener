package ants.p2p.filesharing;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;
import ants.p2p.messages.*;
import ants.p2p.security.*;
import ants.p2p.query.*;
import ants.p2p.query.security.*;
import ants.p2p.utils.addresses.*;
import ants.p2p.utils.encoding.*;
import ants.p2p.*;
import ants.p2p.utils.indexer.*;
import ants.p2p.gui.ConnectingThreadManager;
import ants.p2p.gui.FrameAnt;
import ants.p2p.gui.SwingWorker;
import ants.p2p.http.*;
import kerjodando.casper.util.ConfigUtil;
import org.apache.log4j.*;
import org.cybergarage.upnp.*;

public class WarriorAnt extends Ant {

    java.util.List partialFiles = new ArrayList();

    public java.util.List outputSecureConnections = new ArrayList();

    public java.util.List inputSecureConnections = new ArrayList();

    public java.util.List pendingSecureRequest = new ArrayList();

    public java.util.List inServiceFilePullRequests = new ArrayList();

    public java.util.List inServiceFilePushRequests = new ArrayList();

    public java.util.List pendingFilePullRequests = new ArrayList();

    static int httpProxyPort = 8080;

    HttpProxy httpProxy = null;

    static String httpServerDescription = "My personal Web Server";

    static String httpHomePage = "Default.htm";

    static boolean httpRunning = false;

    static int httpLocalServerPort = 80;

    public java.util.List scheduledHttpProxy = new ArrayList();

    public static int maxScheduledHttpProxy = 100;

    public java.util.List scheduledDownloads = new ArrayList();

    public static int maxScheduledDownloads = 1000;

    public static String workingPath = ConfigUtil.WORKINGPATH;

    public static String ed2kPath = workingPath + "ed2k/";

    public static String previewsPath = workingPath + "previews/";

    public static String chunksHome = workingPath;

    public static String chunksPath = "chunks/";

    public static String downloadPath = workingPath + "downloads/";

    public static int maxPullRequestsToServe = 4;

    public static int maxOwnPullRequestsToTrace = 1000;

    public static int maxSecureConnections = 10000;

    public static int maxPartialFiles = 500;

    public static long partialFilesTimeout = Ant.messageTimeout * Ant.maxRetransmissions;

    public static int blockSizeInDownload = (int) Math.pow(2, 19);

    public static boolean isServer = true;

    public static String password = "";

    public static int processProbability = 45 + (int) (System.currentTimeMillis() % 25);

    public static boolean autoresumeFilesOnRun = true;

    public Integer writingFileLock = new Integer(0);

    static Logger _logger = Logger.getLogger(WarriorAnt.class.getName());

    public WarriorAnt(String id, int maxNeighbours, int serverPort, boolean acceptDC, String localInetAddress, boolean UPnP) {
        super(id, maxNeighbours, serverPort, acceptDC, localInetAddress, UPnP);
        WarriorAnt.processProbability += (int) (System.currentTimeMillis() % 25);
        WarriorAnt.checkChunksPath();
    }

    public synchronized void setWritingFile() {
        this.writingFileLock = new Integer(this.writingFileLock.intValue() + 1);
    }

    public synchronized void resetWritingFile() {
        this.writingFileLock = new Integer(this.writingFileLock.intValue() - 1);
    }

    public void disconnectWarrior() {
        while (this.writingFileLock.intValue() > 0) {
            try {
                sleep(1000);
            } catch (InterruptedException ex) {
            }
        }
        if (InetAddressEngine.getInstance() != null) InetAddressEngine.getInstance().terminate();
        this.disconnect();
    }

    public void scheduleDownload(QueryFileTuple qft) {
        synchronized (this.scheduledDownloads) {
            if (this.scheduledDownloads.contains(qft)) return;
            if (this.scheduledDownloads.size() > WarriorAnt.maxScheduledDownloads) {
                this.scheduledDownloads.remove(0);
            }
            this.scheduledDownloads.add(qft);
        }
    }

    public void unscheduleDownload(String fileHash) {
        synchronized (this.scheduledDownloads) {
            for (int x = this.scheduledDownloads.size() - 1; x >= 0; x--) {
                QueryFileTuple qft = (QueryFileTuple) this.scheduledDownloads.get(x);
                if (fileHash.equals(qft.getFileHash())) {
                    this.scheduledDownloads.remove(x);
                }
            }
        }
    }

    public void scheduleHttpProxy(HttpServerInfo httpServer) {
        if (this.scheduledHttpProxy.contains(httpServer)) return;
        if (this.scheduledHttpProxy.size() > WarriorAnt.maxScheduledHttpProxy) {
            this.scheduledHttpProxy.remove(0);
        }
        this.scheduledHttpProxy.add(httpServer);
    }

    synchronized void checkScheduledDownloads(EndpointSecurityManager esm) throws Exception {
        for (int x = this.scheduledDownloads.size() - 1; x >= 0; x--) {
            QueryFileTuple qft = (QueryFileTuple) this.scheduledDownloads.get(x);
            if (esm.getPeerId().equals(qft.getOwnerID())) {
                this.scheduledDownloads.remove(x);
                this.propertyChangeSupport.firePropertyChange("addSourcePeer", null, qft);
            }
        }
    }

    class ThreadConnectNeighbour extends Thread {

        WarriorAnt antHere;

        String strAddress;

        int intPort;

        ConnectingThreadManager mTm;

        String sURI;

        ThreadConnectNeighbour(ConnectingThreadManager tm_, WarriorAnt ant, String sAddress, int port, String sURI_) {
            antHere = ant;
            mTm = tm_;
            sURI = sURI_;
            strAddress = sAddress;
            intPort = port;
        }

        public void run() {
            try {
                if (antHere.addP2PNeighbour(strAddress, intPort, true, FrameAnt.getInstance(null).getGuiAnt().getConnectionAntPanel().getLocalInetAddress())) {
                    mTm.remove(sURI);
                }
            } catch (Exception e) {
                _logger.error("Failed adding neighbour: " + e.toString());
            }
            mTm.DoNext(sURI);
        }
    }

    ;

    public void ConnectToNeighBours(ConnectingThreadManager tm, String sAddress, int iPort, String sURI) {
        if (this.getNeighboursNumber() < this.getMaxNeighbours()) {
            ThreadConnectNeighbour threadConnectNeighbour = new ThreadConnectNeighbour(tm, this, sAddress, iPort, sURI);
            threadConnectNeighbour.start();
        }
    }

    synchronized void checkScheduledHttpProxy(EndpointSecurityManager esm) {
        for (int x = this.scheduledHttpProxy.size() - 1; x >= 0; x--) {
            HttpServerInfo httpServer = (HttpServerInfo) this.scheduledHttpProxy.get(x);
            if (esm.getPeerId().equals(httpServer.getOwnerId())) {
                this.scheduledHttpProxy.remove(x);
                this.createProxy(esm);
            }
        }
    }

    public static void setHttpServerProperties(String homePage, String description) {
        httpHomePage = homePage;
        httpServerDescription = description;
    }

    public static String getHttpServerDescription() {
        return httpServerDescription;
    }

    public static String getHttpServerHomePage() {
        return httpHomePage;
    }

    public static int getHttpLocalServerPort() {
        return httpLocalServerPort;
    }

    public static int getHttpProxyPort() {
        return httpProxyPort;
    }

    public static void setHttpLocalServerPort(int port) {
        if (port >= 0 && port < 65536) httpLocalServerPort = port;
    }

    public static void setHttpProxyPort(int port) {
        if (port >= 0 && port < 65536) httpProxyPort = port;
    }

    public void getHttpServers() {
        try {
            QueryHttpServerItem httpItem = new QueryHttpServerItem(null);
            AsymmetricProvider ap = new AsymmetricProvider(false);
            QueryMessage qm = new QueryMessage(httpItem, ap.getPublicHeader());
            if (this.getNeighboursNumber() > 0) {
                MessageWrapper wm = this.sendBroadcastMessage(qm);
            }
        } catch (Exception ex) {
            _logger.error("", ex);
        }
    }

    public HttpProxy getCurrentProxy() {
        return this.httpProxy;
    }

    public void resetCurrentProxy() {
        if (this.httpProxy != null) {
            try {
                if (this.httpProxy.terminate()) {
                    this.httpProxy = null;
                    this.propertyChangeSupport.firePropertyChange("currentProxyChanged", null, this.httpProxy);
                }
            } catch (Exception e) {
                _logger.error("Error in stoppoing proxy: " + this.httpProxy.getPort(), e);
            }
        }
    }

    public static boolean isSupernode() {
        return NeighbourAnt.bandwidthLimit > 60 * 1024;
    }

    public void createSecureConnection(final String peerId, final boolean force) {
        final WarriorAnt currentAnt = this;
        if (this.getIdent().equals(peerId)) return;
        Thread secureConnectionCreator = new Thread() {

            public void run() {
                try {
                    if (outputSecureConnections.size() >= maxSecureConnections) return;
                    synchronized (pendingSecureRequest) {
                        for (int x = pendingSecureRequest.size() - 1; x >= 0; x--) {
                            EndpointSecurityManager esm = (EndpointSecurityManager) pendingSecureRequest.get(x);
                            if (System.currentTimeMillis() - esm.getLastTimeUsed() > Ant.messageTimeout * Ant.maxRetransmissions) {
                                pendingSecureRequest.remove(x);
                                esm.resetLastTimeUsed();
                            }
                        }
                        for (int x = pendingSecureRequest.size() - 1; x >= 0; x--) {
                            EndpointSecurityManager esm = (EndpointSecurityManager) pendingSecureRequest.get(x);
                            if (esm.getPeerId().equals(peerId) && !force) return; else if (esm.getPeerId().equals(peerId) && force) {
                                pendingSecureRequest.remove(x);
                                esm.resetLastTimeUsed();
                            }
                        }
                        _logger.info(getShortId() + " Creating secure connection");
                        for (int x = outputSecureConnections.size() - 1; x >= 0; x--) {
                            EndpointSecurityManager esm = (EndpointSecurityManager) outputSecureConnections.get(x);
                            if (esm.getPeerId().equals(peerId)) {
                                outputSecureConnections.remove(esm);
                                esm.resetLastTimeUsed();
                            }
                        }
                        ants.p2p.security.EndpointSecurityManager esm = new ants.p2p.security.EndpointSecurityManager(currentAnt, peerId);
                    }
                } catch (Exception e) {
                    _logger.error("Secure connection creation error", e);
                }
            }
        };
        secureConnectionCreator.setPriority(10);
        secureConnectionCreator.start();
    }

    public void removeInputSecureConnection(String peerId) {
        for (int x = this.inputSecureConnections.size() - 1; x >= 0; x--) {
            if (((EndpointSecurityManager) this.inputSecureConnections.get(x)).getPeerId().equals(peerId)) this.inputSecureConnections.remove(this.inputSecureConnections.get(x));
        }
    }

    public void removeOutputSecureConnection(String peerId) {
        for (int x = this.outputSecureConnections.size() - 1; x >= 0; x--) {
            if (((EndpointSecurityManager) this.outputSecureConnections.get(x)).getPeerId().equals(peerId)) this.outputSecureConnections.remove(this.outputSecureConnections.get(x));
        }
    }

    public void sendPrivateChatMessage(String peerId, String message, boolean initiator) throws Exception {
        ants.p2p.security.EndpointSecurityManager esm = initiator ? this.getOutputSecureConnectionManager(peerId) : this.getInputSecureConnectionManager(peerId);
        if (esm == null) {
            SecureConnectionErrorControlMessage fterrcm = new SecureConnectionErrorControlMessage(new Integer(0), null, "No secure connection");
            MessageWrapper wm = this.sendMessage(fterrcm, peerId, false, false);
            _logger.debug(this.getShortId() + ": No secure connection with " + peerId);
            while (this.myMessages.contains(wm) && !this.isDisconnected()) {
                Thread.sleep(1000);
            }
            if (this.failedMessages.contains(wm)) {
                this.failedMessages.remove(wm);
                _logger.debug(this.getShortId() + ": Error in sending ControlMessage " + fterrcm.getMessage());
            }
            throw new Exception("No secure connection avaiable with endpoint " + peerId);
        }
        PrivateChatMessage pcm = new PrivateChatMessage(message);
        MessageWrapper wm = null;
        pcm.encrypt(esm.getCipherEnc());
        wm = this.sendMessage(pcm, peerId, true, false);
        this.propertyChangeSupport.firePropertyChange("privateChatMessageSending", null, pcm);
        while (this.myMessages.contains(wm)) {
            Thread.sleep(1000);
        }
        if (this.failedMessages.contains(wm)) {
            pcm.decrypt(esm.getCipherDec());
            this.failedMessages.remove(wm);
            this.propertyChangeSupport.firePropertyChange("privateChatMessageFailed", null, pcm);
            return;
        }
        pcm.decrypt(esm.getCipherDec());
        this.propertyChangeSupport.firePropertyChange("privateChatMessageDelivered", null, pcm);
    }

    public void pullFile(String fileName, String fileHash, long offset, long blocks, String peerId, int blockSize, String localFileName, boolean resume) throws Exception {
        ants.p2p.security.EndpointSecurityManager esm = this.getOutputSecureConnectionManager(peerId);
        if (esm == null) {
            throw new Exception("No secure connection avaiable with endpoint " + peerId);
        }
        FilePullMessage frm = new FilePullMessage(fileName, Base16.fromHexString(fileHash), new Long(offset), new Long(blocks), new Integer(blockSize), localFileName, new Boolean(resume));
        MessageWrapper wm = null;
        frm.encrypt(esm.getCipherEnc());
        wm = this.sendMessage(frm, peerId, false, false);
        synchronized (this.pendingFilePullRequests) {
            FilePullMessage frmCopia = new FilePullMessage(frm, fileName, Base16.fromHexString(fileHash), new Long(offset), new Long(blocks), new Integer(blockSize), localFileName, new Boolean(resume));
            boolean removed = false;
            for (int x = this.pendingFilePullRequests.size() - 1; x >= 0; x--) {
                FilePullMessage fpr = ((FilePullMessageWrapper) this.pendingFilePullRequests.get(x)).getFilePullMessage();
                if (fpr.getDest().equals(frmCopia.getDest()) && compareHash(fpr.getHash(), frmCopia.getHash()) && fpr.getLocalFileName().equals(frmCopia.getLocalFileName()) && fpr.getBlocks().equals(frmCopia.getBlocks()) && fpr.getBlockSize().equals(frmCopia.getBlockSize()) && fpr.getOffset().equals(frmCopia.getOffset())) {
                    this.pendingFilePullRequests.remove(x);
                    removed = true;
                }
            }
            for (int x = this.inServiceFilePushRequests.size() - 1; x >= 0; x--) {
                FilePullMessage fpr = (FilePullMessage) (this.inServiceFilePushRequests.get(x));
                if (fpr.getDest().equals(frmCopia.getDest()) && compareHash(fpr.getHash(), frmCopia.getHash()) && fpr.getLocalFileName().equals(frmCopia.getLocalFileName()) && fpr.getBlocks().equals(frmCopia.getBlocks()) && fpr.getBlockSize().equals(frmCopia.getBlockSize()) && fpr.getOffset().equals(frmCopia.getOffset())) {
                    this.inServiceFilePushRequests.remove(x);
                    removed = true;
                }
            }
            if (!removed && this.pendingFilePullRequests.size() >= WarriorAnt.maxOwnPullRequestsToTrace) this.pendingFilePullRequests.remove(0);
            this.pendingFilePullRequests.add(new FilePullMessageWrapper(frmCopia));
        }
        while (this.myMessages.contains(wm)) {
            Thread.sleep(1000);
        }
        if (this.failedMessages.contains(wm)) {
            frm.decrypt(esm.getCipherDec());
            this.failedMessages.remove(wm);
            _logger.debug(this.getShortId() + ": Error in sending PullFileMessage " + frm.getFileName() + " - Cannot send pull request to id " + frm.getDest());
            this.propertyChangeSupport.firePropertyChange("filePullRequestError", null, frm);
            return;
        }
    }

    public void pullFileInfos(String hash, String peerId, boolean getChunkHashes) throws Exception {
        ants.p2p.security.EndpointSecurityManager esm = this.getOutputSecureConnectionManager(peerId);
        if (esm == null) {
            throw new Exception("No secure connection avaiable with endpoint " + peerId);
        }
        FileInfosPullMessage frm = new FileInfosPullMessage(Base16.fromHexString(hash), getChunkHashes);
        MessageWrapper wm = null;
        frm.encrypt(esm.getCipherEnc());
        wm = this.sendMessage(frm, peerId, false, false);
        while (this.myMessages.contains(wm)) {
            Thread.sleep(1000);
        }
        if (this.failedMessages.contains(wm)) {
            this.failedMessages.remove(wm);
            frm.decrypt(esm.getCipherDec());
            _logger.debug(this.getShortId() + ": Error in sending PullFileInfosMessage " + Base16.toHexString(frm.getHash()) + " - Cannot send pull request to id " + frm.getDest());
            throw new Exception("Error in sending PullFileInfosMessage " + peerId);
        }
    }

    public void pullFileSize(String fileName, String fileHash, String peerId) throws Exception {
        ants.p2p.security.EndpointSecurityManager esm = this.getOutputSecureConnectionManager(peerId);
        if (esm == null) throw new Exception("No secure connection avaiable with endpoint " + peerId);
        FileSizePullMessage fsrm = new FileSizePullMessage(fileName, Base16.fromHexString(fileHash));
        fsrm.encrypt(esm.getCipherEnc());
        MessageWrapper wm = this.sendMessage(fsrm, peerId, true, false);
        while (this.myMessages.contains(wm)) {
            Thread.sleep(1000);
        }
        if (this.failedMessages.contains(wm)) {
            this.failedMessages.remove(wm);
            fsrm.decrypt(esm.getCipherDec());
            _logger.debug(this.getShortId() + ": Error in sending PullFileSizeMessage " + fsrm.getFileName() + " - Cannot send pull request to id " + fsrm.getDest());
            this.propertyChangeSupport.firePropertyChange("fileSizePullError", null, fsrm);
            return;
        }
    }

    public void getALanServer() {
    }

    public MessageWrapper doSupernodeQuery() {
        try {
            QuerySupernodeItem qsi = new QuerySupernodeItem(null);
            AsymmetricProvider ap = new AsymmetricProvider(false);
            QueryMessage qm = new QueryMessage(qsi, ap.getPublicHeader());
            if (this.getNeighboursNumber() > 0) {
                MessageWrapper wm = this.sendBroadcastMessage(qm);
                return wm;
            } else {
                return null;
            }
        } catch (Exception ex) {
            _logger.error("", ex);
            return null;
        }
    }

    public synchronized ArrayList getServersWithFreeSlots(InetAddress host, int port, InetAddress localhost) {
        Socket local = null;
        try {
            if (!InetAddressWatchdog.getInstance().allowedAddress(host.getHostAddress())) return null;
            local = ants.p2p.security.sockets.SSLProvider.getSSLSecuredSocket(host.getHostAddress(), port, proxied);
            local.getOutputStream().write(1);
            local.getOutputStream().flush();
            ObjectInputStream ois = new ObjectInputStream(local.getInputStream());
            ArrayList peers = (ArrayList) ois.readObject();
            for (int x = peers.size() - 1; x >= 0; x--) {
                ServerInfo peer = (ServerInfo) peers.get(x);
                if (peer.getAddress().equals(localhost) && peer.getPort().intValue() == this.getServerPort()) {
                    peers.remove(peer);
                }
            }
            ois.close();
            return peers;
        } catch (Exception e) {
            _logger.error("Failed to get peers infos: " + e.getMessage());
            if (local != null) try {
                local.close();
            } catch (Exception ex) {
            }
            return null;
        }
    }

    public MessageWrapper doQuery(QueryNode query, PublicHeader ph) throws Exception {
        QueryMessage qm = new QueryMessage(query, ph);
        MessageWrapper wm = this.sendBroadcastMessage(qm);
        return wm;
    }

    public MessageWrapper[] doSupernodeQuery(QueryNode query, PublicHeader ph, List supernodeSet) throws Exception {
        Iterator superNodes = supernodeSet.iterator();
        ArrayList messages = new ArrayList();
        while (superNodes.hasNext()) {
            final String curSupernode = (String) superNodes.next();
            final MessageWrapper qw = doQuery(query, ph, curSupernode);
            if (qw != null && qw.getMessage() != null) {
                messages.add(qw);
            }
        }
        MessageWrapper[] mw = new MessageWrapper[messages.size()];
        for (int x = 0; x < messages.size(); x++) {
            mw[x] = (MessageWrapper) messages.get(x);
        }
        return mw;
    }

    private MessageWrapper doQuery(QueryNode query, PublicHeader ph, String dest) throws Exception {
        QueryMessage qm = new QueryMessage(query, ph);
        MessageWrapper wm = this.sendMessage(qm, dest, false, false);
        return wm;
    }

    public QueryMessage doRandomQuery(long timeToLive) {
        try {
            QueryRandomItem qri = new QueryRandomItem(null);
            AsymmetricProvider ap = new AsymmetricProvider(false);
            QueryMessage qm = new QueryMessage(qri, ap.getPublicHeader());
            if (this.getNeighboursNumber() > 0) {
                MessageWrapper wm = this.sendBroadcastMessage(qm);
                return qm;
            } else {
                return null;
            }
        } catch (Exception ex) {
            _logger.error("", ex);
            return null;
        }
    }

    public boolean areInMyMessages(ArrayList list) {
        for (int x = 0; x < list.size(); x++) {
            if (this.myMessages.contains(list.get(x))) return true;
        }
        return false;
    }

    public ArrayList removeDeliveredMessagesFromList(ArrayList list) {
        for (int x = list.size() - 1; x >= 0; x--) {
            if (!this.myMessages.contains(list.get(x)) && !this.failedMessages.contains(list.get(x))) list.remove(x);
        }
        return list;
    }

    public boolean areInFailedMessages(ArrayList list) {
        for (int x = 0; x < list.size(); x++) {
            if (this.failedMessages.contains(list.get(x))) return true;
        }
        return false;
    }

    public static void checkChunksPath() {
        checkChuncksHome();
        File cpf = new File(WarriorAnt.chunksHome + WarriorAnt.chunksPath);
        if (cpf.exists() && !cpf.isDirectory()) {
            cpf.delete();
            cpf.mkdir();
        } else if (!cpf.exists()) {
            cpf.mkdir();
        }
    }

    public static void checkPreviewPath() {
        File cpf = new File(WarriorAnt.previewsPath);
        if (cpf.exists() && !cpf.isDirectory()) {
            cpf.delete();
            cpf.mkdir();
        } else if (!cpf.exists()) {
            cpf.mkdir();
        }
    }

    public static void checked2kPath() {
        File cpf = new File(WarriorAnt.ed2kPath);
        boolean created = false;
        if (cpf.exists() && !cpf.isDirectory()) {
            cpf.delete();
            cpf.mkdir();
            created = true;
        } else if (!cpf.exists()) {
            cpf.mkdir();
            created = true;
        }
        if (created) {
            URL url = (new Base16()).getClass().getClassLoader().getResource("anims/ed2k_1.gif");
            if (!(url == null)) {
                try {
                    InputStream is = url.openStream();
                    FileOutputStream fos = new FileOutputStream(new File(WarriorAnt.ed2kPath + "ed2k_1.gif"));
                    while (is.available() > 0) {
                        byte[] buff = new byte[500000];
                        int read = is.read(buff);
                        fos.write(buff, 0, read);
                    }
                    fos.close();
                } catch (Exception e) {
                    _logger.error("Error in copying picture to ed2k folder", e);
                }
            }
            url = (new Base16()).getClass().getClassLoader().getResource("anims/ed2k_2.gif");
            if (!(url == null)) {
                try {
                    InputStream is = url.openStream();
                    FileOutputStream fos = new FileOutputStream(new File(WarriorAnt.ed2kPath + "ed2k_2.gif"));
                    while (is.available() > 0) {
                        byte[] buff = new byte[500000];
                        int read = is.read(buff);
                        fos.write(buff, 0, read);
                    }
                    fos.close();
                } catch (Exception e) {
                    _logger.error("Error in copying picture to ed2k folder", e);
                }
            }
        }
    }

    public static void checkChuncksHome() {
        MakePath(WarriorAnt.chunksHome);
    }

    public static void checkDownloadPath() {
        MakePath(WarriorAnt.downloadPath);
    }

    public static void MakePath(String sPath) {
        File cpf = new File(sPath);
        if (cpf.exists()) {
            if (!cpf.isDirectory()) {
                cpf.delete();
            } else return;
        }
        cpf.mkdir();
    }

    public void createProxy(EndpointSecurityManager esm) {
        try {
            if (this.httpProxy != null && !this.httpProxy.terminate()) throw new Exception("Active proxy not stopped");
            httpProxy = new HttpProxy(WarriorAnt.httpProxyPort, esm.getPeerId(), this);
            this.propertyChangeSupport.firePropertyChange("currentProxyChanged", null, httpProxy);
            _logger.info("Proxy to " + esm.getPeerId() + " started on port " + httpProxy.getPort());
        } catch (Exception e) {
            _logger.error("Error in creating proxy", e);
        }
    }

    protected void processMessage(final Message m, final Router r) {
        Thread processor = new Thread() {

            public void run() {
                processMessageThread(m, r);
            }
        };
        processor.start();
    }

    protected void processMessageThread(Message m, Router r) {
        _logger.debug("Processing message: ID " + m.getAck_Id() + " Type: " + m.getType() + " From: " + m.getSource() + " To: " + m.getDest());
        try {
            if (m.getType() == 1 && (m instanceof QueryMessage)) {
                processQueryMessage((QueryMessage) m, r);
            } else if (m.getType() == 1) {
            } else if (m.getType() != 1) {
                if (m instanceof FilePushMessage) {
                    this.processFilePushMessage((FilePushMessage) m, r);
                }
                if (m instanceof FileSizePushMessage) {
                    this.processFileSizePushMessage((FileSizePushMessage) m, r);
                }
                if (m instanceof FilePullMessage) {
                    this.processFilePullMessage((FilePullMessage) m, r);
                }
                if (m instanceof FileInfosPullMessage) {
                    this.processFileInfosPullMessage((FileInfosPullMessage) m, r);
                }
                if (m instanceof FileInfosPushMessage) {
                    this.processFileInfosPushMessage((FileInfosPushMessage) m, r);
                }
                if (m instanceof FileSizePullMessage) {
                    this.processFileSizePullMessage((FileSizePullMessage) m, r);
                }
                if (m instanceof FilePartMessage) {
                    this.processFilePartMessage((FilePartMessage) m, r);
                }
                if (m instanceof ControlMessage) {
                    this.processControlMessage((ControlMessage) m, r);
                }
                if (m instanceof SecurityRequestMessage) {
                    this.processSecurityRequestMessage((SecurityRequestMessage) m, r);
                }
                if (m instanceof SecurityResponseMessage) {
                    this.processSecurityResponseMessage((SecurityResponseMessage) m, r);
                }
                if (m instanceof QueryMessage) {
                    this.processQueryMessage((QueryMessage) m, r);
                }
                if (m instanceof HttpRequestMessage) {
                    this.processHttpRequestMessage((HttpRequestMessage) m, r);
                }
                if (m instanceof HttpResponsePartMessage) {
                    this.processHttpResponsePartMessage((HttpResponsePartMessage) m, r);
                }
                if (m instanceof PrivateChatMessage) {
                    this.processPrivateChatMessage((PrivateChatMessage) m, r);
                }
            }
            _logger.debug("Processed message: ID " + m.getAck_Id() + " Type: " + m.getType() + " From: " + m.getSource() + " To: " + m.getDest());
        } catch (Exception e) {
            _logger.error(this.getShortId() + ": Error In Processing message: id = " + m.getAck_Id() + " Source: " + m.getSource() + " Dest: " + m.getDest(), e);
        }
    }

    private void processFileInfosPullMessage(FileInfosPullMessage fipm, Router r) throws Exception {
        try {
            ants.p2p.security.EndpointSecurityManager esm = this.getInputSecureConnectionManager(fipm.getSource());
            if (esm == null) {
                SecureConnectionErrorControlMessage fterrcm = new SecureConnectionErrorControlMessage(new Integer(0), null, "No secure connection");
                MessageWrapper wm = this.sendMessage(fterrcm, fipm.getSource(), false, false);
                _logger.debug(this.getShortId() + ": No secure connection with " + fipm.getSource());
                while (this.myMessages.contains(wm) && !this.isDisconnected()) {
                    Thread.sleep(1000);
                }
                if (this.failedMessages.contains(wm)) {
                    this.failedMessages.remove(wm);
                    _logger.debug(this.getShortId() + ": Error in sending ControlMessage " + fterrcm.getMessage());
                }
                throw new Exception("No secure connection avaiable with endpoint " + fipm.getSource());
            }
            fipm.decrypt(esm.getCipherDec());
            byte[] hash = fipm.getHash();
            QueryFileTuple qft = null;
            BackgroundEngine be = BackgroundEngine.getInstance();
            FileInfos fi = be.getLocalFile(Base16.toHexString(hash), QueryHashItem.ANTS_HASH);
            Object pfi = be.getPartialFile(Base16.toHexString(hash), QueryHashItem.ANTS_HASH);
            if (fi != null) {
                qft = new QueryCompletedFileTuple(null, (new File(fi.getName())).getName(), fi.getHash(), fi.getED2KHash(), fipm.getChunkHashes() ? fi.getChunckHashes() : null, new Long(fi.getSize()), this.getIdent(), this.getLocalInetAddress(), new Integer(this.maxPullRequestsToServe - this.inServiceFilePullRequests.size()), WarriorAnt.ConnectionType, fi.getExtendedInfos(), fi.getComment());
            } else if (pfi != null) {
                qft = be.getPartialFileTuple(null, Base16.toHexString(hash), QueryHashItem.ANTS_HASH, this.getIdent(), new Integer(this.maxPullRequestsToServe - this.inServiceFilePullRequests.size()), WarriorAnt.ConnectionType, fipm.getChunkHashes());
            } else {
                FileInfosPullErrorControlMessage fterrcm = new FileInfosPullErrorControlMessage(new Integer(0), hash, "File don't exist");
                fterrcm.encrypt(esm.getCipherEnc());
                MessageWrapper wm = this.sendMessage(fterrcm, fipm.getSource(), false, false);
                _logger.debug(this.getShortId() + ": File " + Base16.toHexString(fipm.getHash()) + " don't exist. Cannot serve pull request from id " + fipm.getSource());
                while (this.myMessages.contains(wm)) {
                    Thread.sleep(1000);
                }
                if (this.failedMessages.contains(wm)) {
                    this.failedMessages.remove(wm);
                    fterrcm.decrypt(esm.getCipherDec());
                    _logger.debug(this.getShortId() + ": Error in sending ControlMessage " + fterrcm.getMessage());
                    return;
                }
                return;
            }
            FileInfosPushMessage firm = new FileInfosPushMessage(hash, qft);
            firm.encrypt(esm.getCipherEnc());
            MessageWrapper wm = this.sendMessage(firm, fipm.getSource(), false, false);
            while (this.myMessages.contains(wm)) {
                Thread.sleep(1000);
            }
            if (this.failedMessages.contains(wm)) {
                this.failedMessages.remove(wm);
                firm.decrypt(esm.getCipherDec());
                _logger.debug(this.getShortId() + ": Error in sending FileSizePushMessage " + Base16.toHexString(firm.getHash()) + " - Cannot serve pull request from id " + fipm.getSource());
                return;
            }
        } catch (Exception e) {
            _logger.error("", e);
            throw new Exception(this.getShortId() + ": Error In Processing FileInfosPullMessage: id = " + fipm.getAck_Id() + " Source: " + fipm.getSource() + " Dest: " + fipm.getDest());
        }
    }

    private void processFileSizePullMessage(FileSizePullMessage fspm, Router r) throws Exception {
        try {
            ants.p2p.security.EndpointSecurityManager esm = this.getInputSecureConnectionManager(fspm.getSource());
            if (esm == null) {
                SecureConnectionErrorControlMessage fterrcm = new SecureConnectionErrorControlMessage(new Integer(0), null, "No secure connection");
                MessageWrapper wm = this.sendMessage(fterrcm, fspm.getSource(), false, false);
                _logger.debug(this.getShortId() + ": No secure connection with " + fspm.getSource());
                while (this.myMessages.contains(wm) && !this.isDisconnected()) {
                    Thread.sleep(1000);
                }
                if (this.failedMessages.contains(wm)) {
                    this.failedMessages.remove(wm);
                    _logger.debug(this.getShortId() + ": Error in sending ControlMessage " + fterrcm.getMessage());
                }
                throw new Exception("No secure connection avaiable with endpoint " + fspm.getSource());
            }
            fspm.decrypt(esm.getCipherDec());
            byte[] hash = fspm.getHash();
            File f = new File(fspm.getFileName());
            if (!f.exists()) {
                FileSizePullErrorControlMessage fterrcm = new FileSizePullErrorControlMessage(new Integer(0), hash, "File don't exist");
                fterrcm.encrypt(esm.getCipherEnc());
                MessageWrapper wm = this.sendMessage(fterrcm, fspm.getSource(), false, false);
                _logger.debug(this.getShortId() + ": File " + fspm.getFileName() + " don't exist. Cannot serve pull request from id " + fspm.getSource());
                while (this.myMessages.contains(wm)) {
                    Thread.sleep(1000);
                }
                if (this.failedMessages.contains(wm)) {
                    this.failedMessages.remove(wm);
                    fterrcm.decrypt(esm.getCipherDec());
                    _logger.debug(this.getShortId() + ": Error in sending ControlMessage " + fterrcm.getMessage());
                    return;
                }
                return;
            }
            FileSizePushMessage fsrm = new FileSizePushMessage(f.getName(), hash, new Long(f.length()));
            fsrm.encrypt(esm.getCipherEnc());
            MessageWrapper wm = this.sendMessage(fsrm, fspm.getSource(), false, false);
            while (this.myMessages.contains(wm)) {
                Thread.sleep(1000);
            }
            if (this.failedMessages.contains(wm)) {
                this.failedMessages.remove(wm);
                fsrm.decrypt(esm.getCipherDec());
                _logger.debug(this.getShortId() + ": Error in sending FileSizePushMessage " + fsrm.getFileName() + " - Cannot serve pull request from id " + fspm.getSource());
                return;
            }
        } catch (Exception e) {
            _logger.error("", e);
            synchronized (inServiceFilePullRequests) {
                if (inServiceFilePullRequests.contains(fspm)) {
                    inServiceFilePullRequests.remove(fspm);
                }
            }
            throw new Exception(this.getShortId() + ": Error In Processing FileSizePullMessage: id = " + fspm.getAck_Id() + " Source: " + fspm.getSource() + " Dest: " + fspm.getDest());
        }
    }

    private void processQueryMessage(QueryMessage qm, Router r) throws Exception {
        try {
            if ((qm.getType() == 2 || (qm.getType() == 0 && !this.inTransitMessages.contains(qm))) && !qm.getProcessed()) {
                double mustProcess = Math.random() * 100;
                if ((qm.getType() == 0 || mustProcess < WarriorAnt.processProbability) && !qm.getSource().equals(this.getIdent())) {
                    _logger.debug(this.getShortId() + "Processing Query");
                    QueryResultSenderThread qrst = new QueryResultSenderThread(this, qm, r);
                    qrst.run();
                    _logger.debug(this.getShortId() + ": Processed query by " + qm.getSource().substring(0, 10) + ".");
                } else {
                    _logger.debug("Unprocessed Query");
                }
            } else if ((qm.getType() == 1 || qm.getType() == 0) && qm.getProcessed()) {
                QueryManager queryManager = new QueryManager(this, qm);
                if (qm.getQuery() instanceof QuerySupernodeItem) {
                    _logger.info("Ricevuta parte di query supernode");
                    this.propertyChangeSupport.firePropertyChange("supernodeQueryCompleted", qm, queryManager.resultSet);
                } else if (qm.getQuery() instanceof QueryInetAddressItem) {
                    _logger.info("Ricevuta parte di query inetAddress");
                    this.propertyChangeSupport.firePropertyChange("inetAddressQueryCompleted", null, queryManager.resultSet);
                } else if (qm.getQuery() instanceof QueryHttpServerItem) {
                    _logger.info("Ricevuta parte di http server query");
                    this.propertyChangeSupport.firePropertyChange("httpServerQueryCompleted", qm, queryManager.resultSet);
                } else {
                    if (queryManager.resultSet.size() > 0 && queryManager.resultSet.get(0) instanceof HttpServerInfo) {
                        ArrayList httpResultSet = new ArrayList();
                        httpResultSet.add(queryManager.resultSet.get(0));
                        _logger.info("Ricevuta parte di http server query");
                        this.propertyChangeSupport.firePropertyChange("httpServerQueryCompleted", qm, httpResultSet);
                        queryManager.resultSet.remove(0);
                    }
                    for (int x = 0; x < queryManager.resultSet.size() && this.acceptTCPDirectConnections; x++) {
                        if (queryManager.resultSet.get(x) != null) {
                            QueryFileTuple qft = (QueryFileTuple) queryManager.resultSet.get(x);
                            if (!qft.getOwnerIP().equals("") && !qft.getOwnerID().equals(this.getIdent())) {
                                if (this.routingTable.get(qft.getOwnerID()) != null) {
                                    RoutingTableElement rte = (RoutingTableElement) this.routingTable.get(qft.getOwnerID());
                                    rte.setIP(qft.getOwnerIP());
                                } else {
                                    RoutingTableElement rte = new RoutingTableElement();
                                    rte.setIP(qft.getOwnerIP());
                                    this.routingTable.put(qft.getOwnerID(), rte);
                                }
                            }
                        }
                    }
                    this.propertyChangeSupport.firePropertyChange("queryCompleted", qm, queryManager.resultSet);
                    String source = qm.getSource();
                    if (source.length() > 10) source = source.substring(0, 10);
                    _logger.info("Ricevuta parte di query file[" + source + "]: " + qm.getQuery() + " " + qm.getTuples().get(0) + " " + qm.getTuples().size());
                }
            } else throw new Exception(this.getShortId() + ": Error In Processing query. Source: " + qm.getSource());
        } catch (Exception e) {
            _logger.error("", e);
            throw new Exception(this.getShortId() + ": Error In Processing query. Source: " + qm.getSource() + "\nMessage dump: type " + qm.getType() + "  processed: " + qm.getProcessed() + "  delivered: " + qm.getDelivered());
        }
    }

    private void processFilePullMessage(FilePullMessage fpm, Router r) throws Exception {
        if (this.getNeighbour(r.getRequirer()) != null) {
            this.getNeighbour(r.getRequirer()).setLastActiveUploadTime();
        }
        _logger.info("Set last active upload for " + r.getRequirer());
        _logger.info("Running FilePullMessageProcessor. Free slots: " + (WarriorAnt.maxPullRequestsToServe - this.inServiceFilePullRequests.size()));
        FilePullMessageProcessor fpmp = new FilePullMessageProcessor(this, fpm);
        fpmp.start();
    }

    public boolean checkInServiceFilePullRequests(boolean addThis, final FilePullMessage fpm) {
        synchronized (this.inServiceFilePullRequests) {
            if (this.inServiceFilePullRequests.contains(fpm)) {
                _logger.info(this.getShortId() + ": Found in service file pull! Rejecting message");
                return true;
            } else {
                for (int x = 0; x < this.inServiceFilePullRequests.size(); x++) {
                    FilePullMessage currentFpM = (FilePullMessage) this.inServiceFilePullRequests.get(x);
                    if (currentFpM != null && currentFpM.getBlocks().equals(fpm.getBlocks()) && currentFpM.getBlockSize().equals(fpm.getBlockSize()) && currentFpM.getFileName().equals(fpm.getFileName()) && WarriorAnt.compareHash(currentFpM.getHash(), fpm.getHash()) && currentFpM.getLocalFileName().equals(fpm.getLocalFileName()) && currentFpM.getOffset().equals(fpm.getOffset()) && currentFpM.getResume().equals(fpm.getResume()) && currentFpM.getSource().equals(fpm.getSource())) {
                        _logger.info(this.getShortId() + ": Found in service file pull! Rejecting message");
                        return true;
                    }
                }
            }
            _logger.info(this.getShortId() + ": Not Found in service file pull! Processing message");
            if (addThis) {
                if (this.inServiceFilePullRequests.size() >= WarriorAnt.maxPullRequestsToServe) {
                    _logger.info(this.getShortId() + ": Already serving max request! Rejecting message");
                    final ants.p2p.security.EndpointSecurityManager esm = this.getInputSecureConnectionManager(fpm.getSource());
                    if (esm == null) {
                        Thread notifier = new Thread() {

                            public void run() {
                                try {
                                    SecureConnectionErrorControlMessage fterrcm = new SecureConnectionErrorControlMessage(new Integer(0), null, "No secure connection");
                                    MessageWrapper wm = sendMessage(fterrcm, fpm.getSource(), false, false);
                                    _logger.debug(getShortId() + ": No secure connection with " + fpm.getSource());
                                    while (myMessages.contains(wm) && !isDisconnected()) {
                                        sleep(1000);
                                    }
                                    if (failedMessages.contains(wm)) {
                                        failedMessages.remove(wm);
                                        _logger.debug(getShortId() + ": Error in sending ControlMessage " + fterrcm.getMessage());
                                    }
                                    _logger.error("No secure connection avaiable with endpoint " + fpm.getSource());
                                } catch (Exception e) {
                                    _logger.error("Exception while trying to notify secure connection error", e);
                                }
                            }
                        };
                        notifier.start();
                        return true;
                    }
                    Thread notifier = new Thread() {

                        public void run() {
                            try {
                                FileTransferErrorControlMessage cm = new FileTransferErrorControlMessage(new Integer(1), fpm.getHash(), "No free slots", fpm);
                                cm.encrypt(esm.getCipherEnc());
                                MessageWrapper wm2 = sendMessage(cm, fpm.getSource(), false, false);
                                while (myMessages.contains(wm2)) {
                                    Thread.sleep(1000);
                                }
                                if (failedMessages.contains(wm2)) {
                                    failedMessages.remove(wm2);
                                    _logger.info(getShortId() + ": Error file transfer interruption " + fpm.getFileName() + ". Cannot notificate interruption of msg id " + fpm.getSource());
                                }
                                _logger.warn(getShortId() + ": File pull interrupted [No free slots]: id = " + fpm.getAck_Id() + " Source: " + fpm.getSource() + " Dest: " + fpm.getDest() + " File Name: " + fpm.getFileName());
                            } catch (Exception e) {
                                _logger.error("Exception while trying to notify secure connection error", e);
                            }
                        }
                    };
                    notifier.start();
                    return true;
                }
                this.inServiceFilePullRequests.add(fpm);
            }
            return false;
        }
    }

    private void processFileInfosPushMessage(FileInfosPushMessage fipm, Router r) throws Exception {
        try {
            ants.p2p.security.EndpointSecurityManager esm = this.getOutputSecureConnectionManager(fipm.getSource());
            if (esm == null) throw new Exception("No secure connection avaiable with endpoint " + fipm.getSource());
            fipm.decrypt(esm.getCipherDec());
            _logger.info("File infos received: hash=" + Base16.toHexString(fipm.getHash()) + "   chunks: " + fipm.getQueryFileTuple().getChunkHashes().getChunkHashes());
            this.propertyChangeSupport.firePropertyChange("fileInfosPullCompleted", null, fipm);
        } catch (Exception e) {
            this.propertyChangeSupport.firePropertyChange("fileInfosPullError", null, fipm);
            throw new Exception(this.getShortId() + ": Error In Processing FileInfosPushMessage: id = " + fipm.getAck_Id() + " Source: " + fipm.getSource() + " Dest: " + fipm.getDest(), e);
        }
    }

    private void processFileSizePushMessage(FileSizePushMessage fspm, Router r) throws Exception {
        try {
            ants.p2p.security.EndpointSecurityManager esm = this.getOutputSecureConnectionManager(fspm.getSource());
            if (esm == null) throw new Exception("No secure connection avaiable with endpoint " + fspm.getSource());
            fspm.decrypt(esm.getCipherDec());
            _logger.info("File: name=" + fspm.getFileName() + " hash=" + Base16.toHexString(fspm.getHash()) + " size=" + fspm.getSize());
            this.propertyChangeSupport.firePropertyChange("fileSizePushReceived", null, fspm);
        } catch (Exception e) {
            this.propertyChangeSupport.firePropertyChange("fileSizePushError", null, fspm);
            _logger.error("", e);
            throw new Exception(this.getShortId() + ": Error In Processing FileSizePushMessage: id = " + fspm.getAck_Id() + " Source: " + fspm.getSource() + " Dest: " + fspm.getDest());
        }
    }

    private void processFilePushMessage(FilePushMessage fpm, Router r) throws Exception {
        try {
            ants.p2p.security.EndpointSecurityManager esm = this.getOutputSecureConnectionManager(fpm.getSource());
            if (esm == null) throw new Exception("No secure connection avaiable with endpoint " + fpm.getSource());
            fpm.decrypt(esm.getCipherDec());
            boolean proceed = false;
            synchronized (this.pendingFilePullRequests) {
                for (int x = this.pendingFilePullRequests.size() - 1; x >= 0; x--) {
                    FilePullMessage fpr = ((FilePullMessageWrapper) (this.pendingFilePullRequests.get(x))).getFilePullMessage();
                    if (fpr.getDest().equals(fpm.getSource()) && fpr.getLocalFileName().equals(fpm.getFileName()) && fpr.getOffset().equals(fpm.getOffset()) && fpr.getBlockSize().equals(fpm.getBlockSize()) && fpr.getBlocks().equals(new Long(PartialFile.computeGroupFactor(fpm.getBlockSize().intValue()))) && fpr.getResume().equals(fpm.getResume()) && compareHash(fpr.getHash(), fpm.getHash())) {
                        synchronized (this.inServiceFilePushRequests) {
                            _logger.info(this.getShortId() + ": Found pending pull request... received serving push!!!");
                            if (this.inServiceFilePushRequests.size() >= WarriorAnt.maxOwnPullRequestsToTrace) this.inServiceFilePushRequests.remove(0);
                            this.inServiceFilePushRequests.add(fpr);
                            this.pendingFilePullRequests.remove(x);
                            proceed = true;
                        }
                    }
                }
                if (!proceed) {
                    _logger.info(this.getShortId() + ": Not Found pending pull request... ingnoring message.");
                    return;
                }
                if (this.getNeighbour(r.getRequirer()) != null) {
                    this.getNeighbour(r.getRequirer()).setLastActiveDownloadTime();
                }
                _logger.info("Set last active download for " + r.getRequirer());
                this.propertyChangeSupport.firePropertyChange("filePushInit", null, fpm);
                _logger.info(this.getShortId() + ": Creating file: name=" + WarriorAnt.chunksHome + fpm.getFileName() + " hash=" + Base16.toHexString(fpm.getHash()));
                byte[] hash = fpm.getHash();
                String name = fpm.getFileName();
                PartialFile pf = new PartialFile(name, fpm.getSource(), hash, fpm.getResume().booleanValue(), fpm.getOffset().longValue(), fpm, this.propertyChangeSupport);
                partialFiles.add(pf);
            }
            this.prunePartialFiles();
        } catch (Exception e) {
            _logger.error("", e);
            this.propertyChangeSupport.firePropertyChange("filePushError", null, fpm);
            throw new Exception(this.getShortId() + ": Error In Processing FilePushMessage: id = " + fpm.getAck_Id() + " Source: " + fpm.getSource() + " Dest: " + fpm.getDest());
        }
    }

    public void prunePartialFiles() {
        for (int x = this.pendingFilePullRequests.size() - 1; x >= 0; x--) {
            FilePullMessageWrapper fpmw = (FilePullMessageWrapper) this.pendingFilePullRequests.get(x);
            if (System.currentTimeMillis() - fpmw.getTimeStamp() > WarriorAnt.partialFilesTimeout) {
                this.pendingFilePullRequests.remove(x);
                this.propertyChangeSupport.firePropertyChange("filePullRequestError", null, fpmw.getFilePullMessage());
            }
        }
        for (int x = this.partialFiles.size() - 1; x >= 0; x--) {
            PartialFile pf = (PartialFile) this.partialFiles.get(x);
            if (System.currentTimeMillis() - pf.getLastActivityTime() > WarriorAnt.partialFilesTimeout) {
                this.partialFiles.remove(pf);
                _logger.info("Partial file timed out: " + pf.getName() + " " + pf.getFilePushMessage().getSource().substring(0, 10));
                for (int y = 0; y < this.inServiceFilePushRequests.size(); y++) {
                    FilePullMessage fpr = (FilePullMessage) (this.inServiceFilePushRequests.get(y));
                    if (fpr.getDest().equals(pf.getFilePushMessage().getSource()) && compareHash(fpr.getHash(), pf.getFilePushMessage().getHash())) {
                        this.inServiceFilePushRequests.remove(fpr);
                        _logger.info("Removed in service file push: " + pf.getName() + " " + pf.getFilePushMessage().getSource().substring(0, 10));
                    }
                }
                this.propertyChangeSupport.firePropertyChange("filePartDownloadInterrupted", null, pf.getFilePushMessage());
                _logger.info("Removed source: " + pf.getName() + " " + pf.getFilePushMessage().getSource().substring(0, 10));
            }
        }
    }

    private void processPrivateChatMessage(PrivateChatMessage pcm, Router r) throws Exception {
        try {
            this.propertyChangeSupport.firePropertyChange("privateChatMessageReceived", null, pcm);
        } catch (Exception e) {
            throw new Exception(this.getShortId() + ": Error In Processing PrivateChatMessage: id = " + pcm.getAck_Id() + " Source: " + pcm.getSource() + " Dest: " + pcm.getDest(), e);
        }
    }

    private void processFilePartMessage(FilePartMessage fpm, Router r) throws Exception {
        try {
            ants.p2p.security.EndpointSecurityManager esm = this.getOutputSecureConnectionManager(fpm.getSource());
            if (esm == null) {
                this.propertyChangeSupport.firePropertyChange("securityConnectionError", fpm.getSource(), fpm);
                throw new Exception("No secure connection avaiable with endpoint " + fpm.getSource());
            }
            fpm.decrypt(esm.getCipherDec());
            byte[] hash = fpm.getHash();
            PartialFile part = this.getPartialFile(hash, fpm.getFilePushMessage());
            if (part != null) {
                part.appendBytes(fpm.getContent(), fpm.getPartId().longValue());
                if (this.getNeighbour(r.getRequirer()) != null) {
                    this.getNeighbour(r.getRequirer()).setLastActiveDownloadTime();
                }
                _logger.info("Set last active download for " + r.getRequirer());
                this.propertyChangeSupport.firePropertyChange("filePartUpdate", null, fpm);
            } else {
                throw new Exception(this.getShortId() + ": Partial file is null. Error In Processing FilePartMessage: id = " + fpm.getAck_Id() + " Source: " + fpm.getSource() + " Dest: " + fpm.getDest() + "\nName: " + fpm.getFilePushMessage().getFileName());
            }
        } catch (Exception e) {
            _logger.error("", e);
            if (!e.getMessage().equals("No secure connection avaiable with endpoint " + fpm.getSource())) {
                this.propertyChangeSupport.firePropertyChange("filePartError", null, fpm);
            }
            throw new Exception(this.getShortId() + ": Error In Processing FilePartMessage: id = " + fpm.getAck_Id() + " Source: " + fpm.getSource() + " Dest: " + fpm.getDest());
        }
    }

    private void processHttpRequestMessage(HttpRequestMessage httpRequest, Router r) throws Exception {
        try {
            ants.p2p.security.EndpointSecurityManager esm = this.getInputSecureConnectionManager(httpRequest.getSource());
            if (esm == null) {
                SecureConnectionErrorControlMessage fterrcm = new SecureConnectionErrorControlMessage(new Integer(0), null, "No secure connection");
                MessageWrapper wm = this.sendMessage(fterrcm, httpRequest.getSource(), false, false);
                _logger.debug(this.getShortId() + ": No secure connection with " + httpRequest.getSource());
                while (this.myMessages.contains(wm) && !this.isDisconnected()) {
                    Thread.sleep(1000);
                }
                if (this.failedMessages.contains(wm)) {
                    this.failedMessages.remove(wm);
                    _logger.debug(this.getShortId() + ": Error in sending ControlMessage " + fterrcm.getMessage());
                }
                throw new Exception("No secure connection avaiable with endpoint " + httpRequest.getSource());
            }
            if (this.getNeighbour(r.getRequirer()) != null) {
                this.getNeighbour(r.getRequirer()).setLastActiveUploadTime();
            }
            _logger.info("Set last active http upload for " + r.getRequirer());
            httpRequest.decrypt(esm.getCipherDec());
            HttpRequestHandler requestHandler = new HttpRequestHandler(httpRequest, this);
        } catch (Exception e) {
            _logger.error("", e);
            if (!e.getMessage().equals("No secure connection avaiable with endpoint " + httpRequest.getSource())) {
                this.propertyChangeSupport.firePropertyChange("httpRequestError", null, httpRequest);
            }
            throw new Exception(this.getShortId() + ": Error In Processing httpRequestMessage: id = " + httpRequest.getAck_Id() + " Source: " + httpRequest.getSource() + " Dest: " + httpRequest.getDest());
        }
    }

    private void processHttpResponsePartMessage(HttpResponsePartMessage httpResponse, Router r) throws Exception {
        try {
            ants.p2p.security.EndpointSecurityManager esm = this.getOutputSecureConnectionManager(httpResponse.getSource());
            if (esm == null) {
                this.propertyChangeSupport.firePropertyChange("securityConnectionError", httpResponse.getSource(), httpResponse);
                throw new Exception("No secure connection avaiable with endpoint " + httpResponse.getSource());
            }
            if (this.getNeighbour(r.getRequirer()) != null) {
                this.getNeighbour(r.getRequirer()).setLastActiveDownloadTime();
            }
            _logger.info("Set last active http download for " + r.getRequirer());
            httpResponse.decrypt(esm.getCipherDec());
            this.propertyChangeSupport.firePropertyChange("httpResponsePart", null, httpResponse);
        } catch (Exception e) {
            _logger.error("", e);
            if (!e.getMessage().equals("No secure connection avaiable with endpoint " + httpResponse.getSource())) {
                this.propertyChangeSupport.firePropertyChange("httpResponseError", null, httpResponse);
            }
            throw new Exception(this.getShortId() + ": Error In Processing httpRequestMessage: id = " + httpResponse.getAck_Id() + " Source: " + httpResponse.getSource() + " Dest: " + httpResponse.getDest());
        }
    }

    private void processHttpTransferEndControlMessage(HttpTransferEndControlMessage httpResponseEnd, Router r) throws Exception {
        if (httpResponseEnd.getControlId().intValue() == 0) {
            _logger.debug("\n" + this.getShortId() + ": Http tranfer ended from id " + httpResponseEnd.getSource() + ".\nReason: " + httpResponseEnd.getMessage());
            if (this.getNeighbour(r.getRequirer()) != null) {
                this.getNeighbour(r.getRequirer()).setLastActiveDownloadTime();
            }
            _logger.info("Set last active http download for " + r.getRequirer());
            this.propertyChangeSupport.firePropertyChange("httpResponseEnd", null, httpResponseEnd);
        }
    }

    private void processHttpInterruptTransferMessage(HttpInterruptTransferMessage httpInterruptTranfer, Router r) throws Exception {
        if (httpInterruptTranfer.getControlId().intValue() == 0) {
            _logger.debug("\n" + this.getShortId() + ": Http tranfer interrupted from id " + httpInterruptTranfer.getSource() + ".\nReason: " + httpInterruptTranfer.getMessage());
            if (this.getNeighbour(r.getRequirer()) != null) {
                this.getNeighbour(r.getRequirer()).setLastActiveUploadTime();
            }
            _logger.info("Set last active http upload for " + r.getRequirer());
            this.propertyChangeSupport.firePropertyChange("httpInterruptTransfer", null, httpInterruptTranfer.getRequest());
        }
    }

    private void processControlMessage(ControlMessage cm, Router r) throws Exception {
        try {
            ants.p2p.security.EndpointSecurityManager esm;
            if (cm instanceof FileTransferEndControlMessage) {
                esm = this.getOutputSecureConnectionManager(cm.getSource());
                if (esm == null) throw new Exception("No secure connection avaiable with endpoint " + cm.getSource());
                cm.decrypt(esm.getCipherDec());
                FileTransferEndControlMessage ftecm = (FileTransferEndControlMessage) cm;
                this.processFileTransferEndControlMessage(ftecm, r);
            } else if (cm instanceof FileTransferErrorControlMessage) {
                esm = this.getOutputSecureConnectionManager(cm.getSource());
                if (esm == null) throw new Exception("No secure connection avaiable with endpoint " + cm.getSource());
                cm.decrypt(esm.getCipherDec());
                FileTransferErrorControlMessage fterrcm = (FileTransferErrorControlMessage) cm;
                this.processFileTransferErrorControlMessage(fterrcm, r);
            } else if (cm instanceof SecureConnectionErrorControlMessage) {
                SecureConnectionErrorControlMessage scecm = (SecureConnectionErrorControlMessage) cm;
                this.processSecureConnectionErrorControlMessage(scecm, r);
            } else if (cm instanceof FileSizePullErrorControlMessage) {
                esm = this.getOutputSecureConnectionManager(cm.getSource());
                if (esm == null) throw new Exception("No secure connection avaiable with endpoint " + cm.getSource());
                cm.decrypt(esm.getCipherDec());
                FileSizePullErrorControlMessage fsperrcm = (FileSizePullErrorControlMessage) cm;
                this.processFileSizePullErrorControlMessage(fsperrcm);
            } else if (cm instanceof FileInfosPullErrorControlMessage) {
                esm = this.getOutputSecureConnectionManager(cm.getSource());
                if (esm == null) throw new Exception("No secure connection avaiable with endpoint " + cm.getSource());
                cm.decrypt(esm.getCipherDec());
                FileInfosPullErrorControlMessage fiperrcm = (FileInfosPullErrorControlMessage) cm;
                this.processFileInfosPullErrorControlMessage(fiperrcm);
            } else if (cm instanceof HttpTransferEndControlMessage) {
                esm = this.getOutputSecureConnectionManager(cm.getSource());
                if (esm == null) throw new Exception("No secure connection avaiable with endpoint " + cm.getSource());
                cm.decrypt(esm.getCipherDec());
                HttpTransferEndControlMessage httpTransferError = (HttpTransferEndControlMessage) cm;
                this.processHttpTransferEndControlMessage(httpTransferError, r);
            } else if (cm instanceof HttpInterruptTransferMessage) {
                esm = this.getInputSecureConnectionManager(cm.getSource());
                if (esm == null) {
                    SecureConnectionErrorControlMessage fterrcm = new SecureConnectionErrorControlMessage(new Integer(0), null, "No secure connection");
                    MessageWrapper wm = this.sendMessage(fterrcm, cm.getSource(), false, false);
                    _logger.debug(this.getShortId() + ": No secure connection with " + cm.getSource());
                    while (this.myMessages.contains(wm) && !this.isDisconnected()) {
                        Thread.sleep(1000);
                    }
                    if (this.failedMessages.contains(wm)) {
                        this.failedMessages.remove(wm);
                        _logger.debug(this.getShortId() + ": Error in sending ControlMessage " + fterrcm.getMessage());
                    }
                    throw new Exception("No secure connection avaiable with endpoint " + cm.getSource());
                }
                cm.decrypt(esm.getCipherDec());
                HttpInterruptTransferMessage httpInterruptTransfer = (HttpInterruptTransferMessage) cm;
                this.processHttpInterruptTransferMessage(httpInterruptTransfer, r);
            } else throw new Exception("Unexpected Control Message");
        } catch (Exception e) {
            _logger.error("", e);
            throw new Exception(this.getShortId() + ": Error In Processing ControlMessage: id = " + cm.getAck_Id() + " Source: " + cm.getSource() + " Dest: " + cm.getDest());
        }
    }

    private void processFileSizePullErrorControlMessage(FileSizePullErrorControlMessage fsperrcm) throws Exception {
        if (fsperrcm.getControlId().intValue() == 0) {
            _logger.info("\n" + this.getShortId() + ": File size pull aborted from id " + fsperrcm.getSource() + ". File hash = " + Base16.toHexString(fsperrcm.getContent()) + ".\nReason: " + fsperrcm.getMessage());
            this.propertyChangeSupport.firePropertyChange("fileSizePullError", null, fsperrcm);
        }
    }

    private void processFileInfosPullErrorControlMessage(FileInfosPullErrorControlMessage fiperrcm) throws Exception {
        if (fiperrcm.getControlId().intValue() == 0) {
            _logger.info("\n" + this.getShortId() + ": File size pull aborted from id " + fiperrcm.getSource() + ". File hash = " + Base16.toHexString(fiperrcm.getContent()) + ".\nReason: " + fiperrcm.getMessage());
            this.propertyChangeSupport.firePropertyChange("fileInfosPullError", null, fiperrcm);
        }
    }

    private void processFileTransferErrorControlMessage(FileTransferErrorControlMessage fterrcm, Router r) throws Exception {
        if (fterrcm.getControlId().intValue() == 0 || fterrcm.getControlId().intValue() == 1) {
            synchronized (this.pendingFilePullRequests) {
                for (int x = this.pendingFilePullRequests.size() - 1; x >= 0; x--) {
                    FilePullMessage fpr = ((FilePullMessageWrapper) (this.pendingFilePullRequests.get(x))).getFilePullMessage();
                    if (fpr.equals(fterrcm.getFilePullMessage())) {
                        this.pendingFilePullRequests.remove(x);
                        _logger.info("\n" + this.getShortId() + ": File transfer aborted from id " + fterrcm.getSource() + ". File hash = " + Base16.toHexString(fterrcm.getContent()) + ".\nReason: " + fterrcm.getMessage());
                        if (this.getNeighbour(r.getRequirer()) != null) {
                            this.getNeighbour(r.getRequirer()).setLastActiveDownloadTime();
                        }
                        _logger.info("Set last active download for " + r.getRequirer());
                        this.propertyChangeSupport.firePropertyChange("filePartDownloadError", null, fterrcm);
                    }
                }
            }
        } else if (fterrcm.getControlId().intValue() == 1) {
            synchronized (this.inServiceFilePushRequests) {
                for (int x = this.inServiceFilePushRequests.size() - 1; x >= 0; x--) {
                    FilePullMessage fpr = (FilePullMessage) (this.inServiceFilePushRequests.get(x));
                    if (fpr.equals(fterrcm.getFilePullMessage())) {
                        this.inServiceFilePushRequests.remove(x);
                        _logger.info("\n" + this.getShortId() + ": File transfer aborted from id " + fterrcm.getSource() + ". File hash = " + Base16.toHexString(fterrcm.getContent()) + ".\nReason: " + fterrcm.getMessage());
                        if (this.getNeighbour(r.getRequirer()) != null) {
                            this.getNeighbour(r.getRequirer()).setLastActiveDownloadTime();
                        }
                        _logger.info("Set last active download for " + r.getRequirer());
                        this.propertyChangeSupport.firePropertyChange("filePartDownloadError", null, fterrcm);
                    }
                }
            }
        }
    }

    private void processSecureConnectionErrorControlMessage(SecureConnectionErrorControlMessage fterrcm, Router r) throws Exception {
        this.removeOutputSecureConnection(fterrcm.getSource());
    }

    private void processFileTransferEndControlMessage(FileTransferEndControlMessage ftecm, Router r) throws Exception {
        if (ftecm.getControlId().intValue() == 0) {
            synchronized (this.inServiceFilePushRequests) {
                for (int x = this.inServiceFilePushRequests.size() - 1; x >= 0; x--) {
                    FilePullMessage fpr = (FilePullMessage) (this.inServiceFilePushRequests.get(x));
                    if (fpr.getDest().equals(ftecm.getSource()) && compareHash(fpr.getHash(), ftecm.getContent())) {
                        this.inServiceFilePushRequests.remove(x);
                        _logger.info("\n" + this.getShortId() + ": File transfer succeded from id " + ftecm.getSource() + "\nFile hash = " + Base16.toHexString(ftecm.getContent()) + "\nFile local name = " + ftecm.getFilePushMessage().getFileName());
                    }
                }
            }
            byte[] hash = ftecm.getContent();
            PartialFile part = this.getPartialFile(hash, ftecm.getFilePushMessage());
            if (part != null) {
                part.finalizeFile();
                this.partialFiles.remove(part);
                _logger.info(this.getShortId() + ": Partial file removed: name=" + WarriorAnt.chunksHome + part.getFilePushMessage().getFileName() + " hash=" + Base16.toHexString(part.getFilePushMessage().getHash()));
            } else {
                _logger.warn("Partial null file hash:" + Base16.toHexString(hash) + " " + ftecm.getFilePushMessage().getAck_Id());
            }
            if (this.getNeighbour(r.getRequirer()) != null) {
                this.getNeighbour(r.getRequirer()).setLastActiveDownloadTime();
            }
            _logger.info("Set last active download for " + r.getRequirer());
            this.propertyChangeSupport.firePropertyChange("filePartDownloadCompleted", null, ftecm.getFilePushMessage());
        } else if (ftecm.getControlId().intValue() == 1) {
            synchronized (this.inServiceFilePushRequests) {
                for (int x = this.inServiceFilePushRequests.size() - 1; x >= 0; x--) {
                    FilePullMessage fpr = (FilePullMessage) (this.inServiceFilePushRequests.get(x));
                    if (fpr.getDest().equals(ftecm.getSource()) && compareHash(fpr.getHash(), ftecm.getContent())) {
                        this.inServiceFilePushRequests.remove(x);
                        _logger.info("\n" + this.getShortId() + ": File transfer aborted from id " + ftecm.getSource() + ". File hash = " + Base16.toHexString(ftecm.getContent()) + ".\nReason: " + ftecm.getMessage());
                        byte[] hash = ftecm.getContent();
                        PartialFile part = this.getPartialFile(hash, ftecm.getFilePushMessage());
                        if (part != null) {
                            this.partialFiles.remove(part);
                        }
                        if (this.getNeighbour(r.getRequirer()) != null) {
                            this.getNeighbour(r.getRequirer()).setLastActiveDownloadTime();
                        }
                        _logger.info("Set last active download for " + r.getRequirer());
                        this.propertyChangeSupport.firePropertyChange("filePartDownloadInterrupted", null, ftecm.getFilePushMessage());
                    }
                }
            }
        }
    }

    private void processSecurityRequestMessage(SecurityRequestMessage srm, Router r) throws Exception {
        _logger.info("Input secure connections: " + this.inputSecureConnections.size());
        for (int x = 0; x < this.inputSecureConnections.size(); x++) {
            EndpointSecurityManager esmTemp = (EndpointSecurityManager) this.inputSecureConnections.get(x);
            _logger.info(esmTemp.getPeerId() + " " + (new Date(esmTemp.getLastTimeUsed())).toGMTString());
        }
        _logger.info("End of input secure connection list (last time used order)");
        if (this.inputSecureConnections.size() >= maxSecureConnections) {
            return;
        }
        ants.p2p.security.EndpointSecurityManager esm = new ants.p2p.security.EndpointSecurityManager(this, srm);
    }

    private void processSecurityResponseMessage(SecurityResponseMessage srm, Router r) throws Exception {
        _logger.info(this.getShortId() + ": Secure connection response received");
        ants.p2p.security.EndpointSecurityManager esm;
        boolean found = false;
        for (int x = this.outputSecureConnections.size() - 1; x >= 0; x--) {
            esm = (EndpointSecurityManager) this.outputSecureConnections.get(x);
            if (esm.getPeerId().equals(srm.getSource()) && esm.getSecurityMessage().equals(srm.getRequestMessage())) {
                found = true;
            }
            if (found) return;
        }
        esm = this.getPendingSecureConnectionRequest(srm.getSource(), srm);
        int counter = 0;
        while (esm == null && counter++ < 60) {
            sleep(1000);
            esm = this.getPendingSecureConnectionRequest(srm.getSource(), srm);
        }
        if (esm != null) {
            esm.processSecurityResponseMessage(srm);
            _logger.info(this.getShortId() + ": Secure connection created");
            this.propertyChangeSupport.firePropertyChange("secureConnectionCreated", null, srm);
            this.checkScheduledDownloads(esm);
            this.checkScheduledHttpProxy(esm);
        }
    }

    PartialFile getPartialFile(byte[] hash, FilePushMessage fpm) {
        for (int x = 0; x < this.partialFiles.size(); x++) {
            if (compareHash(((PartialFile) this.partialFiles.get(x)).getHash(), hash) && ((PartialFile) this.partialFiles.get(x)).getFilePushMessage().equals(fpm)) {
                return (PartialFile) this.partialFiles.get(x);
            }
        }
        return null;
    }

    public EndpointSecurityManager getOutputSecureConnectionManager(String peerId) {
        for (int x = 0; x < this.outputSecureConnections.size(); x++) {
            if (((EndpointSecurityManager) this.outputSecureConnections.get(x)).getPeerId().equals(peerId)) return (EndpointSecurityManager) this.outputSecureConnections.get(x);
        }
        return null;
    }

    public EndpointSecurityManager getInputSecureConnectionManager(String peerId) {
        for (int x = 0; x < this.inputSecureConnections.size(); x++) {
            if (((EndpointSecurityManager) this.inputSecureConnections.get(x)).getPeerId().equals(peerId)) {
                EndpointSecurityManager esm = (EndpointSecurityManager) this.inputSecureConnections.get(x);
                return (EndpointSecurityManager) this.inputSecureConnections.get(x);
            }
        }
        return null;
    }

    public EndpointSecurityManager getPendingSecureConnectionRequest(String peerId, SecurityResponseMessage srm) {
        for (int x = 0; x < this.pendingSecureRequest.size(); x++) {
            if (((EndpointSecurityManager) this.pendingSecureRequest.get(x)).getPeerId().equals(peerId) && ((EndpointSecurityManager) this.pendingSecureRequest.get(x)).getSecurityMessage().equals(srm.getRequestMessage())) return (EndpointSecurityManager) this.pendingSecureRequest.get(x);
        }
        return null;
    }

    public static boolean compareHash(byte[] h1, byte[] h2) {
        if (h1.length != h2.length) return false;
        for (int x = 0; x < h1.length; x++) {
            if (h1[x] != h2[x]) return false;
        }
        return true;
    }
}

class FilePullMessageWrapper {

    FilePullMessage fpm;

    long timeStamp;

    public FilePullMessageWrapper(FilePullMessage fpm) {
        this.fpm = fpm;
        this.timeStamp = System.currentTimeMillis();
    }

    public FilePullMessage getFilePullMessage() {
        return this.fpm;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }
}
