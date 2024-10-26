package net.sourceforge.ondex.webservice;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import javax.jws.WebService;
import javax.xml.stream.XMLStreamException;
import net.sourceforge.ondex.config.Config;
import net.sourceforge.ondex.config.ONDEXGraphRegistry;
import net.sourceforge.ondex.core.AttributeName;
import net.sourceforge.ondex.core.CV;
import net.sourceforge.ondex.core.ConceptAccession;
import net.sourceforge.ondex.core.ConceptClass;
import net.sourceforge.ondex.core.ConceptName;
import net.sourceforge.ondex.core.EvidenceType;
import net.sourceforge.ondex.core.GDS;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.ONDEXGraphMetaData;
import net.sourceforge.ondex.core.ONDEXIterator;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.core.ONDEXView;
import net.sourceforge.ondex.core.RelationType;
import net.sourceforge.ondex.core.Unit;
import net.sourceforge.ondex.core.persistent.BerkeleyEnv;
import net.sourceforge.ondex.core.security.Session;
import net.sourceforge.ondex.core.security.perm.GlobalPermissions;
import net.sourceforge.ondex.export.oxl.Export;
import net.sourceforge.ondex.logging.ONDEXCoreLogger;
import net.sourceforge.ondex.parser.oxl.ConceptParser;
import net.sourceforge.ondex.parser.oxl.RelationParser;
import net.sourceforge.ondex.parser.oxl.XmlParser;
import net.sourceforge.ondex.tools.DirUtils;
import net.sourceforge.ondex.webservice.context.CleanupContextListener;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.io.CharsetNames;
import com.ctc.wstx.stax.WstxOutputFactory;

/**
 * Webservice implementation of OndexGraph.
 * 
 * @author David Withers
 */
@WebService(endpointInterface = "net.sourceforge.ondex.webservice.OndexGraph")
public class OndexGraphImpl implements OndexGraph {

    private static final Logger logger = Logger.getLogger(OndexGraphImpl.class);

    private static final String ROOT_GRAPH_ID = "root";

    private static final String DB_DIR = "ondex-db";

    private static final String DEFAULT_USER = "none";

    private static final String DEFAULT_PASSWORD = "8fe956;lt5";

    private File dbPath;

    private Map<Long, BerkeleyEnv> databases = new HashMap<Long, BerkeleyEnv>();

    private ONDEXGraph rootGraph;

    public OndexGraphImpl() {
    }

    @Override
    public Boolean checkAttributeName(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        return metaData.checkAttributeName(id);
    }

    @Override
    public Boolean checkConceptClass(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        return metaData.checkConceptClass(id);
    }

    @Override
    public Boolean checkCV(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        return metaData.checkCV(id);
    }

    @Override
    public Boolean checkEvidenceType(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        return metaData.checkRelationType(id);
    }

    @Override
    public Boolean checkRelationType(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        return metaData.checkRelationType(id);
    }

    @Override
    public Boolean checkUnit(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        return metaData.checkUnit(id);
    }

    @Override
    public String createAttributeName(Long graphId, String id, String fullname, String description, String unitId, String datatype, String specialisationOfAttributeNameId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        Unit unit = metaData.getUnit(unitId);
        AttributeName specialisationOf = metaData.getAttributeName(specialisationOfAttributeNameId);
        AttributeName attributeName;
        try {
            attributeName = metaData.createAttributeName(id, fullname == null ? "" : fullname, description == null ? "" : description, unit, Class.forName(datatype), specialisationOf);
            databases.get(graph.getSID()).commit();
            return new WSAttributeName(attributeName).getId();
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public Integer createConcept(Long graphId, String annotation, String description, String elementOfCVId, String ofTypeConceptClassId, List<String> evidenceTypeIdList) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        CV cv = metaData.getCV(elementOfCVId);
        ConceptClass conceptClass = metaData.getConceptClass(ofTypeConceptClassId);
        List<EvidenceType> evidence = new ArrayList<EvidenceType>();
        for (String evidenceTypeId : evidenceTypeIdList) {
            evidence.add(metaData.getEvidenceType(evidenceTypeId));
        }
        ONDEXConcept concept = graph.createConcept("", annotation == null ? "" : annotation, description == null ? "" : description, cv, conceptClass, evidence);
        databases.get(graph.getSID()).commit();
        return new WSConcept(concept).getId();
    }

    @Override
    public String createConceptClass(Long graphId, String id, String fullname, String description, String specialisationOfConceptClassId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        ConceptClass specialisationOf = null;
        if (specialisationOfConceptClassId != null) {
            specialisationOf = metaData.getConceptClass(specialisationOfConceptClassId);
        }
        ConceptClass conceptClass = metaData.createConceptClass(id, fullname == null ? "" : fullname, description == null ? "" : description, specialisationOf);
        databases.get(graph.getSID()).commit();
        return new WSConceptClass(conceptClass).getId();
    }

    @Override
    public String createConceptGDS(Long graphId, Integer conceptId, String attributeNameId, String value) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        ONDEXConcept concept = graph.getConcept(conceptId);
        AttributeName attributeName = metaData.getAttributeName(attributeNameId);
        concept.createConceptGDS(attributeName, value, false);
        databases.get(graph.getSID()).commit();
        return attributeNameId;
    }

