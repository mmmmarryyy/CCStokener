package org.apache.jetspeed.portal.portlets;

import org.apache.ecs.ConcreteElement;
import org.apache.ecs.StringElement;
import org.apache.jetspeed.portal.portlets.AbstractPortlet;
import org.apache.turbine.util.RunData;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**  
 *   This portlet class is intended to be used for internal URLs that will 
 *   not work properly if they are part of the same request as the Jetspeed/Turbine
 *   controller servlet--Struts-mapped URLs are an example of these.
 *   <br>
 *   ServletProxyPortlet uses an application context-relative URL as an input parameter.  It uses data obtained from
 *   <code>org.apache.turbine.util.RunData</code> to construct an absolute URL and append the session id to it. Using this
 *   URL, it constructs a java.net.URL object, retrieves the content from it, and converts the content to a String.  Finally,
 *   it returns an ECS StringElement created with this String for Jetspeed to render as a portlet.  Content is returned as-is;
 *   no filtering is performed on the html before returning it.
 *   <br/><br/>
 *   This portlet accepts 3 parameters:<br/>
 *
 *   URL (required) 		  -- the web application-context relative URL (a query string may be used to pass parameters)<br/>
 *   session_token (optional) -- token key used by the web server to pass the session id on the query string<br/>
 *   protocol (optional)	  -- protocol to use to make the URL request
 *
 * @author <a href="mailto:joe.barefoot@motiva.com">Joe Barefoot</a>
 */
public class ServletProxyPortlet extends AbstractPortlet {

    private static final int BUFFER_SIZE = 2048;

    /** The name of the parameter to hold our application context-relative URL */
    public static final String URL_PARAMETER_NAME = "URL";

    /** The name of the parameter to hold the protocol to use (optional, default is http) **/
    public static final String PROTOCOL_PARAMETER_NAME = "protocol";

    /** The name of the parameter to hold the token by which the session ID is passed on the query string (optional, default is jsessionid)
	 *  This is included to accomodate all web servers, as the token is usually different from one server to another.
	 */
    public static final String SESSION_TOKEN_PARAMETER_NAME = "session_token";

    /** The default protocol used to construct the URL -- http */
    public static final String DEFAULT_PROTOCOL = "http";

    /** The default token to use to pass the session ID on the query String -- jsessionid */
    public static final String DEFAULT_SESSION_TOKEN = "jsessionid";

    /**  Gets content by proxy (java.net.URL) from an internal URL and returns it
	 *
	 * @param rundata The RunData object for the current request
	 * @return an ECS StringElement
	 */
    public ConcreteElement getContent(RunData rundata) {
        String servletURL = processURL(rundata);
        if (servletURL == null) {
            return new StringElement("ServletInvokerPortlet:  Must specify a URL using the URL parameter");
        }
        String content;
        try {
            URL url = new URL(servletURL);
            URLConnection connection = url.openConnection();
            InputStream stream = connection.getInputStream();
            BufferedInputStream in = new BufferedInputStream(stream);
            int length = 0;
            byte[] buf = new byte[BUFFER_SIZE];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((in != null) && ((length = in.read(buf)) != -1)) {
                out.write(buf, 0, length);
            }
            content = out.toString();
            return new StringElement(content);
        } catch (Exception e) {
            String message = "ServletInvokerPortlet: Error invoking " + servletURL + ": " + e.getMessage();
            return new StringElement(message);
        }
    }

    /**  Constructs a full URL to retrieve content from.  Override to append custom (default) query parameters, etc.
	 *
	 * @param rundata The RunData object for the current request
	 * @return  An absolute URL with the session ID appended
	 */
    protected String processURL(RunData rundata) {
        String servletURL = getPortletConfig().getInitParameter(URL_PARAMETER_NAME);
        if (servletURL == null) {
            return null;
        }
        String protocol = getPortletConfig().getInitParameter(PROTOCOL_PARAMETER_NAME);
        if (protocol == null) {
            protocol = DEFAULT_PROTOCOL;
        }
        String token = getPortletConfig().getInitParameter(SESSION_TOKEN_PARAMETER_NAME);
        if (token == null) {
            token = DEFAULT_SESSION_TOKEN;
        }
        String queryString = new String();
        int queryIndex = servletURL.indexOf("?");
        if (queryIndex > 0) {
            queryString = servletURL.substring(queryIndex);
            servletURL = servletURL.substring(0, queryIndex);
        }
        servletURL = protocol + "://" + rundata.getServerName() + ":" + rundata.getServerPort() + rundata.getContextPath() + servletURL + ";" + token + "=" + rundata.getSession().getId() + queryString;
        return servletURL;
    }
}
