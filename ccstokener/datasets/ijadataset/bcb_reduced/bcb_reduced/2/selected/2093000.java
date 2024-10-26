package com.liferay.jdbc;

import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * <a href="LiferayDriver.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 *
 */
public class LiferayDriver implements Driver {

    static {
        try {
            DriverManager.registerDriver(new LiferayDriver());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LiferayDriver() {
    }

    public boolean acceptsURL(String url) throws SQLException {
        if (_acceptableURLPrefix == null) {
            _acceptableURLPrefix = _getJDBCProperties().getProperty("acceptable.url.prefix");
        }
        if ((_acceptableURLPrefix == null) || (url.startsWith(_acceptableURLPrefix))) {
            _log.fine("Accepts url " + url);
            if (_acceptableURLPrefix == null) {
                int x = url.indexOf(":");
                int y = url.indexOf(":", x + 1);
                _acceptableURLPrefix = url.substring(0, y + 1);
            }
            return true;
        } else {
            _log.fine("Does not accept url " + url);
            return false;
        }
    }

    public synchronized Connection connect(String url, Properties props) throws SQLException {
        if (_driver == null) {
            _log.fine("Driver is null");
            try {
                _log.fine("Url " + url);
                int x = url.indexOf(":");
                int y = url.indexOf(":", x + 1);
                String jdbcType = url.substring(x + 1, y);
                _log.fine("Type " + jdbcType);
                if (jdbcType.equals("db2")) {
                    _db2 = true;
                }
                String driverClassName = _getJDBCProperties().getProperty(jdbcType);
                _log.fine("Class name " + jdbcType);
                Class driverClass = Class.forName(driverClassName);
                _driver = (Driver) driverClass.newInstance();
            } catch (Exception e) {
                throw new SQLException(e.getMessage());
            }
        } else {
            _log.fine("Driver is not null");
        }
        Connection con = _driver.connect(url, props);
        _log.fine("Connecting " + con);
        return new LiferayConnection(con, _db2);
    }

    public int getMajorVersion() {
        return _driver.getMajorVersion();
    }

    public int getMinorVersion() {
        return _driver.getMinorVersion();
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties props) throws SQLException {
        return _driver.getPropertyInfo(url, props);
    }

    public boolean jdbcCompliant() {
        return _driver.jdbcCompliant();
    }

    private Properties _getJDBCProperties() {
        Properties jdbcProps = new Properties();
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            URL url = classLoader.getResource("jdbc.properties");
            if (url != null) {
                InputStream is = url.openStream();
                jdbcProps.load(is);
                is.close();
                System.out.println("Loading " + url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            URL url = classLoader.getResource("jdbc-ext.properties");
            if (url != null) {
                InputStream is = url.openStream();
                jdbcProps.load(is);
                is.close();
                System.out.println("Loading " + url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jdbcProps;
    }

    private static Logger _log = Logger.getLogger(LiferayDriver.class.getName());

    private Driver _driver;

    private String _acceptableURLPrefix;

    private boolean _db2;
}
