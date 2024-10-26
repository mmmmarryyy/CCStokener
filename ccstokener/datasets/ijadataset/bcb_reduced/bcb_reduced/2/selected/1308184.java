package org.dreamhost.ide.connector;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author Michal Mocnak
 */
public class DreamHostConnector {

    private static DreamHostConnector instance = null;

    private static final String API = "https://api.dreamhost.com/";

    private final DocumentBuilder builder;

    private DreamHostConnector() throws ParserConfigurationException {
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        String u = NbPreferences.forModule(DreamHostConnector.class).get("username", null);
        String k = NbPreferences.forModule(DreamHostConnector.class).get("key", null);
        if (null != u && null != k) {
            login(u, k);
        }
    }

    public static synchronized DreamHostConnector getInstance() {
        if (null == instance) {
            try {
                instance = new DreamHostConnector();
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(DreamHostConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return instance;
    }

    private String username;

    private String key;

    private boolean logged = false;

    private boolean working = false;

    public boolean isLogged() {
        return logged;
    }

    public boolean isWorking() {
        return working;
    }

    public String getUsername() {
        return username;
    }

    private String getKey() {
        return key;
    }

    public void login(String username, String key) {
        if (isLogged()) {
            return;
        }
        if (null == this.username || null == this.key) {
            this.username = username;
            this.key = key;
        }
        final ProgressHandle handle = ProgressHandleFactory.createHandle("Logining into DreamHost");
        handle.start();
        working = true;
        fireChangeEvent();
        RequestProcessor.getDefault().post(new Runnable() {

            public void run() {
                try {
                    HttpsURLConnection connection = (HttpsURLConnection) urlGenerator(DreamHostCommands.CMD_DOMAIN_LIST_DOMAINS, null).openConnection();
                    String response = getResponse(connection);
                    Document document = builder.parse(new ByteArrayInputStream(response.getBytes()));
                    String result = document.getElementsByTagName("result").item(0).getTextContent();
                    logged = result.equals("success");
                } catch (SAXException ex) {
                    Logger.getLogger(DreamHostConnector.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(DreamHostConnector.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    if (isLogged()) {
                        NbPreferences.forModule(DreamHostConnector.class).put("username", getUsername());
                        NbPreferences.forModule(DreamHostConnector.class).put("key", getKey());
                    }
                    handle.finish();
                    working = false;
                    fireChangeEvent();
                }
            }
        });
    }

    public synchronized String submitCommand(String command, String params) {
        if (!isLogged()) {
            return null;
        }
        URLConnection connection = null;
        try {
            connection = urlGenerator(command, params).openConnection();
        } catch (IOException ex) {
            Logger.getLogger(DreamHostConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (null == connection) {
            return null;
        }
        return getResponse(connection);
    }

    private URL urlGenerator(String cmd, String params) {
        try {
            return new URL(API + "?username=" + username + "&key=" + key + "&cmd=" + cmd + "&unique_id=" + new Random().nextLong() + "&format=xml" + ((null == params) ? "" : params));
        } catch (MalformedURLException ex) {
            Logger.getLogger(DreamHostConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private String getResponse(URLConnection connection) {
        InputStream ins = null;
        try {
            ins = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(ins);
            BufferedReader in = new BufferedReader(isr);
            String result = "<dreamhost>";
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                result += inputLine;
            }
            result += "</dreamhost>";
            return result;
        } catch (IOException ex) {
            Logger.getLogger(DreamHostConnector.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                ins.close();
            } catch (IOException ex) {
                Logger.getLogger(DreamHostConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();

    public void addChangeListener(ChangeListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeChangeListener(ChangeListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private synchronized void fireChangeEvent() {
        Iterator it;
        synchronized (listeners) {
            it = new HashSet(listeners).iterator();
        }
        while (it.hasNext()) {
            ((ChangeListener) it.next()).stateChanged(new ChangeEvent(this));
        }
    }
}
