package gate.mimir.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.DatatypeConverter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.log4j.Logger;
import gate.mimir.AbstractSemanticAnnotationHelper;
import gate.mimir.Constraint;
import gate.mimir.ConstraintType;
import gate.mimir.SemanticAnnotationHelper;
import gate.mimir.index.Indexer;
import gate.mimir.index.Mention;
import gate.mimir.search.QueryEngine;
import gate.mimir.util.DelegatingSemanticAnnotationHelper;
import gate.util.GateRuntimeException;

/**
 * A Semantic annotation helper that, at query time, connects to a SPARQL
 * endpoint to obtain a list of candidate URIs that are then passed to the
 * underlying delegate annotation helper.
 */
public class SPARQLSemanticAnnotationHelper extends DelegatingSemanticAnnotationHelper {

    private static final Logger logger = Logger.getLogger(SPARQLSemanticAnnotationHelper.class);

    /**
   * A query fragment that, if set, gets prepended to all SPARQL queries sent 
   * to the end point. This could be used, for example, for setting up a list of 
   * prefixes.
   */
    private String queryPrefix;

    /**
   * A query fragment that, if set, gets appended to all SPARQL queries sent 
   * to the end point. This could be used, for example, for setting up a 
   * LIMIT constraint.
   */
    private String querySuffix;

    /**
   * The name used for the synthetic feature used at query time to supply the
   * SPARQL query.
   */
    public static final String SPARQL_QUERY_FEATURE_NAME = "sparql";

    /**
   * The service endpoint where SPARQL queries are forwarded to.
   */
    private String sparqlEndpoint;

    private transient String sparqlEndpointUser;

    private transient String sparqlEndpointPassword;

    /**
   * HTTP Header used to authenticate with the remote endpoint. If set to 
   * <code>null</code>, then no authentication is done.
   */
    private String authHeader;

    /**
   * See {@link #setQueryPrefix(String)}
   * @return
   */
    public String getQueryPrefix() {
        return queryPrefix;
    }

    /**
   * Sets the query prefix: a query fragment that, if set, gets prepended to 
   * all SPARQL queries sent to the end point. This could be used, for example,
   * for setting up a list of PREFIXes.
   */
    public void setQueryPrefix(String queryPrefix) {
        this.queryPrefix = queryPrefix;
    }

    /**
   * See {@link #setQuerySuffix(String)}.
   * @return
   */
    public String getQuerySuffix() {
        return querySuffix;
    }

    /**
   * Sets the query suffix: a query fragment that, if set, gets appended to 
   * all SPARQL queries sent to the end point. This could be used, for example,
   * for setting up a LIMIT constraint.
   */
    public void setQuerySuffix(String querySuffix) {
        this.querySuffix = querySuffix;
    }

    public String getSparqlEndpoint() {
        return sparqlEndpoint;
    }

    public void setSparqlEndpoint(String sparqlEndpoint) {
        this.sparqlEndpoint = sparqlEndpoint;
    }

    public String getSparqlEndpointUser() {
        return sparqlEndpointUser;
    }

    public void setSparqlEndpointUser(String sparqlEndpointUser) {
        this.sparqlEndpointUser = sparqlEndpointUser;
    }

    public String getSparqlEndpointPassword() {
        return sparqlEndpointPassword;
    }

    public void setSparqlEndpointPassword(String sparqlEndpointPassword) {
        this.sparqlEndpointPassword = sparqlEndpointPassword;
    }

    @Override
    public void init(Indexer indexer) {
        super.init(indexer);
        if (sparqlEndpointUser != null && sparqlEndpointUser.length() > 0) {
            try {
                if (sparqlEndpointPassword == null) sparqlEndpointPassword = "";
                String userPass = sparqlEndpointUser + ":" + sparqlEndpointPassword;
                authHeader = "Basic " + DatatypeConverter.printBase64Binary(userPass.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new UnsupportedCharsetException("UTF-8");
            }
        } else {
            authHeader = null;
        }
        setNominalFeatures(concatenateArrays(getNominalFeatures(), new String[] { SPARQL_QUERY_FEATURE_NAME }));
    }

    @Override
    public List<Mention> getMentions(String annotationType, List<Constraint> constraints, QueryEngine engine) {
        Set<Mention> mentions = new HashSet<Mention>();
        List<Constraint> passThroughConstraints = new ArrayList<Constraint>();
        String query = null;
        String originalQuery = null;
        for (Constraint aConstraint : constraints) {
            if (SPARQL_QUERY_FEATURE_NAME.equals(aConstraint.getFeatureName())) {
                originalQuery = (String) aConstraint.getValue();
                query = (queryPrefix != null ? queryPrefix : "") + originalQuery + (querySuffix != null ? querySuffix : "");
            } else {
                passThroughConstraints.add(aConstraint);
            }
        }
        if (query == null) {
            return delegate.getMentions(annotationType, constraints, engine);
        } else {
            try {
                SPARQLResultSet srs = runQuery(query);
                for (int i = 0; i < srs.getColumnNames().length; i++) {
                    if (srs.getColumnNames()[i].equals("error-message")) {
                        String errorMessage = (srs.getRows().length > 0 && srs.getRows()[0].length > i) ? srs.getRows()[0][i] : null;
                        throw new IllegalArgumentException("Query \"" + originalQuery + "\" resulted in an error" + (errorMessage != null ? (":\n" + errorMessage) : "."));
                    }
                }
                if (srs.getRows() != null) {
                    for (String[] aRow : srs.getRows()) {
                        List<Constraint> delegateConstraints = new ArrayList<Constraint>(passThroughConstraints);
                        for (int i = 0; i < srs.getColumnNames().length; i++) {
                            delegateConstraints.add(new Constraint(ConstraintType.EQ, srs.getColumnNames()[i], aRow[i]));
                        }
                        mentions.addAll(delegate.getMentions(annotationType, delegateConstraints, engine));
                    }
                }
            } catch (IOException e) {
                logger.error("I/O error while communicating with " + "SPARQL endpoint.", e);
                throw new GateRuntimeException("I/O error while communicating with " + "SPARQL endpoint.", e);
            } catch (XMLStreamException e) {
                logger.error("Error parsing results from SPARQL endpoint.", e);
                throw new GateRuntimeException("Error parsing results from SPARQL " + "endpoint.", e);
            }
            return new ArrayList<Mention>(mentions);
        }
    }

    /**
   * Runs a query against the SPARQL endpoint and returns the results.
   * 
   * @param query
   * @return
   * @throws XMLStreamException
   */
    protected SPARQLResultSet runQuery(String query) throws IOException, XMLStreamException {
        try {
            String urlStr = sparqlEndpoint + "?query=" + URLEncoder.encode(query, "UTF-8");
            URL url = new URL(urlStr);
            URLConnection urlConn = url.openConnection();
            urlConn.setRequestProperty("Accept", "application/sparql-results+xml");
            if (authHeader != null) {
                urlConn.setRequestProperty("Authorization", authHeader);
            }
            return new SPARQLResultSet(urlConn.getInputStream());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported by this JVM");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL - have you set the correct " + "SPARQL endpoint?", e);
        }
    }
}
