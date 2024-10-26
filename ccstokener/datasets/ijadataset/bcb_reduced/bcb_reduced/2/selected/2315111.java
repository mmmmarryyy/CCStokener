package org.integration.test;

import it.polimi.MA.service.IManageabilityAgentFacade;
import it.polimi.MA.service.MAService;
import it.polimi.MA.service.exceptions.ServiceStartupException;
import it.polimi.MA.impl.doe.UpdateBinding;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.slasoi.businessManager.common.util.Constants;
import org.slasoi.businessManager.common.model.EmParty;
import org.slasoi.businessManager.common.model.EmPartyPartyrole;
import org.slasoi.businessManager.common.model.EmPartyPartyroleId;
import org.slasoi.businessManager.common.model.EmPartyRole;
import org.slasoi.businessManager.common.service.PartyManager;
import org.slasoi.businessManager.common.service.PartyPartyRoleManager;
import org.slasoi.businessmanager.track.types.BusinessManager_TrackStub;
import org.slasoi.businessmanager.track.types.CustomerNotFoundExceptionException;
import org.slasoi.businessmanager.track.types.ProductNotFoundExceptionException;
import org.slasoi.businessmanager.track.types.BusinessManager_TrackStub.AdjustmentNotificationType;
import org.slasoi.businessmanager.track.types.BusinessManager_TrackStub.GetCustomerPurchaseAuth;
import org.slasoi.businessmanager.track.types.BusinessManager_TrackStub.GetCustomerPurchaseAuthResponse;
import org.slasoi.businessmanager.track.types.BusinessManager_TrackStub.TrackEvent;
import org.slasoi.businessmanager.ws.impl.BusinessManager_PartyStub;
import org.slasoi.businessmanager.ws.impl.BusinessManager_QueryProductCatalogStub;
import org.slasoi.businessmanager.ws.impl.BusinessManager_PartyStub.AuthenticateUser;
import org.slasoi.businessmanager.ws.impl.BusinessManager_PartyStub.AuthenticateUserResponse;
import org.slasoi.businessmanager.ws.impl.BusinessManager_PartyStub.CreateParty;
import org.slasoi.businessmanager.ws.impl.BusinessManager_PartyStub.CreatePartyResponse;
import org.slasoi.businessmanager.ws.impl.BusinessManager_PartyStub.CreatePartyResponseType;
import org.slasoi.businessmanager.ws.impl.BusinessManager_PartyStub.GetParameterList;
import org.slasoi.businessmanager.ws.impl.BusinessManager_PartyStub.GetParameterListResponse;
import org.slasoi.businessmanager.ws.impl.BusinessManager_PartyStub.GetParameterListResponseType;
import org.slasoi.businessmanager.ws.impl.BusinessManager_PartyStub.IndividualType;
import org.slasoi.businessmanager.ws.impl.BusinessManager_PartyStub.OrganizationType;
import org.slasoi.businessmanager.ws.impl.BusinessManager_PartyStub.PartyType;
import org.slasoi.businessmanager.ws.impl.BusinessManager_PartyStub.UserType;
import org.slasoi.businessmanager.ws.impl.BusinessManager_QueryProductCatalogStub.GetProducts;
import org.slasoi.businessmanager.ws.impl.BusinessManager_QueryProductCatalogStub.GetProductsResponse;
import org.slasoi.businessmanager.ws.impl.BusinessManager_QueryProductCatalogStub.GetTemplates;
import org.slasoi.businessmanager.ws.impl.BusinessManager_QueryProductCatalogStub.GetTemplatesResponse;
import org.slasoi.businessmanager.ws.impl.BusinessManager_QueryProductCatalogStub.Product;
import org.slasoi.common.messaging.MessagingException;
import org.slasoi.common.messaging.Settings;
import org.slasoi.common.messaging.pubsub.Channel;
import org.slasoi.common.messaging.pubsub.MessageEvent;
import org.slasoi.common.messaging.pubsub.MessageListener;
import org.slasoi.common.messaging.pubsub.PubSubFactory;
import org.slasoi.common.messaging.pubsub.PubSubManager;
import org.slasoi.common.messaging.pubsub.PubSubMessage;
import org.slasoi.gslam.core.context.GenericSLAManagerUtils;
import org.slasoi.gslam.core.context.SLAManagerContext;
import org.slasoi.gslam.core.context.GenericSLAManagerServices.CreateContextGenericSLAManagerException;
import org.slasoi.gslam.core.context.GenericSLAManagerUtils.GenericSLAManagerUtilsException;
import org.slasoi.gslam.core.context.SLAManagerContext.SLAManagerContextException;
import org.slasoi.gslam.core.negotiation.ISyntaxConverter;
import org.slasoi.gslam.core.negotiation.INegotiation.InvalidNegotiationIDException;
import org.slasoi.gslam.core.negotiation.INegotiation.OperationInProgressException;
import org.slasoi.gslam.core.negotiation.INegotiation.OperationNotPossibleException;
import org.slasoi.gslam.core.negotiation.INegotiation.SLACreationException;
import org.slasoi.gslam.core.negotiation.SLARegistry.InvalidUUIDException;
import org.slasoi.gslam.core.negotiation.SLARegistry.RegistrationFailureException;
import org.slasoi.gslam.core.negotiation.SLARegistry.SLAState;
import org.slasoi.gslam.core.negotiation.SLATemplateRegistry.Exception;
import org.slasoi.infrastructure.servicemanager.exceptions.DescriptorException;
import org.slasoi.infrastructure.servicemanager.exceptions.ProvisionException;
import org.slasoi.infrastructure.servicemanager.types.ProvisionRequestType;
import org.slasoi.infrastructure.servicemanager.types.ProvisionResponseType;
import org.slasoi.models.scm.ServiceBuilder;
import org.slasoi.models.scm.extended.ServiceBuilderExtended;
import org.slasoi.slamodel.primitives.STND;
import org.slasoi.slamodel.primitives.UUID;
import org.slasoi.slamodel.sla.Party;
import org.slasoi.slamodel.sla.SLA;
import org.slasoi.slamodel.sla.SLATemplate;

