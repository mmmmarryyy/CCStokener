package com.swing.services;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.servlet.http.Cookie;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import com.swing.json.JSON;
import com.swing.json.JSONList;
import com.swing.json.JSONObject;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.model.CompanyModel;
import com.liferay.portal.model.User;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.LayoutServiceUtil;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.RoleLocalServiceUtil;
import org.apache.log4j.Logger;

/**
 * This class provides basic tools to access Swing services.
 * It is also an abstract layer linking a Liferay user to a SwingService user.
 */
public class SwingServicesDelegate implements ISwingServices {

    private HashMap<String, String> labels;

    private String jsessionId;

    private String swsessionId;

    private String namespace;

    private String chartId;

    private String language;

    private int languageId;

    private String portalId;

    private int userId;

    private String swingQueryPath;

    private boolean isPost;

    protected HashMap<String, String> values;

    public static final String ACTION_ADD = "add";

    public static final String ACTION_DEL = "del";

    public static final String ACTION_UPD = "upd";

    public static final String QUERY_USER = "User";

    public static final String QUERY_CHART = "Chart";

    public static final String QUERY_REPORT = "Report";

    public static final String QUERY_ANALYSIS = "Analysis";

    private static Logger logger = Logger.getLogger(SwingServicesDelegate.class);

    public SwingServicesDelegate() {
        labels = new HashMap();
        jsessionId = null;
        swsessionId = null;
        namespace = "_";
        chartId = null;
        portalId = null;
        language = null;
        languageId = -1;
        userId = -1;
        swingQueryPath = null;
        values = null;
        isPost = false;
    }

    public HashMap getServletParameters() {
        return values;
    }

    /**
     * Return current sessionId (Liferay session)
     */
    public String getJSessionId() {
        return jsessionId;
    }

    /**
     *
     * @return the Swing service session information.
     */
    public String getServiceSessionId() {
        return swsessionId;
    }

    /**
     *
     * @return the Liferay namespace defined in this object for the portlet.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     *
     * @return the current chart_id given the namespace.
     */
    public String getChart_id() {
        return chartId;
    }

    /**
     *
     * @return the liferay language identifier in this object
     */
    public String getLanguage() {
        return language;
    }

    /**
     *
     * @return the current SwingService language id in this object
     */
    public int getLanguageId() {
        return languageId;
    }

    /**
     *
     * This method must be changed to return the actual portlet id, not the constant 1-
     *
     * @return the SwingService portlet identifier
     */
    public String getPortletId() {
        return "1";
    }

    /**
     *
     * @return the SwingService user defined in this object.
     */
    public int getUserId() {
        return userId;
    }

    /**
     *
     * @return the portal's user ID
     */
    public String getPortalId() {
        return portalId;
    }

    /**
     * Return the full path to Swing Service.
     */
    public String getSwingQueryPath() {
        return swingQueryPath;
    }

    /**
     * extracts a label given a key and a namespace
     * @param key   the label id.
     * @param ns    the namespace
     * @return  the label for the given namespace, or "..." if nothing is found.
     */
    public String getLabel(String key, String ns) {
        if (labels.size() > 0) {
            return labels.get(key + ns);
        }
        return "<n/a>";
    }

