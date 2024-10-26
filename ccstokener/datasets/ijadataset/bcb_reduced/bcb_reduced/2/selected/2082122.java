package rbsla.owl;

import java.io.Writer;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Hashtable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.reasoner.*;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.ontology.OntModel;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.jena.PelletReasoner;
import org.mandarax.kernel.Term;
import ws.prova.reference.VariableTermImpl;
import ws.prova.reference.ConstantTermImpl;
import ws.prova.reference.ComplexTermImpl;

/**
 * <p>Implementation of the OWL2Prova framework. 
 * <br>
 * <br>OWL2Prova can be used to dynamically query RDF/RDFS/OWL models via an external reasoner
 * at runtime or translate them to prova facts 
 * and query the translated RDF models statically.
 * <br>
 * <br>Translates a RDF/RDFS/OWL file to a Prova script which can be consulted and queried.
 * The reasoner is used to reason over the plain RDF data and creates an inferred model. 
 * The converter is used to translate the RDF triples of the form "subject predicate object"
 * into logical facts/rules, e.g. Description Logic Programs.
 *    
 * <p>The following predefined reasoners can be used (see constructor):
 * <ul><li>  "" | "empty" | null = no reasoner
 * <li>  default = OWL reasoner
 * <li>  transitive = transitive reasoner
 * <li>  rdfs = RDFS rule reasoner
 * <li>  owl = OWL reasoner
 * <li>  daml = DAML reasoner
 * <li>  dl = OWL-DL reasoner (uses pellet)
 * <li>  swrl = SWRL reasoner (uses pellet)
 * <li>  rdfs_full = rdfs full reasoner
 * <li>  rdfs_simple = rdfs simple reasoner
 * <li>  owl_mini = owl mini reasoner
 * <li>  owl_micro = owl micro reasoner
 * </ul>
 * User-defined reasoners can be set (<code>setReasoner</code>).
 *   
 * <p>The following predefined converters can be used (see constructor):
 * <ul><li>  default = default converter
 * <li>  simple = simple converter
 * <li>  dlp = plain DLP converter (leads to loops)
 * <li>  dlp_standard = standard DLP with instance equivalence
 * <li>  dlp_full = full DLP converter to be used only with OWL reasoner 
 * <li>  defeasible = defeasible converter 
 * </ul>
 * User-defined converters can be set (<code>setConverter</code>). 
 *   
 * <p>The translaction process starts by calling the <code>run()</code> method with the input file which should be
 * mapped into the output format (ProvaModel) which is written into a prova file which can be 
 * consulted within a prova script. The initialized reasoner and converter are used.
 * If the input file (RDF model) changes the <code>update()</code> method can be used to make an update. 
 * 
 * <p>OWL2Prova also provides the static <code>rdf()</code> "query" method to directly query a RDF/RDFS/OWL file (model) at runtime.
 * The predefined or user-defined reasoners can be used. Inferred models might be cached.  
 * 
 * <p>The RDF is read from a URL. If the URL is a network URL (= not a local file), firewalls and proxies
 * must be configured correctly. Theer are various ways to accomplish this. 
 * E.g., one may include the following parameter on the java command line:
 * <p><code>-DsocksProxyHost=[your-proxy-domain-name-or-ip-address] </code> // for a socks proxy 
 * <br><code>-DproxySet=true -DproxyHost=[your-proxy] -DproxyPort=[your-proxy-port-number] </code> // for an http proxy
 * <p>
 * These properties can also be set programatically as follows:
 * <p><code>
 * <br>System.getProperties().put("proxySet","true");
 * <br>System.getProperties().put("proxyHost","proxy.hostname");
 * <br>System.getProperties().put("proxyPort",port_number);
 * </code>
 * 
 * @author <A HREF="mailto:paschke@in.tum.de">Adrian Paschke</A>
 * @version 0.1 <1 Feb 2005>
 * @since 0.1
 */
public class OWL2PROVA {

    private Reasoner reasoner;

    private Converter converter;

    private Writer _writer;

    private ProvaModel provaModel;

    private static final boolean NO_NS_MAPPING = true;

    private static boolean ns_mapping = true;

    public static void setNSMapping(boolean on) {
        ns_mapping = on;
    }

    public static final long NO_CACHE = -1;

    public static final long NO_TIMEOUT = -1;

    private static Map cache = new Hashtable();

    private static long cacheTimeout = NO_TIMEOUT;

    private static long cacheSize = 3;

