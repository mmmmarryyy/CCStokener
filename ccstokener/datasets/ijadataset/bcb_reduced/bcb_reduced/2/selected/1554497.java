package fr.unice.xmng;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Abstract Servlet.
 * 
 * Handles requests of ".html" and ".xml" files and writes HTML back
 * to the client
 * 
 * @author bgnpst
 */
public abstract class XmingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String EXIST_SERVER = "http://localhost:8080/exist/servlet/xm-ng/";

    private PrintWriter out = null;

    private TransformerFactory factory;

    private DocumentBuilderFactory bfactory;

    private DocumentBuilder parser;

    private String query = null;

    /**
	 * Servlet initialization
	 * 
	 * @see Servlet#init(ServletConfig)
	 */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.factory = TransformerFactory.newInstance();
        this.bfactory = DocumentBuilderFactory.newInstance();
        this.bfactory.setIgnoringElementContentWhitespace(true);
        this.bfactory.setNamespaceAware(true);
        try {
            this.parser = bfactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Handler for the GET http request, unmodified 
	 * 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.manageRequest(request, response);
    }

    /**
	 * Handler for the GET http request, unmodified 
	 * 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.manageRequest(request, response);
    }

    /**
	 * Handler called for every request made to the servlet
	 * 
	 * @param request
	 * @param response
	 */
    protected abstract void manageRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException;

    /**
	 * Executes the treatment of the sent request.
	 * 
	 * @param response
	 * @throws ServletException
	 */
    public void manageRequest(Transformer transformer) throws ServletException {
        try {
            this.parser.reset();
            String encodedQuery = URLEncoder.encode(this.query, "ISO-8859-1");
            URL url = new URL(EXIST_SERVER + "?_query=" + encodedQuery);
            InputStream in = url.openStream();
            Document doc = this.parser.parse(in);
            Source source = new DOMSource(doc);
            transformer.transform(source, new StreamResult(this.getOut()));
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Creates a query string for data search, stores it in local variable
	 * and return the xsl filename.
	 * 
	 * @param u
	 * @return the xsl filename for the query.
	 */
    protected String getQuery(String u) {
        String uri;
        try {
            uri = URLDecoder.decode(u.substring(12, u.lastIndexOf(".")), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            uri = u.substring(12, u.lastIndexOf("."));
        }
        String xsl_file = null;
        this.query = null;
        String[] url = uri.split("/");
        if (url.length > 0) {
            if (url[0].equals("organizations")) {
                if (url.length == 1) {
                    this.query = "<orgs> {for $p in //organization let $name := data($p/@name) order by lower-case($name) return <organization id=\"{data($p/@id)}\" etablished=\"{data($p/@etablished)}\" hqcity=\"{data($p/@hqcity)}\" hqcountry=\"{data($p/@hqcountry)}\"> {data($name)} ({data($p/@abbrev)}) </organization>} </orgs>";
                    xsl_file = "list_orgs.xsl";
                } else if (url.length == 3 && url[1].equals("members")) {
                    this.query = "let $o := //organization[@id=\"" + url[2] + "\"] return <organization id=\"" + url[2] + "\" established=\"{data($o/@established)}\" hqcity=\"{data($o/@hqcity)}\" hqcountry=\"{data($o/@hqcountry)}\" nbcountry=\"{count( $o/member_names)}\"> {for $p in $o/member_names let $name := data($p) let $code := /cia/country[@name=$name] order by lower-case($name) return <country datacode=\"{data($code/@datacode)}\"> {$name} </country>} </organization>";
                    xsl_file = "organization.xsl";
                }
            } else if (url[0].equals("country")) {
                if (url.length == 2) {
                    this.query = "let $p := //country[@name=\"" + url[1] + "\"] return <result> {$p} <borders> {for $b in $p/borders/@country return //country[@id=$b]} </borders> <cities> {for $b in /qiblih/city[@country=\"" + url[1] + "\"] return $b} </cities> </result>";
                    xsl_file = "country.xsl";
                }
            } else if (url[0].equals("countries") || url[0].equals("countries_pop") || url[0].equals("countries_gdp") || url[0].equals("countries_area")) {
                if (url.length == 1) {
                    this.query = "for $cont in /cia/continent/@name\n" + "return <continent name=\"{$cont}\"> {\n" + "for $country in /cia/country\n" + "where $country/@continent = $cont\n" + "return $country\n" + "} </continent>";
                    xsl_file = "list_" + url[0] + ".xsl";
                }
            }
        }
        if (xsl_file != null) xsl_file = this.getServletContext().getRealPath("WEB-INF/" + xsl_file);
        return xsl_file;
    }

    /**
	 * Returns the current query.
	 */
    public String getQuery() {
        return this.query;
    }

    /**
	 * Defines the current text-output stream
	 */
    public void setOut(PrintWriter response) {
        this.out = response;
    }

    /**
	 * Gets the current text-output stream
	 */
    public PrintWriter getOut() {
        return this.out;
    }

    /**
	 * Writes a string on the current text-output stream
	 * 
	 * @param s
	 *            string to write.
	 */
    public void write(String s) {
        this.out.println(s);
    }

    /**
	 * Closes the current text-output stream
	 */
    public void writeEnd() {
        this.out.close();
    }

    /**
	 * Gets the transformer factory.
	 * 
	 * @return the transformer factory.
	 */
    public TransformerFactory getFactory() {
        return this.factory;
    }
}
