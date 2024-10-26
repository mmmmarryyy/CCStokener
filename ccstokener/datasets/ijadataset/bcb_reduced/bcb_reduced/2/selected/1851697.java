package org.personalsmartspace.pss_autoconfig.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;
import org.osgi.framework.Bundle;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Modified version of an XML Pull parser for component.xml
 * 
 * @author mcrotty@users.sourceforge.net, based on an implementation by Mats-Ola Persson, and others 
 * 
 */
public class Parser {

    static String[] supportedTypes = { "Boolean", "Byte", "Char", "Double", "Float", "Integer", "Long", "Short", "String", "Character" };

    private static String SCR_NAMESPACE_URI = "http://www.osgi.org/xmlns/scr/v1.0.0";

    public static Collection<String> readXML(Bundle declaringBundle, URL url) throws XmlPullParserException {
        try {
            return readXML(declaringBundle, url.openStream());
        } catch (IOException e) {
            throw new XmlPullParserException("Could not open \"" + url + "\" got exception:" + e.getLocalizedMessage());
        }
    }

    public static Collection<String> readXML(Bundle declaringBundle, InputStream stream) throws XmlPullParserException {
        try {
            XmlPullParser parser = new MXParser();
            parser.setInput(stream, null);
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            return readDocument(declaringBundle, parser);
        } catch (Exception e) {
            throw new XmlPullParserException("While reading declaration in \"" + stream + "\" got exception " + e.getLocalizedMessage());
        }
    }

    private static ArrayList<String> readDocument(Bundle declaringBundle, XmlPullParser parser) throws XmlPullParserException, IOException {
        ArrayList<String> decls = new ArrayList<String>();
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                event = parser.next();
                continue;
            }
            if (parser.getEventType() == XmlPullParser.START_TAG && "component".equals(parser.getName()) && (parser.getDepth() == 1 || SCR_NAMESPACE_URI.equals(parser.getNamespace()))) {
                try {
                    String config = readComponent(declaringBundle, parser);
                    decls.add(config);
                } catch (Exception e) {
                    continue;
                }
            }
            event = parser.next();
        }
        return decls;
    }

    private static String readComponent(Bundle bundle, XmlPullParser parser) throws XmlPullParserException, IOException {
        String curr = getComponent(parser);
        int event = parser.getEventType();
        while (event != XmlPullParser.END_TAG) {
            if (event != XmlPullParser.START_TAG) {
                event = parser.next();
                continue;
            }
            skip(parser);
            event = parser.getEventType();
        }
        return curr;
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        int level = 0;
        int event = parser.getEventType();
        while (true) {
            if (event == XmlPullParser.START_TAG) {
                level++;
            } else if (event == XmlPullParser.END_TAG) {
                level--;
                if (level == 0) {
                    parser.next();
                    break;
                }
            }
            event = parser.next();
        }
    }

    private static String getComponent(XmlPullParser parser) throws XmlPullParserException, IOException {
        String name = null;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            if (parser.getAttributeName(i).equals("name")) {
                name = parser.getAttributeValue(i);
            }
        }
        if (name == null) {
            missingAttr(parser, "name");
        }
        parser.next();
        return name;
    }

    private static void missingAttr(XmlPullParser parser, String attr) throws XmlPullParserException {
        throw new XmlPullParserException("Missing \"" + attr + "\" attribute in \"" + parser.getName() + "\"-tag.");
    }

    /**
     * Split a string into words separated by specified characters.
     * 
     * @param s
     *            String to split.
     * @param whiteSpace
     *            whitespace to use for splitting. Any of the characters in the
     *            whiteSpace string are considered whitespace between words and
     *            will be removed from the result. If no words are found, return
     *            an array of length zero.
     */
    public static String[] splitwords(String s, String whiteSpace) {
        Vector<String> v = new Vector<String>();
        StringBuffer buf = new StringBuffer();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (whiteSpace.indexOf(c) == -1) {
                if (buf == null) {
                    buf = new StringBuffer();
                }
                buf.append(c);
                i++;
            } else {
                if (buf != null) {
                    v.addElement(buf.toString());
                    buf = null;
                }
                while ((i < s.length()) && (-1 != whiteSpace.indexOf(s.charAt(i)))) {
                    i++;
                }
            }
        }
        if (buf != null) {
            v.addElement(buf.toString());
        }
        String[] r = new String[v.size()];
        v.copyInto(r);
        return r;
    }
}
