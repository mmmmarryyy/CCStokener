package fr.soleil.util.serialized;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import fr.soleil.util.UtilLogger;
import fr.soleil.util.exception.SoleilException;
import fr.soleil.util.exception.WebSecurityException;

/**
 * Provide service to send a WebRequest to a WebServer and return the
 * WebResponse
 * 
 * @author BARBA-ROSSA
 * 
 */
public class WebServerClient implements IWebServerClient {

    private String m_strApplication = null;

    private String m_strJsessionid = null;

    private URL m_Url = null;

    private IRecallManager m_recallManager = null;

    private ILoginAction m_iLoginAction = null;

    /**
     * Default constructor with the Web Server URL
     * 
     * @param newUrl
     */
    public WebServerClient(URL newUrl) {
        m_Url = newUrl;
    }

    private static int i = 0;

    /**
     * Send a web Request and returns a response if ok
     * 
     * @param webRequest
     * @param con
     * @return {@link WebResponse}
     * @throws Exception
     *             if an exception occurs
     */
    private synchronized WebResponse sendWebRequest(WebRequest webRequest) throws Exception {
        WebResponse response = null;
        OutputStream outstream = null;
        URLConnection con = null;
        try {
            i++;
            UtilLogger.logger.addInfoLog("WebServerClient.getObject begin :" + m_Url);
            UtilLogger.logger.addInfoLog("WebServerClient.getObject action :" + webRequest.getAction());
            Object[] args = webRequest.getArguments();
            System.out.println(i + "########### sendWebRequest args " + args.length);
            if (args.length >= 1) {
                System.out.println(args[0].getClass());
                if (args[0] instanceof WebReflectRequest) {
                    System.out.println("action:" + ((WebReflectRequest) args[0]).getAction());
                    System.out.println("method:" + ((WebReflectRequest) args[0]).getMethod());
                    System.out.println("params:" + Arrays.toString(((WebReflectRequest) args[0]).getMethodParam()));
                }
            }
            con = getServletConnection("ActionServlet");
            outstream = con.getOutputStream();
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(outstream);
                oos.writeObject(webRequest);
            } finally {
                if (oos != null) {
                    oos.flush();
                    oos.close();
                    outstream.close();
                }
            }
            System.out.println(i + " sendWebRequest getting answer ");
            InputStream instr = null;
            ObjectInputStream inputFromServlet = null;
            try {
                instr = con.getInputStream();
                String strCookieValue = con.getHeaderField("Set-cookie");
                String sessionID = con.getHeaderField("TANGO_SESSION_ID");
                if (strCookieValue != null) {
                    int iBegin = strCookieValue.indexOf("JSESSIONID=");
                    if (iBegin != -1) {
                        int iEnd = strCookieValue.indexOf(";");
                        if (iEnd != -1) {
                            m_strJsessionid = strCookieValue.substring(iBegin, iEnd);
                        } else {
                            m_strJsessionid = strCookieValue.substring(iBegin);
                        }
                    }
                    if (m_strJsessionid == null) {
                        m_strJsessionid = sessionID.trim();
                    }
                } else {
                    if (m_strJsessionid == null && sessionID != null) {
                        m_strJsessionid = sessionID.trim();
                    }
                }
                inputFromServlet = new ObjectInputStream(instr);
                response = (WebResponse) inputFromServlet.readObject();
                System.out.println(i + "response " + response);
                if (response != null) {
                    System.out.println(i + "results " + response.getResult().getClass().getCanonicalName());
                }
            } finally {
                if (inputFromServlet != null) {
                    inputFromServlet.close();
                }
                if (instr != null) {
                    instr.close();
                }
            }
            UtilLogger.logger.addInfoLog("##########WebServerClient.getObject end :" + m_Url);
            if (response == null) {
                return null;
            }
            if (response.getResult() == null) {
                return null;
            }
            if (response.getResult().length > 0) {
                Object objet = response.getResult()[0];
                if (objet instanceof Exception) {
                    Exception e = (Exception) objet;
                    final StringWriter sw = new StringWriter();
                    if (e.getCause() != null) {
                        e.getCause().printStackTrace(new PrintWriter(sw));
                    } else {
                        e.printStackTrace(new PrintWriter(sw));
                    }
                    final String stacktrace = sw.toString();
                    System.err.println(i + "XXXXXXX ERROR send by server: " + stacktrace);
                    throw (Exception) objet;
                }
            }
            System.out.println(i + "##############sendWebRequest end OK ");
        } catch (SoleilException e) {
            System.err.println(i + " request failed " + e);
            System.out.println("m_strJsessionid :" + m_strJsessionid);
            throw e;
        } catch (WebSecurityException wse) {
            System.err.println(i + " request failed " + wse);
            System.out.println("m_strJsessionid :" + m_strJsessionid);
            if (m_iLoginAction != null && WebSecurityException.USER_NOT_CONNECTED.equals(wse.getExceptionCode())) {
                m_iLoginAction.authenticateUser();
                return sendWebRequest(webRequest);
            } else {
                throw wse;
            }
        } catch (MalformedURLException ex) {
            System.err.println(i + " request failed " + ex);
            ex.printStackTrace();
            UtilLogger.logger.addFATALLog(ex);
            throw new WebServerClientException(WebServerClientException.s_str_URL_MALFORMED, WebServerClientException.FATAL, ex.getMessage());
        } catch (FileNotFoundException ex) {
            System.err.println(i + " request failed " + ex);
            ex.printStackTrace();
            UtilLogger.logger.addFATALLog("Failed to open stream to URL: " + ex);
            throw new WebServerClientException(WebServerClientException.s_str_URL_NOT_FOUND, WebServerClientException.FATAL, ex.getMessage());
        } catch (IOException ex) {
            System.err.println(i + " request failed " + ex);
            ex.printStackTrace();
            UtilLogger.logger.addFATALLog("Error reading URL content: " + ex);
            throw new WebServerClientException(WebServerClientException.s_str_ERROR_READING_URL_CONTENT, WebServerClientException.FATAL, ex.getMessage());
        } catch (ClassNotFoundException cnfe) {
            System.err.println(i + " request failed " + cnfe);
            cnfe.printStackTrace();
            UtilLogger.logger.addFATALLog("CLasse not found excpetion" + cnfe);
            throw new WebServerClientException(WebServerClientException.s_str_CLASS_NOT_FOUND, WebServerClientException.FATAL, cnfe.getMessage());
        }
        return response;
    }

    @Override
    public synchronized WebResponse getObject(WebRequest webRequest) throws Exception {
        if (m_recallManager != null && !m_recallManager.getStatus()) {
            throw new Exception();
        }
        return getResponse(webRequest);
    }

    /**
     * Get a web response for a web request
     * 
     * @param webRequest
     *            {@link WebRequest}
     * @return {@link WebResponse}
     * @throws Exception
     *             : Throws a webServerClientException if we can't establish a
     *             connection with the server. Throws a soleil exception if we
     *             have a problem. Throws other exception otherwise
     * 
     */
    private WebResponse getResponse(WebRequest webRequest) throws Exception {
        try {
            if (webRequest != null) {
                webRequest.setApplication(getApplication());
            }
            WebResponse response = sendWebRequest(webRequest);
            if (m_recallManager != null) {
                m_recallManager.setStatus(true);
            }
            return response;
        } catch (WebServerClientException e) {
            if (m_recallManager != null && m_recallManager.call(e.getTechComment())) {
                m_recallManager.setStatus(false);
                m_recallManager.setMessage(e.getTechComment());
                return getResponse(webRequest);
            } else {
                throw e;
            }
        } catch (SoleilException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("WebServerClient.getResponse(WebRequest webRequest) :" + e.getMessage());
            throw e;
        }
    }

    /**
     * Return an URLConnection to send a query to the server.
     * 
     * @param strServlet_name
     * @return URLConnection
     * @throws MalformedURLException
     * @throws IOException
     */
    private URLConnection getServletConnection(String strServlet_name) throws MalformedURLException, IOException {
        URL urlServlet = null;
        if (strServlet_name == null) {
            urlServlet = m_Url;
        } else {
            urlServlet = new URL(m_Url, strServlet_name);
        }
        URLConnection connection = urlServlet.openConnection();
        connection.setConnectTimeout(180000);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("Content-Type", "application/x-java-serialized-object");
        if (m_strJsessionid != null) {
            connection.setRequestProperty("Cookie", m_strJsessionid);
        }
        return connection;
    }

    public URL getUrl() {
        return m_Url;
    }

    public void setUrl(URL url) {
        this.m_Url = url;
    }

    @Override
    public IRecallManager getRecallManager() {
        return m_recallManager;
    }

    @Override
    public void setRecallManager(IRecallManager manager) {
        m_recallManager = manager;
    }

    @Override
    public ILoginAction getM_iLoginAction() {
        return m_iLoginAction;
    }

    @Override
    public void setM_iLoginAction(ILoginAction loginAction) {
        m_iLoginAction = loginAction;
    }

    @Override
    public String getApplication() {
        return m_strApplication;
    }

    @Override
    public void setApplication(String application) {
        m_strApplication = application;
    }

    public String getM_strJsessionid() {
        return m_strJsessionid;
    }

    public void setM_strJsessionid(String jsessionid) {
        m_strJsessionid = jsessionid;
    }
}
