package net.sf.iqser.plugin.web.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.Adler32;
import org.apache.log4j.Logger;
import org.htmlparser.util.NodeList;
import com.torunski.crawler.events.IParserEventListener;
import com.torunski.crawler.events.ParserEvent;

/**
 * A parser listener to process crawled linked for a specific Content Provider
 * 
 * @author Joerg Wurzer
 *
 */
public class ParserEventListener implements IParserEventListener {

    /** The Logger */
    private static Logger logger = Logger.getLogger(ParserEventListener.class);

    /** The databse Connection */
    private Connection conn = null;

    /** The item-filter */
    private String itemFilter = null;

    /** The provider-id */
    private String providerId = null;

    public ParserEventListener(String iFilter, String provider) {
        logger.debug("Constructor called f�r provider " + provider);
        itemFilter = iFilter;
        providerId = provider;
    }

    protected void setDatabaseConnection(String protocol, String database, String user, String password) {
        logger.debug("setDatabaseConnection() called for " + database);
        try {
            conn = DriverManager.getConnection(protocol + "//" + database + "?user=" + user + "&password=" + password);
        } catch (SQLException e) {
            logger.error("Couldn't access database - " + e.getMessage());
        }
    }

    /**
	 * Implementation of IParserEventListener includes the logic to add, update or delete
	 * content for the index and analysis process of the core engine.
	 */
    public void parse(ParserEvent event) {
        logger.debug("parse() called for link " + event.getLink().getURI());
        if (event.getLink().getURI().matches(itemFilter)) {
            logger.debug("Content " + event.getLink().getURI() + " matched");
            Statement stmt = null;
            ResultSet rs = null;
            long checksum1 = 0;
            try {
                URL url = new URL(event.getLink().getURI());
                checksum1 = url.openConnection().getLastModified();
            } catch (MalformedURLException mfe) {
                logger.error("Malformed url " + event.getLink().getURI() + " - " + mfe.getMessage());
                return;
            } catch (IOException ioe) {
                logger.error("Couldn't read " + event.getLink().getURI() + " - " + ioe.getMessage());
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
                rs = stmt.executeQuery("SELECT * FROM documents WHERE url='" + event.getLink().getURI() + "'");
                if (rs.first()) {
                    long checksum2 = rs.getLong("checksum");
                    if (checksum1 == checksum2) {
                        stmt.executeUpdate("UPDATE documents SET checked=" + String.valueOf(System.currentTimeMillis()) + " WHERE id=" + rs.getString("id"));
                    } else {
                        stmt.executeUpdate("UPDATE documents SET checksum=" + checksum1 + ", checked=" + String.valueOf(System.currentTimeMillis()) + " WHERE id=" + rs.getString("id"));
                    }
                } else {
                    stmt.executeUpdate("INSERT INTO documents VALUES " + "(DEFAULT, '" + event.getLink().getURI() + "', " + checksum1 + ", '" + providerId + "', " + String.valueOf(System.currentTimeMillis()) + ")");
                }
            } catch (SQLException e) {
                logger.error("Could't perform database query or update - " + e.getMessage());
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

    public void destroy() {
        logger.debug("destroy() called");
        try {
            conn.close();
        } catch (SQLException e) {
            logger.error("Could not close the database connection - " + e.getMessage());
        }
    }
}
