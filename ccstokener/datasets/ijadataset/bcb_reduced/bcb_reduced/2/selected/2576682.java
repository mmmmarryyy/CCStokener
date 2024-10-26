package com.jjcp;

import java.io.*;
import java.net.*;

public class FileDownload {

    public boolean download(String address, String localFileName) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
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
            }
            return true;
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
        return false;
    }

    public void download(String address) {
        int lastSlashIndex = address.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < address.length() - 1) {
            download(address, address.substring(lastSlashIndex + 1));
        } else {
            System.err.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("COULD_NOT_FIGURE_OUT_LOCAL_FILE_NAME_FOR_") + address);
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            FileDownload fd = new FileDownload();
            fd.download(args[i]);
        }
    }
}
