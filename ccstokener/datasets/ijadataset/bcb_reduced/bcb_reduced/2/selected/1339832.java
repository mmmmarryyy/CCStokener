package org.xmlcml.noncml;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlcml.cml.CML;
import org.xmlcml.cml.AbstractBase;
import org.xmlcml.cml.CMLDocument;
import org.xmlcml.cml.CMLException;
import org.xmlcml.cml.CMLMolecule;
import org.xmlcml.cml.CMLAtomFactory;
import org.xmlcml.cml.CMLBondFactory;
import org.xmlcml.cml.CMLDocumentFactory;
import org.xmlcml.cml.CMLFloatVal;
import org.xmlcml.cml.CMLFloatArray;
import org.xmlcml.cml.CMLIntegerVal;
import org.xmlcml.cml.CMLIntegerArray;
import org.xmlcml.cml.CMLMoleculeFactory;
import org.xmlcml.cml.CMLStringArray;
import org.xmlcml.cml.metadata.CMLDataType;
import org.xmlcml.cml.metadata.CMLDictionary;
import org.xmlcml.cml.metadata.CMLDictionaryEntry;
import org.xmlcml.cml.metadata.CMLUnits;
import org.xmlcml.cmlimpl.AtomArrayImpl;
import org.xmlcml.cmlimpl.BondArrayImpl;
import org.xmlcml.cmlimpl.CMLImpl;
import org.xmlcml.cmlimpl.CMLDocumentImpl;
import org.xmlcml.cmlimpl.DocumentFactoryImpl;
import org.xmlcml.cmlimpl.FloatArrayImpl;
import org.xmlcml.cmlimpl.FloatValImpl;
import org.xmlcml.cmlimpl.IntegerArrayImpl;
import org.xmlcml.cmlimpl.IntegerValImpl;
import org.xmlcml.cmlimpl.StringArrayImpl;
import org.xmlcml.cmlimpl.StringValImpl;
import org.xmlcml.cmlimpl.metadata.DictionaryImpl;
import org.xmlcml.cmlimpl.jumbo3.JUMBOAtomFactory;
import org.xmlcml.cmlimpl.jumbo3.JUMBOBondFactory;
import org.xmlcml.cmlimpl.jumbo3.JUMBOMoleculeFactory;
import uk.co.demon.ursus.dom.PMRDelegate;
import uk.co.demon.ursus.dom.PMRDocument;
import uk.co.demon.ursus.dom.PMRDocumentImpl;
import uk.co.demon.ursus.dom.PMRNode;
import jumbo.euclid.IntArray;
import jumbo.euclid.RealArray;
import jumbo.xml.util.Util;
import uk.co.demon.ursus.util.Selector;

/** base class for all nonCML molecules
*/
public abstract class NonCMLDocumentImpl extends CMLDocumentImpl implements NonCMLDocument {

    /** crude hacks for traling blanks in fortran */
    public static final String SPACE40 = "                                           ";

    public static final String SPACE80 = SPACE40 + SPACE40;

    protected static CMLDictionary localDictionary = null;

    ;

    /** creates Atoms of the appropriate subclass */
    public static final CMLAtomFactory ATOM_FACTORY = new JUMBOAtomFactory();

    /** creates Bonds of the appropriate subclass */
    public static final CMLBondFactory BOND_FACTORY = new JUMBOBondFactory();

    /** creates Documents of the appropriate subclass */
    public static final CMLDocumentFactory DOCUMENT_FACTORY = new DocumentFactoryImpl();

    /** creates Molecules of the appropriate subclass */
    public static final CMLMoleculeFactory MOLECULE_FACTORY = new JUMBOMoleculeFactory();

    /** the current parse script (messy) - not thread-safe! Set this immediately
	before oarsing legacy file. Set to null to disable script-based parsing
	*/
    static String parseFile = null;

    /** the CML object created from the input legacy */
    protected CML inputCML;

    /** a CML object to be output as legacy */
    protected CML outputCML;

    /** the molecule created from the input legacy */
    protected CMLMolecule inputCMLMolecule;

    /** a CML CMLMolecule to be output as legacy */
    protected CMLMolecule outputCMLMolecule;

    /** result of a parse */
    protected CMLDocument nonCmlDoc;

    /** most input requires a BufferedReader */
    protected BufferedReader bReader;

    /** and uses a line as the container */
    protected String currentLine;

    protected int nLine = 0;

    /** general tools for input and output */
    protected Vector atomVector;

    protected Vector bondVector;

    protected Vector moleculeVector;

    protected int natoms;

