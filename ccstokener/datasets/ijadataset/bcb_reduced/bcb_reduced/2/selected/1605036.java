package hambo.positioningapi.business;

import java.util.Vector;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import hambo.positioningapi.business.mppnv.MPPNamedValue;
import hambo.positioningapi.business.mppnv.MPPNamedValueBuilder;
import hambo.positioningapi.business.PositioningAPIException;
import hambo.positioningapi.business.PositioningDataCollectorAdapter;
import hambo.positioningapi.business.PositioningDataCollector;
import hambo.positioningapi.data.LASPositioningResult;
import hambo.positioningapi.data.PositioningResult;
import hambo.svc.log.LogServiceManager;
import hambo.svc.log.Log;
import hambo.config.ConfigManager;
import hambo.config.Config;

/** 
 * This is the positioning data collector for the LAS protocol.
 * Responsible for communication with the server from which to receive LAS positioning data. 
 *
 * @see PositioningDataCollector
 */
public class LASPositioningDataCollector extends PositioningDataCollectorAdapter {

    private static final String DEFAULT = "DEFAULT";

    private static final String DEFAULTSYSTEM = "WGS84";

    private String session;

    private String horizontal = "1500";

    private String startTime = DEFAULT;

    private String repetitions = DEFAULT;

    private String interval = DEFAULT;

    private String responseTime = "10";

    private String requestID = DEFAULT;

    private String priority = DEFAULT;

    private String dtdUrl;

    private String requestType;

    private String request;

    /**
     * Constructs the LASPositioningDataCollector. If called with <code>true</code>,
     * the LASPositioningDataCollector tries to read userID, password, host, port,
     * coordinate system and geodetic system from config files.
     * In case these properties are not present, an exception is thrown. If it is
     * desirable to have only some properties in config files, the properties that
     * should be manually set or ignored must be set to the string DEFAULT (coordinate system and geodetic system)
     * or MANUAL_SET (user name and password) in the config file.
     * This is so in order to minimize the risc of errors resulting
     * from poor knowledge of what properties were actually set.
     *
     * @param useConfigs whether to try to read properties from config files
     * @exception InternalException if this constructor is called with <code>true</code>
     * and any property mentioned above is non-existent or null in the config file
     * where it should have been, this exception is thrown
     */
    public LASPositioningDataCollector(boolean useConfigs) throws InternalException {
        this.positioningSystem = "LAS";
        if (useConfigs) initiateConfigs();
    }

    /**
     * Calls <code>LASPositioningDataCollector(boolean useConfigs)</code>
     * with the default argument <code>true</code>.
     */
    public LASPositioningDataCollector() throws InternalException {
        this(true);
    }

    /**
     * Sets the current session. As of now, the user of the API need not
     * ever use this, as a new session is set each time a login request is
     * send. In the future, the session may remain the same for optimization
     * reasons.
     *
     * @param session the session
     *
     * @see #sendRequest()
     * @see #sendRequest(String type)
     */
    public void setSession(String session) {
        this.session = session;
    }

    /**
     * Sets the horizontal value that defines the accuracy of the request.
     * @param horizontal the horizontal value
     */
    public void setHorizontal(String horizontal) {
        this.horizontal = horizontal;
    }

    /**
     * Sets the start time for a deferred location request.
     * @param startTime the start time in format; "yyyymmddhhmm".
     */
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    /**
     * Sets number of repetitions for a deferred location request.
     * @param repetitions
     */
    public void setRepetitions(String repetitions) {
        this.repetitions = repetitions;
    }

    /**
     * Sets the interval for a deferred location request.
     * @param interval the interval between requests in seconds.
     */
    public void setInterval(String interval) {
        this.interval = interval;
    }

    /**
     * Sets the response time.
     * If we set the response time to 10 we want an answer within 10 seconds.
     * @param responseTime the response time
     */
    public void setResponseTime(String responseTime) {
        this.responseTime = responseTime;
    }

    /**
     * Sets the url to the directory where the dtd's are located. Should have been provided by the operator.
     * @param url the url to the dtd's
     */
    public void setDtdUrl(String url) {
        dtdUrl = url;
    }

