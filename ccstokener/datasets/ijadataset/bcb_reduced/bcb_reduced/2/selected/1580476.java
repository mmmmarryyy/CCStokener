package org.foafrealm.manage.sioc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.foafrealm.XFOAF_SSCF;
import org.foafrealm.db.RdfQuery;
import org.foafrealm.db.SesameDbFace;
import org.foafrealm.manage.BookmarksHelper;
import org.foafrealm.manage.Person;
import org.foafrealm.manage.PersonFactory;
import org.openrdf.model.Graph;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.sesame.admin.StdOutAdminListener;
import org.openrdf.sesame.config.AccessDeniedException;
import org.openrdf.sesame.config.ConfigurationException;
import org.openrdf.sesame.constants.QueryLanguage;
import org.openrdf.sesame.constants.RDFFormat;
import org.openrdf.sesame.query.MalformedQueryException;
import org.openrdf.sesame.query.QueryEvaluationException;
import org.openrdf.sesame.query.QueryResultsTable;
import org.openrdf.sesame.repository.local.LocalRepository;
import org.openrdf.sesame.server.SesameServer;
import org.openrdf.vocabulary.RDF;

/**
 * This class is for handling bookmarking SIOC data. 
 * 
 * @author Jarosław Dobrzański
 *
 */
public class SiocBookmarks {

    /** 
	 * full URI of current user's SIOC directory 
	 * (in his private bookshelf)  
	 */
    @SuppressWarnings("unused")
    private String ownerSiocDirUri = null;

    /** gives access to the DB */
    private SesameDbFace dbFace = null;

    /** SIOC directory's (in private bookshelf) URI (the begining)*/
    private static final String dirUri = "http://sioc.dir.uri/";

    /** SIOC directory's (in private bookshelf) label */
    private static final String dirLabel = "SIOC";

    /** SIOC directory's (in private bookshelf) comment */
    private static final String dirComment = "SIOC";

    /** Map containing instances of that class. The key is ownerMbox */
    private static final Map<String, SiocBookmarks> MAP_SIOCBOOKMARKS = new HashMap<String, SiocBookmarks>();

    private SiocBookmarks(String _ownerSiocDirUri) {
        this.dbFace = SesameDbFace.getDbFace();
        this.ownerSiocDirUri = _ownerSiocDirUri;
    }

    /**
	 * Gets either the existing instance of SiocBookmarks for given user, 
	 * or if it doesn't exist gets a new one, which also is created then. 
	 * 
	 * @param ownerMbox
	 * @return Instance of SiocBookmarks for given user
	 */
    public static SiocBookmarks getInstace(String ownerMbox) {
        SiocBookmarks sb = null;
        synchronized (MAP_SIOCBOOKMARKS) {
            sb = MAP_SIOCBOOKMARKS.get(ownerMbox);
            if (sb == null) {
                sb = new SiocBookmarks(getSiocDir(ownerMbox));
                MAP_SIOCBOOKMARKS.put(ownerMbox, sb);
            }
        }
        return sb;
    }

    /**
	 * Gets the name of the sioc direciry (the one in which sioc bookmarks aresotred).
	 * If the directory doesn't exist it's created. 
	 * 
	 * @param ownerMbox
	 * @return String representation of current user's sioc dir 
	 */
    public static String getSiocDir(String ownerMbox) {
        QueryResultsTable results = SesameDbFace.getDbFace().performTableQuery(RdfQuery.RDF_GET_USER_SIOC_DIR.toString(), XFOAF_SSCF.Directory, dirLabel, ownerMbox);
        if (results.getRowCount() > 0 && results.getValue(0, 0) != null) {
            return results.getValue(0, 0).toString();
        }
        Person person = PersonFactory.getPersonIfExists(ownerMbox, null);
        if (person == null) {
            person = PersonFactory.getPerson(ownerMbox, null, false);
        }
        BookmarksHelper bh = new BookmarksHelper(dirUri);
        String uri = bh.createResource(dirUri, dirLabel, dirComment, person, true);
        Graph graph = SesameDbFace.getDbFace().getGraph();
        graph.add(new StatementImpl(graph.getValueFactory().createURI(uri), graph.getValueFactory().createURI(XFOAF_SSCF.isIn), graph.getValueFactory().createURI(ownerMbox)));
        return uri;
    }

    /**
	 * Adds a sioc data with given url to current user's sioc directory, 
	 * which is kept in his/her provete bookshelf.  

	 * @param siocDataUri String representation of the URL determining the type of SIOC data (value taken from SIOC namespace)
	 * @param url URL of the SIOC data to be saved. 
	 * 		The HTTP repsponse for that url contains all information about that sioc data saved in the RDF type.   
	 */
    public void bookmarkSiocData(String siocDataUri, String url) {
        try {
            StringBuilder sb = new StringBuilder();
            getRdfResponse(sb, url);
            LocalRepository tmpRepository = SesameServer.getLocalService().createRepository(String.valueOf(this.hashCode()), false);
            tmpRepository.addData(sb.toString(), url, RDFFormat.RDFXML, true, new StdOutAdminListener());
            Graph siocDataGraph = tmpRepository.performGraphQuery(QueryLanguage.SERQL, String.format(RdfQuery.RDF_DESIRED_SIOCDATA_FROM_RESPONSE.toString(), siocDataUri));
            Collection statements = siocDataGraph.getStatementCollection(null, siocDataGraph.getValueFactory().createURI(RDF.TYPE), siocDataGraph.getValueFactory().createURI(siocDataUri));
            List<String> postExludedPredicates = null;
            if (siocDataUri.equals(SIOC.Post)) {
                postExludedPredicates = new ArrayList<String>();
                postExludedPredicates.add(SIOC.contentEncoded);
                postExludedPredicates.add(SIOC.postedBy);
            }
            for (Iterator iter = statements.iterator(); iter.hasNext(); ) {
                Statement stmt = (Statement) iter.next();
                Graph g = tmpRepository.performGraphQuery(QueryLanguage.SERQL, String.format(RdfQuery.RDF_GET_DESIRED_SIOCDATA_DETAILS.toString(), stmt.getSubject().toString()));
                if (siocDataUri.equals(SIOC.Post)) {
                    Collection ss = g.getStatementCollection(null, null, null);
                    for (Object o : ss) {
                        Statement s = (Statement) o;
                        for (String exclPred : postExludedPredicates) {
                            if (exclPred.equals(s.getPredicate().toString())) {
                                g.remove(s);
                            }
                        }
                    }
                }
                siocDataGraph.add(g);
                siocDataGraph.add(new StatementImpl(stmt.getSubject(), dbFace.getGraph().getValueFactory().createURI(XFOAF_SSCF.isIn), dbFace.getGraph().getValueFactory().createURI(ownerSiocDirUri)));
                break;
            }
            dbFace.getRepository().addGraph(siocDataGraph);
            SesameServer.getLocalService().removeRepository(String.valueOf(this.hashCode()));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        } catch (AccessDeniedException e) {
            e.printStackTrace();
        } catch (MalformedQueryException e) {
            e.printStackTrace();
        } catch (QueryEvaluationException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Get the rdf which is the response for the given url.
	 * The rdf is returned through the sb param.
	 * 
	 * @param sb Holds the rdf response and returns it
	 * @param url
	 */
    private void getRdfResponse(StringBuilder sb, String url) {
        try {
            String inputLine = null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            while ((inputLine = reader.readLine()) != null) {
                sb.append(inputLine);
            }
            reader.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
