package com.kerrybuckley.cctrayicon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract implementation of {@link CcStatus}, with no display functionality.
 * 
 * @author Kerry Buckley
 */
public abstract class AbstractCcStatus implements CcStatus {

    protected static final Preferences PREFS = Preferences.userNodeForPackage(CcStatus.class);

    /** Regular expression to extract data from a row in the HTML page. */
    private static final Pattern ROW_PARSER_PATTERN = Pattern.compile(MessageUtils.getString("pattern.ccServer.rowParser"), Pattern.CASE_INSENSITIVE);

    /**
     * Group in <code>ROW_PARSER_PATTERN</code> which contains the project
     * name.
     */
    private static final int GROUP_PROJECT = 2;

    /**
     * Group in <code>ROW_PARSER_PATTERN</code> which contains the build
     * status.
     */
    private static final int GROUP_STATUS = 3;

    protected static final String PREF_KEY_URL = "url";

    protected static final String DEFAULT_URL = MessageUtils.getString("default.serverUrl");

    protected static final String PREF_KEY_PROJECTS = "projects";

    protected static final String PREF_KEY_ALERT_ON_FAIL = "alertOnFail";

    private static final boolean DEFAULT_ALERT_ON_FAIL = true;

    protected static final String PREF_KEY_ALERT_ON_FIX = "alertOnFix";

    private static final boolean DEFAULT_ALERT_ON_FIX = true;

    protected static final String PREF_KEY_ALERT_ON_SUCCESS = "alertOnSuccess";

    private static final boolean DEFAULT_ALERT_ON_SUCCESS = false;

    private URL url = null;

    private CcStatusMonitor monitor = null;

    private boolean validServerUrl = false;

    private boolean serverReachable = false;

    private boolean buildClean = true;

    private List projectsToCheck = null;

    private boolean alertOnFail = true;

    private boolean alertOnFix = true;

    private boolean alertOnSuccess = false;

    private boolean checkAllProjects = true;

    private Preferences prefs = null;

    /**
     * Construct a new status object, reading server details from user
     * preferences.
     * 
     * @param monitor
     *            the monitor which this object should communicate
     */
    public AbstractCcStatus(final CcStatusMonitor monitor) {
        this.monitor = monitor;
        this.prefs = PREFS;
        readPrefs();
        doUpdate(false);
    }

    /**
     * Construct a new status object using the specified URL. Does not read from
     * user preferences. <strong>This constructor is only intended for testing.</strong>
     * 
     * @param monitor
     *            the monitor which this object should communicate
     * @param serverUrl
     *            the URL of the Cruise Control server status page
     * @param prefs
     *            a <code>Preferences</code> object to read from and write to.
     */
    protected AbstractCcStatus(final CcStatusMonitor monitor, final String serverUrl, final Preferences prefs) {
        this.monitor = monitor;
        this.prefs = prefs;
        this.projectsToCheck = new ArrayList();
        try {
            this.url = new URL(serverUrl);
            validServerUrl = true;
            doUpdate(false);
        } catch (MalformedURLException e) {
            validServerUrl = false;
        }
    }

    /**
     * @see com.kerrybuckley.cctrayicon.CcStatus#isBuildClean()
     */
    public boolean isBuildClean() throws IOException {
        return serverReachable && buildClean;
    }

    /**
     * @see com.kerrybuckley.cctrayicon.CcStatus#isServerReachable()
     */
    public boolean isServerReachable() {
        return serverReachable;
    }

    /**
     * @see com.kerrybuckley.cctrayicon.CcStatus#update()
     */
    public void update() {
        doUpdate(true);
    }

    /**
     * Set the URL. <strong>Intended for testing only, to simulate a change in
     * status.</strong>
     * 
     * @param serverUrl
     *            the new URL (as a string)
     * @throws MalformedURLException
     *             if the URL can't be parsed
     */
    protected void setUrl(final String serverUrl) throws MalformedURLException {
        url = new URL(serverUrl);
    }

    /**
     * @see com.kerrybuckley.cctrayicon.CcStatus#setServer(java.lang.String)
     */
    public void setServer(final String serverUrl) {
        URL oldUrl = url;
        try {
            url = new URL(serverUrl);
            validServerUrl = true;
            writePrefs();
            update();
        } catch (MalformedURLException e) {
            validServerUrl = false;
            warn(MessageUtils.getString("error.invalidUrl", new String[] { serverUrl }));
            url = oldUrl;
        }
    }

    /**
     * @return the configured server URL, as a string
     */
    protected String getServer() {
        return url.toString();
    }

    /**
     * Generate a warning to the user.
     * 
     * @param message
     *            the message to display
     */
    protected abstract void warn(final String message);

