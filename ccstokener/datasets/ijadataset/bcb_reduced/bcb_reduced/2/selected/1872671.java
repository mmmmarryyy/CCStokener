package org.s3b.service.nlp;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.foafrealm.XFOAF;
import org.foafrealm.db.SesameDbFace;
import org.openrdf.model.Graph;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.sesame.admin.DummyAdminListener;
import org.openrdf.sesame.config.AccessDeniedException;
import org.openrdf.sesame.config.ConfigurationException;
import org.openrdf.sesame.config.UnknownRepositoryException;
import org.openrdf.sesame.constants.QueryLanguage;
import org.openrdf.sesame.repository.local.LocalRepository;
import org.openrdf.sesame.repository.local.LocalService;
import org.openrdf.sesame.server.SesameServer;
import org.s3b.db.ConfigKeeper;
import org.s3b.db.rdf.Repository;
import org.s3b.stringdict.Statics;

/**
 * 
 * @author Sebastian Ryszard Kruk, Krystian Samp, Mariusz Cygan,
 * TODO Analyze this code and decide about the future of the code
 */
public class DirectQlSearch {

    private static Logger logger = Logger.getLogger("org.jeromedl.service.nlp");

    /**
	 * construct
	 */
    protected static final String CONSTRUCT = "construct";

    /**
	 * When was the last time someone recreated a repository
	 */
    static long lastUpdate = 0;

    /**
	 * time between repository recreations
	 */
    static final Long timeout = 180000L;

    public static String[] performQuery(QueryLanguage ql, String query, String outputType, String serialization) {
        recreateRepository();
        String urlToCall = buildQueryURL(ql, query, outputType, serialization);
        if (urlToCall == null) return null;
        String[] output = callURL(urlToCall);
        return output;
    }

    public static String[] performQuery(String ql, String query, String outputType, String serialization) {
        String _ql = ql.toLowerCase();
        QueryLanguage queryLanguage = QueryLanguage.SERQL;
        if (QueryLanguage.RQL.toString().toLowerCase().equals(_ql)) queryLanguage = QueryLanguage.RQL; else if (QueryLanguage.RDQL.toString().toLowerCase().equals(_ql)) queryLanguage = QueryLanguage.RDQL;
        return performQuery(queryLanguage, query, outputType, serialization);
    }

    public static Date getDateRepositoryLastCreated() {
        return dateRepositoryLastCreated;
    }

