package org.xmlcml.cml.legacy2cml.molecule.msds;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Nodes;
import org.xmlcml.cml.base.CMLRuntimeException;
import org.xmlcml.cml.element.CMLMolecule;
import org.xmlcml.cml.element.CMLName;
import org.xmlcml.cml.legacy2cml.AbstractHTMLConverter;
import org.xmlcml.cml.legacy2cml.AbstractLegacyConverter;
import org.xmlcml.cml.legacy2cml.EntryRecord;
import org.xmlcml.cml.legacy2cml.RDFConverter;
import org.xmlcml.euclid.Util;

/**
 * This class handles molecules/compounds as supplied by Wikipedia
 * @author Peter Murray-Rust
 * 
 */
public class WikipediaConverter extends AbstractHTMLConverter {

    /** type of input
	 * 
	 * @author pm286
	 *
	 */
    public enum Type {

        /** xml from mediawiki */
        XML, /** xml from page scrape */
        HTML
    }

    private String downloadURI;

    static final Logger logger = Logger.getLogger(WikipediaConverter.class.getName());

    /** rdf prefix*/
    public static final String PREFIX = "WIKIPEDIA";

    /** gets prefix for RDF triple abbreviation
     * @return prefix
     */
    protected String getPrefix() {
        return PREFIX;
    }

    static final String ORGANIC = "http://en.wikipedia.org/wiki/List_of_organic_compounds";

    static final String INORGANIC = "http://en.wikipedia.org/wiki/List_of_inorganic_compounds";

    private boolean drugPicture;

    private boolean iupac;

    private boolean start;

    private boolean identifier;

    private Type type;

    static {
        logger.setLevel(Level.ALL);
    }

    protected AbstractLegacyConverter getNewLegacyConverter(AbstractLegacyConverter abstractLegacyConverter) {
        if (!(abstractLegacyConverter instanceof WikipediaConverter)) {
            throw new CMLRuntimeException("can only copy WikipediaConverter");
        }
        return new WikipediaConverter((AbstractLegacyConverter) abstractLegacyConverter);
    }

    /** constructor.
	 * 
	 * @param converter
	 */
    public WikipediaConverter(AbstractLegacyConverter converter) {
        super(converter);
    }

    /** constructor.
	 */
    public WikipediaConverter() {
        super();
    }

    private void download() throws IOException {
        download(ORGANIC);
    }

    /** download from URL 
	 * fails with 403 for XML files so need to download with wget
	 */
    private Document download(String urlS) throws IOException {
        URL url = new URL(urlS);
        Document document = null;
        try {
            document = AbstractLegacyConverter.htmlTidy(url.openStream());
        } catch (Exception e) {
            throw new CMLRuntimeException("parse: " + e);
        }
        return document;
    }

    /**
     * @param inputStream
     * @param fileId
     * @throws Exception
     * @return doc (null if parsing fails)
     */
    public Document parseLegacy(InputStream inputStream, String fileId) throws Exception {
        ByteArrayInputStream bais = convertCharactersToEntitiesInStream(inputStream, 128, false);
        parsedDocumentAsCML = null;
        Type type = readType(bais);
        if (type.equals(Type.XML)) {
            parsedDocumentAsCML = readXML(fileId, bais);
        } else if (type.equals(Type.HTML)) {
            parsedDocumentAsCML = readHTML(fileId, bais);
        }
        return parsedDocumentAsCML;
    }

    private Type readType(ByteArrayInputStream bais) {
        type = null;
        byte[] array = new byte[20];
        bais.read(array, 0, 10);
        String s = new String(array);
        if (s.startsWith("<mediawiki") || s.startsWith("<?xml")) {
            type = Type.XML;
        } else if (s.startsWith("<html") || s.startsWith("<!DOCTYPE")) {
            type = Type.HTML;
        } else {
            throw new CMLRuntimeException("Cannot determine type: " + (s + "..........").substring(0, 10));
        }
        bais.reset();
        return type;
    }

    private Document readXML(String fileId, ByteArrayInputStream bais) throws IOException, CMLRuntimeException {
        this.fileId = fileId;
        Document doc = null;
        try {
            doc = new Builder().build(bais);
        } catch (Exception e) {
            throw new CMLRuntimeException("error: " + e);
        }
        CMLMolecule molecule = createMoleculeXML(doc);
        createAndAddMetadata(molecule, RDFConverter.DC_SOURCE, this.getSource());
        doc = new Document(molecule);
        fileId = translateNonIdCharsToUnderscore(fileId).trim();
        fileId = fileId.toLowerCase();
        molecule.setTitle(fileId);
        createAndAddName(molecule, fileId);
        bais.close();
        return doc;
    }

