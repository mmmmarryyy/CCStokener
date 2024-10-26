package axs.jdbc.driver;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Vector;
import axs.jdbc.dataSourceConfiguration.JdbcSourceConfiguration;
import axs.jdbc.utils.UrlUtils;

/**
 * This class implements the Statement interface for the WsvJdbc driver.
 *
 * @author     Jonathan Ackerman
 * @author     Sander Brienen
 * @author     Michael Maraya
 * @author     Tomasz Skutnik
 * @author     Daniele Pes
 * @created    25 November 2001
 */
public class WsvStatement implements Statement {

    private WsvConnection connection;

    private Vector resultSets = new Vector();

    /**
    *Constructor for the CsvStatement object
    *
    * @param  connection  Description of Parameter
    * @since
    */
    protected WsvStatement(WsvConnection connection) {
        DriverManager.println("WsvJdbc - WsvStatement() - connection=" + connection);
        this.connection = connection;
    }

    /**
    *Sets the maxFieldSize attribute of the CsvStatement object
    *
    * @param  p0                The new maxFieldSize value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public void setMaxFieldSize(int p0) throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Sets the maxRows attribute of the CsvStatement object
    *
    * @param  p0                The new maxRows value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public void setMaxRows(int p0) throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Sets the escapeProcessing attribute of the CsvStatement object
    *
    * @param  p0                The new escapeProcessing value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public void setEscapeProcessing(boolean p0) throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Sets the queryTimeout attribute of the CsvStatement object
    *
    * @param  p0                The new queryTimeout value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public void setQueryTimeout(int p0) throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Sets the cursorName attribute of the CsvStatement object
    *
    * @param  p0                The new cursorName value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public void setCursorName(String p0) throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Sets the fetchDirection attribute of the CsvStatement object
    *
    * @param  p0                The new fetchDirection value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public void setFetchDirection(int p0) throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Sets the fetchSize attribute of the CsvStatement object
    *
    * @param  p0                The new fetchSize value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public void setFetchSize(int p0) throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Gets the maxFieldSize attribute of the CsvStatement object
    *
    * @return                   The maxFieldSize value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public int getMaxFieldSize() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Gets the maxRows attribute of the CsvStatement object
    *
    * @return                   The maxRows value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public int getMaxRows() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Gets the queryTimeout attribute of the CsvStatement object
    *
    * @return                   The queryTimeout value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public int getQueryTimeout() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Gets the warnings attribute of the CsvStatement object
    *
    * @return                   The warnings value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public SQLWarning getWarnings() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Gets the resultSet attribute of the CsvStatement object
    *
    * @return                   The resultSet value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public ResultSet getResultSet() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Gets the updateCount attribute of the CsvStatement object
    *
    * @return                   The updateCount value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public int getUpdateCount() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Gets the moreResults attribute of the CsvStatement object
    *
    * @return                   The moreResults value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public boolean getMoreResults() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Gets the fetchDirection attribute of the CsvStatement object
    *
    * @return                   The fetchDirection value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public int getFetchDirection() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Gets the fetchSize attribute of the CsvStatement object
    *
    * @return                   The fetchSize value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public int getFetchSize() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Gets the resultSetConcurrency attribute of the CsvStatement object
    *
    * @return                   The resultSetConcurrency value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public int getResultSetConcurrency() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Gets the resultSetType attribute of the CsvStatement object
    *
    * @return                   The resultSetType value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public int getResultSetType() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Gets the connection attribute of the CsvStatement object
    *
    * @return                   The connection value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public Connection getConnection() throws SQLException {
        return connection;
    }

    /**
    * (1) Parses SQL statement to identify the name of table and of its colmuns, their types 
    * 	 and to find out if records have to be counted.
    * (2) Gets URL connection to source table.
    * (3) Gets input stream over data source.
    * (4) Gets a Reader. Note: a new Reader is created for every new executeQuery -> a new 
    * 	 buffered reader will be created for each execution of a SQL command 
    * (5) Gets a ResultSet.
    *
    * @param     sql          The SQL statement.
    * @return                 A ResyltSet object holding the result coming from the execution of the SQL statement.
    * @exception SQLException 
    * @since
    */
    public ResultSet executeQuery(String sql) throws SQLException {
        DriverManager.println("WsvJdbc - WsvStatement:executeQuery() - sql= " + sql);
        WsvSqlParser parser;
        parser = new WsvSqlParser();
        try {
            parser.parse(sql, connection);
        } catch (Exception e) {
            throw new SQLException("Syntax Error. " + e.getMessage());
        }
        java.net.URLConnection urlConnection;
        URL fileURL;
        String path;
        fileURL = null;
        path = connection.getPath() + parser.getTableName() + connection.getExtension();
        try {
            fileURL = UrlUtils.verifyAndSetUrl(path);
            urlConnection = fileURL.openConnection();
        } catch (java.io.IOException ioe) {
            throw new SQLException("Cannot open data file '" + fileURL.toExternalForm() + "'  ! Message was: " + ioe);
        }
        try {
            urlConnection.getInputStream();
        } catch (java.io.IOException ioe) {
            throw new SQLException("Data file '" + fileURL.toExternalForm() + "'  not readable ! Message was: " + ioe);
        }
        WsvReader reader;
        try {
            reader = getReader(parser, fileURL);
        } catch (Exception e) {
            throw new SQLException("Error reading data file. Message was: " + e + " (maybe the file is empty!) ");
        }
        WsvResultSet resultSet;
        resultSet = getResultSet(parser, reader);
        resultSets.add(resultSet);
        return resultSet;
    }

