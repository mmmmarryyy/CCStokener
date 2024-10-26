package au.csiro.doiclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import au.csiro.doiclient.business.AndsDoiIdentity;
import au.csiro.doiclient.business.DoiDTO;
import au.csiro.doiclient.utils.DoiMetaDataGenerator;
import au.csiro.pidclient.AndsPidClient.HandleType;

/**
 * This is the main interface to the ANDS DOI Client library. It allows the caller to interact with the <a
 * href="http://www.ands.org.au/services/doi-m2m-identifiers.html">Digital Object IDentifier Service</a> provided by the
 * <a href="http://www.ands.org.au/">Australian National Data Service</a>. You will need to register with ANDS to be
 * able to use the service.
 * <p>
 * <b>Usage:</b>
 * </p>
 * <p>
 * Create a new instance of the class using either the empty constructor and then calling the four setters, or using the
 * constructors. This should be compatible with use as a singleton bean in Spring or other DI (dependency injection)
 * frameworks. Example:
 * </p>
 * <UL>
 * <li>pidServiceHost - test.ands.org.au</li>
 * <li>pidServicePort - 8443</li>
 * <li>pidServicePath - /pids</li>
 * <li>requestorIdentity.appId - 5d9a4da3580c528ba98d8e6f088dab93f680dd6b</li>
 * <li>requestorIdentity.authDomain - mycomputer.edu.au</li>
 * </UL>
 * <p>
 * This class has methods for (a)minting DOIs, (b)updating DOIs, (c) activating and deactivating DOIs.
 * 
 * </p>
 * Copyright 2012, CSIRO Australia All rights reserved.
 * 
 * @author Robert Bridle on 05/02/2010
 * @author John Page on 03/03/2012
 * @version $Revision: 146 $Date: $
 */
public class AndsDoiClient {

    /**
     * Constant that defines the logger to be used.
     */
    private static final Logger LOG = Logger.getLogger(AndsDoiClient.class.getName());

    /**
     * Constant that defines the name of properties file to be used.
     */
    private static final String PROPERTIES_FILENAME = "/ands-doi-client.properties";

    /**
     * The name of the method (RESTful web service) to call when minting a DOI.
     */
    private static String mintMethodName;

    /**
     * The name of the method (RESTful web service) to call when updating a DOI.
     */
    private static String updateMethodName;

    /**
     * The name of the method (RESTful web service) to call when requesting the metadata associated with a DOI.
     */
    private static String doiMetadataRequest;

    /**
     * The name of the method (RESTful web service) to call when activating a DOI.
     */
    private static String activateDoi;

    /**
     * The name of the method (RESTful web service) to call when deactivating a DOI.
     */
    private static String deactivateDoi;

    /**
     * The path to the DOI meta-data template.
     */
    private String metadataTemplatePath;

    /**
     * Represents the identity information of the caller.
     */
    private AndsDoiIdentity requestorIdentity;

    /**
     * The ANDS Digital Object Identifier host name.
     */
    private String doiServiceHost;

    /**
     * The ANDS Digital Object Identifier path name (web application context name).
     */
    private String doiServicePath;

    /**
     * Loads the specified properties file.
     */
    private static void loadProperties() {
        InputStream is = AndsDoiResponse.class.getResourceAsStream(PROPERTIES_FILENAME);
        Properties props = new Properties();
        try {
            props.load(is);
            mintMethodName = props.getProperty("method.mint");
            updateMethodName = props.getProperty("method.update");
            doiMetadataRequest = props.getProperty("method.doi");
            activateDoi = props.getProperty("method.activate");
            deactivateDoi = props.getProperty("method.deactivate");
        } catch (IOException e) {
            LOG.error("Could not load properties file: " + PROPERTIES_FILENAME, e);
        }
    }

    /**
     * Default constructor. You will still need to supply the configuration data via the public setters.
     */
    public AndsDoiClient() {
        this(null, null, null, null);
    }

