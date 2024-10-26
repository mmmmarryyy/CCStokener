package com.moviejukebox.gui.tools;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.MovieDatabasePlugin;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

public class OfdbPlugin {

    protected static Logger logger = Logger.getLogger("moviejukebox");

    protected int maxZeichen;

    public static String OFDB_PLUGIN_ID = "ofdb";

    protected int maxGenres;

    com.moviejukebox.plugin.ImdbPlugin imdbp;

    public OfdbPlugin() {
        imdbp = new com.moviejukebox.plugin.ImdbPlugin();
        try {
            String temp = PropertiesUtil.getProperty("ofdb.zeichen.max", "600");
            maxZeichen = Integer.parseInt(temp);
        } catch (NumberFormatException ex) {
            maxZeichen = 600;
        }
        try {
            String temp = PropertiesUtil.getProperty("imdb.genres.max", "9");
            System.out.println("imdb.genres.max=" + temp);
            maxGenres = Integer.parseInt(temp);
        } catch (NumberFormatException ex) {
            maxGenres = 9;
        }
    }

    public void scan(Movie mediaFile) {
        imdbp.scan(mediaFile);
        if (mediaFile.getId("ofdb").equalsIgnoreCase("UNKNOWN")) {
            getOfdbId(mediaFile);
        }
        this.updateOfdbMediaInfo(mediaFile);
    }

    public void getOfdbId(Movie mediaFile) {
        if (mediaFile.getId("ofdb").equalsIgnoreCase("UNKNOWN")) {
            if (!mediaFile.getId("imdb").equalsIgnoreCase("UNKNOWN")) {
                mediaFile.setId("ofdb", getOfdbIdFromOfdb(mediaFile.getId("imdb")));
            } else {
                mediaFile.setId("ofdb", getofdbIDfromGoogle(mediaFile.getTitle(), mediaFile.getYear()));
            }
        }
    }

