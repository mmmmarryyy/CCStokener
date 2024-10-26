package org.charvolant.tmsnet.resources;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import org.charvolant.tmsnet.AbstractModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.reasoner.ValidityReport.Report;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.util.Locator;
import com.hp.hpl.jena.util.TypedStream;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Locate various resources via RDF.
 * <p>
 * This class builds an ontology model for the resources such
 * as ratings, channel information, etc. etc. that are associated
 * with the TMSNet system. 
 * <p>
 * A {@link FileManager} with suitable redirections maps
 * resource URIs onto RDF files. To do this, it uses the
 * private {@link LocatorExtURL} class to add extensions onto plain
 * URIs and then see whether there is a local version of the file.
 * <p>
 * TODO At the moment, resources are available
 * from the pre-loaded files. Eventually, this will be replaced
 * by a more sophisticated local cache system.
 * 
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
public class ResourceLocator extends AbstractModel {

    /** The logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(ResourceLocator.class);

    /** The base TMSNet URI */
    public static final String BASE_URI = "http://purl.org/tmsnet/";

    /** The base recorder URI */
    public static final String RECORDER_URI = "tmsnet://";

    /** The base remote file URI */
    public static final String FILE_URI = "ftp://";

    /** The DVBT transmitter URI (represents a non-specific DVBT station) */
    public static final String DVBT_TRANSMITTER_URI = BASE_URI + "networks/transmitters/dvbt";

    /** The SCART connector URI  */
    public static final String SCART_URI = BASE_URI + "devices/connectors/SCART";

    /** The basic product URI  */
    public static final String PRODUCT_URI = BASE_URI + "devices/products/topfield/TRF7160";

    /** The date format for dates only */
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /** The local URI */
    public static final String LOCAL_URI = "local://";

    /** The extension to add for rdf */
    public static final String RDF_EXT = "rdf";

    /** The SPARQL prefixes for queries. Includes {@link TMSNet} and {@link RDF}.<p>{@value} */
    public static final String SPARQL_PREFIXES = "prefix tms: <" + TMSNet.getURI() + "> " + "prefix dcterms: <" + DCTerms.getURI() + "> " + "prefix dc: <" + DC.getURI() + "> " + "prefix po: <" + PO.getURI() + "> " + "prefix event: <" + NetEvent.getURI() + "> " + "prefix timeline: <" + Timeline.getURI() + "> " + "prefix rdf: <" + RDF.getURI() + "> ";

    /** 
   * The SPARQL query for finding a channel corresponding to a network id and channel.
   * <p>
   * Originating network ids should also be handled by the ontology and reasoner.
   * <p>
   * {@value}
   */
    public static final String FINDCHANNEL_QUERY_1 = SPARQL_PREFIXES + "select ?channel where { " + "?network a tms:Network. " + "?network tms:networkId $nid. " + "?channel a tms:Channel. " + "?channel tms:logicalChannelNumber $chno. " + "?channel tms:network ?network." + "}";

    /** 
   * The SPARQL query for finding a channel corresponding to a parent network id and channel.
   * <p>
   * {@value}
   */
    public static final String FINDCHANNEL_QUERY_2 = SPARQL_PREFIXES + "select ?channel where { " + "?channel a tms:Channel. " + "?channel tms:logicalChannelNumber $chno. " + "?channel tms:network ?subnetwork." + "?subnetwork a tms:Network. " + "?subnetwork tms:parentNetwork ?network. " + "?network a tms:Network. " + "?network tms:networkId $nid. " + "}";

    /** 
   * The SPARQL query for finding a rating corresponding to a supplied age.
   * <p>
   * {@value}
   */
    public static final String FINDRATING_QUERY = SPARQL_PREFIXES + "select ?rating where { " + "?rating a tms:Rating. " + "?rating tms:system $system. " + "?rating tms:minimumAge ?minAge. " + "filter (?minAge >= $age) " + "} order by ?minAge";

    /** 
   * The SPARQL query for finding a icon of the right size.
   * <p>
   * {@value}
   */
    public static final String FINDICON_QUERY = SPARQL_PREFIXES + "select ?icon where { " + "$resource tms:logo ?icon. " + "?icon a tms:Logo. " + "?icon dcterms:format <http://purl.org/tmsnet/formats/png>. " + "?icon tms:height $size. " + "?icon tms:width $size. " + "}";

    /** 
   * The SPARQL query for finding a parent entity.
   * <p>
   * {@value}
   */
    public static final String FINDPARENT_QUERY = SPARQL_PREFIXES + "select ?parent where { " + "$resource dcterms:isPartOf ?parent. " + "}";

