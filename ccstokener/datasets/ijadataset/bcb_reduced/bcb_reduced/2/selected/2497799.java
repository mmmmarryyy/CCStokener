package docBuilder;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Downloads the given list of WikiPages from the given URL to the given target directory
 * and save these files as xml
 */
public class PageDownloader {

    private String oWikiUrl = null;

    private String oTargetDir = null;

    private List<File> oFileList = null;

    private List<String> oPageList = null;

    /**
     * Init internal Lists
     */
    public PageDownloader() {
        oFileList = new ArrayList<File>();
        oPageList = new ArrayList<String>();
    }

    /**
     * Calls PageDownLoader() for init
     *
     * @param downloadUrl where to get pages
     * @param targetDir   where to save pages
     */
    public PageDownloader(final String downloadUrl, final String targetDir) {
        this();
        setDownloadUrl(downloadUrl);
        setTargetDir(targetDir);
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
        if (oWikiUrl == null || oTargetDir == null) {
            throw new IllegalStateException("One ore more arguments are not set correctly!");
        }
        for (String page : oPageList) {
            final File file = new File(oTargetDir, page + "." + "xml").getAbsoluteFile();
            oFileList.add(file);
            final URL url = new URL(oWikiUrl.replace("PAGE", page));
            final Scanner scanner = new Scanner(url.openStream());
            scanner.useDelimiter(System.getProperty("line.separator"));
            final StringBuilder xml_content = new StringBuilder();
            while (scanner.hasNext()) {
                String s = scanner.next();
                xml_content.append(s);
                xml_content.append("\n");
            }
            scanner.close();
            final FileWriter fw = new FileWriter(file);
            fw.write(xml_content.toString());
            fw.flush();
            fw.close();
        }
    }

    /**
     * @param url where pages should be downloaded
     */
    public void setDownloadUrl(final String url) {
        oWikiUrl = url;
    }

    /**
     * @param dir where files should be stored to
     */
    public void setTargetDir(final String dir) {
        oTargetDir = dir;
    }

    /**
     * @param pageList which have to be downloaded
     */
    public void setPageList(final List<String> pageList) {
        oPageList = pageList;
    }

    /**
     * @return a list of all downloaded xml files
     */
    public List<File> getFileList() {
        return oFileList;
    }
}