    public static class CachedModel {

        private Model model;

        private long timestamp;

        public CachedModel(Model model, long timestamp) {
            this.model = model;
            this.timestamp = timestamp;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public Model getCachedModel() {
            return model;
        }
    }

    /**
	 * Indicates whether the cache can be used.
	 * @return the cachedModel or null
	 */
    private static Model getCachedModel(String name) {
        if (cacheSize == NO_CACHE) return null;
        if (cache.isEmpty()) return null;
        if (!cache.containsKey(name)) return null;
        CachedModel cm = (CachedModel) cache.get(name);
        if (cacheTimeout != NO_TIMEOUT && (System.currentTimeMillis() - cm.getTimestamp()) > cacheTimeout) {
            cache.remove(name);
            return null;
        } else {
            return cm.getCachedModel();
        }
    }

    private static void updateCache(Model model, Object name) {
        if (cacheSize == NO_CACHE) return;
        if (cacheTimeout != NO_TIMEOUT) for (Iterator it = cache.keySet().iterator(); it.hasNext(); ) {
            Object key = it.next();
            CachedModel cm = (CachedModel) cache.get(key);
            if ((System.currentTimeMillis() - cm.getTimestamp()) > cacheTimeout) {
                cache.remove(key);
            }
        }
        if (cache.size() < cacheSize) {
            CachedModel cm = new CachedModel(model, System.currentTimeMillis());
            cache.put(name, cm);
        }
    }

    /**
	 * Set the timeout for the cache
	 * NO_TIMEOUT / -1 = no timeout of cached models
	 * @param timeout in milliseconds
	 */
    public static void setCacheTimeout(long timeout) {
        cacheTimeout = timeout;
    }

    /**
	 * Set the size of the cache
	 * NO_Cache / -1 if no cache should be used
	 * @param size
	 */
    public static void setCacheSize(int size) {
        cacheSize = size;
    }

    /**
	 * Constructor.
	 * Uses the OWL reasoner as default and the default converter. 	
	 */
    public OWL2PROVA() {
        reasoner = ReasonerRegistry.getOWLReasoner();
        converter = ConverterRegistry.getDefaultConverter();
    }

    /**
	 * Constructor.
	 * @param reasoner Reasoner 	
	 * @param converter Converter
	 */
    public OWL2PROVA(Reasoner reasoner, Converter converter) {
        this.reasoner = reasoner;
        this.converter = converter;
    }

    public OWL2PROVA(String reasoner, String converter) {
        if (reasoner == null) this.reasoner = null; else if ((reasoner.toLowerCase().equals("")) || (reasoner.toLowerCase().equals("empty"))) this.reasoner = null; else if (reasoner.toLowerCase().equals("transitive")) this.reasoner = ReasonerRegistry.getTransitiveReasoner(); else if (reasoner.toLowerCase().equals("rdfs")) this.reasoner = ReasonerRegistry.getRDFSReasoner(); else if (reasoner.toLowerCase().equals("owl")) this.reasoner = ReasonerRegistry.getOWLReasoner(); else if (reasoner.toLowerCase().equals("dl")) {
            this.reasoner = PelletReasonerFactory.theInstance().create();
        } else if (reasoner.toLowerCase().equals("swrl")) {
            this.reasoner = PelletReasonerFactory.theInstance().create();
        } else if (reasoner.toLowerCase().equals("rdfs_simple")) this.reasoner = ReasonerRegistry.getRDFSSimpleReasoner(); else if (reasoner.toLowerCase().equals("rdfs_full")) {
            this.reasoner = ReasonerRegistry.getRDFSReasoner();
            this.reasoner.setParameter(ReasonerVocabulary.PROPsetRDFSLevel, ReasonerVocabulary.RDFS_FULL);
        } else if (reasoner.toLowerCase().equals("owl_mini")) {
            this.reasoner = ReasonerRegistry.getOWLReasoner();
        } else if (reasoner.toLowerCase().equals("owl_micro")) {
            this.reasoner = ReasonerRegistry.getOWLReasoner();
        } else this.reasoner = ReasonerRegistry.getOWLReasoner();
        if (converter.toLowerCase().equals("simple")) this.converter = ConverterRegistry.getSimpleConverter(); else if (converter.toLowerCase().equals("dlp")) this.converter = ConverterRegistry.getDLPLiteConverter(); else if (converter.toLowerCase().equals("dlp_standard")) this.converter = ConverterRegistry.getDLPStandardConverter(); else if (converter.toLowerCase().equals("dlp_full")) this.converter = ConverterRegistry.getDLPFullConverter(); else if (converter.toLowerCase().equals("default")) this.converter = ConverterRegistry.getDefaultConverter(); else this.converter = ConverterRegistry.getDefaultConverter();
    }

