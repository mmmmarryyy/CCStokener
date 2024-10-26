package plugins.fanart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import com.umc.dao.UMCDataAccessInterface;
import com.umc.plugins.moviedb.AbstractWebSearchPlugin;

public class GoogleWebSearch extends AbstractWebSearchPlugin {

    private static Logger log = Logger.getLogger("com.umc.plugin.websearch");

    private UMCDataAccessInterface dao = null;

    public GoogleWebSearch() {
        setPluginDBVersion(1);
        setPluginDescriptionEnglish("This plugin enables UMC to search on Google");
        setPluginDescriptionFrench(null);
        setPluginDescriptionGerman("Mit diesem Plugin ist es möglich auf Google zu suchen");
        setPluginNameEnglish("Google-Plugin");
        setPluginNameFrench("Google-Plugin");
        setPluginNameGerman("Google-Plugin");
        setPluginVersion(0.1);
    }

    public Collection<String> searchFor(String aSearchItem) {
        return null;
    }

    public String searchForOFDB(String aTitle, int aYear) {
        int bestMatch = 1000;
        Scanner scanner = null;
        int wordCount = 0;
        String result = null;
        try {
            String ofdbID = null;
            StringBuffer sb = new StringBuffer("http://www.google.de/search?hl=de&q=allintitle:+");
            sb.append(URLEncoder.encode(aTitle, "UTF-8"));
            if (aYear >= 1900 && aYear <= 2100) {
                sb.append("+%28").append(aYear).append("%29");
            }
            sb.append("+site%3Awww.ofdb.de/film");
            String html = request(new URL(sb.toString()));
            if (Pattern.matches(".*http://www.ofdb.de/film/[0-9].*", html) || Pattern.matches(".*http://www.ofdb.de/review/[0-9].*", html)) {
                Pattern p = Pattern.compile("href=\"(http://www\\.ofdb\\.de.*?)(\\?|\">)");
                Matcher m = p.matcher(html);
                while (m.find()) {
                    String url = m.group(1);
                    if (url != null) {
                        String onlineTitle = url.substring(url.lastIndexOf(",") + 1, url.length()).replaceAll("--", "-");
                        onlineTitle = onlineTitle.replaceAll("-", " ");
                        onlineTitle = onlineTitle.replaceAll("  ", " ");
                        if (compareTitles(aTitle, aYear + "", onlineTitle)) {
                            wordCount = 0;
                            scanner = new Scanner(onlineTitle);
                            while (scanner.hasNext()) {
                                scanner.next();
                                wordCount++;
                            }
                            if (bestMatch > wordCount) {
                                bestMatch = wordCount;
                                result = url;
                            }
                        }
                    }
                }
                p = Pattern.compile("film/\\d+");
                m = p.matcher(result);
                m.find();
                ofdbID = m.group();
                if (ofdbID != null) {
                    ofdbID = ofdbID.replaceAll("film/", "");
                } else {
                    p = Pattern.compile("review/\\d+");
                    m = p.matcher(html);
                    m.find();
                    ofdbID = m.group();
                    if (ofdbID != null) {
                        ofdbID = ofdbID.replaceAll("review/", "");
                    }
                }
            }
            return ofdbID;
        } catch (Exception exc) {
            return null;
        }
    }

    public String searchForIMDB2(String aTitle, int aYear) {
        int bestMatch = 1000;
        Scanner scanner = null;
        int wordCount = 0;
        String result = null;
        try {
            String ofdbID = null;
            StringBuffer sb = new StringBuffer("http://www.google.de/search?hl=de&q=allintitle:+");
            sb.append(URLEncoder.encode(aTitle, "UTF-8"));
            if (aYear >= 1900 && aYear <= 2100) {
                sb.append("+%28").append(aYear).append("%29");
            }
            sb.append("+site%3Awww.imdb.com/title");
            String html = request(new URL(sb.toString()));
            if (Pattern.matches(".*http://www.imdb.com/title/tt[0-9].*", html)) {
                Pattern p = Pattern.compile("href=\"(http://www\\.imdb\\.de.*?)(\\?|\">)");
                Matcher m = p.matcher(html);
                while (m.find()) {
                    String url = m.group(1);
                    if (url != null) {
                        String onlineTitle = url.substring(url.lastIndexOf(",") + 1, url.length()).replaceAll("--", "-");
                        onlineTitle = onlineTitle.replaceAll("-", " ");
                        onlineTitle = onlineTitle.replaceAll("  ", " ");
                        if (compareTitles(aTitle, aYear + "", onlineTitle)) {
                            wordCount = 0;
                            scanner = new Scanner(onlineTitle);
                            while (scanner.hasNext()) {
                                scanner.next();
                                wordCount++;
                            }
                            if (bestMatch > wordCount) {
                                bestMatch = wordCount;
                                result = url;
                            }
                        }
                    }
                }
                p = Pattern.compile("film/\\d+");
                m = p.matcher(result);
                m.find();
                ofdbID = m.group();
                if (ofdbID != null) {
                    ofdbID = ofdbID.replaceAll("film/", "");
                } else {
                    p = Pattern.compile("review/\\d+");
                    m = p.matcher(html);
                    m.find();
                    ofdbID = m.group();
                    if (ofdbID != null) {
                        ofdbID = ofdbID.replaceAll("review/", "");
                    }
                }
            }
            return ofdbID;
        } catch (Exception exc) {
            return null;
        }
    }

