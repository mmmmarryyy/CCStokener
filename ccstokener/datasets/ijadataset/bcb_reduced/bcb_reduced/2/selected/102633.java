package org.dcm4che2.tool.dcmecho;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dcm4che2.data.UID;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.Executor;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.UserIdentity;

/**
 * @author gunter zeilinger(gunterze@gmail.com)
 * @version $Revision: 6073 $ $Date: 2008-02-22 07:18:18 -0500 (Fri, 22 Feb 2008) $
 * @since Oct 13, 2005
 */
public class DcmEcho {

    private static final String USAGE = "dcmecho [Options] <aet>[@<host>[:<port>]]";

    private static final String DESCRIPTION = "Send DICOM Echo to the specified remote Application Entity. " + "If <port> is not specified, DICOM default port 104 is assumed. " + "If also no <host> is specified localhost is assumed.\n" + "Options:";

    private static final String EXAMPLE = "\nExample: dcmecho STORESCP@localhost:11112 \n" + "=> Verify connection to Application Entity STORESCP, " + "listening on local port 11112.";

    private static char[] SECRET = { 's', 'e', 'c', 'r', 'e', 't' };

    private static final String[] DEF_TS = { UID.ImplicitVRLittleEndian };

    private static final TransferCapability VERIFICATION_SCU = new TransferCapability(UID.VerificationSOPClass, DEF_TS, TransferCapability.SCU);

    private Executor executor = new NewThreadExecutor("DCMECHO");

    private NetworkApplicationEntity remoteAE = new NetworkApplicationEntity();

    private NetworkConnection remoteConn = new NetworkConnection();

    private Device device = new Device("DCMECHO");

    private NetworkApplicationEntity ae = new NetworkApplicationEntity();

    private NetworkConnection conn = new NetworkConnection();

    private Association assoc;

    private String keyStoreURL = "resource:tls/test_sys_1.p12";

    private char[] keyStorePassword = SECRET;

    private char[] keyPassword;

    private String trustStoreURL = "resource:tls/mesa_certs.jks";

    private char[] trustStorePassword = SECRET;

    public DcmEcho() {
        remoteAE.setInstalled(true);
        remoteAE.setAssociationAcceptor(true);
        remoteAE.setNetworkConnection(new NetworkConnection[] { remoteConn });
        device.setNetworkApplicationEntity(ae);
        device.setNetworkConnection(conn);
        ae.setNetworkConnection(conn);
        ae.setAssociationInitiator(true);
        ae.setAETitle("DCMECHO");
        ae.setTransferCapability(new TransferCapability[] { VERIFICATION_SCU });
    }

    public final void setLocalHost(String hostname) {
        conn.setHostname(hostname);
    }

    public final void setRemoteHost(String hostname) {
        remoteConn.setHostname(hostname);
    }

