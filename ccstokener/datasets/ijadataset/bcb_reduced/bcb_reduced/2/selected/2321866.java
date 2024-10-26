package com.aptana.ide.core.ui.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IStartup;
import org.eclipse.update.core.IFeature;
import com.aptana.ide.core.AptanaCorePlugin;
import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.PluginUtils;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.ui.AbstractPerspectiveFactory;
import com.aptana.ide.core.ui.CoreUIPlugin;
import com.aptana.ide.core.ui.WorkbenchHelper;
import com.aptana.ide.core.ui.preferences.IPreferenceConstants;

/**
 * This plug-in is loaded on startup to fork a job that searches for new plug-ins.
 */
public class SchedulerStartup implements IStartup {

    private static final long SLEEP_DELAY = 86400000L;

    /**
	 * P_ENABLED
	 */
    public static final String P_ENABLED = "enabled";

    /**
	 * P_SCHEDULE
	 */
    public static final String P_SCHEDULE = "schedule";

    /**
	 * VALUE_ON_STARTUP
	 */
    public static final String VALUE_ON_STARTUP = "on-startup";

    /**
	 * VALUE_ON_SCHEDULE
	 */
    public static final String VALUE_ON_SCHEDULE = "on-schedule";

    /**
	 * P_DOWNLOAD
	 */
    public static final String P_DOWNLOAD = "download";

    private Job job;

    static final Object automaticJobFamily = new Object();

    private IJobChangeListener jobListener;

    /**
	 * The constructor.
	 */
    public SchedulerStartup() {
        CoreUIPlugin.setScheduler(this);
    }

