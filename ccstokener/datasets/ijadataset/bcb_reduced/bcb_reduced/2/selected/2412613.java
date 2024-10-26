package org.apache.ws.jaxme.js.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import org.apache.ws.jaxme.logging.Logger;
import org.apache.ws.jaxme.logging.LoggerAccess;

/** <p>Basic implementation of a link checker for the JaxMe
 * HTML distribution.</p>
 *
 * @author <a href="mailto:joe@ispsoft.de">Jochen Wiedmann</a>
 */
public class LinkChecker {

    Logger logger = LoggerAccess.getLogger(LinkChecker.class);

    private Map urls = new HashMap();

    private Map checkedUrls = new HashMap();

    private int severity = Event.ERROR;

    private String proxyHost;

    private String proxyPort;

    private boolean haveErrors;

    private class Event {

        public static final int SUCCESS = 0;

        public static final int WARNING = 1;

        public static final int ERROR = 2;

        int mySeverity;

        private URL url;

        private int pos = -1;

        private String msg;

        private Event(int pSeverity, URL pURL, int pPos, String pMsg) {
            mySeverity = pSeverity;
            url = pURL;
            pos = pPos;
            msg = pMsg;
        }

        public String getMsg() {
            return msg;
        }

        public String toString() {
            StringBuffer result = new StringBuffer();
            if (mySeverity == SUCCESS) {
                result.append("SUCCESS");
            } else if (mySeverity == WARNING) {
                result.append("WARNING");
            } else {
                result.append("  ERROR");
            }
            result.append(" at ").append(url);
            if (pos != -1) {
                result.append(", char ").append(pos);
            }
            result.append(": ").append(getMsg());
            return result.toString();
        }
    }

    private class RefEvent extends Event {

        private URL referencedURL;

        private RefEvent(int pSeverity, URL pURL, URL pRefURL, int pPos, String pMsg) {
            super(pSeverity, pRefURL, pPos, pMsg);
            referencedURL = pURL;
        }

        public String getMsg() {
            return "Failed to reference " + referencedURL + ": " + super.getMsg();
        }
    }

    private class CheckedURL {

        URL url;

        URL referencingURL;

        int referencingPos;

        InputStream stream;

        boolean checkExistsOnly;

        boolean isExtern;

        private List anchors;

        private List refAnchors;

        private class AnchorReference {

            String name;

            URL ref;

            int pos;
        }

        private CheckedURL(URL pURL, URL pRefURL, int pPos) {
            referencingURL = pRefURL;
            referencingPos = pPos;
            String ref = pURL.getRef();
            String anchor = null;
            if (ref != null) {
                String s = pURL.toString();
                try {
                    if (s.endsWith("#" + ref)) {
                        pURL = new URL(s.substring(0, s.length() - ref.length() - 1));
                        anchor = ref;
                    } else {
                        throw new MalformedURLException();
                    }
                } catch (MalformedURLException e) {
                    handleRefError(pURL, pRefURL, pPos, "Unable to parse URL: " + pURL);
                }
            }
            url = pURL;
            if (anchor != null && pRefURL != null) {
                addAnchorRef(anchor, pRefURL, pPos);
            }
        }

        public void addAnchor(String pName) {
            if (anchors == null) {
                anchors = new ArrayList();
            }
            anchors.add(pName);
        }

        public void addAnchorRef(String pAnchor, URL pRefURL, int pPos) {
            AnchorReference anchorReference = new AnchorReference();
            anchorReference.name = pAnchor;
            anchorReference.ref = pRefURL;
            anchorReference.pos = pPos;
            if (refAnchors == null) {
                refAnchors = new ArrayList();
            }
            refAnchors.add(anchorReference);
        }

        public void validate() {
            if (refAnchors != null) {
                for (Iterator iter = refAnchors.iterator(); iter.hasNext(); ) {
                    AnchorReference anchorReference = (AnchorReference) iter.next();
                    if (anchors == null || !anchors.contains(anchorReference.name)) {
                        handleRefError(url, anchorReference.ref, anchorReference.pos, "Invalid anchor: " + anchorReference.name);
                    }
                }
                refAnchors.clear();
            }
        }
    }

    protected void addEvent(Event pEvent) {
        final String mName = "addEvent";
        logger.entering(mName, pEvent.toString());
        if (pEvent.mySeverity >= Event.ERROR) {
            haveErrors = true;
        }
        if (pEvent.mySeverity >= getSeverity()) {
            System.err.println(pEvent.toString());
        }
        logger.exiting(mName);
    }

    protected void handleError(URL pURL, int pPos, String pMsg) {
        addEvent(new Event(Event.ERROR, pURL, pPos, pMsg));
    }