/**
 * Provision Scenario.
 *
 * @author khurshid
 *
 */
public class ProvisioningScenario {

    /** this. **/
    private static ProvisioningScenario instance = null;

    /** business manager query product catalog WS client.**/
    private BusinessManager_QueryProductCatalogStub qpcWSClient = null;

    /** business manager party WS client.**/
    private BusinessManager_PartyStub partyWSClient = null;

    /** business manager track WS client.**/
    private BusinessManager_TrackStub trackWSClient = null;

    /** infrastructure SLA manager context.**/
    private SLAManagerContext isslamContext = null;

    /** software SLA manager context. **/
    private SLAManagerContext sslamContext = null;

    /** business SLA manager context.**/
    private SLAManagerContext bslamContext = null;

    /** web service properties holder. **/
    private static Properties webServiceProps = new Properties();

    /** config properties holder. **/
    private static Properties configProps = new Properties();

    /** URL holder for business manager product WS.**/
    private String bmQueryProductWSURL = null;

    /** URL holder for business manager party WS.**/
    private String bmPartyWSURL = null;

    /** test bus channel name. **/
    private String channelName = "test-messagging";

    /** URL holder for syntax converter negotiation WS.**/
    private String syntaxConverterNegotiationWSURL = null;

    /****/
    private String syntaxConverterControlWSURL = null;

    /****/
    private final Semaphore semSensorFinished = new Semaphore(0);

    /****/
    private final Semaphore semUserCompFinished = new Semaphore(0);

    /****/
    private final Semaphore semMetricsRegistered = new Semaphore(0);

    /****/
    private final Semaphore semMonReqRegistered = new Semaphore(0);

    /****/
    private final Semaphore semDataSent = new Semaphore(0);

    /**manage ability agent service instance.**/
    private static MAService manageabilityAgentService = null;

