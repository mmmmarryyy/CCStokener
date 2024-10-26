package org.jaffa.presentation.portlet.widgets.taglib;

import org.apache.log4j.Logger;
import java.util.*;
import javax.servlet.jsp.JspException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;
import org.apache.struts.taglib.TagUtils;
import org.jaffa.presentation.portlet.session.UserSession;
import org.jaffa.presentation.portlet.FormBase;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.jaffa.presentation.portlet.FormKey;
import org.jaffa.config.Config;
import java.net.*;
import java.io.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import org.jaffa.security.SecurityTag;
import org.jaffa.datatypes.DateTime;
import javax.servlet.jsp.tagext.TryCatchFinally;
import javax.servlet.jsp.tagext.Tag;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.util.RequestUtils;
import org.apache.struts.taglib.html.Constants;
import org.jaffa.presentation.portlet.component.Component;
import org.jaffa.session.ContextManagerFactory;
import org.jaffa.util.SplitString;
import org.jaffa.util.StringHelper;

/** Tag Handler for the Form tag.*/
public class FormTag extends org.apache.struts.taglib.html.FormTag implements TryCatchFinally, ICustomTag {

    private static Logger log = Logger.getLogger(FormTag.class);

    protected String NAME = StringHelper.getShortClassName(FormTag.class);

    /** Constant for the parameter componentId.*/
    public static final String PARAMETER_COMPONENT_ID = "componentId";

    /** Constant for the parameter eventId.*/
    public static final String PARAMETER_EVENT_ID = "eventId";

    /** Constant for the parameter eventId.*/
    public static final String PARAMETER_DATESTAMP_ID = "dateTimeStamp";

    /** Suffix used for the entire page.*/
    public static final String ENTIRE_PAGE_SUFFIX = "_EntirePage";

    /** Suffix used for the push page.*/
    public static final String PUSH_PAGE_SUFFIX = "_PushPage";

    private static final Map c_guardedHtmlCache = new HashMap();

    /** property declaration for tag attribute: url.
     */
    private String m_url;

    private DateTime m_dateTime;

    /**
     * Holds value of property useBean. Used along with 'url' as an alternate to 'action'
     */
    private String m_useBean;

    /** property declaration for tag attribute: guardedHtmlLocation.
     * This will be the location of a Html fragment, relative to the classpath
     */
    private String m_guardedHtmlLocation;

    /** A boolean which indicates if any guarded-button is painted on the JSP */
    private boolean m_guardedButtonPresent;

    /** Flag to indicate if the security context has been set yet */
    private boolean m_securityContextSet = false;

    private HashMap m_headerCache = null;

    private HashMap m_footerCache = null;

    private static final String ID_FORMAT_FORMNAME = "formname";

    private static final String ID_FORMAT_CLASS = "class";

    private static final String ID_FORMAT_NONE = "none";

    private static final String ID_FORMAT_INDEX = "index";

    private static final String FORM_TAG_INDEX = FormTag.class.getName() + ".index";

    private String m_idFormat = (String) ContextManagerFactory.instance().getProperty("jaffa.widgets.form.idFormat");

    /** Name used by the <form name=""> tag */
    private String m_htmlName = null;

    /** Id used by the <form id=""> tag */
    private String m_htmlIdPrefix = null;

    /** Value of previous 'form' attribute */
    private Object m_oldForm = null;

    /** Default constructor. */
    public FormTag() {
        super();
        initTag();
    }

    /** Reset any valued cached by the tag on construction or re-use */
    private void initTag() {
        m_securityContextSet = false;
        m_url = null;
        m_dateTime = new DateTime();
        m_guardedHtmlLocation = null;
        m_guardedButtonPresent = false;
        m_headerCache = new HashMap();
        m_footerCache = new HashMap();
        m_htmlName = null;
        m_htmlIdPrefix = null;
        m_oldForm = null;
    }

    /** Register a guardedButton widget painted inside the Form */
    public void registerGuardedButton() {
        m_guardedButtonPresent = true;
    }

