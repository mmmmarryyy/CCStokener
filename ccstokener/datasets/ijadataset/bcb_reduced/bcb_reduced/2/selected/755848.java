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
import org.xmlcml.cml.CMLAtom;
import org.xmlcml.cml.CMLBond;
import org.xmlcml.cml.CMLBondStereo;
import org.xmlcml.cml.CMLException;
import org.xmlcml.cml.CMLList;
import org.xmlcml.cml.CMLMolecule;
import org.xmlcml.cmlimpl.AtomImpl;
import org.xmlcml.cmlimpl.BondImpl;
import org.xmlcml.cmlimpl.BondStereoImpl;
import org.xmlcml.cmlimpl.Coord2;
import org.xmlcml.cmlimpl.Coord3;
import org.xmlcml.cmlimpl.IsotopeImpl;
import org.xmlcml.cmlimpl.ListImpl;
import org.xmlcml.cmlimpl.MoleculeImpl;
import org.xmlcml.cmlimpl.subset.SpanningTreeImpl;
import org.xmlcml.molutil.ChemicalElement;
import org.xmlcml.cml.subset.SpanningTree;
import uk.co.demon.ursus.dom.PMRDelegate;
import uk.co.demon.ursus.dom.PMRElementImpl;
import uk.co.demon.ursus.dom.PMRNode;
import jumbo.xml.util.Util;

/** class to read (? and write?) mdl-molfiles as described in
chemical MIME.<P>
NOTE:  I have not been able to find any public statement of the
format and am unable to guarantee the correctness of any code
which interprets Mol files.

@author (C) P. Murray-Rust, 1996, 1998, 2000
*/
public class MDLMolImpl extends NonCMLDocumentImpl implements MDLMol {

    public static final String D2 = "2D";

    public static final String D3 = "3D";

    public static final String DUNK = "  ";

    int natomlists = 0;

    int chirflag = 0;

    int nprops = 0;

    protected String d2d3 = DUNK;

    Hashtable serialTable;

    public MDLMolImpl() {
        super();
    }

    /** form a MDLMol object from a local file
@exception Exception file was not a standard MDLMol file
*/
    public MDLMolImpl(BufferedReader bReader, String id) throws IOException, CMLException {
        createAndAddMoleculeElement(MDLMOL, id);
        parse(bReader);
    }

    /** form a MDLMol object from a CML file
*/
    public MDLMolImpl(CMLMolecule outputCMLMolecule) {
        setOutputCMLMolecule(outputCMLMolecule);
    }

    public void parse(BufferedReader bReader) throws IOException, CMLException {
        this.bReader = bReader;
        if (inputCMLMolecule == null) createAndAddMoleculeElement(MDLMOL, "");
        readHeader();
        readAtoms();
        readBonds();
        readFooter();
        ((MoleculeImpl) inputCMLMolecule).updateDOM(true);
    }

    void readHeader() throws IOException, CMLException {
        d2d3 = DUNK;
        String title = getCurrentLine();
        if (title == null) throw new CMLException("Null File");
        title = title.trim();
        if (!(title.equals(""))) inputCMLMolecule.setAttribute("title", title);
        String dim = "";
        String date = "";
        String line = getCurrentLine();
        if (!line.equals("")) {
            line += SPACE80;
            String user = line.substring(0, 2);
            String prog = line.substring(2, 10);
            date = line.substring(10, 20);
            d2d3 = line.substring(20, 22).trim();
        }
        inputCMLMolecule.setAttribute("type", d2d3);
        if (!date.trim().equals("")) {
            try {
                int y = Integer.parseInt(date.substring(4, 6).trim());
                String year = (y > 50) ? "" + (y + 1900) : "" + (y + 2000);
                String month = date.substring(0, 2).trim();
                String day = date.substring(2, 4).trim();
                Element dateElement = (Element) inputCMLMolecule.appendChild(new PMRElementImpl("date", this));
                dateElement.setAttribute("year", year);
                dateElement.setAttribute("month", month);
                dateElement.setAttribute("day", day);
            } catch (NumberFormatException nfe) {
            }
        }
        String comment = getCurrentLine();
        if (!comment.trim().equals("")) {
            Element element = (Element) inputCMLMolecule.appendChild(new PMRElementImpl("comment", this));
            PMRDelegate.setPCDATAContent(element, comment);
        }
        line = getCurrentLine() + SPACE40;
        natoms = parseInteger(line, 0, 3);
        nbonds = parseInteger(line, 3, 6);
        natomlists = parseInteger(line, 6, 9);
        chirflag = parseInteger(line, 9, 12);
        boolean chiral = (chirflag == 1);
        nprops = parseInteger(line, 30, 33);
    }

