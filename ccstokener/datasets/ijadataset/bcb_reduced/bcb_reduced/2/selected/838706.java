package net.sourceforge.buildmonitor.monitors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.sourceforge.buildmonitor.BuildMonitor;
import net.sourceforge.buildmonitor.BuildReport;
import net.sourceforge.buildmonitor.MonitoringException;
import net.sourceforge.buildmonitor.BuildReport.Status;
import net.sourceforge.buildmonitor.dialogs.BambooPropertiesDialog;
import org.xml.sax.InputSource;

/**
 * The run method of this Runnable implementation monitor a Bamboo build
 * 
 * @author sbrunot
 * 
 */
public class BambooMonitor implements Monitor {

    /**
	 * Set of properties needed to monitor a bamboo server.
	 */
    private class BambooProperties {

        private static final String BAMBOO_PASSWORD_PROPERTY_KEY = "bamboo.password";

        private static final String BAMBOO_USERNAME_PROPERTY_KEY = "bamboo.username";

        private static final String UPDATE_PERIOD_IN_SECONDS_PROPERTY_KEY = "update.period.in.seconds";

        private static final String BAMBOO_SERVER_BASE_URL_PROPERTY_KEY = "bamboo.server.base.url";

        private static final String USER_PROPERTIES_FILE = "bamboo-monitor.properties";

        private String serverBaseUrl = null;

        private String username = null;

        private String password = null;

        private Integer updatePeriodInSeconds = null;

        /**
		 * Get URL to the bamboo server
		 * @return the URL to the bamboo server
		 */
        public String getServerBaseUrl() {
            return this.serverBaseUrl;
        }

        /**
		 * Set URL to the bamboo server
		 * @param theServerBaseUrl the URL to the bamboo server
		 */
        public void setServerBaseUrl(String theServerBaseUrl) {
            this.serverBaseUrl = theServerBaseUrl;
            if (this.serverBaseUrl != null && this.serverBaseUrl.endsWith("/")) {
                this.serverBaseUrl = this.serverBaseUrl.substring(0, this.serverBaseUrl.length() - 1);
            }
        }

        /**
		 * Get the period (in seconds) of build status update
		 * @return the period (in seconds) of build status update
		 */
        public Integer getUpdatePeriodInSeconds() {
            return this.updatePeriodInSeconds;
        }

        /**
		 * Set the period (in seconds) of build status update
		 * @param theUpdatePeriodInSeconds the period (in seconds) of build status update
		 */
        public void setUpdatePeriodInSeconds(Integer theUpdatePeriodInSeconds) {
            this.updatePeriodInSeconds = theUpdatePeriodInSeconds;
        }

        /**
		 * Get the bamboo user name
		 * @return the bamboo user name
		 */
        public String getUsername() {
            return this.username;
        }

        /**
		 * Set the bamboo user name
		 * @param theUsername the bamboo user name
		 */
        public void setUsername(String theUsername) {
            this.username = theUsername;
        }

        /**
		 * Get the bamboo user password
		 */
        public String getPassword() {
            return this.password;
        }

        /**
		 * Set the bamboo user password
		 * @param thePassword the bamboo user password
		 */
        public void setPassword(String thePassword) {
            this.password = thePassword;
        }

        /**
		 * Load the properties from the {@link #USER_PROPERTIES_FILE} file in the
		 * user home directory.
		 */
        public void loadFromFile() throws FileNotFoundException, IOException {
            Properties bambooMonitorProperties = new Properties();
            File bambooMonitorPropertiesFile = new File(System.getProperty("user.home"), USER_PROPERTIES_FILE);
            if (bambooMonitorPropertiesFile.exists()) {
                FileInputStream bambooMonitorPropertiesFileIS = new FileInputStream(bambooMonitorPropertiesFile);
                bambooMonitorProperties.load(bambooMonitorPropertiesFileIS);
                bambooMonitorPropertiesFileIS.close();
            }
            synchronized (this) {
                setServerBaseUrl(bambooMonitorProperties.getProperty(BAMBOO_SERVER_BASE_URL_PROPERTY_KEY));
                String updatePeriodInSecondsAsString = bambooMonitorProperties.getProperty(UPDATE_PERIOD_IN_SECONDS_PROPERTY_KEY);
                if (updatePeriodInSecondsAsString != null) {
                    try {
                        setUpdatePeriodInSeconds(Integer.parseInt(updatePeriodInSecondsAsString));
                    } catch (NumberFormatException e) {
                        setUpdatePeriodInSeconds(300);
                    }
                }
                setUsername(bambooMonitorProperties.getProperty(BAMBOO_USERNAME_PROPERTY_KEY));
                setPassword(bambooMonitorProperties.getProperty(BAMBOO_PASSWORD_PROPERTY_KEY));
            }
        }

