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
import org.xmlcml.cmlimpl.AtomImpl;
import org.xmlcml.cmlimpl.BondImpl;
import org.xmlcml.cmlimpl.Coord3;
import org.xmlcml.cmlimpl.MoleculeImpl;
import org.xmlcml.cmlimpl.subset.SpanningTreeImpl;
import org.xmlcml.cml.subset.SpanningTree;
import jumbo.euclid.Point3;
import uk.co.demon.ursus.dom.PMRDelegate;
import uk.co.demon.ursus.dom.PMRNode;
import jumbo.xml.util.Util;

/** class to read and write MOL2 files NYI
@author (C) P. Murray-Rust, 2000
*/
public class MOL2Impl extends NonCMLDocumentImpl implements MOL2 {

    Hashtable serialTable;

    String dict_type;

    String dict_name;

    String mol_type;

    String charge_type;

    int natoms = 0;

    int nbonds = 0;

    int nsubst = 0;

    int nfeat = 0;

    int nset = 0;

    public MOL2Impl() {
        super();
    }

    /** form an MOL2 object from a local file
@exception Exception file was not a standard MOL2 file
*/
    public MOL2Impl(BufferedReader bReader, String id) throws Exception {
        parse(bReader);
    }

    /** form a MOL2 object from a CML file
*/
    public MOL2Impl(CMLMolecule outputCMLMolecule) {
        setOutputCMLMolecule(outputCMLMolecule);
    }

    public void parse(BufferedReader bReader) throws IOException, CMLException {
        this.bReader = bReader;
        createAndAddMoleculeElement(MOL2, "");
        String line = null;
        while (true) {
            if (line == null) line = readLine();
            if (line == null) break;
            line = line.toUpperCase();
            if (false) {
            } else if (line.startsWith("@<TRIPOS>ATOM")) {
                readAtoms();
                line = null;
            } else if (line.startsWith("@<TRIPOS>BOND")) {
                readBonds();
                line = null;
            } else if (line.startsWith("@<TRIPOS>CRYSIN")) {
                readCryst();
                line = null;
            } else if (line.startsWith("@<TRIPOS>DICT")) {
                readDict();
                line = null;
            } else if (line.startsWith("@<TRIPOS>MOLECULE")) {
                readMolecule();
                if (!mol_type.equalsIgnoreCase("SMALL")) {
                    throw new CMLException("Only small molecules implemented so far, sorry!");
                }
                line = null;
            } else if (line.startsWith("@<TRIPOS>SUBSTRUCTURE")) {
                readSubstructure();
                line = null;
            } else if (line.startsWith("@<TRIPOS>SET")) {
                readSet();
                line = null;
            } else if (line.startsWith("@<TRIPOS>")) {
                while (true) {
                    line = readLine();
                    if (line == null) break;
                    line = line.toUpperCase();
                    if (line.startsWith("@<TRIPOS>")) break;
                }
                if (line == null) break;
            } else {
                throw new CMLException("Unexpected/unsupported line: " + line);
            }
        }
        ((MoleculeImpl) inputCMLMolecule).updateDOM(true);
    }

    String readLine() throws IOException {
        String line0 = "";
        String line = "";
        while (true) {
            line = getCurrentLine();
            if (line == null) break;
            if (line.startsWith("#")) continue;
            if (line.trim().equals("")) continue;
            if (line.trim().endsWith("\\")) {
                int idx = line.lastIndexOf("\\");
                line0 += line.substring(0, idx);
            } else {
                break;
            }
        }
        return (line0.equals("")) ? line : line0 + line;
    }

