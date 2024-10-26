package org.brainypdm.modules.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/*******************************************************************************
 * 
 * This is an ant tool class. It receive in input three parameters: -
 * repository's URL - text file path, with a list of files to download -
 * destination dir, where put downloaded files
 * 
 * and downloads the list of files in the destination dir
 * 
 * @author <a HREF="mailto:thomas@brainypdm.org">Thomas Buffagni</a>
 * 
 */
public class FilesDownloader extends BaseTool {

    /***************************************************************************
	 * the URL of files repository
	 */
    private final String address;

    /***************************************************************************
	 * is the path of text file with a list of files to download
	 */
    private final String propertiesFilePath;

    /**
	 * is the destination path
	 */
    private final String destination;

    /***************************************************************************
	 * 
	 * @param args
	 *            args[0]= address, args[1]= propertiesFilePath,
	 *            args[2]=destination
	 * @throws Exception
	 * @author <a HREF="mailto:thomas@brainypdm.org">Thomas Buffagni</a>
	 */
    private FilesDownloader(String[] args) throws Exception {
        if (args == null) {
            throw new Exception("args null not valid");
        }
        if (args.length != 3) {
            throw new Exception("args size not valid");
        }
        this.address = initAddress(args[0]);
        this.propertiesFilePath = args[1];
        this.destination = args[2];
    }

    /***************************************************************************
	 * download a list of files
	 */
    public void downloadFiles() {
        String[] list = getList(propertiesFilePath);
        long numWritten = 0;
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                numWritten = numWritten + download(list[i]);
            }
        }
        System.out.println("Downloaded: " + numWritten / 1000 + " Kb");
    }

    /***************************************************************************
	 * download a file and put it in the destination dir
	 * 
	 * @param fileName
	 * @author <a HREF="mailto:thomas@brainypdm.org">Thomas Buffagni</a>
	 */
    private long download(String fileName) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        long numWritten = 0;
        try {
            URL url = new URL(address + fileName);
            String localName = destination + File.separator + fileName;
            File f = new File(localName);
            if (!f.exists() || (f.exists() && !f.isDirectory() && (f.length() == 0))) {
                System.out.println(localName + "....");
                out = new BufferedOutputStream(new FileOutputStream(localName));
                conn = url.openConnection();
                in = conn.getInputStream();
                byte[] buffer = new byte[1024];
                int numRead;
                while ((numRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, numRead);
                    numWritten += numRead;
                }
                System.out.println("\t" + numWritten);
            } else if (!f.isDirectory()) {
                System.out.println(localName + "\t already exist!");
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
        return numWritten;
    }

    /***************************************************************************
	 * 
	 * @param address
	 * @return address correctly initialized
	 * @author <a HREF="mailto:thomas@brainypdm.org">Thomas Buffagni</a>
	 * 
	 */
    private String initAddress(String address) {
        if (!address.endsWith("/")) {
            address = address.concat("/");
        }
        return address;
    }

    public static void main(String[] args) throws Exception {
        FilesDownloader instance = new FilesDownloader(args);
        instance.downloadFiles();
    }
}
