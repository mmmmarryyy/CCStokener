package net.sf.iqser.plugin.web.base;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.Adler32;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.htmlparser.util.NodeList;
import com.iqser.core.exception.IQserException;
import com.iqser.core.plugin.AbstractContentProvider;
import com.torunski.crawler.Crawler;
import com.torunski.crawler.events.IParserEventListener;
import com.torunski.crawler.events.ParserEvent;
import com.torunski.crawler.filter.ILinkFilter;
import com.torunski.crawler.filter.LinkFilterUtil;
import com.torunski.crawler.filter.RegularExpressionFilter;
import com.torunski.crawler.filter.ServerFilter;
import com.torunski.crawler.model.MaxDepthModel;

/**
 * A base content provider for the iQser GIN Platform to crawle and parse web content 
 * and store links in a database.
 * A content provider has to be implemented for specific content like html pages.
 * 
 * @author Joerg Wurzer
 *
 */
public abstract class CrawlerContentProvider extends AbstractContentProvider implements IParserEventListener {

    /** Version ID */
    private static final long serialVersionUID = 1L;

    /** Default link filter */
    private static final String DEFAULT_REGEX = "(\\S+\\.htm\\S*)||(\\S+\\.html\\S*)||(\\S+\\.php\\S*)||(\\S+\\.jsp\\S*)";

    /** Logger */
    private static Logger logger = Logger.getLogger(CrawlerContentProvider.class);

    /** Crawler */
    private Crawler crawler;

    /** Crawler start time */
    private long crawlerstart;

    /** database connection */
    private Connection conn;

