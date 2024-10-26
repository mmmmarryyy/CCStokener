package org.ethontos.owlwatcher.project;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.coode.owlapi.rdf.model.RDFLiteralNode;
import org.coode.owlapi.rdf.model.RDFNode;
import org.coode.owlapi.rdf.model.RDFResourceNode;
import org.coode.owlapi.rdf.model.RDFTriple;
import org.coode.xml.OWLOntologyXMLNamespaceManager;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyCreationIOException;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyDocumentAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.semanticweb.owlapi.model.UnloadableImportException;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.OWLReasonerRuntimeException;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.ethontos.owlwatcher.exceptions.OntologyNotFoundException;
import org.ethontos.owlwatcher.project.rdf.ProjectModel;
import org.ethontos.owlwatcher.project.rdf.ProjectModelFactory;
import org.ethontos.owlwatcher.project.rdf.projectRenderer.rdfProject.RDFProjectRenderer;
import org.ethontos.owlwatcher.utils.CacheManager;
import org.ethontos.owlwatcher.utils.OWOntologyFile;
import org.ethontos.owlwatcher.view.AbstractView;
import org.ethontos.owlwatcher.view.TabView;
import org.ethontos.owlwatcher.view.ViewerLib.DataViewer;

/**
 * This holds all the other structures (targetOntology. ProjectModel, etc.) and provides a set of utility functions to reduce
 * the need to access the other structures directly.  Ideally most or all of the semantic functionality should be exposed to
 * the UI here.
 * @author peter
 * created 2006
 *
 */
public class Project extends Observable {

    private static int EVENTCACHESIZE = 1000;

    public static enum OntType {

        target, upper, behavior, anatomy, taxonomy
    }

    ;

    private final IRI XSD_DOUBLE_IRI = IRI.create("http://www.w3.org/2001/XMLSchema/double");

    private final String OVERWRITEPROJECTTITLE = "Overwrite Project";

    public OWLDataProperty owTimeStampProperty;

    public OWLObjectProperty owSupportingResourceProperty;

    public final Set<URI> projectPropertyURIs = new HashSet<URI>();

    private OWLOntologyManager projectManager;

    private OWLDataFactory projectFactory;

    private OWLReasonerFactory projectReasonerFactory;

    private OWLReasoner displayReasoner;

    private OWLReasoner outboardReasoner;

    private OWLClass[] nodeHits;

    private long[] hitTimes;

    private DataViewer[] hitSources;

    private int eventCount;

    private ProjectModel model;

    private OWOntology targetOntology;

    private URI sourceURI = null;

    private HashSet<OWLOntology> visibleOntologies = null;

    private RDFResourceNode projectResource;

    private RDFResourceNode ontProperty = null;

    private RDFResourceNode annotatesDataProperty = null;

    private RDFResourceNode nameProperty = null;

    private RDFResourceNode targetOntologyProperty = null;

    private RDFResourceNode localFileProperty = null;

    private final RDFTriple[] tripleArray = new RDFTriple[2];

    static Logger logger = Logger.getLogger(Project.class.getName());

    /**
     * 
     * Constructor for projects creates the manager and datafactory from the OWLAPI, a couple of
     * reasoners (more later...) and starts filling a table of property URI's
     */
    public Project() {
        super();
        logger.setLevel(Level.DEBUG);
        projectManager = OWLManager.createOWLOntologyManager();
        projectFactory = projectManager.getOWLDataFactory();
        owTimeStampProperty = projectFactory.getOWLDataProperty(OWVocabulary.owTimeStampIRI);
        owSupportingResourceProperty = projectFactory.getOWLObjectProperty(OWVocabulary.owSupportingResourceIRI);
        projectReasonerFactory = new StructuralReasonerFactory();
        projectPropertyURIs.add(OWVocabulary.owNameURI);
        projectPropertyURIs.add(OWVocabulary.owTargetOntologyURI);
        projectPropertyURIs.add(OWVocabulary.owLoadsOntologyURI);
        projectPropertyURIs.add(OWVocabulary.owAnnotatesDataURI);
        projectPropertyURIs.add(OWVocabulary.owOntologyDomainURI);
        projectPropertyURIs.add(OWVocabulary.owSelectedResourceURI);
        projectPropertyURIs.add(OWVocabulary.owLocalFileURI);
    }

    /**
     * Initializes a new project model and defines the most commonly used properties from
     * the string constants defined above.
     *
     */
    private void setPropertiesFromModel() {
        if (model == null) model = ProjectModelFactory.createProjectModel();
        model.addPrefixNamespaceMapping(OWVocabulary.rdfNSabbrev, OWVocabulary.rdfSyntaxNSString);
        model.addPrefixNamespaceMapping(OWVocabulary.owProjectNSabbrev, OWVocabulary.owProjectPrefix);
        model.addPrefixNamespaceMapping(OWVocabulary.owResourceAbbrev, OWVocabulary.owResourcePrefix);
        ontProperty = model.createProperty(OWVocabulary.owLoadsOntologyURI);
        annotatesDataProperty = model.createProperty(OWVocabulary.owAnnotatesDataURI);
        nameProperty = model.createProperty(OWVocabulary.owNameURI);
        targetOntologyProperty = model.createProperty(OWVocabulary.owTargetOntologyURI);
        localFileProperty = model.createProperty(OWVocabulary.owLocalFileURI);
    }

    /**
     * for testing
     */
    OWLDataFactory getProjectFactory() {
        return projectFactory;
    }

    /**
     * 
     * Resets and initializes, if necessary, arrays holding event information until it is
     * safe to run them through a reasoner.
     */
    public void initEventCache() {
        if (nodeHits == null || nodeHits.length != EVENTCACHESIZE) {
            nodeHits = new OWLClass[EVENTCACHESIZE];
            hitTimes = new long[EVENTCACHESIZE];
            hitSources = new DataViewer[EVENTCACHESIZE];
        }
        eventCount = 0;
    }

    int getEventCount() {
        return eventCount;
    }

    /**
     * This method should be called to determine whether a project is sufficiently defined to
     * allow observation to begin.
     * @return true if there is a target Ontology, a configured project model and an appropriate resource (currently a video)
     */
    public boolean projectValid() {
        if (targetOntology == null) return false;
        if (model.matchTriples(projectResource, ontProperty, null).isEmpty()) return false;
        if (getVideoResources().isEmpty()) return false;
        return true;
    }