    /** 
   * The SPARQL query for finding a icon of the right size.
   * <p>
   * {@value}
   */
    public static final String FINDTRANSMITTER_QUERY = SPARQL_PREFIXES + "select ?transmitter where { " + "?transmitter a tms:Transmitter. " + "?transmitter tms:label $label. " + "}";

    /** 
   * The SPARQL query for finding a brand with a suitable title.
   * <p>
   * {@value}
   */
    public static final String FINDBRAND_QUERY = SPARQL_PREFIXES + "select ?brand where { " + "?programme a po:Brand. " + "?programme dc:title $title. " + "}";

    /** 
   * The SPARQL query for finding an enumeration resource.
   * <p>
   * {@value}
   */
    public static final String FINDENUM_QUERY = SPARQL_PREFIXES + "select ?resource where { " + "?resource tms:java $enum. " + "}";

    /** 
   * The SPARQL query for finding a program with a suitable title.
   * <p>
   * {@value}
   */
    public static final String FINDEPISODE_QUERY = SPARQL_PREFIXES + "select ?episode where { " + "?episode a po:Episode. " + "?programme dc:description $description. " + "$brand po:episode ?episode. " + "}";

    /** 
   * The SPARQL query for finding a broadcast that is overlapped by an interval.
   * <p>
   * {@value}
   */
    public static final String FINDBROADCAST_QUERY = SPARQL_PREFIXES + "select ?broadcast where { " + "?broadcast a po:Broadcast. " + "?broadcast po:broadcast_on $channel. " + "?broadcast event:time ?btime. " + "?btime timeline:start ?bstart. " + "?btime timeline:end ?bend. " + "$interval timeline:start ?istart. " + "$interval timeline:end ?iend. " + "filter (?istart <= ?bstart && ?iend >= ?bend)." + "}";

    /** 
   * The SPARQL query for deleting a timer resources.
   * <p>
   * {@value}
   */
    public static final String DELETEPENDINGRECORDINGS_QUERY = SPARQL_PREFIXES + "delete { ?s ?v ?o} where {" + "  { " + "   ?s a tms:PendingRecording." + "   ?s ?v ?o." + "  } union {" + "   ?o a tms:PendingRecording." + "   ?s ?v ?o." + "  }" + "}";

    /** 
   * The SPARQL query for deleting a ative recording resources.
   * <p>
   * {@value}
   */
    public static final String DELETEACTIVERECORDINGS_QUERY = SPARQL_PREFIXES + "delete { ?s ?v ?o} where {" + "  { " + "   ?s a tms:ActiveRecording." + "   ?s ?v ?o." + "  } union {" + "   ?o a tms:ActiveRecording." + "   ?s ?v ?o." + "  }" + "}";

    /** The combined resource model */
    private OntModel model;

    /** The file manager */
    private FileManager fileManager;

    /** The locator for resources in jars */
    private LocatorJar locator;

    /** The set of loaded regions */
    private Set<String> loadedRegions;

    /** The find channel SPARQL query */
    private Query findChannelQuery1;

    /** The find channel SPARQL query */
    private Query findChannelQuery2;

    /** The find rating SPARQL query */
    private Query findRatingQuery;

    /** The find icon SPARQL query */
    private Query findIconQuery;

    /** The find parent SPARQL query */
    private Query findParentQuery;

    /** The find transmtter SPARQL query */
    private Query findTransmitterQuery;

    /** The find brand SPARQL query */
    private Query findBrandQuery;

    /** The find episode SPARQL query */
    private Query findEpisodeQuery;

    /** The find enum SPARQL query */
    private Query findEnumQuery;

    /** The find enum SPARQL query */
    private Query findBroadcastQuery;

    /** The delete pending recordings SPARQL query */
    private UpdateRequest deletePendingRecordingsUpdate;

    /** The delete active recordings SPARQL query */
    private UpdateRequest deleteActiveRecordingsUpdate;

    /** The media regime */
    @SuppressWarnings("unused")
    private Resource mediaRegime;

    /** The rating system */
    private Resource ratingSystem;

    /** The default network */
    private Resource defaultNetwork;

    /** The default rating */
    private Resource defaultRating;

    /** The map from brand names to resources (optimisation for speed) */
    private Map<String, Brand> brandMap;

    /** The datatype factory for converting into XSD forms */
    private DatatypeFactory datatypeFactory;

