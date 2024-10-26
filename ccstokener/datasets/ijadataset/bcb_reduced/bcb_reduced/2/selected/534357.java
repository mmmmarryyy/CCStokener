package xbrlcore.taxonomy;

import static xbrlcore.taxonomy.Namespace_.toJDOM;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import xbrlcore.constants.GeneralConstants;
import xbrlcore.constants.NamespaceConstants;
import xbrlcore.exception.TaxonomyCreationException;
import xbrlcore.exception.XBRLException;
import xbrlcore.linkbase.CalculationLinkbase;
import xbrlcore.linkbase.DefinitionLinkbase;
import xbrlcore.linkbase.LabelLinkbase;
import xbrlcore.linkbase.PresentationLinkbase;
import xbrlcore.linkbase.ReferenceLinkbase;
import xbrlcore.logging.LogInterface;
import xbrlcore.logging.LogInterface.LogLevel;
import xbrlcore.taxonomy.TupleDefinition.TupleType;

/**
 * 
 * This class loads a taxonomy from xml files and invokes abstract methods
 * and passes the loaded data in abstract form to delegate any further work
 * to sub classes.<br/><br/>
 * 
 * @param <ResultType> the data type to return as the concrete taxonomy representation
 * @param <TS> taxonomy scheme type
 * 
 * @author Daniel Hamm
 * @author Marvin Froehlich (INFOLOG GmbH)
 */
public abstract class AbstractTaxonomyLoader<ResultType, TS> {

    protected final LogInterface log;

    private static final Class<?> LOG_CHANNEL = AbstractTaxonomyLoader.class;

    protected static final String PRESENTATION_LB_NAME = PresentationLinkbase.NAME;

    protected static final String LABEL_LB_NAME = LabelLinkbase.NAME;

    protected static final String DEFINITION_LB_NAME = DefinitionLinkbase.NAME;

    protected static final String CALCULATION_LB_NAME = CalculationLinkbase.NAME;

    protected static final String REFERENCE_LB_NAME = ReferenceLinkbase.NAME;

    private FileLoader fileLoader = new FileLoader();

    private String taxPath;

    private List<String> loadedTaxonomies;

    private List<String> builtTaxonomies;

    private Map<String, List<String>> includedTaxonomies;

    private SAXBuilder saxBuilder;

    private Map<String, Document> taxonomyNameToDocument;

    private Map<String, TS> taxonomyNameToSchema;

    private Map<String, Set<URL>> builtLinkbases;

    private Map<String, Set<URL>> builtArcs;

    private Map<String, Set<String>> taxonomyNameToRoleTypes;

    private Map<String, String> taxonomyNameToRefBase;

    private Map<URL, Document> instanceNameToDocument;

    private Map<String, List<Element>> taxonomyNameToLinkBaseRefElements;

    protected AbstractTaxonomyLoader(LogInterface log) {
        this.log = log;
    }

    /**
     * Initializes some objects.
     * 
     * @param taxonomySchemaName
     * @param releaseDate
     * 
     * @throws TaxonomyCreationException
     * @throws XBRLException 
     */
    protected abstract void init(String taxonomySchemaName, String releaseDate) throws XBRLException, TaxonomyCreationException;

    /**
     * Creates and returns a discoverable taxonomy set (DTS), represented
     * through a DiscoverableTaxonomySet object. The DTS is created based on the
     * given file, which is expected to be an XBRL taxonomy. If this taxonomy
     * imports other taxonomies, they are also created by this method and can be
     * obtained afterwards via the according methods.
     * 
     * @param taxonomyFile File containing the XBRL taxonomy.
     * @param releaseDate the release date as 'YYYY-MM-DD'
     * @return Discoverable taxonomy set, based on the given file, including all
     *         imported taxonomies.
     * @throws IOException
     * @throws JDOMException
     * @throws TaxonomyCreationException
     * @throws XBRLException 
     */
    public final ResultType loadTaxonomy(File taxonomyFile, String releaseDate) throws IOException, JDOMException, TaxonomyCreationException, XBRLException {
        return loadTaxonomy(taxonomyFile, releaseDate, null);
    }

    /**
     * Creates and returns a discoverable taxonomy set (DTS), represented
     * through a DiscoverableTaxonomySet object. The DTS is created based on the
     * given file, which is expected to be an XBRL taxonomy. If this taxonomy
     * imports other taxonomies, they are also created by this method and can be
     * obtained afterwards via the according methods.
     * 
     * @param taxonomyFile File containing the XBRL taxonomy.
     * @param releaseDate the release date as 'YYYY-MM-DD'
     * @param fileLoader the file loader
     * @return Discoverable taxonomy set, based on the given file, including all
     *         imported taxonomies.
     * @throws IOException
     * @throws JDOMException
     * @throws TaxonomyCreationException
     * @throws XBRLException 
     */
    public final ResultType loadTaxonomy(File taxonomyFile, String releaseDate, FileLoader fileLoader) throws IOException, JDOMException, TaxonomyCreationException, XBRLException {
        return loadTaxonomy(taxonomyFile.toURI().toURL(), releaseDate, fileLoader);
    }

    /**
     * Creates and returns a discoverable taxonomy set (DTS), represented
     * through a DiscoverableTaxonomySet object. The DTS is created based on the
     * given file, which is expected to be an XBRL taxonomy. If this taxonomy
     * imports other taxonomies, they are also created by this method and can be
     * obtained afterwards via the according methods.
     * 
     * @param taxonomyFile File containing the XBRL taxonomy.
     * @param releaseDate the release date as 'YYYY-MM-DD'
     * @return Discoverable taxonomy set, based on the given file, including all
     *         imported taxonomies.
     * @throws IOException
     * @throws JDOMException
     * @throws TaxonomyCreationException
     * @throws XBRLException 
     */
    public final ResultType loadTaxonomy(URL taxonomyFile, String releaseDate) throws IOException, JDOMException, TaxonomyCreationException, XBRLException {
        return loadTaxonomy(taxonomyFile, releaseDate, null);
    }

    /**
     * 
     * @param taxonomyFileName
     * @return taxoPostfix
     */
    protected List<String> getAdditionalTaxonomyPostfixes(String taxonomyFileName) {
        ArrayList<String> list = new ArrayList<String>();
        list.add("shell");
        list.add("shell-fiscal");
        return list;
    }

