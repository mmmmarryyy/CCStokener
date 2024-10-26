package grobid.utilities;

import grobid.data.BiblioItem;
import grobid.exceptions.GROBIDServiceException;
import grobid.sax.crossrefUnixrefSaxParser;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 *  Class for managing the extraction of bibliographical informations from pdf documents.
 *
 *  @author Patrice Lopez
 */
public class Consolidation {

    private Connection cCon = null;

    private String crossref_id = null;

    private String crossref_pw = null;

    private String crossref_host = null;

    private String crossref_port = null;

    private String mysql_host = null;

    private String mysql_port = null;

    private String mysql_username = null;

    private String mysql_passwd = null;

    private String proxy_host = null;

    private String proxy_port = null;

    private String mysql_dbname = null;

    public Consolidation() {
        this.setPropertyValues();
    }

    private void setPropertyValues() {
        crossref_id = System.getProperty(GrobidProperties.PROP_CROSSREF_ID);
        crossref_pw = System.getProperty(GrobidProperties.PROP_CROSSREF_PW);
        crossref_host = System.getProperty(GrobidProperties.PROP_CROSSREF_HOST) + "/servlet";
        crossref_port = System.getProperty(GrobidProperties.PROP_CROSSREF_PORT);
        mysql_host = System.getProperty(GrobidProperties.PROP_MYSQL_HOST);
        mysql_port = System.getProperty(GrobidProperties.PROP_MYSQL_PORT);
        mysql_username = System.getProperty(GrobidProperties.PROP_MYSQL_USERNAME);
        mysql_passwd = System.getProperty(GrobidProperties.PROP_MYSQL_PW);
        mysql_dbname = System.getProperty(GrobidProperties.PROP_MYSQL_DB_NAME);
        proxy_host = System.getProperty(GrobidProperties.PROP_PROXY_HOST);
        proxy_port = System.getProperty(GrobidProperties.PROP_PROXY_PORT);
    }

    private void setProxy() {
        System.setProperty("proxySet", "true");
        System.setProperty("http.proxyHost", proxy_host);
        System.setProperty("http.proxyPort", proxy_port);
    }

