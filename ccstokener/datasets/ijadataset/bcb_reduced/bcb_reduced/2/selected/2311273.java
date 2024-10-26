package net.sf.bluecove.obex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

/**
 *
 *
 */
public class Deploy implements UserInteraction {

    private String fileName;

    private int progressMaximum;

    public static void main(String[] args) {
        if ((args.length < 2) || args[0].equalsIgnoreCase("--help")) {
            StringBuffer usage = new StringBuffer();
            usage.append("Usage:\n java ").append(Deploy.class.getName());
            usage.append(" bluetoothURL yourApp.jar\n");
            System.out.println(usage);
            System.exit(1);
            return;
        }
        String obexUrl = args[0];
        String filePath = args[1];
        Logger.debugOn = false;
        Deploy d = new Deploy();
        byte[] data = d.readFile(filePath);
        if (data == null) {
            System.exit(1);
            return;
        }
        ObexBluetoothClient o = new ObexBluetoothClient(d, d.fileName, data);
        if (o.obexPut(obexUrl)) {
            System.exit(0);
        } else {
            System.exit(2);
        }
    }

    private Deploy() {
    }

    private static String simpleFileName(String filePath) {
        int idx = filePath.lastIndexOf('/');
        if (idx == -1) {
            idx = filePath.lastIndexOf('\\');
        }
        if (idx == -1) {
            return filePath;
        }
        return filePath.substring(idx + 1);
    }

    private byte[] readFile(final String filePath) {
        InputStream is = null;
        byte[] data = null;
        try {
            String path = filePath;
            String inputFileName;
            File file = new File(filePath);
            if (file.exists()) {
                is = new FileInputStream(file);
                inputFileName = file.getName();
            } else {
                URL url = new URL(path);
                is = url.openConnection().getInputStream();
                inputFileName = url.getFile();
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[0xFF];
            int i = is.read(buffer);
            int done = 0;
            while (i != -1) {
                bos.write(buffer, 0, i);
                done += i;
                i = is.read(buffer);
            }
            data = bos.toByteArray();
            fileName = simpleFileName(inputFileName);
            showStatus((data.length / 1024) + "k " + fileName);
        } catch (Throwable e) {
            Logger.error(e);
            showStatus("Download error " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
        return data;
    }

    public void setProgressMaximum(int n) {
        progressMaximum = n;
    }

    public void setProgressValue(int n) {
    }

    public void setProgressDone() {
    }

    public void showStatus(String message) {
        System.out.println(message);
    }
}
