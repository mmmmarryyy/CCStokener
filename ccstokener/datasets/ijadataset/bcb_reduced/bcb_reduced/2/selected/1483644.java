package net.sf.csv2sql.writers;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import net.sf.csv2sql.storage.exceptions.StorageException;
import net.sf.csv2sql.utils.DBUtils;
import net.sf.csv2sql.writers.exceptions.InvalidParameterValueException;
import net.sf.csv2sql.writers.exceptions.WriterException;

/**
 * Load generated statements in a jdbc resource with batch method.
 * Can open connection for every sql statements bundle or use given connection
 * and don't open new connections.
 * @author <a href="mailto:jj1024@users.sourceforge.net">Ivan Ryndin</a>
 */
public class JdbcBatchWriter extends AbstractWriter {

    String driver;

    String url;

    String username;

    String password;

    int batchCount;

    int commitBatchCount;

    File jdbcJar = null;

    Connection connection;

    boolean useGivenConnection = false;

    private void init() throws InvalidParameterValueException {
        driver = getWriterProperties().getProperty("driver");
        url = getWriterProperties().getProperty("url");
        username = getWriterProperties().getProperty("username");
        password = getWriterProperties().getProperty("password");
        batchCount = Integer.parseInt(getWriterProperties().getProperty("batchcount", "10"));
        commitBatchCount = Integer.parseInt(getWriterProperties().getProperty("commitbatchcount", "0"));
        String strJdbcJar = getWriterProperties().getProperty("jdbcjar");
        if (strJdbcJar != null && !strJdbcJar.equals("")) {
            jdbcJar = new File(strJdbcJar);
            if (!jdbcJar.exists()) {
                throw new InvalidParameterValueException("jdbcjar", getWriterProperties().getProperty("jdbcjar"));
            }
        }
    }

    /**
     * @see AbstractWriter#write
     */
    public void write() throws WriterException {
        init();
        String currentSQL = null;
        int sqlIndex = 0;
        try {
            connection = null;
            HashMap writerParameters = getWriterParameters();
            if (writerParameters != null) {
                connection = (Connection) writerParameters.get("connection");
            }
            if (connection == null) {
                if (jdbcJar != null) {
                    connection = DBUtils.openConnection(jdbcJar, driver, url, username, password);
                } else {
                    connection = DBUtils.openConnection(driver, url, username, password);
                }
            } else {
                useGivenConnection = true;
            }
            try {
                Statement stmt = connection.createStatement();
                try {
                    if (commitBatchCount != 1) {
                        connection.setAutoCommit(false);
                    }
                    int batchSize = 0;
                    int commitIdx = 0;
                    for (int i = 0; i < getStorage().size(); i++) {
                        String sql = getStorage().get(i);
                        stmt.addBatch(sql);
                        sqlIndex++;
                        currentSQL = sql;
                        if (++batchSize == batchCount) {
                            batchSize = 0;
                            stmt.executeBatch();
                            if (commitBatchCount > 1 && commitBatchCount == ++commitIdx) {
                                connection.commit();
                                commitIdx = 0;
                            }
                            stmt.clearBatch();
                        }
                    }
                    stmt.executeBatch();
                    stmt.clearBatch();
                    if (commitBatchCount != 1) {
                        connection.commit();
                    }
                } finally {
                    stmt.close();
                }
            } finally {
                if (!useGivenConnection) connection.close();
            }
        } catch (StorageException e) {
            throw new WriterException("cannot read data from temporary storage", e);
        } catch (SQLException e) {
            throw new WriterException("cannot write statement to database: " + sqlIndex + "\n" + e.getMessage() + "\n" + currentSQL, e);
        } catch (IOException e) {
            throw new WriterException("cannot find database driver", e);
        } catch (ClassNotFoundException e) {
            throw new WriterException("cannot load database driver", e);
        }
    }

    protected HashMap requiredParameterList() {
        HashMap hm = new HashMap();
        hm.put("commitbatchcount", "commitbatchcount.");
        return hm;
    }

    protected HashMap optionalParameterList() {
        HashMap hm = new HashMap();
        hm.put("driver", "jdbc driver classname.");
        hm.put("url", "jdbc database url.");
        hm.put("username", "jdbc username.");
        hm.put("password", "jdbc password.");
        hm.put("commit", "true or false. specify the use of transactions.");
        hm.put("jdbcjar", "url of jdbc driver (will be load with a custom classloader).");
        hm.put("batchcount", "batchcount.");
        return hm;
    }
}
