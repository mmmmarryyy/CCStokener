package uk.ac.osswatch.simal.model.jena.simal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import uk.ac.osswatch.simal.SimalProperties;
import uk.ac.osswatch.simal.SimalRepositoryFactory;
import uk.ac.osswatch.simal.model.Foaf;
import uk.ac.osswatch.simal.model.IDoapLicence;
import uk.ac.osswatch.simal.model.IProject;
import uk.ac.osswatch.simal.model.IResource;
import uk.ac.osswatch.simal.model.ModelSupport;
import uk.ac.osswatch.simal.model.jena.Licence;
import uk.ac.osswatch.simal.model.jena.Project;
import uk.ac.osswatch.simal.model.jena.Resource;
import uk.ac.osswatch.simal.model.jena.SparqlResult;
import uk.ac.osswatch.simal.model.simal.SimalOntology;
import uk.ac.osswatch.simal.rdf.AbstractSimalRepository;
import uk.ac.osswatch.simal.rdf.ISimalRepository;
import uk.ac.osswatch.simal.rdf.SimalRepositoryException;
import uk.ac.osswatch.simal.rdf.io.RDFXMLUtils;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;

public final class JenaSimalRepository extends AbstractSimalRepository {

    public static final Logger LOGGER = LoggerFactory.getLogger(JenaSimalRepository.class);

    private static ISimalRepository instance;

    private transient Model model;

    private transient JenaDatabaseSupport jenaDatabaseSupport;

    /**
   * Use getInstance instead.
   */
    private JenaSimalRepository() {
        super();
        model = null;
        jenaDatabaseSupport = new JenaDatabaseSupport();
    }

    /**
   * Get the SimalRepository object. Note that only one of these can exist in a
   * single virtual machine.
   * 
   * @return
   * @throws SimalRepositoryException
   */
    public static synchronized ISimalRepository getInstance() throws SimalRepositoryException {
        if (instance == null) {
            instance = new JenaSimalRepository();
        }
        return instance;
    }

    /**
   * Initialise the Jena repository. Will determine type of database
   * using property SimalProperties.PROPERTY_SIMAL_DB_TYPE.
   * @param directory
   *          the directory for the database if it is a persistent repository
   *          (i.e. not a test repo)
   * @throws SimalRepositoryException
   */
    public void initialise(String directory) throws SimalRepositoryException {
        if (model != null) {
            throw new SimalRepositoryException("Illegal attempt to create a second SimalRepository in the same JAVA VM.");
        }
        if (isTest) {
            model = ModelFactory.createDefaultModel();
            initialised = true;
            initTestData();
        } else {
            String dbType = SimalProperties.getProperty(SimalProperties.PROPERTY_SIMAL_DB_TYPE);
            model = jenaDatabaseSupport.initialiseDatabase(dbType, directory);
            initialised = true;
        }
        initReferenceData();
    }

    /**
   * Initialise reference data for this repository. Currently licences only.
   * 
   * @throws SimalRepositoryException
   */
    private void initReferenceData() throws SimalRepositoryException {
        String fileName = "softwarelicences.n3";
        FileManager.get().readModel(model, fileName);
        LOGGER.info("Initialised reference data from file " + fileName);
    }

    /**
   * Initialise the test data, used when running in test mode.
   * @throws SimalRepositoryException 
   */
    private void initTestData() throws SimalRepositoryException {
        try {
            ModelSupport.addTestData(this);
        } catch (Exception e) {
            throw new SimalRepositoryException("Unable to add test data", e);
        }
    }

