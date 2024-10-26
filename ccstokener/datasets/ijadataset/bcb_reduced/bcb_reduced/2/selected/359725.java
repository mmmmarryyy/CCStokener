package wjhk.jupload2.upload;

import java.io.*;
import java.net.*;
import javax.swing.*;

/**
  * First version of FileUploadThreadV1 : unused. 
  *  
  * Known Bugs: System out of memory Exception.
  * Solution  : Nothing!
  * Tryed but didn't work:
  *             - Flush the OutputStream once it has outputed 1MB.
  *             - Call System.gc() once it has outputed 1MB.
  *             - Sleep for 1 Second so that System GC gets a chance to run.
  * Work Around: Breakup the file into smaller ones then upload =)
  * 
  * 
  * @deprecated Replaced by {@link wjhk.jupload2.upload.FileUploadThreadHTTP}
  */
public class FileUploadThreadV1 extends Thread implements FileUploadThread {

    public static final double VERSION = 1.0;

    public static final String AUTHOR = "William Kwong Jinhua";

    public static final String AUTHOR_EMAIL = "wjhkwong@yahoo.com";

    public static final String DESCRIPTION = "Java Thread to upload files into a web server.";

    public static final String LAST_MODIFIED = "01 July 2002";

    private File[] aTotalFiles;

    private String uploadURL;

    private JProgressBar progress;

    private long totalFilesLength;

    private long uploadedLength;

    private StringBuffer sb = new StringBuffer();

    private void addServerOutPut(String s) {
        if (0 < sb.length() || !s.equals("")) {
            sb.append(s);
        }
    }

    public String getServerOutput() {
        return sb.toString();
    }

    private Exception e = null;

    public Exception getException() {
        return e;
    }

    public void setProgressPanel(JProgressBar pgrBar) {
        progress = pgrBar;
    }

    private boolean stop = false;

    public void stopUpload() {
        this.stop = true;
    }

    public FileUploadThreadV1(File[] files, String uploadURL) {
        aTotalFiles = files;
        this.uploadURL = uploadURL;
        totalFilesLength = 0;
        for (int i = 0; i < aTotalFiles.length; i++) {
            totalFilesLength += aTotalFiles[i].length();
        }
    }

    public void close() {
        aTotalFiles = null;
        e = null;
        sb = null;
    }

    public void run() {
        URL url = null;
        URLConnection urlConn = null;
        DataOutputStream dOut = null;
        BufferedReader bInp = null;
        try {
            url = new URL(uploadURL);
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            String boundary = "-----------------------------" + getRandomString();
            urlConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary.substring(2, boundary.length()));
            String CRLF = "\r\n";
            dOut = new DataOutputStream(urlConn.getOutputStream());
            StringBuffer sb;
            File f;
            uploadedLength = 0;
            progress.setMaximum((int) totalFilesLength);
            for (int i = 0; i < aTotalFiles.length && !stop; i++) {
                f = aTotalFiles[i];
                sb = new StringBuffer();
                sb.append(boundary);
                sb.append(CRLF);
                sb.append("Content-Disposition: form-data; name=\"File");
                sb.append(i);
                sb.append("\"; filename=\"");
                sb.append(f.toString());
                sb.append("\"");
                sb.append(CRLF);
                sb.append("Content-Type: application/octet-stream");
                sb.append(CRLF);
                sb.append(CRLF);
                dOut.writeBytes(sb.toString());
                uploadFileStream(f, dOut);
                dOut.writeBytes(CRLF);
            }
            dOut.writeBytes(boundary);
            dOut.writeBytes("--");
            dOut.writeBytes(CRLF);
            dOut.flush();
            if (!stop) progress.setString("File(s) uploaded. Wait for server response!");
            bInp = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String str;
            while (null != ((str = bInp.readLine()))) {
                this.addServerOutPut(str);
            }
        } catch (Exception e) {
            this.e = e;
        } finally {
            try {
                bInp.close();
            } catch (Exception e) {
            }
            bInp = null;
            try {
                dOut.close();
            } catch (Exception e) {
            }
            dOut = null;
            urlConn = null;
            url = null;
        }
    }

    private String getRandomString() {
        String alphaNum = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuffer sbRan = new StringBuffer(11);
        int num;
        for (int i = 0; i < 11; i++) {
            num = (int) (Math.random() * (alphaNum.length() - 1));
            sbRan.append(alphaNum.substring(num, num + 1));
        }
        return sbRan.toString();
    }

    private void uploadFileStream(File f, DataOutputStream dOut) throws FileNotFoundException, IOException {
        byte[] byteBuff = null;
        FileInputStream fis = null;
        try {
            int numBytes = 0;
            byteBuff = new byte[1024];
            fis = new FileInputStream(f);
            while (-1 != (numBytes = fis.read(byteBuff)) && !stop) {
                dOut.write(byteBuff, 0, numBytes);
                uploadedLength += numBytes;
                if (null != progress) progress.setValue((int) uploadedLength);
            }
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
            byteBuff = null;
        }
    }

    /** @see FileUploadThread#nbBytesUploaded(long) */
    public void nbBytesUploaded(long nbBytes) {
        uploadedLength += nbBytes;
        if (null != progress) progress.setValue((int) uploadedLength);
    }

    /** @see FileUploadThread#isUploadStopped() */
    public boolean isUploadStopped() {
        return stop;
    }
}
