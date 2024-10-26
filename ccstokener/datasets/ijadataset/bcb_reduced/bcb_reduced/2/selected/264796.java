package net.sf.jabref.imports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;
import org.xml.sax.SAXException;

public class ACMFetcher implements EntryFetcher {

    protected boolean shouldContinue = true;

    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter frame) {
        shouldContinue = true;
        int parsed = 0;
        try {
            String hitlist = executeQuery(query);
            int hits = parseNumberOfHits(hitlist);
            if (hits == 0) {
                frame.showMessage(Globals.lang("No entries found for the search string '%0'", query), Globals.lang("Search ACM Digital Library"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
            List<String> articleURLs = loadArticleURLs(hitlist);
            for (String articleURL : articleURLs) {
                if (!shouldContinue) break;
                BibtexEntry entry = fetchArticleBibtex(articleURL);
                dialog.addEntry(entry);
                dialog.setProgress(++parsed, hits);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (ConnectException e) {
            frame.showMessage(Globals.lang("Connection to ACM failed"), Globals.lang("Search ACM"), JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Parses all articles on the given result page and following result pages.
     * 
     * @param hitlist
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public List<String> loadArticleURLs(String hitlist) throws IOException {
        List<String> list = new ArrayList<String>();
        String nextPageUrl = null;
        String page = hitlist;
        do {
            if (!shouldContinue) break;
            if (nextPageUrl != null) {
                page = fetchSite(new URL(nextPageUrl));
            }
            nextPageUrl = hasFollowingPage(page);
            List<String> urls = parseArticleURLs(page);
            for (String uri : urls) {
                list.add(uri);
            }
        } while (nextPageUrl != null);
        return list;
    }

    /**
     * Loads all urls to articles from a given hitlist html code.
     * 
     * @param page
     *            the hitlists html code
     * @return a list of absolute urls to articles
     */
    public List<String> parseArticleURLs(String page) {
        List<String> t = new ArrayList<String>();
        Pattern p = Pattern.compile("citation\\.cfm(.*?)\"");
        Matcher m = p.matcher(page);
        while (m.find()) {
            if (!shouldContinue) break;
            String url = "http://portal.acm.org/" + m.group().substring(0, m.group().length() - 1);
            if (url.indexOf("#") > 0) {
                continue;
            }
            t.add(url);
        }
        return t;
    }

    /**
     * Reads the number of hits for a given hitlist
     * 
     * @param hitlist
     *            The hitlists html code
     * @return The number of hits that can be fetched via the result pages. If
     *         there are more than 200 results, the ACM digital library only
     *         displays 200 results.
     */
    public int parseNumberOfHits(String hitlist) {
        if (isEmptyResult(hitlist)) {
            return 0;
        }
        String t = hitlist.substring(hitlist.indexOf("Found <b>") + 9, hitlist.indexOf("</b>", hitlist.indexOf("Found <b>")));
        t = t.replaceAll(",", "");
        t = t.replaceAll("\\.", "");
        try {
            int h = Integer.parseInt(t);
            return h > 200 ? 200 : h;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Checks if the result page returned indicates that there are no results
     * for the search query
     * 
     * @param result
     *            Result pages html code
     * @return True if the result page html code indicates there are no results
     */
    public static boolean isEmptyResult(String result) {
        return result.contains("did not return any results");
    }

    /**
     * Loads an articles html code and builds the bibtex-output-url. Fetches it
     * and parses the bibtex to a {@see BibtexEntry}
     * 
     * @param urlToArticle
     * @return
     * @throws IOException
     */
    public BibtexEntry fetchArticleBibtex(String urlToArticle) throws IOException {
        String page = fetchSite(new URL(urlToArticle));
        String biburl = makeBibtexURL(page);
        String entryID = biburl.substring(biburl.indexOf("?") + 1, biburl.indexOf("&"));
        page = fetchSite(new URL(biburl));
        BibtexEntry b = parseBibtexHTML(page, entryID);
        return b;
    }

    /**
     * Parses a given article pages html code and finds the url for the
     * bibtex-Output.
     * 
     * 
     * @param articlePage
     *            The article pages html code
     * @return The url for the bibtex-Output of the given article
     */
    public static String makeBibtexURL(String articlePage) {
        String t = articlePage.substring(articlePage.indexOf("popBibTex.cfm?id=") + 17, articlePage.indexOf("'", articlePage.indexOf("popBibTex.cfm?id=")));
        return "http://portal.acm.org/popBibTex.cfm?id=" + t;
    }

    /**
     * Parses a bibtex string and returns a {@see BibtexEntry}
     * 
     * @param input
     *            ACM digital library HTML snippet of popBibtex.cfm?....
     * @return BibtexEntry correspondant to the given Bibtex text in the input
     * @throws IOException
     */
    public static BibtexEntry parseBibtexHTML(String input, String entryID) {
        String bibtex = input.substring(input.indexOf("@"), input.indexOf("</pre>"));
        ParserResult result;
        try {
            result = BibtexParser.parse(new StringReader(bibtex));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Collection<BibtexEntry> c = result.getDatabase().getEntries();
        BibtexEntry e = c.iterator().next();
        return e;
    }

    /**
     * Parses the given hitlist and returns the url to the next hitlist page, if
     * there is one
     * 
     * @param hitlist
     *            The hitlists html code
     * @return The url to the next hitlist page, if there is one
     */
    public static String hasFollowingPage(String hitlist) {
        Pattern p = Pattern.compile("<a href=\"results\\.cfm\\?query=(.+?)\">(\\s*)next(\\s*)</a>");
        Matcher m = p.matcher(hitlist);
        if (!m.find()) {
            return null;
        }
        return "http://portal.acm.org/results.cfm?query=" + m.group(1);
    }

    /**
     * Executes the query that was set via the terms attribute of this fetcher.
     * Creates a POST request since a get request querying the ACM digital
     * library is not possible (for the initial result page).
     * 
     * @return String The result pages html code
     * @throws IOException
     */
    public String executeQuery(String terms) throws IOException {
        URL queryTarget = null;
        try {
            queryTarget = new URL("http://portal.acm.org/results.cfm?coll=Portal&dl=Portal");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        String data = "";
        data += URLEncoder.encode("parser", "UTF-8") + "=" + URLEncoder.encode("Internet", "UTF-8");
        data += "&" + URLEncoder.encode("whichDL", "UTF-8") + "=" + URLEncoder.encode("acm", "UTF-8");
        data += "&" + URLEncoder.encode("query", "UTF-8") + "=" + URLEncoder.encode(terms, "UTF-8");
        data += "&" + URLEncoder.encode("Go.x", "UTF-8") + "=" + URLEncoder.encode("7", "UTF-8");
        return fetchPost(queryTarget, data);
    }

    /**
     * Now comes some uninteresting stuff....
     */
    public void done(int entriesImported) {
    }

    public void stopFetching() {
        shouldContinue = false;
    }

    public String getHelpPage() {
        return null;
    }

    public URL getIcon() {
        return GUIGlobals.getIconUrl("www");
    }

    public String getKeyName() {
        return "acm";
    }

    public JPanel getOptionsPanel() {
        return null;
    }

    public String getTitle() {
        return "Search ACM Digital Library";
    }

    /**
     * This method is called by the dialog when the user has cancelled the
     * import.
     */
    public void cancelled() {
        shouldContinue = false;
    }

    /**
     * Download the URL and return contents as a String.
     * 
     * @param url
     * @return
     * @throws IOException
     */
    public String fetchSite(URL url) throws IOException {
        InputStream in = url.openStream();
        StringBuffer sb = new StringBuffer();
        byte[] buffer = new byte[256];
        while (true) {
            int bytesRead = in.read(buffer);
            if (bytesRead == -1) break;
            for (int i = 0; i < bytesRead; i++) sb.append((char) buffer[i]);
        }
        return sb.toString();
    }

    /**
     * Download from the given URL using a post-query and return contents as a
     * String.
     * 
     * @param url
     *            The url to download
     * @param data
     *            The data to write to the connection before reading.
     * @return
     * @throws IOException
     */
    public String fetchPost(URL url, String data) throws IOException {
        StringBuilder page = new StringBuilder();
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            page.append(line);
        }
        wr.close();
        rd.close();
        return page.toString();
    }
}
