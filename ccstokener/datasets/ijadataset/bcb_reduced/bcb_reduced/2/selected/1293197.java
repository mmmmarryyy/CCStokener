package net.sbbi.upnp.messages;

import java.util.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.*;
import net.sbbi.upnp.services.*;
import javax.xml.parsers.*;

/**
 * Message object for an UPNP action, simply call setInputParameter() to add
 * the required action message params and then service() to receive the ActionResponse
 * built with the parsed UPNP device SOAP xml response.
 * @author <a href="mailto:superbonbon@sbbi.net">SuperBonBon</a>
 * @version 1.0
 */
public class ActionMessage {

    private static final Log log = LogFactory.getLog(ActionMessage.class);

    private static final Map XML_ENTITIES = new HashMap();

    static {
        XML_ENTITIES.put(Character.valueOf('"'), "quot");
        XML_ENTITIES.put(Character.valueOf('&'), "amp");
        XML_ENTITIES.put(Character.valueOf('<'), "lt");
        XML_ENTITIES.put(Character.valueOf('>'), "gt");
        XML_ENTITIES.put(Character.valueOf('\''), "apos");
    }

    private UPNPService service;

    private ServiceAction serviceAction;

    private List inputParameters;

    /**
   * Protected constuctor so that only messages factories can build it
   * @param service the service for which the
   * @param serviceAction
   */
    protected ActionMessage(UPNPService service, ServiceAction serviceAction) {
        this.service = service;
        this.serviceAction = serviceAction;
        if (serviceAction.getInputActionArguments() != null) {
            inputParameters = new ArrayList();
        }
    }

    /**
   * Method to clear all set input parameters so that
   * this object can be reused
   */
    public void clearInputParameters() {
        inputParameters.clear();
    }

