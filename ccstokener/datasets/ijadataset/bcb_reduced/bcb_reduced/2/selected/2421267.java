package gqtiv2;

import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;

public class ResponseProcessor {

    private org.w3c.dom.Node theElement;

    private String myNodeName;

    private String parentNode;

    private DOMNodeParser parser;

    private DocumentDataStore thedataStore;

    private String template;

    private String templateLocation;

    private String templatedirectory;

    private String debugString = "";

    public ResponseProcessor(org.w3c.dom.Node theNode, String pNode, DocumentDataStore ds) {
        thedataStore = ds;
        theElement = theNode;
        parentNode = pNode;
        myNodeName = theElement.getNodeName();
    }

    public void run(String[] Parameters) throws gqtiexcept.XMLException {
        template = qtiv2utils.selectAttribute(theElement, "template", "");
        templateLocation = qtiv2utils.selectAttribute(theElement, "templateLocation", "");
        String debug = debug = thedataStore.getProperty("debug");
        if (debug.equals("ON")) System.out.println("RESPONSE PROCESSING: CHECKING IF TEMPLATE" + template);
        if (!template.equals("")) ProcessexternalTemplate(template, templateLocation, Parameters); else {
            int childCount = theElement.getChildNodes().getLength();
            for (int i = 0; i < childCount; i++) {
                org.w3c.dom.Node thenode = theElement.getChildNodes().item(i);
                if (thenode.getNodeName().equals("exitResponse")) {
                    break;
                }
                if (thenode.getNodeType() == 1) {
                    DOMNodeParser parser = new DOMNodeParser(thenode, myNodeName, thedataStore);
                    parser.parseDocument(thenode, Parameters);
                }
            }
        }
    }