    /**
     * Creates and returns a discoverable taxonomy set (DTS), represented
     * through a DiscoverableTaxonomySet object. The DTS is created based on the
     * given file, which is expected to be an XBRL taxonomy. If this taxonomy
     * imports other taxonomies, they are also created by this method and can be
     * obtained afterwards via the according methods.
     * 
     * @param taxonomyFile File containing the XBRL taxonomy.
     * @param releaseDate the release date as 'YYYY-MM-DD'
     * @param fileLoader the file loader
     * @return Discoverable taxonomy set, based on the given file, including all
     *         imported taxonomies.
     * @throws IOException
     * @throws JDOMException
     * @throws TaxonomyCreationException
     * @throws XBRLException 
     */
    public ResultType loadTaxonomy(URL taxonomyFile, String releaseDate, FileLoader fileLoader) throws IOException, JDOMException, TaxonomyCreationException, XBRLException {
        long t00 = System.nanoTime();
        String taxonomyFileName = getNameFromURL(taxonomyFile);
        log.log(LogLevel.INFO, LOG_CHANNEL, "Processing discoverable taxonomy set " + taxonomyFileName + "...");
        if (fileLoader != null) this.fileLoader = fileLoader;
        loadedTaxonomies = new ArrayList<String>();
        builtTaxonomies = new ArrayList<String>();
        includedTaxonomies = new HashMap<String, List<String>>();
        saxBuilder = new SAXBuilder();
        taxonomyNameToDocument = new HashMap<String, Document>();
        taxonomyNameToSchema = new HashMap<String, TS>();
        builtLinkbases = new HashMap<String, Set<URL>>();
        builtArcs = new HashMap<String, Set<URL>>();
        taxonomyNameToRoleTypes = new HashMap<String, Set<String>>();
        taxonomyNameToRefBase = new HashMap<String, String>();
        instanceNameToDocument = new HashMap<URL, Document>();
        taxonomyNameToLinkBaseRefElements = new HashMap<String, List<Element>>();
        init(taxonomyFileName, releaseDate);
        _loadTaxonomy(taxonomyFile, false);
        ResultType result = finish(getTaxonomySchema(taxonomyFileName), taxonomyFileName);
        cleanup();
        log.log(LogLevel.DEBUG, LOG_CHANNEL, "> Total: " + ((System.nanoTime() - t00) / 1000000000.0f) + " seconds");
        return result;
    }