    /**
   * Executes the message and retuns the UPNP device response, according to the UPNP specs,
   * this method could take up to 30 secs to process ( time allowed for a device to respond to a request )
   * @return a response object containing the UPNP parsed response
   * @throws IOException if some IOException occurs during message send and reception process
   * @throws UPNPResponseException if an UPNP error message is returned from the server
   *         or if some parsing exception occurs ( detailErrorCode = 899, detailErrorDescription = SAXException message )
   */
    public ActionResponse service() throws IOException, UPNPResponseException {
        ActionResponse rtrVal = null;
        UPNPResponseException upnpEx = null;
        IOException ioEx = null;
        StringBuffer body = new StringBuffer(256);
        body.append("<?xml version=\"1.0\"?>\r\n");
        body.append("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"");
        body.append(" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">");
        body.append("<s:Body>");
        body.append("<u:").append(serviceAction.getName()).append(" xmlns:u=\"").append(service.getServiceType()).append("\">");
        if (serviceAction.getInputActionArguments() != null) {
            for (Iterator itr = inputParameters.iterator(); itr.hasNext(); ) {
                InputParamContainer container = (InputParamContainer) itr.next();
                body.append("<").append(container.name).append(">").append(container.value);
                body.append("</").append(container.name).append(">");
            }
        }
        body.append("</u:").append(serviceAction.getName()).append(">");
        body.append("</s:Body>");
        body.append("</s:Envelope>");
        if (log.isDebugEnabled()) log.debug("POST prepared for URL " + service.getControlURL());
        URL url = new URL(service.getControlURL().toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        HttpURLConnection.setFollowRedirects(false);
        conn.setRequestProperty("HOST", url.getHost() + ":" + url.getPort());
        conn.setRequestProperty("CONTENT-TYPE", "text/xml; charset=\"utf-8\"");
        conn.setRequestProperty("CONTENT-LENGTH", Integer.toString(body.length()));
        conn.setRequestProperty("SOAPACTION", "\"" + service.getServiceType() + "#" + serviceAction.getName() + "\"");
        OutputStream out = conn.getOutputStream();
        out.write(body.toString().getBytes());
        out.flush();
        out.close();
        conn.connect();
        InputStream input = null;
        if (log.isDebugEnabled()) log.debug("executing query :\n" + body);
        try {
            input = conn.getInputStream();
        } catch (IOException ex) {
            input = conn.getErrorStream();
        }
        if (input != null) {
            int response = conn.getResponseCode();
            String responseBody = getResponseBody(input);
            if (log.isDebugEnabled()) log.debug("received response :\n" + responseBody);
            SAXParserFactory saxParFact = SAXParserFactory.newInstance();
            saxParFact.setValidating(false);
            saxParFact.setNamespaceAware(true);
            ActionMessageResponseParser msgParser = new ActionMessageResponseParser(serviceAction);
            StringReader stringReader = new StringReader(responseBody);
            InputSource src = new InputSource(stringReader);
            try {
                SAXParser parser = saxParFact.newSAXParser();
                parser.parse(src, msgParser);
            } catch (ParserConfigurationException confEx) {
                throw new RuntimeException("ParserConfigurationException during SAX parser creation, please check your env settings:" + confEx.getMessage());
            } catch (SAXException saxEx) {
                upnpEx = new UPNPResponseException(899, saxEx.getMessage());
            } finally {
                try {
                    input.close();
                } catch (IOException ex) {
                }
            }
            if (upnpEx == null) {
                if (response == HttpURLConnection.HTTP_OK) {
                    rtrVal = msgParser.getActionResponse();
                } else if (response == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    upnpEx = msgParser.getUPNPResponseException();
                } else {
                    ioEx = new IOException("Unexpected server HTTP response:" + response);
                }
            }
        }
        try {
            out.close();
        } catch (IOException ex) {
        }
        conn.disconnect();
        if (upnpEx != null) {
            throw upnpEx;
        }
        if (rtrVal == null && ioEx == null) {
            ioEx = new IOException("Unable to receive a response from the UPNP device");
        }
        if (ioEx != null) {
            throw ioEx;
        }
        return rtrVal;
    }

    private String getResponseBody(InputStream in) throws IOException {
        byte[] buffer = new byte[256];
        int readen = 0;
        StringBuffer content = new StringBuffer(256);
        while ((readen = in.read(buffer)) != -1) {
            content.append(new String(buffer, 0, readen, "UTF-8"));
        }
        int len = content.length();
        while (content.charAt(len - 1) == '\0') {
            len--;
            content.setLength(len);
        }
        return content.toString().trim();
    }

    /**
   * The list of input parameters that should be accepted by the device service for this message
   * @return a list of required input parameters ServiceActionArgument objects for this message
   *         or null if the message does not require any input params
   */
    public List getInputParameterNames() {
        return serviceAction.getInputActionArgumentsNames();
    }

    /**
   * The list of output parameters that should be returned by the device service
   * @return a list of output parameters ServiceActionArgument objects for this message
   *         or null if the message does not contains any output params.
   */
    public List getOutputParameterNames() {
        return serviceAction.getOutputActionArgumentsNames();
    }

    /**
   * Set the value of an input parameter before a message service call. If the param name already
   * exists, the param value will be overwritten with the new value provided.
   * @param parameterName the parameter name
   * @param parameterValue the parameter value as an object, primitive object are handled, all other object
   *                       will be assigned with a call to their toString() method call
   * @return the current ActionMessage object instance
   * @throws IllegalArgumentException if the provided parameterName is not valid for this message
   *         or if no input parameters are required for this message
   */
    public ActionMessage setInputParameter(String parameterName, Object parameterValue) throws IllegalArgumentException {
        if (parameterValue == null) {
            return setInputParameter(parameterName, "");
        } else if (parameterValue instanceof Date) {
            return setInputParameter(parameterName, (Date) parameterValue);
        } else if (parameterValue instanceof Boolean) {
            return setInputParameter(parameterName, ((Boolean) parameterValue).booleanValue());
        } else if (parameterValue instanceof Integer) {
            return setInputParameter(parameterName, ((Integer) parameterValue).intValue());
        } else if (parameterValue instanceof Byte) {
            return setInputParameter(parameterName, ((Byte) parameterValue).byteValue());
        } else if (parameterValue instanceof Short) {
            return setInputParameter(parameterName, ((Short) parameterValue).shortValue());
        } else if (parameterValue instanceof Float) {
            return setInputParameter(parameterName, ((Float) parameterValue).floatValue());
        } else if (parameterValue instanceof Double) {
            return setInputParameter(parameterName, ((Double) parameterValue).doubleValue());
        } else if (parameterValue instanceof Long) {
            return setInputParameter(parameterName, ((Long) parameterValue).longValue());
        }
        return setInputParameter(parameterName, parameterValue.toString());
    }

    /**
   * Set the value of an input parameter before a message service call. If the param name already
   * exists, the param value will be overwritten with the new value provided
   * @param parameterName the parameter name
   * @param parameterValue the string parameter value
   * @return the current ActionMessage object instance
   * @throws IllegalArgumentException if the provided parameterName is not valid for this message
   *         or if no input parameters are required for this message
   */
    public ActionMessage setInputParameter(String parameterName, String parameterValue) throws IllegalArgumentException {
        if (serviceAction.getInputActionArguments() == null) throw new IllegalArgumentException("No input parameters required for this message");
        ServiceActionArgument arg = serviceAction.getInputActionArgument(parameterName);
        if (arg == null) throw new IllegalArgumentException("Wrong input argument name for this action:" + parameterName + " available parameters are : " + getInputParameterNames());
        String escapedVal = escape(parameterValue);
        for (Iterator i = inputParameters.iterator(); i.hasNext(); ) {
            InputParamContainer container = (InputParamContainer) i.next();
            if (container.name.equals(parameterName)) {
                container.value = escapedVal;
                return this;
            }
        }
        InputParamContainer container = new InputParamContainer();
        container.name = parameterName;
        container.value = escapedVal;
        inputParameters.add(container);
        return this;
    }

    /**
   * <p>Escapes the characters in a <code>String</code>.</p>
   *
   * <p>For example, if you have called addEntity(&quot;foo&quot;, 0xA1),
   * escape(&quot;¡&quot;) will return &quot;&amp;foo;&quot;</p>
   *
   * @param str The <code>String</code> to escape.
   * @return A new escaped <code>String</code>.
   */
    public String escape(String str) {
        StringBuffer buf = new StringBuffer(str.length() * 2);
        int i;
        for (i = 0; i < str.length(); ++i) {
            char ch = str.charAt(i);
            String entityName = (String) XML_ENTITIES.get(Character.valueOf(ch));
            if (entityName == null) {
                if (ch > 0x7F) {
                    int intValue = ch;
                    buf.append("&#");
                    buf.append(intValue);
                    buf.append(';');
                } else {
                    buf.append(ch);
                }
            } else {
                buf.append('&');
                buf.append(entityName);
                buf.append(';');
            }
        }
        return buf.toString();
    }

    /**
   * Set the value of an input parameter before a message service call
   * @param parameterName the parameter name
   * @param parameterValue the date parameter value, will be automatically translated to the correct
   *                       ISO 8601 date format for the given action input param related state variable 
   * @return the current ActionMessage object instance
   * @throws IllegalArgumentException if the provided parameterName is not valid for this message
   *         or if no input parameters are required for this message
   */
    public ActionMessage setInputParameter(String parameterName, Date parameterValue) throws IllegalArgumentException {
        if (serviceAction.getInputActionArguments() == null) throw new IllegalArgumentException("No input parameters required for this message");
        ServiceActionArgument arg = serviceAction.getInputActionArgument(parameterName);
        if (arg == null) throw new IllegalArgumentException("Wrong input argument name for this action:" + parameterName + " available parameters are : " + getInputParameterNames());
        ServiceStateVariable linkedVar = arg.getRelatedStateVariable();
        if (linkedVar.getDataType().equals(ServiceStateVariable.TIME)) {
            return setInputParameter(parameterName, ISO8601Date.getIsoTime(parameterValue));
        } else if (linkedVar.getDataType().equals(ServiceStateVariable.TIME_TZ)) {
            return setInputParameter(parameterName, ISO8601Date.getIsoTimeZone(parameterValue));
        } else if (linkedVar.getDataType().equals(ServiceStateVariable.DATE)) {
            return setInputParameter(parameterName, ISO8601Date.getIsoDate(parameterValue));
        } else if (linkedVar.getDataType().equals(ServiceStateVariable.DATETIME)) {
            return setInputParameter(parameterName, ISO8601Date.getIsoDateTime(parameterValue));
        } else if (linkedVar.getDataType().equals(ServiceStateVariable.DATETIME_TZ)) {
            return setInputParameter(parameterName, ISO8601Date.getIsoDateTimeZone(parameterValue));
        } else {
            throw new IllegalArgumentException("Related input state variable " + linkedVar.getName() + " is not of an date type");
        }
    }

    /**
   * Set the value of an input parameter before a message service call
   * @param parameterName the parameter name
   * @param parameterValue the boolean parameter value
   * @return the current ActionMessage object instance
   * @throws IllegalArgumentException if the provided parameterName is not valid for this message
   *         or if no input parameters are required for this message
   */
    public ActionMessage setInputParameter(String parameterName, boolean parameterValue) throws IllegalArgumentException {
        return setInputParameter(parameterName, parameterValue ? "1" : "0");
    }

    /**
   * Set the value of an input parameter before a message service call
   * @param parameterName the parameter name
   * @param parameterValue the byte parameter value
   * @return the current ActionMessage object instance
   * @throws IllegalArgumentException if the provided parameterName is not valid for this message
   *         or if no input parameters are required for this message
   */
    public ActionMessage setInputParameter(String parameterName, byte parameterValue) throws IllegalArgumentException {
        return setInputParameter(parameterName, Byte.toString(parameterValue));
    }

    /**
   * Set the value of an input parameter before a message service call
   * @param parameterName the parameter name
   * @param parameterValue the short parameter value
   * @return the current ActionMessage object instance
   * @throws IllegalArgumentException if the provided parameterName is not valid for this message
   *         or if no input parameters are required for this message
   */
    public ActionMessage setInputParameter(String parameterName, short parameterValue) throws IllegalArgumentException {
        return setInputParameter(parameterName, Short.toString(parameterValue));
    }

    /**
   * Set the value of an input parameter before a message service call
   * @param parameterName the parameter name
   * @param parameterValue the integer parameter value
   * @return the current ActionMessage object instance
   * @throws IllegalArgumentException if the provided parameterName is not valid for this message
   *         or if no input parameters are required for this message
   */
    public ActionMessage setInputParameter(String parameterName, int parameterValue) throws IllegalArgumentException {
        return setInputParameter(parameterName, Integer.toString(parameterValue));
    }

    /**
   * Set the value of an input parameter before a message service call
   * @param parameterName the parameter name
   * @param parameterValue the long parameter value
   * @return the current ActionMessage object instance
   * @throws IllegalArgumentException if the provided parameterName is not valid for this message
   *         or if no input parameters are required for this message
   */
    public ActionMessage setInputParameter(String parameterName, long parameterValue) throws IllegalArgumentException {
        return setInputParameter(parameterName, Long.toString(parameterValue));
    }

    /**
   * Set the value of an input parameter before a message service call
   * @param parameterName the parameter name
   * @param parameterValue the float parameter value
   * @return the current ActionMessage object instance
   * @throws IllegalArgumentException if the provided parameterName is not valid for this message
   *         or if no input parameters are required for this message
   */
    public ActionMessage setInputParameter(String parameterName, float parameterValue) throws IllegalArgumentException {
        return setInputParameter(parameterName, Float.toString(parameterValue));
    }

    /**
   * Set the value of an input parameter before a message service call
   * @param parameterName the parameter name
   * @param parameterValue the double parameter value
   * @return the current ActionMessage object instance
   * @throws IllegalArgumentException if the provided parameterName is not valid for this message
   *         or if no input parameters are required for this message
   */
    public ActionMessage setInputParameter(String parameterName, double parameterValue) throws IllegalArgumentException {
        return setInputParameter(parameterName, Double.toString(parameterValue));
    }

    /**
   * Input params class container
   */
    private class InputParamContainer {

        private String name;

        private String value;
    }
}
