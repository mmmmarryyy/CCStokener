package zing.config;

import zing.vo.TestCaseConfig;
import zing.vo.PerfParameters;
import zing.config.xmlobjects.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.net.URL;

/**
 * Class for reading the test case configuration from config xml file. It uses
 * the singleton pattern and reads the xml once. It creates arraylist of
 * TestCaseConfig objects for each test case configures in the XML and stores
 * it in a map against classname.			  
 */
public class ConfigReader {

    private static final String TESTCONFIGXML_PATH = "TestConfig.xml";

    /**
     * Denotes that the expected output and actual output be compared using their equals method.
     */
    public static final String ASSERTION_EQUALITY = "EQUALITY";

    /**
     * Denotes that the expected output and actual output be compared only for the attributes
     * mentioned in the xml for output-data. Comes handy when an object is huge and one has to
     * assert only a part of it.  
     */
    public static final String ASSERTION_INSPECTION = "INSPECTION";

    public static final String EXCEPTION_VALUE_PREFIX = "VALUE:";

    /**
     * Name of the class used for pre and post process operations.
     */
    public static final String PROCESS_TEST = "zing.tests.ProcessTest";

    /**
     * Instance of itself to implement singleton pattern.
     */
    private static ConfigReader configReader;

    /**
     * Stores the test case configurations of all the classes against the
     * classnames.
     */
    private HashMap testMap;

    /**
     * Represents the pre process scenario name
     */
    private static final String PRETEST = "PRETEST";

    /**
     * Represents the post process scenario name
     */
    private static final String POSTTEST = "POSTTEST";

    /**
     * TestCaseConfig representing the pre test process
     */
    private TestCaseConfig pretest = null;

    /**
     * TestCaseConfig representing the post test process
     */
    private TestCaseConfig posttest = null;

    /**
     * Name of the method(from the class ProcessTest) used for pre and
     * post operations
     */
    private static final String PROCESS_TEST_DEFAULTMETHOD = "testProcess";

    /**
     * Constructor for default initialization of the reader. The tests are read
     * from the configuration xml and stored in a hashmap.
     *
     * @throws Exception In case of error.
     */
    protected ConfigReader() throws Exception {
        loadTests(TESTCONFIGXML_PATH);
    }

    /**
     * Reads configuration for the specified class and returns all the possible
     * test	case configurations present in the xml.
     *
     * @param strClassName Denotes the class name for the test cases to be
     *        created.
     * @return List of test configurations.
     * @throws Exception In case of error.
     */
    public TestCaseConfig[] getTests(String strClassName) throws Exception {
        ArrayList list = new ArrayList();
        if (pretest != null) {
            list.add(pretest);
        }
        if (null != testMap.get(strClassName)) {
            list.addAll((ArrayList) testMap.get(strClassName));
        } else {
            throw new Exception("TestCase with name '" + strClassName + "' not configured. Please check name of the test-case in TestCaseConfiguration.xml");
        }
        if (posttest != null) {
            list.add(posttest);
        }
        return (TestCaseConfig[]) (list).toArray(new TestCaseConfig[0]);
    }

    /**
     * Reads configuration for all the classes configured in the xml.
     *
     * @return List of test configurations.
     * @throws Exception In case of error.
     */
    public TestCaseConfig[] getAllTests() throws Exception {
        Iterator iter = testMap.values().iterator();
        ArrayList arrlTests = new ArrayList();
        if (pretest != null) {
            arrlTests.add(pretest);
        }
        while (iter.hasNext()) {
            arrlTests.addAll((ArrayList) iter.next());
        }
        if (posttest != null) {
            arrlTests.add(posttest);
        }
        return (TestCaseConfig[]) arrlTests.toArray(new TestCaseConfig[0]);
    }

    /**
     * Returns the instance of config reader itself.
     *
     * @return Instance of config reader.
     * @throws Exception In case of error.
     */
    public static synchronized ConfigReader getInstance() throws Exception {
        if (null == configReader) {
            configReader = new ConfigReader();
        }
        return configReader;
    }