    private void ProcessexternalTemplate(String template, String templateLocation, String[] Parameters) throws gqtiexcept.XMLException {
        Document templatexml = null;
        String templatePath = "";
        String templateString = "";
        String templatefile = "";
        java.net.URL templateURL = null;
        String debug = debug = thedataStore.getProperty("debug");
        String isDesktop = thedataStore.getProperty("Desktop");
        String isGuest = thedataStore.getProperty("Guest");
        String templateSourceDirectory = thedataStore.getProperty("XMLfileDirectory");
        debugString = debugString + "TEMPLATE WANTED" + template + "<br />\n";
        try {
            if (!templateLocation.equals("")) {
                debugString = debugString + "EXTERNAL TEMPLATE PROCESSING <br />\n";
                debugString = debugString + "TEMPLATESOURCEDIRECTORY" + templateSourceDirectory + "<br />\n";
                String dots = "../";
                int dotslen = dots.length();
                if (templateLocation.indexOf("http") == 0) {
                    if ((isDesktop == null) || (!isDesktop.equals("TRUE"))) {
                        debugString = debugString + "GET TEMPLATE FROM WEB" + "<br />\n";
                        templateString = GetStringFromURL(templateLocation);
                        debugString = debugString + "FROM URL " + templateString + "<br />\n";
                    }
                } else if (templateLocation.indexOf(dots) == 0) {
                    debugString = debugString + "DOT NOTATION" + "<br />\n";
                    while (templateLocation.indexOf(dots) > -1) {
                        File temp = new File(templateSourceDirectory);
                        templateSourceDirectory = temp.getParent();
                        templateLocation = templateLocation.substring(templateLocation.indexOf(dots) + dotslen);
                        debugString = debugString + "TEMPLATE LOCATION2" + templateLocation + "<br />\n";
                    }
                }
                if (isGuest.equals("YES")) {
                    File temp = new File(templateSourceDirectory);
                    templateSourceDirectory = temp.getParent();
                }
                if ((templateLocation.charAt(0) != '\\') && (templateLocation.charAt(0) != '/')) templateLocation = File.separator + templateLocation;
                templateLocation.replace("/", File.separator);
                templatePath = templateSourceDirectory + templateLocation;
                templatePath = templatePath.replace("/", File.separator);
                debugString = debugString + "TEMPLATE PATH" + templatePath + "<br />\n";
                File templtefile = new File(templatePath);
                if (!templtefile.exists()) templatePath = "";
                debugString = debugString + "TEMPLATE PATH AFTER CHECK 1" + templatePath + "<br />\n";
            }
            if ((templatePath.equals("")) && (templateString.equals(""))) {
                debugString = debugString + "TEMPLATE PATH or STRING NOT FOUND" + templatePath + "<br />\n";
                templateSourceDirectory = thedataStore.getProperty("XMLfileDirectory") + File.separator + "templates";
                templatefile = template.substring(template.lastIndexOf("/") + 1, template.length());
                templatefile = templatefile + ".xml";
                templatePath = templateSourceDirectory + File.separator + templatefile;
                debugString = debugString + "TEMPLATE PATH AFTER STRING AND PATH BLANK, FROM INTERNAL" + templatePath + "<br />\n";
            }
            File templtefile = new File(templatePath);
            if (!templtefile.exists()) templatePath = "";
            debugString = debugString + "TEMPLATE Path At End of second check " + templatePath + "<br />\n";
        } catch (Exception ioe) {
            String info = " EXCEPTION IN RESPONSE TEMPLATE RETREIVAL \n";
            info = info + ioe.getMessage() + "\n";
            info = info + debugString + "\n";
            throw new gqtiexcept.XMLException(info);
        }
        try {
            DOMNodeParser parser;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            templateURL = this.getClass().getResource(templatefile);
            if (!templateString.equals("")) {
                debugString = debugString + "USING TEMPLATE STRING <br />\n";
                templatexml = builder.parse(new org.xml.sax.InputSource(new StringReader(templateString)));
            } else if (!templatePath.equals("")) {
                templatexml = builder.parse(new File(templatePath));
                debugString = debugString + "USING TEMPLATE PATH <br />\n";
            } else if (templateURL != null) {
                templatexml = builder.parse(templateURL.toString());
                debugString = debugString + "USING TEMPLATE URL FROM RESOURCE <br />\n";
            }
            parser = new DOMNodeParser(templatexml, parentNode, thedataStore);
            parser.parseDocument(templatexml, Parameters);
        } catch (SAXParseException spe) {
            String systemId = spe.getSystemId();
            if (systemId == null) systemId = "null";
            String info = "PARSE EXCEPTION IN RESPONSE TEMPLATE PROCESSING" + "\n URI = " + systemId + " \n Line = " + spe.getLineNumber() + ",\n Column = " + spe.getColumnNumber() + ":\n" + spe.getMessage() + "\n INFOEND";
            throw new gqtiexcept.XMLException(info);
        } catch (ParserConfigurationException pce) {
            String info = "PARSE CONFIGURATION EXCEPTION IN RESPONSE TEMPLATE PROCESSING \n";
            info = info + pce.getMessage() + "\n";
            throw new gqtiexcept.XMLException(info);
        } catch (SAXException se) {
            String info = "SAX EXCEPTION IN RESPONSE TEMPLATE PROCESSING \n";
            info = info + se.getMessage() + "\n";
            throw new gqtiexcept.XMLException(info);
        } catch (IOException ioe) {
            String info = "IO EXCEPTION IN RESPONSE TEMPLATE PROCESSING \n";
            info = info + ioe.getMessage() + "\n";
            throw new gqtiexcept.XMLException(info);
        }
    }

    private String GetStringFromURL(String URL) {
        InputStream in = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        String outstring = "";
        try {
            java.net.URL url = new java.net.URL(URL);
            in = url.openStream();
            inputStreamReader = new InputStreamReader(in);
            bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer out = new StringBuffer("");
            String nextLine;
            String newline = System.getProperty("line.separator");
            while ((nextLine = bufferedReader.readLine()) != null) {
                out.append(nextLine);
                out.append(newline);
            }
            outstring = new String(out);
        } catch (IOException e) {
            System.out.println("Failed to read from " + URL);
            outstring = "";
        } finally {
            try {
                bufferedReader.close();
                inputStreamReader.close();
            } catch (Exception e) {
            }
        }
        return outstring;
    }
}
