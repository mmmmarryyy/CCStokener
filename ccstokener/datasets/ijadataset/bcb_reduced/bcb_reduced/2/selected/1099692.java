package database;

import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Makes database work with MySQL
 * @author shaferia
 */
public class MySQLDatabase extends Database {

    /**
	 * Connect to the database using default settings, or overriding them with
	 * the MySQL section from config.ini if applicable.
	 */
    public MySQLDatabase() {
        url = "localhost";
        port = "3306";
        database = "SpASMSdb";
        loadConfiguration("MySQL");
    }

    public MySQLDatabase(String dbName) {
        this();
        database = dbName;
    }

    /**
	 * @see InfoWarehouse.java#isPresent()
	 */
    public boolean isPresent() {
        return isPresentImpl("SHOW DATABASES");
    }

    /**
	 * Open a connection to a MySQL database:
	 * uses the jdbc driver from mysql-connector-java-*-bin.jar
	 * TODO: change security model
	 */
    public boolean openConnection() {
        return openConnectionImpl("com.mysql.jdbc.Driver", "jdbc:mysql://" + url + ":" + port + "/" + database, "root", "sa-account-password");
    }

    /**
	 * Open a connection to a MySQL database:
	 * uses the jdbc driver from mysql-connector-java-*-bin.jar
	 * TODO: change security model
	 */
    public boolean openConnectionNoDB() {
        return openConnectionImpl("com.mysql.jdbc.Driver", "jdbc:mysql://" + url + ":" + port, "root", "sa-account-password");
    }

    /**
	 * @return the MySQL native DATETIME format
	 */
    public DateFormat getDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    /**
	 * @return a BatchExecuter that completes batches of commands with
	 * addBatch() and executeBatch(), as MySQL does not support multi-command strings
	 */
    protected BatchExecuter getBatchExecuter(Statement stmt) {
        return new BatchBatchExecuter(stmt);
    }

    protected class BatchBatchExecuter extends BatchExecuter {

        public BatchBatchExecuter(Statement stmt) {
            super(stmt);
        }

        public void append(String sql) throws SQLException {
            stmt.addBatch(sql);
        }

        public void execute() throws SQLException {
            stmt.executeBatch();
        }
    }

    /**
	 * @return a BulkInserter that provides MySQL with a way to read SQL Server - formatted bulk files.
	 */
    protected Inserter getBulkInserter(BatchExecuter stmt, String table) {
        return new BulkInserter(stmt, table) {

            protected String getBatchSQL() {
                return "LOAD DATA INFILE '" + tempFile.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\") + "' INTO TABLE " + table + " FIELDS TERMINATED BY ','" + " ENCLOSED BY '\\''" + " ESCAPED BY '\\\\'";
            }
        };
    }

    /**
	 * Use to indicate that a method isn't complete - 
	 * goes one frame up on the stack trace for information.
	 */
    public void notdone() {
        StackTraceElement sti = null;
        try {
            throw new Exception();
        } catch (Exception ex) {
            sti = ex.getStackTrace()[1];
        }
        String message = "Not done: ";
        message += sti.getClassName() + "." + sti.getMethodName();
        message += "(" + sti.getFileName() + ":" + sti.getLineNumber() + ")";
        System.err.println(message);
    }

    public boolean openConnection(String dbName) {
        return false;
    }
}
