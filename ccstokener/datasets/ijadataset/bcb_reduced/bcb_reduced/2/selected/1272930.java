package org.xmlcml.noncml;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlcml.cml.AbstractBase;
import org.xmlcml.cml.AbstractStringVal;
import org.xmlcml.cml.AttributeSize;
import org.xmlcml.cml.CMLAtom;
import org.xmlcml.cml.CMLBond;
import org.xmlcml.cml.CMLBondStereo;
import org.xmlcml.cml.CMLDocument;
import org.xmlcml.cml.CMLException;
import org.xmlcml.cml.CMLList;
import org.xmlcml.cml.CMLMolecule;
import org.xmlcml.cmlimpl.AtomImpl;
import org.xmlcml.cmlimpl.BondImpl;
import org.xmlcml.cmlimpl.BondStereoImpl;
import org.xmlcml.cmlimpl.CMLBaseImpl;
import org.xmlcml.cmlimpl.Coord2;
import org.xmlcml.cmlimpl.ListImpl;
import org.xmlcml.cmlimpl.MoleculeImpl;
import org.xmlcml.cmlimpl.StringValImpl;
import org.xmlcml.cmlimpl.StringArrayImpl;
import uk.co.demon.ursus.dom.PMRDelegate;
import uk.co.demon.ursus.dom.PMRElement;
import uk.co.demon.ursus.dom.PMRNode;
import jumbo.xml.util.Util;

/** class to read formatted files
@author (C) P. Murray-Rust, 2000
*/
public class PreStyleImpl extends NonCMLDocumentImpl implements PreStyle {

    URL parserUrl;

    LineMatcher[] lineMatchers;

    public PreStyleImpl() {
    }

    /** Convenience: form a PreStyle object from a local file
@exception Exception file was not a standard PreStyle file
*/
    public PreStyleImpl(BufferedReader bReader, String id) throws Exception {
        createAndAddCMLElement(FORMAT, id);
        parse(bReader);
    }

    public void setParserUrl(URL parserUrl) {
        this.parserUrl = parserUrl;
    }

    /** form a PreStyle object from a CML file
*/
    public PreStyleImpl(CMLMolecule outputCMLMolecule) {
        setOutputCMLMolecule(outputCMLMolecule);
    }

    public void parse(BufferedReader bReader) throws IOException, CMLException {
        this.bReader = bReader;
        if (inputCML == null) createAndAddCMLElement(FORMAT, "");
        try {
            LineMatcher.readMatchers(parserUrl);
            lineMatchers = LineMatcher.lineMatchers;
            System.out.println("LineMatchers: " + lineMatchers.length);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CMLException("" + e);
        }
        Vector lineVector = new Vector();
        while (true) {
            String line = peekLine();
            if (line == null) break;
            line = this.getCurrentLine();
            line = Util.rightTrim(line);
            lineVector.addElement(line);
        }
        for (int i = 0; i < lineVector.size(); i++) {
            StringValImpl sv = null;
            String line = (String) lineVector.elementAt(i);
            System.out.println("[" + line + "]");
            for (int j = 0; j < lineMatchers.length; j++) {
                if (!lineMatchers[j].match(line, 0)) continue;
                boolean matched = true;
                int lineCount = lineMatchers[j].nLines;
                for (int k = 1; k < lineCount; k++) {
                    if (i + k >= lineVector.size()) {
                        matched = false;
                        break;
                    }
                    String nextLine = (String) lineVector.elementAt(i + k);
                    if (j == 0) System.out.println(">>>>>" + nextLine + "<<<<<");
                    if (j == 0) System.out.println("<<<<<" + lineMatchers[j].lines[k] + ">>>>>");
                    if (!lineMatchers[j].match(nextLine, k)) {
                        matched = false;
                        break;
                    }
                }
                if (matched) {
                    String s = "";
                    for (int k = 0; k < lineCount; k++) {
                        s += (String) lineVector.elementAt(i + k);
                        if (k < lineCount - 1) s += "\n";
                    }
                    sv = new StringValImpl(this, s, null);
                    sv.setTitle(lineMatchers[j].title);
                    i += lineCount;
                    System.out.print("+");
                    break;
                }
            }
            if (sv == null) {
                sv = new StringValImpl(this, line, null);
                sv.setTitle(((line.equals("")) ? "BLANK" : "??"));
                System.out.print(".");
            }
            inputCML.appendChild(sv);
        }
        System.out.println("PreStyle finished...");
    }

    public String output(Writer w) {
        return "NOT IMPLEMENTED";
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java PreStyleImpl inputfile");
            System.exit(0);
        }
        PreStyle format = null;
        try {
            URL url = new URL(Util.makeAbsoluteURL(args[0]));
            BufferedReader bReader = new BufferedReader(new InputStreamReader(url.openStream()));
            int idx = args[0].indexOf(".");
            String id = (idx == -1) ? args[0] : args[0].substring(0, idx);
            idx = id.lastIndexOf("\\");
            if (idx != -1) id = id.substring(idx + 1);
            format = new PreStyleImpl(bReader, id);
        } catch (Exception e) {
            System.out.println("PreStyle failed: " + e);
            e.printStackTrace();
            System.exit(0);
        }
    }
}

class LineMatcher {

    PMRElement element;

    String[] lines = null;

    int nLines;

    String title;

    static LineMatcher lineMatchers[];

    public LineMatcher() {
    }

    public LineMatcher(PMRElement elem) {
        this.element = elem;
        String s = PMRDelegate.getPCDATAContent(elem);
        title = elem.getAttribute("title");
        Vector v = new Vector();
        int count = 0;
        while (true) {
            int idx = s.indexOf("\n");
            if (idx == -1) {
                v.addElement(s);
                break;
            }
            String ss = s.substring(0, idx);
            if (count++ == 0 && ss.equals("")) {
            } else {
                v.addElement(ss);
            }
            s = s.substring(idx + 1);
        }
        nLines = v.size();
        if (v.elementAt(nLines - 1).equals("")) nLines--;
        lines = new String[nLines];
        for (int i = 0; i < nLines; i++) {
            lines[i] = Util.rightTrim((String) v.elementAt(i));
        }
        for (int i = 0; i < nLines; i++) {
            System.out.print(lines[i] + "/");
        }
        System.out.println();
    }

    public boolean match(String line, int nLine) {
        return (line.equals(lines[nLine]));
    }

    public static void readMatchers(URL parserUrl) throws Exception {
        CMLDocument format = PreStyleImpl.DOCUMENT_FACTORY.createDocument();
        format.parse(parserUrl);
        NodeList list = format.getDocumentElement().getElementsByTagName("lineMatch");
        lineMatchers = new LineMatcher[list.getLength()];
        for (int i = 0; i < list.getLength(); i++) {
            lineMatchers[i] = new LineMatcher((PMRElement) list.item(i));
        }
    }
}
