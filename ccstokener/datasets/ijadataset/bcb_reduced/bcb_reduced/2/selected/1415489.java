package net.sourceforge.fluxion.pussycat.manager;

import net.sourceforge.fluxion.pussycat.entity.RenderableInPussycat;
import net.sourceforge.fluxion.pussycat.svg.ConceptGlyph;
import net.sourceforge.fluxion.pussycat.util.PussycatResourceHistory;
import net.sourceforge.fluxion.pussycat.util.PussycatOntologySearcher;
import net.sourceforge.fluxion.pussycat.util.PussycatUtils;
import net.sourceforge.fluxion.pussycat.util.exceptions.PussycatException;
import net.sourceforge.fluxion.pussycat.util.ReasonerProgressMonitor;
import net.sourceforge.fluxion.pussycat.util.cache.OWLEntityCache;
import org.mindswap.pellet.exceptions.InconsistentOntologyException;
import org.mindswap.pellet.owlapi.Reasoner;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.utils.progress.ProgressMonitor;
import org.mindswap.pellet.ABox;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.taxonomy.Taxonomy;
import org.mindswap.pellet.utils.progress.ConsoleProgressMonitor;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.impl.Log4JLogger;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import net.sourceforge.fluxion.spi.SPIManager;
import net.sourceforge.fluxion.api.FluxionService;

public class PussycatSessionManager {

    OWLOntologyManager om = null;

    ReasonerProgressMonitor monitor = new ReasonerProgressMonitor(300000);

    OWLOntology ao = null;

    OWLOntology lastAddedOntology = null;

    OWLOntology activeOntology = null;

    OWLOntology pussycatAnnotations = null;

    private Set<FluxionService> fluxionServices;

    Reasoner reasoner = null;

    Hashtable<URI, OWLOntology> uploadedOntologies = new Hashtable<URI, OWLOntology>();

    Hashtable<URI, OWLOntology> ontologies = new Hashtable<URI, OWLOntology>();

    Hashtable<String, RenderableInPussycat> renderables = new Hashtable<String, RenderableInPussycat>();

    Hashtable<String, ConceptGlyph> conceptglyphs = new Hashtable<String, ConceptGlyph>();

    PussycatEntityCacheManager entityCacheManager;

    PussycatOntologySearcher searcher;

    PussycatResourceHistory resourceHistory;

    String userLevel = "developer";

    String httpProxy = "";

    private int httpProxyPort = -1;

    private boolean autoReasoning = true;

    private boolean autoCreateBrowserTab = false;

    private boolean searchPreCaching = true;

    private boolean reasonerListenForOntologyChanges = true;

    private String httpNonProxyHosts = "";

    private SPIManager spiManager;

    private final String sessionId;

    private boolean autoFullEntityCaching;

    public PussycatSessionManager(String sessionId) throws PussycatException {
        this(sessionId, OWLManager.createOWLOntologyManager());
    }