    void readAtoms() throws IOException, CMLException {
        if (natoms < 0) {
            throw new CMLException("MOLECULE records not previously given");
        }
        for (int i = 0; i < natoms; i++) {
            String line = readLine();
            if (line == null) throw new CMLException("Unexpected EOF");
            StringTokenizer st = new StringTokenizer(line);
            int nFields = st.countTokens();
            if (nFields < 6) throw new CMLException("Too few fields for atom: " + line);
            CMLAtom atom = ATOM_FACTORY.createAtom(this);
            String atomId = st.nextToken();
            atom.setId(atomId);
            inputCMLMolecule.addAtom(atom);
            String atomName = st.nextToken();
            atom.setTitle(atomName);
            try {
                double x = new Double(st.nextToken()).doubleValue();
                double y = new Double(st.nextToken()).doubleValue();
                double z = new Double(st.nextToken()).doubleValue();
                Coord3 xyz3 = new Coord3(x, y, z);
                atom.setXYZ3(xyz3);
            } catch (NumberFormatException nfe) {
                throw new CMLException("Bad x/y/z coord in: " + line);
            }
            String atomType = st.nextToken();
            int idx = atomType.indexOf(".");
            String elType = (idx == -1) ? atomType : atomType.substring(0, idx);
            atom.setElementType(elType);
            String subst_id = (nFields >= 7) ? st.nextToken() : null;
            String subst_name = (nFields >= 8) ? st.nextToken() : null;
            String charge = (nFields >= 9) ? st.nextToken() : null;
            String status_bits = (nFields >= 10) ? st.nextToken() : null;
        }
    }

    void readBonds() throws IOException, CMLException {
        if (nbonds < 0) {
            throw new CMLException("MOLECULE records not previously given");
        }
        for (int i = 0; i < nbonds; i++) {
            String line = readLine();
            if (line == null) throw new CMLException("Unexpected EOF");
            StringTokenizer st = new StringTokenizer(line);
            int nFields = st.countTokens();
            if (nFields < 4) throw new CMLException("Too few fields for bond: " + line);
            String bondId = st.nextToken();
            String atRef1 = st.nextToken();
            String atRef2 = st.nextToken();
            String bondType = st.nextToken();
            CMLAtom atom1 = inputCMLMolecule.getAtomById(atRef1);
            if (atom1 == null) throw new CMLException("Cannot resolve atomRef :" + atRef1 + ": in " + line);
            CMLAtom atom2 = inputCMLMolecule.getAtomById(atRef2);
            if (atom2 == null) throw new CMLException("Cannot resolve atomRef :" + atRef2 + ": in " + line);
            String order = cmlBondOrder(bondType);
            CMLBond bond = BOND_FACTORY.createBond(atom1, atom2);
            inputCMLMolecule.addBond(bond);
            bond.setId(bondId);
            bond.setOrder(order);
        }
    }

    void readCryst() throws IOException, CMLException {
        String line = readLine();
        if (line == null) throw new CMLException("Unexpected EOF");
        StringTokenizer st = new StringTokenizer(line);
        if (st.countTokens() != 8) throw new CMLException("Bad CYSIN record");
    }

    void readDict() throws IOException, CMLException {
        String line = readLine();
        if (line == null) throw new CMLException("Unexpected EOF");
        StringTokenizer st = new StringTokenizer(line);
        if (st.countTokens() != 2) throw new CMLException("Bad DICT record");
        this.dict_type = st.nextToken();
        this.dict_name = st.nextToken();
    }

    void readMolecule() throws IOException, CMLException {
        natoms = -1;
        nbonds = -1;
        nsubst = 0;
        nfeat = 0;
        nset = 0;
        String line = readLine();
        if (line == null) throw new CMLException("Unexpected EOF");
        inputCMLMolecule.setTitle(line);
        line = readLine();
        if (line == null) throw new CMLException("Unexpected EOF");
        int[] values = grabInt(line);
        if (values.length > 0) natoms = values[0];
        if (values.length > 1) nbonds = values[1];
        if (values.length > 2) nsubst = values[2];
        if (values.length > 3) nfeat = values[3];
        if (values.length > 4) nset = values[4];
        line = readLine();
        if (line == null) throw new CMLException("Unexpected EOF");
        mol_type = line.toUpperCase();
        line = readLine();
        if (line == null) throw new CMLException("Unexpected EOF");
        charge_type = line.toUpperCase();
        line = readLine();
        if (line == null) throw new CMLException("Unexpected EOF");
        if (line.startsWith("@<TRIPOS>")) return;
        String status_bits = line.toUpperCase();
        line = readLine();
        if (line == null) throw new CMLException("Unexpected EOF");
        if (line.startsWith("@<TRIPOS>")) return;
        String mol_comment = line;
    }

