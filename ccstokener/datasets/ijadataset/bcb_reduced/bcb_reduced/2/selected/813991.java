package org.hip.kernel.code;

import java.io.IOException;
import java.net.URL;
import javax.xml.parsers.SAXParserFactory;
import org.hip.kernel.exc.DefaultExceptionWriter;
import org.hip.kernel.servlet.impl.ServletContainer;
import org.hip.kernel.sys.VSys;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Creates a codelist out of an xml document.
 *
 * @author: Benno Luthiger
 */
public class CodeListFactory extends DefaultHandler {

    public static boolean VALIDATION = false;

    public static String CODELISTS_TAGNAME = "CodeLists".intern();

    public static String CODELIST_TAGNAME = "CodeList".intern();

    public static String CODELISTITEM_TAGNAME = "CodeListItem".intern();

    public static String ELEMENTID_ATTRIBUTENAME = "elementID".intern();

    public static String LABEL_ATTRIBUTENAME = "label".intern();

    public static String LANGUAGE_ATTRIBUTENAME = "language".intern();

    public static String FILE_EXTENSION = ".xml";

    public static String CODESPATH = "org.hip.vif.conf.root";

    private String codeID;

    private String language;

    private boolean codeListForLanguageDetected = false;

    private CodeList currentCodeList;

    /**
  	 * Entry point to create a CodeList out of an XML-document specified by inCodeID and URL.
  	 * The XML is parsed but only the CodeListItems in the specified language are evaluated.
  	 * 
  	 * @param inCodeID String
  	 * @param inLanguage String
  	 * @param inUrl URL the XML file containing the codes.
  	 * @return CodeList
  	 */
    public CodeList createCodeList(String inCodeID, String inLanguage, URL inUrl) {
        codeID = inCodeID;
        IResourceStrategy lStrategy = inUrl == null ? new SystemIDStrategy() : new BundelRelativeStrategy(inUrl);
        return createCodeList(inCodeID, inLanguage, lStrategy);
    }

    /**
	 * Entry point to create a CodeList out of an XML-document specified by inCodeID.
	 * The XML is parsed but only the CodeListItems in the specified language are evaluated.
	 *
	 * @return org.hip.kernel.code.CodeList
	 * @param inCodeID java.lang.String 
	 * @param inLanguage java.lang.String
	 */
    public CodeList createCodeList(String inCodeID, String inLanguage) {
        codeID = inCodeID;
        return createCodeList(inCodeID, inLanguage, new SystemIDStrategy());
    }

    private CodeList createCodeList(String inCodeID, String inLanguage, IResourceStrategy inStrategy) {
        language = inLanguage;
        try {
            XMLReader lParser = getParser();
            lParser.parse(inStrategy.getInputSource());
        } catch (Exception exc) {
            VSys.trace(this, "createCodeList", "Exception creating '" + inCodeID + "', language '" + inLanguage + "' with '" + inStrategy.getSystemID() + "'");
            DefaultExceptionWriter.printOut(this, exc, true);
        }
        return currentCodeList;
    }

    /**
	 * After parsing the document the retrieved CodeListItems have to be
	 * rearranged in the CodeList.
	 */
    public void endDocument() throws SAXException {
        super.endDocument();
        currentCodeList.prepareArrays();
    }

    /**
	 * Receive notification of the end of an element.
	 *
	 * @param inUri java.lang.String The Namespace URI.
	 * @param inLocalName java.lang.String The local name.
	 * @param inRawName java.lang.String The qualified (prefixed) name.
	 * @exception org.xml.sax.SAXException Any SAX exception, possibly wrapping another exception.
	 * @see org.xml.sax.ContentHandler#endElement
	 */
    public void endElement(String inUri, String inLocalName, String inRawName) throws SAXException {
        if (CODELIST_TAGNAME.equals(inRawName)) {
            codeListForLanguageDetected = false;
        }
    }

    /**
	 * @return org.xml.sax.XMLReader
	 */
    private XMLReader getParser() {
        XMLReader outParser = null;
        try {
            outParser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            outParser.setContentHandler(this);
            outParser.setErrorHandler(this);
        } catch (Exception exc) {
            DefaultExceptionWriter.printOut(this, exc, true);
        }
        return outParser;
    }

    /**
	 * Start parsing the document by creating a new CodeList
	 */
    public void startDocument() throws SAXException {
        super.startDocument();
        currentCodeList = new CodeList(codeID, language);
    }

    /**
	 * An element could be <CodeList language="xy"> or <CodeListItem elementID="ab" label="xy"/>
	 *
	 * @param inUri java.lang.String The Namespace URI.
	 * @param inLocalName java.lang.String The local name.
	 * @param inRawName java.lang.String The qualified (prefixed) name.
	 * @param inAttributes org.xml.sax.Attributes The specified or defaulted attributes.
	 * @exception org.xml.sax.SAXException Any SAX exception, possibly wrapping another exception.
	 * @see org.xml.sax.ContentHandler#startElement
	 */
    public void startElement(String inUri, String inLocalName, String inRawName, Attributes inAttributes) throws SAXException {
        if (inRawName != null) inRawName = inRawName.intern();
        if (CODELIST_TAGNAME == inRawName) {
            if (language.equals(inAttributes.getValue(LANGUAGE_ATTRIBUTENAME))) {
                codeListForLanguageDetected = true;
            } else {
                codeListForLanguageDetected = false;
            }
        } else if ((CODELISTITEM_TAGNAME == inRawName) && (codeListForLanguageDetected)) {
            CodeListItem lCurrentElement = new CodeListItem();
            lCurrentElement.setCodeID(codeID);
            lCurrentElement.setLanguage(language);
            lCurrentElement.setElementID(inAttributes.getValue(ELEMENTID_ATTRIBUTENAME));
            lCurrentElement.setLabel(inAttributes.getValue(LABEL_ATTRIBUTENAME));
            currentCodeList.add(lCurrentElement);
        }
    }

    private interface IResourceStrategy {

        public InputSource getInputSource() throws IOException;

        public String getSystemID();
    }

    private class BundelRelativeStrategy implements IResourceStrategy {

        private URL url;

        public BundelRelativeStrategy(URL inUrl) {
            url = inUrl;
        }

        public InputSource getInputSource() throws IOException {
            return new InputSource(url.openStream());
        }

        public String getSystemID() {
            return url.toString();
        }
    }

    private class SystemIDStrategy implements IResourceStrategy {

        private String systemID;

        public SystemIDStrategy() {
            systemID = createSystemID();
        }

        private String createSystemID() {
            String outFilename = null;
            try {
                String lDirectory = VSys.getVSysCanonicalPath(CODESPATH);
                if (lDirectory.length() == 0) {
                    String lProperty = ServletContainer.getInstance().getBasePath();
                    if (lProperty != null) {
                        lDirectory = VSys.getVSysCanonicalPath(CODESPATH, lProperty);
                    }
                }
                outFilename = lDirectory + codeID + FILE_EXTENSION;
            } catch (Exception exc) {
                outFilename = codeID + FILE_EXTENSION;
            }
            return "file:" + outFilename;
        }

        public InputSource getInputSource() throws IOException {
            return new InputSource(systemID);
        }

        public String getSystemID() {
            return systemID;
        }
    }
}
