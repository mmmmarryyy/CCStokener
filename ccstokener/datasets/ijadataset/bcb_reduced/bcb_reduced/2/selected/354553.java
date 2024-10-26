package tabdulin.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.log4j.Logger;

public class FileDownloader {

    private static final Logger logger = Logger.getLogger(FileDownloader.class);

    private static final int BUFFER_SIZE = 1024;

    /**
	 * Downloads file to local computer by HTTP.
	 * 
	 * @param urlString URL to be downloaded.
	 * @param fileName Name of downloaded file on local computer.
	 * @return size of file.
	 * @throws IOException
	 */
    public static long download(String urlString, String fileName) throws IOException {
        logger.info("Downloading file \"" + urlString + "\" to \"" + fileName + "\" ...");
        if (urlString == null || fileName == null) {
            return -1;
        }
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        InputStream is = urlConnection.getInputStream();
        FileOutputStream fos = new FileOutputStream(fileName);
        byte[] buffer = new byte[BUFFER_SIZE];
        int numRead = 0;
        long numWritten = 0;
        while ((numRead = is.read(buffer)) != -1) {
            fos.write(buffer, 0, numRead);
            numWritten += numRead;
        }
        is.close();
        fos.close();
        logger.info("\"" + fileName + "\" was saved.");
        return numWritten;
    }
}