    /**
	 * @param fileId
	 * @param bais
	 * @return document or null
	 * @throws IOException
	 * @throws CMLRuntimeException
	 */
    private Document readHTML(String fileId, InputStream bais) throws IOException, CMLRuntimeException {
        Document doc = createDocFromHTML(bais, fileId);
        if (doc != null) {
            CMLMolecule molecule = createMolecule(doc);
            createAndAddMetadata(molecule, RDFConverter.DC_SOURCE, this.getSource());
            doc = new Document(molecule);
        }
        fileId = translateNonIdCharsToUnderscore(fileId).trim();
        fileId = fileId.toLowerCase();
        molecule.setId(fileId);
        createAndAddName(molecule, fileId);
        bais.close();
        return doc;
    }

    /**
	 * @param document
	 * @return mol
	 */
    protected CMLMolecule createMoleculeXML(Document document) {
        molecule = new CMLMolecule();
        Element page = AbstractLegacyConverter.getDescendantElement(document, "page");
        Element title = AbstractLegacyConverter.getChildElement(page, "title");
        Element id = AbstractLegacyConverter.getChildElement(page, "id");
        Element text = AbstractLegacyConverter.getDescendantElement(page, "text");
        if (title != null) {
            CMLName name = createAndAddName(molecule, title.getValue());
            name.setDictRef("cml:title");
        }
        if (id != null) {
            molecule.setId(id.getValue());
        }
        parseBox(text);
        molecule.setConvention(this.getPrefix());
        return molecule;
    }

    private void parseBox(Element text) {
        if (text == null) {
            currentLog.addString(EntryRecord.Type.WARNING, "No text section ");
        } else {
            String value = text.getValue().trim();
            while (true) {
                value = value.trim();
                if (!value.startsWith("{{")) {
                    break;
                } else {
                    int last = Util.indexOfBalancedBracket(C_LCURLY, value);
                    if (last == -1) {
                        throw new CMLRuntimeException("Bad brackets in: " + value);
                    }
                    value = value.substring(2, value.length() - 2);
                    parseSections(value);
                }
            }
        }
    }

    private void parseSections(String value) {
        String vv = value.toLowerCase();
        if (vv.startsWith("chembox new")) {
            int idx = value.indexOf(" Section");
            if (idx == -1) {
                idx = value.length();
            }
            parseChemBoxHeader(value.substring("Chembox new".length(), idx));
            value = value.substring(idx);
            parseChemBoxSections(value);
        } else if (vv.startsWith("natorganicbox")) {
            currentLog.addString(EntryRecord.Type.WARNING, "Cannot yet parse: NatOrganicBox");
        } else if (vv.startsWith("drugbox")) {
            currentLog.addString(EntryRecord.Type.WARNING, "Cannot yet parse: DrugBox");
        } else {
            currentLog.addString(EntryRecord.Type.WARNING, "Cannot parse Box : " + value);
        }
    }

    private void parseChemBoxHeader(String value) {
        value = value.trim().replace(S_NEWLINE, S_SPACE);
        String[] ss = value.split(S_BACKSLASH + S_PIPE);
        for (String s : ss) {
            addNameValue(s);
        }
    }

    /**
	 * @param s
	 * @throws CMLRuntimeException
	 */
    private void addNameValue(String s) throws CMLRuntimeException {
        if (s.trim().length() == 0) {
        } else if (s.indexOf(S_EQUALS) == -1) {
            currentLog.addString(EntryRecord.Type.WARNING, "No EQUALS: " + s);
        } else {
            String[] ss = s.trim().split(S_EQUALS);
            if (ss.length == 1) {
            } else if (ss[1].trim().length() != 0) {
                if (!addNameValue(ss[0].trim(), ss[1].trim())) {
                    currentLog.addString(EntryRecord.Type.WARNING, " ... unknown " + ss[0] + " == " + ss[1]);
                }
            } else {
                currentLog.addString(EntryRecord.Type.ERROR, "something went wrong: " + Util.truncateAndAddNewlinesAndEllipsis(s, 20));
            }
        }
    }