    protected int nbonds;

    protected Selector selector = null;

    protected AbstractBase atomParent;

    protected AbstractBase bondParent;

    protected String convention;

    public static Hashtable inputDescription2TypeTable = new Hashtable();

    public static Hashtable outputDescription2TypeTable = new Hashtable();

    static {
        for (int i = 0; i < INPUT_DESCRIPTIONS.length; i++) {
            inputDescription2TypeTable.put(INPUT_DESCRIPTIONS[i], INPUT_TYPES[i]);
        }
        for (int i = 0; i < OUTPUT_DESCRIPTIONS.length; i++) {
            outputDescription2TypeTable.put(OUTPUT_DESCRIPTIONS[i], OUTPUT_TYPES[i]);
        }
    }

    ;

    protected NonCMLDocumentImpl() {
    }

    /** The main parsing routine. must be overridden*/
    public abstract void parse(BufferedReader reader) throws Exception;

    /** The main output routine. Must be overridden */
    public abstract String output(Writer w) throws IOException, CMLException;

    /** */
    public void setOutputCMLMolecule(CMLMolecule outputCMLMolecule) {
        this.outputCMLMolecule = outputCMLMolecule;
    }

    /** */
    public void setOutputCML(CML outputCML) {
        this.outputCML = outputCML;
    }

    protected void getAndCheckVectors() throws CMLException {
        if (outputCMLMolecule == null) throw new CMLException("No CMLMolecule to output");
        moleculeVector = outputCMLMolecule.getMoleculeVector();
        if (moleculeVector != null) {
            throw new CMLException("Molecule consists of " + moleculeVector.size() + " submolecules; Sorry, output abandoned");
        }
        atomVector = outputCMLMolecule.getAtomVector();
        if (atomVector == null || atomVector.size() == 0) throw new CMLException("No atoms to write to molfile");
        natoms = atomVector.size();
        bondVector = outputCMLMolecule.getBondVector();
        if (bondVector != null) nbonds = bondVector.size();
    }

    /** create molecule (inputCMLMolecule) *and add as document Element* */
    public void createAndAddMoleculeElement(String convention, String id) {
        if (inputCML == null) {
            createCMLElement(convention, "_cml");
        }
        createMoleculeElement(convention, id);
        this.appendChild(inputCMLMolecule);
    }

    public void createMoleculeElement(String convention, String id) {
        inputCMLMolecule = MOLECULE_FACTORY.createMolecule(this);
        inputCMLMolecule.setId(id);
        inputCMLMolecule.setConventionName(convention);
    }

    protected void createCMLElement(String convention, String id) {
        inputCML = new CMLImpl(this);
        inputCML.setId(id);
        inputCML.setConventionName(convention);
    }

    protected void createAndAddCMLElement(String convention, String id) {
        createCMLElement(convention, id);
        this.appendChild(inputCML);
    }

    /** creates a nonCMLDocument from the input URL.
	Many formats only return one molecule but some (e.g. SDF) may return many
	Perhaps obsolete
	*/
    public static Vector getMoleculeVector(String inputFile, String type) throws Exception {
        URL url = new URL(Util.makeAbsoluteURL(inputFile));
        CMLDocument cmlDoc = createCMLDocument(url, type);
        return cmlDoc.getMoleculeVector();
    }

    /** convenience method - routes to bufferedReader */
    public void parse(URL url) throws Exception {
        BufferedReader bReader = new BufferedReader(new InputStreamReader(url.openStream()));
        parse(bReader);
    }

    /** get allowed file types */
    public static String[] getInputTypes() {
        return INPUT_TYPES;
    }

    /** get allowed file suffixes */
    public static String[][] getInputSuffixes() {
        return INPUT_SUFFIXES;
    }

    /** get allowed file suffixes */
    public static String[] getInputDescriptions() {
        return INPUT_DESCRIPTIONS;
    }

    /** get allowed file types */
    public static String[] getOutputTypes() {
        return OUTPUT_TYPES;
    }

    /** get allowed file suffixes */
    public static String[] getOutputSuffixes() {
        return OUTPUT_SUFFIXES;
    }

    /** get allowed file suffixes */
    public static String[] getOutputDescriptions() {
        return OUTPUT_DESCRIPTIONS;
    }

