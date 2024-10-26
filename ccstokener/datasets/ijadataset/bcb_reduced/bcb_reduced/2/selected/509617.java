package uk.org.ogsadai.test.server.activity.delivery;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import junit.framework.TestCase;
import uk.org.ogsadai.client.toolkit.DataRequestExecutionResource;
import uk.org.ogsadai.client.toolkit.PipelineWorkflow;
import uk.org.ogsadai.client.toolkit.RequestExecutionType;
import uk.org.ogsadai.client.toolkit.RequestResource;
import uk.org.ogsadai.client.toolkit.Server;
import uk.org.ogsadai.client.toolkit.activities.delivery.DeliverToRequestStatus;
import uk.org.ogsadai.client.toolkit.activities.delivery.WriteToWebServerFile;
import uk.org.ogsadai.client.toolkit.activities.util.Echo;
import uk.org.ogsadai.data.BinaryData;
import uk.org.ogsadai.data.CharData;
import uk.org.ogsadai.test.server.ServerTestProperties;
import uk.org.ogsadai.test.server.TestServerProxyFactory;

/**
 * Server tests for WriteToWebServerFile. This class expects  
 * test properties to be provided in a file whose location is
 * specified in a system property,
 * <code>ogsadai.test.properties</code>. The following properties need
 * to be provided:
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
 *
 * @author The OGSA-DAI Project Team.
 */
public class WriteToWebServerFileTest extends TestCase {

    /** Copyright statement. */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh, 2010.";

    /** Test properties. */
    private final ServerTestProperties mProperties;

    /** DRER to test. */
    private DataRequestExecutionResource mDRER;

    /** Basic (insecure) server to test against. */
    private Server mServer;

    /**
     * Constructor.
     *
     * @param name
     *     Test case name.
     * @throws Exception
     *     If any problems arise in reading the test properties.
     */
    public WriteToWebServerFileTest(String name) throws Exception {
        super(name);
        mProperties = new ServerTestProperties();
    }

    /**
     * {@inheriDoc}
     */
    public void setUp() throws Exception {
        mServer = TestServerProxyFactory.getServerProxy(mProperties);
        mDRER = mServer.getDataRequestExecutionResource(mProperties.getDRERID());
    }

    /**
     * Test creation of a file using a character data array.
     * 
     * @throws Exception
     *     If any problems arise.
     */
    public void testCharArrayFile() throws Exception {
        char[] charData = "Some char data".toCharArray();
        Echo echo = new Echo();
        echo.addListBeginToInput();
        echo.addInput(new CharData(charData));
        echo.addListEndToInput();
        WriteToWebServerFile writeFile = new WriteToWebServerFile();
        writeFile.connectDataInput(echo.getOutput());
        validate(echo, writeFile, new char[][] { charData });
    }

    /**
     * Test creation of a file using multiple character data arrays.
     * 
     * @throws Exception
     *     If any problems arise.
     */
    public void testCharArraysFile() throws Exception {
        String str1 = "The quick brown fox ";
        String str2 = "jumped over ";
        String str3 = "the lazy dog ";
        char[] charData = (str1 + str2 + str3).toCharArray();
        Echo echo = new Echo();
        echo.addListBeginToInput();
        echo.addInput(new CharData(str1.toCharArray()));
        echo.addInput(new CharData(str2.toCharArray()));
        echo.addInput(new CharData(str3.toCharArray()));
        echo.addListEndToInput();
        WriteToWebServerFile writeFile = new WriteToWebServerFile();
        writeFile.connectDataInput(echo.getOutput());
        validate(echo, writeFile, new char[][] { charData });
    }

    /**
     * Test creation of a file using multiple byte arrays.
     * 
     * @throws Exception
     *     If any problems arise.
     */
    public void testBinaryArrayFile() throws Exception {
        String str1 = "The quick brown fox ";
        String str2 = "jumped over ";
        String str3 = "the lazy dog ";
        Echo echo = new Echo();
        echo.addListBeginToInput();
        echo.addInput(new BinaryData(str1.getBytes()));
        echo.addInput(new BinaryData(str2.getBytes()));
        echo.addInput(new BinaryData(str3.getBytes()));
        echo.addListEndToInput();
        WriteToWebServerFile writeFile = new WriteToWebServerFile();
        writeFile.connectDataInput(echo.getOutput());
        char[] charData = (str1 + str2 + str3).toCharArray();
        validate(echo, writeFile, new char[][] { charData });
    }

