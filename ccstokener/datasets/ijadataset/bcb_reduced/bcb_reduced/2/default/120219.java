import java.util.*;
import java.net.*;
import java.io.*;

/**
 * This thread downloading a single file
 * @author v0j1
 */
public class DownloadThread extends Thread {

    private frmDownloadR frmOwner;

    private String inUrl;

    private String outPath;

    private int LastNonameIndex;

    /**
     * Creates a new instance of DownloadThread
     * @param ownerfrm the owner of download thread
     * @param inurl source of the download (url)
     * @param path download target
     */
    public DownloadThread(frmDownloadR ownerfrm, String inurl, String path) {
        frmOwner = ownerfrm;
        inUrl = inurl;
        outPath = path;
        LastNonameIndex = 0;
    }

    /**
     * Start the downloading
     */
    public void run() {
        boolean error = false;
        try {
            URL url = new URL(inUrl);
            String filename = url.getFile();
            if (filename.length() == 0) {
                java.text.NumberFormat NumFormat = new java.text.DecimalFormat("##000");
                filename = "noname_" + NumFormat.format(LastNonameIndex);
                LastNonameIndex++;
            } else {
                if (!frmOwner.FilenameContainsPath()) {
                    StringTokenizer srctokens = new StringTokenizer(filename, "/");
                    while (srctokens.hasMoreTokens()) {
                        filename = srctokens.nextToken();
                    }
                }
                if (frmOwner.CovertWebPrefix()) {
                    filename = filename.replace("%20", "_");
                    filename = filename.replace("%27", "'");
                    filename = filename.replace("%28", "(");
                    filename = filename.replace("%29", ")");
                }
                filename = filename.replaceAll("[^a-zA-Z0-9.-_%]", "_");
            }
            try {
                DataInputStream in = new DataInputStream(url.openConnection().getInputStream());
                FileOutputStream fOut = new FileOutputStream(outPath + "/" + filename);
                DataOutputStream out = new DataOutputStream(fOut);
                int chc;
                double downedsize = 0;
                while ((chc = in.read()) != -1) {
                    out.write(chc);
                    downedsize++;
                    frmOwner.SetDownloadedFileSize(downedsize);
                }
                in.close();
                fOut.flush();
                fOut.close();
            } catch (IOException e) {
                frmOwner.PassToLog("Downloader", "Unable to download: " + filename);
                error = true;
            }
        } catch (MalformedURLException e) {
            frmOwner.PassToLog("Downloader", "Unable to download: " + inUrl);
            error = true;
        }
        if (!error) frmOwner.PassToLog("Downloader", "Finished: " + inUrl);
        frmOwner.FinishDownload(error);
        return;
    }
}
