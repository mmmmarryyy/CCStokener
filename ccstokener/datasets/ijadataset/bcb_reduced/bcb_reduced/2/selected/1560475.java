package com.saic.ship.fsml;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import com.saic.ship.util.*;
import org.apache.log4j.*;

/**
 * Overarching base class for documents in FSML. This is similar to using
 * the Document Object Model, but extended to be File-Structure specific. 
 *
 * <p> FsmlDocument is the main class for document operations which FSML will
 * perform. Any combination of FSML file (.xml) and data file can be run 
 * through the FSML processor and all operations are performed on an instance
 * of this FsmlDocument class. This class facilitates parametric reading
 * of template files using fixed or free format. It facilitates run-time
 * modification of file parameter values, and subsequently FSML can write
 * files using the FsmlProcessor implementation and the FsmlDocument instance
 * so that the legacy application can read them. Writing can also be 
 * accomplished with either fixed or free formats. 
 *
 * @author   Created By: <a href=mailto:j@ship.saic.com>J Bergquist</a>
 * @author   Last Revised By: $Author: bergquistj $
 * @see      FsmlErrorHandler 
 * @see      FsmlFileStructureListener 
 * @see      FsmlFileDataListener
 * @since    Created On: 2005/03/01
 * @since    Last Revised on: $Date: 2005/03/03 20:12:09 $
 * @version  $Revision: 1.8 $
 *
 */
public class FsmlDocument {

    /**
   * Logger for this class
   */
    private static Logger logger = Logger.getLogger(FsmlDocument.class);

    /**
   * URL for the fsml file
   *
   */
    private String fsmlUrl;

    private Document dom;

    private FsmlErrorHandler errorHandler;

    private Element root;

    private Element data;

    private Element structure;

    private FsmlFileDataListener fdl;

    private FsmlFileStructureListener fsl;

    private ValidationListener vl;

    private FsmlParameters parameters;

    /**
   * Constructs a new FsmlDocument. Does not call the init method, but does
   * set the fsmlUrl to the passed in URL. Also sets up the error handler. 
   *
   */
    public FsmlDocument(String fsmlUrl) {
        this.fsmlUrl = fsmlUrl;
        errorHandler = new FsmlErrorHandler();
    }

    /**
   * Return the error handler for this instance. 
   *
   * @return a <code>GenericErrorHandler</code> value
   */
    public GenericErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    /**
   * Return the name of this instance. 
   *
   * @return a <code>String</code> value
   */
    public String getName() {
        return root.getAttribute("type");
    }

    /**
   * Return the description for this instance. 
   *
   * @return a <code>String</code> value
   */
    public String getDescription() {
        return root.getAttribute("description");
    }

    /**
   * Return the line comment start character for this instance. 
   *
   * @return a <code>String</code> value
   */
    public String getLineCommentStart() {
        return root.getAttribute("lineCommentStart");
    }

    /**
   * Return the block comment start character for this instance. 
   *
   * @return a <code>String</code> value
   */
    public String getBlockCommentStart() {
        return root.getAttribute("blockCommentStart");
    }

    /**
   * Return the block comment end character for this instance. 
   *
   * @return a <code>String</code> value
   */
    public String getBlockCommentEnd() {
        return root.getAttribute("blockCommentEnd");
    }

    /**
   * Return the ignore blank lines value for this instance. 
   *
   * @return a <code>boolean</code> value
   */
    public boolean ignoreBlankLines() {
        return Boolean.valueOf(root.getAttribute("ignoreBlankLines")).booleanValue();
    }

    /**
   * Return the expected delimiters value for this instance. 
   *
   * @return a <code>String</code> value
   */
    public String getDelimiters() {
        return root.getAttribute("delimiters");
    }

    /**
   * Return the ignore missing values attribute for this instance. 
   *
   * @return boolean
   */
    public boolean getIgnoreMissingValues() {
        return Boolean.valueOf(root.getAttribute("ignoreMissingValues")).booleanValue();
    }

    /**
   * Return the element for a passed in string which is the ID of that element.
   *
   * @param name a <code>String</code> value
   * @return an <code>Element</code> value
   */
    public Element getElement(String name) {
        return dom.getElementById(name);
    }

    /**
   * Get an attribute given the name (id) of the instance and the attribute 
   * name. 
   *
   * @param name a <code>String</code> value
   * @param attr a <code>String</code> value
   * @return a <code>String</code> value
   */
    public String getAttribute(String name, String attr) {
        return dom.getElementById(name).getAttribute(attr);
    }

