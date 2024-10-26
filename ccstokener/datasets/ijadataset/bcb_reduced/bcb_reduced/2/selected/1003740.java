package openfuture.bugbase.test.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import openfuture.bugbase.servlet.BugBaseServletClient;
import org.apache.cactus.JspTestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * <p>Base test case for JSP tests using the
 * <a href="http://jakarta.apache.org/cactus">Cactus</a> framework.</p>
 *
 *
 * Created: Wed Oct 31 14:33:23 2001
 *
 * @author <a href="mailto:wolfgang@openfuture.de">Wolfgang Reissenberger</a>
 * @version $Revision: 1.5 $
 */
public abstract class JspBaseTestCase extends JspTestCase {

    private BugBaseServletClient client;

    private Properties properties;

    /**
     * Creates a new <code>JspLoginTestCase</code> instance.
     * Read the property file "bugbase.properties" from the
     * classpath. The following properties are recognized:
     * <ul>
     *   <li> <strong>openfuture.bugbase.test.host</strong>: host name
     *        of the Bug Base instance.<br>
     *       [<em>Default: localhost:8080</em>]</li>
     *   <li> <strong>openfuture.bugbase.test.context</strong>: context name
     *        of the Bug Base instance.<br>
     *       [<em>Default: bugbase</em>]</li>
     *   <li> <strong>openfuture.bugbase.test.userid</strong>: User ID.<br>
     *       [<em>Default: admin</em>]</li>
     *   <li> <strong>openfuture.bugbase.test.password</strong>:
     *        Password for the user.<br>
     *       [<em>Default: bugbase</em>]</li>
     *   <li> <strong>openfuture.bugbase.test.project</strong>: project name
     *        where the bug reports should be reported for.<br>
     *       [<em>Default: BugBase Test</em>]</li>
     * </ul><p>
     *
     * @param name test case name
     */
    public JspBaseTestCase(String name) {
        super(name);
        String propertyFile = "bugbase.properties";
        Properties properties = new Properties();
        setProperties(properties);
        try {
            URL url = this.getClass().getResource("/" + propertyFile);
            if (url != null) {
                InputStream is = url.openStream();
                properties.load(is);
                is.close();
                getLog().debug("Cactus LogService successfully instantiated.");
                getLog().debug("Log4J successfully instantiated.");
            }
        } catch (IOException e) {
            System.err.println("ERROR: cannot load " + propertyFile + "!");
        }
        setDefault("openfuture.bugbase.test.host", "localhost:8080");
        setDefault("openfuture.bugbase.test.context", "bugbase");
        setDefault("openfuture.bugbase.test.userid", "admin");
        setDefault("openfuture.bugbase.test.password", "bugbase");
        setDefault("openfuture.bugbase.test.project", "BugBase Test");
    }

    /**
     * Setup before executing a test case:
     * <ul>
     *   <li>Create a {@link #getClient() Bug Base Servlet Proxy}.
     *       The URL to the 
     *       {@link openfuture.bugbase.servlet.BugBaseServlet BugBaseServlet}
     *       is constructed as
     *       <code>http://&lt;openfuture.bugbase.test.host&gt;/&lt;openfuture.bugbase.test.context&gt;/servlet/BugBaseServlet</code>.
     *   </li>
     * </ul>
     *
     * @exception Exception if an error occurs
     */
    public void clientSetUp() throws Exception {
        super.setUp();
        String urlString = "http://" + getProperties().getProperty("openfuture.bugbase.test.host") + "/" + getProperties().getProperty("openfuture.bugbase.test.context") + "/servlet/BugBaseServlet";
        BugBaseServletClient client = new BugBaseServletClient(urlString);
        setClient(client);
    }

    /**
     * Currently does nothing.
     *
     * @exception Exception if an error occurs
     */
    public void clientTearDown() throws Exception {
    }

