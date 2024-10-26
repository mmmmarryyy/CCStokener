package at.ac.ait.enviro.dssos.connector;

import at.ac.ait.enviro.dssos.connector.exceptions.SosClientException;
import at.ac.ait.enviro.dssos.connector.exceptions.SosExceptionReport;
import at.ac.ait.enviro.dssos.container.CapabilitiesDocument;
import at.ac.ait.enviro.dssos.container.ExceptionReport;
import at.ac.ait.enviro.dssos.container.Feature;
import at.ac.ait.enviro.dssos.container.ObservationOffering;
import at.ac.ait.enviro.dssos.container.ObservationResult;
import at.ac.ait.enviro.dssos.container.ServiceIdentification;
import com.vividsolutions.jts.geom.Geometry;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSet;
import org.apache.log4j.Logger;
import static at.ac.ait.enviro.dssos.util.URLHandler.addQuery;
import org.xml.sax.SAXException;

public abstract class AbstractSOSConnector implements ISOSConnector {

    protected static final transient Logger log = Logger.getLogger(AbstractSOSConnector.class);

    protected static final String REQUEST_CAPABILITIES = "REQUEST=GetCapabilities&SERVICE=SOS";

    protected CapabilitiesDocument caps;

    protected Map<String, Feature> features;

    /** HTTP POST Request */
    protected static final String POST_REQUEST = "POST";

    /** HTTP GET Request */
    protected static final String GET_REQUEST = "GET";

    /** URL of a SOS, used as an entry point */
    protected final URL sosURL;

    /** "Easting" of SOS coordinates */
    protected final boolean isEasting;

    /** {@link RuleSet} containing all rules to parse a GetCapabilities response */
    protected final RuleSet ruleSetCapabilities;

    /** {@link RuleSet} containing all rules to parse a GetFeatureOfInterest response */
    protected final RuleSet ruleSetFOI;

    /** {@link RuleSet} containing all rules to parse a GetObservation response */
    protected final RuleSet ruleSetObservation;

    /** {@link RuleSet} containing all rules to parse a ExceptionReport response */
    protected final RuleSet ruleSetExceptionReport;

    /**
     * Creates a new AbstractSOSConnector
     * @param sosURL
     *          URL of a Sensor Observation Service
     * @param isEasting
     *          "Easting" of source SOS coordinates
     */
    public AbstractSOSConnector(URL sosURL, boolean isEasting) {
        this.sosURL = sosURL;
        this.features = new HashMap<String, Feature>();
        this.isEasting = isEasting;
        this.ruleSetCapabilities = createGetCapabilitiesRules(isEasting);
        this.ruleSetFOI = createGetFOICoordinateRules(isEasting);
        this.ruleSetObservation = createGetObservationRules();
        this.ruleSetExceptionReport = createExceptionReportRules();
    }

    /**
     * Creates a new AbstractSOSConnector
     * @param sosURL
     *          URL of a Sensor Observation Service
     */
    public AbstractSOSConnector(URL sosURL) {
        this(sosURL, false);
    }

    /**
     * @return Returns a {@link RuleSet} with the rules for parsing a GetCapabilities Response
     */
    protected abstract RuleSet createGetCapabilitiesRules(boolean isEasting);

    /**
     * @return Returns a {@link RuleSet} with the rules for parsing a GetFeature Response
     */
    protected abstract RuleSet createGetFOICoordinateRules(boolean isEasting);

    /**
     * @return Returns a {@link RuleSet} with the rules for parsing a GetObservation Response
     */
    protected abstract RuleSet createGetObservationRules();

    /**
     * @return Returns a {@link RuleSet} with the rules for parsing a GetObservation Response
     */
    protected abstract RuleSet createExceptionReportRules();

    /**
     * Returns the request for the SOS operation {@code GetFeatureOfInterest}
     * @param foiID
     *      ID of the feature that is to be queried.
     * @return A String with the {@code GetFeatureOfInterest} request
     */
    protected abstract String getRequest_GetFOI(String foiID);

    protected abstract String getRequest_GetObservation(String obsProperty, String featureId, String offeringId, String procedure, String responseFormat, String responseMode, long fromTime, long toTime);