    private void initiateConfigs() throws InternalException {
        Config config = ConfigManager.getConfig("positioningapi.las");
        theHost = config.getProperty("host");
        if (theHost == null) {
            LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.ERROR, "host property missing in config file");
            throw new InternalException("host property missing in config file");
        }
        dtdUrl = config.getProperty("dtd");
        if (dtdUrl == null) {
            LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.ERROR, "dtd-URL property missing in config file");
            throw new InternalException("dtd-URL property missing in config file");
        }
        coordinateSystem = config.getProperty("coordinateSystem");
        if (coordinateSystem == null || coordinateSystem.equalsIgnoreCase("DEFAULT")) {
            LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.DEBUG3, "CoordinateSystem property missing in config file, using default.");
            coordinateSystem = DEFAULTSYSTEM;
        }
        geodeticSystem = config.getProperty("geodeticSystem");
        if (geodeticSystem == null || geodeticSystem.equalsIgnoreCase("DEFAULT")) {
            LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.DEBUG3, "GeodeticSystem property missing in config file, using default.");
            geodeticSystem = DEFAULTSYSTEM;
        }
        userName = config.getProperty("user");
        if (userName == null) {
            LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.ERROR, "User property missing in config file");
            throw new InternalException("User property missing in config file");
        } else if (userName.equalsIgnoreCase("MANUAL_SET")) userName = null;
        password = config.getProperty("password");
        if (password == null) {
            LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.ERROR, "Password property missing in config file");
            throw new InternalException("Password property missing in config file");
        } else if (password.equalsIgnoreCase("MANUAL_SET")) password = null;
    }

    private void composeRequest() throws InternalException {
        composeRequest("ImmediateLocationDetermination");
    }

    private void composeRequest(String requestType) throws InternalException {
        this.requestType = requestType;
        String headerString = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + "<!DOCTYPE LAS_Protocol SYSTEM " + "\"" + dtdUrl + requestType + "Request.dtd\">";
        String sessionString;
        if (requestType.equals("Login")) sessionString = "<LAS_Protocol version=\"" + getVersionNr() + "\">"; else sessionString = "<LAS_Protocol version=\"" + getVersionNr() + "\" session=\"" + session + "\">";
        MPPNamedValue requestNV = new MPPNamedValue("Request");
        requestNV.appendAttribute(new MPPNamedValue("type", requestType));
        if (!(requestType.equals("Login")) && !(requestType.equals("Logout"))) validateParameters();
        if (requestType.equals("ImmediateLocationDetermination") || requestType.equals("DeferredLocationDetermination")) {
            MPPNamedValue parametersNV = new MPPNamedValue("Parameters");
            if ((horizontal != null && !horizontal.equalsIgnoreCase(DEFAULT)) || (!responseTime.equalsIgnoreCase(DEFAULT) && responseTime != null)) {
                MPPNamedValue qosNV = new MPPNamedValue("QoS");
                if (!horizontal.equalsIgnoreCase(DEFAULT) && horizontal != null) {
                    MPPNamedValue accuracyNV = new MPPNamedValue("Accuracy");
                    MPPNamedValue horizontalNV = new MPPNamedValue("Horizontal", horizontal);
                    accuracyNV.append(horizontalNV);
                    qosNV.append(accuracyNV);
                }
                if (!responseTime.equalsIgnoreCase(DEFAULT) && responseTime != null) {
                    MPPNamedValue responseTimeNV = new MPPNamedValue("ResponseTime", responseTime);
                    qosNV.append(responseTimeNV);
                }
                parametersNV.append(qosNV);
            }
            if (geodeticSystem != null) {
                if (geodeticSystem.equalsIgnoreCase(DEFAULT)) geodeticSystem = DEFAULTSYSTEM;
            } else geodeticSystem = DEFAULTSYSTEM;
            MPPNamedValue geodeticSystemNV = new MPPNamedValue("GeodeticSystem", geodeticSystem);
            parametersNV.append(geodeticSystemNV);
            if (coordinateSystem != null) {
                if (coordinateSystem.equalsIgnoreCase(DEFAULT)) coordinateSystem = DEFAULTSYSTEM;
            } else coordinateSystem = DEFAULTSYSTEM;
            MPPNamedValue coordinateSystemNV = new MPPNamedValue("CoordinateSystem", coordinateSystem);
            parametersNV.append(coordinateSystemNV);
            if (!priority.equalsIgnoreCase(DEFAULT) && priority != null) {
                MPPNamedValue priorityNV = new MPPNamedValue("Priority", priority);
                parametersNV.append(priorityNV);
            }
            requestNV.append(parametersNV);
            if (requestType.equals("DeferredLocationDetermination")) {
                MPPNamedValue eventNV = new MPPNamedValue("Event");
                eventNV.appendAttribute(new MPPNamedValue("type", "Timer"));
                MPPNamedValue startTimeNV = new MPPNamedValue("StartTime", startTime);
                eventNV.append(startTimeNV);
                MPPNamedValue repetitionsNV = new MPPNamedValue("Repetitions", repetitions);
                eventNV.append(repetitionsNV);
                MPPNamedValue intervalNV = new MPPNamedValue("Interval", interval);
                eventNV.append(intervalNV);
                requestNV.append(eventNV);
            }
            MPPNamedValue itemNV = new MPPNamedValue("Item");
            MPPNamedValue msisdnNV = new MPPNamedValue("MSISDN");
            for (int i = 0; i < MSISDNVector.size(); i++) {
                itemNV = new MPPNamedValue("Item");
                msisdnNV = new MPPNamedValue("MSISDN", (String) MSISDNVector.elementAt(i));
                itemNV.append(msisdnNV);
                requestNV.append(itemNV);
            }
        }
        if (requestType.equals("CancelDeferredRequest") || requestType.equals("Fetch")) {
            MPPNamedValue requestIDNV = new MPPNamedValue("RequestID", requestID);
            requestNV.append(requestIDNV);
        } else if (requestType.equals("Login")) {
            MPPNamedValue credentialsNV = new MPPNamedValue("Credentials");
            MPPNamedValue userNameNV = new MPPNamedValue("UserName", userName);
            credentialsNV.append(userNameNV);
            MPPNamedValue passwordNV = new MPPNamedValue("Password", password);
            credentialsNV.append(passwordNV);
            requestNV.append(credentialsNV);
        }
        this.request = headerString + sessionString + requestNV.toLASString() + "</LAS_Protocol>";
    }

    private void handleResponse(String nvStr) throws ExternalException, InternalException {
        if (nvStr.length() > 0) {
            MPPNamedValueBuilder nvb = new MPPNamedValueBuilder();
            try {
                MPPNamedValue nv = nvb.build(nvStr);
                LASPositioningResult result = new LASPositioningResult(nv);
                if (this.requestType.equalsIgnoreCase("Logout")) {
                    LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.DEBUG3, "User logged out...");
                    this.session = null;
                } else if (this.requestType.equalsIgnoreCase("Login")) {
                    this.session = result.getSession();
                } else positioningResultVector.addElement(result);
            } catch (IOException ioe) {
                LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.ERROR, "Error building LASPositioningResult", ioe);
                throw new ExternalException(positioningSystem, ioe.toString());
            }
        } else {
            LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.ERROR, "no response received from LAS");
            throw new ExternalException("no response received from LAS");
        }
    }

    private void validateParameters() throws InternalException {
        String errorString = "";
        if (!requestType.equals("Login")) {
            if (session == null) {
                errorString += "Session unspecified, the user has not logged in. ";
            }
            if (!requestType.equals("AvailableMSISDN") && !requestType.equals("Logout") && !requestType.equals("Fetch")) {
                if (MSISDNVector.size() == 0) {
                    errorString += "No MSISDN added to the request. ";
                }
            }
        }
        if (theHost == null) {
            errorString += "Host unspecified. ";
        }
        if (userName == null) {
            errorString += "Username unspecified. ";
        }
        if (password == null) {
            errorString += "Password unspecified. ";
        }
        if (dtdUrl == null) {
            errorString += "No URL to the dtd's added to the request. ";
        }
        if (!errorString.equals("")) {
            LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.ERROR, errorString);
            throw new InternalException(errorString);
        }
    }

    private String getVersionNr() {
        Config config = ConfigManager.getConfig("application.positioningapi.LAS.");
        String tempVersion = config.getProperty("version");
        if (tempVersion == null || tempVersion.equals("DEFAULT")) {
            return "0.1.1";
        } else {
            return tempVersion;
        }
    }

    private void simpleSend() throws PositioningAPIException {
        try {
            URL url = new URL(theHost);
            HttpURLConnection connection;
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            OutputStreamWriter outputStream = new OutputStreamWriter(connection.getOutputStream());
            String data = "protocolData=" + request;
            outputStream.write(data, 0, data.length());
            outputStream.flush();
            outputStream.close();
            String responseLine;
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = "";
            while (true) {
                responseLine = inputStream.readLine();
                if (responseLine == null) break;
                response += responseLine;
            }
            if (response.equals("")) {
                LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.ERROR, "No data returned in answer from LAS");
                throw new ExternalException(positioningSystem, "No data returned in answer from LAS");
            }
            handleResponse(response);
            inputStream.close();
        } catch (IOException ioe) {
            LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.ERROR, "Error in communication with LAS positioning server: ", ioe);
            throw new PositioningAPIException(ioe.toString());
        }
    }

    /**
     * Gets the positioning result as a LAS result, meaning that
     * all data retrieved can be accessed.
     * @return the result of the positioning as a LAS result
     * @exception throws InternalException if a result cannot be retrieved
     */
    public LASPositioningResult getLASPositioningResult() throws InternalException {
        if (positioningResultVector.size() > 0) {
            LASPositioningResult returnRes = (LASPositioningResult) positioningResultVector.elementAt(0);
            positioningResultVector.removeElementAt(0);
            return returnRes;
        } else {
            LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.ERROR, "either sendRequest was not called prior to calling getLASPositioningResult or all positioning results have already been retrieved");
            throw new InternalException(positioningSystem, "either sendRequest was not called prior to calling getLASPositioningResult or all positioning results have already been retrieved");
        }
    }

    /**
     * Gets the MSISDN's that are available for positioning by the requester.
     *
     * This method will perform a login if necessary, but it will not logout the user when done, since
     * the user might want to make additional requests.
     * @return a Vector containing the available MSISDN's
     */
    public Vector getAvailableMSISDNS() throws PositioningAPIException {
        Vector v = null;
        if (session == null) {
            sendLoginRequest();
        }
        composeRequest("AvailableMSISDN");
        simpleSend();
        LASPositioningResult result = getLASPositioningResult();
        if (result != null) v = result.getAvailableMSISDN(); else LogServiceManager.getLog("positioningAPI, LASPositioningDataCollector").println(Log.ERROR, "Attempt to retrieve available msisdn vector without an available LASPositioningResult");
        return v;
    }

    /**
     * Sends a login request.
     * A successful login sets the session which allows the user to make positioning requests.
     * 
     */
    public void sendLoginRequest() throws PositioningAPIException {
        composeRequest("Login");
        simpleSend();
    }

    /**
     * Sends a logout request and sets the session to null.
     * In order to perform positioning requests the user has to login again once this method is called.
     */
    public void sendLogoutRequest() throws PositioningAPIException {
        if (session != null) {
            composeRequest("Logout");
            simpleSend();
        }
    }

    /**
     * Cancels a deferred location request.
     * This method will perform a login if necessary, but it will not logout the user when done, since
     * the user might want to make additional requests.
     * @param id the request ID that was received when the deferred request was made
     */
    public void sendCancelDeferredRequest(String id) throws PositioningAPIException {
        this.requestID = id;
        if (session == null) {
            sendLoginRequest();
        }
        composeRequest("CancelDeferredRequest");
        simpleSend();
    }

    /**
     * Sends a deferred location request.
     * Note that <code>setInterval(String interval)</code>, <code>setRepetitions(String repetitions)</code>
     * and <code>setResponseTime(String responseTime)</code> must have been called prior to this method.
     * The result is fetched by calling <code>sendFetchRequest(String id)</code> with the
     * request ID send as an argument.
     * This method will perform a login if necessary, but it will not logout the user when done, since
     * the user might want to make additional requests.
     * @exception InternalException thrown if the requirements
     *            outlined above are not met
     * @exception ExternalException indicates a server side error
     */
    public void sendDeferredRequest() throws PositioningAPIException {
        if (session == null) {
            sendLoginRequest();
        }
        composeRequest("DeferredLocationDetermination");
        simpleSend();
    }

    /**
     * Sends an immediate location request.
     * 
     * This method will perform a login if necessary, but it will not logout the user when done, since
     * the user might want to make additional requests.
     *
     */
    public void sendImmediateRequest() throws PositioningAPIException {
        if (session == null) {
            sendLoginRequest();
        }
        composeRequest("ImmediateLocationDetermination");
        simpleSend();
    }

    /**
     * Sends a fetch request to fetch results from a deferred location request.
     * This method will perform a login if necessary, but it will not logout the user when done, since
     * the user might want to make additional requests.
     * @param id the request ID that was received when the deferred request was made
     *
     */
    public void sendFetchRequest(String id) throws PositioningAPIException {
        this.requestID = id;
        if (session == null) {
            sendLoginRequest();
        }
        composeRequest("Fetch");
        simpleSend();
    }

    /**
     * Sends a request with the default request type <code>ImmediateLocationRequest</code>.
     * This method also takes care of the login and logout requests.
     * <code>addMSISDN()</code> must have been called prior to 
     * calling this method. If the dtdUrl, host, user and password
     * is not specified in config files, <code>setDtdUrl(String dtdUrl)</code>,
     * <code>setHost(String host)</code>, <code>setUser(String user)</code>, 
     * <code>setPassword(String password)</code> must also be called prior to
     * calling this method.
     *
     * @exception InternalException thrown if the requirements
     *            outlined above are not met
     * @exception ExternalException indicates a server side error
     */
    public void sendRequest() throws PositioningAPIException {
        if (session == null) sendLoginRequest();
        sendImmediateRequest();
        sendLogoutRequest();
    }
}