    /**
     * @param doiServiceHost
     *            the ANDS Digital Object Identifier host name.
     * @param doiServicePath
     *            the ANDS Digital Object Identifier path name (web application context name).
     * @param appId
     *            the unique Id provided to the caller upon IP registration with the ANDS Digital Object Identifier
     *            service.
     * @param identifier
     *            the identifier or name of the repository calling the service.
     * @param authDomain
     *            the domain of the organisation calling the service.
     * @param doiDTO
     *            a DTO with the meta data info.
     * 
     */
    public AndsDoiClient(String doiServiceHost, String doiServicePath, String appId, String authDomain) {
        this(doiServiceHost, doiServicePath, new AndsDoiIdentity(appId, authDomain));
    }

    /**
     * @param doiServiceHost
     *            the ANDS Digital Object Identifier host name.
     * @param doiServicePath
     *            the ANDS Digital Object Identifier path name (web application context name).
     * @param requestorIdentity
     *            represents the identity information of the caller {@link AndsDoiIdentity}.
     */
    public AndsDoiClient(String doiServiceHost, String doiServicePath, AndsDoiIdentity requestorIdentity) {
        this.setDoiServiceHost(doiServiceHost);
        this.setDoiServicePath(doiServicePath);
        this.setRequestorIdentity(requestorIdentity);
        loadProperties();
    }

    /**
     * Responsible for the creation of a DOI.
     * <p>
     * If the value arguments are both empty, a handle with no values is created. The handle is assigned to an owner,
     * specified by the {@link AndsDoiIdentity#getAppId()} value. If the owner is not known to the handle system, an
     * owner is created from the {@link AndsDoiIdentity#getIdentifier()} and {@link AndsDoiIdentity#getIdentifier()}
     * values.
     * 
     * @param url
     *            the url pointing to the landing page of the data collection.
     * @param doiDTO
     *            doiDTO with the values for the meta-data update.
     * @return AndsDoiResponse {@link AndsDoiResponse} Response object holding the components of the response.
     * @throws IllegalStateException
     *             thrown if the parameters need to call the ANDS DOI service have not been provided.
     * @throws IllegalArgumentException
     *             thrown when method is called with invalid arguments.
     * @throws IOException
     *             thrown when attempting to read response.
     * @throws HttpException
     *             thrown when attempting to execute method call.
     */
    public AndsDoiResponse mintDOI(String url, DoiDTO doiDTO) throws IllegalStateException, IllegalArgumentException, HttpException, IOException {
        this.validateState();
        this.validateParameters(mintMethodName, url);
        String queryString = MessageFormat.format("app_id={0}&url={1}", new Object[] { getRequestorIdentity().getAppId(), url });
        return executeMethod(queryString, mintMethodName, null, null, doiDTO);
    }

    /**
     * Responsible for the updating a DOI.
     * <p>
     * DOI being updated must belong to the client requesting the update. The update service point allows clients to
     * update their DOIs in 3 ways. Clients can update the URL only, metadata only, or both the URL and metadata
     * 
     * @param doi
     *            that needs to be updated.
     * @param updatedUrl
     *            the url pointing to the landing page of the data collection.
     * @return AndsDoiResponse {@link AndsDoiResponse} Response object holding the components of the response.
     * @throws IllegalStateException
     *             thrown if the parameters need to call the ANDS DOI service have not been provided.
     * @throws IllegalArgumentException
     *             thrown when method is called with invalid arguments.
     * @throws IOException
     *             thrown when attempting to read response.
     * @throws HttpException
     *             thrown when attempting to execute method call.
     */
    public AndsDoiResponse updateDOI(String doi, String updatedUrl) throws HttpException, IOException {
        return this.updateDOI(doi, updatedUrl, null);
    }

    /**
     * Responsible for the updating a DOI.
     * <p>
     * DOI being updated must belong to the client requesting the update. The update service point allows clients to
     * update their DOIs in 3 ways. Clients can update the URL only, metadata only, or both the URL and metadata
     * 
     * @param doi
     *            that needs to be updated.
     * @param doiDTO
     *            doiDTO with the values for the meta-data update.
     * @return AndsDoiResponse {@link AndsDoiResponse} Response object holding the components of the response.
     * @throws IllegalStateException
     *             thrown if the parameters need to call the ANDS DOI service have not been provided.
     * @throws IllegalArgumentException
     *             thrown when method is called with invalid arguments.
     * @throws IOException
     *             thrown when attempting to read response.
     * @throws HttpException
     *             thrown when attempting to execute method call.
     */
    public AndsDoiResponse updateDOI(String doi, DoiDTO doiDTO) throws HttpException, IOException {
        return this.updateDOI(doi, null, doiDTO);
    }