    /**
     * This method sets the component, componentId, tokenError(if found) on the FormBean
     * It also invokes the initForm() method of the FormBean
     *
     * This has been extended so that is now set the security context for the thread
     * of execution within the <xxx:form> tag.
     */
    private void doStartTagExt() throws JspException {
        try {
            Object f = pageContext.findAttribute(getBeanName());
            if (log.isDebugEnabled()) log.debug(this.NAME + ".doStartTagExt: Started FORM tag for bean " + getBeanName() + " id=" + getHtmlIdPrefix());
            ServletRequest request = pageContext.getRequest();
            if (!(request instanceof HttpServletRequest)) {
                log.warn("Security Problem, Can't Set Thread Context Since Request is not HTTP");
            } else {
                try {
                    SecurityTag.setThreadContext((HttpServletRequest) request);
                    m_securityContextSet = true;
                } catch (SecurityException e) {
                    log.error(this.NAME + ".doStartTagExt: Tag Failed, Could Not Set Security Context", e);
                    throw new JspException("Can't Set Security Context");
                }
            }
            if (f instanceof FormBase) {
                FormBase form = (FormBase) f;
                UserSession us = UserSession.getUserSession((HttpServletRequest) request);
                FormKey fk = (FormKey) request.getAttribute(FormKey.class.getName());
                if (fk != null) {
                    String componentId = fk.getComponentId();
                    form.setComponent(us.getComponent(componentId));
                }
                if (log.isDebugEnabled()) log.debug(this.NAME + ".doStartTagExt: Invoking the initForm method of the Form");
                form.initForm();
            }
            pageContext.setAttribute(TagHelper.ATTRIBUTE_FORM_TAG, this, pageContext.REQUEST_SCOPE);
            pageContext.setAttribute(TagHelper.ATTRIBUTE_FORM_BASE, f, pageContext.REQUEST_SCOPE);
            pageContext.setAttribute(TagHelper.ATTRIBUTE_FORM_NAME, getBeanName(), pageContext.REQUEST_SCOPE);
            pageContext.setAttribute(TagHelper.ATTRIBUTE_ENCLOSED, Boolean.FALSE, pageContext.REQUEST_SCOPE);
            pageContext.setAttribute(TagHelper.ATTRIBUTE_ID_PREFIX, getHtmlIdPrefix(), pageContext.REQUEST_SCOPE);
            pageContext.setAttribute(TagHelper.ATTRIBUTE_EVENT_PREFIX, "", pageContext.REQUEST_SCOPE);
            m_oldForm = pageContext.findAttribute(TagHelper.ATTRIBUTE_EL_FORM);
            pageContext.setAttribute(TagHelper.ATTRIBUTE_EL_FORM, f, pageContext.REQUEST_SCOPE);
            JspWriter writer = pageContext.getOut();
            StringBuffer buf = new StringBuffer();
            buf.append("\n<span id='" + getHtmlIdPrefix() + ENTIRE_PAGE_SUFFIX + "' class='FormGuard'>");
            writer.println(buf.toString());
        } catch (IOException e) {
            throw new JspException("error in FormTag: " + e);
        }
    }

    /** This concludes the html of the Form tag.
     * @throws JspException if any error occurs.
     * @return EVAL_PAGE if the JSP engine should continue evaluating the JSP page, otherwise return SKIP_PAGE.
     */
    public int doEndTag() throws JspException {
        try {
            JspWriter writer = pageContext.getOut();
            doEndTagExt1(writer);
            int i = super.doEndTag();
            doEndTagExt2(writer);
            return i;
        } catch (IOException e) {
            throw new JspException("error in FormTag: " + e);
        } finally {
            CustomTag.popParent(this, pageContext);
        }
    }

    /** This method will write out the hidden-fields */
    private void doEndTagExt1(JspWriter writer) throws IOException {
        Object formObj = pageContext.findAttribute(getBeanName());
        if (formObj != null && formObj instanceof FormBase) {
            FormBase f = (FormBase) formObj;
            StringBuffer buf = new StringBuffer();
            buf.append("<input type='hidden' name='" + PARAMETER_COMPONENT_ID + "' value='" + (f.getComponent() == null ? "" : f.getComponent().getComponentId()) + "'>\n");
            buf.append("<input type='hidden' name='" + PARAMETER_EVENT_ID + "' value=''>\n");
            buf.append("<input type='hidden' id='" + PARAMETER_DATESTAMP_ID + "' value='" + m_dateTime.timeInMillis() + "'>\n");
            buf.append("</span>");
            writer.println(buf.toString());
        }
    }

    /** This method will write out all the header-info for the widgets in the form */
    private void doEndTagExt2(JspWriter writer) throws IOException {
        writer.println(determineGuardedHtml());
        for (Iterator iter = m_footerCache.keySet().iterator(); iter.hasNext(); ) {
            Object key = iter.next();
            Object javaScript = m_footerCache.get(key);
            if (javaScript != null) {
                writer.println(javaScript);
                if (log.isDebugEnabled()) log.debug("Write Footer Code For Widget " + key + "\n" + javaScript);
            }
        }
    }

    /** Getter for the attribute url.
     * @return Value of attribute url.
     */
    public String getUrl() {
        return m_url;
    }

