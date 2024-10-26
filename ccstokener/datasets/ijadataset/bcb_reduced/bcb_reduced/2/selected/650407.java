package edu.washington.assist.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import edu.washington.assist.animation.EventSeries;
import edu.washington.assist.animation.MutuallyExclusiveIntervalSeries;
import edu.washington.assist.animation.TimeInterval;
import edu.washington.assist.animation.TimedEvent;
import edu.washington.assist.audio.AudioInterval;
import edu.washington.assist.database.GeneralDataLoader;
import edu.washington.assist.database.QueryModel;

public class WebDataLoader implements GeneralDataLoader {

    private static final String SERVER_URL = "http://localhost:8081/";

    private static String XML_PATH = "AssistGWT/mission/F115/";

    private Document document;

    private GPSTrace trace;

    public static void main(String[] args) {
        long reportID = 115;
        WebDataLoader loader = new WebDataLoader();
        GPSTrace trace = loader.loadGPSTrace(reportID);
        System.out.println(trace.getStart());
    }

    public GPSTrace loadGPSTrace(long reportID) {
        try {
            URL url = new URL(SERVER_URL + XML_PATH + "gps.xml");
            System.out.println(url);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(url.openStream());
            Element customerElement = document.getDocumentElement();
            NodeList gps = customerElement.getElementsByTagName("gps");
            trace = getGPSTrace(gps);
        } catch (SAXException sxe) {
            Exception x = sxe;
            if (sxe.getException() != null) x = sxe.getException();
            x.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return trace;
    }

    private EventSeries<PhotoEvent> loadIncomingEvents(long reportID) {
        EventSeries<PhotoEvent> events = new EventSeries<PhotoEvent>();
        try {
            URL url = new URL(SERVER_URL + XML_PATH + "reports.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = reader.readLine()) != null) {
                String[] values = str.split(",");
                if (values.length == 2) {
                    long id = Long.parseLong(values[0]);
                    if (id == reportID) {
                        long time = Long.parseLong(values[1]);
                        events.addEvent(new PhotoEvent(time));
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return events;
    }

    private GPSTrace getGPSTrace(NodeList gps) {
        GPSTrace trace = new GPSTrace();
        for (int i = 0; i < gps.getLength(); i++) {
            Element parent = (Element) gps.item(i);
            String lng = parent.getElementsByTagName("lng").item(0).getFirstChild().getNodeValue();
            double longitude = Double.parseDouble(lng);
            String lat = parent.getElementsByTagName("lat").item(0).getFirstChild().getNodeValue();
            double latitude = Double.parseDouble(lat);
            String t = parent.getElementsByTagName("time").item(0).getFirstChild().getNodeValue();
            long time = Long.parseLong(t);
            trace.addEvent(new GPSPoint(longitude, latitude, time));
        }
        return trace;
    }

    public int countGPSPoints(QueryModel model) throws IOException {
        return 0;
    }

    public long loadConversationCount() throws IOException {
        return 0;
    }

    public long loadLatestMission() {
        return 0;
    }

    public List<Long> loadMissionIDs(QueryModel model) throws IOException {
        List<Long> list = new ArrayList<Long>();
        list.add(new Long(115));
        return list;
    }

    public TimeInterval loadMissionTimeInterval(long missionID) throws IOException {
        return TimeInterval.FOREVER;
    }

    public List<Long> loadReportIDs(QueryModel model) throws IOException {
        List<Long> list = new ArrayList<Long>();
        list.add(new Long(115));
        return list;
    }

    public List<Report> loadReports(long missionID) throws IOException {
        Report report = new LazilyLoadedReport(115, this);
        List<Report> list = new ArrayList<Report>();
        list.add(report);
        return list;
    }

    public EventSeries<ActivityEvent> loadActivities(long reportID) throws IOException {
        return new EventSeries<ActivityEvent>();
    }

    public byte[] loadAudioData(long reportID, long when) throws IOException {
        return null;
    }

    public EventSeries<AudioEvent> loadAudioEvents(long reportID) throws IOException {
        return new EventSeries<AudioEvent>();
    }

    public MutuallyExclusiveIntervalSeries<AudioInterval> loadAudioTimes(long reportID) throws IOException {
        return null;
    }

    public EventSeries<CompassEvent> loadCompass(long reportID) throws IOException {
        return new EventSeries<CompassEvent>();
    }

    public EventSeries<Conversation> loadConversations(long reportID) throws IOException {
        return new EventSeries<Conversation>();
    }

    public String loadDescription(long reportID) throws IOException {
        return "";
    }

    public Map<String, Integer> loadFeatureSet(long reportID) {
        return null;
    }

    public byte[] loadImageData(long reportID, long when) throws IOException {
        return null;
    }

    public List<EventSeries<ClusterEvent>> loadKClusters(long reportID) throws IOException {
        return null;
    }

    public EventSeries<ClusterEvent> loadKClusters(long reportID, int value, int featureSet) throws IOException {
        return null;
    }

    public int loadKValues(int featureSet) {
        return 0;
    }

    public EventSeries<KeywordEvent> loadKeywords(long reportID) throws IOException {
        return new EventSeries<KeywordEvent>();
    }

    public long loadMissionID(long reportID) throws IOException {
        return 115;
    }

    public EventSeries<PhotoEvent> loadPhotoTimes(long reportID, boolean internal) throws IOException {
        return new EventSeries<PhotoEvent>();
    }

    public RawDataSeries loadRawData(long reportID, String series) throws IOException {
        return null;
    }

    public EventSeries<VideoEvent> loadVideoTimes(long reportID) throws IOException {
        return new EventSeries<VideoEvent>();
    }

    public void validateReport(long reportID) throws IOException {
    }

    public void saveComment(long reportID, TimedEvent event, String text) {
    }

    public String getComment(long id, TimedEvent event) {
        return null;
    }

    public EventSeries<CommentEvent> getComments(long reportID) {
        return null;
    }

    public File getAudioFile(long reportID) {
        return null;
    }

    public long getMsbStartTime() {
        return 0;
    }

    public long getMsbStartTime(long reportID) {
        return 0;
    }
}