    /**
     * Consuming from this may trigger UI updates and reasoner calls, so shouldn't be called
     * in real time or with video playing. This should probably be done with an ArrayBlockingQueue.
     * Reasoning doesn't happen on its own thread yet, but eventually it should.
     * @param eventType class that specifies the type of event being recorded
     * @param when raw time value returned by the data source.
     */
    public synchronized void addEvent(final OWLClass eventType, final long when, final DataViewer source) {
        nodeHits[eventCount] = eventType;
        hitTimes[eventCount] = when;
        hitSources[eventCount++] = source;
    }

    /**
     * 
     * As usual for the OWLAPI, this creates the subclass, then an axiom specifying the relationship,
     * then a change object that wraps the axiom, which the manager then processes.
     * @param parent class that will be (a) parent of the subclass
     * @param name specifies the URI of the subclass
     * @return an OWLClass which has URI name and the specified parent (not necessarily new?)
     */
    public OWLClass addSubClass(final OWLClass parent, final String name) {
        final OWLClass child = createTargetClass(name);
        if (child != null) {
            OWLAxiom axiom = projectFactory.getOWLSubClassOfAxiom(child, parent);
            AddAxiom addAxiom = new AddAxiom(targetOntology.getModel(), axiom);
            try {
                projectManager.applyChange(addAxiom);
            } catch (OWLOntologyChangeException e) {
                e.printStackTrace();
            }
        }
        return child;
    }

    /**
     * Need to bullet-proof this...
     */
    private OWLClass createTargetClass(final String name) {
        final IRI newName = IRI.create(targetOntology.getBaseURI() + "#" + name);
        return projectFactory.getOWLClass(newName);
    }

