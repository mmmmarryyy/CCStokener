package eu.more.gms;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.soda.dpws.DPWSContext;
import org.soda.dpws.DPWSException;
import org.soda.dpws.DeviceExplorer;
import org.soda.dpws.ServiceProxy;
import org.soda.dpws.cache.CachedDevice;
import org.soda.dpws.cache.CachedService;
import org.soda.dpws.metadata.MetadataSection;
import org.soda.dpws.util.jdom.StaxBuilder;
import org.soda.dpws.util.parser.stax.InputFactory;
import eu.more.gms.Exceptions.AlreadyAddedException;
import eu.more.gms.Exceptions.EmptyStringException;
import eu.more.gms.Exceptions.NoGroupFoundException;
import eu.more.gms.Exceptions.ServiceNotInGroupException;
import eu.more.gms.generated.GroupManagementNotifier;
import eu.more.gms.generated.jaxb.AddedToGroupNotificationResponse;
import eu.more.gms.generated.jaxb.GroupDetails;
import eu.more.gms.generated.jaxb.ObjectFactory;
import eu.more.gms.generated.jaxb.QueryGroup;
import eu.more.gms.generated.jaxb.RemovedFromGroupNotificationResponse;

/**
 * 
 * This class implements the four functions, they are addmember, deletemember, creategroup and deletegroup.
 *@author Niall Donnelly
 *  
 * 
 * 
 */
public class groupManagementServiceLogic {

    private HashMap<String, GroupDetails> groups;

    private ObjectFactory factory;

    private HashMap<String, ArrayList<ServiceProxy>> opsList;

    private HashMap<String, ArrayList<String>> opsListForService;

    private DeviceExplorer proxyExplorer;

    private static String DEMONSTRATOR_SCOPE = "http://www.ist-more.org/gms";

    private static Logger logger = null;

    /**
	 * constructure   
	 * @param logger2
	 */
    public groupManagementServiceLogic(Logger logger2) {
        groups = new HashMap<String, GroupDetails>();
        factory = new ObjectFactory();
        opsList = new HashMap<String, ArrayList<ServiceProxy>>();
        opsListForService = new HashMap<String, ArrayList<String>>();
        this.logger = logger2;
        try {
            logger.log(Level.INFO, "Starting deviceExplorer in GroupManagementServiceLogic");
            proxyExplorer = new DeviceExplorer();
        } catch (DPWSException e) {
            logger.log(Level.WARNING, "Error Starting deviceExplorer in GroupManagementServiceLogic", e);
            e.printStackTrace();
        }
    }

    /**
	 * Add one or more members into group
	 * @param context
	 * @param addMemberToGroup
	 * @param opsList2
	 * @param methodNameToOperation
	 * @return Boolean
	 * @throws EmptyStringException
	 * @throws AlreadyAddedException
	 * @throws NullPointerException
	 * @throws NoGroupFoundException
	 * @throws DPWSException
	 */
    @SuppressWarnings("unchecked")
    public boolean addMemberToGroup(DPWSContext context, GroupDetails addMemberToGroup, HashMap<String, ArrayList<ServiceProxy>> opsList2, HashMap<String, String> methodNameToOperation) throws EmptyStringException, AlreadyAddedException, NullPointerException, NoGroupFoundException, DPWSException {
        boolean successful = false;
        GroupDetails groupToAddServiceTo = null;
        int no_already_added_services = 0;
        if (addMemberToGroup != null) {
            if (addMemberToGroup.getGroupName() != null && addMemberToGroup.getGroupName().length() > 0) {
                logger.log(Level.INFO, "Getting group to add the service to");
                groupToAddServiceTo = groups.get(addMemberToGroup.getGroupName());
                if (groupToAddServiceTo != null) {
                    Iterator<ServiceProxy> it = addMemberToGroup.getMember().iterator();
                    while (it.hasNext()) {
                        ServiceProxy serviceToAdd = it.next();
                        if (checkDuplicateServiceInGroup(groupToAddServiceTo, serviceToAdd)) {
                            logger.log(Level.INFO, "The service " + serviceToAdd.getId() + " is already in the group");
                            successful = false;
                            no_already_added_services++;
                        } else {
                            logger.log(Level.INFO, "Service not already in group, adding now");
                            groupToAddServiceTo.getMember().add(serviceToAdd);
                            logger.log(Level.INFO, "Adding service operations to the operations list");
                            addToOpsList(serviceToAdd, opsList2, methodNameToOperation);
                            logger.log(Level.INFO, "Sending added to group notification");
                            addedToGroupNotification(context);
                            successful = true;
                        }
                    }
                    if (successful) {
                        logger.log(Level.INFO, "Adding Services was successful");
                        return true;
                    } else {
                        logger.log(Level.INFO, "Some services are already in the group");
                        throw new AlreadyAddedException(no_already_added_services + " services are already part of the " + addMemberToGroup.getGroupName() + " group.");
                    }
                } else {
                    throw new NoGroupFoundException("The group to add a service to is empty");
                }
            } else {
                throw new EmptyStringException("The group name to add a string to is empty");
            }
        } else {
            throw new NullPointerException("The group Details are NULL");
        }
    }

