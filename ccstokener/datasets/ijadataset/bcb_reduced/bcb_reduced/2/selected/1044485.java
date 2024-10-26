package fairVote.agent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import fairVote.data.Credential;
import fairVote.data.KeyStoreData;
import fairVote.util.Crypto;
import fairVote.util.FairLog;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

public class AgentCore {

    private static Logger LOGGER = FairLog.getLogger(AgentCore.class.getName());

    public static final int SessionTokenSize = 32;

    public static final int FORMAT_HTML = 1;

    public static final int FORMAT_PLAIN = 2;

    public static final int FORMAT_PLAINDOS = 3;

    public static final int FORMAT_XML = 4;

    public static final int FORMAT_XMLNOHEAD = 5;

    public static final String ERRMSG_BUG = "FAIL::BUG";

    public static final String ERRMSG_VOTATIONCLOSED = "FAIL::CLOSED";

    public static final String ERRMSG_CONNECT = "FAIL::CONNECTERROR";

    public static final String ERRMSG_DUPLICATE = "FAIL::DUPLICATE";

    public static final String ERRMSG_GENERIC = "FAIL::GENERIC";

    public static final String ERRMSG_IDCONFLICT = "FAIL::IDCONFLICT";

    public static final String ERRMSG_IO = "FAIL::IOERROR";

    public static final String ERRMSG_MYSQL = "FAIL::MYSQL";

    public static final String ERRMSG_VOTATIONNOTCLOSE = "FAIL::NOTCLOSE";

    public static final String ERRMSG_NOTELIGIBLE = "FAIL::NOTELIGIBLE";

    public static final String ERRMSG_VOTATIONNOTOPEN = "FAIL::NOTOPEN";

    public static final String ERRMSG_OK = "OK";

    public static final String ERRMSG_VOTATIONOPEN = "FAIL::OPEN";

    public static final String ERRMSG_T2DIFFER = "FAIL::T2DIFFER";

    public static final String ERRMSG_VOTATIONLOCKED = "FAIL::VOTELOCKED";

    public static final String ERRMSG_VOTATIONPRESENT = "FAIL::YETINSERTED";

    public static final String ERRMSG_VOTATIONNOTPRESENT = "FAIL::NOTINSERTED";

    public static final String ERRMSG_NOTREGISTRAR = "FAIL::IM_NOT_REGISTRAR";

    public static final String ERRMSG_NOTFORWARDER = "FAIL::IM_NOT_FORWARDER";

    public static final String ERRMSG_NOTCOLLECTOR = "FAIL::IM_NOT_COLLECTOR";

    public static final String ERRMSG_IREGISTRAR = "FAIL::IM_REGISTRAR";

    public static final String ERRMSG_IFORWARDER = "FAIL::IM_FORWARDER";

    public static final String ERRMSG_ICOLLECTOR = "FAIL::IM_COLLECTOR";

    public static final int debuglevel = 5;

    public static String buildMsg(String message, int format) {
        String msgout = "";
        if (format == AgentCore.FORMAT_XML) {
            msgout = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
            msgout += "<message>" + message.replaceAll("[\r\n\t ]", " ").replaceAll(" $", "") + "</message>\n";
        } else if (format == AgentCore.FORMAT_HTML) {
            msgout = "<html><body>\n";
            msgout += message.replaceAll("/[\r\n\t ]/", " ").replaceAll("/ $/", "");
            msgout += "</body></html>\n";
        } else {
            msgout += message;
        }
        return msgout;
    }

    public static Document createRequestCheckID(Credential credential) {
        DocumentFactory df = new DocumentFactory();
        Document request = df.createDocument();
        Element r = request.addElement("RegistrarRequest");
        r.addElement("command").addAttribute("type", "CheckID");
        r.add(credential.getRootElement());
        return request;
    }

