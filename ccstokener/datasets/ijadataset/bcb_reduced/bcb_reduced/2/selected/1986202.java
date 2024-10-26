package at.dotti.check4update;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class Check4Update {

    private String localVersion = null;

    private String latestVersion = null;

    /**
	 * @param url
	 *            URL to the php returning the latest release number
	 * @param ini
	 *            INI containg the applications version
	 * @throws IOException
	 */
    public Check4Update(URL url, File ini) throws IOException {
        localVersion = getLocalVersion(ini);
        latestVersion = getLatestVersion(url);
    }

    /**
	 * @return <p>
	 *         <code>TRUE</code> if versions are equal or <code>FALSE</code> if
	 *         a version could'nt be retrieved or versions are not the same.<br />
	 *         If <code>FALSE</code> is returned you can check
	 *         <code>latestVersionIsNaN()</code> and
	 *         <code>localVersionIsNaN</code> for the reason what went wrong.
	 *         </p>
	 */
    public boolean isLatestVersion() {
        return this.latestVersion != null && this.localVersion != null && this.latestVersion.trim().compareTo(this.localVersion.trim()) == 0;
    }

    /**
	 * @return <p>
	 *         <code>TRUE</code> if the latest version could'nt be retrieved.
	 *         </p>
	 */
    public boolean latestVersionIsNaN() {
        return this.latestVersion == null;
    }

    /**
	 * @return <p>
	 *         <code>TRUE</code> if the local version could'nt be retrieved.
	 *         </p>
	 */
    public boolean localVersionIsNaN() {
        return this.localVersion == null;
    }

    /**
	 * @param url
	 * @return
	 * @throws IOException
	 */
    private String getLatestVersion(URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.connect();
        BufferedReader br = new BufferedReader(new InputStreamReader(new BufferedInputStream(con.getInputStream())));
        String lines = "";
        String line = null;
        while ((line = br.readLine()) != null) {
            lines += line;
        }
        con.disconnect();
        return lines;
    }

    /**
	 * @param ini
	 * @return
	 * @throws IOException
	 */
    private String getLocalVersion(File ini) throws IOException {
        Properties props = new Properties();
        InputStream is = new FileInputStream(ini);
        if (is != null) {
            props.load(is);
        } else {
            throw new FileNotFoundException(ini.getPath());
        }
        return props.getProperty("version");
    }

    /**
	 * @return
	 */
    public String getLatestVersion() {
        return latestVersion;
    }

    /**
	 * @return
	 */
    public String getLocalVersion() {
        return localVersion;
    }
}
