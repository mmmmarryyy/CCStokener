package be.belgium.eid.eidcommon;

import java.io.*;
import java.net.*;

/**
 * The HTTPFileDownload class is able to download files that are located on the
 * internet on a HTTP address.
 * 
 * @author Kristof Overdulve
 * @version 1.0.0 10 Mar 2008
 */
public class HTTPFileDownload {

    /**
	 * Downloads the file on the given HTTP address to the given local filename.
	 * 
	 * @param address
	 *            is the address to download the file from
	 * @param localFileName
	 *            is the name of the file to save the downloaded file to, this
	 *            can be a relative or absolute filename
	 * @throws IOException
	 *             when the file couldn't be downloaded or stored
	 */
    public static void download(final String address, final String localFileName) throws IOException {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            final URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(localFileName));
            conn = url.openConnection();
            in = conn.getInputStream();
            final byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
        } catch (IOException e) {
            final File f = new File(localFileName);
            f.delete();
            throw e;
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
    }

    /**
	 * Downloads the file from the given address and saves it to the same
	 * filename as in the URI.
	 * 
	 * @param address
	 *            is the address to download the file from
	 * @throws IOException
	 *             when the file couldn't be downloaded
	 */
    public static void download(final String address) throws IOException {
        int lastSlashIndex = 0;
        if (address.contains("/")) {
            lastSlashIndex = address.lastIndexOf('/');
        }
        if (lastSlashIndex >= 0 && lastSlashIndex <= address.length() - 1) {
            download(address, address.substring(lastSlashIndex + 1));
        }
    }
}
