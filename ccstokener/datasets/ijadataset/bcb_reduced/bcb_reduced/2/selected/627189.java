package uk.org.ogsadai.test.server.activity.delivery;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import uk.org.ogsadai.client.toolkit.DataRequestExecutionResource;
import uk.org.ogsadai.client.toolkit.PipelineWorkflow;
import uk.org.ogsadai.client.toolkit.RequestExecutionType;
import uk.org.ogsadai.client.toolkit.Server;
import uk.org.ogsadai.client.toolkit.activities.delivery.DeliverToRequestStatus;
import uk.org.ogsadai.client.toolkit.activities.delivery.WriteToWebServerFile;
import uk.org.ogsadai.client.toolkit.activities.util.Echo;
import uk.org.ogsadai.data.CharData;
import uk.org.ogsadai.test.server.ServerTestProperties;
import uk.org.ogsadai.test.server.TestServerProxyFactory;

/**
 * Server tests for {@link uk.org.ogsadai.rest.files.RESTBaseDirectory} REST
 * interface. These tests use WriteToWebServerFile. URLs are therefore expected
 * to be of form
 * 
 * <pre>
 * http://.../requestID/subDir/file/fileName.ext
 * </pre>
 * 
 * This class expects test properties to be provided in a file whose location is
 * specified in a system property, <code>ogsadai.test.properties</code>. The
 * following properties need to be provided:
 * <ul>
 * <li>
 * <code>server.url</code> - server URL (depends on server type).</li>
 * <li>
 * <code>server.proxy.factory</code> - name of class used to create client
 * toolkit proxty server (depends on server type).</li>
 * <li>
 * <code>server.version</code> - server version ID (depends on server type).</li>
 * <li>
 * <code>server.drer.id</code> - DRER ID on test server.</li>
 * <li>
 * Additional properties may be required depending on the server type.</li>
 * </ul>
 * 
 * @author The OGSA-DAI Project Team.
 */
public class WriteToWebServerFileRESTTest extends TestCase {

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
     *            Test case name.
     * @throws Exception
     *             If any problems arise in reading the test properties.
     */
    public WriteToWebServerFileRESTTest(String name) throws Exception {
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
     * Create multiple files in the same sub-directory then test HTTP GET and
     * DELETE methods on each resource, the sub-directory then the request's
     * directory.
     * 
     * @throws Exception
     *             If any problems arise.
     */
    public void testREST() throws Exception {
        String[] str = new String[] { "The quick brown", "fox jumped over", "the lazy dog." };
        String subDir = "subDir";
        int numFiles = str.length;
        Echo[] echo = new Echo[numFiles];
        WriteToWebServerFile[] writeFile = new WriteToWebServerFile[numFiles];
        DeliverToRequestStatus[] deliver = new DeliverToRequestStatus[numFiles];
        PipelineWorkflow pipeline = new PipelineWorkflow();
        for (int i = 0; i < numFiles; i++) {
            echo[i] = new Echo();
            echo[i].addListBeginToInput();
            echo[i].addInput(new CharData(str[i].toCharArray()));
            echo[i].addListEndToInput();
            writeFile[i] = new WriteToWebServerFile();
            writeFile[i].connectDataInput(echo[i].getOutput());
            writeFile[i].addSubDirectory(subDir);
            deliver[i] = new DeliverToRequestStatus();
            deliver[i].connectInput(writeFile[i].getResultOutput());
            pipeline.add(echo[i]);
            pipeline.add(writeFile[i]);
            pipeline.add(deliver[i]);
        }
        mDRER.execute(pipeline, RequestExecutionType.SYNCHRONOUS);
        try {
            Thread.sleep(15 * 1000);
        } catch (InterruptedException e) {
        }
        URL[] url = new URL[numFiles];
        String[] fileNames = new String[numFiles];
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < numFiles; i++) {
            url[i] = writeFile[i].nextURL();
            System.out.println("URL: " + url[i]);
            String urlString = url[i].toString();
            fileNames[i] = urlString.substring(urlString.lastIndexOf("/") + 1, urlString.length());
            buffer.append(fileNames[i]);
            System.out.println("File: " + fileNames[i]);
        }
        String subDirURLStr = sliceBack(url[0].toString(), "/", 2);
        URL subDirURL = new URL(subDirURLStr);
        List<String> fileList = getContentList(subDirURL);
        for (int i = 0; i < fileList.size(); i++) {
            System.out.println("File via REST: " + fileList.get(i));
        }
        assertEquals("Number of files returned by sub-directory URL", numFiles, fileList.size());
        for (int i = 0; i < numFiles; i++) {
            assertTrue("File name " + fileNames[i] + " is in the sub-directory list", fileList.contains(fileNames[i]));
        }
        String requestDirURLStr = sliceBack(subDirURLStr, "/", 1);
        URL requestDirURL = new URL(requestDirURLStr);
        fileList = getContentList(requestDirURL);
        assertTrue("Sub-directory " + subDir + " is in the request directory list", fileList.contains(subDir));
        for (int i = 0; i < numFiles; i++) {
            assertEquals("File URL content at " + url[i], str[i], getContent(url[i]));
            testHTTP(url[i]);
        }
        testHTTP(subDirURL);
        testHTTP(requestDirURL);
    }

