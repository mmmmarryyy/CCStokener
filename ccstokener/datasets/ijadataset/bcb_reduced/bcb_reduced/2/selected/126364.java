package cz.fi.muni.xkremser.editor.server.handler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import javax.servlet.http.HttpSession;
import javax.inject.Inject;
import com.google.inject.Provider;
import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.server.actionhandler.ActionHandler;
import com.gwtplatform.dispatch.shared.ActionException;
import org.apache.log4j.Logger;
import cz.fi.muni.xkremser.editor.server.ServerUtils;
import cz.fi.muni.xkremser.editor.server.config.EditorConfiguration;
import cz.fi.muni.xkremser.editor.server.fedora.utils.RESTHelper;
import cz.fi.muni.xkremser.editor.shared.rpc.action.CheckAvailability;
import cz.fi.muni.xkremser.editor.shared.rpc.action.CheckAvailabilityAction;
import cz.fi.muni.xkremser.editor.shared.rpc.action.CheckAvailabilityResult;

/**
 * The Class PutRecentlyModifiedHandler.
 */
public class CheckAvailabilityHandler implements ActionHandler<CheckAvailabilityAction, CheckAvailabilityResult> {

    private static String SOME_STATIC_KRAMERIUS_PAGE = "/inc/home/info.jsp";

    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(CheckAvailabilityHandler.class.getPackage().toString());

    /** The configuration. */
    private final EditorConfiguration configuration;

    /** The http session provider. */
    @Inject
    private Provider<HttpSession> httpSessionProvider;

    /**
     * Instantiates a new put recently modified handler.
     * 
     * @param configuration
     *        the configuration
     */
    @Inject
    public CheckAvailabilityHandler(final EditorConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public CheckAvailabilityResult execute(final CheckAvailabilityAction action, final ExecutionContext context) throws ActionException {
        if (LOGGER.isDebugEnabled()) {
            String serverName = null;
            if (action.getServerId() == CheckAvailability.FEDORA_ID) {
                serverName = "fedora";
            } else if (action.getServerId() == CheckAvailability.KRAMERIUS_ID) {
                serverName = "kramerius";
            }
            LOGGER.debug("Processing action: CheckAvailability: " + serverName);
        }
        ServerUtils.checkExpiredSession(httpSessionProvider);
        boolean status = true;
        String url = null;
        String usr = "";
        String pass = "";
        if (action.getServerId() == CheckAvailability.FEDORA_ID) {
            url = configuration.getFedoraHost();
            usr = configuration.getFedoraLogin();
            pass = configuration.getFedoraPassword();
        } else if (action.getServerId() == CheckAvailability.KRAMERIUS_ID) {
            url = configuration.getKrameriusHost() + SOME_STATIC_KRAMERIUS_PAGE;
        } else {
            throw new ActionException("Unknown server id");
        }
        try {
            URLConnection con = RESTHelper.openConnection(url, usr, pass, false);
            if (con instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) con;
                int resp = httpConnection.getResponseCode();
                if (resp < 200 || resp >= 308) {
                    status = false;
                    LOGGER.info("Server " + url + " answered with HTTP code " + httpConnection.getResponseCode());
                }
            } else {
                status = false;
            }
        } catch (MalformedURLException e) {
            status = false;
            e.printStackTrace();
        } catch (IOException e) {
            status = false;
            e.printStackTrace();
        }
        return new CheckAvailabilityResult(status, url);
    }

    @Override
    public Class<CheckAvailabilityAction> getActionType() {
        return CheckAvailabilityAction.class;
    }

    @Override
    public void undo(CheckAvailabilityAction action, CheckAvailabilityResult result, ExecutionContext context) throws ActionException {
    }
}
