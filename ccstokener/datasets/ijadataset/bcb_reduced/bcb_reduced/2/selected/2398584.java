package uk.ac.ebi.mg.xchg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import uk.ac.ebi.mg.xchg.objects.AssayBean;
import uk.ac.ebi.mg.xchg.objects.Study;
import uk.ac.ebi.mg.xchg.objects.StudyId;
import uk.ac.ebi.mg.xchg.upload.FileUploadClient;
import uk.ac.ebi.mg.xchg.upload.Sender;
import uk.ac.ebi.mg.xchg.upload.Transaction;
import uk.ac.ebi.mg.xchg.upload.UploadClientProxy;
import uk.ac.ebi.mg.xchg.upload.UploadCore;
import uk.ac.ebi.mg.xchg.upload.UploadServiceProxy;
import assays.com.FileString;
import assays.com.UserHttpSession;
import assays.hibernate.Assay;
import assays.hibernate.StudyGroup;
import com.pri.log.Log;
import com.pri.log.Logger;
import com.pri.messenger.Address;
import com.pri.messenger.ClientMessenger;
import com.pri.messenger.ConnectionStateListener;
import com.pri.messenger.IntegerMessageBody;
import com.pri.messenger.Message;
import com.pri.messenger.MessageBody;
import com.pri.messenger.MessageRecipient;
import com.pri.messenger.NetworkException;
import com.pri.messenger.ObjectMessageBody;
import com.pri.messenger.ProxyUtils;
import com.pri.messenger.server.ServerMessenger;
import com.pri.session.ClientSession;
import com.pri.util.ProgressListener;
import com.pri.util.stream.StreamPump;

public class ExchangeService implements ExchangePipeline, ConnectionStateListener, ServletContextListener, MessageRecipient {

    static Logger log = Log.getLogger(ExchangeService.class);

    private String sessionKey = null;

    private ClientMessenger cMsgr = null;

    private static ServerMessenger sMsgr = null;

    private Address srvAddr;

    private Sender sender;

    private static UserCore uc;

    private static File uploadDir = new File("/tmp/upload");

    private static ExchangeServer core;

    public static ExchangeServer getExchangeServer() {
        return core;
    }

    public ExchangeService() {
    }