    /** business manager party role service instance.**/
    private static PartyPartyRoleManager bmPartyRoleService = null;

    /** business manager party manager service instance.**/
    private static PartyManager bmPartyManagerService = null;

    /** template ID holder.**/
    private String templateID = null;

    /** LOGGER. **/
    private static final Logger LOGGER = Logger.getLogger(ProvisioningScenario.class);

    /**
     * constructor.
     * @param gslamServices GenericSLAManagerUtils
     * @param osgiContext BundleContext
     * @throws CreateContextGenericSLAManagerException exception
     * @throws GenericSLAManagerUtilsException exception
     * @throws SLAManagerContextException exception
     */
    public ProvisioningScenario(final GenericSLAManagerUtils gslamServices, final BundleContext osgiContext) throws CreateContextGenericSLAManagerException, GenericSLAManagerUtilsException, SLAManagerContextException {
        try {
            webServiceProps.load(new FileInputStream(System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "services.properties"));
            bmQueryProductWSURL = webServiceProps.getProperty("BM_QueryProductCatalogWS_URL");
            bmPartyWSURL = webServiceProps.getProperty("BM_PartyWS_URL");
            syntaxConverterNegotiationWSURL = webServiceProps.getProperty("GSLAM_SyntaxConverterWSNegotiation_URL");
            syntaxConverterControlWSURL = webServiceProps.getProperty("GSLAM_SyntaxConverterWSControl_URL");
            qpcWSClient = new BusinessManager_QueryProductCatalogStub(bmQueryProductWSURL);
            partyWSClient = new BusinessManager_PartyStub(bmPartyWSURL);
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
        } catch (AxisFault e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get Instance.
     * @param gslamServices GenericSLAManagerUtils
     * @param partyroleManager PartyPartyRoleManager
     * @param partyManager PartyManager
     * @param maS ManageabilityAgent service
     * @param osgiContex BundleContext
     * @return ProvisioningScenario instance
     * @throws CreateContextGenericSLAManagerException exception
     * @throws GenericSLAManagerUtilsException exception
     * @throws SLAManagerContextException exception
     */
    public static ProvisioningScenario getInstance(final GenericSLAManagerUtils gslamServices, final PartyPartyRoleManager partyroleManager, final PartyManager partyManager, final MAService maS, final BundleContext osgiContex) throws CreateContextGenericSLAManagerException, GenericSLAManagerUtilsException, SLAManagerContextException {
        bmPartyRoleService = partyroleManager;
        bmPartyManagerService = partyManager;
        manageabilityAgentService = maS;
        if (instance == null) {
            instance = new ProvisioningScenario(gslamServices, osgiContex);
        }
        return instance;
    }

    /**
     * INTERACTION 0 # LLMS.
     */
    public final void register() {
    }

    /**
     * INTERACTION 1 # negotiate/coordinate::provision [GSLAM].
     */
    public final void provisionGSLAM() {
        try {
            UUID[] slaUUID = new UUID[] { new UUID("AG-1") };
            SLA[] slas = sslamContext.getSLARegistry().getIQuery().getSLA(slaUUID);
            for (SLA s : slas) {
                Party[] parties = s.getParties();
                System.out.println("SLA Info :::" + s.getUuid().toString());
                for (Party p : parties) {
                    System.out.println("Printing gslam_epr value for Party" + p.getId() + "--" + p.getAgreementRole());
                    System.out.println(parties[0].getPropertyValue(new STND("gslam_epr")));
                }
            }
            sslamContext.getSLARegistry().getIRegister().register(slas[0], slaUUID, SLAState.OBSERVED);
            String xmlFile2Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "provision.xml";
            String soapAction = "";
            URL url;
            url = new URL(syntaxConverterNegotiationWSURL);
            URLConnection connection = url.openConnection();
            HttpURLConnection httpConn = (HttpURLConnection) connection;
            FileInputStream fin = new FileInputStream(xmlFile2Send);
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
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder db;
            db = factory.newDocumentBuilder();
            org.xml.sax.InputSource inStream = new org.xml.sax.InputSource();
            inStream.setCharacterStream(new java.io.StringReader(response.toString()));
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: GSLAM\n" + "Interface Name: negotiate/coordinage\n" + "Operation Name: Provision\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println(response.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (RegistrationFailureException e) {
            e.printStackTrace();
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        } catch (InvalidUUIDException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 1.1 # negotiate/query/coordinate::provision [SWSLAM].
     */
    public final void provisionSoftwareSLAM() {
        try {
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: SWSLAM\n" + "Interface Name: negotiate/query/coordinate\n" + "Operation Name: provision\n" + "Input:UUID('AG-1')\n" + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("--------------------------------");
            System.out.println(sslamContext.getPlanningOptimization().getIAssessmentAndCustomize().provision(new UUID("AG-1")));
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 1.1.1 # negotiate/query/coordinate::provision [ISSLAM].
     */
    public final void provisionInfrastructureSLAM() {
        try {
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: ISSLAM(POC)\n" + "Interface Name: negotiate/query/coordinate\n" + "Operation Name: provision\n" + "Input:UUID('AG-1')\n" + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("--------------------------------");
            System.out.println(isslamContext.getPlanningOptimization().getIAssessmentAndCustomize().provision(new UUID("AG-1")));
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 1.1.1.1 # manage_infrastructure_service::provision [ISM].
     * @param ismServices IsmOCCIService
     */
    public final void provisionInfrastructureSM(final org.slasoi.ism.occi.IsmOcciService ismServices) {
        try {
            ProvisionRequestType provisionRequest = ismServices.createProvisionRequestType(ismServices.getOsregistry().getDefaultCategory().getTerm(), ismServices.getMetricregistry().getDefaultCategory().getTerm(), ismServices.getLocregistry().getDefaultCategory().getTerm(), "", "notification URI");
            ProvisionResponseType provisionResponseType = ismServices.provision(provisionRequest);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: ISM\n" + "Interface Name: manage_infrastructure_service\n" + "Operation Name: provision\n" + "Input: \n" + provisionRequest.toString() + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("--Provision Infrastructure ID--" + provisionResponseType.getInfrastructureID());
        } catch (DescriptorException e) {
            e.printStackTrace();
        } catch (ProvisionException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 1.1.2 # manage_software_service::startInstance [SSM].
     */
    public final void startInstanceSoftwareSM() {
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: SSM\n" + "Interface Name: manage_software_service\n" + "Operation Name: startInstance\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
    }

    /**
     * INTERACTION 2 # native_service_management::createInfraService
     * [ISM ->Manageability's Agent].
     */
    public final void createInfraService() {
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: ISM-MA\n" + "Interface Name: native_service_management\n" + "Operation Name: createInfraService\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
    }

    /**
     * INTERACTION 2.1 # publish_event::creation
     * [Manageability's Agent -> Monitored Event Channel].
     */
    public final void creationMonitorEventChannel() {
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: MA->ME\n" + "Interface Name: publish_event\n" + "Operation Name: creation\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
    }

    /**
     * INTERACTION 2.1.1 # subscribe_event::creation
     * [Infrastructure SLAM <- Monitored Event Channel].
     */
    public final void subscribeInfraStructureSLAM() {
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: ISSLAM\n" + "Interface Name: subscribe_event\n" + "Operation Name: creation\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
    }

    /**
     * INTERACTION 2.1.1.1 #
     * negotiate/query/coordinate(SLA_native_report??)::creation
     * [InfrastructureSLAM -> Software SLAM].
     */
    public final void reportSoftwareSLAM() {
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: SWSLAM\n" + "Interface Name: negotiate/query/coordinate\n" + "Operation Name: SLA_native_report\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
    }

    /**
     * INTERACTION 3 # native_service_management::createSWService()
     * [Software SM -> Manageability's Agent].
     */
    public final void createSWServiceMA() {
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: SSM-MA\n" + "Interface Name: native_service_management\n" + "Operation Name: createSWService\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        try {
            String uuid = "testUUID";
            ServiceBuilder builder = new ServiceBuilderExtended();
            builder.setUuid(uuid);
            Settings settings = new Settings(System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "bus.properties");
            String notificationChannel = "test-MA-Sam";
            manageabilityAgentService.startServiceInstance(builder, settings, notificationChannel);
            manageabilityAgentService.getManagibilityAgentFacade(builder);
            System.out.println("--------------------------------");
        } catch (ServiceStartupException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 3.1 # publish_event::creation
     * [Manageability's Agent -> Monitored Event Channel].
     */
    public final void publishEventMonitoredEventChannel() {
        try {
            String busProps = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "bus.properties";
            PubSubManager pubSubManager1 = PubSubFactory.createPubSubManager(busProps);
            for (int i = 0; i < 20; i++) {
                Thread.sleep(2000);
                pubSubManager1.publish(new PubSubMessage(channelName, "test event"));
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 3.1.1 # subscribe_event::creation
     * [Software SLAM <- MonitoredEvent Channel].
     */
    public final void subscribeEeventMEToSWSLAM() {
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: SWSLAM<-ME\n" + "Interface Name: subscribe_event\n" + "Operation Name: creation\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        try {
            String busProps = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "bus.properties";
            PubSubManager pubSubManager1 = PubSubFactory.createPubSubManager(busProps);
            pubSubManager1.createChannel(new Channel(channelName));
            pubSubManager1.subscribe(channelName);
            LOGGER.info("*************** pubsub manager created **************************");
            pubSubManager1.addMessageListener(new MessageListener() {

                public void processMessage(final MessageEvent messageEvent) {
                    System.out.println("*************** message: " + messageEvent.getMessage().getPayload() + "**************************");
                }
            });
            for (int i = 0; i < 20; i++) {
                Thread.sleep(2000);
                pubSubManager1.publish(new PubSubMessage(channelName, "test event"));
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 3.1.1.1 # control/track/query::trackEvent
     * [Software SLAM -> BM].
     */
    public final void trackEvent() {
        try {
            GetCustomerPurchaseAuth auth = new GetCustomerPurchaseAuth();
            auth.setCustomerId(1);
            auth.setProductId(1);
            GetCustomerPurchaseAuthResponse authRes = trackWSClient.getCustomerPurchaseAuth(auth);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: SWSLAM->BM\n" + "Interface Name: contron/track/query\n" + "Operation Name: CustomerPurchaseAuth()\n" + "Input:CustomerID--1 --ProductID-- 1 " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("--------------------------------");
            System.out.println(authRes.get_return().getResponseMessage());
            AdjustmentNotificationType[] adjustType = new AdjustmentNotificationType[1];
            TrackEvent event = new TrackEvent();
            event.setViolationList(adjustType);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: SWSLAM->BM\n" + "Interface Name: contron/track/query\n" + "Operation Name: trackEvent()\n" + "Input:AdjustmentNotificationType " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("--------------------------------");
            System.out.println(trackWSClient.trackEvent(event).get_return());
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        } catch (AxisFault e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (CustomerNotFoundExceptionException e) {
            e.printStackTrace();
        } catch (ProductNotFoundExceptionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Complete Scenario Run.
     */
    @SuppressWarnings("deprecation")
    public final void runORC() {
        try {
            GetParameterList getParamReq = new GetParameterList();
            getParamReq.setGetParameterType("COUNTRY");
            GetParameterListResponse getParamResp = partyWSClient.getParameterList(getParamReq);
            GetParameterListResponseType resultParam = getParamResp.get_return();
            CreateParty partyReq = new CreateParty();
            PartyType testParty = new PartyType();
            partyReq.setType("I");
            testParty.setCurrencyId(1);
            IndividualType[] testInd = new IndividualType[2];
            testInd[0] = new IndividualType();
            testInd[0].setAddress("3");
            testInd[0].setCountryId(3);
            testInd[0].setEmail("3");
            testInd[0].setFax("0800");
            testInd[0].setFirstName("3First");
            testInd[0].setLastName("wLast");
            testInd[0].setJobdepartment("test dept");
            testInd[0].setJobtitle("tester");
            testInd[0].setLanguageId(2);
            testInd[0].setPhoneNumber("0900");
            testParty.setIndividual(testInd[0]);
            String testIndDetail = "Address-" + testInd[0].getAddress() + ",CountryId-" + testInd[0].getCountryId() + ", Email-" + testInd[0].getEmail() + ", Fax-" + testInd[0].getFax() + ", FirstName-" + testInd[0].getFirstName() + ", LastName-" + testInd[0].getLastName() + ", JobDept-" + testInd[0].getJobdepartment() + ", Job Title-" + testInd[0].getJobtitle() + ", LanguageId-" + testInd[0].getLanguageId() + ", Phone No-" + testInd[0].getPhoneNumber();
            OrganizationType testOrg = new OrganizationType();
            testOrg.setTradingName("TRAVELS AGENCY");
            testOrg.setFiscalId("1111-S");
            testOrg.setIndividuals(testInd);
            testParty.setOrganization(testOrg);
            String orgDetail = "Trading Name-" + testOrg.getTradingName() + ", FiscalId-" + testOrg.getFiscalId();
            UserType[] testUser = new UserType[1];
            testUser[0] = new UserType();
            testUser[0].setUserLogin("Integrator");
            testUser[0].setPasswd("Integrator");
            testParty.setUsers(testUser);
            partyReq.setParty(testParty);
            CreatePartyResponse partyResp = partyWSClient.createParty(partyReq);
            CreatePartyResponseType resultType = partyResp.get_return();
            LOGGER.info("PARTY ID: " + Long.parseLong(String.valueOf(resultType.getResponseCode())));
            EmParty proxyParty = bmPartyManagerService.getPartyById(Long.parseLong(String.valueOf(resultType.getResponseCode())));
            BigDecimal balance = new BigDecimal("2500");
            LOGGER.info("Update Party Credit Limit to " + balance);
            proxyParty.setNuPartyid(Long.parseLong(String.valueOf(resultType.getResponseCode())));
            proxyParty.setNuCreditLimit(balance);
            bmPartyManagerService.saveOrUpdate(proxyParty);
            EmPartyPartyrole emPartyPartyRole = new EmPartyPartyrole();
            EmPartyRole emPartyRole = new EmPartyRole();
            emPartyRole.setNuPartyroleid(new Long(3));
            EmPartyPartyroleId emPartyPartyroleId = new EmPartyPartyroleId(proxyParty.getNuPartyid(), emPartyRole.getNuPartyroleid());
            emPartyPartyRole.setId(emPartyPartyroleId);
            emPartyPartyRole.setTxStatus(Constants.PARTY_STATE_APPROVAL);
            bmPartyRoleService.saveOrUpdate(emPartyPartyRole);
            GetProducts req = new GetProducts();
            req.setCustomerID(String.valueOf(resultType.getResponseCode()));
            GetProductsResponse resp = qpcWSClient.getProducts(req);
            Product[] prod = resp.get_return();
            LOGGER.info("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: BM\n" + "Interface Name: query_product_catalog\n" + "Operation Name: getTemplates\n" + "Input\n" + "Customer ID-" + req.getCustomerID() + "\n" + "Product ID-" + prod[0].getId() + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            LOGGER.info("\n\n\n\n\n");
            GetTemplates getTemplates2 = new GetTemplates();
            getTemplates2.setCustomerId(resultType.getResponseCode());
            getTemplates2.setProductId(prod[0].getId());
            GetTemplatesResponse response = qpcWSClient.getTemplates(getTemplates2);
            LOGGER.debug("###########Response:");
            SLATemplate[] tmps = (SLATemplate[]) Base64Utils.decode(response.get_return());
            LOGGER.debug(tmps[0].toString());
            templateID = tmps[0].getUuid().getValue();
            SLATemplate template = tmps[0];
            LOGGER.debug("###########TemplateID:" + templateID);
            LOGGER.debug(tmps[0].getModelVersion());
            LOGGER.debug("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            AuthenticateUser authReq = new AuthenticateUser();
            authReq.setUserLogin("Integrator");
            authReq.setPasswd("Integrator");
            AuthenticateUserResponse authResp = partyWSClient.authenticateUser(authReq);
            LOGGER.info("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: BM\n" + "Interface Name: customer_relation\n" + "Operation Name: authenticateUser\n" + "Inputs\n" + "UserLogin:" + authReq.getUserLogin() + "\n" + "Password:" + authReq.getPasswd() + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            LOGGER.info(authResp.get_return().getResponseMessage() + "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            LOGGER.info("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            LOGGER.debug("After adding party element dynamically");
            LOGGER.info("Business SLA Template\n" + template.toString());
            LOGGER.debug("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            LOGGER.info("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: GSLAM\n" + "Interface Name: negotiate\n" + "Operation Name: negotiate\n" + "Inputs\n" + "SLATemplate with ID:" + templateID + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            LOGGER.debug("\n\n\n\n\n");
            ISyntaxConverter sc = ((java.util.Hashtable<ISyntaxConverter.SyntaxConverterType, ISyntaxConverter>) bslamContext.getSyntaxConverters()).get(ISyntaxConverter.SyntaxConverterType.SLASOISyntaxConverter);
            String initiateNegResp = sc.getNegotiationClient(syntaxConverterNegotiationWSURL).initiateNegotiation(template);
            LOGGER.debug(initiateNegResp);
            org.slasoi.slamodel.sla.SLATemplate[] temps = sc.getNegotiationClient(syntaxConverterNegotiationWSURL).negotiate(initiateNegResp, template);
            LOGGER.info("Availabe Templates after Negotiation");
            for (SLATemplate slat : temps) {
                if (slat != null) {
                    LOGGER.info(slat.getUuid());
                }
            }
            SLA finalSLA = null;
            if (temps != null && temps.length > 0) {
                if (temps[0] != null) {
                    finalSLA = sc.getNegotiationClient(syntaxConverterNegotiationWSURL).createAgreement(initiateNegResp, temps[0]);
                }
            }
            LOGGER.info("AgreedSLA :" + finalSLA.getUuid() + "\n" + "Time: " + finalSLA.getAgreedAt() + "\n" + "TemplateID: " + finalSLA.getTemplateId() + "\n" + "ModelVersion: " + finalSLA.getModelVersion() + "\n");
            LOGGER.info("\n\n\n\n\n");
            LOGGER.debug(finalSLA.toString());
            LOGGER.info("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: SSM-MA\n" + "Interface Name: native_service_management\n" + "Operation Name: createSWService\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            ServiceBuilder builder = new ServiceBuilderExtended();
            builder.setUuid(finalSLA.getUuid().toString());
            Settings settings = new Settings(System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "bus.properties");
            String notificationChannel = "test-MA-Sam";
            manageabilityAgentService.startServiceInstance(builder, settings, notificationChannel);
            IManageabilityAgentFacade facade = manageabilityAgentService.getManagibilityAgentFacade(builder);
            UpdateBinding uBinding = new UpdateBinding(builder);
            facade.executeAction(uBinding);
            System.out.println("--------------------------------");
            configProps.load(new FileInputStream(System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "config.properties"));
            configProps.setProperty("slaUuid", finalSLA.getUuid().getValue());
            configProps.store(new java.io.FileOutputStream(System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "config.properties"), "Provisioned SLA UUID");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (AxisFault e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        } catch (OperationNotPossibleException e) {
            e.printStackTrace();
        } catch (OperationInProgressException e) {
            e.printStackTrace();
        } catch (SLACreationException e) {
            e.printStackTrace();
        } catch (InvalidNegotiationIDException e) {
            e.printStackTrace();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }
}