    /**
   * Construct an initial resource locator.
   * <p>
   * The initial resource locator comes with ontologies loaded
   * and a file manager in place.
   * 
   * @throws Exception if unable to build the locator
   *
   */
    public ResourceLocator() throws Exception {
        this.loadedRegions = new HashSet<String>();
        this.brandMap = new HashMap<String, Brand>(256);
        this.datatypeFactory = DatatypeFactory.newInstance();
        this.buildFileManager();
        this.buildInitialModel();
        this.buildQueries();
    }

    /**
   * Build the file manager with appropriate mappings.
   * <p>
   * The tmsnet ontology is stored in the resources area
   */
    private void buildFileManager() {
        LocationMapper mapper = LocationMapper.get();
        String res;
        this.locator = new LocatorJar(this.LOCAL_URI, this.RDF_EXT);
        res = this.locateResourceBase("combined.rdf");
        this.logger.debug("Base location for defaults is " + res);
        this.locator.pushRelocation(this.LOCAL_URI, res);
        mapper.addAltPrefix(this.BASE_URI, this.LOCAL_URI);
        this.fileManager = new FileManager(mapper);
        this.fileManager.addLocator(this.locator);
        this.fileManager.addLocatorFile();
        this.fileManager.addLocatorURL();
    }

    /**
   * Work out where we should be looking for a resource.
   * 
   * @param local The local resource name (via org/charvolant/tmsnet/resources/rdf/)
   *
   * @return The base URL of the 
   */
    private String locateResourceBase(String local) {
        String res;
        try {
            res = this.getClass().getResource("/META-INF/rdf/" + local).toString();
            if (!res.endsWith(local)) throw new IllegalStateException("Unable to map local resource " + local + " gets: " + res);
            return res.substring(0, res.length() - local.length());
        } catch (Exception ex) {
            this.logger.info("No resources for " + local);
            return null;
        }
    }

    /**
   * Build the initial model.
   */
    private void buildInitialModel() {
        String name = "/META-INF/rdf/combined.rdf";
        String uri = this.getClass().getResource(name).toString();
        this.model = ModelFactory.createOntologyModel();
        this.model.getDocumentManager().setProcessImports(false);
        this.fileManager.readModel(this.model, uri);
        this.defaultNetwork = this.model.getResource(this.BASE_URI + "networks/default/Network");
        this.defaultRating = this.model.getResource(this.BASE_URI + "ratings/default/Rating");
        this.ratingSystem = this.model.getResource(this.BASE_URI + "ratings/default/tv");
    }

    /**
   * Build the SPARQL queries for locating various resources.
   */
    private void buildQueries() {
        this.findChannelQuery1 = QueryFactory.create(this.FINDCHANNEL_QUERY_1);
        this.findChannelQuery1.setLimit(1);
        this.findChannelQuery2 = QueryFactory.create(this.FINDCHANNEL_QUERY_2);
        this.findChannelQuery2.setLimit(1);
        this.findRatingQuery = QueryFactory.create(this.FINDRATING_QUERY);
        this.findRatingQuery.setLimit(1);
        this.findIconQuery = QueryFactory.create(this.FINDICON_QUERY);
        this.findIconQuery.setLimit(1);
        this.findParentQuery = QueryFactory.create(this.FINDPARENT_QUERY);
        this.findParentQuery.setLimit(1);
        this.findTransmitterQuery = QueryFactory.create(this.FINDTRANSMITTER_QUERY);
        this.findTransmitterQuery.setLimit(1);
        this.findBrandQuery = QueryFactory.create(this.FINDBRAND_QUERY);
        this.findBrandQuery.setLimit(1);
        this.findEpisodeQuery = QueryFactory.create(this.FINDEPISODE_QUERY);
        this.findEpisodeQuery.setLimit(1);
        this.findEnumQuery = QueryFactory.create(this.FINDENUM_QUERY);
        this.findEnumQuery.setLimit(1);
        this.findBroadcastQuery = QueryFactory.create(this.FINDBROADCAST_QUERY);
        this.findBroadcastQuery.setLimit(1);
        this.deletePendingRecordingsUpdate = UpdateFactory.create(this.DELETEPENDINGRECORDINGS_QUERY);
        this.deleteActiveRecordingsUpdate = UpdateFactory.create(this.DELETEACTIVERECORDINGS_QUERY);
    }