    /**
	 * @param sss
	 * @param sss0
	 * @throws CMLRuntimeException
	 */
    private boolean addNameValue(String nn, String vv) throws CMLRuntimeException {
        CMLName name;
        vv = vv.trim();
        vv = processEntitiesAndSpaces(vv);
        if (vv.indexOf("&") != -1) {
            currentLog.addString(EntryRecord.Type.ERROR, "... unresolved entity " + vv);
        }
        boolean parsed = true;
        if (false) {
        } else if (vv.trim().length() == 0) {
        } else if (nn.startsWith("Section")) {
        } else if (nn.startsWith("ExternalMSDS") || nn.startsWith("page") || nn.startsWith("last") || nn.startsWith("first") || nn.startsWith("coauthors") || nn.startsWith("encyclopedia") || nn.startsWith("publisher") || nn.startsWith("location") || nn.startsWith("month") || nn.startsWith("author") || nn.startsWith("isbn") || nn.startsWith("url") || nn.startsWith("title") || nn.startsWith("journal") || nn.startsWith("year") || nn.startsWith("volume") || nn.startsWith("issue") || nn.startsWith("pages") || nn.startsWith("doi") || nn.startsWith("date") || nn.startsWith("FlashPt") || nn.startsWith("Function") || nn.startsWith("MolShape") || nn.startsWith("RPhrases") || nn.startsWith("OtherCp") || nn.startsWith("OtherFunc") || nn.startsWith("NFPA") || nn.startsWith("SPhrases") || nn.startsWith("Autoignition") || nn.startsWith("ExploLimits") || nn.startsWith("MainHazard")) {
        } else if (nn.equals("Appearance")) {
            createAndAddProperty(molecule, "cml:appear", vv);
        } else if (nn.equals("BoilingPt")) {
            createAndAddDoubleProperty(molecule, "cml:bpt", vv, "units:celsius");
        } else if (nn.startsWith("CASNo")) {
            createAndAddIdentifier(Convention.CAS, molecule, vv);
        } else if (nn.startsWith("Density")) {
            createAndAddDoubleProperty(molecule, "cml:density", vv, "units:g.cm-3");
        } else if (nn.startsWith("Dipole")) {
            createAndAddDoubleProperty(molecule, "cml:dipole", vv, "units:debye");
        } else if (nn.startsWith("Formula")) {
            createAndAddFormula(molecule, vv);
        } else if (nn.startsWith("Image")) {
        } else if (nn.equals("IUPACName")) {
            name = createAndAddName(molecule, vv);
            name.setConvention(Convention.IUPAC.v);
        } else if (nn.equals("MolarMass")) {
            createAndAddDoubleProperty(molecule, "cml:molarMass", vv, "units:dalton");
        } else if (nn.equals("pKa")) {
            createAndAddDoubleProperty(molecule, "cml:pka", vv, "units:none");
        } else if (nn.equals("MeltingPt")) {
            createAndAddDoubleProperty(molecule, "cml:mpt", vv, "units:celsius");
        } else if (nn.equals("Name")) {
            name = createAndAddName(molecule, vv);
            name.setDictRef("cml:preferredName");
        } else if (nn.equals("OtherNames")) {
            String[] ss = vv.split("<br />");
            for (String s : ss) {
                name = createAndAddName(molecule, s.trim());
            }
        } else if (nn.startsWith("PubChem")) {
            createAndAddIdentifier(Convention.PUBCHEM, molecule, vv);
        } else if (nn.equals("Reference")) {
            createAndAddMetadata(molecule, RDFConverter.DC_SOURCE, vv);
        } else if (nn.startsWith("SMILES")) {
            createAndAddSMILES(molecule, vv);
        } else if (nn.startsWith("Solubility")) {
            createAndAddDoubleProperty(molecule, "cml:solubility", vv, "units:unk");
        } else if (nn.equals("Solvent")) {
            createAndAddProperty(molecule, "cml:solubility", vv);
        } else if (nn.equals("SystematicName")) {
            String[] ss = vv.split("S_SPACE");
            for (String s : ss) {
                name = createAndAddName(molecule, s.trim());
                name.setDictRef("dictRef:systematic");
            }
        } else if (nn.startsWith("Viscosity")) {
            createAndAddDoubleProperty(molecule, "cml:vaporPressure", vv, "units:unk");
        } else if (nn.startsWith("Viscosity")) {
            createAndAddDoubleProperty(molecule, "cml:viscosity", vv, "units:unk");
        } else {
            parsed = false;
        }
        return parsed;
    }

