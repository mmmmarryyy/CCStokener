package net.sf.iqser.plugin.web.base;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;
import com.iqser.core.exception.IQserException;
import com.iqser.core.plugin.AbstractContentProvider;

/**
 * A base content provider for the iQser GIN Platform 
 * using an external crawler and its crawled links in a database.
 * A content provider has to be implemented for specific content like html pages.
 * 
 * @author Joerg Wurzer
 */
public abstract class BaseContentProvider extends AbstractContentProvider {

    /** Serial ID */
    private static final long serialVersionUID = 9101206034678952772L;

    /** Default link filter */
    private static final String DEFAULT_REGEX = "(.*\\.htm$)||(.*\\.html$)||(.*\\.php\\.*$)||(.*\\.jsp\\.*$)";

    /** Logger */
    private static Logger logger = Logger.getLogger(BaseContentProvider.class);

    /** database connection */
    private Connection conn;

    /** Crawler start time */
    private long lastCrawlerStart;

    /** Synchronization start time */
    private long lastSyncStart;

    /**
	 * Method sets up the provider and save the init params in a database
	 * to configure the external crawler.
	 */
    public void init() {
        logger.debug("init() called");
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
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            ;
            stmt.execute("CREATE TABLE IF NOT EXISTS providers" + "(id INT AUTO_INCREMENT KEY, provider VARCHAR(255) NOT NULL, " + "startserver VARCHAR(255) NOT NULL, startpath VARCHAR(255) NOT NULL, " + "serverfilter VARCHAR(255) NOT NULL, pathfilter VARCHAR(255) NOT NULL, " + "linkfilter VARCHAR(255) NOT NULL, itemfilter VARCHAR(255) NOT NULL, " + "maxdepth INT NOT NULL, lastcrawlerstart BIGINT NOT NULL, " + "lastsyncstart BIGINT NOT NULL)");
            rs = stmt.executeQuery("SELECT * FROM providers WHERE provider='" + getId() + "'");
            if (!rs.first()) {
                if (getInitParams().getProperty("server-filter") != null) {
                    stmt.executeUpdate("INSERT INTO providers VALUES " + "(DEFAULT, '" + getId() + "', '" + getInitParams().getProperty("start-server") + "', '" + getInitParams().getProperty("start-path", "/") + "', '" + getInitParams().getProperty("server-filter", "none") + "', '" + getInitParams().getProperty("path-filter", "/") + "', '" + getInitParams().getProperty("link-filter", DEFAULT_REGEX) + "', '" + getInitParams().getProperty("item-filter", DEFAULT_REGEX) + "', " + Integer.valueOf(getInitParams().getProperty("maxdepth-filter", "2")) + ", " + System.currentTimeMillis() + ", " + System.currentTimeMillis() + ")");
                } else {
                    logger.error("Missing init parameter server-filter");
                }
            }
        } catch (SQLException e) {
            logger.debug("Could't create or query table - " + e.getMessage());
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

    /**
	 * Uses the collected links from the external crawler in the database 
	 * and returns a collection of URLs as Strings.
	 */
    public Collection getContentUrls() {
        logger.debug("getContentUrls() called");
        Collection cl = new ArrayList();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT url FROM documents WHERE provider='" + getId() + "'");
            while (rs.next()) {
                cl.add(rs.getString("url"));
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
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) {
                }
                stmt = null;
            }
        }
        logger.debug("getContentUrls() finished");
        return cl;
    }

    /**
	 * Uses the collected links from the external crawler in the database.
	 */
    public void doSynchonization() {
        logger.debug("doSynchonization() called");
        lastSyncStart = System.currentTimeMillis();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM providers WHERE provider='" + getId() + "'");
            rs.first();
            lastCrawlerStart = rs.getLong("lastcrawlerstart");
            lastSyncStart = rs.getLong("lastsyncstart");
            if (lastSyncStart > lastCrawlerStart) rs = stmt.executeQuery("SELECT url FROM documents WHERE provider='" + getId() + "'" + " AND checked>" + lastSyncStart); else rs = stmt.executeQuery("SELECT url FROM documents WHERE provider='" + getId() + "'" + " AND checked>" + lastCrawlerStart);
            while (rs.next()) {
                String url = rs.getString("url");
                try {
                    if (isExistingContent(url)) {
                        updateContent(getContent(url));
                    } else {
                        addContent(getContent(url));
                    }
                } catch (IQserException e) {
                    logger.error("Couldn't verify, if content " + url + " does exist - " + e.getMessage());
                }
            }
            stmt.executeUpdate("UPDATE providers SET lastsyncstart=" + lastSyncStart + " WHERE provider='" + getId() + "'");
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
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) {
                }
                stmt = null;
            }
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
            rs = stmt1.executeQuery("SELECT * FROM documents WHERE checked<" + lastCrawlerStart + " AND provider='" + getId() + "'");
            while (rs.next()) {
                String s = rs.getString("url");
                try {
                    URL url = new URL(s);
                    url.openConnection().connect();
                    logger.warn("Not checked by the crawler but still available web content");
                } catch (IOException ioe) {
                    logger.error("Couldn't open connection for " + s + " - " + ioe.getMessage());
                    stmt2 = conn.createStatement();
                    stmt2.executeUpdate("DELETE FROM documents WHERE url='" + s + "'" + " AND provider='" + getId() + "'");
                    try {
                        this.removeContent(rs.getString("url"));
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
}
