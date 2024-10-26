package org.mandiwala.selenium.reportscreencastprocessor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mandiwala.PrefixedPropertyReader;
import org.mandiwala.selenium.SeleniumConfiguration;
import org.mandiwala.utils.FilesystemUtils;

/**
 * This is an abstract class for {@link ReportScreencastProcessor}s that
 * download files, such as images or stylesheets. If a file is referenced many
 * times by specifying a certain url, it's downloaded only once.
 */
public abstract class AbstractFileFetchingReportScreencastProcessor implements ReportScreencastProcessor {

    /**
     * Prefix for the stored file
     */
    private static final String PREFIX = "prefix";

    /**
     * Suffix for the stored file
     */
    private static final String SUFFIX = "suffix";

    /**
     * File download directory
     */
    private static final String DOWNLOAD_DIR = "downloadDir";

    private static final Log LOG = LogFactory.getLog("MANDIWALA");

    /**
     * Stores info about already downloaded files, so that we don't download
     * them several times. The entries look like this: {@code [downloadDirectory
     * [sourceHref, downloadedFile]]}
     */
    private static Map<File, Map<String, File>> downloadedFiles = new HashMap<File, Map<String, File>>();

    /**
     * Filename suffix.
     */
    protected String suffix;

    /**
     * Filename prefix.
     */
    protected String prefix;

    private String baseUrl;

    private File downloadDir;

    /**
     * {@inheritDoc}
     */
    public void init(SeleniumConfiguration seleniumConfiguration) {
        PrefixedPropertyReader prefixedPropertyReader = new PrefixedPropertyReader(getClass().getName(), seleniumConfiguration.getProps());
        baseUrl = seleniumConfiguration.getTestConfiguration().getBaseUrl();
        downloadDir = FilesystemUtils.relativePath(seleniumConfiguration.getTestConfiguration().getReportDir(), new File(prefixedPropertyReader.getPrefixedProperty(DOWNLOAD_DIR, true)));
        prefix = prefixedPropertyReader.getPrefixedProperty(PREFIX, true);
        suffix = prefixedPropertyReader.getPrefixedProperty(SUFFIX, false);
        if (suffix == null) {
            suffix = "";
        }
    }

    /**
     * Downloads the specified file.
     * 
     * @param href
     *            Href pointing to the file to download
     * 
     * @return the downloaded file
     */
    protected File downloadFile(String href) {
        Map<String, File> currentDownloadDirMap = downloadedFiles.get(downloadDir);
        if (currentDownloadDirMap != null) {
            File downloadedFile = currentDownloadDirMap.get(href);
            if (downloadedFile != null) {
                return downloadedFile;
            }
        } else {
            downloadedFiles.put(downloadDir, new HashMap<String, File>());
            currentDownloadDirMap = downloadedFiles.get(downloadDir);
        }
        URL url;
        File result;
        try {
            FilesystemUtils.forceMkdirIfNotExists(downloadDir);
            url = generateUrl(href);
            result = createUniqueFile(downloadDir, href);
        } catch (IOException e) {
            LOG.warn("Failed to create file for download", e);
            return null;
        }
        currentDownloadDirMap.put(href, result);
        LOG.info("Downloading " + url);
        try {
            IOUtils.copy(url.openStream(), new FileOutputStream(result));
        } catch (IOException e) {
            LOG.warn("Failed to download file " + url);
        }
        return result;
    }

    private URL generateUrl(String href) throws MalformedURLException {
        if (href.contains(":")) {
            return new URL(href);
        } else {
            if (href.startsWith("/")) {
                baseUrl = baseUrl.replaceFirst(new URL(baseUrl).getPath(), "");
                href = href.substring(1);
            }
            return new URL(baseUrl + (baseUrl.endsWith("/") ? "" : "/") + href);
        }
    }

    private File createUniqueFile(File downloadDir, String href) throws IOException {
        File result;
        for (int i = 0; ; ++i) {
            result = new File(downloadDir, getFileName(i, href));
            if (!result.exists()) {
                if (result.createNewFile()) {
                    return result;
                } else {
                    if (!result.exists()) {
                        throw new IOException("Failed to create file " + result);
                    }
                }
            }
        }
    }

    /**
     * Returns the file name for the stored file
     * 
     * @param i
     *            unique index of the generated file
     * @param href
     *            href pointing to the downloaded file
     * @return name for the stored file
     */
    protected String getFileName(int i, String href) {
        return String.format("%s_%d.%s", prefix, i, suffix);
    }
}