    public String searchForIMDB(String aTitle, int aYear) {
        try {
            String imdbID = null;
            StringBuffer sb = new StringBuffer("http://www.google.de/search?hl=de&q=");
            sb.append(URLEncoder.encode(aTitle, "UTF-8"));
            if (aYear >= 1900 && aYear <= 2100) {
                sb.append("+%28").append(aYear).append("%29");
            }
            sb.append("+site%3Awww.imdb.com/title");
            String html = request(new URL(sb.toString()));
            if (Pattern.matches(".*http://www.imdb.com/title/tt[0-9].*", html)) {
                Pattern p = Pattern.compile("title/tt\\d+");
                Matcher m = p.matcher(html);
                m.find();
                imdbID = m.group();
                if (imdbID != null) {
                    imdbID = imdbID.replaceAll("title/", "");
                }
            }
            return imdbID;
        } catch (Exception exc) {
            return null;
        }
    }

    public String searchForTheMovieDB(String aTitle, int aYear) {
        try {
            String themoviedbID = null;
            StringBuffer sb = new StringBuffer("http://www.google.de/search?hl=de&q=");
            sb.append(URLEncoder.encode(aTitle, "UTF-8"));
            if (aYear >= 1900 && aYear <= 2100) {
                sb.append("+%28").append(aYear).append("%29");
            }
            sb.append("+site%3Awww.themoviedb.org/movie");
            String html = request(new URL(sb.toString()));
            if (Pattern.matches(".*http://www.themoviedb.org/movie/[0-9]*.*", html)) {
                Pattern p = Pattern.compile("movie/\\d+");
                Matcher m = p.matcher(html);
                m.find();
                themoviedbID = m.group();
                if (themoviedbID != null) {
                    themoviedbID = themoviedbID.replaceAll("movie/", "");
                }
            }
            return themoviedbID;
        } catch (Exception exc) {
            return null;
        }
    }

    private static String request(URL url) throws IOException {
        StringWriter sw = null;
        try {
            sw = new StringWriter();
            BufferedReader in = null;
            try {
                URLConnection con = url.openConnection();
                con.setRequestProperty("User-Agent", "Mozilla/5.25 Netscape/5.0 (Windows; I; Win95)");
                in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                String line;
                while ((line = in.readLine()) != null) {
                    sw.write(line);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
            return sw.toString();
        } finally {
            if (sw != null) sw.close();
        }
    }

    /**
	 * Diese Methode vergleicht den übergebenen Titel mit einem Titel aus der Online-Filmdatenbank.
	 * 
	 * @param aTitle
	 * @param onlineTitle
	 * @return true/false
	 */
    private boolean compareTitles(String aTitle, String aYear, String onlineTitle) {
        int tokens = 0;
        int found = 0;
        aTitle = aTitle.toLowerCase();
        onlineTitle = onlineTitle.toLowerCase();
        log.debug("VERGLEICHE " + aTitle + " mit " + onlineTitle);
        Scanner scanner = new Scanner(aTitle);
        if (aYear != null && !aYear.equals("") && !aYear.equals("-1")) scanner = new Scanner(aTitle + " " + aYear);
        String t = "";
        while (scanner.hasNext()) {
            t = scanner.next().toLowerCase();
            if (t.length() > 1 || (t.length() == 1 && !Pattern.matches("[^0-9a-zA-ZöÖüÜäÄ]", t))) {
                tokens++;
                if (Pattern.matches(".*(\\A|[^0-9a-zA-ZüÜäÄöÖ])" + t + "([^0-9a-zA-ZüÜäÄöÖ]|\\Z).*", onlineTitle)) {
                    found++;
                }
            }
        }
        if (tokens == found) return true;
        return false;
    }
}
