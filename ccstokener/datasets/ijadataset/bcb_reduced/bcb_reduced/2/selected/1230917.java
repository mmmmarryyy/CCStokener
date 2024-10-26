package at.ac.arcs.itt.sosclient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.apache.xmlbeans.XmlObject;

/**
 * This class shall be used as an interface class to an OGC SOS Server. The
 * current state of XML-Parsing assumes an N52 server for that as several fields
 * of the XML-Replies are not parsed and interpreted
 */
public class SOSConnector {

    private static final Logger logger = Logger.getLogger(SOSConnector.class.getName());

    protected static final String VERSION_0_0_31 = "0.0.31";

    protected static final String VERSION_1_0_0 = "1.0.0";

    private static final int X_AXIS = 1;

    private static final int Y_AXIS = 0;

    private static final String GMLURL = "gmlUrl";

    private static final String OGCURL = "ogcUrl";

    private static final String SOSURL = "sosUrl";

    private static final String XLINKURL = "xlinkUrl";

    private static final String XPATHCAPS = "xpathCaps";

    private static final String XPATHEXPRESSION = "xpathExpression";

    private static final String XPATHCOORDINATESL = "xpathCoordinatesL";

    private static final String XPATHCOORDINATESU = "xpathCoordinatesU";

    private static final String XPATHTIME = "xpathTime";

    private static final String XPATHNAMES = "xpathNames";

    private static final String XPATHOPMETADATA = "xpathOpMetadata";

    private static final String XPATHPARAMETER = "xpathParameter";

    private static final String XPATHPARAMVALUES = "xpathParamValues";

    private static final String XPATHMEMBERRESULT = "xpathMemberResult";

    private static final String XPATHPROCEDURE = "xpathProcedure";

    private static final String XPATHFEATUREOFINTEREST = "xpathFeatureOfInterest";

    private static final String XPATHOBSPROP = "xpathObsProp";

    private static final String RESPONSEFORMAT = "responseFormat";

    private static final String VERSION = "version";

    private static final String CAPABILITIESSTRING = "capabilitiesString";

    private static final String ACCEPTEDVERSIONS = "acceptedVersions";

    private static final String TEMPORALOPS = "temporalOps";

    private static final String ZIPFORMAT = "application/zip";

    protected XmlObject caps = null;

    private Properties versionProps = null;

    private Set<String> responseFormats = Collections.synchronizedSet(new HashSet<String>());

    private URL sosURL;

    private String[] observationResultPath = null;

    /**
     * Constructor for the SOSConnector class.
     * 
     * @param url the SOS endpoint URL
     * @throws NullPointerException If url is null
     */
    public SOSConnector(URL url) {
        if (url == null) {
            throw new NullPointerException("url must not be null.");
        }
        sosURL = url;
    }

    /**
     * Checks whether the provided URL designates a valid endpoint and this class can connect to.
     * 
     * @throws java.io.IOException If no connection can be established.
     */
    public void checkConnection() throws IOException {
        HttpURLConnection c = (HttpURLConnection) getUrl().openConnection();
        c.connect();
        c.disconnect();
    }

    /**
     * Retrieves the values of the featureOfInterest parameter of the GetObservation SOS operation.
     *
     * @return array of found Features of Interest
     *
     * @throws at.ac.arcs.itt.sosclient.SosClientException If an error occurs
     *         during the connection or data retrieval
     */
    public String[] getFOIs() throws SosClientException {
        init();
        String[] vals = getOperationParameterValues("GetObservation", "featureOfInterest");
        return vals;
    }