    public static LocalRepository recreateRepository() {
        LocalService localService = SesameServer.getLocalService();
        LocalRepository repository = null;
        if (localService == null) {
            logger.log(Level.WARNING, "can't get LocalService from SesameServer");
            return null;
        }
        try {
            repository = (LocalRepository) localService.getRepository(JOINED_REPOSITORY_NAME);
            logger.log(Level.INFO, "repository found");
        } catch (UnknownRepositoryException ure) {
            logger.log(Level.INFO, "there is no " + JOINED_REPOSITORY_NAME + " repository");
        } catch (ConfigurationException ce) {
            logger.log(Level.WARNING, "repository was not configured properly");
        }
        synchronized (timeout) {
            if (lastUpdate + timeout < new Date().getTime()) {
                logger.log(Level.INFO, "recreating repository");
                if (repository == null) return null;
                try {
                    repository.clear(new DummyAdminListener());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "can't clear repository");
                    e.printStackTrace();
                }
                LocalRepository jeromeRepository = Repository.JEROMEDL_REPOSITORY.getLocalRepository();
                LocalRepository foafrealmRepository = SesameDbFace.getDbFace().getRepository();
                try {
                    repository.addGraph(jeromeRepository.getGraph());
                    repository.addGraph(foafrealmRepository.getGraph());
                } catch (AccessDeniedException ade) {
                    logger.log(Level.WARNING, "can't get jerome or foafrealm graph from repository");
                    return null;
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "IO error");
                    return null;
                }
                repository = filterRepository(repository);
                lastUpdate = new Date().getTime();
            }
        }
        return repository;
    }

    private static final String JOINED_REPOSITORY_NAME = "joined-repository";

    private static Date dateRepositoryLastCreated;

    private static LocalRepository filterRepository(LocalRepository repository) {
        Graph graph = null;
        try {
            graph = repository.getGraph();
        } catch (AccessDeniedException ade) {
            logger.log(Level.WARNING, "can't get graph from repository");
            return null;
        }
        URIImpl[] predicatesToRemove = new URIImpl[1];
        predicatesToRemove[0] = new URIImpl(XFOAF.password_sha1sum);
        for (URIImpl predicate : predicatesToRemove) {
            graph.remove(null, predicate, null);
        }
        dateRepositoryLastCreated = new Date();
        return repository;
    }

    private static String stringFromInputStream(InputStream stream) {
        if (stream == null) return null;
        StringBuilder sb = new StringBuilder();
        try {
            int c = stream.read();
            if (c == -1) return null;
            do {
                sb.append((char) c);
            } while ((c = stream.read()) != -1);
        } catch (IOException ioe) {
            logger.log(Level.INFO, "error with input stream when reading");
            return sb.toString();
        }
        return sb.toString();
    }

    public static String getQueryType(String query) {
        if (query == null || query.trim().length() == 0) return null;
        String prefix = null;
        try {
            prefix = query.substring(0, 9);
        } catch (IndexOutOfBoundsException e) {
            try {
                prefix = query.substring(0, 6);
            } catch (IndexOutOfBoundsException ee) {
                return null;
            }
        }
        prefix = prefix.toLowerCase();
        if (prefix.startsWith(Statics.SELECT)) return Statics.SELECT; else if (prefix.startsWith(CONSTRUCT)) return CONSTRUCT; else return null;
    }

    private static String buildQueryURL(QueryLanguage ql, String query, String outputType, String serialization) {
        if (ql == null || query == null) return null;
        String prefix = getQueryType(query);
        if (prefix == null) return null;
        String _outputType = outputType;
        if (_outputType == null || _outputType.trim().length() == 0) _outputType = "html";
        String urlToCall = ConfigKeeper.getProperty("jeromedl.sesame.serviceURL");
        if (!urlToCall.endsWith("/")) urlToCall += "/";
        urlToCall += "servlets/";
        urlToCall += "evaluate";
        if (Statics.SELECT.equals(prefix)) urlToCall += "Table"; else if ("construct".equals(prefix)) urlToCall += "Graph"; else return null;
        urlToCall += "Query?";
        urlToCall += "queryLanguage=";
        if (ql == QueryLanguage.RQL) urlToCall += QueryLanguage.RQL; else if (ql == QueryLanguage.RDQL) urlToCall += QueryLanguage.RDQL; else urlToCall += QueryLanguage.SERQL;
        urlToCall += "&";
        urlToCall += "repository=" + JOINED_REPOSITORY_NAME + "&";
        urlToCall += "resultFormat=" + _outputType;
        if ("rdf".equals(_outputType) && serialization != null && serialization.trim().length() != 0) urlToCall += "&serialization=" + serialization;
        urlToCall += "&";
        try {
            urlToCall += "query=" + URLEncoder.encode(query, "US-ASCII");
        } catch (UnsupportedEncodingException uee) {
            logger.log(Level.WARNING, "unsupported encoding used to encode url string");
            return null;
        }
        return urlToCall;
    }

    private static String[] callURL(String urlToCall) {
        String[] output = new String[2];
        try {
            logger.log(Level.INFO, "servlet url = " + urlToCall);
            URL url = new URL(urlToCall);
            URLConnection urlConnection = url.openConnection();
            InputStream is = null;
            if (((HttpURLConnection) urlConnection).getResponseCode() != 200) {
                is = ((HttpURLConnection) urlConnection).getErrorStream();
            } else {
                is = urlConnection.getInputStream();
            }
            output[0] = stringFromInputStream(is);
            output[1] = urlConnection.getContentType();
            if (is != null) is.close();
        } catch (MalformedURLException me) {
            logger.log(Level.WARNING, "url is malformed (url=" + urlToCall + ")");
            return null;
        } catch (UnsupportedEncodingException uee) {
            logger.log(Level.WARNING, "unsupported encoding used to encode url string");
            return null;
        } catch (IOException e) {
            logger.log(Level.WARNING, "io exception when invoking sesame servlet (url=" + urlToCall + ")");
            return null;
        }
        return output;
    }

    public static void main(String[] args) {
        System.out.println(QueryLanguage.RQL);
    }
}
