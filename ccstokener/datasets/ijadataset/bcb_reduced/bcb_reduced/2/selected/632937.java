package upcoming.client;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import upcoming.client.internal.http.*;
import com.thoughtworks.xstream.XStream;
import upcoming.client.internal.xstream.XStreamFactory;
import java.net.URLEncoder;
import java.util.*;

public class Client {

    private HttpClient httpClient;

    private static final String UPCOMING_ENDPOINT = "http://upcoming.yahooapis.com/services/rest/";

    private String upcomingApiKey;

    private boolean compressionEnabled = false;

    public Client(String upcomingApiKey) {
        this(new DefaultHttpClient(), upcomingApiKey);
    }

    public Client(HttpClient hClient, String upcomingApiKey) {
        this.upcomingApiKey = upcomingApiKey;
        this.httpClient = hClient;
        setUserAgent("upcoming-java-client");
        setConnectionTimeout(10 * 1000);
        setSocketTimeout(25 * 1000);
    }

    public void setUserAgent(String ua) {
        this.httpClient.getParams().setParameter(AllClientPNames.USER_AGENT, ua);
    }

    public void setConnectionTimeout(int milliseconds) {
        httpClient.getParams().setIntParameter(AllClientPNames.CONNECTION_TIMEOUT, milliseconds);
    }

    public void setSocketTimeout(int milliseconds) {
        httpClient.getParams().setIntParameter(AllClientPNames.SO_TIMEOUT, milliseconds);
    }

    protected Response sendUpcomingRequest(String httpMethod, String url, Map<String, String> params) {
        params.put("api_key", getUpcomingApiKey());
        String xmlResponse = sendHttpRequest(httpMethod, url, params);
        Response r = fromXml(xmlResponse);
        r.setXml(xmlResponse);
        if (r.getError() != null) {
            throw new UpcomingException("error response from Yahoo Upcoming", r);
        }
        return r;
    }

    private String getUpcomingApiKey() {
        return upcomingApiKey;
    }

    protected Response fromXml(String xml) {
        XStream xstream = XStreamFactory.createXStream();
        Response r = (Response) xstream.fromXML(xml);
        return r;
    }

    protected String sendHttpRequest(String httpMethod, String url, Map<String, String> params) {
        HttpClient c = getHttpClient();
        HttpUriRequest request = null;
        if ("GET".equalsIgnoreCase(httpMethod)) {
            String queryString = buildQueryString(params);
            url = url + queryString;
            request = new HttpGet(url);
        } else if ("POST".equalsIgnoreCase(httpMethod)) {
            request = new HttpPost(url);
        } else {
            throw new RuntimeException("unsupported method: " + httpMethod);
        }
        HttpResponse response = null;
        HttpEntity entity = null;
        try {
            response = c.execute(request);
            entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
        }
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuffer query = new StringBuffer();
        if (params.size() > 0) {
            query.append("?");
            for (String key : params.keySet()) {
                query.append(key);
                query.append("=");
                query.append(encodeParameter(params.get(key)));
                query.append("&");
            }
            if (query.charAt(query.length() - 1) == '&') {
                query.deleteCharAt(query.length() - 1);
            }
        }
        return query.toString();
    }

    protected String encodeParameter(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected HttpClient getHttpClient() {
        if (this.httpClient instanceof DefaultHttpClient) {
            DefaultHttpClient defaultClient = (DefaultHttpClient) httpClient;
            defaultClient.removeRequestInterceptorByClass(GzipRequestInterceptor.class);
            defaultClient.removeResponseInterceptorByClass(GzipResponseInterceptor.class);
            if (this.isCompressionEnabled()) {
                defaultClient.addRequestInterceptor(GzipRequestInterceptor.getInstance());
                defaultClient.addResponseInterceptor(GzipResponseInterceptor.getInstance());
            }
        }
        return this.httpClient;
    }

    public Event getEvent(String id) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("method", "event.getInfo");
        Response r = sendUpcomingRequest("GET", UPCOMING_ENDPOINT, map);
        if (r.getEvents().size() == 0) {
            return null;
        } else {
            return r.getEvents().get(0);
        }
    }

    public List<Venue> findVenuesNear(double lat, double lon, Integer radius) {
        VenueSearchCriteria crit = new VenueSearchCriteria();
        crit.setLatLong(lat, lon);
        if (radius != null) {
            crit.setRadius(radius);
        }
        return findVenues(crit);
    }

    public List<Venue> findVenues(VenueSearchCriteria crit) {
        Response r = sendUpcomingRequest("GET", UPCOMING_ENDPOINT, crit.toMap());
        return r.getVenues();
    }

