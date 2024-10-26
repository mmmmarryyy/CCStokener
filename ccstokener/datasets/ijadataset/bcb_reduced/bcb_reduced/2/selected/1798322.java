package jgd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import jgd.filters.JGDXMLFilterCharAllowed;
import jgd.filters.JGDXMLFilterRegenerateUTF;
import jgd.jaxb.Results;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * Query for google desktop search
 * 
 * @author maso
 *
 */
public class JGDQuery {

    /**
	 * Flags constants : sorted by date (default - if not given)
	 */
    public static final int FLAG_SORTED_BY_DATE = 0x0000;

    /**
	 * Flags constants : sorted by relevance
	 */
    public static final int FLAG_SORTED_BY_RELEVANCE = 0x0020;

    /**
	  * Flags constants : filter by all
	  */
    public static final int FLAG_FILTER_BY_ALL = 0x0008;

    /**
	  * Flags constants : filter by web
	  */
    public static final int FLAG_FILTER_BY_WEB = 0x0100;

    /**
	  * Flags constants : filter by files
	  */
    public static final int FLAG_FILTER_BY_FILES = 0x0200;

    /**
	  * Flags constants : filter by chats
	  */
    public static final int FLAG_FILTER_BY_CHATS = 0x0800;

    /**
	  * Flags constants : filter by mails
	  */
    public static final int FLAG_FILTER_BY_MAILS = 0x0400;

    /**
	 * Query string
	 */
    private String queryString;

    /**
	 * maximum number of results you'd like returned, to your query.<br>
	 * By default, an search response will only return the first ten results.
	 */
    private Integer num;

    /**
	 * position you want the results to start from. First item is 0 
	 */
    private Integer start;

    /**
	 * JAXB Context
	 */
    private JAXBContext jc;

    /**
	 * flags used in GDS for filtering search results.<br>
	 * Document at http://sourceforge.net/docman/display_doc.php?docid=29090&group_id=124229
	 */
    private Integer flags;

    /**
	 * Last results 
	 */
    private Results results;

    /**
	 * Base URL for GDS access
	 */
    private String desktopURL;

    /**
	 * File type for GDS. Example: "doc"
	 */
    private String fileType;

    /**
	 * Constructor of a query
	 * @param queryString Query string for search
	 * @throws JGDError Error in init query
	 */
    public JGDQuery(String queryString) throws JGDError {
        this.queryString = queryString;
        this.num = null;
        this.start = null;
        this.fileType = null;
        this.resetFlags();
        try {
            jc = JAXBContext.newInstance("jgd.jaxb");
        } catch (JAXBException e) {
            throw new JGDError("Error in init query: " + e.getMessage(), e);
        }
        this.desktopURL = Configuration.getDesktopURL();
    }

    /**
	 * Execute the query 
	 * @return Result of google desktop search
	 * @throws JGDError A error in search has ocurred
	 */
    public Results execute() throws JGDError {
        try {
            Unmarshaller u = jc.createUnmarshaller();
            u.setValidating(false);
            results = (Results) u.unmarshal(this.getResultInputStream());
            return results;
        } catch (UnmarshalException e) {
            int line;
            int column;
            Throwable linkException = e.getLinkedException();
            StringBuffer sb = new StringBuffer("Error executing desktop query: ");
            if (linkException instanceof SAXParseException) {
                line = ((SAXParseException) linkException).getLineNumber();
                column = ((SAXParseException) linkException).getColumnNumber();
                sb.append(" l:c = ");
                sb.append(line);
                sb.append(':');
                sb.append(column);
                sb.append(' ');
            }
            sb.append(e.getMessage());
            throw new JGDError(sb.toString());
        } catch (JAXBException e) {
            throw new JGDError("Error executing desktop query: " + e.getMessage());
        } catch (IOException e) {
            throw new JGDError("I/O Error executing desktop query: " + e.getMessage(), e);
        }
    }

