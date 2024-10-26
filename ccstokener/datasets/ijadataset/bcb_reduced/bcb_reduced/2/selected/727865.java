package cz.darmovzalt.gps;

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;
import cz.darmovzalt.comm.*;

public abstract class Nmea {

    protected InputStream in;

    protected OutputStream out;

    protected Map standardListeners = new HashMap();

    protected Map proprietaryListeners = new HashMap();

    protected NmeaErrorListener errorListener;

    protected int lastSentCheckSum, lastRecvCheckSum;

    public boolean debug = false;

    public Nmea() {
    }

    public Nmea(InputStream in, OutputStream out) {
        this.setInputStream(in);
        this.setOutputStream(out);
    }

    public void setInputStream(InputStream in) {
        this.in = in;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public void open(String openStr) throws IOException {
        String commProtocol = "comm:";
        String rxtxProtocol = "rxtx:";
        String netProtocol = "net:";
        if (openStr.startsWith(commProtocol)) {
            CommConnection commConnection = CommConnection.open(openStr.substring(commProtocol.length()));
            this.setInputStream(commConnection.getInputStream());
            this.setOutputStream(commConnection.getOutputStream());
        } else if (openStr.startsWith(rxtxProtocol)) {
            RXTXConnection rxtxConnection = RXTXConnection.open(openStr.substring(commProtocol.length()));
            this.setInputStream(rxtxConnection.getInputStream());
            this.setOutputStream(rxtxConnection.getOutputStream());
        } else if (openStr.startsWith(netProtocol)) {
            SocketConnection socketConn = SocketConnection.open(openStr.substring(netProtocol.length()));
            this.setInputStream(socketConn.getInputStream());
            this.setOutputStream(socketConn.getOutputStream());
        } else {
            URL url = new URL(openStr);
            URLConnection urlConn = url.openConnection();
            this.setInputStream(urlConn.getInputStream());
            try {
                this.setOutputStream(urlConn.getOutputStream());
            } catch (UnknownServiceException e) {
            }
        }
    }

    public void setNmeaStandardListener(String talker, String id, NmeaStandardListener listener) {
        this.standardListeners.put(talker + "_" + id, listener);
    }

    public NmeaStandardListener getNmeaStandardListener(String talker, String id) {
        return (NmeaStandardListener) this.standardListeners.get(talker + "_" + id);
    }

    public void setNmeaProprietaryListener(String id, NmeaProprietaryListener listener) {
        this.proprietaryListeners.put(id, listener);
    }

    public NmeaProprietaryListener getNmeaProprietaryListener(String id) {
        return (NmeaProprietaryListener) this.proprietaryListeners.get(id);
    }

    public void setNmeaErrorListener(NmeaErrorListener listener) {
        this.errorListener = listener;
    }

    public NmeaErrorListener getNmeaErrorListener() {
        return this.errorListener;
    }

    public void process() throws IOException, GpsException {
        BufferedReader br = new BufferedReader(new InputStreamReader(this.in, "ASCII"));
        String line;
        for (; ; ) {
            try {
                line = br.readLine();
            } catch (IOException ioe) {
                line = "";
            }
            if (line == null) break;
            line = line.trim();
            if (line.length() == 0) continue;
            try {
                if (this.debug) System.out.println("< " + line);
                this.processSentence(line);
            } catch (GpsException e) {
                if (this.errorListener != null) {
                    this.errorListener.sentenceError(line, e);
                } else {
                    throw new GpsException("Error while parsing line \"" + line + "\"", e);
                }
            }
        }
    }

    public void processSentence(String sentence) throws GpsException {
        if (!sentence.startsWith("$")) throw new GpsException("Starting dollar ($) sign missing");
        sentence = sentence.substring(1);
        int asterisk = sentence.lastIndexOf('*');
        if (asterisk != -1) {
            if (asterisk >= sentence.length() - 3) {
                String checksumStr = sentence.substring(asterisk + 1);
                int provchsum = Integer.parseInt(checksumStr, 16);
                sentence = sentence.substring(0, asterisk);
                int countchsum = this.checkSum(sentence);
                if (provchsum != countchsum) throw new GpsException("Wrong checksum (" + countchsum + " != " + provchsum + ")");
                this.lastRecvCheckSum = provchsum;
            }
        }
        List words = this.split(sentence, ',');
        String id = (String) words.get(0);
        if (id.startsWith("P")) {
            String sentenceId = id.substring(1);
            String[] params = new String[words.size() - 1];
            for (int i = 0; i < params.length; i++) params[i] = (String) words.get(i + 1);
            this.processProprietarySentence(sentenceId, params);
        } else {
            if (id.length() != 5) throw new GpsException("Wrong sentence ID: " + id);
            String talker = id.substring(0, 2);
            String sentenceId = id.substring(2);
            String[] params = new String[words.size() - 1];
            for (int i = 0; i < params.length; i++) params[i] = (String) words.get(i + 1);
            this.processStandardSentence(talker, sentenceId, params);
        }
    }

    public void processStandardSentence(String talker, String sentenceId, String[] params) throws GpsException {
        NmeaStandardListener listener = this.getNmeaStandardListener(talker, sentenceId);
        if (listener != null) listener.standardSentenceReceived(params);
    }

    public void processProprietarySentence(String sentenceId, String[] params) throws GpsException {
        NmeaProprietaryListener listener = this.getNmeaProprietaryListener(sentenceId);
        if (listener != null) listener.proprietarySentenceReceived(params);
    }

    public void querySentence(String talker, String listener, String sentenceId) throws IOException {
        this.query(talker + listener + "Q", new String[] { sentenceId });
    }

    public void query(String command, String[] params) throws IOException {
        String content = command + ((params != null) ? "," + this.join(params, ',') : "");
        int chsum = this.checkSum(content);
        String sentence = "$" + content + "*" + this.formatCheckSum(chsum);
        if (this.debug) System.out.println("> " + sentence);
        this.out.write(sentence.getBytes("ASCII"));
        this.out.write('\r');
        this.out.write('\n');
        this.out.flush();
        this.lastSentCheckSum = chsum;
    }

    protected int checkSum(String data) {
        int ret = 0;
        for (int i = 0; i < data.length(); i++) ret ^= data.charAt(i);
        return ret;
    }

    protected String formatCheckSum(int checksum) {
        String digits = "0123456789ABCDEF";
        int hi = checksum / 16;
        int lo = checksum % 16;
        return "" + digits.charAt(hi) + digits.charAt(lo);
    }

    protected List split(String str, char sep) {
        List ret = new ArrayList();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == sep) {
                ret.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        ret.add(sb.toString());
        return ret;
    }

    protected String join(String[] words, char sep) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) sb.append(sep);
            sb.append(words[i]);
        }
        return sb.toString();
    }

    protected void checkParamCount(int real, int expected) throws GpsException {
        if (real != expected) throw new GpsException("Sentence parameter count differs from expected: " + real + " (expected " + expected + ")");
    }
}
