package uiuc.oai;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.xpath.*;
import org.apache.xpath.objects.*;
import org.apache.xml.utils.*;

/**
 * This class is used internally by other classes to handle all of the OAI responses which represent a list of values: OAIRecordList,
 * OAIMetadataFormatList, and OAISetList. This is the class that allows the other classes to implement the forward-only cursor and
 * transparently handle resumptionTokens, flow control, and other nuances of the OAI protocol.
 *
 * This is a private, internal class, and it is included for information only.
 */
public class OAIResumptionStream {

    /**
         * Constructs an OAIResumptionStream object with the given OAIRepository, baseURL, and verb.
         */
    public OAIResumptionStream(OAIRepository repo, String u, String v) throws OAIException {
        initialize(repo, u, v, "");
    }

    /**
         * Constructs an OAIResumptionStream object with the given OAIRepository, baseURL, verb, and query parameters.
         */
    public OAIResumptionStream(OAIRepository repo, String u, String v, String params) throws OAIException {
        initialize(repo, u, v, params);
    }

    /**
	 * init the stream, this will perform the first query and get the results
	 */
    private void initialize(OAIRepository repo, String u, String v, String params) throws OAIException {
        oParent = repo;
        strVerb = v;
        strBaseURL = u;
        strParams = params;
        strResumptionToken = "";
        iResumptionCount = 0;
        boolInitialized = false;
        boolValidResponse = false;
        iIndex = 1;
        iCount = -1;
        iCursor = -1;
        iRealCursor = -1;
        iCompleteListSize = -1;
        if (!strVerb.equals("ListIdentifiers") && !strVerb.equals("ListMetadataFormats") && !strVerb.equals("ListRecords") && !strVerb.equals("ListSets")) {
            throw new OAIException(OAIException.INVALID_VERB_ERR, "Invalid verb");
        }
        if (strBaseURL.length() == 0) {
            throw new OAIException(OAIException.NO_BASE_URL_ERR, "No baseURL");
        }
        if (params.length() > 0) {
            if (params.charAt(0) != '&') {
                params = "&" + params;
            }
        }
        try {
            URL url = new URL(strBaseURL + "?verb=" + strVerb + params);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http = oParent.frndTrySend(http);
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            if (oParent.getValidation() == OAIRepository.VALIDATION_VERY_STRICT) {
                docFactory.setValidating(true);
            } else {
                docFactory.setValidating(false);
            }
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            try {
                xml = docBuilder.parse(http.getInputStream());
                boolValidResponse = true;
            } catch (IllegalArgumentException iae) {
                throw new OAIException(OAIException.CRITICAL_ERR, iae.getMessage());
            } catch (SAXException se) {
                if (oParent.getValidation() != OAIRepository.VALIDATION_LOOSE) {
                    throw new OAIException(OAIException.XML_PARSE_ERR, se.getMessage() + " Try loose validation.");
                } else {
                    try {
                        http.disconnect();
                        url = new URL(strBaseURL + "?verb=" + strVerb + params);
                        http = (HttpURLConnection) url.openConnection();
                        http = oParent.frndTrySend(http);
                        xml = docBuilder.parse(priCreateDummyResponse(http.getInputStream()));
                    } catch (SAXException se2) {
                        throw new OAIException(OAIException.XML_PARSE_ERR, se2.getMessage());
                    }
                }
            }
            namespaceNode = xml.createElement(strVerb);
            namespaceNode.setAttribute("xmlns:oai", OAIRepository.XMLNS_OAI + strVerb);
            namespaceNode.setAttribute("xmlns:dc", OAIRepository.XMLNS_DC);
            PrefixResolverDefault prefixResolver = new PrefixResolverDefault(namespaceNode);
            XPath xpath = new XPath("//oai:" + strVerb + "/oai:" + priGetMainNodeName(), null, prefixResolver, XPath.SELECT, null);
            XPathContext xpathSupport = new XPathContext();
            int ctxtNode = xpathSupport.getDTMHandleFromNode(xml);
            XObject list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
            Node node = list.nodeset().nextNode();
            if (node == null) {
                namespaceNode.setAttribute("xmlns:oai", OAIRepository.XMLNS_OAI_2_0);
                prefixResolver = new PrefixResolverDefault(namespaceNode);
                xpath = new XPath("/oai:OAI-PMH", null, prefixResolver, XPath.SELECT, null);
                list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                node = list.nodeset().nextNode();
                if (node == null) {
                    namespaceNode.setAttribute("xmlns:oai", OAIRepository.XMLNS_OAI_1_0 + strVerb);
                } else {
                    xpath = new XPath("oai:OAI-PMH/oai:error", null, prefixResolver, XPath.SELECT, null);
                    list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                    NodeList nl = list.nodelist();
                    if (nl.getLength() > 0) {
                        oParent.frndSetErrors(nl);
                        throw new OAIException(OAIException.OAI_ERR, oParent.getLastOAIError().getCode() + ": " + oParent.getLastOAIError().getReason());
                    }
                }
            }
            xpath = new XPath("//oai:" + strVerb + "/oai:" + priGetMainNodeName(), null, prefixResolver, XPath.SELECT, null);
            list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
            nodeList = list.nodelist();
            boolInitialized = true;
            oParent.frndSetNamespaceNode(namespaceNode);
            xpath = new XPath("//oai:requestURL | //oai:request", null, prefixResolver, XPath.SELECT, null);
            node = xpath.execute(xpathSupport, ctxtNode, prefixResolver).nodeset().nextNode();
            if (node != null) {
                oParent.frndSetRequest(node);
            }
            oParent.frndSetResponseDate(getResponseDate());
            docFactory = null;
            docBuilder = null;
            url = null;
            prefixResolver = null;
            xpathSupport = null;
            xpath = null;
        } catch (TransformerException te) {
            throw new OAIException(OAIException.CRITICAL_ERR, te.getMessage());
        } catch (MalformedURLException mue) {
            throw new OAIException(OAIException.CRITICAL_ERR, mue.getMessage());
        } catch (FactoryConfigurationError fce) {
            throw new OAIException(OAIException.CRITICAL_ERR, fce.getMessage());
        } catch (ParserConfigurationException pce) {
            throw new OAIException(OAIException.CRITICAL_ERR, pce.getMessage());
        } catch (IOException ie) {
            throw new OAIException(OAIException.CRITICAL_ERR, ie.getMessage());
        }
    }

