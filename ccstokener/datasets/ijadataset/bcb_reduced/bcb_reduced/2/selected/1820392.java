package org.integration.test;

import it.polimi.MA.impl.doe.DOESensorConfiguration;
import it.polimi.MA.impl.doe.UpdateBinding;
import it.polimi.MA.service.IEffectorAction;
import it.polimi.MA.service.IManageabilityAgentFacade;
import it.polimi.MA.service.MAService;
import it.polimi.MA.service.SensorSubscriptionData;
import it.polimi.MA.service.exceptions.ServiceStartupException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.slasoi.common.messaging.MessagingException;
import org.slasoi.common.messaging.Setting;
import org.slasoi.common.messaging.Settings;
import org.slasoi.common.messaging.pubsub.Channel;
import org.slasoi.common.messaging.pubsub.MessageEvent;
import org.slasoi.common.messaging.pubsub.MessageListener;
import org.slasoi.common.messaging.pubsub.PubSubFactory;
import org.slasoi.common.messaging.pubsub.PubSubManager;
import org.slasoi.gslam.core.context.GenericSLAManagerUtils;
import org.slasoi.gslam.core.context.SLAManagerContext;
import org.slasoi.gslam.core.context.GenericSLAManagerServices.CreateContextGenericSLAManagerException;
import org.slasoi.gslam.core.context.GenericSLAManagerUtils.GenericSLAManagerUtilsException;
import org.slasoi.gslam.core.context.SLAManagerContext.SLAManagerContextException;
import org.slasoi.gslam.core.monitoring.IMonitoringManager;
import org.slasoi.gslam.core.negotiation.ISyntaxConverter;
import org.slasoi.gslam.core.negotiation.SLARegistry.InvalidUUIDException;
import org.slasoi.gslam.core.negotiation.SLATemplateRegistry.Metadata;
import org.slasoi.infrastructure.servicemanager.exceptions.DescriptorException;
import org.slasoi.infrastructure.servicemanager.exceptions.ProvisionException;
import org.slasoi.infrastructure.servicemanager.exceptions.StopException;
import org.slasoi.infrastructure.servicemanager.exceptions.UnknownIdException;
import org.slasoi.infrastructure.servicemanager.types.EndPoint;
import org.slasoi.infrastructure.servicemanager.types.ProvisionRequestType;
import org.slasoi.infrastructure.servicemanager.types.ProvisionResponseType;
import org.slasoi.ism.occi.IsmOcciService;
import org.slasoi.models.scm.ServiceBuilder;
import org.slasoi.models.scm.extended.ServiceBuilderExtended;
import org.slasoi.monitoring.city.service.CityRCGService;
import org.slasoi.monitoring.city.utils.RCGConstants;
import org.slasoi.monitoring.common.configuration.Component;
import org.slasoi.monitoring.common.configuration.ComponentConfiguration;
import org.slasoi.monitoring.common.configuration.ConfigurationFactory;
import org.slasoi.monitoring.common.configuration.MonitoringSystemConfiguration;
import org.slasoi.monitoring.common.configuration.OutputReceiver;
import org.slasoi.monitoring.common.configuration.ReasonerConfiguration;
import org.slasoi.monitoring.common.configuration.impl.ConfigurationFactoryImpl;
import org.slasoi.monitoring.common.configuration.impl.OutputReceiverImpl;
import org.slasoi.monitoring.fbk.service.FbkRCGService;
import org.slasoi.slamodel.sla.SLA;
import org.slasoi.slamodel.sla.SLATemplate;
import org.slasoi.slamodel.vocab.bnf;
import eu.slasoi.infrastructure.model.infrastructure.Compute;

/**
 * RunTime Monitoring Scenario.
 *
 * @author khurshid
 *
 */
public class RunTimeMonitoringScenario {

    /** monitoring scenario service. **/
    private static RunTimeMonitoringScenario instance = null;

    /** web services property variable. **/
    private static Properties webServiceProps = new Properties();

    /** business manager reporting web service url. **/
    private String bmReportingWSUrl = null;

    /** fbkrcg service. **/
    private static FbkRCGService fbkRcg = null;

    /** cityrcg service. **/
    private static CityRCGService cityRcg = null;

    /** monitoring manager service. **/
    private static IMonitoringManager mmService = null;

    /** infrastructure SLA manager context. **/
    private SLAManagerContext isslamContext = null;

    /** software SLA manager context. **/
    private SLAManagerContext sslamContext = null;

    /** business SLA manager context. **/
    private SLAManagerContext bslamContext = null;

    /** config properties holder. **/
    private static Properties configProps = new Properties();

    /** LOGGER. **/
    private static final Logger LOGGER = Logger.getLogger(RunTimeMonitoringScenario.class);

    /** business SLA manager context. **/
    private static String notificationURI = "adjustment@testbed.sla-at-soi.eu";

    /** host name. **/
    private String hostname = "it";

    /** monitoring request. **/
    private String monitoringRequest = "http://testbed.sla-at-soi.eu/monitoring/request.xml";

    /** random instance. **/
    private Random rnd = new Random(System.currentTimeMillis());

    /** soap action.**/
    private String soapAction = "";

