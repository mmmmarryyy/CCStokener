package com.trackerdogs.websources.results;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import com.trackerdogs.search.*;
import com.trackerdogs.websources.*;
import com.trackerdogs.tools.*;
import org.w3c.tidy.*;
import org.w3c.dom.*;

/**********************************************************************
 * This class extracts fragments of information out of a page. A
 * Wrapper is unique for each Web Source.
 * 
 * @author Koen Witters
 *
 * @version $Header: /cvsroot/trackerdogs/trackerdogs/src/com/trackerdogs/websources/results/Wrapper.java,v 1.7 2002/08/16 10:11:03 kwitters Exp $
 */
public class Wrapper implements Serializable {

    private String beginOfResult_, endOfResult_;

    private ResultWrapper resultWrapper_;

    private boolean logging;

    public static Wrapper generateWrapper(URL url) {
        Wrapper w = new Wrapper(url);
        if (w.beginOfResult_ == null || w.beginOfResult_ == null) {
            return null;
        } else {
            return w;
        }
    }

    private Wrapper(URL url) {
        resultWrapper_ = new ResultWrapper();
        beginOfResult_ = null;
        endOfResult_ = null;
        try {
            Tidy tidy = new Tidy();
            tidy.setShowWarnings(false);
            tidy.setQuiet(true);
            InputStream is = url.openStream();
            System.out.print("Retrieving page...");
            String thePage = getString(new BufferedReader(new InputStreamReader(is)));
            System.out.print("done \nparsing...");
            NodeList body = tidy.parseDOM(new StringBufferInputStream(thePage), null).getElementsByTagName("body");
            NodeList nl = body.item(0).getChildNodes();
            System.out.print("done \nfinding train...");
            Date start = new Date();
            PatternSequence ps = new PatternSequence();
            ps.findSequence(nl);
            Train train = ps.getBestTrain();
            train.printTrain();
            System.out.print("done \nmatching train...");
            train.matchWith(thePage);
            System.out.println("done");
            Vector textPos = train.getPositionsInText();
            if (textPos != null && textPos.size() > 3) {
                for (int i = 1; i < textPos.size(); i++) {
                    System.out.println("\n====\n" + thePage.substring(((Integer) textPos.elementAt(i - 1)).intValue(), ((Integer) textPos.elementAt(i)).intValue()) + "\n====\n");
                }
                System.out.println(textPos.size());
                textPos.remove(0);
                beginOfResult_ = findCommonBegin(thePage, textPos);
                endOfResult_ = findCommonEnd(thePage, textPos);
                System.out.println("pre:  [" + beginOfResult_ + "]");
                System.out.println("post: [" + endOfResult_ + "]");
            } else if (textPos != null) {
                System.out.println("Train smaller than 4");
            } else {
                System.out.println("no train found");
            }
        } catch (MalformedURLException ex) {
            System.out.println("malformed url given:\n" + ex);
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    /************************************************************
     * Constructs a wrapper from an xml document
     *
     * @param root the root element "wrapper"
     */
    public Wrapper(Element root) throws XMLMarkupException {
        if (!root.getTagName().equals("wrapper")) {
            throw new XMLMarkupException("root tagname is " + root.getTagName() + " when should be <wrapper>");
        }
        beginOfResult_ = XmlTool.getElementCDATASectionValue("beginofresult", root);
        endOfResult_ = XmlTool.getElementCDATASectionValue("endofresult", root);
        resultWrapper_ = new ResultWrapper(XmlTool.getElement("resultwrapper", root));
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            try {
                Wrapper w = new Wrapper(new URL(args[0]));
            } catch (MalformedURLException ex) {
                System.out.println(ex);
            }
        } else {
            System.out.println("usage: Wrapper <arguments>");
        }
    }

    public static String getString(BufferedReader rd) {
        String str = new String(), line;
        try {
            while ((line = rd.readLine()) != null) {
                str += line + "\n";
            }
        } catch (IOException ex) {
            System.out.println(ex);
        }
        return str;
    }

    private String findCommonBegin(String text, Vector starts) {
        int maxLength = 99999;
        for (int i = 0; i < starts.size() - 1; i++) {
            maxLength = Math.min(maxLength, ((Integer) starts.elementAt(i + 1)).intValue() - ((Integer) starts.elementAt(i)).intValue());
        }
        starts = new Vector(starts);
        starts.remove(starts.size() - 1);
        boolean allSame = true;
        String pre = new String();
        int length = 0;
        char girl, leentje;
        while (allSame && (length < maxLength)) {
            int pos = ((Integer) starts.elementAt(0)).intValue();
            girl = text.charAt(pos + length);
            for (int i = 1; i < starts.size(); i++) {
                pos = ((Integer) starts.elementAt(i)).intValue();
                leentje = text.charAt(pos + length);
                if (girl != leentje) {
                    allSame = false;
                }
            }
            pre += girl;
            length++;
        }
        pre = pre.substring(0, pre.length() - 1);
        return pre;
    }

    private String findCommonEnd(String text, Vector starts) {
        starts.remove(0);
        boolean allSame = true;
        String post = new String();
        int length = 1;
        char girl, leentje;
        while (allSame) {
            int pos = ((Integer) starts.elementAt(0)).intValue();
            girl = text.charAt(pos - length);
            for (int i = 1; i < starts.size(); i++) {
                pos = ((Integer) starts.elementAt(i)).intValue();
                leentje = text.charAt(pos - length);
                if (girl != leentje) {
                    allSame = false;
                }
            }
            post = girl + post;
            length++;
        }
        post = post.substring(1);
        return post;
    }

    public void wrapResults(BufferedReader in, WebSourcePage wsPage) {
        try {
            boolean run = true;
            String result;
            while (run) {
                skipTextUntilNoCase(in, beginOfResult_);
                Result res = resultWrapper_.wrapResult(beginOfResult_ + getTextUntilNoCase(in, endOfResult_) + endOfResult_);
                wsPage.addResult(res);
            }
        } catch (IOException ex) {
        } catch (Exception ex) {
            assert false : "strange";
        }
    }

    public Vector resultBlocks(BufferedReader in) {
        Vector blocks = new Vector();
        try {
            boolean run = true;
            String result;
            while (run) {
                skipTextUntilNoCase(in, beginOfResult_);
                String res = beginOfResult_ + getTextUntilNoCase(in, endOfResult_) + endOfResult_;
                blocks.addElement(res);
            }
        } catch (IOException ex) {
        } catch (Exception ex) {
            assert false : "strange";
        }
        return blocks;
    }

    public static void skipTextUntilNoCase(BufferedReader in, String until) throws IOException {
        int pos = 0;
        int ch;
        char chr;
        boolean other = false;
        until = until.toLowerCase();
        boolean run = true;
        while (run) {
            ch = in.read();
            chr = Character.toLowerCase((char) ch);
            if (chr == '\r') {
                chr = (char) in.read();
            }
            if (until.charAt(pos) != chr) {
                if (other) {
                    other = false;
                    in.reset();
                }
                if (ch == -1) {
                    throw new IOException();
                }
                pos = 0;
            } else {
                pos++;
                if (pos == until.length()) {
                    return;
                }
                if (until.charAt(0) == chr && (pos > 1) && !other) {
                    in.mark(until.length());
                    other = true;
                }
            }
        }
    }

    public static String getTextUntilNoCase(Reader in, String until) throws IOException {
        int pos = 0;
        char chr;
        boolean other = false;
        String ret = new String();
        until = until.toLowerCase();
        boolean run = true;
        while (run) {
            chr = Character.toLowerCase((char) in.read());
            if (chr == '\r') {
                chr = (char) in.read();
            }
            ret += chr;
            if (until.charAt(pos) != chr) {
                if (other) {
                    other = false;
                    in.reset();
                }
                if (chr == ((char) -1)) {
                    throw new IOException();
                }
                pos = 0;
            } else {
                pos++;
                if (pos == until.length()) {
                    return ret.substring(0, ret.length() - until.length());
                }
                if (until.charAt(0) == chr && (pos > 1) && !other) {
                    in.mark(until.length());
                    other = true;
                }
            }
        }
        return null;
    }

    public String getBeginOfResult() {
        return beginOfResult_;
    }

    public String getEndOfResult() {
        return endOfResult_;
    }

    public ResultWrapper getResultWrapper() {
        return resultWrapper_;
    }

    public boolean isGenerated() {
        if (beginOfResult_ != null && endOfResult_ != null && resultWrapper_ != null) {
            return true;
        } else {
            return false;
        }
    }

    /************************************************************
     * Returns the root element of the XML representation of this
     * Wrapper.
     *
     * @param doc The root document
     *
     * @return the root element of the XML representation
     */
    public Element getXml(Document doc, int indent) {
        Element root = doc.createElement("wrapper");
        Element el;
        root.appendChild(XmlTool.getIndentNode(doc, 1, indent));
        el = doc.createElement("beginofresult");
        el.appendChild(doc.createCDATASection(beginOfResult_));
        root.appendChild(el);
        root.appendChild(XmlTool.getIndentNode(doc, 1, indent));
        el = doc.createElement("endofresult");
        el.appendChild(doc.createCDATASection(endOfResult_));
        root.appendChild(el);
        root.appendChild(XmlTool.getIndentNode(doc, 2, indent));
        el = resultWrapper_.getXml(doc, indent + 2);
        root.appendChild(el);
        root.appendChild(XmlTool.getIndentNode(doc, 1, indent - 2));
        return root;
    }

    public boolean equals(Object obj) {
        Wrapper w = (Wrapper) obj;
        return this.beginOfResult_.equals(w.beginOfResult_) && this.endOfResult_.equals(w.endOfResult_);
    }
}
