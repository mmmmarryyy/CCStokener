package engine.distribution.slave;

import java.io.IOException;
import java.net.HttpURLConnection;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.testng.annotations.Test;
import engine.distribution.master.servlets.DistributionServlet;
import engine.distribution.utils.WevoURL;

/**
 * Tests for SlaveRegistrator.
 *
 * @author Karol Stosiek (karol.stosiek@gmail.com)
 * @author Michal Anglart (anglart.michal@gmail.com)
 */
public class SlaveRegistratorTest {

    /** Tested instance. */
    private SlaveRegistrator registrator;

    /** Mocked URL. */
    private WevoURL url;

    /** Mocked url connection. */
    private HttpURLConnection connection;

    /** Mock control. */
    private IMocksControl mockControl = EasyMock.createControl();

    /**
   * Tests succesful registration behavior.
   * @throws IOException Never thrown.
   * @throws ClassNotFoundException Never thrown.
   */
    @Test
    public void testSuccessfulRegistration() throws IOException, ClassNotFoundException {
        url = mockControl.createMock(WevoURL.class);
        connection = mockControl.createMock(HttpURLConnection.class);
        EasyMock.expect(url.openConnection()).andReturn(connection);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setRequestProperty(DistributionServlet.SLAVE_ID_PROPERTY, "slaveName");
        connection.connect();
        EasyMock.expect(connection.getResponseMessage()).andReturn("ignored");
        EasyMock.expect(connection.getHeaderField(DistributionServlet.SLAVE_ID_PROPERTY)).andReturn("SlaveID");
        connection.disconnect();
        registrator = new SlaveRegistrator(url);
        mockControl.replay();
        registrator.register("slaveName");
        mockControl.verify();
    }
}