    private void _loadTaxonomy(URL taxonomyFile, boolean imported) throws IOException, JDOMException, TaxonomyCreationException, XBRLException {
        long t0 = System.nanoTime();
        String taxonomyFileName = getNameFromURL(taxonomyFile);
        String refBaseURL = getRefURLBase(taxonomyFile);
        List<String> additionalTaxonomyPostfixes = getAdditionalTaxonomyPostfixes(taxonomyFileName);
        log.log(LogLevel.INFO, LOG_CHANNEL, "Processing discoverable taxonomy set " + taxonomyFileName + "...");
        loadedTaxonomies.add(taxonomyFileName);
        taxonomyNameToRefBase.put(taxonomyFileName, refBaseURL);
        taxPath = null;
        if (taxonomyFile.getProtocol().equals("file")) taxPath = getFileFromURL(taxonomyFile).getParentFile().getAbsolutePath() + File.separator;
        log.log(LogLevel.DEBUG, LOG_CHANNEL, "Collecting imported taxonomies...");
        Set<URL> importedTaxonomies = new HashSet<URL>();
        collectIncludedTaxonomies(taxonomyFile, refBaseURL, false, includedTaxonomies, importedTaxonomies, loadedTaxonomies);
        for (String postFix : additionalTaxonomyPostfixes) {
            URL url = getRefURLTaxo(refBaseURL, postFix);
            String name = getNameFromURL(url);
            if (!loadedTaxonomies.contains(name)) {
                loadedTaxonomies.add(name);
                taxonomyNameToRefBase.put(name, refBaseURL);
                collectIncludedTaxonomies(url, refBaseURL, true, includedTaxonomies, importedTaxonomies, loadedTaxonomies);
            }
        }
        log.log(LogLevel.DEBUG, LOG_CHANNEL, "> Read XML files to JDOM structures: " + ((System.nanoTime() - t0) / 1000000000.0f) + " seconds");
        t0 = System.nanoTime();
        Set<String> references = new HashSet<String>();
        int firstTaxoIndex = 0;
        do {
            references.clear();
            buildLinkbase(REFERENCE_LB_NAME, GeneralConstants.XBRL_LINKBASE_ROLE_REFERENCE, GeneralConstants.XBRL_LINKBASE_LINK_REFERENCE, null, loadedTaxonomies, firstTaxoIndex, references);
            for (String reference : references) {
                if (!reference.equals(taxonomyFileName)) {
                    collectIncludedTaxonomies(this.fileLoader.getFile(taxPath, null, reference), refBaseURL, false, includedTaxonomies, importedTaxonomies, loadedTaxonomies);
                    if (!loadedTaxonomies.contains(reference)) {
                        loadedTaxonomies.add(reference);
                        taxonomyNameToRefBase.put(reference, refBaseURL);
                    }
                }
            }
            firstTaxoIndex = loadedTaxonomies.size();
        } while (!references.isEmpty());
        builtLinkbases.clear();
        for (URL importedTaxonomy : importedTaxonomies) {
            List<String> tmpLoadedTaxonomies = new ArrayList<String>();
            tmpLoadedTaxonomies.addAll(loadedTaxonomies);
            loadedTaxonomies.clear();
            String oldTaxPath = taxPath;
            _loadTaxonomy(importedTaxonomy, true);
            taxPath = oldTaxPath;
            for (String lt : loadedTaxonomies) {
                if (!tmpLoadedTaxonomies.contains(lt)) {
                    tmpLoadedTaxonomies.add(lt);
                }
            }
            loadedTaxonomies.clear();
            loadedTaxonomies.addAll(tmpLoadedTaxonomies);
        }
        log.log(LogLevel.DEBUG, LOG_CHANNEL, "> Collect reference link base file names and read XML files to JDOM structures: " + ((System.nanoTime() - t0) / 1000000000.0f) + " seconds");
        log.log(LogLevel.DEBUG, LOG_CHANNEL, "Building taxonomy schemas...");
        List<String> taxonomiesToBuild = new ArrayList<String>();
        for (String lt : loadedTaxonomies) {
            if (!builtTaxonomies.contains(lt)) {
                builtTaxonomies.add(lt);
                taxonomiesToBuild.add(lt);
            }
        }
        buildTaxonomySchemas(taxonomiesToBuild);
        t0 = System.nanoTime();
        if (!imported) {
            log.log(LogLevel.DEBUG, LOG_CHANNEL, "Interpreting presentation link base and storing to maps/lists...");
            buildLinkbase(PRESENTATION_LB_NAME, GeneralConstants.XBRL_LINKBASE_ROLE_PRESENTATION, GeneralConstants.XBRL_LINKBASE_LINK_PRESENTATION, refBaseURL, loadedTaxonomies, 0, null);
            log.log(LogLevel.DEBUG, LOG_CHANNEL, "Interpreting label link base and storing to maps/lists...");
            buildLinkbase(LABEL_LB_NAME, GeneralConstants.XBRL_LINKBASE_ROLE_LABEL, GeneralConstants.XBRL_LINKBASE_LINK_LABEL, refBaseURL, loadedTaxonomies, 0, null);
            log.log(LogLevel.DEBUG, LOG_CHANNEL, "Interpreting definition link base and storing to maps/lists...");
            buildLinkbase(DEFINITION_LB_NAME, GeneralConstants.XBRL_LINKBASE_ROLE_DEFINITION, GeneralConstants.XBRL_LINKBASE_LINK_DEFINITION, refBaseURL, loadedTaxonomies, 0, null);
            log.log(LogLevel.DEBUG, LOG_CHANNEL, "Interpreting calculation link base and storing to maps/lists...");
            buildLinkbase(CALCULATION_LB_NAME, GeneralConstants.XBRL_LINKBASE_ROLE_CALCULATION, GeneralConstants.XBRL_LINKBASE_LINK_CALCULATION, refBaseURL, loadedTaxonomies, 0, null);
            log.log(LogLevel.DEBUG, LOG_CHANNEL, "Interpreting reference link base and storing to maps/lists...");
            buildLinkbase(REFERENCE_LB_NAME, GeneralConstants.XBRL_LINKBASE_ROLE_REFERENCE, GeneralConstants.XBRL_LINKBASE_LINK_REFERENCE, refBaseURL, loadedTaxonomies, 0, null);
            log.log(LogLevel.DEBUG, LOG_CHANNEL, "> Interpret link bases and store to maps/lists: " + ((System.nanoTime() - t0) / 1000000000.0f) + " seconds");
            t0 = System.nanoTime();
        }
        if (!imported) {
            buildArcs(PRESENTATION_LB_NAME, GeneralConstants.XBRL_LINKBASE_ROLE_PRESENTATION, GeneralConstants.XBRL_LINKBASE_LINK_PRESENTATION, GeneralConstants.XBRL_LINKBASE_ARC_PRESENTATION);
            buildArcs(LABEL_LB_NAME, GeneralConstants.XBRL_LINKBASE_ROLE_LABEL, GeneralConstants.XBRL_LINKBASE_LINK_LABEL, GeneralConstants.XBRL_LINKBASE_ARC_LABEL);
            buildArcs(DEFINITION_LB_NAME, GeneralConstants.XBRL_LINKBASE_ROLE_DEFINITION, GeneralConstants.XBRL_LINKBASE_LINK_DEFINITION, GeneralConstants.XBRL_LINKBASE_ARC_DEFINITION);
            buildArcs(CALCULATION_LB_NAME, GeneralConstants.XBRL_LINKBASE_ROLE_CALCULATION, GeneralConstants.XBRL_LINKBASE_LINK_CALCULATION, GeneralConstants.XBRL_LINKBASE_ARC_CALCULATION);
            buildArcs(REFERENCE_LB_NAME, GeneralConstants.XBRL_LINKBASE_ROLE_REFERENCE, GeneralConstants.XBRL_LINKBASE_LINK_REFERENCE, GeneralConstants.XBRL_LINKBASE_ARC_REFERENCE);
            log.log(LogLevel.DEBUG, LOG_CHANNEL, "> Interpret Arcs and store to maps/lists: " + ((System.nanoTime() - t0) / 1000000000.0f) + " seconds");
            t0 = System.nanoTime();
        }
    }

    private static String deriveNamespacePrefix(String uri) {
        if (uri.toLowerCase().indexOf("gaap") >= 0) return "de-gaap-ci";
        if (uri.toLowerCase().indexOf("gcd") >= 0) return "de-gcd";
        String prefix = uri.substring(uri.lastIndexOf("/") + 1, uri.length());
        return prefix;
    }

    private void buildTaxonomySchemas(Iterable<String> loadedTaxonomies) throws TaxonomyCreationException {
        for (String currTaxonomySchemaName : loadedTaxonomies) {
            log.log(LogLevel.INFO, LOG_CHANNEL, "Processing taxonomy schema " + currTaxonomySchemaName + "...");
            Document taxonomySource = taxonomyNameToDocument.get(currTaxonomySchemaName);
            if (taxonomySource != null) {
                String targetNamespaceURI = taxonomySource.getRootElement().getAttributeValue("targetNamespace");
                Namespace targetNamespace = null;
                if (targetNamespaceURI != null) {
                    String targetNamespacePrefix = deriveNamespacePrefix(targetNamespaceURI);
                    targetNamespace = Namespace.getNamespace(targetNamespacePrefix, targetNamespaceURI);
                }
                TS taxonomySchema = buildTaxonomySchema(currTaxonomySchemaName, targetNamespace);
                taxonomyNameToSchema.put(currTaxonomySchemaName, taxonomySchema);
                Element rootElement = taxonomySource.getRootElement();
                @SuppressWarnings("unchecked") List<Element> conceptElementList = rootElement.getChildren("element", toJDOM(NamespaceConstants.XSD_NAMESPACE));
                createConcepts(taxonomySchema, conceptElementList);
                Set<String> roleTypeIDs = new HashSet<String>();
                getRoleTypes(taxonomySource, taxonomySchema, roleTypeIDs);
                taxonomyNameToRoleTypes.put(currTaxonomySchemaName, roleTypeIDs);
                finishTaxonomySchema(taxonomySchema, currTaxonomySchemaName);
            }
        }
    }

