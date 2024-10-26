package de.marcschumacher.jgrli;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import de.marcschumacher.jgrli.enums.JgrliErrorCode;
import de.marcschumacher.jgrli.enums.JgrliMethod;
import de.marcschumacher.jgrli.enums.JgrliPriority;
import de.marcschumacher.jgrli.exception.JgrliException;
import de.marcschumacher.jgrli.result.JgrliErrorResult;
import de.marcschumacher.jgrli.result.JgrliResult;
import de.marcschumacher.jgrli.result.JgrliSuccessResult;

/**
 * Java Growl Library - Client
 * 
 * @author Marc Schumacher
 * 
 */
public class JgrliClient {

    private static final String SERVER_SCHEME = "https";

    private static final String SERVER_HOST = "prowl.weks.net";

    private static final int SERVER_PORT = -1;

    private static final String SERVER_BASE_FOLDER = "/publicapi";

    private static final int MAX_APPLICATION_LENGTH = 256;

    private static final int MAX_EVENT_LENGTH = 1024;

    private static final int MAX_DESCRIPTION_LENGTH = 10000;

    private static final Logger log = Logger.getLogger(JgrliClient.class);

    private final String apiKey;

    private final String providerKey;

    /**
	 * Default constructor if no provider key available
	 * 
	 * @param apiKey
	 *            API key from prowl website
	 * @throws JgrliException
	 *             Thrown on errors which cannot be handled
	 */
    public JgrliClient(String apiKey) throws JgrliException {
        this(apiKey, null);
    }

    /**
	 * Constructor for using the prowl client with provider key available
	 * 
	 * @param apiKey
	 *            API key from prowl website
	 * @param providerKey
	 *            Provider key received from prowl
	 * @throws JgrliException
	 *             Thrown on errors which cannot be handled
	 */
    public JgrliClient(String apiKey, String providerKey) throws JgrliException {
        log.debug("Creating prowl client with API key " + apiKey + " and provider key " + providerKey);
        if (apiKey == null) {
            throw new JgrliException("API key must not be null!");
        }
        if (apiKey.length() != 40) {
            throw new JgrliException("API key must be 40 char hex string!");
        }
        this.apiKey = apiKey;
        if ((providerKey != null) && (providerKey.length() > 40)) {
            throw new JgrliException("Provider Key must not be longer than 40!");
        }
        this.providerKey = providerKey;
    }

    public JgrliResult add(String application, String event, String description) throws JgrliException {
        return add(application, event, description, null);
    }

    public JgrliResult add(String application, String event, String description, JgrliPriority priority) throws JgrliException {
        log.debug("Sending message with application \"" + application + "\", event \"" + event + "\", description \"" + description + "\", priority " + priority);
        JgrliResult ret = null;
        if ((application != null) && (application.length() > MAX_APPLICATION_LENGTH)) {
            throw new JgrliException("Application name must not be longer than " + MAX_APPLICATION_LENGTH + "!");
        }
        if ((event != null) && (event.length() > MAX_EVENT_LENGTH)) {
            throw new JgrliException("Event must not be longer than " + MAX_EVENT_LENGTH + "!");
        }
        if ((description != null) && (description.length() > MAX_DESCRIPTION_LENGTH)) {
            throw new JgrliException("Description must not be longer than " + MAX_DESCRIPTION_LENGTH + "!");
        }
        if (priority == null) {
            ret = call(JgrliMethod.ADD, new BasicNameValuePair("application", application), new BasicNameValuePair("event", event), new BasicNameValuePair("description", description));
        } else {
            ret = call(JgrliMethod.ADD, new BasicNameValuePair("application", application), new BasicNameValuePair("event", event), new BasicNameValuePair("description", description), new BasicNameValuePair("priority", Integer.toString(priority.getValue())));
        }
        return ret;
    }

    public JgrliResult verify() throws JgrliException {
        return call(JgrliMethod.VERIFY);
    }

    private JgrliResult call(JgrliMethod method, NameValuePair... parameterArray) throws JgrliException {
        JgrliResult ret = null;
        try {
            HttpClient httpClient = new DefaultHttpClient();
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            parameters.add(new BasicNameValuePair("apikey", this.apiKey));
            if (this.providerKey != null) {
                parameters.add(new BasicNameValuePair("providerkey", this.providerKey));
            }
            parameters.addAll(Arrays.asList(parameterArray));
            URI uri = URIUtils.createURI(SERVER_SCHEME, SERVER_HOST, SERVER_PORT, SERVER_BASE_FOLDER + "/" + method.getId(), URLEncodedUtils.format(parameters, "UTF-8"), null);
            HttpGet httpGet = new HttpGet(uri);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            if (httpEntity != null) {
                InputStream inputStream = httpEntity.getContent();
                try {
                    SAXBuilder saxBuilder = new SAXBuilder();
                    Document document = saxBuilder.build(inputStream);
                    Element rootElement = document.getRootElement();
                    String rootElementName = rootElement.getName();
                    if (!"prowl".equals(rootElementName)) {
                        throw new JgrliException("Unknown root element name delivered by prowl (" + rootElementName + "!");
                    }
                    List<?> children = rootElement.getChildren();
                    if (children.size() != 1) {
                        throw new JgrliException("Prowl result contains more than one element!");
                    }
                    Element prowlElement = (Element) children.get(0);
                    String elementName = prowlElement.getName();
                    Attribute codeAttribute = prowlElement.getAttribute("code");
                    String stringCode = codeAttribute.getValue();
                    int code = Integer.parseInt(stringCode);
                    JgrliErrorCode prowlErrorCode = JgrliErrorCode.parse(code);
                    if ("error".equals(elementName)) {
                        String message = prowlElement.getText();
                        ret = new JgrliErrorResult(prowlErrorCode, message);
                    } else if ("success".equals(elementName)) {
                        String stringRemaining = prowlElement.getAttributeValue("remaining");
                        String stringResetDate = prowlElement.getAttributeValue("resetdate");
                        long resetDateTimestamp = Long.parseLong(stringResetDate);
                        int remaining = Integer.parseInt(stringRemaining);
                        Date resetDate = new Date(resetDateTimestamp * 1000);
                        ret = new JgrliSuccessResult(prowlErrorCode, remaining, resetDate);
                    } else {
                        throw new JgrliException("Unknown prowl return content (" + elementName + ")!");
                    }
                } catch (JDOMException e) {
                    throw new JgrliException("Unable to parse result!", e);
                }
            }
        } catch (URISyntaxException e) {
            throw new JgrliException("Unable to encode URL for prowl call!", e);
        } catch (ClientProtocolException e) {
            throw new JgrliException("Unable to process for HTTP prowl call!", e);
        } catch (IOException e) {
            throw new JgrliException("Unable to process prowl call!", e);
        }
        return ret;
    }
}
