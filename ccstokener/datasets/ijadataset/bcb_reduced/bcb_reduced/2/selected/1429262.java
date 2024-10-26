package org.oxyus.crawler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.oxyus.crawler.parser.HTMLParser;
import org.oxyus.store.Store;
import org.oxyus.util.Path;

/**
 * Page persistance.
 *
 * @author Carlos Saltos (csaltos[@]users.sourceforge.net)
 */
public class Page {

    protected Logger log;

    /**
     * Page state for signal a page not collected
     */
    public static int PAGE_NOT_COLLECTED = 0;

    /**
     * Page state for signal a page beign collected
     */
    public static int PAGE_COLLECTING = 1;

    /**
     * Page state for signal a page already collected
     */
    public static int PAGE_COLLECTED = 2;

    /**
     * Primary key
     */
    protected int code;

    /**
     * Server this page belongs
     */
    protected Server server;

    /**
     * Page collecting state
     */
    protected int state;

    /**
     * Ruta de la page.
     */
    protected String path;

    /**
     * Connection to oxyus repository
     */
    protected Store store;

    /**
     * Crawling scope
     */
    protected Scope scope;

    /**
     * Creates a page
     */
    public Page() {
        log = Logger.getLogger(Page.class);
        server = new Server();
        reset();
    }

    /**
     * Reset the internal properties
     */
    public void reset() {
        setCode(-1);
        setState(PAGE_NOT_COLLECTED);
        setPath(null);
        if (server != null) {
            server.reset();
        }
    }

    public void read() throws SQLException {
        boolean success = false;
        if (store == null) {
            throw new SQLException("Connection not opened");
        }
        PreparedStatement statement = store.prepareStatement("select code_server, state, path " + "from ox_page where code_page = ?");
        statement.setInt(1, this.getCode());
        ResultSet result = statement.executeQuery();
        if (result.next()) {
            setState(result.getInt("state"));
            setPath(result.getString("path"));
            this.server = new Server();
            this.server.setStore(this.store);
            this.server.setCode(result.getInt("code_server"));
            this.server.read();
            success = true;
        }
        result.close();
        statement.close();
        if (!success) {
            throw new SQLException("Page not found");
        }
    }

