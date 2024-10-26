package fortunata.util;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.io.*;

/**
 * A utility for comparing local files with Internet files (references).
 * The reference will be downloaded if it's different than the local file.
 * The reference will be always downloaded (as a temporal file) and compared locally. If update is required the temp
 * file will be copied overwriting the local file.
 */
public class FileDownloader {

    private static byte[] buffer = new byte[2048];

    private static byte[] bufferCmp = new byte[2048];

    private File tempFile = null;

    public FileDownloader(String fileURL) throws Exception {
        URL url;
        try {
            url = new URL(fileURL);
        } catch (MalformedURLException e) {
            System.out.println("Error accessing URL " + fileURL + ".");
            throw e;
        }
        InputStream is;
        try {
            is = url.openStream();
        } catch (IOException e) {
            System.out.println("Error creating Input Stream from URL '" + fileURL + "'.");
            throw e;
        }
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
        try {
            tempFile = File.createTempFile("tempz", ".tmpz");
        } catch (IOException e) {
            System.out.println("Error creating temp file. Check space available.");
            throw e;
        }
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(tempFile);
        } catch (FileNotFoundException e) {
            System.out.println("Error creating temp file. Check permissions.");
            throw e;
        }
        try {
            int numBytesRead = 0;
            while ((numBytesRead = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, numBytesRead);
            }
            fos.flush();
            fos.close();
        } catch (IOException e) {
            System.out.println("Error reading from URL " + fileURL + ".");
            throw e;
        }
    }

    public boolean hasSameContent(String fileName) throws Exception {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            System.out.println("Error reading from file " + fileName + ".");
            e.printStackTrace();
            throw e;
        }
        DataInputStream disFromTmp = new DataInputStream(new BufferedInputStream(new FileInputStream(tempFile)));
        DataInputStream disFromFile = new DataInputStream(new BufferedInputStream(fis));
        while (disFromTmp.read(buffer) != -1 && disFromFile.read(bufferCmp) != -1) {
            for (int i = 0; i < buffer.length; i++) {
                if (buffer[i] != bufferCmp[i]) {
                    fis.close();
                    disFromTmp.close();
                    disFromFile.close();
                    return false;
                }
            }
        }
        fis.close();
        disFromTmp.close();
        disFromFile.close();
        return true;
    }

    public void copyToFile(String fileName) throws Exception {
        DataInputStream disFromTmp = null;
        DataOutputStream dos;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(tempFile);
            disFromTmp = new DataInputStream(new BufferedInputStream(fis));
        } catch (FileNotFoundException e) {
            System.out.println("Error reading temp file");
            e.printStackTrace();
            throw e;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName);
            dos = new DataOutputStream(new BufferedOutputStream(fos));
        } catch (FileNotFoundException e) {
            System.out.println("Error creating " + fileName + ".");
            e.printStackTrace();
            throw e;
        }
        int numBytesRead = 0;
        while ((numBytesRead = disFromTmp.read(buffer)) != -1) {
            dos.write(buffer, 0, numBytesRead);
        }
        dos.close();
        fos.close();
        disFromTmp.close();
        fis.close();
    }

    public String getTempFileName() {
        return tempFile.getAbsolutePath();
    }

    public static boolean canWeConnectToInternet() {
        String s = "http://www.google.com/";
        URL url = null;
        boolean can = false;
        URLConnection conection = null;
        try {
            url = new URL(s);
        } catch (MalformedURLException e) {
            System.out.println("This should never happend. Error in URL name. URL specified was:" + s + ".");
        }
        try {
            conection = url.openConnection();
            conection.connect();
            can = true;
        } catch (IOException e) {
            can = false;
        }
        if (can) {
        }
        return can;
    }

    public static void main(String[] args) {
        if (canWeConnectToInternet() == false) {
            System.out.println("No Internet connection available");
            return;
        }
        runTestRightURLFile();
        runTestWrongURLFile();
        runTestWrongURL();
    }

    private static void runTestRightURLFile() {
        try {
            FileDownloader fdl = new FileDownloader("http://xmlns.com/foaf/0.1/20050603.rdf");
            fdl.copyToFile("foaf.20050603.rdf");
            System.out.println(fdl.getTempFileName());
            if (fdl.hasSameContent("foaf.20050603.ok.rdf")) {
                System.out.println("Are equal");
            } else {
                System.out.println("Are different");
            }
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Check if you can delete the file (in theory you should):");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void runTestWrongURLFile() {
        try {
            FileDownloader fdl = new FileDownloader("http://xmlns.com/foaf/0.1/20050603.aggh.rdf");
        } catch (Exception e) {
            System.out.println("Invalid URL file");
        }
    }

    private static void runTestWrongURL() {
        try {
            FileDownloader fdl = new FileDownloader("protoc:///hello");
        } catch (Exception e) {
            System.out.println("Invalid URL");
        }
    }
}