    int parseInteger(String s, int start, int end) throws CMLException {
        int i = 0;
        if (s != null && s.length() >= end) {
            s = s.substring(start, end).trim();
            int l = s.length();
            while (l > 1 && s.charAt(0) == '0') {
                s = s.substring(1);
                l--;
            }
            if (!(s.equals(""))) {
                try {
                    i = Integer.parseInt(s);
                } catch (NumberFormatException nfe) {
                    throw new CMLException("bad integer in: " + s);
                }
            }
        }
        return i;
    }

    CMLAtom[] atomArray;

    void readAtoms() throws IOException, CMLException {
        atomArray = new CMLAtom[natoms];
        double z2 = 0.0;
        for (int i = 0; i < natoms; i++) {
            String line = getCurrentLine() + SPACE80;
            double x = new Double(line.substring(0, 10).trim()).doubleValue();
            double y = new Double(line.substring(10, 20).trim()).doubleValue();
            double z = new Double(line.substring(20, 30).trim()).doubleValue();
            String elType = line.substring(31, 34).trim();
            int delta = parseInteger(line, 34, 36);
            int ch = parseInteger(line, 36, 39);
            int parity = parseInteger(line, 39, 42);
            int nhyd = parseInteger(line, 42, 45) - 1;
            int field5 = parseInteger(line, 45, 48);
            int oxState = parseInteger(line, 48, 51);
            CMLAtom atom = ATOM_FACTORY.createAtom(this);
            atomArray[i] = atom;
            atom.setId("a" + (i + 1));
            inputCMLMolecule.addAtom(atom);
            atom.setElementType(elType);
            if (ch != 0) atom.setFormalCharge(4 - ch);
            if (d2d3.equals(D2)) {
                Coord2 xy = new Coord2(x, y);
                atom.setXY2(xy);
            } else if (d2d3.equals(D2)) {
                Coord3 xyz = new Coord3(x, y, z);
                atom.setXYZ3(xyz);
            } else {
                Coord3 xyz = new Coord3(x, y, z);
                atom.setXYZ3(xyz);
            }
            if (delta != 0) {
                ChemicalElement chemEl = ChemicalElement.getElement(elType);
                int isotope = chemEl.getMainIsotope() + delta;
                atom.setIsotope(new IsotopeImpl(isotope));
            }
            if (nhyd > 1) atom.setHydrogenCount(nhyd - 1);
        }
    }

    void readBonds() throws IOException, CMLException {
        for (int i = 0; i < nbonds; i++) {
            String line = getCurrentLine() + SPACE40;
            int atInt1 = parseInteger(line, 0, 3);
            String atRef1 = "a" + atInt1;
            CMLAtom atom1 = inputCMLMolecule.getAtomById(atRef1);
            if (atom1 == null) throw new CMLException("Cannot resolve atomRef :" + atRef1 + ": in " + line);
            int atInt2 = parseInteger(line, 3, 6);
            String atRef2 = "a" + atInt2;
            CMLAtom atom2 = inputCMLMolecule.getAtomById(atRef2);
            if (atom2 == null) throw new CMLException("Cannot resolve atomRef :" + atRef2 + ": in " + line);
            String order = cmlBondOrder(parseInteger(line, 6, 9));
            String stereo = cmlStereoBond(parseInteger(line, 9, 12));
            CMLBond bond = BOND_FACTORY.createBond(atom1, atom2);
            inputCMLMolecule.addBond(bond);
            bond.setId("b" + (i + 1));
            bond.setOrder(order);
            if (!stereo.equals(CMLBond.NOSTEREO)) bond.setStereo(new BondStereoImpl(stereo), null);
        }
    }

    void readFooter() throws IOException, CMLException {
        while (true) {
            String line = getCurrentLine();
            if (line == null) {
                System.err.println("No M  END in properties; adding by guesswork");
                line = "M  END";
            }
            line = line.trim();
            if (line.equals("M  END")) {
                break;
            } else if (line.startsWith("M  CHG")) {
                ABVal[] abVals = readABVals(line);
                for (int i = 0; i < abVals.length; i++) {
                    int atNo = abVals[i].ab - 1;
                    int ch = abVals[i].val;
                    atomArray[atNo].setFormalCharge(ch);
                }
            } else if (line.startsWith("M  ISO")) {
                System.out.println("M  ISO not yet implemented... ");
            }
        }
    }

    ABVal[] readABVals(String line) throws CMLException {
        ABVal[] abVals;
        try {
            String s = line.substring(6);
            int nFields = Integer.parseInt(s.substring(0, 3).trim());
            s = s.substring(3);
            abVals = new ABVal[nFields];
            for (int i = 0; i < nFields; i++) {
                abVals[i] = new ABVal();
                abVals[i].ab = Integer.parseInt(s.substring(0, 4).trim());
                abVals[i].val = Integer.parseInt(s.substring(4, 8).trim());
                s = s.substring(8);
            }
        } catch (NumberFormatException nfe) {
            throw new CMLException("Bad line: " + line);
        }
        return abVals;
    }

