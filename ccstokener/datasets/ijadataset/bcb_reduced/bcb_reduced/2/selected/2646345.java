package ru.spbu.dorms.geo.rmp.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.eclipse.core.runtime.Platform;

public class DBAccess {

    private static final String protocol = "jdbc:derby:";

    private final Connection connection;

    private static DBAccess instance;

    public static Connection getConnection() {
        return instance.connection;
    }

    public static void execute(String sql) throws SQLException {
        Statement statement = getConnection().createStatement();
        try {
            statement.execute(sql);
        } finally {
            statement.close();
        }
    }

    /**
	 * 
	 * @param sql
	 * @return id of new row
	 * @throws SQLException
	 */
    public static int executeUpdate(String sql) throws SQLException {
        Statement statement = getConnection().createStatement();
        try {
            int count = statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            if (count > 0) {
                ResultSet rs = statement.getGeneratedKeys();
                if (rs.getMetaData().getColumnCount() > 0) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return -1;
        } finally {
            statement.close();
        }
    }

    public DBAccess(String pathToDb) {
        if (instance != null) {
            throw new IllegalStateException();
        }
        System.setProperty("derby.system.home", pathToDb);
        try {
            boolean exists = new File(pathToDb).exists();
            new EmbeddedDriver();
            Properties props = new Properties();
            if (exists) {
                connection = DriverManager.getConnection(protocol + "rmpDB;", props);
                connection.setAutoCommit(true);
                Statement statement = connection.createStatement();
                try {
                } finally {
                    statement.close();
                }
            } else {
                connection = DriverManager.getConnection(protocol + "rmpDB;create=true", props);
                connection.setAutoCommit(true);
                URL url = Platform.getBundle("ru.spbu.dorms.geo.rmp").getResource("sql/createdb.sql");
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                StringBuilder query = new StringBuilder();
                for (String s = reader.readLine(); s != null; s = reader.readLine()) {
                    if (query.length() > 0) {
                        query.append('\n');
                    }
                    query.append(s);
                }
                reader.close();
                Statement statement = connection.createStatement();
                try {
                    String[] statements = query.toString().split(";(\\s)*");
                    for (String s : statements) {
                        statement.execute(s);
                    }
                } finally {
                    statement.close();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        instance = this;
    }

    public void shutdown() throws SQLException {
        if (instance == null) {
            throw new IllegalStateException();
        }
        instance = null;
        connection.close();
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException se) {
        }
    }
}