    /**
     * Formats a given time (in milliseconds) to a String
     * @param millis
     * @return Returns a formatted String
     */
    protected String formatTime(long millis) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(millis);
        cal.set(GregorianCalendar.ZONE_OFFSET, cal.get(GregorianCalendar.ZONE_OFFSET) + cal.get(GregorianCalendar.DST_OFFSET));
        cal.set(GregorianCalendar.DST_OFFSET, 0);
        return String.format("%1$tFT%1$tT%1$tz", cal);
    }

    @Override
    public Map<String, ObservationOffering> getOfferings() throws SosClientException, SosExceptionReport {
        if (caps == null) {
            parseCapabilities();
        }
        return caps.getOfferings();
    }

    @Override
    public ServiceIdentification getServiceIdentification() throws SosClientException, SosExceptionReport {
        if (caps == null) {
            parseCapabilities();
        }
        return caps.getServiceInfo();
    }

    @Override
    public String[] getFOIs() throws SosClientException, SosExceptionReport {
        if (caps == null) {
            parseCapabilities();
        }
        HashSet<String> foiIDs = new HashSet<String>();
        for (ObservationOffering offering : caps.getOfferings().values()) {
            foiIDs.addAll(offering.getFeatures());
        }
        return foiIDs.toArray(new String[foiIDs.size()]);
    }

    @Override
    public Geometry getFOICoordinates(String foiID) throws SosClientException, SosExceptionReport {
        if (!features.containsKey(foiID)) {
            InputStream response = null;
            try {
                final String request = getRequest_GetFOI(foiID);
                response = sendRequest("GetFeatureOfInterest", request);
                final Digester digester = getDigester(ruleSetFOI, ruleSetExceptionReport);
                digester.parse(response);
                if (digester.getRoot() instanceof Feature) {
                    final Feature feature = (Feature) digester.getRoot();
                    features.put(foiID, feature);
                } else {
                    handleException(digester.getRoot());
                }
            } catch (SAXException ex) {
                log.error("Parsing Error while performing getFOICoordinates(" + foiID + ")", ex);
                throw new SosClientException(ex);
            } catch (IOException ex) {
                log.error("Error while performing getFOICoordinates(" + foiID + ")", ex);
                throw new SosClientException(ex);
            } finally {
                try {
                    response.close();
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        } else {
            log.debug("Taking \'" + foiID + "\' from cache!");
        }
        return features.get(foiID).getCoordinates();
    }

    @Override
    public ObservationResult getObservations(String obsProperty, String featureId, String offeringId, String procedure, long fromTime, long toTime) throws SosClientException, SosExceptionReport {
        return getObservations(obsProperty, featureId, offeringId, procedure, fromTime, toTime, null);
    }

    @Override
    public ObservationResult getObservations(String obsProperty, String featureId, String offeringId, long fromTime, long toTime) throws SosClientException, SosExceptionReport {
        return getObservations(obsProperty, featureId, offeringId, null, fromTime, toTime, null);
    }

    @Override
    public ObservationResult getObservations(String obsProperty, String featureId, String offeringId, String procedure, long fromTime, long toTime, String responseMode) throws SosClientException, SosExceptionReport {
        String respFormat = getResponseFormatZipped(offeringId);
        final boolean isZipped = (respFormat != null);
        if (!isZipped) {
            respFormat = getResponseFormatOM(offeringId);
        }
        ObservationResult result = null;
        final String request = getRequest_GetObservation(obsProperty, featureId, offeringId, procedure, respFormat, responseMode, fromTime, toTime);
        InputStream response = null;
        try {
            response = getBufferedResponseStream(sendRequest("GetObservation", request), isZipped);
            final Digester digester = getDigester(ruleSetObservation, ruleSetExceptionReport);
            digester.parse(response);
            if (digester.getRoot() instanceof ObservationResult) {
                result = (ObservationResult) digester.getRoot();
            } else {
                handleException(digester.getRoot());
            }
        } catch (SAXException ex) {
            log.error("Error while performing getObservations(" + obsProperty + ", " + featureId + ", " + offeringId + ", " + procedure + ", " + respFormat + ", " + responseMode + ", " + fromTime + ", " + toTime + ")", ex);
            throw new SosClientException(ex);
        } catch (IOException ex) {
            log.error("Error while performing getObservations(" + obsProperty + ", " + featureId + ", " + offeringId + ", " + procedure + ", " + respFormat + ", " + responseMode + ", " + fromTime + ", " + toTime + ")", ex);
            throw new SosClientException(ex);
        } finally {
            try {
                response.close();
            } catch (Exception ex) {
                log.error(ex);
            }
        }
        return result;
    }

    @Override
    public void refresh() throws SosClientException, SosExceptionReport {
        log.debug("Clearing Feature cache, retrive Capabilities");
        features.clear();
        parseCapabilities();
    }

    @Override
    public URL getSOSUrl() {
        return sosURL;
    }

    /**
     * Performs a GetCapabilities request and parses the received document
     * @throws at.ac.ait.enviro.dssos.SosClientException
     */
    protected void parseCapabilities() throws SosClientException, SosExceptionReport {
        try {
            final URL reqURL = new URL(addQuery(sosURL, REQUEST_CAPABILITIES));
            log.debug("Using URL " + reqURL + " to recieve Capabilities document.");
            final HttpURLConnection connection = doConnect(reqURL, GET_REQUEST);
            final InputStream is = connection.getInputStream();
            final Digester digester = getDigester(ruleSetCapabilities, ruleSetExceptionReport);
            digester.parse(is);
            if (digester.getRoot() instanceof CapabilitiesDocument) {
                caps = (CapabilitiesDocument) digester.getRoot();
            } else {
                handleException(digester.getRoot());
            }
        } catch (SAXException ex) {
            log.error("Could not parse Capabilities document");
            throw new SosClientException(ex);
        } catch (IOException ex) {
            log.error("Could not retrieve Capabilities document", ex);
            throw new SosClientException(ex);
        }
    }

    /**
     * Creates a new Digester instance and adds the supported RuleSets to it
     * @param rules
     * @return A new {@link Digester}
     */
    protected Digester getDigester(final RuleSet... rules) {
        final Digester digester = new Digester();
        digester.setNamespaceAware(true);
        digester.setValidating(false);
        for (RuleSet r : rules) {
            digester.addRuleSet(r);
        }
        return digester;
    }

    /**
     * Sends a request to a SOS. The request is sent to the operation specific URL,
     * listed in the CapabilitiesDocument.
     * @param operationName
     *      Name of the SOS operation that should be performed.
     * @param request
     * @return The response as InputStream
     * @throws java.io.IOException in case of an error
     */
    protected InputStream sendRequest(String operationName, String request) throws IOException {
        URL targetURL = getOperationURL(operationName);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Sending following %s request to '%s': \n%s", operationName, targetURL.toString(), request));
        }
        HttpURLConnection connection = doConnect(targetURL, POST_REQUEST);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        bw.write(request);
        bw.flush();
        bw.close();
        return connection.getInputStream();
    }

    protected URL getOperationURL(String operationName) {
        String strURL = null;
        URL url = getSOSUrl();
        if (caps != null && caps.getOperationURLs() != null) {
            strURL = caps.getOperationURLs().getProperty(operationName);
            if (strURL == null) {
                log.warn(String.format("URL for operation '%s' not found. Using default URL '%s'.", operationName, url.toString()));
            } else {
                try {
                    url = new URL(strURL);
                } catch (MalformedURLException ex) {
                    log.warn(String.format("Malformed URL for operation '%s': '%s'. Using default URL '%s'.", operationName, strURL, url.toString()));
                }
            }
        }
        return url;
    }

    /**
     * Opens a new HttpURLConnection to a specified URL with a specidied HTTP method
     * @param url
     * @param method
     *      HTTP method 
     * @return Returns a new {@link HttpURLConnection}
     * @throws java.io.IOException
     */
    protected HttpURLConnection doConnect(URL url, String method) throws IOException {
        HttpURLConnection con = null;
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);
        con.setDoInput(true);
        con.setDoOutput(true);
        con.connect();
        return con;
    }

    /**
     * Returns {@code inputStream} wrapped into a {@link BufferedInputStream},
     * in case of {@code zipped == true}, {@code inputStream} is wrapped into
     * {@link PushbackInputStream} and a {@link GZIPInputStream}
     * @param inputStream
     *      the original InputStream
     * @param zipped
     *      decides if the InputStream gets wrapped into a GZIPInputStream
     * @return Returns the wrapped {@code inputStream}
     * @throws java.io.IOException
     */
    private BufferedInputStream getBufferedResponseStream(InputStream inputStream, boolean zipped) throws IOException {
        if (zipped) {
            try {
                PushbackInputStream wrapperStream = new PushbackInputStream(new BufferedInputStream(inputStream), 2);
                inputStream = wrapperStream;
                if (isGzipStream(wrapperStream)) {
                    inputStream = new GZIPInputStream(new BufferedInputStream(inputStream));
                }
            } catch (IOException e) {
                if (!e.getMessage().contains("Not in GZIP format")) {
                    throw e;
                }
            }
        }
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        return bis;
    }

    /**
     * Checks if a PushbackInputStream is a {@link GZIPInputStream}
     * @param inputStream
     * @return Returns <b>true</b> if {@code inputStream} is a GZIPInputStream
     * @throws java.io.IOException
     */
    private boolean isGzipStream(PushbackInputStream inputStream) throws IOException {
        int byte0 = readUByte(inputStream);
        int byte1 = readUByte(inputStream);
        boolean result = false;
        if (getUShort(byte0, byte1) != GZIPInputStream.GZIP_MAGIC) {
            result = false;
        } else {
            result = true;
        }
        inputStream.unread(byte1);
        inputStream.unread(byte0);
        return result;
    }

    /**
     * Morge two bytes to integer
     * @param byte0
     * @param byte1
     * @return an integer
     */
    private int getUShort(int byte0, int byte1) {
        return (byte1 << 8) | byte0;
    }

    /**
     * Reads a byte from an InputStream
     * @param in
     * @return the byte
     * @throws java.io.IOException
     */
    private int readUByte(InputStream in) throws IOException {
        int b = in.read();
        if (b == -1) {
            throw new EOFException();
        }
        if (b < -1 || b > 255) {
            throw new IOException("invalid byte received: '" + b + "'.");
        }
        return b;
    }

    /**
     * Tries to retrieve the response format O&M for an offering
     * @param offeringId
     * @return the response format String for O&M, or {@code null} if not available
     * @throws at.ac.ait.enviro.dssos.SosClientException
     */
    protected String getResponseFormatOM(String offeringId) throws SosClientException, SosExceptionReport {
        final ObservationOffering offering = getOfferings().get(offeringId);
        String format = null;
        if (offering != null) {
            List<String> responseFormats = offering.getPropertiesList(ObservationOffering.KEY_RESPONSE_FORMATS);
            for (String rF : responseFormats) {
                if (rF.contains("text/xml") && rF.contains("om/")) {
                    format = rF;
                    log.info("Using Response Format " + format);
                }
            }
        }
        return format;
    }

    /**
     * Tries to retrieve the response format application/zip for an offering
     * @param offeringId
     * @return the response format String for application/zip, or {@code null} if not available
     * @throws at.ac.ait.enviro.dssos.SosClientException
     */
    protected String getResponseFormatZipped(String offeringId) throws SosClientException, SosExceptionReport {
        final ObservationOffering offering = getOfferings().get(offeringId);
        String format = null;
        if (offering != null) {
            List<String> responseFormats = offering.getPropertiesList(ObservationOffering.KEY_RESPONSE_FORMATS);
            for (String rF : responseFormats) {
                if (rF.contains("application/zip")) {
                    format = rF;
                    log.info("Using Response Format " + format);
                }
            }
        }
        return format;
    }

    /**
     * Throws a {@link SosClientException} in case of an invalid service response
     * or a ExceptionReport response
     * 
     * @param o
     *      root object from the Digester stack
     * @throws at.ac.ait.enviro.dssos.SosClientException
     */
    protected void handleException(Object obj) throws SosClientException, SosExceptionReport {
        if (obj == null) {
            throw new SosClientException("Unexpected service response");
        } else if (obj instanceof ExceptionReport) {
            final ExceptionReport r = (ExceptionReport) obj;
            throw new SosExceptionReport(r.getExceptionCode(), r.getExceptionLocator(), r.getExceptionText());
        }
    }
}