    public ExchangeService(URL upURL, String login, String password) {
        super();
        srvAddr = Address.newServerAddress("upload");
        try {
            URL url = new URL(upURL, "messengerLogin?login=" + login + "&password=" + password + "&do=Login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamPump.doPump(conn.getInputStream(), baos);
            String resp = new String(baos.toByteArray());
            conn.disconnect();
            System.out.println("Got response: " + resp);
            if (resp.startsWith("+")) sessionKey = resp.substring(1); else sessionKey = null;
            if (cMsgr == null) {
                cMsgr = new ClientMessenger(new URL(upURL, "messenger?SESSID=" + sessionKey));
                cMsgr.addConnectionStateListener(this);
            }
            sender = new FileUploadClient(new UploadClientProxy(cMsgr, "fileUpload"));
            cMsgr.setConnected(true);
            Thread.sleep(1000);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stateStateChanged(State arg0) {
    }

    public StudyId sendStudy(Study stdy) throws ExchangeException {
        return (StudyId) makeRequest("sendStudy", stdy, null);
    }

    public StudyId sendAssay(AssayBean createAssay) throws ExchangeException {
        return (StudyId) makeRequest("sendAssay", createAssay, null);
    }

    private Object makeRequest(String req, int id, ProgressListener lsnr) throws ExchangeException {
        Message m = new Message(srvAddr, new IntegerMessageBody(id));
        return makeRequest(req, m, lsnr);
    }

    private Object makeRequest(String req, Serializable obj, ProgressListener lsnr) throws ExchangeException {
        Message m = new Message(srvAddr, new ObjectMessageBody(obj));
        return makeRequest(req, m, lsnr);
    }

    private Object makeRequest(String req, Message m, ProgressListener lsnr) throws ExchangeException {
        m.setType(req);
        try {
            cMsgr.syncSend(m, lsnr);
        } catch (Throwable t) {
            throw new ExchangeException(t.getMessage(), t, ExchangeException.NETWORK_ERROR);
        }
        List<MessageBody> resp = m.getResponses();
        if (resp == null || resp.size() != 1) return null;
        MessageBody mb = resp.get(0);
        try {
            ProxyUtils.checkException(mb);
        } catch (ExchangeException e) {
            throw e;
        } catch (Throwable t) {
            throw new ExchangeException(t.getMessage(), t, ExchangeException.SYSTEM_ERROR);
        }
        return ((ObjectMessageBody) mb).getObject();
    }

    public void contextDestroyed(ServletContextEvent arg0) {
        UserCore.getUserCore().destroy();
        if (sMsgr != null) sMsgr.destroy();
    }

    public static File getUploadDir() {
        return uploadDir;
    }

    public void contextInitialized(ServletContextEvent arg0) {
        log.error("Exchange service initialized");
        try {
            uc = UserCore.getUserCore();
            sMsgr = new ServerMessenger(uc);
            core = new ExchangeCore(this);
            uploadDir.mkdirs();
            UploadCore upCore = new UploadCore(uploadDir);
            sMsgr.addRecipient(this, Address.newServerAddress("upload"));
            UploadServiceProxy upProxy = new UploadServiceProxy(upCore, sMsgr);
            sMsgr.addRecipient(upProxy, Address.newServerAddress("fileUpload"));
            MessengerServlet.setWebappReady(true);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Message receive(Message message) {
        String command = message.getType();
        if (command == null) return null;
        ClientSession cliSess = (ClientSession) message.getSenderData();
        if (cliSess == null) {
            System.out.println("Can't get ClientSession from message");
            return null;
        }
        try {
            if (command.equals("sendStudy")) {
                ObjectMessageBody imb = (ObjectMessageBody) message.getBody();
                message.addResponse(new ObjectMessageBody(core.insertStudy((Study) imb.getObject(), cliSess)));
            } else if (command.equals("sendAssay")) {
                ObjectMessageBody imb = (ObjectMessageBody) message.getBody();
                message.addResponse(new ObjectMessageBody(core.insertAssay((AssayBean) imb.getObject(), cliSess)));
            }
        } catch (ExchangeException e) {
            message.addResponse(ProxyUtils.packException(e));
        } catch (Throwable e1) {
            System.out.println("Unknown error while processing incoming message. Client: " + cliSess);
            e1.printStackTrace();
        }
        return message;
    }

    public StudyId transferStudy(long stID, UserHttpSession sess) throws ExchangeException {
        StudyGroup sg = null;
        try {
            sg = (StudyGroup) sess.getObjectById(StudyGroup.class, stID);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExchangeException("AIMS exception: " + e.getMessage(), e, ExchangeException.AIMS_ERROR);
        }
        if (sg == null) throw new ExchangeException("Invalid study group ID: " + stID, ExchangeException.INV_STUDY_ID);
        try {
            return sendStudy(Study.createStudy(sg, sess.getAllParametersSorted(sg.getTechnology().getId(), true)));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExchangeException("AIMS exception", e, ExchangeException.AIMS_ERROR);
        }
    }

    public List<StudyId> transferAssays(long[] asIDs, UserHttpSession sess) throws ExchangeException {
        class LongPair {

            long v1, v2;

            public LongPair() {
            }

            public LongPair(long v1, long v2) {
                super();
                this.v1 = v1;
                this.v2 = v2;
            }

            public long getV1() {
                return v1;
            }

            public void setV1(long v1) {
                this.v1 = v1;
            }

            public long getV2() {
                return v2;
            }

            public void setV2(long v2) {
                this.v2 = v2;
            }
        }
        List<LongPair> idBind = new ArrayList<LongPair>();
        List<StudyId> sids = new ArrayList<StudyId>();
        Assay as = null;
        for (long asID : asIDs) {
            long rSgId;
            as = null;
            try {
                as = (Assay) sess.getObjectById(Assay.class, asID);
                if (as == null) {
                    System.out.println("Invalid assay ID=" + asID);
                    continue;
                }
                StudyGroup sg = as.getStudyGroup();
                long sgId = sg.getId();
                rSgId = -1;
                for (LongPair lp : idBind) {
                    if (lp.getV1() == sgId) {
                        rSgId = lp.getV2();
                        break;
                    }
                }
                if (rSgId == -1) {
                    StudyId sid = sendStudy(Study.createStudy(sg, sess.getAllParametersSorted(sg.getTechnology().getId(), true)));
                    rSgId = sid.getID();
                    idBind.add(new LongPair(sgId, rSgId));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExchangeException("AIMS exception: " + e.getMessage(), e, ExchangeException.AIMS_ERROR);
            }
            if (as == null) throw new ExchangeException("Invalid assay ID: " + asID, ExchangeException.INV_STUDY_ID);
            try {
                String techDir = UserHttpSession.getDataDirectoryPath() + File.separator + as.getTechnology().getVisibleFtpDirectory() + File.separator + FileString.getPrefix(true, true) + '_' + as.getId() + '_';
                String dfs = as.getDataFiles();
                if (dfs != null) {
                    int i = dfs.indexOf('\r');
                    if (i < 0) {
                        techDir = null;
                    } else {
                        i++;
                        int j = dfs.indexOf('\r', i);
                        if (j < 0) techDir = null; else {
                            techDir += dfs.substring(i, j);
                            techDir += ".dat";
                        }
                    }
                }
                System.out.println("Data file: " + techDir);
                String transactionKey = null;
                String dataFN = null;
                if (techDir != null) {
                    File dataFile = new File(techDir);
                    if (dataFile.isFile()) {
                        Transaction trn = sender.createTransaction();
                        transactionKey = trn.getKey();
                        System.out.println("Transaction key: " + transactionKey);
                        dataFN = "assayDataFile";
                        sender.startFileTransfer(trn, dataFN, dataFile.length());
                        sender.upload(trn, dataFN, dataFile);
                        sender.closeTransaction(trn, true);
                    }
                }
                AssayBean asb = AssayBean.createAssayBean(as, sess.getAllParametersSorted(as.getTechnology().getId(), true));
                asb.setStudy(rSgId);
                asb.setTransactionKey(transactionKey);
                asb.setDataFileName(dataFN);
                sids.add(sendAssay(asb));
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExchangeException("AIMS exception", e, ExchangeException.AIMS_ERROR);
            }
        }
        return sids;
    }

    public StudyId transferAssay(long asID, UserHttpSession sess) throws ExchangeException {
        Assay as = null;
        try {
            as = (Assay) sess.getObjectById(Assay.class, asID);
            String techDir = UserHttpSession.getDataDirectoryPath() + File.separator + as.getTechnology().getVisibleFtpDirectory() + File.separator + FileString.getPrefix(true, true) + '_' + as.getId() + '_';
            String dfs = as.getDataFiles();
            if (dfs != null) {
                int i = dfs.indexOf('\r');
                techDir += dfs.substring(i + 1, dfs.indexOf('\r', i + 1));
            }
            techDir += ".dat";
            System.out.println(techDir);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExchangeException("AIMS exception: " + e.getMessage(), e, ExchangeException.AIMS_ERROR);
        }
        if (as == null) throw new ExchangeException("Invalid assay ID: " + asID, ExchangeException.INV_STUDY_ID);
        try {
            return sendAssay(AssayBean.createAssayBean(as, sess.getAllParametersSorted(as.getTechnology().getId(), true)));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExchangeException("AIMS exception", e, ExchangeException.AIMS_ERROR);
        }
    }

    public void destroy() {
        try {
            cMsgr.setConnected(false);
        } catch (NetworkException e) {
            e.printStackTrace();
        }
        cMsgr.destroy();
    }
}
