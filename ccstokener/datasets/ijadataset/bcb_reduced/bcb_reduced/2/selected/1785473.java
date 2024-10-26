package org.xaware.server.engine.controller.transaction.jta;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import junit.framework.Assert;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.xaware.server.engine.controller.transaction.TransactionContext;
import org.xaware.testing.util.BaseDeployedServerTestCase;

/**
 * This class is a base class for J2EE Transaction unit tests where an existing
 * JTA transaction (CMT or UserTransaction) is inherited by the BizDoc processor.
 *
 * It directs requests to a different servlet which provides a J2EE transaction
 * to be inherited by the BizDoc processor.  It also provides for a post-execution
 * BizDoc to be called to check whehter the test BizDoc rolled back or committed.
 *
 * @author Tim Uttormark
 */
public abstract class BaseInheritedJTATransactionTestCase extends BaseDeployedServerTestCase {

    /**
     * Set to true when the JTA transaction should be doomed by having
     * setRollbackOnly() called on it prior to executing the BizDoc.
     */
    protected boolean doomXact = false;

    /**
     * Set to true when a Container Managed Transaction should be used when
     * executing the BizDoc.  When false, a new UserTransaction is used.
     */
    protected boolean useCMT = false;

    /**
     * Constructor
     * @param name the name of the unit test.
     */
    public BaseInheritedJTATransactionTestCase(String name) {
        super(name);
    }

    /**
     * Per test case setup.
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        doomXact = false;
        useCMT = false;
        try {
            TransactionSynchronizationManager.unbindResource(TransactionContext.getTransactionManager());
            fail("Prior test left MasterTransactionObject bound to thread.");
        } catch (Exception e) {
        }
    }

    /**
     * Per test case tear down.
     */
    @Override
    public void tearDown() throws Exception {
        try {
            TransactionSynchronizationManager.unbindResource(TransactionContext.getTransactionManager());
            fail("Test left MasterTransactionObject bound to thread.");
        } catch (Exception e) {
        } finally {
            super.tearDown();
        }
    }

    protected void setDoomXact(boolean doomXact) {
        this.doomXact = doomXact;
    }

    protected void setUseCMT(boolean useCMT) {
        this.useCMT = useCMT;
    }

    private String getBaseServletURL() {
        return this.getServerUrl() + "/xaware/XAUTServlet";
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
        HttpURLConnection connection = getHttpURLConnection(setUpBizDocFileName, false, false);
        getServletResponse(connection, null);
    }

    /**
     * Executes the test BizDoc, and compares its results to expected.
     */
    private void evaluateTestBizDoc() {
        HttpURLConnection connection = getHttpURLConnection(getBizDocFileName(), doomXact, useCMT);
        executeBizDocAndCompareResults(getExpectedOutputFileName(), connection, getInputXml());
    }

    /**
     * Executes the post BizDoc (if one is defined), and compares its
     * results to expected.
     */
    private void evaluatePostConditionBizDoc() {
        HttpURLConnection connection = getHttpURLConnection(postConditionBizDocFileName, false, false);
        executeBizDocAndCompareResults(postConditionExpectedOutputFileName, connection, null);
    }

    /**
     * Creates and returns a new HttpURLConnection object providing an open
     * connection to the server for executing the specified BizDoc.
     * @param bizDocToExecute the BizDoc to be executed
     * @param doom true if the JTA transaction should be doomed using
     * setRollbackOnly() prior to executing the BizDoc.
     * @param cmt true if Container Managed Transactions should be used
     * on the server.
     * @return the new HttpURLConnection object.
     */
    HttpURLConnection getHttpURLConnection(String bizDocToExecute, boolean doom, boolean cmt) {
        StringBuffer servletURL = new StringBuffer();
        servletURL.append(getBaseServletURL());
        servletURL.append("?_BIZVIEW=").append(bizDocToExecute);
        if (doom) {
            servletURL.append("&_DOOM=TRUE");
        }
        if (cmt) {
            servletURL.append("&_CMT=TRUE");
        }
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
}