    /**
	 * Method sets up the crawler and creates tables in the database to persist 
	 * harwested links to parse for the implemented content provider.
	 */
    public void init() {
        logger.debug("init() called");
        crawler = new Crawler();
        ArrayList<ILinkFilter> fCol = new ArrayList<ILinkFilter>();
        if (getInitParams().getProperty("server-filter") != null) fCol.add(new ServerFilter(getInitParams().getProperty("server-filter") + getInitParams().getProperty("path-filter", "/")));
        if (getInitParams().getProperty("link-exclude-filter") != null) fCol.add(LinkFilterUtil.not(new RegularExpressionFilter(getInitParams().getProperty("link-exclude-filter"))));
        fCol.add(new RegularExpressionFilter(getInitParams().getProperty("link-filter", DEFAULT_REGEX)));
        switch(fCol.size()) {
            case 3:
                crawler.setLinkFilter(LinkFilterUtil.and(fCol.get(0), LinkFilterUtil.and(fCol.get(1), fCol.get(2))));
                break;
            case 2:
                crawler.setLinkFilter(LinkFilterUtil.and(fCol.get(0), fCol.get(1)));
                break;
            default:
                crawler.setLinkFilter(fCol.get(0));
                break;
        }
        crawler.setModel(new MaxDepthModel(Integer.valueOf(getInitParams().getProperty("maxdepth-filter", "2"))));
        ExtendedHtmlParser parser = new ExtendedHtmlParser();
        parser.setEncoding(getInitParams().getProperty("charset", "UTF-8"));
        crawler.setParser(parser);
        crawler.addParserListener(this);
        try {
            if (getInitParams().getProperty("jdbcUrl") != null) {
                conn = DriverManager.getConnection(getInitParams().getProperty("jdbcUrl"));
            } else {
                conn = DriverManager.getConnection(getInitParams().getProperty("protocol", "jdbc:mysql:") + "//" + getInitParams().getProperty("database") + "?user=" + getInitParams().getProperty("username") + "&password=" + getInitParams().getProperty("password"));
            }
        } catch (SQLException e) {
            logger.error("Could not establish database connection - " + e.getMessage());
        }
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ;
            stmt.execute("CREATE TABLE IF NOT EXISTS documents" + "(id INT AUTO_INCREMENT KEY, url VARCHAR(255) NOT NULL, " + "checksum BIGINT NOT NULL, provider VARCHAR(255) NOT NULL, " + "checked BIGINT NOT NULL)");
            ;
        } catch (SQLException e) {
            logger.debug("Could't create table - " + e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) {
                }
                stmt = null;
            }
        }
    }

    public void destroy() {
        logger.debug("destroy() called");
        try {
            conn.close();
        } catch (SQLException e) {
            logger.error("Could not close the database connection - " + e.getMessage());
        }
    }

    public Collection getContentUrls() {
        return null;
    }

    /**
	 * Starts the crawler to harwest web links determined by the configuration. 
	 * Content objects are added and updatet in the implementes event listener of the crawler.
	 */
    public void doSynchonization() {
        logger.debug("doSynchonization() called");
        crawlerstart = System.currentTimeMillis();
        try {
            crawler.start(getInitParams().getProperty("start-server"), getInitParams().getProperty("start-path", "/"));
        } catch (Exception e) {
            logger.error("Couldn't start crawler - " + e.getMessage());
        }
        logger.debug("Crawler has finished");
    }

    public void doHousekeeping() {
        logger.debug("doHousekeeping() called");
        try {
            URL url = new URL(getInitParams().getProperty("test-url", "http://www.iqser.com"));
            url.openConnection().connect();
        } catch (IOException ioe) {
            logger.error("Couldn't open connection for test url - " + ioe.getMessage());
            return;
        }
        Statement stmt1 = null;
        Statement stmt2 = null;
        ResultSet rs = null;
        try {
            stmt1 = conn.createStatement();
            rs = stmt1.executeQuery("SELECT * FROM documents WHERE checked<" + crawlerstart + " AND provider='" + getId() + "'");
            while (rs.next()) {
                String s = rs.getString("url");
                try {
                    URL url = new URL(s);
                    url.openConnection().connect();
                    logger.warn("Not checked by the crawler but still available web content");
                } catch (IOException ioe) {
                    logger.error("Couldn't open connection for " + s + " - " + ioe.getMessage());
                    try {
                        this.removeContent(rs.getString("url"));
                        stmt2 = conn.createStatement();
                        stmt2.executeUpdate("DELETE FROM documents WHERE url='" + s + "'" + " AND provider='" + getId() + "'");
                    } catch (IQserException iqe) {
                        logger.error("Couldn't delete object " + s + " - " + iqe.getMessage());
                    }
                }
            }
        } catch (SQLException sqle) {
            logger.error("Couldn't perform sql query or update - " + sqle.getMessage());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) {
                }
                rs = null;
            }
            if (stmt1 != null) {
                try {
                    stmt1.close();
                } catch (SQLException sqlEx) {
                }
                stmt1 = null;
            }
            if (stmt2 != null) {
                try {
                    stmt2.close();
                } catch (SQLException sqlEx) {
                }
                stmt2 = null;
            }
        }
        logger.debug("Housekeeping has finished");
    }

    /**
	 * Implementation of IParserEventListener includes the logic to add, update or delete
	 * content for the index and analysis process of the core engine.
	 */
    public void parse(ParserEvent event) {
        logger.debug("parse() called for link " + event.getLink().getURI());
        String link = event.getLink().getURI();
        if (link.matches("\\S+&\\w{3};\\S+")) {
            link = StringEscapeUtils.unescapeHtml(event.getLink().getURI());
        }
        if (event.getCrawler().equals(crawler) && isContentLink(link)) {
            logger.debug("Content " + link + " matched");
            Statement stmt = null;
            ResultSet rs = null;
            long checksum1 = 0;
            try {
                URL url = new URL(link);
                checksum1 = url.openConnection().getLastModified();
            } catch (MalformedURLException mfe) {
                logger.error("Malformed url " + link + " - " + mfe.getMessage());
                return;
            } catch (IOException ioe) {
                logger.error("Couldn't read " + link + " - " + ioe.getMessage());
                return;
            }
            if ((checksum1 == 0) || (checksum1 == 1)) {
                Adler32 adler32 = new Adler32();
                NodeList nodes = (NodeList) event.getPageData().getData();
                adler32.update(nodes.toHtml().getBytes());
                checksum1 = adler32.getValue();
            }
            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery("SELECT * FROM documents WHERE url='" + link + "'");
                if (rs.first()) {
                    long checksum2 = rs.getLong("checksum");
                    if (checksum1 == checksum2) {
                        stmt.executeUpdate("UPDATE documents SET checked=" + String.valueOf(System.currentTimeMillis()) + " WHERE id=" + rs.getString("id"));
                    } else {
                        this.updateContent(getContent(link));
                        stmt.executeUpdate("UPDATE documents SET checksum=" + checksum1 + ", checked=" + String.valueOf(System.currentTimeMillis()) + " WHERE id=" + rs.getString("id"));
                    }
                } else {
                    this.addContent(getContent(link));
                    stmt.executeUpdate("INSERT INTO documents VALUES " + "(DEFAULT, '" + link + "', " + checksum1 + ", '" + getId() + "', " + String.valueOf(System.currentTimeMillis()) + ")");
                }
            } catch (SQLException e) {
                logger.error("Could't perform database query or update - " + e.getMessage());
            } catch (IQserException e) {
                logger.error("Couldn't perform repository update - " + e.getLocalizedMessage());
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException sqlEx) {
                    }
                    rs = null;
                }
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException sqlEx) {
                    }
                    stmt = null;
                }
            }
        }
    }

    /**
	 * Checks weather a link has to be parsed by the provider. 
	 * This is determined by the initial parameters in the configuration of the plugin.
	 */
    private boolean isContentLink(String link) {
        if (link.matches(getInitParams().getProperty("item-filter", DEFAULT_REGEX))) return true; else return false;
    }
}