    /** Setter for the attribute url.
     * @param value New value of attribute url.
     */
    public void setUrl(String value) {
        m_url = value;
    }

    /**
     * Getter for property useBean.
     * @return Value of property useBean.
     */
    public String getUseBean() {
        return m_useBean;
    }

    /**
     * Setter for property useBean. This can be used in conjunction with a 'url'.
     * This allows the FormTag to bypass using a struts action, and bind the form
     * to an already existing bean in the session. If this method is being used the
     * bean must already exist before FormTag. If you need to have the JSP create the
     * bean you should use a <jsp:useBean name='xxx' class='yyy' scope='request'/>
     * before the FormTag so the bean can be initialized and put into the desired scope.
     * @param useBean New value of property useBean.
     */
    public void setUseBean(String useBean) {
        m_useBean = useBean;
    }

    /** Getter for the attribute guardedHtmlLocation.
     * @return Value of attribute guardedHtmlLocation.
     */
    public String getGuardedHtmlLocation() {
        return m_guardedHtmlLocation;
    }

    /** Setter for the attribute guardedHtmlLocation.
     * @param value New value of attribute guardedHtmlLocation.
     */
    public void setGuardedHtmlLocation(String value) {
        m_guardedHtmlLocation = value;
    }

    private String determineGuardedHtml() {
        StringBuffer buf = new StringBuffer();
        if (m_guardedButtonPresent) {
            buf.append("\n<span id='" + getHtmlIdPrefix() + PUSH_PAGE_SUFFIX + "' style='display:none'>\n");
            String location = m_guardedHtmlLocation != null ? m_guardedHtmlLocation : (String) Config.getProperty(Config.PROP_PRESENTATION_DEFAULT_GUARDED_HTML_LOCATION);
            String html = (String) c_guardedHtmlCache.get(location);
            if (html == null) {
                if (log.isDebugEnabled()) log.debug(this.NAME + ".determineGuardedHtml: Reading the Guarded Html Fragment: " + location);
                URL url = getUrl(location);
                if (url != null) {
                    BufferedReader in = null;
                    try {
                        in = new BufferedReader(new InputStreamReader(url.openStream()));
                        StringBuffer buf1 = new StringBuffer();
                        String line = null;
                        while ((line = in.readLine()) != null) {
                            buf1.append(line);
                            buf1.append('\n');
                        }
                        html = buf1.toString();
                    } catch (IOException e) {
                        log.warn(this.NAME + ".determineGuardedHtml: Failed to read the Guarded Html Fragment: " + location, e);
                    } finally {
                        try {
                            if (in != null) in.close();
                        } catch (IOException ex) {
                            log.warn(this.NAME + ".determineGuardedHtml: Failed to close the Guarded Html Fragment: " + location, ex);
                        }
                    }
                } else {
                    log.warn("Failed to read the Guarded Html Fragment: " + location);
                }
                if (html == null) html = "Transaction in Progress";
                c_guardedHtmlCache.put(location, html);
            }
            buf.append(html);
            buf.append("\n</span>\n");
        }
        return buf.toString();
    }

    private URL getUrl(String fileName) {
        ClassLoader cl = this.getClass().getClassLoader();
        URL url = cl.getResource(fileName);
        if (url == null) url = ClassLoader.getSystemResource(fileName);
        return url;
    }

    /** Invoked if a Throwable occurs while evaluating the BODY inside a tag or in any of the following methods: Tag.doStartTag(), Tag.doEndTag(), IterationTag.doAfterBody() and BodyTag.doInitBody().
     * This method is not invoked if the Throwable occurs during one of the setter methods.
     * This method will merely re-throw the input Throwable.
     * @param t The throwable exception navigating through this tag
     * @throws Throwable The throwable exception navigating through this tag.
     */
    public void doCatch(Throwable t) throws Throwable {
        throw t;
    }

