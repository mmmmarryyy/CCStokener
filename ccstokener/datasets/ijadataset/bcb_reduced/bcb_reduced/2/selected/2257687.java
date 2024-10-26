package nl.divosa.digid.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.MissingResourceException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nl.divosa.digid.servlet.property.DigiDProperties;
import nl.divosa.digid.servlet.state.DigiDCanceled;
import nl.divosa.digid.servlet.state.DigiDFailed;
import nl.divosa.digid.servlet.state.DigiDOkay;
import nl.divosa.digid.servlet.state.DigiDState;
import nl.divosa.digid.servlet.state.InitialState;
import org.apache.log4j.Logger;

/**
 * DigiD Filter for eformulieren.
 * 
 * This filter lets the user authenticate with DigiD.
 * 
 * The DigiD result is added as a {@link DigiDState} session attribute with name: {@link DigiDState#ATTRIBUTE_NAME}
 * 
 * The filter results in a {@link HttpServletResponse#SC_FORBIDDEN} http code when authentication fails.
 * 
 * Logout is executed by using the <code>?logout</code> HTTP parameter.
 * 
 * If DigiD fails, the user cancels or logs out the following request attribute is added for usage in e.g. JSP pages:
 * <code>noDigiDURL</code>.
 */
public class DigidFilter implements Filter {

    private FilterConfig config;

    private Logger logger;

    private String env;

    private String digidURL;

    private String digidSharedSecret;

    private String digidApplID;

    private String digidServerID;

    private int minimumLevel = 0;

    String redirectURL = null;

    /**
     * Default constructor.
     */
    public DigidFilter() {
        logger = Logger.getLogger(DigidFilter.class);
    }

    public void init(final FilterConfig config) throws ServletException {
        this.config = config;
        try {
            env = DigiDProperties.getPropertyStore().getProperty(DigiDProperties.PROP_ENV);
            digidURL = DigiDProperties.getPropertyStore().getProperty(env + DigiDProperties.DOT + DigiDProperties.PROP_URL);
            digidSharedSecret = DigiDProperties.getPropertyStore().getProperty(env + DigiDProperties.DOT + DigiDProperties.PROP_SHARED_SECRET);
            digidApplID = DigiDProperties.getPropertyStore().getProperty(env + DigiDProperties.DOT + DigiDProperties.PROP_APP_ID);
            digidServerID = DigiDProperties.getPropertyStore().getProperty(env + DigiDProperties.DOT + DigiDProperties.PROP_SERVER_ID);
            redirectURL = DigiDProperties.getPropertyStore().getProperty(env + DigiDProperties.DOT + DigiDProperties.PROP_REDIRECT_URL);
            try {
                String sMinimumLevel = DigiDProperties.getPropertyStore().getProperty(env + DigiDProperties.DOT + DigiDProperties.PROP_MINIMAL_LEVEL);
                minimumLevel = Integer.parseInt(sMinimumLevel);
            } catch (MissingResourceException e) {
                logger.info("No optional 'minimumLevel' property found. Level is not verified.");
            }
            logger.info(config.getFilterName() + " started.");
        } catch (MissingResourceException e) {
            logger.fatal("Missing digid resource: " + e.getKey(), e);
            throw new ServletException("Could not start filter: " + config.getFilterName());
        } catch (NumberFormatException e) {
            logger.fatal("Invalid digid resource: " + e.getMessage(), e);
            throw new ServletException("Could not start filter: " + config.getFilterName());
        }
    }