    public static CMLDocument createCMLDocument(String type) throws CMLException {
        CMLDocument cmlDoc = null;
        if (type == null) {
            throw new CMLException("Bad/unsupported non-cml type: " + type);
        } else if (type.equalsIgnoreCase(CML)) {
            cmlDoc = DOCUMENT_FACTORY.createDocument();
        } else if (type.equalsIgnoreCase(XML)) {
            cmlDoc = DOCUMENT_FACTORY.createDocument();
        } else if (type.equalsIgnoreCase(CIF)) {
            cmlDoc = new CIFImpl();
            ((CIFImpl) cmlDoc).setSU(true);
        } else if (type.equalsIgnoreCase(MMCIF)) {
            cmlDoc = new MMCIFImpl();
            ((CIFImpl) cmlDoc).setSU(true);
        } else if (type.equalsIgnoreCase(GAMESS)) {
            cmlDoc = new GAMESSImpl();
        } else if (type.equalsIgnoreCase(CASTEP)) {
            cmlDoc = new CASTEPImpl();
        } else if (type.equalsIgnoreCase(FORMAT)) {
            cmlDoc = new FormatImpl();
        } else if (type.equalsIgnoreCase(TEST)) {
            cmlDoc = new PreStyleImpl();
        } else if (type.equalsIgnoreCase(G94)) {
            cmlDoc = new G94Impl();
        } else if (type.equalsIgnoreCase(JCAMP)) {
            cmlDoc = new JCAMPImpl();
        } else if (type.equalsIgnoreCase(JME)) {
            cmlDoc = new JMEImpl();
        } else if (type.equalsIgnoreCase(MDLMOL)) {
            cmlDoc = new MDLMolImpl();
        } else if (type.equalsIgnoreCase(MOL2)) {
            cmlDoc = new MOL2Impl();
        } else if (type.equalsIgnoreCase(MIF)) {
            cmlDoc = new MIFImpl();
            ((MIFImpl) cmlDoc).setSU(true);
        } else if (type.equalsIgnoreCase(MOPAC)) {
            cmlDoc = new MOPACImpl();
        } else if (type.equalsIgnoreCase(MOPACIn)) {
            cmlDoc = new MOPACInImpl();
        } else if (type.equalsIgnoreCase(PDB)) {
            cmlDoc = new PDBImpl();
        } else if (type.equalsIgnoreCase(PDBConect)) {
            cmlDoc = new PDBConectImpl();
        } else if (type.equalsIgnoreCase(SDF)) {
            cmlDoc = new SDFImpl();
        } else if (type.equalsIgnoreCase(SWISS)) {
            cmlDoc = new SwissFImpl();
        } else if (type.equalsIgnoreCase(SMILES)) {
            cmlDoc = new SMILESImpl();
        } else if (type.equalsIgnoreCase(VAMP)) {
            cmlDoc = new VAMPImpl();
        } else if (type.equalsIgnoreCase(XYZ)) {
            cmlDoc = new XYZImpl();
        } else {
            throw new CMLException("Bad/unsupported non-cml type: " + type);
        }
        return cmlDoc;
    }

    static String makeFileStem(String file) {
        int idx = file.indexOf(".");
        String id = (idx == -1) ? file : file.substring(0, idx);
        idx = id.lastIndexOf("\\");
        if (idx != -1) id = id.substring(idx + 1);
        return id;
    }

    public static void processLinks(CMLDocument cmlDoc) throws Exception {
        if (cmlDoc.hasLinkElements()) {
            cmlDoc.dereferenceLinks();
            cmlDoc.debug();
        }
    }

    /** set the parse file - use null to reset it */
    public static void setParseFile(String f) {
        parseFile = f;
    }

