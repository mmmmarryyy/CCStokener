package org.elmarweber.sf.appletrailerfs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Scans the apple trailers JSON feeds to get a list of all movies and their
 * trailers.
 * 
 * @author Elmar Weber (appletrailerfs@elmarweber.org)
 */
public class UpdateTrailers {

    private static Log log = LogFactory.getLog(UpdateTrailers.class);

    public static void main(String[] args) throws Exception {
        String content = readDocument("http://www.apple.com/trailers/home/feeds/just_hd.json");
        FileUtils.writeStringToFile(new File("./just_hd.json"), content);
        JSONArray movies = new JSONArray(content);
        FileUtils.writeStringToFile(new File("./just_hd-formatted.json"), movies.toString(4));
        for (int i = 0; i < movies.length(); i++) {
            JSONObject movie = movies.getJSONObject(i);
            log.debug(movie.get("title"));
        }
        log.debug(movies.length() + " movies in feed");
        JSONArray database = createDatabase(movies);
        FileUtils.writeStringToFile(new File("./just_hd-database.json"), database.toString());
        FileUtils.writeStringToFile(new File("./just_hd-database-formatted.json"), database.toString(4));
    }

    /**
     * Parses the specified JSON movie array and creates the movie database that
     * is used by the appletrailerfs.
     * 
     * @param movies
     *            the {@link JSONArray} from a movie feed from the trailer page.
     * 
     * @return the JSON movie database.
     * 
     * @throws JSONException
     *             in case any error occurs while parsing the JSON content.
     * @throws MalformedURLException
     *             in case any trailer URL is invalid.
     * @throws IOException
     *             in case any error occurs while downloading trailer resources.
     */
    private static JSONArray createDatabase(JSONArray movies) throws JSONException, MalformedURLException, IOException {
        List<String> trailerDebug = new ArrayList<String>();
        JSONArray database = new JSONArray();
        for (int i = 0; i < movies.length(); i++) {
            JSONObject movie = movies.getJSONObject(i);
            JSONObject dbMovie = createDatabaseMovie(movie);
            JSONArray dbTrailers = new JSONArray();
            dbMovie.put("trailers", dbTrailers);
            for (int j = 0; j < movie.getJSONArray("trailers").length(); j++) {
                String type = movie.getJSONArray("trailers").getJSONObject(j).getString("type");
                trailerDebug.add(type);
            }
            String location = "http://www.apple.com" + movie.getString("location");
            String html = readDocument(location);
            int lastIndex = html.lastIndexOf("href=");
            int index = 0;
            while (index != lastIndex) {
                index = html.indexOf("href=", index + 1);
                if (index != -1) {
                    int from = html.indexOf('"', index + "href=".length()) + 1;
                    int to = html.indexOf('"', from);
                    if ((from != -1) && (to != -1) && (from != to)) {
                        String href = html.substring(from, to);
                        if (href.contains(".mov")) {
                            String realMovName = extractRealMovName(href);
                            String movUrl = href.substring(0, href.lastIndexOf('/')) + "/" + realMovName;
                            int contentLength = getContentLength(movUrl);
                            String type = detectType(movie, dbTrailers, realMovName);
                            String resolution = detectResolution(realMovName);
                            JSONObject dbTrailer = new JSONObject();
                            dbTrailer.put("filename", realMovName);
                            dbTrailer.put("type", type);
                            dbTrailer.put("resolution", resolution);
                            dbTrailer.put("movurl", movUrl);
                            dbTrailer.put("contentlength", contentLength);
                            dbTrailers.put(dbTrailer);
                            trailerDebug.add(realMovName);
                        }
                    }
                }
            }
            if (dbTrailers.length() > 0) {
                database.put(dbMovie);
            }
            trailerDebug.add("--------------");
            log.debug("Processed " + (i + 1) + "/" + movies.length() + " movie trailers (" + movie.getString("title") + ")");
        }
        FileUtils.writeLines(new File("./trailerDebug"), trailerDebug);
        return database;
    }