    /**
     * Retrieves a map of offering ids and offerings which are available at the endpoint SOS.
     * The capabilities of the endpoint SOS are analyzed and for every offering that is found
     * a new Offering object is created, inserted into the result map and keyed by the offering's id.
     *
     * @return map of offeringId - Offering pairs
     *
     * @throws at.ac.arcs.itt.sosclient.SosClientException If an error occurs during
     *         the connection or data retrieval
     */
    public Map<String, Offering> getOfferings() throws SosClientException {
        init();
        Map<String, Offering> result = new Hashtable<String, Offering>();
        XmlObject[] offeringsTags = getCapabilities().selectPath(versionProps.getProperty(XPATHEXPRESSION));
        for (XmlObject o : offeringsTags) {
            Offering offering = new Offering();
            XmlObject id = o.selectAttribute(versionProps.getProperty(GMLURL), "id");
            String offeringId = null;
            if (id != null && id.getDomNode().getFirstChild() != null) {
                offeringId = id.getDomNode().getFirstChild().getNodeValue();
            }
            if (offeringId == null) {
                continue;
            }
            offering.id = offeringId;
            XmlObject[] names = o.selectPath(versionProps.getProperty(XPATHNAMES));
            String offeringName = getStringValue(names[0]);
            offering.name = offeringName;
            offering.procedure = loadHrefIntoSet(o, XPATHPROCEDURE);
            offering.featureOfInterest = loadHrefIntoSet(o, XPATHFEATUREOFINTEREST);
            offering.observedProperty = loadHrefIntoSet(o, XPATHOBSPROP);
            if (offering.procedure == null || offering.featureOfInterest == null || offering.observedProperty == null) {
                continue;
            }
            try {
                XmlObject[] lower = o.selectPath(versionProps.getProperty(XPATHCOORDINATESL));
                XmlObject[] upper = o.selectPath(versionProps.getProperty(XPATHCOORDINATESU));
                String[] lowerCornerStrings = getStringValue(lower[0]).split(" ");
                String[] upperCornerStrings = getStringValue(upper[0]).split(" ");
                if ((lowerCornerStrings.length == 2) && (upperCornerStrings.length == 2)) {
                    @SuppressWarnings("unused") double checkme = Double.parseDouble(lowerCornerStrings[Y_AXIS]);
                    checkme = Double.parseDouble(lowerCornerStrings[X_AXIS]);
                    checkme = Double.parseDouble(upperCornerStrings[Y_AXIS]);
                    checkme = Double.parseDouble(upperCornerStrings[X_AXIS]);
                    offering.lowerX = lowerCornerStrings[X_AXIS];
                    offering.lowerY = lowerCornerStrings[Y_AXIS];
                    offering.upperX = upperCornerStrings[X_AXIS];
                    offering.upperY = upperCornerStrings[Y_AXIS];
                }
            } catch (Exception e) {
            }
            long[] times = new long[2];
            try {
                XmlObject[] begin = o.selectPath(versionProps.getProperty(XPATHTIME) + "/gml:beginPosition");
                XmlObject[] end = o.selectPath(versionProps.getProperty(XPATHTIME) + "/gml:endPosition");
                if (begin.length > 0 && end.length > 0) {
                    String beginString = getStringValue(begin[0]);
                    String endString = getStringValue(end[0]);
                    times[0] = getTimeMsForString(beginString);
                    times[1] = getTimeMsForString(endString);
                }
            } catch (Exception e) {
                times = null;
            }
            if (times != null) {
                offering.startTime = Long.toString(times[0]);
                offering.endTime = Long.toString(times[1]);
            }
            result.put(offeringId, offering);
        }
        return result;
    }

    /**
     * Returns the endpoint URL of the SOS.
     *
     * @return the SOS endpoint URL.
     */
    public URL getUrl() {
        return sosURL;
    }

    public String[] getObservation(String obsProperty, String featureId, String offeringId, String procedure, long fromTime, long toTime, boolean zipped) throws SosClientException, IOException {
        String request = getObservationRequestXml(offeringId, obsProperty, featureId, procedure, fromTime, toTime, zipped);
        StringBuilder response = getStaxEventResponse(request, zipped);
        String[] allFields = null;
        try {
            allFields = Pattern.compile("@@").split(response);
        } catch (Exception e) {
            allFields = new String[0];
        }
        return allFields;
    }

    /**
     * Figures out the version of the endpoint SOS.
     * 
     * @return the version of the endpoint SOS as string or null if unsupported version.
     */
    protected String getServiceVersion() {
        String version = null;
        String prepared = getUrl().toString() + "?service=SOS&request=GetCapabilities&Sections=ServiceIdentification&AcceptVersions=";
        String v031 = "0.0.0,0.0.31";
        String v100 = "1.0.0";
        if (checkVersion(prepared + v031)) {
            version = VERSION_0_0_31;
        } else if (checkVersion(prepared + v100)) {
            version = VERSION_1_0_0;
        }
        return version;
    }