    /**
   * Load the resources associated with a specific locale.
   * <p>
   * At a minimum, load the DVBT channels and the TV ratings system.
   * Where possible, use the country code of the locale.
   * <p>
   * The locale must have a country code.
   * <p>
   * The location of the regions/<var>country</var>.rdf infomation
   * is located and added to the file manager's list of prefix mappings.
   * 
   * @param locale The locale
   * 
   * @throws IllegalArgumentException if the locale is null or doesn't have a country code
   */
    public void loadLocale(Locale locale) {
        String country;
        if (locale == null || locale.getCountry() == null) throw new IllegalArgumentException("No country in locale " + locale);
        country = locale.getCountry().toLowerCase();
        if (!this.loadedRegions.contains(country)) {
            String file = "combined-" + country + ".rdf";
            String name = "/META-INF/rdf/" + file;
            URL resource = this.getClass().getResource(name);
            String res;
            this.logger.debug("Combined resources for " + country + " is " + resource);
            this.loadedRegions.add(country);
            if (resource != null) {
                this.fileManager.readModel(this.model, resource.toString());
                res = this.locateResourceBase(file);
                this.logger.debug("Base location for " + country + " is " + res);
                this.locator.pushRelocation(this.LOCAL_URI, res);
                this.ratingSystem = this.model.getResource(this.BASE_URI + "ratings/" + country + "/tv");
            }
        }
    }

    /**
   * Open a URI for reading.
   * <p>
   * The URI is first processed using the {@link FileManager} locators
   * to load local resources if available.
   * 
   * @param uri The uri
   * 
   * @return The reading stream, or null for not found
   */
    public InputStream open(URI uri) {
        return uri == null ? null : this.fileManager.open(uri.toString());
    }

    /**
   * Find a resource that matches a network id and a channel number.
   * <p>
   * If we can't find one, then build a channel attached to the default network.
   * 
   * @param nid The network id
   * @param chno The channel logical identifier
   * 
   * @return The matching channel resource or the default channel for none
   */
    public Resource findChannel(int nid, int chno) {
        QuerySolutionMap bindings = new QuerySolutionMap();
        QueryExecution qexec;
        ResultSet results;
        Resource channel;
        QuerySolution sol;
        bindings.add("nid", this.model.createTypedLiteral(nid));
        bindings.add("chno", this.model.createTypedLiteral(chno));
        qexec = QueryExecutionFactory.create(this.findChannelQuery1, this.model, bindings);
        results = qexec.execSelect();
        if (results.hasNext()) {
            sol = results.next();
            channel = sol.getResource("channel");
            if (channel != null) return channel;
        }
        qexec = QueryExecutionFactory.create(this.findChannelQuery2, this.model, bindings);
        results = qexec.execSelect();
        if (results.hasNext()) {
            sol = results.next();
            channel = sol.getResource("channel");
            if (channel != null) return channel;
        }
        channel = this.model.createResource(this.BASE_URI + "networks/default/" + nid + "/" + chno, TMSNet.Channel);
        channel.addLiteral(TMSNet.originatingNetworkId, nid);
        channel.addLiteral(TMSNet.logicalChannelNumber, chno);
        channel.addProperty(TMSNet.network, this.defaultNetwork);
        channel.addLiteral(TMSNet.label, "Channel " + chno);
        return channel;
    }

    /**
   * Build a uri for a specific {@link TMSNet#Program}.
   * <p>
   * The Uri is constructed from the network, transport stream and service id
   * 
   * @param nid The network id
   * @param trid The transport stream id
   * @param sid The service id
   * 
   * @return The program URI
   */
    private String buildStationUri(int nid, int trid, int sid) {
        return this.BASE_URI + "networks/programs/" + nid + "/" + trid + "/" + sid;
    }

    /**
   * Create a {@link TMSNet#Station} for a network, transport stream and service id triple.
   * <p>
   * An existing resource may be returned if one already exists.
   * 
   * @param nid The network id
   * @param trid The transport stream id
   * @param sid The service id
   * 
   * @return The program resource.
   */
    public Resource createStation(int nid, int trid, int sid) {
        String uri = this.buildStationUri(nid, trid, sid);
        return this.model.createResource(uri, TMSNet.Station);
    }

    /**
   * Build a URI for an event
   * 
   * @param channel The station the event is associated with
   * @param eventId The event identifier
   * 
   * @return The broadcast URI
   */
    private String buildBroadcastUri(Resource channel, int eventId) {
        return channel.getURI() + "/broadcasts/" + eventId;
    }

    /**
   * Find or create a resource that represents a specific broadcast.
   * 
   * @param channel The channel associated with the broadcast
   * @param eventId The event id for the broadcast
   * 
   * @return The created resource
   */
    public Resource createBroadcast(Resource channel, int eventId) {
        String uri = this.buildBroadcastUri(channel, eventId);
        Resource resource = this.model.createResource(uri, PO.Broadcast);
        this.create(resource, TMSNet.eventId, eventId);
        this.create(resource, PO.broadcast_on, channel);
        return resource;
    }