    /**
	 * Query a RDF/RDFS/OWL file with (subject predicate object).
	 * Uses the reasoner to infer the model.
	 * @param inputFile / URL
	 * @param reasoner name
	 * <ul><li>  "" | "empty" | null = no reasoner
	 * <li>  default = OWL reasoner
	 * <li>  transitive = transitive reasoner
	 * <li>  rdfs = RDFS rule reasoner
	 * <li>  owl = OWL reasoner
	 * <li>  daml = DAML reasoner
	 * <li>  dl = OWL-DL reasoner (uses pellet)
	 * <li>  swrl = SWRL reasoner (uses pellet)
	 * <li>  rdfs_full = rdfs full reasoner
	 * <li>  rdfs_simple = rdfs simple reasoner
	 * <li>  owl_mini = owl mini reasoner
	 * <li>  owl_micro = owl micro reasoner
	 * </ul>
	 *  @param subject RDF subject
	 *  @param predicate RDF property
	 *  @param object RDF object
	 *  @return Object[]
	 */
    public static List rdf(String inputFile, String reasoner, Object subject, Object predicate, Object object) throws Exception {
        Reasoner r = null;
        if (reasoner == null) r = null; else if ((reasoner.toLowerCase().equals("")) || (reasoner.toLowerCase().equals("empty"))) r = null; else if (reasoner.toLowerCase().equals("transitive")) r = ReasonerRegistry.getTransitiveReasoner(); else if (reasoner.toLowerCase().equals("rdfs")) r = ReasonerRegistry.getRDFSReasoner(); else if (reasoner.toLowerCase().equals("owl")) r = ReasonerRegistry.getOWLReasoner(); else if (reasoner.toLowerCase().equals("dl")) {
            r = PelletReasonerFactory.theInstance().create();
        } else if (reasoner.toLowerCase().equals("swrl")) {
            r = PelletReasonerFactory.theInstance().create();
        } else if (reasoner.toLowerCase().equals("rdfs_simple")) r = ReasonerRegistry.getRDFSSimpleReasoner(); else if (reasoner.toLowerCase().equals("rdfs_full")) {
            r = ReasonerRegistry.getRDFSReasoner();
            r.setParameter(ReasonerVocabulary.PROPsetRDFSLevel, ReasonerVocabulary.RDFS_FULL);
        } else if (reasoner.toLowerCase().equals("owl_mini")) {
            r = ReasonerRegistry.getOWLReasoner();
        } else if (reasoner.toLowerCase().equals("owl_micro")) {
            r = ReasonerRegistry.getOWLReasoner();
        } else r = ReasonerRegistry.getOWLReasoner();
        return rdf(inputFile, r, subject, predicate, object);
    }

    public static List rdf(String inputFile, Reasoner reasoner, Object subject, Object predicate, Object object) throws Exception {
        String reasonerName = "";
        if (reasoner != null) reasonerName = reasoner.getClass().getName();
        Model jenaModel = getCachedModel(inputFile + reasonerName);
        if (jenaModel == null) {
            Model defaultModel = ModelFactory.createDefaultModel();
            InputStream in;
            try {
                in = Thread.currentThread().getContextClassLoader().getResourceAsStream(inputFile);
                if (in == null) {
                    in = new FileInputStream(inputFile);
                }
            } catch (Exception e) {
                URL url = new URL(inputFile);
                in = url.openStream();
            }
            defaultModel.read(in, null);
            if (reasoner != null) jenaModel = ModelFactory.createInfModel(reasoner, defaultModel); else jenaModel = defaultModel;
            updateCache(jenaModel, inputFile + reasonerName);
        }
        return rdf(jenaModel, subject, predicate, object);
    }

