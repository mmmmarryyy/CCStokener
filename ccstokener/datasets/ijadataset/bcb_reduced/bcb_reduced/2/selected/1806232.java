package net.datao.datamodel.impl.ibm;

import aterm.ATermAppl;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.ibm.iodt.sor.OntologyStoreManager;
import com.ibm.iodt.sor.SPARQLQueryEngine;
import com.ibm.iodt.sor.db.DAO;
import com.ibm.iodt.sor.query.SPARQLResultSet;
import com.ibm.iodt.sor.translator.EODMTranslator;
import com.ibm.iodt.sor.utils.Config;
import com.ibm.iodt.sor.utils.SORException;
import com.vladium.utils.URLFactory;
import edu.uci.ics.jung.visualization.util.ChangeEventSupport;
import edu.uci.ics.jung.visualization.util.DefaultChangeEventSupport;
import net.datao.datamodel.*;
import net.datao.datamodel.helpers.ibm.MyPelletTBoxInferenceEngine;
import net.datao.datamodel.helpers.ibm.SorToOModelHelper;
import net.datao.datamodel.impl.OClassImpl;
import net.datao.datamodel.impl.OIDImpl;
import net.datao.jung.ontologiesItems.Ont;
import net.datao.repository.OntRepository;
import net.datao.utils.StringHelper;
import net.datao.virtuoso.RunVirtuoso;
import org.eclipse.eodm.owl.resource.parser.impl.OWLParserImpl;
import org.mindswap.pellet.Role;
import org.mindswap.pellet.taxonomy.Taxonomy;
import org.mindswap.pellet.taxonomy.TaxonomyNode;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: lolive
 * Date: 3 mars 2009
 * Time: 10:28:57
 * To change this template use File | Settings | File Templates.
 */
public class OModelSorImpl implements OModel {

    protected boolean inference = false;

    protected ChangeEventSupport changeSupport = new DefaultChangeEventSupport(this);

    protected Model lowLevelModel;

    protected OntModel ontModel;

    protected OntRepository ontRepository;