    private void parseChemBoxSections(String value) {
        value = value.trim();
        while (true) {
            int idx = value.indexOf("| Section");
            if (idx == -1) {
                break;
            }
            value = value.substring(idx + "| Section".length());
            idx = value.indexOf(S_EQUALS);
            String ss = value.substring(0, idx);
            int ii = Integer.parseInt(ss.trim());
            idx = value.indexOf(S_LCURLY + S_LCURLY);
            if (idx != -1) {
                value = value.substring(idx);
                idx = Util.indexOfBalancedBracket(C_LCURLY, value);
                if (idx == -1) {
                    throw new CMLRuntimeException("Unbalanced {{ in :" + value);
                }
                parseSection(value.substring(2, value.length() - 2));
                value = value.substring(idx + 1);
            }
        }
    }

    private void parseSection(String v) {
        String[] ss = v.split(S_BACKSLASH + S_PIPE);
        String ss0 = ss[0].trim();
        String ss0l = ss0.toLowerCase();
        if (ss0l.startsWith("chembox identifiers") || ss0l.startsWith("chembox properties") || ss0l.startsWith("chembox structure")) {
            for (int i = 2; i < ss.length; i++) {
                addNameValue(ss[i]);
            }
        } else if (ss0l.startsWith("chembox related")) {
        } else if (ss0l.startsWith("chembox hazards")) {
        } else {
            System.out.println("SS0 " + ss0 + " ... " + fileId + " ... " + v.substring(0, Math.min(v.length(), 20)) + "...");
        }
    }

    /**
	 * @param document
	 * @return mol
	 */
    protected CMLMolecule createMolecule(Document document) {
        molecule = new CMLMolecule();
        Element body = (Element) document.query("//*[local-name()='body']").get(0);
        Nodes compounds = body.query(".//*[local-name()='table' and @class='toccolours']");
        Nodes drugs = body.query(".//*[local-name()='table' and @id='drugInfoBox']");
        if (drugs.size() > 0) {
            drugs(drugs);
        } else if (compounds.size() > 0) {
            compounds(compounds);
        } else {
            System.err.println("Cannot determine type of entry: " + fileId);
        }
        molecule.setId(fileId);
        molecule.setConvention(this.getPrefix());
        return molecule;
    }

    /**
	 * @param compounds
	 */
    private void compounds(Nodes compounds) {
        Element table = (Element) compounds.get(0);
        Elements trs = table.getChildElements("tr");
        String currentRow = null;
        start = true;
        identifier = false;
        for (int i = 0; i < trs.size(); i++) {
            currentRow = addCompoundRow(trs.get(i), currentRow);
        }
        if (!identifier) {
        }
    }

    /**
	 * @param compounds
	 */
    private void drugs(Nodes drugs) {
        Element table = (Element) drugs.get(0);
        Elements trs = table.getChildElements("tr");
        String currentRow = null;
        for (int i = 0; i < trs.size(); i++) {
            currentRow = addDrugRow(trs.get(i), currentRow);
        }
    }

    private String addCompoundRow(Element row, String rowTitle) {
        String runningTitle = rowTitle;
        Nodes ths = row.query("./*[local-name()='th']");
        Nodes tds = row.query("./*[local-name()='td']");
        if (false) {
        } else if (ths.size() == 1) {
            runningTitle = ths.get(0).getValue();
            runningTitle = runningTitle.replace(S_NEWLINE, S_SPACE).trim();
        } else if (tds.get(0).query(".//*[contains(@href, '.png')]").size() > 0) {
        } else if (tds.size() == 2 && tds.get(0).query(".//*[.='Other names']").size() > 0) {
            createAndAddSynonyms(molecule, tds.get(1).getValue());
        } else if (tds.size() == 2 && tds.get(0).query(".//*[contains(., 'IUPAC')]").size() > 0) {
            CMLName name = createAndAddName(molecule, tds.get(1).getValue());
            name.setConvention(Convention.IUPAC.v);
        } else if ("Hazards".equals(rowTitle)) {
            hazard(row);
        } else if ("Identifiers".equals(rowTitle)) {
            identifier = true;
            identifier(row);
            start = false;
        } else if ("Related Compounds".equals(rowTitle)) {
            relatedCompounds(row);
        } else if ("Structure".equals(rowTitle)) {
        } else if ("Supplementary data page".equals(rowTitle)) {
        } else if ("Thermochemistry".equals(rowTitle)) {
            thermochemistry(row);
        } else if ("Properties".equals(rowTitle)) {
            property(row);
        } else if (!start) {
            System.err.println("Unknown row title for compound (" + fileId + "):" + rowTitle + ":");
        }
        return runningTitle;
    }

