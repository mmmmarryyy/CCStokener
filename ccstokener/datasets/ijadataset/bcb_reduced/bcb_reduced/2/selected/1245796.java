package test.unit.be.fedict.eid.applet.service.signer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.signature.PackageDigitalSignatureManager;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import be.fedict.eid.applet.service.signer.KeyInfoKeySelector;
import be.fedict.eid.applet.service.signer.ooxml.OOXMLProvider;
import be.fedict.eid.applet.service.signer.ooxml.OOXMLSignatureVerifier;
import be.fedict.eid.applet.service.signer.ooxml.OOXMLURIDereferencer;

public class OOXMLSignatureVerifierTest {

    private static final Log LOG = LogFactory.getLog(OOXMLSignatureVerifierTest.class);

    @BeforeClass
    public static void setUp() {
        OOXMLProvider.install();
    }

    @Test
    public void testIsOOXMLDocument() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/hello-world-unsigned.docx");
        boolean result = OOXMLSignatureVerifier.isOOXML(url);
        assertTrue(result);
    }

    @Test
    public void testIsOOXMLDocument2() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/signed-ooxml.docx");
        boolean result = OOXMLSignatureVerifier.isOOXML(url);
        assertTrue(result);
    }

    @Test
    public void testODFIsNotOOXML() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/hello-world.odt");
        boolean result = OOXMLSignatureVerifier.isOOXML(url);
        assertFalse(result);
    }

    @Test
    public void testPOI() throws Exception {
        InputStream inputStream = OOXMLSignatureVerifierTest.class.getResourceAsStream("/hello-world-unsigned.docx");
        boolean result = POIXMLDocument.hasOOXMLHeader(inputStream);
        assertTrue(result);
    }

    @Test
    public void testOPC() throws Exception {
        InputStream inputStream = OOXMLSignatureVerifierTest.class.getResourceAsStream("/hello-world-signed.docx");
        OPCPackage opcPackage = OPCPackage.open(inputStream);
        ArrayList<PackagePart> parts = opcPackage.getParts();
        for (PackagePart part : parts) {
            LOG.debug("part name: " + part.getPartName().getName());
            LOG.debug("part content type: " + part.getContentType());
        }
        ArrayList<PackagePart> signatureParts = opcPackage.getPartsByContentType("application/vnd.openxmlformats-package.digital-signature-xmlsignature+xml");
        assertFalse(signatureParts.isEmpty());
        PackagePart signaturePart = signatureParts.get(0);
        LOG.debug("signature part class type: " + signaturePart.getClass().getName());
        PackageDigitalSignatureManager packageDigitalSignatureManager = new PackageDigitalSignatureManager();
    }

    @Test
    public void testGetSignerUnsigned() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/hello-world-unsigned.docx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetSignerOffice2010Unsigned() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/hello-world-office-2010-technical-preview-unsigned.docx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetSignerUnsignedPowerpoint() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/hello-world-unsigned.pptx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetSignerUnsignedExcel() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/hello-world-unsigned.xlsx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetSigner() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/hello-world-signed.docx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertEquals(1, result.size());
        X509Certificate signer = result.get(0);
        LOG.debug("signer: " + signer.getSubjectX500Principal());
    }

    @Test
    public void testSignedOOXML() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/signed-ooxml.docx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertEquals(1, result.size());
        X509Certificate signer = result.get(0);
        LOG.debug("signer: " + signer.getSubjectX500Principal());
    }

    @Test
    public void testSignedOOXMLOffice2010() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/ms-office-2010-signed.docx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertEquals(1, result.size());
        X509Certificate signer = result.get(0);
        LOG.debug("signer: " + signer.getSubjectX500Principal());
    }

    @Test
    public void testSignedOOXMLOffice2010ValidOOXML() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/hallo.docx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertEquals(1, result.size());
        X509Certificate signer = result.get(0);
        LOG.debug("signer: " + signer.getSubjectX500Principal());
        byte[] document = IOUtils.toByteArray(url.openStream());
        List<String> signatureResourceNames = verifier.getSignatureResourceNames(document);
        Document signatureDocument = verifier.getSignatureDocument(new ByteArrayInputStream(document), signatureResourceNames.get(0));
        NodeList signatureNodeList = signatureDocument.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        Element signatureElement = (Element) signatureNodeList.item(0);
        KeyInfoKeySelector keySelector = new KeyInfoKeySelector();
        DOMValidateContext domValidateContext = new DOMValidateContext(keySelector, signatureElement);
        domValidateContext.setProperty("org.jcp.xml.dsig.validateManifests", Boolean.TRUE);
        OOXMLURIDereferencer dereferencer = new OOXMLURIDereferencer(document);
        domValidateContext.setURIDereferencer(dereferencer);
        XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance();
        XMLSignature xmlSignature = xmlSignatureFactory.unmarshalXMLSignature(domValidateContext);
        assertTrue(verifier.isValidOOXMLSignature(xmlSignature, document));
    }

    @Test
    public void testSignedOOXMLOffice2010Powerpoint() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/ms-office-2010-signed.pptx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertEquals(1, result.size());
        X509Certificate signer = result.get(0);
        LOG.debug("signer: " + signer.getSubjectX500Principal());
    }

    @Test
    public void testSignedOOXMLOffice2010Excel() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/ms-office-2010-signed.xlsx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertEquals(1, result.size());
        X509Certificate signer = result.get(0);
        LOG.debug("signer: " + signer.getSubjectX500Principal());
    }

    public void testOffice2010TechnicalPreview() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/hello-world-office-2010-technical-preview.docx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertEquals(1, result.size());
        X509Certificate signer = result.get(0);
        LOG.debug("signer: " + signer.getSubjectX500Principal());
    }

    public void testGetSignerPowerpoint() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/hello-world-signed.pptx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertEquals(1, result.size());
        X509Certificate signer = result.get(0);
        LOG.debug("signer: " + signer.getSubjectX500Principal());
    }

    @Test
    public void testGetSignerExcel() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/hello-world-signed.xlsx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertEquals(1, result.size());
        X509Certificate signer = result.get(0);
        LOG.debug("signer: " + signer.getSubjectX500Principal());
    }

    @Test
    public void testGetSigners() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/hello-world-signed-twice.docx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertEquals(2, result.size());
        X509Certificate signer1 = result.get(0);
        X509Certificate signer2 = result.get(1);
        LOG.debug("signer 1: " + signer1.getSubjectX500Principal());
        LOG.debug("signer 2: " + signer2.getSubjectX500Principal());
    }

    @Test
    public void testGetSignersOffice2010SP1() throws Exception {
        URL url = OOXMLSignatureVerifierTest.class.getResource("/Office2010-SP1-XAdES-X-L.docx");
        OOXMLSignatureVerifier verifier = new OOXMLSignatureVerifier();
        List<X509Certificate> result = verifier.getSigners(url);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testVerifySignature() throws Exception {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("org.jcp.xml.dsig.internal.dom");
        logger.log(Level.FINE, "test");
        URL url = OOXMLSignatureVerifierTest.class.getResource("/hello-world-signed.docx");
        String signatureResourceName = getSignatureResourceName(url);
        LOG.debug("signature resource name: " + signatureResourceName);
        OOXMLProvider.install();
        ZipInputStream zipInputStream = new ZipInputStream(url.openStream());
        ZipEntry zipEntry;
        while (null != (zipEntry = zipInputStream.getNextEntry())) {
            if (false == signatureResourceName.equals(zipEntry.getName())) {
                continue;
            }
            Document signatureDocument = loadDocument(zipInputStream);
            LOG.debug("signature loaded");
            NodeList signatureNodeList = signatureDocument.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            assertEquals(1, signatureNodeList.getLength());
            Node signatureNode = signatureNodeList.item(0);
            KeyInfoKeySelector keySelector = new KeyInfoKeySelector();
            DOMValidateContext domValidateContext = new DOMValidateContext(keySelector, signatureNode);
            domValidateContext.setProperty("org.jcp.xml.dsig.validateManifests", Boolean.TRUE);
            OOXMLURIDereferencer dereferencer = new OOXMLURIDereferencer(url);
            domValidateContext.setURIDereferencer(dereferencer);
            XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance();
            XMLSignature xmlSignature = xmlSignatureFactory.unmarshalXMLSignature(domValidateContext);
            boolean validity = xmlSignature.validate(domValidateContext);
            assertTrue(validity);
            List<?> objects = xmlSignature.getObjects();
            for (Object object : objects) {
                LOG.debug("ds:Object class type: " + object.getClass().getName());
            }
            break;
        }
    }

    private String getSignatureResourceName(URL url) throws IOException, ParserConfigurationException, SAXException, TransformerException {
        InputStream inputStream = url.openStream();
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry zipEntry;
        while (null != (zipEntry = zipInputStream.getNextEntry())) {
            if (false == "[Content_Types].xml".equals(zipEntry.getName())) {
                continue;
            }
            Document contentTypesDocument = loadDocument(zipInputStream);
            Element nsElement = contentTypesDocument.createElement("ns");
            nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:tns", "http://schemas.openxmlformats.org/package/2006/content-types");
            NodeList nodeList = XPathAPI.selectNodeList(contentTypesDocument, "/tns:Types/tns:Override[@ContentType='application/vnd.openxmlformats-package.digital-signature-xmlsignature+xml']/@PartName", nsElement);
            if (nodeList.getLength() == 0) {
                return null;
            }
            String partName = nodeList.item(0).getTextContent();
            LOG.debug("part name: " + partName);
            partName = partName.substring(1);
            return partName;
        }
        return null;
    }

    private Document loadDocument(InputStream documentInputStream) throws ParserConfigurationException, SAXException, IOException {
        InputSource inputSource = new InputSource(documentInputStream);
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(inputSource);
        return document;
    }
}
