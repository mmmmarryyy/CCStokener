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
import org.w3c.dom.Text;
import org.xmlcml.cml.AbstractBase;
import org.xmlcml.cml.AbstractStringVal;
import org.xmlcml.cml.AttributeSize;
import org.xmlcml.cml.CMLAtom;
import org.xmlcml.cml.CMLBond;
import org.xmlcml.cml.CMLBondStereo;
import org.xmlcml.cml.CMLCrystal;
import org.xmlcml.cml.CMLDocument;
import org.xmlcml.cml.CMLException;
import org.xmlcml.cml.CMLFloatArray;
import org.xmlcml.cml.CMLFloatVal;
import org.xmlcml.cml.CMLIntegerArray;
import org.xmlcml.cml.CMLIntegerVal;
import org.xmlcml.cml.CMLList;
import org.xmlcml.cml.CMLMolecule;
import org.xmlcml.cml.CMLStringArray;
import org.xmlcml.cml.CMLStringVal;
import org.xmlcml.cmlimpl.AbstractBuiltinContainerImpl;
import org.xmlcml.cmlimpl.AtomImpl;
import org.xmlcml.cmlimpl.AtomArrayImpl;
import org.xmlcml.cmlimpl.BondImpl;
import org.xmlcml.cmlimpl.BondArrayImpl;
import org.xmlcml.cmlimpl.BondStereoImpl;
import org.xmlcml.cmlimpl.CMLBaseImpl;
import org.xmlcml.cmlimpl.CMLDocumentImpl;
import org.xmlcml.cmlimpl.Coord2;
import org.xmlcml.cmlimpl.Coord3;
import org.xmlcml.cmlimpl.CrystalImpl;
import org.xmlcml.cmlimpl.FloatArrayImpl;
import org.xmlcml.cmlimpl.FloatValImpl;
import org.xmlcml.cmlimpl.IntegerArrayImpl;
import org.xmlcml.cmlimpl.IntegerValImpl;
import org.xmlcml.cmlimpl.ListImpl;
import org.xmlcml.cmlimpl.MoleculeImpl;
import org.xmlcml.cmlimpl.StringValImpl;
import org.xmlcml.cmlimpl.StringArrayImpl;
import uk.co.demon.ursus.dom.PMRDelegate;
import uk.co.demon.ursus.dom.PMRElement;
import uk.co.demon.ursus.dom.PMRNode;
import org.apache.regexp.*;
import jumbo.xml.util.Util;

/** class to read formatted files
@author (C) P. Murray-Rust, 2000
*/
public class FormatImpl extends NonCMLDocumentImpl implements Format {

    URL parserUrl;

    BlockMatcher blockMatcher = null;

    public FormatImpl() {
    }

    /** Convenience: form a Format object from a local file
@exception Exception file was not a standard Format file
*/
    public FormatImpl(BufferedReader bReader, String id) throws Exception {
        createAndAddCMLElement(FORMAT, id);
        parse(bReader);
    }

    public void setParserUrl(URL parserUrl) {
        this.parserUrl = parserUrl;
    }

    /** form a Format object from a CML file
*/
    public FormatImpl(CMLMolecule outputCMLMolecule) {
        setOutputCMLMolecule(outputCMLMolecule);
    }

    public void parse(BufferedReader bReader) throws Exception {
        this.bReader = bReader;
        blockMatcher = new BlockMatcher(parserUrl, this);
        Element rootMatcher = blockMatcher.element;
        if (rootMatcher.getTagName().equals("block")) {
            blockMatcher.processBlock((PMRElement) blockMatcher.element, this);
        } else {
            throw new CMLException("Block matcher must start with <block>");
        }
        System.out.println("Format finished...");
    }

    public String output(Writer w) {
        return "NOT IMPLEMENTED";
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java FormatImpl inputfile");
            System.exit(0);
        }
        Format format = null;
        try {
            URL url = new URL(Util.makeAbsoluteURL(args[0]));
            BufferedReader bReader = new BufferedReader(new InputStreamReader(url.openStream()));
            int idx = args[0].indexOf(".");
            String id = (idx == -1) ? args[0] : args[0].substring(0, idx);
            idx = id.lastIndexOf("\\");
            if (idx != -1) id = id.substring(idx + 1);
            format = new FormatImpl(bReader, id);
        } catch (Exception e) {
            System.out.println("Format failed: " + e);
            e.printStackTrace();
            System.exit(0);
        }
    }
}

class BlockMatcher {

    String classx = null;

    String type = null;

    Element element;

    FormatImpl format;

    String requireString;

    boolean requires;

    boolean doExit = false;

    boolean doBreak = false;

    boolean doContinue = false;

    boolean eofBlock = false;

    Hashtable inputTable = new Hashtable();

    Hashtable outputTable = new Hashtable();

    String readResult = "";

    Field[] fields = null;

    String[] splitStrings = null;

    StringArrayImpl nameArray = null;

    StringArrayImpl valueArray = null;

    String concat = null;

    Vector listVector = null;

    AttributeSize[] arrayArray = null;

    String line = null;

    String lastLine = null;

    void trace(String s) {
        System.out.println(s);
    }