    /**
     * Update the status from the CruiseControl web page.
     * 
     * @param notifyOnChange
     *            whether to send a notification to the monitor if the build
     *            status has changed.
     */
    private void doUpdate(final boolean notifyOnChange) {
        if (!validServerUrl) {
            return;
        }
        boolean tempBuildClean = true;
        List failedBuilds = new ArrayList();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                Matcher matcher = ROW_PARSER_PATTERN.matcher(line);
                if (matcher.matches() && checkAllProjects) {
                    String project = matcher.group(GROUP_PROJECT);
                    String status = matcher.group(GROUP_STATUS);
                    if (status.equals(MessageUtils.getString("ccOutput.status.failed"))) {
                        tempBuildClean = false;
                        failedBuilds.add(project);
                    }
                }
            }
        } catch (IOException e) {
            serverReachable = false;
            if (notifyOnChange) {
                monitor.notifyServerUnreachable(MessageUtils.getString("error.readError", new String[] { url.toString() }));
            }
            return;
        }
        if (notifyOnChange && buildClean && !tempBuildClean) {
            monitor.notifyBuildFailure(MessageUtils.getString("message.buildFailed", new Object[] { failedBuilds.get(0) }));
        }
        if (notifyOnChange && !buildClean && tempBuildClean) {
            monitor.notifyBuildFixed(MessageUtils.getString("message.allBuildsClean"));
        }
        buildClean = tempBuildClean;
        monitor.setStatus(buildClean);
        serverReachable = true;
    }

    /**
     * Read the current configuration from the user preferences, or set defaults
     * if no preferences found.
     */
    protected void readPrefs() {
        this.alertOnFail = prefs.getBoolean(PREF_KEY_ALERT_ON_FAIL, DEFAULT_ALERT_ON_FAIL);
        this.alertOnFix = prefs.getBoolean(PREF_KEY_ALERT_ON_FIX, DEFAULT_ALERT_ON_FIX);
        this.alertOnSuccess = prefs.getBoolean(PREF_KEY_ALERT_ON_SUCCESS, DEFAULT_ALERT_ON_SUCCESS);
        try {
            url = new URL(prefs.get(PREF_KEY_URL, DEFAULT_URL));
            validServerUrl = true;
        } catch (MalformedURLException e) {
            try {
                url = new URL(DEFAULT_URL);
                validServerUrl = true;
                prefs.put(PREF_KEY_URL, DEFAULT_URL);
            } catch (MalformedURLException e1) {
                validServerUrl = false;
            }
        }
        Preferences projects = prefs.node(PREF_KEY_PROJECTS);
        String[] projectNames = null;
        try {
            projectNames = projects.childrenNames();
        } catch (BackingStoreException e) {
            projectNames = new String[0];
        }
        checkAllProjects = (projectNames.length == 0);
        projectsToCheck = new ArrayList(projectNames.length);
        for (int i = 0; i < projectNames.length; i++) {
            projectsToCheck.add(projectNames[i]);
        }
    }

    /**
     * Write the current configuration to the user preferences.
     */
    protected void writePrefs() {
        prefs.put(PREF_KEY_URL, url.toString());
        prefs.putBoolean(PREF_KEY_ALERT_ON_FAIL, alertOnFail);
        prefs.putBoolean(PREF_KEY_ALERT_ON_FIX, alertOnFix);
        prefs.putBoolean(PREF_KEY_ALERT_ON_SUCCESS, alertOnSuccess);
        Preferences projects = prefs.node(PREF_KEY_PROJECTS);
        try {
            projects.clear();
        } catch (BackingStoreException e) {
        }
        Iterator i = projectsToCheck.iterator();
        while (i.hasNext()) {
            projects.putBoolean((String) i.next(), true);
        }
    }

    /**
     * @return Returns the alertOnFail.
     */
    protected boolean getAlertOnFail() {
        return alertOnFail;
    }

    /**
     * @return Returns the alertOnFix flag. <strong>For testing only.</strong>
     */
    protected boolean getAlertOnFix() {
        return alertOnFix;
    }

    /**
     * @return Returns the alertOnSuccess flag. <strong>For testing only.</strong>
     */
    protected boolean getAlertOnSuccess() {
        return alertOnSuccess;
    }

    /**
     * @return Returns the checkAllProjects flag. <strong>For testing only.</strong>
     */
    protected boolean getCheckAllProjects() {
        return checkAllProjects;
    }

    /**
     * @return Returns the projectsToCheck list. <strong>For testing only.</strong>
     */
    protected List getProjectsToCheck() {
        return projectsToCheck;
    }
}