        /**
		 * Save the properties from the {@link #USER_PROPERTIES_FILE} file in the
		 * user home directory.
		 */
        public void saveToFile() throws FileNotFoundException, IOException {
            Properties bambooMonitorProperties = new Properties();
            synchronized (this) {
                bambooMonitorProperties.setProperty(BAMBOO_SERVER_BASE_URL_PROPERTY_KEY, this.serverBaseUrl);
                bambooMonitorProperties.setProperty(BAMBOO_USERNAME_PROPERTY_KEY, this.username);
                bambooMonitorProperties.setProperty(BAMBOO_PASSWORD_PROPERTY_KEY, this.password);
                bambooMonitorProperties.setProperty(UPDATE_PERIOD_IN_SECONDS_PROPERTY_KEY, "" + this.getUpdatePeriodInSeconds());
            }
            File bambooMonitorPropertiesFile = new File(System.getProperty("user.home"), USER_PROPERTIES_FILE);
            FileOutputStream buildMonitorPropertiesOutputStream = new FileOutputStream(bambooMonitorPropertiesFile);
            bambooMonitorProperties.store(buildMonitorPropertiesOutputStream, "File last updated on " + new Date());
            buildMonitorPropertiesOutputStream.close();
        }
    }

    private static final String REST_LOGIN_URL = "/api/rest/login.action";

    private static final String REST_LIST_BUILD_NAMES_URL = "/api/rest/listBuildNames.action";

    private static final String REST_GET_LATEST_BUILD_RESULTS_URL = "/api/rest/getLatestBuildResults.action";

    private static final String URL_ENCODING = "UTF-8";

    private BuildMonitor buildMonitorInstance = null;

    private boolean stop = false;

    private BambooProperties bambooProperties = new BambooProperties();

    private BambooPropertiesDialog optionsDialog = null;