    /**
   * Create a brand URI.
   * <p>
   * This is currently just a UUID-based URI.
   * 
   * @param title The title (not used)
   * 
   * @return The brand URI
   */
    private String createBrandUri(String title) {
        return this.BASE_URI + "programmes/brands/" + UUID.randomUUID().toString();
    }

    /**
   * Find a resource that matches an enumeration value.
   * <p>
   * Enums are found by looking for a matching
   * {@link TMSNet#javaValue} in the model.
   * 
   * @param value The enum value
   * 
   * @return The corresponding resource or null for not found
   */
    public Resource findEnum(Enum<?> value) {
        QuerySolutionMap bindings = new QuerySolutionMap();
        QueryExecution qexec;
        ResultSet results;
        Resource resource;
        QuerySolution sol;
        if (value == null) return null;
        bindings.add("enum", this.model.createLiteral(value.getClass().getName() + "." + value.toString()));
        qexec = QueryExecutionFactory.create(this.findEnumQuery, this.model, bindings);
        results = qexec.execSelect();
        if (results.hasNext()) {
            sol = results.next();
            resource = sol.getResource("resource");
            if (resource != null) return resource;
        }
        return null;
    }

    /**
   * Create a episode URI.
   * <p>
   * This is currently just a UUID-based URI.
   * 
   * @param brand The brand (not used)
   * @param description The description (not used)
   * 
   * @return The episode URI
   */
    private String createEpisodeUri(Resource brand, String description) {
        return this.BASE_URI + "programmes/episodes/" + UUID.randomUUID().toString();
    }

    /**
   * Create a program episode.
   * 
   * @param brand The episode brand
   * @param description The episode description
   * 
   * @return The corresponding brand, based on title
   */
    public Resource createEpisode(Resource brand, String description) {
        Resource episode;
        episode = this.model.createResource(this.createEpisodeUri(brand, description), PO.Episode);
        episode.addProperty(TMSNet.episode_of, brand);
        episode.addLiteral(PO.short_synopsis, description);
        brand.addProperty(PO.episode, episode);
        return episode;
    }

    /**
   * Find or create a program episode.
   * 
   * @param title The brand title
   * @param description The episode description
   * 
   * @return The corresponding episode, based on title and description
   */
    public Resource findOrCreateEpisode(String title, String description) {
        Brand brand;
        brand = this.brandMap.get(title);
        if (brand == null) {
            Resource br = this.model.createResource(this.createBrandUri(title), PO.Brand);
            br.addLiteral(DCTerms.title, title);
            brand = new Brand(br);
            this.brandMap.put(title, brand);
        }
        return brand.findOrCreateEpisode(description);
    }

    /**
   * Build a URI for a version
   * 
   * @param broadcats The broadcast 
   * @param eventId The event identifier
   * 
   * @return The broadcast URI
   */
    private String buildVersionUri(Resource broadcast) {
        return broadcast.getURI() + "/version";
    }

    /**
   * Find or create a resource that represents a version specific broadcast.
   * 
   * @param channel The channel associated with the broadcast
   * @param eventId The event id for the broadcast
   * 
   * @return The created resource
   */
    public Resource createVersion(Resource broadcast, Resource episode) {
        String uri = this.buildVersionUri(broadcast);
        Resource resource = this.model.createResource(uri, PO.Version);
        broadcast.addProperty(PO.broadcast_of, resource);
        resource.addProperty(TMSNet.version_of, episode);
        episode.addProperty(PO.version, resource);
        return resource;
    }

    /**
   * Create an anonymous interval for timeline-based information
   * 
   * @param start The start date/time
   * @param finish The end date/time
   * 
   * @return The interval
   */
    public Resource createInterval(Date start, Date finish) {
        Calendar calendar = Calendar.getInstance();
        Resource resource = this.model.createResource(Timeline.Interval);
        calendar.setTime(start);
        resource.addLiteral(Timeline.start, calendar);
        calendar.setTime(finish);
        resource.addLiteral(Timeline.end, calendar);
        return resource;
    }

    /**
   * Build a URI for a system with a name
   * 
   * @param name The name
   * 
   * @return The uri
   */
    private String buildSystemUri(String name) {
        return this.RECORDER_URI + name;
    }

    /**
   * Create a system description for a TRF7160.
   * 
   * @param name The device name
   * 
   * @return The system
   */
    public Resource createSystem(String name) {
        String uri = this.buildSystemUri(name);
        Resource system = this.model.createResource(uri, TMSNet.Device);
        return system;
    }

