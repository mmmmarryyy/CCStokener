package com.prolix.editor.oics.get;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

public abstract class OICSGetData {

    public static final Namespace atomNS = Namespace.getNamespace("http://www.w3.org/2005/Atom");

    public static final Namespace metadataNS = Namespace.getNamespace("http://www.cenorm.be/xsd/SPI");

    public static final Namespace lomNS = Namespace.getNamespace("http://ltsc.ieee.org/xsd/LOM");

    private HttpClient client;

    public OICSGetData() {
    }

    private InputStream execute(String filter, String query) {
        client = new DefaultHttpClient();
        String url = getURL();
        String trenn = "?";
        if (filter != null) {
            url += trenn + "filter=" + filter;
            trenn = "&";
        }
        if (query != null) {
            url += trenn + "query=" + query;
        }
        HttpGet get = new HttpGet(url);
        System.out.println("get: " + url);
        try {
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            return entity.getContent();
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }
        return null;
    }

    private void close() {
        client.getConnectionManager().shutdown();
    }

    public String getText() {
        InputStream instream = execute(null, null);
        BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
        String ret = "";
        try {
            String data = reader.readLine();
            while (data != null) {
                ret += data + "\n";
                data = reader.readLine();
            }
            instream.close();
        } catch (IOException e) {
        }
        close();
        return ret;
    }

    public Document getDocument() {
        return getDocument(null, null);
    }

    public Document getDocument(String filter, String query) {
        SAXBuilder builder = new SAXBuilder();
        InputStream instream = execute(filter, query);
        Document doc = null;
        try {
            doc = builder.build(instream);
            instream.close();
        } catch (IOException e) {
        } catch (JDOMException e) {
        }
        return doc;
    }

    public Element getRootElement() {
        return getRootElement(null, null);
    }

    public Element getRootElement(String filter, String query) {
        return getDocument(filter, query).getRootElement();
    }

    public abstract String getURL();

    public Namespace getAtomNamespace() {
        return OICSGetData.atomNS;
    }
}