    /** translates "CML"-MOL codes into MDL numbers
*/
    public static int molBondOrder(String cmlCode) {
        if (cmlCode == null) return 1;
        if (cmlCode.equals(CMLBond.SINGLE)) return 1;
        if (cmlCode.equals(CMLBond.DOUBLE)) return 2;
        if (cmlCode.equals(CMLBond.TRIPLE)) return 3;
        if (cmlCode.equals(CMLBond.AROMATIC)) return 4;
        return 1;
    }

    /** translates MDL bond orders into JUMBO-MOL orders
*/
    public static String cmlBondOrder(int molNumber) {
        if (molNumber == 1) return CMLBond.SINGLE;
        if (molNumber == 2) return CMLBond.DOUBLE;
        if (molNumber == 3) return CMLBond.TRIPLE;
        if (molNumber == 4) return CMLBond.AROMATIC;
        return CMLBond.SINGLE;
    }

    /** translates JUMBO-MOL codes into MDL numbers
*/
    public static int molBondStereo(String cmlCode) {
        if (cmlCode == null) return 0;
        if (cmlCode.equals(CMLBond.WEDGE)) return 1;
        if (cmlCode.equals(CMLBond.HATCH)) return 6;
        return 0;
    }

    /** translates MDL numbers into JUMBO-MOL codes
*/
    public static String cmlStereoBond(int molNumber) {
        if (molNumber == 1) return CMLBond.WEDGE;
        if (molNumber == 6) return CMLBond.HATCH;
        return CMLBond.NOSTEREO;
    }

    /** outputs CMLMolecule as an MDLMofile if possible. This is NOT
a faithful representation as I haven't read the spec completely
@param Writer writer to output it to
*/
    public String output(Writer writer) throws CMLException, IOException {
        getAndCheckVectors();
        atomVector = outputCMLMolecule.getAtomVector();
        if (atomVector == null) {
            throw new CMLException("No atoms to write to MDLMolfile");
        }
        natoms = atomVector.size();
        if (natoms > 999) throw new CMLException("Too many atoms for MDLMolfile: " + natoms);
        bondVector = outputCMLMolecule.getBondVector();
        if (bondVector == null) {
            nbonds = 0;
        } else {
            nbonds = bondVector.size();
        }
        d2d3 = outputCMLMolecule.getAttribute("type");
        writeHeader(writer);
        writeAtoms(writer);
        writeBonds(writer);
        writeFooter(writer);
        return writer.toString();
    }

    public String writeHeader(Writer writer) throws CMLException, IOException {
        CMLAtom atom = (CMLAtom) atomVector.elementAt(0);
        String title = outputCMLMolecule.getAttribute("title");
        writer.write("" + ((title == null) ? "" : title) + "\n");
        writer.write("  CML DOM ");
        writer.write("-date-");
        writer.write("time");
        writer.write(d2d3);
        writer.write("\n");
        writer.write("\n");
        writer.write(Util.outputInteger(3, natoms));
        writer.write(Util.outputInteger(3, nbonds));
        int charged = 0;
        for (int idx = 0; idx < natoms; idx++) {
            atom = (CMLAtom) atomVector.elementAt(idx);
            int fchrg = atom.getFormalCharge();
            if (fchrg != 0) {
                charged = charged + 1;
            }
        }
        int numprop = 1 + charged;
        String propstr;
        if (numprop < 10) {
            propstr = "  " + numprop;
        } else if (numprop < 100) {
            propstr = " " + numprop;
        } else {
            propstr = "" + numprop;
        }
        writer.write("  0  0  0  0  0  0  0  0" + propstr + " V2000\n");
        return writer.toString();
    }

