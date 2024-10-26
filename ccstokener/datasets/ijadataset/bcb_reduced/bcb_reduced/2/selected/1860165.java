package test.xmldb;

import java.io.*;
import junit.framework.*;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.CollectionManagementService;
import java.util.Properties;
import java.util.logging.Logger;
import java.net.URL;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.DOMImplementation;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xerces.dom.DOMImplementationImpl;
import org.ozoneDB.xml.xmldb.cli.CollectionStorageHelper;
import org.ozoneDB.xml.xmldb.cli.CollectionFactory;

/**
 * This is the fixture for the other tests
 */
public class XMLDBTestCase extends TestCase {

    /** the <code>Collection</code> that we use in all tests */
    protected Collection col;

    /** File containing the XML to be used for SAX tests*/
    protected String xmlFileName = "LevelZeroTest.xml";

    /** a starting <code>Document</code> corresponding to the fileName for DOM tests*/
    protected Document document;

    /** the collection used for testing */
    protected String rootCollectionName;

    private CollectionStorageHelper collectionStorageHelper;

    private Logger logger = Logger.getLogger(XMLDBTestCase.class.getName());

    public XMLDBTestCase(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        logger.finer("******************** set up ********************");
        Properties props;
        if (XMLDBTestSuite.propertiesFileName == null) {
            String defaultPropsFileLocation = "test/xmldb/XMLDBTestSuite.properties";
            URL url = this.getClass().getClassLoader().getResource(defaultPropsFileLocation);
            if (url == null) {
                throw new Exception("failed to find default props file at " + defaultPropsFileLocation);
            }
            props = loadProps(url.openConnection().getInputStream());
        } else {
            props = loadProps(XMLDBTestSuite.propertiesFileName);
        }
        String driver = props.getProperty("driverName");
        String collectionURI = props.getProperty("URI");
        Database database = (Database) Class.forName(driver).newInstance();
        collectionStorageHelper = new CollectionStorageHelper(collectionURI);
        rootCollectionName = collectionStorageHelper.getCollectionName();
        Collection root = database.getCollection(collectionURI, null, null);
        CollectionManagementService service = (CollectionManagementService) root.getService(CollectionManagementService.SERVICE_NAME, "1.0");
        String childCollection = "child";
        removeChildCollection(root, childCollection, service);
        col = service.createCollection(childCollection);
        assertNotNull("XMLDBTestCase.setUp() - Collection could not be created", col);
        logger.info("created child collection '" + col.getName() + "' parent is '" + col.getParentCollection().getName() + "'");
        assertEquals("Root collection name should match childs parent name", rootCollectionName, col.getParentCollection().getName());
        document = createXMLFile(xmlFileName);
        assertNotNull("XMLDBTestCase.setUp() - failed to create XML file", document);
    }

    private void removeChildCollection(Collection root, String childCollection, CollectionManagementService service) throws Exception {
        col = root.getChildCollection(childCollection);
        System.out.println("child collection = " + col);
        if (col != null) {
            System.out.println("cleaning up existing child collection");
            service.removeCollection(childCollection);
        }
        if (collectionStorageHelper.collectionExist(childCollection)) {
            fail("Attmpted to remove collection but it still exists!");
        }
    }

    public void tearDown() throws XMLDBException {
        logger.finer("******************** tear down ********************");
        collectionStorageHelper.deleteRootCollection(rootCollectionName);
        col.close();
        assertTrue("XMLDBTestCase.tearDown() - failed to delete XML file", deleteXMLFile(xmlFileName));
        logger.fine("Finished!");
    }

    /** convert a <code>Document</code>into a String */
    protected String toString(Document document) throws Exception {
        StringWriter writer = new StringWriter();
        XMLSerializer serializer = new XMLSerializer(writer, new OutputFormat("xml", "UTF-8", true));
        serializer.serialize(document);
        writer.flush();
        return writer.toString();
    }

    /** convert a <code>Node</code>into a String */
    protected String toString(Node node) throws Exception {
        try {
            if (node instanceof Document) {
                return toString((Document) node);
            } else {
                Document doc = node.getOwnerDocument();
                return toString(doc);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    protected void deleteResource(String id) throws Exception {
        col.removeResource(col.getResource(id));
    }

    private Properties loadProps(InputStream in) {
        Properties props = createDefaultProps();
        try {
            props.load(in);
        } catch (Exception e) {
            logger.warning("Didn't find props file, using defaults");
            props.list(System.out);
        }
        return props;
    }

    private Properties loadProps(String iniFileName) {
        Properties props = createDefaultProps();
        try {
            props.load(new FileInputStream(new File(iniFileName)));
        } catch (Exception e) {
            logger.warning("Didn't find props file, using defaults");
            props.list(System.out);
        }
        return props;
    }

    /**
     *  Helper method to load the XMLDBTestCase.properties file
     *
     *  @return the loaded properties
     */
    private Properties createDefaultProps() {
        Properties defaultProps = new Properties();
        defaultProps.put("driverName", "org.xmldb.api.reference.DatabaseImpl");
        defaultProps.put("URI", "xmldb:ref:///child1");
        Properties props = new Properties(defaultProps);
        return props;
    }

    protected Document createXMLFile(String fileName) throws Exception {
        logger.finer("XMLDBTestCase.createXMLFile() - Writing file= " + fileName);
        FileWriter out = new FileWriter(fileName);
        DOMImplementation documentCreator = new DOMImplementationImpl();
        Document doc = documentCreator.createDocument(null, "XMLDBTests", null);
        Element root = doc.getDocumentElement();
        Element levelZeroTests = doc.createElement("levelZeroTests");
        levelZeroTests.setAttribute("complianceLevel", "0");
        root.appendChild(levelZeroTests);
        Element testName = doc.createElement("testName");
        levelZeroTests.appendChild(testName);
        Node name = doc.createTextNode("testBinary");
        testName.appendChild(name);
        testName = doc.createElement("testName");
        levelZeroTests.appendChild(testName);
        name = doc.createTextNode("testDOM");
        testName.appendChild(name);
        OutputFormat format = new OutputFormat(doc, "UTF-8", true);
        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.serialize(doc);
        out.close();
        return doc;
    }

    protected boolean deleteXMLFile(String xmlFileName) {
        File file = new File(xmlFileName);
        return file.delete();
    }
}
