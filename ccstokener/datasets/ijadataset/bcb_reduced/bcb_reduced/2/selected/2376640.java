package uiuc.oai;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import org.apache.xpath.*;
import org.apache.xpath.objects.*;
import org.apache.xml.utils.*;
import sun.misc.*;

/**
 * This class represents an OAI repository.
 */
public class OAIRepository {

    /**
	 * Construct an empty OAI repository.
	 */
    public OAIRepository() {
        state = STATE_UNIDENTIFIED;
        validation = VALIDATION_STRICT;
        strBaseURL = "";
        iRetryLimit = 5;
        iMaxRetryMinutes = 60;
        strUserAgent = "OAIHarvester University of Illinois Library";
        strFrom = "someone@somewhere.edu";
        strUser = "";
        strPassword = "";
    }

    /**
	 * Sets the BASE-URL of the repository; must be set before most other properties or methods can be used.
	 */
    public void setBaseURL(String url) throws OAIException {
        strBaseURL = url;
        identify();
    }

    /**
	 * Returns the BASE-URL of the repository.
	 */
    public String getBaseURL() throws OAIException {
        priCheckBaseURL();
        return strBaseURL;
    }

    /**
	 * Create a dummy OAI GetRecord.  Used with ValidationLoose if an invalid record is returned.
	 * The original invalid record is placed inside the about element of this dummy record.
	 */
    private InputSource priCreateDummyGetRecord(String id, InputStream xml) throws OAIException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyy-MM-dd");
        String rec;
        if (getProtocolMajorVersion() < 2) {
            rec = "<GetRecord \n";
            rec += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n";
            rec += "xmlns='http://www.openarchives.org/OAI/1.1/OAI_GetRecord' \n";
            rec += "xsi:schemaLocation='http://www.openarchives.org/OAI/1.1/OAI_GetRecord ";
            rec += "http://www.openarchives.org/OAI/1.1/OAI_GetRecord.xsd'>\n";
            rec += "<responseDate>" + formatter.format(new java.util.Date()) + "</responseDate>\n";
            rec += "<requestURL>junk:GetRecord</requestURL>\n";
            rec += "<record>\n";
            rec += "<header>\n";
            rec += "<identifier>" + id + "</identifier>\n";
            rec += "<datestamp>" + formatter.format(new java.util.Date()) + "</datestamp>\n";
            rec += "</header>\n";
            rec += "<about>\n";
            rec += "<junk:junk xmlns:junk='junk:junk'><![CDATA[" + frndMyEncode(xml) + "]]></junk:junk>\n";
            rec += "</about>\n";
            rec += "</record>\n";
            rec += "</GetRecord>";
        } else {
            rec = "<OAI-PMH \n";
            rec += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n";
            rec += "xmlns='http://www.openarchives.org/OAI/2.0/' \n";
            rec += "xsi:schemaLocation='http://www.openarchives.org/OAI/2.0/ ";
            rec += "http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd'>\n";
            rec += "<responseDate>" + formatter.format(new java.util.Date()) + "</responseDate>\n";
            rec += "<request>junk:GetRecord</request>\n";
            rec += "<GetRecord>\n";
            rec += "<record>\n";
            rec += "<header>\n";
            rec += "<identifier>" + id + "</identifier>\n";
            rec += "<datestamp>" + formatter.format(new java.util.Date()) + "</datestamp>\n";
            rec += "</header>\n";
            rec += "<about>\n";
            rec += "<junk:junk xmlns:junk='junk:junk'><![CDATA[" + frndMyEncode(xml) + "]]></junk:junk>\n";
            rec += "</about>\n";
            rec += "</record>\n";
            rec += "</GetRecord>\n";
            rec += "</OAI-PMH>";
        }
        StringReader sr = new StringReader(rec);
        return new InputSource(sr);
    }

    /**
	 * Create a dummy OAI Identify.  Used with ValidationLoose if an invalid record is returned.
 	 * The original invalid record is placed inside the description element of this dummy record.
	 */
    private InputSource priCreateDummyIdentify(InputStream xml) throws OAIException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyy-MM-dd");
        String ret;
        if (getProtocolMajorVersion() < 2) {
            ret = "<Identify \n";
            ret += "xmlns='http://www.openarchives.org/OAI/1.1/OAI_Identify' \n";
            ret += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n";
            ret += "xsi:schemaLocation='http://www.openarchives.org/OAI/1.1/OAI_Identify ";
            ret += "http://www.openarchives.org/OAI/1.1/OAI_Identify.xsd'> \n";
            ret += "<responseDate>" + formatter.format(new java.util.Date()) + "</responseDate> \n";
            ret += "<requestURL>junk:Identify</requestURL> \n";
            ret += "<repositoryName>UNKNOWN</repositoryName> \n";
            ret += "<baseURL>" + strBaseURL + "</baseURL> \n";
            ret += "<protocolVersion>UNKNOWN</protocolVersion> \n";
            ret += "<adminEmail>mailto:UNKNOWN</adminEmail> \n";
            ret += "<description>\n";
            ret += "<junk:junk xmlns:junk='junk:junk'><![CDATA[" + frndMyEncode(xml) + "]]></junk:junk>\n";
            ret += "</description>\n";
            ret += "</Identify>";
        } else {
            ret = "<OAI-PMH \n";
            ret += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n";
            ret += "xmlns='http://www.openarchives.org/OAI/2.0/' \n";
            ret += "xsi:schemaLocation='http://www.openarchives.org/OAI/2.0/ ";
            ret += "http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd'>\n";
            ret += "<responseDate>" + formatter.format(new java.util.Date()) + "</responseDate>\n";
            ret += "<request>junk:Identify</request>\n";
            ret += "<Identify>\n";
            ret += "<repositoryName>UNKNOWN</repositoryName> \n";
            ret += "<baseURL>" + strBaseURL + "</baseURL> \n";
            ret += "<protocolVersion>UNKNOWN</protocolVersion> \n";
            ret += "<adminEmail>mailto:UNKNOWN</adminEmail> \n";
            ret += "<description>\n";
            ret += "<junk:junk xmlns:junk='junk:junk'><![CDATA[" + frndMyEncode(xml) + "]]></junk:junk>\n";
            ret += "</description>\n";
            ret += "</Identify>\n";
            ret += "</OAI-PMH>";
        }
        StringReader sr = new StringReader(ret);
        return new InputSource(sr);
    }

    protected void frndSetErrors(NodeList e) {
        ixmlErrors = e;
    }

    protected void frndSetRepositoryID(String id) {
        strRepositoryId = id;
    }

    protected void frndSetRequest(Node u) {
        ixmlRequest = u;
    }

    protected void frndSetResponseDate(String d) {
        strResponseDate = d;
    }

    protected String frndGetRawResponse() {
        return sRawResponse;
    }

    protected String frndGetUser() {
        return strUser;
    }

    protected String frndGetPassword() {
        return strPassword;
    }

    /**
	 * Set the user and password to use for Basic HTTP Authorization
	 */
    public void setBasicAuthorization(String usr, String pwd) {
        strUser = usr;
        strPassword = pwd;
    }

    /**
	 * Returns one of the errors returned by the repository
	 */
    public OAIError getLastOAIError() throws OAIException {
        return getLastOAIError(0);
    }

    public OAIError getLastOAIError(int i) throws OAIException {
        OAIError err = null;
        if (getLastOAIErrorCount() > 0 && i < getLastOAIErrorCount()) {
            err = new OAIError();
            Node n = ixmlErrors.item(i);
            err.frndSetCode(n.getAttributes().getNamedItem("code").getNodeValue());
            err.frndSetReason(n.getFirstChild().getNodeValue());
        }
        return err;
    }

    /**
	 * Returns how many errors were returned by the repository
	 */
    public int getLastOAIErrorCount() {
        if (ixmlErrors != null) {
            return ixmlErrors.getLength();
        } else {
            return 0;
        }
    }

    /**
	 * Returns the verb query param returned by the most recent response
	 */
    public String getRequestVerb() {
        String ret = "";
        int idx1 = getRequestURL().indexOf("verb=");
        if (idx1 >= 0) {
            int idx2 = getRequestURL().indexOf("&", idx1);
            if (idx2 <= 0) {
                idx2 = getRequestURL().length();
            }
            ret = getRequestURL().substring(idx1 + 5, idx2);
        }
        return ret;
    }

    /**
	 * Returns the identifier query param returned by the most recent response
	 */
    public String getRequestIdentifier() {
        String ret = "";
        int idx1 = getRequestURL().indexOf("identifier=");
        if (idx1 >= 0) {
            int idx2 = getRequestURL().indexOf("&", idx1);
            if (idx2 <= 0) {
                idx2 = getRequestURL().length();
            }
            ret = getRequestURL().substring(idx1 + 11, idx2);
        }
        return ret;
    }

    /**
	 * Returns the metadataPrefix query param returned by the most recent response
	 */
    public String getRequestMetadataPrefix() {
        String ret = "";
        int idx1 = getRequestURL().indexOf("metadataPrefix=");
        if (idx1 >= 0) {
            int idx2 = getRequestURL().indexOf("&", idx1);
            if (idx2 <= 0) {
                idx2 = getRequestURL().length();
            }
            ret = getRequestURL().substring(idx1 + 15, idx2);
        }
        return ret;
    }

    /**
	 * Returns the from query param returned by the most recent response
	 */
    public String getRequestFrom() {
        String ret = "";
        int idx1 = getRequestURL().indexOf("from=");
        if (idx1 >= 0) {
            int idx2 = getRequestURL().indexOf("&", idx1);
            if (idx2 <= 0) {
                idx2 = getRequestURL().length();
            }
            ret = getRequestURL().substring(idx1 + 5, idx2);
        }
        return ret;
    }

    /**
	 * Returns the until query param returned by the most recent response
	 */
    public String getRequestUntil() {
        String ret = "";
        int idx1 = getRequestURL().indexOf("until=");
        if (idx1 >= 0) {
            int idx2 = getRequestURL().indexOf("&", idx1);
            if (idx2 <= 0) {
                idx2 = getRequestURL().length();
            }
            ret = getRequestURL().substring(idx1 + 6, idx2);
        }
        return ret;
    }

    /**
	 * Returns the set query param returned by the most recent response
	 */
    public String getRequestSet() {
        String ret = "";
        int idx1 = getRequestURL().indexOf("set=");
        if (idx1 >= 0) {
            int idx2 = getRequestURL().indexOf("&", idx1);
            if (idx2 <= 0) {
                idx2 = getRequestURL().length();
            }
            ret = getRequestURL().substring(idx1 + 4, idx2);
        }
        return ret;
    }

    /**
	 * Returns the resumptionToken query param returned by the most recent response
	 */
    public String getRequestResumptionToken() {
        String ret = "";
        int idx1 = getRequestURL().indexOf("resumptionToken=");
        if (idx1 >= 0) {
            int idx2 = getRequestURL().indexOf("&", idx1);
            if (idx2 <= 0) {
                idx2 = getRequestURL().length();
            }
            ret = getRequestURL().substring(idx1 + 16, idx2);
        }
        return ret;
    }

    /**
	 * Returns how many admin emails there are for the repository
	 */
    public int getAdminEmailCount() throws OAIException {
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        if (strAdminEmail == null) {
            return 0;
        } else {
            return strAdminEmail.length;
        }
    }

    /**
	 * Returns how many compressions are supported by the repository
	 */
    public int getCompressionCount() throws OAIException {
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        if (strCompression == null) {
            return 0;
        } else {
            return strCompression.length;
        }
    }

    /**
	 * Returns how many compressions are supported by the repository
	 */
    public String getEarliestDatestamp() throws OAIException {
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        if (getProtocolMajorVersion() < 2) {
            throw new OAIException(OAIException.OAI_2_ONLY_ERR, "'EarliestDateStamp' is not supporeted.");
        }
        return strEarliestDatestamp;
    }

    /**
	 * Returns the type of deleted items supported by the repository
	 */
    public String getDeletedRecord() throws OAIException {
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        if (getProtocolMajorVersion() < 2) {
            throw new OAIException(OAIException.OAI_2_ONLY_ERR, "'DeletedRecord' is not supported.");
        }
        return strDeletedRecord;
    }

    /**
	 * Returns the datestamp granularity supported by the repository
	 */
    public String getGranularity() throws OAIException {
        String ret;
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        if (getProtocolMajorVersion() < 2) {
            ret = "YYYY-MM-DD";
        } else if (strGranularity != null) {
            ret = strGranularity;
        } else {
            ret = "YYYY-MM-DD";
            ;
        }
        return ret;
    }

    /**
	 * The Response Date returned by the most recent request. If validation is loose and there is a missing responseDate, an empty
	 *  string will be returned; otherwise, an error will be raised.
	 */
    public String getResponseDate() {
        return strResponseDate;
    }

    /**
	 * The Request URL returned by the most recent request. If validation is loose and there is a missing requestURL, an empty
	 *  string will be returned; otherwise, an error will be raised.
	 */
    public String getRequestBaseURL() {
        String ret = ixmlRequest.getFirstChild().getNodeValue();
        if (ret.endsWith("?")) {
            ret = ret.substring(0, ret.length() - 1);
        }
        return ret;
    }

    /**
	 * Returns the maximum allowable minutes to wait for a retry without failing.
	 */
    public int getMaxRetryMinutes() {
        return iMaxRetryMinutes;
    }

    /**
	 * Sets the maximum allowable minutes to wait for a retry without failing. If a repository returns a 503 status with a
	 *  retry-after field which specifies a retry period which exceeds this value, an error will be raised. The default value is 60
	 *  minutes.
	 */
    public void setMaxRetryMinutes(int m) {
        iMaxRetryMinutes = m;
    }

    /**
  	 * Turn a string into something that can be legally stuffed into an XML CDATA section
	 */
    protected String frndMyEncode(InputStream s) throws OAIException {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(s, "UTF-8"));
            String tmp = "";
            String ret = "";
            int idx = 0;
            while ((tmp = br.readLine()) != null) {
                while ((idx = tmp.indexOf("]]>", idx)) >= 0) {
                    tmp = tmp.substring(0, idx + 2) + "&gt;" + tmp.substring(idx + 2);
                }
                ret += tmp + "\n";
            }
            return ret;
        } catch (IOException ie) {
            throw new OAIException(OAIException.CRITICAL_ERR, ie.getMessage());
        }
    }

    protected void frndSetNamespaceNode(Element ns) {
        namespaceNode = ns;
    }

    protected Element getNamespaceNode() {
        return namespaceNode;
    }

    private String priBuildParamString(String u, String f, String s, String i, String m) {
        String param = "";
        if (u != null) {
            if (u.length() > 0) param += "&until=" + u;
        }
        if (f != null) {
            if (f.length() > 0) param += "&from=" + f;
        }
        if (s != null) {
            if (s.length() > 0) param += "&set=" + s;
        }
        if (i != null) {
            if (i.length() > 0) param += "&identifier=" + i;
        }
        if (m != null) {
            if (m.length() > 0) param += "&metadataPrefix=" + m;
        }
        return param;
    }

    /**
  	 * Purpose: Check that the baseURL has been set and throw an exception if not
	 */
    private void priCheckBaseURL() throws OAIException {
        if (strBaseURL == null || strBaseURL.length() == 0) {
            throw new OAIException(OAIException.NO_BASE_URL_ERR, "No BaseURL");
        }
    }

    /**
	 * Returns the first repository description returned by the Identify request as an XML node;
	 *  if there is no descriptin an empty string is returned.
	 */
    public Node getDescription() throws OAIException {
        return getDescription(0);
    }

    /**
	 * Returns one of the repository descriptions returned by the Identify request as an XML node; the index
	 *  parameter indicates which description to return: 0 to DescriptionCount-1; if there is no descriptin an empty string is
	 *  returned.
	 */
    public Node getDescription(int i) throws OAIException {
        Node ret = null;
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        if ((ixmlDescriptions.getLength() > 0) && (i < ixmlDescriptions.getLength())) {
            ret = ixmlDescriptions.item(i);
        }
        return ret;
    }

    /**
  	 * Returns the number of  descriptions returned by the Identify request.
	 */
    public int getDescriptionCount() {
        return ixmlDescriptions.getLength();
    }

    /**
  	 * Returns an OAIRecord object for the given OAI Identifier with default metadataPrefix as oai_dc.
	 */
    public OAIRecord getRecord(String identifier) throws OAIException {
        return getRecord(identifier, "oai_dc");
    }

    /**
  	 * Returns an OAIRecord object for the given OAI Identifier and the metadataPrefix.
	 */
    public OAIRecord getRecord(String identifier, String metadataPrefix) throws OAIException {
        PrefixResolverDefault prefixResolver;
        XPath xpath;
        XPathContext xpathSupport;
        int ctxtNode;
        XObject list;
        Node node;
        OAIRecord rec = new OAIRecord();
        priCheckBaseURL();
        String params = priBuildParamString("", "", "", identifier, metadataPrefix);
        try {
            URL url = new URL(strBaseURL + "?verb=GetRecord" + params);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http = frndTrySend(http);
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            if (validation == VALIDATION_VERY_STRICT) {
                docFactory.setValidating(true);
            } else {
                docFactory.setValidating(false);
            }
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document xml = null;
            try {
                xml = docBuilder.parse(http.getInputStream());
                rec.frndSetValid(true);
            } catch (IllegalArgumentException iae) {
                throw new OAIException(OAIException.CRITICAL_ERR, iae.getMessage());
            } catch (SAXException se) {
                if (validation != VALIDATION_LOOSE) {
                    throw new OAIException(OAIException.XML_PARSE_ERR, se.getMessage());
                } else {
                    try {
                        url = new URL(strBaseURL + "?verb=GetRecord" + params);
                        http.disconnect();
                        http = (HttpURLConnection) url.openConnection();
                        http = frndTrySend(http);
                        xml = docBuilder.parse(priCreateDummyGetRecord(identifier, http.getInputStream()));
                        rec.frndSetValid(false);
                    } catch (SAXException se2) {
                        throw new OAIException(OAIException.XML_PARSE_ERR, se2.getMessage());
                    }
                }
            }
            try {
                namespaceNode = xml.createElement("GetRecord");
                namespaceNode.setAttribute("xmlns:oai", XMLNS_OAI + "GetRecord");
                namespaceNode.setAttribute("xmlns:dc", XMLNS_DC);
                prefixResolver = new PrefixResolverDefault(namespaceNode);
                xpath = new XPath("/oai:GetRecord/oai:record", null, prefixResolver, XPath.SELECT, null);
                xpathSupport = new XPathContext();
                ctxtNode = xpathSupport.getDTMHandleFromNode(xml);
                list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                node = list.nodeset().nextNode();
                if (node == null) {
                    namespaceNode.setAttribute("xmlns:oai", XMLNS_OAI_2_0);
                    prefixResolver = new PrefixResolverDefault(namespaceNode);
                    xpath = new XPath("/oai:OAI-PMH/oai:GetRecord/oai:record", null, prefixResolver, XPath.SELECT, null);
                    list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                    node = list.nodeset().nextNode();
                    if (node == null) {
                        namespaceNode.setAttribute("xmlns:oai", XMLNS_OAI_1_0 + "GetRecord");
                        prefixResolver = new PrefixResolverDefault(namespaceNode);
                        xpath = new XPath("/oai:GetRecord/oai:record", null, prefixResolver, XPath.SELECT, null);
                        list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                        node = list.nodeset().nextNode();
                    } else {
                        xpath = new XPath("oai:OAI-PMH/oai:error", null, prefixResolver, XPath.SELECT, null);
                        list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                        ixmlErrors = list.nodelist();
                        if (ixmlErrors.getLength() > 0) {
                            strProtocolVersion = "2";
                            throw new OAIException(OAIException.OAI_ERR, getLastOAIError().getCode() + ": " + getLastOAIError().getReason());
                        }
                    }
                }
                if (node != null) {
                    rec.frndSetRepository(this);
                    rec.frndSetMetadataPrefix(metadataPrefix);
                    rec.frndSetIdOnly(false);
                    ctxtNode = xpathSupport.getDTMHandleFromNode(node);
                    xpath = new XPath("//oai:header/oai:identifier", null, prefixResolver, XPath.SELECT, null);
                    list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                    rec.frndSetIdentifier(list.nodeset().nextNode().getFirstChild().getNodeValue());
                    xpath = new XPath("//oai:header/oai:datestamp", null, prefixResolver, XPath.SELECT, null);
                    list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                    rec.frndSetDatestamp(list.nodeset().nextNode().getFirstChild().getNodeValue());
                    rec.frndSetRecord(node);
                    NamedNodeMap nmap = node.getAttributes();
                    if (nmap != null) {
                        if (nmap.getNamedItem("status") != null) {
                            rec.frndSetStatus(nmap.getNamedItem("status").getFirstChild().getNodeValue());
                        }
                    }
                } else {
                    rec = null;
                }
                xpath = new XPath("//oai:responseDate", null, prefixResolver, XPath.SELECT, null);
                list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                node = list.nodeset().nextNode();
                if (node != null) {
                    strResponseDate = node.getFirstChild().getNodeValue();
                } else {
                    if (validation == VALIDATION_LOOSE) {
                        strResponseDate = "";
                    } else {
                        throw new OAIException(OAIException.INVALID_RESPONSE_ERR, "GetRecord missing responseDate");
                    }
                }
                xpath = new XPath("//oai:requestURL | //oai:request", null, prefixResolver, XPath.SELECT, null);
                list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                node = list.nodeset().nextNode();
                if (node != null) {
                    ixmlRequest = node;
                } else {
                    if (validation == VALIDATION_LOOSE) {
                        ixmlRequest = null;
                    } else {
                        throw new OAIException(OAIException.INVALID_RESPONSE_ERR, "GetRecord missing requestURL");
                    }
                }
                xpath = null;
                prefixResolver = null;
                xpathSupport = null;
                list = null;
            } catch (TransformerException te) {
                throw new OAIException(OAIException.CRITICAL_ERR, te.getMessage());
            }
            url = null;
            docFactory = null;
            docBuilder = null;
        } catch (MalformedURLException mue) {
            throw new OAIException(OAIException.CRITICAL_ERR, mue.getMessage());
        } catch (FactoryConfigurationError fce) {
            throw new OAIException(OAIException.CRITICAL_ERR, fce.getMessage());
        } catch (ParserConfigurationException pce) {
            throw new OAIException(OAIException.CRITICAL_ERR, pce.getMessage());
        } catch (IOException ie) {
            throw new OAIException(OAIException.CRITICAL_ERR, ie.getMessage());
        }
        return rec;
    }

    /**
	 * Sends an Identify request to a repository with the baseURL previously set; if the Identify request is successful, the
	 *  RepositoryName will be returned.
	 */
    public String identify() throws OAIException {
        return identify(strBaseURL);
    }

    /**
	 * Sends an Identify request to a repository; the url parameter becomes the new BaseURL for this object;
	 *  if the Identify request is successful, the RepositoryName will be returned.
	 */
    public String identify(String baseURL) throws OAIException {
        PrefixResolverDefault prefixResolver;
        XPath xpath;
        XPathContext xpathSupport;
        int ctxtNode;
        XObject list;
        Node node;
        boolean v2 = false;
        priCheckBaseURL();
        try {
            URL url = new URL(baseURL + "?verb=Identify");
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http = frndTrySend(http);
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            if (validation == VALIDATION_VERY_STRICT) {
                docFactory.setValidating(true);
            } else {
                docFactory.setValidating(false);
            }
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document xml = null;
            try {
                xml = docBuilder.parse(http.getInputStream());
            } catch (IllegalArgumentException iae) {
                throw new OAIException(OAIException.CRITICAL_ERR, iae.getMessage());
            } catch (SAXException se) {
                if (validation != VALIDATION_LOOSE) {
                    throw new OAIException(OAIException.XML_PARSE_ERR, se.getMessage());
                } else {
                    try {
                        url = new URL(baseURL + "?verb=Identify");
                        http.disconnect();
                        http = (HttpURLConnection) url.openConnection();
                        http = frndTrySend(http);
                        xml = docBuilder.parse(priCreateDummyIdentify(http.getInputStream()));
                    } catch (SAXException se2) {
                        throw new OAIException(OAIException.XML_PARSE_ERR, se2.getMessage());
                    }
                }
            }
            try {
                descrNamespaceNode = xml.createElement("Identify");
                descrNamespaceNode.setAttribute("xmlns:oai_id", XMLNS_OAI + "Identify");
                descrNamespaceNode.setAttribute("xmlns:id", XMLNS_ID);
                descrNamespaceNode.setAttribute("xmlns:epr", XMLNS_EPR);
                prefixResolver = new PrefixResolverDefault(descrNamespaceNode);
                xpathSupport = new XPathContext();
                ctxtNode = xpathSupport.getDTMHandleFromNode(xml);
                xpath = new XPath("/oai_id:Identify", null, prefixResolver, XPath.SELECT, null);
                list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                node = list.nodeset().nextNode();
                if (node == null) {
                    descrNamespaceNode.setAttribute("xmlns:oai_id", XMLNS_OAI_2_0);
                    descrNamespaceNode.setAttribute("xmlns:id", XMLNS_ID_2_0);
                    descrNamespaceNode.setAttribute("xmlns:epr", XMLNS_EPR);
                    prefixResolver = new PrefixResolverDefault(descrNamespaceNode);
                    xpath = new XPath("/oai_id:OAI-PMH", null, prefixResolver, XPath.SELECT, null);
                    list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                    node = list.nodeset().nextNode();
                    if (node == null) {
                        descrNamespaceNode.setAttribute("xmlns:oai_id", XMLNS_OAI_1_0 + "Identify");
                        descrNamespaceNode.setAttribute("xmlns:id", XMLNS_ID_1_0);
                        descrNamespaceNode.setAttribute("xmlns:epr", XMLNS_EPR_1_0);
                        prefixResolver = new PrefixResolverDefault(descrNamespaceNode);
                    } else {
                        xpath = new XPath("oai_id:OAI-PMH/oai_id:error", null, prefixResolver, XPath.SELECT, null);
                        list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                        ixmlErrors = list.nodelist();
                        if (getLastOAIErrorCount() > 0) {
                            strProtocolVersion = "2";
                            throw new OAIException(OAIException.OAI_ERR, getLastOAIError().getCode() + ": " + getLastOAIError().getReason());
                        }
                        v2 = true;
                    }
                }
                xpath = new XPath("//oai_id:repositoryName", null, prefixResolver, XPath.SELECT, null);
                list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                node = list.nodeset().nextNode();
                if (node != null) {
                    strRepositoryName = node.getFirstChild().getNodeValue();
                } else {
                    if (validation == VALIDATION_LOOSE) {
                        strRepositoryName = "UNKNOWN";
                    } else {
                        throw new OAIException(OAIException.INVALID_RESPONSE_ERR, "Identify missing repositoryName");
                    }
                }
                xpath = new XPath("//oai_id:baseURL", null, prefixResolver, XPath.SELECT, null);
                list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                node = list.nodeset().nextNode();
                if (node != null) {
                    strBaseURL = node.getFirstChild().getNodeValue();
                } else {
                    if (validation != VALIDATION_LOOSE) {
                        throw new OAIException(OAIException.INVALID_RESPONSE_ERR, "Identify missing baseURL");
                    }
                }
                xpath = new XPath("//oai_id:protocolVersion", null, prefixResolver, XPath.SELECT, null);
                list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                node = list.nodeset().nextNode();
                if (node != null) {
                    strProtocolVersion = node.getFirstChild().getNodeValue();
                } else {
                    if (validation == VALIDATION_LOOSE) {
                        strProtocolVersion = "UNKNOWN";
                    } else {
                        throw new OAIException(OAIException.INVALID_RESPONSE_ERR, "Identify missing protocolVersion");
                    }
                }
                xpath = new XPath("//oai_id:adminEmail", null, prefixResolver, XPath.SELECT, null);
                list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                NodeList nl = list.nodelist();
                if (nl.getLength() > 0) {
                    strAdminEmail = new String[nl.getLength()];
                    for (int i = 0; i < nl.getLength(); i++) {
                        strAdminEmail[i] = nl.item(i).getFirstChild().getNodeValue();
                    }
                } else {
                    if (validation == VALIDATION_LOOSE) {
                        strAdminEmail = new String[1];
                        strAdminEmail[0] = "mailto:UNKNOWN";
                    } else {
                        throw new OAIException(OAIException.INVALID_RESPONSE_ERR, "Identify missing adminEmail");
                    }
                }
                if (v2) {
                    xpath = new XPath("//oai_id:earliestDatestamp", null, prefixResolver, XPath.SELECT, null);
                    list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                    node = list.nodeset().nextNode();
                    if (node != null) {
                        strEarliestDatestamp = node.getFirstChild().getNodeValue();
                    } else {
                        if (validation == VALIDATION_LOOSE) {
                            strEarliestDatestamp = "UNKNOWN";
                        } else {
                            throw new OAIException(OAIException.INVALID_RESPONSE_ERR, "Identify missing earliestDatestamp");
                        }
                    }
                    xpath = new XPath("//oai_id:deletedRecord", null, prefixResolver, XPath.SELECT, null);
                    list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                    node = list.nodeset().nextNode();
                    if (node != null) {
                        strDeletedRecord = node.getFirstChild().getNodeValue();
                    } else {
                        if (validation == VALIDATION_LOOSE) {
                            strDeletedRecord = "UNKNOWN";
                        } else {
                            throw new OAIException(OAIException.INVALID_RESPONSE_ERR, "Identify missing deletedRecordp");
                        }
                    }
                    xpath = new XPath("//oai_id:granularity", null, prefixResolver, XPath.SELECT, null);
                    list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                    node = list.nodeset().nextNode();
                    if (node != null) {
                        strGranularity = node.getFirstChild().getNodeValue();
                    } else {
                        if (validation == VALIDATION_LOOSE) {
                            strGranularity = "UNKNOWN";
                        } else {
                            throw new OAIException(OAIException.INVALID_RESPONSE_ERR, "Identify missing granularity");
                        }
                    }
                    xpath = new XPath("//oai_id:compression", null, prefixResolver, XPath.SELECT, null);
                    list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                    nl = list.nodelist();
                    if (nl.getLength() > 0) {
                        strCompression = new String[nl.getLength()];
                        for (int i = 0; i < nl.getLength(); i++) {
                            strCompression[i] = nl.item(i).getFirstChild().getNodeValue();
                        }
                    }
                }
                xpath = new XPath("//oai_id:description", null, prefixResolver, XPath.SELECT, null);
                list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                ixmlDescriptions = list.nodelist();
                xpath = new XPath("//oai_id:responseDate", null, prefixResolver, XPath.SELECT, null);
                list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                node = list.nodeset().nextNode();
                if (node != null) {
                    strResponseDate = node.getFirstChild().getNodeValue();
                } else {
                    if (validation == VALIDATION_LOOSE) {
                        strResponseDate = "";
                    } else {
                        throw new OAIException(OAIException.INVALID_RESPONSE_ERR, "GetRecord missing responseDate");
                    }
                }
                xpath = new XPath("//oai_id:requestURL | //oai_id:request", null, prefixResolver, XPath.SELECT, null);
                list = xpath.execute(xpathSupport, ctxtNode, prefixResolver);
                node = list.nodeset().nextNode();
                if (node != null) {
                    ixmlRequest = node;
                } else {
                    if (validation == VALIDATION_LOOSE) {
                        ixmlRequest = null;
                    } else {
                        throw new OAIException(OAIException.INVALID_RESPONSE_ERR, "GetRecord missing requestURL");
                    }
                }
                state = STATE_IDENTIFIED;
                xpath = null;
                prefixResolver = null;
                xpathSupport = null;
                list = null;
            } catch (TransformerException te) {
                throw new OAIException(OAIException.CRITICAL_ERR, te.getMessage());
            }
            url = null;
            docFactory = null;
            docBuilder = null;
        } catch (IOException ie) {
            throw new OAIException(OAIException.CRITICAL_ERR, ie.getMessage());
        } catch (FactoryConfigurationError fce) {
            throw new OAIException(OAIException.CRITICAL_ERR, fce.getMessage());
        } catch (ParserConfigurationException pce) {
            throw new OAIException(OAIException.CRITICAL_ERR, pce.getMessage());
        }
        return strRepositoryName;
    }

    /**
	 * Returns an OAIRecordList object containing the records returned by the repository.
	 */
    public OAIRecordList listIdentifiers() throws OAIException {
        return listIdentifiers("", "", "", "oai_dc");
    }

    /**
	 * Returns an OAIRecordList object containing the records returned by the repository for the given untild.
	 */
    public OAIRecordList listIdentifiers(String untild) throws OAIException {
        return listIdentifiers(untild, "", "", "oai_dc");
    }

    /**
	 * Returns an OAIRecordList object containing the records returned by the repository for the given untild and fromd.
	 */
    public OAIRecordList listIdentifiers(String untild, String fromd) throws OAIException {
        return listIdentifiers(untild, fromd, "", "oai_dc");
    }

    /**
	 * Returns an OAIRecordList object containing the records returned by the repository for the given untild, fromd, and SetSpec.
	 */
    public OAIRecordList listIdentifiers(String untild, String fromd, String setSpec) throws OAIException {
        return listIdentifiers(untild, fromd, setSpec, "oai_dc");
    }

    /**
	 * Returns an OAIRecordList object containing the records returned by the repository for the given untild, fromd, and SetSpec.
	 */
    public OAIRecordList listIdentifiers(String untild, String fromd, String setSpec, String metadataPrefix) throws OAIException {
        priCheckBaseURL();
        String prefix = metadataPrefix;
        if (getProtocolMajorVersion() > 1) {
            if (metadataPrefix.length() == 0) {
                prefix = "oai_dc";
            }
        }
        String params = priBuildParamString(untild, fromd, setSpec, "", prefix);
        OAIResumptionStream rs = new OAIResumptionStream(this, strBaseURL, "ListIdentifiers", params);
        OAIRecordList sets = new OAIRecordList();
        sets.frndSetOAIResumptionStream(rs);
        return sets;
    }

    /**
	 * Returns an OAIRecordList object containing the records returned by the repository.
	 */
    public OAIRecordList listRecords() throws OAIException {
        return listRecords("oai_dc", "", "", "");
    }

    /**
	 * Returns an OAIRecordList object containing the records returned by the repository for the given metadataPrefix.
	 */
    public OAIRecordList listRecords(String metadataPrefix) throws OAIException {
        return listRecords(metadataPrefix, "", "", "");
    }

    /**
	 * Returns an OAIRecordList object containing the records returned by the repository for the given metadataPrefix and untild.
	 */
    public OAIRecordList listRecords(String metadataPrefix, String untild) throws OAIException {
        return listRecords(metadataPrefix, untild, "", "");
    }

    /**
	 * Returns an OAIRecordList object containing the records returned by the repository for the given metadataPrefix, untild,
	 *  and fromd.
	 */
    public OAIRecordList listRecords(String metadataPrefix, String untild, String fromd) throws OAIException {
        return listRecords(metadataPrefix, untild, fromd, "");
    }

    /**
	 * Returns an OAIRecordList object containing the records returned by the repository for the given metadataPrefix, untild,
	 *  fromd, and SetSpec.
	 */
    public OAIRecordList listRecords(String metadataPrefix, String untild, String fromd, String setSpec) throws OAIException {
        priCheckBaseURL();
        String prefix = metadataPrefix;
        if (metadataPrefix.length() == 0) {
            prefix = "oai_dc";
        } else {
            prefix = metadataPrefix;
        }
        String params = priBuildParamString(untild, fromd, setSpec, "", prefix);
        OAIResumptionStream rs = new OAIResumptionStream(this, strBaseURL, "ListRecords", params);
        OAIRecordList sets = new OAIRecordList();
        sets.frndSetOAIResumptionStream(rs);
        sets.frndSetMetadataPrefix(metadataPrefix);
        return sets;
    }

    /**
	 * If one of the descriptions returned by the Identify request is an oai-identifier, this parameter will contain the registered
	 *  identifier of the repository. If there is no oai-identifier description, then an empty string will be returned, or if
	 *  Validation is Very Strict an error.
	 */
    public String getRepositoryIdentifier() throws OAIException {
        String ret = "";
        Node node;
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        if (!usesOAIIdentifier() && strRepositoryId.length() == 0 && validation == VALIDATION_VERY_STRICT) {
            throw new OAIException(OAIException.NO_OAI_IDENTIFIER_ERR, "The RepositoryIdentifier is unknown");
        } else if (!usesOAIIdentifier() && strRepositoryId.length() == 0) {
            return ret;
        }
        try {
            for (int i = 0; i < ixmlDescriptions.getLength(); i++) {
                node = XPathAPI.selectSingleNode(ixmlDescriptions.item(i), "//oai_id:description/id:oai-identifier/id:repositoryIdentifier", descrNamespaceNode);
                if (node != null) {
                    ret = node.getFirstChild().getNodeValue();
                }
            }
        } catch (TransformerException te) {
            throw new OAIException(OAIException.CRITICAL_ERR, te.getMessage());
        }
        return ret;
    }

    /**
	 * If one of the descriptions returned by the Identify request is an oai-identifier, this method will return the oai-identifier
	 *  description as an XML node. Nothing is returned if there is no OAI-Identifier description.
	 */
    public Node getOAIIdentifierDescription() throws OAIException {
        Node node = null;
        Node ret = null;
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        if (!usesOAIIdentifier()) {
            throw new OAIException(OAIException.NO_OAI_IDENTIFIER_ERR, "The RepositoryIdentifier is unknown");
        }
        for (int i = 0; i < ixmlDescriptions.getLength(); i++) {
            node = ixmlDescriptions.item(i);
            if (node.getNamespaceURI().equals(XMLNS_ID) || node.getNamespaceURI().equals(XMLNS_ID_1_0) || node.getNamespaceURI().equals("XMLNS_ID_1_0_aps")) {
                ret = node;
                break;
            }
        }
        return ret;
    }

    /**
	 * If one of the descriptions returned by the Identify request is an eprints, this method will return the eprints description
	 * as an XML node. Nothing is returned if there is no E-Prints description.
	 */
    public Node getEPrintsDescription() throws OAIException {
        Node ret = null;
        Node node;
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        if (!usesOAIIdentifier()) {
            throw new OAIException(OAIException.NO_OAI_IDENTIFIER_ERR, "The RepositoryIdentifier is unknown");
        }
        for (int i = 0; i < ixmlDescriptions.getLength(); i++) {
            node = ixmlDescriptions.item(i);
            if (node.getNamespaceURI().equals(XMLNS_EPR) || node.getNamespaceURI().equals(XMLNS_EPR_1_0)) {
                ret = node;
                break;
            }
        }
        return ret;
    }

    /**
	 * Sets the number of times to retry if a HTTP status of 503 is returned by the repository; defaults to 5
	 */
    public void setRetryLimit(int rl) {
        iRetryLimit = rl;
    }

    /**
  	 * Returns the number of times to retry if given a 503 before giving up.
	 */
    public int getRetryLimit() {
        return iRetryLimit;
    }

    /**
	 * If one of the descriptions returned by the Identify request is an oai-identifier, this parameter will contain the sample OAI
	 *  identifier contained therein. If there is no oai-identifier description, then an empty string will be returned, or if
	 *  Validation is Very Strict an error.
	 */
    public String getSampleIdentifier() throws OAIException {
        String ret = "";
        Node node;
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        if (!usesOAIIdentifier() && validation == VALIDATION_VERY_STRICT) {
            throw new OAIException(OAIException.NO_OAI_IDENTIFIER_ERR, "The RepositoryIdentifier is unknown");
        } else if (!usesOAIIdentifier()) {
            return ret;
        }
        try {
            for (int i = 0; i < ixmlDescriptions.getLength(); i++) {
                node = XPathAPI.selectSingleNode(ixmlDescriptions.item(i), "//oai_id:description/id:oai-identifier/id:sampleIdentifier", descrNamespaceNode);
                if (ret != null) {
                    ret = node.getFirstChild().getNodeValue();
                }
            }
        } catch (TransformerException te) {
            throw new OAIException(OAIException.CRITICAL_ERR, te.getMessage());
        }
        return ret;
    }

    /**
	 * Returns the name of the repository
	 */
    public String getRepositoryName() throws OAIException {
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        return strRepositoryName;
    }

    /**
	 * Returns the email address of the repository administrator.
	 */
    public String getAdminEmail() throws OAIException {
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        return getAdminEmail(0);
    }

    /**
	 * Returns the email address of the repository administrator.
	 */
    public String getAdminEmail(int i) throws OAIException {
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        if (getAdminEmailCount() > 0 && i < getAdminEmailCount()) {
            return strAdminEmail[i];
        } else {
            return "";
        }
    }

    /**
	 * Returns the compression supported by the repository.
	 */
    public String getCompression() throws OAIException {
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        return getCompression(0);
    }

    /**
	 * Returns the compression supported by the repository.
	 */
    public String getCompression(int i) throws OAIException {
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        if (getCompressionCount() > 0 && i < getCompressionCount()) {
            return strCompression[i];
        } else {
            return "";
        }
    }

    /**
	 * Returns the major version number of the protocol spec supported by the repository currently either 1 or 2.
	 */
    public int getProtocolMajorVersion() throws OAIException {
        int ver = 0;
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        try {
            ver = Integer.parseInt(strProtocolVersion.trim().substring(0, 1));
        } catch (NumberFormatException ne) {
            ver = 0;
        }
        return ver;
    }

    /**
	 * Returns the version of the protocol spec supported by the repository.
	 */
    public String getProtocolVersion() throws OAIException {
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        return strProtocolVersion;
    }

    /**
	 * Return the complete request url returned by the most recent response.
	 * This includes the baseURL and all of the parameters shown as a concatenated query string.
	 */
    public String getRequestURL() {
        String ret = ixmlRequest.getFirstChild().getNodeValue();
        NamedNodeMap map = ixmlRequest.getAttributes();
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
        return ret;
    }

    /**
	 * Returns the value to be used in the User-Agent field of the HTTP request header.
	 */
    public String getUserAgent() {
        return strUserAgent;
    }

    /**
 	 * Sets the value to be used in the User-Agent field of the HTTP request header. This is usually the name of the harvester.
	 *  Defaults to 'OAIHarvester University of Illinois Library'.
	 */
    public void setUserAgent(String ua) {
        strUserAgent = ua;
    }

    /**
	 * Returns the value to be used in the From field of the HTTP request header.
	 */
    public String getFrom() {
        return strFrom;
    }

    /**
	 * Sets the value to be used in the From field of the HTTP request header. This is usually the email address of the person
	 *  running the harvester. Defaults to 'ytseng1@uiuc.edu'. This property should be reset to email address of the person
	 *  responsible for harvester.
	 */
    public void setFrom(String f) {
        strFrom = f;
    }

    /**
  	 * Purpose: Attempt to send the request to the repository, honoring
  	 *          503 Retry statuses
  	 *
  	 * Inputs:  h the HTTP object to use for the sending
  	 *
  	 * NOTE: Not sure if the http object does redirects (302) or not; may have
  	 *       to do these manually also
  	 *
	 */
    protected HttpURLConnection frndTrySend(HttpURLConnection h) throws OAIException {
        HttpURLConnection http = h;
        boolean done = false;
        GregorianCalendar sendTime = new GregorianCalendar();
        GregorianCalendar testTime = new GregorianCalendar();
        GregorianCalendar retryTime = null;
        String retryAfter;
        int retryCount = 0;
        do {
            try {
                http.setRequestProperty("User-Agent", strUserAgent);
                http.setRequestProperty("From", strFrom);
                if (strUser != null && strUser.length() > 0) {
                    byte[] encodedPassword = (strUser + ":" + strPassword).getBytes();
                    BASE64Encoder encoder = new BASE64Encoder();
                    http.setRequestProperty("Authorization", "Basic " + encoder.encode(encodedPassword));
                }
                sendTime.setTime(new Date());
                http.connect();
                if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    done = true;
                } else if (http.getResponseCode() == HttpURLConnection.HTTP_UNAVAILABLE) {
                    retryCount++;
                    if (retryCount > iRetryLimit) {
                        throw new OAIException(OAIException.RETRY_LIMIT_ERR, "The RetryLimit " + iRetryLimit + " has been exceeded");
                    } else {
                        retryAfter = http.getHeaderField("Retry-After");
                        if (retryAfter == null) {
                            throw new OAIException(OAIException.RETRY_AFTER_ERR, "No Retry-After header");
                        } else {
                            try {
                                int sec = Integer.parseInt(retryAfter);
                                sendTime.add(Calendar.SECOND, sec);
                                retryTime = sendTime;
                            } catch (NumberFormatException ne) {
                                try {
                                    Date retryDate = DateFormat.getDateInstance().parse(retryAfter);
                                    retryTime = new GregorianCalendar();
                                    retryTime.setTime(retryDate);
                                } catch (ParseException pe) {
                                    throw new OAIException(OAIException.CRITICAL_ERR, pe.getMessage());
                                }
                            }
                            if (retryTime != null) {
                                testTime.setTime(new Date());
                                testTime.add(Calendar.MINUTE, iMaxRetryMinutes);
                                if (retryTime.getTime().before(testTime.getTime())) {
                                    try {
                                        while (retryTime.getTime().after(new Date())) {
                                            Thread.sleep(60000);
                                        }
                                        URL url = http.getURL();
                                        http.disconnect();
                                        http = (HttpURLConnection) url.openConnection();
                                    } catch (InterruptedException ie) {
                                        throw new OAIException(OAIException.CRITICAL_ERR, ie.getMessage());
                                    }
                                } else {
                                    throw new OAIException(OAIException.RETRY_AFTER_ERR, "Retry time(" + retryAfter + " sec) is too long");
                                }
                            } else {
                                throw new OAIException(OAIException.RETRY_AFTER_ERR, retryAfter + "is not a valid Retry-After header");
                            }
                        }
                    }
                } else if (http.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                    throw new OAIException(OAIException.CRITICAL_ERR, http.getResponseMessage());
                } else {
                    retryCount++;
                    if (retryCount > iRetryLimit) {
                        throw new OAIException(OAIException.RETRY_LIMIT_ERR, "The RetryLimit " + iRetryLimit + " has been exceeded");
                    } else {
                        int sec = 10 * ((int) Math.exp(retryCount));
                        sendTime.add(Calendar.SECOND, sec);
                        retryTime = sendTime;
                        try {
                            while (retryTime.getTime().after(new Date())) {
                                Thread.sleep(sec * 1000);
                            }
                            URL url = http.getURL();
                            http.disconnect();
                            http = (HttpURLConnection) url.openConnection();
                        } catch (InterruptedException ie) {
                            throw new OAIException(OAIException.CRITICAL_ERR, ie.getMessage());
                        }
                    }
                }
            } catch (IOException ie) {
                throw new OAIException(OAIException.CRITICAL_ERR, ie.getMessage());
            }
        } while (!done);
        return http;
    }

    /**
	 * Returns true if the repository uses OAIIdentifier descriptions
  	 *  in its Identify request; else false.
	 */
    public boolean usesOAIIdentifier() throws OAIException {
        boolean ret = false;
        Node node;
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        try {
            for (int i = 0; i < ixmlDescriptions.getLength(); i++) {
                node = XPathAPI.selectSingleNode(ixmlDescriptions.item(i), "//oai_id:description/*", descrNamespaceNode);
                if (node != null) {
                    if (node.getNamespaceURI().equals(XMLNS_ID) || node.getNamespaceURI().equals(XMLNS_ID_1_0) || node.getNamespaceURI().equals("XMLNS_ID_1_0_aps")) {
                        ret = true;
                        break;
                    }
                }
            }
        } catch (TransformerException te) {
            throw new OAIException(OAIException.CRITICAL_ERR, te.getMessage());
        }
        return ret;
    }

    /**
  	 * Returns true if the repository uses EPrints descriptions
  	 *  in its Identify request; else false.
	 */
    public boolean usesEPrints() throws OAIException {
        boolean ret = false;
        Node node;
        if (state == STATE_UNIDENTIFIED) {
            identify();
        }
        try {
            for (int i = 0; i < ixmlDescriptions.getLength(); i++) {
                node = XPathAPI.selectSingleNode(ixmlDescriptions.item(i), "//oai_id:description/*", descrNamespaceNode);
                if (node != null) {
                    if (node.getNamespaceURI().equals(XMLNS_EPR) || node.getNamespaceURI().equals(XMLNS_EPR_1_0)) {
                        ret = true;
                        break;
                    }
                }
            }
        } catch (TransformerException te) {
            throw new OAIException(OAIException.CRITICAL_ERR, te.getMessage());
        }
        return ret;
    }

    /**
  	 * Returns the type of validation to perform on the OAI response.
	 */
    public int getValidation() {
        return validation;
    }

    /**
  	 * Sets the type of validation to perform on the OAI response.
  	 * VALIDATION_STRICT is the default.
	 */
    public void setValidation(int v) {
        validation = v;
    }

    /**
	 * Returns an OAIMetadataFormatList object containing the metadata formats supported by the repository..
	 */
    public OAIMetadataFormatList listMetadataFormats() throws OAIException {
        return listMetadataFormats("");
    }

    /**
	 * Returns an OAIMetadataFormatList object containing the metadata formats supported by a specific record.
	 */
    public OAIMetadataFormatList listMetadataFormats(String identifier) throws OAIException {
        OAIMetadataFormatList sets = new OAIMetadataFormatList();
        OAIResumptionStream rs;
        String params;
        priCheckBaseURL();
        params = priBuildParamString("", "", "", identifier, "");
        rs = new OAIResumptionStream(this, strBaseURL, "ListMetadataFormats", params);
        sets.frndSetOAIResumptionStream(rs);
        return sets;
    }

    /**
  	 * Returns an OAISetList object containing the sets returned by the repository.
	 */
    public OAISetList listSets() throws OAIException {
        OAISetList sets = new OAISetList();
        OAIResumptionStream rs;
        priCheckBaseURL();
        rs = new OAIResumptionStream(this, strBaseURL, "ListSets");
        sets.frndSetOAIResumptionStream(rs);
        return sets;
    }

    private String strRepositoryId;

    private String strRepositoryName;

    private String strBaseURL;

    private String[] strAdminEmail;

    private String[] strCompression;

    private String strEarliestDatestamp;

    private String strDeletedRecord;

    private String strGranularity;

    private String strProtocolVersion;

    private String strUserAgent;

    private String strFrom;

    private String strUser;

    private String strPassword;

    private String strResponseDate;

    private String sRawResponse;

    private NodeList ixmlDescriptions;

    private NodeList ixmlErrors;

    private Node ixmlRequest;

    ;

    private Element descrNamespaceNode;

    private Element namespaceNode;

    private int validation;

    private int state;

    private int iRetryLimit;

    private int iMaxRetryMinutes;

    /**
	 * The repository is not identified yet.
	 */
    public final int STATE_UNIDENTIFIED = 0;

    /**
	 * The repository has been identified.
	 */
    public final int STATE_IDENTIFIED = 1;

    /**
  	 * This is the default type.  The only check which is initially performed is
  	 * if the XML is well-formed.  If it is not well-formed, including invalid characters,
  	 * an error will be generated and processing will stop. However, the program may still
  	 * generate errors if the XML does not contain certain expected elements.
  	 */
    public static final int VALIDATION_STRICT = 0;

    /**
  	 * This will cause the XML responses to be initially validated
  	 * against the XML schemas, as indicated by the xsi:schemaLocation attributes.
  	 * If the validation passes, this program should encounter no further errors.
  	 */
    public static final int VALIDATION_VERY_STRICT = 1;

    /**
  	 * This will attempt to continue processing even if some non-well-formed
  	 * XML is encountered.  In most case a dummy record will be created which contains the
  	 * invalid record in some fashion.  If there is a resumptionToken, basic string parsing
  	 * techniques will be used to get the value, so it can be used to continue with the
  	 * next chunk of data.
	 */
    public static final int VALIDATION_LOOSE = 2;

    /**
	 * namespace URIs for metadata formats
	 */
    public static String XMLNS_DC = "http://purl.org/dc/elements/1.1/";

    public static String XMLNS_RFC1807 = "http://info.internet.isi.edu:80/in-notes/rfc/files/rfc1807.txt";

    public static String XMLNS_OAI_MARC = "http://www.openarchives.org/OAI/1.1/oai_marc";

    /**
	 * namespaces from the current (1.1) version of the protocol
	 */
    public static String XMLNS_OAI = "http://www.openarchives.org/OAI/1.1/OAI_";

    public static String XMLNS_ID = "http://www.openarchives.org/OAI/1.1/oai-identifier";

    public static String XMLNS_EPR = "http://www.openarchives.org/OAI/1.1/eprints";

    /**
	 * namespaces from the 2.0 version of the protocol
	 */
    public static String XMLNS_OAI_2_0 = "http://www.openarchives.org/OAI/2.0/";

    public static String XMLNS_OAI_DC_2_0 = "http://www.openarchives.org/OAI/2.0/oai_dc/";

    public static String XMLNS_ID_2_0 = "http://www.openarchives.org/OAI/2.0/oai-identifier/";

    /**
	 * namespaces from the 1.0 version of the protocol
	 */
    public static String XMLNS_OAI_1_0 = "http://www.openarchives.org/OAI/1.0/OAI_";

    public static String XMLNS_ID_1_0 = "http://www.openarchives.org/OAI/oai-identifier";

    public static String XMLNS_EPR_1_0 = "http://www.openarchives.org/OAI/eprints";

    public static String XMLNS_OAI_MARC_1_0 = "http://www.openarchives.org/OAI/oai_marc";

    /**
	 * some repositories don't use the correct namespaces (American Physical Society)
	 */
    public static String XMLNS_ID_1_0_aps = "http://www.openarchives.org/OAI/oai-identifier.xsd";
}