    public BlockMatcher(URL parserUrl, FormatImpl format) throws Exception {
        CMLDocument doc = FormatImpl.DOCUMENT_FACTORY.createDocument();
        doc.parse(parserUrl);
        this.element = doc.getDocumentElement();
        this.format = format;
    }

    void processAdd(PMRElement elem) {
        trace("A-");
        String delim1 = elem.getAttribute("delim1");
        String delim2 = elem.getAttribute("delim2");
        String input = elem.getAttribute("input");
        String type = elem.getAttribute("type");
        String title = elem.getAttribute("title");
        String ss = elem.getAttribute("start");
        String ee = elem.getAttribute("end");
        int start = 1;
        int end = 9999999;
        if (ss != null) try {
            start = Integer.parseInt(ss);
        } catch (NumberFormatException nfe) {
        }
        if (ee != null) try {
            end = Integer.parseInt(ee);
        } catch (NumberFormatException nfe) {
        }
    }

    void processBlock(PMRElement elem, Node parent) throws Exception {
        String classx = elem.getAttribute("class");
        if (PMRDelegate.isEmptyAttribute(classx)) throw new CMLException("block: missing class attribute");
        String type = elem.getAttribute("type");
        if (PMRDelegate.isEmptyAttribute(type)) throw new CMLException("block: missing type attribute");
        trace("CL:" + classx);
        int minsize = -1;
        String minsz = elem.getAttribute("minsize");
        if (!PMRDelegate.isEmptyAttribute(minsz)) {
            minsize = Integer.parseInt(minsz);
            if (arrayArray != null && arrayArray[0].getSize() < minsize) {
                System.out.println("omitted block as too small");
                return;
            }
        }
        AbstractBase block = makeBlock(type, classx);
        if (block == null) {
            System.out.println("Null block: " + type + "/" + classx);
            return;
        }
        parent.appendChild(block);
        if (block instanceof CrystalImpl) {
            addFields(block);
        } else if (block instanceof ListImpl) {
            addArrays(block);
            addFields(block);
            addListVector(block);
        } else if (block instanceof MoleculeImpl) {
            addFields(block);
        } else if (block instanceof StringArrayImpl) {
            addListVector(block);
        } else if (block instanceof StringValImpl) {
            addConcat(block);
        }
        Vector childVector = PMRDelegate.getChildElements(elem);
        for (int i = 0; i < childVector.size(); i++) {
            PMRElement child = (PMRElement) childVector.elementAt(i);
            String tagName = child.getTagName();
            processTag(tagName, child, block);
            if (doExit) {
                doExit = false;
                return;
            }
        }
        return;
    }

    void addArrays(AbstractBase block) throws CMLException {
        if (splitStrings == null && nameArray == null) return;
        if (splitStrings != null) {
            if (!(block instanceof CMLStringArray)) {
                throw new CMLException("Can only add split results to a <stringArray> in: " + lastLine);
            }
            for (int i = 0; i < splitStrings.length; i++) {
                ((CMLStringArray) block).addElement(splitStrings[i]);
            }
        } else if (nameArray != null && valueArray != null) {
            if (!(block instanceof CMLList)) {
                throw new CMLException("Can only add split nameValues to a <list> in: " + lastLine);
            }
            block.setAttribute("display", "nvtable");
            block.appendChild(nameArray);
            nameArray.setTitle("name");
            block.appendChild(valueArray);
            valueArray.setTitle("value");
        } else {
            return;
        }
    }

