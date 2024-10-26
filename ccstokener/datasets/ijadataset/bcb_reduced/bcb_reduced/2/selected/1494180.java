package org.integration.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
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
import org.slasoi.gslam.client.syntaxconverter.webservice.GSLAMSyntaxConverterWSControlStub;
import org.slasoi.gslam.client.syntaxconverter.webservice.GSLAMSyntaxConverterWSControlStub.SetPolicies;
import org.slasoi.gslam.client.syntaxconverter.webservice.GSLAMSyntaxConverterWSControlStub.SetPoliciesResponse;
import org.slasoi.gslam.core.context.GenericSLAManagerUtils;
import org.slasoi.gslam.core.context.SLAManagerContext;
import org.slasoi.gslam.core.context.GenericSLAManagerServices.CreateContextGenericSLAManagerException;
import org.slasoi.gslam.core.context.GenericSLAManagerUtils.GenericSLAManagerUtilsException;
import org.slasoi.gslam.core.context.SLAManagerContext.SLAManagerContextException;
import org.slasoi.gslam.core.negotiation.INegotiation;
import org.slasoi.gslam.core.negotiation.ISyntaxConverter;
import org.slasoi.gslam.core.negotiation.INegotiation.InvalidNegotiationIDException;
import org.slasoi.gslam.core.negotiation.INegotiation.OperationInProgressException;
import org.slasoi.gslam.core.negotiation.INegotiation.OperationNotPossibleException;
import org.slasoi.gslam.core.negotiation.INegotiation.SLACreationException;
import org.slasoi.gslam.core.negotiation.INegotiation.TerminationReason;
import org.slasoi.gslam.core.negotiation.SLARegistry.IQuery;
import org.slasoi.gslam.core.negotiation.SLARegistry.InvalidStateException;
import org.slasoi.gslam.core.negotiation.SLARegistry.InvalidUUIDException;
import org.slasoi.gslam.core.negotiation.SLARegistry.SLAState;
import org.slasoi.gslam.core.negotiation.SLARegistry.SLAStateInfo;
import org.slasoi.gslam.core.negotiation.SLATemplateRegistry.Exception;
import org.slasoi.gslam.core.poc.PlanningOptimization;
import org.slasoi.gslam.core.poc.PlanningOptimization.IAssessmentAndCustomize;
import org.slasoi.gslam.syntaxconverter.SLASOITemplateParser;
import org.slasoi.infrastructure.servicemanager.exceptions.DescriptorException;
import org.slasoi.infrastructure.servicemanager.exceptions.UnknownIdException;
import org.slasoi.infrastructure.servicemanager.types.CapacityResponseType;
import org.slasoi.infrastructure.servicemanager.types.ProvisionRequestType;
import org.slasoi.infrastructure.servicemanager.types.ReservationResponseType;
import org.slasoi.models.scm.Dependency;
import org.slasoi.models.scm.Landscape;
import org.slasoi.models.scm.ServiceBinding;
import org.slasoi.models.scm.ServiceBuilder;
import org.slasoi.models.scm.ServiceConstructionModel;
import org.slasoi.models.scm.ServiceConstructionModelFactory;
import org.slasoi.models.scm.ServiceImplementation;
import org.slasoi.models.scm.ServiceType;
import org.slasoi.models.scm.extended.ServiceBuilderExtended;
import org.slasoi.seval.prediction.service.EvaluationMode;
import org.slasoi.seval.prediction.service.IEvaluationResult;
import org.slasoi.seval.prediction.service.ISoftwareServiceEvaluation;
import org.slasoi.seval.prediction.exceptions.UnboundDependencyException;
import org.slasoi.seval.prediction.exceptions.UnsupportedTermException;
import org.slasoi.seval.prediction.service.impl.SoftwareServiceEvaluator;
import org.slasoi.seval.repository.exceptions.ModelNotFoundException;
import org.slasoi.slamodel.primitives.ID;
import org.slasoi.slamodel.primitives.STND;
import org.slasoi.slamodel.primitives.UUID;
import org.slasoi.slamodel.sla.Party;
import org.slasoi.slamodel.sla.SLA;
import org.slasoi.slamodel.sla.SLATemplate;
import org.slasoi.slamodel.vocab.sla;
import org.slasoi.softwareservicemanager.IProvisioning;
import org.slasoi.softwareservicemanager.ISoftwareServiceManagerFacade;
import org.slasoi.softwareservicemanager.exceptions.BookingException;
import org.slasoi.softwareservicemanager.exceptions.ReservationException;
import org.slasoi.softwareservicemanager.provisioning.ProvisionServiceStub;
import org.slasoi.softwareservicemanager.provisioning.ServiceState;

/**
 * Negotiation Scenario.
 *
 * @author khurshid
 *
 */
public class NegotiationScenario {

    /** negotiation scenario instance.**/
    private static NegotiationScenario instance = null;

    /** business manager query product WS client. **/
    private BusinessManager_QueryProductCatalogStub qpcWSClient = null;

    /** business manager party WS client.**/
    private BusinessManager_PartyStub partyWSClient = null;

    /** syntax converter control WS client.**/
    private GSLAMSyntaxConverterWSControlStub controlWSClient = null;

    /** infrastructure SLA manager context.**/
    private SLAManagerContext isslamContext = null;

    /** software SLA manager context.**/
    private SLAManagerContext sslamContext = null;

    /** business SLA manager context.**/
    private SLAManagerContext bslamContext = null;

    /** reservation boolean flag.**/
    private Boolean reservationFlag = false;

    /****/
    private ServiceState state = null;

    /** business manager party role WS client.**/
    private static PartyPartyRoleManager bmPartyRoleService = null;

    /** business manager party manager WS client.**/
    private static PartyManager bmPartyManagerService = null;

    /** web service properties holder.**/
    private static Properties webServiceProps = new Properties();

    /** business manager query product WS URL holder.**/
    private String bmQueryProductWSURL = null;

    /** business manager party WS URL holder.**/
    private String bmPartyWSURL = null;

    /** syntax converter  negotiation WS URL holder.**/
    private String sycNegotiationWSURL = null;

    /** syntax converter control WS URL holder.**/
    private String sycControlWSURL = null;

    /** holder for template ID.**/
    private String templateID = null;

