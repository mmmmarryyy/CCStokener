package org.javacraft.qa.service;

import java.io.IOException;
import org.junit.Ignore;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.NullLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jan
 */
public class LocationsLearningTest {

    private static final String LOCATION_URL_STRING = "http://frontend.myroute.be/Locations.asmx";

    private static URL LOCATION_URL;

    private static Template locationsListTemplate;

    private static final String TRAVEL_URL_STRING = "http://trafficinfoservice.be-mobile.be/ContentService.asmx";

    private static URL TRAVEL_URL;

    private static Template travelTimesTemplate;

    @BeforeClass
    public static void setUpClass() throws Exception {
        LOCATION_URL = new URL(LOCATION_URL_STRING);
        TRAVEL_URL = new URL(TRAVEL_URL_STRING);
        Velocity.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, new NullLogChute());
        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        Velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        Velocity.init();
        locationsListTemplate = Velocity.getTemplate("/LocationsList.vm");
        travelTimesTemplate = Velocity.getTemplate("/TravelTimes.vm");
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Ignore
    @Test
    public void testGetHouthalenLocations() throws Exception {
        final Map variables = new HashMap();
        variables.put("prefix", "houthalen");
        variables.put("language", "nl");
        final StringWriter writer = new StringWriter();
        final VelocityContext context = new VelocityContext(variables);
        locationsListTemplate.merge(context, writer);
        final String request = writer.toString();
        final URLConnection urlConnection = LOCATION_URL.openConnection();
        urlConnection.setUseCaches(false);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("accept-charset", "UTF-8");
        urlConnection.setRequestProperty("content-type", "text/xml; charset=utf-8");
        urlConnection.setRequestProperty("Content-Length", "" + request.length());
        urlConnection.setRequestProperty("SOAPAction", "http://myRoute.be/GetLocationList");
        OutputStreamWriter outputWriter = new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8");
        outputWriter.write(request);
        outputWriter.flush();
        final InputStream result = urlConnection.getInputStream();
        final String response = IOUtils.toString(result);
        System.out.println(response);
        assertTrue(response.contains("<GeoId>"));
    }

    @Test
    public void testGetTravelTime() throws Exception {
        final String houthalenDisplayId = "3065";
        final String houthalenGeoId = "996";
        final String antwerpenDisplayId = "1882";
        final String antwerpenGeoId = "438";
        final Map variables = new HashMap();
        variables.put("fromId", "1");
        variables.put("toId", "2");
        final StringWriter writer = new StringWriter();
        final VelocityContext context = new VelocityContext(variables);
        travelTimesTemplate.merge(context, writer);
        final String request = writer.toString();
        final URLConnection urlConnection = TRAVEL_URL.openConnection();
        urlConnection.setUseCaches(false);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("accept-charset", "UTF-8");
        urlConnection.setRequestProperty("content-type", "application/soap+xml; charset=utf-8");
        urlConnection.setRequestProperty("Content-Length", "" + request.length());
        OutputStreamWriter outputWriter = new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8");
        outputWriter.write(request);
        outputWriter.flush();
        try {
            final InputStream result = urlConnection.getInputStream();
            final String response = IOUtils.toString(result);
            System.out.println(response);
        } catch (IOException iOException) {
            System.out.println("Something happened: " + iOException);
        }
    }
}
