package maldade.action;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import maldade.windowslivelogin.WindowsLiveLogin;
import maldade.windowslivelogin.WindowsLiveXmlParser;
import maldade.windowslivelogin.xmlobjects.Contacts;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Action used to get the user's contacts from hotmail.
 * 
 * @author rnojiri
 */
public class HotmailContactListAction extends AbstractMappingDispatchAction {

    private static final String WINDOWS_LIVE_LOGIN_OFFER = "Contacts.View";

    private static final String ATTRIB_WINDOWS_LIVE_CONSENT_URL = "windowsLiveConsentUrl";

    /**
	 * Redirects to windows live delegated authorization page if no token is
	 * found.
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return ActionForward
	 */
    public ActionForward windowsLiveAuth(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        WindowsLiveLogin windowsLiveLogin = getWindowsLiveLogin();
        URL consentUrl = windowsLiveLogin.getConsentUrl(WINDOWS_LIVE_LOGIN_OFFER);
        request.setAttribute(ATTRIB_WINDOWS_LIVE_CONSENT_URL, consentUrl.toString());
        return mapping.findForward("windowsLiveAuth");
    }

    /**
	 * Process the windows live delegated authorization return and tries to get the user contact list from the Microsoft's web service.
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return ActionForward
	 */
    @SuppressWarnings("unchecked")
    public ActionForward windowsLiveAuthReturn(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        WindowsLiveLogin windowsLiveLogin = getWindowsLiveLogin();
        WindowsLiveLogin.ConsentToken consentToken = windowsLiveLogin.processConsent(request.getParameterMap());
        if (consentToken != null && consentToken.isValid()) {
            try {
                URL url = new URL("https://livecontacts.services.live.com/users/@L@" + consentToken.getLocationID() + "/rest/LiveContacts/Contacts");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                setupConnection(connection, consentToken.getDelegationToken());
                connection.connect();
                WindowsLiveXmlParser windowsLiveXmlParser = WindowsLiveXmlParser.getInstance();
                InputStream inputStream = connection.getInputStream();
                if (inputStream == null) {
                    log.error("Input stream received from connection is null!");
                } else {
                    Contacts contacts = windowsLiveXmlParser.convertoXml2Object(inputStream);
                    request.setAttribute("contacts", contacts);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return mapping.findForward("windowsLiveContacts");
    }

    /**
	 * Setups the URL connection to the web service.
	 * 
	 * @param connection
	 * @param delegationToken
	 * @throws ProtocolException
	 */
    private void setupConnection(HttpURLConnection connection, String delegationToken) throws ProtocolException {
        connection.setRequestProperty("Request-Method", "GET");
        connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        connection.setRequestProperty("Authorization", "DelegatedToken dt=\"" + delegationToken + "\"");
        connection.setRequestProperty("Content-Length", "0");
        connection.setRequestMethod("GET");
        connection.setDoOutput(false);
        connection.setDoInput(true);
    }

    /**
	 * Returns a new instance of WindowsLiveLogin.
	 * 
	 * @return WindowsLiveLogin
	 */
    private WindowsLiveLogin getWindowsLiveLogin() {
        WindowsLiveLogin windowsLiveLogin = new WindowsLiveLogin(configuration.getWindowsLiveDelegatedAuthPath());
        WindowsLiveLogin.setDebug(false);
        return windowsLiveLogin;
    }
}