    private static String detectType(JSONObject movie, JSONArray dbTrailers, String movName) throws JSONException {
        JSONArray trailers = movie.getJSONArray("trailers");
        if (trailers.length() == 0) {
            log.warn("Could not detect trailer type: movie " + movie.getString("title") + " has no trailer objects");
            return "Unknown";
        } else if (trailers.length() == 1) {
            return trailers.getJSONObject(0).getString("type");
        } else {
            int trailerIndex = 0;
            String[] fileNames = new String[dbTrailers.length() + 1];
            int[] distances = new int[dbTrailers.length()];
            for (int i = 0; i < dbTrailers.length(); i++) {
                fileNames[i] = dbTrailers.getJSONObject(i).getString("filename");
            }
            fileNames[dbTrailers.length()] = movName;
            for (int i = 0; i < distances.length; i++) {
                distances[i] = StringUtils.getLevenshteinDistance(fileNames[i], fileNames[i + 1]);
            }
            for (int i = 0; i < distances.length; i++) {
                if (distances[i] > 4) {
                    trailerIndex++;
                }
            }
            if (trailerIndex >= trailers.length()) {
                log.warn("Could not detect trailer type for trailer " + movName + " of movie " + movie.getString("title") + " computed trailer index is out of range");
                return "Unknown";
            } else {
                return trailers.getJSONObject(trailerIndex).getString("type");
            }
        }
    }

    private static String detectResolution(String movName) {
        if (movName.contains("1080p")) {
            return "1080p";
        } else if (movName.contains("720p")) {
            return "720p";
        } else if (movName.contains("480p")) {
            return "480p";
        } else if (movName.contains("640")) {
            return "Large";
        } else if (movName.contains("480")) {
            return "Medium";
        } else if (movName.contains("320")) {
            return "Small";
        } else {
            log.warn("Could not detect resolution of trailer with filename " + movName);
            return "Unknown";
        }
    }

    /**
     * Creates a movie that is viable for the appletrailerfs database based on
     * the specified original movie object.
     * 
     * @param movie
     *            the original movie object.
     * 
     * @return an adapted movie object for the appletrailerfs database.
     * 
     * @throws JSONException
     *             in case any error occurs while parsing the JSON content.
     */
    private static JSONObject createDatabaseMovie(JSONObject movie) throws JSONException {
        JSONObject dbMovie = new JSONObject();
        dbMovie.put("title", movie.get("title"));
        return dbMovie;
    }

    /**
     * 
     * @param location
     *            the URL to check the content length of.
     * 
     * @return the content length of the file behind the specified URL.
     * 
     * @throws MalformedURLException
     *             in case <code>location</code> is invalid.
     * @throws IOException
     *             in case any error occurs while opening a URL.
     */
    private static int getContentLength(String location) throws MalformedURLException, IOException {
        URL url = new URL(location);
        return url.openConnection().getContentLength();
    }

    /**
     * Scans the pseudo .mov file that is returned as a first link and extracts
     * the real .mov file name that is used for the download.
     * 
     * @param location
     *            the pseudo .mov file location (URL).
     * @return the name of the real .mov file behind the specified URL.
     * 
     * @throws MalformedURLException
     *             in case <code>location</code> is invalid.
     * @throws IOException
     *             in case any error occurs while opening a URL.
     */
    private static String extractRealMovName(String location) throws MalformedURLException, IOException {
        String movResource = readDocument(location);
        int to = movResource.indexOf(".mov") + ".mov".length();
        int from = -1;
        for (int i = to - 1; i > 0; i--) {
            char c = movResource.charAt(i);
            if (Character.isLetter(c) || Character.isDigit(c) || (c == '.') || (c == '-') || (c == '_')) {
                from = i;
            } else {
                break;
            }
        }
        String movName = movResource.substring(from, to);
        while (!Character.isLetter(movName.charAt(0))) {
            movName = movName.substring(1);
        }
        return movName;
    }

    /**
     * Downloads the contents of the specified location and returns it as a
     * string.
     * 
     * @param location
     *            the location / URL of the document to download.
     * 
     * @return the content of the specified URL in one string.
     * 
     * @throws MalformedURLException
     *             in case <code>location</code> is invalid.
     * @throws IOException
     *             in case any error occurs while opening a URL.
     */
    private static String readDocument(String location) throws MalformedURLException, IOException {
        String content;
        URL url = new URL(location);
        URLConnection con = url.openConnection();
        InputStream in = con.getInputStream();
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, read));
        }
        content = sb.toString();
        return content;
    }
}
