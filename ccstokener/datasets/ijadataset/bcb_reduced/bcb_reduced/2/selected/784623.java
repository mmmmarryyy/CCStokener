package org.jboss.shrinkwrap.mobicents.servlet.sip.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import junit.framework.Assert;
import org.apache.catalina.startup.Embedded;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.mobicents.servlet.sip.api.ShrinkWrapSipStandardContext;
import org.jboss.shrinkwrap.mobicents.servlet.sip.servlet.ContextServlet;
import org.jboss.shrinkwrap.mobicents.servlet.sip.servlet.ForwardingServlet;
import org.jboss.shrinkwrap.mobicents.servlet.sip.servlet.RequestParamEchoServlet;
import org.jboss.shrinkwrap.mobicents.servlet.sip.util.SipEmbedded;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.servlet.sip.core.session.SipStandardManager;
import org.mobicents.servlet.sip.startup.SipContextConfig;
import org.mobicents.servlet.sip.startup.SipStandardContext;

/**
 * Ensures that deployment on {@link ShrinkWrapSipStandardContext} into
 * the Tomcat {@link Embedded} works as expected.
 * 
 * @author Jean Deruelle
 * @version $Revision: $
 */
public class MobicentsSipServletsDeploymentIntegrationUnitTestCase {

    /**
    * Logger
    */
    private static final Logger log = Logger.getLogger(MobicentsSipServletsDeploymentIntegrationUnitTestCase.class.getName());

    private static final String BIND_HOST = "127.0.0.1";

    /**
    * Port to which the HTTP server should bind (above 1024 for *nix non-root environments)
    */
    private static final int HTTP_BIND_PORT = 12345;

    /**
    * Port to which the SIP server should bind (above 1024 for *nix non-root environments)
    */
    private static final int SIP_BIND_PORT = 5080;

    /**
    * Path, relative to the resources base, of the directory containing web.xml descriptor for tests
    */
    private static final String PATH_RESOURCE_WEB_XML = "webxml/";

    /**
    * Path, relative to the resources base, of a test web.xml
    */
    private static final String PATH_ACTUAL_WEB_XML = PATH_RESOURCE_WEB_XML + "servletForwarding.xml";

    /**
    * Path, relative to the resources base, of the directory containing web.xml descriptor for tests
    */
    private static final String PATH_RESOURCE_SIP_XML = "sipxml/";

    /**
    * Path, relative to the resources base, of a test web.xml
    */
    private static final String PATH_ACTUAL_SIP_XML = PATH_RESOURCE_SIP_XML + "servletForwarding.xml";

    /**
    * Path, relative to the resources base, of a test JSP
    */
    private static final String PATH_ECHO_SERVLET = "RequestParamEchoServlet";

    /**
    * URI Separator
    */
    private static final char SEPARATOR = '/';

    /**
    * Tomcat server
    */
    private static SipEmbedded server;

    /**
    * Servlet Class under test
    */
    private static final Class<?> servletClass = ForwardingServlet.class;

    private static final Class<?> echoServletClass = RequestParamEchoServlet.class;

    /**
    * Sip Servlet Class under test
    */
    private static final Class<?> sipServletClass = ContextServlet.class;

    /**
    * Name of the web application
    */
    private static final String NAME_SIPAPP = "ShrinkWrapMobicentsSipServletsTestApplication";

    /**
    * Name to assign to the WAR
    */
    private static final String NAME_WAR = NAME_SIPAPP + ".war";

    /**
    * Configures and starts the Mobicents Sip Servlets on Tomcat Embedded Server
    */
    @BeforeClass
    public static void createServerAndDeployWebapp() throws Exception {
        createMobicentsSipServlets();
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, NAME_WAR);
        archive.addAsWebInfResource(PATH_ACTUAL_WEB_XML, "web.xml");
        archive.addAsWebInfResource(PATH_ACTUAL_SIP_XML, "sip.xml");
        archive.addClasses(sipServletClass, servletClass, echoServletClass);
        log.info(archive.toString(true));
        final SipStandardContext context = archive.as(ShrinkWrapSipStandardContext.class);
        context.setXmlNamespaceAware(true);
        context.addLifecycleListener(new SipContextConfig());
        context.setManager(new SipStandardManager());
        server.deployContext(context);
    }

    protected static void createMobicentsSipServlets() throws Exception {
        server = new SipEmbedded("mobicents-sip-servlets");
        server.initTomcat(null);
        server.addHttpConnector(BIND_HOST, HTTP_BIND_PORT);
        server.addSipConnector("mobicents-sip-servlets", BIND_HOST, SIP_BIND_PORT, "UDP", null);
        server.startTomcat();
    }

    /**
    * Stops the Tomcat Server
    * @throws Exception
    */
    @AfterClass
    public static void stopServer() throws Exception {
        server.stopTomcat();
    }

    /**
    * Tests that we can execute an HTTP request on a Converged SIP Servlets application
    * and it's fulfilled as expected by returning the SIP application name in addition
    * to the echo value, proving our deployment succeeded
    */
    @Test
    public void requestWebapp() throws Exception {
        final HttpClient client = new DefaultHttpClient();
        final String echoValue = "ShrinkWrap>Tomcat Integration";
        final List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("to", PATH_ECHO_SERVLET));
        params.add(new BasicNameValuePair("echo", echoValue));
        final URI uri = URIUtils.createURI("http", BIND_HOST, HTTP_BIND_PORT, NAME_SIPAPP + SEPARATOR + servletClass.getSimpleName(), URLEncodedUtils.format(params, "UTF-8"), null);
        final HttpGet request = new HttpGet(uri);
        log.info("Executing request to: " + request.getURI());
        final HttpResponse response = client.execute(request);
        System.out.println(response.getStatusLine());
        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            Assert.fail("Request returned no entity");
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
        final String line = reader.readLine();
        Assert.assertEquals("Unexpected response from Servlet", echoValue + NAME_SIPAPP, line);
    }
}
