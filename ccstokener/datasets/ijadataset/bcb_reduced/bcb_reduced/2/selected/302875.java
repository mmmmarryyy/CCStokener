package plugins;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author ghast
 */
public class DownloadPlugin extends Plugin {

    private static Logger logger = Logger.getLogger(DownloadPlugin.class);

    private static String outputLocation = null;

    private static Map<String, String> mimeTypes = new HashMap<String, String>();

    public DownloadPlugin() {
        populateMimeTypes();
        outputLocation = System.getProperty("DOWNLOADS");
    }

    @Override
    public String execute(String command) {
        if (null == outputLocation || outputLocation.length() < 1) return "Missing output location, URL will not be downloaded";
        String[] commands = command.split(" ");
        if (commands.length < 2) return "useage:\ndownloading a file: download <url(s)>";
        StringBuffer errors = new StringBuffer();
        for (int i = 1; i < commands.length; i++) {
            try {
                download(commands[i]);
            } catch (Exception ex) {
                errors.append("\nError occured while downloading " + commands[i] + "\n\t" + ex.getMessage());
            }
        }
        if (errors.length() > 0) return errors.toString();
        return "Download successful";
    }

    private String getFileName(String address, String mimeType) throws Exception {
        if (null == outputLocation || outputLocation.length() < 1) throw new Exception("Missing output location, URL " + address + " will not be downloaded");
        String fileName = outputLocation + File.separator;
        if (!address.contains("/")) throw new Exception("Malformed address " + address);
        if (address.lastIndexOf("/") == address.length() - 1) throw new Exception("Malformed address " + address);
        fileName += address.substring(address.lastIndexOf("/") + 1, address.length());
        String extension = mimeTypes.get(mimeType);
        if (null != extension) {
            if (!fileName.endsWith(extension)) fileName += extension;
        }
        return fileName;
    }

    private void download(String address) throws Exception {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(address);
            conn = url.openConnection();
            String localFileName = getFileName(address, conn.getContentType());
            out = new BufferedOutputStream(new FileOutputStream(localFileName));
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
            }
        } catch (MalformedURLException mue) {
            logger.error("Malformed URL " + address, mue);
            throw new Exception("Malformed URL " + address);
        } catch (IOException ioe) {
            logger.error("IO Exception occured while downloading " + address, ioe);
            throw new Exception("IO error occured while downloading " + address);
        } catch (Exception e) {
            logger.error("Exception occured while downloading " + address, e);
            throw new Exception("Error occured while downloading " + address);
        } finally {
            try {
                if (null != in) in.close();
                if (null != out) out.close();
            } catch (IOException ioe) {
                logger.error("Failed to close the input/output stream " + ioe.getMessage(), ioe);
            }
        }
    }

    @Override
    public String getCallName() {
        return "download";
    }

    private void populateMimeTypes() {
        mimeTypes.put("application/x-bittorrent", ".torrent");
        mimeTypes.put("text/html", ".html");
    }
}
