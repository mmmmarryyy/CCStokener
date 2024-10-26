package au.edu.usyd.cs.rlog;

import java.math.*;
import java.util.*;
import java.io.*;
import java.net.*;
import netscape.javascript.JSObject;
import netscape.javascript.JSException;

public class rlogClient {

    private long lastTime;

    private String logUrl;

    private StringBuffer buffy;

    private boolean saved = false;

    private static final float version = 2.1f;

    public rlogClient(String url) {
        logUrl = url;
        lastTime = (new Date()).getTime();
        buffy = new StringBuffer();
        buffy.append("#log format v " + version + "\n");
        saved = false;
    }

    public void log(String m) {
        long t = (new Date()).getTime();
        m = t + "|||" + (int) (t - lastTime) + "|||" + m;
        lastTime = t;
        buffy.append(m + "\n");
    }

    public static void main(String args[]) throws Exception {
        String name = "http://www.gmp.usyd.edu.au/servlets/rlogServlet";
        rlogClient r = new rlogClient(name);
        r.log("a test");
        Thread.sleep(1000);
        r.log("another test");
        r.save();
        Thread.sleep(1000);
        r.log("further another test");
        r.save();
    }

    protected void finalize() {
        save();
    }

    public void save() {
        try {
            doSave();
        } catch (Exception e) {
            emit("error in save: " + e.toString());
        }
    }

    public void save(JSObject thisWin) {
        try {
            doSave();
        } catch (Exception e) {
            thisWin.call("writePage", new String[] { "topicframe", "<pre>" + buffy.toString() + "</pre>" });
        }
    }

    private void doSave() throws Exception {
        this.log("--rlog save--");
        PrintWriter printout;
        URLConnection urlConn;
        URL uurl = new URL(logUrl);
        urlConn = uurl.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "text/plain");
        printout = new PrintWriter(urlConn.getOutputStream());
        if (saved) {
            buffy.insert(0, "#Previously saved!\n");
        }
        printout.write(buffy.toString());
        DataInputStream input;
        printout.flush();
        printout.close();
        input = new DataInputStream(urlConn.getInputStream());
        BufferedReader i = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
        String str;
        while (null != ((str = i.readLine()))) {
            emit(str);
        }
        input.close();
        saved = true;
    }

    public void emit(String s) {
        System.out.println(s);
    }
}
