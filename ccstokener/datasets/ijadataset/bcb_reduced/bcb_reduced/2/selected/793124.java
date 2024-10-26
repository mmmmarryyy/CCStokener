package updater.model.downloader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import updater.UpdaterConstants;

/**
 * Utility class providing file download methods
 *
 * @author Dominik Schaufelberger
 */
public class Downloader {

    /**
     * Private Constructor to prevent instantiation.
     */
    private Downloader() {
    }

    /**
     * Downloads a file from the given URL {@code fileURL} and stores it in a 
     * tmp file. The method aswell takes an prefix argument {@code tmpFilePrefix}
     * to identify the file in the tmp folder.
     *
     * @param fileURL
     *          Url to the download file
     * @param tmpFilePrefix
     *          Tmp file identifier
     * @return
     *      Tmp file
     */
    public static File downloadFileAsTmp(String fileURL, String tmpFilePrefix) throws MalformedURLException, FileNotFoundException, IOException {
        File tmpFile = null;
        InputStream inStream = null;
        OutputStream outStream = null;
        try {
            tmpFile = File.createTempFile(tmpFilePrefix, null, new File(UpdaterConstants.TMP_FILE_FOLDER));
            final URL url = new URL(fileURL);
            final URLConnection connection = url.openConnection();
            inStream = new BufferedInputStream(connection.getInputStream());
            outStream = new BufferedOutputStream(new FileOutputStream(tmpFile));
            byte[] buffer = new byte[32768];
            int bufferSize;
            while ((bufferSize = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bufferSize);
            }
        } finally {
            if (outStream != null) outStream.close();
            if (inStream != null) inStream.close();
        }
        return tmpFile;
    }
}
