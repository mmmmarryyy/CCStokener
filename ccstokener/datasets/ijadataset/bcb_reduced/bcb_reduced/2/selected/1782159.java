package org.xmlcml.cml.element;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlcml.cml.attribute.DictRefAttribute;
import org.xmlcml.cml.attribute.MetadataNameAttribute;
import org.xmlcml.cml.attribute.UnitsAttribute;
import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.base.CMLConstants;
import org.xmlcml.cml.base.CMLRuntimeException;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.cml.map.DictionaryMap;
import org.xmlcml.cml.map.NamespaceToUnitListMap;
import org.xmlcml.euclid.Util;

/**
 * tests all examples in examples directory.
 * 
 * @author pm286
 * 
 */
public class ExamplesTest extends AbstractTest implements CMLConstants {

    /** parser type. */
    public enum Type {

        /** use XOM parser with OldNodeFactory */
        CML, /** use XOM parser */
        XOM, /** use Xerces */
        XERCES;

        private Type() {
        }
    }

    private static long time0 = -1;

    /**
	 * setup.
	 * 
	 * @throws Exception
	 */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (time0 == -1) {
            time0 = System.currentTimeMillis();
        }
    }

    /**
	 * tests.
	 * @exception Exception
	 */
    @Test
    public void testValidateX() throws Exception {
        boolean parseXsdExamples = true;
        boolean parseComplex = true;
        boolean checkDict = true;
        if (parseXsdExamples) {
            validate(SIMPLE_RESOURCE, false);
        }
        if (parseComplex) {
            System.out.println("------start complex-------" + (System.currentTimeMillis() - time0));
            try {
                validate(COMPLEX_RESOURCE, checkDict);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CMLRuntimeException("BUG" + e);
            }
            System.out.println("------end complex-------" + (System.currentTimeMillis() - time0));
        }
        System.out.println("*** FINISHED VALIDATING ***");
    }

    private void validate(String resource, boolean checkData) throws Exception {
        boolean print = false;
        boolean validate = false;
        validate(resource, Type.XOM, validate, print, checkData);
        validate(resource, Type.CML, validate, print, checkData);
        validate = true;
    }

    /**
	 * use xomXerces validation on all files in complex directory.
	 * 
	 * 
	 */
    private void validate(String resource, Type type, boolean validate, boolean print, boolean checkData) throws Exception {
        System.out.println("==== parsing " + resource + " with " + type + " === " + (System.currentTimeMillis() - time0));
        Document index = CMLUtil.getXMLResource(resource + S_SLASH + INDEX);
        boolean fail = false;
        Nodes files = index.query("//file");
        DocumentBuilder docBuilder = null;
        if (Type.XERCES.equals(type)) {
            docBuilder = getXercesDocumentBuilder(validate);
            System.err.println("XERCES VALIDATION NOT YET IMPLEMENTED");
        }
        for (int i = 0; i < files.size(); i++) {
            Element f = (Element) files.get(i);
            String name = f.getAttributeValue("name");
            String filename = resource + S_SLASH + name;
            System.out.print(" .. " + name.substring(0, name.length() - 4));
            if ((i + 1) % 6 == 0) System.out.println();
            URL url = Util.getResource(filename);
            long time = System.currentTimeMillis();
            if (print) {
                System.out.println("file: " + filename);
            }
            if (Type.XERCES.equals(type)) {
                if (validate) {
                } else {
                    try {
                        fail |= !validateXerces1(url, docBuilder);
                    } catch (Exception e) {
                        throw new RuntimeException("Problem when validating: " + filename, e);
                    }
                }
            } else {
                fail |= !parse(Type.CML, url, checkData);
            }
            if (print) {
                System.out.println(" = " + (System.currentTimeMillis() - time));
                System.out.flush();
            }
        }
        System.out.println("\n============ end of " + type + "==============");
        if (fail) {
            throw new RuntimeException("One or more files failed to validate, for a reason that has been consumed in the test harness :-(");
        }
    }

    private boolean validateXerces1(URL url, DocumentBuilder docBuilder) throws Exception {
        InputStream in = url.openStream();
        docBuilder.parse(in);
        in.close();
        return true;
    }

    /**
	 * parse with CMLBuilder. will validate and transform with OldNodeFactory
	 * 
	 * @param type
	 *            of parse
	 * @param url
	 * @param checkDict
	 *            check dictRefs work
	 * 
	 * @return true if parsed OK
	 * @throws Exception
	 */
    private boolean parse(Type type, URL url, boolean checkDict) throws Exception {
        boolean ok = true;
        Exception ee = null;
        Element rootElement = null;
        try {
            InputStream in = url.openStream();
            if (type.equals(Type.XOM)) {
                new Builder().build(in);
            } else if (type.equals(Type.CML)) {
                rootElement = new CMLBuilder().build(in).getRootElement();
            }
            in.close();
        } catch (Exception e) {
            ee = e;
        }
        if (ee != null) {
            logger.severe("failed to cmlParse: " + url + "\n..... because: [" + ee + "] [" + ee.getMessage() + "] in [" + url + S_RSQUARE);
            ok = false;
            throw new RuntimeException("Problem in test harness when parsing " + url, ee);
        }
        if (ok && checkDict) {
            ok = checkDict(rootElement);
        }
        return ok;
    }

    private boolean checkDict(Element rootElement) throws Exception {
        boolean ok = true;
        DictionaryMap dictionaryMap = null;
        dictionaryMap = new DictionaryMap(Util.getResource(DICT_RESOURCE + U_S + CATALOG_XML), new CMLDictionary());
        List<String> errorList = new DictRefAttribute().checkAttribute(rootElement, dictionaryMap);
        if (errorList.size() > 0) {
            for (String error : errorList) {
                System.err.println(error);
            }
            ok = false;
        }
        errorList = new MetadataNameAttribute().checkAttribute(rootElement, dictionaryMap);
        if (errorList.size() > 0) {
            for (String error : errorList) {
                System.err.println(error);
            }
            ok = false;
        }
        NamespaceToUnitListMap unitListMap = null;
        unitListMap = new NamespaceToUnitListMap(Util.getResource(UNIT_RESOURCE + U_S + CATALOG_XML), new CMLUnitList());
        errorList = new UnitsAttribute().checkAttribute(rootElement, unitListMap);
        if (errorList.size() > 0) {
            for (String error : errorList) {
                System.err.println(error);
            }
            ok = false;
        }
        return ok;
    }

    private DocumentBuilder getXercesDocumentBuilder(boolean validate) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(validate);
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        String schemaName = EXAMPLES_RESOURCE + S_SLASH + "schema.xsd";
        InputStream in = Util.getInputStreamFromResource(schemaName);
        Source schemaSource = new StreamSource(in);
        Schema schema = null;
        try {
            schema = factory.newSchema(schemaSource);
        } catch (SAXException e1) {
            e1.printStackTrace();
        }
        dbf.setSchema(schema);
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            neverThrow(e);
        }
        ErrorHandler errorHandler = new MyErrorHandler();
        docBuilder.setErrorHandler(errorHandler);
        return docBuilder;
    }
}

class MyErrorHandler extends DefaultHandler {

    /**
	 * constructor.
	 */
    public MyErrorHandler() {
    }

    /**
	 * fatal error. currently prints message.
	 * 
	 * @param e
	 */
    public void fatalError(SAXParseException e) {
        System.err.println("SAX FATAL ERROR: " + e);
    }

    /**
	 * error. currently prints message.
	 * 
	 * @param e
	 */
    public void error(SAXParseException e) {
        System.err.println("SAX ERROR: " + e);
    }

    /**
	 * warning. currently prints message.
	 * 
	 * @param e
	 */
    public void warning(SAXParseException e) {
        System.err.println("SAX WARNING: " + e);
    }
}
