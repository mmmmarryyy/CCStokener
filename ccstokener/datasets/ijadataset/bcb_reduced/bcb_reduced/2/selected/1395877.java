package soundlibrary;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;
import org.w3c.dom.*;
import javax.xml.parsers.*;

/**
 * This class acts as an intermediary to the server-side MySQL access. Because
 * MySQL access is blocked from all non-UML IPs, only HTML requests can go
 * through. As such, there is a .jsp which will serve as a stepping stone
 * between the Java application and the MySQL database.
 * This class defaults to search for all entries in the Database, but can be
 * given a custom query. If all goes well, the executeQuery() method will return
 * a Vector of SoundLibraryEntries, which hold all information for the results
 * of the query. If the process fails, it will throw an exception.
 * @author dan
 */
public class SoundLibraryQuery {

    /**
     * Instead of hardcoding the URL within the code, I have put it here to make
     * moving to other servers etc. easier. Just point to the page you want!
     */
    private String page_url = "http://teaching.cs.uml.edu/~daniel/sound_library/remote_query.jsp?sql_query=";

    /**
     * This is the local copy of the results of the query. This member will be
     * stored in case the user wishes to look at results from previous queries
     * without having to connect to the server again.
     */
    private Vector<SoundLibraryEntry> results;

    /**
     * This is the XML document that is returned by the .jsp page on the
     * teaching server. Although the executeQuery() method parses this document
     * in order to create the results data member, the user may want to have
     * access to the raw XML document.
     */
    private Document document;

    /**
     * This string is the SQL query that is sent to the SQL database. be careful
     * when setting this String manually. A bad query will cause the SQl 
     * database to return an error.
     */
    private String query;

    /**
     * This object is the connection object to the remote query page on the
     * teaching server.
     */
    private URL url_connection;

    /**
     * This is the default constructor. It simply sets the query to select all
     * entries from the Database.
     */
    public SoundLibraryQuery() {
        query = "SELECT * FROM library";
    }

    /**
     * This constructor takes in a given query and sets that to the
     * SoundLibraryQuery's query. This query will be used instead of the one
     * seen above.
     * @param given_query
     */
    public SoundLibraryQuery(String given_query) {
        query = given_query;
    }

    /**
     * This method is used to set the query manually.
     * @param new_query
     */
    public String setQuery(String new_query) {
        query = new_query;
        return (query);
    }

    /**
     * This method will take in a String for the desired Title to search by.
     * @param title
     * @return
     */
    public String setQuerySearchByTitle(String title) {
        query = "SELECT * FROM library WHERE MATCH (Title) AGAINST (\'" + title + "\' IN BOOLEAN MODE)";
        return (query);
    }

    /**
     * This method will take in a String for the desired Title to search by.
     * @param title
     * @return
     */
    public String setQuerySearchByAuthor(String author) {
        query = "SELECT * FROM library WHERE MATCH (Author) AGAINST (\'" + author + "\' IN BOOLEAN MODE)";
        return (query);
    }

    /**
     * This method will take in a String for the desired Title to search by.
     * @param title
     * @return
     */
    public String setQuerySearchByGenre(String genre) {
        query = "SELECT * FROM library WHERE MATCH (Genre) AGAINST (\'" + genre + "\' IN BOOLEAN MODE)";
        return (query);
    }

    /**
     * This method will take in a String for the desired Title to search by.
     * @param title
     * @return
     */
    public String setQuerySearchByTags(String tags) {
        query = "SELECT * FROM library WHERE MATCH (Tags) AGAINST (\'" + tags + "\' IN BOOLEAN MODE)";
        return (query);
    }

    /**
     * This method executes the query and is the main guts of the class. It will
     * open a connection to the server and access the remote_query page with the
     * given query. It will read the resulting buffer which is an XML file. This
     * file is kept for future reference, but is also parsed into a
     * Vector<SoundLibraryQuery>. Once the results have been propigated, the
     * data member results is returned.
     * @return A Vector of SoundLibraryEntry
     * @throws java.lang.Exception
     */
    public Vector<SoundLibraryEntry> executeQuery() throws Exception {
        String url_string = page_url + query;
        url_string = url_string.replace(" ", "%20");
        url_connection = new URL(url_string);
        url_connection.openConnection();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        try {
            document = builder.parse(url_connection.openStream());
        } catch (Exception e) {
            throw e;
        }
        results = new Vector<SoundLibraryEntry>();
        NodeList entries = document.getElementsByTagName("Entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Node entry = entries.item(i);
            Node data = entry.getFirstChild();
            URL url = new URL((String) data.getTextContent());
            data = data.getNextSibling();
            String title = (String) data.getTextContent();
            data = data.getNextSibling();
            String author = (String) data.getTextContent();
            data = data.getNextSibling();
            String genre = (String) data.getTextContent();
            data = data.getNextSibling();
            String tags = (String) data.getTextContent();
            data = data.getNextSibling();
            String date = (String) data.getTextContent();
            data = data.getNextSibling();
            String sequence = (String) data.getTextContent();
            results.add(new SoundLibraryEntry(url, title, author, genre, tags, date, sequence));
        }
        return (results);
    }

    /**
     * If the user wants to look at a set of results again, then th user may
     * call this mathod to get the set of previously retrieved URLs. If this
     * method is called before executeQuery() is called, it throws an exception.
     * @return Previously retrieved results
     * @throws java.lang.Exception
     */
    public Vector<SoundLibraryEntry> getResults() throws Exception {
        if (results == null) throw new SoundLibraryException("getResults() called before executeQuery().");
        return (results);
    }

    /**
     * In case the user does not want to use the Vector<SoundLibraryEntry> as
     * results, they may also use the straight XML returned from the server. A
     * copy of the parsed file is kept for future reference (just as a copy of
     * the results is kept). If the method executeQuery() has not been called,
     * the XML file will not have been initialized and it will throw an
     * exception!
     * @return Original XML returned by Server
     * @throws java.lang.Exception
     */
    public Document getXMLDocument() throws Exception {
        if (document == null) throw new SoundLibraryException("getXMLDocument() called before executeQuery().");
        return (document);
    }
}