    protected abstract TS buildTaxonomySchema(String taxonomySchemaName, Namespace targetNamespace) throws TaxonomyCreationException;

    private ConceptTypeRestriction parseComplexTypeSimpleContent(Element conceptElement) {
        Element complexType = conceptElement.getChild("complexType", toJDOM(NamespaceConstants.XSD_NAMESPACE));
        if (complexType == null) return null;
        Element simpleContent = complexType.getChild("simpleContent", toJDOM(NamespaceConstants.XSD_NAMESPACE));
        if (simpleContent == null) return null;
        Element restriction = simpleContent.getChild("restriction", toJDOM(NamespaceConstants.XSD_NAMESPACE));
        if (restriction == null) return null;
        String baseAttrib = restriction.getAttributeValue("base");
        Element patternElem = restriction.getChild("pattern", toJDOM(NamespaceConstants.XSD_NAMESPACE));
        String pattern = null;
        if (patternElem != null) pattern = patternElem.getAttributeValue("value");
        Element attributeGroupElem = restriction.getChild("attributeGroup", toJDOM(NamespaceConstants.XSD_NAMESPACE));
        String attributeGroup = null;
        if (attributeGroupElem != null) attributeGroup = attributeGroupElem.getAttributeValue("ref");
        return new ConceptTypeRestriction(baseAttrib, pattern, attributeGroup);
    }

    private void createConcepts(TS taxonomySchema, List<Element> conceptElementList) throws TaxonomyCreationException {
        for (Element currConceptElement : conceptElementList) {
            String id = currConceptElement.getAttributeValue("id");
            if (id != null) {
                String name = currConceptElement.getAttributeValue("name");
                String type = currConceptElement.getAttributeValue("type");
                String substitutionGroup = currConceptElement.getAttributeValue("substitutionGroup");
                String periodType = currConceptElement.getAttributeValue("periodType", toJDOM(NamespaceConstants.XBRLI_NAMESPACE));
                boolean abstract_ = Boolean.parseBoolean(currConceptElement.getAttributeValue("abstract"));
                boolean nillable = Boolean.parseBoolean(currConceptElement.getAttributeValue("nillable"));
                String typedDomainRef = currConceptElement.getAttributeValue("typedDomainRef", toJDOM(NamespaceConstants.XBRLDT_NAMESPACE));
                if ("xbrli:tuple".equalsIgnoreCase(substitutionGroup)) {
                    Element complexType = currConceptElement.getChild("complexType", toJDOM(NamespaceConstants.XSD_NAMESPACE));
                    if (complexType == null) {
                        throw new TaxonomyCreationException("<complexType> expected under tuple element \"" + id + "\", but missing.");
                    }
                    parseTuple(taxonomySchema, id, name, type, substitutionGroup, periodType, abstract_, nillable, typedDomainRef, complexType);
                } else {
                    ConceptTypeRestriction typeRestriction = parseComplexTypeSimpleContent(currConceptElement);
                    createConcept(taxonomySchema, id, name, type, typeRestriction, substitutionGroup, periodType, abstract_, nillable, typedDomainRef);
                }
            }
        }
    }

    private static final String getElementNameFromTT(TupleType tt) throws TaxonomyCreationException {
        switch(tt) {
            case CHOICE:
            case MULTIPLE_CHOICE:
                return "choice";
            case SEQUENCE:
                return "sequence";
        }
        throw new TaxonomyCreationException("Unsupported tuple type " + tt.name());
    }

    private static TupleType parseTupleType(String id, Element complexType) throws TaxonomyCreationException {
        for (TupleType tt : TupleType.values()) {
            Element child = complexType.getChild(getElementNameFromTT(tt), toJDOM(NamespaceConstants.XSD_NAMESPACE));
            if (child != null) {
                if (tt == TupleType.CHOICE && "unbounded".equals(child.getAttributeValue("maxOccurs"))) return TupleType.MULTIPLE_CHOICE;
                return tt;
            }
        }
        throw new TaxonomyCreationException("Unknown type for tuple element \"" + id + "\".");
    }

    private void parseTuple(TS taxonomySchema, String id, String name, String type, String substitutionGroup, String periodType, boolean abstract_, boolean nillable, String typedDomainRef, Element complexType) throws TaxonomyCreationException {
        TupleType tupleType = parseTupleType(id, complexType);
        ArrayList<String> refs = new ArrayList<String>();
        @SuppressWarnings("unchecked") Element listElem = complexType.getChild(getElementNameFromTT(tupleType), toJDOM(NamespaceConstants.XSD_NAMESPACE));
        List<Element> elementsList = listElem.getChildren("element", toJDOM(NamespaceConstants.XSD_NAMESPACE));
        for (Element element : elementsList) {
            refs.add(element.getAttributeValue("ref"));
        }
        createTuple(taxonomySchema, id, name, type, substitutionGroup, periodType, abstract_, nillable, typedDomainRef, tupleType, refs);
    }

    protected abstract void createConcept(TS taxonomySchema, String id, String name, String type, ConceptTypeRestriction typeRestriction, String substitutionGroup, String periodType, boolean abstract_, boolean nillable, String typedDomainRef) throws TaxonomyCreationException;

    protected abstract void createTuple(TS taxonomySchema, String id, String name, String type, String substitutionGroup, String periodType, boolean abstract_, boolean nillable, String typedDomainRef, TupleType tupleType, List<String> refs) throws TaxonomyCreationException;

