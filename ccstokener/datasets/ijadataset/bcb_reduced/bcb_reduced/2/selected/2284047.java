package com.vividsolutions.jts.io.oracle;

import java.net.URL;
import java.sql.*;
import java.util.Properties;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import oracle.jdbc.OracleConnection;
import junit.framework.TestCase;

/**
 * 
 * Abstract Test Case. Intended to provide a connection with which test may be performed.
 * 
 * Sub-classes should not assume that either the connection will always exist, or 
 * the connection has the required permissions.
 *
 * @author David Zwiers, Vivid Solutions. 
 */
public class ConnectedTestCase extends TestCase {

    /**
	 * @param arg
	 */
    public ConnectedTestCase(String arg) {
        super(arg);
    }

    private OracleConnection connection = null;

    /**
	 * Sub-classes should not assume that either the connection will always exist, or 
	 * the connection has the required permissions.
	 * 
	 * @return OracleConnection
	 */
    protected OracleConnection getConnection() {
        return connection;
    }

    /**
	 * @see junit.framework.TestCase#setUp()
	 */
    protected void setUp() throws Exception {
        super.setUp();
        Properties props = new Properties();
        URL path = ClassLoader.getSystemResource("com/vividsolutions/jts/io/oracle/connection.properties");
        props.load(path.openStream());
        connection = getOracleConnection(props.getProperty("test.server"), props.getProperty("test.port"), props.getProperty("test.sid"), props.getProperty("test.user"), props.getProperty("test.pwd"));
    }

    private static OracleConnection getOracleConnection(String server, String port, String sid, String userid, String pwd) throws SQLException {
        String url = "jdbc:oracle:thin:@" + server + ":" + port + ":" + sid;
        return (OracleConnection) openConnection("oracle.jdbc.driver.OracleDriver", url, userid, pwd);
    }

    private static Connection openConnection(String driver, String url, String uid, String pwd) throws SQLException {
        Connection conn = null;
        try {
            Class.forName(driver);
        } catch (java.lang.ClassNotFoundException e) {
            fail(e.getMessage());
        }
        conn = DriverManager.getConnection(url, uid, pwd);
        return conn;
    }

    /**
	 * @see junit.framework.TestCase#tearDown()
	 */
    protected void tearDown() throws Exception {
        super.tearDown();
        if (connection != null && !connection.isClosed()) connection.close();
    }

    protected static PrecisionModel precisionModel = new PrecisionModel(1000);

    protected static GeometryFactory geometryFactory = new GeometryFactory(precisionModel);
}
