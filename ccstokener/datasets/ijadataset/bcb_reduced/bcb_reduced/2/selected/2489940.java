package at.ac.ait.enviro.dssos.sink;

import at.ac.ait.enviro.dssos.exceptions.SosClientException;
import at.ac.ait.enviro.dssos.exceptions.SosExceptionReport;
import at.ac.ait.enviro.dssos.source.SOSConnectorFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Ponweiser
 */
public class TransactionalSOSConnector100Test {

    public TransactionalSOSConnector100 con;

    public TransactionalSOSConnector100Test() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws SosClientException, MalformedURLException {
        System.setProperty("java.protocol.handler.pkgs", "at.ac.ait.protocols");
        final SOSConnectorFactory factory = SOSConnectorFactory.getInstance();
        con = (TransactionalSOSConnector100) factory.getNewConnector(new URL("jres://SOSResponses/52North/v1/CapabilitiesDocument.xml"));
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testRegisterSensorTemplate() throws SosClientException {
        final TemplateWriter request = con.createRequest("RegisterSensorRequest100.xml");
        assertNotNull("request is null.", request);
        final String[] varNames = { "SENSOR_ID", "SENSOR_STATUS", "SENSOR_IS_MOBILE", "EASTING", "NORTHING", "ALTITUDE", "INPUT_PHENOMENON_ID", "INPUT_PHENOMENON_NAME", "OUTPUT_PHENOMENON_ID", "OUTPUT_PHENOMENON_NAME", "OFFERING_ID", "OFFERING_NAME", "UNIT_OF_MEASUREMENT" };
        final Set<String> reqVars = request.getVariableNames();
        assertEquals("wrong variable count in template.", varNames.length, reqVars.size());
        for (String varName : varNames) {
            assertTrue(String.format("varable '%s' not found.", varName), reqVars.contains(varName));
        }
    }

    @Test
    public void testInsertMeasurementTemplate() throws SosClientException {
        final TemplateWriter request = con.createRequest("InsertMeasurementRequest100.xml");
        assertNotNull("request is null.", request);
        final String[] varNames = { "PROCEDURE_ID", "OBSERVED_PROP_ID", "SAMPLING_POINT_ID", "SAMPLING_POINT_NAME", "SRS_NAME", "FIRST_COORDINATE", "SECOND_COORDINATE", "TIMESTAMP", "MEASUREMENT_VALUE", "UNIT_OF_MEASUREMENT" };
        final Set<String> reqVars = request.getVariableNames();
        assertEquals("wrong variable count in template.", varNames.length, reqVars.size());
        for (String varName : varNames) {
            assertTrue(String.format("varable '%s' not found.", varName), reqVars.contains(varName));
        }
    }

    @Test
    public void testParseRegisterSensorResponse() throws IOException, SosClientException, SosExceptionReport {
        final URL url = new URL("jres://SOSResponses/52North/v1/RegisterSensorResponse.xml");
        final InputStream response = url.openStream();
        assertEquals("wrong sensor id.", "urn:ogc:object:feature:Sensor:IFGI:ifgi-sensor-1", con.receiveRegisterSensorResponse(response));
    }

    @Test
    public void testParseInsertMeasurementResponse() throws IOException, SosClientException, SosExceptionReport {
        final URL url = new URL("jres://SOSResponses/52North/v1/InsertObservationResponse.xml");
        final InputStream response = url.openStream();
        assertEquals("wrong observation id.", "o_1", con.receiveInsertMeasurementResponse(response));
    }

    @Test
    public void testParseExceptionReport() throws IOException, SosClientException {
        final URL url = new URL("jres://SOSResponses/52North/v1/ExceptionDocGetObs.xml");
        final InputStream response = url.openStream();
        try {
            con.receiveInsertMeasurementResponse(response);
            fail("exception report not thrown.");
        } catch (SosExceptionReport r) {
            assertTrue("wrong exception code.", r.getMessage().contains("InvalidParameterValue"));
        }
    }
}
