package org.xaware.testing.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import junit.framework.Assert;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format.TextMode;
import org.xaware.server.resources.ResourceHelper;
import org.xaware.shared.util.PublishResponse;
import org.xaware.shared.util.PublishUsingSOAP;
import org.xaware.shared.util.Zip;

/**
 * This class is a base class for unit tests which run BizDocs deployed
 * on a remote server via the HTTP connector.
 *
 * @author Tim Uttormark
 */
public abstract class BaseDeployedServerTestCase extends BaseBdpTestCase {

    protected static final XMLOutputter compactXmlOutputter = new XMLOutputter(Format.getCompactFormat());

    protected static final XMLOutputter prettyXmlOutputter;

    static {
        Format f = Format.getPrettyFormat();
        f.setTextMode(TextMode.TRIM_FULL_WHITE);
        prettyXmlOutputter = new XMLOutputter(f);
    }

    /** The name of the BizDoc executed at the start of the test to set initial conditions. */
    protected String setUpBizDocFileName = null;

    /** The name of the BizDoc executed at the end of the test to fetch the resulting state. */
    protected String postConditionBizDocFileName = null;

    /** The name of the file containing the expected results from executing the postConditionBizDoc. */
    protected String postConditionExpectedOutputFileName = null;

    /**
     * Constructor.
     * @param name The name of the unit test.
     */
    public BaseDeployedServerTestCase(String name) {
        super(name);
    }

    protected abstract String getServerUrl();

    public abstract void testCreateXarAndDeploy() throws Exception;

    /**
     * Per-test setup.
     *
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        setUpBizDocFileName = null;
        postConditionBizDocFileName = null;
        postConditionExpectedOutputFileName = null;
    }

    /**
     * Gets the URL of the servlet to be called to execute BizDocs,
     * exclusive of any parameter names and values.
     * @return the base servlet URL as a String.
     */
    private String getBaseServletURL() {
        return this.getServerUrl() + "/xaware/XAServlet";
    }

    protected void setSetUpBizDocFileName(String setUpBizDocFileName) {
        this.setUpBizDocFileName = setUpBizDocFileName;
    }

    protected void setPostConditionBizDocFileName(String postConditionBizDocFileName) {
        this.postConditionBizDocFileName = postConditionBizDocFileName;
    }

    protected void setPostConditionExpectedOutputFileName(String postConditionExpectedOutputFileName) {
        this.postConditionExpectedOutputFileName = postConditionExpectedOutputFileName;
    }

    /**
     * Setter for the name of the current test method, for logging purposes.
     * @param methodName
     */
    protected void setTestMethodName(String methodName) {
        testHelper.setTestMethodName(methodName);
    }

    /**
     * Executes the BizDocs for the current test, and evaluates the results.
     */
    @Override
    protected void evaluateBizDoc() {
        executeSetUpBizDoc();
        evaluateTestBizDoc();
        evaluatePostConditionBizDoc();
    }

    /**
     * Executes the set-up BizDoc, if one is defined.
     * Its results are NOT compared to expected.
     */
    private void executeSetUpBizDoc() {
        if (setUpBizDocFileName == null) return;
        HttpURLConnection connection = getHttpURLConnection(setUpBizDocFileName);
        getServletResponse(connection, null);
    }

    /**
     * Executes the test BizDoc, and compares its results to expected.
     */
    private void evaluateTestBizDoc() {
        HttpURLConnection connection = getHttpURLConnection(getBizDocFileName());
        executeBizDocAndCompareResults(getExpectedOutputFileName(), connection, getInputXml());
    }

    /**
     * Executes the post BizDoc (if one is defined), and compares its
     * results to expected.
     */
    private void evaluatePostConditionBizDoc() {
        if (postConditionBizDocFileName == null) return;
        HttpURLConnection connection = getHttpURLConnection(postConditionBizDocFileName);
        executeBizDocAndCompareResults(postConditionExpectedOutputFileName, connection, null);
    }

    /**
     * Creates and returns a new HttpURLConnection object providing an open
     * connection to the server for executing the specified BizDoc.
     * @param bizDocToExecute the BizDoc to be executed
     * @return the new HttpURLConnection object.
     */
    private HttpURLConnection getHttpURLConnection(String bizDocToExecute) {
        StringBuffer servletURL = new StringBuffer();
        servletURL.append(getBaseServletURL());
        servletURL.append("?_BIZVIEW=").append(bizDocToExecute);
        Map<String, Object> inputParms = getInputParams();
        if (inputParms != null) {
            Set<Entry<String, Object>> entrySet = inputParms.entrySet();
            for (Entry<String, Object> entry : entrySet) {
                String name = entry.getKey();
                String value = entry.getValue().toString();
                servletURL.append("&").append(name).append("=").append(value);
            }
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(servletURL.toString());
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            Assert.fail("Failed to connect to the test servlet: " + e);
        }
        return connection;
    }

    /**
     * Gets the input XML as a string, by parsing the inputXmlFileName
     * if one is specified.
     * @return the contents of the input XML file as a String, or null
     * if there is no input XML file. 
     */
    protected String getInputXml() {
        if ((getInputXmlFileName() == null) || (getInputXmlFileName().trim().length() == 0)) {
            return null;
        }
        String fileName = getDataFolder() + getInputXmlFileName();
        Document inputXmlDoc = null;
        try {
            inputXmlDoc = ResourceHelper.loadXmlResourceIntoDocument(fileName);
        } catch (final IOException ioe) {
            final String errMsg = "IOException loading XML file:" + ioe.getMessage() + "  File name:" + fileName;
            ioe.printStackTrace(System.out);
            Assert.fail(errMsg);
        } catch (final JDOMException e) {
            final String errMsg = "JDOMException parsing XML file:" + e.getMessage() + "  File name:" + fileName;
            e.printStackTrace(System.out);
            Assert.fail(errMsg);
        }
        return compactXmlOutputter.outputString(inputXmlDoc);
    }