    protected void handleRefError(URL pURL, URL pRefURL, int pPos, String pMsg) {
        if (pRefURL == null) {
            handleError(pURL, pPos, pMsg);
        }
        addEvent(new RefEvent(Event.ERROR, pURL, pRefURL, pPos, pMsg));
    }

    protected void handleWarning(URL pURL, int pPos, String pMsg) {
        addEvent(new Event(Event.WARNING, pURL, pPos, pMsg));
    }

    private class URLChecker extends HTMLEditorKit.ParserCallback {

        CheckedURL url;

        private URLChecker(CheckedURL pURL) {
            url = pURL;
        }

        protected void addLink(String pTagName, String pAttributeName, String pAttributeValue, int pPos, boolean pCheckExistsOnly) {
            final String mName = "URLChecker.addLink";
            logger.finest(mName, "->", new Object[] { pTagName, pAttributeName, pAttributeValue, Integer.toString(pPos), pCheckExistsOnly ? Boolean.TRUE : Boolean.FALSE });
            logger.finest(mName, "My URL: " + this.url.url);
            URL myUrl = null;
            boolean isAbsolute = false;
            try {
                myUrl = new URL(pAttributeValue);
                isAbsolute = true;
            } catch (MalformedURLException e) {
            }
            if (!isAbsolute) {
                try {
                    myUrl = new URL(this.url.url, pAttributeValue);
                } catch (MalformedURLException e) {
                    LinkChecker.this.handleError(this.url.url, pPos, "Failed to parse URL of attribute " + pAttributeName + " in tag " + pTagName);
                    return;
                }
            }
            if ("mailto".equals(myUrl.getProtocol())) {
                return;
            }
            CheckedURL checkedURL = new CheckedURL(myUrl, this.url.url, pPos);
            checkedURL.checkExistsOnly = pCheckExistsOnly;
            checkedURL.isExtern = isAbsolute;
            addURL(checkedURL);
            logger.finest(mName, "<-");
        }