    public final void setRemotePort(int port) {
        remoteConn.setPort(port);
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

    public final void setCalledAET(String called, boolean reuse) {
        remoteAE.setAETitle(called);
        if (reuse) ae.setReuseAssocationToAETitle(new String[] { called });
    }

    public final void setCalling(String calling) {
        ae.setAETitle(calling);
    }

    public final void setUserIdentity(UserIdentity userIdentity) {
        ae.setUserIdentity(userIdentity);
    }

    public final void setIdleTimeout(int timeout) {
        ae.setIdleTimeout(timeout);
    }

    public final void setAssociationReaperPeriod(int period) {
        device.setAssociationReaperPeriod(period);
    }

    public final void setDimseRspTimeout(int timeout) {
        ae.setDimseRspTimeout(timeout);
    }

    public final void setConnectTimeout(int connectTimeout) {
        conn.setConnectTimeout(connectTimeout);
    }

    public final void setAcceptTimeout(int timeout) {
        conn.setAcceptTimeout(timeout);
    }

    public final void setReleaseTimeout(int timeout) {
        conn.setReleaseTimeout(timeout);
    }

    public final void setSocketCloseDelay(int timeout) {
        conn.setSocketCloseDelay(timeout);
    }

    private static CommandLine parse(String[] args) {
        Options opts = new Options();
        OptionBuilder.withArgName("aet[@host]");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("set AET and local address of local Application Entity, use " + "ANONYMOUS and pick up any valid local address to bind the " + "socket by default");
        opts.addOption(OptionBuilder.create("L"));
        OptionBuilder.withArgName("username");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("enable User Identity Negotiation with specified username and " + " optional passcode");
        opts.addOption(OptionBuilder.create("username"));
        OptionBuilder.withArgName("passcode");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("optional passcode for User Identity Negotiation, " + "only effective with option -username");
        opts.addOption(OptionBuilder.create("passcode"));
        opts.addOption("uidnegrsp", false, "request positive User Identity Negotation response, " + "only effective with option -username");
        OptionBuilder.withArgName("NULL|3DES|AES");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("enable TLS connection without, 3DES or AES encryption");
        opts.addOption(OptionBuilder.create("tls"));
        OptionBuilder.withArgName("file|url");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("file path or URL of P12 or JKS keystore, resource:tls/test_sys_1.p12 by default");
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
        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("timeout in ms for TCP connect, no timeout by default");
        opts.addOption(OptionBuilder.create("connectTO"));
        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("delay in ms for Socket close after sending A-ABORT, 50ms by default");
        opts.addOption(OptionBuilder.create("soclosedelay"));
        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("period in ms to check for outstanding DIMSE-RSP, 10s by default");
        opts.addOption(OptionBuilder.create("reaper"));
        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("timeout in ms for receiving DIMSE-RSP, 60s by default");
        opts.addOption(OptionBuilder.create("rspTO"));
        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("timeout in ms for receiving A-ASSOCIATE-AC, 5s by default");
        opts.addOption(OptionBuilder.create("acceptTO"));
        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("timeout in ms for receiving A-RELEASE-RP, 5s by default");
        opts.addOption(OptionBuilder.create("releaseTO"));
        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("timeout in ms for receiving DIMSE-RQ, 10s by default");
        opts.addOption(OptionBuilder.create("idleTO"));
        OptionBuilder.withArgName("num");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("repeat C-ECHO RQ several times");
        opts.addOption(OptionBuilder.create("repeat"));
        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("delay in ms between repeated C-FIND RQ, immediately after C-FIND RSP by default");
        opts.addOption(OptionBuilder.create("repeatdelay"));
        opts.addOption("reuseassoc", false, "Reuse association for repeated C-ECHO");
        opts.addOption("closeassoc", false, "Close association after each C-ECHO");
        opts.addOption("h", "help", false, "print this message");
        opts.addOption("V", "version", false, "print the version information and exit");
        CommandLine cl = null;
        try {
            cl = new GnuParser().parse(opts, args);
        } catch (ParseException e) {
            exit("dcmecho: " + e.getMessage());
            throw new RuntimeException("unreachable");
        }
        if (cl.hasOption('V')) {
            Package p = DcmEcho.class.getPackage();
            System.out.println("dcmecho v" + p.getImplementationVersion());
            System.exit(0);
        }
        if (cl.hasOption('h') || cl.getArgList().size() != 1) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(USAGE, DESCRIPTION, opts, EXAMPLE);
            System.exit(0);
        }
        return cl;
    }

