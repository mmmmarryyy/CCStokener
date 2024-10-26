package net.sf.csv2sql.utils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class for jdbc resources.
 * @author <a href="mailto:dconsonni@enter.it">Davide Consonni</a>
 */
public class DBUtils {

    /**
     * Open a connection with jdbc resource.
     * load jdbc driver from file (can be also out of classpath)
     */
    public static Connection openConnection(File jdbcJar, String driverClass, String url, String username, String password) throws SQLException, IOException, ClassNotFoundException {
        JarLoader.addFile(jdbcJar);
        Class.forName(driverClass);
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Open a connection with jdbc resource.
     */
    public static Connection openConnection(String driverClass, String url, String username, String password) throws SQLException, ClassNotFoundException {
        Class.forName(driverClass);
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Close current connection with jdbc resource.
     */
    public static void closeConnection(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