    /**
	 * Return the complete size of a returned list (if possible).
	 *
	 * For OAI 2.0 repositories which return the completeListSize attribute on their
	 * resumptionTokens, that value will be used.  Otherwise, this value will be -1, meaning
	 * unknown.  If this is the last chunk of data to be retrieved (there is no
	 * resumptionToken) the CompleteListSize can be calculated from the count of all
	 * previously retrieved records, plus the size of this chunk, and that value will be
	 * returned.
	 */
    public int getCompleteSize() throws OAIException {
        priCheckInitialized();
        priGetResumptionToken();
        return iCompleteListSize;
    }

    /**
	 * Return the index which starts the most recent response.
	 *
	 * For OAI 2.0 repositories which return the cursor attribute on their
	 * resumptionTokens, that value will be used.  Otherwise, this value will be
	 * calculated based on the count of total items returned thus far
	 */
    public int getResumptionCursor() throws OAIException {
        priCheckInitialized();
        priGetResumptionToken();
        if (iCursor == -1) {
            return iRealCursor;
        } else {
            return iCursor;
        }
    }

    /**
	 * Return the expiration date of the current resumptionToken or an empty string if there is none
	 */
    public String getResumptionExpirationDate() throws OAIException {
        priCheckInitialized();
        priGetResumptionToken();
        return strExpirationDate;
    }

    /**
	 * Return the index of the current record: 0 to CompleteListSize-1
	 */
    public int getIndex() throws OAIException {
        return getResumptionCursor() + iIndex - 1;
    }

    /**
	 * Returns true if the response appears to be valid (well-formed, and if the Validation if Very Strict also valid according to
	 *  the XML Schemas); if the Validation is Loose and the record is not well-formed, false is returned
	 */
    public boolean isResponseValid() {
        return boolValidResponse;
    }

