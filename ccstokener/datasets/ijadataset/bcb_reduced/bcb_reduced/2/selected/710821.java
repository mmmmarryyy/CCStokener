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
 * Load generated statements in a jdbc resource.
 * @see AbstractWriter AbstractWriter
 * @author <a href="mailto:dconsonni@enter.it">Davide Consonni</a>
 */
public class JdbcWriter extends AbstractWriter {

    String driver;

    String url;

    String username;

    String password;

    File jdbcJar = null;

    boolean commit;

    private void init() throws InvalidParameterValueException {
        driver = getWriterProperties().getProperty("driver");
        url = getWriterProperties().getProperty("url");
        username = getWriterProperties().getProperty("username");
        password = getWriterProperties().getProperty("password");
        commit = "true".equalsIgnoreCase(getWriterProperties().getProperty("commit"));
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
        Connection connection = null;
        Statement stmt = null;
        try {
            init();
            if (jdbcJar != null) {
                connection = DBUtils.openConnection(jdbcJar, driver, url, username, password);
            } else {
                connection = DBUtils.openConnection(driver, url, username, password);
            }
            stmt = connection.createStatement();
            if (commit) {
                connection.setAutoCommit(false);
            }
            for (int i = 0; i < getStorage().size(); i++) {
                String line = (String) getStorage().get(i);
                stmt.executeUpdate(line.substring(0, line.length() - 1));
            }
            if (commit) {
                connection.commit();
            }
        } catch (StorageException e) {
            throw new WriterException("cannot read data from temporary storage", e);
        } catch (SQLException e) {
            throw new WriterException("cannot write statement to database", e);
        } catch (IOException e) {
            throw new WriterException("cannot find database driver", e);
        } catch (ClassNotFoundException e) {
            throw new WriterException("cannot load database driver", e);
        } finally {
            DBUtils.closeConnection(connection);
        }
    }

    protected HashMap requiredParameterList() {
        HashMap hm = new HashMap();
        hm.put("driver", "jdbc driver classname.");
        hm.put("url", "jdbc database url.");
        hm.put("username", "jdbc username.");
        return hm;
    }

    protected HashMap optionalParameterList() {
        HashMap hm = new HashMap();
        hm.put("password", "jdbc password.");
        hm.put("commit", "true or false. specify the use of transactions.");
        hm.put("jdbcjar", "url of jdbc driver (will be load with a custom classloader).");
        return hm;
    }
}
