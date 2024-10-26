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
import org.xmlcml.cml.CMLBondStereo;
import org.xmlcml.cml.CMLException;
import org.xmlcml.cml.CMLMolecule;
import org.xmlcml.cmlimpl.AtomImpl;
import org.xmlcml.cmlimpl.BondImpl;
import org.xmlcml.cmlimpl.BondStereoImpl;
import org.xmlcml.cmlimpl.Coord2;
import org.xmlcml.cmlimpl.MoleculeImpl;
import org.xmlcml.cml.subset.SpanningTree;
import org.xmlcml.cmlimpl.subset.SpanningTreeImpl;
import uk.co.demon.ursus.dom.PMRDelegate;
import uk.co.demon.ursus.dom.PMRNode;
import jumbo.xml.util.Util;

/** class to read and write JME files (Peter Ertl's applet)
@author (C) P. Murray-Rust, 2000
*/
public class JMEImpl extends NonCMLDocumentImpl implements JME {

    Hashtable serialTable;

    public JMEImpl() {
    }

    /** Convenience: form a JME object from a local file
@exception Exception file was not a standard JME file
*/
    public JMEImpl(BufferedReader bReader, String id) throws Exception {
        createAndAddMoleculeElement(JME, id);
        parse(bReader);
    }

    /** form a JME object from a CML file
*/
    public JMEImpl(CMLMolecule outputCMLMolecule) {
        setOutputCMLMolecule(outputCMLMolecule);
    }

    public void parse(BufferedReader bReader) throws IOException, CMLException {
        this.bReader = bReader;
        if (inputCMLMolecule == null) createAndAddMoleculeElement(JME, "");
        String line = getCurrentLine();
        StringTokenizer st = new StringTokenizer(line);
        readHeader(st);
        readAtoms(st);
        readBonds(st);
        readFooter(st);
        ((MoleculeImpl) inputCMLMolecule).updateDOM(true);
    }

    void readHeader(StringTokenizer st) throws IOException, CMLException {
        natoms = Integer.parseInt(st.nextToken());
        nbonds = Integer.parseInt(st.nextToken());
    }

    void readAtoms(StringTokenizer st) throws IOException, CMLException {
        serialTable = new Hashtable();
        for (int i = 0; i < natoms; i++) {
            CMLAtom atom = ATOM_FACTORY.createAtom(this);
            String id = "a" + (i + 1);
            atom.setId(id);
            serialTable.put(id, atom);
            inputCMLMolecule.addAtom(atom);
            String elType = st.nextToken();
            int charge = 0;
            int len = elType.length();
            while (true) {
                if (elType.endsWith("+")) {
                    charge++;
                    elType = elType.substring(0, --len);
                } else if (elType.endsWith("-")) {
                    charge--;
                    elType = elType.substring(0, --len);
                } else {
                    break;
                }
            }
            atom.setElementType(elType);
            if (charge != 0) atom.setFormalCharge(charge);
            String s = "";
            try {
                s = st.nextToken();
                double x = new Double(s).doubleValue();
                s = st.nextToken();
                double y = new Double(s).doubleValue();
                atom.setXY2(new Coord2(x, y));
            } catch (NumberFormatException nfe) {
                throw new CMLException("Bad coordinate: " + s);
            }
        }
    }

    void readBonds(StringTokenizer st) throws IOException, CMLException {
        for (int i = 0; i < nbonds; i++) {
            String atRef1 = "a" + st.nextToken();
            CMLAtom atom1 = inputCMLMolecule.getAtomById(atRef1);
            if (atom1 == null) throw new CMLException("Cannot resolve atomRef :" + atRef1 + ": in " + currentLine);
            String atRef2 = "a" + st.nextToken();
            CMLAtom atom2 = inputCMLMolecule.getAtomById(atRef2);
            if (atom2 == null) throw new CMLException("Cannot resolve atomRef :" + atRef2 + ": in " + currentLine);
            CMLBond bond = BOND_FACTORY.createBond(atom1, atom2);
            inputCMLMolecule.addBond(bond);
            bond.setId("b" + (i + 1));
            int order = Integer.parseInt(st.nextToken());
            if (order > 0) {
                bond.setOrder(cmlBondOrder(order));
            } else {
                bond.setOrder(cmlBondOrder(1));
                bond.setStereo(new BondStereoImpl(cmlBondStereo(order)), null);
            }
        }
    }

    void readFooter(StringTokenizer st) throws IOException, CMLException {
    }

    /** translates "CML" codes into jme numbers
*/
    public static int jmeBondOrder(String cmlCode) {
        if (cmlCode == null) return 1;
        if (cmlCode.equals(CMLBond.SINGLE)) return 1;
        if (cmlCode.equals(CMLBond.DOUBLE)) return 2;
        if (cmlCode.equals(CMLBond.TRIPLE)) return 3;
        return 1;
    }

