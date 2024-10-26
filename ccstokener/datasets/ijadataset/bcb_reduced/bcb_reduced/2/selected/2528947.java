package com.patientis.framework.jarnal;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.lang.Math.*;
import java.lang.Number.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.net.*;

public class Jtool {

    public static float bWidth = .6f;

    public String type = "Medium";

    public float width = bWidth;

    public String color = "black";

    public boolean highlighter = false;

    public static Rectangle maxR(Rectangle r1, Rectangle r2, int pad) {
        int x = (int) r1.getX();
        if ((int) r2.getX() < x) x = (int) r2.getX();
        int y = (int) r1.getY();
        if ((int) r2.getY() < y) y = (int) r2.getY();
        int xx = (int) r1.getX() + r1.width;
        int xxx = (int) r2.getX() + r2.width;
        if (xxx > xx) xx = xxx;
        int yy = (int) r1.getY() + r1.height;
        int yyy = (int) r2.getY() + r2.height;
        if (yyy > yy) yy = yyy;
        return new Rectangle(x - pad, y - pad, xx - x + (2 * pad), yy - y + 2 * pad);
    }

    public static String getLine(String s, String n) {
        if (s == null) return null;
        int pos = s.indexOf(n);
        if (pos < 0) return null;
        s = s.substring(pos);
        pos = s.indexOf("=");
        if (pos < 0) return null;
        s = s.substring(pos + 1);
        pos = s.indexOf("\n");
        if (pos < 0) return null;
        s = s.substring(0, pos);
        s = s.trim();
        return s;
    }

    public static Jtool getTool(String y) {
        if (y == null) return null;
        Jtool jt = new Jtool();
        String z = getLine(y, "type");
        if (z == null) return null;
        jt.setWidth(z);
        z = getLine(y, "color");
        if (z == null) return null;
        jt.color = z;
        z = getLine(y, "highlighter");
        if (z == null) return null;
        if (z.equals("true")) jt.highlighter = true; else jt.highlighter = false;
        return jt;
    }

    public static String getOnlyEntry(String s, String n) {
        s = getEntry(s, n);
        if (s == null) return null;
        int pos = s.indexOf("\n");
        if (pos < 0) return null;
        return s.substring(pos + 1, s.length());
    }

    public static String getEntry(String s, String n) {
        int pos = s.indexOf(n);
        if (pos < 0) return null;
        s = s.substring(pos);
        pos = s.indexOf("\n\n");
        if (pos < 0) return null;
        s = s.substring(0, pos);
        return s + "\n";
    }

    public String getConf() {
        String s = "type=" + type + "\n";
        s = s + "color=" + color + "\n";
        s = s + "highlighter=" + highlighter;
        s = s + "\n";
        return s;
    }

    public float getWidth() {
        return width;
    }

    public float getHeavy() {
        return 2.0f * getBaseWidth();
    }

    public void setWidth(String type) {
        this.type = type;
        if (type.equals("Fine")) width = 0.60f * getBaseWidth();
        if (type.equals("Medium")) width = getBaseWidth();
        if (type.equals("Heavy")) width = 2.0f * getBaseWidth();
        if (type.equals("Fat")) width = 11.0f * getBaseWidth();
    }

    public float getBaseWidth() {
        return bWidth;
    }

    public Color getColor() {
        Color c = Color.black;
        if (color.equals("blue")) c = Color.blue;
        if (color.equals("green")) c = Color.green;
        if (color.equals("dark gray")) c = Color.darkGray;
        if (color.equals("gray")) c = Color.gray;
        if (color.equals("light gray")) c = Color.lightGray;
        if (color.equals("magenta")) c = Color.magenta;
        if (color.equals("orange")) c = Color.orange;
        if (color.equals("pink")) c = Color.pink;
        if (color.equals("red")) c = Color.red;
        if (color.equals("white")) c = Color.white;
        if (color.equals("yellow")) c = Color.yellow;
        return c;
    }

    public void copy(Jtool jt) {
        this.width = jt.width;
        this.color = jt.color;
    }

    public void fullCopy(Jtool jt) {
        copy(jt);
        this.type = jt.type;
        this.highlighter = jt.highlighter;
    }

    public String desc() {
        String pen = "pen";
        if (highlighter) pen = "highlighter";
        return type + " " + color + " " + pen;
    }
}

