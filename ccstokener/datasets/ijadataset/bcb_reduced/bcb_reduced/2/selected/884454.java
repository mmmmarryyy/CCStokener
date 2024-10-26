package eu.more.core.internal;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.xml.namespace.QName;
import org.apache.commons.logging.Log;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.soda.dpws.ClientEndpointReference;
import org.soda.dpws.DPWSContext;
import org.soda.dpws.DPWSException;
import org.soda.dpws.DeviceExplorer;
import org.soda.dpws.DeviceProxy;
import org.soda.dpws.ScopeMatchRule;
import org.soda.dpws.ServiceProxy;
import org.soda.dpws.addressing.RandomGUID;
import org.soda.dpws.addressing.UserEndpointReference;
import org.soda.dpws.cache.CachedDevice;
import org.soda.dpws.exchange.AbstractMessage;
import org.soda.dpws.exchange.InMessage;
import org.soda.dpws.exchange.MessageExchange;
import org.soda.dpws.fault.DPWSFault;
import org.soda.dpws.handler.DynamicInvocationHandler;
import org.soda.dpws.handler.DynamicSubscriptionHandler;
import org.soda.dpws.internal.DPWS;
import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.internal.DPWSFactory;
import org.soda.dpws.metadata.DeviceInfo;
import org.soda.dpws.metadata.DeviceMetadata;
import org.soda.dpws.metadata.MetadataSection;
import org.soda.dpws.metadata.ServiceMetadata;
import org.soda.dpws.registry.Device;
import org.soda.dpws.registry.Registry;
import org.soda.dpws.registry.ServiceClass;
import org.soda.dpws.registry.ServiceEndpoint;
import org.soda.dpws.server.EventHandler;
import org.soda.dpws.server.EventHandlerListener;
import org.soda.dpws.server.ServicePort;
import org.soda.dpws.wsdl.OperationInfo;
import org.soda.dpws.wsdl.PortTypeInfo;
import org.soda.dpws.wsdl.WSDLInfo;
import org.soda.jetty.JettyServletContainer;
import eu.more.core.internal.exception.RequiredInvokerNotFoundException;
import eu.more.core.internal.exception.ServiceNotRegisteredException;
import eu.more.core.internal.msoa.MSOAinfo;
import eu.more.core.internal.proxy.Ifmappingtable;
import eu.more.core.internal.proxy.ProxyUtils;

/**
 *
 *
 */
public class MOREService implements BundleActivator {

    protected ServiceEndpoint serviceEndpoint;

    protected Device device;

    protected ServiceRegistration reg;

    protected BundleContext bundleContext;

    protected final ClassLoader thisClassLoader = this.getClass().getClassLoader();

    protected String SERVICE_ID;

    protected String WSDL_NAME;

    protected String ServiceUID;

    protected String ServiceName;

    protected ServiceReference deviceServiceReference;

    protected Log logging;

    protected WSDLInfo wsdlInfo;

    protected Class<?> WSDLFactory;

    protected EventHandlerListener eventHandlerListener = null;

    protected JettyServletContainer jettyServletContainer = null;

    protected boolean isRegistered = false;

    public boolean isproxy = false;

    private UserEndpointReference uepr = null;

    private boolean usemsoa = false;

    private String discoverip = null;

    protected Class<?> Invoker;

    public Ifmappingtable ifmappingpe = new Ifmappingtable();

    public MSOAinfo msoainfo = new MSOAinfo();

    public static String MSOA_SCOPE = "http://www.ist-more.org/MSOADevice";

    /**
	 * Enabling Proxy Server using UserEndpointReference to choose Server device
	 * If UserEndpointReference is null a fallback to discover is initialized
	 * IMPORTANT: Has to be called before start method of MORE Service
	 * @param uepr
	 */
    public void enableProxy(UserEndpointReference tuepr) {
        isproxy = true;
        uepr = tuepr;
    }

    /**
	 * Enabling Proxy Server using Discovery for choosing Server device
	 * IMPORTANT: Has to be called before start method of MORE Service
	 */
    public void enableProxy() {
        isproxy = true;
        uepr = null;
    }

    /**
	 * Enabling Proxy Server isolating the Server device to the specified IP adress
	 * @param ipadresse
	 */
    public void enableProxy(String ipadresse) {
        isproxy = true;
        uepr = null;
        discoverip = ipadresse;
    }

    /**
	 * Enabeling MSOA Transmission to according service
	 * Method has to be called after device was started!
	 */
    public void enableMSOA() {
        ifmappingpe.usemsoa = true;
        usemsoa = true;
        msoainfo.usemsoa = true;
    }

