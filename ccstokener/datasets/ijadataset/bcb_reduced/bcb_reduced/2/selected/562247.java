package eu.bseboy.tvrss.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import eu.bseboy.tvrss.config.Configuration;

public class Downloader {

    private Configuration config;

    private void debug(String message) {
        System.out.println(message);
    }

    private void error(String message) {
        System.err.println(message);
    }

    private void moveProgressDisplay() {
        System.out.print(".");
    }

    private void endProgressDisplay() {
        System.out.println("]");
    }

    private void startProgressDisplay() {
        System.out.print("[");
    }

    public Downloader(Configuration configuration) {
        this.config = configuration;
    }

    public boolean downloadItem(String itemURL) {
        boolean success = false;
        try {
            byte[] chunk = new byte[1024];
            int bytesRead = 0;
            URL url = new URL(itemURL);
            InputStream inS = url.openStream();
            OutputStream[] outS = DownloadStreamFactory.createDownloadOutputStreams(config);
            startProgressDisplay();
            do {
                bytesRead = inS.read(chunk);
                for (int i = 0; (bytesRead > 0) && (i < outS.length); i++) {
                    outS[i].write(chunk, 0, bytesRead);
                }
                moveProgressDisplay();
            } while (bytesRead > 0);
            endProgressDisplay();
            for (int i = 0; i < outS.length; i++) {
                outS[i].flush();
                outS[i].close();
            }
            inS.close();
            success = true;
        } catch (Exception e) {
            error("Failed to download item : " + itemURL);
            error(e.getMessage());
        }
        return success;
    }
}
