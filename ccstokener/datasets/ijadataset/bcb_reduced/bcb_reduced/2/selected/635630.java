package fedora.utilities.policyEditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class PolicyEditorInputkXML {

    static boolean verbose = true;

    static XmlPullParser xr = null;

    static FedoraNode fedoraNode = null;

    static FedoraNode fedoraRoot = null;

    static Object semaphore = null;

    static {
        init();
    }

    private PolicyEditorInputkXML() {
    }

    public static void init() {
        xr = new KXmlParser();
        semaphore = new Object();
        fedoraNode = null;
        fedoraRoot = null;
    }

    static FedoraNode readInputStream(InputStream stream, String name) {
        verbose = false;
        System.out.println("Opening metadata url " + name);
        try {
            synchronized (semaphore) {
                xr.setInput(new InputStreamReader(stream));
                parseMetadata(xr);
                return (fedoraRoot);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Unable to open URL: " + name);
        } catch (IOException ioe) {
            System.out.println("Problems reading file: " + name);
        } catch (XmlPullParserException ioe) {
            System.out.println("Error parsing file: " + name);
        }
        fedoraRoot = null;
        return (null);
    }

    static FedoraNode readURLbyName(String urlName) {
        try {
            URL url = new URL(urlName);
            return (readInputStream(url.openStream(), url.toString()));
        } catch (MalformedURLException e) {
            return (null);
        } catch (IOException ioe) {
            System.out.println("Problems Opening URL: " + urlName);
            return (null);
        }
    }

    static FedoraNode readResourcebyName(String resourceName) {
        System.out.println("resource URL is: " + resourceName);
        URL url = ClassLoader.getSystemClassLoader().getResource(resourceName);
        if (url == null) {
            System.out.println("Resource not found: " + resourceName);
            return (null);
        }
        try {
            fedoraNode = null;
            fedoraRoot = null;
            FedoraNode.model = null;
            return (readInputStream(url.openStream(), url.toString()));
        } catch (IOException ioe) {
            System.out.println("Problems Opening Resource: " + resourceName);
            return (null);
        }
    }

    static void readFile(File file) {
        try {
            readInputStream(new FileInputStream(file), file.toString());
        } catch (FileNotFoundException e) {
            System.out.println("Problems Opening File: " + file.toString());
        }
    }

    protected static void parseMetadata(XmlPullParser xr) throws XmlPullParserException, IOException {
        int eventType = xr.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_DOCUMENT) {
            } else if (eventType == XmlPullParser.END_DOCUMENT) {
            } else if (eventType == XmlPullParser.START_TAG) {
                if (xr.getName().equals("resource")) {
                    startResourceElement(xr);
                }
                if (xr.getName().equals("template")) {
                    startGroupTemplateElement(xr);
                }
                if (xr.getName().equals("access")) {
                    startAccessElement(xr);
                }
                if (xr.getName().equals("ruledef")) {
                    startRuledefElement(xr);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (xr.getName().equals("resource")) {
                    endResourceElement(xr);
                }
                if (xr.getName().equals("template")) {
                    endGroupTemplateElement(xr);
                }
                if (xr.getName().equals("access")) {
                    endAccessElement(xr);
                }
                if (xr.getName().equals("ruledef")) {
                    endRuledefElement(xr);
                }
            } else if (eventType == XmlPullParser.TEXT) {
                characters(xr);
            }
            eventType = xr.next();
        }
    }

    protected static void startResourceElement(XmlPullParser xr) {
        String name = xr.getAttributeValue(null, "name");
        if (name == null) {
            name = xr.getName();
        }
        String shortname = xr.getAttributeValue(null, "shortname");
        String value = xr.getAttributeValue(null, "value");
        String value2 = xr.getAttributeValue(null, "value1");
        String action = htmlunescape(xr.getAttributeValue(null, "action"));
        String resource = htmlunescape(xr.getAttributeValue(null, "resource"));
        if (verbose) System.out.println("start element: " + name);
        Object parent = null;
        if (fedoraNode != null) parent = fedoraNode;
        fedoraNode = new FedoraNode(parent, name, shortname, action, resource);
        if (value != null) {
            if (value.equals(FedoraNode.seeParentShort) || value.equals("0") || value.length() == 0) {
                fedoraNode.setValue(0, FedoraNode.seeParent);
            } else if (value.equals(FedoraNode.seeChildrenShort) || value.equals("1")) {
                fedoraNode.setValue(0, FedoraNode.seeChildren);
            } else {
                fedoraNode.setValue(0, GroupRuleInfo.findEntryByShortName(true, value));
            }
        }
        if (value2 != null) {
            if (value2.equals(FedoraNode.seeParentShort) || value2.equals("0") || value2.length() == 0) {
                fedoraNode.setValue(1, FedoraNode.seeParent);
            } else if (value2.equals(FedoraNode.seeChildrenShort) || value2.equals("1")) {
                fedoraNode.setValue(1, FedoraNode.seeChildren);
            } else {
                fedoraNode.setValue(1, GroupRuleInfo.findEntryByShortName(false, value2));
            }
        }
        if (fedoraRoot == null) fedoraRoot = fedoraNode;
        if (parent != null) {
            ((FedoraNode) parent).children.add(fedoraNode);
        }
    }

    protected static void endResourceElement(XmlPullParser xr) {
        fedoraNode = (FedoraNode) (fedoraNode.parent);
    }

    protected static void startGroupTemplateElement(XmlPullParser xr) {
        String name = xr.getAttributeValue(null, "name");
        String description = xr.getAttributeValue(null, "description");
        String rule = xr.getAttributeValue(null, "rule");
        String subject = xr.getAttributeValue(null, "subject");
        String condition = xr.getAttributeValue(null, "condition");
        String parms = xr.getAttributeValue(null, "parms");
        if (rule.equals("permit")) {
            GroupRuleInfo newRule = new GroupRuleInfo(name, description, subject, condition, parms, true);
            GroupRuleInfo.permitTemplates.addElement(newRule);
        } else if (rule.equals("deny")) {
            GroupRuleInfo newRule = new GroupRuleInfo(name, description, subject, condition, parms, false);
            GroupRuleInfo.denyTemplates.addElement(newRule);
        }
    }

    protected static void endGroupTemplateElement(XmlPullParser xr) {
    }

    protected static void startAccessElement(XmlPullParser xr) {
        String shortname = xr.getAttributeValue(null, "shortname");
        String value = xr.getAttributeValue(null, "value");
        String value2 = xr.getAttributeValue(null, "value1");
        fedoraNode = Utility.findNodeByShortName(PolicyEditor.mainWin.getRootNode(), shortname);
        if (fedoraNode == null) {
            return;
        }
        if (value != null) {
            if (value.equals(FedoraNode.seeParentShort) || value.equals("0") || value.length() == 0) {
                fedoraNode.setValue(0, FedoraNode.seeParent);
            } else if (value.equals(FedoraNode.seeChildrenShort) || value.equals("1")) {
                fedoraNode.setValue(0, FedoraNode.seeChildren);
            } else {
                fedoraNode.setValue(0, GroupRuleInfo.findEntryByShortName(true, value));
            }
        }
        if (value2 != null) {
            if (value2.equals(FedoraNode.seeParentShort) || value2.equals("0") || value2.length() == 0) {
                fedoraNode.setValue(1, FedoraNode.seeParent);
            } else if (value2.equals(FedoraNode.seeChildrenShort) || value2.equals("1")) {
                fedoraNode.setValue(1, FedoraNode.seeChildren);
            } else {
                fedoraNode.setValue(1, GroupRuleInfo.findEntryByShortName(false, value2));
            }
        }
    }

    protected static void endAccessElement(XmlPullParser xr) {
    }

    protected static void startRuledefElement(XmlPullParser xr) {
        String buildparm = xr.getAttributeValue(null, "buildparm");
        String buildparms[] = buildparm.split(",");
        String parms = xr.getAttributeValue(null, "parms");
        boolean accept = buildparms[0].equals("permit");
        if (buildparms[1].equals("Template")) {
            int templateNum = Integer.parseInt(buildparms[2]);
            GroupRuleInfo.buildFromTemplate(accept, templateNum, parms);
        } else if (buildparms[1].equals("Combo")) {
            int andOrOr = buildparms[2].equals("and") ? GroupRuleInfo.AND : GroupRuleInfo.OR;
            int rules[] = new int[buildparms.length - 3];
            for (int i = 0; i < buildparms.length - 3; i++) {
                rules[i] = Integer.parseInt(buildparms[i + 3]);
            }
            GroupRuleInfo.buildFromRules(accept, rules, andOrOr);
        }
    }

    protected static void endRuledefElement(XmlPullParser xr) {
    }

    protected static void characters(XmlPullParser xr) {
    }

    static Object[][] entities = { { "#10", new Integer(10) }, { "quot", new Integer(34) }, { "amp", new Integer(38) }, { "nbsp", new Integer(160) }, { "copy", new Integer(169) }, { "reg", new Integer(174) }, { "Agrave", new Integer(192) }, { "Aacute", new Integer(193) }, { "Acirc", new Integer(194) }, { "Atilde", new Integer(195) }, { "Auml", new Integer(196) }, { "Aring", new Integer(197) }, { "AElig", new Integer(198) }, { "Ccedil", new Integer(199) }, { "Egrave", new Integer(200) }, { "Eacute", new Integer(201) }, { "Ecirc", new Integer(202) }, { "Euml", new Integer(203) }, { "Igrave", new Integer(204) }, { "Iacute", new Integer(205) }, { "Icirc", new Integer(206) }, { "Iuml", new Integer(207) }, { "ETH", new Integer(208) }, { "Ntilde", new Integer(209) }, { "Ograve", new Integer(210) }, { "Oacute", new Integer(211) }, { "Ocirc", new Integer(212) }, { "Otilde", new Integer(213) }, { "Ouml", new Integer(214) }, { "Oslash", new Integer(216) }, { "Ugrave", new Integer(217) }, { "Uacute", new Integer(218) }, { "Ucirc", new Integer(219) }, { "Uuml", new Integer(220) }, { "Yacute", new Integer(221) }, { "THORN", new Integer(222) }, { "szlig", new Integer(223) }, { "agrave", new Integer(224) }, { "aacute", new Integer(225) }, { "acirc", new Integer(226) }, { "atilde", new Integer(227) }, { "auml", new Integer(228) }, { "aring", new Integer(229) }, { "aelig", new Integer(230) }, { "ccedil", new Integer(231) }, { "egrave", new Integer(232) }, { "eacute", new Integer(233) }, { "ecirc", new Integer(234) }, { "euml", new Integer(235) }, { "igrave", new Integer(236) }, { "iacute", new Integer(237) }, { "icirc", new Integer(238) }, { "iuml", new Integer(239) }, { "igrave", new Integer(236) }, { "iacute", new Integer(237) }, { "icirc", new Integer(238) }, { "iuml", new Integer(239) }, { "eth", new Integer(240) }, { "ntilde", new Integer(241) }, { "ograve", new Integer(242) }, { "oacute", new Integer(243) }, { "ocirc", new Integer(244) }, { "otilde", new Integer(245) }, { "ouml", new Integer(246) }, { "oslash", new Integer(248) }, { "ugrave", new Integer(249) }, { "uacute", new Integer(250) }, { "ucirc", new Integer(251) }, { "uuml", new Integer(252) }, { "yacute", new Integer(253) }, { "thorn", new Integer(254) }, { "yuml", new Integer(255) }, { "euro", new Integer(8364) } };

    static Map e2i = new HashMap();

    static Map i2e = new HashMap();

    static {
        for (int i = 0; i < entities.length; ++i) {
            e2i.put(entities[i][0], entities[i][1]);
            i2e.put(entities[i][1], entities[i][0]);
        }
    }

    /**
     * Turns funky characters into HTML entity equivalents<p>
     * e.g. <tt>"bread" & "butter"</tt> => <tt>&amp;quot;bread&amp;quot; &amp;amp; &amp;quot;butter&amp;quot;</tt>.
     * Update: supports nearly all HTML entities, including funky accents. See the source code for more detail.
     * @see #htmlunescape(String)
     **/
    public static String htmlescape(String s1) {
        if (s1 == null) return (null);
        StringBuffer buf = new StringBuffer();
        int i;
        for (i = 0; i < s1.length(); ++i) {
            char ch = s1.charAt(i);
            String entity = (String) i2e.get(new Integer((int) ch));
            if (entity == null) {
                if (((int) ch) > 128) {
                    buf.append("&#" + ((int) ch) + ";");
                } else {
                    buf.append(ch);
                }
            } else {
                buf.append("&" + entity + ";");
            }
        }
        return buf.toString();
    }

    /**
     * Given a string containing entity escapes, returns a string
     * containing the actual Unicode characters corresponding to the
     * escapes.
     *
     * Note: nasty bug fixed by Helge Tesgaard (and, in parallel, by
     * Alex, but Helge deserves major props for emailing me the fix).
     * 15-Feb-2002 Another bug fixed by Sean Brown <sean@boohai.com>
     *
     * @see #htmlescape(String)
     **/
    public static String htmlunescape(String s1) {
        if (s1 == null) return (null);
        StringBuffer buf = new StringBuffer();
        int i;
        for (i = 0; i < s1.length(); ++i) {
            char ch = s1.charAt(i);
            if (ch == '&') {
                int semi = s1.indexOf(';', i + 1);
                if (semi == -1) {
                    buf.append(ch);
                    continue;
                }
                String entity = s1.substring(i + 1, semi);
                Integer iso;
                if (entity.charAt(0) == '#') {
                    iso = new Integer(entity.substring(1));
                } else {
                    iso = (Integer) e2i.get(entity);
                }
                if (iso == null) {
                    buf.append("&" + entity + ";");
                } else {
                    buf.append((char) (iso.intValue()));
                }
                i = semi;
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }
}