    private String addDrugRow(Element row, String rowTitle) {
        String runningTitle = null;
        Nodes tds = row.query("./*[local-name()='td']");
        if (false) {
        } else if (tds.size() == 0) {
            currentLog.addString(EntryRecord.Type.ERROR, "Empty tr in drug");
        } else if (tds.size() == 1) {
            runningTitle = tds.get(0).getValue();
            runningTitle = runningTitle.replace(S_NEWLINE, S_SPACE);
        } else if (rowTitle == null && row.query(".//@href[contains(., '.png')]").size() > 0) {
            drugPicture = true;
        } else if (rowTitle == null && drugPicture && row.query("./*[local-name()='td' and *[local-name()='div']]").size() > 0) {
            createAndAddName(molecule, row.getValue());
        } else if (rowTitle == null && drugPicture && row.getValue().indexOf("IUPAC") != -1) {
            iupac = true;
        } else if (rowTitle == null && iupac) {
            createAndAddName(molecule, row.getValue()).setConvention(Convention.IUPAC.v);
            iupac = false;
        } else if ("Hazards".equals(rowTitle)) {
            hazard(row);
            runningTitle = rowTitle;
        } else if ("Identifiers".equals(rowTitle)) {
            identifier(row);
            runningTitle = rowTitle;
        } else if ("Chemical data".equals(rowTitle) || "Physical data".equals(rowTitle)) {
            property(row);
            runningTitle = rowTitle;
        } else if ("Pharmacokinetic data".equals(rowTitle)) {
        } else if ("Therapeutic considerations".equals(rowTitle)) {
        } else if (rowTitle != null) {
            currentLog.addString(EntryRecord.Type.ERROR, "Unknown row title for drug (" + fileId + "):" + rowTitle + ":");
        }
        return runningTitle;
    }

    private void identifier(Element row) {
        Nodes tds = row.query(".//*[local-name()='td']");
        if (tds.size() == 2) {
            String type = tds.get(0).getValue();
            type = type.replace(S_NEWLINE, S_SPACE);
            String content = tds.get(1).getValue();
            if (false) {
            } else if (type.equals("3DMet")) {
                createAndAddIdentifier(Convention.MET3D, molecule, content);
            } else if (type.equals("ATC code")) {
                createAndAddIdentifier(Convention.ATC, molecule, content);
            } else if (type.equals("Beilstein Reference") || type.equals("Beilstein")) {
                createAndAddIdentifier(Convention.BEILSTEIN, molecule, content);
            } else if (type.equals("CAS number") || type.equals("CAS")) {
                createAndAddIdentifier(Convention.CAS, molecule, content);
            } else if (type.equals("DrugBank")) {
                createAndAddIdentifier(Convention.DRUGBANK, molecule, content);
            } else if (type.equals("ChEBI number") || type.equals("ChEBI")) {
                createAndAddIdentifier(Convention.CHEBI, molecule, content);
            } else if (type.equals("EINECS number") || type.equals("EINECS")) {
                createAndAddIdentifier(Convention.EINECS, molecule, content);
            } else if (type.equals("Gmelin Reference") || type.equals("Gmelin")) {
                createAndAddIdentifier(Convention.GMELIN, molecule, content);
            } else if (type.equals("InChI")) {
                createAndAddIdentifier(Convention.INCHI, molecule, content);
            } else if (type.equals("KEGG")) {
                createAndAddIdentifier(Convention.KEGG, molecule, content);
            } else if (type.equals("MeSH")) {
                createAndAddIdentifier(Convention.MESH, molecule, content);
            } else if (type.equals("PubChem")) {
                createAndAddIdentifier(Convention.PUBCHEM, molecule, content);
            } else if (type.equals("RTECS number")) {
                createAndAddIdentifier(Convention.RTECS, molecule, content);
            } else if (type.equals("SMILES")) {
                createAndAddSMILES(molecule, content);
            } else {
                currentLog.addString(EntryRecord.Type.ERROR, "Unknown identifier type: " + type);
            }
        } else {
        }
    }

