package engine.distribution.slave;

import java.util.List;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import junit.framework.Assert;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import engine.Population;
import engine.PopulationEvaluatorTest.DummyIndividual;
import engine.distribution.master.servlets.DistributionServlet;
import engine.distribution.serialization.EvaluationResult;
import engine.distribution.serialization.EvaluationTask;
import engine.distribution.serialization.ResultSerializer;
import engine.distribution.serialization.TaskSerializer;
import engine.distribution.utils.WevoURL;

/**
 * Tests for {@link TaskExchanger}.
 *
 * @author Karol Stosiek (karol.stosiek@gmail.com)
 * @author Michal Anglart (anglart.michal@gmail.com)
 */
@Test(sequential = true)
public class TaskExchangerTest {

    /** ID of the slave. */
    private final String slaveId = "slaveID";

    /** Exchange URL mock. */
    private WevoURL urlMock;

    /** Mock of the server connection. */
    private HttpURLConnection connectionMock;

    /** Task serializer mock. */
    private TaskSerializer<DummyIndividual> taskSerializerMock;

    /** Task serializer mock. */
    private ResultSerializer<DummyIndividual> resultSerializerMock;

    /** Mock control. */
    private IMocksControl mockControl = EasyMock.createNiceControl();

    /** Tested instance. */
    private TaskExchanger<DummyIndividual> exchanger;

    /** Population object sent from server. */
    private final Population<DummyIndividual> transferredPopulation = new Population<DummyIndividual>() {

        /** Default serial UID to silence the compiler. */
        private static final long serialVersionUID = 1L;

        {
            addIndividual(new DummyIndividual());
            addIndividual(new DummyIndividual());
            addIndividual(new DummyIndividual());
            addIndividual(new DummyIndividual());
            addIndividual(new DummyIndividual());
        }
    };

    /** Evaluation task sent from the server. */
    private final EvaluationTask<DummyIndividual> transferredTask = new EvaluationTask<DummyIndividual>(transferredPopulation);

    /** Evaluation result returned to the server. */
    private final EvaluationResult<DummyIndividual> returnedResult = new EvaluationResult<DummyIndividual>(new HashMap<DummyIndividual, List<Double>>());

    /**
   * Tests retrieving population from master. 
   * @throws ClassNotFoundException Never thrown.
   * @throws IOException Never thrown.
   */
    @Test
    public void testGettingTaskAtOnce() throws IOException, ClassNotFoundException {
        setUpCommonGettingBehavior();
        EasyMock.expect(connectionMock.getResponseCode()).andReturn(HttpURLConnection.HTTP_OK);
        EasyMock.expect(connectionMock.getInputStream()).andReturn(null);
        EasyMock.expect(taskSerializerMock.deserialize(null)).andReturn(transferredTask);
        mockControl.replay();
        EvaluationTask<DummyIndividual> actualTask = exchanger.getTask(slaveId, 0);
        mockControl.verify();
        Assert.assertEquals(transferredTask, actualTask);
    }

    /**
   * Tests sending population to master with a couple
   * of rejects from server.
   * @throws IOException Never thrown.
   * @throws ClassNotFoundException Never thrown.
   */
    @Test
    public void testGettingPopulationWithFewRejects() throws IOException, ClassNotFoundException {
        setUpCommonGettingBehavior();
        EasyMock.expect(connectionMock.getResponseCode()).andReturn(HttpURLConnection.HTTP_NO_CONTENT).times(5);
        EasyMock.expect(connectionMock.getResponseCode()).andReturn(HttpURLConnection.HTTP_OK);
        EasyMock.expect(connectionMock.getInputStream()).andReturn(null);
        EasyMock.expect(taskSerializerMock.deserialize(null)).andReturn(transferredTask);
        mockControl.replay();
        EvaluationTask<DummyIndividual> actualTask = exchanger.getTask(slaveId, 0);
        mockControl.verify();
        Assert.assertEquals(transferredTask, actualTask);
    }

    /**
   * Tests sending population to master with a fatal error.
   * @throws IOException Never thrown.
   * @throws ClassNotFoundException Never thrown.
   */
    @Test(expectedExceptions = { ConnectException.class })
    public void testGettingPopulationWithoutSuccess() throws IOException, ClassNotFoundException {
        setUpCommonGettingBehavior();
        EasyMock.expect(connectionMock.getResponseCode()).andReturn(HttpURLConnection.HTTP_UNAVAILABLE).anyTimes();
        EasyMock.expect(connectionMock.getResponseMessage()).andReturn("").anyTimes();
        mockControl.replay();
        exchanger.getTask(slaveId, 0);
        mockControl.verify();
    }

    /**
   * Sets up mock behavior common for all tests related to getting population.
   * @throws IOException Never thrown.
   */
    private void setUpCommonGettingBehavior() throws IOException {
        EasyMock.expect(urlMock.openConnection()).andStubReturn(connectionMock);
        connectionMock.setDoInput(true);
        connectionMock.setUseCaches(false);
        connectionMock.setRequestMethod("GET");
        connectionMock.setRequestProperty(DistributionServlet.SLAVE_ID_PROPERTY, slaveId);
        connectionMock.connect();
        connectionMock.disconnect();
    }

    /**
   * Tests population sending behavior. 
   * @throws IOException Never thrown.
   */
    @Test
    public void testSendingPopulation() throws IOException {
        mockControl.reset();
        EasyMock.expect(urlMock.openConnection()).andStubReturn(connectionMock);
        connectionMock.setDoOutput(true);
        connectionMock.setUseCaches(false);
        connectionMock.setRequestMethod("POST");
        connectionMock.setRequestProperty("Content-Type", "application/octet-stream");
        connectionMock.setRequestProperty(DistributionServlet.SLAVE_ID_PROPERTY, slaveId);
        connectionMock.connect();
        EasyMock.expect(connectionMock.getOutputStream()).andReturn(null);
        resultSerializerMock.serialize(null, returnedResult);
        EasyMock.expect(connectionMock.getResponseMessage()).andReturn("");
        connectionMock.disconnect();
        mockControl.replay();
        exchanger.sendResult(returnedResult, slaveId);
        mockControl.verify();
    }

    /** Sets up testing environment. */
    @SuppressWarnings({ "unchecked", "unused" })
    @BeforeMethod
    private void setUp() {
        mockControl.reset();
        urlMock = mockControl.createMock(WevoURL.class);
        connectionMock = mockControl.createMock(HttpURLConnection.class);
        taskSerializerMock = mockControl.createMock(TaskSerializer.class);
        resultSerializerMock = mockControl.createMock(ResultSerializer.class);
        exchanger = new TaskExchanger<DummyIndividual>(taskSerializerMock, resultSerializerMock, urlMock);
    }
}
