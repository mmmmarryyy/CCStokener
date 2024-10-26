package ch.nostromo.lib.util;

import java.io.*;
import java.net.*;
import java.awt.event.*;
import java.util.*;

/** Thread for HTTP requests. Fires ActionEvents while reading. */
public class NosHTTPThread extends Thread {

    /** Constants with Thread State (Run / Suspended / Stopped) */
    static final int RUN = 0;

    static final int SUSPEND = 1;

    static final int STOP = 2;

    public static final int ACTION_EVENT_LINE = 0;

    public static final int ACTION_EVENT_FINISHED = 1;

    public static final int ACTION_EVENT_ERROR = 3;

    private String url = null;

    private transient Vector<ActionListener> actionListeners;

    private boolean atOnce = false;

    private StringBuffer buffer = new StringBuffer();

    private String proxyHost = "";

    private String proxyPort = "";

    public NosHTTPThread(String url, boolean atOnce, String proxyHost, String proxyPort) {
        this.url = url;
        this.atOnce = atOnce;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    public synchronized void setState(int s) {
        if (s == RUN) notify();
    }

    public void run() {
        getURLContent();
    }

    private void getURLContent() {
        try {
            URL url = new URL(this.url);
            Properties props = System.getProperties();
            props.put("http.proxyHost", this.proxyHost);
            props.put("http.proxyPort", this.proxyPort);
            URLConnection connection = url.openConnection();
            InputStream stream = connection.getInputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(stream));
            String line = "";
            while ((line = input.readLine()) != null) {
                if (atOnce) {
                    buffer.append(line + "\n");
                } else {
                    this.fireActionPerformed(new ActionEvent(this, ACTION_EVENT_LINE, line + "\n"));
                }
            }
            if (atOnce) this.fireActionPerformed(new ActionEvent(this, ACTION_EVENT_LINE, buffer.toString()));
        } catch (Exception e) {
            this.fireActionPerformed(new ActionEvent(this, ACTION_EVENT_ERROR, e.toString()));
        }
        this.fireActionPerformed(new ActionEvent(this, ACTION_EVENT_FINISHED, ""));
    }

    public synchronized void removeActionListener(ActionListener l) {
        if (actionListeners != null && actionListeners.contains(l)) {
            actionListeners.remove(l);
        }
    }

    public synchronized void addActionListener(ActionListener l) {
        if (actionListeners == null) {
            actionListeners = new Vector<ActionListener>();
        }
        if (!actionListeners.contains(l)) {
            actionListeners.addElement(l);
        }
    }

    protected void fireActionPerformed(ActionEvent e) {
        if (actionListeners != null) {
            Vector<ActionListener> listeners = actionListeners;
            int count = listeners.size();
            for (int i = 0; i < count; i++) {
                listeners.elementAt(i).actionPerformed(e);
            }
        }
    }
}