class JarnalSelection implements Transferable, ClipboardOwner {

    DataFlavor flavors[] = { DataFlavor.stringFlavor, new DataFlavor("text/svg", "SVG Text Data"), new DataFlavor("text/html", "HTML (HyperText Markup Language)") };

    private String data_plain, data_svg, data_html;

    public JarnalSelection(String data_plain, String data_svg, String data_html) {
        this.data_plain = data_plain;
        this.data_svg = data_svg;
        this.data_html = data_html;
    }

    public synchronized DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return (flavor.equals(flavors[0]) || flavor.equals(flavors[1]) || flavor.equals(flavors[2]));
    }

    public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.equals(flavors[0])) {
            return (Object) data_plain;
        } else if (flavor.equals(flavors[1])) {
            return (Object) data_svg;
        } else if (flavor.equals(flavors[2])) {
            return (Object) data_html;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}

class b64 {

    static int len;

    static int ng;

    static int ni;

    static int nout;

    static byte b[] = new byte[3];

    static byte ba[];

    static char c[] = new char[4];

    static char ca[];

    static int ml = 75;

    static int nl;

    static char[] ct = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray();

    static byte getB() {
        if (ni >= len) return (byte) 0;
        byte b = ba[ni];
        ni++;
        return b;
    }

    static void putC(char c) {
        if (nl >= ml) {
            nl = 0;
            ca[nout] = '\n';
            nout++;
        } else nl++;
        ca[nout] = c;
        nout++;
    }

    public static String encode(byte bax[]) {
        ba = bax;
        ni = 0;
        nout = 0;
        nl = 0;
        len = ba.length;
        if (len == 0) return null;
        ng = (len + 2) / 3;
        ca = new char[(4 * ng) + ((4 * ng) / ml)];
        for (int ii = 0; ii < ng; ii++) {
            for (int jj = 0; jj < 3; jj++) b[jj] = getB();
            c[0] = ct[(b[0] >>> 2) & 0x3f];
            c[1] = ct[((b[0] << 4) & 0x30) + ((b[1] >>> 4) & 0xf)];
            c[2] = ct[((b[1]) << 2 & 0x3c) + ((b[2] >>> 6) & 0x3)];
            c[3] = ct[b[2] & 0x3f];
            if (ni >= len) {
                int jj = (3 * ng) - len;
                for (int kk = 0; kk < jj; kk++) c[3 - kk] = ct[64];
            }
            for (int jj = 0; jj < 4; jj++) putC(c[jj]);
        }
        return new String(ca);
    }
}

class HtmlPost {

    public HtmlPost(String xserver, String xmessage, Jpages xjpages, Hashtable xht, String xconf, boolean xurlencoded) {
        urlencoded = xurlencoded;
        server = xserver;
        message = xmessage + "\n";
        jpages = xjpages;
        if (xht != null) {
            ht = xht;
            fname = (String) ht.get("$f");
            if (fname == null) {
                fname = "noname";
                ht.put("$f", fname);
            }
        }
        conf = xconf;
        boundary = "---------------------------";
        for (int ii = 0; ii < 3; ii++) boundary = boundary + Long.toString((new Random()).nextLong(), 36);
    }

    public boolean withBorders = false;

    private String server;

    private String message;

    private Jpages jpages;

    private String boundary;

    private HttpURLConnection conn;

    private OutputStream out;

    private Hashtable ht;

    private String conf;

    private String crlf = "\r\n";

    private String fname = "noname";

    private boolean urlencoded = false;

    public String serverMsg;

    public boolean netError = false;

    public static boolean checkURL(String s) {
        if (s.length() <= 7) return false;
        if (s.substring(0, 7).equals("http://")) return true;
        if (s.length() == 8) return false;
        if (s.substring(0, 8).equals("https://")) return true;
        return false;
    }

    private String urlencode(String s) {
        String ans = s;
        try {
            ans = URLEncoder.encode(s, "UTF-8");
        } catch (java.io.UnsupportedEncodingException uee) {
            System.err.println(uee);
        }
        return ans;
    }

    private void writeVar(String key, String val) {
        String prn = "";
        if (!urlencoded) {
            prn = "--" + boundary + crlf + "Content-Disposition: form-data; name=\"";
            prn = prn + key + "\"";
            prn = prn + crlf + crlf;
            prn = prn + val + crlf;
        } else prn = key + "=" + urlencode(val) + "&";
        try {
            out.write(prn.getBytes());
        } catch (IOException ex) {
        }
    }