    public void init(HttpServletRequest request, String context) throws IOException {
        String configurationFile = System.getProperty("system.root.path") + "/conf/dashboard.properties";
        logger.debug("Loading file: " + configurationFile);
        Properties properties = new Properties();
        properties.load(new FileInputStream(configurationFile));
        swingQueryPath = properties.getProperty("SwingServerUrl") + properties.getProperty("SwingQueryPath");
        String portletLanguage = LanguageUtil.getLanguage().getLanguageId(request);
        isPost = request.getMethod().equals("POST");
        values = getParametersMap(request);
        namespace = values.get("namespace");
        if (namespace != null && !namespace.isEmpty()) {
            chartId = "chart_" + namespace + "_chart1";
        } else {
            chartId = "";
            namespace = "_";
        }
        HttpSession httpSession = request.getSession();
        if (httpSession != null) {
            jsessionId = httpSession.getId();
            Object co = httpSession.getAttribute(jsessionId);
            if (co != null) {
                swsessionId = co.toString();
            }
        }
        if (jsessionId != null) {
            if (swsessionId == null) {
                language = portletLanguage;
                portalId = getUserId(request.getCookies());
                initSwingSession(context);
                storeDataToServletSession(request);
            } else {
                loadDataFromServletSession(request);
                if (!getLanguage().equalsIgnoreCase(portletLanguage)) {
                    try {
                        String url = SwingUtils.combineUrl(getSwingQueryPath(), "query=Language&method=getLanguage_id&render=html&SWparam1=" + portletLanguage + "&namespace=" + getNamespace());
                        String language_id = _doGet(url);
                        language = portletLanguage;
                        languageId = Integer.valueOf(language_id);
                        loadLabels(context);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    storeDataToServletSession(request);
                }
            }
        }
    }

    public void init(PortletRequest request, String aNamespace, String context) throws IOException {
        String configurationFile = System.getProperty("system.root.path") + "/conf/dashboard.properties";
        logger.debug("Loading file: " + configurationFile);
        Properties properties = new Properties();
        properties.load(new FileInputStream(configurationFile));
        swingQueryPath = properties.getProperty("SwingServerUrl") + properties.getProperty("SwingQueryPath");
        String portletLanguage = LanguageUtil.getLanguage().getLanguageId(request);
        namespace = aNamespace;
        if (namespace != null && !namespace.isEmpty()) {
            chartId = "chart_" + namespace + "_chart1";
        } else {
            chartId = "";
            namespace = "_";
        }
        values = new HashMap();
        for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
            String name = (String) e.nextElement();
            values.put(name, request.getParameter(name));
        }
        PortletSession portletSession = request.getPortletSession();
        if (portletSession != null) {
            jsessionId = portletSession.getId();
            Object co = portletSession.getAttribute(jsessionId, PortletSession.APPLICATION_SCOPE);
            if (co != null) {
                swsessionId = co.toString();
            }
        }
        if (jsessionId != null) {
            if (swsessionId == null) {
                language = portletLanguage;
                portalId = getUserId(request.getCookies());
                initSwingSession(context);
                storeDataToPortletSession(request);
            } else {
                loadDataFromPortletSession(request);
                if (!getLanguage().equalsIgnoreCase(portletLanguage)) {
                    try {
                        String url = SwingUtils.combineUrl(getSwingQueryPath(), "query=Language&method=getLanguage_id&render=html&SWparam1=" + portletLanguage + "&namespace=" + getNamespace());
                        String language_id = _doGet(url);
                        language = portletLanguage;
                        languageId = Integer.valueOf(language_id);
                        loadLabels(context);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    storeDataToPortletSession(request);
                }
            }
        }
    }

    private void initSwingSession(String context) {
        String url = SwingUtils.combineUrl(getSwingQueryPath(), "query=Session" + "&method=getSession" + "&render=html" + "&namespace=" + getNamespace() + "&JSESSIONID=" + jsessionId + "&language=" + getLanguage() + "&portal_id=" + getPortalId());
        try {
            URL sessionUrl = new URL(url);
            URLConnection connection = sessionUrl.openConnection();
            connection.connect();
            String inputLine = "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuffer response = new StringBuffer(1000);
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            userId = -1;
            JSON jsonResponse = JSON.parse(response.toString());
            if (jsonResponse instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) jsonResponse;
                if (jsonObject.contains("SWSESSIONID")) {
                    swsessionId = jsonObject.get("SWSESSIONID").toString();
                }
                if (jsonObject.contains("user_id")) {
                    userId = (Integer) jsonObject.get("user_id");
                }
                if (jsonObject.contains("language_id")) {
                    languageId = Integer.valueOf(jsonObject.get("language_id").toString());
                }
            }
            reader.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        loadLabels(context);
    }

    /**
     * Fills the list of labels
     * @param key   the label id.
     * @param ns    the namespace
     * @return  the label for the given namespace, or ... if nothing is found.
     */
    public void loadLabels(String context) {
        String url = SwingUtils.combineUrl(getSwingQueryPath(), "query=Label&method=getLabelList&render=json&SWparam1=" + context);
        try {
            String label = _doGet(url);
            JSONObject o = (JSONObject) JSON.parse(label);
            if (o != null) {
                JSONList l = (JSONList) o.get("row");
                labels.clear();
                if (l != null) {
                    for (int i = 0; i < l.size(); i++) {
                        JSONObject obj = (JSONObject) l.get(i);
                        labels.put(obj.get("code").toString() + obj.get("namespace").toString(), obj.get("label").toString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Calls a SwingService url (or in fact any other one...)
     * If the SwingServicesDelegate (this) was created from a post request, and the parameters should
     * be appended, the request is posted. In all other cases, it is sent by a get.
     *
     * @return  the content of the url, or an error JSON in case of a problem.
     */
    public String executeSwingRequest() {
        String url = getSwingQueryPath();
        String jsCallback = (String) values.get("callback");
        try {
            JSONObject jsonObject = null;
            String paramRow = values.get("row");
            if (paramRow != null) {
                jsonObject = (JSONObject) JSON.parse(paramRow);
            }
            int pp = 0;
            String query = values.get("query");
            if (query != null && query.equals(QUERY_USER)) {
                if (jsonObject != null) {
                    String paramAction = jsonObject.getString("action");
                    if (paramAction == null) {
                        paramAction = "";
                    }
                    boolean isUserAdmin = jsonObject.getInt("id") == 1;
                    if (isUserAdmin) {
                        if (paramAction.equals(ACTION_DEL)) {
                            throw new Exception("Master admin user cannot be deleted");
                        } else if (paramAction.equals(ACTION_UPD)) {
                            jsonObject.put("isadmin", 1);
                        }
                    }
                    onUserAction(jsonObject, paramAction);
                    pp = 1;
                }
            } else if (query != null && (query.equals(QUERY_CHART) || query.equals(QUERY_REPORT) || query.equals(QUERY_ANALYSIS))) {
                String paramMethod = values.get("method");
                if (paramMethod != null && (paramMethod.startsWith("update") || paramMethod.startsWith("create"))) {
                    String actionAdd = ACTION_ADD;
                    if (paramMethod.startsWith("update")) {
                        actionAdd = ACTION_UPD;
                        pp = 3;
                    } else {
                        pp = 2;
                    }
                    String ons = values.get("namespace");
                    onPortletAction(jsonObject, actionAdd, query);
                    if (query.equals(QUERY_ANALYSIS)) {
                        values.put("SWparam1", values.get("namespace"));
                        values.put("namespace", ons);
                    }
                }
            }
            if (pp != 0) {
                values.put("row", jsonObject.toString());
            }
            String paramString = getParametersString();
            String result = null;
            if (isPost) {
                result = _doPost(url);
            } else {
                url = SwingUtils.combineUrl(url, paramString);
                result = _doGet(url);
            }
            if (pp != 0) {
                String aResult = result;
                if (jsCallback != null && jsCallback.length() > 0 && aResult.startsWith(jsCallback)) {
                    aResult = aResult.substring(jsCallback.length() + 1, aResult.length() - 1);
                }
                jsonObject = null;
                try {
                    jsonObject = (JSONObject) JSON.parse(aResult);
                } catch (Exception e) {
                }
                if (jsonObject != null && !jsonObject.getBoolean("success")) {
                    String paramAction = jsonObject.getString("action");
                    if (paramAction == null) {
                        paramAction = "";
                    }
                    if (pp == 2 || pp == 3) {
                        String mtd = ACTION_ADD;
                        if (pp == 3) {
                            mtd = ACTION_UPD;
                        }
                        onPortletActionError(jsonObject, mtd);
                    } else if (pp == 1) {
                        onUserActionError(jsonObject, paramAction);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            JSONObject jsonError = new JSONObject("success", false);
            jsonError.put("errors", new JSONObject());
            jsonError.put("_msg", e.getLocalizedMessage());
            String output = jsonError.toString();
            if (jsCallback != null && jsCallback.length() > 0) {
                output = jsCallback + "( " + output + " )";
            }
            return output;
        }
    }

    /**
     * Function provides remote call with GET method
     * The Liferay and SwingService cookies are passed.
     *
     *  This method should probably be improved to take an optional outputStream. If present, the result could be written directly.
     *
     * @param url the url from where to retrieve data.
     * @return the content of the url, or an empty string in case of problems.
     */
    protected String _doGet(String url) throws IOException {
        if (userId == -1) throw new IOException("User not logged in");
        url = SwingUtils.combineUrl(url, "portlet_id=" + namespace);
        URL urlGet = new URL(url);
        URLConnection connection = urlGet.openConnection();
        connection.setRequestProperty("Cookie", "JSESSIONID=" + swsessionId);
        connection.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine = "";
        StringBuffer stringBuffer = new StringBuffer(1000);
        while ((inputLine = reader.readLine()) != null) {
            stringBuffer.append(inputLine);
        }
        reader.close();
        return stringBuffer.toString();
    }

    /**
     * Function provides remote call with POST method
     * The Liferay and SwingService cookies are passed.
     *
     * @param url
     * @return
     */
    protected String _doPost(String url) throws IOException {
        if (userId == -1) throw new IOException("User not logged in");
        url = SwingUtils.combineUrl(url, "portlet_id=" + namespace);
        URL urlPost = new URL(url);
        URLConnection connection = urlPost.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Cookie", "JSESSIONID=" + swsessionId);
        connection.connect();
        OutputStream outputStream = null;
        try {
            outputStream = connection.getOutputStream();
            outputStream.write(getParametersString().getBytes());
        } finally {
            if (outputStream != null) {
                outputStream.flush();
                outputStream.close();
            }
        }
        InputStream inputStream = null;
        StringBuffer stringBuffer = new StringBuffer(1000);
        try {
            inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String inputLine = "";
            while ((inputLine = reader.readLine()) != null) {
                stringBuffer.append(inputLine);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return stringBuffer.toString();
    }

    /**
     *
     * Build the SwingService from a portlet request (Liferay).
     */
    private void loadDataFromPortletSession(PortletRequest renderRequest) {
        PortletSession session = renderRequest.getPortletSession();
        jsessionId = (String) session.getAttribute("JSESSIONID", PortletSession.APPLICATION_SCOPE);
        language = session.getAttribute("language", PortletSession.APPLICATION_SCOPE).toString();
        labels = (HashMap) session.getAttribute("label", PortletSession.APPLICATION_SCOPE);
        portalId = (String) session.getAttribute("portalId", PortletSession.APPLICATION_SCOPE);
        swsessionId = (String) session.getAttribute("SWSESSIONID", PortletSession.APPLICATION_SCOPE);
        userId = (Integer) session.getAttribute("user_id", PortletSession.APPLICATION_SCOPE);
        languageId = Integer.valueOf(session.getAttribute("language_id", PortletSession.APPLICATION_SCOPE).toString());
    }

    /**
     *
     * This method stores all relevant informations into a portlet session (Liferay).
     *
     * @param renderRequest the portlet request to fill.
     */
    public void storeDataToPortletSession(PortletRequest renderRequest) {
        PortletSession session = renderRequest.getPortletSession();
        session.setAttribute("JSESSIONID", getJSessionId(), PortletSession.APPLICATION_SCOPE);
        session.setAttribute(jsessionId, getServiceSessionId(), PortletSession.APPLICATION_SCOPE);
        session.setAttribute("language", getLanguage(), PortletSession.APPLICATION_SCOPE);
        session.setAttribute("label", labels, PortletSession.APPLICATION_SCOPE);
        session.setAttribute("portalId", portalId, PortletSession.APPLICATION_SCOPE);
        session.setAttribute("SWSESSIONID", getServiceSessionId(), PortletSession.APPLICATION_SCOPE);
        session.setAttribute("user_id", getUserId(), PortletSession.APPLICATION_SCOPE);
        session.setAttribute("language_id", getLanguageId(), PortletSession.APPLICATION_SCOPE);
        session.setAttribute("portlet_id", getPortletId(), PortletSession.APPLICATION_SCOPE);
    }

    /**
     * Read all relevant information from a servlet session (Swing)
     */
    private void loadDataFromServletSession(HttpServletRequest renderRequest) {
        HttpSession session = renderRequest.getSession();
        jsessionId = (String) session.getAttribute("JSESSIONID");
        language = session.getAttribute("language").toString();
        labels = (HashMap) session.getAttribute("label");
        portalId = (String) session.getAttribute("portalId");
        swsessionId = (String) session.getAttribute("SWSESSIONID");
        userId = (Integer) session.getAttribute("user_id");
        languageId = Integer.valueOf(session.getAttribute("language_id").toString());
    }

    /**
     * Writes all relevant information to a serlvet session (Swing)
     */
    public void storeDataToServletSession(HttpServletRequest renderRequest) {
        HttpSession session = renderRequest.getSession();
        session.setAttribute("JSESSIONID", getJSessionId());
        session.setAttribute(jsessionId, getJSessionId());
        session.setAttribute("language", getLanguage());
        session.setAttribute("label", labels);
        session.setAttribute("portalId", portalId);
        session.setAttribute("SWSESSIONID", getServiceSessionId());
        session.setAttribute("user_id", getUserId());
        session.setAttribute("language_id", getLanguageId());
        session.setAttribute("portlet_id", getPortletId());
    }

    protected String getParametersString() throws UnsupportedEncodingException {
        boolean isFirst = true;
        StringBuffer stringBuffer = new StringBuffer(1000);
        Iterator it = values.keySet().iterator();
        while (it.hasNext()) {
            String str = (String) it.next();
            Object o = values.get(str);
            if (str == null || o == null) {
                continue;
            }
            if (!isFirst) {
                stringBuffer.append("&");
            }
            stringBuffer.append(str);
            stringBuffer.append("=");
            String s = null;
            if (o instanceof byte[]) {
                s = new String((byte[]) o);
            } else {
                s = o.toString();
            }
            stringBuffer.append(URLEncoder.encode(s, "utf-8"));
            isFirst = false;
        }
        return stringBuffer.toString();
    }

    /**
     * Transforms request parameters into a HashMap.
     * If the request is Multipart, it reads the items as multipart. If one of them is a file
     * then two entries are stored, the parameter name holding the file content and
     * the [parameter name]+'name' holding the file name.
     */
    private HashMap getParametersMap(HttpServletRequest request) {
        HashMap parameterMap = new HashMap();
        if (ServletFileUpload.isMultipartContent(request)) {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(100000);
            factory.setRepository(factory.getRepository());
            try {
                ServletFileUpload upload = new ServletFileUpload(factory);
                List items = upload.parseRequest(request);
                Iterator itemIterator = items.iterator();
                while (itemIterator.hasNext()) {
                    FileItem fileItem = (FileItem) itemIterator.next();
                    if (fileItem.isFormField()) {
                        parameterMap.put(fileItem.getFieldName(), fileItem.getString());
                    } else {
                        parameterMap.put(fileItem.getFieldName() + "name", fileItem.getName());
                        parameterMap.put(fileItem.getFieldName(), fileItem.get());
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
                String sx = (String) e.nextElement();
                parameterMap.put(sx, request.getParameter(sx));
            }
        }
        return parameterMap;
    }

    private String getUserId(Cookie[] cookies) {
        String uid = null;
        String pwd = null;
        String comp = null;
        for (int i = 0; i < cookies.length; i++) {
            if ("COMPANY_ID".equals(cookies[i].getName())) {
                comp = cookies[i].getValue();
            } else if ("ID".equals(cookies[i].getName())) {
                uid = SwingUtils.toAsciiString(cookies[i].getValue());
            } else if ("PASSWORD".equals(cookies[i].getName())) {
                pwd = SwingUtils.toAsciiString(cookies[i].getValue());
            }
        }
        try {
            KeyValuePair kvp = UserLocalServiceUtil.decryptUserId(Long.parseLong(comp), uid, pwd);
            return kvp.getKey();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    protected void onUserAction(JSONObject jsonObject, String action) throws Exception {
        long creatorUserId = 0;
        long companyId = 0;
        String openId = "";
        Locale locale = Locale.US;
        int prefixId = 0;
        int suffixId = 0;
        boolean male = true;
        int birthdayMonth = Calendar.JANUARY;
        int birthdayDay = 1;
        int birthdayYear = 1970;
        String jobTitle = "";
        long[] groupIds = null;
        long[] organizationIds = null;
        long[] roleIds = null;
        long[] userGroupIds = null;
        String lastName = jsonObject.getString("username");
        String middleName = "";
        String firstName = jsonObject.getString("firstname");
        String screenName = jsonObject.getString("loginame");
        String emailAddress = jsonObject.getString("email");
        String password = jsonObject.getString("password");
        String admin = jsonObject.getString("isadmin");
        boolean isAdmin = admin != null && admin.equals("1");
        System.out.println(("User is admin = " + admin + ", " + isAdmin));
        if (emailAddress == null) {
            emailAddress = "";
        }
        ServiceContext serviceContext = new ServiceContext();
        if (jsonObject.contains("language_id")) {
            int langId = jsonObject.getInt("language_id");
            String queryLanguage = SwingUtils.combineUrl(getSwingQueryPath(), "query=Language&method=getLanguage_locale&render=json&SWparam1=" + langId);
            String res = _doGet(queryLanguage);
            langId = res.indexOf("_");
            if (langId > 0) {
                locale = new Locale(res.substring(0, langId), res.substring(langId + 1));
            }
        }
        if (action.equals(ACTION_ADD)) {
            List l = CompanyLocalServiceUtil.getCompanies();
            if (l.size() >= 1) {
                companyId = ((CompanyModel) l.get(0)).getCompanyId();
            }
            User user = UserLocalServiceUtil.addUser(creatorUserId, companyId, false, password, password, false, screenName, emailAddress, openId, locale, firstName, middleName, lastName, prefixId, suffixId, male, birthdayMonth, birthdayDay, birthdayYear, jobTitle, groupIds, organizationIds, roleIds, userGroupIds, false, serviceContext);
            jsonObject.put("portal_id", "" + user.getUserId());
            RoleLocalServiceUtil.addUserRoles(user.getUserId(), (isAdmin ? new long[] { 10113, 10116 } : new long[] { 10113 }));
        } else if (action.equals(ACTION_UPD)) {
            long uid = Long.valueOf("" + jsonObject.get("portal_id")).longValue();
            User user = UserLocalServiceUtil.getUser(uid);
            List l = user.getUserGroups();
            String oPass = user.getPassword();
            for (int i = 0; i < 2; i++) {
                if (i == 1) {
                    password = null;
                }
                try {
                    UserLocalServiceUtil.updateUser(uid, oPass, password, password, false, user.getReminderQueryQuestion(), user.getReminderQueryAnswer(), screenName, emailAddress, openId, locale.toString(), user.getTimeZoneId(), user.getGreeting(), user.getComments(), firstName, middleName, lastName, prefixId, suffixId, user.isMale(), birthdayMonth, birthdayDay, birthdayYear, "", "", "", "", "", "", "", "", "", "", jobTitle, groupIds, organizationIds, roleIds, l, userGroupIds, serviceContext);
                    user = UserLocalServiceUtil.getUser(uid);
                    if (isAdmin) {
                        RoleLocalServiceUtil.addUserRoles(uid, new long[] { 10116 });
                    } else {
                        RoleLocalServiceUtil.unsetUserRoles(uid, new long[] { 10116 });
                    }
                    jsonObject.put("loginame", user.getLogin());
                    jsonObject.put("username", user.getLastName());
                    jsonObject.put("firstname", user.getFirstName());
                    jsonObject.put("email", user.getEmailAddress());
                    break;
                } catch (Exception e) {
                    if (i == 1) {
                        throw e;
                    }
                }
            }
        } else if (action.equals(ACTION_DEL)) {
            long uid = Long.valueOf("" + jsonObject.get("portal_id")).longValue();
            UserLocalServiceUtil.deleteUser(uid);
        } else {
            throw new Exception("unknown command " + action);
        }
    }

    private void onPortletAction(JSONObject row, String action, String qry) throws Exception {
        String portletId = "SwingDashboard_WAR_SwingDashboard";
        if (qry.equals(QUERY_REPORT)) {
            portletId = "SwingReport_WAR_SwingReport";
        }
        String caption = row.getString("caption");
        if (qry.equals(QUERY_ANALYSIS)) {
            caption = row.getString("name");
        }
        String targetColumn = "column-1";
        long plid = Long.valueOf(values.get("plid")).longValue();
        long userId = Long.valueOf(getPortalId());
        User usr = UserLocalServiceUtil.getUser(userId);
        Layout layout = LayoutLocalServiceUtil.getLayout(plid);
        LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet) layout.getLayoutType();
        layoutTypePortlet.resetStates();
        String portletIdInc = values.get("namespace");
        if (!action.equals(ACTION_ADD)) {
            portletIdInc = portletIdInc.substring(1, portletIdInc.length() - 1);
        }
        if (action.equals(ACTION_ADD)) {
            portletIdInc = layoutTypePortlet.addPortletId(userId, portletId, targetColumn, 1, false);
            values.put("namespace", "_" + portletIdInc + "_");
        } else if (action.equals(ACTION_DEL)) {
            layoutTypePortlet.removePortletId(userId, portletIdInc);
        }
        try {
            if (!action.equals(ACTION_DEL)) {
                PortletPreferences portletSetup = PortletPreferencesFactoryUtil.getLayoutPortletSetup(layout, portletIdInc);
                portletSetup.setValue("portlet-setup-title-" + usr.getLocale(), caption);
                portletSetup.setValue("portlet-setup-use-custom-title", String.valueOf(true));
                portletSetup.store();
            }
            LayoutLocalServiceUtil.updateLayout(layout.getGroupId(), layout.isPrivateLayout(), layout.getLayoutId(), layout.getTypeSettings());
        } catch (Exception e) {
            if (action.equals(ACTION_ADD)) {
                layoutTypePortlet.removePortletId(userId, portletIdInc);
                throw new Exception("Unable to add portlet, please retry ");
            } else {
                throw new Exception("Unable to perform action");
            }
        }
    }

    private void onUserActionError(JSONObject row, String action) {
        if (action.equals(ACTION_ADD)) {
            try {
                long uid = Long.valueOf("" + row.get("portal_id")).longValue();
                UserLocalServiceUtil.deleteUser(uid);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void onPortletActionError(JSONObject row, String action) {
        if (action.equals(ACTION_ADD)) {
            try {
                String sx = values.get("namespace");
                sx = sx.substring(1, sx.length() - 1);
                long plid = Long.valueOf(values.get("plid")).longValue();
                long userId = Long.valueOf(values.get("userid")).longValue();
                Layout layout = LayoutLocalServiceUtil.getLayout(plid);
                LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet) layout.getLayoutType();
                layoutTypePortlet.resetStates();
                layoutTypePortlet.removePortletId(userId, sx);
                LayoutServiceUtil.updateLayout(layout.getGroupId(), layout.isPrivateLayout(), layout.getLayoutId(), layout.getTypeSettings());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
