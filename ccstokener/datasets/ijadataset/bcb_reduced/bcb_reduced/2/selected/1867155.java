package org.eaasyst.eaa.servlets.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;
import java.text.SimpleDateFormat;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.eaasyst.eaa.Constants;
import org.eaasyst.eaa.service.SimpleVelocityService;
import org.eaasyst.eaa.syst.data.transients.MailMessage;
import org.eaasyst.eaa.utils.MailSender;
import org.eaasyst.eaa.utils.StringUtils;

/**
 * <p>This servlet serves as a generic service to which forms can be posted for
 * the purpose of generating e-mail responses and notifications.</p>
 *
 * @version 2.9.7
 * @author Jeff Chilton
 */
public class MailFormServlet extends HttpServlet {

    private static final long serialVersionUID = 1;

    private static final Log log = LogFactory.getLog(MailFormServlet.class.getName());

    private static final List controlParameterNames = getControlParameterNames();

    private static final List multiValueControlParameterNames = getMultiValueControlParameterNames();

    private static final String contentType = "text/html";

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE MM/dd/yyyy 'at' hh:mm a zzzz");

    private Map responseHeaders = new HashMap();

    private MailSender sender = null;

    private DataSource dataSource = null;

    private DataSource alternateDataSource = null;

    /**
	 * <p>The Servlet "init" method.</p>
	 *
	 * @param config the <code>ServletConfig</code> object
	 * @since Eaasy Street 2.8
	 */
    public void init(ServletConfig config) throws ServletException {
        responseHeaders.put("Cache-Control", "no-cache");
        String mailSession = config.getInitParameter("mailSession");
        if (StringUtils.nullOrBlank(mailSession)) {
            log.error("Initialization error: Required init parameter \"mailSession\" not provided.");
        } else {
            log.info("Using mail session \"" + mailSession + "\".");
            sender = new MailSender(mailSession, true);
        }
        String alternateDataSourceName = config.getInitParameter("alternateDataSourceName");
        if (StringUtils.nullOrBlank(alternateDataSourceName)) {
            log.error("Optional init parameter \"alternateDataSourceName\" not provided. Requests for the alternate datasource will be ignored.");
        } else {
            log.info("Using alternate DataSource \"" + alternateDataSourceName + "\".");
            try {
                Context ctx = new InitialContext(new Hashtable());
                alternateDataSource = (DataSource) ctx.lookup(alternateDataSourceName);
                log.info("Alternate DataSource \"" + alternateDataSourceName + "\" successfully acquired.");
            } catch (Throwable t) {
                log.error("Initialization error: Exception obtaining alternate DataSource (\"" + alternateDataSourceName + "\"): " + t.toString(), t);
            }
        }
        String dataSourceName = config.getInitParameter("dataSourceName");
        if (StringUtils.nullOrBlank(dataSourceName)) {
            if (alternateDataSource != null) {
                log.error("Initial parameter \"dataSourceName\" not provided; defaulting to alternate datasource.");
                dataSource = alternateDataSource;
            } else {
                log.error("Optional init parameter \"dataSourceName\" not provided. No database inserts will take place.");
            }
        } else {
            log.info("Using DataSource \"" + dataSourceName + "\".");
            try {
                Context ctx = new InitialContext(new Hashtable());
                dataSource = (DataSource) ctx.lookup(dataSourceName);
                log.info("DataSource \"" + dataSourceName + "\" successfully acquired.");
            } catch (Throwable t) {
                log.error("Initialization error: Exception obtaining DataSource (\"" + dataSourceName + "\"): " + t.toString(), t);
            }
        }
    }

