package org.dllearner.scripts.evaluation;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.prefs.Preferences;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.dllearner.algorithms.DisjointClassesLearner;
import org.dllearner.algorithms.SimpleSubclassLearner;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.algorithms.properties.DataPropertyDomainAxiomLearner;
import org.dllearner.algorithms.properties.DataPropertyRangeAxiomLearner;
import org.dllearner.algorithms.properties.DisjointDataPropertyAxiomLearner;
import org.dllearner.algorithms.properties.EquivalentDataPropertyAxiomLearner;
import org.dllearner.algorithms.properties.EquivalentObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.FunctionalDataPropertyAxiomLearner;
import org.dllearner.algorithms.properties.FunctionalObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.InverseFunctionalObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.IrreflexiveObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.ObjectPropertyDomainAxiomLearner;
import org.dllearner.algorithms.properties.ObjectPropertyRangeAxiomLearner;
import org.dllearner.algorithms.properties.ReflexiveObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.SubDataPropertyOfAxiomLearner;
import org.dllearner.algorithms.properties.SubObjectPropertyOfAxiomLearner;
import org.dllearner.algorithms.properties.SymmetricObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.TransitiveObjectPropertyAxiomLearner;
import org.dllearner.core.AbstractAxiomLearningAlgorithm;
import org.dllearner.core.AbstractReasonerComponent;
import org.dllearner.core.AnnComponentManager;
import org.dllearner.core.AxiomLearningAlgorithm;
import org.dllearner.core.ComponentAnn;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.ComponentManager;
import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.LearningAlgorithm;
import org.dllearner.core.LearningProblemUnsupportedException;
import org.dllearner.core.Score;
import org.dllearner.core.config.ConfigHelper;
import org.dllearner.core.owl.Axiom;
import org.dllearner.core.owl.DatatypeProperty;
import org.dllearner.core.owl.EquivalentClassesAxiom;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.core.owl.SubClassAxiom;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SPARQLTasks;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlKnowledgeSource;
import org.dllearner.learningproblems.ClassLearningProblem;
import org.dllearner.learningproblems.Heuristics.HeuristicType;
import org.dllearner.reasoning.FastInstanceChecker;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.utilities.CommonPrefixMap;
import org.dllearner.utilities.Files;
import org.dllearner.utilities.Helper;
import org.dllearner.utilities.datastructures.Datastructures;
import org.dllearner.utilities.datastructures.SetManipulation;
import org.dllearner.utilities.datastructures.SortedSetTuple;
import org.dllearner.utilities.examples.AutomaticNegativeExampleFinderSPARQL2;
import org.dllearner.utilities.owl.DLLearnerAxiomConvertVisitor;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.ini4j.IniPreferences;
import org.ini4j.InvalidFileFormatException;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.TimeOutException;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Evaluation of enrichment algorithms on DBpedia (Live).
 * 
 * @author Jens Lehmann
 * 
 */
public class EnrichmentEvaluation {

    private static Logger logger = Logger.getLogger(EnrichmentEvaluation.class);

    private final int maxAttempts = 5;

    private final int delayInMilliseconds = 15000;

    private int maxExecutionTimeInSeconds = 10;

    private int nrOfAxiomsToLearn = 10;

    private double threshold = 0.7;

    private SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();

    private int maxObjectProperties = 0;

    private int maxDataProperties = 0;

    private int maxClasses = 0;

    private List<Class<? extends AxiomLearningAlgorithm>> objectPropertyAlgorithms;

    private List<Class<? extends AxiomLearningAlgorithm>> dataPropertyAlgorithms;

    private List<Class<? extends LearningAlgorithm>> classAlgorithms;

    private String baseURI = "http://dbpedia.org/resource/";

    private Map<String, String> prefixes = new CommonPrefixMap();

    private Connection conn;

    private PreparedStatement ps;

    private OWLOntology dbPediaOntology;

    private OWLReasoner reasoner;

    private OWLDataFactory factory = new OWLDataFactoryImpl();