    /**
     * Test creation of a in a sub-directory with a given file
     * extension using a character data array.
     * 
     * @throws Exception
     *     If any problems arise.
     */
    public void testCharArrayFileInSubDirWithExt() throws Exception {
        char[] charData = "Some char data".toCharArray();
        String ext = "ext";
        String subDir = "subDir";
        Echo echo = new Echo();
        echo.addListBeginToInput();
        echo.addInput(new CharData(charData));
        echo.addListEndToInput();
        WriteToWebServerFile writeFile = new WriteToWebServerFile();
        writeFile.connectDataInput(echo.getOutput());
        writeFile.addExtension(ext);
        writeFile.addSubDirectory(subDir);
        validate(echo, writeFile, new char[][] { charData });
    }

    /**
     * Test creation of multiple files using character data arrays.
     * 
     * @throws Exception
     *     If any problems arise.
     */
    public void testCharArrayFiles() throws Exception {
        char[] charData1 = "Some char data".toCharArray();
        char[] charData2 = "Some more char data".toCharArray();
        char[] charData3 = "Yet more char data".toCharArray();
        Echo echo = new Echo();
        echo.addListBeginToInput();
        echo.addInput(new CharData(charData1));
        echo.addListEndToInput();
        echo.addListBeginToInput();
        echo.addInput(new CharData(charData2));
        echo.addListEndToInput();
        echo.addListBeginToInput();
        echo.addInput(new CharData(charData3));
        echo.addListEndToInput();
        WriteToWebServerFile writeFile = new WriteToWebServerFile();
        writeFile.connectDataInput(echo.getOutput());
        validate(echo, writeFile, new char[][] { charData1, charData2, charData3 });
    }

    /**
     * Run workflow and validate the data returned via the WriteToWebServerFile 
     * activity proxy. Get each URL from the activity and check that the content at the URL 
     * equals that of the corresponding character array
     * 
     * @param echo
     *     Client toolkit activity proxy.
     * @param writeFile
     *     Client toolkit activity proxy.
     * @param charData 
     *     Expected content at each of the N URLs.
     * @throws Exception
     *     If any problems arise.
     */
    private void validate(Echo echo, WriteToWebServerFile writeFile, char[][] charData) throws Exception {
        DeliverToRequestStatus deliver = new DeliverToRequestStatus();
        deliver.connectInput(writeFile.getResultOutput());
        PipelineWorkflow pipeline = new PipelineWorkflow();
        pipeline.add(echo);
        pipeline.add(writeFile);
        pipeline.add(deliver);
        RequestResource request = mDRER.execute(pipeline, RequestExecutionType.SYNCHRONOUS);
        String id = request.getResourceID().toString();
        validate(id, writeFile, charData);
    }

    /**
     * Test creation of multiple files using character data arrays.
     * 
     * @throws Exception
     *     If any problems arise.
     */
    public void testMultipleActivities() throws Exception {
        char[] charData1 = "Some char data".toCharArray();
        char[] charData2 = "Some more char data".toCharArray();
        char[] charData3 = "Yet more char data".toCharArray();
        Echo echo1 = new Echo();
        echo1.addListBeginToInput();
        echo1.addInput(new CharData(charData1));
        echo1.addListEndToInput();
        Echo echo2 = new Echo();
        echo2.addListBeginToInput();
        echo2.addInput(new CharData(charData2));
        echo2.addListEndToInput();
        Echo echo3 = new Echo();
        echo3.addListBeginToInput();
        echo3.addInput(new CharData(charData3));
        echo3.addListEndToInput();
        WriteToWebServerFile writeFile1 = new WriteToWebServerFile();
        writeFile1.connectDataInput(echo1.getOutput());
        WriteToWebServerFile writeFile2 = new WriteToWebServerFile();
        writeFile2.connectDataInput(echo2.getOutput());
        WriteToWebServerFile writeFile3 = new WriteToWebServerFile();
        writeFile3.connectDataInput(echo3.getOutput());
        DeliverToRequestStatus deliver1 = new DeliverToRequestStatus();
        deliver1.connectInput(writeFile1.getResultOutput());
        DeliverToRequestStatus deliver2 = new DeliverToRequestStatus();
        deliver2.connectInput(writeFile2.getResultOutput());
        DeliverToRequestStatus deliver3 = new DeliverToRequestStatus();
        deliver3.connectInput(writeFile3.getResultOutput());
        PipelineWorkflow pipeline = new PipelineWorkflow();
        pipeline.add(echo1);
        pipeline.add(echo2);
        pipeline.add(echo3);
        pipeline.add(writeFile1);
        pipeline.add(writeFile2);
        pipeline.add(writeFile3);
        pipeline.add(deliver1);
        pipeline.add(deliver2);
        pipeline.add(deliver3);
        RequestResource request = mDRER.execute(pipeline, RequestExecutionType.SYNCHRONOUS);
        String id = request.getResourceID().toString();
        validate(id, writeFile1, new char[][] { charData1 });
        validate(id, writeFile2, new char[][] { charData2 });
        validate(id, writeFile3, new char[][] { charData3 });
    }