    private void property(Element row) {
        Nodes tds = row.query(".//*[local-name()='td']");
        if (tds.size() == 2) {
            String type = tds.get(0).getValue();
            type = type.replace(S_NEWLINE, S_SPACE);
            String data = tds.get(1).getValue();
            if (false) {
            } else if (type.equals("Acidity (pKa)")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:pka", data);
            } else if (type.equals("Basicity (pKb)")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:pkb", data);
            } else if (type.equals("Appearance")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:appear", data);
            } else if (type.equals("Boiling point")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:bpt", data);
            } else if (type.equals("Density")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:density", data);
            } else if (type.equals("Dipole moment")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:dipole", data);
            } else if (type.equals("Formula")) {
                try {
                    createAndAddFormula(molecule, data);
                } catch (CMLRuntimeException e) {
                    currentLog.addString(EntryRecord.Type.ERROR, "Bad formula for (" + fileId + ") " + e.getMessage());
                }
            } else if (type.equals("kH")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:kh", data);
            } else if (type.equals("log P")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:logP", data);
            } else if (type.equals("Molecular formula")) {
                try {
                    createAndAddFormula(molecule, data);
                } catch (CMLRuntimeException e) {
                    currentLog.addString(EntryRecord.Type.ERROR, "Bad formula: " + e.getMessage());
                }
            } else if (type.equals("Melting point") || type.equals("Melt. point")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:mpt", data);
            } else if (type.equals("Molar mass") || type.equals("Mol. mass")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:molarMass", data);
            } else if (type.equals("Refractive index (nD)")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:refractiveIndex", data);
            } else if (type.equals("SMILES")) {
                createAndAddSMILES(molecule, data);
            } else if (type.equals("Solubility in other solvents")) {
            } else if (type.equals("Solubility")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:solubility", data);
            } else if (type.equals("Solubility in water")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:waterSolubility", data);
            } else if (type.equals("Spec. rot")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:specRot", data);
            } else if (type.equals("Synonyms")) {
                createAndAddSynonyms(molecule, data);
            } else if (type.equals("Vapor pressure")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:vaporPressure", data);
            } else if (type.equals("Viscosity")) {
                createAndAddGuessedPropertyTypeAndUnits(molecule, "cml:viscosity", data);
            } else {
                currentLog.addString(EntryRecord.Type.ERROR, "Unknown property type: " + type);
            }
        } else {
        }
    }

    /** currently no-op
     * @param row
     */
    private void hazard(Element row) {
    }

    /** currently no-op
     * @param row
     */
    private void relatedCompounds(Element row) {
    }

    /** currently no-op
     * @param row
     */
    private void thermochemistry(Element row) {
    }

    /**
     * Provides a command line and graphical user interface to PTCLConverter.
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            usage();
            System.exit(0);
        }
        WikipediaConverter wikip = new WikipediaConverter();
        wikip.processArgs1(args);
    }

    /**
	 * @param args
	 */
    private void processArgs1(String[] args) {
        int i = 0;
        while (i < args.length) {
            if (args[i].equalsIgnoreCase("-FOO")) {
                i++;
            } else if (args[i].equalsIgnoreCase("-DOWNLOAD")) {
                setDownloadURI(args[++i]);
                i++;
            } else {
                i = this.processArgs(args, i);
            }
        }
        try {
            if (downloadURI != null) {
                download();
            } else {
                this.runIterator();
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Exception: " + e);
        }
    }

    /**
	 * 
	 */
    protected static void usage() {
        System.out.println("Usage: org.xmlcml.legacy.molecule.msds.WikipediaConverter [options]");
        System.out.println("       -FOO (does nothing)");
        System.out.println("       -DOWNLOAD file (get XML from WP site)");
        AbstractHTMLConverter.usage();
    }

    /**
	 * @return the downloadURI
	 */
    public String getDownloadURI() {
        return downloadURI;
    }

    /**
	 * @param downloadURI the downloadURI to set
	 */
    public void setDownloadURI(String downloadURI) {
        this.downloadURI = downloadURI;
    }
}

;