    public String getOfdbIdFromOfdb(String imdbId) {
        try {
            URL url;
            URLConnection urlConn;
            DataOutputStream printout;
            url = new URL("http://www.ofdb.de/view.php?page=suchergebnis");
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            printout = new DataOutputStream(urlConn.getOutputStream());
            String content = "&SText=" + imdbId + "&Kat=IMDb";
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            StringWriter site = new StringWriter();
            BufferedReader input = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));
            String line;
            while ((line = input.readLine()) != null) {
                site.write(line);
            }
            input.close();
            String xml = site.toString();
            String ofdbID;
            int beginIndex = xml.indexOf("film/");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(xml.substring(beginIndex), "\"");
                ofdbID = st.nextToken();
                ofdbID = "http://www.ofdb.de/" + ofdbID;
            } else {
                ofdbID = "UNKNOWN";
            }
            System.out.println(ofdbID);
            return ofdbID;
        } catch (Exception e) {
            logger.severe("Failed retreiving ofdb URL for movie : ");
            logger.severe("Error : " + e.getMessage());
            return "UNKNOWN";
        }
    }

    private String getofdbIDfromGoogle(String movieName, String year) {
        try {
            String ofdbID = "UNKNOWN";
            StringBuffer sb = new StringBuffer("http://www.google.de/search?hl=de&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));
            if (year != null && !year.equalsIgnoreCase("UNKNOWN")) {
                sb.append("+%28").append(year).append("%29");
            }
            sb.append("+site%3Awww.ofdb.de/film");
            String xml = request(new URL(sb.toString()));
            int beginIndex = xml.indexOf("http://www.ofdb.de/film");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(xml.substring(beginIndex), "\"");
                ofdbID = st.nextToken();
            } else {
                ofdbID = "UNKNOWN";
            }
            return ofdbID;
        } catch (Exception e) {
            logger.severe("Failed retreiving ofdb Id for movie : " + movieName);
            logger.severe("Error : " + e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * Scan OFDB html page for the specified movie
     */
    private void updateOfdbMediaInfo(Movie movie) {
        try {
            if (movie.getId(OFDB_PLUGIN_ID).equalsIgnoreCase("UNKNOWN")) {
                System.out.println("no ofdb Id");
            }
            String xml = request(new URL(movie.getId(OFDB_PLUGIN_ID)));
            movie.setTitleSort(extractTag(xml, "<title>OFDb - ", 0, "("));
            movie.setTitle(movie.getTitleSort());
            movie.setRating(parseRating(extractTag(xml, "<br>Note: ", 0)));
            movie.setDirector(extractTag(xml, "Regie:", 11));
            movie.setCountry(extractTag(xml, "Herstellungsland:", 11));
            int count = 0;
            movie.getGenres().removeAll(movie.getGenres());
            for (String genre : extractTags(xml, "Genre(s):", "</table>", "<a href=\"view.php?page=genre&Genre=", "</a>")) {
                movie.addGenre(genre);
                if (++count >= maxGenres) {
                    break;
                }
            }
            URL plotURL = new URL("http://www.ofdb.de/plot/" + extractTag(xml, "<a href=\"plot/", 0, "\""));
            String plot = getPlot(movie, plotURL);
            movie.setPlot(plot);
            String outline = plot;
            if (outline.length() > 150) {
                outline = outline.substring(0, 150) + "... ";
            }
            movie.setOutline(outline);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String request(URL url) throws IOException {
        StringWriter content = null;
        try {
            content = new StringWriter();
            BufferedReader in = null;
            try {
                URLConnection cnx = url.openConnection();
                cnx.setRequestProperty("User-Agent", "Mozilla/5.25 Netscape/5.0 (Windows; I; Win95)");
                in = new BufferedReader(new InputStreamReader(cnx.getInputStream(), "UTF-8"));
                String line;
                while ((line = in.readLine()) != null) {
                    content.write(line);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
            return content.toString();
        } finally {
            if (content != null) {
                content.close();
            }
        }
    }

    protected String extractTag(String src, String findStr, int skip) {
        return this.extractTag(src, findStr, skip, "><");
    }

    protected String extractTag(String src, String findStr, int skip, String separator) {
        int beginIndex = src.indexOf(findStr);
        StringTokenizer st = new StringTokenizer(src.substring(beginIndex + findStr.length()), separator);
        for (int i = 0; i < skip; i++) {
            st.nextToken();
        }
        String value = HTMLTools.decodeHtml(st.nextToken().trim());
        if (value.indexOf("uiv=\"content-ty") != -1 || value.indexOf("cast") != -1 || value.indexOf("title") != -1 || value.indexOf("<") != -1) {
            value = "UNKNOWN";
        }
        return value;
    }

    private int parseRating(String rating) {
        StringTokenizer st = new StringTokenizer(rating, "/ ()");
        try {
            return (int) (Float.parseFloat(st.nextToken()) * 10);
        } catch (Exception e) {
            return -1;
        }
    }

    protected ArrayList<String> extractTags(String src, String sectionStart, String sectionEnd) {
        return extractTags(src, sectionStart, sectionEnd, null, "|");
    }

    protected ArrayList<String> extractTags(String src, String sectionStart, String sectionEnd, String startTag, String endTag) {
        ArrayList<String> tags = new ArrayList<String>();
        int index = src.indexOf(sectionStart);
        if (index == -1) {
            return tags;
        }
        index += sectionStart.length();
        int endIndex = src.indexOf(sectionEnd, index);
        if (endIndex == -1) {
            return tags;
        }
        String sectionText = src.substring(index, endIndex);
        int lastIndex = sectionText.length();
        index = 0;
        int startLen = 0;
        int endLen = endTag.length();
        if (startTag != null) {
            index = sectionText.indexOf(startTag);
            startLen = startTag.length();
        }
        while (index != -1) {
            index += startLen;
            int close = sectionText.indexOf('>', index);
            if (close != -1) {
                index = close + 1;
            }
            endIndex = sectionText.indexOf(endTag, index);
            if (endIndex == -1) {
                endIndex = lastIndex;
            }
            String text = sectionText.substring(index, endIndex);
            tags.add(HTMLTools.decodeHtml(text.trim()));
            endIndex += endLen;
            if (endIndex > lastIndex) {
                break;
            }
            if (startTag != null) {
                index = sectionText.indexOf(startTag, endIndex);
            } else {
                index = endIndex;
            }
        }
        return tags;
    }

    private String getPlot(Movie movie, URL plotURL) {
        String plot;
        try {
            String xml = request(plotURL);
            int firstindex = xml.indexOf("gelesen</b></b><br><br>") + 23;
            int lastindex = xml.indexOf("</font>", firstindex);
            plot = xml.substring(firstindex, lastindex);
            plot = plot.replaceAll("<br />", " ");
            if (plot.length() > maxZeichen) {
                plot = plot.substring(0, maxZeichen - 3) + "...";
            }
        } catch (Exception e) {
            plot = "None";
        }
        return plot;
    }
}
