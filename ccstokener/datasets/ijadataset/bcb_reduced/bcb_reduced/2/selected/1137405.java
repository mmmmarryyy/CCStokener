package org.dcm4che2.tool.dcmof;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.UID;
import org.dcm4che2.io.ContentHandlerAdapter;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.io.SAXWriter;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.Executor;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.VerificationService;
import org.dcm4che2.util.CloseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gunter zeilinger(gunterze@gmail.com)
 * @version $Revision: 5664 $ $Date: 2008-01-11 11:05:30 -0500 (Fri, 11 Jan 2008) $
 * @since Jan 22, 2006
 * 
 */
public class DcmOF {

    private static final int KB = 1024;

    private static final String DEF_TS = "defts";

    private static final String BIG_ENDIAN = "bigendian";

    private static final String SCN = "scn";

    private static final String SCNXML = "scnxml";

    private static final String IAN = "ian";

    private static final String IANXML = "ianxml";

    private static final String MPPS = "mpps";

    private static final String MPPSXML = "mppsxml";

    private static final String MWL = "mwl";

    private static final String PDV1 = "pdv1";

    private static final String ASYNC = "async";

    private static final String REQUEST_TO = "requestTO";

    private static final String RELEASE_TO = "releaseTO";

    private static final String SO_CLOSEDELAY = "soclosedelay";

    private static final String TCP_DELAY = "tcpdelay";

    private static final String SO_RCVBUF = "sorcvbuf";

    private static final String SO_SNDBUF = "sosndbuf";

    private static final String SND_PDULEN = "sndpdulen";

    private static final String RCV_PDULEN = "rcvpdulen";

    private static final String IDLE_TO = "idleTO";

    private static final String REAPER = "reaper";

    private static final String USAGE = "dcmof [Options] [<aet>[@<ip>]:]<port>";

    private static final String DESCRIPTION = "DICOM Server providing DICOM service of IHE actor Order Filler:\n" + "- Modality Worklist (MWL SCP),\n" + "- Modality Performed Procedure Step (MPPS SCP)\n" + "- Image Availability Notification (IAN SCP)\n" + "- Basic Study Content Notification (SCN SCP) {not specified by IHE}\n" + "listening on specified <port> for incoming association requests. " + "If no local IP address of the network interface is specified " + "connections on  any/all local addresses are accepted. " + "If <aet> is specified, only requests with matching called AE " + "title will be accepted.\n" + "Options:";

    private static final String EXAMPLE = "\nExample 1: dcmof DCM4CHE_OF:11112 -mwl /var/local/dcmof/mwl\n" + "=> Starts MWL SCP listening on port 11112, accepting association " + "requests with DCM4CHE_OF as called AE title, provides worklist items " + "stored in files in directory /var/local/dcmof/mwl as MWL SCP.\n" + "Example 2: dcmof DCM4CHE_OF:11112 -mpps /tmp -ian /tmp -scn /tmp\n" + "=> Starts MPPS+IAN+SCN SCP listening on port 11112, accepting association " + "requests with DCM4CHE_OF as called AE title, storing received messages " + "to /tmp.";

    private static char[] SECRET = { 's', 'e', 'c', 'r', 'e', 't' };

    private static final String[] ONLY_DEF_TS = { UID.ImplicitVRLittleEndian };

    private static final String[] NATIVE_TS = { UID.ExplicitVRLittleEndian, UID.ExplicitVRBigEndian, UID.ImplicitVRLittleEndian };

    private static final String[] NATIVE_LE_TS = { UID.ExplicitVRLittleEndian, UID.ImplicitVRLittleEndian };

    private static Logger log = LoggerFactory.getLogger(DcmOF.class);

    private static Executor executor = new NewThreadExecutor("DCMOF");

    private Device device = new Device("DCMOF");

    private NetworkApplicationEntity ae = new NetworkApplicationEntity();

    private NetworkConnection nc = new NetworkConnection();

    private String[] tsuids = NATIVE_LE_TS;

    private boolean indent = false;

    private boolean comments = false;