    /** translates jme bond orders into JUMBO-MOL orders
*/
    public static String cmlBondOrder(int molNumber) {
        if (molNumber == 1) return CMLBond.SINGLE;
        if (molNumber == 2) return CMLBond.DOUBLE;
        if (molNumber == 3) return CMLBond.TRIPLE;
        return CMLBond.SINGLE;
    }

    /** translates JUMBO-MOL codes into jme numbers
*/
    public static int jmeBondStereo(String cmlCode) {
        if (cmlCode == null) return 0;
        if (cmlCode.equals(CMLBond.WEDGE)) return -1;
        if (cmlCode.equals(CMLBond.HATCH)) return -2;
        return 0;
    }

    /** translates jme numbers into JUMBO-MOL codes
*/
    public static String cmlBondStereo(int molNumber) {
        if (molNumber == -1) return CMLBond.WEDGE;
        if (molNumber == -2) return CMLBond.HATCH;
        return CMLBond.NOSTEREO;
    }

    /** outputs CMLMolecule as a JME
@param Writer writer to output it to
*/
    public String output(Writer writer) throws CMLException, IOException {
        getAndCheckVectors();
        writeHeader(writer);
        writeAtoms(writer);
        writeBonds(writer);
        writeFooter(writer);
        return writer.toString();
    }

    public String writeHeader(Writer writer) throws CMLException, IOException {
        writer.write("" + natoms);
        writer.write(" " + nbonds);
        return writer.toString();
    }

    public String writeAtoms(Writer writer) throws CMLException, IOException {
        serialTable = new Hashtable();
        for (int i = 0; i < natoms; i++) {
            CMLAtom atom = (CMLAtom) atomVector.elementAt(i);
            String id = atom.getId();
            serialTable.put(id, "" + (i + 1));
            String elType = atom.getElementType();
            writer.write(" " + elType);
            int charge = atom.getFormalCharge();
            String chString = "";
            if (charge > 0) {
                chString = "+";
            } else if (charge < 0) {
                chString = "-";
                charge = -charge;
            }
            for (int j = 0; j < charge; j++) {
                writer.write(chString);
            }
            Coord2 xy = (Coord2) atom.getXY2();
            if (xy == null) throw new CMLException("JME needs 2D coordinates");
            writer.write(" " + outform(xy.x, 8, 3).trim() + " " + outform(xy.y, 8, 3).trim());
        }
        return writer.toString();
    }

    public String writeBonds(Writer writer) throws CMLException, IOException {
        for (int i = 0; i < nbonds; i++) {
            CMLBond bond = (CMLBond) bondVector.elementAt(i);
            String atRef1 = bond.getAtom(0).getId();
            String atString1 = (String) serialTable.get(atRef1);
            if (atString1 == null) throw new CMLException("Cannot find atom: " + atRef1);
            writer.write(" " + atString1);
            String atRef2 = bond.getAtom(1).getId();
            String atString2 = (String) serialTable.get(atRef2);
            if (atString2 == null) throw new CMLException("Cannot find atom: " + atRef2);
            writer.write(" " + atString2);
            int order = jmeBondOrder(bond.getOrder());
            CMLBondStereo bs = bond.getStereo();
            int stereo = (bs == null) ? 0 : jmeBondStereo(bs.getStringValue());
            if (stereo == 0) {
                writer.write(" " + order);
            } else {
                writer.write(" " + stereo);
            }
        }
        return writer.toString() + "\n";
    }

    public String writeFooter(Writer writer) throws CMLException, IOException {
        return writer.toString();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java JMEImpl inputfile");
            System.exit(0);
        }
        JME jme = null;
        try {
            URL url = new URL(Util.makeAbsoluteURL(args[0]));
            BufferedReader bReader = new BufferedReader(new InputStreamReader(url.openStream()));
            int idx = args[0].indexOf(".");
            String id = (idx == -1) ? args[0] : args[0].substring(0, idx);
            idx = id.lastIndexOf("\\");
            if (idx != -1) id = id.substring(idx + 1);
            jme = new JMEImpl(bReader, id);
            CMLMolecule mol = jme.getMolecule();
            StringWriter sw = new StringWriter();
            mol.debug(sw);
            System.out.println(sw.toString());
            SpanningTree sTree = new SpanningTreeImpl(mol);
            System.out.println(sTree.toSMILES());
            Writer w = new OutputStreamWriter(new FileOutputStream(id + ".xml"));
            PMRDelegate.outputEventStream(mol, w, PMRNode.PRETTY, 0);
            w.close();
            w = new OutputStreamWriter(new FileOutputStream(id + "-new.mol"));
            jme.setOutputCMLMolecule(mol);
            jme.output(w);
            w.close();
        } catch (Exception e) {
            System.out.println("JME failed: " + e);
            e.printStackTrace();
            System.exit(0);
        }
    }
}
