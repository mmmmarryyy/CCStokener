package org.placelab.mapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import org.placelab.collections.HashMap;
import org.placelab.collections.Iterator;
import org.placelab.collections.LinkedList;
import org.placelab.collections.List;
import org.placelab.core.PlacelabProperties;
import org.placelab.core.TwoDCoordinate;
import org.placelab.core.Types;
import org.placelab.util.NumUtil;
import org.placelab.util.StringUtil;

/**
 * This class is used by the {@link WigleMapper} to actually 
 * screen-scrape for the Beacon data.  Yes, screen-scraping is 
 * the only way to do it as of this writing.
 * <p>
 * It uses the following system properties for wigle authentication:
 * <pre>
 * placelab.wigle_username
 * placelab.wigle_password
 * </pre>
 * 
 * 
 * 
 */
public class WigleDownloader {

    protected String cookie;

    public static String AUTH_URL = "http://wigle.net/gps/gps/GPSDB/login";

    public static String QUERY_URL = "http://wigle.net/gpsopen/gps/GPSDB/confirmquery";

    public static final int REALLY_BAD = 1;

    public static final int NOT_SO_BAD = 2;

    public static final int DONE = 4;

    public WigleDownloader() {
        cookie = null;
    }

    public boolean isAuthenticated() {
        return cookie != null;
    }

    public boolean authenticate() throws IOException {
        return authenticate(PlacelabProperties.get("placelab.wigle_username"), PlacelabProperties.get("placelab.wigle_password"));
    }

    public boolean authenticate(String user, String pass) throws IOException {
        URL auth_url = new URL(AUTH_URL);
        HttpURLConnection urlConn = (HttpURLConnection) auth_url.openConnection();
        urlConn.setInstanceFollowRedirects(false);
        urlConn.setDoOutput(true);
        urlConn.setRequestMethod("POST");
        urlConn.connect();
        String credstr = "credential_0=" + URLEncoder.encode(user) + "&credential_1=" + URLEncoder.encode(pass) + "&noexpire=1";
        OutputStreamWriter out = new OutputStreamWriter(urlConn.getOutputStream());
        out.write(credstr);
        out.flush();
        out.close();
        String cookie_header = urlConn.getHeaderField("Set-Cookie");
        if (null == cookie_header) return false;
        StringTokenizer st = new StringTokenizer(cookie_header, ";");
        cookie = st.nextToken();
        return true;
    }

    public Iterator query(TwoDCoordinate coord1, TwoDCoordinate coord2) {
        return query(coord1.getLatitude(), coord1.getLongitude(), coord2.getLatitude(), coord2.getLongitude());
    }

    public Iterator query(double lat1, double lon1, double lat2, double lon2) {
        return query("latrange1=" + NumUtil.doubleToString(lat1, 6) + "&latrange2=" + NumUtil.doubleToString(lat2, 6) + "&longrange1=" + NumUtil.doubleToString(lon1, 6) + "&longrange2=" + NumUtil.doubleToString(lon2, 6));
    }

    public Iterator query(String query) {
        return new WigleIterator(query);
    }

    private class WigleIterator implements Iterator {

        private String baseQuery;

        private int pageStart;

        private Iterator current;

        public WigleIterator(String query) {
            baseQuery = query;
            pageStart = 0;
            current = getNext();
        }

        public boolean hasNext() {
            return current != null && current.hasNext();
        }

        public Object next() {
            Object next = current.next();
            if (!current.hasNext()) current = getNext();
            return next;
        }

        public void remove() {
            throw new UnsupportedOperationException("Wigle does not support remove()");
        }

        public Iterator getNext() {
            try {
                List beacons = downloadQuery(baseQuery, pageStart);
                pageStart += 1000;
                return beacons == null ? null : beacons.iterator();
            } catch (IOException e) {
                return null;
            }
        }

        public List downloadQuery(String query, int start) throws IOException {
            if (cookie == null) return null;
            String fullQuery = QUERY_URL + "?" + query;
            if (start != 0) fullQuery = fullQuery + "&pagestart=" + start;
            URL queryURL = new URL(fullQuery);
            URLConnection urlConn = queryURL.openConnection();
            urlConn.setRequestProperty("Cookie", cookie);
            urlConn.setRequestProperty("Accept-Encoding", "gzip");
            urlConn.connect();
            InputStream input = urlConn.getInputStream();
            String encode = urlConn.getContentEncoding();
            if ("gzip".equalsIgnoreCase(encode)) {
                input = new GZIPInputStream(input);
            }
            List beacons = new LinkedList();
            parseStream(input, beacons);
            input.close();
            return beacons;
        }

        public void parseStream(InputStream input, List beacons) throws IOException {
            BufferedReader messagesReader = new BufferedReader(new InputStreamReader(input));
            String line;
            for (; ; ) {
                try {
                    line = messagesReader.readLine();
                } catch (Exception ex) {
                    line = null;
                    System.out.println("*** Wigle gave me some bad stream");
                }
                if (line == null) break;
                while (parseLine(line, beacons) == NOT_SO_BAD) {
                    String nextLine = messagesReader.readLine();
                    if (nextLine == null) break;
                    if (parseLine(nextLine, beacons) == 0) break;
                    line = line + nextLine;
                }
            }
        }

        public int parseLine(String line, List beacons) {
            String realLine = line;
            if (line.startsWith("Showing")) {
                int index = line.indexOf("<p>");
                if (index != -1) {
                    String message = line.substring(0, index);
                }
                index = line.indexOf("Next 1000 >>");
                return DONE;
            }
            if (!line.startsWith("<td>")) return REALLY_BAD;
            int index = line.indexOf("</td></tr>");
            if (index != -1) line = line.substring(4, index);
            String[] fields = StringUtil.split(line, "</td><td>");
            if (fields.length < 12) {
                return NOT_SO_BAD;
            }
            HashMap beacon = new HashMap();
            beacon.put(Types.TYPE, Types.WIFI);
            beacon.put(Types.LATITUDE, fields[10]);
            beacon.put(Types.LONGITUDE, fields[11]);
            beacon.put(Types.ID, fields[0].toLowerCase());
            beacon.put(Types.HUMANREADABLENAME, fields[1]);
            beacons.add(StringUtil.hashMapToStorageString(beacon));
            return 0;
        }
    }
}