    private void getRoleTypes(Document taxonomySource, TS taxonomySchema, Collection<String> roleTypeIDs) throws TaxonomyCreationException {
        Element appInfoElement = getAppInfoElement(taxonomySource);
        if (appInfoElement != null) {
            @SuppressWarnings("unchecked") List<Element> roleTypeElementList = appInfoElement.getChildren("roleType", toJDOM(NamespaceConstants.LINK_NAMESPACE));
            for (Element currRoleTypeElement : roleTypeElementList) {
                String currRoleTypeElementRoleURI = currRoleTypeElement.getAttributeValue("roleURI");
                if ((currRoleTypeElementRoleURI != null) && (currRoleTypeElementRoleURI.length() > 0)) {
                    String id = currRoleTypeElement.getAttributeValue("id");
                    List<String> roleTypeDefinitionList = null;
                    @SuppressWarnings("unchecked") List<Element> roleTypeDefinitionElementList = currRoleTypeElement.getChildren("definition", toJDOM(NamespaceConstants.LINK_NAMESPACE));
                    if (roleTypeDefinitionElementList != null) {
                        roleTypeDefinitionList = new ArrayList<String>();
                        for (Element currRoleTypeDefinitionElement : roleTypeDefinitionElementList) {
                            roleTypeDefinitionList.add(currRoleTypeDefinitionElement.getValue());
                        }
                    }
                    List<String> roleTypeUsedOnList = null;
                    @SuppressWarnings("unchecked") List<Element> roleTypeUsedOnElementList = currRoleTypeElement.getChildren("usedOn", toJDOM(NamespaceConstants.LINK_NAMESPACE));
                    if (roleTypeUsedOnElementList != null) {
                        roleTypeUsedOnList = new ArrayList<String>();
                        for (int iUsedOn = 0; iUsedOn < roleTypeUsedOnElementList.size(); iUsedOn++) {
                            Element currRoleTypeUsedOnElement = roleTypeUsedOnElementList.get(iUsedOn);
                            roleTypeUsedOnList.add(currRoleTypeUsedOnElement.getValue());
                        }
                    }
                    createRoleType(taxonomySchema, id, currRoleTypeElementRoleURI, roleTypeDefinitionList.toArray(new String[roleTypeDefinitionList.size()]), roleTypeUsedOnList.toArray(new String[roleTypeUsedOnList.size()]));
                    String rtid = id;
                    if (rtid.startsWith("role_")) rtid = rtid.substring(5);
                    roleTypeIDs.add(rtid);
                }
            }
        }
    }

    protected abstract void createRoleType(TS taxonomySchema, String id, String roleURI, String[] definitions, String[] usedOns) throws TaxonomyCreationException;

    protected abstract void finishTaxonomySchema(TS taxonomySchema, String taxonomySchemaName) throws TaxonomyCreationException;

    protected static List<Element> getLinkBaseRefElements(Document taxonomySource) {
        Element appInfoElement = getAppInfoElement(taxonomySource);
        if (appInfoElement == null) return null;
        @SuppressWarnings("unchecked") List<Element> linkbaseRefList = appInfoElement.getChildren("linkbaseRef", toJDOM(NamespaceConstants.LINK_NAMESPACE));
        return linkbaseRefList;
    }

    protected static Element getAppInfoElement(Document taxonomySource) {
        if (taxonomySource == null) return null;
        Element rootElement = taxonomySource.getRootElement();
        Element annotationElement = rootElement.getChild("annotation", toJDOM(NamespaceConstants.XSD_NAMESPACE));
        if (annotationElement == null) return null;
        return annotationElement.getChild("appinfo", toJDOM(NamespaceConstants.XSD_NAMESPACE));
    }

    protected static URL getRelativeResource(String baseURI, String resource) throws MalformedURLException {
        int p = baseURI.lastIndexOf('/');
        String uri;
        if (p >= 0) uri = FileLoader.getCanonicalFile(baseURI.substring(0, p + 1), resource); else uri = resource;
        return new URL(uri);
    }