    void addFields(AbstractBase block) throws CMLException {
        trace("BL" + block);
        if (fields == null && arrayArray == null) return;
        if (false) {
        } else if (block instanceof BondArrayImpl) {
            if (arrayArray == null) {
                System.out.println("emptyBondArray");
                return;
            }
            int idNo = -1;
            int atomRef1 = -1;
            int atomRef2 = -1;
            int order = -1;
            int bondStereo = -1;
            String pref = AbstractBase.CML_PREFIX + ":";
            for (int j = 0; j < fields.length; j++) {
                String dictRef = fields[j].dictRef;
                if (false) {
                } else if (dictRef.equals(pref + CMLBond.ATOMREF_NAME + "1")) {
                    atomRef1 = j;
                } else if (dictRef.equals(pref + CMLBond.ATOMREF_NAME + "2")) {
                    atomRef2 = j;
                } else if (dictRef.equals(pref + CMLBond.ORDER)) {
                    order = j;
                } else if (dictRef.equals(pref + CMLBond.STEREO)) {
                    bondStereo = j;
                }
            }
            if (idNo == -1) {
                System.out.println("No id field given for atom - using serialNo");
            }
        } else if (block instanceof MoleculeImpl) {
            if (arrayArray == null) {
                System.out.println("emptyMolecule");
                return;
            }
            int idNo = -1;
            int x2No = -1;
            int y2No = -1;
            int x3No = -1;
            int y3No = -1;
            int z3No = -1;
            int xf3No = -1;
            int yf3No = -1;
            int zf3No = -1;
            int occNo = -1;
            int nHNo = -1;
            int nonHNo = -1;
            int elNo = -1;
            int parNo = -1;
            int isotNo = -1;
            int fchNo = -1;
            String pref = AbstractBase.CML_PREFIX + ":";
            for (int j = 0; j < fields.length; j++) {
                String dictRef = fields[j].dictRef;
                if (dictRef.equals(pref + CMLAtom.ATOMID_NAME)) {
                    idNo = j;
                } else if (dictRef.equals(pref + CMLAtom.X2_NAME)) {
                    x2No = j;
                } else if (dictRef.equals(pref + CMLAtom.Y2_NAME)) {
                    y2No = j;
                } else if (dictRef.equals(pref + CMLAtom.X3_NAME)) {
                    x3No = j;
                } else if (dictRef.equals(pref + CMLAtom.Y3_NAME)) {
                    y3No = j;
                } else if (dictRef.equals(pref + CMLAtom.Z3_NAME)) {
                    z3No = j;
                } else if (dictRef.equals(pref + CMLAtom.XFRACT_NAME)) {
                    xf3No = j;
                } else if (dictRef.equals(pref + CMLAtom.YFRACT_NAME)) {
                    yf3No = j;
                } else if (dictRef.equals(pref + CMLAtom.ZFRACT_NAME)) {
                    zf3No = j;
                } else if (dictRef.equals(pref + CMLAtom.OCC_NAME)) {
                    occNo = j;
                } else if (dictRef.equals(pref + CMLAtom.HCOUNT_NAME)) {
                    nHNo = j;
                } else if (dictRef.equals(pref + CMLAtom.NONH_NAME)) {
                    nonHNo = j;
                } else if (dictRef.equals(pref + CMLAtom.ELTYPE_NAME)) {
                    elNo = j;
                } else if (dictRef.equals(pref + CMLAtom.PARITY_NAME)) {
                    parNo = j;
                } else if (dictRef.equals(pref + CMLAtom.ISOTOPE_NAME)) {
                    isotNo = j;
                } else if (dictRef.equals(pref + CMLAtom.FCHARGE_NAME)) {
                    fchNo = j;
                }
            }
            if (idNo == -1) {
                System.out.println("No id field given for atom - using serialNo");
            }
            MoleculeImpl molecule = (MoleculeImpl) block;
            for (int i = 0; i < arrayArray[0].getSize(); i++) {
                String atomId = "a" + (i + 1);
                if (idNo != -1) {
                    atomId = ((CMLStringArray) arrayArray[idNo]).getString(i).trim();
                }
                CMLAtom atom = format.ATOM_FACTORY.createAtom(molecule, atomId);
                if (x2No != -1 && y2No != -1) {
                    double x = ((CMLFloatArray) arrayArray[x2No]).getFloat(i);
                    double y = ((CMLFloatArray) arrayArray[y2No]).getFloat(i);
                    atom.setXY2(new Coord2(x, y));
                }
                if (x3No != -1 && y3No != -1 && z3No != -1) {
                    double x = ((CMLFloatArray) arrayArray[x3No]).getFloat(i);
                    double y = ((CMLFloatArray) arrayArray[y3No]).getFloat(i);
                    double z = ((CMLFloatArray) arrayArray[z3No]).getFloat(i);
                    atom.setXYZ3(new Coord3(x, y, z));
                }
                if (xf3No != -1 && yf3No != -1 && zf3No != -1) {
                    double x = ((CMLFloatArray) arrayArray[xf3No]).getFloat(i);
                    double y = ((CMLFloatArray) arrayArray[yf3No]).getFloat(i);
                    double z = ((CMLFloatArray) arrayArray[zf3No]).getFloat(i);
                    atom.setXYZFract(new Coord3(x, y, z));
                }
                if (occNo != -1) {
                    double occ = ((CMLFloatArray) arrayArray[occNo]).getFloat(i);
                    atom.setOccupancy(occ);
                }
                if (parNo != -1) {
                }
                if (nHNo != -1) {
                    int nh = ((CMLIntegerArray) arrayArray[nHNo]).getInteger(i);
                    atom.setHydrogenCount(nh);
                }
                if (nonHNo != -1) {
                    int nonh = ((CMLIntegerArray) arrayArray[nonHNo]).getInteger(i);
                    atom.setNonHydrogenCount(nonh);
                }
                if (isotNo != -1) {
                    int isot = ((CMLIntegerArray) arrayArray[isotNo]).getInteger(i);
                }
                if (elNo != -1) {
                    String elType = ((CMLStringArray) arrayArray[elNo]).getString(i).trim();
                    atom.setElementType(elType);
                }
                if (fchNo != -1) {
                    int fch = ((CMLIntegerArray) arrayArray[fchNo]).getInteger(i);
                    atom.setFormalCharge(fch);
                }
                for (int j = 0; j < arrayArray.length; j++) {
                    if (j == idNo || j == x2No || j == y2No || j == x3No || j == y3No || j == z3No || j == xf3No || j == yf3No || j == zf3No || j == nonHNo || j == nHNo || j == elNo || j == parNo || j == isotNo || j == occNo || j == fchNo) continue;
                }
            }
            trace("MOL");
            molecule.debug();
        } else if (block instanceof CrystalImpl) {
            CrystalImpl crystal = (CrystalImpl) block;
            Field a = Field.getField(AbstractBase.CML_PREFIX + ":" + CMLCrystal.ACELL_NAME);
            Field b = Field.getField(AbstractBase.CML_PREFIX + ":" + CMLCrystal.BCELL_NAME);
            Field c = Field.getField(AbstractBase.CML_PREFIX + ":" + CMLCrystal.CCELL_NAME);
            if (a != null && b != null && c != null) crystal.setCellLengths(((CMLFloatVal) a.av).getRealValue(), ((CMLFloatVal) b.av).getRealValue(), ((CMLFloatVal) c.av).getRealValue());
            a = Field.getField(AbstractBase.CML_PREFIX + ":" + CMLCrystal.ALPHA_NAME);
            b = Field.getField(AbstractBase.CML_PREFIX + ":" + CMLCrystal.BETA_NAME);
            c = Field.getField(AbstractBase.CML_PREFIX + ":" + CMLCrystal.GAMMA_NAME);
            if (a != null && b != null && c != null) crystal.setCellAngles(((CMLFloatVal) a.av).getRealValue(), ((CMLFloatVal) b.av).getRealValue(), ((CMLFloatVal) c.av).getRealValue());
            Field f = Field.getField(AbstractBase.CML_PREFIX + ":" + CMLCrystal.SPACEGROUP_NAME);
            if (f != null) crystal.setSpacegroup(f.av.getStringValue());
            f = Field.getField(AbstractBase.CML_PREFIX + ":" + CMLCrystal.Z_NAME);
            if (f != null) crystal.setMoleculesPerCell(((CMLIntegerVal) f.av).getIntValue());
        } else if (block instanceof ListImpl) {
            if (arrayArray != null) {
                for (int i = 0; i < arrayArray.length; i++) {
                    block.appendChild((Node) arrayArray[i]);
                }
                block.setAttribute("display", "nvtable");
            } else if (fields != null) {
                if (!(block instanceof CMLList)) {
                    throw new CMLException("Can only add fields to a <list> in: " + lastLine);
                }
                for (int i = 0; i < fields.length; i++) block.appendChild(fields[i].av);
                block.setAttribute("display", "nvlist");
            }
        } else {
            throw new CMLException("Cannot add fields to a: " + block.getClass() + " in: " + lastLine);
        }
    }