    /**
     * Responsible for the updating a DOI.
     * <p>
     * DOI being updated must belong to the client requesting the update. The update service point allows clients to
     * update their DOIs in 3 ways. Clients can update the URL only, metadata only, or both the URL and metadata
     * 
     * @param doi
     *            that needs to be updated.
     * @param updatedUrl
     *            the url pointing to the landing page of the data collection.
     * @param doiDTO
     *            doiDTO with the values for the meta-data update.
     * @return AndsDoiResponse {@link AndsDoiResponse} Response object holding the components of the response.
     * @throws IllegalStateException
     *             thrown if the parameters need to call the ANDS DOI service have not been provided.
     * @throws IllegalArgumentException
     *             thrown when method is called with invalid arguments.
     * @throws IOException
     *             thrown when attempting to read response.
     * @throws HttpException
     *             thrown when attempting to execute method call.
     */
    public AndsDoiResponse updateDOI(String doi, String updatedUrl, DoiDTO doiDTO) throws HttpException, IOException {
        String queryString = null;
        this.validateState();
        this.validateParameters(updateMethodName, doi, updatedUrl);
        if (updatedUrl != null) {
            queryString = MessageFormat.format("app_id={0}&doi={1}&url={2}", new Object[] { getRequestorIdentity().getAppId(), doi, updatedUrl });
        } else {
            queryString = MessageFormat.format("app_id={0}&doi={1}", new Object[] { getRequestorIdentity().getAppId(), doi });
        }
        AndsDoiResponse existingMetaDataXML = requestMetaDataOfDOI(doi);
        return executeMethod(queryString, updateMethodName, updatedUrl, existingMetaDataXML.getMetaData(), doiDTO);
    }

    /**
     * Responsible for Activating a DOI.
     * <p>
     * 
     * Activates deactivated metadata associated with a given DOI. A DOI�s metadata is active by default, and can only
     * by activated if it has previously been deactivated. Activating a DOI�s metadata allows the metadata to be
     * returned by the public �Request Metadata Associated with DOI� service endpoint.
     * 
     * @param doi
     *            that needs to be activated/ deactivated.
     * @param activate
     *            boolean flag indicating whether to activate or deactivate.
     * @return AndsDoiResponse {@link AndsDoiResponse} Response object holding the components of the response.
     * @throws IllegalStateException
     *             thrown if the parameters need to call the ANDS DOI service have not been provided.
     * @throws IllegalArgumentException
     *             thrown when method is called with invalid arguments.
     * @throws IOException
     *             thrown when attempting to read response.
     * @throws HttpException
     *             thrown when attempting to execute method call.
     */
    public AndsDoiResponse activateDOI(String doi) throws HttpException, IOException {
        String methodName = activateDoi;
        this.validateParameters(methodName, doi);
        String queryString = MessageFormat.format("app_id={0}&doi={1}", new Object[] { getRequestorIdentity().getAppId(), doi });
        return executeMethod(queryString, methodName, null, null, null);
    }

    /**
     * Responsible for Deactivating a DOI.
     * <p>
     * Deactivates metadata associated with a DOI. A DOI�s metadata is active by default. Deactivating a DOI�s metadata
     * prevents the metadata from being returned by the public �Request Metadata Associated with DOI� service endpoint.
     * 
     * 
     * @param doi
     *            that needs to be activated/ deactivated.
     * @param activate
     *            boolean flag indicating whether to activate or deactivate.
     * @return AndsDoiResponse {@link AndsDoiResponse} Response object holding the components of the response.
     * @throws IllegalStateException
     *             thrown if the parameters need to call the ANDS DOI service have not been provided.
     * @throws IllegalArgumentException
     *             thrown when method is called with invalid arguments.
     * @throws IOException
     *             thrown when attempting to read response.
     * @throws HttpException
     *             thrown when attempting to execute method call.
     */
    public AndsDoiResponse deActivateDOI(String doi) throws HttpException, IOException {
        String methodName = deactivateDoi;
        this.validateParameters(methodName, doi);
        String queryString = MessageFormat.format("app_id={0}&doi={1}", new Object[] { getRequestorIdentity().getAppId(), doi });
        return executeMethod(queryString, methodName, null, null, null);
    }

