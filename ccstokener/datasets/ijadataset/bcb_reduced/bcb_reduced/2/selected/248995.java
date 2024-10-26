package uk.org.ogsadai.test.server.scenarios;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import junit.framework.TestCase;
import uk.org.ogsadai.activity.delivery.FTPTestConstants;
import uk.org.ogsadai.activity.delivery.FTPTestUtilities;
import uk.org.ogsadai.client.toolkit.DataRequestExecutionResource;
import uk.org.ogsadai.client.toolkit.PipelineWorkflow;
import uk.org.ogsadai.client.toolkit.RequestExecutionType;
import uk.org.ogsadai.client.toolkit.Server;
import uk.org.ogsadai.client.toolkit.activities.delivery.DeliverToFTP;
import uk.org.ogsadai.client.toolkit.activities.sql.SQLQuery;
import uk.org.ogsadai.client.toolkit.activities.transform.TupleToWebRowSetCharArrays;
import uk.org.ogsadai.converters.webrowset.resultset.WebRowSetToResultSet;
import uk.org.ogsadai.database.jdbc.JDBCConnection;
import uk.org.ogsadai.database.jdbc.book.JDBCBookDataCreator;
import uk.org.ogsadai.resource.ResourceID;
import uk.org.ogsadai.resource.request.RequestExecutionStatus;
import uk.org.ogsadai.resource.request.RequestStatus;
import uk.org.ogsadai.test.jdbc.TestJDBCConnectionProperties;
import uk.org.ogsadai.test.jdbc.JDBCTestHelper;
import uk.org.ogsadai.test.server.ServerTestProperties;
import uk.org.ogsadai.test.server.TestServerProxyFactory;
import uk.org.ogsadai.test.server.activity.sql.JDBCServerTestConstants;

/**
 * Tests the scenario when we execute an SQL query, convert the result to
 * WebRowSet and then FTP it. This class expects test properties to be
 * provided in a file whose location is specified in a system property,
 * <code>ogsadai.test.properties</code>. The following properties need
 * to be provided:
 * <ul>
 * <li>General server test properties:
 * <ul>
 * <li>
 * <code>server.url</code> - server URL (depends on server type).
 * </li>
 * <li>
 * <code>server.proxy.factory</code> - name of class used to create
 * client toolkit proxty server (depends on server type).
 * </li>
 * <li>
 * <code>server.version</code> - server version ID (depends on server type). 
 * </li>
 * <li>
 * <code>server.drer.id</code> - DRER ID on test server.
 * </li>
 * <li>
 * Additional properties may be required depending on the server type.
 * </li>
 * </ul>
 * </li>
 * <li>
 * Test-specific properties.
 * <ul>
 * <li>
 * <code>server.jdbc.resource.id</code> - ID of OGSA-DAI data
 * resource on server exposing a relational database. 
 * </li>
 * <li>
 * <code>jdbc.connection.url</code> - URL of the relational database
 * exposed by the above resource. 
 * </li>
 * <li>
 * <code>jdbc.driver.class</code> - JDBC driver class name.
 * </li>
 * <li>
 * <code>jdbc.user.name</code> - user name for above URL.
 * </li>
 * <li>
 * <code>jdbc.password</code> - password for above user name.
 * </li>
 * <li>
 * <code>ftp.host</code> - FTP host e.g. <code>something.someplace.org</code>
 * </li>
 * <li>
 * <code>ftp.port</code> - FTP port (typically 21).
 * </li>
 * <li>
 * <code>ftp.user</code> - FTP user name.
 * </li>
 * <li>
 * <code>ftp.password</code> - FTP password.
 * </li>
 * </ul>
 * </li>
 * </ul>
 *
 * @author The OGSA-DAI Project Team.
 */
public class SQLQueryWebRowSetDeliverToFTPTest extends TestCase {

    /** Copyright notice. */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh, 2007-2009.";

    /** Test table name. */
    private static final String TABLE = "SQCDTFTPTestTable";

    /** Test file name. */
    private static final String FILE = "SQLQueryWebRowSetDeliverToFTP.txt";

    /** Test properties. */
    private final ServerTestProperties mProperties;