    public BambooMonitor(BuildMonitor theBuildMonitorInstance) throws FileNotFoundException, IOException {
        this.buildMonitorInstance = theBuildMonitorInstance;
        this.optionsDialog = new BambooPropertiesDialog(null, true);
        this.optionsDialog.setIconImage(this.buildMonitorInstance.getDialogsDefaultIcon());
        this.optionsDialog.setTitle("Bamboo server monitoring parameters");
        this.optionsDialog.pack();
        this.bambooProperties.loadFromFile();
        if ((this.bambooProperties.getServerBaseUrl() == null) || (this.bambooProperties.getUpdatePeriodInSeconds() == null) || (this.bambooProperties.getUsername() == null) || (this.bambooProperties.getPassword() == null)) {
            displayOptionsDialog(true);
            if (this.optionsDialog.getLastClickedButton() != BambooPropertiesDialog.BUTTON_OK) {
                System.exit(0);
            }
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void run() {
        String authenticationIdentifier = getNewBambooTicket();
        while (!this.stop) {
            try {
                String bambooServerBaseUrl = null;
                Integer updatePeriodInSeconds = null;
                synchronized (this.bambooProperties) {
                    bambooServerBaseUrl = this.bambooProperties.getServerBaseUrl();
                    updatePeriodInSeconds = this.bambooProperties.getUpdatePeriodInSeconds();
                }
                Map<String, String> builds = listBuildNames(bambooServerBaseUrl, authenticationIdentifier);
                List<BuildReport> lastBuildStatus = new ArrayList<BuildReport>();
                for (String key : builds.keySet()) {
                    BuildReport lastBuildReport = getLatestBuildResults(bambooServerBaseUrl, authenticationIdentifier, key);
                    lastBuildReport.setName(builds.get(key));
                    lastBuildStatus.add(lastBuildReport);
                }
                this.buildMonitorInstance.updateBuildStatus(lastBuildStatus);
                try {
                    Thread.sleep(updatePeriodInSeconds * 1000);
                } catch (InterruptedException e) {
                }
            } catch (MonitoringException e) {
                if (e.getCause() != null && e.getCause() instanceof BambooTicketNeedToBeRenewedError) {
                    authenticationIdentifier = getNewBambooTicket();
                } else {
                    this.buildMonitorInstance.reportMonitoringException(e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e2) {
                    }
                }
            }
        }
    }

    /**
	 * Returns a new Bamboo authentication identifier (bamboo ticket)
	 * @return
	 */
    private String getNewBambooTicket() {
        while (true) {
            try {
                String bambooUsername = null;
                String bambooPassword = null;
                String bambooServerBaseUrl = null;
                synchronized (this.bambooProperties) {
                    bambooUsername = this.bambooProperties.getUsername();
                    bambooPassword = this.bambooProperties.getPassword();
                    bambooServerBaseUrl = this.bambooProperties.getServerBaseUrl();
                }
                return login(bambooServerBaseUrl, bambooUsername, bambooPassword);
            } catch (MonitoringException e) {
                this.buildMonitorInstance.reportMonitoringException(e);
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void stop() {
        this.stop = true;
    }

    /**
	 * {@inheritDoc}
	 */
    public URI getMainPageURI() {
        URI returnedValue = null;
        try {
            synchronized (this.bambooProperties) {
                returnedValue = new URI(this.bambooProperties.getServerBaseUrl());
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return returnedValue;
    }

    /**
	 * {@inheritDoc}
	 */
    public URI getBuildURI(String theIdOfTheBuild) {
        URI returnedValue = null;
        try {
            synchronized (this.bambooProperties) {
                returnedValue = new URI(this.bambooProperties.getServerBaseUrl() + "/browse/" + theIdOfTheBuild);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return returnedValue;
    }

    /**
	 * {@inheritDoc}
	 */
    public String getSystemTrayIconTooltipHeader() {
        synchronized (this.bambooProperties) {
            return "Monitoring Bamboo server at " + this.bambooProperties.getServerBaseUrl();
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void displayOptionsDialog() {
        displayOptionsDialog(false);
    }

    /**
	 * {@inheritDoc}
	 */
    public String getMonitoredBuildSystemName() {
        return "Bamboo server";
    }

    private void displayOptionsDialog(boolean isDialogOpenedForPropertiesCreation) {
        if (!this.optionsDialog.isVisible()) {
            if (this.bambooProperties.getServerBaseUrl() != null) {
                this.optionsDialog.baseURLField.setText(this.bambooProperties.getServerBaseUrl());
            } else {
                this.optionsDialog.baseURLField.setText("http://localhost:8085");
            }
            if (this.bambooProperties.getUsername() != null) {
                this.optionsDialog.usernameField.setText(this.bambooProperties.getUsername());
            } else {
                this.optionsDialog.usernameField.setText("");
            }
            if (this.bambooProperties.getPassword() != null) {
                this.optionsDialog.passwordField.setText(this.bambooProperties.getPassword());
            } else {
                this.optionsDialog.passwordField.setText("");
            }
            if (this.bambooProperties.getUpdatePeriodInSeconds() != null) {
                this.optionsDialog.updatePeriodField.setValue(this.bambooProperties.getUpdatePeriodInSeconds() / 60);
            } else {
                this.optionsDialog.updatePeriodField.setValue(5);
            }
            if (!isDialogOpenedForPropertiesCreation) {
                this.optionsDialog.updateBaseURLFieldStatus();
                this.optionsDialog.updateUsernameFieldStatus();
                this.optionsDialog.updatePasswordFieldStatus();
            }
            if (!this.optionsDialog.isDisplayable()) {
                this.optionsDialog.pack();
            }
            this.optionsDialog.setVisible(true);
            this.optionsDialog.toFront();
            if (this.optionsDialog.getLastClickedButton() == BambooPropertiesDialog.BUTTON_OK) {
                synchronized (this.bambooProperties) {
                    this.bambooProperties.setServerBaseUrl(this.optionsDialog.baseURLField.getText());
                    this.bambooProperties.setUsername(this.optionsDialog.usernameField.getText());
                    this.bambooProperties.setPassword(new String(this.optionsDialog.passwordField.getPassword()));
                    this.bambooProperties.setUpdatePeriodInSeconds((Integer) (this.optionsDialog.updatePeriodField.getValue()) * 60);
                }
                try {
                    this.bambooProperties.saveToFile();
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                this.buildMonitorInstance.reportConfigurationUpdatedToBeTakenIntoAccountImmediately();
            }
        } else {
            this.optionsDialog.setVisible(true);
            this.optionsDialog.toFront();
        }
    }

    /**
	 * Login and create an authentication token.
	 * Returns the token if login was successful, or returns an error (MonitoringException) otherwise.
	 * @param theBambooServerBaseURL base URL to the bamboo server
	 */
    private String login(String theBambooServerBaseURL, String theUsername, String thePassword) throws MonitoringException {
        String returnedValue = null;
        try {
            URL methodURL = new URL(theBambooServerBaseURL + REST_LOGIN_URL + "?username=" + URLEncoder.encode(theUsername, URL_ENCODING) + "&password=" + URLEncoder.encode(thePassword, URL_ENCODING));
            String serverResponse = returnedValue = callBambooApi(methodURL);
            InputSource serverResponseIS = new InputSource(new StringReader(serverResponse));
            returnedValue = XPathFactory.newInstance().newXPath().evaluate("/response/auth", serverResponseIS);
        } catch (MonitoringException e) {
            throw e;
        } catch (Throwable e) {
            throw new MonitoringException(e, null);
        }
        return returnedValue;
    }

    /**
	 * Provides a list of all the builds on this Bamboo server.
	 * @param theBambooServerBaseURL
	 * @param theAuthenticationIdentifier
	 * @return
	 */
    private Map<String, String> listBuildNames(String theBambooServerBaseURL, String theAuthenticationIdentifier) throws MonitoringException {
        Map<String, String> returnedValue = new Hashtable<String, String>();
        try {
            URL methodURL = new URL(theBambooServerBaseURL + REST_LIST_BUILD_NAMES_URL + "?auth=" + URLEncoder.encode(theAuthenticationIdentifier, URL_ENCODING));
            String serverResponse = callBambooApi(methodURL);
            int currentBuildNameIndex = 1;
            boolean moreBuildNames = true;
            while (moreBuildNames) {
                InputSource serverResponseIS = new InputSource(new StringReader(serverResponse));
                String currentBuildName = XPathFactory.newInstance().newXPath().evaluate("/response/build[" + currentBuildNameIndex + "]/name", serverResponseIS);
                serverResponseIS = new InputSource(new StringReader(serverResponse));
                String currentBuildKey = XPathFactory.newInstance().newXPath().evaluate("/response/build[" + currentBuildNameIndex + "]/key", serverResponseIS);
                if ("".equals(currentBuildKey)) {
                    moreBuildNames = false;
                } else {
                    returnedValue.put(currentBuildKey, currentBuildName);
                    currentBuildNameIndex++;
                }
            }
        } catch (MonitoringException e) {
            throw e;
        } catch (Throwable e) {
            throw new MonitoringException(e, null);
        }
        return returnedValue;
    }

    /**
	 * Provides the latest build results for the given buildName.
	 * @param theAuthenticationIdentifier
	 * @param theBuildKey
	 * @return
	 */
    private BuildReport getLatestBuildResults(String theBambooServerBaseURL, String theAuthenticationIdentifier, String theBuildKey) throws MonitoringException {
        BuildReport returnedValue = null;
        try {
            URL methodURL = new URL(theBambooServerBaseURL + REST_GET_LATEST_BUILD_RESULTS_URL + "?auth=" + URLEncoder.encode(theAuthenticationIdentifier, URL_ENCODING) + "&buildKey=" + URLEncoder.encode(theBuildKey, URL_ENCODING));
            String serverResponse = callBambooApi(methodURL);
            returnedValue = new BuildReport();
            returnedValue.setId(theBuildKey);
            InputSource serverResponseIS = new InputSource(new StringReader(serverResponse));
            String buildState = XPathFactory.newInstance().newXPath().evaluate("/response/buildState", serverResponseIS);
            if ("Successful".equals(buildState)) {
                returnedValue.setStatus(Status.OK);
            } else if ("Failed".equals(buildState)) {
                returnedValue.setStatus(Status.FAILED);
            } else {
                throw new MonitoringException("Unknown build state " + buildState + " returned for build " + theBuildKey, null);
            }
            serverResponseIS = new InputSource(new StringReader(serverResponse));
            String buildTime = XPathFactory.newInstance().newXPath().evaluate("/response/buildTime", serverResponseIS);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            try {
                returnedValue.setDate(dateFormat.parse(buildTime));
            } catch (ParseException e) {
            }
            serverResponseIS = new InputSource(new StringReader(serverResponse));
        } catch (MonitoringException e) {
            throw e;
        } catch (Throwable t) {
            throw new MonitoringException(t, null);
        }
        return returnedValue;
    }

    /**
	 * Call a bamboo REST api method and return the result (or throw a MonitoringException)
	 * @param theURL
	 * @return
	 * @throws BambooTicketNeedToBeRenewedError error thrown is the current bamboo authentication
	 * ticket is not valid (anymore) and needs to be renewed.
	 */
    private String callBambooApi(URL theURL) throws MonitoringException, BambooTicketNeedToBeRenewedError {
        String returnedValue = null;
        HttpURLConnection urlConnection = null;
        BufferedReader urlConnectionReader = null;
        try {
            urlConnection = (HttpURLConnection) theURL.openConnection();
            urlConnection.connect();
            urlConnectionReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line = null;
            StringBuffer serverResponse = new StringBuffer();
            while ((line = urlConnectionReader.readLine()) != null) {
                serverResponse.append(line);
            }
            returnedValue = serverResponse.toString();
            if (returnedValue.contains("<title>Bamboo Setup Wizard - Atlassian Bamboo</title>")) {
                throw new MonitoringException("Your Bamboo server installation is not finished ! Double click here to complete the Bamboo Setup Wizard !", getMainPageURI());
            }
            InputSource is = new InputSource(new StringReader(serverResponse.toString()));
            XPath xpath = XPathFactory.newInstance().newXPath();
            String error = xpath.evaluate("/errors/error", is);
            if (!"".equals(error)) {
                if ("User not authenticated yet, or session timed out.".equals(error)) {
                    throw new BambooTicketNeedToBeRenewedError();
                } else {
                    boolean isErrorOptionsRelated = false;
                    URI uriForNonOptionsRelatedErrors = getMainPageURI();
                    if ("Invalid username or password.".equals(error)) {
                        isErrorOptionsRelated = true;
                    }
                    if ("The remote API has been disabled.".equals(error)) {
                        error += " Double click here to enable it.";
                        try {
                            synchronized (this.bambooProperties) {
                                uriForNonOptionsRelatedErrors = new URI(this.bambooProperties.getServerBaseUrl() + "/admin/configure!default.action");
                            }
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    throw new MonitoringException("Error reported by the Bamboo server: " + error, isErrorOptionsRelated, uriForNonOptionsRelatedErrors);
                }
            }
        } catch (ClassCastException e) {
            throw new MonitoringException("Problem: the base URL defined for the Bamboo server in Options is not an http URL.", true, null);
        } catch (UnknownHostException e) {
            throw new MonitoringException("Problem: cannot find host " + theURL.getHost() + " on the network.", true, null);
        } catch (ConnectException e) {
            throw new MonitoringException("Problem: cannot connect to port " + theURL.getPort() + " on host " + theURL.getHost() + ".", true, null);
        } catch (FileNotFoundException e) {
            throw new MonitoringException("Problem: cannot find the Bamboo server REST api using the base URL defined for the Bamboo server in Options. Seems that this URL is not the one to your Bamboo server home page...", true, null);
        } catch (SocketException e) {
            throw new MonitoringException("Problem: network error, connection lost.", null);
        } catch (XPathExpressionException e) {
            throw new MonitoringException("Problem: the Bamboo Server returned an unexpected content for attribute <error>: " + returnedValue, null);
        } catch (MonitoringException e) {
            throw e;
        } catch (Throwable t) {
            throw new MonitoringException(t, null);
        } finally {
            if (urlConnectionReader != null) {
                try {
                    urlConnectionReader.close();
                } catch (IOException e) {
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return returnedValue;
    }
}