    public void destroy() {
        logger.info(config.getFilterName() + " stopped.");
    }

    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        HttpSession session = ((HttpServletRequest) request).getSession();
        boolean alreadyValidDigiDSession = false;
        if (session != null && session.getAttribute(DigiDState.ATTRIBUTE_NAME) != null) {
            DigiDState digidState = (DigiDState) session.getAttribute(DigiDState.ATTRIBUTE_NAME);
            if (digidState != null) {
                switch(digidState.getState()) {
                    case DIGID_OK:
                        {
                            alreadyValidDigiDSession = true;
                        }
                }
            }
        }
        if (alreadyValidDigiDSession && request.getParameter("logout") == null) {
            chain.doFilter(request, response);
        } else {
            if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
                logger.error("Request is not a HttpServletRequest");
                throw new RuntimeException("Request is not a HttpServletRequest");
            }
            session = ((HttpServletRequest) request).getSession(true);
            DigiDState state = (DigiDState) session.getAttribute(DigiDState.ATTRIBUTE_NAME);
            if (state == null && session.getAttribute("referer") == null) {
                String referer = ((HttpServletRequest) request).getHeader("Referer");
                if (logger.isDebugEnabled()) {
                    logger.debug("referer = " + referer);
                }
                session.setAttribute("referer", referer);
            }
            try {
                if (state != null) {
                    switch(state.getState()) {
                        case INITIAL:
                            {
                                state = fromDigid(request, (InitialState) state);
                                break;
                            }
                        case DIGID_FAILED:
                        case DIGID_CANCELED:
                            {
                                break;
                            }
                        case DIGID_OK:
                            {
                                if (request.getParameter("logout") != null) {
                                    String referer = (String) session.getAttribute("referer");
                                    session.invalidate();
                                    ((HttpServletRequest) request).getSession(true).setAttribute("referer", referer);
                                    addDigiDURL((HttpServletRequest) request);
                                    addNoDigiDURL((HttpServletRequest) request);
                                    RequestDispatcher dispacher = request.getRequestDispatcher("/logout.jsp");
                                    dispacher.forward(request, response);
                                    return;
                                }
                                break;
                            }
                    }
                } else {
                    state = toDigid((HttpServletRequest) request, (HttpServletResponse) response, session);
                }
            } catch (Exception e) {
                logger.fatal("Internal error occurred during filter process", e);
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            switch(state.getState()) {
                case INITIAL:
                    {
                        break;
                    }
                case DIGID_FAILED:
                case DIGID_CANCELED:
                    {
                        addDigiDURL((HttpServletRequest) request);
                        addNoDigiDURL((HttpServletRequest) request);
                        request.setAttribute("errorMessage", getErrorMessage(state));
                        session.removeAttribute(DigiDState.ATTRIBUTE_NAME);
                        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                        break;
                    }
                default:
                    {
                        session.setAttribute(DigiDState.ATTRIBUTE_NAME, state);
                        addDigiDURLTpSession((HttpServletRequest) request);
                        chain.doFilter(request, response);
                    }
            }
        }
    }

    /**
     * Get an error message based on the DigiDState.
     * 
     * @param state the DigiDState
     * @return the error message
     */
    private String getErrorMessage(DigiDState state) {
        String message = null;
        switch(state.getState()) {
            case DIGID_FAILED:
                {
                    message = "Er is een fout opgetreden in de communicatie met DigiD. Probeert u het later nogmaals.";
                    break;
                }
            case DIGID_CANCELED:
                {
                    message = "U heeft geannuleerd.";
                    break;
                }
        }
        return message;
    }

    private void addNoDigiDURL(HttpServletRequest servletRequest) {
        String applChosen = redirectURL + servletRequest.getRequestURI();
        if (applChosen.endsWith(".digid")) {
            applChosen = applChosen.substring(0, applChosen.length() - 6);
        }
        servletRequest.setAttribute("noDigiDURL", applChosen);
    }

    private void addDigiDURL(HttpServletRequest servletRequest) {
        StringBuffer sb = getDigiDURL(servletRequest);
        servletRequest.setAttribute("digiDURL", sb.toString());
    }

    private void addDigiDURLTpSession(HttpServletRequest servletRequest) {
        StringBuffer sb = getDigiDURL(servletRequest);
        servletRequest.getSession().setAttribute("digiDURL", sb.toString());
    }

    private StringBuffer getDigiDURL(HttpServletRequest servletRequest) {
        StringBuffer sb = new StringBuffer(redirectURL).append(servletRequest.getRequestURI());
        if (servletRequest.getParameter("logout") == null) {
            StringBuilder query = new StringBuilder();
            Enumeration<String> en = servletRequest.getParameterNames();
            while (en.hasMoreElements()) {
                String parameterName = en.nextElement();
                if (!parameterName.equals("aselect_credentials") && !parameterName.equals("a-select-server") && !parameterName.equals("rid")) {
                    query.append(parameterName).append("=").append(servletRequest.getParameter(parameterName)).append("&");
                }
            }
            if (query != null) {
                sb.append('?').append(query.toString());
            }
        }
        return sb;
    }

    private DigiDState fromDigid(ServletRequest request, InitialState state) throws IOException {
        String digidServerID;
        DigiDState newState = state;
        String rid = request.getParameter("rid");
        if (rid == null || rid.equals("")) {
            logger.warn("URL parameter 'rid' is missing");
            newState = new DigiDFailed();
            return newState;
        }
        if (!state.getRid().equalsIgnoreCase(rid)) {
            logger.warn("rid does not match initial rid, try again");
            newState = new DigiDFailed();
            return newState;
        }
        String aselCredentials = request.getParameter("aselect_credentials");
        if (aselCredentials == null || aselCredentials.equals("")) {
            logger.warn("URL parameter 'aselect_credentials' is missing");
            newState = new DigiDFailed();
            return newState;
        }
        digidServerID = request.getParameter("a-select-server");
        if (digidServerID == null || digidServerID.equals("")) {
            logger.info("URL parameter 'a-select-server' is missing this time (but is not used)");
        }
        logger.debug("FromDigiD: rid=" + rid + ", aselServer=" + digidServerID + ", aselCredentials: " + aselCredentials.substring(0, 20));
        String soapMessage = Soap.buildDigiDVerify(digidSharedSecret, digidURL, digidServerID, aselCredentials, rid);
        if (logger.isDebugEnabled()) {
            logger.debug("Send [" + soapMessage + "]");
        }
        String result = soapToDigiD(soapMessage);
        if (result == null) {
            throw new RuntimeException("soapToDigiD() failed...");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Recv [" + result + "]");
        }
        String uid = Soap.extractFromXml(result, "m:uid");
        String sTrustLevel = Soap.extractFromXml(result, "m:betrouwbaarheidsniveau");
        int trustLevel = -1;
        if (sTrustLevel != null) {
            try {
                trustLevel = Integer.parseInt(sTrustLevel);
            } catch (NumberFormatException e) {
                logger.warn("Invalid betrouwbaarheidsniveau from DigiD", e);
                newState = new DigiDFailed();
                return newState;
            }
        }
        String resultCode = Soap.extractFromXml(result, "m:result_code");
        if (resultCode == null || (resultCode.equals("0000") && (uid == null || sTrustLevel == null))) {
            logger.debug("Invalid response from DigiD");
            newState = new DigiDFailed();
        } else if (resultCode.equals("0040")) {
            logger.debug("User canceled DigiD");
            newState = new DigiDCanceled();
        } else if (!resultCode.equals("0000")) {
            logger.debug("Error response from DigiD: " + resultCode);
            newState = new DigiDFailed(resultCode);
        } else if (trustLevel < minimumLevel) {
            logger.debug("DigiD level to low: " + trustLevel);
            newState = new DigiDFailed();
            ((DigiDFailed) newState).setResult("0003");
        } else {
            logger.debug("DigiD Okay");
            newState = new DigiDOkay(trustLevel, uid);
        }
        return newState;
    }

    private DigiDState toDigid(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException {
        DigiDState state = null;
        StringBuffer redirectURL = new StringBuffer(this.redirectURL).append(request.getRequestURI());
        String applChosen = redirectURL.toString();
        int c;
        String input;
        URL url;
        String encURL = URLEncoder.encode(applChosen, "UTF-8");
        String soapMessage = Soap.buildDigiDAuthenticate(digidURL, digidSharedSecret, digidApplID, digidServerID, applChosen, encURL);
        if (logger.isDebugEnabled()) {
            logger.debug("Send [" + soapMessage + "]");
        }
        PrintWriter pw = null;
        BufferedReader in = null;
        try {
            url = new URL(digidURL);
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Connection", "Close");
            connection.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
            connection.connect();
            OutputStream os = connection.getOutputStream();
            pw = new PrintWriter(os);
            pw.println(soapMessage);
            pw.flush();
            Soap.displayConnectionHeaders(connection);
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            for (input = ""; ; ) {
                c = in.read();
                if (c < 0) break;
                input = input + (char) c;
            }
            if (logger.isDebugEnabled()) logger.debug("Received [" + input + "]");
        } catch (IOException e) {
            logger.warn("I/O exception: " + e.getMessage(), e);
            throw e;
        } finally {
            if (pw != null) pw.close();
            if (in != null) in.close();
        }
        String m_result = Soap.extractFromXml(input, "m:result_code");
        if (m_result == null) {
            state = new DigiDFailed();
        } else if (!m_result.equals("0000")) {
            state = new DigiDFailed(m_result);
        } else {
            state = new InitialState();
            String m_url = Soap.extractFromXml(input, "m:as_url");
            String m_rid = Soap.extractFromXml(input, "m:rid");
            String m_server = Soap.extractFromXml(input, "m:a-select-server");
            String msg = "";
            if (m_url == null) msg += " m:as_url";
            if (m_rid == null) msg += " m:rid";
            if (m_server == null) msg += " m:a-select-server";
            if (!msg.equals("")) {
                logger.error("Missing DigiD fields in SOAP 'authenticate'" + " message: " + msg.substring(1));
                state = new DigiDFailed();
                return state;
            }
            ((InitialState) state).setRid(m_rid);
            if (logger.isDebugEnabled()) {
                logger.debug("m_rid:" + m_rid);
                logger.debug("m_server:" + m_server);
                logger.debug("applChosen:" + applChosen);
            }
            session.setAttribute(DigiDState.ATTRIBUTE_NAME, state);
            String urlDigiD = m_url + "&rid=" + m_rid + "&a-select-server=" + m_server;
            logger.debug("Redirect to: " + urlDigiD);
            ((HttpServletResponse) response).sendRedirect(urlDigiD);
        }
        return state;
    }

    public String soapToDigiD(String soapMessage) throws IOException {
        String input = null;
        int c;
        BufferedReader in = null;
        PrintWriter pw = null;
        try {
            if (digidURL.equals("")) return null;
            URL url = new URL(digidURL);
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Connection", "Close");
            connection.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
            connection.connect();
            OutputStream os = connection.getOutputStream();
            pw = new PrintWriter(os);
            pw.println(soapMessage);
            pw.flush();
            displayConnectionHeaders(connection);
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            for (input = ""; ; ) {
                c = in.read();
                if (c < 0) break;
                input = input + (char) c;
            }
        } catch (IOException e) {
            logger.error("I/O Exception: " + e.getMessage(), e);
            throw e;
        } finally {
            if (pw != null) pw.close();
            if (in != null) in.close();
        }
        return input;
    }

    protected void displayConnectionHeaders(URLConnection connection) {
        logger.debug("ConnectionHeaders:");
        for (int i = 0; ; i++) {
            String key = connection.getHeaderFieldKey(i);
            if (key == null) break;
            logger.debug("  " + key + "-" + connection.getHeaderField(key));
        }
    }
}