    /**
     * Call a page, that requires a login first. If the login screen
     * does not come first, a logoff is tried and the page is called again.
     *
     * @param wc the web conversation
     * @param request Cactus request object
     * @param jsp the JSP that requires a login
     * @param searchLogin if <code>true</code>, the result
     *        page will be searched for a login form. The login form is
     *        {@link #findForm(com.meterware.httpunit.WebForm[], String) recognized}
     *        by its action. The action must contain "login.do" as substring.
     *
     * @return the response receive
     * @exception Exception if an error occurs
     */
    public WebResponse loginPage(WebConversation wc, org.apache.cactus.WebRequest request, String jsp, boolean searchLogin) throws Exception {
        String urlString = "http://" + getProperties().getProperty("openfuture.bugbase.test.host") + "/" + getProperties().getProperty("openfuture.bugbase.test.context") + "/" + jsp;
        getLog().info("Logging in to " + urlString);
        WebRequest req = new GetMethodWebRequest(urlString);
        WebResponse resp = wc.getResponse(req);
        copyCookies(resp, request);
        if (findForm(resp.getForms(), "login.do") == null) {
            getLog().info("Already logged in. Logging off first.");
            resp = logoff(wc, resp);
        }
        resp = login(wc, resp, searchLogin);
        getLog().info("Login succeeded.");
        copyCookies(resp, request);
        setUrl(resp.getURL(), request);
        return resp;
    }

    /**
     * Call a page and logoff directly. It is assumed, that
     * no intermediatary login is required.
     *
     * @param wc the web conversation
     * @param request Cactus request object
     * @param jsp the JSP from where the logoff will be processed
     * @return the response receive
     * @exception Exception if an error occurs
     */
    public WebResponse logoffPage(WebConversation wc, org.apache.cactus.WebRequest request, String jsp) throws Exception {
        WebRequest req = new GetMethodWebRequest("http://" + getProperties().getProperty("openfuture.bugbase.test.host") + "/" + getProperties().getProperty("openfuture.bugbase.test.context") + "/" + jsp);
        WebResponse resp = wc.getResponse(req);
        resp = logoff(wc, resp);
        copyCookies(resp, request);
        setUrl(resp.getURL(), request);
        return resp;
    }

    /**
     * Login into Bug Base using HttpUnit. The current page must
     * contain a login form.
     *
     * @param wc the current web conversation
     * @param resp the web response, where the input form will be searched
     * @param searchLogin if <code>true</code>, the result
     *        page will be searched for a login form. The login form is
     *        {@link #findForm(com.meterware.httpunit.WebForm[], String) recognized}
     *        by its action. The action must contain "login.do" as substring.
     * @return the HTTPUnit response object
     * @exception Exception if an error occurs
     */
    protected WebResponse login(WebConversation wc, WebResponse resp, boolean searchLogin) throws Exception {
        WebForm[] forms = resp.getForms();
        WebForm loginForm = findForm(forms, "login.do");
        if (loginForm != null) {
            String names[] = loginForm.getParameterNames();
            assertTrue("Missing submit button", loginForm.getSubmitButtons().length > 0);
            assertTrue("No projects in the selection", loginForm.getOptions("project").length > 0);
            WebRequest req = loginForm.getRequest(loginForm.getSubmitButtons()[0]);
            assertNotNull("No submit button found!", req);
            req.setParameter("userid", getProperties().getProperty("openfuture.bugbase.test.userid"));
            getLog().info("set userid=\"" + req.getParameter("userid") + "\"");
            req.setParameter("password", getProperties().getProperty("openfuture.bugbase.test.password"));
            getLog().info("set password=\"" + req.getParameter("password") + "\"");
            req.setParameter("project", getProperties().getProperty("openfuture.bugbase.test.project"));
            getLog().info("set project=\"" + req.getParameter("project") + "\"");
            getLog().info("Calling " + req);
            resp = wc.getResponse(req);
            if (searchLogin) assertNull("Login failed. We detected a login form on the page!", findForm(resp.getForms(), "login.do"));
        }
        if (resp.getText().indexOf("Exception occured") >= 0) {
            fail("Exception occured!\n" + resp.getText());
        }
        return resp;
    }

