package jsattrak.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class TLEDownloader implements java.io.Serializable {

    String rootWeb = "http://celestrak.com/NORAD/elements/";

    public String[] fileNames = new String[] { "sts.txt", "stations.txt", "tle-new.txt", "weather.txt", "noaa.txt", "goes.txt", "resource.txt", "sarsat.txt", "dmc.txt", "tdrss.txt", "geo.txt", "intelsat.txt", "gorizont.txt", "raduga.txt", "molniya.txt", "iridium.txt", "orbcomm.txt", "globalstar.txt", "amateur.txt", "x-comm.txt", "other-comm.txt", "gps-ops.txt", "glo-ops.txt", "galileo.txt", "sbas.txt", "nnss.txt", "musson.txt", "science.txt", "geodetic.txt", "engineering.txt", "education.txt", "military.txt", "radar.txt", "cubesat.txt", "other.txt" };

    public String[] primCat = new String[] { "Special-Interest", "Special-Interest", "Special-Interest", "Weather & Earth Resources", "Weather & Earth Resources", "Weather & Earth Resources", "Weather & Earth Resources", "Weather & Earth Resources", "Weather & Earth Resources", "Weather & Earth Resources", "Communications", "Communications", "Communications", "Communications", "Communications", "Communications", "Communications", "Communications", "Communications", "Communications", "Communications", "Navigation", "Navigation", "Navigation", "Navigation", "Navigation", "Navigation", "Scientific", "Scientific", "Scientific", "Scientific", "Miscellaneous", "Miscellaneous", "Miscellaneous", "Miscellaneous" };

    public String[] secondCat = new String[] { "STS", "International Space Station", "Last 30 Days' Launches", "Weather", "NOAA", "GOES", "Earth Resources", "Search & Rescue (SARSAT)", "Disaster Monitoring", "Tracking and Data Relay Satellite System (TDRSS)", "Geostationary", "Intelsat", "Gorizont", "Raduga", "Molniya", "Iridium", "Orbcomm", "Globalstar", "Amateur Radio", "Experimental", "Other", "GPS Operational", "Glonass Operational", "Galileo", "Satellite-Based Augmentation System (WAAS/EGNOS/MSAS)", "Navy Navigation Satellite System (NNSS)", "Russian LEO Navigation", "Space & Earth Science", "Geodetic", "Engineering", "Education", "Miscellaneous Military", "Radar Calibration", "CubeSats", "Other" };

    private String localPath = "data/tle/";

    private String proxyHost = "proxy1.lmco.com";

    private String proxyPort = "80";

    private boolean usingProxy = false;

    String errorText = "";

    boolean downloadINI = false;

    int currentTLEindex = 0;

    public TLEDownloader() {
    }

    /**
     * downloads all the TLEs without stopping inbetween each file
     * @return if all files were downloaded successfully (if returns false see getErrorText for reason)
     */
    public boolean downloadAllTLEs() {
        boolean success = true;
        success = startTLEDownload();
        if (success) {
            while (this.hasMoreToDownload() && success) {
                success = this.downloadNextTLE();
            }
        }
        return success;
    }

    /**
     * Starts the TLE download process
     * return boolean indecating if everything is ready to download
     */
    public boolean startTLEDownload() {
        boolean success = true;
        if (usingProxy) {
            Properties systemSettings = System.getProperties();
            systemSettings.put("proxySet", "true");
            systemSettings.put("http.proxyHost", proxyHost);
            systemSettings.put("http.proxyPort", proxyPort);
        }
        if (!(new File(getLocalPath()).exists())) {
            success = (new File(getLocalPath())).mkdirs();
            if (!success) {
                errorText = "Error Creating Local TLE Data Directory: Check File Permissions";
                return false;
            }
        }
        currentTLEindex = 0;
        downloadINI = success;
        return success;
    }

    /**
     * a check to see if there are more files to download
     * @return
     */
    public boolean hasMoreToDownload() {
        if (!downloadINI) {
            return false;
        }
        if (currentTLEindex >= fileNames.length) {
            return false;
        }
        return true;
    }

    public String getNextFileName() {
        String result = "";
        if (!this.hasMoreToDownload()) {
            return result;
        }
        result = fileNames[currentTLEindex];
        return result;
    }

    /**
     * gets the percent complete - integers 0-100
     * @return percent complete
     */
    public int getPercentComplete() {
        if (!this.hasMoreToDownload()) {
            return 0;
        } else {
            return (int) Math.round((currentTLEindex * 100.0) / fileNames.length);
        }
    }

    /**
     * downloads all the TLEs without stopping inbetween each file
     * @return if all files were downloaded successfully
     */
    public boolean downloadNextTLE() {
        boolean success = true;
        if (!downloadINI) {
            errorText = "startTLEDownload() must be ran before downloadNextTLE() can begin";
            return false;
        }
        if (!this.hasMoreToDownload()) {
            errorText = "There are no more TLEs to download";
            return false;
        }
        int i = currentTLEindex;
        try {
            URL url = new URL(rootWeb + fileNames[i]);
            URLConnection c = url.openConnection();
            InputStreamReader isr = new InputStreamReader(c.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            File outFile = new File(localPath + fileNames[i]);
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
            String currentLine = "";
            while ((currentLine = br.readLine()) != null) {
                writer.write(currentLine);
                writer.newLine();
            }
            br.close();
            writer.close();
        } catch (Exception e) {
            System.out.println("Error Reading/Writing TLE - " + fileNames[i] + "\n" + e.toString());
            success = false;
            errorText = e.toString();
            return false;
        }
        currentTLEindex++;
        return success;
    }

    public String getErrorText() {
        return errorText;
    }

    public static void main(String[] args) {
        TLEDownloader td = new TLEDownloader();
        boolean result = td.downloadAllTLEs();
        System.out.println("Update of TLEs was sucessful? : " + result);
    }

    public int getTleFileCount() {
        return fileNames.length;
    }

    public String getTleFilePath(int index) {
        return (localPath + fileNames[index]);
    }

    public String getTleWebPath(int index) {
        return rootWeb + fileNames[index];
    }

    public void setUsingProxy(boolean b) {
        this.usingProxy = b;
    }

    public boolean getUsingProxy() {
        return usingProxy;
    }

    public void setProxyPort(String portStr) {
        this.proxyPort = portStr;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }
}