    public OModelSorImpl(OntRepository ont) {
        ontRepository = ont;
        try {
            initialize();
        } catch (SORException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    OntologyStoreManager sor;

    protected Map<String, TaxonomyNode> uriToNode = new HashMap<String, TaxonomyNode>();

    protected List<OClass> setOfRootOClasses = new ArrayList<OClass>();

    private void initialize() throws SORException, FileNotFoundException {
        initializeStore();
        addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                parseTaxonomy();
            }
        });
    }

    private void initializeStore() throws SORException, FileNotFoundException {
        Config.setConfigFile(new FileInputStream("configuration/sor-virtuoso.cfg"));
        DAO.install();
        OntologyStoreManager.newOntologyStore("tata");
        sor = new OntologyStoreManager("tata");
        sor.selectOntologyStore("tata");
    }

    protected Set<Ont> loadedOnts = new HashSet<Ont>();

    private void addOntToDocumentManager(Ont o) {
        try {
            String physicalURI = o.getURL();
            String logicalURI = o.getURI();
            OWLParserImpl owlParser = ((OWLParserImpl) ((EODMTranslator) Config.getOWLTranslator("tata")).parser);
            Hashtable hashtable = owlParser.URLmapping;
            if (!hashtable.containsKey(logicalURI)) {
                owlParser.addURL2FileMapping(logicalURI, physicalURI);
                for (Ont imp : o.getImports()) addOntToDocumentManager(imp);
            }
        } catch (SORException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void addOnt(Ont o, boolean infer) {
        try {
            sor.getConnection();
        } catch (SORException e) {
            e.printStackTrace();
        }
        if (loadedOnts.contains(o)) return;
        addOntToDocumentManager(o);
        try {
            URL url = null;
            try {
                String urlOfOnto = o.getURL();
                url = URLFactory.newURL(urlOfOnto);
                InputStream fis = url.openStream();
                sor.addOWLDocument(fis, "", false);
                try {
                    sor.commit();
                    loadedOnts.add(o);
                    fireStateChanged();
                } catch (SORException e) {
                    e.printStackTrace();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SORException e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        try {
            sor.getConnection();
        } catch (SORException e) {
            e.printStackTrace();
        }
        for (Ont o : loadedOnts) {
            String uri = o.getURI();
            try {
                sor.deleteOntology(uri);
            } catch (SORException e) {
                e.printStackTrace();
            }
        }
        try {
            sor.commit();
        } catch (SORException e) {
            e.printStackTrace();
        }
        OntologyStoreManager.deleteOntologyStore("tata");
        OntologyStoreManager.newOntologyStore("tata");
    }

    public void addRule(Object rule) {
        throw new RuntimeException("Rules are not implemented yet.");
    }

    public void doInference() {
    }

    protected Map<String, OClass> uriToOClasses = new HashMap<String, OClass>();

    protected void parseTaxonomy() {
        taxonomy = null;
        if (uriToNode != null) {
            uriToNode.clear();
        }
        if (uriToOClasses != null) {
            uriToOClasses.clear();
        }
        if (setOfRootOClasses != null) {
            setOfRootOClasses.clear();
        }
        Set<ATermAppl> termApplSet = (Set<ATermAppl>) getTaxonomy().getClasses();
        for (ATermAppl a : termApplSet) {
            TaxonomyNode value = getTaxonomy().getNode(a);
            String s = value.getName().getName();
            if (!s.contains("#")) continue;
            uriToNode.put(s, value);
            OClassImpl aClass = new OClassImpl(new OIDImpl(s));
            uriToOClasses.put(s, aClass);
            if (isRoot(s) && !setOfRootOClasses.contains(aClass)) setOfRootOClasses.add(aClass);
        }
    }

    Taxonomy taxonomy;

    protected Taxonomy getTaxonomy() {
        if (taxonomy == null) try {
            taxonomy = getInferenceEngine().getKB().getTaxonomy();
            return taxonomy;
        } catch (SORException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return taxonomy;
    }

    private MyPelletTBoxInferenceEngine getInferenceEngine() throws SORException {
        return (MyPelletTBoxInferenceEngine) Config.getTBoxInferenceEngine();
    }

    public boolean isRoot(OID id) {
        return isRoot(id.getURI());
    }

    public boolean isRoot(String uri) {
        TaxonomyNode taxonomyNode = uriToNode.get(uri);
        List<TaxonomyNode> supers = taxonomyNode.getSupers();
        if (uri.contains("Thing")) System.err.println("Talking about Thing");
        TaxonomyNode top = getTaxonomy().getTop();
        boolean b = supers.contains(top) && (!uri.startsWith("http://www.w3.org/2001/XMLSchema#"));
        if (b) return true; else return false;
    }

    public boolean isRoot(OClass oClass) {
        return isRoot(oClass.getURI());
    }

    public List<OClass> getRootOClasses() {
        return setOfRootOClasses;
    }

    protected Map<OClass, List<OProperty>> classOProperties = new HashMap<OClass, List<OProperty>>();

    protected List<OProperty> getOProperties(OClass originClass, List<OID> oids) {
        List<OProperty> oProperties = new ArrayList<OProperty>();
        for (OID id : oids) {
            oProperties.add(generateOProperty(originClass, id.getURI()));
        }
        return oProperties;
    }

    public List<OProperty> getOProperties(OClass originClass) {
        if (!classOProperties.containsKey(originClass)) {
            TaxonomyNode taxonomyNode = uriToNode.get(originClass.getURI());
            ATermAppl from = taxonomyNode.getName();
            Set<Role> rr = null;
            try {
                rr = getInferenceEngine().getKB().getPossibleProperties(from);
            } catch (SORException e) {
                e.printStackTrace();
            }
            List<OID> subOids = SorToOModelHelper.getProperties(rr);
            List<OProperty> oProperties = getOProperties(originClass, subOids);
            classOProperties.put(originClass, oProperties);
        }
        return classOProperties.get(originClass);
    }

    protected Map<OClass, List<OClass>> subOClasses = new HashMap<OClass, List<OClass>>();

    protected List<OClass> getOClassList(List<OID> oids) {
        List<OClass> subClasses = new ArrayList<OClass>();
        for (OID id : oids) {
            subClasses.add(generateOClass(id.getURI()));
        }
        return subClasses;
    }

    public List<OClass> getSubOClasses(OClass oClass) {
        if (!subOClasses.containsKey(oClass)) {
            TaxonomyNode taxonomyNode = uriToNode.get(oClass.getURI());
            ATermAppl from = taxonomyNode.getName();
            List<OID> subOids = null;
            try {
                subOids = SorToOModelHelper.getSubOClasses(getInferenceEngine().getKB(), from);
            } catch (SORException e) {
                e.printStackTrace();
            }
            List<OClass> subCLasses = getOClassList(subOids);
            Collections.sort(subCLasses);
            subOClasses.put(oClass, subCLasses);
        }
        return subOClasses.get(oClass);
    }

    public List<OClassRange> getRange(OProperty p) {
        return null;
    }

    public OCardinality getCardinality(OProperty p, OClass targetOClass) {
        return null;
    }

    public Set<Ont> getLoadedOnts() {
        return loadedOnts;
    }

    public void close() {
    }

    public void dispose() {
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
        changeSupport.fireStateChanged();
    }

    public OProperty generateOProperty(OClass originClass, String propertyUri) {
        return null;
    }

    protected HashMap<String, OClass> uriToOClass;

    private void manageOntologyLoadingFromResourceUri(String uri) {
        if (StringHelper.isEmpty(uri)) {
            System.err.println("Ontology to load from resource uri " + uri + " failed");
            return;
        }
        if (!uri.contains("w3")) try {
            addOnt(OModelSingleton.getOntRepository().getOntFromLogicalUri(uri.substring(0, uri.indexOf('#'))), true);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getCause());
        }
    }

    public OClass generateOClass(String uri) {
        if (StringHelper.isEmpty(uri)) {
            System.err.println("OClass could not be generated for null URI");
            return null;
        }
        if (uriToOClass == null || !uriToOClass.containsKey(uri)) {
            if (uriToOClass == null) uriToOClass = new HashMap<String, OClass>();
            if (!uriToNode.containsKey(uri)) System.out.println("Generating OClass " + uri + " failed.");
            OClass oc = null;
            try {
                oc = new OClassImpl(new OIDImpl(uri));
                uriToOClass.put(uri, oc);
            } catch (Exception e) {
                System.err.println("Problem while managing " + (uri == null ? "null" : uri));
                if (uriToOClass == null) System.err.println("uriToOClass is null");
                if (oc == null) System.err.println("Generated OClass is null");
                e.printStackTrace();
            }
        }
        return uriToOClass.get(uri);
    }

    public TableModel getResultsAsTableModel(int language, String query) {
        SPARQLQueryEngine sparql = null;
        try {
            sparql = new SPARQLQueryEngine("tata");
        } catch (SORException e) {
            e.printStackTrace();
        }
        sparql.setInference(true);
        DefaultTableModel tm = new DefaultTableModel();
        try {
            long start = System.currentTimeMillis();
            SPARQLResultSet result;
            result = sparql.getQueryResult(query);
            String[] names = (String[]) result.getVariableNameList().toArray(new String[0]);
            String row[] = new String[result.getVariableNameList().size()];
            while (result.next()) {
                for (int i = 0; i < names.length; i++) {
                    String col = names[i];
                    String rVar = result.getString(col);
                    if (i >= tm.getColumnCount()) tm.addColumn(col.replaceFirst("[0-9]*$", ""));
                    row[i] = rVar;
                }
                tm.addRow(row);
            }
            result.close();
            sparql.close();
            return tm;
        } catch (SQLException e) {
            System.err.println("Encounting database error");
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (SORException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
