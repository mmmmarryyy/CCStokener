package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.apache.log4j.Logger;

/**
 * Checks a single file on a web server that supports the last-modified header
 *
 * @author <a href="mailto:yourgod@users.sourceforge.net">Brad Clarke</a>
 */
public class HttpFile extends FakeUserSourceControl {

    private static Logger log = Logger.getLogger(HttpFile.class);

    private String urlString;

    public void setURL(String urlString) {
        this.urlString = urlString;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(urlString, "url", this.getClass());
        try {
            new URL(this.urlString);
        } catch (MalformedURLException e) {
            ValidationHelper.fail("'url' is not a valid connection string", e);
        }
    }

    /**
     * For this case, we don't care about the quietperiod, only that
     * one user is modifying the build.
     *
     * @param lastBuild date of last build
     * @param now IGNORED
     */
    public List getModifications(Date lastBuild, Date now) {
        long lastModified;
        final URL url;
        try {
            url = new URL(this.urlString);
        } catch (MalformedURLException e) {
            return new ArrayList();
        }
        try {
            lastModified = getURLLastModified(url);
        } catch (IOException e) {
            log.error("Could not connect to 'url'", e);
            return new ArrayList();
        }
        List modifiedList = new ArrayList();
        if (lastModified > lastBuild.getTime()) {
            Modification mod = new Modification("http");
            mod.createModifiedFile(getFilename(url), url.getHost());
            mod.userName = getUserName();
            mod.modifiedTime = new Date(lastModified);
            mod.comment = "";
            modifiedList.add(mod);
        }
        if (!modifiedList.isEmpty()) {
            getSourceControlProperties().modificationFound();
        }
        return modifiedList;
    }

    private String getFilename(final URL url) {
        String fileName = url.getFile();
        if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }
        return fileName;
    }

    protected long getURLLastModified(final URL url) throws IOException {
        final URLConnection con = url.openConnection();
        long lastModified = con.getLastModified();
        try {
            con.getInputStream().close();
        } catch (IOException ignored) {
        }
        return lastModified;
    }
}
