package de.banh.bibo.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamException;
import net.sourceforge.bibtexml.BibTeXConverter;

/**
 *  A proof of concept prototype for retrieving bibliographic 
 *  data (e.g., bibtex records) to a given ISBN from the web. 
 * 
 *  - Example: ISBN with more than one result: 9780321356680
 *  - Example: ISBN with more than one author: 3827318211
 *  - Example: ISBN with exactly one result: 3528147180
 *
 *  Remark: No comprehensive and reliable specification of the underlying 
 *  OCLC (Online Computer Library Center) Pica (Project of Integrated 
 *  Catalogue Automation) OPAC interface seems to be available, so 
 *  reverse engineering is unavoidable. Some useful references:
 *  
 *  [1] Livetrix (Ed.): OCLC Pica Notes. 2010. Available as 
 *      http://livetrix.wiki.ub.rug.nl/index.php/OCLC_Pica_notes
 *  [2] de Bakker, Bas: Search Server Request Format. 1998. 
 *
 *  BibTeXConvert is available at 
 *  http://sourceforge.net/projects/bibtexml/files/BibTeXConverter/
 *  Sources are available at 
 *  http://sourceforge.net/projects/bibtexml/files/BibTeXConverter/0.5.3/bibtexconverter-0.5.3-src.zip
 *
 *  @author Dieter Hofbauer
 *  @version 22.4.2010, 19.10.2010
 */
public class IsbnToBibConverter {

    /** This version retrieves data from the SWB Online-Katalog. */
    private static final String libraryUrl = "http://swb.bsz-bw.de";

    /** For simulating a cookie. */
    private final String cookieValue;

    /** For simulating a session identifier. */
    private final String sessionId;