    void addListVector(AbstractBase block) throws CMLException {
        if (listVector == null) return;
        if (block instanceof CMLStringArray) {
            for (int i = 0; i < listVector.size(); i++) {
                ((CMLStringArray) block).addElement((String) listVector.elementAt(i));
            }
        } else {
            System.out.println("Can only add list to a <stringArray> in: " + lastLine);
        }
    }

    void addConcat(AbstractBase block) throws CMLException {
        if (concat == null || !(concat instanceof String)) return;
        if (!(block instanceof CMLStringVal)) {
            System.out.println("Can only add string data to a <string>, <float>, <integer>  in: " + lastLine);
            return;
        }
        ((CMLStringVal) block).setStringValue((String) concat);
    }

    void processChoose(PMRElement elem, Node parent) throws Exception {
        trace("CH-");
        Vector childVector = PMRDelegate.getChildElements(elem);
        if (childVector.size() < 1 || !((Element) childVector.elementAt(0)).getTagName().equals("if")) {
            throw new CMLException("<choose> must have <if> as first child");
        }
        for (int i = 0; i < childVector.size(); i++) {
            PMRElement child = (PMRElement) childVector.elementAt(i);
            String tagName = child.getTagName();
            if (tagName.equals("if") && i > 0) {
                throw new CMLException("Only one <if> allowed in <choose>");
            }
            if (tagName.equals("if") || tagName.equals("elseif")) {
                boolean ok = processIf(child, parent);
                if (ok) break;
            } else if (tagName.equals("else")) {
                if (i != childVector.size() - 1) {
                    throw new CMLException("<else> only occurs as last child of <choose>");
                }
                processChildren(child, parent);
            }
            if (doContinue) break;
            if (doBreak) break;
            if (doExit) break;
        }
    }

    void processFormat(PMRElement elem) throws Exception {
        trace("FO-");
        setSkip(elem.getAttribute("skipRegexp"));
        setPad(elem.getAttribute("padLine"));
    }

    boolean processIf(PMRElement elem, Node parent) throws Exception {
        trace("IF-");
        String result = elem.getAttribute("result");
        String test = elem.getAttribute("test");
        if ("read".equals(result) && !(readResult.equals("read"))) {
            trace("!testRD");
            return false;
        }
        if ("eof".equals(result) && !eofBlock) {
            trace("!testEOF");
            return false;
        }
        Vector childVector = PMRDelegate.getChildElements(elem);
        for (int i = 0; i < childVector.size(); i++) {
            PMRElement child = (PMRElement) childVector.elementAt(i);
            String tagName = child.getTagName();
            processTag(tagName, child, parent);
            if (doContinue) break;
            if (doBreak) break;
            if (doExit) break;
        }
        return true;
    }