    /**
     * Loads the capabilities into the caps member from the endpoint SOS for the
     * version which was found by <code>getServiceVersion()</code>.
     * 
     * @throws at.ac.arcs.itt.sosclient.SosClientException if an exception was returned
     *         or the capabilities returned could not be parsed.
     */
    protected void loadCapabilities() throws SosClientException {
        HttpURLConnection con = null;
        try {
            String capString = getUrl() + versionProps.getProperty(CAPABILITIESSTRING) + versionProps.getProperty(ACCEPTEDVERSIONS);
            con = doConnect(new URL(capString), "GET");
            BufferedInputStream bis = new BufferedInputStream(con.getInputStream());
            XmlObject o = XmlObject.Factory.parse(bis);
            bis.close();
            XmlObject[] temp = o.selectPath(versionProps.getProperty(XPATHCAPS));
            assert (temp.length == 1);
            if (temp.length > 0) {
                caps = temp[0];
            } else {
                throw new SosClientException("Couldn't get correct capabilities from Server-URL: " + getUrl());
            }
        } catch (Exception e) {
            throw new SosClientException("Server-URL: " + getUrl(), e);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private void checkCapsConfig() throws MissingResourceException {
        checkCapProperty(GMLURL);
        checkCapProperty(OGCURL);
        checkCapProperty(SOSURL);
        checkCapProperty(XLINKURL);
        checkCapProperty(XPATHCAPS);
        checkCapProperty(XPATHEXPRESSION);
        checkCapProperty(XPATHCOORDINATESL);
        checkCapProperty(XPATHCOORDINATESU);
        checkCapProperty(XPATHTIME);
        checkCapProperty(XPATHNAMES);
        checkCapProperty(XPATHOPMETADATA);
        checkCapProperty(XPATHPARAMETER);
        checkCapProperty(XPATHPARAMVALUES);
        checkCapProperty(XPATHMEMBERRESULT);
        checkCapProperty(XPATHPROCEDURE);
        checkCapProperty(XPATHOBSPROP);
        checkCapProperty(XPATHFEATUREOFINTEREST);
        checkCapProperty(RESPONSEFORMAT);
        checkCapProperty(VERSION);
        checkCapProperty(CAPABILITIESSTRING);
        checkCapProperty(ACCEPTEDVERSIONS);
        checkCapProperty(TEMPORALOPS);
    }

    private void checkCapProperty(String p) throws MissingResourceException {
        if (!versionProps.containsKey(p)) {
            throw new MissingResourceException("Couldn't find mandatory property '" + p + "'.", "", p);
        }
    }

    private boolean checkVersion(String connection) {
        boolean result = true;
        HttpURLConnection con = null;
        try {
            con = doConnect(new URL(connection), "GET");
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.toLowerCase().contains("versionnegotiationfailed")) {
                    result = false;
                    break;
                }
            }
            br.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "couldn't retrieve result vom server." + e.getMessage());
            result = false;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return result;
    }

    private BufferedInputStream getBufferedResponseStream(HttpURLConnection connection, boolean zipped) throws IOException {
        InputStream is = connection.getInputStream();
        if (zipped) {
            try {
                PushbackInputStream pis = new PushbackInputStream(new BufferedInputStream(is), 2);
                is = pis;
                if (isGzipStream(pis)) {
                    is = new GZIPInputStream(new BufferedInputStream(is));
                }
            } catch (IOException e) {
                if (!e.getMessage().contains("Not in GZIP format")) {
                    throw e;
                }
            }
        }
        BufferedInputStream bis = new BufferedInputStream(is);
        return bis;
    }

    private synchronized XmlObject getCapabilities() {
        return caps;
    }

    private boolean useZipping(boolean zipped) {
        return zipped && responseFormats.contains(ZIPFORMAT);
    }

