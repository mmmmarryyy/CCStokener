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
import org.xmlcml.cml.CMLAtom;
import org.xmlcml.cml.CMLBond;
import org.xmlcml.cml.CMLException;
import org.xmlcml.cml.CMLMolecule;
import org.xmlcml.cml.subset.SpanningTree;
import org.xmlcml.cml.normalise.NormalMolecule;
import org.xmlcml.cmlimpl.AtomImpl;
import org.xmlcml.cmlimpl.BondImpl;
import org.xmlcml.cmlimpl.Coord3;
import org.xmlcml.cmlimpl.MoleculeImpl;
import org.xmlcml.cmlimpl.subset.SpanningTreeImpl;
import jumbo.euclid.Point3;
import uk.co.demon.ursus.dom.PMRDelegate;
import uk.co.demon.ursus.dom.PMRNode;
import jumbo.xml.util.Util;

/** class to read and write xyz files (Peter Ertl's applet)
@author (C) P. Murray-Rust, 2000
*/
public class XYZImpl extends NonCMLDocumentImpl implements XYZ {

    Hashtable serialTable;

    public XYZImpl() {
        super();
    }

    /** form an XYZ object from a local file
@exception Exception file was not a standard XYZ file
*/
    public XYZImpl(BufferedReader bReader, String id) throws Exception {
        createAndAddMoleculeElement(XYZ, id);
        parse(bReader);
    }

    /** form a XYZ object from a CML file
*/
    public XYZImpl(CMLMolecule outputCMLMolecule) {
        setOutputCMLMolecule(outputCMLMolecule);
    }

    public void parse(BufferedReader bReader) throws IOException, CMLException {
        this.bReader = bReader;
        if (inputCMLMolecule == null) createAndAddMoleculeElement(XYZ, "");
        readHeader();
        readAtoms();
        ((MoleculeImpl) inputCMLMolecule).updateDOM(true);
    }

    void readHeader() throws IOException, CMLException {
        String title = "";
        String nn = "";
        String line = bReader.readLine();
        if (line == null) throw new CMLException("Unexpected EOF");
        line = line.trim();
        int idx = line.indexOf(" ");
        if (idx == -1) {
            nn = line;
        } else {
            nn = line.substring(0, idx);
            title = line.substring(idx + 1);
        }
        if (nn.equals("")) throw new CMLException("No atom count in first line");
        try {
            natoms = Integer.parseInt(nn);
        } catch (NumberFormatException nfe) {
            throw new CMLException("Bad atom count: " + line);
        }
        line = bReader.readLine();
        if (title.equals("")) title = line;
        if (!title.equals("")) inputCMLMolecule.setTitle(title);
    }

    void readAtoms() throws IOException, CMLException {
        for (int i = 0; i < natoms; i++) {
            String line = bReader.readLine() + "                                    ";
            CMLAtom atom = ATOM_FACTORY.createAtom(this);
            String id = "a" + (i + 1);
            atom.setId(id);
            inputCMLMolecule.addAtom(atom);
            StringTokenizer st = new StringTokenizer(line);
            if (st.countTokens() != 4) {
                throw new CMLException("XYZ atoms require 4 fields/line: " + line);
            }
            String elType = st.nextToken();
            atom.setElementType(elType);
            String s = "";
            try {
                double x = new Double(st.nextToken()).doubleValue();
                double y = new Double(st.nextToken()).doubleValue();
                double z = new Double(st.nextToken()).doubleValue();
                atom.setXYZ3(new Coord3(x, y, z));
            } catch (NumberFormatException nfe) {
                throw new CMLException("Bad coordinate: " + s);
            }
        }
    }

    /** outputs CMLMolecule as an YZ
@param Writer writer to output it to
*/
    public String output(Writer writer) throws CMLException, IOException {
        getAndCheckVectors();
        writeHeader(writer);
        writeAtoms(writer);
        return writer.toString();
    }

    public String writeHeader(Writer writer) throws CMLException, IOException {
        writer.write("" + natoms + " " + outputCMLMolecule.getTitle() + "\n");
        writer.write("\n");
        return writer.toString();
    }

    public String writeAtoms(Writer writer) throws CMLException, IOException {
        serialTable = new Hashtable();
        for (int i = 0; i < natoms; i++) {
            CMLAtom atom = (CMLAtom) atomVector.elementAt(i);
            String id = atom.getId();
            serialTable.put(id, "" + i);
            String elType = atom.getElementType();
            writer.write(("" + elType + "   ").substring(0, 2));
            Coord3 xyz = (Coord3) atom.getXYZ3();
            if (xyz == null) throw new CMLException("XYZ needs 3D coordinates");
            writer.write((" " + xyz.getArray()[0] + "     ").substring(0, 8));
            writer.write((" " + xyz.getArray()[1] + "     ").substring(0, 9));
            writer.write((" " + xyz.getArray()[2] + "     ").substring(0, 9));
            writer.write("\n");
        }
        return writer.toString();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java xyzImpl inputfile");
            System.exit(0);
        }
        XYZ xyz = null;
        try {
            URL url = new URL(Util.makeAbsoluteURL(args[0]));
            BufferedReader bReader = new BufferedReader(new InputStreamReader(url.openStream()));
            int idx = args[0].indexOf(".");
            String id = (idx == -1) ? args[0] : args[0].substring(0, idx);
            idx = id.lastIndexOf("\\");
            if (idx != -1) id = id.substring(idx + 1);
            xyz = new XYZImpl(bReader, id);
            CMLMolecule mol = xyz.getMolecule();
            StringWriter sw = new StringWriter();
            mol.debug(sw);
            System.out.println(sw.toString());
            SpanningTree sTree = new SpanningTreeImpl(mol);
            System.out.println(sTree.toSMILES());
            Writer w = new OutputStreamWriter(new FileOutputStream(id + ".xml"));
            PMRDelegate.outputEventStream(mol, w, PMRNode.PRETTY, 0);
            w.close();
            w = new OutputStreamWriter(new FileOutputStream(id + "-new.mol"));
            xyz.setOutputCMLMolecule(mol);
            xyz.output(w);
            w.close();
        } catch (Exception e) {
            System.out.println("xyz failed: " + e);
            e.printStackTrace();
            System.exit(0);
        }
    }
}
