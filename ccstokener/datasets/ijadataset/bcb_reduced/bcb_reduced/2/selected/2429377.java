package slotimaker.SipgateSMSClient.utils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import slotimaker.SipgateSMSClient.core.Constants;
import slotimaker.SipgateSMSClient.core.Core;

/**
 * Checks if a newer version of this program is downloadable
 */
public class UpdateChecker implements Runnable {

    private static boolean isRunning = false;

    private boolean firstRun;

    /**
	 * if firstrun, there won't be an errormessage if no connection is possible
	 */
    public UpdateChecker(boolean firstRun) {
        super();
        this.firstRun = firstRun;
    }

    public UpdateChecker() {
        super();
        this.firstRun = false;
    }

    public void run() {
        if (isRunning) return;
        isRunning = true;
        Core core = Core.getInstance();
        URL url = null;
        InputStream input = null;
        DataInputStream datastream;
        try {
            url = new URL(Constants.UpdateCheckUrl);
        } catch (MalformedURLException e) {
            if (!firstRun) core.showMessage(1, core.getString("error"), core.getString("errorUpdateCheck"));
            isRunning = false;
            return;
        }
        try {
            input = url.openStream();
        } catch (IOException e) {
            if (!firstRun) core.showMessage(1, core.getString("error"), core.getString("errorUpdateCheck"));
            isRunning = false;
            return;
        }
        datastream = new DataInputStream(new BufferedInputStream(input));
        String line = null;
        try {
            line = datastream.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            if (!firstRun) core.showMessage(1, core.getString("error"), core.getString("errorUpdateCheck"));
            isRunning = false;
            return;
        }
        if (line == null) {
            if (!firstRun) core.showMessage(1, core.getString("error"), core.getString("errorUpdateCheck"));
            isRunning = false;
            return;
        }
        if (line.trim().equalsIgnoreCase(Constants.version)) {
            if (!firstRun) core.showMessage(0, core.getString("checkUpdateButton"), core.getString("versionMatch"));
        } else {
            core.showMessage(1, core.getString("checkUpdateButton"), core.getString("errorNewerVersion") + ": " + line);
        }
        isRunning = false;
    }
}