    /**
     * Responsible for requesting the meta-data for a given DOI.
     * <p>
     * 
     * @param doi
     *            DOI for which the meta-data is requested.
     * @throws IllegalStateException
     *             thrown if the parameters need to call the ANDS DOI service have not been provided.
     * @throws IllegalArgumentException
     *             thrown when method is called with invalid arguments.
     * @throws IOException
     *             thrown when attempting to read response.
     * @throws HttpException
     *             thrown when attempting to execute method call.
     */
    public AndsDoiResponse requestMetaDataOfDOI(String doi) throws HttpException, IOException {
        this.validateState();
        this.validateParameters(doiMetadataRequest, doi);
        String queryString = MessageFormat.format("doi={0}", new Object[] { doi });
        return executeMethod(queryString, doiMetadataRequest, null, null, null);
    }

    /**
     * Checks that this class is in a valid state to call the ANDS DOI service.
     * 
     * @throws IllegalStateException
     *             thrown if the parameters need to call the ANDS DOI service have not been provided.
     */
    private void validateState() throws IllegalStateException {
        StringBuffer errorMsg = new StringBuffer();
        if (StringUtils.isEmpty(this.getDoiServiceHost())) {
            errorMsg.append("The host name of the ANDS DOI service has not been provided. e.g. test.org.au\n");
        }
        if (getRequestorIdentity() == null || StringUtils.isEmpty(this.getRequestorIdentity().getAppId())) {
            errorMsg.append("The appID of the caller has not been provided. " + "e.g. unique Id provided by ANDS upon IP registration.\n");
        }
        if (getRequestorIdentity() == null || StringUtils.isEmpty(this.getRequestorIdentity().getAuthDomain())) {
            errorMsg.append("The authDomain of the caller has not been provided. " + "e.g. the domain of the organisation calling the service.");
        }
        if (errorMsg.length() != 0) {
            throw new IllegalStateException(errorMsg.toString());
        }
    }

    /**
     * Checks the arguments passed to the {@link #mintDOI(String)} and the
     * {@link #mintHandleByIndex(HandleType, int, String)} methods.
     * 
     * @param methodName
     *            the method that is being called on the DOI service call.
     * @param doi
     *            for an update or get meta-data request service call.
     * @param url
     *            for a mint DOI service call.
     * @throws IllegalStateException
     *             thrown if the arguments provided to {@link #mintDOI(HandleType, String)} or to
     *             {@link #mintHandleByIndex(HandleType, int, String)} are not valid.
     */
    private void validateParameters(String... values) throws IllegalArgumentException {
        if (mintMethodName.equals(values[0]) && StringUtils.isEmpty(values[1])) {
            throw new IllegalArgumentException(MessageFormat.format("The method to mint a DOI can only be called if the url value is a non-empty value:values={1}\n", new Object[] { values[1] }));
        }
        if (((updateMethodName.equals(values[0])) || (values[0].equals(doiMetadataRequest))) && StringUtils.isEmpty(values[1])) {
            throw new IllegalArgumentException(MessageFormat.format("The method to update or request meta-data can only be called if the DOI and updated url are " + "non-empty values:values={1}, values={2}\n", new Object[] { values[1], values[2] }));
        }
        if (((activateDoi.equals(values[0])) || (deactivateDoi.equals(values[0]))) && StringUtils.isEmpty(values[1])) {
            throw new IllegalArgumentException(MessageFormat.format("The method to activate / deactivate DOIs can only be called " + "if the DOI is non-empty:values={1}\n", new Object[] { values[1] }));
        }
    }