    /**
     * Given a string and a separator, slice off the last N
     * occurrences of the separator e.g. given separator <code>/</code>,
     * string <code>a/b/c/d/e</code> and N=2 the string returned is
     * <code>a/b/c</code>.

     * @param str
     * @param separator
     * @param count
     *     Number of separators to slice off.
     * @return trimmed string.
     */
    private String sliceBack(String str, String separator, int count) {
        String trimmed = str;
        for (int i = 0; i < count; i++) {
            int index = trimmed.lastIndexOf(separator);
            trimmed = trimmed.substring(0, index);
        }
        return trimmed;
    }

    /**
     * Get a list of the content at the given URL. Assumes that the
     * URL provides content of form <code>[...,...,...]</code>.
     * 
     * @param url
     * @return content.
     * @throws Exception
     *             If any problems arise.
     */
    private List<String> getContentList(URL url) throws Exception {
        String content = getContent(url);
        content = content.substring(1, content.length() - 1);
        String[] files = content.split(",");
        return Arrays.asList(files);
    }

    /**
     * Get the content at the given URL.
     * 
     * @param url
     * @return content.
     * @throws Exception
     *             If any problems arise.
     */
    private String getContent(URL url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuffer buffer = new StringBuffer();
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();
    }

    /**
     * Check that a URL responds to HTTP GET, then HTTP DELETE then redo
     * HTTP GET to validate it's no longer there.
     * 
     * @param url
     * @throws IOException
     *             If any problems arise.
     * @throws ProtocolException
     *             If any problems arise.
     */
    private void testHTTP(URL url) throws IOException, ProtocolException {
        int getResponse = httpGET(url);
        assertEquals("HTTP GET response pre-DELETE request-directory", HttpURLConnection.HTTP_OK, getResponse);
        httpDELETE(url);
        getResponse = httpGET(url);
        assertEquals("HTTP GET response post-DELETE request-directory", HttpURLConnection.HTTP_NOT_FOUND, getResponse);
    }

    /**
     * Send an HTTP DELETE to the given URL.
     * 
     * @param url
     * @return HTTP response code.
     * @throws IOException
     *             If any problems arise.
     * @throws ProtocolException
     *             If any problems arise.
     */
    private int httpDELETE(URL url) throws IOException, ProtocolException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestMethod("DELETE");
        connection.connect();
        int response = connection.getResponseCode();
        connection.disconnect();
        return response;
    }

    /**
     * Send an HTTP DELETE to the given URL.
     * 
     * @param url
     * @return HTTP response code.
     * @throws IOException
     *             If any problems arise.
     * @throws ProtocolException
     *             If any problems arise.
     */
    private int httpGET(URL url) throws IOException, ProtocolException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestMethod("GET");
        connection.connect();
        int response = connection.getResponseCode();
        connection.disconnect();
        return response;
    }
}
