package org.dspace.app.xmlui.cocoon;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.cocoon.servlet.CocoonServlet;
import org.apache.log.ContextMap;
import org.dspace.app.xmlui.configuration.XMLUIConfiguration;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.ConfigurationManager;

/**
 * This is a wrapper servlet around the cocoon servlet that prefroms two functions, 1) it 
 * initializes DSpace / XML UI configuration parameters, and 2) it will preform inturrupted 
 * request resumption.
 * 
 * @author scott philips
 */
public class DSpaceCocoonServlet extends CocoonServlet {

    private static final long serialVersionUID = 1L;

    /**
     * The DSpace config paramater, this is where the path to the DSpace
     * configuration file can be obtained
     */
    public static final String DSPACE_CONFIG_PARAMETER = "dspace-config";

    /**
     * This method holds code to be removed in the next version 
     * of the DSpace XMLUI, it is now managed by a Shared Context 
     * Listener inthe dspace-api project. 
     * 
     * It is deprecated, rather than removed to maintain backward 
     * compatibility for local DSpace 1.5.x customized overlays.
     * 
     * TODO: Remove in trunk
     *
     * @deprecated Use Servlet Context Listener provided 
     * in dspace-api (remove in > 1.5.x)
     * @throws ServletException
     */
    private void initDSpace() throws ServletException {
        try {
            String osName = System.getProperty("os.name");
            if (osName != null) osName = osName.toLowerCase();
            if (osName != null && osName.contains("windows")) {
                URL url = new URL("http://localhost/");
                URLConnection urlConn = url.openConnection();
                urlConn.setDefaultUseCaches(false);
            }
        } catch (Throwable t) {
        }
        String dspaceConfig = null;
        String log4jConfig = null;
        dspaceConfig = super.getInitParameter(DSPACE_CONFIG_PARAMETER);
        if (dspaceConfig == null) dspaceConfig = super.getServletContext().getInitParameter(DSPACE_CONFIG_PARAMETER);
        if (dspaceConfig == null || "".equals(dspaceConfig)) {
            throw new ServletException("\n\nDSpace has failed to initialize. This has occurred because it was unable to determine \n" + "where the dspace.cfg file is located. The path to the configuration file should be stored \n" + "in a context variable, '" + DSPACE_CONFIG_PARAMETER + "', in either the local servlet or global contexts. \n" + "No context variable was found in either location.\n\n");
        }
        try {
            if (!ConfigurationManager.isConfigured()) {
                ConfigurationManager.loadConfig(dspaceConfig);
            }
        } catch (Throwable t) {
            throw new ServletException("\n\nDSpace has failed to initialize, during stage 2. Error while attempting to read the \n" + "DSpace configuration file (Path: '" + dspaceConfig + "'). \n" + "This has likely occurred because either the file does not exist, or it's permissions \n" + "are set incorrectly, or the path to the configuration file is incorrect. The path to \n" + "the DSpace configuration file is stored in a context variable, 'dspace-config', in \n" + "either the local servlet or global context.\n\n", t);
        }
    }

    /**
     * Before this servlet will become functional replace 
     */
    public void init() throws ServletException {
        this.initDSpace();
        super.init();
        String webappConfigPath = null;
        String installedConfigPath = null;
        try {
            webappConfigPath = super.getServletContext().getRealPath("/") + File.separator + "WEB-INF" + File.separator + "xmlui.xconf";
            installedConfigPath = ConfigurationManager.getProperty("dspace.dir") + File.separator + "config" + File.separator + "xmlui.xconf";
            XMLUIConfiguration.loadConfig(webappConfigPath, installedConfigPath);
        } catch (Throwable t) {
            throw new ServletException("\n\nDSpace has failed to initialize, during stage 3. Error while attempting to read \n" + "the XML UI configuration file (Path: " + webappConfigPath + " or '" + installedConfigPath + "').\n" + "This has likely occurred because either the file does not exist, or it's permissions \n" + "are set incorrectly, or the path to the configuration file is incorrect. The XML UI \n" + "configuration file should be named \"xmlui.xconf\" and located inside the standard \n" + "DSpace configuration directory. \n\n", t);
        }
    }

    /**
     * Before passing off a request to the cocoon servlet check to see if there is a request that 
     * should be resumed? If so replace the real request with a faked request and pass that off to 
     * cocoon.
     */
    public void service(HttpServletRequest realRequest, HttpServletResponse realResponse) throws ServletException, IOException {
        try {
            realRequest = AuthenticationUtil.resumeRequest(realRequest);
            if ((ConfigurationManager.getBooleanProperty("xmlui.force.ssl")) && (realRequest.getSession().getAttribute("dspace.current.user.id") != null) && (!realRequest.isSecure())) {
                StringBuffer location = new StringBuffer("https://");
                location.append(ConfigurationManager.getProperty("dspace.hostname")).append(realRequest.getContextPath()).append(realRequest.getServletPath()).append(realRequest.getQueryString() == null ? "" : ("?" + realRequest.getQueryString()));
                realResponse.sendRedirect(location.toString());
            }
            super.service(realRequest, realResponse);
            ContextUtil.closeContext(realRequest);
        } finally {
            try {
                ContextMap.removeCurrentContext();
            } catch (NoSuchMethodError nsme) {
            }
        }
    }
}