    /**
     * Loads all the test case configurations from the config xml.
     *
     * @param strConfigXmlPath Denotes the xml path to read the configuration
     *        from.
     * @throws Exception In case of error.
     */
    private void loadTests(String strConfigXmlPath) throws Exception {
        InputStreamReader isr;
        try {
            isr = new FileReader(strConfigXmlPath);
        } catch (FileNotFoundException e) {
            URL url = this.getClass().getClassLoader().getResource(strConfigXmlPath);
            if (null != url) {
                try {
                    isr = new InputStreamReader(url.openStream());
                } catch (Exception e1) {
                    throw new Exception("Unable to find TestCaseConfiguration.xml");
                }
            } else {
                throw new Exception("Could not load test case configuration.");
            }
        }
        TestSuite testsuite = TestSuite.unmarshal(isr);
        isr.close();
        initConnection(testsuite.getTestConnection());
        TestCase testCase[] = testsuite.getTestCase();
        TestMethod testMethod[];
        TestScenario testScenario[];
        TestType testType[];
        ArrayList arrlTestCases;
        TestCaseConfig TestCaseConfig;
        testMap = new HashMap();
        for (int tc = 0; tc < testCase.length; tc++) {
            arrlTestCases = new ArrayList();
            testMethod = testCase[tc].getTestMethod();
            for (int tm = 0; tm < testMethod.length; tm++) {
                testScenario = testMethod[tm].getTestScenario();
                for (int ts = 0; ts < testScenario.length; ts++) {
                    if (testScenario[ts].getTestTypeCount() > 0) {
                        testType = testScenario[ts].getTestType();
                    } else if (testMethod[tm].getTestTypeCount() > 0) {
                        testType = testMethod[tm].getTestType();
                    } else {
                        TestCaseConfig = createTestConfig(testCase[tc], testMethod[tm], testScenario[ts]);
                        arrlTestCases.add(TestCaseConfig);
                        continue;
                    }
                    for (int tt = 0; tt < testType.length; tt++) {
                        TestCaseConfig = createTestConfig(testCase[tc], testMethod[tm], testScenario[ts]);
                        TestCaseConfig.setTestType(testType[tt].getTesttypeid());
                        if (null != testType[tt].getPerfparams()) {
                            TestCaseConfig.setPerfParams(getPerfParams(testType[tt]));
                        }
                        arrlTestCases.add(TestCaseConfig);
                    }
                }
            }
            testMap.put(testCase[tc].getName(), arrlTestCases);
        }
        if (null != testsuite.getPreTest() && null != testsuite.getPreTest().getTestData()) {
            pretest = new TestCaseConfig();
            createTest(pretest, PRETEST, testsuite.getPreTest().getTestData());
        }
        if (null != testsuite.getPostTest() && null != testsuite.getPostTest().getTestData()) {
            posttest = new TestCaseConfig();
            createTest(posttest, POSTTEST, testsuite.getPostTest().getTestData());
        }
    }

    private void initConnection(TestConnection connection) throws Exception {
        try {
            if (null != connection) DatabaseHelper.createConnection(connection.getDriver(), connection.getUrl(), connection.getUsername(), connection.getPassword());
        } catch (Exception e) {
            throw new Exception("Could not create connection.", e);
        }
    }

    /**
     * Method sets the required values of TestCaseConfig.
     *
     * @param test representing the TestCaseConfig object.
     * @param scenarioName scenario type
     * @param testData to be refreshed/deleted
     */
    private void createTest(TestCaseConfig test, String scenarioName, TestData[] testData) {
        test.setClassName(PROCESS_TEST);
        test.setMethodName(PROCESS_TEST_DEFAULTMETHOD);
        test.setScenario(scenarioName);
        HashMap datasets = new HashMap();
        getDataSets(testData, datasets);
        test.setDataSets(datasets);
    }

    /**
     * Creates test case configuration.
     * @param testCase The test case.
     * @param testMethod The test method.
     * @param testScenario The test scenario.
     * @return Test case configuration.
     */
    private TestCaseConfig createTestConfig(TestCase testCase, TestMethod testMethod, TestScenario testScenario) throws Exception {
        TestCaseConfig TestCaseConfig = new TestCaseConfig();
        TestCaseConfig.setClassName(testCase.getName());
        TestCaseConfig.setMethodName(testMethod.getName());
        TestCaseConfig.setScenario(testScenario.getId());
        TestCaseConfig.setInputData(getInputData(testScenario));
        TestCaseConfig.setOutputData(getOutputData(testScenario));
        TestCaseConfig.setExceptions(getExceptions(testScenario));
        HashMap hmpDataSets = new HashMap();
        getDataSets(testMethod.getTestData(), hmpDataSets);
        getDataSets(testScenario.getTestData(), hmpDataSets);
        TestCaseConfig.setDataSets(hmpDataSets);
        return TestCaseConfig;
    }