    /**
	 * <p>The Servlet "service" method.</p>
	 *
	 * @param req the <code>HttpServletRequest</code> object
	 * @param res the <code>HttpServletResponse</code> object
	 * @since Eaasy Street 2.9
	 */
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType(contentType);
        Iterator i = responseHeaders.keySet().iterator();
        while (i.hasNext()) {
            String header = (String) i.next();
            res.setHeader(header, (String) responseHeaders.get(header));
        }
        PrintWriter pw = res.getWriter();
        pw.println(getContent(req));
    }

    /**
	 * <p>This method builds and returns the content of the response.</p>
	 *
	 * @param req the <code>HttpServletRequest</code> object
	 * @return the content of the response
	 * @since Eaasy Street 2.8
	 */
    private String getContent(HttpServletRequest req) {
        boolean debugMode = false;
        Properties controlProperties = new Properties();
        Map controlMap = new HashMap();
        controlMap.put("indexedValues", new TreeMap());
        Properties formProperties = new Properties();
        Map formMap = new HashMap();
        ArrayList formValueParams = new ArrayList();
        ArrayList formEmptyParams = new ArrayList();
        String allParamsStr = null;
        try {
            allParamsStr = req.getReader().readLine();
        } catch (IOException e) {
            log.error("Exception reading Posted parameters: ", e);
            return null;
        }
        String[] allParams = allParamsStr.split("&");
        for (int index = 0; index < allParams.length; index++) {
            String currentParamStr = null;
            try {
                currentParamStr = URLDecoder.decode(allParams[index].trim(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
            }
            int equalSignIndex = currentParamStr.indexOf('=');
            String paramName = (equalSignIndex > 0) ? currentParamStr.substring(0, equalSignIndex) : currentParamStr;
            String paramValue = (equalSignIndex > 0 && equalSignIndex < currentParamStr.length() - 1) ? currentParamStr.substring(equalSignIndex + 1) : null;
            if (thisIsAControlParameter(paramName)) {
                if (paramValue != null) processControlParameter(paramName, paramValue, controlProperties, controlMap);
            } else if (paramValue == null) formEmptyParams.add(paramName); else {
                formValueParams.add(currentParamStr);
                storeParam(paramName, paramValue, formProperties, formMap);
            }
        }
        Iterator controlParamsIter = controlMap.entrySet().iterator();
        String[] templateArray = new String[] { "" };
        while (controlParamsIter.hasNext()) {
            Map.Entry currentEntry = (Map.Entry) controlParamsIter.next();
            if (currentEntry.getKey().equals("indexedValues")) {
                TreeMap indexedValuesMap = (TreeMap) currentEntry.getValue();
                Iterator indexedValuesIter = indexedValuesMap.entrySet().iterator();
                while (indexedValuesIter.hasNext()) {
                    Map.Entry indexedEntry = (Map.Entry) indexedValuesIter.next();
                    Map currentParamsMap = (Map) indexedEntry.getValue();
                    Iterator paramMapIter = currentParamsMap.entrySet().iterator();
                    while (paramMapIter.hasNext()) {
                        Map.Entry paramEntry = (Map.Entry) paramMapIter.next();
                        ArrayList currentValue = (ArrayList) paramEntry.getValue();
                        if (currentValue.size() == 1) paramEntry.setValue(currentValue.get(0)); else paramEntry.setValue(currentValue.toArray(templateArray));
                    }
                }
            } else {
                ArrayList currentValue = (ArrayList) currentEntry.getValue();
                currentEntry.setValue(currentValue.toArray(templateArray));
            }
        }
        if (!StringUtils.nullOrBlank(controlProperties.getProperty("configurationURL"))) {
            String configurationURL = controlProperties.getProperty("configurationURL");
            if (log.isDebugEnabled()) {
                log.debug("Using configuration file at " + configurationURL);
            }
            String controlPropertiesData = getTextFromURL(req, configurationURL);
            if (!StringUtils.nullOrBlank(controlPropertiesData)) {
                Properties fileProperties = new Properties();
                try {
                    fileProperties.load(new ByteArrayInputStream(controlPropertiesData.getBytes()));
                    if (log.isDebugEnabled()) {
                        log.debug("Successfully loaded configuration file from " + configurationURL);
                    }
                } catch (IOException ioe) {
                    log.error("Exception while loading base properties: " + controlPropertiesData, ioe);
                }
                processFileProperties(fileProperties, controlProperties, controlMap);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("The configuration file at " + configurationURL + " is empty.");
                }
            }
        }
        if ("true".equalsIgnoreCase(controlProperties.getProperty("debugMode"))) {
            debugMode = true;
            log.info("Debug mode is turned on for this request from " + req.getHeader("Referer"));
        }
        Map indexedValues = (Map) controlMap.get("indexedValues");
        Iterator i = indexedValues.keySet().iterator();
        while (i.hasNext()) {
            String index = (String) i.next();
            Map indexedValueMap = (Map) indexedValues.get(index);
            String mailFrom = "";
            String mailTo = "";
            try {
                mailFrom = (String) indexedValueMap.get("mailFrom");
            } catch (ClassCastException e1) {
                mailFrom = ((String[]) indexedValueMap.get("mailFrom"))[0];
            }
            try {
                mailTo = (String) indexedValueMap.get("mailTo");
            } catch (ClassCastException e1) {
                mailTo = ((String[]) indexedValueMap.get("mailTo"))[0];
            }
            if (!StringUtils.nullOrBlank(mailFrom) && !StringUtils.nullOrBlank(mailTo)) {
                if (log.isDebugEnabled()) {
                    log.debug("Sending mail message(s) for index " + index);
                } else if (debugMode) {
                    log.info("Sending mail message(s) for index " + index);
                }
                buildAndSendMessage(req, sender, indexedValueMap, formProperties, formMap, formValueParams, formEmptyParams, debugMode);
            }
        }
        if (dataSource != null) {
            String sql = controlProperties.getProperty("sqlStatement");
            if (!StringUtils.nullOrBlank(sql)) {
                if (log.isDebugEnabled()) {
                    log.debug("Saving form data to the form processing database.");
                } else if (debugMode) {
                    log.info("Saving form data to the form processing database.");
                }
                DataSource dataSourceToUse = dataSource;
                if (alternateDataSource != null) {
                    if ("true".equalsIgnoreCase(controlProperties.getProperty("useAlternateDataSource"))) {
                        if (log.isDebugEnabled()) {
                            log.debug("Using alternate dataSource as directed by configuration properties.");
                        } else if (debugMode) {
                            log.info("Using alternate dataSource as directed by configuration properties.");
                        }
                        dataSourceToUse = alternateDataSource;
                    }
                }
                saveFormDataToDatabase(req, sql, dataSourceToUse, formProperties, formMap, debugMode);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("SQL statement not provided; form data will not be saved.");
                } else if (debugMode) {
                    log.info("SQL statement not provided; form data will not be saved.");
                }
            }
        }
        return buildResponse(req, controlProperties, controlMap, formProperties, formMap, debugMode);
    }

    /**
	 * <p>This method stores a parameter either as a single value, or as part of Multiple values.</p>
	 *
	 * @param name the parameter name
	 * @param value the parameter value
	 * @param singleValueProps properties containing parameters' name/value
	 * @param multipleValuesMap the map containing parameters' name/multiple values
	 * @since Eaasy Street x.y
	 */
    private static void storeParam(String name, String value, Properties singleValueProps, Map multipleValuesMap) {
        ArrayList multipleValues = (ArrayList) multipleValuesMap.get(name);
        if (multipleValues != null) {
            multipleValues.add(value);
            multipleValuesMap.put(name, multipleValues);
        } else {
            String storedValue = (String) singleValueProps.remove(name);
            if (storedValue == null) singleValueProps.setProperty(name, value); else {
                multipleValues = new ArrayList();
                multipleValues.add(storedValue);
                multipleValues.add(value);
                multipleValuesMap.put(name, multipleValues);
            }
        }
    }

    /**
	 * <p>This method determines if a parameter is a control parameter.</p>
	 *
	 * @param fileProperties the configuration properties from the file
	 * @param controlProperties the configuration properties from the form
	 * @param controlMap the multi-value configuration properties from the form
	 * @since Eaasy Street 2.8
	 */
    private static void processFileProperties(Properties fileProperties, Properties controlProperties, Map controlMap) {
        Properties originalProperties = new Properties(controlProperties);
        Map originalMap = new HashMap(controlMap);
        Map indexedValues = new TreeMap((Map) controlMap.get("indexedValues"));
        originalMap.put("indexedValues", indexedValues);
        Iterator i = indexedValues.keySet().iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            Map originalValues = new HashMap((Map) indexedValues.get(key));
            indexedValues.put(key, originalValues);
        }
        i = fileProperties.keySet().iterator();
        while (i.hasNext()) {
            String name = (String) i.next();
            if (thisIsAControlParameter(name)) {
                String value = fileProperties.getProperty(name);
                if (!StringUtils.nullOrBlank(value)) {
                    value = value.trim();
                    String[] values = null;
                    if (value.startsWith("[") && value.endsWith("]")) {
                        String valueList = value.substring(1, value.length() - 1);
                        values = valueList.split(";");
                        values = removeBlankEntries(values);
                        value = null;
                    }
                    processControlParameter(name, value, values, controlProperties, controlMap);
                }
            }
        }
        controlProperties.putAll(originalProperties);
        Map newIndexedValues = (Map) controlMap.get("indexedValues");
        i = indexedValues.keySet().iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            Map thisMap = (Map) newIndexedValues.get(key);
            thisMap.putAll((Map) indexedValues.get(key));
        }
        controlMap.putAll(originalMap);
        controlMap.put("indexedValues", newIndexedValues);
    }

    /**
	 * <p>This method removes any blank entries from a String array.</p>
	 *
	 * @param values the original String array
	 * @return the modified String array
	 * @since Eaasy Street 2.8
	 */
    private static String[] removeBlankEntries(String[] values) {
        String[] returnValues = null;
        boolean hasBlanks = false;
        for (int i = 0; i < values.length; i++) {
            if (StringUtils.nullOrBlank(values[i])) {
                hasBlanks = true;
            }
        }
        if (hasBlanks) {
            List nonBlanks = new ArrayList();
            for (int i = 0; i < values.length; i++) {
                if (!StringUtils.nullOrBlank(values[i])) {
                    nonBlanks.add(values[i]);
                }
            }
            if (nonBlanks.size() > 0) {
                returnValues = (String[]) nonBlanks.toArray(new String[0]);
            }
        } else {
            if (values.length > 0) {
                returnValues = values;
            }
        }
        return returnValues;
    }

    /**
	 * <p>This method determines if a parameter is a control parameter.</p>
	 *
	 * @param name the name of the parameter
	 * @since Eaasy Street 2.8
	 */
    private static boolean thisIsAControlParameter(String name) {
        boolean controlParameter = false;
        if (controlParameterNames.contains(name)) {
            controlParameter = true;
        } else {
            if (name.endsWith("]")) {
                Iterator i = multiValueControlParameterNames.iterator();
                while (i.hasNext()) {
                    if (name.startsWith(i.next() + "[")) {
                        controlParameter = true;
                    }
                }
            }
        }
        return controlParameter;
    }

    /**
	 * <p>This method builds and sends the e-mail message.</p>
	 *
	 * @param name the property name
	 * @param value the property value (if only one)
	 * @param values the property values (if more than one)
	 * @param controlProperties the control properties
	 * @param controlMap the control map
	 * @since Eaasy Street 2.8
	 */
    private static void processControlParameter(String name, String value, String[] values, Properties controlProperties, Map controlMap) {
        if (controlParameterNames.contains(name)) {
            if (multiValueControlParameterNames.contains(name)) {
                processMultiValueControlParameter(name + "[1]", value, values, controlMap);
            } else {
                if (!StringUtils.nullOrBlank(value)) {
                    controlProperties.setProperty(name, value);
                } else if (values != null) {
                    controlMap.put(name, values);
                }
            }
        } else {
            processMultiValueControlParameter(name, value, values, controlMap);
        }
    }

    private static void processControlParameter(String name, String value, Properties controlProperties, Map controlMap) {
        if (controlParameterNames.contains(name)) {
            if (multiValueControlParameterNames.contains(name)) processMultiValueControlParameter(name + "[1]", value, controlMap); else storeParam(name, value, controlProperties, controlMap);
        } else processMultiValueControlParameter(name, value, controlMap);
    }

    /**
	 * <p>This method builds and sends the e-mail message.</p>
	 *
	 * @param name the property name
	 * @param value the property value (if only one)
	 * @param values the property values (if more than one)
	 * @param controlMap the control map
	 * @since Eaasy Street 2.8
	 */
    private static void processMultiValueControlParameter(String name, String value, String[] values, Map controlMap) {
        Map indexedValues = (Map) controlMap.get("indexedValues");
        String index = name.substring(name.indexOf("[") + 1, name.indexOf("]"));
        Map indexedValueMap = (Map) indexedValues.get(index);
        if (indexedValueMap == null) {
            indexedValueMap = new TreeMap();
            indexedValues.put(index, indexedValueMap);
        }
        String key = name.substring(0, name.indexOf("["));
        if (!StringUtils.nullOrBlank(value)) {
            indexedValueMap.put(key, value);
        } else if (values != null) {
            indexedValueMap.put(key, values);
        }
    }

    private static void processMultiValueControlParameter(String name, String value, Map controlMap) {
        Map indexedValues = (Map) controlMap.get("indexedValues");
        String index = name.substring(name.indexOf("[") + 1, name.indexOf("]"));
        Map indexedValueMap = (Map) indexedValues.get(index);
        if (indexedValueMap == null) indexedValueMap = new TreeMap();
        String key = name.substring(0, name.indexOf("["));
        ArrayList paramValues = (ArrayList) indexedValueMap.get(key);
        if (paramValues == null) paramValues = new ArrayList();
        paramValues.add(value);
        indexedValueMap.put(key, paramValues);
        indexedValues.put(index, indexedValueMap);
    }

    /**
	 * <p>This method builds and sends the e-mail message.</p>
	 *
	 * @param req the <code>HttpServletRequest</code> object
	 * @param controlData the configuration parameters
	 * @param formData the user input
	 * @since Eaasy Street 2.8
	 */
    private static void buildAndSendMessage(HttpServletRequest req, MailSender sender, Map controlMap, Properties formProperties, Map formMap, ArrayList formValueParams, ArrayList formEmptyParams, boolean debugMode) {
        String messageBody = "";
        String template = (String) controlMap.get("mailTemplate");
        if (StringUtils.nullOrBlank(template)) {
            String url = (String) controlMap.get("mailTemplateURL");
            if (!StringUtils.nullOrBlank(url)) {
                template = getTextFromURL(req, url);
            }
        }
        if (StringUtils.nullOrBlank(template)) {
            String fileName = (String) controlMap.get("mailTemplateFile");
            if (!StringUtils.nullOrBlank(fileName)) {
                template = getTextFromFile(fileName);
            }
        }
        String contentType = "text/plain";
        if (StringUtils.nullOrBlank(template)) {
            if (log.isDebugEnabled()) {
                log.debug("Mail template not provided; using default message format.");
            } else if (debugMode) {
                log.info("Mail template not provided; using default message format.");
            }
            messageBody = formatSimpleMessage(req, formValueParams, formEmptyParams);
        } else {
            if (!StringUtils.nullOrBlank((String) controlMap.get("contentType"))) {
                contentType = (String) controlMap.get("contentType");
            }
            if ("true".equalsIgnoreCase((String) controlMap.get("mailTemplateFullyResolved"))) {
                if (log.isDebugEnabled()) {
                    log.debug("Mail template fully resolved; skipping data merge.");
                } else if (debugMode) {
                    log.info("Mail template fully resolved; skipping data merge.");
                }
                messageBody = template;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Merging form data with mail template.");
                } else if (debugMode) {
                    log.info("Merging form data with mail template.");
                }
                Map formData = new HashMap();
                formData.put("singleValues", formProperties);
                formData.put("multipleValues", formMap);
                formData.put("request", req);
                messageBody = SimpleVelocityService.evaluate(template, new VelocityContext(formData));
            }
        }
        String subject = "Form submission response";
        if (!StringUtils.nullOrBlank((String) controlMap.get("subject"))) {
            if (log.isDebugEnabled()) {
                log.debug("Merging form data with mail subject.");
            } else if (debugMode) {
                log.info("Merging form data with mail subject.");
            }
            Map formData = new HashMap();
            formData.put("singleValues", formProperties);
            formData.put("multipleValues", formMap);
            formData.put("request", req);
            subject = SimpleVelocityService.evaluate((String) controlMap.get("subject"), new VelocityContext(formData));
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No subject provided; using default mail subject.");
            } else if (debugMode) {
                log.info("No subject provided; using default mail subject.");
            }
        }
        Vector mailTo = new Vector();
        Vector mailCc = new Vector();
        Vector mailBc = new Vector();
        try {
            String address = (String) controlMap.get("mailTo");
            if (!StringUtils.nullOrBlank(address)) {
                mailTo.addElement(address);
            }
        } catch (ClassCastException e) {
            if (controlMap.get("mailTo") != null) {
                String[] values = (String[]) controlMap.get("mailTo");
                for (int i = 0; i < values.length; i++) {
                    if (!StringUtils.nullOrBlank(values[i])) {
                        mailTo.addElement(values[i]);
                    }
                }
            }
        }
        try {
            String address = (String) controlMap.get("mailCc");
            if (!StringUtils.nullOrBlank(address)) {
                mailCc.addElement(address);
            }
        } catch (ClassCastException e) {
            if (controlMap.get("mailCc") != null) {
                String[] values = (String[]) controlMap.get("mailCc");
                for (int i = 0; i < values.length; i++) {
                    if (!StringUtils.nullOrBlank(values[i])) {
                        mailCc.addElement(values[i]);
                    }
                }
            }
        }
        try {
            String address = (String) controlMap.get("mailBc");
            if (!StringUtils.nullOrBlank(address)) {
                mailBc.addElement(address);
            }
        } catch (ClassCastException e) {
            if (controlMap.get("mailBc") != null) {
                String[] values = (String[]) controlMap.get("mailBc");
                for (int i = 0; i < values.length; i++) {
                    if (!StringUtils.nullOrBlank(values[i])) {
                        mailBc.addElement(values[i]);
                    }
                }
            }
        }
        MailMessage mailMessage = new MailMessage((String) controlMap.get("mailFrom"), mailTo, mailCc, mailBc, null, null, null, subject, contentType, messageBody);
        if (sender.sendMail(mailMessage)) {
            if (log.isDebugEnabled()) {
                log.debug("Message sent via " + req.getHeader("Referer"));
            } else if (debugMode) {
                log.info("Message sent via " + req.getHeader("Referer"));
            }
        } else {
            log.error("Error sending message in MailFormServlet for " + req.getHeader("Referer"));
        }
    }

    /**
	 * <p>This method builds the response page.</p>
	 *
	 * @param req the <code>HttpServletRequest</code> object
	 * @param controlProperties the configuration properties
	 * @param controlMap the configuration map
	 * @param formProperties the single-value user input
	 * @param formMap the multi-value user input
	 * @return the response page
	 * @since Eaasy Street 2.8
	 */
    private static String buildResponse(HttpServletRequest req, Properties controlProperties, Map controlMap, Properties formProperties, Map formMap, boolean debugMode) {
        String response = "";
        String template = controlProperties.getProperty("responseTemplate");
        if (StringUtils.nullOrBlank(template)) {
            String url = controlProperties.getProperty("responseTemplateURL");
            if (!StringUtils.nullOrBlank(url)) {
                if ("true".equalsIgnoreCase(controlProperties.getProperty("responseTemplateFullyResolved"))) {
                    if (log.isDebugEnabled()) {
                        log.debug("Response template fully resolved; building redirect page.");
                    } else if (debugMode) {
                        log.info("Response template fully resolved; building redirect page.");
                    }
                    template = getRedirectPage(url);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Using response template from " + url);
                    } else if (debugMode) {
                        log.info("Using response template from " + url);
                    }
                    template = getTextFromURL(req, url);
                }
            }
        }
        if (StringUtils.nullOrBlank(template)) {
            String fileName = controlProperties.getProperty("responseTemplateFile");
            if (!StringUtils.nullOrBlank(fileName)) {
                template = getTextFromFile(fileName);
            }
        }
        if (StringUtils.nullOrBlank(template)) {
            if (log.isDebugEnabled()) {
                log.debug("Response template not provided; using default response page.");
            } else if (debugMode) {
                log.info("Response template not provided; using default response page.");
            }
            response = "<html><head><title>Form processed</title></head><body><h3>Form processed</h3></body></html>";
        } else {
            if ("true".equalsIgnoreCase(controlProperties.getProperty("responseTemplateFullyResolved"))) {
                if (log.isDebugEnabled()) {
                    log.debug("Response template fully resolved; skipping data merge.");
                } else if (debugMode) {
                    log.info("Response template fully resolved; skipping data merge.");
                }
                response = template;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Merging form data with response template.");
                } else if (debugMode) {
                    log.info("Merging form data with response template.");
                }
                Map formData = new HashMap();
                formData.put("singleValues", formProperties);
                formData.put("multipleValues", formMap);
                formData.put("request", req);
                response = SimpleVelocityService.evaluate(template, new VelocityContext(formData));
            }
        }
        return response;
    }

    /**
	 * <p>This method builds a redirect page to the URL passed.</p>
	 *
	 * @param url the target URL for the redirection
	 * @return the complete redirect page
	 * @since Eaasy Street 2.8
	 */
    private static String getRedirectPage(String url) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<HTML>");
        buffer.append(Constants.LF);
        buffer.append("<HEAD>");
        buffer.append(Constants.LF);
        buffer.append("<META http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">");
        buffer.append(Constants.LF);
        buffer.append("<META HTTP-EQUIV=\"Refresh\" CONTENT=\"0;URL=");
        buffer.append(url);
        buffer.append("\">");
        buffer.append(Constants.LF);
        buffer.append("<TITLE>One Moment Please</TITLE>");
        buffer.append(Constants.LF);
        buffer.append("</HEAD>");
        buffer.append(Constants.LF);
        buffer.append("<BODY>");
        buffer.append(Constants.LF);
        buffer.append("</BODY>");
        buffer.append(Constants.LF);
        buffer.append("</HTML>");
        buffer.append(Constants.LF);
        return buffer.toString();
    }

    /**
	 * <p>This method builds a simple message on data alone.</p>
	 *
	 * @param formData the user input
	 * @return the formatted message
	 * @since Eaasy Street 2.8
	 */
    private static String formatSimpleMessage(HttpServletRequest req, ArrayList formValueParams, ArrayList formEmptyParams) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("This form was sent on ");
        buffer.append(dateFormatter.format(new Date()));
        buffer.append(" by " + req.getRemoteAddr());
        buffer.append(Constants.LF);
        buffer.append("User referred from ");
        buffer.append(req.getHeader("referer"));
        buffer.append(Constants.LF);
        buffer.append("-------------------------------------------------------------");
        buffer.append(Constants.LF);
        buffer.append(Constants.LF);
        for (int index = 0; index < formValueParams.size(); index++) {
            buffer.append(((String) formValueParams.get(index)).replaceFirst("=", ": "));
            buffer.append(Constants.LF);
        }
        buffer.append(Constants.LF);
        buffer.append("-------------------------------------------------------------");
        buffer.append(Constants.LF);
        buffer.append("The following fields returned no value:");
        buffer.append(Constants.LF);
        buffer.append(Constants.LF);
        for (int index = 0; index < formEmptyParams.size(); index++) {
            buffer.append((String) formEmptyParams.get(index));
            buffer.append(Constants.LF);
        }
        buffer.append("-------------------------------------------------------------");
        return buffer.toString();
    }

    /**
	 * <p>This method returns the contents of the specified URL.</p>
	 *
	 * @param urlString the string containing the URL
	 * @return the contents of the specified URL
	 * @since Eaasy Street 2.8
	 */
    private static String getTextFromURL(HttpServletRequest req, String urlString) {
        StringBuffer buffer = new StringBuffer();
        if (!urlString.startsWith("http")) {
            String requestURL = req.getRequestURL().toString();
            urlString = requestURL.substring(0, requestURL.lastIndexOf("/")) + urlString;
        }
        try {
            URL url = new URL(urlString);
            BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = "";
            while ((line = input.readLine()) != null) {
                buffer.append(line);
                buffer.append(Constants.LF);
            }
        } catch (FileNotFoundException nf) {
            log.error("File not found: " + urlString, nf);
        } catch (Exception e) {
            log.error("Exception while reading file: " + urlString, e);
        }
        return buffer.toString();
    }

    /**
	 * <p>This method returns the contents of the specified file.</p>
	 *
	 * @param fileName the name of the file
	 * @return the contents of the specified file
	 * @since Eaasy Street 2.8
	 */
    private static String getTextFromFile(String fileName) {
        return fileName;
    }

    /**
	 * <p>This method returns the contents of the specified file.</p>
	 *
	 * @param req the <code>HttpServletRequest</code> object
	 * @param formProperties the single-value user input
	 * @param formMap the multi-value user input
	 * @since Eaasy Street 2.8.2
	 */
    private static void saveFormDataToDatabase(HttpServletRequest req, String sqlTemplate, DataSource dataSource, Properties formProperties, Map formMap, boolean debugMode) {
        Map formData = new HashMap();
        formData.put("singleValues", formProperties);
        formData.put("multipleValues", formMap);
        formData.put("request", req);
        String sql = SimpleVelocityService.evaluate(sqlTemplate, new VelocityContext(formData));
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            if (log.isDebugEnabled()) {
                log.debug("Executing query: " + sql);
            } else if (debugMode) {
                log.info("Executing query: " + sql);
            }
            int results = stmt.executeUpdate(sql);
            if (results != 1) {
                log.error("Unexpected results from sql execution: " + results);
            }
        } catch (SQLException e) {
            log.error("SQL statement: " + sql);
            log.error("SQL error: " + e.toString() + "; " + e.getMessage(), e);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqle) {
                    log.error("SQL error: " + sqle.toString() + "; " + sqle.getMessage(), sqle);
                }
                stmt = null;
            }
            if (conn != null) {
                try {
                    conn.commit();
                    conn.close();
                } catch (SQLException sqle) {
                    log.error("SQL error: " + sqle.toString() + "; " + sqle.getMessage(), sqle);
                }
                conn = null;
            }
        }
    }

    /**
	 * <p>This method builds and returns the list of control parameter names.</p>
	 *
	 * @return the list of control parameter names
	 * @since Eaasy Street 2.8
	 */
    private static List getControlParameterNames() {
        List parmNames = new ArrayList();
        parmNames.add("configurationURL");
        parmNames.add("contentType");
        parmNames.add("debugMode");
        parmNames.add("mailBc");
        parmNames.add("mailCc");
        parmNames.add("mailFrom");
        parmNames.add("mailTemplate");
        parmNames.add("mailTemplateFile");
        parmNames.add("mailTemplateFullyResolved");
        parmNames.add("mailTemplateName");
        parmNames.add("mailTemplateURL");
        parmNames.add("mailTo");
        parmNames.add("responseTemplate");
        parmNames.add("responseTemplateFile");
        parmNames.add("responseTemplateFullyResolved");
        parmNames.add("responseTemplateName");
        parmNames.add("responseTemplateURL");
        parmNames.add("sqlStatement");
        parmNames.add("subject");
        parmNames.add("useAlternateDataSource");
        return parmNames;
    }

    /**
	 * <p>This method builds and returns the list of multiple value control
	 * parameter names (parameters than can have more than one value).</p>
	 *
	 * @return the list of multiple value control parameter names
	 * @since Eaasy Street 2.8
	 */
    private static List getMultiValueControlParameterNames() {
        List parmNames = new ArrayList();
        parmNames.add("mailFrom");
        parmNames.add("mailTo");
        parmNames.add("mailCc");
        parmNames.add("mailBc");
        parmNames.add("subject");
        parmNames.add("contentType");
        parmNames.add("mailTemplate");
        parmNames.add("mailTemplateURL");
        parmNames.add("mailTemplateFile");
        parmNames.add("mailTemplateName");
        parmNames.add("mailTemplateFullyResolved");
        return parmNames;
    }
}
