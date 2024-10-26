package edu.upmc.opi.caBIG.caTIES.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Filters Vocabulary to eliminate conflicts with the NegEx Algorithm.
 * 
 * The NegEx Filter algorithm provided by Wendy Chapman and Webster
 * 
 * First, build a NegationDictionary, contains all types of phrases used by the
 * Negex algo (neg/pneg/pseudoneg/conj).
 * 
 * Next, build a TermDicitonary. Each term is evaluated before adding to
 * TermDictionary.
 * 
 * If term is an exact copy of a NegationDictionary phrase, then reject it.
 * 
 * If a term contains any NegationDictionary phrase, then add it to a list of
 * terms for further scrutiny.
 * 
 * Else add the term to TermDictionary.
 * 
 * Last, evaluate each term in the list marked for further scrutiny. For each
 * NegationDictionary phrase: Split the term on the negation phrase.
 * 
 * Check if each and every one of the fragments left after splitting is already
 * present in the TermDicitonary.
 * 
 * If so, then reject the term, and write it to ExlcudedTerms file.
 * 
 * If not, then add the term to TermDictionary.
 * 
 * We implement this algorithm using the UMLS stored in a MySQL database. Note
 * that by default MySQL searches are NOT case sensitive. This is a good
 * behavior.
 * 
 * @author mitchellkj@upmc.edu
 * @version $Id: CaTIES_UmlsNegExFilter.java,v 1.1 2005/12/06 18:51:54
 * mitchellkj Exp $
 * @since 1.4.2_04
 */
public class CaTIES_UmlsNegExFilter {

    /**
     * Field ACCEPTED. (value is "0 ;")
     */
    private static final int ACCEPTED = 0;

    /**
     * Field SCRUTINY. (value is "1 ;")
     */
    private static final int SCRUTINY = 1;

    /**
     * Field REJECTED. (value is "2 ;")
     */
    private static final int REJECTED = 2;

    /**
     * Field logger.
     */
    private static Logger logger = Logger.getLogger(CaTIES_UmlsNegExFilter.class);

    /**
     * Field preNegations.
     */
    private Vector preNegations = new Vector();

    /**
     * Field postNegations.
     */
    private Vector postNegations = new Vector();

    /**
     * Field pseudoNegations.
     */
    private Vector pseudoNegations = new Vector();

    /**
     * Field allNegations.
     */
    private Vector allNegations = new Vector();

    /**
     * Field driver.
     */
    private String driver = null;

    /**
     * Field connection.
     */
    private Connection connection = null;

    /**
     * Field user.
     */
    private String user = null;

    /**
     * Field password.
     */
    private String password = null;

    /**
     * Field dataBaseName.
     */
    private String dataBaseName = null;

    /**
     * Constructor for CaTIES_UmlsNegExFilter.
     */
    public CaTIES_UmlsNegExFilter() {
        this.preNegations = loadFile("umls.negex.filter.pre.url");
        this.postNegations = loadFile("umls.negex.filter.post.url");
        this.pseudoNegations = loadFile("umls.negex.filter.pseudo.url");
        this.allNegations.addAll(this.preNegations);
        this.allNegations.addAll(this.postNegations);
        this.allNegations.addAll(this.pseudoNegations);
        logger.debug(this);
        this.driver = (String) System.getProperties().get("umls.negex.filter.db.driver");
        this.dataBaseName = (String) System.getProperties().get("umls.negex.filter.public.url");
        this.user = (String) System.getProperties().get("umls.negex.filter.public.user");
        this.password = (String) System.getProperties().get("umls.negex.filter.public.password");
        if (this.password == null) {
            this.password = "";
        }
        openPublicDbConnection();
        process();
        closePublicDbConnection();
    }