    /**
   * Return the fsml parameter list. 
   *
   * @return a <code>List</code> value
   */
    public List getParameters() {
        return getFsmlParameters();
    }

    /**
   * Sets the FsmlFileDataListener reference. 
   *
   * @param fdl a <code>FsmlFileDataListener</code> value
   */
    public void setFsmlFileDataListener(FsmlFileDataListener fdl) {
        this.fdl = fdl;
    }

    /**
   * Sets the FsmlFileStructureListener reference. 
   *
   * @param fsl a <code>FsmlFileStructureListener</code> value
   */
    public void setFsmlFileStructureListener(FsmlFileStructureListener fsl) {
        this.fsl = fsl;
    }

    /**
   * Sets the ValidationListener reference. 
   *
   * @param vl a <code>ValidationListener</code> value
   */
    public void setValidationListener(ValidationListener vl) {
        this.vl = vl;
    }

    /**
   * Initializes the FsmlDocument. Sets up the dom, parses the FSML file, 
   * checks for errors, and throws exceptions if they are encountered. The
   * init method is called by the constructor and is public in scope so that
   * a given FSML document can be re-initialized. 
   *
   * @exception Exception if an error occurs
   */
    public void init() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        builder.setErrorHandler(errorHandler);
        URL urlFsmlUrl = null;
        try {
            urlFsmlUrl = new URL(fsmlUrl);
            dom = builder.parse(urlFsmlUrl.openStream());
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            throw new Exception("Malformed URL " + fsmlUrl + ".");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new Exception("Could not open stream to URL " + urlFsmlUrl.toExternalForm() + ".");
        } catch (SAXParseException err) {
            throw new Exception("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() + "   " + err.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
        if (errorHandler.getNumErrors() > 0) {
            throw new FsmlException("Errors ocurred while parsing FSML");
        }
        root = dom.getDocumentElement();
        data = (Element) root.getElementsByTagName("FileData").item(0);
        structure = (Element) root.getElementsByTagName("FileStructure").item(0);
    }

    /**
   * Reads the FileData section of the FSML file. 
   *
   * @exception Exception if an error occurs
   */
    public void readFileData() throws Exception {
        NodeList fieldElements = data.getElementsByTagName("*");
        int fieldCount = fieldElements.getLength();
        fdl.beginFileData(fieldCount);
        Element e;
        for (int i = 0; i < fieldCount; i++) {
            e = (Element) fieldElements.item(i);
            fdl.dataElementDeclared(e.getAttribute("name"), FsmlDataTypes.forString(e.getTagName()), e.getAttribute("description"), e.getAttribute("units"));
        }
        fdl.endFileData();
    }

    /**
   * Reads the FileStructure section of the FSML file. 
   *
   * @exception Exception if an error occurs
   */
    public void readFileStructure() throws Exception {
        Stack activeParents = new Stack();
        Element ap;
        Element c;
        Element e = this.structure;
        activeParents.push(e);
        openEventForElement(e);
        int numChildren;
        NodeList nl = this.structure.getElementsByTagName("*");
        for (int i = 0; i < nl.getLength(); i++) {
            e = (Element) nl.item(i);
            numChildren = e.getElementsByTagName("*").getLength();
            if (numChildren > 0) {
                activeParents.push(e);
                openEventForElement(e);
            } else {
                openEventForElement(e);
                closeParents(activeParents, e);
            }
        }
    }

    /**
   * Validates the 2nd level of the fsml file. This makes sure that all of 
   * the read/write specific operations will work properly. 
   *
   * @exception Exception if an error occurs
   */
    private void perform2ndLevelValidation() throws Exception {
        Element e;
        vl.beginDocument();
        NodeList nl = this.root.getElementsByTagName("*");
        for (int i = 0; i < nl.getLength(); i++) {
            e = (Element) nl.item(i);
            vl.elementDeclared(e);
            NamedNodeMap attributes = e.getAttributes();
            for (int j = 0; j < attributes.getLength(); j++) {
                vl.attributeDeclared(e, (Attr) attributes.item(j));
            }
        }
        vl.endDocument();
        if (errorHandler.getNumErrors() > 0) {
            throw new FsmlException("Errors ocurred while parsing FSML");
        }
    }

    /**
   * Closes out active parents for a given element. 
   *
   * @param activeParents a <code>Stack</code> value
   * @param e an <code>Element</code> value
   * @exception Exception if an error occurs
   */
    private void closeParents(Stack activeParents, Element e) throws Exception {
        Element ap = (Element) activeParents.peek();
        while ((!activeParents.empty()) && (getLastChildElement(ap) == e)) {
            e = (Element) activeParents.pop();
            logger.debug("Number of active parents: " + activeParents.size());
            closeEventForElement(e);
            if (!activeParents.empty()) {
                ap = (Element) activeParents.peek();
                logger.debug("Active parent: " + ap.getTagName());
            }
        }
    }

    /**
   * Gets last child for a given element. 
   *
   * @param e an <code>Element</code> value
   * @return an <code>Element</code> value
   */
    private Element getLastChildElement(Element e) {
        Node lastChild = e.getLastChild();
        while (lastChild.getNodeType() != Node.ELEMENT_NODE) {
            lastChild = lastChild.getPreviousSibling();
        }
        return (Element) lastChild;
    }

    /**
   * For passed in element, open the appropriate event. Elements correspond
   * 1:1 with events. 
   *
   * @param e an <code>Element</code> value
   * @exception Exception if an error occurs
   */
    private void openEventForElement(Element e) throws Exception {
        String name = e.getTagName();
        if (name.equals("FileStructure")) {
            fsl.beginFileStructure();
        }
        if (name.equals("Record")) {
            fsl.beginRecord(Boolean.valueOf(e.getAttribute("ignoreOnRead")).booleanValue(), e.getAttribute("format"), Boolean.valueOf(e.getAttribute("ignoreFormatOnRead")).booleanValue());
            String s = e.getAttribute("format");
        }
        if (name.equals("Separator")) {
            fsl.separator(Boolean.valueOf(e.getAttribute("ignoreOnRead")).booleanValue(), e.getAttribute("string"), "");
        }
        if (name.equals("Loop")) {
            fsl.beginLoop(e.getAttribute("loopVariable"), e.getAttribute("start"), e.getAttribute("end"), e.getAttribute("incr"), e.getAttribute("check"), FsmlOperatorTypes.forString(e.getAttribute("operator")), e.getAttribute("checkValue"), Boolean.valueOf(e.getAttribute("doOnce")).booleanValue(), Boolean.valueOf(e.getAttribute("stopOnEof")).booleanValue());
        }
        if (name.equals("Condition")) {
            fsl.beginCondition(e.getAttribute("check"), FsmlOperatorTypes.forString(e.getAttribute("operator")), e.getAttribute("checkValue"));
        }
        if (name.equals("SetValue")) {
            fsl.setValue(e.getAttribute("target"));
        }
        if (name.equals("AddValue")) {
            fsl.addValue(e.getAttribute("target"));
        }
        if (name.equals("SetDims")) {
            fsl.setDims(e.getAttribute("target"), e.getAttribute("dims"));
        }
        if (name.equals("SetSize")) {
            fsl.setSize(e.getAttribute("target"), e.getAttribute("dimIndex"), e.getAttribute("size"));
        }
        if (name.equals("StaticText")) {
            Text t = (Text) e.getFirstChild();
            if (t != null) {
                fsl.staticText(t.getData());
            } else {
                fsl.staticText("");
            }
        }
    }

    /**
   * For a given element, closes the event. Again, elements correspond 1:1 with
   * events. 
   *
   * @param e an <code>Element</code> value
   * @exception Exception if an error occurs
   */
    private void closeEventForElement(Element e) throws Exception {
        String name = e.getTagName();
        logger.debug("Closing " + name);
        if (name.equals("FileStructure")) {
            fsl.endFileStructure();
        }
        if (name.equals("Record")) {
            fsl.endRecord();
        }
        if (name.equals("Loop")) {
            fsl.endLoop();
        }
        if (name.equals("Condition")) {
            fsl.endCondition();
        }
    }

    /**
   * Package-visible method to perform the logic of getting the FsmlParameters 
   * list. 
   *
   * @return a <code>FsmlParameters</code> value
   */
    FsmlParameters getFsmlParameters() {
        if (parameters == null) {
            parameters = new FsmlParameters();
            setFsmlFileDataListener(parameters);
            try {
                readFileData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return parameters;
    }
}
