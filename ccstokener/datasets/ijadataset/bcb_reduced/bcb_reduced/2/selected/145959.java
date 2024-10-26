package yaddur.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import yaddur.util.IPValidator;
import yaddur.util.Printer;
import yaddur.util.PropertiesFile;

/**
 * Checks current IP address
 * 
 * @author Viktoras Agejevas
 * @version $Id: IPChecker.java 16 2008-11-28 18:32:07Z inversion $
 *
 */
public class IPChecker {

    /**
	 * Number of times to try to resolve IP
	 */
    private int retries;

    /**
	 * List of IP resolving sites
	 */
    private String[] sites;

    /**
	 * Number of IP resolving sites
	 */
    private int size;

    /**
	 * Index number of current IP resolver site.
	 * Sites rotate for every request.
	 */
    private int currentIndex;

    public IPChecker() throws IOException {
        PropertiesFile pf = new PropertiesFile("config.properties");
        sites = pf.getProperty("yaddur.ipchecker.sites").split(",");
        size = sites.length;
        currentIndex = 0;
        retries = size - 1;
    }

    /**
	 * Get current IP address. If resolving fails, it will attempt to retry
	 * with another site. With no retries left it will return null.
	 * 
	 * @return resolved IP
	 */
    public String getCurrentIp() {
        String site = sites[getCurrentIndex()];
        rotateCurrentIndex();
        Printer.debug("Checking IP with: " + site);
        try {
            URL url = new URL(site);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            String fullResult = null;
            while ((line = in.readLine()) != null) {
                fullResult = fullResult + line;
            }
            connection.disconnect();
            String currentIp = IPValidator.findIp(fullResult);
            if (currentIp == null) {
                throw new IOException("Failed to determine current IP");
            }
            Printer.debug("IP resolved to: " + currentIp);
            retries = size - 1;
            return currentIp;
        } catch (IOException e) {
            if (retries > 0) {
                Printer.debug("Failed to resolve IP, retrying...");
                retries--;
                return getCurrentIp();
            } else {
                Printer.error(e.getMessage());
                retries = size - 1;
                return null;
            }
        }
    }

    /**
	 * Return current position of site to use.
	 * @return position of current site in site list
	 */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
	 * Move index value by one position to the right, 
	 * if index is at the rightmost position,
	 * reset it to initial value of 0.
	 */
    private void rotateCurrentIndex() {
        if (currentIndex < size - 1) {
            currentIndex++;
        } else {
            currentIndex = 0;
        }
    }

    public int getRetries() {
        return retries;
    }

    public String[] getSites() {
        return sites;
    }

    public int getSize() {
        return size;
    }
}
