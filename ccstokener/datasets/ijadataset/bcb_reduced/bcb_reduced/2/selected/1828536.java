package au.edu.archer.metadata.mdsr.utils;

import static au.edu.archer.metadata.mdsr.config.RepositoryConstants.FEDORA_PASSWORD;
import static au.edu.archer.metadata.mdsr.config.RepositoryConstants.FEDORA_REPOSITORY_URL;
import static au.edu.archer.metadata.mdsr.config.RepositoryConstants.FEDORA_USERNAME;
import static au.edu.archer.metadata.mdsr.config.RepositoryConstants.OBJECT_NAMESPACE;
import java.io.StringWriter;
import java.net.URL;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import au.edu.archer.metadata.mdsr.repository.FedoraRepositoryAccessAPI;
import au.edu.archer.metadata.mdsr.repository.FedoraRepositoryManagmentAPI;

/**
 * Helper class to perform common operations
 *
 * @author alabri
 *
 */
public class MDSRepositoryHelper {

    private static Logger logger = Logger.getLogger(MDSRepositoryHelper.class);

    private static String url = PropertyContainer.instance().getProperty(FEDORA_REPOSITORY_URL);

    private static String username = PropertyContainer.instance().getProperty(FEDORA_USERNAME);

    private static String password = PropertyContainer.instance().getProperty(FEDORA_PASSWORD);

    /**
     * Returns an instance of FedoraRepositoryManagmentAPI
     *
     * @return instance of FedoraRepositoryManagmentAPI
     */
    public static FedoraRepositoryManagmentAPI getManagementAPI() {
        return new FedoraRepositoryManagmentAPI(url, username, password);
    }

    /**
     * Returns an instance of FedoraRepositoryAccessAPI
     *
     * @return instance of FedoraRepositoryAccessAPI
     */
    public static FedoraRepositoryAccessAPI getAccessAPI() {
        return new FedoraRepositoryAccessAPI(url, username, password);
    }

    /**
     * This method sends responses to clients.
     *
     * @param status
     *            The status of ingestion operation
     * @param message
     *            A message to the client
     * @param response
     *            The servlet response
     */
    public static void sendResponse(int status, String message, HttpServletResponse response) {
        try {
            response.sendError(status, message);
        } catch (Exception e) {
            logger.error("Unable to send response", e);
        }
    }

    public static void updateDcRecord(String pid, String dcSubject, String dcCreator, String dcPublisher, String dcContributor, String dcDescription) {
        DocumentBuilder builder;
        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
            String dcRecordURL = PropertyContainer.instance().getProperty(FEDORA_REPOSITORY_URL) + "/get/" + OBJECT_NAMESPACE + pid + "/DC";
            URL url = new URL(dcRecordURL);
            InputSource is = new InputSource(url.openStream());
            doc = builder.parse(is);
            Element root = doc.getDocumentElement();
            Node subjectNode = doc.createElement("dc:subject");
            subjectNode.appendChild(doc.createTextNode(dcSubject));
            root.appendChild(subjectNode);
            Node creatorNode = doc.createElement("dc:creator");
            creatorNode.appendChild(doc.createTextNode(dcCreator));
            root.appendChild(creatorNode);
            Node publisherNode = doc.createElement("dc:publisher");
            publisherNode.appendChild(doc.createTextNode(dcPublisher));
            root.appendChild(publisherNode);
            Node contributorNode = doc.createElement("dc:contributor");
            contributorNode.appendChild(doc.createTextNode(dcContributor));
            root.appendChild(contributorNode);
            Node descriptionNode = doc.createElement("dc:description");
            descriptionNode.appendChild(doc.createTextNode(dcDescription));
            root.appendChild(descriptionNode);
            StringWriter sw = new StringWriter();
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(sw);
            transformer.transform(source, result);
            String xmlString = sw.toString();
            getManagementAPI().modifyDatastreamByValue(pid, "DC", new String[] {}, "Dublin Core Metadata", "text/xml", "", xmlString.getBytes(), null, null, "Edited DC Record", true);
        } catch (Exception ex) {
            logger.error("Failed to load XML content", ex);
        }
    }
}