    int[] grabInt(String line) throws CMLException {
        StringTokenizer st = new StringTokenizer(line);
        int ntok = st.countTokens();
        int value[] = new int[ntok];
        for (int i = 0; i < ntok; i++) {
            try {
                value[i] = Integer.parseInt(st.nextToken());
            } catch (NumberFormatException nfe) {
                throw new CMLException("Bad integers in: " + line);
            }
        }
        return value;
    }

    void readSet() throws IOException, CMLException {
        for (int i = 0; i < nset; i++) {
            String line = readLine();
            if (line == null) throw new CMLException("Unexpected EOF");
            line = readLine();
            if (line == null) throw new CMLException("Unexpected EOF");
        }
    }

    void readSubstructure() throws IOException, CMLException {
        for (int i = 0; i < nsubst; i++) {
            String line = readLine();
            if (line == null) throw new CMLException("Unexpected EOF");
        }
    }

    /** translates "CML"-MOL codes into MOL2 numbers
*/
    public static String molBondOrder(String cmlCode) {
        if (cmlCode == null) return "un";
        if (cmlCode.equals(CMLBond.SINGLE)) return "1";
        if (cmlCode.equals(CMLBond.DOUBLE)) return "2";
        if (cmlCode.equals(CMLBond.TRIPLE)) return "3";
        if (cmlCode.equals(CMLBond.AROMATIC)) return "ar";
        return "1";
    }

    /** translates MOL2 bond orders into JUMBO-MOL orders
*/
    public static String cmlBondOrder(String bondType) {
        if (bondType.equals("1")) return CMLBond.SINGLE;
        if (bondType.equals("2")) return CMLBond.DOUBLE;
        if (bondType.equals("3")) return CMLBond.TRIPLE;
        if (bondType.equals("ar")) return CMLBond.AROMATIC;
        if (bondType.equals("un")) return null;
        return CMLBond.SINGLE;
    }

    /** outputs CMLMolecule as an YZ
@param Writer writer to output it to
*/
    public String output(Writer writer) throws CMLException, IOException {
        if (outputCMLMolecule == null) throw new CMLException("No CMLMolecule to output");
        return writer.toString();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java MOL2Impl inputfile");
            System.exit(0);
        }
        MOL2 mol2 = null;
        try {
            URL url = new URL(Util.makeAbsoluteURL(args[0]));
            BufferedReader bReader = new BufferedReader(new InputStreamReader(url.openStream()));
            int idx = args[0].indexOf(".");
            String id = (idx == -1) ? args[0] : args[0].substring(0, idx);
            idx = id.lastIndexOf("\\");
            if (idx != -1) id = id.substring(idx + 1);
            mol2 = new MOL2Impl(bReader, id);
            CMLMolecule mol = mol2.getMolecule();
            StringWriter sw = new StringWriter();
            mol.debug(sw);
            System.out.println(sw.toString());
            SpanningTree sTree = new SpanningTreeImpl(mol);
            System.out.println(sTree.toSMILES());
            Writer w = new OutputStreamWriter(new FileOutputStream(id + ".xml"));
            PMRDelegate.outputEventStream(mol, w, PMRNode.PRETTY, 0);
            w.close();
            w = new OutputStreamWriter(new FileOutputStream(id + "-new.mol"));
            mol2.setOutputCMLMolecule(mol);
            mol2.output(w);
            w.close();
        } catch (Exception e) {
            System.out.println("MOL2 failed: " + e);
            e.printStackTrace();
            System.exit(0);
        }
    }
}