    private String getObservationRequestXml(String oid, String phen, String fid, String proc, long start, long end, boolean zipped) {
        String result = "";
        StringBuilder sb = new StringBuilder(800);
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(start);
        cal.set(GregorianCalendar.ZONE_OFFSET, cal.get(GregorianCalendar.ZONE_OFFSET) + cal.get(GregorianCalendar.DST_OFFSET));
        cal.set(GregorianCalendar.DST_OFFSET, 0);
        String startTime = String.format("%1$tFT%1$tT%1$tz", cal);
        cal.setTimeInMillis(end);
        cal.set(GregorianCalendar.ZONE_OFFSET, cal.get(GregorianCalendar.ZONE_OFFSET) + cal.get(GregorianCalendar.DST_OFFSET));
        cal.set(GregorianCalendar.DST_OFFSET, 0);
        String endTime = String.format("%1$tFT%1$tT%1$tz", cal);
        String req_version = versionProps.getProperty(VERSION);
        String req_sosurl = versionProps.getProperty(SOSURL);
        String req_gmlurl = versionProps.getProperty(GMLURL);
        String req_ogcurl = versionProps.getProperty(OGCURL);
        String req_tempops = versionProps.getProperty(TEMPORALOPS);
        String req_respform = versionProps.getProperty(RESPONSEFORMAT);
        if (useZipping(zipped)) {
            req_respform = ZIPFORMAT;
        }
        sb.append("<GetObservation service=\"SOS\" version=\"" + req_version + "\" \n");
        sb.append("   xmlns=\"" + req_sosurl + "\" \n");
        sb.append("   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n");
        sb.append("   xmlns:gml=\"" + req_gmlurl + "\"> \n");
        sb.append("\n");
        sb.append("   <offering>" + oid + "</offering> \n");
        sb.append("   <eventTime> \n");
        sb.append("      <ogc:" + req_tempops + " xsi:type=\"ogc:BinaryTemporalOpType\" xmlns:ogc=\"" + req_ogcurl + "\"> \n");
        sb.append("          <ogc:PropertyName>urn:ogc:data:time:iso8601</ogc:PropertyName> \n");
        sb.append("          <gml:TimePeriod> \n");
        sb.append("             <gml:beginPosition>" + startTime + "</gml:beginPosition> \n");
        sb.append("             <gml:endPosition>" + endTime + "</gml:endPosition> \n");
        sb.append("          </gml:TimePeriod> \n");
        sb.append("      </ogc:" + req_tempops + "> \n");
        sb.append("   </eventTime> \n");
        if (proc != null && !proc.equals("")) {
            sb.append("   <procedure>" + proc + "</procedure>");
        }
        sb.append("   <observedProperty>" + phen + "</observedProperty> \n");
        sb.append("   <featureOfInterest> \n");
        sb.append("      <ObjectID>" + fid + "</ObjectID> \n");
        sb.append("   </featureOfInterest> \n");
        sb.append("   <responseFormat>" + req_respform + "</responseFormat> \n");
        sb.append("</GetObservation> \n");
        result = sb.toString();
        return result;
    }

    private String[] getOperationParameterValues(String opName, String paramName) throws SosClientException {
        XmlObject op = getOp(opName);
        XmlObject param = getOpParam(op, paramName);
        String[] vals = getParameterValues(param);
        return vals;
    }

    private XmlObject getOp(String name) throws SosClientException {
        XmlObject foundop = null;
        XmlObject[] opMeta = getCapabilities().selectPath(versionProps.getProperty(XPATHOPMETADATA));
        for (XmlObject op : opMeta) {
            String opName = getStringValue(op.selectAttribute("", "name"));
            if (opName.equals(name)) {
                foundop = op;
                break;
            }
        }
        return foundop;
    }

    private XmlObject getOpParam(XmlObject operation, String paramName) {
        XmlObject result = null;
        XmlObject[] params = operation.selectPath(versionProps.getProperty(XPATHPARAMETER));
        for (XmlObject param : params) {
            String name = getStringValue(param.selectAttribute("", "name"));
            if (name.equals(paramName)) {
                result = param;
                break;
            }
        }
        return result;
    }

    private String[] getParameterValues(XmlObject parameter) {
        XmlObject[] vals = parameter.selectPath(versionProps.getProperty(XPATHPARAMVALUES));
        String[] phens = new String[vals.length];
        for (int i = 0; i < vals.length; ++i) {
            phens[i] = getStringValue(vals[i]);
        }
        return phens;
    }