    public EnrichmentEvaluation() {
        prefixes = new HashMap<String, String>();
        prefixes.put("dbp", "http://dbpedia.org/property/");
        prefixes.put("dbo", "http://dbpedia.org/ontology/");
        prefixes.put("yago", "http://dbpedia.org/class/");
        objectPropertyAlgorithms = new LinkedList<Class<? extends AxiomLearningAlgorithm>>();
        objectPropertyAlgorithms.add(EquivalentObjectPropertyAxiomLearner.class);
        objectPropertyAlgorithms.add(FunctionalObjectPropertyAxiomLearner.class);
        objectPropertyAlgorithms.add(InverseFunctionalObjectPropertyAxiomLearner.class);
        objectPropertyAlgorithms.add(ObjectPropertyDomainAxiomLearner.class);
        objectPropertyAlgorithms.add(ObjectPropertyRangeAxiomLearner.class);
        objectPropertyAlgorithms.add(SubObjectPropertyOfAxiomLearner.class);
        objectPropertyAlgorithms.add(SymmetricObjectPropertyAxiomLearner.class);
        objectPropertyAlgorithms.add(TransitiveObjectPropertyAxiomLearner.class);
        objectPropertyAlgorithms.add(IrreflexiveObjectPropertyAxiomLearner.class);
        objectPropertyAlgorithms.add(ReflexiveObjectPropertyAxiomLearner.class);
        dataPropertyAlgorithms = new LinkedList<Class<? extends AxiomLearningAlgorithm>>();
        dataPropertyAlgorithms.add(EquivalentDataPropertyAxiomLearner.class);
        dataPropertyAlgorithms.add(FunctionalDataPropertyAxiomLearner.class);
        dataPropertyAlgorithms.add(DataPropertyDomainAxiomLearner.class);
        dataPropertyAlgorithms.add(DataPropertyRangeAxiomLearner.class);
        dataPropertyAlgorithms.add(SubDataPropertyOfAxiomLearner.class);
        classAlgorithms = new LinkedList<Class<? extends LearningAlgorithm>>();
        classAlgorithms.add(DisjointClassesLearner.class);
        classAlgorithms.add(SimpleSubclassLearner.class);
        initDBConnection();
        loadCurrentDBpediaOntology2();
    }

