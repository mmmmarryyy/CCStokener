package com.sts.webmeet.tests.client;

import com.sts.webmeet.client.*;
import com.sts.webmeet.common.*;
import com.sts.webmeet.content.common.*;
import com.sts.webmeet.applets.*;
import java.awt.*;
import java.net.*;
import java.io.*;

public class ThinletScriptUIHarness implements ScriptEditContext {

    public ThinletScriptUIHarness(Integer customerID) {
        this.customerID = customerID;
    }

    private static final String SCRIPT_URL = "http://127.0.0.1:8443/webmeet/script";

    private static final String[] ITEM_CLASSES = { "com.sts.webmeet.content.common.questions.ScriptItem" };

    public void scriptDone(MeetingScript script) {
        try {
            postObject(script, SCRIPT_URL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void allDone() {
        throw new RuntimeException("allDone not implemented");
    }

    public void setScriptUI(ScriptUI ui) {
        this.ui = ui;
    }

    public String getSessionID() {
        return "whatever";
    }

    public String[] getItemClassNames() {
        return ITEM_CLASSES;
    }

    public ScriptInfoList getScriptList() {
        ScriptInfoList scripts = null;
        try {
            URL url = new URL(SCRIPT_URL + "?customer=" + customerID);
            ObjectInputStream ois = new ObjectInputStream(url.openStream());
            scripts = (ScriptInfoList) ois.readObject();
            ois.close();
            System.out.println("got script list");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scripts;
    }

    public MeetingScript getScript(Integer id) {
        MeetingScript script = null;
        try {
            URL url = new URL(SCRIPT_URL + "?script=" + id);
            ObjectInputStream ois = new ObjectInputStream(url.openStream());
            script = (MeetingScript) ois.readObject();
            ois.close();
            System.out.println("got script");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return script;
    }

    private ScriptUI ui;

    private MeetingScript script;

    private int iTotal;

    private Integer customerID;

    public static void main(String[] args) throws Exception {
        Frame frame = new Frame("ThinletScriptPanel");
        frame.setLayout(new BorderLayout());
        ThinletScriptUI sui = new ThinletScriptUI();
        frame.add(sui, BorderLayout.CENTER);
        frame.pack();
        frame.show();
        frame.setBounds(100, 100, 640, 480);
        ThinletScriptUIHarness harness = new ThinletScriptUIHarness(new Integer(args[0]));
        harness.setScriptUI(sui);
        sui.setScriptEditContext(harness);
        sui.editScripts();
    }

    private void postObject(Object obj, String strURL) throws Exception {
        print("entering post object");
        URL url = new URL(strURL);
        URLConnection urlConn = url.openConnection();
        print("HttpNetworkMessageConnection.postObject:returned from url.openConnection()");
        urlConn.setUseCaches(false);
        urlConn.setDoOutput(true);
        ObjectOutputStream oos = new ObjectOutputStream(urlConn.getOutputStream());
        print("HttpNetworkMessageConnection.postObject:returned from urlConn.getOutputStream()");
        oos.writeObject(obj);
        print("HttpNetworkMessageConnection.postObject:returned from writeObject()");
        oos.flush();
        oos.close();
        InputStream is = urlConn.getInputStream();
        print("HttpNetworkMessageConnection.postObject:returned from getInputStream()");
        while (is.read() != -1) {
        }
        is.close();
        print("exiting postObject");
    }

    private static boolean bDebug = false;

    private static void print(String str) {
        if (bDebug) {
            System.out.println(str);
        }
    }
}