    private boolean buildLinkbase(String name, String role, String extendedLinkRole, String refURLBase, List<String> loadedTaxonomies, int firstTaxoIndex, Collection<String> references) throws IOException, JDOMException, TaxonomyCreationException {
        for (int i = firstTaxoIndex; i < loadedTaxonomies.size(); i++) {
            String currTaxonomySchemaName = loadedTaxonomies.get(i);
            TS taxonomySchema = getTaxonomySchema(currTaxonomySchemaName);
            Document taxonomySource = taxonomyNameToDocument.get(currTaxonomySchemaName);
            if (!taxonomyNameToLinkBaseRefElements.containsKey(currTaxonomySchemaName)) {
                taxonomyNameToLinkBaseRefElements.put(currTaxonomySchemaName, getLinkBaseRefElements(taxonomySource));
            }
            List<Element> linkbaseRefList = taxonomyNameToLinkBaseRefElements.get(currTaxonomySchemaName);
            if (linkbaseRefList != null) {
                for (Element currLinkbaseRefElement : linkbaseRefList) {
                    if (currLinkbaseRefElement.getAttributeValue("role", toJDOM(NamespaceConstants.XLINK_NAMESPACE)).equals(role)) {
                        String linkbaseSource = currLinkbaseRefElement.getAttributeValue("href", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
                        URL url = getRelativeResource(taxonomySource.getBaseURI(), linkbaseSource);
                        buildLinkbase(taxonomySchema, currTaxonomySchemaName, name, extendedLinkRole, url, linkbaseSource, references);
                    }
                }
            }
            Set<String> roleTypes = taxonomyNameToRoleTypes.get(currTaxonomySchemaName);
            if (roleTypes != null) {
                for (String roleType : roleTypes) {
                    try {
                        URL url = getRefURL(refURLBase, name, roleType);
                        if (url != null) {
                            buildLinkbase(taxonomySchema, currTaxonomySchemaName, name, extendedLinkRole, url, getNameFromURL(url), references);
                        }
                    } catch (MalformedURLException e) {
                    }
                }
            }
            try {
                URL url = getRefURL(refURLBase, "dimensions-" + name, null);
                if (url != null) {
                    buildLinkbase(taxonomySchema, currTaxonomySchemaName, name, extendedLinkRole, url, getNameFromURL(url), references);
                }
            } catch (MalformedURLException e) {
            }
        }
        return true;
    }

    private boolean buildLinkbase(TS taxonomySchema, String taxonomySchemaName, String name, String extendedLinkRole, URL url, String linkbaseSource, Collection<String> references) throws IOException, JDOMException, TaxonomyCreationException {
        Set<URL> builtLinkbases_ = builtLinkbases.get(name);
        if (builtLinkbases_ == null) {
            builtLinkbases_ = new HashSet<URL>();
            builtLinkbases_.add(url);
            builtLinkbases.put(name, builtLinkbases_);
        } else if (!builtLinkbases_.contains(url)) {
            builtLinkbases_.add(url);
        } else {
            return true;
        }
        Document linkbaseDocument = instanceNameToDocument.get(url);
        if (linkbaseDocument == null) {
            try {
                URLConnection conn = fileLoader.openConnection(url);
                if (conn.getContentType() == null || (!conn.getContentType().contains("application/xml") && !conn.getContentType().contains("text/xml"))) return false;
                InputStream is = conn.getInputStream();
                if (is == null) return false;
                if (references == null) log.log(LogLevel.INFO, LOG_CHANNEL, "Building linkbase document " + linkbaseSource + "...");
                linkbaseDocument = saxBuilder.build(is);
                instanceNameToDocument.put(url, linkbaseDocument);
            } catch (FileNotFoundException e) {
                return false;
            } catch (ConnectException e) {
                return false;
            }
        } else {
            if (references == null) log.log(LogLevel.INFO, LOG_CHANNEL, "Building linkbase document " + linkbaseSource + "...");
        }
        @SuppressWarnings("unchecked") List<Element> extendedLinkRolesList = linkbaseDocument.getRootElement().getChildren(extendedLinkRole, toJDOM(NamespaceConstants.LINK_NAMESPACE));
        for (Element newExtendedLinkRoleElement : extendedLinkRolesList) {
            String currExtendedLinkRole = newExtendedLinkRoleElement.getAttributeValue("role", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
            if (references == null) addExtendedLinkRole(taxonomySchema, taxonomySchemaName, name, currExtendedLinkRole);
            @SuppressWarnings("unchecked") List<Element> linkbaseElements = newExtendedLinkRoleElement.getChildren();
            for (Element currLinkbaseElement : linkbaseElements) {
                String typeAttrValue = currLinkbaseElement.getAttributeValue("type", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
                if (typeAttrValue != null && (typeAttrValue.equals("locator") || typeAttrValue.equals("resource"))) {
                    String label = currLinkbaseElement.getAttributeValue("label", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
                    if (label == null || label.length() == 0) {
                        log.log(LogLevel.ERROR, LOG_CHANNEL, "Could not find label for extended link element");
                    }
                    if (typeAttrValue.equals("locator")) {
                        String conceptName = currLinkbaseElement.getAttributeValue("href", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
                        if (conceptName == null) {
                            log.log(LogLevel.ERROR, LOG_CHANNEL, "Could not find concept the label refers to");
                        } else {
                            try {
                                conceptName = java.net.URLDecoder.decode(conceptName, "UTF-8");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (references != null) {
                            if (conceptName != null) {
                                String reference = conceptName.substring(0, conceptName.indexOf("#"));
                                references.add(reference);
                            }
                        } else {
                            String elementId = null;
                            if (conceptName != null) {
                                elementId = conceptName.substring(conceptName.indexOf("#") + 1);
                            }
                            String id = currLinkbaseElement.getAttributeValue("id");
                            String role = currLinkbaseElement.getAttributeValue("role", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
                            String title = currLinkbaseElement.getAttributeValue("title", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
                            createLocator(taxonomySchema, taxonomySchemaName, name, linkbaseSource, label, currExtendedLinkRole, id, role, title, conceptName, elementId);
                        }
                    } else if (references == null) {
                        String role = currLinkbaseElement.getAttributeValue("role", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
                        String title = currLinkbaseElement.getAttributeValue("title", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
                        String id = currLinkbaseElement.getAttributeValue("id");
                        String lang = currLinkbaseElement.getAttributeValue("lang", toJDOM(NamespaceConstants.XML_NAMESPACE));
                        String value = currLinkbaseElement.getValue();
                        ArrayList<String[]> hgbRefs = null;
                        ArrayList<String[]> refs = null;
                        {
                            hgbRefs = new ArrayList<String[]>();
                            for (Object o : currLinkbaseElement.getChildren()) {
                                if ((o instanceof Element) && (((Element) o).getNamespace().getPrefix().equals(NamespaceConstants.HGBREF_NAMESPACE.getPrefix()))) {
                                    Element elem = (Element) o;
                                    hgbRefs.add(new String[] { elem.getName(), elem.getValue() });
                                }
                            }
                            refs = new ArrayList<String[]>();
                            for (Object o : currLinkbaseElement.getChildren()) {
                                if ((o instanceof Element) && (((Element) o).getNamespace().getPrefix().equals(NamespaceConstants.REF_NAMESPACE.getPrefix()))) {
                                    Element elem = (Element) o;
                                    refs.add(new String[] { elem.getName(), elem.getValue() });
                                }
                            }
                        }
                        createResource(taxonomySchema, taxonomySchemaName, name, linkbaseSource, label, currExtendedLinkRole, role, title, id, lang, value, hgbRefs, refs);
                    }
                }
            }
        }
        return true;
    }

    protected abstract void addExtendedLinkRole(TS taxonomySchema, String taxonomySchemaName, String linkbaseName, String extendedLinkRole) throws TaxonomyCreationException;

    protected abstract void createLocator(TS taxonomySchema, String taxonomySchemaName, String linkbaseName, String linkbaseSource, String label, String extLinkRole, String id, String role, String title, String conceptName, String elementId) throws TaxonomyCreationException;

    protected abstract void createResource(TS taxonomySchema, String taxonomySchemaName, String linkbaseName, String linkbaseSource, String label, String extLinkRole, String role, String title, String id, String lang, String value, List<String[]> hgbRefs, List<String[]> refs) throws TaxonomyCreationException;

    private void buildArcs(String linkbaseName, String role, String xbrlExtendedLinkRole, String arcName) throws IOException, JDOMException, TaxonomyCreationException {
        log.log(LogLevel.INFO, LOG_CHANNEL, "Building Arcs for " + linkbaseName + " link base...");
        for (String currTaxonomySchemaName : loadedTaxonomies) {
            TS taxonomySchema = getTaxonomySchema(currTaxonomySchemaName);
            Document taxonomySource = taxonomyNameToDocument.get(currTaxonomySchemaName);
            if (!taxonomyNameToLinkBaseRefElements.containsKey(currTaxonomySchemaName)) {
                taxonomyNameToLinkBaseRefElements.put(currTaxonomySchemaName, getLinkBaseRefElements(taxonomySource));
            }
            List<Element> linkbaseRefList = taxonomyNameToLinkBaseRefElements.get(currTaxonomySchemaName);
            if (linkbaseRefList == null) continue;
            for (Element currLinkbaseRefElement : linkbaseRefList) {
                if (currLinkbaseRefElement.getAttributeValue("role", toJDOM(NamespaceConstants.XLINK_NAMESPACE)).equals(role)) {
                    String linkbaseSource = currLinkbaseRefElement.getAttributeValue("href", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
                    URL url = getRelativeResource(taxonomySource.getBaseURI(), linkbaseSource);
                    buildArcs(taxonomySchema, currTaxonomySchemaName, linkbaseName, url, linkbaseSource, xbrlExtendedLinkRole, arcName);
                }
            }
            Set<String> roleTypes = taxonomyNameToRoleTypes.get(currTaxonomySchemaName);
            String refURLBase = taxonomyNameToRefBase.get(currTaxonomySchemaName);
            if (roleTypes != null) {
                for (String roleType : roleTypes) {
                    try {
                        URL url = getRefURL(refURLBase, linkbaseName, roleType);
                        if (url != null) {
                            buildArcs(taxonomySchema, currTaxonomySchemaName, linkbaseName, url, getNameFromURL(url), xbrlExtendedLinkRole, arcName);
                        }
                    } catch (MalformedURLException e) {
                    }
                }
            }
            try {
                URL url = getRefURL(refURLBase, "dimensions-" + linkbaseName, null);
                if (url != null) {
                    buildArcs(taxonomySchema, currTaxonomySchemaName, linkbaseName, url, getNameFromURL(url), xbrlExtendedLinkRole, arcName);
                }
            } catch (MalformedURLException e) {
            }
        }
    }

    private void buildArcs(TS taxonomySchema, String taxonomySchemaName, String linkbaseName, URL url, String linkbaseSource, String xbrlExtendedLinkRole, String arcName) throws IOException, JDOMException, TaxonomyCreationException {
        Set<URL> builtArcs_ = builtArcs.get(linkbaseName);
        if (builtArcs_ == null) {
            builtArcs_ = new HashSet<URL>();
            builtArcs_.add(url);
            builtArcs.put(linkbaseName, builtArcs_);
        } else if (!builtArcs_.contains(url)) {
            builtArcs_.add(url);
        } else {
            return;
        }
        Document linkbaseDocument = instanceNameToDocument.get(url);
        if (linkbaseDocument == null) {
            try {
                InputStream is = fileLoader.openConnection(url).getInputStream();
                if (is == null) return;
                linkbaseDocument = saxBuilder.build(is);
                instanceNameToDocument.put(url, linkbaseDocument);
            } catch (FileNotFoundException e) {
                return;
            } catch (ConnectException e) {
                return;
            }
        } else {
        }
        @SuppressWarnings("unchecked") List<Element> extendedLinkRolesList = linkbaseDocument.getRootElement().getChildren(xbrlExtendedLinkRole, toJDOM(NamespaceConstants.LINK_NAMESPACE));
        for (Element newExtendedLinkRoleElement : extendedLinkRolesList) {
            String currExtendedLinkRole = newExtendedLinkRoleElement.getAttributeValue("role", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
            @SuppressWarnings("unchecked") List<Element> arcElementsList = newExtendedLinkRoleElement.getChildren(arcName, toJDOM(NamespaceConstants.LINK_NAMESPACE));
            for (Element currArcElement : arcElementsList) {
                String fromAttribute = currArcElement.getAttributeValue("from", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
                String toAttribute = currArcElement.getAttributeValue("to", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
                String arcRole = currArcElement.getAttributeValue("arcrole", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
                String targetRole = currArcElement.getAttributeValue("targetRole", toJDOM(NamespaceConstants.XBRLDT_NAMESPACE));
                String title = currArcElement.getAttributeValue("title", toJDOM(NamespaceConstants.XLINK_NAMESPACE));
                String contextElementName = currArcElement.getAttributeValue("contextElement", toJDOM(NamespaceConstants.XBRLDT_NAMESPACE));
                String order_ = currArcElement.getAttributeValue("order");
                Float order = (order_ != null) ? Float.valueOf(order_) : null;
                String use = currArcElement.getAttributeValue("use");
                String priority_ = currArcElement.getAttributeValue("priority");
                Integer priority = (priority_ != null) ? Integer.valueOf(priority_) : null;
                boolean usable = Boolean.valueOf(currArcElement.getAttributeValue("usable", toJDOM(NamespaceConstants.XBRLDT_NAMESPACE)));
                String weight_ = currArcElement.getAttributeValue("weight");
                Float weight = (weight_ != null) ? Float.valueOf(weight_) : null;
                createArcs(taxonomySchema, taxonomySchemaName, linkbaseName, linkbaseSource, fromAttribute, toAttribute, currExtendedLinkRole, arcRole, targetRole, title, contextElementName, order, use, priority, usable, weight);
            }
        }
    }

    protected abstract void createArcs(TS taxonomySchema, String taxonomySchemaName, String linkbaseName, String linkbaseSource, String from, String to, String extLinkRole, String arcRole, String targetRole, String title, String contextElementName, Float order, String use, Integer priority, boolean usable, Float weight) throws TaxonomyCreationException;

    protected abstract ResultType finish(TS taxonomySchema, String taxonomyFileName) throws IOException, XBRLException, TaxonomyCreationException;

    protected void cleanup() {
        fileLoader = null;
        taxPath = null;
        loadedTaxonomies = null;
        builtTaxonomies = null;
        includedTaxonomies = null;
        saxBuilder = null;
        taxonomyNameToDocument = null;
        builtArcs = null;
        builtLinkbases = null;
        taxonomyNameToSchema = null;
        taxonomyNameToRoleTypes = null;
        taxonomyNameToRefBase = null;
        instanceNameToDocument = null;
        taxonomyNameToLinkBaseRefElements = null;
    }

    protected TS getTaxonomySchema(String taxonomySchemaName) {
        return taxonomyNameToSchema.get(taxonomySchemaName);
    }

    protected void getIncludedTaxonomyFileNames(String taxonomyName, Collection<String> result) {
        List<String> imports = includedTaxonomies.get(taxonomyName);
        if (imports != null) result.addAll(imports);
    }

    private boolean collectIncludedTaxonomies(URL taxonomyFile, String refBaseURL, boolean isExplicitTry, Map<String, List<String>> includedTaxonomies, Set<URL> importedTaxonomies, List<String> loadedTaxonomies) throws IOException, JDOMException {
        String name = getNameFromURL(taxonomyFile);
        if (taxonomyNameToDocument.containsKey(name)) return true;
        Document tmpTaxDocument = null;
        try {
            tmpTaxDocument = saxBuilder.build(fileLoader.openConnection(taxonomyFile).getInputStream());
            tmpTaxDocument.setBaseURI(taxonomyFile.toURI().toString());
            dumpFileInfo(taxonomyFile, tmpTaxDocument);
            taxonomyNameToDocument.put(name, tmpTaxDocument);
        } catch (IOException e) {
            if (!isExplicitTry) {
                log.log(LogLevel.WARNING, LOG_CHANNEL, "Referenced file not found: " + taxonomyFile);
            }
            taxonomyNameToDocument.put(name, tmpTaxDocument);
            return false;
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage());
        }
        List<URL> tmpIncludedTaxonomyNames = new ArrayList<URL>();
        getImportedTaxonomyFileNames(tmpTaxDocument, tmpIncludedTaxonomyNames, importedTaxonomies);
        if (tmpIncludedTaxonomyNames.size() > 0) {
            List<String> includedTaxonomies_ = includedTaxonomies.get(name);
            if (includedTaxonomies_ == null) {
                includedTaxonomies_ = new ArrayList<String>();
                includedTaxonomies.put(name, includedTaxonomies_);
            }
            for (URL url : tmpIncludedTaxonomyNames) {
                String name2 = getNameFromURL(url);
                if (!loadedTaxonomies.contains(name2)) {
                    loadedTaxonomies.add(0, name2);
                    taxonomyNameToRefBase.put(name2, refBaseURL);
                }
                if (!includedTaxonomies_.contains(name2)) {
                    includedTaxonomies_.add(name2);
                }
            }
            for (URL url : tmpIncludedTaxonomyNames) {
                {
                    if (!collectIncludedTaxonomies(url, refBaseURL, false, includedTaxonomies, importedTaxonomies, loadedTaxonomies)) ;
                }
            }
        }
        return true;
    }

    /**
     * 
     * @param taxonomySource
     * @param includedTaxonomyNames
     * @param importedTaxonomyNames
     * @throws IOException
     */
    private void getImportedTaxonomyFileNames(Document taxonomySource, Collection<URL> includedTaxonomyNames, Collection<URL> importedTaxonomyNames) throws IOException {
        Element rootElement = taxonomySource.getRootElement();
        @SuppressWarnings("unchecked") List<Element> children = rootElement.getChildren("include", toJDOM(NamespaceConstants.XSD_NAMESPACE));
        for (Element currElement : children) {
            String schemaLocation = currElement.getAttributeValue("schemaLocation");
            URL file = fileLoader.getFile(taxPath, null, schemaLocation);
            if (file != null) includedTaxonomyNames.add(file);
        }
        children = rootElement.getChildren("import", toJDOM(NamespaceConstants.XSD_NAMESPACE));
        for (Element currElement : children) {
            String namespace = currElement.getAttributeValue("namespace");
            String schemaLocation = currElement.getAttributeValue("schemaLocation");
            URL file = fileLoader.getFile(taxPath, namespace, schemaLocation);
            if (file != null) {
                if (!schemaLocation.startsWith("http://") && !schemaLocation.startsWith("https://")) importedTaxonomyNames.add(file); else includedTaxonomyNames.add(file);
            }
        }
    }

    protected static File getFileFromURL(URL url) throws MalformedURLException {
        if (!url.getProtocol().equals("file")) throw new IllegalArgumentException("The passed URL does not point to a file.");
        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new MalformedURLException(e.getMessage());
        }
    }

    protected static String getNameFromURL(URL url) {
        String s = url.toString();
        if (s.length() <= 1) return s;
        int p = s.lastIndexOf('/');
        if (p == s.length() - 1) {
            int p2 = s.lastIndexOf('/', p - 1);
            if (p2 >= 0) {
                if (p2 == p - 1) return "";
                return s.substring(p + 1, p);
            }
            return s.substring(0, p);
        }
        if (p >= 0) return s.substring(p + 1);
        return s;
    }

    protected String getRefURLBase(URL url) {
        String s = url.toString();
        int p = s.lastIndexOf('.');
        if (p >= 0) s = s.substring(0, p);
        return s;
    }

    protected URL getRefURL(String base, String linkbaseName, String roleType) {
        try {
            return new URL(base + "-" + linkbaseName + (roleType == null ? "" : "-" + roleType) + ".xml");
        } catch (MalformedURLException e) {
            return null;
        }
    }

    protected URL getRefURLTaxo(String base, String postfix) {
        try {
            return new URL(base + "-" + postfix + ".xsd");
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private void dumpFileInfo(URL file, Document document) {
        try {
            log.log(LogLevel.DEBUG, LOG_CHANNEL, file);
            log.log(LogLevel.DEBUG, LOG_CHANNEL, "  file size: " + getFileSize(file) + ", element count: " + countElements(document.getRootElement()));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private long getFileSize(URL file) throws IOException, URISyntaxException {
        if (file.getProtocol().equals("file")) return new File(file.toURI()).length();
        long size = 0;
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(fileLoader.openConnection(file).getInputStream());
            byte[] buffer = new byte[1024];
            int n = 0;
            while ((n = in.read(buffer, 0, Math.min(in.available() + 1, buffer.length))) >= 0) {
                size += n;
            }
        } finally {
            if (in != null) in.close();
        }
        return size;
    }

    private int countElements(Element parent) {
        int n = 1;
        for (Object o : parent.getChildren()) {
            n += countElements((Element) o);
        }
        return n;
    }
}