    /**
     * Constructs and executes an HTTP POST call.
     * 
     * @param queryString
     *            the query string to provide the POST call.
     * @param methodName
     *            the method to call.
     * @param updateURL
     *            associated with a DOI that needs to be updated.
     * @param existingMetaDataXML
     *            meta-data associated with a DOI as held by ANDS.
     * @param doiDTO
     *            holds the objects for constructing / updating the meta-data XML.
     * @return a formatted XML response.
     * @throws IOException
     *             thrown when attempting to read response.
     * @throws HttpException
     *             thrown when attempting to execute method call.
     */
    private AndsDoiResponse executeMethod(String queryString, String methodName, String updateURL, String existingMetaDataXML, DoiDTO doiDTO) throws HttpException, IOException {
        String metaDataXML = null;
        String requestType = getRequestType(methodName, (doiDTO != null ? true : false));
        if (LOG.isDebugEnabled()) {
            LOG.debug("ExecuteMethod : Query String : ->" + queryString);
            LOG.debug("ExecuteMethod : Method Name : ->" + methodName);
        }
        HttpsURL url = new HttpsURL(this.getDoiServiceHost(), this.getDoiServicePath(), queryString, "");
        url.setPath(url.getPath() + "/" + methodName);
        if (methodName.equals(mintMethodName)) {
            metaDataXML = DoiMetaDataGenerator.generateMetaDataXMLFromDTO(doiDTO);
        }
        if (methodName.equals(updateMethodName) && (doiDTO != null)) {
            metaDataXML = DoiMetaDataGenerator.updateMetaDataXMLFromDTO(existingMetaDataXML, doiDTO);
        }
        return doiRequest(url.toString(), metaDataXML, requestType);
    }

    /**
     * Returns the Request Type : GET or POST
     * 
     * @param methodName
     *            call that is to be made to the DOI service.
     * @param updateURL
     *            the URL to be updated.
     * @return requestType GET or POST.
     */
    private String getRequestType(String methodName, boolean updateMetaData) {
        if (mintMethodName.equals(methodName) || (updateMetaData)) {
            return "POST";
        } else {
            return "GET";
        }
    }

