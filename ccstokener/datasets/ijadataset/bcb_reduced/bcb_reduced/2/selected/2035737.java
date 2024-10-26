package org.gdi3d.xnavi.services.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.opengis.cat.csw.x202.GetRecordByIdResponseDocument;
import net.opengis.cat.csw.x202.GetRecordByIdResponseType;
import net.opengis.cat.csw.x202.GetRecordsResponseDocument;
import net.opengis.cat.csw.x202.GetRecordsResponseType;
import net.opengis.cat.csw.x202.SearchResultsType;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import x0.oasisNamesTcEbxmlRegrepXsdRim3.ClassificationType;
import x0.oasisNamesTcEbxmlRegrepXsdRim3.RegistryObjectDocument;
import x0.oasisNamesTcEbxmlRegrepXsdRim3.RegistryObjectType;
import x0.oasisNamesTcEbxmlRegrepXsdRim3.ServiceBindingType;
import x0.oasisNamesTcEbxmlRegrepXsdRim3.ServiceDocument;
import x0.oasisNamesTcEbxmlRegrepXsdRim3.ServiceType;
import org.gdi3d.xnavi.navigator.Navigator;

public class WebCatalogService_XMLBeans {

    private String serviceEndPoint;

    public WebCatalogService_XMLBeans(String serviceEndPoint) {
        this.serviceEndPoint = serviceEndPoint;
    }

    public String getServiceEndPoint() {
        return serviceEndPoint;
    }

    public void setServiceEndPoint(String serviceEndPoint) {
        this.serviceEndPoint = serviceEndPoint;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        WebCatalogService_XMLBeans c = new WebCatalogService_XMLBeans("http://registry.wrs.galdosinc.com/ows6/query");
        Vector<ServiceType> wmss;
        wmss = c.search("urn:ogc:serviceType:WebMapService:1.1");
        System.out.println("found " + wmss.size() + " WMS");
        for (int i = 0; i < wmss.size(); i++) {
            ServiceType wms = wmss.get(i);
            System.out.println("wms.getName() " + wms.getName());
            System.out.println("wms.getDescription() " + wms.getDescription());
            ServiceBindingType[] serviceBindings = wms.getServiceBindingArray();
            String accessURI = null;
            for (int j = 0; j < serviceBindings.length; j++) {
                ServiceBindingType serviceBinding = serviceBindings[j];
                if (serviceBinding != null) {
                    if (serviceBinding.getAccessURI() != null) {
                        accessURI = serviceBinding.getAccessURI();
                    }
                }
            }
            System.out.println("wms accessURI " + accessURI);
        }
    }

    public Vector<ServiceType> search() throws XmlException {
        return search(null);
    }