    private WsvResultSet getResultSet(WsvSqlParser parser, WsvReader reader) {
        WsvResultSet resultSet;
        if (parser.haveRecordsToBeCounted()) resultSet = new WsvResultSet(this, reader, parser.getTableName(), parser.getColumnNames(), parser.getColumnTypes(), parser.getTrimIfString()); else {
            String[] colNames;
            Class[] colTypes;
            colNames = connection.getColumnNames() != null ? connection.getColumnNames() : reader.getColumnNames();
            colTypes = connection.getColumnTypes() != null ? connection.getColumnTypes() : reader.getColumnTypes();
            resultSet = new WsvResultSet(this, reader, parser.getTableName(), colNames, colTypes, parser.getTrimIfString());
        }
        return resultSet;
    }

    private WsvReader getReader(WsvSqlParser parser, URL fileURL) throws Exception {
        WsvReader reader;
        Integer[] beginBoundaries, endBoundaries, widths;
        widths = connection.getColumnWidths();
        if (connection.getSeperator().trim().equalsIgnoreCase(JdbcSourceConfiguration.FIXED_LENGHT_SEPARATOR)) {
            beginBoundaries = connection.getColumnBeginBoundaries();
            endBoundaries = connection.getColumnEndBoundaries();
        } else {
            beginBoundaries = null;
            endBoundaries = null;
        }
        if (parser.colsIncludeCounter()) reader = new WsvReader(fileURL, connection.getSeperator(), connection.isSuppressHeaders(), connection.getCharset(), parser.getColumnNames(), parser.getColumnTypes(), widths, beginBoundaries, endBoundaries, true, connection.getSkipLines(), connection.getDateFormats()); else reader = new WsvReader(fileURL, connection.getSeperator(), connection.isSuppressHeaders(), connection.getCharset(), connection.getColumnNames(), connection.getColumnTypes(), widths, beginBoundaries, endBoundaries, false, connection.getSkipLines(), connection.getDateFormats());
        return reader;
    }

    /**
    *Description of the Method
    *
    * @param  sql               Description of Parameter
    * @return                   Description of the Returned Value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public int executeUpdate(String sql) throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    * Releases this <code>Statement</code> object's database
    * and JDBC resources immediately instead of waiting for
    * this to happen when it is automatically closed.
    * It is generally good practice to release resources as soon as
    * you are finished with them to avoid tying up database
    * resources.
    * <P>
    * Calling the method <code>close</code> on a <code>Statement</code>
    * object that is already closed has no effect.
    * <P>
    * <B>Note:</B> A <code>Statement</code> object is automatically closed
    * when it is garbage collected. When a <code>Statement</code> object is
    * closed, its current <code>ResultSet</code> object, if one exists, is
    * also closed.
    *
    * @exception SQLException if a database access error occurs
    */
    public void close() throws SQLException {
        for (Enumeration i = resultSets.elements(); i.hasMoreElements(); ) {
            WsvResultSet resultSet = (WsvResultSet) i.nextElement();
            resultSet.close();
        }
    }

    /**
    *Description of the Method
    *
    * @exception  SQLException  Description of Exception
    * @since
    */
    public void cancel() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Description of the Method
    *
    * @exception  SQLException  Description of Exception
    * @since
    */
    public void clearWarnings() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Description of the Method
    *
    * @param  p0                Description of Parameter
    * @return                   Description of the Returned Value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public boolean execute(String p0) throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Adds a feature to the Batch attribute of the CsvStatement object
    *
    * @param  p0                The feature to be added to the Batch attribute
    * @exception  SQLException  Description of Exception
    * @since
    */
    public void addBatch(String p0) throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Description of the Method
    *
    * @exception  SQLException  Description of Exception
    * @since
    */
    public void clearBatch() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    /**
    *Description of the Method
    *
    * @return                   Description of the Returned Value
    * @exception  SQLException  Description of Exception
    * @since
    */
    public int[] executeBatch() throws SQLException {
        throw new SQLException("Not Supported !");
    }

    public boolean getMoreResults(int current) throws SQLException {
        throw new UnsupportedOperationException("Statement.getMoreResults(int) unsupported");
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        throw new UnsupportedOperationException("Statement.getGeneratedKeys() unsupported");
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw new UnsupportedOperationException("Statement.executeUpdate(String,int) unsupported");
    }

    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw new UnsupportedOperationException("Statement.executeUpdate(String,int[]) unsupported");
    }

    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw new UnsupportedOperationException("Statement.executeUpdate(String,String[]) unsupported");
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw new UnsupportedOperationException("Statement.execute(String,int) unsupported");
    }

    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw new UnsupportedOperationException("Statement.execute(String,int[]) unsupported");
    }

    public boolean execute(String sql, String[] columnNames) throws SQLException {
        throw new UnsupportedOperationException("Statement.execute(String,String[]) unsupported");
    }

    public int getResultSetHoldability() throws SQLException {
        throw new UnsupportedOperationException("Statement.getResultSetHoldability() unsupported");
    }
}