    void processRead(PMRElement elem) throws Exception {
        trace("RD-");
        String mark = elem.getAttribute("mark");
        if ("set".equals(mark)) format.bReader.mark(10000);
        if ("reset".equals(mark)) format.bReader.reset();
        String build = elem.getAttribute("build");
        if (PMRDelegate.isEmptyAttribute(build)) build = "concat";
        String whilex = elem.getAttribute("while");
        String input = elem.getAttribute("input");
        Object inputObject = null;
        if (PMRDelegate.isEmptyAttribute(input)) input = "_reader";
        if (input.equals("_reader")) {
            inputObject = format;
        } else {
            inputObject = inputTable.get(input);
            if (inputObject == null) throw new CMLException("Unknown input source: " + input);
        }
        String output = elem.getAttribute("output");
        if (PMRDelegate.isEmptyAttribute(output)) output = "_writer";
        String regexp = elem.getAttribute("regexp");
        if (PMRDelegate.isEmptyAttribute(regexp)) regexp = null;
        if (regexp != null) throw new CMLException("regexp attribute discontinued");
        Element reElement = PMRDelegate.getFirstChildWithElementName(elem, "re");
        String formatAtt = null;
        Vector reVector = null;
        if (reElement != null) {
            formatAtt = reElement.getAttribute("format");
            reVector = new Vector();
            String re1 = PMRDelegate.getPCDATAContent(reElement);
            if (re1 == null) re1 = "";
            re1 = Util.rightTrim(re1);
            if (re1.startsWith("\n")) re1 = re1.substring(1);
            if (re1.endsWith("\n")) re1 = re1.substring(0, re1.length() - 1);
            while (true) {
                int idx = re1.indexOf("\n");
                if (idx == -1) {
                    addEscapedString(reVector, formatAtt, re1);
                    break;
                } else {
                    addEscapedString(reVector, formatAtt, re1.substring(0, idx));
                    re1 = re1.substring(idx + 1);
                }
            }
            for (int i = 0; i < reVector.size(); i++) {
            }
        }
        if (reVector != null) {
            if (reVector.size() == 1 && regexp == null) {
                regexp = (String) reVector.elementAt(0);
            } else {
                format.setMark();
                for (int i = 0; i < reVector.size(); i++) {
                    String l = peekNextLine(inputObject);
                    if (l == null) {
                        readResult = "null";
                        return;
                    }
                    String reS = (String) reVector.elementAt(i);
                    RE reV = new RE(reS);
                    if (!reV.match(l)) {
                        trace("RE FAIL: " + reS + "/" + l);
                        format.resetMark();
                        readResult = "none";
                        return;
                    } else {
                        trace("MATCH: " + reS + "/" + l);
                    }
                    l = getCurrentLine(inputObject);
                }
                readResult = "read";
                return;
            }
        }
        String delim1 = elem.getAttribute("delim1");
        String delim2 = elem.getAttribute("delim2");
        if (!PMRDelegate.isEmptyAttribute(delim2)) {
            if (PMRDelegate.isEmptyAttribute(delim1)) throw new CMLException("delim2 requires non-empty delim1");
        }
        if (!PMRDelegate.isEmptyAttribute(delim1)) {
        }
        String constantName = elem.getAttribute("constantField");
        if ("".equals(constantName)) constantName = null;
        RE re = null;
        if (regexp != null) {
            re = new RE(regexp);
        }
        if (whilex.equals("match") && re == null) {
            throw new CMLException("while='match' must have <re> or regexp attribute");
        }
        fields = null;
        arrayArray = null;
        Vector childVector = PMRDelegate.getChildElements(elem);
        Vector fieldVector = new Vector();
        for (int i = 0; i < childVector.size(); i++) {
            Element child = (Element) childVector.elementAt(i);
            String tagName = child.getTagName();
            if (tagName.equals("field")) {
                fieldVector.addElement(child);
            } else if (tagName.equals("re")) {
            } else {
                throw new CMLException("<read> can only contain <field>");
            }
        }
        listVector = null;
        concat = null;
        splitStrings = null;
        nameArray = null;
        valueArray = null;
        readResult = "";
        lastLine = line;
        line = null;
        String constantValue = null;
        while (true) {
            line = peekNextLine(inputObject);
            trace("peek:" + line + ":");
            if (line == null) {
                trace("NULL-LINE");
                readResult = "null";
                return;
            }
            if (whilex == null) whilex = "";
            if (true || whilex.equals("match") || whilex.equals("notMatch")) {
                int parenCount = 0;
                if (((whilex.equals("match") || whilex.equals("")) && re != null && !re.match(line)) || (whilex.equals("notMatch") && re.match(line))) {
                    break;
                } else {
                    System.out.println("MATCH: " + regexp + "~~" + line);
                    readResult = "read";
                    makeFieldsAndArrays(fieldVector, build);
                    if (re != null) {
                        parenCount = re.getParenCount();
                        boolean constantChanged = false;
                        if (fields != null && parenCount - 1 == fields.length) {
                            for (int i = 0; i < fields.length; i++) {
                                String fValue = re.getParen(i + 1);
                                if (fields[i].title.equals(constantName)) {
                                    if (constantValue != null && !constantValue.equals(fValue)) {
                                        trace("Changed" + constantValue + "/" + fValue);
                                        constantChanged = true;
                                        break;
                                    }
                                    constantValue = fValue;
                                }
                                fields[i].setValue(fValue);
                                trace("[" + fValue + "]");
                            }
                            if (constantChanged) break;
                        } else if (parenCount <= 2) {
                            if (parenCount > 0) {
                                String par = re.getParen(parenCount - 1);
                                storeLine(par, build);
                            }
                        } else {
                            trace("Cannot match parens (" + parenCount + ") with fields (" + fields + ") in: " + lastLine);
                        }
                    }
                }
                line = getCurrentLine(inputObject);
                trace("<<" + line + "/" + regexp);
                if (line == null) readResult = "null";
            } else {
                throw new CMLException("Bad value for while attribute  in: " + lastLine);
            }
            if (arrayArray != null) {
                for (int i = 0; i < fields.length; i++) {
                    Field f = fields[i];
                    String s = f.getStringValue();
                    try {
                        arrayArray[i].addElement(s);
                    } catch (Exception e) {
                        throw new CMLException("Bad value (" + s + ") for field (" + i + ") : " + f + " in: " + lastLine);
                    }
                }
            }
            if (whilex.equals("")) break;
        }
        if (arrayArray != null) {
            System.out.println("Created " + arrayArray[0].getSize() + " rows in block");
        }
        splitConcat(delim1, delim2);
        if (arrayArray != null) {
        }
        if (concat != null) outputTable.put(output, concat);
        if (listVector != null) outputTable.put(output, listVector);
        trace("C/L" + concat + "/" + listVector);
        trace("RR" + readResult);
    }