    private String keyStoreURL = "resource:tls/test_sys_2.p12";

    private char[] keyStorePassword = SECRET;

    private char[] keyPassword;

    private String trustStoreURL = "resource:tls/mesa_certs.jks";

    private char[] trustStorePassword = SECRET;

    public DcmOF() {
        device.setNetworkApplicationEntity(ae);
        device.setNetworkConnection(nc);
        ae.setNetworkConnection(nc);
        ae.setAssociationAcceptor(true);
        ae.register(new VerificationService());
    }

    public final void setAEtitle(String aet) {
        ae.setAETitle(aet);
    }

    public final void setHostname(String hostname) {
        nc.setHostname(hostname);
    }

    public final void setPort(int port) {
        nc.setPort(port);
    }

    public final void setTlsWithoutEncyrption() {
        nc.setTlsWithoutEncyrption();
    }

    public final void setTls3DES_EDE_CBC() {
        nc.setTls3DES_EDE_CBC();
    }

    public final void setTlsAES_128_CBC() {
        nc.setTlsAES_128_CBC();
    }

    public final void disableSSLv2Hello() {
        nc.disableSSLv2Hello();
    }

    public final void setTlsNeedClientAuth(boolean needClientAuth) {
        nc.setTlsNeedClientAuth(needClientAuth);
    }

    public final void setKeyStoreURL(String url) {
        keyStoreURL = url;
    }

    public final void setKeyStorePassword(String pw) {
        keyStorePassword = pw.toCharArray();
    }

    public final void setKeyPassword(String pw) {
        keyPassword = pw.toCharArray();
    }

    public final void setTrustStorePassword(String pw) {
        trustStorePassword = pw.toCharArray();
    }

    public final void setTrustStoreURL(String url) {
        trustStoreURL = url;
    }

    public final void setPackPDV(boolean packPDV) {
        ae.setPackPDV(packPDV);
    }

    public final void setAssociationReaperPeriod(int period) {
        device.setAssociationReaperPeriod(period);
    }

    public final void setTcpNoDelay(boolean tcpNoDelay) {
        nc.setTcpNoDelay(tcpNoDelay);
    }

    public final void setRequestTimeout(int timeout) {
        nc.setRequestTimeout(timeout);
    }

    public final void setReleaseTimeout(int timeout) {
        nc.setReleaseTimeout(timeout);
    }

    public final void setSocketCloseDelay(int delay) {
        nc.setSocketCloseDelay(delay);
    }

    public final void setIdleTimeout(int timeout) {
        ae.setIdleTimeout(timeout);
    }

    public final void setDimseRspTimeout(int timeout) {
        ae.setDimseRspTimeout(timeout);
    }

    public final void setMaxPDULengthSend(int maxLength) {
        ae.setMaxPDULengthSend(maxLength);
    }

    public void setMaxPDULengthReceive(int maxLength) {
        ae.setMaxPDULengthReceive(maxLength);
    }

    public final void setReceiveBufferSize(int bufferSize) {
        nc.setReceiveBufferSize(bufferSize);
    }

    public final void setSendBufferSize(int bufferSize) {
        nc.setSendBufferSize(bufferSize);
    }

