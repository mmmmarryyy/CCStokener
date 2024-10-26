package org.akrogen.tkui.impl.core.objects;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.akrogen.core.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventException;
import org.w3c.dom.events.EventListener;
import org.w3c.objects.xmlhttprequest.XMLHttpRequest;

public class XMLHttpRequestImpl implements XMLHttpRequest {

    public static final String GET_METHOD = "GET";

    public static final String PUT_METHOD = "PUT";

    public static final String DELETE_METHOD = "DELETE";

    private String method;

    private String url;

    private boolean async;

    private String user;

    private String password;

    private String responseText;

    private Document responseXML;

    private short readyState;

    private int status = 0;

    private String statusText;

    private Map eventsMap;

    private Map headers = Collections.EMPTY_MAP;

    private static URL curLocation = null;

    static {
        try {
            curLocation = (new java.io.File("./")).toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnreadystatechange(EventListener eventListener) {
        addEventListener(readystatechange, eventListener, false);
    }

    public void addEventListener(String type, EventListener listener, boolean useCapture) {
        if (eventsMap == null) eventsMap = new HashMap();
        List events = (List) eventsMap.get(type);
        if (events == null) {
            events = new ArrayList();
            eventsMap.put(type, events);
        }
        events.add(listener);
    }

    public boolean dispatchEvent(Event evt) throws EventException {
        if (eventsMap == null) return false;
        String type = evt.getType();
        List events = (List) eventsMap.get(type);
        if (events == null) return false;
        for (Iterator iterator = events.iterator(); iterator.hasNext(); ) {
            EventListener eventListener = (EventListener) iterator.next();
            eventListener.handleEvent(evt);
        }
        return false;
    }

    public void removeEventListener(String type, EventListener listener, boolean useCapture) {
        if (eventsMap == null) return;
        List events = (List) eventsMap.get(type);
        if (events == null) {
            return;
        }
        events.remove(listener);
    }

    public void abort() {
    }

    public String getAllResponseHeaders() {
        return null;
    }

    public short getReadyState() {
        return readyState;
    }

    public String getResponseHeader(String header) {
        return null;
    }

    public String getResponseText() {
        return responseText;
    }

    public String getResponseXML() {
        return null;
    }

    public String getStatus() {
        return null;
    }

    public String getStatusText() {
        return null;
    }

    public void open(String method, String url, boolean async, String user, String password) {
        if (StringUtils.isEmpty(method)) method = "GET";
        this.method = method;
        this.url = url;
    }

    public void open(String method, String url, boolean async, String user) {
        open(method, url, async, user, null);
    }

    public void open(String method, String url, boolean async) {
        open(method, url, async, null);
    }

    public void open(String method, String url) {
        open(method, url, true);
    }

    public void send() {
        send((String) null);
    }

    public void send(Document data) {
        if (data == null) {
            send((String) null);
        } else {
            try {
                Transformer tx = TransformerFactory.newInstance().newTransformer();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                tx.transform(new DOMSource(data), new StreamResult(out));
                String s = out.toString();
                out.close();
                send(s);
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert DOM Document to String", e);
            }
        }
    }

    public void send(final String data) {
        if (this.async) {
            new Thread(new Runnable() {

                public void run() {
                    makeRequest(data);
                }
            }).start();
        } else {
            makeRequest(data);
        }
    }

    public void setRequestHeader(String header, String value) {
    }

    private void makeRequest(String data) {
        try {
            URL url = new java.net.URL(curLocation, this.url);
            if ("file".equals(url.getProtocol())) {
                if (PUT_METHOD.equals(method)) {
                    String text = "";
                    if (data != null) text = data.toString();
                    FileWriter out = new FileWriter(new java.io.File(new java.net.URI(url.toString())));
                    out.write(text, 0, text.length());
                    out.flush();
                    out.close();
                } else if (DELETE_METHOD.equals(method)) {
                    File file = new java.io.File(new java.net.URI(url.toString()));
                    file.delete();
                } else {
                    URLConnection connection = url.openConnection();
                    connection.connect();
                    handleResponse(connection);
                }
            } else {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                for (Iterator iterator = headers.entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    String property = (String) entry.getKey();
                    String value = (String) entry.getValue();
                    connection.addRequestProperty(property, value);
                }
                connection.connect();
                handleResponse(connection);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleResponse(URLConnection connection) throws IOException {
        this.readyState = 4;
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
            this.status = httpURLConnection.getResponseCode();
            this.statusText = httpURLConnection.getResponseMessage();
        }
        InputStreamReader stream = new java.io.InputStreamReader(connection.getInputStream());
        BufferedReader buffer = new java.io.BufferedReader(stream);
        String line = null;
        while ((line = buffer.readLine()) != null) this.responseText += line;
        buffer.close();
        this.responseXML = null;
        Event event = new EventImpl();
        event.initEvent(readystatechange, false, false);
        dispatchEvent(event);
    }
}