    /**
     * Reads the next page in not collected state.
     */
    public boolean nextForCollect() throws CrawlingException {
        try {
            boolean hasNextForCollect = false;
            if (store == null) {
                throw new SQLException("Connection not opened");
            }
            PreparedStatement statement = store.prepareStatement("select code_page from ox_page where state = ?");
            statement.setInt(1, Page.PAGE_NOT_COLLECTED);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                this.setCode(result.getInt("code_page"));
                hasNextForCollect = true;
            }
            result.close();
            statement.close();
            if (hasNextForCollect) {
                this.read();
            }
            return hasNextForCollect;
        } catch (SQLException sqle) {
            log.error("Unable to get the next page for collect", sqle);
            throw new CrawlingException("Unable to get the next page for " + "collect", sqle);
        }
    }

    /**
     * Calculate a page address based in a link without add it to the crawler
     * scope
     */
    public void recordLink(String link) {
        recordLink(link, false);
    }

    /**
     * Calculate a page address based in a link with the posibility to
     * add it to the crawler scope
     */
    public void recordLink(String link, boolean addToRules) {
        if (link == null) {
            log.warn("Attemp to record a null link");
            return;
        }
        link = link.trim();
        if (link.equals("")) {
            log.warn("Attemp to record an empty link");
            return;
        }
        URL url = null;
        try {
            url = new URL(link);
        } catch (MalformedURLException e) {
            url = null;
        }
        if (url == null) {
            if (server == null) {
                log.error("Server NULL registering address: " + link);
                return;
            }
            try {
                if (link.charAt(0) == '/') {
                    url = new URL(server.getProtocol() + "://" + server.getHost() + ":" + server.getPort() + link);
                } else {
                    String prefix = getPath();
                    if (prefix == null) {
                        log.error("Path not established registering address: " + link);
                        return;
                    }
                    int length = prefix.length();
                    if (length == 0) {
                        log.error("Path not established registering address: " + link);
                        return;
                    }
                    if (prefix.charAt(length - 1) != '/') {
                        int lastSlash = prefix.lastIndexOf('/');
                        if (lastSlash == -1) {
                            log.error("Path error registering address: " + link);
                            return;
                        }
                        prefix = prefix.substring(0, lastSlash + 1);
                    }
                    url = new URL("http://" + server.getHost() + ":" + server.getPort() + Path.normalize(prefix + link));
                }
            } catch (MalformedURLException e) {
                log.warn("Attemp to create an URL address using the link '" + link + "' has failed");
                return;
            }
        }
        if (url.getProtocol().toLowerCase().equals("http")) {
            if (addToRules) {
                scope.acceptDomain(url.getHost());
            }
            if (scope.inScope(url.getHost())) {
                Server server = new Server();
                server.setStore(store);
                server.setProtocol(url.getProtocol());
                server.setHost(url.getHost());
                server.setPort(url.getPort());
                log.debug("Port " + server.getPort());
                try {
                    Page page = new Page();
                    page.setStore(store);
                    page.setServer(server);
                    page.setPath(url.getPath());
                    page.setState(Page.PAGE_NOT_COLLECTED);
                    page.locateOrCreate();
                } catch (SQLException e) {
                    log.error("Error registering page: " + url.toExternalForm(), e);
                }
            }
        }
    }

    /**
     * Stores this new page is the path is not duplicated for the same server
     */
    public void locateOrCreate() throws SQLException {
        if (store == null) {
            throw new SQLException("Connection not opened");
        }
        if (this.getServer() == null) {
            throw new SQLException("Server not specified");
        }
        if (this.getPath() == null) {
            throw new SQLException("Path not specified");
        }
        this.getServer().locateOrCreate();
        PreparedStatement statement = store.prepareStatement("select code_page from ox_page, ox_server where " + "ox_page.code_server = ox_server.code_server and " + "ox_server.code_server = ? and " + "path = ?");
        statement.setInt(1, this.getServer().getCode());
        statement.setString(2, this.getPath());
        ResultSet result = statement.executeQuery();
        if (result.next()) {
            this.setCode(result.getInt("code_page"));
            result.close();
            statement.close();
            return;
        }
        this.setCode(store.nextCode("page"));
        statement = store.prepareStatement("insert into ox_page(code_page, code_server, state, path) " + "values(?,?,?,?)");
        statement.setInt(1, this.getCode());
        statement.setInt(2, this.getServer().getCode());
        statement.setInt(3, Page.PAGE_NOT_COLLECTED);
        statement.setString(4, this.getPath());
        statement.executeUpdate();
        statement.close();
    }

    /**
     * Sets the page state in PAGE_COLLECTING
     */
    public void markAsCollecting() throws CrawlingException {
        Statement sentencia = null;
        if (store == null) {
            throw new CrawlingException("Connection not opened");
        }
        try {
            sentencia = store.createStatement();
            if (sentencia == null) {
                throw new CrawlingException("Unable to create sentence");
            }
            sentencia.executeUpdate("UPDATE ox_page SET state='" + PAGE_COLLECTING + "' WHERE code_page=" + getCode());
            try {
                sentencia.close();
            } catch (SQLException e) {
                log.error("Unable to close the sentence correctly");
            }
        } catch (SQLException e) {
            throw new CrawlingException("Error closing page", e);
        }
    }

    /**
     * Sets the page state in PAGE_COLLECTED.
     */
    public void markAsCollected() throws CrawlingException {
        Statement sentencia = null;
        if (store == null) {
            throw new CrawlingException("Connection not opened");
        }
        try {
            sentencia = store.createStatement();
            if (sentencia == null) {
                throw new CrawlingException("Unable to create sentence");
            }
            sentencia.executeUpdate("UPDATE ox_page SET state='" + PAGE_COLLECTED + "' WHERE code_page=" + getCode());
            try {
                sentencia.close();
            } catch (SQLException e) {
                log.error("Unable to close the sentence correctly");
            }
        } catch (SQLException e) {
            throw new CrawlingException("Error closing page", e);
        }
    }

    public Document index() throws CrawlingException {
        log.debug("BEGINIG indexing page [code=" + getCode() + "] ...");
        URL url = null;
        InputStream in = null;
        String contentType = null;
        try {
            url = new URL(getServer().getProtocol() + "://" + getServer().getHost() + ":" + getServer().getPort() + getPath());
            HttpURLConnection pageContent = (HttpURLConnection) url.openConnection();
            if (pageContent.getResponseCode() != HttpURLConnection.HTTP_OK) {
                log.debug("page pk[" + getCode() + "," + url.toExternalForm() + "] is invalid");
                return null;
            }
            String redireccion = pageContent.getHeaderField("location");
            if (redireccion != null) {
                log.debug("Page " + url.toExternalForm() + " redirected to " + redireccion);
                recordLink(redireccion);
                return null;
            }
            contentType = pageContent.getContentType();
            in = new BufferedInputStream(pageContent.getInputStream(), 32768);
        } catch (MalformedURLException e) {
            log.error("Invalid page address", e);
        } catch (ConnectException e) {
            if (getServer() != null) {
                log.error("Unable to connect to page: " + getServer().getProtocol() + "://" + getServer().getHost() + ":" + getServer().getPort() + getPath(), e);
            }
        } catch (UnknownHostException uhe) {
            log.warn("Unknow host indexing page " + getURL(), uhe);
        } catch (IOException e) {
            log.warn("Unable to index page " + getURL(), e);
        }
        Document doc = generateDocument(contentType, in);
        log.debug("END indexing page [code=" + getCode() + "]");
        return doc;
    }

    public Document generateDocument(String contentType, InputStream in) throws CrawlingException {
        Document doc = null;
        if (contentType.indexOf("text/html") == 0) {
            doc = new HTMLParser(this).collect(in);
        } else {
            log.warn("No indexor for content type " + contentType);
        }
        return doc;
    }

    public String getURL() {
        String address = null;
        if (server.getProtocol() != null && server.getHost() != null && this.getPath() != null) {
            address = server.getProtocol() + "://" + server.getHost() + ":" + server.getPort() + this.getPath();
        }
        return address;
    }

    public void setStore(Store connection) {
        this.store = connection;
        if (server != null) {
            server.setStore(connection);
        }
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        if (state != PAGE_NOT_COLLECTED && state != PAGE_COLLECTING && state != PAGE_COLLECTED) {
            log.error("Invalid page state " + state);
            throw new IllegalArgumentException("Invalid page state" + state);
        }
        this.state = state;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        if (path != null) {
            this.path = path.trim();
        }
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope dominiosPermitidos) {
        this.scope = dominiosPermitidos;
    }
}