    private static CommandLine parse(String[] args) {
        Options opts = new Options();
        OptionBuilder.withArgName("NULL|3DES|AES");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("enable TLS connection without, 3DES or AES encryption");
        opts.addOption(OptionBuilder.create("tls"));
        opts.addOption("nossl2", false, "disable acceptance of SSLv2Hello TLS handshake");
        opts.addOption("noclientauth", false, "disable client authentification for TLS");
        OptionBuilder.withArgName("file|url");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("file path or URL of P12 or JKS keystore, resource:tls/test_sys_2.p12 by default");
        opts.addOption(OptionBuilder.create("keystore"));
        OptionBuilder.withArgName("password");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("password for keystore file, 'secret' by default");
        opts.addOption(OptionBuilder.create("keystorepw"));
        OptionBuilder.withArgName("password");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("password for accessing the key in the keystore, keystore password by default");
        opts.addOption(OptionBuilder.create("keypw"));
        OptionBuilder.withArgName("file|url");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("file path or URL of JKS truststore, resource:tls/mesa_certs.jks by default");
        opts.addOption(OptionBuilder.create("truststore"));
        OptionBuilder.withArgName("password");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("password for truststore file, 'secret' by default");
        opts.addOption(OptionBuilder.create("truststorepw"));
        OptionBuilder.withArgName("dir");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Activate MWL SCP, providing MWL Items stored in specified directory.");
        opts.addOption(OptionBuilder.create(MWL));
        OptionGroup mpps = new OptionGroup();
        OptionBuilder.withArgName("dir");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Activate MPPS SCP, storing received MPPS in specified directory.");
        mpps.addOption(OptionBuilder.create(MPPS));
        OptionBuilder.withArgName("dir");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Activate MPPS SCP, storing XML received MPPS in specified directory in XML format.");
        mpps.addOption(OptionBuilder.create(MPPSXML));
        opts.addOptionGroup(mpps);
        OptionGroup ian = new OptionGroup();
        OptionBuilder.withArgName("dir");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Activate IAN SCP, storing received IAN in specified directory.");
        ian.addOption(OptionBuilder.create(IAN));
        OptionBuilder.withArgName("dir");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Activate IAN SCP, storing received IAN in specified directory in XML format.");
        ian.addOption(OptionBuilder.create(IANXML));
        opts.addOptionGroup(ian);
        OptionGroup scn = new OptionGroup();
        OptionBuilder.withArgName("dir");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Activate SCN SCP, storing received SCN in specified directory.");
        scn.addOption(OptionBuilder.create(SCN));
        OptionBuilder.withArgName("dir");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Activate SCN SCP, storing received SCN in specified directory in XML format.");
        scn.addOption(OptionBuilder.create(SCNXML));
        opts.addOptionGroup(scn);
        opts.addOption("c", "compact", false, "suppress additional whitespaces in XML output");
        opts.addOption("C", "comments", false, "include attribute names as comments in XML output");
        OptionGroup ts = new OptionGroup();
        OptionBuilder.withDescription("accept only default Transfer Syntax.");
        ts.addOption(OptionBuilder.create(DEF_TS));
        OptionBuilder.withDescription("accept Explict VR Big Endian Transfer Syntax.");
        ts.addOption(OptionBuilder.create(BIG_ENDIAN));
        opts.addOptionGroup(ts);
        OptionBuilder.withArgName("maxops");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("maximum number of outstanding operations performed " + "asynchronously, unlimited by default.");
        opts.addOption(OptionBuilder.create(ASYNC));
        opts.addOption(PDV1, false, "send only one PDV in one P-Data-TF PDU, " + "pack command and data PDV in one P-DATA-TF PDU by default.");
        opts.addOption(TCP_DELAY, false, "set TCP_NODELAY socket option to false, true by default");
        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("delay in ms for Socket close after sending A-ABORT, 50ms by default");
        opts.addOption(OptionBuilder.create(SO_CLOSEDELAY));
        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("timeout in ms for receiving -ASSOCIATE-RQ, 5s by default");
        opts.addOption(OptionBuilder.create(REQUEST_TO));
        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("timeout in ms for receiving A-RELEASE-RP, 5s by default");
        opts.addOption(OptionBuilder.create(RELEASE_TO));
        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("period in ms to check for outstanding DIMSE-RSP, 10s by default");
        opts.addOption(OptionBuilder.create(REAPER));
        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("timeout in ms for receiving DIMSE-RQ, 60s by default");
        opts.addOption(OptionBuilder.create(IDLE_TO));
        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("maximal length in KB of received P-DATA-TF PDUs, 16KB by default");
        opts.addOption(OptionBuilder.create(RCV_PDULEN));
        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("maximal length in KB of sent P-DATA-TF PDUs, 16KB by default");
        opts.addOption(OptionBuilder.create(SND_PDULEN));
        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("set SO_RCVBUF socket option to specified value in KB");
        opts.addOption(OptionBuilder.create(SO_RCVBUF));
        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("set SO_SNDBUF socket option to specified value in KB");
        opts.addOption(OptionBuilder.create(SO_SNDBUF));
        opts.addOption("h", "help", false, "print this message");
        opts.addOption("V", "version", false, "print the version information and exit");
        CommandLine cl = null;
        try {
            cl = new GnuParser().parse(opts, args);
        } catch (ParseException e) {
            exit("dcmof: " + e.getMessage());
            throw new RuntimeException("unreachable");
        }
        if (cl.hasOption("V")) {
            Package p = DcmOF.class.getPackage();
            System.out.println("dcmof v" + p.getImplementationVersion());
            System.exit(0);
        }
        if (cl.hasOption("h") || cl.getArgList().size() == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(USAGE, DESCRIPTION, opts, EXAMPLE);
            System.exit(0);
        }
        return cl;
    }