    /** Constructor extracts a cookie value and a session identifier. */
    public IsbnToBibConverter() {
        URLConnection conn = null;
        try {
            URL url = new URL(libraryUrl);
            conn = url.openConnection();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String cookie = extractCookie(conn);
        cookieValue = extractCookieValue(cookie);
        sessionId = extractSessionId(cookieValue);
    }

    /** Returns the cookie for the given URL connection. */
    private String extractCookie(URLConnection conn) {
        Map<String, List<String>> headers = conn.getHeaderFields();
        List<String> values = headers.get("Set-Cookie");
        StringBuilder cookie = new StringBuilder();
        for (String v : values) {
            cookie.append(v);
        }
        return cookie.toString();
    }

    /** Returns the cookie value for the given cookie. */
    private String extractCookieValue(String cookie) {
        Pattern pattern = Pattern.compile("COOKIE=\".*?\"");
        Matcher matcher = pattern.matcher(cookie);
        matcher.find();
        String result = matcher.group();
        return result.replace("COOKIE=", "").replace("\"", "");
    }

    /** Returns the session identifier for the given cookie value. */
    private String extractSessionId(String cookieValue) {
        Pattern pattern = Pattern.compile("D2\\.1,.*?,");
        Matcher matcher = pattern.matcher(cookieValue);
        matcher.find();
        String result = matcher.group();
        return result.replace("D2.1,", "").replace(",", "");
    }

    private String urlInitialPart() {
        return libraryUrl + "/COOKIE=" + cookieValue + "/DB=2.1";
    }

    private String urlLIBIDPart() {
        return "/LIBID=0728%2B/LNG=DU";
    }

    private String urlSessionIdPart() {
        return "/SID=" + sessionId;
    }

    private String urlIsbnPart(String isbn) {
        return "/FKT=1007/FRM=" + isbn;
    }

    private String urlIMPLANDPart() {
        return "/IMPLAND=Y";
    }

    private String urlSortByRelevancePart() {
        return "/SRT=RLV/TTL=1";
    }

    private String urlShowPositionPart(int position) {
        return "/SHW?FRST=" + String.valueOf(position) + "&ADI_LND=";
    }

    private String urlBibTeXPart() {
        return "/PRS=bibtex";
    }

    private String urlDefaultPart() {
        return "/PRS=DEFAULT";
    }

    private String isbnQuery(String isbn) {
        return urlInitialPart() + urlLIBIDPart() + urlSessionIdPart() + "/CMD?ACT=SRCHA&IKT=1007&SRT=RLV&TRM=" + isbn + "&MATCFILTER=N&MATCSET=N&NOABS=Y";
    }

    private String queryTemplate(String isbn, int position, String s) {
        return urlInitialPart() + urlIsbnPart(isbn) + urlIMPLANDPart() + urlLIBIDPart() + "/LRSET=1/MATC=/SET=1" + urlSessionIdPart() + urlSortByRelevancePart() + s + urlShowPositionPart(position);
    }

    private String resultSelectionQuery(String isbn, int position) {
        return queryTemplate(isbn, position, new String());
    }

    private String bibtexQuery(String isbn, int position) {
        return queryTemplate(isbn, position, urlBibTeXPart());
    }

    private String rvkQuery(String isbn) {
        return queryTemplate(isbn, 1, urlDefaultPart());
    }

    private String getResource(String urlSpec) {
        InputStream in = null;
        String result = null;
        try {
            URL url = new URL(urlSpec);
            URLConnection conn = url.openConnection();
            conn.connect();
            in = conn.getInputStream();
            result = new Scanner(in).useDelimiter("\\Z").next();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return result;
    }

    /** Extracts a BibTeX record from the given HTML source, assuming that 
	 *  the string contains exactly one occurrence of the letter '@'.
	 */
    private static String extractBibTex(String html) {
        Pattern pattern = Pattern.compile("@.*\\}");
        Matcher matcher = pattern.matcher(html);
        matcher.find();
        String result = matcher.group();
        return result.replace("</div><div>", "").replace("<NOBR>", "\n").replace("</NOBR>", "").replace("<strong>", "").replace("</strong>", "").replace("&lt;", "<").replace("&gt;", ">");
    }

    /** Returns the number of hits for the original query, which is extracted
	 *  from the given HTML source. 
	 */
    private static int extractHits(String html) {
        Pattern pattern = Pattern.compile("HITS=\\d*");
        Matcher matcher = pattern.matcher(html);
        matcher.find();
        String result = matcher.group();
        return Integer.parseInt(result.replace("HITS=", ""));
    }

    /** Extracts the RVK notation from the given HTML source of the tab "Felder". 
	 *  If more that one RVKN is provided, the first one is chosen. 
	 *  If no RVKN is found, the empty string is returned. 
	 */
    private static String extractRVK(String html) {
        Pattern pattern = Pattern.compile("RVK-Notation:.*</a>&#xA0");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            String tmpResult = matcher.group();
            pattern = Pattern.compile(">[\\w|\\s]*</a>");
            matcher = pattern.matcher(tmpResult);
            matcher.find();
            String result = matcher.group();
            return result.replace("</a>", "").replace(">", "").trim();
        } else {
            return new String();
        }
    }

    /** Adds the given field with given value to the given bibtex record. 
	 *  We assume that bibTeXRecord contains the field "title". 
	 */
    private static String addBibTeXField(String field, String value, String bibTeXRecord) {
        return bibTeXRecord.replace("title", field + " = {" + value + "},\n title");
    }

    /** Returns the BibTeX record at the given position in the list of search 
	 *  results, if any. Otherwise, an appropropriate message is returned. 
	 */
    public String getBibTeXRecord(String isbn, int position) {
        IsbnToBibConverter conv = new IsbnToBibConverter();
        conv.getResource(conv.isbnQuery(isbn));
        conv.getResource(conv.resultSelectionQuery(isbn, 1));
        String resource = conv.getResource(conv.bibtexQuery(isbn, position));
        String result;
        try {
            result = extractBibTex(resource);
        } catch (IllegalStateException e) {
            result = "\nNo BibTex record found.\n";
        }
        return result;
    }

    /** Returns a list of BibTeX records for the given ISBN. */
    public List<String> getBibTeXRecords(String isbn) {
        List<String> bibTeXRecords = new LinkedList<String>();
        IsbnToBibConverter conv = new IsbnToBibConverter();
        int hits = 0;
        try {
            hits = extractHits(conv.getResource(conv.isbnQuery(isbn)));
        } catch (IllegalStateException e) {
        }
        for (int pos = 1; pos <= hits; pos++) {
            conv.getResource(conv.isbnQuery(isbn));
            conv.getResource(conv.resultSelectionQuery(isbn, pos));
            String resource = conv.getResource(conv.bibtexQuery(isbn, pos));
            try {
                String bibTeXRecord = extractBibTex(resource);
                String rvkResource = conv.getResource(conv.rvkQuery(isbn));
                String rvkn = extractRVK(rvkResource);
                bibTeXRecord = addBibTeXField("isbnumber", isbn, bibTeXRecord);
                bibTeXRecord = addBibTeXField("rvknumber", rvkn, bibTeXRecord);
                bibTeXRecords.add(bibTeXRecord);
            } catch (IllegalStateException e) {
                System.err.println(e.getMessage());
            }
        }
        return bibTeXRecords;
    }

    /** Converts the given BibTeX record into BibXML format. */
    private static String convertToBibXmlRecord(String bibTeXRecord) {
        String bibXmlRecord = null;
        try {
            FileWriter bibTeXFile = new FileWriter("tmp.bib");
            bibTeXFile.write(bibTeXRecord);
            bibTeXFile.close();
            new BibTeXConverter().bibTexToXml(new File("tmp.bib"), new File("tmp.xml"));
            char[] buffer = new char[(int) new File("tmp.xml").length()];
            new BufferedReader(new FileReader("tmp.xml")).read(buffer);
            bibXmlRecord = new String(buffer).trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bibXmlRecord;
    }

    /** Returns a list of BibXML records for the given ISBN. */
    public List<String> getBibXmlRecords(String isbn) {
        List<String> bibXmlRecords = new LinkedList<String>();
        for (String bibTeXRecord : getBibTeXRecords(isbn)) {
            bibXmlRecords.add(convertToBibXmlRecord(bibTeXRecord));
        }
        return bibXmlRecords;
    }

    /** Returns a list of bib records for the given ISBN. */
    public static Set<BibRecord> getBibRecords(String isbn) throws XMLStreamException {
        List<String> bibXmlRecordList = new IsbnToBibConverter().getBibXmlRecords(isbn);
        Set<BibRecord> bibRecords = new TreeSet<BibRecord>();
        for (String bibXmlRecord : bibXmlRecordList) {
            bibRecords.add(new BibRecord(bibXmlRecord));
        }
        return bibRecords;
    }
}