    /**
     * Calls a POST method of the ANDS Digital Object identifier service in a RESTful web service manner. The query
     * string of the URI defines the type of operation that is to be performed. The request body contains an XML
     * fragment that identifies the caller.
     * 
     * @param serviceUrl
     *            the URI of the RESTful ANDS Digital Object Identifier web service.
     * @param metaDataXML
     *            an XML fragment that details the meta data of the document for which the DOI is queried.
     * @param updateURL
     *            URL for the DOI that needs to be updated.
     * @param requestType
     *            POST or GET
     * @return
     * @throws IOException
     */
    private static AndsDoiResponse doiRequest(String serviceUrl, String metaDataXML, String requestType) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Method URL: " + serviceUrl);
            LOG.debug("Metadata XML NULL ?: " + StringUtils.isEmpty(metaDataXML));
            LOG.debug("Request Type: " + requestType);
        }
        AndsDoiResponse doiResponse = null;
        OutputStreamWriter wr = null;
        BufferedReader rd = null;
        StringBuffer outputBuffer;
        URL url = new URL(serviceUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setDoInput(true);
            if (requestType.equals("POST")) {
                conn.setDoOutput(true);
                wr = new OutputStreamWriter(conn.getOutputStream());
                if (metaDataXML != null) {
                    wr.write("xml=" + URLEncoder.encode(metaDataXML, "UTF-8"));
                }
                wr.flush();
            } else {
                conn.setDoOutput(false);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(conn.getResponseCode() + " - " + conn.getResponseMessage());
            }
            outputBuffer = new StringBuffer();
            outputBuffer.append(conn.getResponseMessage() + "\n");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                outputBuffer.append(line);
            }
            doiResponse = new AndsDoiResponse();
            doiResponse.setMessage(outputBuffer.toString());
            setResponseFlag(conn.getResponseCode(), doiResponse);
        } catch (Exception e) {
            doiResponse = new AndsDoiResponse();
            outputBuffer = new StringBuffer();
            outputBuffer.append(conn.getResponseMessage() + "\n");
            BufferedReader rde = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            String line;
            while ((line = rde.readLine()) != null) {
                outputBuffer.append(line);
            }
            doiResponse.setSuccess(false);
            doiResponse.setMessage(outputBuffer.toString());
            rde.close();
        } finally {
            if (wr != null) {
                wr.close();
            }
            if (rd != null) {
                rd.close();
            }
        }
        return doiResponse;
    }

    /**
     * Sets a boolean flag indicating success or failure based on the returned code.
     * 
     * @param responseCode
     *            code returned from the web service invocation.
     * @param responseMessage
     *            message returned from the web service invocation.
     * @param doiResponse
     *            response to be sent back to the client.
     * 
     */
    private static void setResponseFlag(int responseCode, AndsDoiResponse doiResponse) {
        switch(responseCode) {
            case 200:
                if (doiResponse.getMessage().contains("OK") && ((doiResponse.getMessage().contains("successfully")) || doiResponse.getMessage().contains("<?xml version="))) {
                    doiResponse.setSuccess(true);
                } else {
                    doiResponse.setSuccess(false);
                }
                break;
            case 415:
                doiResponse.setSuccess(false);
                break;
            case 500:
                doiResponse.setSuccess(false);
                break;
            default:
                doiResponse.setSuccess(false);
                break;
        }
    }

    /**
     * Parses an XML document which contains an ANDS DOI service response for a node that specifies whether the service
     * call was successful.
     * 
     * @param doc
     *            an XML document of the ANDS DOI service response.
     * @return whether the response represents a success
     * @throws XPathExpressionException
     *             thrown when attempting to execute XPath on XML response.
     * @throws ParserConfigurationException
     *             thrown when attempting to convert response to an XML document.
     * @throws SAXException
     *             thrown when attempting to convert response to an XML document.
     * @throws IOException
     *             thrown when attempting to read response.
     */
    @SuppressWarnings(value = "all")
    private boolean parseForSuccess(Document doc) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression expr = xpath.compile("//response[@type]");
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        for (int i = 0; i < nodes.getLength(); i++) {
            for (int j = 0; j < nodes.item(i).getAttributes().getLength(); j++) {
                Node node = (Node) nodes.item(i).getAttributes().item(j);
                return "success".equals(node.getNodeValue());
            }
        }
        return false;
    }

    /**
     * Parses an XML document which contains an ANDS DOI service response for a node that specifies what the response
     * message is.
     * 
     * @param doc
     *            an XML document of the ANDS DOI service response.
     * @return the response message
     * @throws XPathExpressionException
     *             thrown when attempting to execute XPath on XML response.
     * @throws ParserConfigurationException
     *             thrown when attempting to convert response to an XML document.
     * @throws SAXException
     *             thrown when attempting to convert response to an XML document.
     * @throws IOException
     *             thrown when attempting to read response.
     */
    @SuppressWarnings(value = "all")
    private String parseForMessage(Document doc) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression expr = xpath.compile("//response/message");
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        for (int i = 0; i < nodes.getLength(); i++) {
            return nodes.item(i).getTextContent();
        }
        return null;
    }

    /**
     * Retrieve the current ANDS Digital Object Identifier host name
     * 
     * @return the ANDS Digital Object Identifier host name
     */
    public String getDoiServiceHost() {
        return doiServiceHost;
    }

    /**
     * Set the ANDS Digital Object Identifier host name.
     * 
     * @param pidServiceHost
     *            the ANDS Digital Object Identifier host name to set
     */
    public void setDoiServiceHost(String pidServiceHost) {
        this.doiServiceHost = pidServiceHost;
    }

    /**
     * Retrieve the current ANDS Digital Object Identifier path name (web application context name)
     * 
     * @return the ANDS Digital Object Identifier path name
     */
    public String getDoiServicePath() {
        return doiServicePath;
    }

    /**
     * Set the current ANDS Digital Object Identifier path name (web application context name)
     * 
     * @param pidServicePath
     *            the ANDS Digital Object Identifier path name to set
     */
    public void setDoiServicePath(String pidServicePath) {
        this.doiServicePath = pidServicePath;
    }

    /**
     * Retrieve the identity information of the calling application/organisation.
     * 
     * @return the identity of the caller
     */
    public AndsDoiIdentity getRequestorIdentity() {
        return requestorIdentity;
    }

    /**
     * Set the identity information of the calling application/organisation.
     * 
     * @param requestorIdentity
     *            the identity object to set
     */
    public void setRequestorIdentity(AndsDoiIdentity requestorIdentity) {
        this.requestorIdentity = requestorIdentity;
    }

    /**
     * @return the metadataTemplatePath
     */
    public String getMetadataTemplatePath() {
        return metadataTemplatePath;
    }

    /**
     * @param metadataTemplatePath
     *            the metadataTemplatePath to set
     */
    public void setMetadataTemplatePath(String metadataTemplatePath) {
        this.metadataTemplatePath = metadataTemplatePath;
    }
}
