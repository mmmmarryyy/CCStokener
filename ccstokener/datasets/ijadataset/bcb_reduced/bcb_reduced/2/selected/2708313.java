package at.ac.ait.enviro.dssos.connector;

import at.ac.ait.enviro.dssos.exceptions.SosClientException;
import at.ac.ait.enviro.dssos.exceptions.SosExceptionReport;
import at.ac.ait.enviro.tsapi.util.text.TemplateWriter;
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
import static at.ac.ait.enviro.dssos.connector.AbstractSOSConnector.ReqVariable;

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
        System.setProperty("java.protocol.handler.pkgs", "at.ac.ait.protocols");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws SosClientException, MalformedURLException {
        con = new TransactionalSOSConnector100(new URL("jres://SOSResponses/52North/v1/CapabilitiesDocument.xml"));
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testRegisterSensorTemplate() throws SosClientException {
        final TemplateWriter request = con.createRequest(con.getClass(), "RegisterSensorRequest100.xml");
        assertNotNull("request is null.", request);
        final ReqVariable[] vars = { ReqVariable.SENSOR_ID, ReqVariable.SENSOR_STATUS, ReqVariable.SENSOR_IS_MOBILE, ReqVariable.EASTING, ReqVariable.NORTHING, ReqVariable.ALTITUDE, ReqVariable.INPUT_LIST, ReqVariable.OUTPUT_LIST, ReqVariable.UNIT_OF_MEASUREMENT };
        final Set<String> reqVars = request.getVariableNames();
        assertEquals("wrong variable count in template.", vars.length, reqVars.size());
        for (ReqVariable var : vars) {
            assertTrue(String.format("varable '%s' not found.", var.name()), reqVars.contains(var.name()));
        }
    }

    @Test
    public void testInsertMeasurementTemplate() throws SosClientException {
        final TemplateWriter request = con.createRequest(con.getClass(), "InsertMeasurementRequest100.xml");
        assertNotNull("request is null.", request);
        final ReqVariable[] vars = { ReqVariable.PROCEDURE_ID, ReqVariable.OBS_PROP_ID, ReqVariable.SAMPLING_POINT_ID, ReqVariable.SAMPLING_POINT_NAME, ReqVariable.SRS_NAME, ReqVariable.FIRST_COORDINATE, ReqVariable.SECOND_COORDINATE, ReqVariable.TIMESTAMP, ReqVariable.MEASUREMENT_VALUE, ReqVariable.UNIT_OF_MEASUREMENT };
        final Set<String> reqVars = request.getVariableNames();
        assertEquals("wrong variable count in template.", vars.length, reqVars.size());
        for (ReqVariable var : vars) {
            assertTrue(String.format("varable '%s' not found.", var.name()), reqVars.contains(var.name()));
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