    /**
     * Test creation of multiple files to the same sub-directory using character data arrays.
     * 
     * @throws Exception
     *     If any problems arise.
     */
    public void testMultipleActivitiesSameSubdir() throws Exception {
        char[] charData1 = "Some char data".toCharArray();
        char[] charData2 = "Some more char data".toCharArray();
        char[] charData3 = "Yet more char data".toCharArray();
        Echo echo1 = new Echo();
        echo1.addListBeginToInput();
        echo1.addInput(new CharData(charData1));
        echo1.addListEndToInput();
        Echo echo2 = new Echo();
        echo2.addListBeginToInput();
        echo2.addInput(new CharData(charData2));
        echo2.addListEndToInput();
        Echo echo3 = new Echo();
        echo3.addListBeginToInput();
        echo3.addInput(new CharData(charData3));
        echo3.addListEndToInput();
        String subDir = "subDir";
        WriteToWebServerFile writeFile1 = new WriteToWebServerFile();
        writeFile1.connectDataInput(echo1.getOutput());
        writeFile1.addSubDirectory(subDir);
        WriteToWebServerFile writeFile2 = new WriteToWebServerFile();
        writeFile2.connectDataInput(echo2.getOutput());
        writeFile2.addSubDirectory(subDir);
        WriteToWebServerFile writeFile3 = new WriteToWebServerFile();
        writeFile3.connectDataInput(echo3.getOutput());
        writeFile3.addSubDirectory(subDir);
        DeliverToRequestStatus deliver1 = new DeliverToRequestStatus();
        deliver1.connectInput(writeFile1.getResultOutput());
        DeliverToRequestStatus deliver2 = new DeliverToRequestStatus();
        deliver2.connectInput(writeFile2.getResultOutput());
        DeliverToRequestStatus deliver3 = new DeliverToRequestStatus();
        deliver3.connectInput(writeFile3.getResultOutput());
        PipelineWorkflow pipeline = new PipelineWorkflow();
        pipeline.add(echo1);
        pipeline.add(echo2);
        pipeline.add(echo3);
        pipeline.add(writeFile1);
        pipeline.add(writeFile2);
        pipeline.add(writeFile3);
        pipeline.add(deliver1);
        pipeline.add(deliver2);
        pipeline.add(deliver3);
        RequestResource request = mDRER.execute(pipeline, RequestExecutionType.SYNCHRONOUS);
        String id = request.getResourceID().toString();
        validate(id, writeFile1, new char[][] { charData1 });
        validate(id, writeFile2, new char[][] { charData2 });
        validate(id, writeFile3, new char[][] { charData3 });
    }

    /**
     * Validate the data returned via the WriteToWebServerFile 
     * activity proxy. Get each URL from the activity and check that the content at the URL 
     * equals that of the corresponding character array
     * 
     * @param id
     *     Request resource ID.
     * @param writeFile
     *     Client toolkit activity proxy.
     * @param charData 
     *     Expected content at each of the N URLs.
     * @throws Exception
     *     If any problems arise.
     */
    private void validate(String id, WriteToWebServerFile writeFile, char[][] charData) throws Exception {
        for (int i = 0; i < charData.length; i++) {
            assertTrue("There is a URL for input " + i, writeFile.hasNextURL());
            URL url = writeFile.nextURL();
            String path = url.getPath();
            assertTrue("URL " + url + " contains request resource ID", path.indexOf(id) != -1);
            URLConnection connection = url.openConnection();
            Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            int value;
            int index = 0;
            while (((value = reader.read()) != -1) && (index < charData[i].length)) {
                assertEquals("Character data " + i + " : " + index, (int) charData[i][index], value);
                index++;
            }
        }
    }
}
