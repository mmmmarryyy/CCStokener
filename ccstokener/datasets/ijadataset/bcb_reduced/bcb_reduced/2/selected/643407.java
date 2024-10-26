package ca.sqlpower.architect.swingui.action;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import ca.sqlpower.architect.ArchitectVersion;
import ca.sqlpower.architect.swingui.ASUtils;
import ca.sqlpower.architect.swingui.ArchitectFrame;
import ca.sqlpower.util.BrowserUtil;

/**
 * Checks for a newer version available and prompts user to update when a newer version is found.
 *
 */
public class CheckForUpdateAction extends AbstractArchitectAction {

    private static final Logger logger = Logger.getLogger(CheckForUpdateAction.class);

    private static final String VERSION_FILE_URL = "http://power-architect.sqlpower.ca/current_version";

    private static final String UPDATE_URL = "http://download.sqlpower.ca/architect/current.html";

    private String versionPropertyString;

    private HttpURLConnection urlc;

    private InputStream propertyInputStream;

    public CheckForUpdateAction(ArchitectFrame frame) {
        super(frame, Messages.getString("CheckForUpdateAction.name"), Messages.getString("CheckForUpdateAction.description"));
    }

    /**
     * Sends request to website to check for current version and displays result
     * message as appropriate.
     */
    public void actionPerformed(ActionEvent e) {
        checkForUpdate(true);
    }

    public void checkForUpdate(boolean verbose) {
        try {
            URL url = new URL(VERSION_FILE_URL);
            urlc = (HttpURLConnection) url.openConnection();
            urlc.setAllowUserInteraction(false);
            urlc.setRequestMethod("GET");
            urlc.setDoInput(true);
            urlc.setDoOutput(false);
            urlc.connect();
            propertyInputStream = urlc.getInputStream();
            Properties properties = new Properties();
            properties.load(propertyInputStream);
            versionPropertyString = properties.getProperty("app.version");
            ArchitectVersion latestVersion = new ArchitectVersion(versionPropertyString);
            ArchitectVersion userVersion = ArchitectVersion.APP_FULL_VERSION;
            if (userVersion.compareTo(latestVersion) < 0) {
                promptUpdate();
            } else if (verbose) {
                JOptionPane.showMessageDialog(getSession().getArchitectFrame(), Messages.getString("CheckForUpdateAction.upToDate"), Messages.getString("CheckForUpdateAction.name"), JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            logger.error("Fail to retrieve current version number!");
            if (verbose) {
                ASUtils.showExceptionDialogNoReport(getSession().getArchitectFrame(), Messages.getString("CheckForUpdateAction.failedToCheckForUpdate"), ex);
            }
        } finally {
            urlc.disconnect();
            try {
                if (propertyInputStream != null) {
                    propertyInputStream.close();
                }
            } catch (IOException ex2) {
                logger.error("Exception while trying to close input stream.");
                throw new RuntimeException(ex2);
            }
        }
    }

    /**
     * Prompts the user for update and opens browser to current version if accepted.
     */
    private void promptUpdate() {
        int response = JOptionPane.showConfirmDialog(getSession().getArchitectFrame(), Messages.getString("CheckForUpdateAction.newerVersionAvailable"), Messages.getString("CheckForUpdateAction.name"), JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            try {
                BrowserUtil.launch(UPDATE_URL);
            } catch (IOException e) {
                ASUtils.showExceptionDialogNoReport(getSession().getArchitectFrame(), Messages.getString("CheckForUpdateAction.failedToUpdate"), e);
            }
        }
    }
}