    /**
	 *  Open database connection 
	 */
    public void openDb() throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
        String dbUrl2 = "jdbc:mysql://" + "localhost" + ":" + "3306" + "/" + mysql_dbname + "?useUnicode=true&characterEncoding=utf8";
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            cCon = DriverManager.getConnection(dbUrl2, mysql_username, mysql_passwd);
            if (cCon != null) {
                cCon.createStatement().execute("SET NAMES utf8");
            }
        } catch (Exception e) {
            System.err.println("The connection to the MySQL database could not be established. \n" + "The call to Crossref service will not be cached.");
        }
    }

    /** 
     *  Close database connection 
     */
    public void closeDb() {
        try {
            if (cCon != null) {
                cCon.close();
            }
        } catch (SQLException se) {
        }
    }

    static final String INSERT_CROSSREF_SQL = "INSERT INTO AuthorTitle (Author, Title, Unixref) VALUES (?,?,?)";

    static final String INSERT_CROSSREF_SQL2 = "INSERT INTO AllSubFields (Request, Unixref) VALUES (?,?)";

    static final String INSERT_CROSSREF_SQL3 = "INSERT INTO DOIRequest (Request, Unixref) VALUES (?,?)";

    static final String QUERY_CROSSREF_SQL = "SELECT Unixref FROM AuthorTitle WHERE Author LIKE ? AND Title LIKE ?";

    static final String QUERY_CROSSREF_SQL2 = "SELECT Unixref FROM AllSubFields WHERE Request LIKE ?";

    static final String QUERY_CROSSREF_SQL3 = "SELECT Unixref FROM DOIRequest WHERE Request DOIRequest ?";

    /** Lookup by DOI */
    private static final String DOI_BASE_QUERY = "openurl?url_ver=Z39.88-2004&pid=%s:%s&rft_id=info:doi/%s&noredirect=true&format=unixref";

    /** Lookup by journal title, volume and first page */
    private static final String JOURNAL_BASE_QUERY = "query?usr=%s&pwd=%s&type=a&format=unixref&qdata=|%s||%s||%s|||KEY|";

    /** Lookup first author surname and  article title */
    private static final String TITLE_BASE_QUERY = "query?usr=%s&pwd=%s&type=a&format=unixref&qdata=%s|%s||key|";

    /**
     *  Try to consolidate some uncertain bibliographical data with crossref web service based on 
     *  title and first author
     */
    public boolean consolidateCrossrefGet(BiblioItem bib, ArrayList<BiblioItem> bib2) throws Exception {
        boolean result = false;
        String doi = bib.getDOI();
        String aut = bib.getFirstAuthorSurname();
        String title = bib.getTitle();
        String firstPage = null;
        String pageRange = bib.getPageRange();
        int beginPage = bib.getBeginPage();
        if (beginPage != -1) {
            firstPage = "" + beginPage;
        } else if (pageRange != null) {
            StringTokenizer st = new StringTokenizer(pageRange, "--");
            if (st.countTokens() == 2) {
                firstPage = st.nextToken();
            } else if (st.countTokens() == 1) firstPage = pageRange;
        }
        if (aut != null) {
            aut = TextUtilities.removeAccents(aut);
        }
        if (title != null) {
            title = TextUtilities.removeAccents(title);
        }
        if (doi != null) {
            if (doi.startsWith("doi:") | doi.startsWith("DOI:")) {
                doi.substring(4, doi.length());
                doi = doi.trim();
            }
            doi = doi.replace(" ", "");
            String xml = null;
            if (cCon != null) {
                PreparedStatement pstmt = null;
                try {
                    pstmt = cCon.prepareStatement(QUERY_CROSSREF_SQL3);
                    pstmt.setString(1, doi);
                    ResultSet res = pstmt.executeQuery();
                    if (res.next()) {
                        xml = res.getString(1);
                    }
                    res.close();
                    pstmt.close();
                } catch (SQLException se) {
                    System.err.println("EXCEPTION HANDLING CROSSREF CACHE");
                    throw new GROBIDServiceException("EXCEPTION HANDLING CROSSREF CACHE.", se);
                } finally {
                    try {
                        if (pstmt != null) pstmt.close();
                    } catch (SQLException se) {
                    }
                }
                if (xml != null) {
                    InputSource is = new InputSource();
                    is.setCharacterStream(new StringReader(xml));
                    DefaultHandler crossref = new crossrefUnixrefSaxParser(bib2);
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser parser = spf.newSAXParser();
                    parser.parse(is, crossref);
                    if (bib2.size() > 0) {
                        if (!bib2.get(0).getError()) result = true;
                    }
                }
            }
            if (xml == null) {
                String subpath = String.format(DOI_BASE_QUERY, crossref_id, crossref_pw, doi);
                URL url = new URL("http://" + crossref_host + "/" + subpath);
                System.out.println("Sending: " + url.toString());
                HttpURLConnection urlConn = null;
                try {
                    urlConn = (HttpURLConnection) url.openConnection();
                } catch (Exception e) {
                    this.setProxy();
                    try {
                        urlConn = (HttpURLConnection) url.openConnection();
                    } catch (Exception e2) {
                        urlConn = null;
                        throw new GROBIDServiceException("An exception occured while running Grobid.", e2);
                    }
                }
                if (urlConn != null) {
                    try {
                        urlConn.setDoOutput(true);
                        urlConn.setDoInput(true);
                        urlConn.setRequestMethod("GET");
                        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        InputStream in = urlConn.getInputStream();
                        xml = TextUtilities.convertStreamToString(in);
                        InputSource is = new InputSource();
                        is.setCharacterStream(new StringReader(xml));
                        DefaultHandler crossref = new crossrefUnixrefSaxParser(bib2);
                        SAXParserFactory spf = SAXParserFactory.newInstance();
                        SAXParser parser = spf.newSAXParser();
                        parser.parse(is, crossref);
                        if (bib2.size() > 0) {
                            if (!bib2.get(0).getError()) result = true;
                        }
                        urlConn.disconnect();
                    } catch (Exception e) {
                        System.err.println("Warning: Consolidation set true, " + "but the online connection to Crossref fails.");
                    }
                    if (cCon != null) {
                        PreparedStatement pstmt2 = null;
                        try {
                            pstmt2 = cCon.prepareStatement(INSERT_CROSSREF_SQL3);
                            pstmt2.setString(1, doi);
                            pstmt2.setString(2, xml);
                            pstmt2.executeUpdate();
                            pstmt2.close();
                        } catch (SQLException se) {
                            System.err.println("EXCEPTION HANDLING CROSSREF UPDATE");
                        } finally {
                            try {
                                if (pstmt2 != null) pstmt2.close();
                            } catch (SQLException se) {
                            }
                        }
                    }
                }
            }
        } else if ((title != null) & (aut != null)) {
            String xml = null;
            if (cCon != null) {
                PreparedStatement pstmt = null;
                try {
                    pstmt = cCon.prepareStatement(QUERY_CROSSREF_SQL);
                    pstmt.setString(1, aut);
                    pstmt.setString(2, title);
                    ResultSet res = pstmt.executeQuery();
                    if (res.next()) {
                        xml = res.getString(1);
                    }
                    res.close();
                    pstmt.close();
                } catch (SQLException se) {
                    throw new GROBIDServiceException("EXCEPTION HANDLING CROSSREF CACHE", se);
                } finally {
                    try {
                        if (pstmt != null) pstmt.close();
                    } catch (SQLException se) {
                    }
                }
                if (xml != null) {
                    InputSource is = new InputSource();
                    is.setCharacterStream(new StringReader(xml));
                    DefaultHandler crossref = new crossrefUnixrefSaxParser(bib2);
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser parser = spf.newSAXParser();
                    parser.parse(is, crossref);
                    if (bib2.size() > 0) {
                        if (!bib2.get(0).getError()) result = true;
                    }
                }
            }
            if (xml == null) {
                String subpath = String.format(TITLE_BASE_QUERY, crossref_id, crossref_pw, URLEncoder.encode(title), URLEncoder.encode(aut));
                URL url = new URL("http://" + crossref_host + "/" + subpath);
                System.out.println("Sending: " + url.toString());
                HttpURLConnection urlConn = null;
                try {
                    urlConn = (HttpURLConnection) url.openConnection();
                } catch (Exception e) {
                    this.setProxy();
                    try {
                        urlConn = (HttpURLConnection) url.openConnection();
                    } catch (Exception e2) {
                        urlConn = null;
                        throw new GROBIDServiceException("An exception occured while running Grobid.", e2);
                    }
                }
                if (urlConn != null) {
                    try {
                        urlConn.setDoOutput(true);
                        urlConn.setDoInput(true);
                        urlConn.setRequestMethod("GET");
                        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        InputStream in = urlConn.getInputStream();
                        xml = TextUtilities.convertStreamToString(in);
                        InputSource is = new InputSource();
                        is.setCharacterStream(new StringReader(xml));
                        DefaultHandler crossref = new crossrefUnixrefSaxParser(bib2);
                        SAXParserFactory spf = SAXParserFactory.newInstance();
                        SAXParser parser = spf.newSAXParser();
                        parser.parse(is, crossref);
                        if (bib2.size() > 0) {
                            if (!bib2.get(0).getError()) result = true;
                        }
                        urlConn.disconnect();
                    } catch (Exception e) {
                        System.err.println("Warning: Consolidation set true, " + "but the online connection to Crossref fails.");
                    }
                    if (cCon != null) {
                        PreparedStatement pstmt2 = null;
                        try {
                            pstmt2 = cCon.prepareStatement(INSERT_CROSSREF_SQL);
                            pstmt2.setString(1, aut);
                            pstmt2.setString(2, bib.getTitle());
                            pstmt2.setString(3, xml);
                            pstmt2.executeUpdate();
                            pstmt2.close();
                        } catch (SQLException se) {
                            System.err.println("EXCEPTION HANDLING CROSSREF UPDATE");
                        } finally {
                            try {
                                if (pstmt2 != null) pstmt2.close();
                            } catch (SQLException se) {
                            }
                        }
                    }
                }
            }
        } else if ((firstPage != null) & (bib.getJournal() != null) & (bib.getVolume() != null)) {
            String subpath = String.format(JOURNAL_BASE_QUERY, crossref_id, crossref_pw, URLEncoder.encode(bib.getJournal()), URLEncoder.encode(bib.getVolume()), firstPage);
            URL url = new URL("http://" + crossref_host + "/" + subpath);
            String urlmsg = url.toString();
            System.out.println(urlmsg);
            String xml = null;
            if (cCon != null) {
                PreparedStatement pstmt = null;
                try {
                    pstmt = cCon.prepareStatement(QUERY_CROSSREF_SQL2);
                    pstmt.setString(1, urlmsg);
                    ResultSet res = pstmt.executeQuery();
                    if (res.next()) {
                        xml = res.getString(1);
                    }
                    res.close();
                    pstmt.close();
                } catch (SQLException se) {
                    System.err.println("EXCEPTION HANDLING CROSSREF CACHE");
                    throw new GROBIDServiceException("EXCEPTION HANDLING CROSSREF CACHE.", se);
                } finally {
                    try {
                        if (pstmt != null) pstmt.close();
                    } catch (SQLException se) {
                    }
                }
                if (xml != null) {
                    InputSource is = new InputSource();
                    is.setCharacterStream(new StringReader(xml));
                    DefaultHandler crossref = new crossrefUnixrefSaxParser(bib2);
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser parser = spf.newSAXParser();
                    parser.parse(is, crossref);
                    if (bib2.size() > 0) {
                        if (!bib2.get(0).getError()) result = true;
                    }
                }
            }
            if (xml == null) {
                System.out.println("Sending: " + urlmsg);
                HttpURLConnection urlConn = null;
                try {
                    urlConn = (HttpURLConnection) url.openConnection();
                } catch (Exception e) {
                    this.setProxy();
                    try {
                        urlConn = (HttpURLConnection) url.openConnection();
                    } catch (Exception e2) {
                        urlConn = null;
                        throw new GROBIDServiceException("An exception occured while running Grobid.", e2);
                    }
                }
                if (urlConn != null) {
                    try {
                        urlConn.setDoOutput(true);
                        urlConn.setDoInput(true);
                        urlConn.setRequestMethod("GET");
                        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        InputStream in = urlConn.getInputStream();
                        xml = TextUtilities.convertStreamToString(in);
                        InputSource is = new InputSource();
                        is.setCharacterStream(new StringReader(xml));
                        DefaultHandler crossref = new crossrefUnixrefSaxParser(bib2);
                        SAXParserFactory spf = SAXParserFactory.newInstance();
                        SAXParser p = spf.newSAXParser();
                        p.parse(is, crossref);
                        if (bib2.size() > 0) {
                            if (!bib2.get(0).getError()) result = true;
                        }
                        in.close();
                        urlConn.disconnect();
                    } catch (Exception e) {
                        System.err.println("Warning: Consolidation set true, " + "but the online connection to Crossref fails.");
                    }
                    if (cCon != null) {
                        PreparedStatement pstmt2 = null;
                        try {
                            pstmt2 = cCon.prepareStatement(INSERT_CROSSREF_SQL2);
                            pstmt2.setString(1, urlmsg);
                            pstmt2.setString(2, xml);
                            pstmt2.executeUpdate();
                            pstmt2.close();
                        } catch (SQLException se) {
                            System.err.println("EXCEPTION HANDLING CROSSREF UPDATE");
                        } finally {
                            try {
                                if (pstmt2 != null) pstmt2.close();
                            } catch (SQLException se) {
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     *  Try to consolidate some uncertain bibliographical data with crossref web service - post version
     */
    public boolean consolidateCrossrefPostBatch(ArrayList<BiblioItem> bib, ArrayList<BiblioItem> bib2) throws Exception {
        boolean result = true;
        DefaultHandler crossref = new crossrefUnixrefSaxParser(bib2);
        int p = 0;
        String pipedQuery = null;
        for (int n = 0; n < bib.size(); n++) {
            if (p == 0) {
                pipedQuery = "";
            }
            BiblioItem bibo = bib.get(n);
            String aut = bibo.getFirstAuthorSurname();
            if ((bibo.getTitle() != null) & (aut != null)) {
                if (p != 0) pipedQuery += "\n";
                pipedQuery += bibo.getTitle() + "|" + aut + "||key" + n + "|";
            }
            if (p == 9) {
                p = 0;
            } else p++;
        }
        if (p != 0) {
            System.out.println("Sending: " + pipedQuery);
        }
        return true;
    }
}