    /**
	 * Returns true if there are more objects which can be returned; else false.
	 */
    public boolean more() throws OAIException {
        boolean ret = false;
        priCheckInitialized();
        if ((strResumptionToken.length() > 0) || (iIndex <= priGetSetCount())) {
            ret = true;
        }
        return ret;
    }

    /**
	 * Moves the cursor location to the next object in the list.
	 */
    public void moveNext() throws OAIException {
        int cnt;
        priCheckInitialized();
        cnt = priGetSetCount();
        if (more()) {
            if (iIndex <= cnt) {
                iIndex++;
            }
            if (iIndex > cnt) {
                priResumption();
            }
        } else {
            throw new OAIException(OAIException.NO_MORE_SETS_ERR, "No more sets");
        }
    }

    /**
	 * Returns the BASE-URL of the query.
	 */
    public String getBaseURL() throws OAIException {
        priCheckInitialized();
        return strBaseURL;
    }

    /**
	 * Return the responseDate returned by the most recent response
	 */
    public String getResponseDate() throws OAIException {
        String ret = "";
        priCheckInitialized();
        NodeList list = xml.getElementsByTagName("responseDate");
        if (list.getLength() > 0) {
            ret = list.item(0).getFirstChild().getNodeValue();
        } else {
            throw new OAIException(OAIException.INVALID_RESPONSE_ERR, strVerb + " missing responseDate");
        }
        return ret;
    }

