package au.edu.diasb.annotation.dannotate;

import static au.edu.diasb.danno.constants.DannotateProtocolConstants.DANNOTATE_ANNOY;
import static au.edu.diasb.danno.constants.DannotateProtocolConstants.DANNOTATE_CAN_SET_NAME;
import static au.edu.diasb.danno.constants.DannotateProtocolConstants.DANNOTATE_CREATE_GROUPS;
import static au.edu.diasb.danno.constants.DannotateProtocolConstants.DANNOTATE_DEBUG;
import static au.edu.diasb.danno.constants.DannotateProtocolConstants.DANNOTATE_FILTER;
import static au.edu.diasb.danno.constants.DannotateProtocolConstants.DANNOTATE_GROUP;
import static au.edu.diasb.danno.constants.DannotateProtocolConstants.DANNOTATE_LIVE_UPDATE;
import static au.edu.diasb.danno.constants.DannotateProtocolConstants.DANNOTATE_NAME;
import static au.edu.diasb.danno.constants.DannotateProtocolConstants.DANNOTATE_READ_GROUPS;
import static au.edu.diasb.danno.constants.DannotateProtocolConstants.DANNOTATE_REPLY_GROUPS;
import static au.edu.diasb.danno.constants.DannotateProtocolConstants.DANNOTATE_RESIZE;
import static au.edu.diasb.danno.constants.DannotateProtocolConstants.DANNOTATE_STYLE;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.SetCookie2;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import au.edu.diasb.annotation.danno.DannoIdentityProvider;
import au.edu.diasb.annotation.danno.DannotateAccessPolicy;
import au.edu.diasb.annotation.danno.protocol.JSONObjectResponseHandler;
import au.edu.diasb.chico.config.ImpossibleException;
import au.edu.diasb.chico.mvc.BaseController;
import au.edu.diasb.chico.mvc.RequestFailureException;
import au.edu.diasb.emmet.protocol.EmmetAction;
import au.edu.diasb.emmet.protocol.EmmetParameters;

/**
 * This implementation of the preference manager stores the logged in
 * user's preferences in his/her Emmet profile.
 * 
 * @author crawley
 */
public class EmmetPreferenceManager implements DannotatePreferenceManager {

    private final Logger logger = Logger.getLogger(this.getClass());

    private String emmetServiceUrl;

    private String emmetDomain;

    private int[] emmetPorts = new int[1];

    private boolean emmetSecure;

    private DannoIdentityProvider identityProvider;

    private DannotateAccessPolicy accessPolicy;

    private int defaultResize;

    public EmmetPreferenceManager(String emmetServiceUrl, boolean useHttps, int defaultResize, DannoIdentityProvider identityProvider, DannotateAccessPolicy accessPolicy) {
        super();
        if (useHttps && emmetServiceUrl.startsWith("http:")) {
            this.emmetServiceUrl = "https:" + emmetServiceUrl.substring("http:".length());
        } else {
            this.emmetServiceUrl = emmetServiceUrl;
        }
        try {
            URI uri = new URI(emmetServiceUrl);
            emmetDomain = uri.getHost();
            emmetSecure = uri.getScheme().equals("https");
            int port = uri.getPort();
            emmetPorts[0] = port > 0 ? port : emmetSecure ? 443 : 80;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid emmet service URL", ex);
        }
        this.identityProvider = identityProvider;
        this.accessPolicy = accessPolicy;
        this.defaultResize = defaultResize;
    }

    public DannotateResponse savePreferences(HttpServletRequest request, HttpServletResponse response) throws RequestFailureException {
        if (logger.isDebugEnabled()) {
            logger.debug("saving preferences to Emmet");
        }
        setProfileProperty(request, DANNOTATE_NAME, BaseController.getParameter(request, DANNOTATE_NAME));
        setProfileProperty(request, DANNOTATE_GROUP, BaseController.getOptionalParameter(request, DANNOTATE_GROUP, ""));
        setProfileProperty(request, DANNOTATE_STYLE, BaseController.getNonEmptyParameter(request, DANNOTATE_STYLE));
        setProfileProperty(request, DANNOTATE_RESIZE, BaseController.getMandatoryBooleanParameter(request, DANNOTATE_RESIZE));
        setProfileProperty(request, DANNOTATE_DEBUG, BaseController.getMandatoryBooleanParameter(request, DANNOTATE_DEBUG));
        setProfileProperty(request, DANNOTATE_FILTER, BaseController.getMandatoryBooleanParameter(request, DANNOTATE_FILTER));
        setProfileProperty(request, DANNOTATE_ANNOY, BaseController.getMandatoryBooleanParameter(request, DANNOTATE_ANNOY));
        setProfileProperty(request, DANNOTATE_LIVE_UPDATE, BaseController.getMandatoryBooleanParameter(request, DANNOTATE_LIVE_UPDATE));
        return new DannotateResponse(null, "updated");
    }

