package org.corrib.s3b.sscf.manage.sioc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.corrib.s3b.sscf.S3B_SSCF;
import org.corrib.s3b.sscf.manage.BookmarksHelper;
import org.corrib.s3b.sscf.manage.XfoafSscfResource;
import org.foafrealm.db.DbFace;
import org.foafrealm.db.RdfQuery;
import org.foafrealm.manage.Person;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.ModelSet;
import org.ontoware.rdf2go.model.QueryResultTable;
import org.ontoware.rdf2go.model.QueryRow;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.vocabulary.RDF;

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
    private String ownerSiocDirUri = null;

    /** gives access to the DB */
    private ModelSet model = null;

    /** SIOC directory's (in private bookshelf) URI (the begining)*/
    private static final String dirUri = "http://sioc.dir.uri/";

    /** SIOC directory's (in private bookshelf) label */
    private static final String dirLabel = "SIOC";

    /** SIOC directory's (in private bookshelf) comment */
    private static final String dirComment = "SIOC";

    /** Map containing instances of that class. The key is ownerMbox */
    private static final Map<String, SiocBookmarks> MAP_SIOCBOOKMARKS = new HashMap<String, SiocBookmarks>();

    private SiocBookmarks(String _ownerSiocDirUri) {
        this.model = DbFace.getModel();
        this.ownerSiocDirUri = _ownerSiocDirUri;
    }

    private SiocBookmarks(String _ownerSiocDirUri, ModelSet _model) {
        if (_model != null) this.model = _model; else this.model = DbFace.getModel();
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
        return SiocBookmarks.getInstace(ownerMbox, null);
    }

    /**
	 * Gets either the existing instance of SiocBookmarks for given user, 
	 * or if it doesn't exist gets a new one, which also is created then. 
	 * 
	 * @param ownerMbox
	 * @param dbmodel
	 * 
	 * @return Instance of SiocBookmarks for given user
	 */
    public static SiocBookmarks getInstace(String ownerMbox, ModelSet _model) {
        SiocBookmarks sb = null;
        synchronized (MAP_SIOCBOOKMARKS) {
            sb = MAP_SIOCBOOKMARKS.get(ownerMbox);
            if (sb == null) {
                sb = new SiocBookmarks(getSiocDir(ownerMbox), _model);
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
        return getSiocDir(ownerMbox, null);
    }

    /**
	 * Gets the name of the sioc direciry (the one in which sioc bookmarks aresotred).
	 * If the directory doesn't exist it's created. 
	 * 
	 * @param ownerMbox
	 * @return String representation of current user's sioc dir 
	 */
    public static String getSiocDir(String ownerMbox, ModelSet _model) {
        ModelSet innerModel = null;
        if (_model != null) innerModel = _model; else innerModel = DbFace.getModel();
        try {
            QueryResultTable results = innerModel.querySelect(RdfQuery.RDF_GET_USER_SIOC_DIR.toString(S3B_SSCF.Directory, dirLabel, ownerMbox), "SPARQL");
            ClosableIterator<QueryRow> it = results.iterator();
            if (it.hasNext()) {
                QueryRow qr = it.next();
                if (qr.getValue(results.getVariables().get(0)) != null) return qr.getValue(results.getVariables().get(0)).toString();
            }
        } catch (Exception e) {
        }
        Person person = null;
        BookmarksHelper bh = new BookmarksHelper(dirUri);
        String uri = bh.createResource(dirUri, dirLabel, dirComment, person, true);
        try {
            innerModel.addStatement(null, innerModel.createURI(uri), innerModel.createURI(S3B_SSCF.isIn), innerModel.createURI(ownerMbox));
        } catch (Exception e) {
            XfoafSscfResource xfsr = XfoafSscfResource.getXfoafSscfResource(uri, innerModel);
            if (xfsr != null) bh.removeResource(xfsr, person);
        }
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
        Model tempModel = null;
        try {
            StringBuilder sb = new StringBuilder();
            getRdfResponse(sb, url);
            tempModel = DbFace.getTempModel();
            tempModel.readFrom(new StringReader(sb.toString()));
            ClosableIterator<Statement> statements = tempModel.findStatements(Variable.ANY, RDF.type, tempModel.createURI(siocDataUri));
            List<String> postExludedPredicates = null;
            if (siocDataUri.equals(SIOC.Post)) {
                postExludedPredicates = new ArrayList<String>();
                postExludedPredicates.add(SIOC.contentEncoded);
                postExludedPredicates.add(SIOC.postedBy);
            }
            while (statements.hasNext()) {
                Statement stmt = statements.next();
                if (siocDataUri.equals(SIOC.Post)) {
                    ClosableIterator<Statement> ss = tempModel.findStatements(stmt.getSubject(), null, null);
                    while (ss.hasNext()) {
                        Statement s = ss.next();
                        if (postExludedPredicates != null) for (String exclPred : postExludedPredicates) {
                            if (exclPred.equals(s.getPredicate().toString())) {
                                tempModel.removeStatement(s);
                            }
                        }
                    }
                    ss.close();
                }
                tempModel.addStatement(stmt.getSubject(), tempModel.createURI(S3B_SSCF.isIn), tempModel.createURI(ownerSiocDirUri));
                break;
            }
            model.addModel(tempModel);
            tempModel = null;
            statements.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            tempModel = null;
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