    /**
	 * Method for discovering DPWS client device by MSOA Proxy Service.
	 * At the moment: Discovering all devices in LAN and filter by given ipadress in ipadress-String
	 * @return DeviceProxy discovered device
	 * @throws DPWSException
	 */
    @SuppressWarnings("unchecked")
    public DeviceProxy discoverclient(String discoverIP) throws DPWSException {
        String localip = "unknown";
        try {
            localip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        DeviceProxy devproxy = null;
        DeviceExplorer deviceExplorer = new DeviceExplorer();
        List devices = null;
        List types = new ArrayList(1);
        types.add(new QName("http://www.ist-more.org/Device", "MOREDevice"));
        List scopes = new ArrayList(1);
        String scope = "http://www.ist-more.org";
        scopes.add(scope);
        System.out.println("Looking for device on IP " + discoverIP + " ... (please wait)");
        devices = deviceExplorer.lookup(null, null, ScopeMatchRule.RFC2396, 10, 10000);
        if ((devices.size() != 0) && (discoverIP != null)) {
            Iterator it = devices.iterator();
            while (it.hasNext()) {
                DeviceProxy deviterator = (DeviceProxy) it.next();
                if (deviterator.getDefaultTransportAddress().contains(discoverIP)) {
                    if (deviterator.getScopes() != null) devproxy = deviterator;
                }
            }
            if (devproxy != null) {
                System.out.println("Chosen Device " + devproxy.getDefaultTransportAddress());
                return devproxy;
            } else {
                System.out.println("No Device Choosen!");
                return null;
            }
        } else if (devices.size() == 1) {
            return (DeviceProxy) devices.get(0);
        } else return null;
    }

    /**
	 * @param context
	 * @return the {@link BundleException}
	 */
    public BundleException initialize(BundleContext context) {
        bundleContext = context;
        Thread.currentThread().setContextClassLoader(thisClassLoader);
        System.out.println("Activating " + ServiceName + "...");
        ServiceTracker deviceTracker = new ServiceTracker(bundleContext, Device.class.getName(), null);
        deviceTracker.open();
        if ((deviceServiceReference = deviceTracker.getServiceReference()) != null) {
            device = (Device) bundleContext.getService(deviceServiceReference);
        } else {
            return new BundleException("No device available");
        }
        if (usemsoa) {
            try {
                device.addScope(MSOA_SCOPE);
            } catch (DPWSException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void start(BundleContext context) throws Exception {
        if (isproxy) {
            if ((ifmappingpe.devclient == null) && (uepr == null)) ifmappingpe.devclient = discoverclient(discoverip);
            if ((ifmappingpe.devclient == null) && (uepr == null)) {
                System.out.println("Error Discovery Client ... Proxy disabled!");
                isproxy = false;
            }
            ifmappingpe.invoker = Invoker;
        }
        start(context, this);
        if (isproxy) {
            Constructor[] cons = Invoker.getConstructors();
            Constructor tcon = null;
            for (int i = 0; i < cons.length; i++) {
                if (uepr != null) {
                    Constructor con = cons[i];
                    Class[] pts = con.getParameterTypes();
                    for (int j = 0; j < pts.length; j++) {
                        if (pts[j].getName().equals("org.soda.dpws.ClientEndpointReference")) tcon = con;
                    }
                } else if (uepr == null) {
                    Constructor con = cons[i];
                    Class[] pts = con.getParameterTypes();
                    for (int j = 0; j < pts.length; j++) {
                        if (pts[j].getName().equals("org.soda.dpws.Proxy")) tcon = con;
                    }
                }
            }
            Object[] params = new Object[tcon.getParameterAnnotations().length];
            if (uepr != null) {
                params[0] = uepr;
            } else if (uepr == null) {
                params[0] = ifmappingpe.devclient.getHostedServiceById(SERVICE_ID);
            }
            ifmappingpe.invokerinstanz = tcon.newInstance(params);
        }
        if (usemsoa) {
            msoainfo.wsdlinfo = wsdlInfo;
            msoainfo.WSDL = WSDL_NAME;
            if (msoainfo.buildschema(context, new ByteArrayInputStream((byte[]) serviceEndpoint.getProperty("WSDL")))) ifmappingpe.msoainfo = msoainfo;
            if (ifmappingpe.invokerinstanz != null) {
                Class[] iicpara = new Class[2];
                iicpara[0] = String.class;
                iicpara[1] = Object.class;
                Method iisetProperty = org.soda.dpws.invocation.Call.class.getDeclaredMethod("setProperty", iicpara);
                Object[] iiopara = new Object[2];
                iiopara[0] = new String("msoa");
                iiopara[1] = ifmappingpe.msoainfo;
                iisetProperty.invoke(ifmappingpe.invokerinstanz, iiopara);
            }
        }
    }

    /**
	 * @param context
	 * @param implementor
	 * @throws Exception
	 */
    public void start(BundleContext context, Object implementor) throws Exception {
        if (SERVICE_ID == null) throw new BundleException("No name available for this service");
        if (WSDL_NAME == null) throw new BundleException("No wsdl available for this service");
        System.out.println("Starting: " + SERVICE_ID);
        BundleException init = initialize(context);
        if (init != null) throw init;
        init = serviceInitilize();
        if (init != null) throw init;
        init = serviceCreation(implementor, SERVICE_ID, WSDL_NAME);
        if (init != null) throw init;
    }

    /**
	 * @return the {@link BundleException}
	 */
    public BundleException serviceInitilize() {
        return null;
    }

    /**
	 * @param implementor
	 * @param serviceId
	 * @param wsdlName
	 * @return the {@link BundleException}
	 */
    public BundleException serviceCreation(Object implementor, String serviceId, String wsdlName) {
        try {
            if ((serviceEndpoint = CreateService(bundleContext, device, implementor, serviceId, wsdlName)) == null) return new BundleException("Error creating DPWS Service.");
        } catch (Exception e) {
            e.printStackTrace();
            return new BundleException("Error creating DPWS Service.");
        }
        this.serviceEndpoint.setIfmappingpe(ifmappingpe);
        reg = bundleContext.registerService(ServiceEndpoint.class.getName(), serviceEndpoint, null);
        if (reg == null) return new BundleException("DPWS Service cannot created");
        Collection<ServicePort> physicalBindings = serviceEndpoint.getPhysicalBindings();
        Iterator<ServicePort> itAddr = physicalBindings.iterator();
        while (itAddr.hasNext()) this.wsdlInfo.addLocation((itAddr.next()).getFullAddress() + "/getwsdl");
        return null;
    }

    /**
	 * @param context
	 * @param device
	 * @param implementor
	 * @param serviceId
	 * @param wsdlName
	 * @return the {@link ServiceEndpoint}
	 * @throws DPWSException
	 */
    @SuppressWarnings("unchecked")
    public ServiceEndpoint CreateService(BundleContext context, Device device, Object implementor, String serviceId, String wsdlName) throws DPWSException {
        ServiceEndpoint ServiceInst = null;
        ServiceClass ServiceClassInst = new ServiceClass();
        try {
            Method[] classMethods = WSDLFactory.getDeclaredMethods();
            Method choosenMethod = null;
            for (int i = 0; i < classMethods.length - 1; i++) {
                if ((classMethods[i].getName().equals("getWSDLInfo")) && ((classMethods[i].getParameterTypes()).length == 1)) {
                    choosenMethod = classMethods[i];
                }
            }
            if (choosenMethod == null) return null;
            Object[] params = new Object[1];
            params[0] = "Service.wsdl";
            Object obj = choosenMethod.invoke(null, params);
            wsdlInfo = (WSDLInfo) obj;
            if (isproxy) ifmappingpe.createtable(obj);
        } catch (Exception e) {
            e.printStackTrace();
            log("Error at searching WSDLFactory", e);
            return null;
        }
        ServiceClassInst.addWebService(wsdlInfo);
        device.getDeviceModel().addServiceClass(ServiceClassInst, serviceId);
        ServiceInst = ServiceClassInst.createService(implementor, serviceId, true);
        wsdlInfo.removeLocation("Service.wsdl");
        Bundle bundle = context.getBundle();
        if ((ServiceUID != null) && (!ServiceUID.equals(""))) {
            ServiceInst.setProperty("UID", ServiceUID);
        } else {
            if (bundle.findEntries("config", "*.uid", true) != null) {
                String line = null;
                for (Enumeration<URL> uids = bundle.findEntries("config", "*.uid", true); uids.hasMoreElements(); ) try {
                    URL url = uids.nextElement();
                    if (url == null) break;
                    InputStream input = (url).openStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader(input));
                    line = in.readLine();
                    if (line == null) continue;
                    ServiceUID = line;
                } catch (IOException e) {
                    System.out.println("Cannot read uid: " + e);
                    ServiceInst.setProperty("UID", ((new RandomGUID()).toString()));
                }
                if ((ServiceUID.equals("")) || (ServiceUID == null)) {
                    ServiceUID = ((new RandomGUID()).toString());
                    ServiceInst.setProperty("UID", ServiceUID);
                } else ServiceInst.setProperty("UID", ServiceUID);
            }
        }
        System.out.println("Service UID:" + ServiceUID);
        if (bundle.findEntries("config", "local", true) != null) {
            ServiceInst.setProperty("local", "true");
        }
        if (!loadWsdlFromDir(wsdlName, ServiceInst, bundle, "WSDL")) {
            loadWsdlFromDir(wsdlName, ServiceInst, bundle, "wsdl");
        }
        return ServiceInst;
    }

    private boolean loadWsdlFromDir(String wsdlName, ServiceEndpoint ServiceInst, Bundle bundle, String wsdlDir) {
        if (bundle.findEntries(wsdlDir, wsdlName, true) != null) {
            System.out.println("WSDL detected");
            for (Enumeration<URL> wsdlobjects = bundle.findEntries(wsdlDir, wsdlName, true); wsdlobjects.hasMoreElements(); ) {
                try {
                    URL url = wsdlobjects.nextElement();
                    if (url == null) break;
                    InputStream input = (url).openStream();
                    byte[] WSDL = new byte[input.available()];
                    input.read(WSDL);
                    ServiceInst.setProperty("WSDL", WSDL);
                    return true;
                } catch (IOException e) {
                    System.out.println("Cannot read wsdl: " + e);
                    break;
                }
            }
        }
        return false;
    }

    /**
	 * @param id
	 * @return the {@link ServicePort}
	 */
    public ServiceEndpoint getService(String id) {
        Registry registry = device.getRegistry();
        Collection<Device> devicesCollenction = registry.getLocalDevices();
        Iterator<Device> localDevices = devicesCollenction.iterator();
        while (localDevices.hasNext()) {
            Device localDevice = localDevices.next();
            Collection<ServiceEndpoint> hostedServices = localDevice.getHostedServices();
            Iterator<ServiceEndpoint> serviceEndpoints = hostedServices.iterator();
            while (serviceEndpoints.hasNext()) {
                ServiceEndpoint serv = serviceEndpoints.next();
                if (serv.getId().equals(id)) {
                    return serv;
                }
            }
        }
        return null;
    }

    /**
	 * @param type
	 * @return the {@link ServicePort}
	 */
    public ServicePort getServicebyType(QName type) {
        Registry registry = device.getRegistry();
        Collection<Device> localDevices2 = registry.getLocalDevices();
        Iterator<Device> localDevices = localDevices2.iterator();
        while (localDevices.hasNext()) {
            Device localDevice = localDevices.next();
            Iterator<ServiceEndpoint> serviceEndpoints = localDevice.getHostedServices().iterator();
            while (serviceEndpoints.hasNext()) {
                ServiceEndpoint serv = serviceEndpoints.next();
                Iterator<QName> itTypes = serv.getSupportedTypes().iterator();
                while (itTypes.hasNext()) {
                    QName supportedType = itTypes.next();
                    if (supportedType.equals(type)) {
                        Iterator<ServicePort> itAddr = serv.getPhysicalBindings().iterator();
                        while (itAddr.hasNext()) {
                            ServicePort servicePortIter = itAddr.next();
                            return servicePortIter;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
	 * @param type
	 * @return the {@link ServicePort}
	 */
    public ServiceEndpoint getServiceEndpointbyType(QName type) {
        Registry registry = device.getRegistry();
        Collection<Device> localDevices2 = registry.getLocalDevices();
        Iterator<Device> localDevices = localDevices2.iterator();
        while (localDevices.hasNext()) {
            Device localDevice = localDevices.next();
            Iterator<ServiceEndpoint> serviceEndpoints = localDevice.getHostedServices().iterator();
            while (serviceEndpoints.hasNext()) {
                ServiceEndpoint serv = serviceEndpoints.next();
                Iterator<QName> itTypes = serv.getSupportedTypes().iterator();
                while (itTypes.hasNext()) {
                    QName supportedType = itTypes.next();
                    if (supportedType.equals(type)) {
                        return serv;
                    }
                }
            }
        }
        return null;
    }

    public void stop(BundleContext context) throws Exception {
        System.out.println("Stopping " + ServiceName + "...");
        if (isproxy) {
            Iterator it = device.getPhysicalBindings().iterator();
            while (it.hasNext()) ProxyUtils.removeProxyInstance(((ServicePort) it.next()).getFullAddress());
        }
        context.ungetService(deviceServiceReference);
        reg.unregister();
    }

    private void log(String string, Throwable e) {
        System.err.println(string + ": " + e);
        logging.info(e);
        e.printStackTrace();
    }

    /**
	 * @param service
	 * @param operation
	 * @param params
	 * @return the result
	 */
    public Object[] sendToOtherServiceOnNode(DPWSContext context, String service, String operation, List<Object> params) {
        Object[] returnObject = null;
        try {
            ServiceEndpoint remoteServiceEndpoint = getService(service);
            OperationInfo op = remoteServiceEndpoint.getOperation(operation);
            returnObject = remoteServiceEndpoint.invoke((DPWSContextImpl) context, op, params);
        } catch (Exception e) {
            logging.error("Service:" + service + " Operation:" + operation, e);
            return null;
        }
        return returnObject;
    }

    /**
	 * @param service
	 * @param operation
	 * @param params
	 * @return the result
	 */
    public Object[] sendToOtherServiceOnNode(String service, String operation, List<Object> params) {
        Object[] returnObject = null;
        try {
            ServiceEndpoint remoteServiceEndpoint = getService(service);
            OperationInfo op = remoteServiceEndpoint.getOperation(operation);
            returnObject = remoteServiceEndpoint.invoke(null, op, params);
        } catch (NullPointerException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return returnObject;
    }

    /**
	 * @param serviceType
	 * @param operation
	 * @param params
	 * @return result object
	 */
    public Object[] sendToOtherServiceOnNodeByType(DPWSContext context, QName serviceType, String operation, List<Object> params) {
        Object[] returnObject = null;
        try {
            ServiceEndpoint remoteServiceEndpoint = getServiceEndpointbyType(serviceType);
            OperationInfo op = remoteServiceEndpoint.getOperation(operation);
            returnObject = remoteServiceEndpoint.invoke((DPWSContextImpl) context, op, params);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return returnObject;
    }

    /**
	 * @param bundleContext
	 * @param bundleName
	 * @param className
	 * @return the class
	 */
    public Class<?> getClassOfAnotherBundleByName(BundleContext bundleContext, String bundleName, String className) {
        try {
            for (Bundle bundle : bundleContext.getBundles()) {
                if (bundle.getSymbolicName().equals(bundleName)) {
                    return bundle.loadClass(className);
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
	 *
	 * @param serviceID
	 * @param serviceAdress
	 * @return the {@link ServiceProxy}
	 * @throws DPWSException
	 */
    @SuppressWarnings("unchecked")
    public ServiceProxy retrieveServiceFromOtherNode(String serviceID, String serviceAdress) throws DPWSException {
        List<QName> types = new ArrayList<QName>();
        List<String> scopes = new ArrayList<String>();
        scopes.add("http://www.ist-more.org");
        return retrieveServiceFromOtherNode(serviceID, serviceAdress, types, scopes);
    }

    /**
	 *
	 * @param serviceID
	 * @param serviceAdress
	 * @param typez
	 * @param scopez
	 * @return the {@link ServiceProxy}
	 * @throws DPWSException
	 */
    @SuppressWarnings("unchecked")
    public ServiceProxy retrieveServiceFromOtherNode(String serviceID, String serviceAdress, List<QName> typez, List<String> scopez) throws DPWSException {
        DeviceExplorer deviceExplorer = new DeviceExplorer();
        List<QName> types = typez;
        List<String> scopes = scopez;
        List<CachedDevice> devices = deviceExplorer.lookup(types, scopes);
        if (devices == null || devices.isEmpty()) {
            throw new DPWSException("No nodes found");
        }
        int devSize = devices.size();
        ServiceProxy serviceProxy;
        logging.info("Starting DPWS-Explore");
        logging.info("Looking for service with id " + serviceID + " at host " + serviceAdress);
        for (int idx = 0; idx < devSize; idx++) {
            DeviceProxy devProxy = devices.get(idx);
            Collection<ServiceProxy> hostedServices = devProxy.getHostedServices();
            Iterator<ServiceProxy> hostedServicesIterator = hostedServices.iterator();
            ClientEndpointReference defaultEndpoint;
            while (hostedServicesIterator != null && hostedServicesIterator.hasNext()) {
                serviceProxy = hostedServicesIterator.next();
                defaultEndpoint = serviceProxy.getDefaultEndpoint();
                String id = serviceProxy.getId();
                String address = defaultEndpoint.getAddress();
                if (id.equals(serviceID) && address.startsWith(serviceAdress)) {
                    logging.info(serviceID + " at address " + serviceAdress + " found");
                    return serviceProxy;
                }
            }
        }
        logging.info("Looking up for " + serviceID + " at address " + serviceAdress + " not successfully!");
        throw new DPWSException("Service " + serviceID + " at address " + serviceAdress + " not found!");
    }

    /**
	 *
	 * @param serviceUUID
	 * @return the {@link ServiceProxy}
	 * @throws DPWSException
	 */
    public ServiceProxy retrieveServiceFromOtherNode(String serviceUUID) throws DPWSException {
        List<QName> types = new ArrayList<QName>();
        List<String> scopes = new ArrayList<String>();
        scopes.add("http://www.ist-more.org");
        return retrieveServiceFromOtherNode(serviceUUID, types, scopes);
    }

    /**
	 *
	 * @param serviceUUID
	 * @param typez
	 * @param scopez
	 * @return the {@link ServiceProxy}
	 * @throws DPWSException
	 */
    @SuppressWarnings("unchecked")
    public ServiceProxy retrieveServiceFromOtherNode(String serviceUUID, List<QName> typez, List<String> scopez) throws DPWSException {
        DeviceExplorer deviceExplorer = new DeviceExplorer();
        List<QName> types = typez;
        List<String> scopes = scopez;
        List<CachedDevice> devices = deviceExplorer.lookup(types, scopes);
        logging.info("Starting DPWS-Explore");
        logging.info("Looking for service with uid " + serviceUUID);
        if (devices == null || devices.isEmpty()) {
            throw new DPWSException("No nodes found");
        }
        int devSize = devices.size();
        ServiceProxy serviceProxy;
        for (int idx = 0; idx < devSize; idx++) {
            DeviceProxy devProxy = devices.get(idx);
            Collection<ServiceProxy> hostedServices = devProxy.getHostedServices();
            Iterator<ServiceProxy> hostedServicesIterator = hostedServices.iterator();
            ClientEndpointReference defaultEndpoint;
            while (hostedServicesIterator != null && hostedServicesIterator.hasNext()) {
                serviceProxy = hostedServicesIterator.next();
                defaultEndpoint = serviceProxy.getDefaultEndpoint();
                String address = defaultEndpoint.getAddress();
                if (address.endsWith(serviceUUID)) {
                    logging.info("Service with uid " + serviceUUID + " found!");
                    return serviceProxy;
                }
            }
        }
        throw new DPWSException("Service with UUID " + serviceUUID + " not found!");
    }

    /**
	 *
	 * @param serviceType
	 * @param deviceId
	 * @return the {@link ServiceProxy}
	 * @throws DPWSException
	 */
    @SuppressWarnings("unchecked")
    public ServiceProxy retrieveServiceFromOtherNodeByType(QName serviceType, String deviceId) throws DPWSException {
        DeviceExplorer deviceExplorer = new DeviceExplorer();
        List deviceTypes = new ArrayList(1);
        List deviceScopes = new ArrayList(1);
        deviceScopes.add("http://www.ist-more.org");
        List<String> serviceTypes = new ArrayList<String>();
        serviceTypes.add(serviceType.getNamespaceURI());
        List<ServiceProxy> serviceProxies = deviceExplorer.servicelookup(deviceTypes, deviceScopes, serviceTypes);
        if (serviceProxies == null || serviceProxies.size() < 1) {
            throw new DPWSException("Service type " + serviceType + " not found at all");
        }
        Iterator<ServiceProxy> it = serviceProxies.iterator();
        while (it.hasNext()) {
            ServiceProxy sp = it.next();
            CachedDevice cd = (CachedDevice) sp.getServiceMetadata().getHostEndpoint();
            String foundDeviceId = cd.getId();
            if (foundDeviceId != null && foundDeviceId.equals(deviceId)) {
                return sp;
            }
        }
        throw new DPWSException("Service " + serviceType + " at address " + deviceId + " not found!");
    }

    /**
	 *
	 * @param serviceID
	 * @param deviceUID
	 * @return the {@link ServiceProxy}
	 * @throws DPWSException
	 */
    @SuppressWarnings("unchecked")
    public ServiceProxy retrieveServiceFromOtherNodeByDeviceUID(String serviceID, String deviceUID) throws DPWSException {
        List<QName> types = new ArrayList<QName>();
        List<String> scopes = new ArrayList<String>();
        scopes.add("http://www.ist-more.org");
        return retrieveServiceFromOtherNodeByDeviceUID(serviceID, deviceUID, types, scopes);
    }

    /**
	 *
	 * @param serviceID
	 * @param deviceUID
	 * @param typez
	 * @param scopez
	 * @return the {@link ServiceProxy}
	 * @throws DPWSException
	 */
    @SuppressWarnings("unchecked")
    public ServiceProxy retrieveServiceFromOtherNodeByDeviceUID(String serviceID, String deviceUID, List<QName> typez, List<String> scopez) throws DPWSException {
        DeviceExplorer deviceExplorer = new DeviceExplorer();
        List<QName> types = typez;
        List<String> scopes = scopez;
        List<CachedDevice> devices = deviceExplorer.lookup(types, scopes);
        if (devices == null || devices.isEmpty()) {
            throw new DPWSException("No nodes found");
        }
        int devSize = devices.size();
        ServiceProxy serviceProxy;
        logging.info("Starting DPWS-Explore");
        logging.info("Looking for service with id " + serviceID + " at device " + deviceUID);
        for (Iterator<CachedDevice> devIter = devices.iterator(); devIter.hasNext(); ) {
            DeviceProxy deviceProxy = devIter.next();
            String devUID = deviceProxy.getAddress();
            logging.info("current device [" + devUID + "]");
            if (!devUID.equals(deviceUID)) continue;
            logging.info("current device equal to searched one");
            DeviceMetadata deviceMetadata = deviceProxy.getDeviceMetadata();
            DeviceInfo deviceInfo = deviceMetadata.getDeviceInfo();
            String serialNumber = deviceInfo.getSerialNumber();
            List<Serializable> typs = new ArrayList<Serializable>();
            List<ServiceProxy> hostedServices = deviceProxy.getHostedServices(typs);
            logging.info("Iterating over services!");
            for (ServiceProxy servicePrxy : hostedServices) {
                String id = servicePrxy.getId();
                logging.info("Current service [" + id + "] [" + serialNumber + "] found!");
                if (id.equals(serviceID)) {
                    logging.info(serviceID + " at device " + deviceUID + " found");
                    return servicePrxy;
                }
            }
        }
        logging.info("Looking up for " + serviceID + " at address " + deviceUID + " not successfully!");
        throw new DPWSException("Service " + serviceID + " at address " + deviceUID + " not found!");
    }

    /**
	 * Reads a properties file from eu.more.core.configuration-directory and
	 * returns the PropertiesObject
	 *
	 * @param filename
	 *            Name of the properties file
	 * @return readed {@link Properties}
	 */
    public Properties readPropertiesFile(String filename) {
        Properties properties = null;
        try {
            String rootDir = System.getProperty("eu.more.core.configuration");
            if (rootDir == null) rootDir = "./configuration/";
            String fileSep = System.getProperty("file.separator");
            String resourceDir = rootDir + ((rootDir.endsWith(fileSep)) ? "" : fileSep);
            properties = new Properties();
            FileInputStream props = new FileInputStream(resourceDir + filename);
            properties.load(props);
            props.close();
            props = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    /**
	 * Creates an event handler which can used for dynamic subscription.
	 *
	 * @param operationNamespace
	 * @param operationPortName
	 * @param operationName
	 * @param sourceOperation
	 * @param serviceProxy
	 * @return
	 * @throws DPWSException
	 * @throws UnknownHostException
	 */
    protected EventHandler createDynamicSubscriptionEventHandler(String operationNamespace, String operationPortName, String operationName, ServiceProxy serviceProxy, DynamicInvocationHandler handler) throws DPWSException, UnknownHostException {
        String sourceOperation = operationNamespace + "/" + operationPortName + "/" + operationName;
        ServiceMetadata serviceMetadata = serviceProxy.getServiceMetadata();
        List<MetadataSection> wsdls = serviceMetadata.getWsdls();
        MetadataSection wInfo = null;
        if (wsdls == null || wsdls.size() < 1) {
            logging.info("Sorry, no remote WSDL avaiable!");
            return null;
        }
        wInfo = wsdls.get(0);
        if (jettyServletContainer == null) jettyServletContainer = new JettyServletContainer();
        if (eventHandlerListener == null) {
            eventHandlerListener = new EventHandlerListener(jettyServletContainer, 4325);
        }
        if (!eventHandlerListener.isStarted()) eventHandlerListener.defaultStart();
        WSDLInfo info = new WSDLInfo(wInfo.getLocation());
        Method[] methods = DynamicSubscriptionHandler.class.getMethods();
        Method method = null;
        for (int idx = 0; idx < methods.length; idx++) if (methods[idx].getName().equals("invoke")) method = methods[idx];
        QName portTypeQName = new QName(operationNamespace, operationPortName);
        PortTypeInfo portTypeInfo = new PortTypeInfo(portTypeQName, null, InvocationHandler.class, true);
        portTypeInfo.addOperation(operationName, method, null, sourceOperation, true);
        info.addPortType(portTypeInfo);
        EventHandler eventHandler = null;
        eventHandler = eventHandlerListener.createEventHandler(info, new DynamicSubscriptionHandler(handler));
        return eventHandler;
    }

    /**
	 * @param service
	 * @param operation
	 * @param params
	 * @return the result
	 */
    public Object[] sendToOtherServiceOnNode(DPWSContext context, String service, String operation, List<Object> params, List<String> faults) {
        Object[] returnObject = null;
        try {
            ServiceEndpoint remoteServiceEndpoint = getService(service);
            OperationInfo op = remoteServiceEndpoint.getOperation(operation);
            returnObject = remoteServiceEndpoint.invoke((DPWSContextImpl) context, op, params);
        } catch (DPWSFault e) {
            return new Object[] { checkExceptionForFaults(e, faults) };
        } catch (Exception e) {
            logging.error("Service:" + service + " Operation:" + operation, e);
            return null;
        }
        return returnObject;
    }

    /**
	 * This hook is called by the ConfigService after updating or adding a
	 * property value.
	 *
	 * @param key
	 *            the (new) key
	 * @param newValue
	 *            the new value
	 * @param oldValue
	 *            the old value
	 */
    public void messageHandleHook(String key, String newValue, String oldValue) {
        System.out.println(key + " NV: " + newValue + " OV:");
    }

    /**
	 * Adds a property.
	 *
	 * @param propertyName
	 *            - the key of the property to add
	 * @param propertyValue
	 *            - the value of the property to add
	 * @return <i>true</i> if the property was successfully added. Otherwise
	 *         <i>false</i>
	 * @throws DPWSException
	 *             - This exception is thrown on several faults to be compatible
	 *             with DPWS-Stack
	 * @throws ServiceNotRegisteredException
	 *             - This exception is thrown, if the service is currently not
	 *             registered
	 * @throws RequiredInvokerNotFoundException
	 *             - This exception is thrown, if a required invoker could not
	 *             be found
	 */
    public boolean addPropertyAtConfigService(String propertyName, String propertyValue) throws DPWSException, ServiceNotRegisteredException, RequiredInvokerNotFoundException {
        if (!isRegistered) throw new ServiceNotRegisteredException();
        try {
            int bundleId = (int) bundleContext.getBundle().getBundleId();
            Class setPropertyClass = getClassOfAnotherBundleByName(bundleContext, "ConfigService", "eu.more.configservice.generated.jaxb.impl.AddPropertyImpl");
            if (setPropertyClass == null) throw new RequiredInvokerNotFoundException();
            Object addPropertyObject = setPropertyClass.newInstance();
            Method setServiceIDMethod = setPropertyClass.getMethod("setServiceID", int.class);
            setServiceIDMethod.invoke(addPropertyObject, bundleId);
            Method setPropertyNameMethod = setPropertyClass.getMethod("setPropertyName", String.class);
            setPropertyNameMethod.invoke(addPropertyObject, propertyName);
            Method addPropertyMethod = setPropertyClass.getMethod("setPropertyValue", String.class);
            addPropertyMethod.invoke(addPropertyObject, propertyValue);
            List<Object> params = new LinkedList<Object>();
            params.add(addPropertyObject);
            DPWSContext context = new DPWSContextImpl();
            new MessageExchange((DPWSContextImpl) context);
            InMessage inMessage = new InMessage();
            context.getExchange().setInMessage(inMessage);
            List<String> faults = new ArrayList<String>();
            faults.add("ServiceUnkownFault");
            faults.add("OperationFailedFault");
            faults.add("MissingPropertiesFault");
            Object[] returnObject = sendToOtherServiceOnNode(context, "http://www.ist-more.org/ConfigService", "addProperty", params, faults);
            if (returnObject == null) {
                return true;
            }
            if (returnObject[0] == null) {
                logging.warn("Setting property " + propertyName + " failed! Reason: Unknown!");
                return false;
            }
            Object retOne = returnObject[0];
            if (retOne instanceof String && ((String) retOne).equals("Unknown!")) {
                logging.warn("Setting property " + propertyName + " failed! Reason: Unknown!");
                return false;
            }
            if (retOne instanceof DPWSFault) {
                DPWSFault f = (DPWSFault) retOne;
                logging.warn("Setting property " + propertyName + " failed! Reason: " + f.getReason() + " Fault: " + f.getFaultCode().getLocalPart());
                return false;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            throw new DPWSException(e);
        } catch (InstantiationException e) {
            throw new DPWSException(e);
        } catch (IllegalAccessException e) {
            throw new DPWSException(e);
        } catch (SecurityException e) {
            throw new DPWSException(e);
        } catch (NoSuchMethodException e) {
            throw new DPWSException(e);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new DPWSException(e);
        } catch (InvocationTargetException e) {
            throw new DPWSException(e);
        }
        return true;
    }

    /**
	 * Updates a property.
	 *
	 * @param propertyName
	 *            - the key under of the updated property
	 * @param propertyValue
	 *            - the new value
	 * @return <i>true</i> if the property was successfully updated. Otherwise
	 *         <i>false</i>
	 * @throws DPWSException
	 *             - This exception is thrown on several faults to be compatible
	 *             with DPWS-Stack
	 * @throws ServiceNotRegisteredException
	 *             - This exception is thrown, if the service is currently not
	 *             registered
	 * @throws RequiredInvokerNotFoundException
	 *             - This exception is thrown, if a required invoker could not
	 *             be found
	 */
    public boolean setPropertyAtConfigService(String propertyName, String propertyValue) throws DPWSException, ServiceNotRegisteredException, RequiredInvokerNotFoundException {
        if (!isRegistered) throw new ServiceNotRegisteredException();
        try {
            int bundleId = (int) bundleContext.getBundle().getBundleId();
            Class setPropertyClass = getClassOfAnotherBundleByName(bundleContext, "ConfigService", "eu.more.configservice.generated.jaxb.impl.SetPropertyValueImpl");
            if (setPropertyClass == null) throw new RequiredInvokerNotFoundException();
            Object setPropertyObject = setPropertyClass.newInstance();
            Method setServiceIDMethod = setPropertyClass.getMethod("setServiceID", int.class);
            setServiceIDMethod.invoke(setPropertyObject, bundleId);
            Method setPropertyNameMethod = setPropertyClass.getMethod("setPropertyName", String.class);
            setPropertyNameMethod.invoke(setPropertyObject, propertyName);
            Method setPropertyValueMethod = setPropertyClass.getMethod("setPropertyValue", String.class);
            setPropertyValueMethod.invoke(setPropertyObject, propertyValue);
            List<Object> params = new LinkedList<Object>();
            params.add(setPropertyObject);
            DPWSContext context = new DPWSContextImpl();
            new MessageExchange((DPWSContextImpl) context);
            InMessage inMessage = new InMessage();
            context.getExchange().setInMessage(inMessage);
            List<String> faults = new ArrayList<String>();
            faults.add("ServiceUnkownFault");
            faults.add("OperationFailedFault");
            faults.add("MissingPropertiesFault");
            faults.add("PropertyUnkownFault");
            Object[] returnObject = sendToOtherServiceOnNode(context, "http://www.ist-more.org/ConfigService", "setPropertyValue", params, faults);
            if (returnObject == null) {
                return true;
            }
            if (returnObject[0] == null) {
                logging.warn("Setting property " + propertyName + " failed! Reason: Unknown!");
                return false;
            }
            Object retOne = returnObject[0];
            if (retOne instanceof String && ((String) retOne).equals("Unknown!")) {
                logging.warn("Setting property " + propertyName + " failed! Reason: Unknown!");
                return false;
            }
            if (retOne instanceof DPWSFault) {
                DPWSFault f = (DPWSFault) retOne;
                logging.warn("Setting property " + propertyName + " failed! Reason: " + f.getReason() + " Fault: " + f.getFaultCode().getLocalPart());
                return false;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            throw new DPWSException(e);
        } catch (InstantiationException e) {
            throw new DPWSException(e);
        } catch (IllegalAccessException e) {
            throw new DPWSException(e);
        } catch (SecurityException e) {
            throw new DPWSException(e);
        } catch (NoSuchMethodException e) {
            throw new DPWSException(e);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new DPWSException(e);
        } catch (InvocationTargetException e) {
            throw new DPWSException(e);
        }
        return true;
    }

    /**
	 * Retrieves a value stored under propertyName for this service.
	 *
	 * @param propertyName
	 *            - the key of interested value
	 * @return the value stored under propertyName
	 * @throws DPWSException
	 *             - This exception is thrown on several faults to be compatible
	 *             with DPWS-Stack
	 * @throws ServiceNotRegisteredException
	 *             - This exception is thrown, if the service is currently not
	 *             registered
	 * @throws RequiredInvokerNotFoundException
	 *             - This exception is thrown, if a required invoker could not
	 *             be found
	 */
    public String getPropertyFromConfigService(String propertyName) throws DPWSException, ServiceNotRegisteredException, RequiredInvokerNotFoundException {
        if (!isRegistered) throw new ServiceNotRegisteredException();
        try {
            int bundleId = (int) bundleContext.getBundle().getBundleId();
            Class getPropertyClass = getClassOfAnotherBundleByName(bundleContext, "ConfigService", "eu.more.configservice.generated.jaxb.impl.GetPropertyImpl");
            if (getPropertyClass == null) throw new RequiredInvokerNotFoundException();
            Object getPropertyObject = getPropertyClass.newInstance();
            Method setServiceIDMethod = getPropertyClass.getMethod("setServiceID", int.class);
            setServiceIDMethod.invoke(getPropertyObject, bundleId);
            Method setPropertyNameMethod = getPropertyClass.getMethod("setPropertyName", String.class);
            setPropertyNameMethod.invoke(getPropertyObject, propertyName);
            List<Object> params = new LinkedList<Object>();
            params.add(getPropertyObject);
            DPWSContext context = new DPWSContextImpl();
            new MessageExchange((DPWSContextImpl) context);
            InMessage inMessage = new InMessage();
            context.getExchange().setInMessage(inMessage);
            List<String> faults = new ArrayList<String>();
            faults.add("ServiceUnkownFault");
            faults.add("OperationFailedFault");
            faults.add("PropertyUnkownFault");
            Object[] returnObject = sendToOtherServiceOnNode(context, "http://www.ist-more.org/ConfigService", "getProperty", params, faults);
            if (returnObject == null || returnObject[0] == null) {
                logging.warn("Retrieving property " + propertyName + " failed! Reason: Unknown!");
                return null;
            }
            Object retOne = returnObject[0];
            if (retOne instanceof String) {
                String ret = (String) retOne;
                System.out.println(ret);
                if (ret.equals("Unknown!")) {
                    logging.warn("Retrieving property " + propertyName + " failed! Reason: Unknown!");
                    return null;
                }
            }
            if (retOne instanceof DPWSFault) {
                DPWSFault f = (DPWSFault) retOne;
                logging.warn("Retrieving property " + propertyName + " failed! Reason: " + f.getReason() + " Fault: " + f.getFaultCode().getLocalPart());
                return null;
            }
            Class responseClass = getClassOfAnotherBundleByName(bundleContext, "ConfigService", "eu.more.configservice.generated.jaxb.impl.GetPropertyResponse1Impl");
            Object response = null;
            response = responseClass.cast(returnObject[0]);
            Object responsedKVP = responseClass.getMethod("getProperty").invoke(response);
            String valueOfResponse = (String) responsedKVP.getClass().getMethod("getValue").invoke(responsedKVP);
            return valueOfResponse;
        } catch (NullPointerException e) {
            throw new DPWSException(e);
        } catch (InstantiationException e) {
            throw new DPWSException(e);
        } catch (IllegalAccessException e) {
            throw new DPWSException(e);
        } catch (SecurityException e) {
            throw new DPWSException(e);
        } catch (NoSuchMethodException e) {
            throw new DPWSException(e);
        } catch (IllegalArgumentException e) {
            throw new DPWSException(e);
        } catch (InvocationTargetException e) {
            throw new DPWSException(e);
        }
    }

    /**
	 * This method registers the service by a local ConfigService
	 *
	 * @param activatorClassName
	 *            - the complete class name (include path) of the service
	 *            activator
	 * @param keyValuePairs
	 *            - a preset of key-value-pairs
	 * @return <i>true</i> if registering was successfully done. Otherwise
	 *         <i>false</i>
	 * @throws DPWSException
	 *             - This exception is thrown on several faults to be compatible
	 *             with DPWS-Stack
	 * @throws RequiredInvokerNotFoundException
	 *             - This exception is thrown, if a required invoker could not
	 *             be found
	 */
    @SuppressWarnings("unchecked")
    public boolean registerAtConfigService(String activatorClassName, HashMap<String, String> keyValuePairs) throws DPWSException, RequiredInvokerNotFoundException {
        int bundleIdAsPrimitive = (int) bundleContext.getBundle().getBundleId();
        try {
            List<Object> keyValuePairList = new LinkedList<Object>();
            Set<String> keySet = keyValuePairs.keySet();
            for (String key : keySet) {
                String value = keyValuePairs.get(key);
                Class keyValuePairClass = getClassOfAnotherBundleByName(bundleContext, "ConfigService", "eu.more.configservice.generated.jaxb.impl.KeyValuePairImpl");
                if (keyValuePairClass == null) throw new RequiredInvokerNotFoundException();
                Object keyValuePairObject = keyValuePairClass.newInstance();
                if (keyValuePairClass != null) {
                    System.out.println("Adding " + key + "/" + value + " to list");
                    Method setKeyMethod = keyValuePairClass.getDeclaredMethod("setKey", String.class);
                    Method setValueMethod = keyValuePairClass.getDeclaredMethod("setValue", String.class);
                    setKeyMethod.invoke(keyValuePairObject, key);
                    setValueMethod.invoke(keyValuePairObject, value);
                }
                keyValuePairList.add(keyValuePairObject);
            }
            List topList = new LinkedList();
            Class invokeClass = getClassOfAnotherBundleByName(bundleContext, "ConfigService", "eu.more.configservice.generated.jaxb.impl.RegisterServiceImpl");
            if (invokeClass == null) throw new RequiredInvokerNotFoundException();
            Object invokeObject = invokeClass.newInstance();
            (invokeClass.getMethod("setServiceName", String.class)).invoke(invokeObject, SERVICE_ID);
            (invokeClass.getMethod("setServiceID", int.class)).invoke(invokeObject, bundleIdAsPrimitive);
            (invokeClass.getMethod("setServiceActivator", String.class)).invoke(invokeObject, activatorClassName);
            List list = (List) (invokeClass.getMethod("getProperties")).invoke(invokeObject);
            list.addAll(keyValuePairList);
            topList.add(invokeObject);
            DPWSContext context = new DPWSContextImpl();
            new MessageExchange((DPWSContextImpl) context);
            InMessage inMessage = new InMessage();
            context.getExchange().setInMessage(inMessage);
            List<String> faults = new ArrayList<String>();
            faults.add("ServiceUnkownFault");
            faults.add("OperationFailedFault");
            faults.add("MissingPropertiesFault");
            Object[] returnObject = sendToOtherServiceOnNode(context, "http://www.ist-more.org/ConfigService", "registerService", topList, faults);
            Class responseClass = getClassOfAnotherBundleByName(bundleContext, "ConfigService", "eu.more.configservice.generated.jaxb.impl.RegisterServiceResponseImpl");
            if (returnObject == null || returnObject[0] == null) {
                logging.warn("Registering failed! Reason: Unknown!");
                return false;
            }
            Object retOne = returnObject[0];
            if (retOne instanceof String && ((String) retOne).equals("Unknown!")) {
                logging.warn("Registering failed! Reason: Unknown!");
                return false;
            }
            if (retOne instanceof DPWSFault) {
                DPWSFault f = (DPWSFault) retOne;
                logging.warn("Registering failed! Reason: " + f.getReason() + " Fault: " + f.getFaultCode().getLocalPart());
                return false;
            }
            Object response = null;
            response = responseClass.cast(returnObject[0]);
            boolean valueOfResponse = Boolean.TRUE.equals((responseClass.getMethod("isSuccess", new Class[] {}).invoke(response, new Object[] {})));
            if (valueOfResponse) logging.info("Registering successfully done!"); else logging.info("Registering failed!");
            isRegistered = valueOfResponse;
            return valueOfResponse;
        } catch (NoSuchMethodException e) {
            throw new DPWSException(e);
        } catch (IllegalArgumentException e) {
            throw new DPWSException(e);
        } catch (IllegalAccessException e) {
            throw new DPWSException(e);
        } catch (InvocationTargetException e) {
            throw new DPWSException(e);
        } catch (InstantiationException e) {
            throw new DPWSException(e);
        }
    }

    /**
	 * Try to remove the current service from the ConfigService.
	 *
	 * @return <i>true</i> on success, otherwise <i>false</i>
	 * @throws DPWSException
	 *             - This exception is thrown on several faults to be compatible
	 *             with DPWS-Stack
	 * @throws ServiceNotRegisteredException
	 *             - This exception is thrown, if the service is currently not
	 *             registered
	 * @throws RequiredInvokerNotFoundException
	 *             - This exception is thrown, if a required invoker could not
	 *             be found
	 */
    public boolean unregisterAtConfigService() throws DPWSException, ServiceNotRegisteredException, RequiredInvokerNotFoundException {
        if (!isRegistered) throw new ServiceNotRegisteredException();
        try {
            int bundleId = (int) bundleContext.getBundle().getBundleId();
            Class unregisterParameterClass = getClassOfAnotherBundleByName(bundleContext, "ConfigService", "eu.more.configservice.generated.jaxb.impl.UnregisterServiceImpl");
            if (unregisterParameterClass == null) throw new RequiredInvokerNotFoundException();
            Object unregisterParameterObject = unregisterParameterClass.newInstance();
            Method setServiceIDMethod = unregisterParameterClass.getMethod("setServiceID", int.class);
            setServiceIDMethod.invoke(unregisterParameterObject, bundleId);
            List<Object> params = new LinkedList<Object>();
            params.add(unregisterParameterObject);
            DPWSContext context = new DPWSContextImpl();
            new MessageExchange((DPWSContextImpl) context);
            InMessage inMessage = new InMessage();
            context.getExchange().setInMessage(inMessage);
            List<String> faults = new ArrayList<String>();
            faults.add("ServiceUnkownFault");
            faults.add("OperationFailedFault");
            faults.add("MissingPropertiesFault");
            Object[] returnObject = sendToOtherServiceOnNode(context, "ConfigService", "unregisterService", null, faults);
            if (returnObject == null || returnObject[0] == null) {
                logging.warn("Unregistering failed! Reason: Unknown!");
                return false;
            }
            Object retOne = returnObject[0];
            if (retOne instanceof String && ((String) retOne).equals("Unknown!")) {
                logging.warn("Unregistering failed! Reason: Unknown!");
                return false;
            }
            if (retOne instanceof DPWSFault) {
                DPWSFault f = (DPWSFault) retOne;
                logging.warn("Unregistering failed! Reason: " + f.getReason() + " Fault: " + f.getFaultCode().getLocalPart());
                return false;
            }
            Class responseClass = getClassOfAnotherBundleByName(bundleContext, "ConfigService", "eu.more.configservice.generated.jaxb.impl.UnregisterServiceResponseImpl");
            Object response = null;
            if ((returnObject != null) && (returnObject[0] != null)) response = responseClass.cast(returnObject[0]); else return false;
            String valueOfResponse = (String) responseClass.getMethod("getOut").invoke(response);
            isRegistered = false;
            return valueOfResponse == "Success!";
        } catch (NullPointerException e) {
            throw new DPWSException(e);
        } catch (InstantiationException e) {
            throw new DPWSException(e);
        } catch (IllegalAccessException e) {
            throw new DPWSException(e);
        } catch (SecurityException e) {
            throw new DPWSException(e);
        } catch (NoSuchMethodException e) {
            throw new DPWSException(e);
        } catch (IllegalArgumentException e) {
            throw new DPWSException(e);
        } catch (InvocationTargetException e) {
            throw new DPWSException(e);
        }
    }

    private Object checkExceptionForFaults(DPWSFault fault, List<String> faultMessageImplClasses) {
        QName faultCode = fault.getFaultCode();
        if (faultCode == null) return null;
        String string = faultCode.toString();
        System.out.println(string);
        for (String faultImplClass : faultMessageImplClasses) {
            System.out.println(faultImplClass);
            if (string.indexOf(faultImplClass) >= 0) {
                return fault;
            }
        }
        return "Unknown!";
    }
}
