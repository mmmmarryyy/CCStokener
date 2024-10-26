package kentriko;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

public class FileDownload extends SwingWorker {

    long numWritten;

    private String myaddress;

    private String localFileName;

    private JLabel myLabel;

    public FileDownload(String inUrl, String inName, JLabel inLabel) {
        numWritten = 0L;
        myaddress = inUrl;
        localFileName = inName;
        myLabel = inLabel;
    }

    protected Object doInBackground() throws Exception {
        download(myaddress, localFileName);
        return null;
    }

    public void download(String address, String localFileName) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            System.out.println("Downloading: " + address);
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(localFileName));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
                myLabel.setText("Downloading " + (numWritten / 1024));
            }
            System.out.println(localFileName + "\t" + numWritten);
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
    }
}