    private HttpClient buildClient(HttpServletRequest request) {
        DefaultHttpClient client = new DefaultHttpClient();
        for (Cookie cookie : request.getCookies()) {
            SetCookie2 apacheCookie = new BasicClientCookie2(cookie.getName(), cookie.getValue());
            apacheCookie.setDomain(emmetDomain);
            apacheCookie.setPorts(emmetPorts);
            apacheCookie.setSecure(emmetSecure);
            apacheCookie.setVersion(cookie.getVersion());
            apacheCookie.setPath("/");
            client.getCookieStore().addCookie(apacheCookie);
        }
        return client;
    }

    private void setProfileProperty(HttpServletRequest request, String name, boolean value) throws RequestFailureException {
        setProfileProperty(request, name, value ? "1" : "0");
    }

    private void setProfileProperty(HttpServletRequest request, String name, String value) throws RequestFailureException {
        HttpClient httpclient = buildClient(request);
        try {
            HttpPost post;
            try {
                post = new HttpPost(emmetServiceUrl + "?" + EmmetParameters.ACTION_PARAM + "=" + EmmetAction.setProperty + "&" + EmmetParameters.PROPERTY_NAME_PARAM + "=dannotate-" + name + "&" + EmmetParameters.PROPERTY_VALUE_PARAM + "=" + URLEncoder.encode(value, "UTF-8") + "&format=json");
            } catch (UnsupportedEncodingException ex) {
                throw new ImpossibleException(ex);
            }
            try {
                HttpResponse response = httpclient.execute(post);
                if (response.getStatusLine().getStatusCode() >= 300) {
                    logger.error("Emmet request failed: " + response.getStatusLine());
                    throw new RequestFailureException(500, "Cannot save property '" + name + "'");
                }
            } catch (ClientProtocolException ex) {
                throw new RequestFailureException(500, "Cannot save property '" + name + "'", ex);
            } catch (IOException ex) {
                throw new RequestFailureException(500, "Cannot save property '" + name + "'", ex);
            }
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
    }

    public DannotateResponse loadPreferences(HttpServletRequest request) throws IOException, RequestFailureException, JSONException {
        if (logger.isDebugEnabled()) {
            logger.debug("loading preferences from Emmet");
        }
        JSONObject json = new JSONObject();
        boolean canChangeNames = accessPolicy.canChangeNames(request);
        json.put(DANNOTATE_NAME, canChangeNames ? getProfileProperty(request, DANNOTATE_NAME, "Guest") : identityProvider.obtainHumanReadableName(request, true));
        json.put(DANNOTATE_GROUP, getProfileProperty(request, DANNOTATE_GROUP, ""));
        json.put(DANNOTATE_CAN_SET_NAME, canChangeNames ? 1 : 0);
        json.put(DANNOTATE_STYLE, toInt(getProfileProperty(request, DANNOTATE_STYLE, "1")));
        json.put(DANNOTATE_DEBUG, toInt(getProfileProperty(request, DANNOTATE_DEBUG, "0")));
        json.put(DANNOTATE_ANNOY, toInt(getProfileProperty(request, DANNOTATE_ANNOY, "1")));
        json.put(DANNOTATE_RESIZE, toInt(getProfileProperty(request, DANNOTATE_RESIZE, Integer.toString(defaultResize))));
        json.put(DANNOTATE_FILTER, toInt(getProfileProperty(request, DANNOTATE_FILTER, "0")));
        json.put(DANNOTATE_LIVE_UPDATE, toInt(getProfileProperty(request, DANNOTATE_LIVE_UPDATE, "1")));
        json.put(DANNOTATE_CREATE_GROUPS, new JSONArray(identityProvider.obtainCreateAccessGroups(request)));
        json.put(DANNOTATE_READ_GROUPS, new JSONArray(identityProvider.obtainReadAccessGroups(request)));
        json.put(DANNOTATE_REPLY_GROUPS, new JSONArray(identityProvider.obtainReplyAccessGroups(request)));
        JSONArray array = new JSONArray();
        array.put(json);
        return new DannotateResponse((HttpResponse) null, array);
    }

    private int toInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String getProfileProperty(HttpServletRequest request, String name, String dflt) throws RequestFailureException {
        HttpClient httpclient = buildClient(request);
        try {
            HttpGet get = new HttpGet(emmetServiceUrl + "?" + EmmetParameters.ACTION_PARAM + "=" + EmmetAction.getProperty + "&" + EmmetParameters.PROPERTY_NAME_PARAM + "=dannotate-" + name + "&format=json");
            JSONObjectResponseHandler handler = new JSONObjectResponseHandler();
            try {
                JSONObject response = httpclient.execute(get, handler);
                if (response == null || response.isNull("property")) {
                    return dflt;
                }
                JSONObject prop = response.getJSONObject("property");
                if (prop.isNull("value")) {
                    return dflt;
                } else {
                    return prop.getString("value");
                }
            } catch (ClientProtocolException ex) {
                throw new RequestFailureException(500, "Cannot fetch property '" + name + "'", ex);
            } catch (IOException ex) {
                throw new RequestFailureException(500, "Cannot fetch property '" + name + "'", ex);
            } catch (JSONException ex) {
                throw new RequestFailureException(500, "Cannot fetch property '" + name + "'", ex);
            }
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
    }
}