    /** Convenience method - uses defaults to create a CMLDocument from the input URL.
	Very variable returns according to type.
	Many formats only return one molecule but some (e.g. SDF) may return many
	@param URL the url
	@param String type; if null tries to guess from url suffix
	*/
    public static CMLDocument createCMLDocument(URL urlx, String type) throws Exception {
        if (type == null) {
            type = getTypeFromFile(urlx.toString());
        }
        if (type == null) throw new CMLException("Unknown file type: " + urlx);
        CMLDocument cmlDoc = null;
        BufferedReader bReader = new BufferedReader(new InputStreamReader(urlx.openStream()));
        String id = makeFileStem(urlx.toString());
        if (false) {
        } else if (type.equalsIgnoreCase(CML) || type.equalsIgnoreCase(XML)) {
            cmlDoc = DOCUMENT_FACTORY.createDocument();
            cmlDoc.parse(urlx);
        } else if (type.equalsIgnoreCase(CIF)) {
            cmlDoc = new CIFImpl();
            ((CIFImpl) cmlDoc).setSU(true);
            cmlDoc.parse(urlx);
        } else if (type.equalsIgnoreCase(MMCIF)) {
            cmlDoc = new MMCIFImpl(urlx);
        } else if (type.equalsIgnoreCase(GAMESS)) {
            cmlDoc = new GAMESSImpl();
            ((NonCMLDocument) cmlDoc).parse(urlx);
        } else if (type.equalsIgnoreCase(CASTEP)) {
            cmlDoc = new CASTEPImpl();
            ((NonCMLDocument) cmlDoc).parse(urlx);
        } else if (type.equalsIgnoreCase(FORMAT)) {
            if (parseFile == null) throw new CMLException("Format: No PARSEFILE given");
            cmlDoc = new FormatImpl();
            ((FormatImpl) cmlDoc).setParserUrl(new URL(Util.makeAbsoluteURL(parseFile)));
            ((NonCMLDocument) cmlDoc).parse(urlx);
        } else if (type.equalsIgnoreCase(TEST)) {
            if (parseFile == null) throw new CMLException("Test: No PARSEFILE given");
            cmlDoc = new PreStyleImpl();
            ((PreStyleImpl) cmlDoc).setParserUrl(new URL(Util.makeAbsoluteURL(parseFile)));
            ((NonCMLDocument) cmlDoc).parse(urlx);
        } else if (type.equalsIgnoreCase(G94)) {
            cmlDoc = new G94Impl();
            ((NonCMLDocument) cmlDoc).parse(urlx);
        } else if (type.equalsIgnoreCase(JCAMP)) {
            cmlDoc = new JCAMPImpl();
            ((NonCMLDocument) cmlDoc).parse(urlx);
        } else if (type.equalsIgnoreCase(JME)) {
            cmlDoc = new JMEImpl(bReader, id);
        } else if (type.equalsIgnoreCase(MDLMOL)) {
            cmlDoc = new MDLMolImpl(bReader, id);
        } else if (type.equalsIgnoreCase(MOL2)) {
            cmlDoc = new MOL2Impl(bReader, id);
        } else if (type.equalsIgnoreCase(MIF)) {
            cmlDoc = new MIFImpl();
            ((MIFImpl) cmlDoc).setSU(true);
            cmlDoc.parse(urlx);
        } else if (type.equalsIgnoreCase(MOPAC)) {
            if (parseFile == null) throw new CMLException("MOPAC: No PARSEFILE given");
            cmlDoc = new MOPACFImpl();
            ((MOPACF) cmlDoc).setParserUrl(new URL(Util.makeAbsoluteURL(parseFile)));
            cmlDoc.parse(urlx);
        } else if (type.equalsIgnoreCase(MOPACIn)) {
            cmlDoc = new MOPACInImpl();
            ((NonCMLDocument) cmlDoc).parse(urlx);
        } else if (type.equalsIgnoreCase(PDB)) {
            if (parseFile == null) {
                System.out.println("PDB: No PARSEFILE given; using hardcoded");
                cmlDoc = new PDBImpl();
                ((NonCMLDocument) cmlDoc).parse(urlx);
            } else {
                cmlDoc = new PDBFImpl();
                ((PDBF) cmlDoc).setParserUrl(new URL(Util.makeAbsoluteURL(parseFile)));
                cmlDoc.parse(urlx);
            }
        } else if (type.equalsIgnoreCase(PDBConect)) {
            cmlDoc = new PDBConectImpl();
            ((NonCMLDocument) cmlDoc).createMoleculeElement(PDBConect, id);
            ((NonCMLDocumentImpl) cmlDoc).parse(urlx);
        } else if (type.equalsIgnoreCase(SDF)) {
            cmlDoc = new SDFImpl(bReader, id);
        } else if (type.equalsIgnoreCase(SMILES)) {
            cmlDoc = new SMILESImpl(bReader, id);
        } else if (type.equalsIgnoreCase(SWISS)) {
            if (parseFile == null) throw new CMLException("SWISS: No PARSEFILE given");
            cmlDoc = new SwissFImpl();
            ((SwissF) cmlDoc).setParserUrl(new URL(Util.makeAbsoluteURL(parseFile)));
            cmlDoc.parse(urlx);
        } else if (type.equalsIgnoreCase(VAMP)) {
            cmlDoc = new VAMPImpl(bReader, id);
        } else if (type.equalsIgnoreCase(XYZ)) {
            cmlDoc = new XYZImpl(bReader, id);
        } else {
            System.out.println("Bad/unsupported non-cml type: " + type);
        }
        return cmlDoc;
    }