    /**
     * Logoff from Bug Base. If the current page must contain a logoff form,
     * it will be called.
     *
     * @param wc the current web conversation
     * @param resp the web response, where the input form will be searched
     * @return the HTTPUnit response object
     * @exception Exception if an error occurs
     */
    protected WebResponse logoff(WebConversation wc, WebResponse resp) throws Exception {
        WebForm[] forms = resp.getForms();
        WebForm logoffForm = findForm(forms, "logoff.do");
        if (logoffForm != null) {
            String names[] = logoffForm.getParameterNames();
            assertTrue("Missing submit button", logoffForm.getSubmitButtons().length > 0);
            WebRequest req = logoffForm.getRequest(logoffForm.getSubmitButtons()[0]);
            getLog().info("Calling " + req);
            resp = wc.getResponse(req);
            WebForm loginForm = findForm(resp.getForms(), "login.do");
            assertTrue("Exception occured!", resp.getText().indexOf("Exception occured") < 0);
            assertNotNull("Log off failed, since no login form is visible!", loginForm);
        }
        return resp;
    }

    /**
     * copy the new cookies received by the response to the new request.
     *
     * @param from received response
     * @param to target request
     */
    public void copyCookies(WebResponse from, org.apache.cactus.WebRequest to) {
        for (int i = 0; i < from.getNewCookieNames().length; i++) {
            String name = from.getNewCookieNames()[i];
            String value = from.getNewCookieValue(name);
            to.addCookie(name, value);
        }
    }

    /**
     * Find a certain form. The form is recognized by the action which
     * should contain the given action string.
     *
     * @param forms array of form objects
     * @param action action string that must be contained in the action string.
     * @return form or <code>null</code>, if the
     * desired form cannot be found.
     */
    public WebForm findForm(WebForm[] forms, String action) {
        if (forms == null) return null;
        for (int i = 0; i < forms.length; i++) {
            if (forms[i].getAction().indexOf(action) > 0) return forms[i];
        }
        return null;
    }

    /**
     * Set a property to a default value, if the property
     * is <b>NOT</b> already set.
     *
     * @param key property key
     * @param value property value
     */
    protected void setDefault(String key, String value) {
        if (getProperties().getProperty(key) == null) {
            getProperties().setProperty(key, value);
        }
    }

    /**
     * Set the URL in the Cactus request
     *
     * @param url URL to be set
     * @param request Cactus request the URL should be set
     * @see org.apache.cactus.WebRequest#setURL(String, String, String, String, String)
     */
    protected void setUrl(URL url, org.apache.cactus.WebRequest request) {
        if (url == null) return;
        String servername = url.getHost();
        if (url.getPort() > -1) servername = servername + ":" + url.getPort();
        String path = url.getPath();
        String context = "";
        if (path.indexOf("/", 1) > -1) {
            context = path.substring(0, path.indexOf("/", 1));
            path = path.substring(path.indexOf("/", 1));
        }
        request.setURL(servername, context, path, null, url.getQuery());
    }

    /**
     * Get the value of properties.
     * @return value of properties.
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Set the value of properties.
     * @param v  Value to assign to properties.
     */
    public void setProperties(Properties v) {
        this.properties = v;
    }

    /**
     * Get the value of client.
     *
     * @return value of client.
     */
    public BugBaseServletClient getClient() {
        return client;
    }

    /**
     * Set the value of client.
     * @param v  Value to assign to client.
     */
    public void setClient(BugBaseServletClient v) {
        this.client = v;
    }

    /**
     * Convenience method accessing the Cactus LogService Log.
     *
     * @return the current Log instance
     */
    public Log getLog() {
        return LogFactory.getLog(getClass().getName());
    }
}