    /**
	 * Return the requestURL returned by the most recent response
         */
    public String getRequestURL() throws OAIException {
        priCheckInitialized();
        String ret = "";
        try {
            Node node = XPathAPI.selectSingleNode(xml, "//oai:requestURL | //oai:request", namespaceNode);
            if (node != null) {
                ret = node.getFirstChild().getNodeValue();
                NamedNodeMap map = node.getAttributes();
                Node n;
                n = map.getNamedItem("verb");
                if (n != null) {
                    ret += "?verb=" + n.getNodeValue();
                }
                n = map.getNamedItem("identifier");
                if (n != null) {
                    ret += "&identifier=" + n.getNodeValue();
                }
                n = map.getNamedItem("metadataPrefix");
                if (n != null) {
                    ret += "&metadataPrefix=" + n.getNodeValue();
                }
                n = map.getNamedItem("from");
                if (n != null) {
                    ret += "&from=" + n.getNodeValue();
                }
                n = map.getNamedItem("until");
                if (n != null) {
                    ret += "&until=" + n.getNodeValue();
                }
                n = map.getNamedItem("set");
                if (n != null) {
                    ret += "&set=" + n.getNodeValue();
                }
                n = map.getNamedItem("resumptionToken");
                if (n != null) {
                    try {
                        ret += "&resumptionToken=" + URLEncoder.encode(n.getNodeValue(), "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        ret += "&resumptionToken=" + n.getNodeValue();
                    }
                }
            } else {
                throw new OAIException(OAIException.INVALID_RESPONSE_ERR, strVerb + " missing requestURL/request");
            }
        } catch (TransformerException te) {
            throw new OAIException(OAIException.CRITICAL_ERR, te.getMessage());
        }
        return ret;
    }

    /**
	 * Returns an XML node for the current object in the list. The type of node returned will depend on the type of
	 *  request (the verb parameter).
	 */
    public Node getItem() throws OAIException {
        priCheckInitialized();
        return priGetXMLItem(iIndex);
    }

    /**
	 * Returns the query string parameters used for the request (minus the verb parameter).
	 */
    public String getParams() throws OAIException {
        priCheckInitialized();
        return strParams;
    }

    /**
	 * Returns the OAIRepository object from which this list was created.
	 */
    public OAIRepository getRepository() {
        return oParent;
    }

    /**
	 * The verb parameter used for this request.
	 */
    public String getVerb() throws OAIException {
        priCheckInitialized();
        return strVerb;
    }

    /**
	 * This will reset the entire list to the beginning, redoing the query from scratch.
	 */
    public void requery() throws OAIException {
        priCheckInitialized();
        initialize(oParent, strBaseURL, strVerb, strParams);
    }

    /**
	 * Purpose: check whether the query has been initizlized or not, raise an error if not
	 */
    private void priCheckInitialized() throws OAIException {
        if (!boolInitialized) {
            throw new OAIException(OAIException.NOT_INITIALIZED_ERR, "Not initialized");
        }
    }

    /**
	 * Purpose: return the dom node at a gioven index
	 */
    private Node priGetXMLItem(int i) {
        if (nodeList != null && i <= nodeList.getLength()) {
            return nodeList.item(i - 1);
        } else {
            return null;
        }
    }

    /**
	 * Purpose: return how many records are contained in the current response
	 */
    private int priGetSetCount() {
        if (iCount >= 0) {
            return iCount;
        }
        iCount = nodeList.getLength();
        return iCount;
    }

    /**
	 * Return the number of items contained in the most recent response
	 */
    public int getResponseSize() throws OAIException {
        priCheckInitialized();
        return priGetSetCount();
    }

    /**
	 * Purpose: return the resumptionToken from the response, if none an empty string is returned
	 */
    private String priGetResumptionToken() throws OAIException {
        try {
            Node node = XPathAPI.selectSingleNode(xml, "//oai:" + strVerb + "/oai:resumptionToken/text()", namespaceNode);
            if (node != null) {
                strResumptionToken = node.getNodeValue().trim();
                NamedNodeMap map = node.getParentNode().getAttributes();
                Node n = map.getNamedItem("expirationDate");
                if (n != null) {
                    strExpirationDate = n.getNodeValue();
                } else {
                    strExpirationDate = "";
                }
                n = map.getNamedItem("completeListSize");
                if (n != null) {
                    try {
                        iCompleteListSize = Integer.parseInt(n.getNodeValue());
                    } catch (NumberFormatException ne) {
                        iCompleteListSize = -1;
                    }
                } else {
                    iCompleteListSize = -1;
                }
                n = map.getNamedItem("cursor");
                if (n != null) {
                    try {
                        iCursor = Integer.parseInt(n.getNodeValue());
                    } catch (NumberFormatException ne) {
                        iCursor = -1;
                    }
                } else {
                    iCursor = -1;
                }
            } else {
                strResumptionToken = "";
                strExpirationDate = "";
                iCompleteListSize = -1;
                iCursor = -1;
            }
        } catch (TransformerException te) {
            throw new OAIException(OAIException.CRITICAL_ERR, te.getMessage());
        }
        if (strResumptionToken.length() == 0 && iCompleteListSize == -1) {
            if (iCursor == -1) {
                iCompleteListSize = priGetSetCount() + iRealCursor;
            } else {
                iCompleteListSize = priGetSetCount() + iCursor;
            }
        }
        return strResumptionToken;
    }

    /**
	 * Purpose: return the name of the main node in response, based on the verb
	 */
    private String priGetMainNodeName() throws OAIException {
        String ret;
        if (strVerb.equals("ListSets")) {
            ret = "set";
        } else if (strVerb.equals("ListIdentifiers")) {
            if (oParent.getProtocolMajorVersion() < 2) {
                ret = "identifier";
            } else {
                ret = "header";
            }
        } else if (strVerb.equals("ListMetadataFormats")) {
            ret = "metadataFormat";
        } else if (strVerb.equals("ListRecords")) {
            ret = "record";
        } else {
            throw new OAIException(OAIException.INVALID_VERB_ERR, "Invalid verb");
        }
        return ret;
    }

    /**
  	 * Purpose: If the validation is loose, create a dummy response so that processing can
  	 *          continue if possible.
  	 *
  	 * Inputs:  x   the invalid XML string
  	 *
	 * Returns: the dummy record appropriate for the verb.  The invalid XML will be included in the
  	 *          dummy record as a CDATA section
	 */
    private InputSource priCreateDummyResponse(InputStream x) throws OAIException {
        String ret;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        if (oParent.getProtocolMajorVersion() < 2) {
            if (strVerb.equals("ListSets")) {
                ret = "<ListSets xmlns='http://www.openarchives.org/OAI/1.1/OAI_ListSets' \n";
                ret += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n";
                ret += "xsi:schemaLocation='http://www.openarchives.org/OAI/1.1/OAI_ListSets ";
                ret += "http://www.openarchives.org/OAI/1.1/OAI_ListSets.xsd'> \n";
                ret += "<responseDate>" + formatter.format(new Date()) + "</responseDate> \n";
                ret += "<requestURL>" + strBaseURL + "?verb=" + strVerb + priXMLEncode(strParams) + "</requestURL> \n";
                ret += "<set> \n";
                ret += "<setSpec>junk:set" + iResumptionCount + "</setSpec> \n";
                ret += "<setName><![CDATA[" + oParent.frndMyEncode(x) + "]]></setName> \n";
                ret += "</set> \n";
                ret += priTryToGetResumptionToken(x) + "\n";
                ret += "</ListSets>\n";
            } else if (strVerb.equals("ListIdentifiers")) {
                ret = "<ListIdentifiers xmlns='http://www.openarchives.org/OAI/1.1/OAI_ListIdentifiers' \n";
                ret += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n";
                ret += "xsi:schemaLocation='http://www.openarchives.org/OAI/1.1/OAI_ListIdentifiers ";
                ret += "http://www.openarchives.org/OAI/1.1/OAI_ListIdentifiers.xsd'> \n";
                ret += "<responseDate>" + formatter.format(new Date()) + "</responseDate> \n";
                ret += "<requestURL>" + strBaseURL + "?verb=" + strVerb + priXMLEncode(strParams) + "</requestURL> \n";
                ret += "<identifier><![CDATA[" + oParent.frndMyEncode(x) + "]]></identifier> \n";
                ret += priTryToGetResumptionToken(x) + "\n";
                ret += "</ListIdentifiers>\n";
            } else if (strVerb.equals("ListMetadataFormats")) {
                ret = "<ListMetadataFormats xmlns='http://www.openarchives.org/OAI/1.1/OAI_ListMetadataFormats' \n";
                ret += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n";
                ret += "xsi:schemaLocation='http://www.openarchives.org/OAI/1.1/OAI_ListMetadataFormats ";
                ret += "http://www.openarchives.org/OAI/1.1/OAI_ListMetadataFormats.xsd'> \n";
                ret += "<responseDate>" + formatter.format(new Date()) + "</responseDate> \n";
                ret += "<requestURL>" + strBaseURL + "?verb=" + strVerb + priXMLEncode(strParams) + "</requestURL> \n";
                ret += "<metadataFormat> \n";
                ret += "<metadataPrefix>junk_metadataPrefix" + iResumptionCount + "</metadataPrefix> \n";
                ret += "<schema><![CDATA[" + oParent.frndMyEncode(x) + "]]></schema> \n";
                ret += "</metadataFormat> \n";
                ret += priTryToGetResumptionToken(x) + "\n";
                ret += "</ListMetadataFormats>\n";
            } else if (strVerb.equals("ListRecords")) {
                ret = "<ListRecords \n";
                ret += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n";
                ret += "xmlns='http://www.openarchives.org/OAI/1.1/OAI_ListRecords' \n";
                ret += "xsi:schemaLocation='http://www.openarchives.org/OAI/1.1/OAI_ListRecords ";
                ret += "http://www.openarchives.org/OAI/1.1/OAI_ListRecords.xsd'>\n";
                ret += "<responseDate>" + formatter.format(new Date()) + "</responseDate>\n";
                ret += "<requestURL>" + strBaseURL + "?verb=" + strVerb + priXMLEncode(strParams) + "</requestURL>";
                ret += "<record>\n";
                ret += "<header>\n";
                if (oParent.usesOAIIdentifier()) {
                    ret += "<identifier>" + oParent.getRepositoryIdentifier() + "junk:identifier" + iResumptionCount + "</identifier>\n";
                } else {
                    ret += "<identifier>junk:identifier" + iResumptionCount + "</identifier>\n";
                }
                ret += "<datestamp>" + formatter.format(new Date()) + "</datestamp>\n";
                ret += "</header>\n";
                ret += "<about>\n";
                ret += "<junk:junk xmlns:junk='junk:junk'><![CDATA[" + oParent.frndMyEncode(x) + "]]></junk:junk>\n";
                ret += "</about>\n";
                ret += "</record>\n";
                ret += priTryToGetResumptionToken(x) + "\n";
                ret += "</ListRecords>\n";
            } else {
                throw new OAIException(OAIException.INVALID_VERB_ERR, "Invalid verb");
            }
        } else {
            if (strVerb.equals("ListSets")) {
                ret = "<OAI-PMH xmlns='http://www.openarchives.org/OAI/2.0/' \n";
                ret += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n";
                ret += "xsi:schemaLocation='http://www.openarchives.org/OAI/2.0/ ";
                ret += "http://www.openarchives.org/OAI/2.0/OAI_PMH.xsd'> \n";
                ret += "<responseDate>" + formatter.format(new Date()) + "</responseDate> \n";
                ret += "<request>" + strBaseURL + "?verb=" + strVerb + priXMLEncode(strParams) + "</request> \n";
                ret += "<ListSets> \n";
                ret += "<set> \n";
                ret += "<setSpec>junk:set" + iResumptionCount + "</setSpec> \n";
                ret += "<setName>INVALID SET</setName> \n";
                ret += "<setDescription><![CDATA[" + oParent.frndMyEncode(x) + "]]></setDescription> \n";
                ret += "</set> \n";
                ret += priTryToGetResumptionToken(x) + "\n";
                ret += "</ListSets>\n";
                ret += "</OAI-PMH>\n";
            } else if (strVerb.equals("ListIdentifiers")) {
                ret = "<OAI-PMH xmlns='http://www.openarchives.org/OAI/2.0/' \n";
                ret += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n";
                ret += "xsi:schemaLocation='http://www.openarchives.org/OAI/2.0/ ";
                ret += "http://www.openarchives.org/OAI/2.0/OAI_PMH.xsd'> \n";
                ret += "<responseDate>" + formatter.format(new Date()) + "</responseDate> \n";
                ret += "<request>" + strBaseURL + "?verb=" + strVerb + priXMLEncode(strParams) + "</request> \n";
                ret += "<ListIdentifiers>\n";
                ret += "<header>\n";
                if (oParent.usesOAIIdentifier()) {
                    ret += "<identifier>" + oParent.getRepositoryIdentifier() + "junk:identifier" + iResumptionCount + "</identifier>\n";
                } else {
                    ret += "<identifier>junk:identifier" + iResumptionCount + "</identifier>\n";
                }
                ret += "<datestamp>" + formatter.format(new Date()) + "</datestamp>\n";
                ret += "<setSpec><![CDATA[" + oParent.frndMyEncode(x) + "]]>\n";
                ret += "</header>\n";
                ret += priTryToGetResumptionToken(x) + "\n";
                ret += "</ListIdentifiers>\n";
                ret += "</OAI-PMH>\n";
            } else if (strVerb.equals("ListMetadataFormats")) {
                ret = "<OAI-PMH xmlns='http://www.openarchives.org/OAI/2.0/' \n";
                ret += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n";
                ret += "xsi:schemaLocation='http://www.openarchives.org/OAI/2.0/ ";
                ret += "http://www.openarchives.org/OAI/2.0/OAI_PMH.xsd'> \n";
                ret += "<responseDate>" + formatter.format(new Date()) + "</responseDate> \n";
                ret += "<request>" + strBaseURL + "?verb=" + strVerb + priXMLEncode(strParams) + "</request> \n";
                ret += "<metadataFormat> \n";
                ret += "<metadataPrefix>junk_metadataPrefix" + iResumptionCount + "</metadataPrefix> \n";
                ret += "<schema><![CDATA[" + oParent.frndMyEncode(x) + "]]></schema> \n";
                ret += "</metadataFormat> \n";
                ret += priTryToGetResumptionToken(x) + "\n";
                ret += "</ListMetadataFormats>\n";
                ret += "</OAI-PMH>\n";
            } else if (strVerb.equals("ListRecords")) {
                ret = "<OAI-PMH xmlns='http://www.openarchives.org/OAI/2.0/' \n";
                ret += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n";
                ret += "xsi:schemaLocation='http://www.openarchives.org/OAI/2.0/ ";
                ret += "http://www.openarchives.org/OAI/2.0/OAI_PMH.xsd'> \n";
                ret += "<responseDate>" + formatter.format(new Date()) + "</responseDate> \n";
                ret += "<request>" + strBaseURL + "?verb=" + strVerb + priXMLEncode(strParams) + "</request> \n";
                ret += "<record>\n";
                ret += "<header>\n";
                if (oParent.usesOAIIdentifier()) {
                    ret += "<identifier>" + oParent.getRepositoryIdentifier() + "junk:identifier" + iResumptionCount + "</identifier>\n";
                } else {
                    ret += "<identifier>junk:identifier" + iResumptionCount + "</identifier>\n";
                }
                ret += "<datestamp>" + formatter.format(new Date()) + "</datestamp>\n";
                ret += "</header>\n";
                ret += "<about>\n";
                ret += "<junk:junk xmlns:junk='junk:junk'><![CDATA[" + oParent.frndMyEncode(x) + "]]></junk:junk>\n";
                ret += "</about>\n";
                ret += "</record>\n";
                ret += priTryToGetResumptionToken(x) + "\n";
                ret += "</ListRecords>\n";
                ret += "</OAI-PMH>\n";
            } else {
                throw new OAIException(OAIException.INVALID_VERB_ERR, "Invalid verb");
            }
        }
        StringReader sr = new StringReader(ret);
        return new InputSource(sr);
    }

    /**
  	 * Inputs:  x   the string which may contain a resumptionToken
  	 *
  	 * Returns: the resumptionToken or an empty string
	 */
    private String priTryToGetResumptionToken(InputStream x) throws OAIException {
        String ret = "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(x, "UTF-8"));
            String tmp = "";
            int startIdx = -1;
            int endIdx = -1;
            while ((tmp = br.readLine()) != null) {
                if (startIdx < 0) {
                    startIdx = tmp.indexOf("<resumptionToken");
                    ret = tmp.substring(startIdx + 17);
                    endIdx = ret.indexOf("</resumptionToken");
                    if (endIdx > 0) {
                        ret = ret.substring(0, endIdx);
                        break;
                    }
                }
                if (startIdx > 0) {
                    ret += tmp;
                    endIdx = ret.indexOf("</resumptionToken");
                    if (endIdx > 0) {
                        ret = ret.substring(0, endIdx);
                        break;
                    }
                }
            }
        } catch (IOException ie) {
            throw new OAIException(OAIException.CRITICAL_ERR, ie.getMessage());
        }
        return ret;
    }