    /**
   * Build a URI for a pending recording timer
   * 
   * @param systemUri The system uri
   * @param slot The timer slot
   * 
   * @return The uri
   */
    private String buildPendingRecordingUri(String systemUri, int slot) {
        return systemUri + "/timers/" + slot;
    }

    /**
   * Create a timer description.
   * 
   * @param system The system (device)
   * @param slot The timer slot
   * 
   * @return The timer
   */
    public Resource createPendingRecording(Resource system, int slot) {
        String uri = this.buildPendingRecordingUri(system.getURI(), slot);
        Resource timer = this.model.createResource(uri, TMSNet.PendingRecording);
        timer.addProperty(TMSNet.recorder, system);
        return timer;
    }

    /**
   * Build a URI for a playback
   * 
   * @param systemUri The system uri
   * 
   * @return The uri
   */
    private String buildPlaybackUri(String systemUri) {
        return systemUri + "/playback";
    }

    /**
   * Create a playback description.
   * 
   * @param system The system (device)
   * @param slot The timer slot
   * 
   * @return The timer
   */
    public Resource createPlayback(Resource system) {
        String uri = this.buildPlaybackUri(system.getURI());
        Resource timer = this.model.createResource(uri, TMSNet.Playback);
        timer.addProperty(TMSNet.recorder, system);
        return timer;
    }

    /**
   * Build a URI for an active recording timer
   * 
   * @param systemUri The system uri
   * @param slot The timer slot
   * 
   * @return The uri
   */
    private String buildActiveRecordingUri(String systemUri, int slot) {
        return systemUri + "/activeRecordings/" + slot;
    }

    /**
   * Create an active recording description.
   * 
   * @param system The system (device)
   * @param slot The timer slot
   * 
   * @return The timer
   */
    public Resource createActiveRecording(Resource system, int slot) {
        String uri = this.buildActiveRecordingUri(system.getURI(), slot);
        Resource timer = this.model.createResource(uri, TMSNet.ActiveRecording);
        timer.addProperty(TMSNet.recorder, system);
        return timer;
    }

    /**
   * Find or create a transmitter with the supplied name.
   * 
   * @param name The name
   * @param polar Is this a polar satellite
   * 
   * @return The transmitter
   */
    public Resource findTransmitter(String name, boolean polar) {
        QuerySolutionMap bindings = new QuerySolutionMap();
        QueryExecution qexec;
        ResultSet results;
        Resource transmitter;
        QuerySolution sol;
        bindings.add("name", this.model.createLiteral(name));
        qexec = QueryExecutionFactory.create(this.findTransmitterQuery, this.model, bindings);
        results = qexec.execSelect();
        if (results.hasNext()) {
            sol = results.next();
            transmitter = sol.getResource("transmitter");
            return transmitter;
        }
        transmitter = this.model.createResource(this.BASE_URI + "networks/transmitters/" + name, TMSNet.Satellite);
        transmitter.addLiteral(DCTerms.title, name);
        transmitter.addLiteral(TMSNet.polar, polar);
        return transmitter;
    }

    /**
   * Find a resource that matches a rating.
   * <p>
   * The minimum rating at or above the supplied age is returned
   * 
   * @param age The age of the recording
   * 
   * @return The matching rating resource or null for none
   */
    public Resource findRating(int age) {
        QuerySolutionMap bindings = new QuerySolutionMap();
        QueryExecution qexec;
        ResultSet results;
        bindings.add("system", this.ratingSystem);
        bindings.add("age", this.model.createTypedLiteral(age));
        qexec = QueryExecutionFactory.create(this.findRatingQuery, this.model, bindings);
        results = qexec.execSelect();
        if (!results.hasNext()) return this.defaultRating;
        QuerySolution sol = results.next();
        return sol.getResource("rating");
    }

