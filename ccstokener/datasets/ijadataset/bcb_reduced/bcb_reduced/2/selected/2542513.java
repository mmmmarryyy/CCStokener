package org.wsml.reasoner.ext.sql;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.omwg.logicalexpression.LogicalExpression;
import org.omwg.logicalexpression.terms.Term;
import org.omwg.ontology.Ontology;
import org.omwg.ontology.Variable;
import org.wsml.reasoner.api.LPReasoner;
import org.wsml.reasoner.api.WSMLReasonerFactory;
import org.wsml.reasoner.api.inconsistency.InconsistencyException;
import org.wsml.reasoner.impl.DefaultWSMLReasonerFactory;
import org.wsml.reasoner.impl.WSMO4JManager;
import org.wsmo.common.TopEntity;
import org.wsmo.common.exception.InvalidModelException;
import org.wsmo.factory.Factory;
import org.wsmo.factory.LogicalExpressionFactory;
import org.wsmo.wsml.Parser;
import org.wsmo.wsml.ParserException;

/**
 * This class encapsulates the reasoner and performs necessary pre-processing.
 */
public class WSMLReasonerFacade {

    /**
     * Constructs a new WSMLReasonerFacade by setting up WSMO4J resources, the
     * parser, the reasoner.
     */
    public WSMLReasonerFacade() {
        manager = new WSMO4JManager();
        parser = Factory.createParser(null);
        reasoner = DefaultWSMLReasonerFactory.getFactory().createFlightReasoner(null);
    }

    /**
     *  Constructs a new WSMLReasonerFacade by setting up WSMO4j resources, the parser 
     *  and used a predefined reasoner.
     * @param reasoner The reasoner to use.
     */
    public WSMLReasonerFacade(LPReasoner reasoner) {
        manager = new WSMO4JManager();
        parser = Factory.createParser(null);
        this.reasoner = reasoner;
    }

    public ReasonerResult executeWsmlQuery(String query) throws InconsistencyException, IOException, InvalidModelException, ParserException {
        if (query == null) {
            throw new IllegalArgumentException();
        }
        LogicalExpressionFactory leFactory = manager.getLogicalExpressionFactory();
        LogicalExpression lequery = leFactory.createLogicalExpression(query);
        return doQuery(lequery);
    }

    /**
     * Sets the reasoner object that should be used internally.
     * 
     * @param reasoner The reasoner
     */
    public void setReasoner(LPReasoner reasoner) {
        this.reasoner = reasoner;
    }

    /**
     * Executes a WSML query against a certain ontology.
     * 
     * @param query
     *            the query to be answered.
     * @param ontologyIRI
     *            an IRI pointing to the ontology to be used
     * @return the query results
     * @throws ParserException
     * @throws InconsistencyException
     * @throws InvalidModelException
     * @throws IOException
     */
    public ReasonerResult executeWsmlQuery(String query, String ontologyIRI) throws ParserException, InconsistencyException, IOException, InvalidModelException {
        if (ontologyIRI == null || query == null) {
            throw new IllegalArgumentException();
        }
        onto = loadOntology(ontologyIRI);
        if (onto == null) {
            return null;
        }
        reasoner.registerOntology(onto);
        LogicalExpressionFactory leFactory = manager.getLogicalExpressionFactory();
        LogicalExpression lequery = leFactory.createLogicalExpression(query, onto);
        return doQuery(lequery);
    }

    /**
     * Returns the loaded ontology.
     * 
     * @return
     */
    public Ontology getOntology() {
        return onto;
    }

    protected ReasonerResult doQuery(LogicalExpression lequery) {
        Set<Map<Variable, Term>> queryResult = reasoner.executeQuery(lequery);
        ReasonerResult reasonerResult = new ReasonerResult(queryResult);
        return reasonerResult;
    }

    /**
     * Loads an ontology given an IRI.
     * 
     * @param ontologyIri
     *            an IRR pointed to the ontology location. E.g. on the local
     *            file system, on the web, ...
     * @return the loaded ontology
     * @throws InvalidModelException
     * @throws ParserException
     * @throws IOException
     */
    protected Ontology loadOntology(String ontologyIri) throws IOException, ParserException, InvalidModelException {
        assert ontologyIri != null;
        URL url = null;
        Ontology ontology = null;
        url = new URL(ontologyIri);
        InputStream is = url.openStream();
        TopEntity[] identifiable = parser.parse(new InputStreamReader(is));
        if (identifiable.length > 0 && identifiable[0] instanceof Ontology) {
            ontology = ((Ontology) identifiable[0]);
        }
        return ontology;
    }

    private WSMO4JManager manager;

    private Parser parser;

    private LPReasoner reasoner;

    private Ontology onto;
}