    public void add(String data) throws SimalRepositoryException {
        LOGGER.debug("Adding RDF data string:\n\t" + data);
        File file = new File("tempData.rdf");
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(file);
            bw = new BufferedWriter(fw);
            bw.write(data);
            bw.flush();
            addProject(file.toURI().toURL(), "");
        } catch (MalformedURLException mue) {
            throw new SimalRepositoryException("Strange... a file we created has a malformed URL", mue);
        } catch (IOException e) {
            throw new SimalRepositoryException("Unable to write file from data string", e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    LOGGER.warn("Unable to close writer", e);
                }
            }
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    LOGGER.warn("Unable to close writer", e);
                }
            }
            boolean deleted = file.delete();
            if (!deleted) {
                LOGGER.warn("Failt to delete temporary file {}", file.toString());
            }
        }
    }

    public void addRDFXML(URL url, String baseURI) throws SimalRepositoryException {
        try {
            model.read(url.openStream(), baseURI);
            LOGGER.debug("Added RDF/XML from " + url.toString());
        } catch (IOException e) {
            throw new SimalRepositoryException("Unable to open stream for " + url, e);
        }
    }

    public void addRDFXML(Document doc) throws SimalRepositoryException {
        RDFXMLUtils.addRDFXMLToModel(doc, model);
    }

    public void destroy() throws SimalRepositoryException {
        LOGGER.info("Destorying the SimalRepository");
        if (model != null) {
            model.close();
            model = null;
        }
        initialised = false;
    }

    /**
   * Generic method for querying the RDF backend.
   * 
   * @param queryStr
   *          valid SPARQL query
   * @return
   * @throws SimalRepositoryException
   */
    public SparqlResult getSparqlQueryResult(String queryStr) throws SimalRepositoryException {
        SparqlResult sparqlResult = null;
        QueryExecution qe = null;
        try {
            Query query = QueryFactory.create(queryStr);
            qe = QueryExecutionFactory.create(query, model);
            ResultSet results = qe.execSelect();
            sparqlResult = new SparqlResult(results.getResultVars());
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                List<RDFNode> result = new ArrayList<RDFNode>();
                Iterator<String> varNamesIter = soln.varNames();
                while (varNamesIter.hasNext()) {
                    String varName = varNamesIter.next();
                    result.add(soln.get(varName));
                }
                sparqlResult.addResult(result);
            }
        } catch (QueryException e) {
            String message = "QueryException when trying to SPARQLquery with query: " + queryStr;
            LOGGER.warn(message + "; message: " + e.getMessage());
            throw new SimalRepositoryException(message, e);
        } finally {
            if (qe != null) {
                qe.close();
            }
        }
        return sparqlResult;
    }

    /**
   * @deprecated Use IProjectService.findProjectByHomepage instead.
   */
    public IProject findProjectByHomepage(String homepage) throws SimalRepositoryException {
        return SimalRepositoryFactory.getProjectService().findProjectByHomepage(homepage);
    }

    /**
   * @refactor should be moved to ProjectService class
   */
    public Set<IProject> getAllProjects() throws SimalRepositoryException {
        Property o = model.createProperty(SIMAL_PROJECT_URI);
        StmtIterator itr = model.listStatements(null, RDF.type, o);
        Set<IProject> projects = new HashSet<IProject>();
        while (itr.hasNext()) {
            String uri = itr.nextStatement().getSubject().getURI();
            projects.add(new Project(model.getResource(uri)));
        }
        return projects;
    }

    public Set<IDoapLicence> getAllLicences() {
        String CC_LICENCE = "http://creativecommons.org/ns#License";
        Property o = model.createProperty(CC_LICENCE);
        StmtIterator itr = model.listStatements(null, RDF.type, o);
        Set<IDoapLicence> licences = new HashSet<IDoapLicence>();
        while (itr.hasNext()) {
            String uri = itr.nextStatement().getSubject().getURI();
            licences.add(new Licence(model.getResource(uri)));
        }
        return licences;
    }

    /**
	 * Get a featured project. At the present time this will
	 * return a single random project from the repository.
	 * 
	 * @return
	 * @throws SimalRepositoryException 
	 */
    public IProject getFeaturedProject() throws SimalRepositoryException {
        IProject project;
        Set<IProject> allProjects = getAllProjects();
        Random rand = new Random();
        int size = allProjects.size();
        if (size > 0) {
            int idx = rand.nextInt(size);
            project = (IProject) allProjects.toArray()[idx];
        } else {
            project = null;
        }
        return project;
    }

    /**
   * @refactor should be moved to ProjectService class
   */
    public String getAllProjectsAsJSON() throws SimalRepositoryException {
        StringBuffer json = new StringBuffer("{ \"items\": [");
        Iterator<IProject> projects = getAllProjects().iterator();
        IProject project;
        while (projects.hasNext()) {
            project = projects.next();
            json.append(project.toJSONRecord());
            if (projects.hasNext()) {
                json.append(",");
            }
        }
        json.append("]}");
        return json.toString();
    }

    /**
   * Test to see if an organisation with the given String exists.
   * 
   * @param uri
   * @return
   */
    public boolean containsOrganisation(String uri) {
        Property o = model.createProperty(Foaf.NS + "Organization");
        com.hp.hpl.jena.rdf.model.Resource r = model.createResource(uri);
        Statement foaf = model.createStatement(r, RDF.type, o);
        return model.contains(foaf);
    }

    public boolean containsPerson(String uri) {
        Property o = model.createProperty(Foaf.NS + "Person");
        com.hp.hpl.jena.rdf.model.Resource r = model.createResource(uri);
        Statement foaf = model.createStatement(r, RDF.type, o);
        Statement simal = model.createStatement(r, RDF.type, SimalOntology.PERSON);
        return model.contains(foaf) || model.contains(simal);
    }

    public boolean containsResource(String uri) {
        com.hp.hpl.jena.rdf.model.Resource r = model.createResource(uri);
        return model.containsResource(r);
    }

    public void removeAllData() {
        LOGGER.warn("Removing all data from the repository.");
        model = jenaDatabaseSupport.removeAllData(model);
    }

    /**
   * Get a Jena Resource.
   * 
   * @param uri
   * @return
   */
    public com.hp.hpl.jena.rdf.model.Resource getJenaResource(String uri) {
        return model.getResource(uri);
    }

    public IResource getResource(String uri) {
        return new Resource(model.getResource(uri));
    }

    public void writeBackup(Writer writer) {
        model.write(writer, "N3");
    }

    /** 
   * @see uk.ac.osswatch.simal.rdf.ISimalRepository#addProject(org.w3c.dom.Document, java.net.URL, java.lang.String)
   * @deprecated Use IProjectService.createProject(Document doc) instead
   */
    public void addProject(Document originalDoc, URL sourceURL, String baseURI) throws SimalRepositoryException {
        SimalRepositoryFactory.getProjectService().createProject(originalDoc);
    }

    /**
	 * Get the RDF model for this repository.
	 * @return
	 */
    public Model getModel() {
        return model;
    }
}