    public static void main(String[] args) {
        CommandLine cl = parse(args);
        DcmOF dcmof = new DcmOF();
        final List argList = cl.getArgList();
        String port = (String) argList.get(0);
        String[] aetPort = split(port, ':', 1);
        dcmof.setPort(parseInt(aetPort[1], "illegal port number", 1, 0xffff));
        if (aetPort[0] != null) {
            String[] aetHost = split(aetPort[0], '@', 0);
            dcmof.setAEtitle(aetHost[0]);
            dcmof.setHostname(aetHost[1]);
        }
        if (cl.hasOption(DEF_TS)) dcmof.setTransferSyntax(ONLY_DEF_TS); else if (cl.hasOption(BIG_ENDIAN)) dcmof.setTransferSyntax(NATIVE_TS);
        if (cl.hasOption(REAPER)) dcmof.setAssociationReaperPeriod(parseInt(cl.getOptionValue(REAPER), "illegal argument of option -reaper", 1, Integer.MAX_VALUE));
        if (cl.hasOption(IDLE_TO)) dcmof.setIdleTimeout(parseInt(cl.getOptionValue(IDLE_TO), "illegal argument of option -idleTO", 1, Integer.MAX_VALUE));
        if (cl.hasOption(REQUEST_TO)) dcmof.setRequestTimeout(parseInt(cl.getOptionValue(REQUEST_TO), "illegal argument of option -requestTO", 1, Integer.MAX_VALUE));
        if (cl.hasOption(RELEASE_TO)) dcmof.setReleaseTimeout(parseInt(cl.getOptionValue(RELEASE_TO), "illegal argument of option -releaseTO", 1, Integer.MAX_VALUE));
        if (cl.hasOption(SO_CLOSEDELAY)) dcmof.setSocketCloseDelay(parseInt(cl.getOptionValue(SO_CLOSEDELAY), "illegal argument of option -soclosedelay", 1, 10000));
        if (cl.hasOption(RCV_PDULEN)) dcmof.setMaxPDULengthReceive(parseInt(cl.getOptionValue(RCV_PDULEN), "illegal argument of option -rcvpdulen", 1, 10000) * KB);
        if (cl.hasOption(SND_PDULEN)) dcmof.setMaxPDULengthSend(parseInt(cl.getOptionValue(SND_PDULEN), "illegal argument of option -sndpdulen", 1, 10000) * KB);
        if (cl.hasOption(SO_SNDBUF)) dcmof.setSendBufferSize(parseInt(cl.getOptionValue(SO_SNDBUF), "illegal argument of option -sosndbuf", 1, 10000) * KB);
        if (cl.hasOption(SO_RCVBUF)) dcmof.setReceiveBufferSize(parseInt(cl.getOptionValue(SO_RCVBUF), "illegal argument of option -sorcvbuf", 1, 10000) * KB);
        dcmof.setPackPDV(!cl.hasOption(PDV1));
        dcmof.setTcpNoDelay(!cl.hasOption(TCP_DELAY));
        if (cl.hasOption(ASYNC)) dcmof.setMaxOpsPerformed(parseInt(cl.getOptionValue(ASYNC), "illegal argument of option -async", 0, 0xffff));
        ArrayList<TransferCapability> tc = new ArrayList<TransferCapability>();
        tc.add(new TransferCapability(UID.VerificationSOPClass, ONLY_DEF_TS, TransferCapability.SCP));
        if (cl.hasOption(MWL)) dcmof.registerMWLSCP(new File(cl.getOptionValue(MWL)), tc);
        if (cl.hasOption(MPPS)) dcmof.registerMPPSSCP(new File(cl.getOptionValue(MPPS)), tc);
        if (cl.hasOption(MPPSXML)) dcmof.registerMPPSXMLSCP(new File(cl.getOptionValue(MPPSXML)), tc);
        if (cl.hasOption(IAN)) dcmof.registerIANSCP(new File(cl.getOptionValue(IAN)), tc);
        if (cl.hasOption(IANXML)) dcmof.registerIANXMLSCP(new File(cl.getOptionValue(IANXML)), tc);
        if (cl.hasOption(SCN)) dcmof.registerSCNSCP(new File(cl.getOptionValue(SCN)), tc);
        if (cl.hasOption(SCNXML)) dcmof.registerSCNXMLSCP(new File(cl.getOptionValue(SCNXML)), tc);
        dcmof.setComments(cl.hasOption("C"));
        dcmof.setIndent(!cl.hasOption("c"));
        dcmof.setTransferCapability(tc.toArray(new TransferCapability[tc.size()]));
        if (cl.hasOption("tls")) {
            String cipher = cl.getOptionValue("tls");
            if ("NULL".equalsIgnoreCase(cipher)) {
                dcmof.setTlsWithoutEncyrption();
            } else if ("3DES".equalsIgnoreCase(cipher)) {
                dcmof.setTls3DES_EDE_CBC();
            } else if ("AES".equalsIgnoreCase(cipher)) {
                dcmof.setTlsAES_128_CBC();
            } else {
                exit("Invalid parameter for option -tls: " + cipher);
            }
            if (cl.hasOption("nossl2")) {
                dcmof.disableSSLv2Hello();
            }
            dcmof.setTlsNeedClientAuth(!cl.hasOption("noclientauth"));
            if (cl.hasOption("keystore")) {
                dcmof.setKeyStoreURL(cl.getOptionValue("keystore"));
            }
            if (cl.hasOption("keystorepw")) {
                dcmof.setKeyStorePassword(cl.getOptionValue("keystorepw"));
            }
            if (cl.hasOption("keypw")) {
                dcmof.setKeyPassword(cl.getOptionValue("keypw"));
            }
            if (cl.hasOption("truststore")) {
                dcmof.setTrustStoreURL(cl.getOptionValue("truststore"));
            }
            if (cl.hasOption("truststorepw")) {
                dcmof.setTrustStorePassword(cl.getOptionValue("truststorepw"));
            }
            try {
                dcmof.initTLS();
            } catch (Exception e) {
                System.err.println("ERROR: Failed to initialize TLS context:" + e.getMessage());
                System.exit(2);
            }
        }
        try {
            dcmof.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setIndent(boolean b) {
        this.indent = b;
    }

    private void setComments(boolean b) {
        this.comments = b;
    }

    private void setTransferSyntax(String[] tsuids) {
        this.tsuids = tsuids;
    }

    private void registerMWLSCP(File dir, ArrayList<TransferCapability> tc) {
        MWLSCP mwlscp = new MWLSCP(executor, this);
        mwlscp.setSource(dir);
        ae.register(mwlscp);
        tc.add(new TransferCapability(mwlscp.getSopClass(), tsuids, TransferCapability.SCP));
    }

    private void registerMPPSSCP(File dir, ArrayList<TransferCapability> tc) {
        register(new MPPSSCP(this), dir, tc);
    }

    private void registerMPPSXMLSCP(File dir, ArrayList<TransferCapability> tc) {
        register(new MPPSSCP.XML(this), dir, tc);
    }

    private void register(MPPSSCP mppsscp, File dir, ArrayList<TransferCapability> tc) {
        mppsscp.setDestination(dir);
        ae.register(mppsscp.getNCreateSCP());
        ae.register(mppsscp.getNSetSCP());
        tc.add(new TransferCapability(mppsscp.getNCreateSCP().getSopClass(), tsuids, TransferCapability.SCP));
    }

    private void registerIANXMLSCP(File dir, ArrayList<TransferCapability> tc) {
        register(new IANSCP.XML(this), dir, tc);
    }

    private void registerIANSCP(File dir, ArrayList<TransferCapability> tc) {
        register(new IANSCP(this), dir, tc);
    }

    private void register(IANSCP ianscp, File dir, ArrayList<TransferCapability> tc) {
        ianscp.setDestination(dir);
        ae.register(ianscp);
        tc.add(new TransferCapability(ianscp.getSopClass(), tsuids, TransferCapability.SCP));
    }

    private void registerSCNSCP(File dir, ArrayList<TransferCapability> tc) {
        register(new SCNSCP(this), dir, tc);
    }

    private void registerSCNXMLSCP(File dir, ArrayList<TransferCapability> tc) {
        register(new SCNSCP.XML(this), dir, tc);
    }

    private void register(SCNSCP scnscp, File dir, ArrayList<TransferCapability> tc) {
        scnscp.setDestination(dir);
        ae.register(scnscp);
        tc.add(new TransferCapability(scnscp.getSopClass(), tsuids, TransferCapability.SCP));
    }

    private void setTransferCapability(TransferCapability[] tc) {
        ae.setTransferCapability(tc);
    }

    private void setMaxOpsPerformed(int maxOps) {
        ae.setMaxOpsPerformed(maxOps);
    }

    public void start() throws IOException {
        device.startListening(executor);
        System.out.println("Start Server listening on port " + nc.getPort());
    }

    private static String[] split(String s, char delim, int defPos) {
        String[] s2 = new String[2];
        s2[defPos] = s;
        int pos = s.indexOf(delim);
        if (pos != -1) {
            s2[0] = s.substring(0, pos);
            s2[1] = s.substring(pos + 1);
        }
        return s2;
    }

    private static void exit(String msg) {
        System.err.println(msg);
        System.err.println("Try 'dcmof -h' for more information.");
        System.exit(1);
    }

    private static int parseInt(String s, String errPrompt, int min, int max) {
        try {
            int i = Integer.parseInt(s);
            if (i >= min && i <= max) return i;
        } catch (NumberFormatException e) {
        }
        exit(errPrompt);
        throw new RuntimeException();
    }

    void storeAsXML(File f, DicomObject data) throws Exception {
        log.info("M-WRITE " + f);
        SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
        TransformerHandler th = tf.newTransformerHandler();
        if (indent) th.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            th.setResult(new StreamResult(fos));
            new SAXWriter(th, comments ? th : null).write(data);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ioe) {
                }
            }
        }
    }

    void storeAsDICOM(File f, DicomObject data) throws Exception {
        log.info("M-WRITE " + f);
        DicomOutputStream out = new DicomOutputStream(new FileOutputStream(f));
        try {
            out.writeDicomFile(data);
        } finally {
            CloseUtils.safeClose(out);
        }
    }

    DicomObject load(File f) throws Exception {
        log.info("M-READ " + f);
        return f.getName().endsWith(".xml") ? loadXML(f) : loadDICOM(f);
    }

    private DicomObject loadDICOM(File f) throws Exception {
        DicomInputStream in = new DicomInputStream(f);
        try {
            return in.readDicomObject();
        } finally {
            in.close();
        }
    }

    private DicomObject loadXML(File f) throws Exception {
        DicomObject dcmobj = new BasicDicomObject();
        SAXParser p = SAXParserFactory.newInstance().newSAXParser();
        ContentHandlerAdapter ch = new ContentHandlerAdapter(dcmobj);
        p.parse(f, ch);
        return dcmobj;
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

    private static InputStream openFileOrURL(String url) throws IOException {
        if (url.startsWith("resource:")) {
            return DcmOF.class.getClassLoader().getResourceAsStream(url.substring(9));
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