    /** gives all possible types which have the suffix on fileName. Thus "foo.mol" could
	return MDLMol and MOL2
	*/
    public static final String[] getTypesFromFile(String fileName) {
        fileName = fileName.toLowerCase();
        Vector typeVector = new Vector();
        for (int i = 0; i < INPUT_TYPES.length; i++) {
            for (int j = 0; j < INPUT_SUFFIXES[i].length; j++) {
                if (fileName.endsWith(INPUT_SUFFIXES[i][j])) {
                    typeVector.addElement(INPUT_TYPES[i]);
                }
            }
        }
        if (typeVector.size() == 0) return null;
        String[] typeList = new String[typeVector.size()];
        for (int i = 0; i < typeVector.size(); i++) {
            typeList[i] = (String) typeVector.elementAt(i);
        }
        return typeList;
    }

    /** hardcoded types - probably will be phased out in favout of getTypesFromFile
*/
    public static final String getTypeFromFile(String fileName) {
        fileName = fileName.toLowerCase();
        if (false) {
        } else if (fileName.endsWith(".cif")) {
            return CIF;
        } else if (fileName.endsWith(".cml")) {
            return CML;
        } else if (fileName.endsWith(".mmcif")) {
            return MMCIF;
        } else if (fileName.endsWith(".gam")) {
            return GAMESS;
        } else if (fileName.endsWith(".fmt")) {
            return FORMAT;
        } else if (fileName.endsWith(".tst")) {
            return TEST;
        } else if (fileName.endsWith(".g94")) {
            return G94;
        } else if (fileName.endsWith(".cst")) {
            return CASTEP;
        } else if (fileName.endsWith(".jdx")) {
            return JCAMP;
        } else if (fileName.endsWith(".jme")) {
            return JME;
        } else if (fileName.endsWith(".mol")) {
            return MDLMOL;
        } else if (fileName.endsWith(".mol2")) {
            return MOL2;
        } else if (fileName.endsWith(".mif")) {
            return MIF;
        } else if (fileName.endsWith(".mop")) {
            return MOPAC;
        } else if (fileName.endsWith(".mopin")) {
            return MOPACIn;
        } else if (fileName.endsWith(".pdb")) {
            return PDB;
        } else if (fileName.endsWith(".sdf")) {
            return SDF;
        } else if (fileName.endsWith(".sw")) {
            return SWISS;
        } else if (fileName.endsWith(".smi")) {
            return SMILES;
        } else if (fileName.endsWith(".vmp")) {
            return VAMP;
        } else if (fileName.endsWith(".xyz")) {
            return XYZ;
        } else if (fileName.endsWith(".xml")) {
            return XML;
        } else {
            return null;
        }
        return null;
    }

    /** hardcoded suffixes - probably will be phased out in favout of getTypesFromFile
*/
    public static final String getSuffixFromType(String type) {
        if (false) {
        } else if (type.equals(CIF)) {
            return ".cif";
        } else if (type.equals(CML)) {
            return ".cml";
        } else if (type.equals(MMCIF)) {
            return ".mmcif";
        } else if (type.equals(GAMESS)) {
            return ".gam";
        } else if (type.equals(CASTEP)) {
            return ".cst";
        } else if (type.equals(FORMAT)) {
            return ".fmt";
        } else if (type.equals(TEST)) {
            return ".tst";
        } else if (type.equals(G94)) {
            return ".g94";
        } else if (type.equals(JCAMP)) {
            return ".jdx";
        } else if (type.equals(JME)) {
            return ".jme";
        } else if (type.equals(MDLMOL)) {
            return ".mol";
        } else if (type.equals(MOL2)) {
            return ".mol2";
        } else if (type.equals(MIF)) {
            return ".mif";
        } else if (type.equals(MOPAC)) {
            return ".mop";
        } else if (type.equals(MOPACIn)) {
            return ".mopin";
        } else if (type.equals(PDB)) {
            return ".pdb";
        } else if (type.equals(SDF)) {
            return ".sdf";
        } else if (type.equals(SWISS)) {
            return ".sw";
        } else if (type.equals(SMILES)) {
            return ".smi";
        } else if (type.equals(VAMP)) {
            return ".vmp";
        } else if (type.equals(XYZ)) {
            return ".xyz";
        } else if (type.equals(XML)) {
            return ".xml";
        } else if (type.equals(CMLARRAY)) {
            return ".xml";
        } else {
            return null;
        }
        return null;
    }