    /** Basic (unsecure) server. */
    private Server mServer;

    /** DRER to test. */
    private DataRequestExecutionResource mDRER;

    /** JDBC test book data. */
    private JDBCBookDataCreator mBookDataCreator;

    /** Connection to database. */
    private JDBCConnection mConnection;

    /** Resource ID. */
    private ResourceID mResourceID;

    /** FTP host. */
    private String mHost;

    /** FTP port. */
    private int mPort;

    /** FTP user. */
    private String mUser;

    /** FTP password. */
    private String mPassword;

    /** FTP URL. */
    private String mURL;

    /**
     * Constructor.
     *
     * @param name
     *     Test case name.
     * @throws Exception
     *     If any problems arise in reading the test properties.
     */
    public SQLQueryWebRowSetDeliverToFTPTest(final String name) throws Exception {
        super(name);
        mProperties = new ServerTestProperties();
    }

    /**
     * Runs the test cases.
     * 
     * @param args
     *     Not used
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(SQLQueryWebRowSetDeliverToFTPTest.class);
    }

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        mResourceID = new ResourceID(mProperties.getProperty(JDBCServerTestConstants.ID));
        mHost = mProperties.getProperty(FTPTestConstants.FTP_HOST);
        mPort = Integer.parseInt(mProperties.getProperty(FTPTestConstants.FTP_PORT));
        mUser = mProperties.getProperty(FTPTestConstants.FTP_USER);
        mPassword = mProperties.getProperty(FTPTestConstants.FTP_PASSWORD);
        mURL = mUser + ":" + mPassword + "@" + mHost + ":" + mPort;
        TestJDBCConnectionProperties connectionProperties = new TestJDBCConnectionProperties(mProperties);
        mConnection = new JDBCConnection(connectionProperties);
        mConnection.openConnection();
        mBookDataCreator = new JDBCBookDataCreator();
        mBookDataCreator.create(mConnection.getConnection(), TABLE);
        mBookDataCreator.populate(mConnection.getConnection(), TABLE, 20);
        mServer = TestServerProxyFactory.getServerProxy(mProperties);
        mDRER = mServer.getDataRequestExecutionResource(mProperties.getDRERID());
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {
        try {
            FTPTestUtilities.deleteFiles(mHost, mPort, mUser, mPassword, new String[] { FILE });
        } catch (Exception e) {
        }
        mBookDataCreator.drop(mConnection.getConnection(), TABLE);
        mConnection.closeConnection();
    }

    /**
     * Tests the scenario.
     * 
     * @throws Exception 
     *     If an unexpected error occurs.
     */
    public void testScenario() throws Exception {
        String expression = "SELECT id, name, address, phone FROM " + TABLE + " where id > 2 and id < 12 order by id";
        SQLQuery query = new SQLQuery();
        query.setResourceID(mResourceID);
        query.addExpression(expression);
        TupleToWebRowSetCharArrays tupleToWebRowSet = new TupleToWebRowSetCharArrays();
        tupleToWebRowSet.connectDataInput(query.getDataOutput());
        DeliverToFTP deliverToFTP = new DeliverToFTP();
        deliverToFTP.connectDataInput(tupleToWebRowSet.getResultOutput());
        deliverToFTP.addFilename(FILE);
        deliverToFTP.addHost(mURL);
        PipelineWorkflow pipeline = new PipelineWorkflow();
        pipeline.add(query);
        pipeline.add(tupleToWebRowSet);
        pipeline.add(deliverToFTP);
        mDRER.execute(pipeline, RequestExecutionType.SYNCHRONOUS);
        final URL url = new URL("ftp://" + mURL + "/" + FILE);
        final URLConnection connection = url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(false);
        InputStream is = connection.getInputStream();
        WebRowSetToResultSet converter = new WebRowSetToResultSet(new InputStreamReader(is));
        converter.setResultSetType(ResultSet.TYPE_FORWARD_ONLY);
        ResultSet rs = converter.getResultSet();
        JDBCTestHelper.validateResultSet(mConnection, expression, rs, 1);
        rs.close();
    }
}
