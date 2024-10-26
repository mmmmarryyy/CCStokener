package net.webappservicefixture.fixture;

import fit.exception.FitFailureException;
import fitlibrary.DoFixture;
import fitlibrary.table.Row;
import fitlibrary.utility.TestResults;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;

public class XmlValidator extends DoFixture {

    private String xml;

    private XPath xpath;

    private static final String CLASS_PREFIX = "class:";

    public XmlValidator(String aXml) {
        super();
        setTraverse(new PlaceHolderTraverse(this));
        xml = aXml;
    }

    public void schema(final Row row, TestResults testResults) throws Exception {
        String urlString = row.text(1);
        String schemaBase = null;
        if (row.cellExists(2)) {
            schemaBase = row.text(2);
        }
        try {
            StreamSource schemaSource;
            if (urlString.startsWith(CLASS_PREFIX)) {
                InputStream schema = XmlValidator.class.getClassLoader().getResourceAsStream(urlString.substring(CLASS_PREFIX.length()));
                schemaSource = new StreamSource(schema);
            } else {
                URL url = new URL(urlString);
                URLConnection urlConnection = url.openConnection();
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                schemaSource = new StreamSource(inputStream);
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            if (schemaBase != null) {
                DefaultLSResourceResolver resolver = new DefaultLSResourceResolver(schemaBase);
                factory.setResourceResolver(resolver);
            }
            factory.newSchema(new URL(urlString));
            Validator validator = factory.newSchema(schemaSource).newValidator();
            StreamSource source = new StreamSource(new StringReader(xml));
            validator.validate(source);
            row.pass(testResults);
        } catch (SAXException e) {
            Loggers.SERVICE_LOG.warn("schema error", e);
            throw new FitFailureException(e.getMessage());
        } catch (IOException e) {
            Loggers.SERVICE_LOG.warn("schema error", e);
            throw new FitFailureException(e.getMessage());
        }
    }

    /**
     * execute a xpath evaluation against response. The second cell defines a xpath string, the third expected value and
     * can be empty, the forth the symbol saved under. The xpath result can be later referred using |!-symbol-!|
     * @param row sample: |xpath|!-/Response/Success-!|true|success|
     * @param testResults testResult
     */
    public void xpath(final Row row, TestResults testResults) {
        try {
            String xpathString = row.text(1);
            String expectedValue = null;
            if (row.cellExists(2)) {
                expectedValue = row.text(2);
            }
            initXpath();
            String xpathResult = xpath.compile(xpathString).evaluate(new InputSource(new StringReader(xml)));
            if (row.cellExists(3)) {
                setSymbol(row.text(3), xpathResult);
            }
            if (expectedValue != null && !xpathResult.equals(expectedValue)) {
                wrong(row.parse, xpathResult);
                row.fail(testResults);
            } else {
                row.pass(testResults);
            }
        } catch (ParserConfigurationException e) {
            Loggers.SERVICE_LOG.warn(e);
            throw new FitFailureException(e.getMessage());
        } catch (XPathExpressionException e) {
            Loggers.SERVICE_LOG.warn(e);
            throw new FitFailureException(e.getMessage());
        }
    }

    private void initXpath() throws ParserConfigurationException {
        if (xpath == null) {
            xpath = XPathFactory.newInstance().newXPath();
        }
    }
}