    public String writeAtoms(Writer writer) throws CMLException, IOException {
        double x = 0;
        double y = 0;
        double z = 0;
        serialTable = new Hashtable();
        for (int i = 0; i < natoms; i++) {
            CMLAtom atom = (CMLAtom) atomVector.elementAt(i);
            if (d2d3.equals(D2)) {
                Coord2 xy = (Coord2) atom.getXY2();
                x = xy.x;
                y = xy.y;
                z = 0.0;
            } else {
                Coord3 xyz = (Coord3) atom.getXYZ3();
                x = xyz.getArray()[0];
                y = xyz.getArray()[1];
                z = xyz.getArray()[2];
            }
            writer.write(Util.outputFloat(10, 4, x));
            writer.write(Util.outputFloat(10, 4, y));
            writer.write(Util.outputFloat(10, 4, z));
            String elType = atom.getElementType();
            ChemicalElement chemEl = ChemicalElement.getElement(elType);
            writer.write((" " + elType + " ").substring(0, 3));
            String isoString = "  0";
            int isotope = 0;
            try {
                isotope = (int) atom.getIsotope().getIntegerValue();
            } catch (CMLException cme) {
            }
            ;
            if (isotope != 0 && chemEl != null) {
                chemEl = ChemicalElement.getElement(elType);
                int mainIsotope = chemEl.getMainIsotope();
                int delta = (mainIsotope > 0) ? isotope - mainIsotope : 0;
                isoString = (delta >= 0) ? "  " + delta : " " + delta;
            }
            writer.write(isoString);
            String chString = "   0";
            writer.write(chString);
            String parity = "  0";
            writer.write(parity);
            int nhyd = 0;
            try {
                nhyd = atom.getHydrogenCount();
            } catch (CMLException cme) {
            }
            String nhString = (nhyd > 0) ? "  " + (nhyd + 1) : "  0";
            writer.write(nhString);
            writer.write("  0");
            String oxState = "  0";
            writer.write(oxState);
            for (int j = 6; j < 12; j++) {
                writer.write("  0");
            }
            writer.write("\n");
            serialTable.put(atom.getId(), "" + (i + 1));
        }
        return writer.toString();
    }

    public String writeBonds(Writer writer) throws CMLException, IOException {
        for (int i = 0; i < nbonds; i++) {
            CMLBond bond = (CMLBond) bondVector.elementAt(i);
            String atRef1 = bond.getAtom(0).getId();
            String atString1 = (String) serialTable.get(atRef1);
            if (atString1 == null) throw new CMLException("Cannot find atom: " + atRef1);
            writer.write(Util.outputInteger(3, Integer.parseInt(atString1)));
            String atRef2 = bond.getAtom(1).getId();
            String atString2 = (String) serialTable.get(atRef2);
            if (atString2 == null) throw new CMLException("Cannot find atom: " + atRef2);
            writer.write(Util.outputInteger(3, Integer.parseInt(atString2)));
            writer.write("  " + molBondOrder(bond.getOrder()));
            CMLBondStereo bs = bond.getStereo();
            if (bs == null) {
                writer.write("  0");
            } else {
                writer.write("  " + molBondStereo(bs.getStringValue()));
            }
            writer.write("  0");
            writer.write("  0");
            writer.write("\n");
        }
        return writer.toString();
    }

    public String writeFooter(Writer writer) throws CMLException, IOException {
        int numthisline = 0;
        for (int idx = 0; idx < natoms; idx++) {
            CMLAtom atom = (CMLAtom) atomVector.elementAt(idx);
            int fchrg = atom.getFormalCharge();
            if (fchrg != 0) {
                String atnum;
                String chnum;
                int idxp = idx + 1;
                if (idxp < 10) {
                    atnum = "  " + idxp;
                } else if (idxp < 100) {
                    atnum = " " + idxp;
                } else {
                    atnum = "" + idxp;
                }
                if (fchrg > 0) {
                    if (fchrg < 10) {
                        chnum = "  " + fchrg;
                    } else {
                        chnum = " " + fchrg;
                    }
                } else {
                    if (fchrg > -10) {
                        chnum = " " + fchrg;
                    } else {
                        chnum = "" + fchrg;
                    }
                }
                writer.write("M  CHG  1 " + atnum + " " + chnum + "\n");
            }
        }
        writer.write("M  END\n");
        return writer.toString();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java org.xmlcml.noncml.MDLMolImpl inputfile");
            System.exit(0);
        }
        MDLMol mdl = null;
        try {
            URL url = new URL(Util.makeAbsoluteURL(args[0]));
            BufferedReader bReader = new BufferedReader(new InputStreamReader(url.openStream()));
            int idx = args[0].indexOf(".");
            String id = (idx == -1) ? args[0] : args[0].substring(0, idx);
            idx = id.lastIndexOf("\\");
            if (idx != -1) id = id.substring(idx + 1);
            mdl = new MDLMolImpl(bReader, id);
            CMLMolecule mol = mdl.getMolecule();
            StringWriter sw = new StringWriter();
            mol.debug(sw);
            System.out.println(sw.toString());
            SpanningTree sTree = new SpanningTreeImpl(mol);
            System.out.println(sTree.toSMILES());
            Writer w = new OutputStreamWriter(new FileOutputStream(id + ".xml"));
            PMRDelegate.outputEventStream(mol, w, PMRNode.PRETTY, 0);
            w.close();
        } catch (Exception e) {
            System.out.println("MDLMol failed: " + e);
            e.printStackTrace();
            System.exit(0);
        }
    }
}

;

class ABVal {

    int ab;

    int val;
}