    /**
     * Uses the connection provided to execute the BizDoc, and returns
     * the result parsed as a JDOM Document. 
     * @param connection the HttpURLConnection, configured to execute a
     * specific BizDoc.
     * @param inputXml the input XML to be provided to the BizDoc
     * @return a JDOM Document containing the HTTP connector servlet's
     * response.
     */
    protected Document getServletResponse(HttpURLConnection connection, String inputXml) {
        connection.setDoInput(true);
        if (inputXml != null && inputXml.length() > 0) {
            try {
                connection.setRequestMethod("POST");
                if (connection.getDoOutput() == false) {
                    connection.setDoOutput(true);
                }
                OutputStream outStream = null;
                try {
                    outStream = connection.getOutputStream();
                    outStream.write(inputXml.getBytes());
                } finally {
                    if (outStream != null) {
                        try {
                            outStream.close();
                        } catch (IOException e) {
                        }
                    }
                }
            } catch (Exception e) {
                Assert.fail("POST to servlet failed: " + e);
            }
        } else {
            try {
                connection.setRequestMethod("GET");
            } catch (Exception e) {
                Assert.fail("GET from servlet failed: " + e);
            }
        }
        BufferedReader in = null;
        Document doc = null;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            SAXBuilder builder = new SAXBuilder();
            doc = builder.build(in);
        } catch (JDOMException e) {
            Assert.fail("Failed to parse the execution results into XML: " + e);
        } catch (IOException e) {
            Assert.fail("Failed to read servlet response: " + e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        String result = prettyXmlOutputter.outputString(doc);
        System.out.println(result);
        return doc;
    }

    /**
     * Uses the connection provided to execute the BizDoc, then compares
     * the actual and expected results.
     * @param expResults the expected results XML fileName
     * @param connection the HttpURLConnection, configured to execute a
     * specific BizDoc.
     * @param inputXml the input XML to be provided to the BizDoc
     */
    protected void executeBizDocAndCompareResults(String expResults, HttpURLConnection connection, String inputXml) {
        Document actualResults = getServletResponse(connection, inputXml);
        testHelper.evaluateExecutionResults(expResults, getBizDocFileName(), isSaveOutput(), isTransformOutputToExpected(), actualResults, null);
    }

    /**
     * Builds a xar file from the data directory and deploys it
     * to each server being tested.
     */
    protected void buildAndDeployXar() {
        File xarFile = buildXar();
        deployXarToServer(xarFile);
    }

    /**
     * Builds a xar file from the data directory and returns it.
     * @return a File referencing a newly built XAR containing the
     * contents of the data folder.
     */
    private File buildXar() {
        File dataDir = new File(getDataFolder());
        String xarName = "unittest.xar";
        File xarFile = new File(dataDir, xarName);
        xarFile.delete();
        String xarFileName = null;
        try {
            xarFileName = Zip.buildXarFromDirectory(getDataFolder(), xarName);
        } catch (Exception e) {
            Assert.fail("Failed to create XAR file: " + e);
        }
        File resultingXarFile = new File(xarFileName);
        if (!resultingXarFile.exists()) {
            Assert.fail("XAR file does not exist.");
        }
        return resultingXarFile;
    }

    /**
     * Deploys the XAR file provided to the server.
     * @param xarFile the XAR file to be deployed.
     */
    private void deployXarToServer(File xarFile) {
        Hashtable<String, String> fileAttrs = new Hashtable<String, String>();
        String xarName = xarFile.getName();
        fileAttrs.put(PublishUsingSOAP.propAttrs[PublishUsingSOAP.OVERWRITE], "yes");
        fileAttrs.put(PublishUsingSOAP.propAttrs[PublishUsingSOAP.ALIAS], xarName);
        fileAttrs.put(PublishUsingSOAP.propAttrs[PublishUsingSOAP.TYPE], PublishUsingSOAP.XAR_FILE);
        fileAttrs.put(PublishUsingSOAP.propAttrs[PublishUsingSOAP.CHECKIN], "false");
        fileAttrs.put(PublishUsingSOAP.propAttrs[PublishUsingSOAP.SOAPACTION], "");
        fileAttrs.put(PublishUsingSOAP.propAttrs[PublishUsingSOAP.DESCRIPTION], "XAR File");
        fileAttrs.put(PublishUsingSOAP.propAttrs[PublishUsingSOAP.LOCATION], xarName);
        fileAttrs.put(PublishUsingSOAP.propAttrs[PublishUsingSOAP.LOC_LOCATION], xarFile.getAbsolutePath());
        Vector deployFiles = new Vector();
        deployFiles.add(fileAttrs);
        String serverUrl = this.getServerUrl();
        try {
            PublishUsingSOAP soap = new PublishUsingSOAP(serverUrl, deployFiles, null, null);
            Element reply = soap.publish();
            PublishResponse pr = new PublishResponse(reply);
            String dtlMsg = pr.getMessage();
            String summaryMsg = pr.getStatus();
            Assert.assertTrue("XAR failed to deploy on " + serverUrl + ": " + summaryMsg + " " + dtlMsg, "Documents deployed successfully".equals(summaryMsg));
        } catch (Exception e) {
            Assert.fail("Failed to deploy xar to " + serverUrl + ": " + e);
        }
    }
}
