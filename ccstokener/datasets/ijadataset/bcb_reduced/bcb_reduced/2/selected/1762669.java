package se.studieren.dbvote.db.dao.connection;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;

public class PooledConnectionFactory implements IConnectionFactory {

    private static int POOL_MINSIZE = 1;

    private static int POOL_MAXSIZE = 50;

    private static PooledConnectionFactory INSTANCE;

    private DataSource ds = null;

    private PooledConnectionFactory(Properties properties) {
        try {
            String dbDriverClass = properties.getProperty("drivername");
            String dbUrl = properties.getProperty("connectionstring");
            String dbUsername = properties.getProperty("username");
            String dbPassword = properties.getProperty("password");
            Class.forName(dbDriverClass).newInstance();
            ds = setup(dbUrl, dbUsername, dbPassword, POOL_MINSIZE, POOL_MAXSIZE);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static DataSource setup(String dbUrl, String dbUsername, String dbPassword, int minIdle, int maxActive) throws Exception {
        GenericObjectPool connectionPool = new GenericObjectPool(null);
        connectionPool.setMinIdle(minIdle);
        connectionPool.setMaxActive(maxActive);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(dbUrl, dbUsername, dbPassword);
        new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true);
        PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
        return dataSource;
    }

    public static PooledConnectionFactory getConnectionFactory() {
        if (INSTANCE == null) {
            try {
                URL url = PooledConnectionFactory.class.getResource("/se/studieren/dbvote/db/connection.properties");
                Properties properties = new Properties();
                properties.load(url.openStream());
                INSTANCE = new PooledConnectionFactory(properties);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