    /** ORC template object.**/
    private static org.slasoi.slamodel.sla.SLATemplate orcTemplate;

    /** LOGGER.**/
    private static final Logger LOGGER = Logger.getLogger(NegotiationScenario.class);

    /**
     * constructor.
     * @param gslamServices GenericSLAManagerUtils
     * @param osgiContext bundleContext
     * @throws CreateContextGenericSLAManagerException exception
     * @throws GenericSLAManagerUtilsException exception
     * @throws SLAManagerContextException exception
     */
    public NegotiationScenario(final GenericSLAManagerUtils gslamServices, final BundleContext osgiContext) throws CreateContextGenericSLAManagerException, GenericSLAManagerUtilsException, SLAManagerContextException {
        try {
            webServiceProps.load(new FileInputStream(System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "services.properties"));
            bmQueryProductWSURL = webServiceProps.getProperty("BM_QueryProductCatalogWS_URL");
            bmPartyWSURL = webServiceProps.getProperty("BM_PartyWS_URL");
            sycNegotiationWSURL = webServiceProps.getProperty("GSLAM_SyntaxConverterWSNegotiation_URL");
            sycControlWSURL = webServiceProps.getProperty("GSLAM_SyntaxConverterWSControl_URL");
            qpcWSClient = new BusinessManager_QueryProductCatalogStub(bmQueryProductWSURL);
            partyWSClient = new BusinessManager_PartyStub(bmPartyWSURL);
            controlWSClient = new GSLAMSyntaxConverterWSControlStub(sycControlWSURL);
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
     * Get instance.
     * @param gslamServices GenericSLAManagerUtils
     * @param partyroleManager PartyPartyRoleManager
     * @param partyManager PartyManager
     * @param osgiContext BundleContext
     * @return Negotiation scenario instance
     * @throws CreateContextGenericSLAManagerException exception
     * @throws GenericSLAManagerUtilsException exception.
     * @throws SLAManagerContextException exception
     */
    public static NegotiationScenario getInstance(final GenericSLAManagerUtils gslamServices, final PartyPartyRoleManager partyroleManager, final PartyManager partyManager, final BundleContext osgiContext) throws CreateContextGenericSLAManagerException, GenericSLAManagerUtilsException, SLAManagerContextException {
        bmPartyRoleService = partyroleManager;
        bmPartyManagerService = partyManager;
        if (instance == null) {
            instance = new NegotiationScenario(gslamServices, osgiContext);
        }
        return instance;
    }

    /**
     * INTERACTION 1,2,2 # setPolicies (GSLAM).
     */
    public final void setPolicies() {
        try {
            SetPolicies policyData = new SetPolicies();
            policyData.setPolicies("NegotiationPolicy");
            SetPoliciesResponse resp = controlWSClient.setPolicies(policyData);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: GSLAM\n" + "Interface Name: Control\n" + "Operation Name: setPolicies()\n" + "Input:Policy" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("setPolicies('NegotiationPolicy')" + resp.get_return() + "+ ToString--" + resp.toString() + "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            SetPolicies policyData2 = new SetPolicies();
            policyData2.setPolicies("AdjustmentPolicy");
            resp = controlWSClient.setPolicies(policyData2);
            System.out.println("setPolicies('AdjustmentPolicy')" + resp.get_return() + "+ ToString--" + resp.toString() + "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            SetPolicies policyData3 = new SetPolicies();
            policyData3.setPolicies("ProviderControlPolicy");
            resp = controlWSClient.setPolicies(policyData3);
            System.out.println("setPolicies('ProviderControlPolicy')" + resp.get_return() + "+ ToString--" + resp.toString() + "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 4 # customer_relation::register() (BM).
     */
    public final void register() {
        try {
            GetParameterList getParamReq = new GetParameterList();
            getParamReq.setGetParameterType("COUNTRY");
            GetParameterListResponse getParamResp = partyWSClient.getParameterList(getParamReq);
            GetParameterListResponseType resultParam = getParamResp.get_return();
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: BM\n" + "Interface Name: customer_relation\n" + "Operation Name: getParameterList()\n" + "Input:parameterType:" + getParamReq.getGetParameterType() + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println(resultParam.getResponseMessage() + "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
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
            testUser[0].setUserLogin("test");
            testUser[0].setPasswd("test");
            testParty.setUsers(testUser);
            partyReq.setParty(testParty);
            String userDetails = "UserLogin-" + testUser[0].getUserLogin() + ", Password-" + testUser[0].getPasswd();
            CreatePartyResponse partyResp = partyWSClient.createParty(partyReq);
            CreatePartyResponseType resultType = partyResp.get_return();
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: BM\n" + "Interface Name: customer_relation\n" + "Operation Name: createParty\n" + "Inputs:\n " + "Type" + partyReq.getType() + "\n" + "Individual:" + testIndDetail + "\n" + "Organization:" + orgDetail + "\n" + "User:" + userDetails + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println(resultType.getResponseMessage() + "--------Code" + resultType.getResponseCode() + "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        } catch (AxisFault e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 5 # query_product_catalog::getProducts() (BM).
     */
    public final void getProducts() {
        try {
            GetProducts req = new GetProducts();
            req.setCustomerID("1");
            GetProductsResponse resp = qpcWSClient.getProducts(req);
            Product[] prod = resp.get_return();
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: BM\n" + "Interface Name: query_product_catalog\n" + "Operation Name: get_products\n" + "Input:Customer_ID-" + req.getCustomerID() + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            for (Product p : prod) {
                System.out.println(p.getBrand() + "--" + p.getCategory() + "--" + p.getDescription() + "--" + p.getId() + "--" + p.getName() + "\n\n");
            }
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        } catch (AxisFault e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 6 # query_product_catalog::getTemplates (BM).
     * @return template string
     */
    public final String getTemplates() {
        try {
            System.out.println("###########Into Get template method");
            GetTemplates getTemplates2 = new GetTemplates();
            getTemplates2.setCustomerId(1);
            getTemplates2.setProductId(1);
            GetTemplatesResponse response = qpcWSClient.getTemplates(getTemplates2);
            System.out.println("###########Response:");
            SLATemplate[] temps = (SLATemplate[]) Base64Utils.decode(response.get_return());
            System.out.println(temps[0].toString());
            templateID = temps[0].getUuid().getValue();
            System.out.println("###########TemplateID:" + templateID);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return templateID;
    }

    /**
     * INTERACTION 6.1 control/track/query::query SLARegistry(BSLAM).
     */
    public final void querySLARegistry() {
        try {
            IQuery query = sslamContext.getSLARegistry().getIQuery();
            UUID uuID = new UUID("AG-2");
            SLAStateInfo[] stateHistory = query.getStateHistory(uuID, false);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: GSLAM\n" + "Interface Name: control/track/query\n" + "Operation Name: SLARegistry::query\n" + "Input" + "UUIDs-" + uuID.toString() + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("testStateHistory (" + stateHistory.length + ")");
            System.out.println("--------------------------------");
            for (SLAStateInfo ssi : stateHistory) {
                System.out.println(ssi);
            }
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            UUID[] slaUUID = new UUID[] { new UUID("AG-1"), new UUID("AG-2"), new UUID("AG-3") };
            SLA[] slas = query.getSLA(slaUUID);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: GSLAM\n" + "Interface Name: control/track/query\n" + "Operation Name: SLARegistry::query->getSLAs()\n" + "Inputs\n" + "UUIDs-" + slaUUID[0].toString() + "," + slaUUID[1].toString() + "," + slaUUID[2].toString() + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            for (SLA s : slas) {
                System.out.println("************** SLAs Description ****************");
                System.out.println(s.getTemplateId());
            }
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            UUID[] slAsByState = query.getSLAsByState(new SLAState[] { SLAState.WARN, SLAState.EXPIRED }, false);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: GSLAM\n" + "Interface Name: control/track/query\n" + "Operation Name:" + " SLARegistry::query->getSLAsByState\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            if (slAsByState == null) {
                System.out.println("testSLAsByState (empty)");
                return;
            }
            System.out.println("testSLAsByState (" + slAsByState.length + ")");
            System.out.println("--------------------------------");
            for (UUID smi : slAsByState) {
                System.out.println(smi.getValue());
            }
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        } catch (InvalidStateException e) {
            e.printStackTrace();
        } catch (InvalidUUIDException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 7 # customer_relation::authenticateUser (BM).
     */
    public final void authenticateUser() {
        try {
            AuthenticateUser authReq = new AuthenticateUser();
            authReq.setUserLogin("sm");
            authReq.setPasswd("sm");
            AuthenticateUserResponse authResp = partyWSClient.authenticateUser(authReq);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: BM\n" + "Interface Name: customer_relation\n" + "Operation Name: authenticateUser\n" + "Inputs\n" + "UserLogin:" + authReq.getUserLogin() + "\n" + "Password:" + authReq.getPasswd() + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println(authResp.get_return().getResponseMessage() + "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        } catch (AxisFault e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 8# negotiate/coordinate::negotiate (GSLAM).
     */
    @SuppressWarnings("deprecation")
    public final void negotiateGSLAM() {
        try {
            ISyntaxConverter sc = ((java.util.Hashtable<ISyntaxConverter.SyntaxConverterType, ISyntaxConverter>) sslamContext.getSyntaxConverters()).get(ISyntaxConverter.SyntaxConverterType.SLASOISyntaxConverter);
            System.out.println("Invoking SyntaxConverter Service on \n" + sycNegotiationWSURL);
            SLATemplate template = sslamContext.getSLATemplateRegistry().getSLATemplate(new UUID("ORC_SW_SLAT"));
            LOGGER.info("Software SLA Template\n" + template.toString());
            String initiateNegResp = sc.getNegotiationClient(sycNegotiationWSURL).initiateNegotiation(template);
            System.out.println("#############Initiate id:  " + initiateNegResp);
            org.slasoi.slamodel.sla.SLATemplate[] temps = sc.getNegotiationClient(sycNegotiationWSURL).negotiate(initiateNegResp, template);
            System.out.println("Availabe Templates after Negotiation");
            LOGGER.debug("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: GSLAM\n" + "Interface Name: negotiate\n" + "Operation Name: createAgreement\n" + "Inputs\n" + "initiateNegotiationReponse\n" + "SLATemplate with ID:" + templateID + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            LOGGER.debug("\n\n\n\n\n");
            for (SLATemplate slat : temps) {
                if (slat != null) {
                    System.out.println(slat.getUuid());
                }
            }
            SLA finalSLA = null;
            if (temps != null && temps.length > 0) {
                if (temps[0] != null) {
                    finalSLA = sc.getNegotiationClient(sycNegotiationWSURL).createAgreement(initiateNegResp, temps[0]);
                }
            }
            LOGGER.info("AgreedSLA :" + finalSLA.getUuid() + "\n" + "Time: " + finalSLA.getAgreedAt() + "\n" + "TemplateID: " + finalSLA.getTemplateId() + "\n" + "ModelVersion: " + finalSLA.getModelVersion() + "\n");
            LOGGER.info("\n\n\n\n\n");
        } catch (AxisFault e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 8.1 # checkCustomer (BM).
     */
    @SuppressWarnings("deprecation")
    public final void checkCustomer() {
        try {
            org.slasoi.slamodel.sla.SLATemplate slaTemplate = new org.slasoi.slamodel.sla.SLATemplate();
            slaTemplate.setUuid(new UUID("SLATTest"));
            slaTemplate.setDescr("Desc");
            Party[] parties = new Party[1];
            Party party = new Party();
            party.setId(new ID("id"));
            party.setDescr("party");
            slaTemplate.setParties(parties);
            boolean valid = bslamContext.getAuthorization().checkAccess(slaTemplate);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: BM\n" + "Interface Name: control/track/query\n" + "Operation Name: checkCustomer\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("The customer validation results from BM is " + valid);
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 8.2 # negotiate/query/coordinate::query SLATemplates SWSLAM.
     */
    public final void querySoftwareSLATemplates() {
        try {
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: SWSLAM(SLAT Registry)\n" + "Interface Name: negotiate/query/coordinate \n" + "Operation Name: query\n" + "Description: Query Software SLA Templates\n" + "Input:UUID('SWAG-1')\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            org.slasoi.slamodel.sla.SLATemplate slat = sslamContext.getSLATemplateRegistry().getSLATemplate(new UUID("SWAG-1"));
            System.out.println(slat.getUuid() + "--" + slat.getModelVersion() + "--" + slat.getUuid() + "\n\n");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 8.3 # negotiate/query/coordinate::negotiate (SWSLAM).
     */
    public final void negotiateSWSLAM() {
    }

    /**
     * INTERACTION 8.3.1 # prepare_software_service::query (SSM).
     * @param iSSMFacade ISoftwareManagerFacade.
     */
    public final void querySSM(final ISoftwareServiceManagerFacade iSSMFacade) {
        iSSMFacade.getBookingManager();
        String landscapePath = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "software-servicemanager" + System.getProperty("file.separator") + "ORC.scm";
        IProvisioning provisioningManager = new ProvisionServiceStub();
        Landscape landscape = (Landscape) ServiceConstructionModel.loadFromXMI(landscapePath);
        iSSMFacade.setLandscape(landscape);
        iSSMFacade.setProvisioningManager(provisioningManager);
        List<ServiceType> listSTypes = iSSMFacade.queryServiceTypes();
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: SSM\n" + "Interface Name: prepare_software_service\n" + "Operation Name: query\n" + "Input:Landscape\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        for (int i = 0; i < listSTypes.size(); i++) {
            System.out.println("ServiceType::description: " + listSTypes.get(i).getDescription() + "\nServiceType::ID: " + listSTypes.get(i).getID() + "\nServiceType::Name: " + listSTypes.get(i).getServiceTypeName() + "\nServiceType::interface0: " + listSTypes.get(i).getInterfaces(i));
        }
        List<ServiceImplementation> listImplT1 = iSSMFacade.queryServiceImplementations(listSTypes.get(0));
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        for (int i = 0; i < listImplT1.size(); i++) {
            System.out.println("ServiceTypeImplementations::description: " + listImplT1.get(i).getDescription() + "\nServiceTypeImplementations::ID: " + listImplT1.get(i).getID() + "\nServiceTypeImplementations::Name: " + listImplT1.get(i).getServiceImplementationName() + "\nServiceTypeImplementations::Version: " + listImplT1.get(i).getVersion() + "\nServiceTypeImplementations::" + "MonitoringFeaturesLength: " + listImplT1.get(i).getComponentMonitoringFeaturesLength());
        }
    }

    /**
     * INTERACTION 8.3.2 # negotiate/query/coordinate::query.
     * SLATemplates(ISSLAM)
     */
    public final void queryISSLAM() {
        try {
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: ISSLAM(SLAT Registry)\n" + "Interface Name: negotiate/query/coordinate \n" + "Operation Name: query\n" + "Description: Query Infrastructure SLA Templates\n" + "Input: 'UUID('ISAG-1')\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            org.slasoi.slamodel.sla.SLATemplate slat = isslamContext.getSLATemplateRegistry().getSLATemplate(new UUID("ISAG-1"));
            System.out.println("UUID-" + slat.getUuid() + "," + "Model Version-" + slat.getModelVersion() + "\n\n");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 8.3.3 negotiate/query/coordinate::negotiate (ISSLAM).
     */
    public final void negotiateISSLAM() {
        try {
            FileReader read;
            String row;
            StringBuffer sb = new StringBuffer();
            System.out.println("***New SLAT***");
            read = new FileReader(System.getenv("SLASOI_HOME") + File.separator + "infrastructure-slamanager" + File.separator + "planning-optimization" + File.separator + "A4" + File.separator + "A4_SLATemplate(New).xml");
            BufferedReader br = new BufferedReader(read);
            sb = new StringBuffer();
            while ((row = br.readLine()) != null) {
                sb.append(row);
            }
            SLASOITemplateParser slasoieTemplatParser = new SLASOITemplateParser();
            SLATemplate slat;
            slat = slasoieTemplatParser.parseTemplate(sb.toString());
            SLATemplate[] run = isslamContext.getPlanningOptimization().getIAssessmentAndCustomize().negotiate("", slat);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: ISSLAM(POC)\n" + "Interface Name: negotiate/query/coordinate\n" + "Operation Name: negotiate\n" + "Input:(1, empty SLATemplate) " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("--------------------------------");
            for (org.slasoi.slamodel.sla.SLATemplate s : run) {
                System.out.println(s.getUuid() + "--" + s.getModelVersion() + "--" + s.getUuid() + "\n\n");
            }
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 8.3.3.1 prepare_infrastructure_service::reserve (ISM).
     * @param ismServices IsmOcciService
     */
    public final void queryISM(final org.slasoi.ism.occi.IsmOcciService ismServices) {
        try {
            List<CapacityResponseType> capacityList = ismServices.queryCapacity();
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: ISM\n" + "Interface Name: prepare_infrastructure_service\n" + "Operation Name: query\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            for (CapacityResponseType capacity : capacityList) {
                System.out.println("Capacity : " + capacity.toString());
            }
        } catch (DescriptorException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 8.3.4 # evaluate.
     * @param seval IServiceEvaluation
     */
    public final void evaluate(final ISoftwareServiceEvaluation seval) {
        System.out.println("InteractionTest.testEvaluate() START");
        SoftwareServiceEvaluator evaluator = initServiceEvaluator();
        SLATemplate customerRequestSLATemplate;
        try {
            customerRequestSLATemplate = initCusterSLATemplate();
        } catch (java.lang.Exception e) {
            LOGGER.error("customer SLA Template could not be loaded", e);
            return;
        }
        Set<ServiceBuilder> builders = initServiceBuilders();
        Set<IEvaluationResult> results = new HashSet<IEvaluationResult>();
        try {
            results = evaluator.evaluate(builders, customerRequestSLATemplate);
        } catch (UnsupportedTermException e) {
            e.printStackTrace();
        } catch (UnboundDependencyException e) {
            e.printStackTrace();
        } catch (ModelNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "Component Name: Service Evaluation\n" + "Interface Name: evaluate\n" + "Operation Name: evaluate\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n" + "####################################################" + "#################################################\n");
        IEvaluationResult temp = results.iterator().next();
        System.out.println(temp.toString());
        System.out.println("InteractionTest.testEvaluate() SUCCESS");
    }

    /**
     * Init the customer sla template that describes the requested
     * service.
     * @return The initialized software service template.
     * @throws java.lang.Exception Unable to load the software SLA template
     */
    private SLATemplate initCusterSLATemplate() throws java.lang.Exception {
        String slaFilePath = System.getenv("SLASOI_HOME") + File.separator + "software-servicemanager" + File.separator + "orc" + File.separator + "infrastructure-templates" + File.separator + "software_SLA_template.xml";
        return initSLATemplate(slaFilePath);
    }

    /**
     * Initialize the evaluation component before its use.
     * 
     * @return The prepared software service evaluation component.
     */
    private SoftwareServiceEvaluator initServiceEvaluator() {
        SoftwareServiceEvaluator evaluator = new SoftwareServiceEvaluator();
        evaluator.setEvaluationMode(EvaluationMode.Auto);
        evaluator.setEvaluationServerEndpoint("http://localhost:8082/services");
        return evaluator;
    }

    /**
     * Prepares a list of service builders.
     *
     * The service builders inform about possible realizations of the target service that is requested through the
     * SLATemplate (which has been created in Step 2). Thereby, a realization includes information about the internal
     * structure of the service, as well as the properties of the required external software services (if any) and
     * infrastructure services. This information stems from the service landscape and is provided to the service
     * evaluation component via its caller, the software POC.
     *
     * @return A set of initialized service builders.
     */
    private Set<ServiceBuilder> initServiceBuilders() {
        Set<ServiceBuilder> builders = new HashSet<ServiceBuilder>();
        ServiceConstructionModelFactory factory = ServiceConstructionModel.getFactory();
        String landscapePath = System.getenv("SLASOI_HOME") + File.separator + "software-servicemanager" + File.separator + "ORC.scm";
        Landscape landscape = (Landscape) ServiceConstructionModel.loadFromXMI(landscapePath);
        String slaFilePath = System.getenv("SLASOI_HOME") + File.separator + "software-servicemanager" + File.separator + "orc" + File.separator + "infrastructure-templates" + File.separator + "infrastructure_SLA_template.xml";
        SLATemplate infrastructureSLATemplate;
        try {
            infrastructureSLATemplate = initSLATemplate(slaFilePath);
        } catch (java.lang.Exception e) {
            throw new RuntimeException("Failed to load the infrastructure template ", e);
        }
        ServiceBinding binding = factory.createServiceBinding();
        binding.setSlaTemplate(infrastructureSLATemplate);
        Dependency dep = landscape.getImplementations(0).getDependencies().get(0);
        ServiceBuilderExtended serviceBuilder = new ServiceBuilderExtended();
        serviceBuilder.setUuid("ORC_AllInOne");
        serviceBuilder.setImplementation(landscape.getImplementations(0));
        serviceBuilder.addBinding(dep, infrastructureSLATemplate);
        landscape.addBuilder(serviceBuilder);
        builders.add(serviceBuilder);
        return builders;
    }

    /**
     * Build up the general SLA Template object based on the test infrastructure sla file.
     * @param slaFilePath The sla template file to load.
     * @return The prepared sla template.
     * @throws java.lang.Exception e
     * @throws Exception
     */
    private SLATemplate initSLATemplate(final String slaFilePath) throws java.lang.Exception {
        StringBuilder sb = new StringBuilder();
        String line = new String();
        BufferedReader reader = null;
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(slaFilePath), "UTF-8"));
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        SLATemplate slaTemplate = null;
        if (this.sslamContext != null) {
            java.util.Hashtable<ISyntaxConverter.SyntaxConverterType, ISyntaxConverter> systaxconverterslam = this.sslamContext.getSyntaxConverters();
            ISyntaxConverter sc = systaxconverterslam.get(ISyntaxConverter.SyntaxConverterType.SLASOISyntaxConverter);
            slaTemplate = (SLATemplate) (sc.parseSLATemplate(sb.toString()));
        } else {
            throw new NullPointerException("Syntax converter not initialized properly. [this.sslamContext: " + this.sslamContext + "]");
        }
        return slaTemplate;
    }

    /**
     * INTERACTION 8.3.5.
     */
    public final void negotiateISSLAMSecond() {
        try {
            String xmlFile2Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "negotiate.xml";
            String soapAction = "";
            URL url;
            url = new URL(sycNegotiationWSURL);
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
        }
    }

    /**
     * INTERACTION 8.3.5.1 prepare_infrastructure_service::reserve (ISM).
     * @param ismServices IsmOcciService.
     */
    public final void reserveISM(final org.slasoi.ism.occi.IsmOcciService ismServices) {
        try {
            ProvisionRequestType provisionRequest = ismServices.createProvisionRequestType(ismServices.getOsregistry().getDefaultCategory().getTerm(), ismServices.getMetricregistry().getDefaultCategory().getTerm(), ismServices.getLocregistry().getDefaultCategory().getTerm(), "", "notification URI");
            ReservationResponseType reservationResponseType = null;
            reservationResponseType = ismServices.reserve(provisionRequest);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: ISM\n" + "Interface Name: prepare_infrastructure_service\n" + "Operation Name: reserve\n" + "Input:Type \n" + provisionRequest.toString() + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("reservationResponseType Infrastructure ID-" + reservationResponseType.getInfrastructureID() + "\n\n");
        } catch (DescriptorException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 8.3.6 # prepare_software_service::reserve [SSM].
     * @param iSSMFacade ISoftwareServiceManagerFacade
     */
    public final void reserveSoftwareSM(final ISoftwareServiceManagerFacade iSSMFacade) {
        try {
            iSSMFacade.getBookingManager();
            String landscapePath = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "software-servicemanager" + System.getProperty("file.separator") + "ORC.scm";
            IProvisioning provisioningManager = new ProvisionServiceStub();
            Landscape landscape = (Landscape) ServiceConstructionModel.loadFromXMI(landscapePath);
            iSSMFacade.setLandscape(landscape);
            iSSMFacade.setProvisioningManager(provisioningManager);
            List<ServiceType> listSTypes = iSSMFacade.queryServiceTypes();
            for (int i = 0; i < listSTypes.size(); i++) {
                System.out.println("ServiceType::description: " + listSTypes.get(i).getDescription() + "\nServiceType::ID: " + listSTypes.get(i).getID() + "\nServiceType::Name: " + listSTypes.get(i).getServiceTypeName() + "\nServiceType::interface0: " + listSTypes.get(i).getInterfaces(i));
            }
            List<ServiceImplementation> listImplT1 = iSSMFacade.queryServiceImplementations(listSTypes.get(0));
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            for (int i = 0; i < listImplT1.size(); i++) {
                System.out.println("ServiceTypeImplementations::description: " + listImplT1.get(i).getDescription() + "\nServiceTypeImplementations::ID: " + listImplT1.get(i).getID() + "\nServiceTypeImplementations::Name: " + listImplT1.get(i).getServiceImplementationName() + "\nServiceTypeImplementations::Version: " + listImplT1.get(i).getVersion() + "\nServiceTypeImplementations::" + "MonitoringFeaturesLength: " + listImplT1.get(i).getComponentMonitoringFeaturesLength());
            }
            iSSMFacade.capacityCheck(listImplT1.get(0));
            ServiceBuilder builder = iSSMFacade.createBuilder(listImplT1.get(0));
            Date startTime = new Date();
            Date stopTime = new Date();
            startTime.setTime(startTime.getTime() + 1000);
            stopTime.setTime(stopTime.getTime() + 10000);
            reservationFlag = iSSMFacade.reserve(builder, startTime, stopTime);
            state = iSSMFacade.queryServiceStatus(builder);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: SSM\n" + "Interface Name: prepare_software_service\n" + "Operation Name: reserve\n" + "Input:(builder, startTime, stopTime) " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            System.out.println("Reserve::ServiceState: " + state.name() + "- code: " + state.hashCode());
        } catch (ReservationException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 8.4 # ??::customize [BSLAM -> BM].
     */
    @SuppressWarnings("deprecation")
    public final void customize() {
        try {
            IAssessmentAndCustomize bslamAssessCustomise = bslamContext.getPlanningOptimization().getIAssessmentAndCustomize();
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: BM\n" + "Interface Name: IAssessmentAndCustomize\n" + "Operation Name: negotiate,createAgreement,terminate\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            org.slasoi.slamodel.sla.SLATemplate slaTemplate = new org.slasoi.slamodel.sla.SLATemplate();
            slaTemplate.setUuid(new UUID("SLATTest"));
            slaTemplate.setDescr("Desc");
            Party[] parties = new Party[1];
            Party party = new Party();
            party.setId(new ID("id"));
            party.setDescr("party");
            parties[0] = party;
            slaTemplate.setParties(parties);
            String negotiationID = java.util.UUID.randomUUID().toString();
            System.out.println("NegotiationID:" + negotiationID);
            org.slasoi.slamodel.sla.SLATemplate[] templates = bslamAssessCustomise.negotiate(negotiationID, slaTemplate);
            System.out.println("template returned:" + templates.length);
            UUID slaid = null;
            if (templates.length > 0) {
                System.out.println("tamplateID: " + templates[0].getUuid().getValue());
                System.out.println("Call CreateAgreement operation");
                SLA finalSLA = bslamAssessCustomise.createAgreement(negotiationID, templates[0]);
                System.out.println("SLAID: " + finalSLA.getUuid().getValue());
                slaid = finalSLA.getUuid();
            }
            List<TerminationReason> terminationReason = new ArrayList<TerminationReason>();
            terminationReason.add(TerminationReason.BUSINESS_DECISION);
            bslamAssessCustomise.terminate(slaid, terminationReason);
            System.out.println("Return null if the negotiationID does not exist");
            SLA sla = bslamAssessCustomise.createAgreement("id", slaTemplate);
            System.out.println("SLA:" + sla);
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 9 # negotiate/coordinate::createAgreement [GSLAM].
     */
    public final void createAgreementGSLAM() {
        try {
            String xmlFile2Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "createAgreement.xml";
            String soapAction = "";
            URL url;
            url = new URL(sycNegotiationWSURL);
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
            System.out.println(response.toString());
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: GSLAM\n" + "Interface Name: negotiate/coordinage\n" + "Operation Name: createAgreement\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 9.1 # ??::assess [BSLAM -> BM].
     */
    @SuppressWarnings("deprecation")
    public final void assess() {
        try {
            IAssessmentAndCustomize bslamAssessCustomise = bslamContext.getPlanningOptimization().getIAssessmentAndCustomize();
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: BM\n" + "Interface Name: IAssessmentAndCustomize\n" + "Operation Name: negotiate,createAgreement,terminate\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            org.slasoi.slamodel.sla.SLATemplate slaTemplate = new org.slasoi.slamodel.sla.SLATemplate();
            slaTemplate.setUuid(new UUID("SLATTest"));
            slaTemplate.setDescr("Desc");
            Party[] parties = new Party[1];
            Party party = new Party();
            party.setId(new ID("id"));
            party.setDescr("party");
            parties[0] = party;
            slaTemplate.setParties(parties);
            String negotiationID = java.util.UUID.randomUUID().toString();
            System.out.println("NegotiationID:" + negotiationID);
            org.slasoi.slamodel.sla.SLATemplate[] templates = bslamAssessCustomise.negotiate(negotiationID, slaTemplate);
            System.out.println("template returned:" + templates.length);
            UUID slaid = null;
            if (templates.length > 0) {
                System.out.println("tamplateID: " + templates[0].getUuid().getValue());
                System.out.println("Call CreateAgreement operation");
                SLA finalSLA = bslamAssessCustomise.createAgreement(negotiationID, templates[0]);
                System.out.println("SLAID: " + finalSLA.getUuid().getValue());
                slaid = finalSLA.getUuid();
            }
            List<TerminationReason> terminationReason = new ArrayList<TerminationReason>();
            terminationReason.add(TerminationReason.BUSINESS_DECISION);
            bslamAssessCustomise.terminate(slaid, terminationReason);
            System.out.println("Return null if the negotiationID does not exist");
            SLA sla = bslamAssessCustomise.createAgreement("id", slaTemplate);
            System.out.println("SLA:" + sla);
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 9.2 # negotiate/query/coordinate::createAgreement
     * [BSLAM ->Software SLAM].
     */
    public final void createAgreementSoftwareSLAM() {
        try {
            String xmlFile2Send = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "Integration" + System.getProperty("file.separator") + "soap" + System.getProperty("file.separator") + "initiateNegotiation.xml";
            String soapAction = "";
            URL url;
            url = new URL(sycNegotiationWSURL);
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
        }
    }

    /**
     * INTERACTIO 9.2.1 # negotiate/query/coordinate::createAgreement
     * [Infrastructure SLAM].
     */
    public final void createAgreementInfrastructureSLAM() {
        try {
            INegotiation negotiate = isslamContext.getProtocolEngine().getINegotiation();
            org.slasoi.slamodel.sla.SLATemplate slaTemplate = new org.slasoi.slamodel.sla.SLATemplate();
            Party party = new Party(new ID("129.217.130.220"), new STND(org.slasoi.slamodel.vocab.sla.$provider));
            slaTemplate.setParties(new Party[] { party });
            String negotiationId = negotiate.initiateNegotiation(slaTemplate);
            org.slasoi.slamodel.sla.SLATemplate[] counterOffers = negotiate.negotiate(negotiationId, slaTemplate);
            counterOffers = negotiate.negotiate(negotiationId, slaTemplate);
            counterOffers = negotiate.negotiate(negotiationId, slaTemplate);
            SLA slaPE = negotiate.createAgreement(negotiationId, slaTemplate);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: ISSLAM(Protocol Engine)\n" + "Interface Name: negotiate/query/coordinate\n" + "Operation Name: createAgreement(Protocol Engine)\n" + "Input: ('1', new org.slasoi.slamodel.sla.SLATemplate())" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("SLA [description-" + slaPE.getDescr() + ", ModelVersion" + slaPE.getModelVersion() + "]");
        } catch (OperationInProgressException e) {
            e.printStackTrace();
        } catch (SLACreationException e) {
            e.printStackTrace();
        } catch (InvalidNegotiationIDException e) {
            e.printStackTrace();
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        } catch (OperationNotPossibleException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 9.2.1.1 # manage_infrastructure_service::commit
     * [Infrastructure SLAM -> Infrastructure SM].
     * @param ismServices IsmOcciService
     */
    public final void commitInfrastructureSM(final org.slasoi.ism.occi.IsmOcciService ismServices) {
        try {
            ProvisionRequestType provisionRequest = ismServices.createProvisionRequestType(ismServices.getOsregistry().getDefaultCategory().getTerm(), ismServices.getMetricregistry().getDefaultCategory().getTerm(), ismServices.getLocregistry().getDefaultCategory().getTerm(), "", "notification URI");
            ReservationResponseType reservationResponseType = null;
            reservationResponseType = ismServices.reserve(provisionRequest);
            reservationResponseType = ismServices.commit(reservationResponseType.getInfrastructureID());
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: ISM\n" + "Interface Name: manage_infrastructure_service\n" + "Operation Name: commit\n" + "Input \n" + provisionRequest.toString() + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("reservationResponseType" + reservationResponseType + "\n" + "reservationResponseType - infrastructureID - " + reservationResponseType.getInfrastructureID());
        } catch (DescriptorException e) {
            e.printStackTrace();
        } catch (UnknownIdException e) {
            e.printStackTrace();
        }
    }

    /**
     * INTERACTION 9.2.2 # prepare_software_service::book
     * [Software SLAM -> Software SM].
     * @param iSSMFacade ISoftwareServiceManagerFacade
     *
     */
    public final void bookSoftwareSM(final ISoftwareServiceManagerFacade iSSMFacade) {
        try {
            iSSMFacade.getBookingManager();
            String landscapePath = System.getenv("SLASOI_HOME") + System.getProperty("file.separator") + "software-servicemanager" + System.getProperty("file.separator") + "ORC.scm";
            Landscape landscape = (Landscape) ServiceConstructionModel.loadFromXMI(landscapePath);
            IProvisioning provisioningManager = new ProvisionServiceStub();
            iSSMFacade.setLandscape(landscape);
            iSSMFacade.setProvisioningManager(provisioningManager);
            List<ServiceType> listSTypes = iSSMFacade.queryServiceTypes();
            for (int i = 0; i < listSTypes.size(); i++) {
                System.out.println("ServiceType::description: " + listSTypes.get(i).getDescription() + "\nServiceType::ID: " + listSTypes.get(i).getID() + "\nServiceType::Name: " + listSTypes.get(i).getServiceTypeName() + "\nServiceType::interface0: " + listSTypes.get(i).getInterfaces(i));
            }
            List<ServiceImplementation> listImplT1 = iSSMFacade.queryServiceImplementations(listSTypes.get(0));
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            for (int i = 0; i < listImplT1.size(); i++) {
                System.out.println("ServiceTypeImplementations::description: " + listImplT1.get(i).getDescription() + "\nServiceTypeImplementations::ID: " + listImplT1.get(i).getID() + "\nServiceTypeImplementations::Name: " + listImplT1.get(i).getServiceImplementationName() + "\nServiceTypeImplementations::Version: " + listImplT1.get(i).getVersion() + "\nServiceTypeImplementations::" + "MonitoringFeaturesLength: " + listImplT1.get(i).getComponentMonitoringFeaturesLength());
            }
            iSSMFacade.capacityCheck(listImplT1.get(0));
            ServiceBuilder builder = iSSMFacade.createBuilder(listImplT1.get(0));
            Date startTime = new Date();
            Date stopTime = new Date();
            startTime.setTime(startTime.getTime() + 1000);
            stopTime.setTime(stopTime.getTime() + 10000);
            reservationFlag = iSSMFacade.reserve(builder, startTime, stopTime);
            state = iSSMFacade.queryServiceStatus(builder);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            System.out.println("Reserve::ServiceState: " + state.name() + "- code: " + state.hashCode());
            iSSMFacade.book(builder);
            state = iSSMFacade.queryServiceStatus(builder);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: SSM\n" + "Interface Name: prepare_software_service\n" + "Operation Name: book\n" + "Input:builder\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            System.out.println("Book::ServiceState:" + state.name() + "- code: " + state.hashCode());
        } catch (ReservationException re) {
            System.out.println("Reservaton failed!");
        } catch (BookingException e) {
            System.out.println("Booking failed!");
        }
    }

    /**
     * INTERACTION 9.3 # ??::customize
     * [BSLAM -> BM].
     * @param bmServices PlanningOptimization
     */
    @SuppressWarnings("deprecation")
    public final void customizeSecond(final PlanningOptimization bmServices) {
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: BM\n" + "Interface Name: IAssessmentAndCustomize\n" + "Operation Name: negotiate,createAgreement,terminate\n" + "Input:Type " + "void" + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
        org.slasoi.slamodel.sla.SLATemplate slaTemplate = new org.slasoi.slamodel.sla.SLATemplate();
        slaTemplate.setUuid(new UUID("SLATTest"));
        slaTemplate.setDescr("Desc");
        Party[] parties = new Party[1];
        Party party = new Party();
        party.setId(new ID("id"));
        party.setDescr("party");
        parties[0] = party;
        slaTemplate.setParties(parties);
        String negotiationID = java.util.UUID.randomUUID().toString();
        System.out.println("NegotiationID:" + negotiationID);
        org.slasoi.slamodel.sla.SLATemplate[] templates = bmServices.getIAssessmentAndCustomize().negotiate(negotiationID, slaTemplate);
        System.out.println("template returned:" + templates.length);
        UUID slaid = null;
        if (templates.length > 0) {
            System.out.println("tamplateID: " + templates[0].getUuid().getValue());
            System.out.println("Call CreateAgreement operation");
            SLA finalSLA = bmServices.getIAssessmentAndCustomize().createAgreement(negotiationID, templates[0]);
            System.out.println("SLAID: " + finalSLA.getUuid().getValue());
            slaid = finalSLA.getUuid();
        }
        List<TerminationReason> terminationReason = new ArrayList<TerminationReason>();
        terminationReason.add(TerminationReason.BUSINESS_DECISION);
        bmServices.getIAssessmentAndCustomize().terminate(slaid, terminationReason);
        System.out.println("Return null if the negotiationID does not exist");
        SLA sla = bmServices.getIAssessmentAndCustomize().createAgreement("id", slaTemplate);
        System.out.println("SLA:" + sla);
    }

    /**
     * ORC Run, simulating customer.
     */
    @SuppressWarnings("deprecation")
    public final void runORC() {
        try {
            GetParameterList getParamReq = new GetParameterList();
            getParamReq.setGetParameterType("COUNTRY");
            GetParameterListResponse getParamResp = partyWSClient.getParameterList(getParamReq);
            GetParameterListResponseType resultParam = getParamResp.get_return();
            LOGGER.debug("**********************" + "<<customer_relations::register>>*****************");
            LOGGER.debug("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: Business Manager\n" + "Interface Name: customer_relation\n" + "Operation Name: getParameterList()\n" + "Input:parameterType:" + getParamReq.getGetParameterType() + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            LOGGER.debug("\n\n\n\n\n");
            LOGGER.debug(resultParam.getResponseMessage());
            LOGGER.debug("\n\n\n\n\n");
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
            String userDetails = "UserLogin-" + testUser[0].getUserLogin() + ", Password-" + testUser[0].getPasswd();
            CreatePartyResponse partyResp = partyWSClient.createParty(partyReq);
            CreatePartyResponseType resultType = partyResp.get_return();
            LOGGER.info("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: BM\n" + "Interface Name: customer_relation\n" + "Operation Name: createParty\n" + "Inputs:\n " + "Type" + partyReq.getType() + "\n" + "Individual:" + testIndDetail + "\n" + "Organization:" + orgDetail + "\n" + "User:" + userDetails + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            LOGGER.info("\n\n\n\n\n");
            LOGGER.info(resultType.getResponseMessage() + "------Code(CustomerID):" + resultType.getResponseCode() + "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            LOGGER.info("\n\n\n\n\n");
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
            LOGGER.info("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: BM\n" + "Interface Name: query_product_catalog\n" + "Operation Name: get_products\n" + "Input:Customer_ID-" + req.getCustomerID() + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            LOGGER.info("\n\n\n\n\n");
            for (Product p : prod) {
                LOGGER.info("Product ID: " + p.getId() + "\n" + "Description: " + p.getDescription() + "\n" + "Name: " + p.getName() + "\n\n");
            }
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
            LOGGER.info("Business SLA Template\n" + template.toString());
            LOGGER.info("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: GSLAM\n" + "Interface Name: negotiate\n" + "Operation Name: negotiate\n" + "Inputs\n" + "SLATemplate with ID:" + templateID + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
            LOGGER.debug("\n\n\n\n\n");
            try {
                ISyntaxConverter sc = ((java.util.Hashtable<ISyntaxConverter.SyntaxConverterType, ISyntaxConverter>) bslamContext.getSyntaxConverters()).get(ISyntaxConverter.SyntaxConverterType.SLASOISyntaxConverter);
                String initiateNegResp = sc.getNegotiationClient(sycNegotiationWSURL).initiateNegotiation(template);
                LOGGER.debug(initiateNegResp);
                org.slasoi.slamodel.sla.SLATemplate[] temps = sc.getNegotiationClient(sycNegotiationWSURL).negotiate(initiateNegResp, template);
                LOGGER.info("Availabe Templates after Negotiation");
                for (SLATemplate slat : temps) {
                    if (slat != null) {
                        LOGGER.info(slat.getUuid());
                    }
                }
                LOGGER.info("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "Component Name: GSLAM\n" + "Interface Name: negotiate\n" + "Operation Name: createAgreement\n" + "Inputs\n" + "initiateNegotiationReponse\n" + "SLATemplate with ID:" + templateID + "\n" + "####################################################" + "#################################################\n" + "####################################################" + "#################################################\n" + "######################################## RESPONSE" + "############################################\n\n");
                LOGGER.debug("\n\n\n\n\n");
                SLA finalSLA = null;
                if (temps != null && temps.length > 0) {
                    if (temps[0] != null) {
                        finalSLA = sc.getNegotiationClient(sycNegotiationWSURL).createAgreement(initiateNegResp, temps[0]);
                    }
                }
                LOGGER.info("AgreedSLA :" + finalSLA.getUuid() + "\n" + "Time: " + finalSLA.getAgreedAt() + "\n" + "TemplateID: " + finalSLA.getTemplateId() + "\n" + "ModelVersion: " + finalSLA.getModelVersion() + "\n");
                LOGGER.info("\n\n\n\n\n");
            } catch (AxisFault e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } catch (SLAManagerContextException e) {
                e.printStackTrace();
            }
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