        protected void handleTag(HTML.Tag t, MutableAttributeSet a, int pPos) {
            final String mName = "URLChecker.handleTag";
            logger.finest(mName, "->", new Object[] { t, a, Integer.toString(pPos) });
            String tagName = t.toString().toLowerCase();
            for (Enumeration en = a.getAttributeNames(); en.hasMoreElements(); ) {
                Object attributeNameObj = en.nextElement();
                String attributeName = attributeNameObj.toString().toLowerCase();
                Object o = a.getAttribute(attributeNameObj);
                if (o instanceof String) {
                    String attributeValue = (String) o;
                    if (tagName.equals("a")) {
                        if (attributeName.equals("href")) {
                            addLink(tagName, attributeName, attributeValue, pPos, false);
                        } else if (attributeName.equals("name")) {
                            url.addAnchor(attributeValue);
                        }
                    } else if (tagName.equals("img")) {
                        if (attributeName.equals("src")) {
                            addLink(tagName, attributeName, attributeValue, pPos, true);
                        }
                    }
                } else if (o instanceof Boolean) {
                } else {
                    handleWarning(url.url, pPos, "Unknown attribute type: " + (o == null ? "null" : o.getClass().getName()));
                }
            }
            logger.finest(mName, "<-");
        }

        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pPos) {
            super.handleSimpleTag(t, a, pPos);
            handleTag(t, a, pPos);
        }

        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pPos) {
            super.handleStartTag(t, a, pPos);
            handleTag(t, a, pPos);
        }

        public void handleError(String pErrorMsg, int pPos) {
            super.handleError(pErrorMsg, pPos);
            handleWarning(url.url, pPos, "Error reported by parser: " + pErrorMsg);
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger pLogger) {
        logger = pLogger;
    }

    public void setSeverity(String pSeverity) {
        if (pSeverity.equalsIgnoreCase("success")) {
            severity = Event.SUCCESS;
        } else if (pSeverity.equalsIgnoreCase("warning")) {
            severity = Event.WARNING;
        } else if (pSeverity.equalsIgnoreCase("error")) {
            severity = Event.ERROR;
        } else {
            throw new IllegalArgumentException("Invalid severity, neither of success|warning|error: " + pSeverity);
        }
    }

    public int getSeverity() {
        return severity;
    }

    public void setProxy(String pProxy) {
        if (pProxy == null && "".equals(pProxy)) {
            setProxyHost(null);
            setProxyPort(null);
        } else {
            int offset = pProxy.indexOf(':');
            if (offset == -1) {
                setProxyHost(pProxy);
                setProxyPort(null);
            } else {
                setProxyHost(pProxy.substring(0, offset));
                setProxyPort(pProxy.substring(offset + 1));
            }
        }
    }

    public void setProxyHost(String pHost) {
        if (pHost != null && "".equals(pHost)) {
            pHost = null;
        }
        proxyHost = pHost;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyPort(String pPort) {
        if (pPort != null && "".equals(pPort)) {
            pPort = null;
        }
        proxyPort = pPort;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public void addURL(URL pURL, InputStream pStream) {
        CheckedURL url = new CheckedURL(pURL, null, -1);
        url.stream = pStream;
        addURL(url);
    }

    public void addURL(URL pURL) {
        addURL(new CheckedURL(pURL, null, -1));
    }

    public void addURL(CheckedURL pURL) {
        final String mName = "addURL(URL)";
        logger.finest(mName, "->", new Object[] { pURL.url, pURL.referencingURL, Integer.toString(pURL.referencingPos) });
        if (urls.containsKey(pURL.url) || checkedUrls.containsKey(pURL.url)) {
            logger.exiting(mName, "Already registered");
            return;
        }
        urls.put(pURL.url, pURL);
        logger.finest(mName, "<-", "New URL");
    }

    public void parse(CheckedURL pURL) throws IOException {
        final String mName = "parse(CheckedURL)";
        logger.finest(mName, "->", pURL.url);
        logger.fine(mName, "Open", pURL.url);
        InputStream stream = pURL.stream;
        if (stream == null) {
            try {
                stream = pURL.url.openStream();
            } catch (IOException e) {
                handleRefError(pURL.url, pURL.referencingURL, pURL.referencingPos, "Failed to open URL: " + e.getMessage());
                return;
            }
        }
        if (pURL.checkExistsOnly || pURL.isExtern) {
            stream.close();
        } else {
            BufferedInputStream bStream = new BufferedInputStream(stream, 4096);
            ParserDelegator parser = new ParserDelegator();
            HTMLEditorKit.ParserCallback callback = new URLChecker(pURL);
            parser.parse(new InputStreamReader(bStream), callback, false);
            bStream.close();
        }
        logger.finest(mName, "<-");
    }

    public void parse() {
        final String mName = "parse";
        logger.finest(mName, "->");
        haveErrors = false;
        boolean isProxySetSet = System.getProperties().contains("http.proxySet");
        String proxySet = System.getProperty("http.proxySet");
        boolean isProxyHostSet = System.getProperties().contains("http.proxyHost");
        String myProxyHost = System.getProperty("http.proxyHost");
        boolean isProxyPortSet = System.getProperties().contains("http.proxyPort");
        String myProxyPort = System.getProperty("http.proxyPort");
        String host = getProxyHost();
        if (host != null) {
            System.setProperty("http.proxySet", "true");
            System.setProperty("http.proxyHost", host);
            String port = getProxyPort();
            if (port != null) {
                System.setProperty("http.proxyPort", port);
            }
        }
        try {
            while (!urls.isEmpty()) {
                Iterator iter = urls.values().iterator();
                CheckedURL checkedURL = (CheckedURL) iter.next();
                try {
                    parse(checkedURL);
                } catch (IOException e) {
                } finally {
                    urls.remove(checkedURL.url);
                    checkedUrls.put(checkedURL.url, checkedURL);
                }
            }
        } finally {
            if (host != null) {
                if (isProxySetSet) {
                    System.setProperty("http.proxySet", proxySet);
                } else {
                    System.getProperties().remove("http.proxySet");
                }
                if (isProxyHostSet) {
                    System.setProperty("http.proxyHost", myProxyHost);
                } else {
                    System.getProperties().remove("http.proxyHost");
                }
                if (isProxyPortSet) {
                    System.setProperty("http.proxyPort", myProxyPort);
                } else {
                    System.getProperties().remove("http.proxyPort");
                }
            }
        }
        for (Iterator iter = checkedUrls.values().iterator(); iter.hasNext(); ) {
            CheckedURL checkedURL = (CheckedURL) iter.next();
            checkedURL.validate();
        }
        if (!haveErrors) {
            System.out.println("No errors found.");
        }
        logger.finest(mName, "<-");
    }

    public static void main(String[] args) {
        LinkChecker checker = new LinkChecker();
        for (int i = 0; i < args.length; i++) {
            URL url;
            InputStream stream;
            try {
                url = new URL(args[i]);
                stream = url.openStream();
            } catch (IOException e) {
                try {
                    File f = new File(args[i]);
                    stream = new FileInputStream(f);
                    url = f.toURL();
                } catch (IOException f) {
                    System.err.println("Failed to open URL: " + args[i]);
                    continue;
                }
            }
            checker.addURL(url, stream);
            checker.parse();
        }
    }
}
