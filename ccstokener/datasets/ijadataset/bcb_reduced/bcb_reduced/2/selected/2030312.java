package com.hp.hpl.MeetingMachine.webid;

import java.net.*;
import java.util.*;
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import com.hp.hpl.webid.client.*;
import com.hp.hpl.webid.resolver.*;
import java.net.*;
import com.hp.hpl.PropertyEvents.*;
import com.hp.hpl.PropertyEvents.EventHeapAddress.*;

public class WebIdPropertyEventHandler extends PropertyEventHandlerBase {

    public WebIdPropertyEventHandler(String eh_name, URL base_url, URL resolution_failure_url) throws MalformedEventHeapAddressException {
        super("WebIdPropertyEventHandler", eh_name);
        this.base = base_url;
        this.resolution_failure = resolution_failure_url;
    }

    public void regisiterForPropertyEvent() throws iwork.eheap2.EventHeapException {
        Properties template = new Properties();
        registerForPropertyEvent("ID");
    }

    public static void usage() {
        System.out.println("usage: WebIdPropertyEventHandler base_url resolution_failure_url");
        System.out.println("base_url is a prefix for the webid service, eg");
        System.out.println("http://webid.hpl.hp.com:5190/webid/ResolverServlet?wpid=JohnBarton&method=resolve&uri=");
        System.out.println("resolution_failure_url is put on the eventheap if resolution fails.");
    }

    URL base;

    URL resolution_failure;

    public void doPropertyEvent(Properties eventProperties) {
        String id = (String) eventProperties.get("ID");
        URL url = getURLfromID(id);
        System.out.println("Resolved id " + id + " to " + url);
        if (url == null) {
            url = resolution_failure;
        }
        eventProperties.put(PropertyEvent.EVENTTYPE, "browse");
        eventProperties.put("URL", url.toString());
        eventProperties.put("url", url.toString());
        try {
            eheap.putPropertyEvent(eventProperties);
        } catch (Exception e) {
            System.out.println("WebIdPropertyEventHandler.doPropertyEvent put event failed:" + e);
        }
    }

    public URL getURLfromID(String id) {
        URL u = null;
        if (PropertyEventHeap.debug) {
            PropertyEventHeap.log("Resolving " + id + " using proxy=" + System.getProperty("http.proxyHost") + ":" + System.getProperty("http.proxyPort"));
        }
        try {
            WebIDClient c = new WebIDClient(base.toString());
            BindingList bl = c.resolveToBindingList(id);
            if (bl == null) {
                return resolution_failure;
            }
            Binding b = (Binding) bl.elementAt(0);
            String us = b.getHref();
            u = new URL(us);
        } catch (Exception e) {
            if (PropertyEventHeap.debug) {
                PropertyEventHeap.log("Cannot get URL from id=" + id + " with proxy set at " + System.getProperty("http.proxyHost") + ":" + System.getProperty("http.proxyPort") + e);
            }
        } finally {
        }
        return u;
    }

    public URL rawGetURLfromWebID(String id) {
        try {
            System.out.println("Resolving id" + id);
            String resolve = "/webid/ResolverServlet?wpid=MeetingMachine&method=form&uri=" + id + "&href=_[text/url]";
            String resolver = "http://webid.hpl.hp.com:5190";
            URL url = new URL(resolve + resolver);
            URLConnection c = url.openConnection();
            c.setDoOutput(true);
            c.setDoInput(true);
            c.setUseCaches(false);
        } catch (Exception e) {
            if (PropertyEventHeap.debug) {
                PropertyEventHeap.log("rawGetURLfromWebID " + e);
            }
        }
        return null;
    }

    public static String getResourceFromURL(URL url, String acceptHeader) throws java.io.IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Accept", acceptHeader);
        urlConnection.setInstanceFollowRedirects(true);
        BufferedReader input = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String content = "";
        String line;
        while ((line = input.readLine()) != null) {
            content += line;
        }
        input.close();
        return content;
    }

    public static String DEFAULT_WEBID_NAMESPACE = "webid";

    public static String TAG_BINDINGS = DEFAULT_WEBID_NAMESPACE + ":bindings";

    public static String TAG_DESCRIPTION = DEFAULT_WEBID_NAMESPACE + ":description";

    public static String TAG_WEB_PAGE = DEFAULT_WEBID_NAMESPACE + ":webPage";

    public static void parseIntoBindinglist(String xmlString) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes()));
        NodeList nodeList = doc.getChildNodes();
        Node node = nodeList.item(0);
        if (node != null && node instanceof Element && node.getNodeName().equals(TAG_BINDINGS)) {
            nodeList = node.getChildNodes();
            for (int j = 0; j < nodeList.getLength(); j++) {
                Node cnode = nodeList.item(j);
                if (cnode instanceof Element) {
                    node = cnode.getFirstChild();
                    if (cnode.getNodeName().equals(TAG_DESCRIPTION)) {
                    } else if (cnode.getNodeName().equals(TAG_WEB_PAGE)) {
                    } else if (cnode.getNodeName().equals(DEFAULT_WEBID_NAMESPACE + ":binding")) {
                    } else {
                        if (PropertyEventHeap.debug) {
                            PropertyEventHeap.log("wiseWebIDResolver fails");
                        }
                    }
                }
            }
        } else {
            throw new Exception("BindingList: Malformed bindinglist XML " + xmlString);
        }
    }

    public String toString() {
        return "wiseWebIDResolver: webid client to retrive URL from IDs";
    }
}