    public PussycatSessionManager(String sessionId, OWLOntologyManager om) throws PussycatException {
        this.sessionId = sessionId;
        this.om = om;
        this.reasoner = new Reasoner(om);
        setPelletLogLevel(Level.INFO);
        PelletOptions.USE_TRACING = true;
        if (this.reasonerListenForOntologyChanges) {
            this.reasoner.getManager().addOntologyChangeListener(this.reasoner);
        }
        this.resourceHistory = new PussycatResourceHistory();
        this.searcher = new PussycatOntologySearcher();
        this.entityCacheManager = new PussycatEntityCacheManager(this);
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public Set<FluxionService> getFluxionServices() {
        return this.fluxionServices;
    }

    public void setFluxionServices(Set<FluxionService> fluxionServices) {
        this.fluxionServices = fluxionServices;
    }

    public OWLOntologyManager getOntologyManager() {
        return this.om;
    }

    public void setOntologyManager(OWLOntologyManager om) {
        this.om = om;
        this.ontologies.clear();
        this.conceptglyphs.clear();
        this.reasoner = new Reasoner(om);
        this.resourceHistory.getHistory().clear();
        this.searcher.destroy();
        this.entityCacheManager = new PussycatEntityCacheManager(this);
    }

    public PussycatEntityCacheManager getEntityCacheManager() {
        return entityCacheManager;
    }

    public void setProgressMonitor(ReasonerProgressMonitor monitor) {
        this.monitor = monitor;
    }

    public ReasonerProgressMonitor getProgressMonitor() {
        return this.monitor;
    }

    public PussycatOntologySearcher getOntologySearcher() {
        return this.searcher;
    }

    public void setApplicationOntology(OWLOntology ontology) {
        this.ao = ontology;
    }

    public OWLOntology getApplicationOntology() {
        return this.ao;
    }

    public OWLOntology loadOntology(URI physURI) throws OWLException {
        return reasoner.getManager().loadOntology(physURI);
    }

    public void addOntology(URI physURI) throws OWLException, PussycatException {
        if (this.ontologies.containsKey(physURI)) {
            setActiveOntology(physURI);
        } else {
            addOntology(physURI, loadOntology(physURI));
        }
    }

    public void addOntology(URI physURI, OWLOntology ontology) throws OWLException, PussycatException {
        this.lastAddedOntology = ontology;
        if (this.ontologies.isEmpty()) {
            this.ontologies.put(physURI, ontology);
            setActiveOntology(physURI);
        } else {
            this.ontologies.put(physURI, ontology);
        }
    }

    public void setActiveOntology(URI physURI) throws OWLException, PussycatException {
        if (this.ontologies.containsKey(physURI)) {
            setActiveOntology(this.ontologies.get(physURI));
        } else {
            throw new PussycatException("Requested ontology does not exist in the session manager.  Please upload the ontology or select a service datasource.");
        }
    }

    public void setActiveOntology(OWLOntology ontology) throws OWLException, PussycatException {
        URI ontUri = ontology.getURI();
        if (this.ontologies.contains(ontology)) {
            PelletOptions.REALIZE_INDIVIDUAL_AT_A_TIME = true;
            monitor.reset();
            if (isAutoReasoning()) {
                classify(ontology);
                if (!monitor.isCanceled()) {
                    realise(ontology);
                }
            }
            if (!entityCacheManager.containsKey(ontUri)) {
                OWLEntityCache oec = new OWLEntityCache();
                if (isAutoFullEntityCaching()) {
                    oec.setReasoner(this.reasoner);
                }
                entityCacheManager.put(ontUri, oec);
            }
            if (!monitor.isCanceled()) {
                this.activeOntology = ontology;
                if (isSearchPreCaching()) {
                    searcher.precache(this.activeOntology);
                }
            } else {
                System.out.println("-----------------\nProcess cancelled\n----------------");
            }
        } else {
            throw new PussycatException("Requested ontology does not exist in the session manager.  Please upload the ontology or select a service datasource.");
        }
    }

    public OWLOntology getActiveOntology() {
        return this.activeOntology;
    }

    public void unloadOntology(URI physURI) {
        if (this.ontologies.containsKey(physURI)) {
            this.reasoner.unloadOntology(this.ontologies.get(physURI));
        }
    }

    public void unloadAllOntologies() {
        this.resourceHistory.getHistory().clear();
        if (!reasoner.getLoadedOntologies().isEmpty()) {
            this.reasoner.unloadOntologies(reasoner.getLoadedOntologies());
        }
    }

    public void removeOntology(URI physURI) {
        if (this.ontologies.containsKey(physURI)) {
            this.reasoner.unloadOntology(this.ontologies.get(physURI));
            this.ontologies.remove(physURI);
            searcher.removeCachedEntities(physURI);
            this.entityCacheManager.remove(physURI);
        }
    }

    public void removeAllOntologies() {
        this.ontologies.clear();
        this.resourceHistory.getHistory().clear();
        searcher.destroy();
        this.entityCacheManager.clear();
        if (!reasoner.getLoadedOntologies().isEmpty()) {
            this.reasoner.unloadOntologies(reasoner.getLoadedOntologies());
        }
    }

    public Hashtable<URI, OWLOntology> getOntologies() {
        return this.ontologies;
    }

    public Set<OWLOntology> getOntologySet() {
        HashSet<OWLOntology> os = new HashSet<OWLOntology>();
        for (URI key : ontologies.keySet()) {
            os.add(ontologies.get(key));
        }
        return os;
    }

    public PussycatResourceHistory getResourceHistory() {
        return this.resourceHistory;
    }

    public void classify(OWLOntology ontology) throws OWLException, InconsistentOntologyException {
        if (!reasoner.getLoadedOntologies().contains(ontology)) {
            HashSet<OWLOntology> o = new HashSet<OWLOntology>();
            o.add(ontology);
            this.reasoner.loadOntologies(o);
        }
        if (monitor == null) {
            monitor = new ReasonerProgressMonitor(300000);
        }
        reasoner.getKB().getTaxonomyBuilder().setProgressMonitor(monitor);
        reasoner.getKB().classify();
        System.out.println();
        System.out.println("Classification successful: " + reasoner.getKB().isClassified());
        System.out.println();
    }

    public void realise(OWLOntology ontology) throws OWLException, OWLReasonerException {
        if (!reasoner.getLoadedOntologies().contains(ontology)) {
            HashSet<OWLOntology> o = new HashSet<OWLOntology>();
            o.add(ontology);
            this.reasoner.loadOntologies(o);
        }
        if (monitor == null) {
            monitor = new ReasonerProgressMonitor(300000);
        }
        if (!reasoner.getKB().isClassified()) {
            System.out.print("PussycatSessionManager:: " + ontology + " not classified.  Classifying...");
            classify(ontology);
        }
        reasoner.getKB().getTaxonomyBuilder().setProgressMonitor(monitor);
        reasoner.getKB().realize();
        System.out.println();
        System.out.println("Realisation successful: " + reasoner.getKB().isRealized());
        System.out.println();
    }

    public OWLOntology getLastAddedOntology() {
        return this.lastAddedOntology;
    }

    public Reasoner getReasoner() {
        return this.reasoner;
    }

    public void setReasoner(Reasoner reasoner) {
        this.reasoner = reasoner;
    }

    public Hashtable getConceptGlyphs() {
        return this.conceptglyphs;
    }

    public boolean isActive(OWLOntology ontology) {
        return this.activeOntology == ontology;
    }

    public void setUserLevel(String level) throws PussycatException {
        if (!level.equals("user") || !level.equals("power") || !level.equals("developer")) {
            throw new PussycatException("Cannot set user level - invalid user level \"" + level + "\" specified");
        } else {
            this.userLevel = level;
        }
    }

    public String getUserLevel() {
        return this.userLevel;
    }

    public void setAutoCreateBrowserTab(boolean auto) {
        this.autoCreateBrowserTab = auto;
    }

    public boolean isAutoCreateBrowserTab() {
        return this.autoCreateBrowserTab;
    }

    public void setAutoReasoning(boolean auto) {
        this.autoReasoning = auto;
    }

    public boolean isAutoReasoning() {
        return this.autoReasoning;
    }

    public void setAutoFullEntityCaching(boolean auto) {
        this.autoFullEntityCaching = auto;
    }

    public boolean isAutoFullEntityCaching() {
        return this.autoFullEntityCaching;
    }

    public void setSearchPreCaching(boolean precache) {
        this.searchPreCaching = precache;
    }

    public boolean isSearchPreCaching() {
        return this.searchPreCaching;
    }

    public void setReasonerListenForOntologyChanges(boolean listen) {
        this.reasonerListenForOntologyChanges = listen;
    }

    public boolean isReasonerListenForOntologyChanges() {
        return this.reasonerListenForOntologyChanges;
    }

    public void setPelletLogLevel(Level level) {
        ABox.log.setLevel(level);
        Taxonomy.log.setLevel(level);
        KnowledgeBase.log.setLevel(level);
    }

    public void setSessionProxy(String proxyHost, int proxyPort, String nonProxyHosts) {
        this.httpProxy = proxyHost;
        this.httpProxyPort = proxyPort;
        this.httpNonProxyHosts = nonProxyHosts;
    }

    public HttpURLConnection proxiedURLConnection(URL url, String serverName) throws IOException, PussycatException {
        if (this.httpProxy == null || this.httpProxy.equals("") || PussycatUtils.isLocalURL(url.toString()) || url.toString().contains(serverName)) {
            System.getProperties().put("proxySet", "false");
        } else {
            System.getProperties().put("proxySet", "true");
        }
        if (System.getProperties().getProperty("proxySet").equals("true")) {
            return (java.net.HttpURLConnection) url.openConnection(new java.net.Proxy(java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress(this.httpProxy, this.httpProxyPort)));
        } else {
            return (java.net.HttpURLConnection) url.openConnection();
        }
    }

    public void setSpiManager(SPIManager spiManager) {
        this.spiManager = spiManager;
    }

    public SPIManager getSpiManager() {
        return this.spiManager;
    }
}