    /**
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
    public void earlyStartup() {
        scheduleUpdateJob();
    }

    /**
	 * scheduleUpdateJob
	 */
    public void scheduleUpdateJob() {
        CoreUIPlugin plugin = CoreUIPlugin.getDefault();
        Preferences pref = plugin.getPluginPreferences();
        if (pref.getBoolean(P_ENABLED) == false) {
            return;
        }
        String schedule = pref.getString(P_SCHEDULE);
        long delay = -1L;
        if (schedule.equals(VALUE_ON_STARTUP)) {
            if (job == null) {
                delay = 30L * 1000L;
            } else {
                delay = -1L;
            }
        }
        if (delay == -1L) {
            return;
        }
        startSearch(delay, false);
        final Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    while (true) {
                        Thread.sleep(SLEEP_DELAY);
                        startSearch(0, false);
                    }
                } catch (InterruptedException e) {
                    IdeLog.logError(CoreUIPlugin.getDefault(), Messages.SchedulerStartup_Interrupting24HourThread, e);
                }
            }
        }, "Aptana: 24-hour update check");
        t.setDaemon(true);
        t.start();
    }

    /**
	 * startSearch
	 *
	 * @param delay
	 * @param showPrompt
	 */
    public void startSearch(long delay, boolean showPrompt) {
        if (job != null) {
            if (jobListener != null) {
                Platform.getJobManager().removeJobChangeListener(jobListener);
            }
            Platform.getJobManager().cancel(job);
        }
        if (jobListener == null) {
            jobListener = createJobChangeAdapter();
            if (jobListener == null) {
                return;
            }
        }
        Platform.getJobManager().addJobChangeListener(jobListener);
        String jobName = Messages.SchedulerStartup_SearchingForUpdates;
        boolean download = CoreUIPlugin.getDefault().getPluginPreferences().getBoolean(IPreferenceConstants.P_DOWNLOAD);
        job = createUpdateJob(jobName, download, showPrompt);
        if (job != null) {
            updateAnonymousId();
            job.schedule(delay);
        }
    }

    /**
	 * updateAnonymousId
	 * 
	 * Updates the IDE ID if it hasn't been set. This ID is used just to track updates, no personal information
	 * is shared or retained.
	 *
	 */
    private void updateAnonymousId() {
        URL url = null;
        boolean needId = false;
        ApplicationPreferences preferences = ApplicationPreferences.getInstance();
        preferences.loadPreferences();
        boolean hasRun = preferences.getBoolean(IPreferenceConstants.P_IDE_HAS_RUN);
        String applicationId = preferences.getString(IPreferenceConstants.P_IDE_ID);
        Preferences p = CoreUIPlugin.getDefault().getPluginPreferences();
        String workspaceId = p.getString(IPreferenceConstants.P_IDE_ID);
        if (applicationId == null || applicationId.length() == 0) {
            if (workspaceId == null || workspaceId.length() == 0) {
                applicationId = "none";
                needId = true;
            } else {
                applicationId = workspaceId;
                preferences.setString(IPreferenceConstants.P_IDE_ID, applicationId);
                preferences.savePreferences();
            }
        } else {
            if (workspaceId != null && workspaceId.length() > 0 && workspaceId.equals(applicationId) == false) {
                applicationId = workspaceId;
            }
        }
        String queryString = null;
        try {
            String version = PluginUtils.getPluginVersion(AptanaCorePlugin.getDefault());
            String eclipseVersion = System.getProperty("osgi.framework.version");
            String product = StringUtils.replaceNullWithEmpty(System.getProperty("eclipse.product"));
            String osarch = StringUtils.replaceNullWithEmpty(StringUtils.urlEncodeForSpaces(System.getProperty("os.arch")));
            String osname = StringUtils.replaceNullWithEmpty(StringUtils.urlEncodeForSpaces(System.getProperty("os.name")));
            String osversion = StringUtils.replaceNullWithEmpty(StringUtils.urlEncodeForSpaces(System.getProperty("os.version")));
            queryString = "id=" + applicationId + "&v=" + version + "&p=" + product + "&ev=" + eclipseVersion + "&osa=" + osarch + "&osn=" + osname + "&osv=" + osversion;
            url = new URL("http://www.aptana.com/update.php?" + queryString);
            URLConnection conn = url.openConnection();
            OutputStreamWriter wr = null;
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            if (wr != null) {
                wr.close();
            }
            rd.close();
            String newId = sb.toString();
            if (needId == true) {
                preferences.setString(IPreferenceConstants.P_IDE_ID, newId);
                preferences.setBoolean(IPreferenceConstants.P_IDE_HAS_RUN, true);
                preferences.savePreferences();
            }
        } catch (UnknownHostException e) {
            return;
        } catch (MalformedURLException e) {
            IdeLog.logError(CoreUIPlugin.getDefault(), StringUtils.format(Messages.SchedulerStartup_UrlIsMalformed, url), e);
        } catch (IOException e) {
            if (needId && hasRun == false) {
                WorkbenchHelper.launchBrowser("http://www.aptana.com/install.php?" + queryString);
                preferences.setBoolean(IPreferenceConstants.P_IDE_HAS_RUN, true);
                preferences.savePreferences();
            }
        } catch (Exception e) {
            IdeLog.logError(CoreUIPlugin.getDefault(), Messages.SchedulerStartup_UnableToContactUpdateServer, e);
        }
    }

    private Job createUpdateJob(String name, boolean download, boolean showPrompt) {
        try {
            IFeature[] features = AbstractPerspectiveFactory.getFeature("com.aptana.ide.feature.rcp", false);
            if (features.length == 0) {
                features = AbstractPerspectiveFactory.getFeature("com.aptana.ide.feature", false);
            }
            return new AutomaticUpdateJob(name, true, download, features, showPrompt);
        } catch (Exception e) {
            CoreUIPlugin.logException(e, false);
            return null;
        }
    }

    private IJobChangeListener createJobChangeAdapter() {
        try {
            Class theClass = Class.forName("com.aptana.ide.core.ui.update.UpdateJobChangeAdapter");
            Constructor constructor = theClass.getConstructor(new Class[] { SchedulerStartup.class });
            return (IJobChangeListener) constructor.newInstance(new Object[] { this });
        } catch (Exception e) {
            CoreUIPlugin.logException(e, false);
            return null;
        }
    }

    Job getJob() {
        return job;
    }
}