    /** Invoked in all cases after doEndTag() for any class implementing Tag, IterationTag or BodyTag. This method is invoked even if an exception has occurred in the BODY of the tag, or in any of the following methods: Tag.doStartTag(), Tag.doEndTag(), IterationTag.doAfterBody() and BodyTag.doInitBody().
     * This method is not invoked if the Throwable occurs during one of the setter methods.
     * This will reset the internal fields, so that the Tag can be re-used.
     */
    public void doFinally() {
        if (m_securityContextSet) {
            ServletRequest request = pageContext.getRequest();
            if (!(request instanceof HttpServletRequest)) {
                log.warn("Security Problem, Can't Set Thread Context Since Request is not HTTP");
            } else {
                try {
                    SecurityTag.unsetThreadContext((HttpServletRequest) request);
                    m_securityContextSet = false;
                } catch (SecurityException e) {
                    log.warn("Tag Failed, Could Not Un-Set Security Context", e);
                }
            }
        }
        if (m_oldForm == null) pageContext.removeAttribute(TagHelper.ATTRIBUTE_EL_FORM); else pageContext.setAttribute(TagHelper.ATTRIBUTE_EL_FORM, m_oldForm, pageContext.REQUEST_SCOPE);
        initTag();
        pageContext.removeAttribute(TagHelper.ATTRIBUTE_FORM_TAG);
        pageContext.removeAttribute(TagHelper.ATTRIBUTE_FORM_NAME);
        pageContext.removeAttribute(TagHelper.ATTRIBUTE_FORM_BASE);
        pageContext.removeAttribute(TagHelper.ATTRIBUTE_ENCLOSED);
        pageContext.removeAttribute(TagHelper.ATTRIBUTE_ID_PREFIX);
        pageContext.removeAttribute(TagHelper.ATTRIBUTE_EVENT_PREFIX);
    }

    /**
    * This method implements the register method in the ICustomTag interface
    * @param tag
    */
    public void register(ICustomTag tag) {
        if (log.isDebugEnabled()) log.debug(this.NAME + ".register: tag=" + tag);
        ICustomTag icmtag = (ICustomTag) tag;
        String name = icmtag.getClass().getName();
        if (!m_headerCache.containsKey(name)) {
            m_headerCache.put(name, icmtag.getHeaderHtml());
            if (log.isDebugEnabled()) log.debug(this.NAME + ".register: added header html for widget " + name);
        }
        if (!m_footerCache.containsKey(name)) {
            m_footerCache.put(name, icmtag.getFooterHtml());
            if (log.isDebugEnabled()) log.debug(this.NAME + ".register: added footer html for widget " + name);
        }
    }

    /** The form has no parent
     * @return null
     */
    public ICustomTag getParentCustomTag() {
        return null;
    }

    /** The form has no parent, this does nothing
     */
    public void setParentCustomTag(ICustomTag jaffaParent) {
    }

    public String getHeaderHtml() {
        return null;
    }

    public String getFooterHtml() {
        return null;
    }

    /** Get the HTML id for the form. This is based on the application rule
     * "jaffa.widgets.form.idFormat" which can have the following values
     * <ul>
     * <li>none - No id will be used for the form
     * <li>formname - The struts form name will be used
     * <li>index - an index value, based on the number of forms on the page will be used.
     * The value will be prefixed with 'j', so the first form will be id='j0'
     * <li>class - Will used the classname of the formbean (without the package name) for the id
     * </ul>
     * Older version of JAFFA used the equivilent of 'formname' which is the default if nothing is set.
     */
    public String getHtmlIdPrefix() {
        if (m_htmlName != null) {
            m_htmlIdPrefix = m_htmlName;
        } else if (m_htmlIdPrefix == null) {
            if (ID_FORMAT_INDEX.equals(m_idFormat)) {
                Integer index = (Integer) pageContext.getAttribute(FORM_TAG_INDEX, PageContext.REQUEST_SCOPE);
                if (index == null) index = new Integer(1);
                m_htmlIdPrefix = "j" + index;
                pageContext.setAttribute(FORM_TAG_INDEX, new Integer(index.intValue() + 1), PageContext.REQUEST_SCOPE);
            } else if (ID_FORMAT_NONE.equals(m_idFormat)) m_htmlIdPrefix = ""; else if (ID_FORMAT_CLASS.equals(m_idFormat)) {
                m_htmlIdPrefix = StringHelper.getShortClassName(FormTag.class);
            } else {
                m_htmlIdPrefix = getBeanName();
            }
        }
        return m_htmlIdPrefix;
    }

    /** Get the HTML name for the form. This is the same as the HTML id unless
     * an id of "none" is specified, in which case the (Struts) form name is used.
     * <p>
     * Older version of JAFFA used the equivilent of 'formname' which is the default
     * for the the HtmlIdPrefix.
     */
    public String getHtmlName() {
        if (m_htmlName == null) {
            m_htmlName = getHtmlIdPrefix();
            if (("").equals(m_htmlName)) m_htmlName = getBeanName();
        }
        return m_htmlName;
    }

    /** Set the HTML name for the form. This allows jsp to set the name and id of the form.
     * This feature is especially usefull in AJAX, where new page fragments are generated with
     * new forms inside.
     */
    public void setHtmlName(String name) {
        this.m_htmlName = name;
    }

