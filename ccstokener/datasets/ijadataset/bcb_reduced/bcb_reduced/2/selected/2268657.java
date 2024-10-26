package il.ac.tau.dbcourse.db;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Properties;

public class ConnectionFactory {

    private static String configPath = "il/ac/tau/dbcourse/db/db.properties";

    private static Properties load() throws DBException {
        Properties props = new Properties() {

            String computerName = "";

            {
                try {
                    computerName = java.net.InetAddress.getLocalHost().getHostName();
                } catch (Exception e) {
                }
            }

            @Override
            public String getProperty(String key) {
                String val = super.getProperty(computerName + "." + key);
                if (val != null) return val; else return super.getProperty(key);
            }
        };
        try {
            URL url = ClassLoader.getSystemResource(ConnectionFactory.configPath);
            props.load(url.openStream());
        } catch (IOException e) {
            throw new DBException("Could not load config file - " + e.getMessage());
        }
        return props;
    }

    public static void setConfigPath(String path) {
        ConnectionFactory.configPath = path;
    }

    private static Connection getConnection(String driverName, String url, String username, String password) throws DBException {
        try {
            Class.forName(driverName);
            Connection connection = DriverManager.getConnection(url, username, password);
            if (connection == null) throw new DBException("Could not connect to DB");
            return connection;
        } catch (ClassNotFoundException e) {
            throw new DBException("Could not find JDBC driver - " + e.getMessage());
        } catch (SQLException e) {
            throw new DBException("Could not connect to DB - " + e.getMessage());
        }
    }

    public static Connection getConnection(String username, String password) throws DBException {
        Properties prop = new Properties(ConnectionFactory.load());
        String dbServer = prop.getProperty("dbServer");
        String dbPort = prop.getProperty("dbPort");
        String dbSID = prop.getProperty("dbSID");
        String driverName = prop.getProperty("driverName");
        String connectionString = prop.getProperty("connectionString");
        connectionString = MessageFormat.format(connectionString, dbServer, dbPort, dbSID);
        return getConnection(driverName, connectionString, username, password);
    }

    public static Connection getConnection() throws DBException {
        Properties prop = load();
        return getConnection(prop.getProperty("dbUsername"), prop.getProperty("dbPassword"));
    }
}