    /**
     * Constructor.
     * @param gslamServices GenericSLAManagerUtils
     * @param osgiContext bundle context
     * @throws CreateContextGenericSLAManagerException exception
     * @throws GenericSLAManagerUtilsException exception
     * @throws SLAManagerContextException exception
     */
    public RunTimeMonitoringScenario(final GenericSLAManagerUtils gslamServices, final BundleContext osgiContext) throws CreateContextGenericSLAManagerException, GenericSLAManagerUtilsException, SLAManagerContextException {
        try {
            webServiceProps.load(new FileInputStream(System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "services.properties"));
            bmReportingWSUrl = webServiceProps.getProperty("BM_ReportingWS_URL");
            if (gslamServices != null) {
                SLAManagerContext[] context = gslamServices.getContextSet("GLOBAL");
                if (context == null) {
                    System.out.println("******************* " + "Gslam Contexts = null ************** ");
                } else {
                    for (SLAManagerContext c : context) {
                        System.out.println("Available Contexts: " + c.getSLAManagerID() + "--" + c.getEPR());
                        if (c.getWSPrefix().equalsIgnoreCase("IS")) {
                            isslamContext = c;
                            System.out.println("Infrastructure SLAM context" + " injected successfully");
                        } else if (c.getWSPrefix().equalsIgnoreCase("SW")) {
                            sslamContext = c;
                            System.out.println("Software SLAM context" + " injected successfully");
                        } else if (c.getWSPrefix().equalsIgnoreCase("BZ")) {
                            bslamContext = c;
                            System.out.println("Business SLAM context" + " injected successfully");
                        }
                    }
                }
            } else {
                System.out.println("******************* " + "gslamServices = null ************** ");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get instance.
     * @param gslamServices GenericSLAManagerUtils
     * @param osgiContext bundle context
     * @param fbkService FbkRCGService
     * @param cityService CityRCGService.
     * @param mmServiceIn IMonitoringManager
     * @return RuntimeMonitoringScenario instance
     * @throws CreateContextGenericSLAManagerException exception
     * @throws GenericSLAManagerUtilsException exception
     * @throws SLAManagerContextException exception
     */
    public static RunTimeMonitoringScenario getInstance(final GenericSLAManagerUtils gslamServices, final BundleContext osgiContext, final FbkRCGService fbkService, final CityRCGService cityService, final IMonitoringManager mmServiceIn) throws CreateContextGenericSLAManagerException, GenericSLAManagerUtilsException, SLAManagerContextException {
        fbkRcg = fbkService;
        cityRcg = cityService;
        mmService = mmServiceIn;
        if (instance == null) {
            instance = new RunTimeMonitoringScenario(gslamServices, osgiContext);
        }
        return instance;
    }

    /**
     * INTERACTION 1 # RUNTIME MONITORING.
     */
    public final void execute() {
        System.out.println("\n\n\n\n####################################" + " RUNTIME MONITORING SCENARIO RUN " + "########################\n\n\n\n");
    }

    /**
     * INTERACTION 1# BM:customer_relation::getReport().
     */
    public final void getReport() {
        try {
            getGTStatusBySLAIdGTReport();
            getGTStatusBySLAIdGTKPIReport();
            getPenaltiesReportByProductOfferIdPartyIdYearMonthReport();
            getPenaltiesReportByProductOfferIdPartyIdYearMonthDayReport();
            getProductReportByProductOfferIdYearMonthReport();
            getProductReportBySLATIdYearMonthReport();
            getSLAReport();
            getViolationsReportByProductOfferIdYearMonth();
            getViolationsReportByProductOfferIdYearMonthDay();
            getViolationsReportBySLATIdYearMonth();
            getViolationsReportBySLATIdYearMonthDay();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * GTStatusBySLAIdGTReport.
     * @throws IOException exception
     */
    private void getGTStatusBySLAIdGTReport() throws IOException {
        String xmlFile1Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "getGTStatusBySLAIdGT.xml";
        URL url;
        url = new URL(bmReportingWSUrl);
        URLConnection connection = url.openConnection();
        HttpURLConnection httpConn = (HttpURLConnection) connection;
        FileInputStream fin = new FileInputStream(xmlFile1Send);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        SOAPClient4XG.copy(fin, bout);
        fin.close();
        byte[] b = bout.toByteArray();
        httpConn.setRequestProperty("Content-Length", String.valueOf(b.length));
        httpConn.setRequestProperty("Content-Type", "application/soap+xml; charset=UTF-8");
        httpConn.setRequestProperty("SOAPAction", soapAction);
        httpConn.setRequestMethod("POST");
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);
        OutputStream out = httpConn.getOutputStream();
        out.write(b);
        out.close();
        InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
        BufferedReader in = new BufferedReader(isr);
        String inputLine;
        StringBuffer response1 = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response1.append(inputLine);
        }
        in.close();
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: Business Manager\n" + "Interface Name: getReport\n" + "Operation Name: GetGTStatusBySLAIdGT\n" + "Input" + "BSLA Id-BSLATID1\n" + "GuranteeTerm-1\n" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        System.out.println("--------------------------------");
        System.out.println("Response\n" + response1.toString());
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
    }

    /**
     * getGTStatusBySLAIdGTKPIReport.
     * @throws IOException exception
     */
    private void getGTStatusBySLAIdGTKPIReport() throws IOException {
        String xmlFile2Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "getGTStatusBySLAIdGTKPI.xml";
        URL url2;
        url2 = new URL(bmReportingWSUrl);
        URLConnection connection2 = url2.openConnection();
        HttpURLConnection httpConn2 = (HttpURLConnection) connection2;
        FileInputStream fin2 = new FileInputStream(xmlFile2Send);
        ByteArrayOutputStream bout2 = new ByteArrayOutputStream();
        SOAPClient4XG.copy(fin2, bout2);
        fin2.close();
        byte[] b2 = bout2.toByteArray();
        httpConn2.setRequestProperty("Content-Length", String.valueOf(b2.length));
        httpConn2.setRequestProperty("Content-Type", "application/soap+xml; charset=UTF-8");
        httpConn2.setRequestProperty("SOAPAction", soapAction);
        httpConn2.setRequestMethod("POST");
        httpConn2.setDoOutput(true);
        httpConn2.setDoInput(true);
        OutputStream out2 = httpConn2.getOutputStream();
        out2.write(b2);
        out2.close();
        InputStreamReader isr2 = new InputStreamReader(httpConn2.getInputStream());
        BufferedReader in2 = new BufferedReader(isr2);
        String inputLine2;
        StringBuffer response2 = new StringBuffer();
        while ((inputLine2 = in2.readLine()) != null) {
            response2.append(inputLine2);
        }
        in2.close();
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: Business Manager\n" + "Interface Name: getReport\n" + "Operation Name: getGTStatusBySLAIdGTKPI\n" + "Input" + "ProductOfferID-1\n" + "PartyID-1\n" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        System.out.println("--------------------------------");
        System.out.println("Response\n" + response2.toString());
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
    }

    /**
     * getPenaltiesReportByProductOfferIdPartyIdYearMonthReport.
     * @throws IOException exception
     */
    private void getPenaltiesReportByProductOfferIdPartyIdYearMonthReport() throws IOException {
        String xmlFile3Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "getPenaltiesReportByProductOfferIdPartyIdYearMonth.xml";
        URL url3;
        url3 = new URL(bmReportingWSUrl);
        URLConnection connection3 = url3.openConnection();
        HttpURLConnection httpConn3 = (HttpURLConnection) connection3;
        FileInputStream fin3 = new FileInputStream(xmlFile3Send);
        ByteArrayOutputStream bout3 = new ByteArrayOutputStream();
        SOAPClient4XG.copy(fin3, bout3);
        fin3.close();
        byte[] b3 = bout3.toByteArray();
        httpConn3.setRequestProperty("Content-Length", String.valueOf(b3.length));
        httpConn3.setRequestProperty("Content-Type", "application/soap+xml; charset=UTF-8");
        httpConn3.setRequestProperty("SOAPAction", soapAction);
        httpConn3.setRequestMethod("POST");
        httpConn3.setDoOutput(true);
        httpConn3.setDoInput(true);
        OutputStream out3 = httpConn3.getOutputStream();
        out3.write(b3);
        out3.close();
        InputStreamReader isr3 = new InputStreamReader(httpConn3.getInputStream());
        BufferedReader in3 = new BufferedReader(isr3);
        String inputLine3;
        StringBuffer response3 = new StringBuffer();
        while ((inputLine3 = in3.readLine()) != null) {
            response3.append(inputLine3);
        }
        in3.close();
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: Business Manager\n" + "Interface Name: getReport\n" + "Operation Name:" + " getPenaltiesReportByProduct" + "OfferIdPartyIdYearMonth\n" + "Input" + "ProductOfferID-1\n" + "PartyID-1\n" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        System.out.println("--------------------------------");
        System.out.println("Response\n" + response3.toString());
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
    }

    /**
     * getPenaltiesReportByProductOfferIdPartyIdYearMonthDayReport.
     * @throws IOException exception
     */
    private void getPenaltiesReportByProductOfferIdPartyIdYearMonthDayReport() throws IOException {
        String xmlFile4Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "getPenaltiesReportByProductOfferIdPartyIdYearMonthDay.xml";
        URL url4;
        url4 = new URL(bmReportingWSUrl);
        URLConnection connection4 = url4.openConnection();
        HttpURLConnection httpConn4 = (HttpURLConnection) connection4;
        FileInputStream fin4 = new FileInputStream(xmlFile4Send);
        ByteArrayOutputStream bout4 = new ByteArrayOutputStream();
        SOAPClient4XG.copy(fin4, bout4);
        fin4.close();
        byte[] b4 = bout4.toByteArray();
        httpConn4.setRequestProperty("Content-Length", String.valueOf(b4.length));
        httpConn4.setRequestProperty("Content-Type", "application/soap+xml; charset=UTF-8");
        httpConn4.setRequestProperty("SOAPAction", soapAction);
        httpConn4.setRequestMethod("POST");
        httpConn4.setDoOutput(true);
        httpConn4.setDoInput(true);
        OutputStream out4 = httpConn4.getOutputStream();
        out4.write(b4);
        out4.close();
        InputStreamReader isr4 = new InputStreamReader(httpConn4.getInputStream());
        BufferedReader in4 = new BufferedReader(isr4);
        String inputLine4;
        StringBuffer response4 = new StringBuffer();
        while ((inputLine4 = in4.readLine()) != null) {
            response4.append(inputLine4);
        }
        in4.close();
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: Business Manager\n" + "Interface Name: getReport\n" + "Operation Name:" + " getPenaltiesReportByProductOfferIdPartyIdYearMonthDay\n" + "Input" + "ProductOfferID-1\n" + "PartyID-1\n" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        System.out.println("Response\n" + response4.toString());
    }

    /**
     * getProductReportByProductOfferIdYearMonthReport.
     * @throws IOException exception
     */
    private void getProductReportByProductOfferIdYearMonthReport() throws IOException {
        String xmlFile5Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "getProductReportByProductOfferIdYearMonth.xml";
        URL url5;
        url5 = new URL(bmReportingWSUrl);
        URLConnection connection5 = url5.openConnection();
        HttpURLConnection httpConn5 = (HttpURLConnection) connection5;
        FileInputStream fin5 = new FileInputStream(xmlFile5Send);
        ByteArrayOutputStream bout5 = new ByteArrayOutputStream();
        SOAPClient4XG.copy(fin5, bout5);
        fin5.close();
        byte[] b5 = bout5.toByteArray();
        httpConn5.setRequestProperty("Content-Length", String.valueOf(b5.length));
        httpConn5.setRequestProperty("Content-Type", "application/soap+xml; charset=UTF-8");
        httpConn5.setRequestProperty("SOAPAction", soapAction);
        httpConn5.setRequestMethod("POST");
        httpConn5.setDoOutput(true);
        httpConn5.setDoInput(true);
        OutputStream out5 = httpConn5.getOutputStream();
        out5.write(b5);
        out5.close();
        InputStreamReader isr5 = new InputStreamReader(httpConn5.getInputStream());
        BufferedReader in5 = new BufferedReader(isr5);
        String inputLine5;
        StringBuffer response5 = new StringBuffer();
        while ((inputLine5 = in5.readLine()) != null) {
            response5.append(inputLine5);
        }
        in5.close();
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: Business Manager\n" + "Interface Name: getReport\n" + "Operation Name: getProductReportByProductOfferIdYearMonth\n" + "Input" + "ProductOfferID-1\n" + "PartyID-1\n" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        System.out.println("Response\n" + response5.toString());
    }

    /**
     * getProductReportBySLATIdYearMonthReport.
     * @throws IOException exception
     */
    private void getProductReportBySLATIdYearMonthReport() throws IOException {
        String xmlFile6Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "getProductReportBySLATIdYearMonth.xml";
        URL url6;
        url6 = new URL(bmReportingWSUrl);
        URLConnection connection6 = url6.openConnection();
        HttpURLConnection httpConn6 = (HttpURLConnection) connection6;
        FileInputStream fin6 = new FileInputStream(xmlFile6Send);
        ByteArrayOutputStream bout6 = new ByteArrayOutputStream();
        SOAPClient4XG.copy(fin6, bout6);
        fin6.close();
        byte[] b6 = bout6.toByteArray();
        httpConn6.setRequestProperty("Content-Length", String.valueOf(b6.length));
        httpConn6.setRequestProperty("Content-Type", "application/soap+xml; charset=UTF-8");
        httpConn6.setRequestProperty("SOAPAction", soapAction);
        httpConn6.setRequestMethod("POST");
        httpConn6.setDoOutput(true);
        httpConn6.setDoInput(true);
        OutputStream out6 = httpConn6.getOutputStream();
        out6.write(b6);
        out6.close();
        InputStreamReader isr6 = new InputStreamReader(httpConn6.getInputStream());
        BufferedReader in6 = new BufferedReader(isr6);
        String inputLine6;
        StringBuffer response6 = new StringBuffer();
        while ((inputLine6 = in6.readLine()) != null) {
            response6.append(inputLine6);
        }
        in6.close();
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: Business Manager\n" + "Interface Name: getReport\n" + "Operation Name: getProductReportBySLATIdYearMonth\n" + "Input" + "BSLA-ID-BSLATID1\n" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        System.out.println("--------------------------------");
        System.out.println("Response\n" + response6.toString());
    }

    /**
     * getSLAReport.
     * @throws IOException exception
     */
    private void getSLAReport() throws IOException {
        String xmlFile7Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "getSLA.xml";
        URL url7;
        url7 = new URL(bmReportingWSUrl);
        URLConnection connection7 = url7.openConnection();
        HttpURLConnection httpConn7 = (HttpURLConnection) connection7;
        FileInputStream fin7 = new FileInputStream(xmlFile7Send);
        ByteArrayOutputStream bout7 = new ByteArrayOutputStream();
        SOAPClient4XG.copy(fin7, bout7);
        fin7.close();
        byte[] b7 = bout7.toByteArray();
        httpConn7.setRequestProperty("Content-Length", String.valueOf(b7.length));
        httpConn7.setRequestProperty("Content-Type", "application/soap+xml; charset=UTF-8");
        httpConn7.setRequestProperty("SOAPAction", soapAction);
        httpConn7.setRequestMethod("POST");
        httpConn7.setDoOutput(true);
        httpConn7.setDoInput(true);
        OutputStream out7 = httpConn7.getOutputStream();
        out7.write(b7);
        out7.close();
        InputStreamReader isr7 = new InputStreamReader(httpConn7.getInputStream());
        BufferedReader in7 = new BufferedReader(isr7);
        String inputLine7;
        StringBuffer response7 = new StringBuffer();
        while ((inputLine7 = in7.readLine()) != null) {
            response7.append(inputLine7);
        }
        in7.close();
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: Business Manager\n" + "Interface Name: getReport\n" + "Operation Name: getSLA\n" + "Input" + "ProductOfferID-1\n" + "PartyID-1\n" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        System.out.println("--------------------------------");
        System.out.println("Response\n" + response7.toString());
    }

    /**
     * getViolationsReportByProductOfferIdYearMonth.
     * @throws IOException exception
     */
    private void getViolationsReportByProductOfferIdYearMonth() throws IOException {
        String xmlFile8Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "getViolationsReportByProductOfferIdYearMonth.xml";
        URL url8;
        url8 = new URL(bmReportingWSUrl);
        URLConnection connection8 = url8.openConnection();
        HttpURLConnection httpConn8 = (HttpURLConnection) connection8;
        FileInputStream fin8 = new FileInputStream(xmlFile8Send);
        ByteArrayOutputStream bout8 = new ByteArrayOutputStream();
        SOAPClient4XG.copy(fin8, bout8);
        fin8.close();
        byte[] b8 = bout8.toByteArray();
        httpConn8.setRequestProperty("Content-Length", String.valueOf(b8.length));
        httpConn8.setRequestProperty("Content-Type", "application/soap+xml; charset=UTF-8");
        httpConn8.setRequestProperty("SOAPAction", soapAction);
        httpConn8.setRequestMethod("POST");
        httpConn8.setDoOutput(true);
        httpConn8.setDoInput(true);
        OutputStream out8 = httpConn8.getOutputStream();
        out8.write(b8);
        out8.close();
        InputStreamReader isr8 = new InputStreamReader(httpConn8.getInputStream());
        BufferedReader in8 = new BufferedReader(isr8);
        String inputLine8;
        StringBuffer response8 = new StringBuffer();
        while ((inputLine8 = in8.readLine()) != null) {
            response8.append(inputLine8);
        }
        in8.close();
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: Business Manager\n" + "Interface Name: getReport\n" + "Operation Name:" + "getViolationsReportByProductOfferIdYearMonth\n" + "Input" + "ProductOfferID-1\n" + "PartyID-1\n" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        System.out.println("--------------------------------");
        System.out.println("Response\n" + response8.toString());
    }

    /**
     * getViolationsReportByProductOfferIdYearMonthDay.
     * @throws IOException exception.
     */
    private void getViolationsReportByProductOfferIdYearMonthDay() throws IOException {
        String xmlFile9Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "getViolationsReportByProductOfferIdYearMonthDay.xml";
        URL url9;
        url9 = new URL(bmReportingWSUrl);
        URLConnection connection9 = url9.openConnection();
        HttpURLConnection httpConn9 = (HttpURLConnection) connection9;
        FileInputStream fin9 = new FileInputStream(xmlFile9Send);
        ByteArrayOutputStream bout9 = new ByteArrayOutputStream();
        SOAPClient4XG.copy(fin9, bout9);
        fin9.close();
        byte[] b9 = bout9.toByteArray();
        httpConn9.setRequestProperty("Content-Length", String.valueOf(b9.length));
        httpConn9.setRequestProperty("Content-Type", "application/soap+xml; charset=UTF-8");
        httpConn9.setRequestProperty("SOAPAction", soapAction);
        httpConn9.setRequestMethod("POST");
        httpConn9.setDoOutput(true);
        httpConn9.setDoInput(true);
        OutputStream out9 = httpConn9.getOutputStream();
        out9.write(b9);
        out9.close();
        InputStreamReader isr9 = new InputStreamReader(httpConn9.getInputStream());
        BufferedReader in9 = new BufferedReader(isr9);
        String inputLine9;
        StringBuffer response9 = new StringBuffer();
        while ((inputLine9 = in9.readLine()) != null) {
            response9.append(inputLine9);
        }
        in9.close();
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: Business Manager\n" + "Interface Name: getReport\n" + "Operation Name: " + "getViolationsReportByProductOfferIdYearMonthDay\n" + "Input" + "ProductOfferID-1\n" + "PartyID-1\n" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        System.out.println("--------------------------------");
        System.out.println("Response\n" + response9.toString());
    }

    /**
     * getViolationsReportBySLATIdYearMonth.
     * @throws IOException exception
     */
    private void getViolationsReportBySLATIdYearMonth() throws IOException {
        String xmlFile10Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "getViolationsReportBySLATIdYearMonth.xml";
        URL url10;
        url10 = new URL(bmReportingWSUrl);
        URLConnection connection10 = url10.openConnection();
        HttpURLConnection httpConn10 = (HttpURLConnection) connection10;
        FileInputStream fin10 = new FileInputStream(xmlFile10Send);
        ByteArrayOutputStream bout10 = new ByteArrayOutputStream();
        SOAPClient4XG.copy(fin10, bout10);
        fin10.close();
        byte[] b10 = bout10.toByteArray();
        httpConn10.setRequestProperty("Content-Length", String.valueOf(b10.length));
        httpConn10.setRequestProperty("Content-Type", "application/soap+xml; charset=UTF-8");
        httpConn10.setRequestProperty("SOAPAction", soapAction);
        httpConn10.setRequestMethod("POST");
        httpConn10.setDoOutput(true);
        httpConn10.setDoInput(true);
        OutputStream out10 = httpConn10.getOutputStream();
        out10.write(b10);
        out10.close();
        InputStreamReader isr10 = new InputStreamReader(httpConn10.getInputStream());
        BufferedReader in10 = new BufferedReader(isr10);
        String inputLine10;
        StringBuffer response10 = new StringBuffer();
        while ((inputLine10 = in10.readLine()) != null) {
            response10.append(inputLine10);
        }
        in10.close();
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: Business Manager\n" + "Interface Name: getReport\n" + "Operation Name: getViolationsReportBySLATIdYearMonth\n" + "Input" + "ProductOfferID-1\n" + "PartyID-1\n" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        System.out.println("--------------------------------");
        System.out.println("Response\n" + response10.toString());
    }

    /**
     * getViolationsReportBySLATIdYearMonthDay.
     * @throws IOException exception.
     */
    private void getViolationsReportBySLATIdYearMonthDay() throws IOException {
        String xmlFile11Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "getViolationsReportBySLATIdYearMonthDay.xml";
        URL url11;
        url11 = new URL(bmReportingWSUrl);
        URLConnection connection11 = url11.openConnection();
        HttpURLConnection httpConn11 = (HttpURLConnection) connection11;
        FileInputStream fin11 = new FileInputStream(xmlFile11Send);
        ByteArrayOutputStream bout11 = new ByteArrayOutputStream();
        SOAPClient4XG.copy(fin11, bout11);
        fin11.close();
        byte[] b11 = bout11.toByteArray();
        httpConn11.setRequestProperty("Content-Length", String.valueOf(b11.length));
        httpConn11.setRequestProperty("Content-Type", "application/soap+xml; charset=UTF-8");
        httpConn11.setRequestProperty("SOAPAction", soapAction);
        httpConn11.setRequestMethod("POST");
        httpConn11.setDoOutput(true);
        httpConn11.setDoInput(true);
        OutputStream out11 = httpConn11.getOutputStream();
        out11.write(b11);
        out11.close();
        InputStreamReader isr11 = new InputStreamReader(httpConn11.getInputStream());
        BufferedReader in11 = new BufferedReader(isr11);
        String inputLine11;
        StringBuffer response11 = new StringBuffer();
        while ((inputLine11 = in11.readLine()) != null) {
            response11.append(inputLine11);
        }
        in11.close();
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: Business Manager\n" + "Interface Name: getReport\n" + "Operation Name: getViolationsReportBySLATIdYearMonthDay\n" + "Input" + "ProductOfferID-1\n" + "PartyID-1\n" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        System.out.println("--------------------------------");
        System.out.println("Response\n" + response11.toString());
    }

    /**
     * INTERACTION 2# LLMS:send_observation::storeMetricObservation().
     */
    public void storeMetricObservation() {
    }

    /**
     * INTERACTION 3.1# Monitoring:createEvent::slaViolation().
     * @param fbkrcgService FbkRCGService
     */
    public final void slaViolation(final FbkRCGService fbkrcgService) {
        listenOnChannel();
        LOGGER.debug("*****************************************");
        LOGGER.debug("START TEST testA4");
        LOGGER.debug("*****************************************");
        try {
            String slatFileName = "testSLA1.xml";
            InputStream is = new FileInputStream(System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "fbkrcg" + System.getProperty("file.separator") + "test" + System.getProperty("file.separator") + slatFileName);
            String configurationId = "gggg:ggggg:gggg:gggg";
            fbkrcgService.cleanDeployDir();
            fbkrcgService.startMonitoring(configurationId);
            fbkrcgService.addSLATToConfiguration(is);
            fbkrcgService.stopMonitoring(configurationId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *Interaction 3.1.1.
     *@param ismServices IsmOcciService
     */
    public final void reprovision(final IsmOcciService ismServices) {
        List<ProvisionResponseType> infrastructures = new ArrayList<ProvisionResponseType>();
        Map<String, Compute> computeResources = new HashMap<String, Compute>();
        ProvisionResponseType provisionResponseType = null;
        ProvisionRequestType provisionRequestType = null;
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: ISM\n" + "Interface Name: manage_infrastructure_service\n" + "Operation Name: Reprovision\n" + "Input:Images.UBUNTU_9_10, Slas.GOLD, Locations.IE, 2, 512," + " 'myHostName', 'notification URI'" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        Compute vmConfiguration1 = ismServices.createComputeConfiguration(ismServices.getOsregistry().getDefaultCategory().getTerm(), ismServices.getMetricregistry().getDefaultCategory().getTerm(), ismServices.getLocregistry().getDefaultCategory().getTerm(), new Hashtable<String, String>());
        String hostName = vmConfiguration1.getHostname();
        computeResources.put(hostName, vmConfiguration1);
        Set<Compute> computeConfigurations = new HashSet<Compute>();
        computeConfigurations.add(vmConfiguration1);
        LOGGER.info(vmConfiguration1);
        try {
            String monitoringRequest = "";
            provisionRequestType = ismServices.createProvisionRequestType(monitoringRequest, computeConfigurations);
            LOGGER.info(provisionRequestType);
            provisionResponseType = ismServices.provision(provisionRequestType);
            LOGGER.info(provisionResponseType);
            if (provisionResponseType.getEndPoints().size() == 0) {
                LOGGER.info("provisionResponseType.getEndPoints().size() ==0");
            }
            LOGGER.info("provision - infrastructureID - " + provisionResponseType.getInfrastructureID());
            List<EndPoint> endPoints = provisionResponseType.getEndPoints();
            for (Iterator<EndPoint> iterator = endPoints.iterator(); iterator.hasNext(); ) {
                EndPoint endPoint = iterator.next();
                LOGGER.info("EndPoint - getHostName - " + endPoint.getHostName());
                Compute compute = computeResources.get(endPoint.getHostName());
                LOGGER.info("Compute Resource associated to this hostName " + compute);
                LOGGER.info("EndPoint - getResourceUrl - " + endPoint.getResourceUrl().toExternalForm());
            }
            infrastructures.add(provisionResponseType);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (DescriptorException e) {
            e.printStackTrace();
        } catch (ProvisionException e) {
            e.printStackTrace();
        }
        EndPoint endPoint = provisionResponseType.getEndPoints().get(rnd.nextInt(provisionResponseType.getEndPoints().size()));
        LOGGER.info("Compute full URL - " + endPoint.getResourceUrl());
        LOGGER.info("Compute Path - " + endPoint.getResourceUrl().getPath());
        try {
            EndPoint endpoint = provisionResponseType.getEndPoints().get(rnd.nextInt(provisionResponseType.getEndPoints().size()));
            Compute compute = computeResources.get(endpoint.getHostName());
            float cpuSpeed = 0;
            LOGGER.info("Current CPU Speed is " + cpuSpeed);
            cpuSpeed = cpuSpeed * 1.5f;
            Set<Compute> newComputeResources = new HashSet<Compute>();
            newComputeResources.add(compute);
            ProvisionRequestType newProvisionRequestType = ismServices.createProvisionRequestType(monitoringRequest, newComputeResources);
            newProvisionRequestType.setProvId(provisionRequestType.getProvId());
            provisionResponseType = ismServices.reprovision(newProvisionRequestType.getProvId(), newProvisionRequestType);
        } catch (DescriptorException e) {
            e.printStackTrace();
        } catch (ProvisionException e) {
            e.printStackTrace();
        } catch (UnknownIdException e) {
            e.printStackTrace();
        }
        if (infrastructures.size() == 0) {
            LOGGER.info("We didnt provision anything");
        }
        for (Iterator<ProvisionResponseType> iterator = infrastructures.iterator(); iterator.hasNext(); ) {
            provisionResponseType = iterator.next();
            LOGGER.info(provisionResponseType.getInfrastructureID());
            String infrastructureID = provisionResponseType.getInfrastructureID();
            LOGGER.info("infrastructureID - " + infrastructureID);
            try {
                ismServices.stop(infrastructureID);
            } catch (StopException e) {
                LOGGER.info(e);
                e.printStackTrace();
            } catch (UnknownIdException e) {
                LOGGER.info(e);
                e.printStackTrace();
            }
        }
    }

    /**
     * INTERACTION 3.1.1.1#LLMS:manage_infrastructure_service::getMetricX().
     */
    public void getMetricX() {
    }

    /**
     * INTERACTION 4# LLMS:send_observation::storeMetricObservation().
     */
    public void storeMetricObservation2() {
    }

    /**
     * INTERACTION 4.1.1.1.
     * MangeabilitAgent:manage_software_service::executeAction()
     * @param manageabilityAgentService MAService
     */
    public final void executeAction(final MAService manageabilityAgentService) {
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: Manageability Agent\n" + "Interface Name: manage_software_service\n" + "Operation Name: executeAction\n" + "Input" + "ProductOfferID-1\n" + "PartyID-1\n" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        try {
            String uuid = "testUUID";
            ServiceBuilder builder = new ServiceBuilderExtended();
            builder.setUuid(uuid);
            Settings settings = new Settings();
            settings.setSetting(Setting.pubsub, "xmpp");
            settings.setSetting(Setting.xmpp_username, "primitive-ecf");
            settings.setSetting(Setting.xmpp_password, "primitive-ecf");
            settings.setSetting(Setting.xmpp_host, "testbed.sla-at-soi.eu");
            settings.setSetting(Setting.xmpp_port, "5222");
            settings.setSetting(Setting.messaging, "xmpp");
            settings.setSetting(Setting.pubsub, "xmpp");
            settings.setSetting(Setting.xmpp_service, "testbed.sla-at-soi.eu");
            settings.setSetting(Setting.xmpp_resource, "test");
            settings.setSetting(Setting.xmpp_pubsubservice, "pubsub.testbed.sla-at-soi.eu");
            String notificationChannel = "test-MA-Sam";
            manageabilityAgentService.startServiceInstance(builder, settings, notificationChannel);
            new UpdateBinding(builder);
            IManageabilityAgentFacade facade = manageabilityAgentService.getManagibilityAgentFacade(builder);
            ConfigurationFactoryImpl factory = new ConfigurationFactoryImpl();
            MonitoringSystemConfiguration msc = factory.createMonitoringSystemConfiguration();
            msc.setUuid(UUID.randomUUID().toString());
            Component[] components = new Component[1];
            Component c = factory.createComponent();
            c.setType("Sensor");
            DOESensorConfiguration config = new DOESensorConfiguration();
            config.setConfigurationId(UUID.randomUUID().toString());
            String serviceID = "paymentService";
            config.setServiceID(serviceID);
            String operationID = "/process/flow/receive[@name=$$ReceivePaymentRequest$$]";
            config.setOperationID(operationID);
            String status = "input";
            config.setStatus(status);
            String correlationKey = "cardNumber";
            config.setCorrelationKey(correlationKey);
            String correlationValue = "7777";
            config.setCorrelationValue(correlationValue);
            OutputReceiver[] newOutputReceivers = new OutputReceiverImpl[1];
            OutputReceiver receiver = new ConfigurationFactoryImpl().createOutputReceiver();
            receiver.setEventType("event");
            receiver.setUuid("tcp:localhost:10000");
            newOutputReceivers[0] = receiver;
            config.setOutputReceivers(newOutputReceivers);
            ComponentConfiguration[] configs = new ComponentConfiguration[1];
            configs[0] = config;
            c.setConfigurations(configs);
            components[0] = c;
            msc.setComponents(components);
            facade.configureMonitoringSystem(msc);
            List<SensorSubscriptionData> list = facade.getSensorSubscriptionData();
            for (SensorSubscriptionData d : list) {
                System.out.println("[DOE - facade] Sensor id: " + d.getSensorID());
            }
            facade.deconfigureMonitoring();
            list = facade.getSensorSubscriptionData();
            for (SensorSubscriptionData d : list) {
                System.out.println("[DOE - facade] Sensor id: " + d.getSensorID());
            }
            IEffectorAction updateAction = new UpdateBinding(builder);
            try {
                facade.executeAction(updateAction);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (ServiceStartupException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 4.1.1.1#LLMS:manage_infrastructure_service::getMetricX().
     */
    public void getMetricXSecond() {
    }

    /**
     * Complete Scenario Run.
     */
    public final void runORC() {
        try {
            fbkRcg.cleanDeployDir();
            configProps.load(new FileInputStream(System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "config.properties"));
            String slaUuid = configProps.getProperty("slaUuid");
            org.slasoi.slamodel.primitives.UUID[] slaUUID = new org.slasoi.slamodel.primitives.UUID[] { new org.slasoi.slamodel.primitives.UUID(slaUuid) };
            SLA[] slas = sslamContext.getSLARegistry().getIQuery().getSLA(slaUUID);
            LOGGER.debug(slas[0]);
            String configurationId = "gggg:ggggg:gggg:gggg";
            fbkRcg.startMonitoring(configurationId);
            ConfigurationFactory cf = new ConfigurationFactoryImpl();
            ReasonerConfiguration rc = cf.createReasonerConfiguration();
            rc.setSpecification(slas[0]);
            fbkRcg.addConfiguration(rc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Bus.
     */
    public final void listenOnChannel() {
        try {
            String eventBusProps = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "fbkrcg" + System.getProperty("file.separator") + "properties" + System.getProperty("file.separator") + "interactioneventbus.properties";
            String eventChannelName = "InteractionEventChannel2";
            PubSubManager pubSubManager1 = PubSubFactory.createPubSubManager(eventBusProps);
            pubSubManager1.createChannel(new Channel(eventChannelName));
            pubSubManager1.subscribe(eventChannelName);
            pubSubManager1.addMessageListener(new org.slasoi.common.messaging.pubsub.MessageListener() {

                public void processMessage(final org.slasoi.common.messaging.pubsub.MessageEvent messageEvent) {
                    LOGGER.debug("*************** event Bus message: " + messageEvent.getMessage().getPayload() + "**************************");
                }
            });
            String resultBusProps = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "fbkrcg" + System.getProperty("file.separator") + "properties" + System.getProperty("file.separator") + "monitorresulteventbus.properties";
            String resultChannelName = "FBKMonResultEventChannel2";
            PubSubManager pubSubManager2 = PubSubFactory.createPubSubManager(resultBusProps);
            pubSubManager2.createChannel(new Channel(resultChannelName));
            pubSubManager2.subscribe(resultChannelName);
            pubSubManager2.addMessageListener(new org.slasoi.common.messaging.pubsub.MessageListener() {

                public void processMessage(final org.slasoi.common.messaging.pubsub.MessageEvent messageEvent) {
                    LOGGER.debug("*************** result bus message: " + messageEvent.getMessage().getPayload() + "**************************");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 4.1 Generate events for ORC scenario.
     */
    public final void generateEvents() {
        fbkRcg.generateEvents();
    }

    /**
     * method for the validation and uploading of SLA templates.
     */
    public final void validateUploadTemplate() {
        String filePath = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "templates" + System.getProperty("file.separator") + "ORC_Business-SLAT.xml";
        try {
            File file = new File(filePath);
            String slaTemplateXml = FileUtils.readFileToString(file);
            ISyntaxConverter sc = ((Hashtable<ISyntaxConverter.SyntaxConverterType, ISyntaxConverter>) bslamContext.getSyntaxConverters()).get(ISyntaxConverter.SyntaxConverterType.SLASOISyntaxConverter);
            SLATemplate slaTemplate = (SLATemplate) sc.parseSLATemplate(slaTemplateXml);
            slaTemplate.setPropertyValue(org.slasoi.slamodel.vocab.sla.service_type, "e19cd2cb-07b1-432b-bd82-ffa33f3fb0fc");
            String htmlRendering = bnf.render(slaTemplate, true);
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File("ORC_Business-SLAT" + "-parsing.html")));
            dos.writeChars(htmlRendering);
            dos.close();
            org.slasoi.slamodel.sla.tools.Validator.Warning[] warnings = bslamContext.getSLATemplateRegistry().validateSLATemplate(slaTemplate);
            for (org.slasoi.slamodel.sla.tools.Validator.Warning warning : warnings) {
                LOGGER.info("TestActivator : SLATR warning = " + warning.message());
            }
            Metadata metaData = new Metadata();
            metaData.setPropertyValue(Metadata.provider_uuid, "4");
            metaData.setPropertyValue(Metadata.service_type, "b2c3f591-f2ca-42b3-92a0-955ba2b36035");
            metaData.setPropertyValue(Metadata.registrar_id, "ORCRegistrar1");
            bslamContext.getSLATemplateRegistry().addSLATemplate(slaTemplate, metaData);
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * test for b6 usecase specific sla parsing.
     */
    public final void testSLAParsing() {
        String b6HOME = System.getenv("B6_HOME");
        String slasPath = b6HOME + System.getProperty("file.separator") + "b6sla-creation" + System.getProperty("file.separator") + "test" + System.getProperty("file.separator");
        fbkRcg.cleanDeployDir();
        try {
            InputStream isB6SLA1 = new FileInputStream(slasPath + "B6SLA1.xml");
            InputStream isB6SLAHumanOperator1 = new FileInputStream(slasPath + "B6SLAHumanOperator1.xml");
            InputStream isB6SLAHumanOperator2 = new FileInputStream(slasPath + "B6SLAHumanOperator2.xml");
            InputStream isB6SLAShuttle1 = new FileInputStream(slasPath + "B6SLAShuttle1.xml");
            InputStream isB6SLAShuttle2 = new FileInputStream(slasPath + "B6SLAShuttle2.xml");
            InputStream isB6SLACab1 = new FileInputStream(slasPath + "B6SLACab1.xml");
            InputStream isB6SLACab2 = new FileInputStream(slasPath + "B6SLACab2.xml");
            InputStream isB6SLA2 = new FileInputStream(slasPath + "B6SLA2.xml");
            InputStream isB6SLA22 = new FileInputStream(slasPath + "B6SLA22.xml");
            String configurationId = "gggg:ggggg:gggg:gggg";
            fbkRcg.startMonitoring(configurationId);
            fbkRcg.addSLATToConfiguration(isB6SLA1);
            System.out.println("B6SLA1 added to MONITORING ENGINE");
            fbkRcg.addSLATToConfiguration(isB6SLAHumanOperator1);
            System.out.println("B6SLAHumanOperator1 added to MONITORING ENGINE");
            fbkRcg.addSLATToConfiguration(isB6SLAHumanOperator2);
            System.out.println("B6SLAHumanOperator2 added to MONITORING ENGINE");
            fbkRcg.addSLATToConfiguration(isB6SLAShuttle1);
            System.out.println("B6SLAShuttle1 added to MONITORING ENGINE");
            fbkRcg.addSLATToConfiguration(isB6SLAShuttle2);
            System.out.println("B6SLAShuttle2 added to MONITORING ENGINE");
            fbkRcg.addSLATToConfiguration(isB6SLACab1);
            System.out.println("B6SLACab1 added to MONITORING ENGINE");
            fbkRcg.addSLATToConfiguration(isB6SLACab2);
            System.out.println("B6SLACab2 added to MONITORING ENGINE");
            System.out.println("B6SLA2 replaces B6SLA1");
            fbkRcg.replaceSLA(isB6SLA2);
            System.out.println("B6SLA22 added to MONITORING ENGINE");
            fbkRcg.addSLATToConfiguration(isB6SLA22);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * orc run with City Rcg.
     * @throws IOException
     * @throws MessagingException
     * @throws FileNotFoundException
     * @throws SLAManagerContextException
     * @throws InvalidUUIDException
     */
    public final void orcRunWithEverest() {
        try {
            configProps.load(new FileInputStream(System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "config.properties"));
            String slaUuid = configProps.getProperty("slaUuid");
            org.slasoi.slamodel.primitives.UUID[] slaUUID = new org.slasoi.slamodel.primitives.UUID[] { new org.slasoi.slamodel.primitives.UUID(slaUuid) };
            SLA[] slas;
            slas = sslamContext.getSLARegistry().getIQuery().getSLA(slaUUID);
            String slasoiOrcHome = System.getenv().get("SLASOI_HOME");
            ConfigurationFactory cf = ConfigurationFactory.eINSTANCE;
            ReasonerConfiguration reasonerConfiguration = cf.createReasonerConfiguration();
            reasonerConfiguration.setConfigurationId(RCGConstants.EVEREST_RCG_UUID);
            reasonerConfiguration.setSpecification(slas[0]);
            LOGGER.debug("Submitted SLA " + reasonerConfiguration.getSpecification().toString());
            cityRcg.addConfiguration(reasonerConfiguration);
            cityRcg.startMonitoring(reasonerConfiguration.getConfigurationId());
            PubSubManager pubSubManager = PubSubFactory.createPubSubManager(slasoiOrcHome + "/monitoring-system/sla-level-monitoring/city/slasoiInteractionEventBus.properties");
            Properties props = new Properties();
            String resChName = "MonitoringResultEvent";
            props.load(new FileInputStream(slasoiOrcHome + "/monitoring-system/sla-level-monitoring/city/slasoiMonResultsEventBus.properties"));
            resChName = props.getProperty("xmpp_channel");
            pubSubManager.subscribe(resChName);
            pubSubManager.addMessageListener(new MessageListener() {

                public void processMessage(final MessageEvent messageEvent) {
                    System.out.println("********* Received Monitoring result in RCG Client *******\n" + messageEvent.getMessage().getPayload());
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (InvalidUUIDException e) {
            e.printStackTrace();
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        }
    }
}
