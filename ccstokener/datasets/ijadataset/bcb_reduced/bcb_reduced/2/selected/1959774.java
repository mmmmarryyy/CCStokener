package transxchange2GoogleTransitHandler;

import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.util.zip.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;

public class TransxchangeHandlerEngine extends DefaultHandler {

    TransxchangeAgency agencies;

    TransxchangeStops stops;

    TransxchangeRoutes routes;

    TransxchangeTrips trips;

    TransxchangeStopTimes stopTimes;

    TransxchangeCalendar calendar;

    TransxchangeCalendarDates calendarDates;

    static String parseError = "";

    static String parseInfo = "";

    static String gtfsUrl = "";

    static String gtfsTimezone = "";

    static String gtfsDefaultRouteType = "";

    static String gtfsLang = "";

    static String gtfsPhone = "";

    static String gtfsOutfile = "";

    static final String agencyFilename = "agency";

    static final String stopsFilename = "stops";

    static final String routesFilename = "routes";

    static final String tripsFilename = "trips";

    static final String stop_timesFilename = "stop_times";

    static final String calendarFilename = "calendar";

    static final String calendar_datesFilename = "calendar_dates";

    static final String extension = ".txt";

    static final String gtfsZipfileName = "google_transit.zip";

    static PrintWriter agenciesOut = null;

    static PrintWriter stopsOut = null;

    static PrintWriter routesOut = null;

    static PrintWriter tripsOut = null;

    static PrintWriter calendarsOut = null;

    static PrintWriter calendarDatesOut = null;

    static ArrayList filenames = null;

    static String outdir = "";

    static boolean useAgencyShortName = false;

    static boolean skipEmptyService = false;

    static boolean skipOrphanStops = false;

    static boolean geocodeMissingStops = false;

    static HashMap modeList = null;

    static ArrayList stopColumns = null;

    static String stopfilecolumnseparator = ",";

    static int naptanHelperStopColumn = -1;

    static HashMap naptanStopnames = null;

    HashMap calendarServiceIds = null;

    HashMap calendarDatesServiceIds = null;

    HashMap tripServiceIds = null;

    static String rootDirectory = "";

    static String workDirectory = "";

    static String agencyOverride = "";

    static HashMap agencyMap = null;

    public void setUrl(String url) {
        gtfsUrl = url;
    }

    public void setTimezone(String timezone) {
        gtfsTimezone = timezone;
    }

    public void setDefaultRouteType(String defaultRouteType) {
        gtfsDefaultRouteType = defaultRouteType;
    }

    public void setLang(String lang) {
        gtfsLang = lang;
    }

    public void setPhone(String phone) {
        gtfsPhone = phone;
    }

    public String getUrl() {
        return gtfsUrl;
    }

    public String getTimezone() {
        return gtfsTimezone;
    }

    public String getDefaultRouteType() {
        return gtfsDefaultRouteType;
    }

    public String getLang() {
        return gtfsLang;
    }

    public String getPhone() {
        return gtfsPhone;
    }

    public TransxchangeAgency getAgencies() {
        return agencies;
    }

    public TransxchangeStops getStops() {
        return stops;
    }

    public TransxchangeRoutes getRoutes() {
        return routes;
    }

    public TransxchangeTrips getTrips() {
        return trips;
    }

    public TransxchangeStopTimes getStopTimes() {
        return stopTimes;
    }

    public TransxchangeCalendar getCalendar() {
        return calendar;
    }

    public TransxchangeCalendarDates getCalendarDates() {
        return calendarDates;
    }

    public void setParseError(String txt) {
        parseError = txt;
    }

    public void setUseAgencyShortname(boolean flag) {
        useAgencyShortName = flag;
    }

    public void setSkipEmptyService(boolean flag) {
        skipEmptyService = flag;
    }

    public void setSkipOrphanStops(boolean flag) {
        skipOrphanStops = flag;
    }

    public void setGeocodeMissingStops(boolean flag) {
        geocodeMissingStops = flag;
    }

    public void setModeList(HashMap list) {
        modeList = list;
    }

