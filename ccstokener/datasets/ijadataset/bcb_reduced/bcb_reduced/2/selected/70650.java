package de.moonflower.jfritz.autoupdate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Dieser Thread �berpr�ft, ob eine neue Programmversion verf�gbar ist
 * 
 * Ist eine neue Version verf�gbar, werden die neuen Dateien in den
 * update-Ordner heruntergeladen und eine Datei deletefiles erstellt, in der die
 * zu l�schenden Dateien und Ordner drinstehen
 * 
 * @author Robert Palmer
 * 
 */
public class CheckVersionThread extends Thread {

    private static final String className = "(CheckVersionThread) ";

    private static final String pointPattern = "\\.";

    private static final String dividerPattern = "\\|";

    private transient String updateURL = "";

    private transient String versionFile = "";

    private transient String programVersion = "";

    private transient String newVersion = "";

    private transient String urlstr = "";

    private transient boolean newVerAvailable = false;

    public CheckVersionThread(final String programVersion, final String updateURL, final String versionFile) {
        super();
        this.programVersion = programVersion;
        this.updateURL = updateURL;
        this.versionFile = versionFile;
        urlstr = updateURL + versionFile;
    }

    public void run() {
        Logger.msg(className + "Check for new program version...");
        if (checkForNewVersion()) {
            newVerAvailable = true;
        } else {
            newVerAvailable = false;
        }
        Logger.msg(className + "...done");
    }

    /**
	 * Setzt die URL zum Update-Ordner auf der Homepage
	 * 
	 * @param URL
	 *            zum Update-Ordner auf der Homepage
	 */
    public void setUpdateURL(final String updateURL) {
        if (!updateURL.endsWith("/")) {
            updateURL.concat("/");
        }
        this.updateURL = updateURL;
    }

    /**
	 * Setzt den Dateiname auf die Datei, die die Versionsinformationen auf der
	 * Homepage enth�lt
	 * 
	 * @param Dateiname
	 */
    public void setVersionFile(final String versionFile) {
        this.versionFile = versionFile;
    }

    /**
	 * Setzt die aktuelle Programmversionsnummer
	 * 
	 * @param programVersion
	 */
    public void setProgramVersion(final String programVersion) {
        this.programVersion = programVersion;
    }

    /**
	 * �berpr�ft, ob eine neue Version verf�gbar ist
	 * 
	 * @return true, wenn neue Version verf�gbar
	 */
    private boolean checkForNewVersion() {
        boolean result = true;
        if ("0".equals(programVersion)) {
            result = false;
        }
        if (result) {
            urlstr = updateURL + versionFile;
            boolean foundNewVersion = false;
            BufferedReader buffReader;
            String currentLine;
            String remoteVersion;
            String localVersion;
            try {
                final URL url = new URL(urlstr);
                if (url != null) {
                    URLConnection con;
                    try {
                        con = url.openConnection();
                        con.setConnectTimeout(5000);
                        con.setReadTimeout(30000);
                        buffReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        currentLine = buffReader.readLine();
                        remoteVersion = currentLine.replaceFirst(pointPattern, dividerPattern).replaceAll(pointPattern, "").replaceFirst(dividerPattern, pointPattern);
                        localVersion = programVersion.replaceFirst(pointPattern, dividerPattern).replaceAll(pointPattern, "").replaceFirst(dividerPattern, pointPattern);
                        if (Double.valueOf(remoteVersion).compareTo(Double.valueOf(localVersion)) > 0) {
                            newVersion = currentLine;
                            foundNewVersion = true;
                        }
                        buffReader.close();
                    } catch (IOException e1) {
                        Logger.err(className + "Error while retrieving " + urlstr + " (possibly no connection to the internet)");
                    }
                }
            } catch (MalformedURLException e) {
                Logger.err(className + "URL invalid: " + urlstr);
            }
            result = foundNewVersion;
        }
        return result;
    }

    /**
	 * Ist eine neue Version verf�gbar?
	 * @return
	 */
    public boolean isNewVersionAvailable() {
        return newVerAvailable;
    }

    /**
	 * Liefert die neue Version zur�ck
	 * @return
	 */
    public String getNewVersion() {
        return newVersion;
    }
}
