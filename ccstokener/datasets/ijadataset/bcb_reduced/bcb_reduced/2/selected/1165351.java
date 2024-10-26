package cat.inkubator.plugin4j.download;

import cat.inkubator.plugin4j.xml.Plugin;
import java.io.*;
import java.net.*;

/**
 *
 * @author gato
 * Based on FileDownload.java http://schmidt.devlib.org/java/file-download.html
 * Author: Marco Schmidt
 */
public class Downloader implements Runnable {

    private String address;

    private String fileName;

    private Plugin plugin;

    private DownloadMonitor downMonit;

    public Downloader(Plugin plugin, DownloadMonitor downMonit, String targetFolder) {
        this.plugin = plugin;
        this.downMonit = downMonit;
        File file = new File(targetFolder);
        if (!file.isDirectory()) {
            file.mkdir();
        }
        this.fileName = file.getAbsolutePath() + file.separatorChar + plugin.getArtifactId() + "-" + plugin.getVersion() + "." + plugin.getPackaging();
        this.address = plugin.getUrlDownload();
    }

    public void run() {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(fileName));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
        downMonit.downloadFinish();
    }
}