    public static List rdf(Model infModel, Object s, Object p, Object o) {
        String subject = null;
        String predicate = null;
        String object = null;
        if (!(s instanceof VariableTermImpl)) subject = s.toString();
        if (!(p instanceof VariableTermImpl)) predicate = p.toString();
        if (!(o instanceof VariableTermImpl)) object = o.toString();
        String sub = subject;
        String pred = predicate;
        String obj = object;
        NameSpaceList nsl = new NameSpaceList(infModel);
        Resource resource = null;
        if (subject != null) {
            if (subject.indexOf("-") != -1 && subject.indexOf(":") != -1) {
                resource = infModel.createResource(new AnonId(subject));
            } else if (subject.indexOf("#") == -1) {
                String ns = subject.substring(0, subject.indexOf("_"));
                String value = subject.substring(subject.indexOf("_") + 1);
                subject = nsl.getNameSpace(ns) + value;
                resource = infModel.createResource(subject);
            } else resource = infModel.createResource(subject);
        }
        Property property = null;
        if (predicate != null) {
            if (predicate.indexOf("#") == -1) {
                String ns = predicate.substring(0, predicate.indexOf("_"));
                String value = predicate.substring(predicate.indexOf("_") + 1);
                predicate = nsl.getNameSpace(ns) + value;
            }
            property = infModel.createProperty(predicate);
        }
        Resource node = null;
        if (object != null) {
            if (object.indexOf("-") != -1 && object.indexOf(":") != -1) {
                node = infModel.createResource(new AnonId(object));
            } else if (object.indexOf("#") == -1) {
                String ns = object.substring(0, object.indexOf("_"));
                String value = object.substring(object.indexOf("_") + 1);
                object = nsl.getNameSpace(ns) + value;
                node = infModel.createResource(object);
            } else node = infModel.createResource(object);
        }
        Iterator stmtIt = infModel.listStatements((Resource) resource, (Property) property, (Resource) node);
        ArrayList results = new ArrayList();
        for (; stmtIt.hasNext(); ) {
            Statement st = (Statement) stmtIt.next();
            if (ns_mapping) {
                if (subject == null && !st.getSubject().isAnon() && st.getSubject() instanceof Resource) sub = nsl.getAbbreviation(st.getSubject().getNameSpace()) + st.getSubject().getLocalName(); else if (st.getSubject().isAnon()) sub = ((Resource) st.getSubject()).getId().toString(); else if (st.getSubject() instanceof Resource) sub = nsl.getAbbreviation(st.getSubject().getNameSpace()) + st.getSubject().getLocalName(); else sub = st.getSubject().toString();
                if (predicate == null) pred = nsl.getAbbreviation(st.getPredicate().getNameSpace()) + st.getPredicate().getLocalName();
                if ((object == null) && (!st.getObject().isAnon()) && (st.getObject() instanceof Resource)) obj = nsl.getAbbreviation(((Resource) st.getObject()).getNameSpace()) + ((Resource) st.getObject()).getLocalName(); else if (st.getObject().isAnon()) obj = ((Resource) st.getObject()).getId().toString(); else if (st.getObject() instanceof Resource) obj = nsl.getAbbreviation(((Resource) st.getObject()).getNameSpace()) + ((Resource) st.getObject()).getLocalName(); else obj = st.getObject().toString();
            } else {
                sub = st.getSubject().toString();
                pred = st.getPredicate().toString();
                obj = st.getObject().toString();
            }
            ArrayList triple = new ArrayList();
            triple.add(sub);
            triple.add(pred);
            triple.add(obj);
            results.add(triple);
        }
        return results;
    }

    /**
	 * Returns the reasoner
	 * @return Reasoner
	 */
    public Reasoner getReasoner() {
        return reasoner;
    }

    /**
	 * Set the reasoner
	 * @param reasoner
	 */
    public void setReasoner(Reasoner reasoner) {
        this.reasoner = reasoner;
    }

    /**
	 * Returns the converter
	 * @return Converter
	 */
    public Converter getConverter() {
        return converter;
    }

    /**
	 * Set the converter
	 * @param converter Converter
	 */
    public void setConverter(Converter converter) {
        this.converter = converter;
    }

    /**
	 * <p>Converts a RDF/RDFS/OWL file to a prova model.
	 * <p>It uses the reasoner to infer the model and the converter
	 * to translate the inferred model to a prova model.
	 * @param inputFile RDF file
	 * @param reasoner Reasoner
	 * @param converter Converter
	 * @return ProvaModel prova model
	 */
    public ProvaModel convert(String inputFile, Reasoner reasoner, Converter converter) throws Exception {
        Converter old_conv = this.getConverter();
        Reasoner old_reasoner = this.getReasoner();
        this.setConverter(converter);
        this.setReasoner(reasoner);
        ProvaModel provaModel = convert(inputFile);
        this.setConverter(old_conv);
        this.setReasoner(old_reasoner);
        return provaModel;
    }