    private String priXMLEncode(String s) {
        String ret = s;
        int idx = 0;
        while ((idx = ret.indexOf('&', idx)) >= 0) {
            ret = ret.substring(0, idx) + "&amp;" + ret.substring(idx + 1);
            idx += 4;
        }
        return ret;
    }

    /**
	 * Purpose: resume the query using a resumption token
	 */
    private void priResumption() throws OAIException {
        String rt = priGetResumptionToken();
        if (rt.length() == 0) {
            return;
        }
        int prevCount = priGetSetCount();
        iCount = -1;
        iResumptionCount++;
        try {
            URL url = new URL(strBaseURL + "?verb=" + strVerb + "&resumptionToken=" + URLEncoder.encode(rt, "UTF-8"));
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http = oParent.frndTrySend(http);
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            if (oParent.getValidation() == OAIRepository.VALIDATION_VERY_STRICT) {
                docFactory.setValidating(true);
            } else {
                docFactory.setValidating(false);
            }
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            try {
                xml = docBuilder.parse(http.getInputStream());
                boolValidResponse = true;
            } catch (IllegalArgumentException iae) {
                throw new OAIException(OAIException.CRITICAL_ERR, iae.getMessage());
            } catch (SAXException se) {
                if (oParent.getValidation() != OAIRepository.VALIDATION_LOOSE) {
                    throw new OAIException(OAIException.XML_PARSE_ERR, se.getMessage() + " Try loose validation.");
                } else {
                    try {
                        http.disconnect();
                        url = new URL(strBaseURL + "?verb=" + strVerb + "&resumptionToken=" + URLEncoder.encode(rt, "UTF-8"));
                        http = (HttpURLConnection) url.openConnection();
                        http = oParent.frndTrySend(http);
                        xml = docBuilder.parse(priCreateDummyResponse(http.getInputStream()));
                    } catch (SAXException se2) {
                        throw new OAIException(OAIException.XML_PARSE_ERR, se2.getMessage());
                    }
                }
            }
            try {
                namespaceNode = xml.createElement(strVerb);
                namespaceNode.setAttribute("xmlns:oai", OAIRepository.XMLNS_OAI + strVerb);
                namespaceNode.setAttribute("xmlns:dc", OAIRepository.XMLNS_DC);
                PrefixResolverDefault prefixResolver = new PrefixResolverDefault(namespaceNode);
                XPath xpath = new XPath("/oai:" + strVerb, null, prefixResolver, XPath.SELECT, null);
                XPathContext xpathSupport = new XPathContext();
                int ctxtNode = xpathSupport.getDTMHandleFromNode(xml);
                XObject list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                Node node = list.nodeset().nextNode();
                if (node == null) {
                    namespaceNode.setAttribute("xmlns:oai", OAIRepository.XMLNS_OAI_2_0);
                    prefixResolver = new PrefixResolverDefault(namespaceNode);
                    xpath = new XPath("/oai:OAI-PMH", null, prefixResolver, XPath.SELECT, null);
                    list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                    node = list.nodeset().nextNode();
                    if (node == null) {
                        namespaceNode.setAttribute("xmlns:oai", OAIRepository.XMLNS_OAI_1_0 + strVerb);
                    } else {
                        xpath = new XPath("oai:OAI-PMH/oai:error", null, prefixResolver, XPath.SELECT, null);
                        list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                        NodeList nl = list.nodelist();
                        if (nl.getLength() > 0) {
                            oParent.frndSetErrors(nl);
                            throw new OAIException(OAIException.OAI_ERR, oParent.getLastOAIError().getCode() + ": " + oParent.getLastOAIError().getReason());
                        }
                    }
                }
                xpath = new XPath("//oai:" + strVerb + "/oai:" + priGetMainNodeName(), null, prefixResolver, XPath.SELECT, null);
                list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                nodeList = list.nodelist();
                oParent.frndSetNamespaceNode(namespaceNode);
                xpath = new XPath("//oai:requestURL | //oai:request", null, prefixResolver, XPath.SELECT, null);
                node = xpath.execute(xpathSupport, ctxtNode, prefixResolver).nodeset().nextNode();
                if (node != null) {
                    oParent.frndSetRequest(node);
                }
                oParent.frndSetResponseDate(getResponseDate());
                iRealCursor += prevCount;
                prefixResolver = null;
                xpathSupport = null;
                xpath = null;
            } catch (TransformerException te) {
                throw new OAIException(OAIException.CRITICAL_ERR, te.getMessage());
            } catch (IllegalArgumentException iae) {
                throw new OAIException(OAIException.CRITICAL_ERR, iae.getMessage());
            }
            docFactory = null;
            docBuilder = null;
            url = null;
        } catch (MalformedURLException mue) {
            throw new OAIException(OAIException.CRITICAL_ERR, mue.getMessage());
        } catch (UnsupportedEncodingException ex) {
            throw new OAIException(OAIException.CRITICAL_ERR, ex.getMessage());
        } catch (FactoryConfigurationError fce) {
            throw new OAIException(OAIException.CRITICAL_ERR, fce.getMessage());
        } catch (ParserConfigurationException pce) {
            throw new OAIException(OAIException.CRITICAL_ERR, pce.getMessage());
        } catch (IOException ie) {
            throw new OAIException(OAIException.CRITICAL_ERR, ie.getMessage());
        }
        iIndex = 1;
    }

    private String strBaseURL;

    private String strVerb;

    private String strParams;

    private String strResumptionToken;

    private String strExpirationDate;

    private Document xml;

    private NodeList nodeList;

    private int iIndex;

    private int iCompleteListSize;

    private int iCursor;

    private int iCount;

    private int iRealCursor;

    private Element namespaceNode;

    private boolean boolInitialized;

    private boolean boolValidResponse;

    private int iResumptionCount;

    private OAIRepository oParent;
}