    /**
     * Method process.
     */
    protected void process() {
        try {
            rejectExactMatches();
            buildSrutinizationSet();
            scrutinize();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    /**
     * Method simpleTest.
     * 
     * @throws Exception the exception
     */
    protected void simpleTest() throws Exception {
        String select = "select cui, str from mrcon where str like '%malignancy%'";
        Statement statement = this.connection.createStatement();
        ResultSet rs = statement.executeQuery(select);
        while (rs.next()) {
            String cui = rs.getString(1);
        }
        statement.close();
    }

    /**
     * Method rejectExactMatches.
     * 
     * @throws Exception the exception
     */
    protected void rejectExactMatches() throws Exception {
        String update = "update mrcon set status = " + REJECTED + " where ";
        for (Enumeration e = this.allNegations.elements(); e.hasMoreElements(); ) {
            String matchTerm = (String) e.nextElement();
            update += " str = '" + matchTerm + "'" + " or ";
        }
        update = update.substring(0, update.length() - 3);
        logger.debug("\n\n");
        logger.debug(update);
        logger.debug("\n\n");
        Statement statement = this.connection.createStatement();
        statement.executeUpdate(update);
        statement.close();
    }

    /**
     * Method buildSrutinizationSet.
     * 
     * @throws Exception the exception
     */
    protected void buildSrutinizationSet() throws Exception {
        String update = "update mrcon set status = " + SCRUTINY + " where ";
        for (Enumeration e = this.allNegations.elements(); e.hasMoreElements(); ) {
            String matchTerm = (String) e.nextElement();
            update += " str like '" + matchTerm + " %'" + " or ";
            update += " str like '% " + matchTerm + "'" + " or ";
            update += " str like '% " + matchTerm + " %'" + " or ";
        }
        update = update.substring(0, update.length() - 3);
        logger.debug("\n\n");
        logger.debug(update);
        logger.debug("\n\n");
        Statement statement = this.connection.createStatement();
        statement.executeUpdate(update);
        statement.close();
    }

    /**
     * Method scrutinize.
     * 
     * @throws Exception the exception
     */
    protected void scrutinize() throws Exception {
        String select = "select cui, str from mrcon where status = " + SCRUTINY;
        Statement statement = this.connection.createStatement();
        ResultSet rs = statement.executeQuery(select);
        while (rs.next()) {
            String cui = rs.getString(1);
            String str = rs.getString(2);
            for (Enumeration e = this.allNegations.elements(); e.hasMoreElements(); ) {
                String matchTerm = (String) e.nextElement();
                matchTerm = matchTerm.trim();
                int matchIdx = str.indexOf(matchTerm + " ");
                if (matchIdx == 0) {
                    String matchSuffix = str.substring(matchTerm.length(), str.length());
                    Vector sv = new Vector();
                    sv.add(matchSuffix.trim());
                    boolean allTermsPresent = areAllTermsPresent(sv);
                    if (allTermsPresent) {
                        rejectConcept(cui, str);
                        continue;
                    }
                }
                matchIdx = str.indexOf(" " + matchTerm);
                if (matchIdx != -1 && matchIdx == str.length() - matchTerm.length()) {
                    String matchPrefix = str.substring(0, matchIdx);
                    Vector sv = new Vector();
                    sv.add(matchPrefix.trim());
                    boolean allTermsPresent = areAllTermsPresent(sv);
                    if (allTermsPresent) {
                        rejectConcept(cui, str);
                        continue;
                    }
                }
            }
        }
        statement.close();
    }

    /**
     * Method forceEndOfString.
     * 
     * @param str String
     * 
     * @return String
     */
    protected String forceEndOfString(String str) {
        String result = str;
        if (str != null && str.length() > 0) {
            char[] chars = str.toCharArray();
            if (chars[chars.length - 1] != '\0') {
                char[] appendChars = new char[chars.length + 1];
                for (int idx = 0; idx < chars.length; idx++) {
                    appendChars[idx] = chars[idx];
                }
                appendChars[chars.length] = '\0';
                result = new String(appendChars);
            }
        }
        return result;
    }

    /**
     * Method areAllTermsPresent.
     * 
     * @param termSearchVector Vector
     * 
     * @return boolean
     */
    protected boolean areAllTermsPresent(Vector termSearchVector) {
        boolean result = termSearchVector.size() > 0;
        try {
            for (Enumeration e = termSearchVector.elements(); e.hasMoreElements() && result; ) {
                String matchTerm = (String) e.nextElement();
                String select = "select count(*) from mrcon where str = '" + matchTerm + "'";
                Statement statement = this.connection.createStatement();
                ResultSet rs = statement.executeQuery(select);
                while (rs.next()) {
                    long count = rs.getLong(1);
                    if (count == 0) {
                        result = false;
                    }
                }
                statement.close();
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
        return result;
    }

    /**
     * Method rejectConcept.
     * 
     * @param cui String
     * @param str String
     */
    protected void rejectConcept(String cui, String str) {
        try {
            String update = "update mrcon set status = " + REJECTED + " where " + " cui = '" + cui + "'" + " and " + "str = '" + str + "'";
            Statement statement = this.connection.createStatement();
            statement.executeUpdate(update);
            statement.close();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    /**
     * Method openPublicDbConnection.
     */
    protected void openPublicDbConnection() {
        try {
            Class.forName(this.driver);
            this.connection = DriverManager.getConnection(this.dataBaseName, this.user, this.password);
            this.connection.setAutoCommit(true);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    /**
     * Method closePublicDbConnection.
     */
    protected void closePublicDbConnection() {
        try {
            this.connection.close();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    /**
     * Method toString.
     * 
     * @return String
     */
    public String toString() {
        return stringifyVector(this.allNegations);
    }

    /**
     * Method stringifyVector.
     * 
     * @param stringVector Vector
     * 
     * @return String
     */
    protected String stringifyVector(Vector stringVector) {
        String result = "";
        if (stringVector.size() > 0) {
            for (Enumeration e = stringVector.elements(); e.hasMoreElements(); ) {
                result += (String) e.nextElement() + "\n";
            }
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * Method loadFile.
     * 
     * @param propertyName String
     * 
     * @return Vector
     */
    protected Vector loadFile(String propertyName) {
        Vector result = new Vector();
        try {
            String fileURLName = (String) System.getProperties().get(propertyName);
            URL fileURL = new URL(fileURLName);
            String resultAsString = readFile(fileURL);
            StringTokenizer st = new StringTokenizer(resultAsString, "\n");
            while (st.hasMoreTokens()) {
                result.add(st.nextToken());
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
        return result;
    }

    /**
     * Method readFile.
     * 
     * @param url URL
     * 
     * @return String
     * 
     * @throws Exception the exception
     */
    protected String readFile(URL url) throws Exception {
        URLConnection connection = url.openConnection();
        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
        httpURLConnection.setRequestProperty("Content-Type", "text/html");
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setAllowUserInteraction(false);
        httpURLConnection.setDoInput(true);
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.connect();
        httpURLConnection.getResponseCode();
        byte[] byteArray = consumeResponse(httpURLConnection.getInputStream());
        return new String(byteArray);
    }

    /**
     * Method consumeResponse.
     * 
     * @param instream InputStream
     * 
     * @return byte[]
     * 
     * @throws IOException the IO exception
     */
    private static byte[] consumeResponse(final InputStream instream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
        int l = -1;
        byte[] tmp = new byte[1024];
        while ((l = instream.read(tmp)) >= 0) {
            buffer.write(tmp, 0, l);
        }
        return buffer.toByteArray();
    }

    /**
     * Method main.
     * 
     * @param args String[]
     */
    public static void main(String[] args) {
        BasicConfigurator.configure();
        new CaTIES_PropertyLoader();
        logger.setLevel(Level.DEBUG);
        new CaTIES_UmlsNegExFilter();
    }
}
