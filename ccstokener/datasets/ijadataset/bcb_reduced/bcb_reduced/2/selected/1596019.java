package uk.ac.osswatch.simal.importData.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.osswatch.simal.importData.PTSWImport;
import uk.ac.osswatch.simal.rdf.SimalException;
import uk.ac.osswatch.simal.rdf.io.RDFUtils;

public class TestPTSWImport {

    private static final int NUM_OF_PINGS = 3;

    static PTSWImport importer;

    static Document ptswExport;

    @BeforeClass
    public static void createImporter() {
        importer = new PTSWImport();
    }

    @BeforeClass
    public static void readExportDoc() throws ParserConfigurationException, SAXException, IOException {
        URL url = TestPTSWImport.class.getResource("/testData/ptswExport.xml");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        ptswExport = db.parse(url.openStream());
    }

    @Test
    public void testListOfPings() throws SimalException {
        Set<URI> pings = importer.getListOfPings(ptswExport);
        assertEquals("Incorrect number of pings", NUM_OF_PINGS, pings.size());
    }

    /**
   * Test the generation of an RDF/XML document containing all pinged DOAP
   * files. Allow for failure of project imports when offline operation
   * is detected.
   * 
   * @throws SimalException
   * @throws IOException
   */
    @Test
    public void testRDFXML() throws SimalException, IOException {
        Document doc = importer.getPingsAsRDF(ptswExport);
        Element root = doc.getDocumentElement();
        NodeList projects = root.getElementsByTagNameNS(RDFUtils.DOAP_NS, "Project");
        try {
            new URL("http://simal.googlecode.com").openStream();
            assertEquals("Incorrect number of project elements", NUM_OF_PINGS, projects.getLength());
            assertTrue("RDF namespaces does not seem to be defined", serialise(doc).contains(RDFUtils.RDF_NS));
        } catch (UnknownHostException e) {
            assertEquals("Unexpectly found project elements", 0, projects.getLength());
        }
    }

    private String serialise(Document doc) throws IOException {
        OutputFormat format = new OutputFormat(doc);
        StringWriter writer = new StringWriter();
        XMLSerializer serial = new XMLSerializer(writer, format);
        serial.serialize(doc);
        return writer.toString();
    }
}
