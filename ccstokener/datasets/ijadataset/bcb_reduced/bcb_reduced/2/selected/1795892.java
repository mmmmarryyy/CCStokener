package tr.autoversion;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import org.openide.util.NbBundle;
import tr.appl.prefs.ApplicationPrefs;
import tr.appl.Constants;
import tr.util.DateUtils;

/**
 * Version check thread.
 *
 * @author Jeremy Moore (jimoore@netspace.net.au)
 */
final class VersionCheckThread extends Thread {

    private static final Logger LOG = Logger.getLogger("tr.autoversion");

    /**
     * Constructs a new instance.
     */
    public VersionCheckThread() {
    }

    /**
     * Runs the version check thread.
     */
    @Override
    public void run() {
        LOG.info("Starting");
        process();
        LOG.info("Finished");
    }

    private void process() {
        switch(ApplicationPrefs.getVersionCheckPeriod()) {
            case ApplicationPrefs.VERSION_CHECK_PERIOD_NEVER:
                LOG.fine("User preference Check Period is set to NEVER");
                return;
            case ApplicationPrefs.VERSION_CHECK_PERIOD_STARTUP:
                LOG.fine("User preference Check Period is set to STARTUP");
                checkVersion();
                return;
            default:
                if (checkPeriodExpired()) {
                    LOG.fine("Period has expired");
                    checkVersion();
                } else {
                    LOG.fine("Period has not expired");
                }
        }
    }

    private boolean checkPeriodExpired() {
        LOG.fine("Checking if period has expired ... ");
        Calendar checkDate = Calendar.getInstance();
        checkDate.setTimeInMillis(ApplicationPrefs.getCheckVersionLastTime());
        switch(ApplicationPrefs.getVersionCheckPeriod()) {
            case ApplicationPrefs.VERSION_CHECK_PERIOD_DAY:
                LOG.fine("User preference Check Period is set to DAY");
                checkDate.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case ApplicationPrefs.VERSION_CHECK_PERIOD_WEEK:
                LOG.fine("User preference Check Period is set to WEEK");
                checkDate.add(Calendar.DAY_OF_YEAR, 7);
                break;
            case ApplicationPrefs.VERSION_CHECK_PERIOD_2_WEEKS:
                LOG.fine("User preference Check Period is set to TWO WEEKS");
                checkDate.add(Calendar.DAY_OF_YEAR, 14);
                break;
            case ApplicationPrefs.VERSION_CHECK_PERIOD_MONTH:
                LOG.fine("User preference Check Period is set to MONTH");
                checkDate.add(Calendar.MONTH, 1);
                break;
            default:
                LOG.severe("User preference Check Period is not recognized.");
                return false;
        }
        Calendar now = Calendar.getInstance();
        return now.after(checkDate);
    }

    private void checkVersion() {
        LOG.fine("Getting latest version number from web site");
        String latestVersion = getLatestVersion();
        if (latestVersion == null) {
            LOG.severe("Latest version was not obtained from web site");
            return;
        }
        try {
            int[] latestVersionArr = getVersionArr(latestVersion);
            int[] currentVersionArr = getVersionArr(Constants.VERSION);
            if (isNewer(latestVersionArr, currentVersionArr)) {
                notifyUser(latestVersion);
            }
        } catch (NumberFormatException ex) {
            LOG.severe("Version is not of correct format.");
            return;
        }
        LOG.fine("Setting last time checked");
        Date startOfToday = DateUtils.getStart(Calendar.getInstance().getTime());
        ApplicationPrefs.setCheckVersionLastTime(startOfToday.getTime());
    }

    String getLatestVersion() {
        try {
            URL url = new URL(Constants.VERSION_FILE_URL);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(15000);
            InputStream in = connection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            return br.readLine();
        } catch (Exception ex) {
            return null;
        }
    }

    int[] getVersionArr(String version) {
        StringTokenizer st = new StringTokenizer(version, ".");
        if (st.countTokens() < 3) {
            throw new NumberFormatException();
        }
        return new int[] { Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()) };
    }

    boolean isNewer(int[] latestVersionArr, int[] currentVersionArr) {
        for (int i = 0; i < 3; i++) {
            if (latestVersionArr[i] > currentVersionArr[i]) {
                return true;
            }
            if (latestVersionArr[i] < currentVersionArr[i]) {
                return false;
            }
        }
        return false;
    }

    private void notifyUser(String version) {
        String t = getHeading();
        String m = "\n\n" + getMessage(version, Constants.WEB_SITE) + "    \n\n ";
        JCheckBox icb = new JCheckBox(getTurnOff());
        Object[] options = { m, icb };
        JOptionPane.showOptionDialog(null, options, t, JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null);
        if (icb.isSelected()) {
            LOG.fine("Setting user preference Check Period to NEVER");
            ApplicationPrefs.setVersionCheckPeriod(ApplicationPrefs.VERSION_CHECK_PERIOD_NEVER);
        }
    }

    private String getHeading() {
        return NbBundle.getMessage(getClass(), "CTL_Heading");
    }

    private String getMessage(String version, String url) {
        return NbBundle.getMessage(getClass(), "CTL_Message", version, url);
    }

    private String getTurnOff() {
        return NbBundle.getMessage(getClass(), "CTL_TurnOff");
    }
}