    /**
     * Creates a list of datasets for a scenario. The list is stored in the
     * hashmap against connection as the key.
     *
     * @param testData Denotes list of data sets configured for the test case.
     * @param hmpDataSets Hashmap of datasets.
     */
    private void getDataSets(TestData[] testData, HashMap hmpDataSets) {
        if (null != testData && testData.length > 0) {
            for (int i = 0; i < testData.length; i++) {
                String operation = testData[i].getOperation();
                if (null == operation || !DatabaseHelper.DELETE.equalsIgnoreCase(operation)) {
                    operation = DatabaseHelper.DEFAULT;
                }
                ArrayList arrlDataSets = null;
                String strConnection = testData[i].getConnection();
                String strDataSet = operation + ":" + testData[i].getDataset();
                if (null != hmpDataSets.get(strConnection)) {
                    arrlDataSets = (ArrayList) hmpDataSets.get(strConnection);
                    arrlDataSets.add(strDataSet);
                } else {
                    arrlDataSets = new ArrayList();
                    arrlDataSets.add(strDataSet);
                    hmpDataSets.put(strConnection, arrlDataSets);
                }
            }
        }
    }

    /**
     * Creates the hashmap of input parameters configured for a scenario.
     *
     * @param ts Denotes the configuration of a test scenario.
     * @return HashMap of input parameters.
     */
    private HashMap getInputData(TestScenario ts) {
        HashMap hmpInputData = null;
        if (ts.getInputDataCount() > 0) {
            hmpInputData = new HashMap();
            InputData inputData[] = ts.getInputData();
            for (int i = 0; i < inputData.length; i++) {
                hmpInputData.put(inputData[i].getName(), inputData[i].getValue());
            }
        }
        return hmpInputData;
    }

    /**
     * Creates the hashmap of output parameters configured for a scenario.
     *
     * @param ts Denotes the configuration of a test scenario.
     * @return HashMap of output parameters.
     */
    private HashMap getOutputData(TestScenario ts) throws Exception {
        HashMap hmpOutputData = null;
        if (ts.getOutputDataCount() > 0) {
            hmpOutputData = new HashMap();
            OutputData outputData[] = ts.getOutputData();
            String assertionType;
            for (int i = 0; i < outputData.length; i++) {
                assertionType = outputData[i].getAssertionType();
                if (null == assertionType) {
                    assertionType = ASSERTION_EQUALITY;
                }
                if (!(assertionType.equals(ASSERTION_EQUALITY) || assertionType.equals(ASSERTION_INSPECTION))) {
                    throw new Exception("Unknown assertion-type " + assertionType + ".\n It can be one of " + ASSERTION_EQUALITY + " or " + ASSERTION_INSPECTION + ".");
                }
                hmpOutputData.put(outputData[i].getName() + ":" + assertionType, outputData[i].getValue());
            }
        }
        return hmpOutputData;
    }

    /**
     * Creates the hashmap of exceptions configured for a scenario.
     *
     * @param ts Denotes the configuration of a test scenario.
     * @return HashMap of exceptions.
     */
    private HashMap getExceptions(TestScenario ts) {
        HashMap hmpExceptions = null;
        String strType;
        String strErrorCode;
        String strValue;
        if (ts.getExceptionDataCount() > 0) {
            hmpExceptions = new HashMap();
            ExceptionData exceptionData[] = ts.getExceptionData();
            for (int i = 0; i < exceptionData.length; i++) {
                strType = exceptionData[i].getType();
                strErrorCode = exceptionData[i].getErrorCode();
                strValue = exceptionData[i].getValue();
                hmpExceptions.put(exceptionData[i].getName(), null == strValue ? (null == strErrorCode ? strType : strErrorCode) : EXCEPTION_VALUE_PREFIX + strValue);
            }
        }
        return hmpExceptions;
    }

    /**
     * Creates PerfParameters from xml object TestType.
     *
     * @param tt Denotes the configuration of a test type.
     * @return Performance parameters.
     */
    private PerfParameters getPerfParams(TestType tt) {
        Perfparams perfparams = tt.getPerfparams();
        PerfParameters perfParameters = new PerfParameters();
        perfParameters.setMaxElapsedTime(perfparams.getMaxelapsedtime());
        perfParameters.setMaxIterations(perfparams.getMaxiterations());
        perfParameters.setMaxUsers(perfparams.getMaxusers());
        perfParameters.setResponseTime(perfparams.getResponsetime());
        perfParameters.setThroughputTime(perfparams.getThroughputtime());
        return perfParameters;
    }
}
