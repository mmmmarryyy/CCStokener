package com.dilanperera.rapidws.core;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Provides helper functionality for downloading files.
 */
public class DownloadHelper {

    /**
	 * Downloads a file from the given URL to the specified local file name.
	 *
	 * @param sourceUrl     the URL of the file to be downloaded.
	 * @param localFileName the location to which the file is to be downloaded.
	 * @throws IOException thrown when an input/output error occurs.
	 */
    public static void downloadFile(String sourceUrl, String localFileName) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        if ((sourceUrl.toLowerCase().startsWith("http")) || (sourceUrl.toLowerCase().startsWith("ftp"))) {
            URL url = new URL(sourceUrl);
            URLConnection connection = url.openConnection();
            inputStream = connection.getInputStream();
        } else {
            inputStream = new FileInputStream(sourceUrl);
        }
        File outputFile = new File(localFileName);
        if (outputFile.exists()) {
            outputFile.delete();
        }
        outputFile.getParentFile().mkdir();
        outputStream = new FileOutputStream(outputFile);
        IOHelper.copyContents(inputStream, outputStream);
    }
}
