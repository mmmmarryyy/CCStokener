package com.liferay.portal.util;

import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.util.Validator;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * <a href="BrowserLauncher.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 *
 */
public class BrowserLauncher implements Runnable {

    public void run() {
        if (Validator.isNull(PropsValues.BROWSER_LAUNCHER_URL)) {
            return;
        }
        for (int i = 0; i < 300; i++) {
            try {
                Thread.sleep(Time.SECOND * 1);
            } catch (InterruptedException ie) {
            }
            try {
                URL url = new URL(PropsValues.BROWSER_LAUNCHER_URL);
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                int responseCode = urlc.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try {
                        launchBrowser();
                    } catch (Exception e2) {
                    }
                    break;
                }
            } catch (Exception e1) {
            }
        }
    }

    protected void launchBrowser() throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        Runtime runtime = Runtime.getRuntime();
        if (os.indexOf("mac") >= 0) {
            launchBrowserApple(runtime);
        } else if (os.indexOf("win") >= 0) {
            launchBrowserWindows(runtime);
        } else {
            launchBrowserUnix(runtime);
        }
    }

    protected void launchBrowserApple(Runtime runtime) throws Exception {
        runtime.exec("open " + PropsValues.BROWSER_LAUNCHER_URL);
    }

    protected void launchBrowserUnix(Runtime runtime) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < _BROWSERS.length; i++) {
            if (i != 0) {
                sb.append(" || ");
            }
            sb.append(_BROWSERS[i]);
            sb.append(" \"");
            sb.append(PropsValues.BROWSER_LAUNCHER_URL);
            sb.append("\" ");
        }
        runtime.exec(new String[] { "sh", "-c", sb.toString() });
    }

    protected void launchBrowserWindows(Runtime runtime) throws Exception {
        runtime.exec("cmd.exe /c start " + PropsValues.BROWSER_LAUNCHER_URL);
    }

    private static final String[] _BROWSERS = { "firefox", "mozilla", "konqueror", "opera" };
}
