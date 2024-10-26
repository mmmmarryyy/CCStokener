package org.intelligentsia.keystone.updater;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.intelligentsia.utilities.CheckSum;
import org.intelligentsia.utilities.FileUtils;

/**
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 */
public class WebUpdate {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String updateSite;

    private final File root;

    private UpdateInformation updateInformation;

    /**
	 * Used for testing
	 */
    WebUpdate() {
        this("file:" + new File(".").getPath());
    }

    /**
	 * Build a new instance of WebUpdate.
	 * 
	 * @param updateSite
	 */
    public WebUpdate(final String updateSite) {
        this(updateSite, new File("."));
    }

    /**
	 * Build a new instance of WebUpdate.
	 * 
	 * @param updateSite
	 * @param root
	 */
    public WebUpdate(final String updateSite, final File root) {
        assert (updateSite != null) && (root != null) && root.exists();
        this.updateSite = updateSite.endsWith("/") ? updateSite : updateSite + "/";
        this.root = root;
    }

    /**
	 * Check if resource located at ${updatesite}/update.json contains update.
	 * 
	 * @return true if an update is availaible
	 */
    public boolean checkForUpdate() {
        String data = null;
        try {
            data = WebUpdate.getData(updateSite + "update.json");
        } catch (final IOException ex) {
            return false;
        }
        if (data != null) {
            try {
                updateInformation = new UpdateInformation(new JSONObject(data));
            } catch (final JSONException ex) {
                logger.error("when reading json update information", ex);
                return false;
            }
            return (updateInformation.getVersion() > 0);
        }
        return false;
    }

    /**
	 * Download resources in update folder specified by update information previously loaded.
	 * If checksum is present in update information, then it will be checked.
	 * 
	 * @return a list of resources successfully downloaded.
	 */
    public List<Link> download() {
        final List<Link> result = new ArrayList<Link>();
        if (updateInformation != null) {
            final File update = new File(root, "update");
            if (download(update, updateInformation)) {
                result.add(updateInformation);
            }
            for (final Link link : updateInformation.getPlugins()) {
                final File plugins = new File(update, "plugins");
                if (download(plugins, link)) {
                    result.add(link);
                }
            }
        }
        return result;
    }

    /**
	 * @return name of target update information if present, null otherwise.
	 */
    public String getTarget() {
        return updateInformation != null ? updateInformation.getName() : null;
    }

    protected boolean download(final File home, final Link link) {
        logger.trace("download {}", link);
        if ((link.getUrl() == null) || "".equals(link.getUrl())) {
            return false;
        }
        final String linkUrl = updateSite + link.getUrl();
        final File target = new File(home, link.getName().replace("/", File.pathSeparator));
        if (target.exists()) {
            target.delete();
        }
        if (!target.getParentFile().exists()) {
            target.getParentFile().mkdirs();
        }
        logger.debug("Download {} to {}", linkUrl, target.getPath());
        URL url = null;
        try {
            url = new URL(linkUrl);
        } catch (final MalformedURLException ex) {
            logger.error("Downloading {} ", linkUrl, ex);
            target.delete();
            return false;
        }
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            final URLConnection conn = url.openConnection();
            inputStream = conn.getInputStream();
            logger.debug("Downloading file, update Size(compressed): {} Bytes", conn.getContentLength());
            outputStream = new BufferedOutputStream(new FileOutputStream(target));
            FileUtils.copyStream(inputStream, outputStream);
            outputStream.flush();
            final Boolean check = link != null ? CheckSum.validate(target.getPath(), link.getChecksum()) : Boolean.TRUE;
            logger.debug("Download Complete! {}", check);
            return check;
        } catch (final IOException ex) {
            logger.error("Downloading {} ", linkUrl, ex);
            target.delete();
            return false;
        } finally {
            FileUtils.close(inputStream);
            FileUtils.close(outputStream);
        }
    }

    private static String getData(final String address) throws IOException {
        final URL url = new URL(address);
        InputStream html = null;
        try {
            html = url.openStream();
            int c = 0;
            final StringBuilder buffer = new StringBuilder("");
            while (c != -1) {
                c = html.read();
                buffer.append((char) c);
            }
            return buffer.toString();
        } finally {
            if (html != null) {
                html.close();
            }
        }
    }
}