    @Override
    public String createConceptName(Long graphId, Integer conceptId, String name, Boolean isPreferred) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXConcept concept = graph.getConcept(conceptId);
        concept.createConceptName(name, isPreferred);
        databases.get(graph.getSID()).commit();
        return name;
    }

    @Override
    public String createConceptAccession(Long graphId, Integer conceptId, String accession, String elementOfCVId, Boolean isAmbiguous) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        CV cv = metaData.getCV(elementOfCVId);
        ONDEXConcept concept = graph.getConcept(conceptId);
        concept.createConceptAccession(accession, cv, isAmbiguous == null ? true : isAmbiguous);
        databases.get(graph.getSID()).commit();
        return accession;
    }

    @Override
    public String createCV(Long graphId, String id, String fullname, String description) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        CV cv = metaData.createCV(id, fullname == null ? "" : fullname, description == null ? "" : description);
        databases.get(graph.getSID()).commit();
        return new WSCV(cv).getId();
    }

    @Override
    public String createEvidenceType(Long graphId, String id, String fullname, String description) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        EvidenceType evidenceType = metaData.createEvidenceType(id, fullname == null ? "" : fullname, description == null ? "" : description);
        databases.get(graph.getSID()).commit();
        return new WSEvidenceType(evidenceType).getId();
    }

    @Override
    public Long createGraph(String name) {
        ONDEXGraph graph = null;
        try {
            graph = makeGraph(name);
            logger.info("Created graph " + graph.getSID());
            databases.get(graph.getSID()).commit();
        } catch (Exception e) {
            logger.error("Error creating graph: " + e);
        }
        return new WSGraph(graph).getId();
    }

    @Override
    public Integer createRelation(Long graphId, Integer fromConceptId, Integer toConceptId, Integer qualifierConceptId, String relationTypeId, List<String> evidenceTypeIdList) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        ONDEXConcept fromConcept = graph.getConcept(fromConceptId);
        ONDEXConcept toConcept = graph.getConcept(toConceptId);
        ONDEXConcept qualifierConcept = graph.getConcept(qualifierConceptId);
        RelationType relationType = metaData.getRelationType(relationTypeId);
        List<EvidenceType> evidence = new ArrayList<EvidenceType>();
        for (String evidenceTypeId : evidenceTypeIdList) {
            evidence.add(metaData.getEvidenceType(evidenceTypeId));
        }
        ONDEXRelation relation = graph.createRelation(fromConcept, toConcept, qualifierConcept, relationType, evidence);
        databases.get(graph.getSID()).commit();
        return new WSRelation(relation).getId();
    }

    @Override
    public String createRelationGDS(Long graphId, Integer relationId, String attributeNameId, String value) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        ONDEXRelation relation = graph.getRelation(relationId);
        AttributeName attributeName = metaData.getAttributeName(attributeNameId);
        relation.createRelationGDS(attributeName, value, false);
        databases.get(graph.getSID()).commit();
        return attributeNameId;
    }

    @Override
    public String createRelationType(Long graphId, String id, String fullname, String description, String inverseName, Boolean isAntisymmetric, Boolean isReflexive, Boolean isSymmetric, Boolean isTransitiv, String specialisationOfRelationTypeId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        RelationType specialisationOf = metaData.getRelationType(specialisationOfRelationTypeId);
        RelationType relationType = metaData.createRelationType(id, fullname == null ? "" : fullname, description == null ? "" : description, inverseName == null ? "" : inverseName, isAntisymmetric == null ? false : isAntisymmetric, isReflexive == null ? false : isReflexive, isSymmetric == null ? false : isSymmetric, isTransitiv == null ? false : isTransitiv, specialisationOf);
        databases.get(graph.getSID()).commit();
        return new WSRelationType(relationType).getId();
    }

    private void createRootGraph(File dbDir) {
        BerkeleyEnv berkeleyEnv = new BerkeleyEnv(dbDir.getPath() + File.separator + ROOT_GRAPH_ID, ROOT_GRAPH_ID, new ONDEXCoreLogger());
        berkeleyEnv.commit();
        rootGraph = berkeleyEnv.getAbstractONDEXGraph();
        databases.put(rootGraph.getSID(), berkeleyEnv);
    }

    @Override
    public String createUnit(Long graphId, String id, String fullname, String description) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        Unit unit = metaData.createUnit(id, fullname == null ? "" : fullname, description == null ? "" : description);
        databases.get(graph.getSID()).commit();
        return new WSUnit(unit).getId();
    }

    @Override
    public void deleteAttributeName(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        metaData.deleteAttributeName(id);
        databases.get(graph.getSID()).commit();
    }

    @Override
    public void deleteConcept(Long graphId, Integer id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        graph.deleteConcept(id);
        databases.get(graph.getSID()).commit();
    }

    @Override
    public void deleteConceptClass(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        metaData.deleteConceptClass(id);
        databases.get(graph.getSID()).commit();
    }

    @Override
    public void deleteConceptGDS(Long graphId, Integer conceptId, String attributeNameId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        ONDEXConcept concept = graph.getConcept(conceptId);
        AttributeName attributeName = metaData.getAttributeName(attributeNameId);
        concept.deleteConceptGDS(attributeName);
        databases.get(graph.getSID()).commit();
    }

    @Override
    public void deleteConceptName(Long graphId, Integer conceptId, String name) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXConcept concept = graph.getConcept(conceptId);
        concept.deleteConceptName(name);
        databases.get(graph.getSID()).commit();
    }

    @Override
    public void deleteConceptAccession(Long graphId, Integer conceptId, String accession, String elementOfCVId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        CV cv = metaData.getCV(elementOfCVId);
        ONDEXConcept concept = graph.getConcept(conceptId);
        concept.deleteConceptAccession(accession, cv);
        databases.get(graph.getSID()).commit();
    }

    @Override
    public void deleteCV(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        metaData.deleteCV(id);
        databases.get(graph.getSID()).commit();
    }

    @Override
    public void deleteEvidenceType(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        metaData.deleteEvidenceType(id);
        databases.get(graph.getSID()).commit();
    }

    @Override
    public void deleteGraph(Long graphId) {
        try {
            ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
            ONDEXGraphRegistry.graphs.remove(graphId);
            removeGraph(graph.getSID());
        } catch (Exception e) {
            logger.error("Problem deleting graph " + e);
        }
    }

    @Override
    public void deleteRelation(Long graphId, Integer id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        graph.deleteRelation(id);
        databases.get(graph.getSID()).commit();
    }

    @Override
    public void deleteRelationGDS(Long graphId, Integer relationId, String attributeNameId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        ONDEXRelation relation = graph.getRelation(relationId);
        AttributeName attributeName = metaData.getAttributeName(attributeNameId);
        relation.deleteRelationGDS(attributeName);
        databases.get(graph.getSID()).commit();
    }

    @Override
    public void deleteRelationOfTypeAndQualifier(Long graphId, Integer fromConceptId, Integer toConceptId, Integer qualifierConceptId, String relationTypeId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        ONDEXConcept from = graph.getConcept(fromConceptId);
        ONDEXConcept to = graph.getConcept(toConceptId);
        ONDEXConcept qualifier = graph.getConcept(qualifierConceptId);
        RelationType relationType = metaData.getRelationType(relationTypeId);
        graph.deleteRelation(from, to, qualifier, relationType);
        databases.get(graph.getSID()).commit();
    }

    @Override
    public void deleteRelationType(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        metaData.deleteRelationType(id);
        databases.get(graph.getSID()).commit();
    }

    @Override
    public void deleteUnit(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        metaData.deleteUnit(id);
        databases.get(graph.getSID()).commit();
    }

    @Override
    public String exportGraph(Long graphId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        StringWriter writer = new StringWriter();
        try {
            write(graph, writer);
        } catch (XMLStreamException e) {
            logger.error("Error exporting graph '" + graph.getName() + "'", e);
        }
        return writer.toString();
    }

    @Override
    public WSAttributeName getAttributeName(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        return new WSAttributeName(metaData.getAttributeName(id));
    }

    @Override
    public List<WSAttributeName> getAttributeNames(Long graphId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSAttributeName> list = new ArrayList<WSAttributeName>();
        ONDEXIterator<AttributeName> attributeNames = metaData.getAttributeNames();
        while (attributeNames.hasNext()) {
            list.add(new WSAttributeName(attributeNames.next()));
        }
        return list;
    }

    @Override
    public WSConcept getConcept(Long graphId, Integer id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        return new WSConcept(graph.getConcept(id));
    }

    @Override
    public WSConceptClass getConceptClass(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        return new WSConceptClass(metaData.getConceptClass(id));
    }

    @Override
    public List<WSConceptClass> getConceptClasses(Long graphId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSConceptClass> list = new ArrayList<WSConceptClass>();
        ONDEXIterator<ConceptClass> conceptClasses = metaData.getConceptClasses();
        while (conceptClasses.hasNext()) {
            list.add(new WSConceptClass(conceptClasses.next()));
        }
        return list;
    }

    @Override
    public WSConceptGDS getConceptGDS(Long graphId, Integer conceptId, String attributeNameId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        ONDEXConcept concept = graph.getConcept(conceptId);
        AttributeName attributeName = metaData.getAttributeName(attributeNameId);
        return new WSConceptGDS(concept.getConceptGDS(attributeName));
    }

    @Override
    public List<WSConceptGDS> getConceptGDSs(Long graphId, Integer conceptId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXConcept concept = graph.getConcept(conceptId);
        List<WSConceptGDS> list = new ArrayList<WSConceptGDS>();
        ONDEXIterator<GDS<ONDEXConcept>> conceptGDSs = concept.getConceptGDSs();
        while (conceptGDSs.hasNext()) {
            list.add(new WSConceptGDS(conceptGDSs.next()));
        }
        return list;
    }

    @Override
    public WSConceptName getConceptName(Long graphId, Integer conceptId, String name) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXConcept concept = graph.getConcept(conceptId);
        return new WSConceptName(concept.getConceptName(name));
    }

    @Override
    public List<WSConceptName> getConceptNames(Long graphId, Integer conceptId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXConcept concept = graph.getConcept(conceptId);
        List<WSConceptName> list = new ArrayList<WSConceptName>();
        ONDEXIterator<ConceptName> conceptNames = concept.getConceptNames();
        while (conceptNames.hasNext()) {
            list.add(new WSConceptName(conceptNames.next()));
        }
        return list;
    }

    @Override
    public WSConceptAccession getConceptAccession(Long graphId, Integer conceptId, String accession, String elementOfCVId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        CV cv = metaData.getCV(elementOfCVId);
        ONDEXConcept concept = graph.getConcept(conceptId);
        return new WSConceptAccession(concept.getConceptAccession(accession, cv));
    }

    @Override
    public List<WSConceptAccession> getConceptAccessions(Long graphId, Integer conceptId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXConcept concept = graph.getConcept(conceptId);
        List<WSConceptAccession> list = new ArrayList<WSConceptAccession>();
        ONDEXIterator<ConceptAccession> conceptAccessions = concept.getConceptAccessions();
        while (conceptAccessions.hasNext()) {
            list.add(new WSConceptAccession(conceptAccessions.next()));
        }
        return list;
    }

    @Override
    public List<WSConcept> getConcepts(Long graphId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        logger.info("Retrieved graph " + graph.getSID());
        List<WSConcept> list = new ArrayList<WSConcept>();
        ONDEXView<ONDEXConcept> concepts = null;
        try {
            concepts = graph.getConcepts();
        } catch (Exception e) {
            logger.error("Error getting concepts " + e);
        }
        while (concepts.hasNext()) {
            list.add(new WSConcept(concepts.next()));
        }
        return list;
    }

    @Override
    public List<WSConcept> getConceptsOfAttributeName(Long graphId, String attributeNameId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSConcept> list = new ArrayList<WSConcept>();
        AttributeName attributeName = metaData.getAttributeName(attributeNameId);
        ONDEXView<ONDEXConcept> concepts = graph.getConceptsOfAttributeName(attributeName);
        while (concepts.hasNext()) {
            list.add(new WSConcept(concepts.next()));
        }
        return list;
    }

    @Override
    public List<WSConcept> getConceptsOfConceptClass(Long graphId, String conceptClassId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSConcept> list = new ArrayList<WSConcept>();
        ConceptClass conceptClass = metaData.getConceptClass(conceptClassId);
        ONDEXView<ONDEXConcept> concepts = graph.getConceptsOfConceptClass(conceptClass);
        while (concepts.hasNext()) {
            list.add(new WSConcept(concepts.next()));
        }
        return list;
    }

    @Override
    public List<WSConcept> getConceptsOfContext(Long graphId, Integer contextConceptId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        List<WSConcept> list = new ArrayList<WSConcept>();
        ONDEXConcept concept = graph.getConcept(contextConceptId);
        ONDEXView<ONDEXConcept> concepts = graph.getConceptsOfContext(concept);
        while (concepts.hasNext()) {
            list.add(new WSConcept(concepts.next()));
        }
        return list;
    }

    @Override
    public List<WSConcept> getConceptsOfCV(Long graphId, String cvId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSConcept> list = new ArrayList<WSConcept>();
        CV cv = metaData.getCV(cvId);
        ONDEXView<ONDEXConcept> concepts = graph.getConceptsOfCV(cv);
        while (concepts.hasNext()) {
            list.add(new WSConcept(concepts.next()));
        }
        return list;
    }

    @Override
    public List<WSConcept> getConceptsOfEvidenceType(Long graphId, String evidenceTypeId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSConcept> list = new ArrayList<WSConcept>();
        EvidenceType evidenceType = metaData.getEvidenceType(evidenceTypeId);
        ONDEXView<ONDEXConcept> concepts = graph.getConceptsOfEvidenceType(evidenceType);
        while (concepts.hasNext()) {
            list.add(new WSConcept(concepts.next()));
        }
        return list;
    }

    @Override
    public WSCV getCV(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        return new WSCV(metaData.getCV(id));
    }

    @Override
    public List<WSCV> getCVs(Long graphId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        logger.info("Got graph " + graph.getSID());
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSCV> list = new ArrayList<WSCV>();
        ONDEXIterator<CV> CVs = metaData.getCVs();
        logger.info("Got list of CVs for graph " + graph.getSID());
        while (CVs.hasNext()) {
            list.add(new WSCV(CVs.next()));
        }
        return list;
    }

    @Override
    public WSEvidenceType getEvidenceType(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        return new WSEvidenceType(metaData.getEvidenceType(id));
    }

    @Override
    public List<WSEvidenceType> getEvidenceTypes(Long graphId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSEvidenceType> list = new ArrayList<WSEvidenceType>();
        ONDEXIterator<EvidenceType> evidenceTypes = metaData.getEvidenceTypes();
        while (evidenceTypes.hasNext()) {
            list.add(new WSEvidenceType(evidenceTypes.next()));
        }
        return list;
    }

    @Override
    public WSGraph getGraph(Long graphId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        return new WSGraph(graph);
    }

    @Override
    public List<WSGraph> getGraphsOfName(String name) {
        List<WSGraph> graphs = new ArrayList<WSGraph>();
        for (ONDEXGraph ondexGraph : ONDEXGraphRegistry.graphs.values()) {
            if (name.equals(ondexGraph.getName())) {
                graphs.add(new WSGraph(ondexGraph));
            }
        }
        return graphs;
    }

    @Override
    public List<WSGraph> getGraphs() {
        List<WSGraph> list = new ArrayList<WSGraph>();
        for (ONDEXGraph graph : ONDEXGraphRegistry.graphs.values()) {
            if (!graph.getName().equals(ROOT_GRAPH_ID)) {
                list.add(new WSGraph(graph));
            }
        }
        return list;
    }

    @Override
    public WSRelation getRelation(Long graphId, Integer id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        return new WSRelation(graph.getRelation(id));
    }

    @Override
    public WSRelationGDS getRelationGDS(Long graphId, Integer relationId, String attributeNameId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        ONDEXRelation relation = graph.getRelation(relationId);
        AttributeName attributeName = metaData.getAttributeName(attributeNameId);
        return new WSRelationGDS(relation.getRelationGDS(attributeName));
    }

    @Override
    public List<WSRelationGDS> getRelationGDSs(Long graphId, Integer relationId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXRelation relation = graph.getRelation(relationId);
        List<WSRelationGDS> list = new ArrayList<WSRelationGDS>();
        ONDEXIterator<GDS<ONDEXRelation>> relationGDSs = relation.getRelationGDSs();
        while (relationGDSs.hasNext()) {
            list.add(new WSRelationGDS(relationGDSs.next()));
        }
        return list;
    }

    @Override
    public WSRelation getRelationOfType(Long graphId, Integer fromConceptId, Integer toConceptId, String relationTypeId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        ONDEXConcept from = graph.getConcept(fromConceptId);
        ONDEXConcept to = graph.getConcept(toConceptId);
        RelationType typeSet = metaData.getRelationType(relationTypeId);
        return new WSRelation(graph.getRelation(from, to, typeSet));
    }

    @Override
    public WSRelation getRelationOfTypeAndQualifier(Long graphId, Integer fromConceptId, Integer toConceptId, Integer qualifierConceptId, String relationTypeId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        ONDEXConcept from = graph.getConcept(fromConceptId);
        ONDEXConcept to = graph.getConcept(toConceptId);
        ONDEXConcept qualify = graph.getConcept(qualifierConceptId);
        RelationType typeSet = metaData.getRelationType(relationTypeId);
        return new WSRelation(graph.getRelation(from, to, qualify, typeSet));
    }

    @Override
    public List<WSRelation> getRelations(Long graphId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        logger.info("Got Ondex graph " + graph.getSID());
        List<WSRelation> list = new ArrayList<WSRelation>();
        ONDEXView<ONDEXRelation> relations = graph.getRelations();
        logger.info("Got relations");
        while (relations.hasNext()) {
            list.add(new WSRelation(relations.next()));
        }
        logger.info("Returning list of relations");
        return list;
    }

    @Override
    public List<WSRelation> getRelationsOfAttributeName(Long graphId, String attributeNameId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSRelation> list = new ArrayList<WSRelation>();
        AttributeName attributeName = metaData.getAttributeName(attributeNameId);
        ONDEXView<ONDEXRelation> relations = graph.getRelationsOfAttributeName(attributeName);
        if (relations != null) {
            while (relations.hasNext()) {
                list.add(new WSRelation(relations.next()));
            }
        }
        return list;
    }

    @Override
    public List<WSRelation> getRelationsOfConcept(Long graphId, Integer conceptId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        List<WSRelation> list = new ArrayList<WSRelation>();
        ONDEXConcept concept = graph.getConcept(conceptId);
        ONDEXView<ONDEXRelation> relations = graph.getRelationsOfConcept(concept);
        if (relations != null) {
            while (relations.hasNext()) {
                list.add(new WSRelation(relations.next()));
            }
        }
        return list;
    }

    @Override
    public List<WSRelation> getRelationsOfConceptClass(Long graphId, String conceptClassId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSRelation> list = new ArrayList<WSRelation>();
        ConceptClass conceptClass = metaData.getConceptClass(conceptClassId);
        ONDEXView<ONDEXRelation> relations = graph.getRelationsOfConceptClass(conceptClass);
        if (relations != null) {
            while (relations.hasNext()) {
                list.add(new WSRelation(relations.next()));
            }
        }
        return list;
    }

    @Override
    public List<WSRelation> getRelationsOfContext(Long graphId, Integer contextConceptId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        List<WSRelation> list = new ArrayList<WSRelation>();
        ONDEXConcept concept = graph.getConcept(contextConceptId);
        ONDEXView<ONDEXRelation> relations = graph.getRelationsOfContext(concept);
        if (relations != null) {
            while (relations.hasNext()) {
                list.add(new WSRelation(relations.next()));
            }
        }
        return list;
    }

    @Override
    public List<WSRelation> getRelationsOfCV(Long graphId, String cvId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSRelation> list = new ArrayList<WSRelation>();
        CV cv = metaData.getCV(cvId);
        ONDEXView<ONDEXRelation> relations = graph.getRelationsOfCV(cv);
        if (relations != null) {
            while (relations.hasNext()) {
                list.add(new WSRelation(relations.next()));
            }
        }
        return list;
    }

    @Override
    public List<WSRelation> getRelationsOfEvidenceType(Long graphId, String evidenceTypeId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSRelation> list = new ArrayList<WSRelation>();
        EvidenceType evidenceType = metaData.getEvidenceType(evidenceTypeId);
        ONDEXView<ONDEXRelation> relations = graph.getRelationsOfEvidenceType(evidenceType);
        if (relations != null) {
            while (relations.hasNext()) {
                list.add(new WSRelation(relations.next()));
            }
        }
        return list;
    }

    @Override
    public List<WSRelation> getRelationsOfRelationType(Long graphId, String relationTypeId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSRelation> list = new ArrayList<WSRelation>();
        RelationType relationType = metaData.getRelationType(relationTypeId);
        ONDEXView<ONDEXRelation> relations = graph.getRelationsOfRelationType(relationType);
        if (relations != null) {
            while (relations.hasNext()) {
                list.add(new WSRelation(relations.next()));
            }
        }
        return list;
    }

    @Override
    public WSRelationType getRelationType(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        return new WSRelationType(metaData.getRelationType(id));
    }

    @Override
    public List<WSRelationType> getRelationTypes(Long graphId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSRelationType> list = new ArrayList<WSRelationType>();
        ONDEXIterator<RelationType> relationTypes = metaData.getRelationTypes();
        while (relationTypes.hasNext()) {
            list.add(new WSRelationType(relationTypes.next()));
        }
        return list;
    }

    @Override
    public WSUnit getUnit(Long graphId, String id) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        return new WSUnit(metaData.getUnit(id));
    }

    @Override
    public List<WSUnit> getUnits(Long graphId) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        ONDEXGraphMetaData metaData = graph.getMetaData();
        List<WSUnit> list = new ArrayList<WSUnit>();
        ONDEXIterator<Unit> units = metaData.getUnits();
        while (units.hasNext()) {
            list.add(new WSUnit(units.next()));
        }
        return list;
    }

    @Override
    public Long importGraph(Long graphId, String xml) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        try {
            read(graph, new StringReader(xml));
            databases.get(graph.getSID()).commit();
        } catch (IOException e) {
            logger.error("Error importing into graph '" + graph.getName() + "'", e);
        } catch (XMLStreamException e) {
            logger.error("Error importing into graph '" + graph.getName() + "'", e);
        } catch (ClassNotFoundException e) {
            logger.error("Error importing into graph '" + graph.getName() + "'", e);
        }
        return new WSGraph(graph).getId();
    }

    @Override
    public Long importGraphGzip(Long graphId, byte[] gzippedXml) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        logger.info("Importing graph " + graph.getSID() + " from GZIP");
        try {
            readZipped(graph, new ByteArrayInputStream(gzippedXml));
            databases.get(graph.getSID()).commit();
        } catch (IOException e) {
            logger.error("Error importing into graph '" + graph.getName() + "'", e);
        } catch (XMLStreamException e) {
            logger.error("Error importing into graph '" + graph.getName() + "'", e);
        } catch (ClassNotFoundException e) {
            logger.error("Error importing into graph '" + graph.getName() + "'", e);
        }
        return new WSGraph(graph).getId();
    }

    @Override
    public Long importGraphUrl(Long graphId, String url) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        try {
            read(graph, new InputStreamReader(new URL(url).openStream()));
            databases.get(graph.getSID()).commit();
        } catch (IOException e) {
            logger.error("Error importing into graph '" + graph.getName() + "'", e);
        } catch (XMLStreamException e) {
            logger.error("Error importing into graph '" + graph.getName() + "'", e);
        } catch (ClassNotFoundException e) {
            logger.error("Error importing into graph '" + graph.getName() + "'", e);
        }
        return new WSGraph(graph).getId();
    }

    @Override
    public Long importGraphUrlGzip(Long graphId, String url) {
        ONDEXGraph graph = ONDEXGraphRegistry.graphs.get(graphId);
        try {
            readZipped(graph, new URL(url).openStream());
            databases.get(graph.getSID()).commit();
        } catch (IOException e) {
            logger.error("Error importing into graph '" + graph.getName() + "'", e);
        } catch (XMLStreamException e) {
            logger.error("Error importing into graph '" + graph.getName() + "'", e);
        } catch (ClassNotFoundException e) {
            logger.error("Error importing into graph '" + graph.getName() + "'", e);
        }
        return new WSGraph(graph).getId();
    }

    private void loadTestGraphs() {
        List<String> testGraphs = new ArrayList<String>();
        testGraphs.add("phibase_Bcinerea_filtered");
        for (String testGraph : testGraphs) {
            ONDEXGraph graph = makeGraph(testGraph);
            String graphFile = "data/" + testGraph + ".xml.gz";
            InputStream is = OndexGraphImpl.class.getClassLoader().getResourceAsStream(graphFile);
            try {
                readZipped(graph, is);
                databases.get(graph.getSID()).commit();
            } catch (IOException e) {
                logger.error("Error loading test graph '" + graph.getName() + "'", e);
            } catch (XMLStreamException e) {
                logger.error("Error loading test graph '" + graph.getName() + "'", e);
            } catch (ClassNotFoundException e) {
                logger.error("Error loading test graph '" + graph.getName() + "'", e);
            }
        }
    }

    private ONDEXGraph makeGraph(String name) {
        String graphID = UUID.randomUUID().toString();
        File graphDbDir = new File(dbPath, graphID);
        graphDbDir.mkdirs();
        BerkeleyEnv berkeleyEnv = new BerkeleyEnv(graphDbDir.getPath(), name, new ONDEXCoreLogger());
        logger.info("Graph directory is " + graphDbDir.toURI());
        ONDEXGraph graph = berkeleyEnv.getAbstractONDEXGraph();
        databases.put(graph.getSID(), berkeleyEnv);
        ONDEXGraphMetaData metaData = rootGraph.getMetaData();
        metaData.getFactory().createEvidenceType(graphID, name);
        metaData.getFactory().createEvidenceType(String.valueOf(graph.getSID()), graphID);
        databases.get(rootGraph.getSID()).commit();
        return graph;
    }

    private void read(ONDEXGraph graph, Reader input) throws IOException, XMLStreamException, ClassNotFoundException {
        logger.info("Reading graph");
        XMLInputFactory2 xmlif = (XMLInputFactory2) XMLInputFactory2.newInstance();
        xmlif.configureForSpeed();
        XMLStreamReader2 xmlr = (XMLStreamReader2) xmlif.createXMLStreamReader(input);
        XmlParser parser = new XmlParser();
        Int2IntOpenHashMap table = new Int2IntOpenHashMap();
        Int2ObjectOpenHashMap<IntOpenHashSet> context = new Int2ObjectOpenHashMap<IntOpenHashSet>();
        ConceptParser cp = new ConceptParser(graph, table, context);
        parser.registerParser("concept", cp);
        RelationParser rp = new RelationParser(graph, table);
        parser.registerParser("relation", rp);
        try {
            parser.parse(xmlr);
        } catch (Exception e) {
            logger.warn("Problem with parsing graph: " + e);
        }
        ConceptParser.syncContext(graph, table, context);
        xmlr.close();
    }

    private void readDatabase(File dbDir) {
        Session session = Session.getSession(DEFAULT_USER, DEFAULT_PASSWORD);
        Session.setSessionForThread(session);
        if (dbDir.exists() && dbDir.listFiles().length > 0) {
            for (File file : dbDir.listFiles()) {
                if (file.isDirectory()) {
                    String dirName = file.getName();
                    if (dirName.equals(ROOT_GRAPH_ID)) {
                        BerkeleyEnv berkeleyEnv = new BerkeleyEnv(file.getPath(), ROOT_GRAPH_ID, new ONDEXCoreLogger());
                        rootGraph = berkeleyEnv.getAbstractONDEXGraph();
                        databases.put(rootGraph.getSID(), berkeleyEnv);
                    }
                }
            }
            if (rootGraph == null) {
                logger.error("Failed to load root graph");
                createRootGraph(dbDir);
            }
            for (File file : dbDir.listFiles()) {
                if (file.isDirectory()) {
                    String dirName = file.getName();
                    if (!dirName.equals(ROOT_GRAPH_ID)) {
                        ONDEXGraphMetaData metaData = rootGraph.getMetaData();
                        EvidenceType evidenceType = metaData.getEvidenceType(dirName);
                        String graphName = dirName;
                        if (evidenceType != null) {
                            graphName = evidenceType.getFullname();
                        }
                        BerkeleyEnv berkeleyEnv = new BerkeleyEnv(file.getPath(), graphName, new ONDEXCoreLogger());
                        ONDEXGraph graph = berkeleyEnv.getAbstractONDEXGraph();
                        databases.put(graph.getSID(), berkeleyEnv);
                    }
                }
            }
        } else {
            createRootGraph(dbDir);
            loadTestGraphs();
        }
        Session.setSessionForThread(null);
    }

    private void readZipped(ONDEXGraph graph, InputStream inputStream) throws IOException, XMLStreamException, ClassNotFoundException {
        logger.info("Reading zipped graph");
        InputStreamReader gzipInputStream = new InputStreamReader(new GZIPInputStream(inputStream), CharsetNames.CS_UTF8);
        read(graph, gzipInputStream);
    }

    private void removeGraph(long graphID) {
        String name = null;
        try {
            databases.remove(graphID);
            ONDEXGraphMetaData metaData = rootGraph.getMetaData();
            if (metaData != null) {
                name = metaData.getEvidenceType(String.valueOf(graphID)).getFullname();
                metaData.deleteEvidenceType(String.valueOf(graphID));
                metaData.deleteEvidenceType(name);
            }
            databases.get(rootGraph.getSID()).commit();
            DirUtils.deleteTree(new File(dbPath, name));
        } catch (Exception e) {
            logger.error("Error removing graph '" + name + "'", e);
        }
    }

    public void setOndexDir(String ondexDir) {
        logger.info("ondexDir set to " + ondexDir);
        File ondexDirFile = new File(ondexDir);
        if (!ondexDirFile.exists()) {
            if (ondexDirFile.mkdirs()) {
                copyResourceToDir(ondexDir, "config.xml");
                copyResourceToDir(ondexDir, "passwords");
                copyResourceToDir(ondexDir, "log4j.properties");
            } else {
                logger.error("Unable to create directory '" + ondexDir + "'");
            }
        } else if (!ondexDirFile.canWrite()) {
            logger.error("Unable to write to directory '" + ondexDir + "'");
        }
        Config.ondexDir = ondexDir;
        dbPath = new File(ondexDir, DB_DIR);
        logger.info("dbPath Directory is " + dbPath.toURI());
        CleanupContextListener.setOndexGraph(this);
        readDatabase(dbPath);
    }

    public void cleanup() {
        for (BerkeleyEnv database : databases.values()) {
            database.commit();
            database.cleanup();
        }
    }

    private void copyResourceToDir(String ondexDir, String resource) {
        InputStream inputStream = OndexGraphImpl.class.getClassLoader().getResourceAsStream(resource);
        try {
            FileWriter fileWriter = new FileWriter(new File(ondexDir, resource));
            IOUtils.copy(inputStream, fileWriter);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            logger.error("Unable to copy '" + resource + "' file to " + ondexDir + "'");
        }
    }

    private void write(ONDEXGraph graph, Writer writer) throws XMLStreamException {
        WstxOutputFactory xmlw = (WstxOutputFactory) WstxOutputFactory.newInstance();
        xmlw.configureForRobustness();
        xmlw.setProperty(XMLOutputFactory2.IS_REPAIRING_NAMESPACES, false);
        xmlw.setProperty(WstxOutputProperties.P_OUTPUT_FIX_CONTENT, true);
        xmlw.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_CONTENT, true);
        XMLStreamWriter2 xmlWriteStream = (XMLStreamWriter2) xmlw.createXMLStreamWriter(writer, CharsetNames.CS_UTF8);
        GlobalPermissions.getInstance(graph.getSID());
        new Export().buildDocument(xmlWriteStream, graph);
        xmlWriteStream.flush();
        xmlWriteStream.close();
    }
}