    public void setStopColumns(ArrayList list) {
        stopColumns = list;
    }

    public void setStopfilecolumnseparator(String separator) {
        if (separator == null) stopfilecolumnseparator = "";
        stopfilecolumnseparator = separator;
    }

    public String getStopfilecolumnseparator() {
        return stopfilecolumnseparator;
    }

    public void setNaptanHelperStopColumn(int column) {
        naptanHelperStopColumn = column;
    }

    public int getNaptanHelperStopColumn() {
        return naptanHelperStopColumn;
    }

    public void setNaPTANStopnames(HashMap stopnames) {
        naptanStopnames = stopnames;
    }

    public String getNaPTANStopname(String atcoCode) {
        if (naptanStopnames == null || atcoCode == null) return "";
        if (!naptanStopnames.containsKey(atcoCode)) return "";
        return (String) naptanStopnames.get(atcoCode);
    }

    public HashMap getModeList() {
        return modeList;
    }

    public ArrayList getStopColumns() {
        return stopColumns;
    }

    public void setRootDirectory(String eRootDirectory) {
        rootDirectory = eRootDirectory;
    }

    public void setWorkDirectory(String eWorkDirectory) {
        workDirectory = eWorkDirectory;
    }

    public void setAgencyMap(HashMap agencies) {
        agencyMap = agencies;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public String getWorkDirectory() {
        return workDirectory;
    }

    public String getParseError() {
        return parseError;
    }

    public void setParseInfo(String txt) {
        parseInfo = txt;
    }

    public String getParseInfo() {
        return parseInfo;
    }

    public boolean isAgencyShortName() {
        return useAgencyShortName;
    }

    public boolean isSkipEmptyService() {
        return skipEmptyService;
    }

    public boolean isSkipOrphanStops() {
        return skipOrphanStops;
    }

    public boolean isGeocodeMissingStops() {
        return geocodeMissingStops;
    }

    public void addFilename(String fileName) {
        if (fileName == null || filenames == null) return;
        filenames.add(fileName);
    }

    public void addTripServiceId(String tripId, String serviceId) {
        if (tripServiceIds == null) tripServiceIds = new HashMap();
        tripServiceIds.put(tripId, serviceId);
    }

    public boolean hasTripServiceId(String testId) {
        if (tripServiceIds == null || testId == null || testId.length() == 0) return false;
        return tripServiceIds.containsKey(testId);
    }

    public String getTripServiceId(String tripId) {
        if (tripServiceIds == null || tripId == null || tripId.length() == 0) return "";
        return (String) tripServiceIds.get(tripId);
    }

    public boolean hasCalendarServiceId(String testId) {
        if (testId == null || calendarServiceIds == null) return false;
        return (calendarServiceIds.containsKey(testId));
    }

    public boolean hasCalendarDatesServiceId(String testId) {
        if (testId == null || calendarDatesServiceIds == null) return false;
        return (calendarDatesServiceIds.containsKey(testId));
    }

    public void setAgencyOverride(String agency) {
        agencyOverride = agency;
    }

    public String getAgencyOverride() {
        return agencyOverride;
    }

    public HashMap getAgencyMap() {
        return agencyMap;
    }

    public void startElement(String uri, String name, String qName, Attributes atts) throws SAXParseException {
        agencies.startElement(uri, name, qName, atts);
        stops.startElement(uri, name, qName, atts);
        routes.startElement(uri, name, qName, atts);
        trips.startElement(uri, name, qName, atts);
        stopTimes.startElement(uri, name, qName, atts);
        calendar.startElement(uri, name, qName, atts);
        calendarDates.startElement(uri, name, qName, atts);
    }

    public void characters(char ch[], int start, int length) {
        agencies.characters(ch, start, length);
        stops.characters(ch, start, length);
        routes.characters(ch, start, length);
        trips.characters(ch, start, length);
        stopTimes.characters(ch, start, length);
        calendar.characters(ch, start, length);
        calendarDates.characters(ch, start, length);
    }

    public void endElement(String uri, String name, String qName) {
        agencies.endElement(uri, name, qName);
        stops.endElement(uri, name, qName);
        routes.endElement(uri, name, qName);
        trips.endElement(uri, name, qName);
        stopTimes.endElement(uri, name, qName);
        calendar.endElement(uri, name, qName);
        calendarDates.endElement(uri, name, qName);
        agencies.clearKeys(qName);
        stops.clearKeys(qName);
        routes.clearKeys(qName);
        trips.clearKeys(qName);
        stopTimes.clearKeys(qName);
        calendar.clearKeys(qName);
        calendarDates.clearKeys(qName);
    }

    public void endDocument() {
        try {
            agencies.endDocument();
            stops.endDocument();
            routes.endDocument();
            trips.endDocument();
            stopTimes.endDocument();
            calendar.endDocument();
            calendarDates.endDocument();
        } catch (IOException e) {
            System.out.println("transxchange2GTFS endDocument() exception: " + e.getMessage());
            System.exit(0);
        }
        agencies.completeData();
        stops.completeData();
        routes.completeData();
        trips.completeData();
        stopTimes.completeData();
        calendar.completeData();
        calendarDates.completeData();
    }

    public static void prepareOutput(String rootDirectory, String workDirectory) throws IOException {
        outdir = rootDirectory + workDirectory;
        filenames = new ArrayList();
        new File(outdir + "/" + agencyFilename + extension).delete();
        new File(outdir + "/" + stopsFilename + extension).delete();
        new File(outdir + "/" + routesFilename + extension).delete();
        new File(outdir + "/" + tripsFilename + extension).delete();
        new File(outdir + "/" + stop_timesFilename + extension).delete();
        new File(outdir + "/" + calendarFilename + extension).delete();
        new File(outdir + "/" + calendar_datesFilename + extension).delete();
        new File(outdir + "/" + gtfsZipfileName).delete();
        new File(outdir).mkdirs();
    }

    public void writeOutputSansAgenciesStopsRoutes() throws IOException {
        String outfileName = "";
        File outfile = null;
        String daytypesJourneyPattern;
        String daytypesService;
        String serviceId;
        if (calendarsOut == null) {
            outfileName = calendarFilename + extension;
            outfile = new File(outdir + "/" + outfileName);
            filenames.add(outfileName);
            calendarsOut = new PrintWriter(new FileWriter(outfile));
            calendarsOut.println("service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date");
        }
        calendarServiceIds = new HashMap();
        String outLine;
        for (int i = 0; i < this.getCalendar().getListCalendar__service_id().size(); i++) {
            outLine = "";
            serviceId = (String) (((ValueList) this.getCalendar().getListCalendar__service_id().get(i))).getValue(0);
            daytypesJourneyPattern = (String) ((ValueList) this.getCalendar().getListCalendar__monday().get(i)).getValue(1);
            daytypesService = (String) ((ValueList) this.getCalendar().getListCalendar__monday().get(i)).getValue(2);
            if (daytypesService == null) daytypesService = "";
            if (daytypesService.equals(serviceId) && daytypesJourneyPattern.length() == 0) outLine += ((ValueList) this.getCalendar().getListCalendar__monday().get(i)).getValue(0); else outLine += "0";
            outLine += ",";
            daytypesJourneyPattern = (String) ((ValueList) this.getCalendar().getListCalendar__tuesday().get(i)).getValue(1);
            daytypesService = (String) ((ValueList) this.getCalendar().getListCalendar__tuesday().get(i)).getValue(2);
            if (daytypesService == null) daytypesService = "";
            if (daytypesService.equals(serviceId) && daytypesJourneyPattern.length() == 0) outLine += ((ValueList) this.getCalendar().getListCalendar__tuesday().get(i)).getValue(0); else outLine += "0";
            outLine += ",";
            daytypesJourneyPattern = (String) ((ValueList) this.getCalendar().getListCalendar__wednesday().get(i)).getValue(1);
            daytypesService = (String) ((ValueList) this.getCalendar().getListCalendar__wednesday().get(i)).getValue(2);
            if (daytypesService == null) daytypesService = "";
            if (daytypesService.equals(serviceId) && daytypesJourneyPattern.length() == 0) outLine += ((ValueList) this.getCalendar().getListCalendar__wednesday().get(i)).getValue(0); else outLine += "0";
            outLine += ",";
            daytypesJourneyPattern = (String) ((ValueList) this.getCalendar().getListCalendar__thursday().get(i)).getValue(1);
            daytypesService = (String) ((ValueList) this.getCalendar().getListCalendar__thursday().get(i)).getValue(2);
            if (daytypesService == null) daytypesService = "";
            if (daytypesService.equals(serviceId) && daytypesJourneyPattern.length() == 0) outLine += ((ValueList) this.getCalendar().getListCalendar__thursday().get(i)).getValue(0); else outLine += "0";
            outLine += ",";
            daytypesJourneyPattern = (String) ((ValueList) this.getCalendar().getListCalendar__friday().get(i)).getValue(1);
            daytypesService = (String) ((ValueList) this.getCalendar().getListCalendar__friday().get(i)).getValue(2);
            if (daytypesService == null) daytypesService = "";
            if (daytypesService.equals(serviceId) && daytypesJourneyPattern.length() == 0) outLine += ((ValueList) this.getCalendar().getListCalendar__friday().get(i)).getValue(0); else outLine += "0";
            outLine += ",";
            daytypesJourneyPattern = (String) ((ValueList) this.getCalendar().getListCalendar__saturday().get(i)).getValue(1);
            daytypesService = (String) ((ValueList) this.getCalendar().getListCalendar__saturday().get(i)).getValue(2);
            if (daytypesService == null) daytypesService = "";
            if (daytypesService.equals(serviceId) && daytypesJourneyPattern.length() == 0) outLine += ((ValueList) this.getCalendar().getListCalendar__saturday().get(i)).getValue(0); else outLine += "0";
            outLine += ",";
            daytypesJourneyPattern = (String) ((ValueList) this.getCalendar().getListCalendar__sunday().get(i)).getValue(1);
            daytypesService = (String) ((ValueList) this.getCalendar().getListCalendar__sunday().get(i)).getValue(2);
            if (daytypesService == null) daytypesService = "";
            if (daytypesService.equals(serviceId) && daytypesJourneyPattern.length() == 0) outLine += ((ValueList) this.getCalendar().getListCalendar__sunday().get(i)).getValue(0); else outLine += "0";
            outLine += ",";
            if (outLine.contains("1") || !skipEmptyService) {
                calendarsOut.print(serviceId);
                calendarsOut.print(",");
                calendarsOut.print(outLine);
                calendarsOut.print(((ValueList) this.getCalendar().getListCalendar__start_date().get(i)).getValue(0));
                calendarsOut.print(",");
                calendarsOut.print(((ValueList) this.getCalendar().getListCalendar__end_date().get(i)).getValue(0));
                calendarsOut.println();
                if (skipEmptyService) calendarServiceIds.put(serviceId, serviceId);
            }
        }
        if (this.getCalendarDates().getListCalendarDates__service_id().size() > 0) {
            if (calendarDatesOut == null) {
                outfileName = calendar_datesFilename + extension;
                outfile = new File(outdir + "/" + outfileName);
                calendarDatesOut = new PrintWriter(new FileWriter(outfile));
                filenames.add(outfileName);
                calendarDatesOut.println("service_id,date,exception_type");
            }
            calendarDatesServiceIds = new HashMap();
            String calendarDateServiceId;
            String calendarDateExceptionType;
            HashMap calendarExceptions = new HashMap();
            for (int i = 0; i < this.getCalendarDates().getListCalendarDates__service_id().size(); i++) {
                calendarDateServiceId = ((ValueList) this.getCalendarDates().getListCalendarDates__service_id().get(i)).getValue(0);
                calendarDateExceptionType = ((ValueList) this.getCalendarDates().getListCalendarDates__exception_type().get(i)).getValue(0);
                if (this.hasCalendarServiceId(calendarDateServiceId) || !calendarDateExceptionType.equals("2") || !skipEmptyService) {
                    outLine = calendarDateServiceId + "," + ((ValueList) this.getCalendarDates().getListCalendarDates__date().get(i)).getValue(0) + "," + calendarDateExceptionType;
                    if (!calendarExceptions.containsKey(outLine)) {
                        calendarDatesOut.println(outLine);
                        calendarExceptions.put(outLine, "");
                    }
                    if (skipEmptyService) calendarDatesServiceIds.put(calendarDateServiceId, calendarDateServiceId);
                }
            }
        }
        if (tripsOut == null) {
            outfileName = tripsFilename + extension;
            outfile = new File(outdir + "/" + outfileName);
            filenames.add(outfileName);
            tripsOut = new PrintWriter(new FileWriter(outfile));
            tripsOut.println("route_id,service_id,trip_id,trip_headsign,direction_id,block_id,shape_id");
        }
        String tripsRouteId;
        String tripsServiceId;
        String tripsDirectionId;
        String tripsRouteRef;
        for (int i = 0; i < this.getTrips().getListTrips__route_id().size(); i++) {
            tripsServiceId = ((ValueList) this.getTrips().getListTrips__service_id().get(i)).getValue(0);
            tripsDirectionId = ((ValueList) this.getTrips().getListTrips__direction_id().get(i)).getValue(0);
            tripsRouteRef = ((ValueList) this.getTrips().getListTrips__routeref().get(i)).getValue(0);
            if (!skipEmptyService || this.hasCalendarServiceId(tripsServiceId) || this.hasCalendarDatesServiceId(tripsServiceId)) {
                tripsRouteId = ((ValueList) this.getTrips().getListTrips__route_id().get(i)).getValue(0);
                tripsOut.print(tripsRouteId);
                tripsOut.print(",");
                tripsOut.print(tripsServiceId);
                tripsOut.print(",");
                tripsOut.print(((ValueList) this.getTrips().getListTrips__trip_id().get(i)).getKeyName());
                tripsOut.print(",");
                tripsOut.print((this.getRoutes().getRouteDescription(tripsRouteRef)));
                tripsOut.print(",");
                tripsOut.print(tripsDirectionId);
                tripsOut.print(",");
                tripsOut.print(((ValueList) this.getTrips().getListTrips__block_id().get(i)).getValue(0));
                tripsOut.print(",");
                tripsOut.println();
            }
        }
    }

    public void writeOutputAgenciesStopsRoutes() throws IOException {
        String outfileName = "";
        File outfile = null;
        if (agenciesOut == null) {
            outfileName = agencyFilename + extension;
            outfile = new File(outdir + "/" + outfileName);
            filenames.add(outfileName);
            agenciesOut = new PrintWriter(new FileWriter(outfile));
            agenciesOut.println("agency_id,agency_name,agency_url,agency_timezone,agency_lang,agency_phone");
        }
        for (int i = 0; i < this.getAgencies().getListAgency__agency_name().size(); i++) {
            if (((String) (((ValueList) this.getAgencies().getListAgency__agency_id().get(i))).getValue(0)).length() > 0) {
                agenciesOut.print(((ValueList) this.getAgencies().getListAgency__agency_id().get(i)).getValue(0));
                agenciesOut.print(",");
                agenciesOut.print(((ValueList) this.getAgencies().getListAgency__agency_name().get(i)).getValue(0));
                agenciesOut.print(",");
                agenciesOut.print(((ValueList) this.getAgencies().getListAgency__agency_url().get(i)).getValue(0));
                agenciesOut.print(",");
                agenciesOut.print(((ValueList) this.getAgencies().getListAgency__agency_timezone().get(i)).getValue(0));
                agenciesOut.print(",");
                agenciesOut.print(((ValueList) this.getAgencies().getListAgency__agency_lang().get(i)).getValue(0));
                agenciesOut.print(",");
                agenciesOut.print(((ValueList) this.getAgencies().getListAgency__agency_phone().get(i)).getValue(0));
                agenciesOut.println();
            }
        }
        if (stopsOut == null) {
            outfileName = stopsFilename + extension;
            outfile = new File(outdir + "/" + outfileName);
            filenames.add(outfileName);
            stopsOut = new PrintWriter(new FileWriter(outfile));
            stopsOut.println("stop_id,stop_name,stop_desc,stop_lat,stop_lon,zone_id,stop_url");
        }
        String stopId, stopName;
        for (int i = 0; i < this.getStops().getListStops__stop_id().size(); i++) {
            stopId = ((ValueList) this.getStops().getListStops__stop_id().get(i)).getValue(0);
            if (stopId.length() > 0 && (!skipOrphanStops || stops.hasStop(stopId))) {
                stopName = ((ValueList) this.getStops().getListStops__stop_name().get(i)).getValue(0);
                String[] coordinates = { ((ValueList) this.getStops().getListStops__stop_lat().get(i)).getValue(0), ((ValueList) this.getStops().getListStops__stop_lon().get(i)).getValue(0) };
                if (isGeocodeMissingStops() && (coordinates[0].equals("OpenRequired") || coordinates[1].equals("OpenRequired"))) {
                    try {
                        System.out.println("Geocoding stop (id / name): " + stopId + " / " + stopName);
                        geocodeMissingStop(stopName, coordinates);
                    } catch (Exception e) {
                        System.out.println("Geocoding exception: " + e.getMessage() + " for stop: " + stopName);
                    }
                }
                stopsOut.print(stopId);
                stopsOut.print(",");
                stopsOut.print(stopName);
                stopsOut.print(",");
                stopsOut.print(((ValueList) this.getStops().getListStops__stop_desc().get(i)).getValue(0));
                stopsOut.print(",");
                stopsOut.print(coordinates[0]);
                stopsOut.print(",");
                stopsOut.print(coordinates[1]);
                stopsOut.print(",");
                stopsOut.print(",");
                stopsOut.println();
            }
        }
        if (routesOut == null) {
            outfileName = routesFilename + extension;
            outfile = new File(outdir + "/" + outfileName);
            filenames.add(outfileName);
            routesOut = new PrintWriter(new FileWriter(outfile));
            routesOut.println("route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color");
        }
        for (int i = 0; i < this.getRoutes().getListRoutes__route_id().size(); i++) {
            if (((String) (((ValueList) this.getRoutes().getListRoutes__route_id().get(i))).getValue(0)).length() > 0) {
                routesOut.print(((ValueList) this.getRoutes().getListRoutes__route_id().get(i)).getValue(0));
                routesOut.print(",");
                routesOut.print(((ValueList) this.getRoutes().getListRoutes__agency_id().get(i)).getValue(0));
                routesOut.print(",");
                routesOut.print(((ValueList) this.getRoutes().getListRoutes__route_short_name().get(i)).getValue(0));
                routesOut.print(",");
                routesOut.print(((ValueList) this.getRoutes().getListRoutes__route_long_name().get(i)).getValue(0));
                routesOut.print(",");
                routesOut.print(((ValueList) this.getRoutes().getListRoutes__route_desc().get(i)).getValue(0));
                routesOut.print(",");
                routesOut.print(((ValueList) this.getRoutes().getListRoutes__route_type().get(i)).getValue(0));
                routesOut.print(",");
                routesOut.print(",");
                routesOut.print(",");
                routesOut.println();
            }
        }
    }

    public void clearDataSansAgenciesStopsRoutes() {
        trips = null;
        stopTimes = null;
        calendar = null;
        calendarDates = null;
    }

    public void closeStopTimes() {
        stopTimes.closeStopTimesOutput();
    }

    public String closeOutput(String rootDirectory, String workDirectory) throws IOException {
        agenciesOut.close();
        stopsOut.close();
        routesOut.close();
        tripsOut.close();
        calendarsOut.close();
        if (calendarDatesOut != null) calendarDatesOut.close();
        agenciesOut = null;
        stopsOut = null;
        routesOut = null;
        tripsOut = null;
        calendarsOut = null;
        calendarDatesOut = null;
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outdir + "/" + gtfsZipfileName));
        byte[] buf = new byte[1024];
        for (int i = 0; i < filenames.size(); i++) {
            FileInputStream in = new FileInputStream(outdir + "/" + (String) filenames.get(i));
            zipOut.putNextEntry(new ZipEntry((String) filenames.get(i)));
            int len;
            while ((len = in.read(buf)) > 0) {
                zipOut.write(buf, 0, len);
            }
            zipOut.closeEntry();
            in.close();
        }
        zipOut.close();
        return workDirectory + "/" + "google_transit.zip";
    }

    private void geocodeMissingStop(String stopname, String[] coordinates) throws MalformedURLException, UnsupportedEncodingException, XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        float[] coordFloat = { -999999, -999999 };
        String broadenedStopname;
        String token;
        StringTokenizer st;
        geocodeStop(stopname, coordFloat);
        if ((coordFloat[0] == -999999 || coordFloat[1] == -999999) && stopname.contains("/")) {
            broadenedStopname = "";
            st = new StringTokenizer(stopname, ",");
            while (st.hasMoreTokens()) {
                token = st.nextToken();
                if (token.contains("/")) token = token.substring(0, token.indexOf("/"));
                if (broadenedStopname.length() > 0) broadenedStopname += ", ";
                broadenedStopname += token;
            }
            if (!broadenedStopname.equals(stopname)) {
                stopname = broadenedStopname;
                geocodeStop(stopname, coordFloat);
            }
        }
        if ((coordFloat[0] == -999999 || coordFloat[1] == -999999) && stopname.contains("(")) {
            broadenedStopname = "";
            st = new StringTokenizer(stopname, ",");
            while (st.hasMoreTokens()) {
                token = st.nextToken();
                if (token.contains("(")) token = token.substring(0, token.indexOf("("));
                if (broadenedStopname.length() > 0) broadenedStopname += ", ";
                broadenedStopname += token;
            }
            if (!broadenedStopname.equals(stopname)) {
                stopname = broadenedStopname;
                geocodeStop(stopname, coordFloat);
            }
        }
        while ((coordFloat[0] == -999999 || coordFloat[1] == -999999) && stopname.lastIndexOf(",") >= 0) {
            stopname = stopname.substring(0, stopname.lastIndexOf(","));
            geocodeStop(stopname, coordFloat);
        }
        if (coordFloat[0] == -999999) coordinates[0] = "OpenRequired"; else coordinates[0] = "" + coordFloat[0];
        if (coordFloat[1] == -999999) coordinates[1] = "OpenRequired"; else coordinates[1] = "" + coordFloat[1];
    }

    private void geocodeStop(String stopname, float[] coordinates) throws MalformedURLException, UnsupportedEncodingException, XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        final String geocoderPrefix = "http://maps.google.com/maps/api/geocode/xml?address=";
        final String geocoderPostfix = "&sensor=false";
        if (stopname == null || coordinates == null || coordinates.length != 2) return;
        String geoaddress = geocoderPrefix + stopname + geocoderPostfix;
        System.out.println("	Trying: " + geoaddress);
        URL url = new URL(geocoderPrefix + URLEncoder.encode(stopname, "UTF-8") + geocoderPostfix);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        InputSource inputStream = new InputSource(conn.getInputStream());
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList geocodedNodes = (NodeList) xp.evaluate("/GeocodeResponse/result[1]/geometry/location/*", doc, XPathConstants.NODESET);
        float lat = -999999;
        float lon = -999999;
        Node node;
        for (int i = 0; i < geocodedNodes.getLength(); i++) {
            node = geocodedNodes.item(i);
            if ("lat".equals(node.getNodeName())) lat = Float.parseFloat(node.getTextContent());
            if ("lng".equals(node.getNodeName())) lon = Float.parseFloat(node.getTextContent());
        }
        coordinates[0] = lat;
        coordinates[1] = lon;
    }

    public TransxchangeHandlerEngine() throws UnsupportedEncodingException, IOException {
        agencies = new TransxchangeAgency(this);
        stops = new TransxchangeStops(this);
        routes = new TransxchangeRoutes(this);
        trips = new TransxchangeTrips(this);
        stopTimes = new TransxchangeStopTimes(this);
        calendar = new TransxchangeCalendar(this);
        calendarDates = new TransxchangeCalendarDates(this);
    }
}
