package org.vrspace.vfs;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.util.*;
import java.net.*;
import java.io.*;
import com.arthurdo.parser.*;
import org.vrspace.util.*;

public class Location {

    SAXParserFactory parserFactory = SAXParserFactory.newInstance();

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Start me with an URL argument!");
        }
        for (int i = 0; i < args.length; i++) {
            Location loc = Location.getInstance(null, args[i], true);
            long startTime = System.currentTimeMillis();
            System.err.println("Parsing " + loc + "...");
            loc.parse(true);
            System.err.println(loc + " parsed in " + (System.currentTimeMillis() - startTime) + " ms (connect in " + loc.connectTime + " ms)");
        }
    }

    /** is this location valid (url is valid and was accessible during last try)*/
    public boolean valid = false;

    /** MIME retreived by URLConnection.getContentType() */
    public String urlType;

    /** MIME retreived by URLConnection.guessContentTypeFromStream() */
    public String javaType;

    /** last access time (ms) */
    public long lastAccess;

    /** content length, default = -1 (N/A)*/
    public int length = -1;

    /** can we parse content of this location (false if !valid) */
    public boolean canParse = false;

    /** time needed to connect (ms) */
    public long connectTime;

    /** parent location */
    public Location parent;

    /** http fields */
    public HashMap fields = new HashMap();

    /** last modification date */
    public long lastModified;

    /** expires */
    public long expires;

    /** time distance: remote time - local time (most servers get time wrong) */
    public long timeDistance;

    /** URL of this location. */
    public URL url;

    /** Original URL used to create this location; i.e. may be relative to parent.*/
    public String originalUrl;

    /** local copy, if available */
    public File file;

    /** Location type - link, requisite, default = unknown */
    public int locType = ROOT;

    /** Is this link a directory */
    public boolean directory = false;

    /** Is this location relative to it's parent */
    public boolean relative = false;

    public static final int UNKNOWN = 0, LINK = 1, REQ = 2, ROOT = 3;

    public static final String[] locTypeStrings = { "unknown", "link", "requisite", "root" };

    public LinkedList children = new LinkedList();

    /**
  Utility method.
  Checks whether connection exists, performs some url checks, updates state variables (parent, children).

  @param parent Parent Location. May be null for root element.
  @param loc URL spec. Can be file relative to current dir etc.
  @param test Whether to check connection. Once connected, mime types and other fields are updated.
  @return new Location, absolute or relative to parent, or null if it couln't be created
  @see #urlType
  @see #javaType
  */
    public static Location getInstance(Location parent, String loc, boolean test) throws IOException, MalformedURLException {
        Location ret = null;
        try {
            ret = new Location(loc);
            ret.parent = parent;
            if (test) ret.testConnect();
        } catch (MalformedURLException mue) {
            if (loc.indexOf("://") == -1) {
                try {
                    ret = new Location(parent, loc);
                    ret.testConnect();
                } catch (MalformedURLException e) {
                    try {
                        ret = new Location("file://" + loc);
                        ret.testConnect();
                    } catch (MalformedURLException mue2) {
                        Logger.logError("Can't guess valid url for " + loc + ": " + mue2);
                    }
                }
            }
        }
        if (parent != null && ret != null) {
            if (!parent.children.contains(ret)) parent.children.add(ret);
            ret.parent = parent;
        }
        return ret;
    }

    /**
  New location.
  @param url absolute url
  */
    public Location(String url) throws MalformedURLException {
        this.url = new URL(url);
        this.originalUrl = url;
    }

    /**
  New location relative to parent location.
  @param loc parent location
  @param url relative url
  */
    public Location(Location loc, String url) throws MalformedURLException {
        this.parent = loc;
        if (loc != null) {
            this.url = new URL(loc.url, url);
            this.relative = true;
        } else {
            this.url = new URL(url);
        }
        this.originalUrl = url;
    }

    /**
  Returns directory portion of this URL.
  */
    public URL getDir() throws MalformedURLException {
        String file = url.getFile();
        String dir = url.toString();
        dir = dir.substring(0, dir.length() - file.length());
        return new URL(dir);
    }

    /**
  Returns string representation of this location's URL.
  */
    public String toString() {
        if (this.url != null) return this.url.toString();
        return "null";
    }

    /**
  Returns true if this is XML document: it's MIME type is application/xml or it's WML.
  */
    public boolean isXML() {
        return "application/xml".equals(this.urlType) || "application/xml".equals(this.javaType) || isWML();
    }

    /**
  Returns true if this is WML document: it's MIME type is "text/vnd.wap.wml".
  */
    public boolean isWML() {
        return "text/vnd.wap.wml".equals(this.urlType) || "text/vnd.wap.wml".equals(this.javaType);
    }

    /**
  Returns true if this is HTML document: it's MIME type is "text/html".
  */
    public boolean isHTML() {
        return (this.urlType != null && this.urlType.startsWith("text/html")) || "text/html".equals(this.javaType);
    }

    /**
  Returns true if this is plain text document: it's MIME type is "text/plain".
  */
    public boolean isText() {
        return (this.urlType != null && this.urlType.startsWith("text/plain")) || "text/plain".equals(this.javaType);
    }

    /**
  Returns true if this is VRML document: it's MIME type is "x-world/x-vrml".
  */
    public boolean isVRML() {
        return "x-world/x-vrml".equals(this.urlType) || "x-world/x-vrml".equals(this.javaType);
    }

    /**
  Returns true if this document can be parsed - HTML, VRML or WML.
  */
    public boolean canParse() {
        return isHTML() || isVRML() || isWML();
    }

    public void addField(String name, String value) {
        fields.put(name, value);
    }

    public String getField(String name) {
        return name;
    }

    public Map getFields() {
        return fields;
    }

    public boolean testConnect() {
        boolean ret = false;
        try {
            long startTime = System.currentTimeMillis();
            InputStream in = getInputStream();
            this.javaType = URLConnection.guessContentTypeFromStream(in);
            in.close();
            this.connectTime = System.currentTimeMillis() - startTime;
            this.valid = true;
            this.canParse = canParse();
            ret = true;
        } catch (UnknownServiceException e) {
            Logger.logWarning("Can't verify " + url + " - " + e);
        } catch (Throwable t) {
            if (parent == null) {
                Logger.logError("Can't connect to " + url + " - " + t);
            } else {
                Logger.logError("Can't connect to " + url + " required by " + parent + " " + t);
            }
            this.valid = false;
        }
        return ret;
    }

    /**
  Connect to the source. Updates header fields.
  */
    public URLConnection openConnection() throws IOException {
        URLConnection conn = this.url.openConnection();
        return conn;
    }

    public InputStream getInputStream() throws IOException {
        URLConnection conn = openConnection();
        conn.connect();
        this.urlType = conn.getContentType();
        length = conn.getContentLength();
        return conn.getInputStream();
    }

    /**
  Parse the content.
  Supported file formats: HTML, VRML, WML.
  Prefers to use local document copy over url, so parses file if file exists, otherwise parses url content.
  Each new location found in this document will have parent set to this document.
  Each location type is also set to appropriate LINK or REQ.
  @param verify Check if links inside document point to an existing document
  @return Array of locations contained in this document.
  */
    public Location[] parse(boolean verify) throws Exception {
        if (this.isHTML()) {
            InputStream is = null;
            if (file == null) {
                is = getInputStream();
            } else {
                is = new FileInputStream(file);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            HtmlStreamTokenizer st = new HtmlStreamTokenizer(in);
            HtmlTag tag = new HtmlTag();
            while (st.nextToken() != HtmlStreamTokenizer.TT_EOF) {
                int ttype = st.getTokenType();
                if (ttype == HtmlStreamTokenizer.TT_TAG) {
                    st.parseTag(st.getStringValue(), tag);
                    int type = tag.getTagType();
                    if (type == tag.T_A) {
                        String href = tag.getParam("href");
                        if (href != null) {
                            Location url = Location.getInstance(this, href, verify);
                            url.locType = LINK;
                        }
                    } else if (type == tag.T_IMG) {
                        String src = tag.getParam("src");
                        Location url = Location.getInstance(this, src, verify);
                        url.locType = REQ;
                    } else if (type == tag.T_EMBED) {
                        String src = tag.getParam("src");
                        Location url = Location.getInstance(this, src, verify);
                        url.locType = REQ;
                    } else if (type == tag.T_TD) {
                        String src = tag.getParam("background");
                        if (src != null) {
                            Location url = Location.getInstance(this, src, verify);
                            url.locType = REQ;
                        }
                    } else if (type == tag.T_BODY) {
                        String src = tag.getParam("background-image");
                        if (src != null) {
                            Location url = Location.getInstance(this, src, verify);
                            url.locType = REQ;
                        } else {
                            src = tag.getParam("style");
                            int pos = 0;
                            if (src != null && (pos = src.toLowerCase().indexOf("background-image:")) >= 0) {
                                pos = src.toLowerCase().indexOf("url(", pos);
                                int pos2 = src.indexOf(")", pos + 1);
                                String tmp = src.substring(pos + 4, pos2);
                                Location url = Location.getInstance(this, tmp, verify);
                                url.locType = REQ;
                            }
                        }
                    } else if (type == tag.T_FRAME) {
                        String src = tag.getParam("src");
                        Location url = Location.getInstance(this, src, verify);
                        url.locType = REQ;
                    }
                }
            }
            in.close();
            return (Location[]) children.toArray(new Location[children.size()]);
        } else if (this.isVRML()) {
            InputStream is = null;
            if (file == null) {
                is = getInputStream();
            } else {
                is = new FileInputStream(file);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = in.readLine()) != null) {
                int pos = 0;
                if ((pos = line.indexOf("url", pos)) >= 0) {
                    int comPos = line.indexOf("#");
                    if (comPos == -1 || comPos > pos) {
                        StringTokenizer st = new StringTokenizer(line.substring(pos), "\"");
                        st.nextToken();
                        if (st.hasMoreTokens()) {
                            String tmp = st.nextToken();
                            if (tmp.toLowerCase().indexOf("script:") == -1) {
                                Location url = Location.getInstance(this, tmp, verify);
                                url.locType = REQ;
                            }
                        }
                    }
                }
            }
            return (Location[]) children.toArray(new Location[children.size()]);
        } else if (this.isWML()) {
            SAXParser parser = parserFactory.newSAXParser();
            WMLHandler handler = new WMLHandler(this, verify);
            if (file == null) {
                parser.parse(url.toString(), handler);
            } else {
                parser.parse(file, handler);
            }
            return handler.getLocations();
        } else {
            throw new UnsupportedOperationException("Unsupported file format - java format:" + javaType + " url format:" + urlType);
        }
    }

    public class WMLHandler extends DefaultHandler {

        Location loc;

        boolean verify;

        Vector locations = new Vector();

        public WMLHandler(Location loc, boolean verify) {
            this.loc = loc;
            this.verify = verify;
        }

        public Location[] getLocations() {
            return (Location[]) locations.toArray(new Location[locations.size()]);
        }

        public void warning(SAXParseException e) throws SAXException {
            Logger.logWarning("Line " + e.getLineNumber() + " col " + e.getColumnNumber() + " - " + e);
        }

        public void error(SAXParseException e) throws SAXException {
            Logger.logWarning("Line " + e.getLineNumber() + " col " + e.getColumnNumber() + " - " + e);
        }

        public void fatalError(SAXParseException e) throws SAXException {
            Logger.logWarning("ERROR: Line " + e.getLineNumber() + " col " + e.getColumnNumber() + " - " + e);
            super.fatalError(e);
        }

        public void startDocument() throws SAXException {
            Logger.logDebug("Document started");
            super.startDocument();
        }

        public void endDocument() throws SAXException {
            Logger.logDebug("Document ended");
            super.endDocument();
        }

        public void skippedEntity(String name) throws SAXException {
            Logger.logDebug("Skipped entity " + name);
            super.skippedEntity(name);
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if ("go".equals(qName)) {
                String loc = attributes.getValue("href");
                addChild(loc, LINK);
            } else if ("a".equals(qName)) {
                String loc = attributes.getValue("href");
                addChild(loc, LINK);
            } else if ("img".equals(qName)) {
                String loc = attributes.getValue("src");
                addChild(loc, REQ);
            }
            super.startElement(uri, localName, qName, attributes);
        }

        private void addChild(String loc, int type) {
            try {
                if (loc == null) {
                    Logger.logError("URI is null!!!");
                } else {
                    Location url = Location.getInstance(this.loc, loc, verify);
                    url.locType = type;
                    locations.add(url);
                }
            } catch (Throwable t) {
                Logger.logError(t);
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
        }

        public void notationDecl(String name, String publicId, String systemId) throws SAXException {
            Logger.logError("Notation: name = " + name + " publicId = " + publicId + " systemId = " + systemId);
            super.notationDecl(name, publicId, systemId);
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            Logger.logDebug("End prefix mapping: " + prefix);
            super.endPrefixMapping(prefix);
        }

        public void processingInstruction(String target, String data) throws SAXException {
            Logger.logDebug("Processing instruction " + target + " " + data);
            super.processingInstruction(target, data);
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            Logger.logDebug("Start prefix mapping: " + prefix + " " + uri);
            super.startPrefixMapping(prefix, uri);
        }
    }

    /** Returns root, link, requisite or unknown. */
    public String getTypeString() {
        return locTypeStrings[locType];
    }

    /** returns MIME type as string */
    public String getMimeString() {
        String ret = null;
        if (this.urlType != null) {
            ret = this.urlType;
        } else {
            ret = this.javaType;
        }
        if (ret != null) {
            if (ret.indexOf(" ") > 0 || ret.indexOf(";") > 0) {
                StringTokenizer st = new StringTokenizer(ret, " ;");
                ret = st.nextToken();
            }
        }
        return ret;
    }

    /**
  Two locations are equals if their respective urls are equal.
  */
    public boolean equals(Object loc) {
        return loc instanceof Location && url.equals(((Location) loc).url);
    }

    /**
  Convert all links in given Location to relative.
  This only makes sense if Location is cached, IOW it was already downloaded, IOW file member variable is not null.
  Location must have been parsed already.
  Only links to already downloaded files are converted.
  */
    public void convert() throws FileNotFoundException, IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        br.close();
        Iterator it = children.iterator();
        while (it.hasNext()) {
            Location child = (Location) it.next();
            if (child.file != null && (child.directory || !child.relative || child.originalUrl.startsWith("/"))) {
                String newPath = convertLink(child);
                Logger.logDebug("Converting " + child.originalUrl + " to " + newPath + " in " + file.getPath());
                int pos = -1;
                while ((pos = sb.indexOf(child.originalUrl, pos)) > 0) {
                    sb.replace(pos, pos + child.originalUrl.length(), newPath);
                    pos += newPath.length();
                }
            }
        }
        FileWriter fw = new FileWriter(file, false);
        fw.write(sb.toString(), 0, sb.length());
        fw.close();
    }

    /**
  Converts link to local file relative to referer.
  Requires that link was already downloaded.
  */
    public String convertLink(Location link) {
        String ret = link.file.getPath();
        String ref = parent.file.getPath();
        String sep = System.getProperty("file.separator");
        Vector retDirs = new Vector();
        Vector refDirs = new Vector();
        StringTokenizer st = new StringTokenizer(ret, sep);
        while (st.hasMoreTokens()) {
            retDirs.add(st.nextToken());
        }
        st = new StringTokenizer(ref, sep);
        while (st.hasMoreTokens()) {
            refDirs.add(st.nextToken());
        }
        int same = 0;
        for (int i = 0; i < retDirs.size() && i < refDirs.size(); i++) {
            if (retDirs.get(i).equals(refDirs.get(i))) {
                same = i;
            } else {
                break;
            }
        }
        StringBuffer sb = new StringBuffer();
        for (int i = same + 1; i < refDirs.size() - 1; i++) {
            sb.append("..");
            sb.append(sep);
        }
        for (int i = same + 1; i < retDirs.size() - 1; i++) {
            sb.append(retDirs.get(i));
            sb.append(sep);
        }
        sb.append(retDirs.get(retDirs.size() - 1));
        ret = sb.toString();
        return ret;
    }
}