    /**
	 * Return the XML input stream that contents the query result 
	 * @return XML Input stream 
	 */
    private InputSource getResultInputStream() throws JGDError, IOException {
        String urlString = getURL();
        URL url = new URL(urlString);
        InputStream is = url.openStream();
        InputSource iSource = new InputSource(getFilteredStream(is));
        return iSource;
    }

    /**
	 * Return an input stream UTF-8 compatible. 
	 * Version 20050227 of GDS returns invalid result XML:
	 * It can contains invalid UTF-8 characters.<br>
	 * This filter is necessary until this GDS bug is fixed. 
	 * @param is Invalid UTF-8 Input stream. 
	 * @return Valid UTF-8 input stream.
	 * 
	 */
    private InputStream getFilteredStream(InputStream is) throws IOException {
        JGDXMLFilterChain filter = new JGDXMLFilterCharAllowed(new JGDXMLFilterRegenerateUTF());
        return filter.getFilteredStream(is);
    }

    /**
	 * Return the URL that corresponds to this query
	 * @return URL 
	 * 
	 */
    public String getURL() throws JGDError {
        StringBuffer urlString = new StringBuffer();
        StringBuffer queryString = new StringBuffer(this.queryString);
        if (this.fileType != null) {
            queryString.append(" filetype:");
            queryString.append(this.fileType);
            queryString.append(' ');
        }
        urlString.append(MessageFormat.format(this.desktopURL, new String[] { URLEncoder.encode(queryString.toString()) }));
        if (this.num != null) {
            urlString.append("&num=");
            urlString.append(this.num);
        }
        if (this.start != null) {
            urlString.append("&start=");
            urlString.append(this.start);
        }
        if (this.flags != null) {
            urlString.append("&flags=");
            urlString.append(this.flags);
        }
        return urlString.toString();
    }

    /**
	 * maximum number of results you'd like returned, to your query.<br>
	 * By default, an search response will only return the first ten results.
	 */
    public Integer getNum() {
        return num;
    }

    /**
	 * Query string
	 */
    public String getQueryString() {
        return queryString;
    }

    /**
	 * position you want the results to start from. First item is 0 
	 */
    public Integer getStart() {
        return start;
    }

    /**
	 * maximum number of results you'd like returned, to your query.<br>
	 * By default, an search response will only return the first ten results.
	 */
    public void setNum(Integer integer) {
        num = integer;
    }

    /**
	 * Query string
	 */
    public void setQueryString(String string) {
        queryString = string;
    }

    /**
	 * position you want the results to start from. First item is 0 
	 */
    public void setStart(Integer integer) {
        start = integer;
    }

    /**
	 * flags used in GDS for filtering search results.<br>
	 * Document at http://sourceforge.net/docman/display_doc.php?docid=29090&group_id=124229<br>
	 * <br>Examples:<br>
	 * 8	sorted by date (default - if not given)<br>
	 * 40	sorted by relevance<br>
	 */
    public void setFlags(Integer flags) {
        this.flags = flags;
    }

    /**
	 * flags used in GDS for filtering search results.<br>
	 * Document at http://sourceforge.net/docman/display_doc.php?docid=29090&group_id=124229<br>
	 * <br>Examples:<br>
	 * 8	sorted by date (default - if not given)<br>
	 * 40	sorted by relevance<br>
	 */
    public Integer getFlags() {
        return flags;
    }

    /**
	 * Last results.
	 * return Last results, or null if not exists
	 */
    public Results getLastResult() {
        return results;
    }

    /**
	 * Write las results in XML into a output stream	 
	 * @param o Output Stream
	 * @throws JGDError Error marshalling XML
	 */
    public void writeLastResultInXML(OutputStream o) throws JGDError {
        if (results != null) {
            try {
                Marshaller m = jc.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                m.marshal(results, o);
            } catch (PropertyException e) {
                throw new JGDError("Error writing last result xml in a marshaller property: " + e.getMessage(), e);
            } catch (JAXBException e) {
                throw new JGDError("Error writing last result xml: " + e.getMessage(), e);
            }
        }
    }