    private void initDBConnection() {
        try {
            String iniFile = "db_settings.ini";
            Preferences prefs = new IniPreferences(new FileReader(iniFile));
            String dbServer = prefs.node("database").get("server", null);
            String dbName = prefs.node("database").get("name", null);
            String dbUser = prefs.node("database").get("user", null);
            String dbPass = prefs.node("database").get("pass", null);
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + dbServer + "/" + dbName;
            conn = DriverManager.getConnection(url, dbUser, dbPass);
            ps = conn.prepareStatement("INSERT INTO evaluation (" + "entity, algorithm, axiom, score, runtime_ms, entailed ) " + "VALUES(?,?,?,?,?,?)");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InvalidFileFormatException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dropAndCreateTable() {
        try {
            Statement s = conn.createStatement();
            s.executeUpdate("DROP TABLE IF EXISTS evaluation");
            s.executeUpdate("CREATE TABLE evaluation (" + "id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," + "entity VARCHAR(200), algorithm VARCHAR(100), axiom VARCHAR(500), score DOUBLE, runtime_ms INT(20), entailed BOOLEAN)");
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void writeToDB(String entity, String algorithm, String axiom, double score, long runTime, boolean entailed) {
        try {
            ps.setString(1, entity);
            ps.setString(2, algorithm);
            ps.setString(3, axiom);
            ps.setDouble(4, score);
            ps.setLong(5, runTime);
            ps.setBoolean(6, entailed);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error while writing to DB.", e);
            e.printStackTrace();
        }
    }

    public void start() throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ComponentInitException, InterruptedException {
        long overallStartTime = System.currentTimeMillis();
        SparqlEndpointKS ks = new SparqlEndpointKS(endpoint);
        ks.init();
        Thread.sleep(20000);
        evaluateClasses(ks);
        System.out.println("Overall runtime: " + (System.currentTimeMillis() - overallStartTime) / 1000 + "s.");
    }

    private void evaluateObjectProperties(SparqlEndpointKS ks) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ComponentInitException, InterruptedException {
        Set<ObjectProperty> properties = new SPARQLTasks(ks.getEndpoint()).getAllObjectProperties();
        for (Class<? extends AxiomLearningAlgorithm> algorithmClass : objectPropertyAlgorithms) {
            int objectProperties = 0;
            Thread.sleep(10000);
            String algName = "";
            for (ObjectProperty property : properties) {
                try {
                    AxiomLearningAlgorithm learner = algorithmClass.getConstructor(SparqlEndpointKS.class).newInstance(ks);
                    ConfigHelper.configure(learner, "propertyToDescribe", property.toString());
                    ConfigHelper.configure(learner, "maxExecutionTimeInSeconds", maxExecutionTimeInSeconds);
                    learner.init();
                    algName = AnnComponentManager.getName(learner);
                    int attempt = 0;
                    long startTime = 0;
                    boolean timeout = true;
                    while (((AbstractAxiomLearningAlgorithm) learner).isTimeout() && attempt++ < maxAttempts) {
                        if (attempt > 1) {
                            try {
                                System.out.println("Got timeout. Waiting " + delayInMilliseconds + " ms ...");
                                Thread.sleep(delayInMilliseconds);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("Applying " + algName + " on " + property + " ... (Attempt " + attempt + ")");
                        startTime = System.currentTimeMillis();
                        try {
                            learner.start();
                            timeout = false;
                        } catch (Exception e) {
                            if (e.getCause() instanceof SocketTimeoutException) {
                            } else {
                                e.printStackTrace();
                            }
                        }
                    }
                    long runTime = System.currentTimeMillis() - startTime;
                    List<EvaluatedAxiom> learnedAxioms = learner.getCurrentlyBestEvaluatedAxioms(nrOfAxiomsToLearn);
                    if (timeout && learnedAxioms.isEmpty()) {
                        writeToDB(property.toManchesterSyntaxString(baseURI, prefixes), algName, "TIMEOUT", 0, runTime, false);
                    } else if (learnedAxioms == null || learnedAxioms.isEmpty()) {
                        writeToDB(property.toManchesterSyntaxString(baseURI, prefixes), algName, "NULL", 0, runTime, false);
                    } else {
                        for (EvaluatedAxiom learnedAxiom : learnedAxioms) {
                            double score = learnedAxiom.getScore().getAccuracy();
                            if (Double.isNaN(score)) {
                                score = -1;
                            }
                            writeToDB(property.toManchesterSyntaxString(baseURI, prefixes).toString(), algName, learnedAxiom.getAxiom().toManchesterSyntaxString(baseURI, prefixes), score, runTime, isEntailed(learnedAxiom));
                        }
                    }
                    objectProperties++;
                    if (maxObjectProperties != 0 && objectProperties > maxObjectProperties) {
                        break;
                    }
                } catch (Exception e) {
                    logger.error("Error occured for object property " + property.getName() + " with algorithm " + algName, e);
                }
            }
        }
    }

    private void evaluateDataProperties(SparqlEndpointKS ks) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ComponentInitException, InterruptedException {
        Set<DatatypeProperty> properties = new SPARQLTasks(ks.getEndpoint()).getAllDataProperties();
        for (Class<? extends AxiomLearningAlgorithm> algorithmClass : dataPropertyAlgorithms) {
            int dataProperties = 0;
            Thread.sleep(10000);
            String algName = "";
            for (DatatypeProperty property : properties) {
                try {
                    AxiomLearningAlgorithm learner = algorithmClass.getConstructor(SparqlEndpointKS.class).newInstance(ks);
                    ConfigHelper.configure(learner, "propertyToDescribe", property.toString());
                    ConfigHelper.configure(learner, "maxExecutionTimeInSeconds", maxExecutionTimeInSeconds);
                    learner.init();
                    algName = AnnComponentManager.getName(learner);
                    int attempt = 0;
                    long startTime = 0;
                    boolean timeout = true;
                    while (((AbstractAxiomLearningAlgorithm) learner).isTimeout() && attempt++ < maxAttempts) {
                        if (attempt > 1) {
                            try {
                                Thread.sleep(delayInMilliseconds);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("Applying " + algName + " on " + property + " ... (Attempt " + attempt + ")");
                        startTime = System.currentTimeMillis();
                        try {
                            learner.start();
                            timeout = false;
                        } catch (Exception e) {
                            if (e.getCause() instanceof SocketTimeoutException) {
                            } else {
                                e.printStackTrace();
                            }
                        }
                    }
                    long runTime = System.currentTimeMillis() - startTime;
                    List<EvaluatedAxiom> learnedAxioms = learner.getCurrentlyBestEvaluatedAxioms(nrOfAxiomsToLearn);
                    if (timeout && learnedAxioms.isEmpty()) {
                        writeToDB(property.toManchesterSyntaxString(baseURI, prefixes), algName, "TIMEOUT", 0, runTime, false);
                    } else if (learnedAxioms == null || learnedAxioms.isEmpty()) {
                        writeToDB(property.toManchesterSyntaxString(baseURI, prefixes), algName, "NULL", 0, runTime, false);
                    } else {
                        for (EvaluatedAxiom learnedAxiom : learnedAxioms) {
                            double score = learnedAxiom.getScore().getAccuracy();
                            if (Double.isNaN(score)) {
                                score = -1;
                            }
                            writeToDB(property.toManchesterSyntaxString(baseURI, prefixes).toString(), algName, learnedAxiom.getAxiom().toManchesterSyntaxString(baseURI, prefixes), score, runTime, isEntailed(learnedAxiom));
                        }
                    }
                    dataProperties++;
                    if (maxDataProperties != 0 && dataProperties > maxDataProperties) {
                        break;
                    }
                } catch (Exception e) {
                    logger.error("Error occured for data property " + property.getName() + " with algorithm " + algName, e);
                }
            }
        }
    }

    private void evaluateClasses(SparqlEndpointKS ks) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ComponentInitException, InterruptedException {
        Set<NamedClass> classes = new SPARQLTasks(ks.getEndpoint()).getAllClasses();
        for (Class<? extends LearningAlgorithm> algorithmClass : classAlgorithms) {
            int classesCnt = 0;
            Thread.sleep(10000);
            for (NamedClass cls : classes) {
                try {
                    List<EvaluatedAxiom> learnedAxioms = null;
                    long startTime = System.currentTimeMillis();
                    boolean timeout = false;
                    String algName;
                    if (algorithmClass == CELOE.class) {
                        algName = CELOE.class.getAnnotation(ComponentAnn.class).name();
                        System.out.println("Applying " + algName + " on " + cls + " ... ");
                        learnedAxioms = applyCELOE(ks, cls, false);
                    } else {
                        LearningAlgorithm learner = algorithmClass.getConstructor(SparqlEndpointKS.class).newInstance(ks);
                        ConfigHelper.configure(learner, "classToDescribe", cls.toString());
                        ConfigHelper.configure(learner, "maxExecutionTimeInSeconds", maxExecutionTimeInSeconds);
                        learner.init();
                        algName = AnnComponentManager.getName(learner);
                        int attempt = 0;
                        timeout = true;
                        while (((AbstractAxiomLearningAlgorithm) learner).isTimeout() && attempt++ < maxAttempts) {
                            if (attempt > 1) {
                                try {
                                    Thread.sleep(delayInMilliseconds);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            System.out.println("Applying " + algName + " on " + cls + " ... (Attempt " + attempt + ")");
                            startTime = System.currentTimeMillis();
                            try {
                                learner.start();
                                timeout = false;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        learnedAxioms = ((AxiomLearningAlgorithm) learner).getCurrentlyBestEvaluatedAxioms(nrOfAxiomsToLearn);
                    }
                    long runTime = System.currentTimeMillis() - startTime;
                    if (timeout && learnedAxioms.isEmpty()) {
                        writeToDB(cls.toManchesterSyntaxString(baseURI, prefixes), algName, "TIMEOUT", 0, runTime, false);
                    } else if (learnedAxioms == null || learnedAxioms.isEmpty()) {
                        writeToDB(cls.toManchesterSyntaxString(baseURI, prefixes), algName, "NULL", 0, runTime, false);
                    } else {
                        for (EvaluatedAxiom learnedAxiom : learnedAxioms) {
                            double score = learnedAxiom.getScore().getAccuracy();
                            if (Double.isNaN(score)) {
                                score = -1;
                            }
                            writeToDB(cls.toManchesterSyntaxString(baseURI, prefixes).toString(), algName, learnedAxiom.getAxiom().toManchesterSyntaxString(baseURI, prefixes), score, runTime, isEntailed(learnedAxiom));
                        }
                    }
                    classesCnt++;
                    if (maxClasses != 0 && classesCnt > maxClasses) {
                        break;
                    }
                } catch (Exception e) {
                    logger.error("Error occured for class " + cls.getName(), e);
                }
            }
        }
    }

    private List<EvaluatedAxiom> applyCELOE(SparqlEndpointKS ks, NamedClass nc, boolean equivalence) throws ComponentInitException {
        SPARQLReasoner sr = new SPARQLReasoner(ks);
        SortedSet<Individual> posExamples = sr.getIndividuals(nc, 20);
        SortedSet<String> posExStr = Helper.getStringSet(posExamples);
        System.out.print("finding negatives ... ");
        AutomaticNegativeExampleFinderSPARQL2 finder = new AutomaticNegativeExampleFinderSPARQL2(ks.getEndpoint());
        SortedSet<String> negExStr = finder.getNegativeExamples(nc.getName(), posExStr);
        negExStr = SetManipulation.fuzzyShrink(negExStr, 20);
        SortedSet<Individual> negExamples = Helper.getIndividualSet(negExStr);
        SortedSetTuple<Individual> examples = new SortedSetTuple<Individual>(posExamples, negExamples);
        System.out.println("done (" + negExStr.size() + ")");
        ComponentManager cm = ComponentManager.getInstance();
        SparqlKnowledgeSource ks2 = cm.knowledgeSource(SparqlKnowledgeSource.class);
        ks2.setInstances(Datastructures.individualSetToStringSet(examples.getCompleteSet()));
        ks2.setUrl(ks.getEndpoint().getURL());
        ks2.setDefaultGraphURIs(new TreeSet<String>(ks.getEndpoint().getDefaultGraphURIs()));
        ks2.setUseLits(false);
        ks2.setUseCacheDatabase(true);
        ks2.setRecursionDepth(2);
        ks2.setCloseAfterRecursion(true);
        System.out.println("getting fragment ... ");
        ks2.init();
        System.out.println("done");
        AbstractReasonerComponent rc = cm.reasoner(FastInstanceChecker.class, ks2);
        rc.init();
        ClassLearningProblem lp = cm.learningProblem(ClassLearningProblem.class, rc);
        lp.setClassToDescribe(nc);
        lp.setEquivalence(true);
        lp.setHeuristic(HeuristicType.FMEASURE);
        lp.setUseApproximations(false);
        lp.setMaxExecutionTimeInSeconds(10);
        lp.init();
        CELOE la = null;
        try {
            la = cm.learningAlgorithm(CELOE.class, lp, rc);
        } catch (LearningProblemUnsupportedException e) {
            e.printStackTrace();
        }
        la.setMaxExecutionTimeInSeconds(10);
        la.setNoisePercentage(25);
        la.init();
        System.out.print("running CELOE ... ");
        la.start();
        System.out.println("done");
        List<? extends EvaluatedDescription> learnedDescriptions = la.getCurrentlyBestEvaluatedDescriptions(threshold);
        List<EvaluatedAxiom> evaluatedAxioms = new LinkedList<EvaluatedAxiom>();
        for (EvaluatedDescription learnedDescription : learnedDescriptions) {
            Axiom axiom;
            if (equivalence) {
                axiom = new EquivalentClassesAxiom(nc, learnedDescription.getDescription());
            } else {
                axiom = new SubClassAxiom(nc, learnedDescription.getDescription());
            }
            Score score = lp.computeScore(learnedDescription.getDescription());
            evaluatedAxioms.add(new EvaluatedAxiom(axiom, score));
        }
        cm.freeAllComponents();
        return evaluatedAxioms;
    }

    public void printResultsPlain() {
    }

    public void printResultsLaTeX() throws Exception {
        List<Class<? extends LearningAlgorithm>> algorithms = new ArrayList<Class<? extends LearningAlgorithm>>();
        algorithms.addAll(classAlgorithms);
        algorithms.addAll(objectPropertyAlgorithms);
        algorithms.addAll(dataPropertyAlgorithms);
        PreparedStatement ps = conn.prepareStatement("CREATE OR REPLACE VIEW evaluation_cleaned AS (SELECT * FROM evaluation WHERE axiom != ? AND axiom != ?)");
        ps.setString(1, "NULL");
        ps.setString(2, "TIMEOUT");
        ps.execute();
        StringBuilder table1 = new StringBuilder();
        table1.append("\\begin{tabulary}{\\textwidth}{LRRRRR}\\toprule\n");
        table1.append(" algorithm & Avg. \\#suggestions & Avg. runtime in ms & timeout in \\% & Avg. score & Avg. maximum score\\\\\\midrule\n");
        for (Class<? extends LearningAlgorithm> algo : algorithms) {
            String algoName = algo.getAnnotation(ComponentAnn.class).name();
            ps = conn.prepareStatement("SELECT COUNT(DISTINCT entity) FROM evaluation WHERE algorithm=?");
            ps.setString(1, algoName);
            java.sql.ResultSet rs = ps.executeQuery();
            rs.next();
            int overallNumberOfEntities = rs.getInt(1);
            ps = conn.prepareStatement("SELECT COUNT(DISTINCT entity) FROM evaluation WHERE algorithm=? AND axiom=?");
            ps.setString(1, algoName);
            ps.setString(2, "NULL");
            rs = ps.executeQuery();
            rs.next();
            int numberOfEntitiesWithEmptyResult = rs.getInt(1);
            ps = conn.prepareStatement("SELECT COUNT(DISTINCT entity) FROM evaluation WHERE algorithm=? AND axiom=?");
            ps.setString(1, algoName);
            ps.setString(2, "TIMEOUT");
            rs = ps.executeQuery();
            rs.next();
            int numberOfEntitiesWithTimeout = rs.getInt(1);
            ps = conn.prepareStatement("SELECT AVG(cnt) FROM (SELECT entity, COUNT(axiom) AS cnt FROM (SELECT * FROM evaluation WHERE algorithm=? AND score >=?) AS A GROUP BY entity) AS B");
            ps.setString(1, algoName);
            ps.setDouble(2, threshold);
            rs = ps.executeQuery();
            rs.next();
            double avgSuggestionsAboveThreshold = round(rs.getDouble(1));
            ps = conn.prepareStatement("SELECT AVG(runtime) FROM (SELECT MAX(runtime_ms) AS runtime FROM evaluation WHERE algorithm=?) AS A");
            ps.setString(1, algoName);
            rs = ps.executeQuery();
            rs.next();
            double avgRuntimeInMilliseconds = rs.getDouble(1);
            double timeoutRatio = round((double) numberOfEntitiesWithTimeout / overallNumberOfEntities);
            ps = conn.prepareStatement("SELECT AVG(avg) FROM (SELECT AVG(score) AS avg FROM evaluation_cleaned WHERE algorithm=? AND score >= ? GROUP BY entity) AS A");
            ps.setString(1, algoName);
            ps.setDouble(2, threshold);
            rs = ps.executeQuery();
            rs.next();
            double avgScore = round(rs.getDouble(1));
            ps = conn.prepareStatement("SELECT AVG(max) FROM (SELECT MAX(score) AS max FROM evaluation_cleaned WHERE algorithm=? AND score>=? GROUP BY entity) AS A");
            ps.setString(1, algoName);
            ps.setDouble(2, threshold);
            rs = ps.executeQuery();
            rs.next();
            double avgMaxScore = round(rs.getDouble(1));
            table1.append(algoName.replace("axiom learner", "").trim()).append(" & ").append(avgSuggestionsAboveThreshold).append(" & ").append(avgRuntimeInMilliseconds).append(" & ").append(timeoutRatio).append(" & ").append(avgScore).append(" & ").append(avgMaxScore).append("\\\\\n");
        }
        table1.append("\\bottomrule\n\\end{tabulary}");
        System.out.println(table1.toString());
        StringBuilder table2 = new StringBuilder();
        table2.append("\\begin{tabulary}{\\textwidth}{LCCCCC}\\toprule\n");
        table2.append("& & & \\multicolumn{3}{c}{Estimated precision} \\\\\n");
        table2.append(" axiom type & recall & additional axioms & no & maybe & yes \\\\\\midrule\n");
        Map<AxiomType<? extends OWLAxiom>, List<Class<? extends LearningAlgorithm>>> axiomType2Algorithm = getAxiomTypesWithLearningAlgorithms();
        java.sql.ResultSet rs = conn.prepareStatement("SELECT DISTINCT entity FROM evaluation WHERE axiom != 'NULL'").executeQuery();
        Set<OWLEntity> entities = new HashSet<OWLEntity>();
        IRI iri;
        while (rs.next()) {
            iri = IRI.create("http://dbpedia.org/ontology/" + rs.getString(1).substring(4));
            if (dbPediaOntology.containsClassInSignature(iri)) {
                entities.add(factory.getOWLClass(iri));
            } else if (dbPediaOntology.containsObjectPropertyInSignature(iri)) {
                entities.add(factory.getOWLObjectProperty(iri));
            } else if (dbPediaOntology.containsDataPropertyInSignature(iri)) {
                entities.add(factory.getOWLDataProperty(iri));
            }
        }
        ps = conn.prepareStatement("SELECT axiom, entailed, score FROM evaluation WHERE algorithm=? AND score>=?");
        for (Entry<AxiomType<? extends OWLAxiom>, List<Class<? extends LearningAlgorithm>>> entry : axiomType2Algorithm.entrySet()) {
            AxiomType<? extends OWLAxiom> type = entry.getKey();
            algorithms = entry.getValue();
            ps.setString(1, algorithms.get(0).getAnnotation(ComponentAnn.class).name());
            ps.setDouble(2, threshold);
            Set<String> foundAxioms = new TreeSet<String>();
            Map<String, Double> foundAndNotEntailedAxioms = new TreeMap<String, Double>();
            rs = ps.executeQuery();
            String axiom;
            boolean entailed;
            double score;
            while (rs.next()) {
                axiom = rs.getString(1);
                entailed = rs.getBoolean(2);
                score = rs.getDouble(3);
                foundAxioms.add(axiom);
                if (!entailed) {
                    foundAndNotEntailedAxioms.put(axiom, score);
                }
            }
            Set<String> relevantAxioms = getRelevantAxioms(type, entities);
            Set<String> missedAxioms = org.mindswap.pellet.utils.SetUtils.difference(relevantAxioms, foundAxioms);
            System.out.println(missedAxioms);
            for (String relAxiom : relevantAxioms) {
                foundAndNotEntailedAxioms.remove(relAxiom);
            }
            Set<String> additionalAxioms = foundAndNotEntailedAxioms.keySet();
            int total = relevantAxioms.size();
            int found = total - missedAxioms.size();
            table2.append(type.getName()).append(" & ").append(found + "/" + total).append(" & ").append(additionalAxioms.size()).append(" & & & \\\\\n");
            System.out.println(type.getName() + ": " + found + "/" + total);
            writeToDisk(type, foundAndNotEntailedAxioms);
            writeToDisk(type, missedAxioms);
        }
        table2.append("\\end{tabulary}");
        System.out.println(table2.toString());
    }

    private void writeToDisk(AxiomType<? extends OWLAxiom> axiomType, Map<String, Double> axiomsWithAccurracy) {
        String fileName = axiomType.getName().replaceAll(" ", "_") + ".txt";
        BufferedWriter out = null;
        try {
            File dir = new File("evaluation/additional");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir + File.separator + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            out = new BufferedWriter(new FileWriter(file));
            int i = 0;
            for (Entry<String, Double> entry : sortByValues(axiomsWithAccurracy)) {
                i++;
                out.write(entry.getKey() + " (" + round(entry.getValue()) * 100 + "%)");
                out.newLine();
                if (i == 100) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void writeToDisk(AxiomType<? extends OWLAxiom> axiomType, Set<String> axioms) {
        String fileName = axiomType.getName().replaceAll(" ", "_") + ".txt";
        BufferedWriter out = null;
        try {
            File dir = new File("evaluation/missed");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir + File.separator + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            out = new BufferedWriter(new FileWriter(file));
            for (String axiom : axioms) {
                out.write(axiom);
                out.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected <K, V extends Comparable<V>> List<Entry<K, V>> sortByValues(Map<K, V> map) {
        List<Entry<K, V>> entries = new ArrayList<Entry<K, V>>(map.entrySet());
        Collections.sort(entries, new Comparator<Entry<K, V>>() {

            @Override
            public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        return entries;
    }

    private Map<AxiomType<? extends OWLAxiom>, List<Class<? extends LearningAlgorithm>>> getAxiomTypesWithLearningAlgorithms() {
        Map<AxiomType<? extends OWLAxiom>, List<Class<? extends LearningAlgorithm>>> axiomType2Algorithm = new LinkedHashMap<AxiomType<? extends OWLAxiom>, List<Class<? extends LearningAlgorithm>>>();
        axiomType2Algorithm.put(AxiomType.SUBCLASS_OF, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { SimpleSubclassLearner.class, CELOE.class }));
        axiomType2Algorithm.put(AxiomType.EQUIVALENT_CLASSES, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { CELOE.class }));
        axiomType2Algorithm.put(AxiomType.DISJOINT_CLASSES, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { DisjointClassesLearner.class }));
        axiomType2Algorithm.put(AxiomType.SUB_OBJECT_PROPERTY, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { SubObjectPropertyOfAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.EQUIVALENT_OBJECT_PROPERTIES, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { EquivalentObjectPropertyAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.OBJECT_PROPERTY_DOMAIN, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { ObjectPropertyDomainAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.OBJECT_PROPERTY_RANGE, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { ObjectPropertyRangeAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.TRANSITIVE_OBJECT_PROPERTY, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { TransitiveObjectPropertyAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.FUNCTIONAL_OBJECT_PROPERTY, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { FunctionalObjectPropertyAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { InverseFunctionalObjectPropertyAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.SYMMETRIC_OBJECT_PROPERTY, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { SymmetricObjectPropertyAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.REFLEXIVE_OBJECT_PROPERTY, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { ReflexiveObjectPropertyAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { IrreflexiveObjectPropertyAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.SUB_DATA_PROPERTY, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { SubDataPropertyOfAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.EQUIVALENT_DATA_PROPERTIES, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { EquivalentDataPropertyAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.DATA_PROPERTY_DOMAIN, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { DataPropertyDomainAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.DATA_PROPERTY_RANGE, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { DataPropertyRangeAxiomLearner.class }));
        axiomType2Algorithm.put(AxiomType.FUNCTIONAL_DATA_PROPERTY, Arrays.asList((Class<? extends LearningAlgorithm>[]) new Class[] { FunctionalDataPropertyAxiomLearner.class }));
        return axiomType2Algorithm;
    }

    private Set<String> getRelevantAxioms(AxiomType<? extends OWLAxiom> axiomType, Set<OWLEntity> entities) {
        Set<String> relevantAxioms = new HashSet<String>();
        for (OWLAxiom axiom : dbPediaOntology.getAxioms(axiomType)) {
            if (!axiom.getClassesInSignature().contains(factory.getOWLThing())) {
                if (isRelevantAxiom(axiom, entities)) {
                    String axiomString = DLLearnerAxiomConvertVisitor.getDLLearnerAxiom(axiom).toManchesterSyntaxString(baseURI, prefixes);
                    relevantAxioms.add(axiomString);
                }
            }
        }
        return relevantAxioms;
    }

    private boolean isRelevantAxiom(OWLAxiom axiom, Set<OWLEntity> entities) {
        if (axiom instanceof OWLObjectPropertyAxiom) {
            return containsOneOf(axiom.getObjectPropertiesInSignature(), entities);
        } else if (axiom instanceof OWLDataPropertyAxiom) {
            return containsOneOf(axiom.getDataPropertiesInSignature(), entities);
        } else if (axiom instanceof OWLSubClassOfAxiom) {
            return entities.contains(((OWLSubClassOfAxiom) axiom).getSubClass());
        } else if (axiom instanceof OWLDisjointClassesAxiom) {
            return containsOneOf(axiom.getClassesInSignature(), entities);
        }
        return false;
    }

    private <T> boolean containsOneOf(Collection<? extends T> c1, Collection<? extends T> c2) {
        for (T element : c2) {
            if (c1.contains(element)) {
                return true;
            }
        }
        return false;
    }

    private boolean existsInDatabase(OWLAxiom ax) {
        if (ax.getClassesInSignature().contains(factory.getOWLThing())) {
            return true;
        }
        try {
            Axiom axiom = DLLearnerAxiomConvertVisitor.getDLLearnerAxiom(ax);
            PreparedStatement ps = conn.prepareStatement("SELECT axiom FROM evaluation WHERE axiom = ?");
            ps.setString(1, axiom.toManchesterSyntaxString(baseURI, prefixes));
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            ps.close();
            return exists;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private double round(double value) {
        return Math.round(value * 10.) / 10.;
    }

    public String printHTMLTable() throws SQLException {
        StringBuffer sb = new StringBuffer();
        Statement s = conn.createStatement();
        s.executeQuery("SELECT * FROM evaluation");
        java.sql.ResultSet rs = s.getResultSet();
        ResultSetMetaData md = rs.getMetaData();
        int count = md.getColumnCount();
        sb.append("<table border=1>");
        sb.append("<tr>");
        for (int i = 1; i <= count; i++) {
            sb.append("<th>");
            sb.append(md.getColumnLabel(i));
            sb.append("</th>");
        }
        sb.append("</tr>");
        while (rs.next()) {
            sb.append("<tr>");
            for (int i = 1; i <= count; i++) {
                sb.append("<td>");
                sb.append(rs.getString(i));
                sb.append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        rs.close();
        s.close();
        return sb.toString();
    }

    private boolean isEntailed(EvaluatedAxiom evalAxiom) {
        OWLAxiom axiom = OWLAPIConverter.getOWLAPIAxiom(evalAxiom.getAxiom());
        boolean entailed = reasoner.isEntailed(axiom);
        return entailed;
    }

    /**
	 * Loads DBpedia ontology from remote URL and initializes the reasoner.
	 */
    private void loadDBpediaOntology() {
        try {
            URL url = new URL("http://downloads.dbpedia.org/3.6/dbpedia_3.6.owl.bz2");
            InputStream is = new BufferedInputStream(url.openStream());
            CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("bzip2", is);
            dbPediaOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(in);
            reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(dbPediaOntology);
            reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CompressorException e) {
            e.printStackTrace();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    private void loadCurrentDBpediaOntology() {
        int limit = 1000;
        int offset = 0;
        String query = "CONSTRUCT {?s ?p ?o.} WHERE {?s ?p ?o} LIMIT %d OFFSET %d";
        Model model = ModelFactory.createDefaultModel();
        QueryExecution qExec;
        Model newModel;
        boolean repeat = true;
        while (repeat) {
            repeat = false;
            qExec = QueryExecutionFactory.sparqlService("http://live.dbpedia.org/sparql", QueryFactory.create(String.format(query, limit, offset)), "http://live.dbpedia.org/ontology");
            newModel = qExec.execConstruct();
            model.add(newModel);
            repeat = newModel.size() > 0;
            offset += limit;
        }
        try {
            dbPediaOntology = convert(model);
            reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(dbPediaOntology);
            reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
            System.out.println(reasoner.getSuperClasses(factory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Actor")), false).getFlattened());
        } catch (TimeOutException e) {
            e.printStackTrace();
        } catch (InconsistentOntologyException e) {
            e.printStackTrace();
        } catch (ReasonerInterruptedException e) {
            e.printStackTrace();
        }
    }

    private void loadCurrentDBpediaOntology2() {
        dbPediaOntology = null;
        try {
            dbPediaOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new FileInputStream(new File("evaluation/currentDBpediaSchema.owl")));
        } catch (OWLOntologyCreationException e1) {
            e1.printStackTrace();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        if (dbPediaOntology == null) {
            System.out.println("Loading schema ...");
            SPARQLReasoner r = new SPARQLReasoner(new SparqlEndpointKS(endpoint));
            dbPediaOntology = convert(r.loadSchema());
            try {
                new File("evaluation").mkdir();
                new File("evaluation/currentDBpediaSchema.owl").createNewFile();
                OWLManager.createOWLOntologyManager().saveOntology(dbPediaOntology, new RDFXMLOntologyFormat(), new FileOutputStream(new File("evaluation/currentDBpediaSchema.owl")));
            } catch (OWLOntologyStorageException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Preparing reasoner ...");
        reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(dbPediaOntology);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
    }

    private OWLOntology convert(Model model) {
        OWLOntology ontology = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            model.write(baos, "RDF/XML");
            ByteArrayInputStream bs = new ByteArrayInputStream(baos.toByteArray());
            ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(bs);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        return ontology;
    }

    public static void main(String[] args) throws Exception {
        EnrichmentEvaluation ee = new EnrichmentEvaluation();
        ee.dropAndCreateTable();
        ee.start();
        ee.printResultsLaTeX();
        Files.createFile(new File("enrichment_eval.html"), ee.printHTMLTable());
        FileAppender app = new FileAppender(new SimpleLayout(), "log/enrichmentEvalErrors.log");
        Logger.getRootLogger().setLevel(Level.ERROR);
        Logger.getRootLogger().removeAllAppenders();
        Logger.getRootLogger().addAppender(app);
    }
}
