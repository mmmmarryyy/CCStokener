package docBuilder.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;
import org.apache.log4j.Logger;

/**
 * Downloads the page given by the URL to the given file
 */
public class PageDownloader {

    private static final Logger LOG = Logger.getLogger(PageDownloader.class.getName());

    private String oUrl = null;

    private File oFile = null;

    private String oPageContent = null;

    /**
     * Calls PageDownLoader() for init
     *
     * @param url where to get pages
     */
    public PageDownloader(final String url) {
        setUrl(url);
        if (LOG.isDebugEnabled()) {
            LOG.debug(getUrl());
        }
    }

    /**
     * Executes the downloads and saving of the given pages
     *
     * @throws Exception if download-url or target directory are not set
     *                   or if file could not be opened for saving
     *                   or there is no connection to internet
     *                   or if given url is malformed
     */
    public void doDownload() throws Exception {
        if (getUrl() == null) {
            throw new IllegalStateException("No URL set!");
        }
        final URL url = new URL(getUrl());
        final Scanner scanner = new Scanner(url.openStream());
        scanner.useDelimiter(System.getProperty("line.separator"));
        final StringBuilder xml_content = new StringBuilder();
        while (scanner.hasNext()) {
            String s = scanner.next();
            xml_content.append(s);
            xml_content.append("\n");
        }
        scanner.close();
        oPageContent = xml_content.toString();
    }

    /**
     * Save page content string to file
     *
     * @throws IllegalStateException if no file name is set
     * @throws IOException           if an error occurs during writing
     */
    public void saveContent() throws IllegalStateException, IOException {
        if (getFile() == null) {
            throw new IllegalStateException("No file name set!");
        }
        if (getFile() != null) {
            final FileWriter fw = new FileWriter(getFile());
            fw.write(getPageContent());
            fw.flush();
            fw.close();
        }
    }

    protected void saveContentToFile(final File fileName) throws IOException {
        setFile(fileName);
        saveContent();
    }

    /**
     * @param url where pages should be downloaded
     */
    public void setUrl(final String url) {
        oUrl = url;
    }

    protected void setFile(final File file) {
        oFile = file;
    }

    protected File getFile() {
        return oFile;
    }

    protected String getUrl() {
        return oUrl;
    }

    protected String getPageContent() {
        return oPageContent;
    }
}