    /**
	 * Check the Service is already exits
	 * @param groupToAddServiceTo
	 * @param serviceToAdd
	 * @return
	 * @throws DPWSException
	 */
    private boolean checkDuplicateServiceInGroup(GroupDetails groupToAddServiceTo, ServiceProxy serviceToAdd) throws DPWSException {
        logger.log(Level.INFO, "Checking for duplicate services in the group");
        if (serviceToAdd != null) {
            Iterator it = groupToAddServiceTo.getMember().iterator();
            MetadataSection test2 = (MetadataSection) serviceToAdd.getServiceMetadata().getWsdls().get(0);
            while (it.hasNext()) {
                CachedService serv = (CachedService) it.next();
                if (serviceToAdd.getId().equals(serv.getId())) {
                    CachedService c_serv = (CachedService) serviceToAdd;
                    CachedDevice device = c_serv.getHost();
                    if (device.getId().equals(serv.getHost().getId())) {
                        String deviceSer = device.getDeviceMetadata().getDeviceInfo().getSerialNumber();
                        String deviceSer2 = serv.getHost().getDeviceMetadata().getDeviceInfo().getSerialNumber();
                        if (deviceSer.equals(deviceSer2)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } else {
            throw new DPWSException("The service to add was null");
        }
    }

    @SuppressWarnings("unchecked")
    private void addToOpsList(ServiceProxy serviceToAdd, HashMap<String, ArrayList<ServiceProxy>> opsList2, HashMap<String, String> methodNameToOperation) {
        MetadataSection meta = null;
        try {
            logger.log(Level.INFO, "Getting wsdl location from the service");
            meta = (MetadataSection) serviceToAdd.getServiceMetadata().getWsdls().get(0);
        } catch (DPWSException e1) {
            logger.log(Level.WARNING, "Error getting metadata for the service To add", e1);
            e1.printStackTrace();
        }
        try {
            logger.log(Level.INFO, "Setting up the url for adding to the ops list");
            URL url = new URL(meta.getLocation());
            logger.log(Level.INFO, "The URL for the service is" + meta.getLocation());
            StaxBuilder t = new StaxBuilder();
            InputFactory n = new InputFactory();
            XMLStreamReader reader = null;
            try {
                logger.log(Level.INFO, "Reading the stream from the url to the XMLStreamReader");
                reader = n.createXMLStreamReader(url.openStream());
            } catch (XMLStreamException e) {
                logger.log(Level.WARNING, "Error while trying to make an XMLStreamReader from url stream input", e);
                e.printStackTrace();
            }
            Document testDoc = null;
            try {
                logger.log(Level.INFO, "Trying to build the JDOM Document from the XMLStream");
                testDoc = t.build(reader);
            } catch (XMLStreamException e) {
                logger.log(Level.WARNING, "Error building the JDOM Document from the XMLStream", e);
                e.printStackTrace();
            }
            List contentList1 = testDoc.getContent();
            Element content1 = (Element) contentList1.get(0);
            logger.log(Level.INFO, "Filtering for portType in the Document");
            ElementFilter elFilter = new ElementFilter("portType");
            List filteredContent = content1.getContent(elFilter);
            Element portTypeElement = (Element) filteredContent.get(0);
            String portTypeName = portTypeElement.getAttributeValue("name");
            logger.log(Level.INFO, "Filtering for operations in the portType");
            ElementFilter opsFilter = new ElementFilter("operation");
            List filteredOps = portTypeElement.getContent(opsFilter);
            for (int i = 0; i < filteredOps.size(); i++) {
                Element opsElement = (Element) filteredOps.get(i);
                String opName = opsElement.getAttributeValue("name");
                logger.log(Level.INFO, "Filtering for input in the operations");
                ElementFilter inputFilter = new ElementFilter("input");
                List inputActionList = opsElement.getContent(inputFilter);
                if (inputActionList != null && inputActionList.size() > 0) {
                    logger.log(Level.INFO, "Operation has an input");
                    String totalRequestString = null;
                    Element requestElement = (Element) inputActionList.get(0);
                    String inputName = requestElement.getAttributeValue("message");
                    StringTokenizer tokenizer = new StringTokenizer(inputName, ":");
                    String prefix = tokenizer.nextToken();
                    String uri = null;
                    Iterator it = requestElement.getAdditionalNamespaces().iterator();
                    while (it.hasNext()) {
                        Namespace namespaceAttribute = (Namespace) it.next();
                        if (prefix.equals(namespaceAttribute.getPrefix())) {
                            uri = namespaceAttribute.getURI();
                            break;
                        }
                    }
                    logger.log(Level.INFO, "Filtering for output in the operation");
                    ElementFilter outputFilter = new ElementFilter("output");
                    List outputActionList = opsElement.getContent(outputFilter);
                    if (outputActionList != null && outputActionList.size() > 0) {
                        logger.log(Level.INFO, "Has Output");
                        totalRequestString = uri + portTypeName + "/" + tokenizer.nextToken();
                    } else {
                        logger.log(Level.INFO, "Does not have output");
                        totalRequestString = uri + portTypeName + "/" + opName;
                    }
                    String uniqueOpName = opName + "*" + serviceToAdd.getId();
                    methodNameToOperation.put(uniqueOpName, totalRequestString);
                    if (opsList2.get(opName) != null) {
                        opsList2.get(opName).add(serviceToAdd);
                    } else {
                        ArrayList<ServiceProxy> opsList = new ArrayList<ServiceProxy>();
                        opsList.add(serviceToAdd);
                        opsList2.put(opName, opsList);
                        if (opsListForService.get(serviceToAdd.getId()) == null) {
                            ArrayList<String> serviceList = new ArrayList<String>();
                            serviceList.add(opName);
                            opsListForService.put(serviceToAdd.getId(), serviceList);
                        } else {
                            opsListForService.get(serviceToAdd.getId()).add(opName);
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, "Error with the URL when adding operations to the operation list", e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error with getting the stream from the URL", e);
        }
    }

    /**
	 * Noice the service when it is added in group
	 * @param context
	 */
    public void addedToGroupNotification(DPWSContext context) {
        AddedToGroupNotificationResponse addedNotification = null;
        try {
            addedNotification = factory.createAddedToGroupNotificationResponse();
            addedNotification.setOut("Service added");
            GroupManagementNotifier notifyAdded = new GroupManagementNotifier(context.getService());
            notifyAdded.notifyaddedToGroupNotification(addedNotification);
        } catch (JAXBException e) {
            logger.log(Level.WARNING, "JAXBException when calling addedToGroupNotification", e);
            e.printStackTrace();
        } catch (DPWSException e) {
            logger.log(Level.WARNING, "DPWSException when calling addedToGroupNotification", e);
            e.printStackTrace();
        }
    }

    /**
	 * Add a new group
	 * @param context
	 * @param createGroup
	 * @param opsList2
	 * @param methodNameToOperation
	 * @return Boolean
	 * @throws AlreadyAddedException
	 * @throws EmptyStringException
	 * @throws NullPointerException
	 */
    public boolean createGroup(DPWSContext context, GroupDetails createGroup, HashMap<String, ArrayList<ServiceProxy>> opsList2, HashMap<String, String> methodNameToOperation) throws AlreadyAddedException, EmptyStringException, NullPointerException {
        if (createGroup != null) {
            if (createGroup.getGroupName() != null) {
                if (createGroup.getGroupName().length() > 0) {
                    if (!groups.containsKey(createGroup.getGroupName())) {
                        if (createGroup.getMember() != null) {
                            Iterator it = createGroup.getMember().iterator();
                            while (it.hasNext()) {
                                ServiceProxy newService = (ServiceProxy) it.next();
                                addToOpsList(newService, opsList2, methodNameToOperation);
                                addedToGroupNotification(context);
                            }
                        }
                        logger.log(Level.INFO, "Generating group Id");
                        double id = Math.random() * 10000000;
                        Double d = new Double(id);
                        createGroup.setGroupIdentifier(d.toString());
                        groups.put(createGroup.getGroupName(), createGroup);
                        return true;
                    } else {
                        throw new AlreadyAddedException("The group " + createGroup.getGroupName() + " already exists");
                    }
                } else {
                    throw new EmptyStringException("There is no name entered for the new group");
                }
            } else {
                throw new NullPointerException("The group Name does not exist");
            }
        } else {
            throw new NullPointerException("The group that was passed in to createGroup was NULL");
        }
    }

    public boolean deleteGroup(DPWSContext context, GroupDetails deleteGroup, HashMap<String, ArrayList<ServiceProxy>> hashMap, HashMap<String, String> methodNameToOperation) throws EmptyStringException, NullPointerException, NoGroupFoundException, DPWSException {
        if (deleteGroup != null) {
            if (deleteGroup.getGroupName() != null && deleteGroup.getGroupName().length() > 0) {
                if (groups.containsKey(deleteGroup.getGroupName())) {
                    Iterator it = groups.get(deleteGroup.getGroupName()).getMember().iterator();
                    while (it.hasNext()) {
                        ServiceProxy deletedMember = (ServiceProxy) it.next();
                        removeFromOpsList(deletedMember, hashMap, methodNameToOperation);
                        removedFromGroupNotification(context);
                    }
                    groups.remove(deleteGroup.getGroupName());
                    return true;
                } else {
                    throw new NoGroupFoundException("The group you are trying to delete does not exist");
                }
            } else {
                throw new EmptyStringException("The group name parameter for the deleteGroup() method is empty");
            }
        } else {
            throw new NullPointerException("The groupName is NULL");
        }
    }

    /**
	 * Remove a member from given group 
	 * @param context
	 * @param deleteMemberFromGroup
	 * @param hashMap
	 * @param methodNameToOperation
	 * @return boolean
	 * @throws NullPointerException
	 * @throws NoGroupFoundException
	 * @throws EmptyStringException
	 * @throws ServiceNotInGroupException
	 * @throws DPWSException
	 */
    public boolean deleteMemberFromGroup(DPWSContext context, GroupDetails deleteMemberFromGroup, HashMap<String, ArrayList<ServiceProxy>> hashMap, HashMap<String, String> methodNameToOperation) throws NullPointerException, NoGroupFoundException, EmptyStringException, ServiceNotInGroupException, DPWSException {
        boolean success = true;
        GroupDetails groupToRemoveServiceFrom = null;
        int services_not_deleted = 0;
        if (deleteMemberFromGroup != null) {
            if (deleteMemberFromGroup.getGroupName() != null && deleteMemberFromGroup.getGroupName().length() > 0) {
                groupToRemoveServiceFrom = groups.get(deleteMemberFromGroup.getGroupName());
                if (groupToRemoveServiceFrom != null) {
                    Iterator<ServiceProxy> it = deleteMemberFromGroup.getMember().iterator();
                    while (it.hasNext()) {
                        ServiceProxy test = (ServiceProxy) it.next();
                        if (checkIfMemberInGroup(groupToRemoveServiceFrom, test)) {
                            removeFromOpsList(test, hashMap, methodNameToOperation);
                            if (removeMemberFromGroup(groupToRemoveServiceFrom, test)) {
                                removedFromGroupNotification(context);
                            } else {
                                throw new DPWSException("Error Deleting service from the group");
                            }
                        } else {
                            success = false;
                            services_not_deleted++;
                        }
                    }
                    if (success) {
                        return true;
                    } else {
                        throw new ServiceNotInGroupException(services_not_deleted + " services were not in the group");
                    }
                } else {
                    throw new NoGroupFoundException("The group " + deleteMemberFromGroup.getGroupName() + " does not correspond with any groups in the list.");
                }
            } else {
                throw new EmptyStringException("The service to be added to the " + deleteMemberFromGroup.getGroupName() + " groups is NULL");
            }
        } else {
            throw new NullPointerException("The group parameter is NULL");
        }
    }

    private boolean removeMemberFromGroup(GroupDetails groupToRemoveServiceFrom, ServiceProxy test) {
        Iterator it = groupToRemoveServiceFrom.getMember().iterator();
        while (it.hasNext()) {
            ServiceProxy serv = (ServiceProxy) it.next();
            if (test.getId().equals(serv.getId())) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    private boolean checkIfMemberInGroup(GroupDetails groupToRemoveServiceFrom, ServiceProxy test) throws DPWSException {
        Iterator it = groupToRemoveServiceFrom.getMember().iterator();
        while (it.hasNext()) {
            CachedService serv = (CachedService) it.next();
            if (test.getId().equals(serv.getId())) {
                CachedService c_serv = (CachedService) test;
                CachedDevice device = c_serv.getHost();
                if (device.getId().equals(serv.getHost().getId())) {
                    String deviceSer = device.getDeviceMetadata().getDeviceInfo().getSerialNumber();
                    String deviceSer2 = serv.getHost().getDeviceMetadata().getDeviceInfo().getSerialNumber();
                    if (deviceSer.equals(deviceSer2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void removeFromOpsList(ServiceProxy serviceToRemove, HashMap<String, ArrayList<ServiceProxy>> opsList, HashMap<String, String> methodNameToOperation) throws DPWSException {
        ArrayList<String> opsForService = opsListForService.get(serviceToRemove.getId());
        if (opsForService != null && opsForService.size() > 0) {
            Iterator<String> it = opsForService.iterator();
            while (it.hasNext()) {
                String opName = it.next();
                String uniqueOpName = opName + "*" + serviceToRemove.getId();
                if (methodNameToOperation.remove(uniqueOpName) == null) {
                }
                ArrayList<ServiceProxy> servicesForOp = opsList.get(opName);
                Iterator<ServiceProxy> it2 = servicesForOp.iterator();
                while (it2.hasNext()) {
                    if (it2.next().getId().equals(serviceToRemove.getId())) {
                        it2.remove();
                        break;
                    }
                }
            }
        }
    }

    /**
	 * Get a GroupDetails
	 * @param queryDetails
	 * @return GroupDetails
	 * @throws NoGroupFoundException
	 * @throws NullPointerException
	 * @throws EmptyStringException
	 */
    public GroupDetails queryGroup(QueryGroup queryDetails) throws NoGroupFoundException, NullPointerException, EmptyStringException {
        if (queryDetails != null) {
            if (queryDetails.getGroupName() != null && queryDetails.getGroupName().length() > 0) {
                GroupDetails tempGroup = groups.get(queryDetails.getGroupName());
                if (tempGroup != null) {
                    return tempGroup;
                } else {
                    return null;
                }
            } else {
                throw new EmptyStringException("The groupName parameter in queryDetails is missing");
            }
        } else {
            throw new NullPointerException("No parameter entered");
        }
    }

    /**
	 * Notice the Service it have been removed.
	 * @param context
	 * 
	 */
    public void removedFromGroupNotification(DPWSContext context) {
        RemovedFromGroupNotificationResponse notifyRemovedResponse = null;
        try {
            notifyRemovedResponse = factory.createRemovedFromGroupNotificationResponse();
            notifyRemovedResponse.setOut("Group Removed");
            GroupManagementNotifier notifyRemoved = new GroupManagementNotifier(context.getService());
            notifyRemoved.notifyremovedFromGroupNotification(notifyRemovedResponse);
        } catch (JAXBException e) {
            logger.log(Level.WARNING, "JAXBException when calling removedFromGroupNotification", e);
            e.printStackTrace();
        } catch (DPWSException e) {
            logger.log(Level.WARNING, "DPWSException when calling removedFromGroupNotification", e);
            e.printStackTrace();
        }
    }
}
