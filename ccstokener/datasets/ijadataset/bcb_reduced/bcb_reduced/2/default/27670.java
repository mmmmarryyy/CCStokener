import javax.swing.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.*;

class Base64Encoder {

    public String encode(String data) {
        return (getString(encode(getBinaryBytes(data))));
    }

    private byte[] encode(byte[] data) {
        int c;
        int len = data.length;
        StringBuffer ret = new StringBuffer(((len / 3) + 1) * 4);
        for (int i = 0; i < len; ++i) {
            c = (data[i] >> 2) & 0x3f;
            ret.append(cvt.charAt(c));
            c = (data[i] << 4) & 0x3f;
            if (++i < len) c |= (data[i] >> 4) & 0x0f;
            ret.append(cvt.charAt(c));
            if (i < len) {
                c = (data[i] << 2) & 0x3f;
                if (++i < len) c |= (data[i] >> 6) & 0x03;
                ret.append(cvt.charAt(c));
            } else {
                ++i;
                ret.append((char) fillchar);
            }
            if (i < len) {
                c = data[i] & 0x3f;
                ret.append(cvt.charAt(c));
            } else {
                ret.append((char) fillchar);
            }
        }
        return (getBinaryBytes(ret.toString()));
    }

    private String getString(byte[] arr) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < arr.length; ++i) buf.append((char) arr[i]);
        return (buf.toString());
    }

    private byte[] getBinaryBytes(String str) {
        byte[] b = new byte[str.length()];
        for (int i = 0; i < b.length; ++i) b[i] = (byte) str.charAt(i);
        return (b);
    }

    private final int fillchar = '=';

    private final String cvt = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789+/";
}

public class ConnectionHandler extends javax.swing.JApplet {

    private MainJApplet mainClass;

    private String defaultUrl = "http://localhost:8080/panoptes";

    private String extUrl;

    private HttpURLConnection conn = null;

    private String usr;

    private String psw;

    private OutputStream os = null;

    private InputStream is = null;

    private boolean reqType = true;

    public InputStream getIs() {
        return is;
    }

    public ConnectionHandler(boolean method, MainJApplet reference, String eUrl) {
        mainClass = reference;
        reqType = method;
        usr = mainClass.getUsr();
        psw = mainClass.getPsw();
        extUrl = eUrl;
    }

    public void httpConnection(String toSend) {
        try {
            URL url = new URL(defaultUrl + extUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            if (reqType) conn.setRequestMethod("POST"); else conn.setRequestMethod("GET");
            String userPassword = usr + ":" + psw;
            Base64Encoder be = new Base64Encoder();
            String encoding = be.encode(userPassword);
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("Authorization", "Basic " + encoding);
            if (reqType) {
                os = conn.getOutputStream();
                if (toSend != "") {
                    byte data[];
                    data = (toSend).getBytes();
                    os.write(data);
                }
            }
            is = conn.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String processServerResponse() {
        try {
            String str;
            int rc = conn.getResponseCode();
            if (rc == HttpURLConnection.HTTP_OK) {
                int length = conn.getContentLength();
                if (length != -1) {
                    byte servletData[] = new byte[length];
                    is.read(servletData);
                    str = new String(servletData);
                } else {
                    ByteArrayOutputStream bStrm = new ByteArrayOutputStream();
                    int ch;
                    while ((ch = is.read()) != -1) bStrm.write(ch);
                    str = new String(bStrm.toByteArray());
                    bStrm.close();
                }
                return str;
            } else throw new IOException("HTTP response code: " + rc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