    /** This writes the initial html.
     * @throws JspException if any error occurs.
     * @return EVAL_BODY_TAG if the JSP engine should evaluate the tag body, otherwise return SKIP_BODY.
     */
    public int doStartTag() throws JspException {
        if ((action == null && (m_url == null || m_useBean == null)) || (action != null && (m_useBean != null))) throw new JspException("Invalid parameters, either provide 'action' or 'useBean'/'url'");
        CustomTag.pushParent(this, pageContext);
        if (m_useBean != null) {
            beanName = m_useBean;
            int scope = pageContext.getAttributesScope(m_useBean);
            switch(scope) {
                case PageContext.SESSION_SCOPE:
                    beanScope = "session";
                    break;
                case PageContext.REQUEST_SCOPE:
                    beanScope = "request";
                    break;
                default:
                    throw new JspException("Can't find object of name'" + m_useBean + "' in session or request scope?");
            }
        }
        int i = super.doStartTag();
        doStartTagExt();
        return i;
    }

    /** Override default implementation (struts 1.27) so that it doesn't check
     * for action-mapping if the 'url' attribute is specified.
     *
     * @exception JspException if a required value cannot be looked up
     */
    protected void lookup() throws JspException {
        moduleConfig = TagUtils.getInstance().getModuleConfig(pageContext);
        if (moduleConfig == null) {
            JspException e = new JspException(messages.getMessage("formTag.collections"));
            pageContext.setAttribute(Globals.EXCEPTION_KEY, e, PageContext.REQUEST_SCOPE);
            throw e;
        }
        servlet = (ActionServlet) pageContext.getServletContext().getAttribute(Globals.ACTION_SERVLET_KEY);
        if (m_url == null) {
            String mappingName = TagUtils.getInstance().getActionMappingName(action);
            mapping = (ActionMapping) moduleConfig.findActionConfig(mappingName);
            if (mapping == null) {
                JspException e = new JspException(messages.getMessage("formTag.mapping", mappingName));
                pageContext.setAttribute(Globals.EXCEPTION_KEY, e, PageContext.REQUEST_SCOPE);
                throw e;
            }
            FormBeanConfig formBeanConfig = moduleConfig.findFormBeanConfig(mapping.getName());
            if (formBeanConfig == null) {
                JspException e = new JspException(messages.getMessage("formTag.formBean", mapping.getName(), action));
                pageContext.setAttribute(Globals.EXCEPTION_KEY, e, PageContext.REQUEST_SCOPE);
                throw e;
            }
            beanName = mapping.getAttribute();
            beanScope = mapping.getScope();
            beanType = formBeanConfig.getType();
        }
    }

    /** Override default implementation (struts 1.27) to use the form name we need
     */
    protected void renderName(StringBuffer results) {
        results.append(" name=\"").append(getHtmlName()).append("\"");
    }

    /** This is the exact copy of the renderToken() method in Struts 1.1, except that it gets the token information from the component, as opposed to the session.
     * Generates a hidden input field with token information, if any.
     * @return A hidden input field containing the token.
     */
    protected String renderToken() {
        StringBuffer results = new StringBuffer();
        FormKey fk = (FormKey) pageContext.getRequest().getAttribute(FormKey.class.getName());
        if (fk != null && fk.getComponentId() != null) {
            Component component = UserSession.getUserSession((HttpServletRequest) pageContext.getRequest()).getComponent(fk.getComponentId());
            if (component != null && component.getToken() != null) {
                results.append("<input type=\"hidden\" name=\"");
                results.append(Constants.TOKEN_KEY);
                results.append("\" value=\"");
                results.append(component.getToken());
                results.append("\" />");
            }
        }
        return results.toString();
    }

    /**
     * Override default implementation (struts 1.27) to use the specified
     * 'url' as the action to post the form back to
     */
    protected void renderAction(StringBuffer results) {
        HttpServletResponse response = (HttpServletResponse) this.pageContext.getResponse();
        results.append(" action=\"");
        if (m_url != null) results.append(response.encodeURL(m_url)); else results.append(response.encodeURL(TagUtils.getInstance().getActionMappingURL(this.action, this.pageContext)));
        results.append("\"");
    }

    /**
     * Generates the opening <code>&lt;form&gt;</code> element with appropriate
     * attributes.
     *
     * Intercepts the call to the default implementation (struts 1.27) to set the styleId to our own HtmlIdPrefix.
     * We need the form tag to have a specific Id.
     */
    protected String renderFormStartElement() {
        setStyleId(getHtmlIdPrefix());
        return super.renderFormStartElement();
    }
}
