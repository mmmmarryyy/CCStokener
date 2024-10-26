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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlcml.cml.CMLAtom;
import org.xmlcml.cml.CMLBond;
import org.xmlcml.cml.CML;
import org.xmlcml.cml.AbstractBase;
import org.xmlcml.cml.CMLException;
import org.xmlcml.cml.CMLList;
import org.xmlcml.cml.CMLMolecule;
import org.xmlcml.cml.CMLStringVal;
import org.xmlcml.cmlimpl.AtomImpl;
import org.xmlcml.cmlimpl.BondImpl;
import org.xmlcml.cmlimpl.CMLBaseImpl;
import org.xmlcml.cmlimpl.CMLImpl;
import org.xmlcml.cmlimpl.ListImpl;
import org.xmlcml.cmlimpl.StringValImpl;
import org.xmlcml.cmlimpl.subset.SpanningTreeImpl;
import org.xmlcml.molutil.ChemicalElement;
import org.xmlcml.cml.subset.SpanningTree;
import jumbo.euclid.Point3;
import jumbo.euclid.Real2;
import uk.co.demon.ursus.dom.PMRDelegate;
import uk.co.demon.ursus.dom.PMRElementImpl;
import uk.co.demon.ursus.dom.PMRNode;
import jumbo.xml.util.Util;

/** class to read and write SDFiles

@author (C) P. Murray-Rust, 1996, 1998, 2000
*/
public class SDFImpl extends NonCMLDocumentImpl implements SDF {

    MDLMol mdlMol;

    String id;

    public SDFImpl() {
    }

    /** form a SDF object from a local file
@exception Exception file was not a standard SDF file
*/
    public SDFImpl(BufferedReader bReader, String id) throws IOException, CMLException {
        createCMLElement(SDF, id);
        this.id = id;
        parse(bReader);
    }

    /** form a SDF object from a CML file
*/
    public SDFImpl(CML outputCML) {
        setOutputCML(outputCML);
    }

    public void parse(BufferedReader bReader) throws IOException, CMLException {
        this.bReader = bReader;
        mdlMol = new MDLMolImpl(bReader, id);
        readData();
        ((CMLBaseImpl) inputCML).updateDOM(true);
    }

    void readData() throws IOException, CMLException {
        CMLList list = null;
        while (true) {
            String line = bReader.readLine();
            if (line.length() >= 4 && line.substring(0, 4).equals("$$$$")) break;
            if (line.length() >= 1 && line.substring(0, 1).equals(">")) {
                list = new ListImpl(this);
                list.setAttribute(AbstractBase.TITLE, DATA_HEADER);
                list.setAttribute(AbstractBase.CONVENTION, MDLMOL);
                inputCML.appendChild(list);
                CMLStringVal stringVal = new StringValImpl(this);
                stringVal.setStringValue(line);
                list.appendChild(stringVal);
                stringVal.setAttribute(AbstractBase.TITLE, "fields");
            } else {
                if (list == null) throw new CMLException("Possible irregular SDF datafields:" + line + ":");
                CMLStringVal stringVal = new StringValImpl(this);
                stringVal.setStringValue(line);
                list.appendChild(stringVal);
            }
        }
    }

    /** outputs CML as an SDF if possible. This is NOT
a faithful representation as I haven't read the spec completely
@param Writer writer to output it to
*/
    public String output(Writer writer) throws CMLException, IOException {
        if (outputCML == null) throw new CMLException("No CML to output");
        StringWriter w = new StringWriter();
        PMRDelegate.outputEventStream(outputCML, w, PMRNode.PRETTY, 0);
        if (mdlMol != null) mdlMol.output(writer);
        writeData(writer);
        return writer.toString();
    }

    public String writeData(Writer writer) throws CMLException, IOException {
        NodeList childNodes = outputCML.getElementsByTagName(AbstractBase.ELEMENT_NAMES[AbstractBase.LIST]);
        if (childNodes != null) {
            for (int i = 0; i < childNodes.getLength(); i++) {
                output(writer, (CMLList) childNodes.item(i));
            }
        }
        return writer.toString();
    }

    public String writeFieldData(Writer writer) throws CMLException, IOException {
        NodeList childNodes = outputCMLMolecule.getElementsByTagName(AbstractBase.ELEMENT_NAMES[AbstractBase.LIST]);
        if (childNodes != null) {
            for (int i = 0; i < childNodes.getLength(); i++) {
                output(writer, (CMLList) childNodes.item(i));
            }
        }
        return writer.toString();
    }

    void output(Writer writer, CMLList list) throws CMLException, IOException {
        if (!(list.getAttribute(AbstractBase.CONVENTION).equals(MDLMOL))) return;
        if (!(list.getAttribute(AbstractBase.TITLE).equals(DATA_HEADER))) return;
        NodeList childNodes = list.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (!(child instanceof CMLStringVal)) return;
            CMLStringVal stringVal = (CMLStringVal) child;
            String value = stringVal.getStringValue();
            if (i == 0) {
                if (value.length() < 1 || !(value.substring(0, 1).equals(">"))) return;
            }
            writer.write(value + "\n");
        }
    }

    public CMLMolecule getMolecule() {
        return (mdlMol != null) ? mdlMol.getMolecule() : null;
    }

    public static void splitSDF(BufferedReader bReader, String idBase) throws IOException, CMLException {
        int count = 0;
        boolean domore = true;
        SDF sdf;
        while (domore) {
            String safeid = idBase + (++count);
            try {
                sdf = new SDFImpl(bReader, safeid);
            } catch (Exception ex) {
                domore = false;
                break;
            }
            CMLMolecule mol = sdf.getMolecule();
            if (mol == null) break;
            String id = mol.getAttribute("title");
            if ((id == null) || id.equals("")) {
                id = safeid;
            }
            Writer w = new OutputStreamWriter(new FileOutputStream(id + ".xml"));
            PMRDelegate.outputEventStream(mol, w, PMRNode.PRETTY, 0);
            w.close();
            w = new OutputStreamWriter(new FileOutputStream(id + ".mol"));
            try {
                MDLMol mdlMol = new MDLMolImpl();
                mdlMol.setOutputCMLMolecule(mol);
                mdlMol.output(w);
                w.close();
            } catch (CMLException cmle) {
                break;
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java org.xmlcml.noncml.SDFImpl inputfile");
            System.exit(0);
        }
        SDF sdf = null;
        try {
            URL url = new URL(Util.makeAbsoluteURL(args[0]));
            BufferedReader bReader = new BufferedReader(new InputStreamReader(url.openStream()));
            int idx = args[0].indexOf(".");
            String id = (idx == -1) ? args[0] : args[0].substring(0, idx);
            idx = id.lastIndexOf("\\");
            if (idx != -1) id = id.substring(idx + 1);
            SDFImpl.splitSDF(bReader, id);
        } catch (Exception e) {
            System.out.println("SDF failed: " + e);
            e.printStackTrace();
            System.exit(0);
        }
    }
}

;