    /**
	 * Execute the query, returns XML result
	 * @return Result of google desktop search in XML
	 * @throws JGDError A error in search has ocurred
	 */
    public String executeXML() throws JGDError {
        execute();
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        this.writeLastResultInXML(o);
        return new String(o.toByteArray());
    }

    /**
	 * Base URL for GDS access
	 */
    public String getDesktopURL() {
        return desktopURL;
    }

    /**
	 * Base URL for GDS access. <br>
	 * GDS URL must be MessageFormat-style. It looks like this:<br>
	 * <code>http://127.0.0.1:4664/search&s=Ac63pzBzTxSLEtSJ1XYCv0mXq00?q={0}&format=xml</code>
	 */
    public void setDesktopURL(String desktopURL) {
        this.desktopURL = desktopURL;
    }

    /**
	 * File type for GDS. Example: "doc"
	 */
    public String getFileType() {
        return fileType;
    }

    /**
	 * File type for GDS. Example: "doc"
	 */
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    /**
	 * Sorted By Relevance
	 */
    public void setSortedByRelevance() {
        this.removeFlag(FLAG_SORTED_BY_DATE);
        this.addFlag(FLAG_SORTED_BY_RELEVANCE);
    }

    /**
	 * Sorted By Date
	 */
    public void setSortedByDate() {
        this.removeFlag(FLAG_SORTED_BY_RELEVANCE);
        this.addFlag(FLAG_SORTED_BY_DATE);
    }

    /**
	 * Filter by all, this is, reset any filter.
	 *
	 */
    public void setFilterByAll() {
        this.removeFilterFlags();
        this.addFlag(FLAG_FILTER_BY_ALL);
    }

    /**
	 * Filter by web history
	 *
	 */
    public void setFilterByWeb() {
        this.removeFilterFlags();
        this.addFlag(FLAG_FILTER_BY_WEB);
    }

    /**
	 * Filter by files
	 *
	 */
    public void setFilterByFiles() {
        this.removeFilterFlags();
        this.addFlag(FLAG_FILTER_BY_FILES);
    }

    /**
	 * Filter by chats
	 *
	 */
    public void setFilterByChats() {
        this.removeFilterFlags();
        this.addFlag(FLAG_FILTER_BY_CHATS);
    }

    /**
	 * Filter by mails 
	 *
	 */
    public void setFilterByMails() {
        this.removeFilterFlags();
        this.addFlag(FLAG_FILTER_BY_MAILS);
    }

    /**
	 * Add a binary flag to the flags of query
	 * @param flag Binary flag
	 */
    private void addFlag(int flag) {
        int result = (this.flags == null) ? 0 : this.flags.intValue();
        result = result | flag;
        this.flags = new Integer(result);
    }

    /**
	 * Remove a binary flag from the flags of query
	 * @param flag Binary flag
	 */
    private void removeFlag(int flag) {
        int result = (this.flags == null) ? 0 : this.flags.intValue();
        result = result & ~flag;
        this.flags = new Integer(result);
    }

    /**
	 * Reset a initial flags value
	 *
	 */
    private void resetFlags() {
        this.setFilterByAll();
        this.addFlag(FLAG_SORTED_BY_DATE);
        this.removeFlag(FLAG_SORTED_BY_RELEVANCE);
    }

    /**
	 * Remove all filter flags
	 *
	 */
    private void removeFilterFlags() {
        this.removeFlag(FLAG_FILTER_BY_ALL);
        this.removeFlag(FLAG_FILTER_BY_CHATS);
        this.removeFlag(FLAG_FILTER_BY_FILES);
        this.removeFlag(FLAG_FILTER_BY_MAILS);
        this.removeFlag(FLAG_FILTER_BY_WEB);
    }
}
