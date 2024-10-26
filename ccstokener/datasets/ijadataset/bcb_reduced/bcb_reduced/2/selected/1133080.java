package org.dcm4chee.xero.search.study;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.dcm4chee.xero.metadata.servlet.JAXBProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Tests the JAXB encoding of studies, series and images.
 * 
 * @author bwallace
 * 
 */
public class TestJaxbEncode {

    private static Logger log = LoggerFactory.getLogger(TestJaxbEncode.class);

    static XPathFactory factory = XPathFactory.newInstance();

    static XPath xpath = factory.newXPath();

    static DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();

    static DocumentBuilder builder;

    static {
        try {
            builder = domFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static final URL singleUrl = Thread.currentThread().getContextClassLoader().getResource("org/dcm4chee/xero/search/study/singleFrame.dcm");

    public static final StopTagInputHandler stopHandler = new StopTagInputHandler(Tag.PixelData);

    @Test
    public void testPatientEncode() throws Exception {
        String studyStr = getStudyStr(singleUrl);
        log.debug("study=" + studyStr);
        assert getXpathStr(studyStr, "study/@AccessionNumber").equals("THU9948");
        assert getXpathStr(studyStr, "study/series/@SeriesNumber").equals("1");
    }

    @Test
    public void testResultsEncode_NameSpacePrefixAreAssignedCorrectly() throws Exception {
        String studyStr = getStudyStr(singleUrl);
        log.debug("study=" + studyStr);
        assert studyStr.contains("xmlns=\"http://www.dcm4chee.org/xero/search/study") : "XERO data types must be in the default namespace";
        assert studyStr.contains("xmlns:h=\"http://www.w3.org/1999/xhtml") : "XHTML must use the 'h' namespace prefix";
        assert studyStr.contains("xmlns:s=\"http://www.w3.org/2000/svg") : "SVG must use the 's' namespace prefix";
        assert studyStr.contains("xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance") : "XML Schema must use the 'i' namespace prefix";
    }

    public static String getStudyStr(URL url) throws Exception {
        PatientBean patient = TestJaxbEncode.loadPatient(url);
        StudyBean study = (StudyBean) patient.getStudy().get(0);
        JAXBContext context = JAXBContext.newInstance(ResultsBean.class, StudyBean.class, SeriesBean.class, ImageBean.class);
        JAXBProvider provider = new JAXBProvider(context);
        Marshaller m = provider.createMarshaller();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        m.marshal(study, baos);
        return baos.toString("UTF-8");
    }

    public static String getXpathStr(String xmlStr, String xpathStr) throws XPathExpressionException, SAXException, IOException {
        if (xmlStr == null || xmlStr.length() == 0) return null;
        ByteArrayInputStream bais = new ByteArrayInputStream(xmlStr.getBytes());
        Document doc = builder.parse(bais);
        String ret = (String) xpath.evaluate(xpathStr, doc, XPathConstants.STRING);
        log.debug("XPath " + xpathStr + "=" + ret);
        return ret;
    }

    public static Number getXpathNum(String xmlStr, String xpathStr) throws XPathExpressionException, SAXException, IOException {
        if (xmlStr == null || xmlStr.length() == 0) return null;
        ByteArrayInputStream bais = new ByteArrayInputStream(xmlStr.getBytes());
        Document doc = builder.parse(bais);
        Number ret = (Number) xpath.evaluate(xpathStr, doc, XPathConstants.NUMBER);
        log.debug("XPath " + xpathStr + "=" + ret);
        return ret;
    }

    public static DicomObject loadDicomObject(URL url) {
        DicomInputStream dis;
        if (url == null) {
            TestMultiFrameImage.log.warn("No URL provided.");
            return null;
        }
        try {
            dis = new DicomInputStream(url.openStream());
            dis.setHandler(TestJaxbEncode.stopHandler);
            return (dis.readDicomObject());
        } catch (IOException e) {
            TestMultiFrameImage.log.warn("Unable to load object for " + url + " tests will not be run.");
            return null;
        }
    }

    public static PatientBean loadPatient(URL url) {
        Map<Object, Object> children = new HashMap<Object, Object>();
        DicomObject dcmObj = TestJaxbEncode.loadDicomObject(url);
        if (dcmObj == null) return null;
        PatientBean patient = new PatientBean(children, dcmObj);
        return patient;
    }
}