    /**
	 * Converts a RDF/RDFS/OWL file to a prova model.
	 * <p>It uses the default reasoner to infer the model and the default converter
	 * to translate the inferred model to a prova model.
	 * @param inputFile RDF file
	 * @return ProvaModel a prova model
	 * @throws Exception
	 */
    public ProvaModel convert(String inputFile) throws Exception {
        Model defaultModel = ModelFactory.createDefaultModel();
        try {
            InputStream in = new FileInputStream(inputFile);
            defaultModel.read(in, null);
        } catch (Exception ex) {
            System.err.println(ex);
        }
        Model jenaModel;
        if (reasoner != null) jenaModel = ModelFactory.createInfModel(reasoner, defaultModel); else jenaModel = defaultModel;
        return convert(jenaModel);
    }

    public ProvaModel convert(Model infModel) throws Exception {
        ProvaModel nsModel = this.NStoProva(infModel);
        ProvaModel provaModel = converter.translate(infModel);
        ProvaModel resultModel = (ProvaModel) nsModel.append("% ---------------------------------------------------------------------\r\n" + "% Ontology data\r\n" + "%---------------------------------------------------------------------\r\n\r\n");
        resultModel.append(provaModel.toString());
        return resultModel;
    }

    public ProvaModel update(Model infModel) throws Exception {
        ProvaModel provaModel = converter.update(infModel);
        return provaModel;
    }

    /**
	 * Convert a rdf/rdfs/owl/daml file to a prova script. 
	 * <p>The reasoner and converter are used.
	 * @param inputFile 
	 * @param outputFile prova script
	 * @throws Exception of input/output
	 */
    public void run(String inputFile, String outputFile) throws Exception {
        ProvaModel provaModel = new ProvaModel();
        provaModel = this.convert(inputFile);
        provaModel.writeModel(outputFile);
        provaModel.close();
    }

    /**
	 * Update.
	 * @param inputFile
	 * @param outputFile update prova script
	 * @throws Exception
	 */
    public void update(String inputFile, String outputFile) throws Exception {
        Model defaultModel = ModelFactory.createDefaultModel();
        InputStream in = new FileInputStream(inputFile);
        defaultModel.read(in, "");
        Model jenaModel;
        if (reasoner != null) jenaModel = ModelFactory.createInfModel(null, defaultModel); else jenaModel = defaultModel;
        ProvaModel provaModel = this.update(jenaModel);
        provaModel.writeModel(outputFile);
        provaModel.close();
    }

    /**
	 * Translate the namespaces to prova facts.
	 * @param model
	 * @return
	 */
    public ProvaModel NStoProva(Model model) {
        ProvaModel pmodel = new ProvaModel();
        NameSpaceList ns = new NameSpaceList(model);
        Map nsmap = ns.getNameSpaces();
        pmodel.append("% ---------------------------------------------------------------------\r\n" + "% Namespaces\r\n" + "%---------------------------------------------------------------------\r\n\r\n");
        for (Iterator it = nsmap.keySet().iterator(); it.hasNext(); ) {
            Object abrev = it.next();
            Object uri = nsmap.get(abrev);
            if (abrev.toString().equals("")) abrev = "default";
            pmodel.append("ns(" + abrev + ",\"" + uri + "\").\r\n");
        }
        pmodel.append("\r\n\r\n");
        return pmodel;
    }

    /**
	 * Get the DL type of the term
	 * Return null if the term has no DL type
	 */
    public static String getDLType(Term t) {
        try {
            String term = t.toString();
            if (term.indexOf(":") == -1) if (t.getType() == Object.class || t.getType() == ws.prova.RList.class) return "rdfs_Resource"; else return null; else if (t.getType() == String.class || t.getType() == Object.class) {
                if (t instanceof VariableTermImpl) {
                    return term.substring(term.lastIndexOf(":") + 1, term.length() - 1);
                } else if (t instanceof ConstantTermImpl) {
                    return term.substring(1, term.indexOf(":"));
                } else if (t instanceof ComplexTermImpl) {
                    return null;
                } else return null;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
	 * True / False if term has / has not a DL type
	 */
    public static boolean hasDLType(Term term) {
        try {
            if (term.getType() == java.lang.String.class) {
                if (term.toString().indexOf(":") != -1) return true;
            }
            if (term.getType() == Object.class) return true;
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
