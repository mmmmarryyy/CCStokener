package org.linkedgeodata.rest;

import java.awt.geom.Point2D;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.aksw.commons.sparql.api.cache.extra.CacheCoreEx;
import org.aksw.commons.sparql.api.cache.extra.CacheCoreH2;
import org.aksw.commons.sparql.api.cache.extra.CacheEx;
import org.aksw.commons.sparql.api.cache.extra.CacheExImpl;
import org.aksw.commons.sparql.api.core.QueryExecutionFactory;
import org.aksw.commons.sparql.api.delay.extra.Delayer;
import org.aksw.commons.sparql.api.delay.extra.DelayerDefault;
import org.aksw.commons.sparql.api.http.QueryExecutionFactoryHttp;
import org.aksw.commons.util.strings.StringUtils;
import org.hibernate.engine.jdbc.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

@Path("/api/3/")
public class RestApi {

    private static final Logger logger = LoggerFactory.getLogger(RestApi.class);

    private static LGDVocab vocab = new LGDVocabDefault();

    private QueryExecutionFactory<?> qeFactory;

    private Map<String, String> prefixMap = new HashMap<String, String>();

    public RestApi() throws ClassNotFoundException, SQLException {
        String service = "http://test.linkedgeodata.org/sparql";
        QueryExecutionFactory<?> tmp = new QueryExecutionFactoryHttp(service);
        CacheCoreEx cacheBackend = CacheCoreH2.create("sparql", 24l * 60l * 60l * 1000l, true);
        CacheEx cacheFrontend = new CacheExImpl(cacheBackend);
        qeFactory = tmp;
    }

    private Model createModel() {
        Model result = ModelFactory.createDefaultModel();
        result.setNsPrefixes(prefixMap);
        return result;
    }

    @GET
    @Path("/node/{id}")
    public Model describeNode(@PathParam("id") long id) {
        return describe("http://linkedgeodata.org/triplify/node" + id);
    }

    @GET
    @Path("/way/{id}")
    public Model describeWay(@PathParam("id") long id) {
        return describe("http://linkedgeodata.org/triplify/way" + id);
    }

    @GET
    @Path("/relation/{id}")
    public Model describeRelation(@PathParam("id") long id) {
        return describe("http://linkedgeodata.org/triplify/relation" + id);
    }

    @Path("/intersects/{yMin}-{yMax},{xMin}-{xMax}/{className}")
    public Model nearClass(@PathParam("xMin") double xMin, @PathParam("xMax") double xMax, @PathParam("yMin") double yMin, @PathParam("yMax") double yMax, @PathParam("className") String className) {
        Point2D.Double a = new Point2D.Double(xMin, yMin);
        Point2D.Double b = new Point2D.Double(xMax, yMax);
        String polygon = "POLYGON((" + a.x + " " + a.y + "," + b.x + " " + a.y + "," + b.x + " " + b.y + "," + a.x + " " + b.y + "," + a.x + " " + a.y + "))";
        String filter = "Filter(ogc:intersects(?geo, ogc:geomFromText('" + polygon + "')))";
        String typeTriple = "";
        if (className != null) {
            typeTriple = "?s a <http://linkedgeodata.org/ontology/" + className + "> . ";
        }
        String query = "Prefix geom:<http://geovocab.org/geometry#> Prefix ogc:<http://www.opengis.net/rdf#> Construct { ?s ?p ?o } { " + typeTriple + " ?s geom:geometry ?x . ?x ogc:asWKT ?geo . ?s ?p ?o . " + filter + "} Limit 1000";
        logger.debug(query);
        Model result = qeFactory.createQueryExecution(query).execConstruct();
        return result;
    }

    @GET
    @Path("/intersects/{yMin}-{yMax},{xMin}-{xMax}/")
    public Model nearBasic(@PathParam("xMin") double xMin, @PathParam("xMax") double xMax, @PathParam("yMin") double yMin, @PathParam("yMax") double yMax) {
        return nearClass(xMin, xMax, yMin, yMax, null);
    }

    @GET
    @Path("/ontology/")
    public Model getOntology() throws Exception {
        String query = "Construct { ?s ?p ?o } { ?s a <" + OWL.Class.toString() + "> . ?s ?p ?o . }";
        Model result = qeFactory.createQueryExecution(query).execConstruct();
        return result;
    }

    class JsonResponseItem {

        String osm_type;

        long osm_id;

        public JsonResponseItem() {
        }

        public String getOsm_type() {
            return osm_type;
        }

        public void setOsm_type(String osm_type) {
            this.osm_type = osm_type;
        }

        public long getOsm_id() {
            return osm_id;
        }

        public void setOsm_id(long osm_id) {
            this.osm_id = osm_id;
        }
    }

    private Delayer delayer = new DelayerDefault(1000);

    @GET
    @Path("/geocode/")
    public Model geocode(@QueryParam("q") String queryString) throws Exception {
        delayer.doDelay();
        String geocodeService = "http://open.mapquestapi.com/nominatim/v1/search";
        String uri = geocodeService + "?format=json&q=" + queryString;
        URL url = new URL(uri);
        URLConnection c = url.openConnection();
        c.setRequestProperty("User-Agent", "http://linkedgeodata.org, mailto:cstadler@informatik.uni-leipzig.de");
        InputStream ins = c.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamUtils.copy(ins, out);
        String json = out.toString();
        Gson gson = new Gson();
        Type collectionType = new TypeToken<Collection<JsonResponseItem>>() {
        }.getType();
        Collection<JsonResponseItem> items = gson.fromJson(json, collectionType);
        List<Resource> resources = new ArrayList<Resource>();
        for (JsonResponseItem item : items) {
            Resource resource = null;
            if (item.getOsm_type().equals("node")) {
                resource = vocab.createNIRNodeURI(item.getOsm_id());
            } else if (item.getOsm_type().equals("way")) {
                resource = vocab.createNIRWayURI(item.getOsm_id());
            } else {
                continue;
            }
            resources.add(resource);
        }
        Model result = createModel();
        String lgdService = "http://test.linkedgeodata.org/sparql";
        QueryExecutionFactoryHttp qef = new QueryExecutionFactoryHttp(lgdService, Collections.singleton("http://linkedgeodata.org"));
        for (Resource resource : resources) {
            String serviceUri = "http://test.linkedgeodata.org/sparql?format=text%2Fplain&default-graph-uri=http%3A%2F%2Flinkedgeodata.org&query=DESCRIBE+<" + StringUtils.urlEncode(resource.toString()) + ">";
            URL serviceUrl = new URL(serviceUri);
            URLConnection conn = serviceUrl.openConnection();
            conn.addRequestProperty("Accept", "text/plain");
            InputStream in = null;
            try {
                in = conn.getInputStream();
                result.read(in, null, "N-TRIPLE");
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
        return result;
    }

    public Model describe(String uri) {
        String query = "Construct { ?s ?p ?o . } { ?s ?p ?o . Filter(?s = <" + uri + ">) . }";
        return qeFactory.createQueryExecution(query).execConstruct();
    }
}
