package de.pannenleiter.client;

import java.io.*;
import java.net.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import de.pannenleiter.util.aelfred.*;

/**
 * NetInterface -- http Server connection manager
 *
 *
 */
public class NetInterface {

    String server;

    String db;

    String document;

    Parser parser;

    TreeBuilder builder;

    public TreeNode fetch(TreeNode owner, String pattern, String fetchChilds, String fetchAttributes, String flags, boolean updateOwner) throws Exception {
        builder.start(owner, updateOwner);
        parser.setDocumentHandler(builder);
        pattern = URLEncoder.encode(pattern);
        String arg = server + "?todo=fetch&db=" + db + "&document=" + document + "&pattern=" + pattern;
        if (fetchChilds != null) {
            arg += "&fetch-childs=" + URLEncoder.encode(fetchChilds);
        }
        if (fetchAttributes != null) {
            arg += "&fetch-attributes=" + URLEncoder.encode(fetchAttributes);
        }
        if (flags != null) {
            arg += "&flags=" + URLEncoder.encode(flags);
        }
        URL url = new URL(arg);
        URLConnection con = url.openConnection();
        con.setUseCaches(false);
        con.connect();
        InputSource xmlInput = new InputSource(new InputStreamReader(con.getInputStream(), "ISO-8859-1"));
        parser.parse(xmlInput);
        return owner;
    }

    public TreeNode fetchArchive(TreeNode owner, int id) throws Exception {
        builder.start(owner, false);
        parser.setDocumentHandler(builder);
        String arg = server + "?todo=archive&db=" + db + "&document=" + document + "&id=" + id;
        URL url = new URL(arg);
        URLConnection con = url.openConnection();
        con.setUseCaches(false);
        con.connect();
        InputSource xmlInput = new InputSource(new InputStreamReader(con.getInputStream(), "ISO-8859-1"));
        parser.parse(xmlInput);
        return owner;
    }

    public String fetch(String pattern, String fetchChilds, String fetchAttributes, String flags) throws Exception {
        pattern = URLEncoder.encode(pattern);
        String arg = server + "?todo=fetch&db=" + db + "&document=" + document + "&pattern=" + pattern;
        if (fetchChilds != null) {
            arg += "&fetch-childs=" + URLEncoder.encode(fetchChilds);
        }
        if (fetchAttributes != null) {
            arg += "&fetch-attributes=" + URLEncoder.encode(fetchAttributes);
        }
        if (flags != null) {
            arg += "&flags=" + URLEncoder.encode(flags);
        }
        return getString(arg);
    }

    public String write(String xml, int owner) throws Exception {
        xml = URLEncoder.encode(xml);
        String arg = server + "?todo=write&db=" + db + "&document=" + document + "&xml=" + xml + "&owner=" + owner;
        return getString(arg);
    }

    public String remove(int id) throws Exception {
        String arg = server + "?todo=remove&db=" + db + "&document=" + document + "&id=" + id;
        return getString(arg);
    }

    public String getString(String arg) throws Exception {
        URL url = new URL(arg);
        URLConnection con = url.openConnection();
        con.setUseCaches(false);
        con.connect();
        InputStreamReader src = new InputStreamReader(con.getInputStream(), "ISO-8859-1");
        StringBuffer stb = new StringBuffer();
        char[] buf = new char[1024];
        int l;
        while ((l = src.read(buf, 0, 1024)) >= 0) {
            stb.append(buf, 0, l);
        }
        String res = stb.toString();
        if (res.startsWith("<pannenleiter-exception")) {
            builder.start(new TreeNode((TreeWidget) null, false), false);
            InputSource xmlInput = new InputSource(new StringReader(res));
            parser.setDocumentHandler(builder);
            parser.parse(xmlInput);
        }
        return res;
    }

    public NetInterface(String server, String db, String document) throws Exception {
        this.server = server;
        this.db = URLEncoder.encode(db);
        this.document = URLEncoder.encode(document);
        parser = new SAXDriver();
        builder = new TreeBuilder();
    }

    public static void main(String args[]) {
        try {
            NetInterface n = new NetInterface("http://localhost:8080/servlet/rmdms", "test", "forum");
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(2);
        }
    }
}

class TreeBuilder implements DocumentHandler {

    boolean updateOwner;

    TreeNode current;

    int indent;

    boolean isException;

    String tag;

    String message;

    public void start(TreeNode node, boolean updateOwner) {
        this.updateOwner = updateOwner;
        current = node;
        indent = 0;
        isException = false;
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (isException) {
            if (tag != null) message += "\n" + tag + ": " + new String(ch, start, length);
        }
    }

    public void endDocument() throws SAXException {
    }

    public void endElement(String element) throws SAXException {
        if (indent == 0) {
            if ("pannenleiter-exception".equals(element)) {
                throw new SAXException(message);
            }
            if (isException) {
                tag = null;
            }
            return;
        }
        indent--;
        if (indent == 1 && updateOwner) {
        } else if (indent > 0) {
            current = current.getParent();
        }
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    public void processingInstruction(String target, String data) throws SAXException {
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
    }

    public void startElement(String element, AttributeList atts) throws SAXException {
        if (indent == 0) {
            if ("pannenleiter-exception".equals(element)) {
                isException = true;
                message = "Server failed:";
                tag = null;
                return;
            }
            if (isException) {
                tag = element;
                return;
            }
        }
        if (indent == 1 && updateOwner) {
            ((ElementNode) current).init(element, atts);
        } else if (indent > 0) {
            current = new ElementNode(current, element, atts);
        }
        indent++;
    }
}