    public Vector<ServiceType> search(String classificationNode) {
        Vector<ServiceType> services = new Vector<ServiceType>();
        String errorMessage = "";
        try {
            GetRecordsResponseType getRecordsResponse = findServices();
            if (getRecordsResponse != null) {
                SearchResultsType searchResults = getRecordsResponse.getSearchResults();
                int numresults = searchResults.getNumberOfRecordsReturned().intValue();
                System.out.println("WebCatalogService.test found " + numresults + " results");
                Node node = searchResults.getDomNode();
                NodeList childNodes = node.getChildNodes();
                int childNodes_length = childNodes.getLength();
                for (int i = 0; i < childNodes_length; i++) {
                    Node childNode = childNodes.item(i);
                    if (childNode.getLocalName() != null && childNode.getLocalName().equals("RegistryObject")) {
                        RegistryObjectDocument registryObjectDocument = RegistryObjectDocument.Factory.parse(childNode);
                        RegistryObjectType registryObject = registryObjectDocument.getRegistryObject();
                        System.out.println("found registryObject " + registryObject.toString());
                        String id = registryObject.getId();
                        System.out.println(id);
                        System.out.println("registryObject.getId() " + registryObject.getId());
                        System.out.println("registryObject.getLid() " + registryObject.getLid());
                        System.out.println("registryObject.getObjectType() " + registryObject.getObjectType());
                        System.out.println("registryObject.getStatus() " + registryObject.getStatus());
                        System.out.println("registryObject.getDescription() " + registryObject.getDescription());
                        System.out.println("registryObject.getName() " + registryObject.getName());
                        System.out.println("registryObject.getVersionInfo() " + registryObject.getVersionInfo());
                        if (id != null) {
                            GetRecordByIdResponseType getRecordByIdResponse = GetRecordById(id);
                            ServiceType service = null;
                            Node node1 = getRecordByIdResponse.getDomNode();
                            NodeList childNodes1 = node1.getChildNodes();
                            int childNodes_length1 = childNodes1.getLength();
                            for (int j = 0; j < childNodes_length1; j++) {
                                Node childNode1 = childNodes1.item(j);
                                System.out.println("childNode1.getNamespaceURI() " + childNode1.getNamespaceURI());
                                System.out.println("childNode1.getLocalName() " + childNode1.getLocalName());
                                System.out.println("childNode1.getNodeName() " + childNode1.getNodeName());
                                System.out.println("childNode1.getNodeType() " + childNode1.getNodeType());
                                System.out.println("childNode1.getNodeValue() " + childNode1.getNodeValue());
                                System.out.println("childNode1.getPrefix() " + childNode1.getPrefix());
                                if (childNode1.getLocalName() != null && childNode1.getLocalName().equals("Service")) {
                                    ServiceDocument serviceDocument = ServiceDocument.Factory.parse(childNode1);
                                    service = serviceDocument.getService();
                                    System.out.println("found service " + service.toString());
                                    if (service != null) {
                                        ClassificationType[] classificationArray = service.getClassificationArray();
                                        if (classificationArray != null) {
                                            for (int k = 0; k < classificationArray.length; k++) {
                                                ClassificationType classification = classificationArray[k];
                                                String cn = classification.getClassificationNode();
                                                if (classificationNode == null || (cn != null && cn.equals(classificationNode))) {
                                                    services.add(service);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (ConnectException e) {
            errorMessage += "<p>Connection refused</p>";
            e.printStackTrace();
        } catch (XmlException e) {
            errorMessage += "<p>Malformed XML</p>";
            e.printStackTrace();
        } catch (Exception e) {
            errorMessage += "<p>Error. See Console for Details</p>";
            e.printStackTrace();
        }
        if (!errorMessage.equals("")) {
            JLabel label1 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: bold;}--></style></head><body><span class=\"Stil2\">Catalog Error</span></body></html>");
            JLabel label2 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: normal;}--></style></head><body><span class=\"Stil2\">" + "<br>" + errorMessage + "<br>" + "</span></body></html>");
            Object[] objects = { label1, label2 };
            JOptionPane.showMessageDialog(null, objects, Navigator.i18n.getString("WARNING_MESSAGE"), JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return services;
    }

    public GetRecordByIdResponseType GetRecordById(String id) throws IOException, XmlException {
        GetRecordByIdResponseType getRecordByIdResponse = null;
        String getRequest = serviceEndPoint + "?request=GetRecordById&ElementSetName=full&id=" + id;
        System.out.println("getRequest " + getRequest);
        if (Navigator.isVerbose()) {
            System.out.println("contacting " + getRequest);
        }
        URL url = new URL(getRequest);
        URLConnection urlc = url.openConnection();
        urlc.setReadTimeout(Navigator.TIME_OUT);
        InputStream urlIn = urlc.getInputStream();
        GetRecordByIdResponseDocument obsCollDoc = GetRecordByIdResponseDocument.Factory.parse(urlIn);
        getRecordByIdResponse = obsCollDoc.getGetRecordByIdResponse();
        return getRecordByIdResponse;
    }

    public GetRecordsResponseType findServices() throws IOException, XmlException {
        GetRecordsResponseType getRecordsResponse = null;
        String getRequest = serviceEndPoint + "?request=Query&qid=urn:ogc:def:ebRIM-Query:OGC:findServices";
        System.out.println("getRequest " + getRequest);
        if (Navigator.isVerbose()) {
            System.out.println("contacting " + getRequest);
        }
        URL url = new URL(getRequest);
        URLConnection urlc = url.openConnection();
        urlc.setReadTimeout(Navigator.TIME_OUT);
        InputStream urlIn = urlc.getInputStream();
        GetRecordsResponseDocument obsCollDoc = GetRecordsResponseDocument.Factory.parse(urlIn);
        getRecordsResponse = obsCollDoc.getGetRecordsResponse();
        return getRecordsResponse;
    }
}