    /**
   * Find an icon for a specific resource.
   * <p>
   * Only PNG icons are returned.
   * 
   * @param resource The resource
   * @param size The icon size
   * 
   * @return The matching icon or null for none
   */
    public Resource findIcon(Resource resource, int size) {
        QuerySolutionMap bindings = new QuerySolutionMap();
        QueryExecution qexec;
        ResultSet results;
        while (resource != null) {
            bindings.add("resource", resource);
            bindings.add("size", this.model.createTypedLiteral(size));
            qexec = QueryExecutionFactory.create(this.findIconQuery, this.model, bindings);
            results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution sol = results.next();
                return sol.getResource("icon");
            }
            qexec = QueryExecutionFactory.create(this.findParentQuery, this.model, bindings);
            results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution sol = results.next();
                resource = sol.getResource("parent");
            } else resource = null;
        }
        return null;
    }

    /**
   * Find a network corresponding to a channel.
   * 
   * @param channel The channel resource
   * 
   * @return The matching network or the default network for none
   */
    public Resource findNetwork(Resource channel) {
        Statement s = channel.getProperty(TMSNet.network);
        return s == null ? this.defaultNetwork : s.getObject().asResource();
    }

    /**
   * Get the label associated with a resource.
   * 
   * @param res The resource
   * 
   * @return The label or null for not found
   */
    public String findLabel(Resource res) {
        Statement s = res.getProperty(TMSNet.label);
        return s == null ? null : s.getString();
    }

    /**
   * Find an event that is covered by a specific interval.
   * 
   * @param interval The covering interval
   * @param channel The channel that provides the timeline
   * 
   * @return The largest covering event or null for none
   */
    public Resource findEvent(Resource interval, Resource channel) {
        QuerySolutionMap bindings = new QuerySolutionMap();
        QueryExecution qexec;
        ResultSet results;
        Resource transmitter;
        QuerySolution sol;
        bindings.add("interval", interval);
        bindings.add("channel", channel);
        qexec = QueryExecutionFactory.create(this.findBroadcastQuery, this.model, bindings);
        results = qexec.execSelect();
        if (results.hasNext()) {
            sol = results.next();
            transmitter = sol.getResource("broadcast");
            return transmitter;
        }
        return null;
    }

    /**
   * Check that what we have got is valid.
   * <p>
   * All problems (clean or valid) are reported through the logger.
   * 
   * @return True if the model is currently valid.
   */
    public boolean isValid() {
        ValidityReport report;
        Iterator<Report> reports;
        report = this.model.validate();
        reports = report.getReports();
        while (reports.hasNext()) this.logger.warn("Model problem: " + reports.next());
        return report.isValid();
    }

    /**
   * Either add an object property to a resource.
   * <p>
   * A utility method for importing descriptions from the TMSNet protocol.
   * 
   * @param resource The resource
   * @param property The property
   * @param value The value
   */
    public void create(Resource resource, com.hp.hpl.jena.rdf.model.Property property, Resource value) {
        resource.removeAll(property);
        resource.addProperty(property, value);
    }

    /**
   * Either add an integer property to a resource or update the existing property.
   * <p>
   * A utility method for importing descriptions from the TMSNet protocol.
   * 
   * @param resource The resource
   * @param property The property
   * @param value The value
   */
    public void create(Resource resource, com.hp.hpl.jena.rdf.model.Property property, boolean value) {
        resource.removeAll(property);
        resource.addLiteral(property, value);
    }

    /**
   * Either add an integer property to a resource or update the existing property.
   * <p>
   * A utility method for importing descriptions from the TMSNet protocol.
   * 
   * @param resource The resource
   * @param property The property
   * @param value The value
   */
    public void create(Resource resource, com.hp.hpl.jena.rdf.model.Property property, int value) {
        resource.removeAll(property);
        resource.addLiteral(property, (Integer) value);
    }

    /**
   * Either add a long property to a resource.
   * <p>
   * A utility method for importing descriptions from the TMSNet protocol.
   * 
   * @param resource The resource
   * @param property The property
   * @param value The value
   */
    public void create(Resource resource, com.hp.hpl.jena.rdf.model.Property property, long value) {
        resource.removeAll(property);
        resource.addLiteral(property, (Long) value);
    }

    /**
   * Either add an string property to a resource or update the existing property.
   * 
   * @param resource The resource
   * @param property The property
   * @param value The value
   */
    public void create(Resource resource, com.hp.hpl.jena.rdf.model.Property property, String value) {
        resource.removeAll(property);
        if (value != null) resource.addLiteral(property, value);
    }

    /**
   * Either add an enum property to a resource or update the existing property.
   * <p>
   * A utility method for importing descriptions from the TMSNet protocol.
   * <p>
   * Enums are mapped onto resources by searching for a 
   * {@link TMSNet#javaValue} property.
   * 
   * @param resource The resource
   * @param property The property
   * @param value The value
   */
    public void create(Resource resource, com.hp.hpl.jena.rdf.model.Property property, Enum<?> value) {
        Resource r = this.findEnum(value);
        resource.removeAll(property);
        if (r != null) resource.addProperty(property, r);
    }

    /**
   * Either add a date-time property to a resource or update the existing property.
   * <p>
   * A utility method for importing descriptions from the TMSNet protocol.
   * 
   * @param resource The resource
   * @param property The property
   * @param value The value
   */
    public void create(Resource resource, com.hp.hpl.jena.rdf.model.Property property, Date value) {
        Calendar date = Calendar.getInstance();
        date.setTime(value);
        resource.removeAll(property);
        resource.addLiteral(property, date);
    }

    /**
   * Either add an date (only) property to a resource or update the existing property.
   * <p>
   * A utility method for importing descriptions from the TMSNet protocol.
   * 
   * @param resource The resource
   * @param property The property
   * @param value The value
   */
    public void createDate(Resource resource, com.hp.hpl.jena.rdf.model.Property property, Date value) {
        Calendar date = Calendar.getInstance();
        date.setTime(value);
        resource.removeAll(property);
        resource.addLiteral(property, this.model.createTypedLiteral(javax.xml.bind.DatatypeConverter.printDate(date), XSDDatatype.XSDdate));
    }

    /**
   * Either add a duration property to a resource or update the existing property.
   * <p>
   * A utility method for importing descriptions from the TMSNet protocol.
   * 
   * @param resource The resource
   * @param property The property
   * @param minutes The number of minutes in the duration
   * @param seconds The number of seconds in the duration
   */
    public void createDuration(Resource resource, com.hp.hpl.jena.rdf.model.Property property, int minutes, int seconds) {
        Duration duration = this.datatypeFactory.newDuration(true, 0, 0, 0, 0, minutes, seconds);
        resource.addLiteral(property, this.model.createTypedLiteral(duration.toString(), XSDDatatype.XSDduration));
    }

    /**
   * Parse an XSD duration
   * 
   * @param duration The XSD duration
   * 
   * @return The duration
   * 
   * @see DataypeFactory#newDuration(String)
   * 
   * @throws IllegalArgumentException if this is not a valid duration
   */
    public Duration parseDuration(String duration) throws IllegalArgumentException {
        return this.datatypeFactory.newDuration(duration);
    }

    /**
   * Remove all timers from the model.
   */
    public void deletePendingRecordings() {
        UpdateAction.execute(this.deletePendingRecordingsUpdate, this.model);
    }

    /**
   * Remove all active recordings from the model.
   */
    public void deleteActiveRecordings() {
        UpdateAction.execute(this.deleteActiveRecordingsUpdate, this.model);
    }

    /**
   * A locator that loads from a url.
   * <p>
   * There are two things that the locator does:
   * <ul>
   * <li>Re-directing URIs with a base of {@link ResourceLocator#BASE_URI} to
   * a stack of possible jar-based locations. The {@link ResourceLocator#loadLocale(Locale)}
   * method adds ext adding a ".rdf" extension along
   * the way.
   */
    public class LocatorExtURL implements Locator {

        public LocatorExtURL() {
            super();
        }

        /**
     * Get the name of the locator
     *
     * @return The name of the locator
     * 
     * @see com.hp.hpl.jena.util.LocatorFile#getName()
     */
        @Override
        public String getName() {
            return "LocatorExtURL";
        }

        /**
     * See if we can locate the RDF for a URI by adding a ".rdf" on the end.
     * <p>
     * The URI is then opened as a URL for reading.
     * This means that file: and jar: urls which have been created
     * by the {@link LocationMapper} will be read correctly. 
     *
     * @param uri The uri
     * 
     * @return The stream, if found, or null for not found
     * 
     * @see com.hp.hpl.jena.util.LocatorFile#open(java.lang.String)
     */
        @Override
        public TypedStream open(String uri) {
            int lastSlash = uri.lastIndexOf('/');
            if (uri.lastIndexOf('.') <= lastSlash) uri = uri + ".rdf";
            try {
                URL url = new URL(uri);
                ResourceLocator.this.logger.debug("Load " + uri);
                return new TypedStream(url.openStream());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private class Brand {

        /** The brand resource */
        private Resource brand;

        /** The episodes of the brand */
        private Map<String, Resource> episodeMap;

        /**
     * Construct a Brand.
     *
     * @param brand
     */
        public Brand(Resource brand) {
            super();
            this.brand = brand;
            this.episodeMap = new HashMap<String, Resource>(16);
        }

        /**
     * Get the brand.
     *
     * @return the brand
     */
        @SuppressWarnings("unused")
        public Resource getBrand() {
            return this.brand;
        }

        /**
     * Find or create an episode for this brand.
     * 
     * @param description The episode description
     * 
     * @return The matching episode
     */
        public Resource findOrCreateEpisode(String description) {
            Resource episode = this.episodeMap.get(description);
            if (episode == null) {
                episode = ResourceLocator.this.createEpisode(this.brand, description);
                this.episodeMap.put(description, episode);
            }
            return episode;
        }
    }
}