    private void writeFile(String key, String fname, String op) {
        String prn = "";
        if (!urlencoded) {
            prn = "--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"";
            prn = prn + key + "\"; filename=\"";
            String xfname = fname;
            if (op.equals("$$snapshot")) xfname = "attach.jpg";
            prn = prn + xfname + "\"";
            prn = prn + crlf;
            prn = prn + "Content-Type: \"application/octet-stream\"";
            prn = prn + crlf + crlf;
            try {
                out.write(prn.getBytes());
                jpages.netWrite(out, op, conf, withBorders);
                out.write(crlf.getBytes());
            } catch (IOException ex) {
            }
            ;
        } else {
            prn = key + "=";
            ByteArrayOutputStream baost = new ByteArrayOutputStream();
            jpages.netWrite(baost, op, conf, withBorders);
            prn = prn + urlencode(b64.encode(baost.toByteArray())) + "&";
            try {
                out.write(prn.getBytes());
            } catch (IOException ex) {
            }
        }
    }

    private String parseline() {
        if (message.equals("")) return null;
        int pos = message.indexOf("\n");
        if (pos < 0) return null;
        String ans = message.substring(0, pos);
        ans = ans.trim();
        message = message.substring(pos + 1);
        return ans;
    }

    private void writeLine(String line) {
        if (line == null) {
            System.err.println("null line");
            return;
        }
        int pos = line.indexOf("=");
        if (pos < 0) return;
        String key = line.substring(0, pos);
        String val = line.substring(pos + 1);
        if (val.equals("")) val = "none";
        if (val.substring(0, 1).equals("$")) {
            if (val.equals("$$jarnal") || val.equals("$$snapshot")) writeFile(key, fname, val); else {
                String xval = (String) ht.get(val);
                if (xval != null) {
                    writeVar(key, xval);
                }
            }
        } else writeVar(key, val);
    }

    public byte[] pipeBytes() {
        byte ba[] = null;
        try {
            URL url = new URL(server);
            conn = (HttpURLConnection) url.openConnection();
            InputStream is = conn.getInputStream();
            ByteArrayOutputStream tout = new ByteArrayOutputStream();
            int nmax = 10000;
            byte b[] = new byte[nmax + 1];
            int nread = 0;
            while ((nread = is.read(b, 0, nmax)) >= 0) tout.write(b, 0, nread);
            ba = tout.toByteArray();
        } catch (Exception ex) {
            System.err.println(ex);
        }
        return ba;
    }

    public String pipe(String ext) {
        String nfile = null;
        try {
            URL url = new URL(server);
            conn = (HttpURLConnection) url.openConnection();
            InputStream is = conn.getInputStream();
            File tfile = File.createTempFile(Jarnal.jarnalTmp, ext);
            nfile = tfile.getPath();
            FileOutputStream tout = new FileOutputStream(tfile);
            int nmax = 10000;
            byte b[] = new byte[nmax + 1];
            int nread = 0;
            while ((nread = is.read(b, 0, nmax)) >= 0) tout.write(b, 0, nread);
        } catch (Exception ex) {
            System.err.println(ex);
        }
        return nfile;
    }

    public void post() {
        netError = false;
        try {
            URL url = new URL(server);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            if (!urlencoded) conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            out = new ByteArrayOutputStream();
            String line;
            while ((line = parseline()) != null) writeLine(line);
            if (!urlencoded) {
                String term = "--" + boundary + "--";
                out.write(term.getBytes());
                conn.setRequestProperty("Content-Length", "" + ((ByteArrayOutputStream) out).size());
            }
            OutputStream hout = conn.getOutputStream();
            ((ByteArrayOutputStream) out).writeTo(hout);
            out.close();
            hout.close();
            InputStream is = conn.getInputStream();
            int nmax = 100000;
            int nread;
            byte b[] = new byte[nmax];
            serverMsg = "";
            while ((nread = is.read(b)) >= 0) serverMsg = serverMsg + new String(b, 0, nread);
            System.out.print(serverMsg);
        } catch (Exception ex) {
            System.err.println(ex);
            netError = true;
        }
    }
}