    public static void convertList(String inList, String inType, String outDir, String outFile, String outType) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new URL(Util.makeAbsoluteURL(inList)).openStream()));
        String outSuffix = getSuffixFromType(outType);
        if (outSuffix == null) throw new CMLException("Bad type: " + outType);
        FileWriter fw = null;
        if (outFile != null) {
            fw = new FileWriter(outFile);
            fw.write("<cml>\n");
        }
        while (true) {
            String line = br.readLine();
            if (line == null) break;
            String inFile = line.trim();
            if (outFile == null) fw = new FileWriter(makeFileStem(inFile) + "." + outSuffix);
            convertFile(inFile, inType, fw, outType);
            if (outFile == null) fw.close();
        }
        if (outFile != null) {
            fw.write("</cml>\n");
            fw.close();
        }
    }

    public static void convertFile(String inFile, String inType, FileWriter fw, String outType) throws Exception {
        System.out.println("Converting " + inFile + " (" + inType + ") to: (" + outType + ")");
        CMLDocument cmlDoc = createCMLDocument(new URL(Util.makeAbsoluteURL(inFile)), inType);
        if (outType.equals(CMLARRAY)) {
            cmlDoc.arrayify();
            outType = CML;
        }
        if (outType.equals(CML)) {
            PMRDelegate.outputEventStream(cmlDoc.getDocumentElement(), fw, PMRNode.PRETTY, 0, null, true, null);
        }
    }

    /** Convenience method - uses defaults to create a CMLDocument from the input URL.
	and its suffix. Very variable returns according to type.
	Many formats only return one molecule but some (e.g. SDF) may return many
	*/
    public static CMLDocument createCMLDocument(URL url) throws Exception {
        String[] types = getTypesFromFile(url.toString());
        return (types == null) ? null : createCMLDocument(url, types[0]);
    }

    /** creates an object of type determined by a dictionary entry
	@param CMLDictionaryEntry entry the entry from a dictionary
	@param String value data to be processed (could be an array, etc.)
	@throws e.g. invalid data, etc.
	@return AbstractBase such as StringVal, FloatArray, etc.
	*/
    public AbstractBase makeDataItem(CMLDictionaryEntry entry, String value) throws CMLException {
        CMLDataType dataType = entry.getDataType();
        AbstractBase ab = null;
        String type = dataType.getType();
        String structure = dataType.getStructure();
        CMLUnits units = dataType.getUnits();
        if (type.equals(CMLDataType.STRING)) {
            if (structure.equals(CMLDataType.ARRAY)) {
                StringTokenizer st = new StringTokenizer(value);
                String[] ss = new String[st.countTokens()];
                int count = 0;
                for (int i = 0; i < ss.length; i++) {
                    ss[count++] = st.nextToken();
                }
                ab = new StringArrayImpl(this, ss, null);
            } else if (structure.equals(CMLDataType.SCALAR)) {
                ab = new StringValImpl(this, value, null);
            } else {
                throw new CMLException("Unsupported type/structure: " + type + "/" + structure);
            }
        } else if (type.equals(CMLDataType.FLOAT)) {
            if (structure.equals(CMLDataType.ARRAY)) {
                try {
                    RealArray ra = new RealArray(value);
                    ab = new FloatArrayImpl(this, ra.getArray(), null);
                } catch (NumberFormatException nfe) {
                    throw nfe;
                }
            } else if (structure.equals(CMLDataType.SCALAR)) {
                try {
                    double d = new Double(value).doubleValue();
                    ab = new FloatValImpl(this, d, null);
                } catch (NumberFormatException nfe) {
                    throw nfe;
                }
            } else {
                throw new CMLException("Unsupported type/structure: " + type + "/" + structure);
            }
            if (units != null) ((CMLFloatVal) ab).setUnits(units);
        } else if (type.equals(CMLDataType.INTEGER)) {
            if (structure.equals(CMLDataType.ARRAY)) {
                try {
                    IntArray ia = new IntArray(value);
                    ab = new IntegerArrayImpl(this, ia.getArray(), null);
                } catch (NumberFormatException nfe) {
                    throw nfe;
                }
            } else if (structure.equals(CMLDataType.SCALAR)) {
                try {
                    int i = Integer.parseInt(value);
                    ab = new FloatValImpl(this, i, null);
                } catch (NumberFormatException nfe) {
                    throw nfe;
                }
            } else {
                throw new CMLException("Unsupported type/structure: " + type + "/" + structure);
            }
            if (units != null) ((CMLIntegerVal) ab).setUnits(units);
        } else {
            throw new CMLException("Unsupported datatype: " + type);
        }
        ab.setTitle(entry.getTitle());
        String id = entry.getId();
        id = (localDictionary == null) ? id : localDictionary.getPrefix() + ":" + id;
        ab.setDictRef(id);
        return ab;
    }

    /** creates an object of type determined by a dictionary id
	@param Element the parent to add entry to
	@param String the dictionary id
	@param Object data to be processed (could be an array, etc.)
	@throws e.g. invalid data, etc.
	@return AbstractBase such as StringVal, FloatArray, etc.
	*/
    public void addDataItem(Element parent, String id, Object obj) throws CMLException {
        CMLDictionaryEntry entry = localDictionary.getEntryById(id);
        String objValue = obj.toString();
        if (entry == null) throw new CMLException("Unknown id: " + id);
        CMLDataType dataType = entry.getDataType();
        AbstractBase ab = null;
        String type = dataType.getType();
        String structure = dataType.getStructure();
        CMLUnits units = dataType.getUnits();
        if (type.equals(CMLDataType.STRING)) {
            if (structure.equals(CMLDataType.ARRAY)) {
                if (obj instanceof CMLStringArray) {
                    ab = (StringArrayImpl) obj;
                } else if (obj instanceof String) {
                    StringTokenizer st = new StringTokenizer(objValue);
                    String[] ss = new String[st.countTokens()];
                    int count = 0;
                    for (int i = 0; i < ss.length; i++) {
                        ss[count++] = st.nextToken();
                    }
                    ab = new StringArrayImpl(this, ss, null);
                } else {
                    throw new CMLException("Incompatible type for STRING/ARRAY");
                }
            } else if (structure.equals(CMLDataType.SCALAR)) {
                ab = new StringValImpl(this, objValue, null);
            } else {
                throw new CMLException("Unsupported type/structure: " + type + "/" + structure);
            }
        } else if (type.equals(CMLDataType.FLOAT)) {
            if (structure.equals(CMLDataType.ARRAY)) {
                if (obj instanceof CMLFloatArray) {
                    ab = (FloatArrayImpl) obj;
                } else if (obj instanceof RealArray) {
                    ab = new FloatArrayImpl(this, ((RealArray) obj).getArray(), null);
                } else if (obj instanceof String) {
                    try {
                        RealArray ra = new RealArray(objValue);
                        ab = new FloatArrayImpl(this, ra.getArray(), null);
                    } catch (NumberFormatException nfe) {
                        throw nfe;
                    }
                } else {
                    throw new CMLException("Incompatible type for FLOAT/ARRAY: " + obj.getClass() + "/" + objValue);
                }
            } else if (structure.equals(CMLDataType.SCALAR)) {
                try {
                    double d = new Double(objValue).doubleValue();
                    ab = new FloatValImpl(this, d, null);
                } catch (NumberFormatException nfe) {
                    throw nfe;
                }
            } else {
                throw new CMLException("Unsupported type/structure: " + type + "/" + structure);
            }
            if (units != null) ((CMLFloatVal) ab).setUnits(units);
        } else if (type.equals(CMLDataType.INTEGER)) {
            if (structure.equals(CMLDataType.ARRAY)) {
                if (obj instanceof CMLIntegerArray) {
                    ab = (IntegerArrayImpl) obj;
                } else if (obj instanceof IntArray) {
                    ab = new IntegerArrayImpl(this, ((IntArray) obj).getArray(), null);
                } else if (obj instanceof String) {
                    try {
                        IntArray ia = new IntArray(objValue);
                        ab = new IntegerArrayImpl(this, ia.getArray(), null);
                    } catch (NumberFormatException nfe) {
                        throw nfe;
                    }
                } else {
                    throw new CMLException("Incompatible type for INTEGER/ARRAY: " + obj.getClass() + "/" + objValue);
                }
            } else if (structure.equals(CMLDataType.SCALAR)) {
                try {
                    int i = Integer.parseInt(objValue);
                    ab = new FloatValImpl(this, i, null);
                } catch (NumberFormatException nfe) {
                    throw nfe;
                }
            } else {
                throw new CMLException("Unsupported type/structure: " + type + "/" + structure);
            }
            if (units != null) ((CMLIntegerVal) ab).setUnits(units);
        } else {
            throw new CMLException("Unsupported datatype: " + type);
        }
        ab.setTitle(entry.getTitle());
        String idd = entry.getId();
        idd = (localDictionary == null) ? idd : localDictionary.getPrefix() + ":" + idd;
        ab.setDictRef(idd);
        parent.appendChild(ab);
    }

    public static CMLDictionary getDictionary(CMLDocumentImpl doc) {
        if (localDictionary == null) {
            localDictionary = getDictionary(doc);
        }
        return localDictionary;
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    public Selector getSelector() {
        return this.selector;
    }

    String nextLine = null;

    String getCurrentLine() throws IOException {
        currentLine = readNextLine();
        nLine++;
        nextLine = null;
        return currentLine;
    }

    String peekLine() throws IOException {
        readNextLine();
        return nextLine;
    }

    String readNextLine() throws IOException {
        if (nextLine == null) {
            nextLine = bReader.readLine();
        }
        return nextLine;
    }

    int markLine;

    void setMark() throws IOException {
        markLine = nLine;
        bReader.mark(10000);
    }

    void resetMark() throws IOException {
        nLine = markLine;
        bReader.reset();
    }

    public static String outform(double v, int f, int d) {
        String pattern = "############0";
        pattern = pattern.substring(pattern.length() - (f - d - 1));
        pattern += "." + ("000000000".substring(0, d));
        DecimalFormat df = new DecimalFormat(pattern);
        StringBuffer sb = new StringBuffer();
        df.format(v, sb, new FieldPosition(0));
        String s = "              " + sb.toString();
        return s.substring(s.length() - f);
    }

    public static double readFortranReal(String f) throws NumberFormatException {
        int idx = f.indexOf("D");
        if (idx != -1) f = f.substring(0, idx) + "E" + f.substring(idx + 1);
        double d = new Double(f).doubleValue();
        return d;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java org.xmlcml.noncml.NonCMLDocumentImpl [options]");
            System.out.println("        -IN			infile (single molecule file)");
            System.out.println("        -INLIST		inlist (file of input files)");
            System.out.println("        -INFORM		format (input format - no default)");
            System.out.println("        -OUT		outfile (single molecule file; default munged name)");
            System.out.println("        -OUTDIR		outdir (directory for files; default current Dir)");
            System.out.println("        -OUTFORM	format (output format - default CML)");
            System.out.println("        [current formats:");
            System.out.println("                   " + CIF);
            System.out.println("                   " + CML);
            System.out.println("                   " + MMCIF);
            System.out.println("                   " + CASTEP);
            System.out.println("                   " + FORMAT);
            System.out.println("                   " + TEST);
            System.out.println("                   " + G94);
            System.out.println("                   " + GAMESS);
            System.out.println("                   " + JCAMP);
            System.out.println("                   " + JME);
            System.out.println("                   " + MDLMOL);
            System.out.println("                   " + MIF);
            System.out.println("                   " + MOL2);
            System.out.println("                   " + MOPAC);
            System.out.println("                   " + MOPACIn);
            System.out.println("                   " + PDB);
            System.out.println("                   " + SDF);
            System.out.println("                   " + SWISS);
            System.out.println("                   " + SMILES);
            System.out.println("                   " + VAMP);
            System.out.println("                   " + XYZ);
            System.out.println("                   " + XML);
            System.out.println("                   " + CMLARRAY);
            System.out.println("                   ]");
            System.exit(0);
        }
        String inFile = null;
        String inFormat = null;
        String inList = null;
        String outFile = null;
        String outFormat = CML;
        String outDir = ".";
        String scriptFile = "";
        int i = 0;
        try {
            while (i < args.length) {
                if (false) {
                } else if (args[i].equals("-IN")) {
                    ++i;
                    inFile = args[i++];
                } else if (args[i].equals("-INLIST")) {
                    ++i;
                    inList = args[i++];
                } else if (args[i].equals("-INFORM")) {
                    ++i;
                    inFormat = args[i++];
                } else if (args[i].equals("-OUT")) {
                    ++i;
                    outFile = args[i++];
                } else if (args[i].equals("-OUTFORM")) {
                    ++i;
                    outFormat = args[i++];
                } else if (args[i].equals("-OUTDIR")) {
                    ++i;
                    outDir = args[i++];
                } else if (args[i].equals("-SCRIPT")) {
                    ++i;
                    scriptFile = args[i++];
                } else {
                    System.out.println("Bad arg: " + args[i]);
                }
            }
            if (inFormat == null) {
                throw new CMLException("No input format");
            }
            if (inList != null) {
                convertList(inList, inFormat, outDir, outFile, outFormat);
            } else if (inFile != null) {
                if (outFile == null) {
                    outFile = makeFileStem(inFile) + "." + getSuffixFromType(outFormat);
                }
                FileWriter fw = new FileWriter(outFile);
                convertFile(inFile, inFormat, fw, outFormat);
                fw.close();
            } else {
                throw new CMLException("No input source given");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