    public static void main(String[] args) {
        CommandLine cl = parse(args);
        DcmEcho dcmecho = new DcmEcho();
        final List argList = cl.getArgList();
        String remoteAE = (String) argList.get(0);
        String[] calledAETAddress = split(remoteAE, '@');
        dcmecho.setCalledAET(calledAETAddress[0], cl.hasOption("reuseassoc"));
        if (calledAETAddress[1] == null) {
            dcmecho.setRemoteHost("127.0.0.1");
            dcmecho.setRemotePort(104);
        } else {
            String[] hostPort = split(calledAETAddress[1], ':');
            dcmecho.setRemoteHost(hostPort[0]);
            dcmecho.setRemotePort(toPort(hostPort[1]));
        }
        if (cl.hasOption("L")) {
            String localAE = cl.getOptionValue("L");
            String[] callingAETHost = split(localAE, '@');
            dcmecho.setCalling(callingAETHost[0]);
            if (callingAETHost[1] != null) {
                dcmecho.setLocalHost(callingAETHost[1]);
            }
        }
        if (cl.hasOption("username")) {
            String username = cl.getOptionValue("username");
            UserIdentity userId;
            if (cl.hasOption("passcode")) {
                String passcode = cl.getOptionValue("passcode");
                userId = new UserIdentity.UsernamePasscode(username, passcode.toCharArray());
            } else {
                userId = new UserIdentity.Username(username);
            }
            userId.setPositiveResponseRequested(cl.hasOption("uidnegrsp"));
            dcmecho.setUserIdentity(userId);
        }
        if (cl.hasOption("connectTO")) dcmecho.setConnectTimeout(parseInt(cl.getOptionValue("connectTO"), "illegal argument of option -connectTO", 1, Integer.MAX_VALUE));
        dcmecho.setIdleTimeout(cl.hasOption("idleTO") ? parseInt(cl.getOptionValue("idleTO"), "illegal argument of option -idleTO", 1, Integer.MAX_VALUE) : 10000);
        if (cl.hasOption("reaper")) dcmecho.setAssociationReaperPeriod(parseInt(cl.getOptionValue("reaper"), "illegal argument of option -reaper", 1, Integer.MAX_VALUE));
        if (cl.hasOption("rspTO")) dcmecho.setDimseRspTimeout(parseInt(cl.getOptionValue("rspTO"), "illegal argument of option -rspTO", 1, Integer.MAX_VALUE));
        if (cl.hasOption("acceptTO")) dcmecho.setAcceptTimeout(parseInt(cl.getOptionValue("acceptTO"), "illegal argument of option -acceptTO", 1, Integer.MAX_VALUE));
        if (cl.hasOption("releaseTO")) dcmecho.setReleaseTimeout(parseInt(cl.getOptionValue("releaseTO"), "illegal argument of option -releaseTO", 1, Integer.MAX_VALUE));
        if (cl.hasOption("soclosedelay")) dcmecho.setSocketCloseDelay(parseInt(cl.getOptionValue("soclosedelay"), "illegal argument of option -soclosedelay", 1, 10000));
        int repeat = cl.hasOption("repeat") ? parseInt(cl.getOptionValue("repeat"), "illegal argument of option -repeat", 1, Integer.MAX_VALUE) : 0;
        int interval = cl.hasOption("repeatdelay") ? parseInt(cl.getOptionValue("repeatdelay"), "illegal argument of option -repeatdelay", 1, Integer.MAX_VALUE) : 0;
        boolean closeAssoc = cl.hasOption("closeassoc");
        if (cl.hasOption("tls")) {
            String cipher = cl.getOptionValue("tls");
            if ("NULL".equalsIgnoreCase(cipher)) {
                dcmecho.setTlsWithoutEncyrption();
            } else if ("3DES".equalsIgnoreCase(cipher)) {
                dcmecho.setTls3DES_EDE_CBC();
            } else if ("AES".equalsIgnoreCase(cipher)) {
                dcmecho.setTlsAES_128_CBC();
            } else {
                exit("Invalid parameter for option -tls: " + cipher);
            }
            if (cl.hasOption("keystore")) {
                dcmecho.setKeyStoreURL(cl.getOptionValue("keystore"));
            }
            if (cl.hasOption("keystorepw")) {
                dcmecho.setKeyStorePassword(cl.getOptionValue("keystorepw"));
            }
            if (cl.hasOption("keypw")) {
                dcmecho.setKeyPassword(cl.getOptionValue("keypw"));
            }
            if (cl.hasOption("truststore")) {
                dcmecho.setTrustStoreURL(cl.getOptionValue("truststore"));
            }
            if (cl.hasOption("truststorepw")) {
                dcmecho.setTrustStorePassword(cl.getOptionValue("truststorepw"));
            }
            long t1 = System.currentTimeMillis();
            try {
                dcmecho.initTLS();
            } catch (Exception e) {
                System.err.println("ERROR: Failed to initialize TLS context:" + e.getMessage());
                System.exit(2);
            }
            long t2 = System.currentTimeMillis();
            System.out.println("Initialize TLS context in " + ((t2 - t1) / 1000F) + "s");
        }
        long t1 = System.currentTimeMillis();
        try {
            dcmecho.open();
        } catch (Exception e) {
            System.err.println("ERROR: Failed to establish association:" + e.getMessage());
            System.exit(2);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("Connected to " + remoteAE + " in " + ((t2 - t1) / 1000F) + "s");
        for (; ; ) {
            try {
                dcmecho.echo();
                long t3 = System.currentTimeMillis();
                System.out.println("Perform Verification in " + ((t2 - t3) / 1000F) + "s");
                if (repeat == 0 || closeAssoc) {
                    dcmecho.close();
                    System.out.println("Released connection to " + remoteAE);
                }
                if (repeat-- == 0) break;
                Thread.sleep(interval);
                long t4 = System.currentTimeMillis();
                dcmecho.open();
                t2 = System.currentTimeMillis();
                System.out.println("Reconnect or reuse connection to " + remoteAE + " in " + ((t2 - t4) / 1000F) + "s");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
        }
    }

    private static int toPort(String port) {
        return port != null ? parseInt(port, "illegal port number", 1, 0xffff) : 104;
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

    private static String[] split(String s, char delim) {
        String[] s2 = { s, null };
        int pos = s.indexOf(delim);
        if (pos != -1) {
            s2[0] = s.substring(0, pos);
            s2[1] = s.substring(pos + 1);
        }
        return s2;
    }

    private static void exit(String msg) {
        System.err.println(msg);
        System.err.println("Try 'dcmecho -h' for more information.");
        System.exit(1);
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
            return DcmEcho.class.getClassLoader().getResourceAsStream(url.substring(9));
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

    public void open() throws IOException, ConfigurationException, InterruptedException {
        assoc = ae.connect(remoteAE, executor);
    }

    public void echo() throws IOException, InterruptedException {
        assoc.cecho().next();
    }

    public void close() throws InterruptedException {
        assoc.release(true);
    }
}