    void addEscapedString(Vector reVector, String formatAtt, String line) throws CMLException {
        StringBuffer newLine = new StringBuffer();
        System.out.println("FA" + formatAtt);
        if ("verbatim".equals(formatAtt)) {
            newLine = new StringBuffer('^');
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (("*+?()[]$^").indexOf(c) != -1) newLine.append('\\');
                newLine.append(c);
            }
        } else if ("embeddedRegexp".equals(formatAtt)) {
            if (line.trim().equals("")) {
                newLine = new StringBuffer();
                newLine.append("^ *$");
            } else {
                newLine = new StringBuffer();
                newLine.append('^');
                boolean inRegexp = false;
                int l = line.length();
                for (int i = 0; i < l; i++) {
                    char c = line.charAt(i);
                    if (!inRegexp) {
                        if (c == '\\' && i < l - 1 && line.charAt(i + 1) == '(') {
                            inRegexp = true;
                            i++;
                            c = '(';
                        } else {
                            if (("*+?()[]$^").indexOf(c) != -1) {
                                newLine.append('\\');
                            }
                        }
                    } else {
                        if (c == '\\' && i < l - 1 && line.charAt(i + 1) == ')') {
                            inRegexp = false;
                            i++;
                            c = ')';
                        }
                    }
                    newLine.append(c);
                }
                if (inRegexp) throw new CMLException("Regexp(s) not closed in: " + line);
            }
        } else if ("regexp".equals(formatAtt)) {
            System.out.println("L" + line + ":");
            newLine = new StringBuffer();
            newLine.append(line);
        } else if ("fortran".equals(formatAtt)) {
            newLine = new StringBuffer('^');
            boolean inString = false;
            int i = 0;
            int l = line.length();
            while (i < l) {
                char c = line.charAt(i++);
                if (inString) {
                    if (c == '\'') {
                        inString = false;
                    } else {
                        newLine.append(c);
                    }
                } else if (c == ' ') {
                } else if (c == ',') {
                    int j = line.substring(i + 1).indexOf(',');
                    if (j == -1) throw new CMLException("Bad FORTRAN: " + line);
                    String f = line.substring(i, i + j).trim();
                    i += j + 1;
                    char ff = f.charAt(0);
                    if (ff == 'A' || ff == 'a' || ff == 'I' || ff == 'i' || ff == 'F' || ff == 'f') {
                    } else {
                        throw new CMLException("Bad FORTRAN: " + line);
                    }
                    try {
                        int w = Integer.parseInt(f.substring(1));
                        for (int k = 0; k < w; k++) {
                            newLine.append('.');
                        }
                    } catch (NumberFormatException nfe) {
                        throw new CMLException("Bad FORTRAN: " + line);
                    }
                } else {
                    throw new CMLException("Bad <re> format: " + format);
                }
            }
        }
        String regString = newLine.toString();
        System.out.println("regexp: " + regString);
        reVector.addElement(regString);
    }

    private void storeLine(String line, String build) {
        if (build.equals("concat")) {
            concat = (concat == null) ? line : concat + line;
        }
        if (build.equals("list")) {
            if (listVector == null) listVector = new Vector();
            listVector.addElement(line);
        }
        trace("STORE: " + line);
    }

    private void splitConcat(String delim1, String delim2) throws CMLException {
        if (concat != null && !PMRDelegate.isEmptyAttribute(delim1)) {
            StringTokenizer st = null;
            if (delim1.trim().equals("")) {
                st = new StringTokenizer(concat);
            } else {
                st = new StringTokenizer(concat, delim1);
            }
            splitStrings = new String[st.countTokens()];
            for (int i = 0; i < splitStrings.length; i++) {
                splitStrings[i] = st.nextToken().trim();
            }
            concat = null;
            if (!PMRDelegate.isEmptyAttribute(delim2)) {
                nameArray = new StringArrayImpl(format);
                valueArray = new StringArrayImpl(format);
                for (int i = 0; i < splitStrings.length; i++) {
                    StringTokenizer st2 = new StringTokenizer(splitStrings[i], delim2);
                    if (st2.countTokens() != 2) {
                        System.out.println("Must have 2 tokens for nameValue pair: " + splitStrings[i] + " before line: [" + line + "]");
                        nameArray.addElement("?");
                        valueArray.addElement(splitStrings[i]);
                    } else {
                        nameArray.addElement(st2.nextToken().trim());
                        valueArray.addElement(st2.nextToken().trim());
                    }
                }
                splitStrings = null;
            } else {
                for (int i = 0; i < splitStrings.length; i++) {
                    storeLine(splitStrings[i], "list");
                }
            }
        }
    }

    void makeFieldsAndArrays(Vector fieldVector, String build) throws CMLException {
        if (fields != null || arrayArray != null) return;
        if (fieldVector.size() == 0) return;
        Field.clearTable();
        fields = new Field[fieldVector.size()];
        for (int i = 0; i < fields.length; i++) {
            fields[i] = new Field((Element) fieldVector.elementAt(i), format);
        }
        trace("BUILD" + build);
        if ("list".equals(build)) {
            arrayArray = new AttributeSize[fields.length];
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].cmlType.equals("string")) {
                    arrayArray[i] = new StringArrayImpl(format);
                } else if (fields[i].cmlType.equals("float")) {
                    arrayArray[i] = new FloatArrayImpl(format);
                } else if (fields[i].cmlType.equals("integer")) {
                    arrayArray[i] = new IntegerArrayImpl(format);
                }
                ((AbstractBase) arrayArray[i]).setTitle(fields[i].title);
                ((AbstractBase) arrayArray[i]).setDictRef(fields[i].dictRef);
            }
        }
    }

    void processWhile(PMRElement elem, Node parent) throws Exception {
        trace("WH-");
        Vector childVector = PMRDelegate.getChildElements(elem);
        while (true) {
            trace("<<while>>");
            for (int i = 0; i < childVector.size(); i++) {
                PMRElement child = (PMRElement) childVector.elementAt(i);
                String tagName = child.getTagName();
                processTag(tagName, child, parent);
                if (doContinue) {
                    doContinue = false;
                    trace("CONTINUE IN WHILE");
                    break;
                }
                if (doBreak) {
                    doBreak = false;
                    trace("BREAK OUT OF WHILE");
                    return;
                }
                if (doExit) return;
            }
        }
    }

    void processTag(String tagName, PMRElement child, Node parent) throws Exception {
        if (false) {
        } else if (tagName.equals("add")) {
            processAdd(child);
        } else if (tagName.equals("block")) {
            processBlock(child, parent);
        } else if (tagName.equals("break")) {
            trace("BREAK+++++");
            doBreak = true;
        } else if (tagName.equals("choose")) {
            processChoose(child, parent);
        } else if (tagName.equals("continue")) {
            doContinue = true;
        } else if (tagName.equals("exit")) {
            doExit = true;
        } else if (tagName.equals("format")) {
            processFormat(child);
        } else if (tagName.equals("if")) {
            processIf(child, parent);
        } else if (tagName.equals("message")) {
            processMessage(child);
        } else if (tagName.equals("read")) {
            processRead(child);
        } else if (tagName.equals("while")) {
            processWhile(child, parent);
        } else {
            throw new CMLException("Unexpected tag: " + tagName);
        }
    }

    void processChildren(PMRElement child, Node parent) throws Exception {
        for (int i = 0; i < child.getChildNodes().getLength(); i++) {
            Node childChild = child.getChildNodes().item(i);
            if (childChild instanceof PMRElement) {
                processTag(((Element) childChild).getTagName(), (PMRElement) childChild, parent);
            }
        }
    }

    void processMessage(PMRElement child) {
        String s = "";
        if ("line".equals(child.getAttribute("output"))) {
            s = lastLine;
        } else {
            s = PMRDelegate.getPCDATAContent(child);
        }
        System.out.print("Message: " + s);
    }

    int padWidth = -1;

    void setPad(String pad) {
        try {
            padWidth = Integer.parseInt(pad);
        } catch (NumberFormatException nfe) {
            padWidth = -1;
        }
        trace(pad + "/" + padWidth);
    }

    RE skipRegexp = null;

    void setSkip(String skip) throws CMLException {
        if (PMRDelegate.isEmptyAttribute(skip)) {
            skipRegexp = null;
        } else {
            try {
                skipRegexp = new RE(skip);
            } catch (Exception e) {
                throw new CMLException("Bad regexp: " + e);
            }
        }
    }

    String padLine(String line) {
        if (line == null) return null;
        if (padWidth > line.length()) {
            String b = Util.spaces(padWidth - line.length());
            line += b;
        }
        return line;
    }

    String peekNextLine(Object inputObject) throws IOException {
        if (inputObject instanceof NonCMLDocumentImpl) {
            String line = ((NonCMLDocumentImpl) inputObject).peekLine();
            if (skipRegexp != null) {
                while (true) {
                    line = ((NonCMLDocumentImpl) inputObject).peekLine();
                    if (line == null) break;
                    if (!skipRegexp.match(line)) break;
                    line = ((NonCMLDocumentImpl) inputObject).getCurrentLine();
                }
            }
            return padLine(line);
        } else if (inputObject instanceof StringVector) {
            return ((StringVector) inputObject).peekLine();
        }
        return null;
    }

    String getCurrentLine(Object inputObject) throws IOException {
        if (inputObject instanceof NonCMLDocumentImpl) {
            String line = ((NonCMLDocumentImpl) inputObject).getCurrentLine();
            if (line != null && skipRegexp != null) {
                while (skipRegexp.match(line)) {
                    line = ((NonCMLDocumentImpl) inputObject).getCurrentLine();
                    if (line == null) break;
                }
            }
            return padLine(line);
        } else if (inputObject instanceof StringVector) {
            return ((StringVector) inputObject).getCurrentLine();
        }
        return null;
    }

    public AbstractBase makeBlock(String type, String classx) {
        AbstractBase ab = null;
        if (false) {
        } else if (type.equals("atomArray")) {
            ab = new AtomArrayImpl(format);
        } else if (type.equals("bondArray")) {
            ab = new BondArrayImpl(format);
        } else if (type == null || type.equals("") || type.equals("null")) {
        } else if (type.equals("crystal")) {
            ab = new CrystalImpl(format);
        } else if (type.equals("list")) {
            ab = new ListImpl(format);
        } else if (type.equals("molecule")) {
            ab = format.MOLECULE_FACTORY.createMolecule(format);
            trace("Make mol: " + ab);
        } else if (type.equals("string")) {
            ab = new StringValImpl(format);
        } else if (type.equals("stringArray")) {
            ab = new StringArrayImpl(format);
        } else {
            ab = new ListImpl(format);
        }
        if (ab == null) return null;
        ab.setTitle(classx);
        return ab;
    }
}