    private StringBuilder getStaxEventResponse(String query, boolean zipped) throws SosClientException, IOException {
        HttpURLConnection connection = null;
        StringBuilder response = null;
        try {
            connection = doConnect(getUrl(), "POST");
            writeRequest(query, connection);
            BufferedInputStream bis = getBufferedResponseStream(connection, zipped);
            response = getResultNoParsingWithEvent(bis);
            bis.close();
        } catch (XMLStreamException e) {
            throw new SosClientException("Invalid result XML received.", e);
        } catch (SosExceptionReport e) {
            throw new SosClientException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
        }
        return response;
    }

    private HttpURLConnection doConnect(URL url, String method) throws IOException {
        HttpURLConnection con = null;
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);
        con.setDoInput(true);
        con.setDoOutput(true);
        con.connect();
        return con;
    }

    private String getStringValue(XmlObject o) {
        String val = "";
        if (o != null) {
            val = o.getDomNode().getFirstChild().getNodeValue();
        }
        return val != null ? val : "";
    }

    private long getTimeMsForString(String beginString) {
        String[] dateParts = beginString.split("[-T:+]", 7);
        assert dateParts.length == 7;
        TimeZone tz = TimeZone.getTimeZone("GMT" + (beginString.indexOf('+') >= 0 ? "+" : "-") + dateParts[6]);
        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.clear();
        cal.set(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]) - 1, Integer.parseInt(dateParts[2]), Integer.parseInt(dateParts[3]), Integer.parseInt(dateParts[4]), Integer.parseInt(dateParts[5]));
        long t = cal.getTimeInMillis();
        return t;
    }

    private synchronized void init() throws SosClientException {
        if (getCapabilities() == null) {
            initCapabilities();
        }
    }

    private void initCapabilities() throws SosClientException {
        String version = getServiceVersion();
        InputStream propIs = null;
        if (version.equals("0.0.31")) {
            propIs = SOSConnector.class.getResourceAsStream("sos031.properties");
        } else if (version.equals("1.0.0")) {
            propIs = SOSConnector.class.getResourceAsStream("sos100.properties");
        } else {
            throw new SosClientException("Can't handle unexpected version '" + version + "'.");
        }
        try {
            versionProps = new Properties();
            versionProps.load(propIs);
            checkCapsConfig();
        } catch (Exception e) {
            throw new SosClientException("Can't find properties for version '" + version + "'.");
        }
        loadCapabilities();
        try {
            setAcceptedFormats();
        } catch (Exception e) {
            throw new SosClientException("Coudln't load accepted response formats.", e);
        }
        observationResultPath = prepareObsResultPath();
    }

    private String[] prepareObsResultPath() {
        String[] result = null;
        String[] opMeta = versionProps.getProperty(XPATHMEMBERRESULT).split("this/");
        if (opMeta.length == 2) {
            String interesting = opMeta[1];
            result = interesting.split("/");
            for (int i = 0; i < result.length; ++i) {
                if (!result[i].equals("")) {
                    String[] pathElem = result[i].split(":");
                    if (pathElem.length == 2) {
                        result[i] = pathElem[1];
                    } else {
                        result[i] = pathElem[0];
                    }
                }
            }
        }
        return result;
    }

    private void setAcceptedFormats() {
        responseFormats.add("text/xml");
        logger.log(Level.FINER, "Added accepted format: 'text/xml'");
        XmlObject[] opm = getCapabilities().selectPath(versionProps.getProperty(XPATHOPMETADATA));
        for (XmlObject o : opm) {
            if (hasAttributeValue(o, "name", "GetCapabilities")) {
                XmlObject[] params = o.selectPath(versionProps.getProperty(XPATHPARAMETER));
                for (XmlObject p : params) {
                    if (hasAttributeValue(p, "name", "AcceptFormats")) {
                        XmlObject[] formats = p.selectPath(versionProps.getProperty(XPATHPARAMVALUES));
                        for (XmlObject format : formats) {
                            String f = getStringValue(format);
                            if (responseFormats.add(f)) {
                                logger.log(Level.FINER, "Added accepted format: '" + f + "'");
                            }
                        }
                        return;
                    }
                }
            }
        }
    }

    private boolean hasAttributeValue(final XmlObject o, String attr, String val) {
        boolean result = false;
        if (val != null) {
            XmlObject att = o.selectAttribute("", attr);
            if (att != null) {
                String attv = getStringValue(att);
                if (val.equals(attv)) {
                    result = true;
                }
            }
        }
        return result;
    }

    private StringBuilder getResultNoParsingWithEvent(BufferedInputStream is) throws XMLStreamException, SosExceptionReport {
        XMLInputFactory fac = XMLInputFactory.newInstance();
        XMLEventReader reader = fac.createXMLEventReader(is);
        checkServiceException(reader);
        StringBuilder result = getCharactersForPath(reader, observationResultPath);
        return result;
    }

    private StartElement getNextStartElementByName(XMLEventReader reader, String name) throws XMLStreamException {
        StartElement result = null;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.getEventType() == XMLEvent.START_ELEMENT) {
                StartElement element = event.asStartElement();
                if (element.getName().getLocalPart().equals(name)) {
                    result = element;
                    break;
                }
            }
        }
        return result;
    }

    private StringBuilder getCharactersForPath(XMLEventReader reader, String[] pathElements) throws XMLStreamException {
        StringBuilder result = null;
        for (String s : pathElements) {
            if (reader.hasNext()) {
                getNextStartElementByName(reader, s);
            }
        }
        if (reader.hasNext()) {
            result = getCharacters(reader);
        }
        return result;
    }

    private StringBuilder getCharacters(XMLEventReader reader) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        XMLEvent event = null;
        while ((event = reader.nextEvent()).isCharacters()) {
            sb.append(event.asCharacters().getData());
        }
        return sb;
    }

    private Set<String> loadHrefIntoSet(XmlObject src, String path) {
        Set<String> target = null;
        XmlObject[] objects = src.selectPath(versionProps.getProperty(path));
        if (objects.length > 0) {
            target = new HashSet<String>(objects.length);
            for (XmlObject o : objects) {
                XmlObject procId = o.selectAttribute(versionProps.getProperty(XLINKURL), "href");
                String pid = getStringValue(procId);
                target.add(pid);
            }
        }
        return target;
    }

    private void writeRequest(String query, HttpURLConnection connection) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        bw.write(query);
        bw.flush();
        bw.close();
    }

    private void checkServiceException(XMLEventReader reader) throws SosExceptionReport, XMLStreamException {
        XMLEvent event = null;
        while ((event = reader.peek()) != null) {
            if (event.getEventType() != XMLEvent.START_ELEMENT) {
                reader.nextEvent();
            } else {
                StartElement selem = event.asStartElement();
                if (selem.getName().getLocalPart().toLowerCase().contains("exception")) {
                    buildServiceException(reader);
                } else {
                    return;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void buildServiceException(XMLEventReader reader) throws SosExceptionReport, XMLStreamException {
        XMLEvent event = null;
        String code = null;
        String locator = null;
        String text = null;
        while (reader.hasNext() && (event = reader.nextEvent()) != null) {
            if (event.getEventType() == XMLEvent.START_ELEMENT) {
                StartElement selem = event.asStartElement();
                String sname = selem.getName().getLocalPart().toLowerCase();
                if (sname.equals("exception")) {
                    Iterator<Attribute> it = selem.getAttributes();
                    while (it.hasNext()) {
                        Attribute at = it.next();
                        String name = at.getName().getLocalPart().toLowerCase();
                        if (name.equals("exceptioncode")) {
                            code = at.getValue();
                        } else if (name.equals("locator")) {
                            locator = at.getValue();
                        }
                    }
                } else if (sname.equals("exceptiontext")) {
                    text = getCharacters(reader).toString();
                }
            }
        }
        throw new SosExceptionReport(code, locator, text);
    }

    private boolean isGzipStream(PushbackInputStream pis) throws IOException {
        int byte0 = readUByte(pis);
        int byte1 = readUByte(pis);
        boolean result = false;
        if (getUShort(byte0, byte1) != GZIPInputStream.GZIP_MAGIC) {
            result = false;
        } else {
            result = true;
        }
        pis.unread(byte1);
        pis.unread(byte0);
        return result;
    }

    private int getUShort(int byte0, int byte1) {
        return (byte1 << 8) | byte0;
    }

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
}