    /**
     * 
     * @param o the ontology that the individual is to be removed from
     * @param victim the class to remove
     * @return true if the class was found in target ontology
     */
    public boolean deleteClass(final OWOntology o, final OWLClass victim) {
        if (o.equals(targetOntology)) {
            OWLEntityRemover remover = new OWLEntityRemover(projectManager, Collections.singleton(targetOntology.getModel()));
            victim.accept(remover);
            try {
                projectManager.applyChanges(remover.getChanges());
            } catch (OWLOntologyChangeException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            logger.error("Tried to delete a class from an imported ontology - should have been caught sooner");
            return false;
        }
    }

    /**
     * Wrapper that supplies the current targetOntology.  Can't delete from anywhere else.
     * @param victim
     * @return boolean returned by wrapped two argument method
     */
    public boolean deleteClass(final OWLClass victim) {
        return deleteClass(targetOntology, victim);
    }

    /**
     * 
     * @param o the ontology that the individual is to be removed from
     * @param victim the class to remove
     * @return true if the class was found in target ontology
     */
    public boolean deleteIndividual(final OWOntology o, final OWLNamedIndividual victim) {
        if (o.equals(targetOntology)) {
            OWLEntityRemover remover = new OWLEntityRemover(projectManager, Collections.singleton(o.getModel()));
            victim.accept(remover);
            try {
                projectManager.applyChanges(remover.getChanges());
            } catch (OWLOntologyChangeException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            logger.error("Tried to delete an individual from an imported ontology - should have been caught sooner");
            return false;
        }
    }

    /**
     * Wrapper that supplies the current targetOntology.  Can't delete from anywhere else.
     * @param victim
     * @return true if deletion was successful
     */
    public boolean deleteIndividual(final OWLNamedIndividual victim) {
        return deleteIndividual(targetOntology, victim);
    }

    /**
     * This method takes events off the cache and adds them to the ontology model.  
     * @see addEvent
     */
    public void processEventCache() {
        final String base = targetOntology.getBaseURI() + "#";
        final OWLOntology targetModel = targetOntology.getModel();
        try {
            Set<OWLAxiom> allAxioms = new HashSet<OWLAxiom>();
            for (int i = 0; i < eventCount; i++) {
                final OWLClass myClass = nodeHits[i];
                final String baseName = base + myClass.getIRI().getFragment();
                final double timeStamp = hitTimes[i] / (1.0d * hitSources[i].getTimeScaleDivisor());
                final IRI support = hitSources[i].currentView();
                final String timeString = Double.toString(timeStamp);
                IRI proposedIRI = IRI.create(baseName + timeString);
                if (targetModel.containsIndividualInSignature(proposedIRI)) {
                    int count = 1;
                    do {
                        proposedIRI = IRI.create(baseName + timeString + "_" + Integer.toString(count++));
                    } while (targetModel.containsIndividualInSignature(proposedIRI));
                }
                OWLIndividual eventI = projectFactory.getOWLNamedIndividual(proposedIRI);
                OWLLiteral eventTime = projectFactory.getOWLLiteral(timeStamp);
                allAxioms.add(projectFactory.getOWLClassAssertionAxiom(myClass, eventI));
                allAxioms.add(projectFactory.getOWLDataPropertyAssertionAxiom(owTimeStampProperty, eventI, eventTime));
                OWLIndividual sourceIndividual = projectFactory.getOWLNamedIndividual(support);
                allAxioms.add(projectFactory.getOWLObjectPropertyAssertionAxiom(owSupportingResourceProperty, eventI, sourceIndividual));
            }
            projectManager.addAxioms(targetModel, allAxioms);
        } catch (OWLOntologyChangeException e) {
            e.printStackTrace();
        }
        eventCount = 0;
    }

    /**
     * In case the default name is too ugly
     */
    public String displayName(Project.OntType ot) {
        return ot.toString();
    }

    /**
     * Convenience method for the IRI-based method version following
     * @param o the wrapper for the ontology to add
     * @param uri the uri to associate with the added ontology
     */
    public void addImport(final OWOntology o, final URI uri) {
        addImport(o, IRI.create(uri));
    }

    /**
     * This actually adds an import declaration; the importing happens somewhere else
     * @param o the wrapper for the ontology to add
     * @param iri the IRI to associate with the added ontology
     */
    public void addImport(final OWOntology o, final IRI iri) {
        OWLImportsDeclaration dcl = projectFactory.getOWLImportsDeclaration(iri);
        final AddImport importAdd = new AddImport(o.getModel(), dcl);
        try {
            projectManager.applyChange(importAdd);
        } catch (OWLOntologyChangeException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param ont OWLOntology to test
     * @return true if the ontology is marked as loaded in the model (which might not mean successfully loaded?)
     */
    public boolean isLoaded(final OWLOntology ont) {
        RDFNode ontNode = model.createResource(ont.getOntologyID().getOntologyIRI());
        RDFNode loadsOntProperty = model.createResource(OWVocabulary.owLoadsOntologyURI);
        Set<RDFTriple> query = model.matchTriples(projectResource, loadsOntProperty, ontNode);
        return !query.isEmpty();
    }

    /**
     * 
     * @param c OWClass to test
     * @return true if the class is from an imported ontology, not defined in this project.
     */
    public boolean isImported(final OWLClass c) {
        final Set<OWLOntology> importSet = projectManager.getImports(targetOntology.getModel());
        for (OWLOntology o : importSet) {
            if (o.containsClassInSignature(c.getIRI())) return true;
        }
        return false;
    }

    public boolean hasInstances(final OWLClass c) {
        return !targetOntology.getModel().getClassAssertionAxioms(c).isEmpty();
    }

    public Set<OWLIndividual> getInstances(final OWLClass c) {
        final Set<OWLClassAssertionAxiom> targets = targetOntology.getModel().getClassAssertionAxioms(c);
        final Set<OWLIndividual> result = new HashSet<OWLIndividual>();
        for (OWLClassAssertionAxiom a : targets) {
            OWLIndividual x = a.getIndividual();
            result.add(x);
        }
        return result;
    }

    /**
     * @return the (rdf-based) ontology representing the project
     */
    public ProjectModel getProjectModel() {
        return model;
    }

    /**
     * Add an Ontology to those available to supply terms
     * Note: This will (and ought to) fail if no network connection for remote ontologies
     * @param uriSpec
     * @param view a parent for any error dialogs
     */
    public void addOntology(final String uriSpec, TabView view) {
        assert (targetOntology != null);
        IRI iri = null;
        if (OWOntology.uriCheck(uriSpec)) {
            iri = IRI.create(uriSpec);
            if (iri != null) {
                CacheManager.getInstance().getCachedCopy(uriSpec);
                try {
                    final OWLOntology preRead = projectManager.loadOntologyFromOntologyDocument(iri);
                    final IRI ontIRI = preRead.getOntologyID().getOntologyIRI();
                    final RDFResourceNode targetResource = model.createResource(ontIRI);
                    model.addTriple(projectResource, ontProperty, targetResource);
                    setChanged();
                    notifyObservers();
                } catch (OWLOntologyCreationException e) {
                    view.genericError("Error in loading ontology from: " + iri.toString());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Add an Ontology to those available to supply terms
     * @param f
     * @param view
     */
    public void addOntology(final File f, final TabView view) {
        assert (targetOntology != null);
        if (OWOntology.fileCheck(f)) {
            try {
                final OWLOntology preRead = projectManager.loadOntologyFromOntologyDocument(f);
                final IRI ontIRI = preRead.getOntologyID().getOntologyIRI();
                final RDFResourceNode targetResource = model.createResource(ontIRI);
                model.addTriple(projectResource, ontProperty, targetResource);
                setChanged();
                notifyObservers();
            } catch (OWLOntologyCreationException e) {
                view.genericError("Error in loading ontology from: " + f.toString());
                e.printStackTrace();
            }
        } else if (OBOReader.fileCheck(f)) {
        }
    }

    /**
     * Records data source (movie, text file, etc.)
     * @param uriSpec specifies the url of the data source
     */
    public void addDataSource(final URI uri) {
        if (model == null) {
            model = ProjectModelFactory.createProjectModel();
            setPropertiesFromModel();
        }
        RDFResourceNode targetResource = model.createResource(uri);
        model.addTriple(projectResource, annotatesDataProperty, targetResource);
        setChanged();
        notifyObservers();
    }

    /**
     * Records data source (movie, text file, etc.)
     * 
     * @param f file containing the data source
     */
    public URI addDataSource(final File f, final URI fileURI) {
        addDataSource(fileURI);
        RDFResourceNode dataResource = model.getResource(fileURI);
        model.addTriple(dataResource, localFileProperty, model.createLiteralNode(f.getAbsolutePath()));
        return f.toURI();
    }

    public File getLocalFile(final URI fileURI) {
        RDFResourceNode dataResource = model.getResource(fileURI);
        Set<RDFTriple> query = model.matchTriples(dataResource, localFileProperty, null);
        if (query.isEmpty()) return null;
        for (RDFTriple t : query) {
            RDFNode ob = t.getObject();
            if (ob.isLiteral()) {
                String fstr = ((RDFLiteralNode) ob).getLiteral();
                File result = new File(fstr);
                return result;
            }
        }
        return null;
    }

    /**
     * viewers have names, now just need to figure out how to call this
     *
     */
    public void associateDataSourceWithViewer() {
    }

    public void addRawData() {
    }

    /**
     * This creates a new project and associates it with an rdf file.
     * @param projectFile the file to read; if null, a file called unNamed.rdf will be created
     * @param targetFile
     * @param view AbstractView (pane) that will own the confirm dialog, if any
     * @param projectURI String
     * @param targetURI String
     */
    public void newProject(final File projectFile, final File targetFile, final TabView view, final URI projectURI, final URI targetURI) {
        assert (projectURI != null);
        if (model != null) {
            if (view.queryUser("Overwrite loaded project?", OVERWRITEPROJECTTITLE) != AbstractView.QueryValues.YES) return;
            model.close();
            model = null;
            setChanged();
        }
        if (projectFile.exists()) {
            if (view.queryUser("Project file already exists; Delete and Overwrite", "Overwrite Project File?") == AbstractView.QueryValues.YES) {
                if (targetFile.exists()) {
                    if (view.queryUser("Delete and Overwrite Target Ontology file as well?", "Overwrite Ontology File?") == AbstractView.QueryValues.YES) {
                        targetFile.delete();
                    } else return;
                }
                projectFile.delete();
            }
        }
        if (targetFile.exists()) {
            if (view.queryUser("An ontology file with the same name as the project's ontology target already exists; Delete?", "Delete Unassociated Ontology") == AbstractView.QueryValues.YES) if (!targetFile.delete()) {
                view.genericError("Existing file (" + targetFile.toString() + ") could not be deleted.");
                return;
            } else return;
        }
        model = ProjectModelFactory.createProjectModel();
        setPropertiesFromModel();
        sourceURI = projectURI;
        setChanged();
        if (projectResource == null) projectResource = model.createResource(sourceURI);
        RDFResourceNode projectNode = model.setProjectURI(projectURI);
        model.setTargetURI(projectNode, targetURI);
        if (sourceURI == null) {
            model.close();
            model = null;
            setChanged();
        }
        targetOntology = new OWOntology(targetFile, IRI.create(targetURI), this, OntType.target, view);
        displayReasoner = projectReasonerFactory.createReasoner(targetOntology.getModel());
        outboardReasoner = projectReasonerFactory.createReasoner(targetOntology.getModel());
        notifyObservers();
    }

    /**
     * This loads a project from an rdf file.
     * @param f the file to read
     * @param view Tabview pane that will own the confirm dialog, if any
     */
    public void loadProject(final File f, final TabView view) {
        final URI modelURI = f.toURI();
        if (model != null) {
            if (view.queryUser("Overwrite loaded project?", OVERWRITEPROJECTTITLE) != AbstractView.QueryValues.YES) return;
            model.close();
            model = null;
            setChanged();
        }
        model = ProjectModelFactory.createProjectModel(modelURI, view);
        setPropertiesFromModel();
        sourceURI = modelURI;
        setChanged();
        projectResource = model.getResource(modelURI);
        if (projectResource == null) {
            final Set<RDFTriple> names = model.matchTriples(null, nameProperty, null);
            if (names.isEmpty()) {
                view.namelessProjectMessage();
                model.close();
                model = null;
                setChanged();
                notifyObservers();
                return;
            } else if (names.size() > 1) {
                view.duplicateNamesMessage();
            }
            RDFTriple[] tmp = new RDFTriple[1];
            names.toArray(tmp);
            projectResource = tmp[0].getSubject();
        }
        final Set<RDFTriple> targets = model.matchTriples(null, targetOntologyProperty, null);
        if (targets.isEmpty()) {
            view.namelessProjectMessage();
            model.close();
            model = null;
            setChanged();
            notifyObservers();
            return;
        } else if (targets.size() > 1) view.duplicateTargetsMessage();
        final RDFNode targetOntNode = targets.toArray(tripleArray)[0].getObject();
        if (targetOntNode instanceof RDFLiteralNode) {
            final String literal = ((RDFLiteralNode) targetOntNode).getLiteral();
            IRI litTest = IRI.create(literal);
            if (litTest != null) targetOntology = new OWOntology(f, litTest, this, Project.OntType.target, view); else logger.error("Null literal in creating target ontology");
        } else {
            targetOntology = new OWOntology(f, targetOntNode.getIRI(), this, Project.OntType.target, view);
        }
        if (targetOntology.getModel() == null) {
            view.ontologyDidntLoadMessage();
            model.close();
            model = null;
            setChanged();
            notifyObservers();
            return;
        }
        displayReasoner = new StructuralReasoner(targetOntology.getModel(), new SimpleConfiguration(), BufferingMode.BUFFERING);
        outboardReasoner = new StructuralReasoner(targetOntology.getModel(), new SimpleConfiguration(), BufferingMode.BUFFERING);
        Set<RDFTriple> ontSet = model.matchTriples(projectResource, ontProperty, null);
        Iterator<RDFTriple> iter = ontSet.iterator();
        boolean aborted = false;
        while (iter.hasNext() & !aborted) {
            try {
                RDFTriple t = iter.next();
                if (!importIsLoaded(targetOntology.getModel(), t.getObject())) targetOntology.loadImport(t.getObject().getIRI().toString());
            } catch (OntologyNotFoundException e) {
                if (view.queryUser("Imported Ontology not found; quit loading?", "Import Not Found") == AbstractView.QueryValues.YES) {
                    aborted = true;
                } else {
                }
            }
        }
        targetOntology.loadImports();
        setChanged();
        notifyObservers();
    }

    private boolean importIsLoaded(OWLOntology o, RDFNode ontSpec) {
        final Set<OWLOntology> imported = o.getImports();
        for (OWLOntology importOnt : imported) if (importOnt.getOntologyID().getOntologyIRI().equals(ontSpec.getIRI())) return true;
        return false;
    }

    /**
     * This loads a project file from an rdf file specified as a URL
     * @param u the url of the file
     * @param view the view (pane) that will own the confirm dialog, if any
     */
    public void loadProject(URL u, TabView view) {
        if (model != null) {
            if (view.queryUser("Overwrite loaded project?", OVERWRITEPROJECTTITLE) != AbstractView.QueryValues.YES) return;
            model.close();
            model = null;
        }
        URI modelURI;
        try {
            modelURI = u.toURI();
            model = ProjectModelFactory.createProjectModel(modelURI, view);
            setPropertiesFromModel();
            sourceURI = modelURI;
            projectResource = model.getResource(modelURI);
            if (projectResource == null) {
                if (nameProperty == null) nameProperty = model.createProperty(OWVocabulary.owNameURI);
                final Set<RDFTriple> names = model.matchTriples(null, nameProperty, null);
                if (names.isEmpty()) {
                    handleNamelessProject(view);
                    return;
                } else if (names.size() > 1) {
                    view.duplicateNamesMessage();
                }
                RDFTriple[] tmp = new RDFTriple[1];
                names.toArray(tmp);
                projectResource = tmp[0].getSubject();
            }
            final Set<RDFTriple> targets = model.matchTriples(null, targetOntologyProperty, null);
            if (targets.isEmpty()) {
                handleNamelessProject(view);
                return;
            } else if (targets.size() > 1) view.duplicateTargetsMessage();
            final RDFNode targetOntNode = targets.toArray(tripleArray)[0].getObject();
            if (targetOntNode instanceof RDFLiteralNode) {
                final String literal = ((RDFLiteralNode) targetOntNode).getLiteral();
                IRI litTest = IRI.create(literal);
                if (litTest != null) targetOntology = new OWOntology(litTest, this, Project.OntType.target, view); else logger.error("Null target ontology literal in loading project");
            } else {
                targetOntology = new OWOntology(targetOntNode.getIRI(), this, Project.OntType.target, view);
            }
            displayReasoner = new StructuralReasoner(targetOntology.getModel(), new SimpleConfiguration(), BufferingMode.BUFFERING);
            outboardReasoner = new StructuralReasoner(targetOntology.getModel(), new SimpleConfiguration(), BufferingMode.BUFFERING);
            Set<RDFTriple> ontSet = model.matchTriples(projectResource, ontProperty, null);
            Iterator<RDFTriple> iter = ontSet.iterator();
            boolean aborted = false;
            while (iter.hasNext() & !aborted) {
                try {
                    RDFTriple t = iter.next();
                    targetOntology.loadImport(t.getObject().getIRI().toString());
                } catch (OntologyNotFoundException e) {
                    if (view.queryUser("Imported Ontology not found; quit loading?", "Import Not Found") == AbstractView.QueryValues.YES) {
                        aborted = true;
                    } else {
                    }
                }
            }
            targetOntology.loadImports();
            setChanged();
            notifyObservers();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void handleNamelessProject(AbstractView view) {
        view.namelessProjectMessage();
        model.close();
        model = null;
        setChanged();
        notifyObservers();
    }

    /**
     * This saves the project to a file
     * @param view a pane that will serve as parent to any notification windows
     */
    public void saveProject(TabView view) {
        assert (model != null);
        assert (projectResource != null);
        logger.debug("Triple store when entering saveProject");
        model.dumpTriplesToConsole();
        model.setDefaultNamespaceFromProject();
        if (model == null) return;
        try {
            if (sourceURI != null) {
                saveProjectFile(sourceURI);
            }
            if (targetOntology != null) {
                processEventCache();
                final File targetFile = getTargetFile();
                final IRI fileIRI = IRI.create(targetFile);
                if (targetFile != null && fileIRI != null) {
                    projectManager.saveOntology(targetOntology.getModel(), new RDFXMLOntologyFormat(), fileIRI);
                } else projectManager.saveOntology(targetOntology.getModel(), new RDFXMLOntologyFormat());
            }
        } catch (UnknownOWLOntologyException e) {
            view.genericError(e.getMessage());
            e.printStackTrace();
        } catch (OWLOntologyStorageException e) {
            view.genericError(e.getMessage());
            e.printStackTrace();
        }
        logger.debug("Triple store when leaving saveProject");
        model.dumpTriplesToConsole();
    }

    private File getTargetFile() {
        if (targetOntology.fileAlias != null) return targetOntology.fileAlias;
        Set<RDFTriple> targetTriples = model.matchTriples(projectResource, targetOntologyProperty, null);
        if (!targetTriples.isEmpty()) {
            final RDFNode targetOntObject = targetTriples.toArray(tripleArray)[0].getObject();
            if (targetOntObject.isLiteral()) {
                String targetURLStr = ((RDFLiteralNode) targetOntObject).getLiteral();
                try {
                    URI targetURI = new URI(targetURLStr);
                    return new File(targetURI.getPath());
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        }
        return null;
    }

    /**
     * Note: this method should always return a copy since it might be passed to a potentially
     * misbehaving plug-in.  If the File comes from the target ontology, it needs to be copied,
     * otherwise, the File is already freshly created from an rdf statement.
     * @return file associated with the target (modified) ontology
     */
    public File getProjectTargetFile() {
        if (targetOntology.fileAlias != null) return getTargetFile().getAbsoluteFile();
        return getTargetFile();
    }

    /**
     * 
     * @param newProjectFile File specifying where to save the new copy of the project rdf
     * @param newOntologyFile File specifying where to save the new copy of the ontology
     * @param view TabView pane that will own any dialog boxes that appear
     * @param newProjectURI String containing the URI for the new project rdf
     * @param newTargetURI String containing the URI for the new ontology
     */
    public void saveProjectCopy(File newProjectFile, File newOntologyFile, TabView view, URI newProjectURI, URI newTargetURI) {
        assert (newProjectFile != null);
        assert (model != null);
        assert (projectResource != null);
        RDFResourceNode tmpProjectResource = model.pushProjectURI(newProjectURI, newTargetURI);
        model.setDefaultNamespace(newProjectURI.toString());
        try {
            saveProjectFile(newProjectURI);
            if (newOntologyFile != null) {
                model.pushTargetURI(tmpProjectResource, newTargetURI);
                processEventCache();
                IRI iri2 = IRI.create(newOntologyFile);
                RDFXMLOntologyFormat renameFormat = new RDFXMLOntologyFormat();
                projectManager.saveOntology(targetOntology.getModel(), renameFormat, iri2);
            }
        } catch (UnknownOWLOntologyException e) {
            view.genericError(e.getMessage());
            e.printStackTrace();
        } catch (OWLOntologyStorageException e) {
            view.genericError(e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                model.popTargetURI();
                model.popProjectURI();
                model.setDefaultNamespaceFromProject();
            } catch (Exception e) {
                view.genericError("Couldn't restore project name - internal error");
            }
        }
    }

    /**
     * 
     * @param newProjectFile File specifying where to save the new copy of the project rdf
     * @param newOntologyFile File specifying where to save the new copy of the ontology
     * @param view TabView pane that will own any dialog boxes that appear
     * @param newProjectURI String containing the URI for the new project rdf
     * @param newTargetURI String containing the URI for the new ontology
     */
    public void saveProjectAs(File newProjectFile, File newOntologyFile, TabView view, URI newProjectURI, URI newTargetURI) {
        assert (newProjectFile != null);
        assert (model != null);
        assert (projectResource != null);
        RDFResourceNode tmpProjectResource = model.pushProjectURI(newProjectURI, newTargetURI);
        model.setDefaultNamespace(newProjectURI.toString());
        try {
            saveProjectFile(newProjectURI);
            if (newOntologyFile != null) {
                model.pushTargetURI(tmpProjectResource, newTargetURI);
                processEventCache();
                IRI physicalIRI2 = IRI.create(newOntologyFile);
                OWLOntology newOntology = projectManager.createOntology(physicalIRI2);
                List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
                Set<OWLOntology> oldImports = targetOntology.getModel().getImports();
                for (OWLOntology impOnt : oldImports) {
                    OWLImportsDeclaration dcl = projectFactory.getOWLImportsDeclaration(impOnt.getOntologyID().getOntologyIRI());
                    changes.add(new AddImport(newOntology, dcl));
                }
                for (OWLAxiom ax : targetOntology.getModel().getAxioms()) changes.add(new AddAxiom(newOntology, ax));
                for (OWLAnnotation ann : targetOntology.getModel().getAnnotations()) {
                    changes.add(new AddOntologyAnnotation(newOntology, ann));
                }
                projectManager.applyChanges(changes);
                RDFXMLOntologyFormat renameFormat = new RDFXMLOntologyFormat();
                projectManager.saveOntology(newOntology, renameFormat, physicalIRI2);
            }
        } catch (UnknownOWLOntologyException e) {
            view.genericError(e.getMessage());
            e.printStackTrace();
        } catch (OWLOntologyStorageException e) {
            view.genericError(e.getMessage());
            e.printStackTrace();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        } catch (OWLOntologyChangeException e) {
            e.printStackTrace();
        } finally {
            try {
                model.popTargetURI();
                model.popProjectURI();
                model.setDefaultNamespaceFromProject();
            } catch (Exception e) {
                view.genericError("Couldn't restore project name - internal error");
            }
        }
    }

    protected void saveProjectFile(URI physicalURI) throws OWLOntologyStorageException {
        try {
            OutputStream os;
            if (!physicalURI.isAbsolute()) {
                throw new OWLOntologyStorageException("Physical URI must be absolute: " + physicalURI);
            }
            if (physicalURI.getScheme().equals("file")) {
                File file = new File(physicalURI);
                file.getParentFile().mkdirs();
                os = new FileOutputStream(file);
            } else {
                URL url = physicalURI.toURL();
                URLConnection conn = url.openConnection();
                os = conn.getOutputStream();
            }
            Writer w = new BufferedWriter(new OutputStreamWriter(os));
            RDFProjectRenderer renderer = new RDFProjectRenderer(this, model, w);
            renderer.render();
            w.close();
        } catch (IOException e) {
            throw new OWLOntologyStorageException(e);
        }
    }

    /**
     * 
     * @param ontURI - uri of the OWLOntology to create
     * @return an ontology loaded from the physical URI ontURI
     */
    OWLOntology loadOntologyFromIRI(IRI ontIRI, TabView view) {
        try {
            return projectManager.loadOntologyFromOntologyDocument(ontIRI);
        } catch (OWLOntologyAlreadyExistsException e) {
            e.printStackTrace();
            view.importDidntLoadMessage();
            return null;
        } catch (OWLOntologyDocumentAlreadyExistsException e) {
            e.printStackTrace();
            view.importDidntLoadMessage();
            return null;
        } catch (OWLOntologyCreationIOException e) {
            e.printStackTrace();
            view.importDidntLoadMessage();
            return null;
        } catch (UnloadableImportException e) {
            e.printStackTrace();
            view.importDidntLoadMessage();
            return null;
        } catch (UnparsableOntologyException e) {
            e.printStackTrace();
            view.importDidntLoadMessage();
            return null;
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            view.importDidntLoadMessage();
            return null;
        }
    }

    /**
     * 
     * @param u URI of the new ontology
     * @return a new ontology with URI u
     */
    OWLOntology createOntologyFromDocumentIRI(IRI i) {
        try {
            return projectManager.createOntology(i);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 
     * @param u URI of the new ontology
     * @return a new ontology with URI u
     */
    OWLOntology createOntologyFromPhysicalIRI(IRI u) {
        try {
            return projectManager.createOntology(u);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 
     * @param exporter
     */
    public void exportTarget(OWOntologyFile exporter, File exportFile, TabView view, String targetURI) {
        assert (targetOntology != null);
        processEventCache();
        exporter.export(exportFile, targetOntology, this, view, targetURI);
    }

    /**
     * 
     * @return name of the project as stored in the model (null if no model or resource)
     */
    public String getLongName() {
        if (model == null) return null;
        Iterator<RDFTriple> it = (model.matchTriples(null, nameProperty, null)).iterator();
        if (!it.hasNext()) {
            logger.warn("Project model has no resource representing itself");
            return null;
        }
        RDFTriple hit = it.next();
        projectResource = hit.getSubject();
        if (it.hasNext()) {
            logger.warn("More than one resource has a name property claiming to represent the project");
            logger.warn("Model dump follows:");
            model.dumpTriplesToConsole();
        }
        RDFNode tripleObject = hit.getObject();
        if (tripleObject.isLiteral()) {
            return ((RDFLiteralNode) tripleObject).getLiteral();
        } else return tripleObject.getIRI().toString();
    }

    /**
     * 
     * @return name of the project as stored in the model (null if no model or resource)
     */
    public String getTargetName() {
        if (model == null) return null;
        Iterator<RDFTriple> it = (model.matchTriples(null, targetOntologyProperty, null)).iterator();
        if (!it.hasNext()) {
            logger.warn("Project model has no resource representing its target");
            return null;
        }
        RDFTriple hit = it.next();
        projectResource = hit.getSubject();
        if (it.hasNext()) {
            logger.warn("More than one resource has a name property claiming to represent the project");
            logger.warn("Model dump follows:");
            model.dumpTriplesToConsole();
        }
        RDFNode tripleObject = hit.getObject();
        if (tripleObject.isLiteral()) {
            return ((RDFLiteralNode) tripleObject).getLiteral();
        } else return tripleObject.getIRI().toString();
    }

    /**
     * 
     * @return the uri's of each loaded ontology
     */
    public List<String> getLoadedOntologyNames() {
        List<String> ontNames = new ArrayList<String>();
        for (RDFTriple t : model.matchTriples(projectResource, ontProperty, null)) ontNames.add(t.getObject().getIRI().toString());
        return ontNames;
    }

    /**
     * 
     * @return the uri's of each loaded ontology
     */
    public List<IRI> getLoadedOntologies() {
        List<IRI> ontNames = new ArrayList<IRI>();
        for (RDFTriple t : model.matchTriples(projectResource, ontProperty, null)) ontNames.add(t.getObject().getIRI());
        return ontNames;
    }

    /**
     * 
     * @return the uri's (generally local files) of each loaded resource
     */
    public List<String> getLoadedResourceNames() {
        final List<String> resNames = new ArrayList<String>();
        if (model != null && projectResource != null & annotatesDataProperty != null) {
            final Set<RDFTriple> triples = model.matchTriples(projectResource, annotatesDataProperty, null);
            if (!triples.isEmpty()) for (RDFTriple t : triples) resNames.add(t.getObject().getIRI().toString());
        }
        return resNames;
    }

    /**
     * Returns a list of URIs of all resources
     * @return URIs of video resources (as of 0.04 - this means all project defined resources)
     */
    public List<IRI> getLoadedResources() {
        final List<IRI> resURIs = new ArrayList<IRI>();
        if (model != null && projectResource != null & annotatesDataProperty != null) {
            final Set<RDFTriple> triples = model.matchTriples(projectResource, annotatesDataProperty, null);
            if (!triples.isEmpty()) for (RDFTriple t : triples) resURIs.add(t.getObject().getIRI());
        }
        return resURIs;
    }

    /**
     * 
     * @param selectedVideo
     * @param timeScale
     */
    public void addResourceTimeScale(URI selectedResource, double timeScale) {
        final RDFResourceNode resourceNode = model.createResource(selectedResource);
        final RDFResourceNode timeScalePred = model.createProperty(OWVocabulary.owResourceTimeScaleURI);
        final Set<RDFTriple> matchResult = model.matchTriples(resourceNode, timeScalePred, null);
        System.out.println("before adding time scale triple");
        model.dumpTriplesToConsole();
        if (matchResult.isEmpty()) model.addTriple(resourceNode, timeScalePred, model.createLiteralNodeWithType(Double.toString(timeScale), XSD_DOUBLE_IRI));
        System.out.println("after adding time scale triple");
        model.dumpTriplesToConsole();
    }

    /**
     * This will change as other types of resources are added to projects
     * @return URIs of video resources (as of 0.04 - this means all project defined resources)
     */
    public List<IRI> getVideoResources() {
        return getLoadedResources();
    }

    public ArrayList<OWLOntology> getLoadedModelsList() {
        candidateVisibleOntologies();
        ArrayList<OWLOntology> result = new ArrayList<OWLOntology>();
        assert (targetOntology != null);
        if (!targetOntology.isEmpty()) {
            result.add(getTargetModel());
        }
        final Set<OWLOntology> importList = projectManager.getImports(targetOntology.getModel());
        Iterator<RDFTriple> iter = (model.matchTriples(projectResource, ontProperty, null)).iterator();
        while (iter.hasNext()) {
            IRI nextOntIRI = iter.next().getObject().getIRI();
            if (!nameInImports(nextOntIRI.toString(), importList)) {
                addImport(targetOntology, nextOntIRI);
            }
            try {
                result.add(projectManager.loadOntology(nextOntIRI));
                CacheManager.getInstance().getDocumentIRI(nextOntIRI);
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        }
        candidateVisibleOntologies();
        return result;
    }

    public Set<OWLOntology> getLoadedModels() {
        candidateVisibleOntologies();
        Set<OWLOntology> result = new HashSet<OWLOntology>();
        assert (targetOntology != null);
        final Set<OWLOntology> importList = projectManager.getImports(targetOntology.getModel());
        Iterator<RDFTriple> iter = (model.matchTriples(projectResource, ontProperty, null)).iterator();
        while (iter.hasNext()) {
            IRI nextOntIRI = iter.next().getObject().getIRI();
            if (!nameInImports(nextOntIRI.toString(), importList)) {
                addImport(targetOntology, nextOntIRI);
            }
            try {
                result.add(projectManager.loadOntology(nextOntIRI));
                CacheManager.getInstance().getDocumentIRI(nextOntIRI);
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        }
        if (!targetOntology.isEmpty()) {
            result.add(getTargetModel());
        }
        candidateVisibleOntologies();
        return result;
    }

    private boolean nameInImports(String name, Set<OWLOntology> imports) {
        final IRI nameIRI = IRI.create(name);
        Iterator<OWLOntology> importIt = imports.iterator();
        while (importIt.hasNext()) {
            if (importIt.next().getOntologyID().getOntologyIRI().equals(nameIRI)) return true;
        }
        return false;
    }

    public OWLOntology getTargetModel() {
        if (targetOntology == null) return null;
        return targetOntology.getModel();
    }

    /**
     * 
     * @param s specifies the URI of the new class
     * @return the new class
     */
    public OWLClass createOWClass(String s) {
        return projectFactory.getOWLClass(IRI.create(s));
    }

    /**
     * 
     * @param s
     * @return the new individual returned by the project's datafactory
     */
    public OWLIndividual createOWLIndividual(String s) {
        IRI u = IRI.create(targetOntology.getModel().getOntologyID().getDefaultDocumentIRI() + "#" + s);
        return projectFactory.getOWLNamedIndividual(u);
    }

    /**
     * 
     * @param propertyURIString
     * @return the property returned by the project's datafactory
     */
    public OWLDataProperty createDatatypeProperty(String propertyIRIString) {
        return projectFactory.getOWLDataProperty(IRI.create(propertyIRIString));
    }

    /**
     * 
     * @param i the individual being 'defined'
     * @param c the class that defines the individual
     * @return the axiom returned by the project's datafactory
     */
    public OWLClassAssertionAxiom createOWLClassAssertionAxiom(OWLIndividual i, OWLClassExpression c) {
        return projectFactory.getOWLClassAssertionAxiom(c, i);
    }

    /**
     * 
     * @param i individual that is assigned the property
     * @param p 
     * @param o
     * @return the axiom returned by the project's datafactory
     */
    public OWLDataPropertyAssertionAxiom createOWLDataPropertyAssertionAxiom(OWLIndividual i, OWLDataProperty p, OWLLiteral o) {
        return projectFactory.getOWLDataPropertyAssertionAxiom(p, i, o);
    }

    /**
     * 
     * @param i
     * @param p
     * @param o
     * @return the axiom returned by the project's dataFactory
     */
    public OWLObjectPropertyAssertionAxiom createOWLObjectPropertyAssertionAxiom(OWLIndividual i, OWLObjectProperty p, OWLIndividual o) {
        return projectFactory.getOWLObjectPropertyAssertionAxiom(p, i, o);
    }

    /**
     * 
     * @return the class OWL_THING
     */
    public OWLClass getOWL_THING() {
        return projectManager.getOWLDataFactory().getOWLClass(OWLRDFVocabulary.OWL_THING.getIRI());
    }

    /**
     * 
     * @param a the change to apply the ontology
     * @throws OWLOntologyChangeException
     */
    public void applyChange(OWLOntologyChange a) throws OWLOntologyChangeException {
        projectManager.applyChange(a);
    }

    /**
     * 
     * @param l the list of changes to apply to the ontology
     * @throws OWLOntologyChangeException
     */
    public void applyChanges(List<OWLOntologyChange> l) throws OWLOntologyChangeException {
        projectManager.applyChanges(l);
    }

    /**
     * 
     * @return an entity remover for the target ontology
     */
    public OWLEntityRemover createOWLEntityRemover() {
        return new OWLEntityRemover(projectManager, Collections.singleton(targetOntology.getModel()));
    }

    /**
     * 
     * @param classNode
     * @param reasoner the reasoner used to determine subclasses
     * @return a set of sets of classes (each set is a set of equivalent classes (as specified by the reasoner))
     */
    public Set<OWLClass> getSubClasses(OWLClass classNode, OWLReasoner reasoner) {
        try {
            if (!reasoner.getPendingChanges().isEmpty()) {
            }
            return reasoner.getSubClasses((OWLClassExpression) classNode, true).getFlattened();
        } catch (OWLReasonerRuntimeException e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

    /**
     * 
     * @param classNode
     * @param reasoner the reasoner used to determine superclasses
     * @return a set of sets of classes (each set is a set of equivalent classes (as specified by the reasoner))
     */
    public Set<OWLClass> getSuperClasses(OWLClass classNode, OWLReasoner reasoner) {
        try {
            if (!reasoner.getPendingChanges().isEmpty()) {
            }
            return reasoner.getSuperClasses((OWLClassExpression) classNode, true).getFlattened();
        } catch (OWLReasonerRuntimeException e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

    public OWLReasoner getDisplayReasoner() {
        return displayReasoner;
    }

    public OWLReasoner getOutboardReasoner() {
        return outboardReasoner;
    }

    /**
     * 
     * @param mapper the mapper for uri's to files
     */
    public void addMapper(OWLOntologyIRIMapper mapper) {
        projectManager.addIRIMapper(mapper);
    }

    /**
     * 
     * @return sorted list of ontologies imported by the project
     */
    public List<OWLOntology> getSortedImportsClosure() {
        return projectManager.getSortedImportsClosure(getTargetModel());
    }

    /**
     * 
     * @return sorted list of potentially visible ontologies
     */
    public List<OWLOntology> candidateVisibleOntologies() {
        List<OWLOntology> imports = getSortedImportsClosure();
        List<IRI> loads = getLoadedOntologies();
        List<OWLOntology> result = imports;
        for (IRI u : loads) {
            OWLOntology ont = projectManager.getOntology(u);
            if (ont != null) {
                if (!result.contains(ont)) result.add(projectManager.getOntology(u));
            }
        }
        return result;
    }

    /**
     * Utility for working with visible ontologies
     * @return visible ontologies list as a set
     */
    public Set<OWLOntology> candidateVisibleOntologiesAsSet() {
        Set<OWLOntology> result = new HashSet<OWLOntology>();
        result.addAll(candidateVisibleOntologies());
        return result;
    }

    /**
     * Sets the visible ontologies to the candidates
     */
    public void initVisibleOntologies() {
        if (visibleOntologies == null) {
            visibleOntologies = new HashSet<OWLOntology>(candidateVisibleOntologies().size());
            for (IRI u : getLoadedOntologies()) {
                visibleOntologies.add(projectManager.getOntology(u));
            }
        }
    }

    /**
     * 
     * @param uCollection
     */
    public void updateVisibleOntologies(Collection<OWLOntology> uCollection) {
        HashSet<OWLOntology> hold = new HashSet<OWLOntology>(uCollection.size());
        for (OWLOntology ont : visibleOntologies) {
            if (!uCollection.contains(ont)) {
                hold.add(ont);
            }
        }
        if (!hold.isEmpty()) {
            setChanged();
            for (OWLOntology ont : hold) visibleOntologies.remove(ont);
        }
        for (OWLOntology ont2 : uCollection) {
            if (!visibleOntologies.contains(ont2)) {
                visibleOntologies.add(ont2);
                setChanged();
            }
        }
        notifyObservers(visibleOntologies);
    }

    /**
     * 
     * @param ont ontology to test
     * @return true if ont is in the visibleOntologies set
     */
    public boolean isVisibleOntology(OWLOntology ont) {
        return visibleOntologies.contains(ont);
    }

    /**
     * 
     * @param ont
     * @param view
     */
    private void setOntologyVisible(OWLOntology ont, TabView view) {
        final OWLOntology loadedOnt = projectManager.getOntology(ont.getOntologyID());
        if (!isLoaded(ont)) {
            final IRI ontIRI = ont.getOntologyID().getOntologyIRI();
            final RDFResourceNode targetResource = model.createResource(ontIRI);
            model.addTriple(projectResource, ontProperty, targetResource);
        }
        if (!visibleOntologies.contains(ont)) {
            visibleOntologies.add(ont);
            setChanged();
        }
        notifyObservers(visibleOntologies);
    }

    private void setOntologyInvisible(OWLOntology ont) {
        if (visibleOntologies.contains(ont)) {
            visibleOntologies.remove(ont);
            setChanged();
        }
        notifyObservers(visibleOntologies);
    }

    /**
     * 
     * @return set of visible ontologies
     */
    public Set<OWLOntology> getVisibleOntologies() {
        return visibleOntologies;
    }

    /**
     * 
     * @return copy of visibleOntologies with the target ontology added
     */
    public Set<OWLOntology> getVisibleOntologiesPlusTarget() {
        Set<OWLOntology> onts = new HashSet<OWLOntology>();
        onts.addAll(getVisibleOntologies());
        onts.add(getTargetModel());
        return onts;
    }

    /**
     * Checks if a class is in a visible ontology
     * @param c class to check
     * @return true is c is mentioned in an axiom in a visible ontology
     */
    public boolean visibleClass(OWLClass c) {
        for (OWLOntology o : visibleOntologies) {
            if (!o.getAxioms(c).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