class Field {

    String cmlType;

    String title;

    String dictRef;

    String defalt = null;

    AbstractStringVal av;

    FormatImpl format;

    static Hashtable fieldTable;

    public static void clearTable() {
        fieldTable = new Hashtable();
    }

    public Field(Element elem, FormatImpl format) throws CMLException {
        cmlType = elem.getAttribute("type");
        title = elem.getAttribute("title");
        dictRef = elem.getAttribute("dictRef");
        defalt = elem.getAttribute("default");
        this.format = format;
        String[] builtinNames = null;
        if (false) {
        } else if (cmlType.equals("date")) {
            av = new StringValImpl(format);
        } else if (cmlType.equals("float")) {
            av = new FloatValImpl(format);
            builtinNames = AbstractBuiltinContainerImpl.getBuiltinFloatValNames();
        } else if (cmlType.equals("integer")) {
            av = new IntegerValImpl(format);
            builtinNames = AbstractBuiltinContainerImpl.getBuiltinIntegerValNames();
        } else if (cmlType.equals("string")) {
            av = new StringValImpl(format);
            builtinNames = AbstractBuiltinContainerImpl.getBuiltinStringValNames();
        } else {
            throw new CMLException("unknown CML type: " + cmlType);
        }
        av.setTitle(title);
        if (builtinNames != null && dictRef.startsWith(AbstractBase.CML_PREFIX + ":")) {
            String localDictRef = dictRef.substring((AbstractBase.CML_PREFIX + ":").length());
            int idx = Util.indexOf(localDictRef, builtinNames, Util.CASE);
            if (idx == -1) {
            } else {
                fieldTable.put(dictRef, this);
            }
        }
        av.setDictRef(dictRef);
    }

    public void setValue(String s) throws CMLException {
        format.trace("/" + s + "/");
        av.setStringValue(s);
        format.trace("/" + av.getStringValue() + "/");
    }

    /** gets string value (if value is "" returns default) */
    public String getStringValue() {
        String s = av.getStringValue();
        if (s.equals("")) {
            return (defalt == null) ? "" : defalt;
        }
        return s;
    }

    public static Field getField(String dictRef) {
        if (!dictRef.startsWith(AbstractBase.CML_PREFIX + ":")) {
            dictRef += AbstractBase.CML_PREFIX + ":";
        }
        Field f = (Field) ((fieldTable == null) ? null : fieldTable.get(dictRef));
        return f;
    }

    public String toString() {
        return cmlType + "/" + title + "/" + dictRef + "/" + defalt + ": " + av;
    }
}

class StringVector {

    Vector v;

    int current;

    public StringVector() {
        v = new Vector();
        current = 0;
    }

    public void setLine(String s) {
        v.addElement(s);
    }

    public String peekLine() {
        if (current >= v.size()) return null;
        return (String) v.elementAt(current);
    }

    public String getCurrentLine() {
        if (current >= v.size()) return null;
        return (String) v.elementAt(current++);
    }
}
