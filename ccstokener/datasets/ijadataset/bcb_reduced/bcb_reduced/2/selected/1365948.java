package net.datao.datamodel.impl.jena;

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.ontology.impl.OWLProfile;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasonerFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;
import com.vladium.utils.URLFactory;
import edu.uci.ics.jung.visualization.util.ChangeEventSupport;
import edu.uci.ics.jung.visualization.util.DefaultChangeEventSupport;
import net.datao.datamodel.*;
import net.datao.datamodel.helpers.jena.OntClassHelper;
import net.datao.datamodel.helpers.jena.OntPropertyHelper;
import net.datao.datamodel.helpers.jena.QueryHelper;
import net.datao.datamodel.impl.*;
import net.datao.jung.ontologiesItems.Ont;
import net.datao.repository.OntRepository;
import net.datao.utils.StringHelper;
import org.apache.commons.collections.map.MultiKeyMap;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableModel;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OModelJenaImpl implements OModel {

    protected boolean inference = false;

    protected ChangeEventSupport changeSupport = new DefaultChangeEventSupport(this);

    protected OntModel ontModel;

    protected OntRepository ontRepository;

    public OModelJenaImpl(OntRepository ont) {
        ontRepository = ont;
        initialize();
    }

    public void close() {
        ontModel.close();
    }

    public void dispose() {
        ontModel.close();
        ontModel = null;
    }

    protected void initialize() {
        Model lowLevelModel;
        lowLevelModel = ModelFactory.createOntologyModel();
        Resource configuration = ModelFactory.createDefaultModel().createResource();
        lowLevelModel.createProperty("http://internalStuff/is-A");
        GenericRuleReasoner r = (GenericRuleReasoner) GenericRuleReasonerFactory.theInstance().create(configuration);
        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF, lowLevelModel);
        OntDocumentManager.getInstance().setProcessImports(false);
        addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (uriToOClass != null) {
                    uriToOClass.clear();
                    uriToOClass = null;
                }
                if (setOfRootOClasses != null) {
                    setOfRootOClasses.clear();
                    setOfRootOClasses = null;
                }
            }
        });
    }

    protected List<OClass> setOfRootOClasses;

    public synchronized List<OClass> getRootOClasses() {
        if (setOfRootOClasses == null) {
            setOfRootOClasses = new ArrayList<OClass>();
            for (Iterator i = listRootClasses(); i.hasNext(); ) {
                Statement st = (Statement) i.next();
                Resource resource = st.getSubject();
                if (resource.canAs(OntClass.class)) {
                    String uri = ((OntClass) resource.as(OntClass.class)).getURI();
                    if (!StringHelper.isEmpty(uri)) {
                        OClass oc = generateOClass(uri);
                        if (setOfRootOClasses == null) throw new RuntimeException("setOfRootOClasses became null");
                        if (oc != null && !setOfRootOClasses.contains(oc)) setOfRootOClasses.add(oc);
                    }
                }
            }
            Collections.sort(setOfRootOClasses);
        }
        return setOfRootOClasses;
    }

    protected ExtendedIterator listRootClasses() {
        return ontModel.listStatements(null, ontModel.createProperty("http://www.w3.org/2000/01/rdf-schema#", "subClassOf"), ontModel.createProperty("http://www.w3.org/2000/01/rdf-schema#", "Resource")).filterKeep(new Filter() {

            @Override
            public boolean accept(Object st) {
                Resource o = ((Statement) st).getSubject();
                if (o == null) return false;
                if (o.getURI() == null) return false;
                if (o.isAnon() || o.isLiteral()) return false;
                if (o.getURI().contains("w3")) return false;
                if (!o.canAs(OntClass.class)) return false;
                OntClass c = (OntClass) o.as(OntClass.class);
                if (c.isProperty()) return false;
                return acceptClassOrNot(c);
            }
        });
    }

    protected boolean acceptClassOrNot(OntClass c) {
        return (c.getEquivalentClass() == null);
    }

    public boolean isRoot(OID id) {
        return isRoot(id.getURI());
    }

    public boolean isRoot(OClass oc) {
        return isRoot(oc.getURI());
    }

    public boolean isRoot(String uri) {
        for (OClass oc : getRootOClasses()) if (oc.getURI().equals(uri)) return true;
        return false;
    }

    private static OID getOID(Resource r) {
        if (r == null) throw new RuntimeException("Trying to get OID for a null resource");
        return new OIDImpl(r.getNameSpace(), r.getLocalName(), r.getURI());
    }

    protected HashMap<String, OClass> uriToOClass;

    protected boolean autoLoadFromClassUri = true;

    protected boolean autoLoadFromPropertyUri = true;

    public OClass generateOClass(String uri) {
        if (StringHelper.isEmpty(uri)) {
            System.err.println("OClass could not be generated for null URI");
            return null;
        }
        if (uriToOClass == null || !uriToOClass.containsKey(uri)) {
            if (autoLoadFromClassUri) manageOntologyLoadingFromResourceUri(uri);
            if (uriToOClass == null) uriToOClass = new HashMap<String, OClass>();
            Resource jenaClass = getOntModel().getResource(uri);
            if (jenaClass == null) System.out.println("Generating OClass " + uri + " failed.");
            OClass oc = null;
            try {
                oc = new OClassImpl(getOID(jenaClass));
                uriToOClass.put(uri, oc);
            } catch (Exception e) {
                System.err.println("Problem while managing " + (jenaClass == null ? "null" : jenaClass.getLocalName()));
                if (uriToOClass == null) System.err.println("uriToOClass is null");
                if (oc == null) System.err.println("Generated OClass is null");
                e.printStackTrace();
            }
        }
        return uriToOClass.get(uri);
    }

    private void manageOntologyLoadingFromResourceUri(String uri) {
        if (StringHelper.isEmpty(uri)) {
            System.err.println("Ontology to load from resource uri " + uri + " failed");
            return;
        }
        if (!uri.contains("w3")) try {
            String s = uri.substring(0, uri.indexOf('#'));
            if (ontRepository == null) System.err.println("Please initialize ontRepository");
            Ont o = ontRepository.getOntFromLogicalUri(s);
            System.err.println("Ready to add ont " + o.getURI());
            addOnt(o);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private MultiKeyMap originClassAndPropertyUriToOProperty = new MultiKeyMap();

    public OProperty generateOProperty(OClass originClass, String propertyUri) {
        if (!originClassAndPropertyUriToOProperty.containsKey(originClass, propertyUri)) {
            if (autoLoadFromPropertyUri) manageOntologyLoadingFromResourceUri(propertyUri);
            OntProperty jenaProperty = getOntModel().getOntProperty(propertyUri);
            if (jenaProperty == null) System.err.println("Unable to get Property: " + propertyUri + ". Returning null");
            OProperty newOProperty = new OPropertyImpl(originClass, getOID(jenaProperty));
            System.out.println("Creating " + newOProperty.getName() + " as an OProperty");
            originClassAndPropertyUriToOProperty.put(originClass, propertyUri, newOProperty);
        }
        return (OProperty) originClassAndPropertyUriToOProperty.get(originClass, propertyUri);
    }

    public void addOnt(Ont o) {
        addOnt(o, false);
    }

    public void addOnt(Ont o, boolean infer) {
        try {
            loadOntInModel(o);
            addOntToDocumentManager(o);
            loadedOnts.add(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (inference) doInference();
    }

    public void clear() {
        ontModel.removeAll();
        loadedOnts.clear();
    }

    public void addRule(Object rule) {
        throw new RuntimeException("Rules are not implemented yet.");
    }

    public void doInference() {
        System.out.println("JenaOntModelImpl does not handle inference. Use PelletOntModelImpl instead");
    }

    public OntModel getOntModel() {
        return ontModel;
    }

    protected void addOntToDocumentManager(Ont o) {
        String physicalURI = o.getURL();
        String logicalURI = o.getURI();
        Logger.getLogger("OntDocumentManager").log(Level.INFO, "Mapping " + logicalURI + " to " + physicalURI);
        OntDocumentManager.getInstance().addAltEntry(logicalURI, physicalURI);
    }

    public Set<Ont> getLoadedOnts() {
        return loadedOnts;
    }

    protected boolean ontWithThisUriIsAlreadyLoaded(String uri) {
        for (Ont o : loadedOnts) if (o.getURI().equals(uri)) return true;
        return false;
    }

    protected Set<Ont> loadedOnts = new HashSet<Ont>();

    protected void loadOntInModel(Ont o) throws Exception {
        String uri = o.getURI();
        OntDocumentManager.getInstance().setProcessImports(false);
        if (ontWithThisUriIsAlreadyLoaded(uri)) return;
        String altUri = o.getURL();
        System.out.println("Loading ontology with uri: " + uri + "in OModel. Real URL is: " + altUri);
        String lang = "";
        if (altUri.contains(".nt")) lang = "N-TRIPLE";
        URL url;
        if (!uri.equals(altUri)) url = URLFactory.newURL(altUri); else url = URLFactory.newURL(uri);
        InputStream fis = url.openStream();
        getOntModel().read(fis, "", lang);
        fis.close();
        Set<String> toBeLoaded = new HashSet<String>();
        StmtIterator i = getOntModel().listStatements(null, (new OWLProfile()).IMPORTS(), (RDFNode) null);
        while (i.hasNext()) {
            Statement s = i.nextStatement();
            String urii = s.getObject().toString();
            if (!ontWithThisUriIsAlreadyLoaded(urii)) {
                loadedOnts.add(ontRepository.getOntFromLogicalUri(urii));
            }
        }
        i.close();
        for (String urii : toBeLoaded) addOnt(ontRepository.getOntFromLogicalUri(urii));
        loadedOnts.add(o);
        fireStateChanged();
    }

    public void addChangeListener(ChangeListener l) {
        changeSupport.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        changeSupport.removeChangeListener(l);
    }

    public ChangeListener[] getChangeListeners() {
        return changeSupport.getChangeListeners();
    }

    public void fireStateChanged() {
        System.err.println("OModel changed. Firing changeEvent");
        setOfRootOClasses = null;
        changeSupport.fireStateChanged();
    }

    public OCardinality getCardinality(OProperty oProp, OClass oClass) {
        OntProperty p = getOntModel().getOntProperty(oProp.getURI());
        OntClass c = getOntModel().getOntClass(oClass.getURI());
        return OntPropertyHelper.getCardinality(p, c);
    }

    public TableModel getResultsAsTableModel(int language, String query) {
        return QueryHelper.getResultsAsTableModel(language, query, getOntModel());
    }

    protected List<OClass> getOClassList(List<OID> oids) {
        List<OClass> subClasses = new ArrayList<OClass>();
        for (OID id : oids) {
            subClasses.add(generateOClass(id.getURI()));
        }
        return subClasses;
    }

    protected List<OProperty> getOProperties(OClass originClass, List<OID> oids) {
        List<OProperty> oProperties = new ArrayList<OProperty>();
        for (OID id : oids) {
            oProperties.add(generateOProperty(originClass, id.getURI()));
        }
        return oProperties;
    }

    protected Map<OClass, List<OClass>> subOClasses = new HashMap<OClass, List<OClass>>();

    public List<OClass> getSubOClasses(OClass oClass) {
        if (!subOClasses.containsKey(oClass)) {
            List<OID> subOids = OntClassHelper.getSubOClasses(getOntModel().getOntClass(oClass.getURI()));
            List<OClass> subCLasses = getOClassList(subOids);
            Collections.sort(subCLasses);
            subOClasses.put(oClass, subCLasses);
        }
        return subOClasses.get(oClass);
    }

    protected Map<OClass, List<OProperty>> classOProperties = new HashMap<OClass, List<OProperty>>();

    public List<OProperty> getOProperties(OClass originClass) {
        if (!classOProperties.containsKey(originClass)) {
            List<OID> subOids = OntClassHelper.getProperties(getOntModel().getOntClass(originClass.getURI()));
            List<OProperty> oProperties = getOProperties(originClass, subOids);
            classOProperties.put(originClass, oProperties);
        }
        return classOProperties.get(originClass);
    }

    public List<OClassRange> getRange(OProperty p) {
        OClass c = p.getOriginClass();
        OntClass ontClass = getOntModel().getOntClass(c.getURI());
        OntProperty ontProperty = getOntModel().getOntProperty(p.getURI());
        List<OIDRange> oidRange = OntPropertyHelper.getPropertyRange(ontClass, ontProperty);
        List<OClassRange> oclassRanges = new ArrayList();
        for (OIDRange r : oidRange) {
            if (r != null) {
                OClassRange oClassRange = new OClassRangeImpl(generateOClass(r.getTarget().getURI()), r.getCardinality());
                oclassRanges.add(oClassRange);
            }
        }
        return oclassRanges;
    }
}