    public static String sendBytes(byte[] data, String urlServer) throws UnsupportedEncodingException, MalformedURLException, IOException {
        return sendBytes(data, urlServer, "");
    }

    public static String sendBytes(byte[] data, String urlServer, String rt) throws UnsupportedEncodingException, MalformedURLException, IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sending bytes to url: " + urlServer);
        }
        URL url = new URL(urlServer);
        URLConnection conn = url.openConnection();
        conn.setReadTimeout(0);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestProperty("Content-Transfer-Encoding", "binary");
        conn.setRequestProperty("Content-Length", new Integer(data.length).toString());
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.connect();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("must send " + data.length + " bytes ");
        OutputStream out = conn.getOutputStream();
        out.write(data);
        out.flush();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        StringBuilder readContent = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            readContent.append(line);
            readContent.append(rt);
        }
        out.close();
        rd.close();
        return readContent.toString();
    }

    public static String __sendBytes(byte[] data, String urlServer, String rt) throws UnsupportedEncodingException, MalformedURLException, IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sending bytes to url: " + urlServer);
        }
        URL url = new URL(urlServer);
        String host = url.getHost();
        int port = url.getPort();
        if (port == -1) port = url.getDefaultPort();
        Socket sock = new Socket();
        try {
            sock.bind(null);
            sock.setKeepAlive(true);
            sock.connect(new InetSocketAddress(host, port), 1000);
            OutputStream out = sock.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String path = url.getPath();
            if (path.startsWith("//")) path = path.substring(1);
            StringBuilder buildStr = new StringBuilder();
            buildStr.append("POST " + path + " HTTP/1.0\r\n");
            buildStr.append("Host: " + url.getHost() + "\r\n");
            buildStr.append("Content-Type: application/octet-stream\r\n");
            buildStr.append("Content-Transfer-Encoding: binary\r\n");
            buildStr.append("Content-Length: " + data.length + "\r\n");
            buildStr.append("\r\n");
            if (LOGGER.isTraceEnabled()) LOGGER.trace("must send " + data.length + " bytes ");
            out.write(buildStr.toString().getBytes("ASCII"));
            out.write(data);
            out.flush();
            String line;
            StringBuilder readContent = new StringBuilder();
            while ((line = in.readLine()) != null) {
                readContent.append(line);
                readContent.append(rt);
            }
            out.close();
            in.close();
            sock.close();
            return readContent.toString();
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException e) {
            try {
                sock.close();
            } catch (Exception ex) {
            }
            ;
            throw e;
        }
    }

    public static byte[] receiveBytes(Socket s) {
        byte[] data = null;
        try {
            LOGGER.trace("opening input stream");
            s.setSoTimeout(0);
            java.io.DataInputStream abc = new java.io.DataInputStream(s.getInputStream());
            String line = "";
            byte[] c = new byte[1];
            int contentLength = 0, idx;
            String contentType = "", header, content;
            while (true) {
                c[0] = abc.readByte();
                line += new String(c, "ascii");
                if (line.endsWith("\r\n")) {
                    if (line.charAt(0) == ' ' || line.charAt(0) == '\t') {
                        line = "";
                        break;
                    }
                    LOGGER.trace("> " + line);
                    if ((idx = line.indexOf(":")) != -1) {
                        header = line.substring(0, idx).toLowerCase();
                        content = line.substring(idx + 1).trim();
                        if (header.equals("content-length")) {
                            contentLength = Integer.parseInt(content);
                        } else if (header.equals("content-type")) {
                            contentType = content;
                        }
                    } else if (line.equals("\r\n")) break;
                    line = "";
                }
            }
            ByteArrayOutputStream byte_stream = new ByteArrayOutputStream();
            byte[] tmpBuffer = new byte[2048];
            int count = 0;
            int numBytesRead = 0;
            LOGGER.trace("going to read something");
            while ((count = abc.read(tmpBuffer)) != -1) {
                byte_stream.write(tmpBuffer, 0, count);
                numBytesRead += count;
                if (LOGGER.isTraceEnabled()) LOGGER.trace("read = " + numBytesRead);
                if (numBytesRead == contentLength) break;
            }
            data = byte_stream.toByteArray();
            if (contentType.contains("urlencoded")) {
                LOGGER.trace("decoding urlencoded data");
                data = URLDecoder.decode(new String(data, "UTF-8"), "UTF-8").getBytes("UTF-8");
            }
        } catch (IOException e) {
            LOGGER.error("IOError while receiving bytes", e);
        } catch (Exception e) {
            LOGGER.error("Unknown Error while receiving bytes", e);
        } finally {
            return data;
        }
    }

    public static byte[] createMSGR(byte[] S2, byte[] signS2) throws Exception {
        int nS2 = S2.length;
        int nsignS2 = signS2.length;
        if ((nS2 > 65535) || (nsignS2 > 65535)) throw new Exception("something toobig");
        byte[] msg = new byte[4 + nS2 + nsignS2];
        int lo = nS2 % 256;
        int hi = (nS2 - lo) / 256;
        msg[0] = (byte) lo;
        msg[1] = (byte) hi;
        int offset = 2;
        for (int i = 0; i < nS2; i++) msg[offset + i] = S2[i];
        offset += nS2;
        lo = nsignS2 % 256;
        hi = (nsignS2 - lo) / 256;
        msg[offset] = (byte) lo;
        msg[offset + 1] = (byte) hi;
        offset += 2;
        for (int i = 0; i < nsignS2; i++) msg[offset + i] = signS2[i];
        offset += nsignS2;
        return msg;
    }

    public static byte[] getS1RFromMSGR(byte[] msg) {
        int lo = msg[0];
        if (lo < 0) lo += 256;
        int hi = msg[1];
        if (hi < 0) hi += 256;
        int nS1R = lo + hi * 256;
        byte[] S1R = new byte[nS1R];
        for (int i = 0; i < nS1R; i++) {
            S1R[i] = msg[2 + i];
        }
        return S1R;
    }

    public static byte[] getSignS1RFromMSGR(byte[] msg) {
        int lo = msg[0];
        if (lo < 0) lo += 256;
        int hi = msg[1];
        if (hi < 0) hi += 256;
        int nS1R = lo + hi * 256;
        int offset = 2 + nS1R;
        lo = msg[offset];
        if (lo < 0) lo += 256;
        hi = msg[offset + 1];
        if (hi < 0) hi += 256;
        int nsignS1R = lo + hi * 256;
        offset += 2;
        byte[] signS1R = new byte[nsignS1R];
        for (int i = 0; i < nsignS1R; i++) {
            signS1R[i] = msg[offset + i];
        }
        return signS1R;
    }

    public static byte[] createMSGT2(byte[] S1, byte[] T2) throws Exception {
        int nS1 = S1.length;
        int nT2 = T2.length;
        if ((nS1 > 65535) || (nT2 > 65535)) throw new Exception("something toobig");
        byte[] msg = new byte[4 + nS1 + nT2];
        int lo = nS1 % 256;
        int hi = (nS1 - lo) / 256;
        msg[0] = (byte) lo;
        msg[1] = (byte) hi;
        int offset = 2;
        for (int i = 0; i < nS1; i++) msg[offset + i] = S1[i];
        offset += nS1;
        lo = nT2 % 256;
        hi = (nT2 - lo) / 256;
        msg[offset] = (byte) lo;
        msg[offset + 1] = (byte) hi;
        offset += 2;
        for (int i = 0; i < nT2; i++) msg[offset + i] = T2[i];
        offset += nT2;
        return msg;
    }

    public static byte[] getS1FromMSGT2(byte[] msg) {
        int lo = msg[0];
        if (lo < 0) lo += 256;
        int hi = msg[1];
        if (hi < 0) hi += 256;
        int nS1 = lo + hi * 256;
        byte[] S1 = new byte[nS1];
        for (int i = 0; i < nS1; i++) {
            S1[i] = msg[2 + i];
        }
        return S1;
    }

    public static byte[] getT2FromMSGT2(byte[] msg) {
        int lo = msg[0];
        if (lo < 0) lo += 256;
        int hi = msg[1];
        if (hi < 0) hi += 256;
        int nS1 = lo + hi * 256;
        int offset = 2 + nS1;
        lo = msg[offset];
        if (lo < 0) lo += 256;
        hi = msg[offset + 1];
        if (hi < 0) hi += 256;
        int nT2 = lo + hi * 256;
        offset += 2;
        byte[] T2 = new byte[nT2];
        for (int i = 0; i < nT2; i++) {
            T2[i] = msg[offset + i];
        }
        return T2;
    }

    public static byte[] createMSGT1(byte[] S1, String IDVotazione) throws Exception {
        int nS1 = S1.length;
        byte[] IDBytes = IDVotazione.getBytes();
        int nIDBytes = IDBytes.length;
        if ((nS1 > 65535) || (nIDBytes > 65535)) throw new Exception("something toobig");
        byte[] msg = new byte[4 + nS1 + nIDBytes];
        int lo = nS1 % 256;
        int hi = (nS1 - lo) / 256;
        msg[0] = (byte) lo;
        msg[1] = (byte) hi;
        int offset = 2;
        for (int i = 0; i < nS1; i++) msg[offset + i] = S1[i];
        offset += nS1;
        lo = nIDBytes % 256;
        hi = (nIDBytes - lo) / 256;
        msg[offset] = (byte) lo;
        msg[offset + 1] = (byte) hi;
        offset += 2;
        for (int i = 0; i < nIDBytes; i++) msg[offset + i] = IDBytes[i];
        offset += nIDBytes;
        return msg;
    }

    public static byte[] getS1FromMSGT1(byte[] msg) {
        int lo = msg[0];
        if (lo < 0) lo += 256;
        int hi = msg[1];
        if (hi < 0) hi += 256;
        int nS1 = lo + hi * 256;
        byte[] S1 = new byte[nS1];
        for (int i = 0; i < nS1; i++) {
            S1[i] = msg[2 + i];
        }
        return S1;
    }

    public static String getIDVotazioneFromMSGT1(byte[] msg) {
        int lo = msg[0];
        if (lo < 0) lo += 256;
        int hi = msg[1];
        if (hi < 0) hi += 256;
        int nS1 = lo + hi * 256;
        int offset = 2 + nS1;
        lo = msg[offset];
        if (lo < 0) lo += 256;
        hi = msg[offset + 1];
        if (hi < 0) hi += 256;
        int nIDBytes = lo + hi * 256;
        offset += 2;
        byte[] IDBytes = new byte[nIDBytes];
        for (int i = 0; i < nIDBytes; i++) {
            IDBytes[i] = msg[offset + i];
        }
        String IDVotazione = "";
        for (int j = 0; j < nIDBytes; j++) IDVotazione += (char) IDBytes[j];
        return IDVotazione;
    }

    public static byte[] createMsg(byte[] S1, byte[] S2) throws Exception {
        int nS1 = S1.length;
        int nS2 = S2.length;
        if ((nS1 > 65535) || (nS2 > 65535)) throw new Exception("Something too big");
        byte[] msg = new byte[4 + nS1 + nS2];
        int lo = nS1 % 256;
        int hi = (nS1 - lo) / 256;
        msg[0] = (byte) lo;
        msg[1] = (byte) hi;
        int offset = 2;
        for (int i = 0; i < nS1; i++) msg[offset + i] = S1[i];
        offset += nS1;
        lo = nS2 % 256;
        hi = (nS2 - lo) / 256;
        msg[offset] = (byte) lo;
        msg[offset + 1] = (byte) hi;
        offset += 2;
        for (int i = 0; i < nS2; i++) msg[offset + i] = S2[i];
        return msg;
    }

    public static byte[] createMsg(byte[] S1, byte[] S2, byte[] S3) throws Exception {
        int nS1 = S1.length;
        int nS2 = S2.length;
        int nS3 = S3.length;
        if ((nS1 > 65535) || (nS2 > 65535) || (nS3 > 65535)) throw new Exception("Something too big");
        byte[] msg = new byte[6 + nS1 + nS2 + nS3];
        int lo = nS1 % 256;
        int hi = (nS1 - lo) / 256;
        msg[0] = (byte) lo;
        msg[1] = (byte) hi;
        int offset = 2;
        for (int i = 0; i < nS1; i++) msg[offset + i] = S1[i];
        offset += nS1;
        lo = nS2 % 256;
        hi = (nS2 - lo) / 256;
        msg[offset] = (byte) lo;
        msg[offset + 1] = (byte) hi;
        offset += 2;
        for (int i = 0; i < nS2; i++) msg[offset + i] = S2[i];
        offset += nS2;
        lo = nS3 % 256;
        hi = (nS3 - lo) / 256;
        msg[offset] = (byte) lo;
        msg[offset + 1] = (byte) hi;
        offset += 2;
        for (int i = 0; i < nS3; i++) msg[offset + i] = S3[i];
        return msg;
    }

    public static byte[] createMsg(byte[] S1, byte[] S2, byte[] S3, byte[] S4) throws Exception {
        int nS1 = S1.length;
        int nS2 = S2.length;
        int nS3 = S3.length;
        int nS4 = S4.length;
        if ((nS1 > 65535) || (nS2 > 65535) || (nS3 > 65535) || (nS4 > 65535)) throw new Exception("Something too big");
        byte[] msg = new byte[8 + nS1 + nS2 + nS3 + nS4];
        int lo = nS1 % 256;
        int hi = (nS1 - lo) / 256;
        msg[0] = (byte) lo;
        msg[1] = (byte) hi;
        int offset = 2;
        for (int i = 0; i < nS1; i++) msg[offset + i] = S1[i];
        offset += nS1;
        lo = nS2 % 256;
        hi = (nS2 - lo) / 256;
        msg[offset] = (byte) lo;
        msg[offset + 1] = (byte) hi;
        offset += 2;
        for (int i = 0; i < nS2; i++) msg[offset + i] = S2[i];
        offset += nS2;
        lo = nS3 % 256;
        hi = (nS3 - lo) / 256;
        msg[offset] = (byte) lo;
        msg[offset + 1] = (byte) hi;
        offset += 2;
        for (int i = 0; i < nS3; i++) msg[offset + i] = S3[i];
        offset += nS3;
        lo = nS4 % 256;
        hi = (nS4 - lo) / 256;
        msg[offset] = (byte) lo;
        msg[offset + 1] = (byte) hi;
        offset += 2;
        for (int i = 0; i < nS4; i++) msg[offset + i] = S4[i];
        return msg;
    }

    public static byte[] getS1FromMsg(byte[] msg) {
        int lo = msg[0];
        if (lo < 0) lo += 256;
        int hi = msg[1];
        if (hi < 0) hi += 256;
        int nS1 = lo + hi * 256;
        byte[] S1 = new byte[nS1];
        for (int i = 0; i < nS1; i++) {
            S1[i] = msg[2 + i];
        }
        return S1;
    }

    public static byte[] getS2FromMsg(byte[] msg) {
        int lo = msg[0];
        if (lo < 0) lo += 256;
        int hi = msg[1];
        if (hi < 0) hi += 256;
        int nS1 = lo + hi * 256;
        int offset = 2 + nS1;
        lo = msg[offset];
        if (lo < 0) lo += 256;
        hi = msg[offset + 1];
        if (hi < 0) hi += 256;
        int nS2 = lo + hi * 256;
        offset += 2;
        byte[] S2 = new byte[nS2];
        for (int i = 0; i < nS2; i++) {
            S2[i] = msg[offset + i];
        }
        return S2;
    }

    public static byte[] getS3FromMsg(byte[] msg) {
        int lo = msg[0];
        if (lo < 0) lo += 256;
        int hi = msg[1];
        if (hi < 0) hi += 256;
        int nS1 = lo + hi * 256;
        int offset = 2 + nS1;
        lo = msg[offset];
        if (lo < 0) lo += 256;
        hi = msg[offset + 1];
        if (hi < 0) hi += 256;
        int nS2 = lo + hi * 256;
        offset += 2 + nS2;
        lo = msg[offset];
        if (lo < 0) lo += 256;
        hi = msg[offset + 1];
        if (hi < 0) hi += 256;
        int nS3 = lo + hi * 256;
        offset += 2;
        byte[] S3 = new byte[nS3];
        for (int i = 0; i < nS3; i++) {
            S3[i] = msg[offset + i];
        }
        return S3;
    }

    public static byte[] getS4FromMsg(byte[] msg) {
        int lo = msg[0];
        if (lo < 0) lo += 256;
        int hi = msg[1];
        if (hi < 0) hi += 256;
        int nS1 = lo + hi * 256;
        int offset = 2 + nS1;
        lo = msg[offset];
        if (lo < 0) lo += 256;
        hi = msg[offset + 1];
        if (hi < 0) hi += 256;
        int nS2 = lo + hi * 256;
        offset += 2 + nS2;
        lo = msg[offset];
        if (lo < 0) lo += 256;
        hi = msg[offset + 1];
        if (hi < 0) hi += 256;
        int nS3 = lo + hi * 256;
        offset += 2 + nS3;
        lo = msg[offset];
        if (lo < 0) lo += 256;
        hi = msg[offset + 1];
        if (hi < 0) hi += 256;
        int nS4 = lo + hi * 256;
        offset += 2;
        byte[] S4 = new byte[nS4];
        for (int i = 0; i < nS4; i++) {
            S4[i] = msg[offset + i];
        }
        return S4;
    }

    public static PrivateKey getKey(KeyStoreData ksd) {
        return Crypto.loadKey(ksd.keystore, ksd.keystorepasswd, ksd.keyalias, ksd.keypasswd);
    }

    public static Certificate getCert(KeyStoreData ksd) {
        return Crypto.loadCert(ksd.keystore, ksd.keystorepasswd, ksd.certalias);
    }

    public static String sendRawData(String urlServer, String adata) {
        try {
            URL url = new URL(urlServer);
            String data = URLEncoder.encode("data", "UTF-8") + "=" + URLEncoder.encode(adata, "UTF-8");
            if (LOGGER.isTraceEnabled()) LOGGER.trace("Sending raw data to " + url.getHost() + ":" + url.getPort());
            InetAddress addr = InetAddress.getByName(url.getHost());
            Socket socket = null;
            if (url.getPort() == -1) socket = new Socket(addr, 80); else socket = new Socket(addr, url.getPort());
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            wr.write("POST " + url.getPath() + " HTTP/1.0\r\n");
            wr.write("Host: " + url.getHost() + "\r\n");
            wr.write("Content-Length: " + data.length() + "\r\n");
            wr.write("Content-Type: application/x-www-form-urlencoded\r\n");
            wr.write("\r\n");
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = "";
            String line;
            boolean fltext = false;
            while ((line = rd.readLine()) != null) {
                if (fltext) response += line; else {
                    if (line.startsWith("Content-Type:")) {
                        if ((line = rd.readLine()) == null) break;
                        fltext = true;
                    }
                }
            }
            wr.close();
            rd.close();
            return response;
        } catch (IOException e) {
            return "FAIL";
        }
    }
}