    public Venue getVenue(String id) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("method", "venue.getInfo");
        params.put("venue_id", id);
        Response r = sendUpcomingRequest("GET", UPCOMING_ENDPOINT, params);
        if (r.getVenues().size() == 0) {
            return null;
        } else {
            return r.getVenues().get(0);
        }
    }

    public Metro getMetro(double lat, double lon) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("method", "metro.getForLatLon");
        map.put("latitude", "" + lat);
        map.put("longitude", "" + lon);
        Response r = sendUpcomingRequest("GET", UPCOMING_ENDPOINT, map);
        List<Metro> metroList = r.getMetroList();
        if (metroList.size() == 0) {
            return null;
        } else {
            return metroList.get(0);
        }
    }

    public List<Event> findEventsNear(double lat, double lon, Integer radius) {
        EventSearchCriteria crit = new EventSearchCriteria();
        crit.setLatLong(lat, lon);
        if (radius != null) {
            crit.setRadius(radius.intValue());
        }
        return findEvents(crit);
    }

    public List<Event> findEventsNear(String location, Integer radius) {
        EventSearchCriteria crit = new EventSearchCriteria();
        crit.setLocation(location);
        if (radius != null) {
            crit.setRadius(radius.intValue());
        }
        return findEvents(crit);
    }

    public List<Event> findEventsNear(String search, double lat, double lon, Integer radius) {
        EventSearchCriteria crit = new EventSearchCriteria();
        crit.setSearch(search);
        crit.setLatLong(lat, lon);
        if (radius != null) {
            crit.setRadius(radius.intValue());
        }
        return findEvents(crit);
    }

    public List<Event> findEvents(String search) {
        EventSearchCriteria crit = new EventSearchCriteria();
        crit.setSearch(search);
        return findEvents(crit);
    }

    public List<Event> findEvents(EventSearchCriteria crit) {
        Response r = sendUpcomingRequest("GET", UPCOMING_ENDPOINT, crit.toMap());
        return r.getEvents();
    }

    public List<Event> getBestInPlaceByWhereOnEarthId(String woeid) {
        EventSearchCriteria crit = new EventSearchCriteria();
        crit.setWhereOnEarthId(woeid);
        crit.setBest(true);
        return findEvents(crit);
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public void setCompressionEnabled(boolean b) {
        this.compressionEnabled = b;
    }

    public void setUpcomingApiKey(String key) {
        this.upcomingApiKey = key;
    }

    /**
	 * 
	 * 
	 * @return String that contains iCalendar file contents
	 * 
	 */
    public String getICalendarForPlace(String placeId) {
        if ((placeId == null) || (placeId.trim().length() == 0)) {
            throw new IllegalArgumentException("placeId: " + placeId);
        }
        return get("http://upcoming.yahoo.com/calendar/v2/place/" + placeId);
    }

    /**
	 * 
	 * 
	 * 
	 * @return String containing RSS file contents
	 * 
	 */
    public String getRSSForPlace(String placeId) {
        if ((placeId == null) || (placeId.trim().length() == 0)) {
            throw new IllegalArgumentException("placeId: " + placeId);
        }
        return get("http://upcoming.yahoo.com/syndicate/v2/place/" + placeId);
    }

    public String getPostalCode(double lat, double lon) {
        String url = "http://ws.geonames.org/findNearbyPostalCodes";
        HashMap<String, String> queryParams = new LinkedHashMap<String, String>();
        queryParams.put("lat", "" + lat);
        queryParams.put("lng", "" + lon);
        String xml = sendHttpRequest("GET", url, queryParams);
        return xml;
    }

    /**
	 * 
	 * 
	 *   send HTTP GET
	 *   
	 *   This method can be used to retrieve images  (JPEG, PNG, GIF)
	 *   or any other file type
	 *   
	 *   @return byte array
	 *  
	 */
    public byte[] getBytesFromUrl(String url) {
        try {
            HttpGet get = new HttpGet(url);
            HttpResponse response = this.getHttpClient().execute(get);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new RuntimeException("response body was empty");
            }
            return EntityUtils.toByteArray(entity);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
	 * 
	 * send HTTP GET
	 * 
	 */
    public String get(String url) {
        try {
            HttpGet get = new HttpGet(url);
            HttpResponse response = this.getHttpClient().execute(get);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new RuntimeException("response body was empty");
            }
            return EntityUtils.toString(entity);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Category> getCategories() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("method", "category.getList");
        Response r = sendUpcomingRequest("GET", UPCOMING_ENDPOINT, map);
        return r.getCategories();
    }

    public void shutdown() {
        try {
            this.getHttpClient().getConnectionManager().shutdown();
        } catch (Exception ignore) {
        }
    }
}
