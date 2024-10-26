package org.apache.batik.dom;

import org.w3c.dom.*;
import java.io.*;
import java.net.*;
import org.apache.batik.dom.util.*;
import org.apache.batik.util.*;
import org.apache.batik.test.*;

/**
 * This class tests the hasChildNodes method.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id: HasChildNodesTest.java 475477 2006-11-15 22:44:28Z cam $
 */
public class HasChildNodesTest extends AbstractTest {

    public static String ERROR_GET_ELEMENT_BY_ID_FAILED = "error.get.element.by.id.failed";

    public static String ENTRY_KEY_ID = "entry.key.id";

    protected String testFileName;

    protected String rootTag;

    protected String targetId;

    public HasChildNodesTest(String file, String root, String id) {
        testFileName = file;
        rootTag = root;
        targetId = id;
    }

    public TestReport runImpl() throws Exception {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        DocumentFactory df = new SAXDocumentFactory(GenericDOMImplementation.getDOMImplementation(), parser);
        File f = (new File(testFileName));
        URL url = f.toURL();
        Document doc = df.createDocument(null, rootTag, url.toString(), url.openStream());
        Element e = doc.getElementById(targetId);
        if (e == null) {
            DefaultTestReport report = new DefaultTestReport(this);
            report.setErrorCode(ERROR_GET_ELEMENT_BY_ID_FAILED);
            report.addDescriptionEntry(ENTRY_KEY_ID, targetId);
            report.setPassed(false);
            return report;
        }
        while (e.hasChildNodes()) {
            e.removeChild(e.getFirstChild());
        }
        return reportSuccess();
    }
}
