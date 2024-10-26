package bookshepherd.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexFile;
import bibtex.dom.BibtexString;
import bibtex.parser.BibtexParser;
import bibtex.parser.ParseException;
import bookshepherd.BSConstant;
import bookshepherd.model.ReferenceItem;
import bookshepherd.util.Util;

/**
 * Search for references using Google Scholar
 * 
 */
public class GSSearch {

    private static final String SET_COOKIE = "Set-Cookie";

    private static final String COOKIE_VALUE_DELIMITER = ";";

    private static final String SET_COOKIE_SEPARATOR = "; ";

    private static final String COOKIE = "Cookie";

    private static final char NAME_VALUE_SEPARATOR = '=';

    private static final String BIBTEX_START = "<a href=\"";

    private static final String BIBTEX_END = "\">Import into BibTeX";

    public static String getBibTeX(String title) throws Exception {
        StringBuffer request = new StringBuffer();
        request.append("http://scholar.google.de/scholar?hl=en&q=");
        request.append(prepareTitle(title));
        request.append("&btnG=Search&as_sdt=2000&as_ylo=&as_vis=0");
        return getContent(request.toString());
    }

    public static String getContent(String url) throws Exception {
        Map<String, String> cookies = getCookies();
        URL google = new URL(url);
        HttpURLConnection gc = (HttpURLConnection) google.openConnection();
        setCookies(gc, cookies);
        gc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3");
        gc.connect();
        getCookies(gc);
        BufferedReader in = new BufferedReader(new InputStreamReader(gc.getInputStream()));
        String inputLine;
        StringBuffer res = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            res.append(inputLine);
        }
        in.close();
        String reference = retrieveReference(res.toString());
        if (reference.equals("")) {
            return "";
        }
        return getBibTeX(new URL(reference));
    }

    public static String getBibTeX(URL google) throws Exception {
        Map<String, String> cookies = getCookies();
        HttpURLConnection gc = (HttpURLConnection) google.openConnection();
        setCookies(gc, cookies);
        gc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3");
        gc.connect();
        getCookies(gc);
        BufferedReader in = new BufferedReader(new InputStreamReader(gc.getInputStream()));
        String inputLine;
        StringBuffer res = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            res.append(inputLine);
        }
        in.close();
        return res.toString();
    }

    public static String retrieveReference(String content) {
        int k = content.indexOf(BIBTEX_END);
        if (k == -1) {
            return "";
        }
        String res = content.substring(0, k);
        k = res.lastIndexOf(BIBTEX_START);
        res = "http://scholar.google.de" + res.substring(k + BIBTEX_START.length(), res.length());
        res = res.replaceAll("&amp;", "&");
        return res;
    }

    /**
	 * Replace spaces with +
	 * 
	 * @param title
	 * @return
	 */
    public static String prepareTitle(String title) {
        StringTokenizer strt = new StringTokenizer(title);
        StringBuffer res = new StringBuffer();
        boolean first = true;
        while (strt.hasMoreTokens()) {
            if (first) {
                first = false;
            } else {
                res.append("+");
            }
            res.append(strt.nextToken());
        }
        return res.toString();
    }

    public static Map<String, String> getCookies() throws Exception {
        URL url = new URL("http://scholar.google.com");
        URLConnection conn = url.openConnection();
        conn.connect();
        return getCookies(conn);
    }

    /**
	 * Get cookies.
	 * 
	 * @return
	 * @throws Exception
	 */
    public static Map<String, String> getCookies(URLConnection conn) throws Exception {
        Map<String, String> cookies = new HashMap<String, String>();
        String headerName = null;
        for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equalsIgnoreCase(SET_COOKIE)) {
                StringTokenizer st = new StringTokenizer(conn.getHeaderField(i), COOKIE_VALUE_DELIMITER);
                if (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    String name = token.substring(0, token.indexOf(NAME_VALUE_SEPARATOR));
                    String value = token.substring(token.indexOf(NAME_VALUE_SEPARATOR) + 1, token.length());
                    if (name.equalsIgnoreCase("GSP")) {
                        value += ":CF=4";
                    }
                    cookies.put(name, value);
                }
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    cookies.put(token.substring(0, token.indexOf(NAME_VALUE_SEPARATOR)).toLowerCase(), token.substring(token.indexOf(NAME_VALUE_SEPARATOR) + 1, token.length()));
                }
            }
        }
        return cookies;
    }

    public static void setCookies(URLConnection conn, Map<String, String> cookies) throws IOException {
        StringBuffer cookieStringBuffer = new StringBuffer();
        Iterator<String> cookieNames = cookies.keySet().iterator();
        while (cookieNames.hasNext()) {
            String cookieName = cookieNames.next();
            cookieStringBuffer.append(cookieName);
            cookieStringBuffer.append("=");
            cookieStringBuffer.append((String) cookies.get(cookieName));
            if (cookieNames.hasNext()) cookieStringBuffer.append(SET_COOKIE_SEPARATOR);
        }
        try {
            conn.setRequestProperty(COOKIE, cookieStringBuffer.toString());
        } catch (java.lang.IllegalStateException ise) {
            IOException ioe = new IOException("Illegal State! Cookies cannot be set on a URLConnection that is already connected. Only call setCookies(java.net.URLConnection) AFTER calling java.net.URLConnection.connect().");
            throw ioe;
        }
    }

    public static void update(ReferenceItem item) {
        String query = item.getAttribute(BSConstant.BIB_TEX_TITLE);
        String bibtex = "";
        if (query != null) {
            try {
                bibtex = GSSearch.getBibTeX(query);
            } catch (Exception e) {
                bibtex = "";
                Logger.getLogger("bookshepherd").log(Level.SEVERE, e.getMessage(), e);
            }
        }
        if (bibtex.equals("")) {
            return;
        }
        BibtexParser parser = new BibtexParser(true);
        BibtexFile bib = new BibtexFile();
        StringReader reader = new StringReader(bibtex);
        try {
            parser.parse(bib, reader);
            if (bib.getEntries().size() != 1) {
                return;
            }
            BibtexEntry entry = (BibtexEntry) bib.getEntries().get(0);
            Object[] keys = entry.getFields().keySet().toArray();
            BibtexAbstractValue value;
            for (int i = 0; i < keys.length; i++) {
                value = entry.getFieldValue((String) keys[i]);
                if (value instanceof BibtexString) {
                    if (keys[i].equals(BSConstant.BIB_TEX_AUTHOR) || keys[i].equals(BSConstant.BIB_TEX_EDITOR)) {
                        item.setAttribute((String) keys[i], Util.parseBibTeXString(((BibtexString) value).getContent()));
                    } else {
                        String valueStr = ((BibtexString) value).getContent();
                        valueStr = Util.replace(valueStr, "{", "");
                        valueStr = Util.replace(valueStr, "}", "");
                        item.setAttribute((String) keys[i], valueStr);
                    }
                }
            }
            item.setAttribute(BSConstant.ATTR_BIB_TEX_DOC_TYPE, entry.getEntryType());
        } catch (ParseException e) {
            Logger.getLogger("bookshepherd").log(Level.SEVERE, e.getMessage(), e);
        } catch (IOException e) {
            Logger.getLogger("bookshepherd").log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static void update(List<ReferenceItem> list) {
        for (ReferenceItem item : list) {
            update(item);
        }
    }

    /**
	 * Main.
	 * 
	 * @param argv
	 */
    public static void main(String[] argv) {
        try {
            System.out.println(GSSearch.getBibTeX("EJOR - Mixed integer programming approach for index tracking and enhansed indexation"));
        } catch (Exception e) {
            Logger.getLogger("bookshepherd").log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
