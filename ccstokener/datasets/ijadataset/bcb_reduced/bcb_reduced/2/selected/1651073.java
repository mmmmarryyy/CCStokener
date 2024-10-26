package tr.com.srdc.www.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author tuncay
 */
public class FileUtil {

    public static byte[] getBytesFromFile(String fileURI) throws IOException {
        File file = new File(fileURI);
        return getBytes(file);
    }

    private static String readWholeFile(URI uri) throws IOException {
        BufferedReader buf;
        StringBuffer rules = new StringBuffer();
        FileInputStream fis = new FileInputStream(new File(uri));
        InputStreamReader inputStreamReader = new InputStreamReader(fis, "UTF-8");
        buf = new BufferedReader(inputStreamReader);
        String temp;
        while ((temp = buf.readLine()) != null) rules.append(temp).append("\n");
        buf.close();
        return rules.toString();
    }

    public static String readWholeFile(String filePath) throws IOException {
        BufferedReader buf;
        StringBuffer rules = new StringBuffer();
        FileInputStream fis = new FileInputStream(filePath);
        InputStreamReader inputStreamReader = new InputStreamReader(fis, "UTF-8");
        buf = new BufferedReader(inputStreamReader);
        String temp;
        while ((temp = buf.readLine()) != null) rules.append(temp).append("\n");
        buf.close();
        return rules.toString();
    }

    public static String readWholeFile(File file) throws IOException {
        BufferedReader buf;
        StringBuffer rules = new StringBuffer();
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader inputStreamReader = new InputStreamReader(fis, "UTF-8");
        buf = new BufferedReader(inputStreamReader);
        String temp;
        while ((temp = buf.readLine()) != null) rules.append(temp).append("\n");
        buf.close();
        return rules.toString();
    }

    public static String readWholeFile(String filePath, String encoding) throws IOException {
        BufferedReader buf;
        StringBuffer rules = new StringBuffer();
        FileInputStream fis = new FileInputStream(filePath);
        InputStreamReader inputStreamReader = new InputStreamReader(fis, encoding);
        buf = new BufferedReader(inputStreamReader);
        String temp;
        while ((temp = buf.readLine()) != null) rules.append(temp).append("\n");
        buf.close();
        return rules.toString();
    }

    public static byte[] readFromURI(URI uri) throws IOException {
        if (uri.toString().contains("http:")) {
            URL url = uri.toURL();
            URLConnection urlConnection = url.openConnection();
            int length = urlConnection.getContentLength();
            System.out.println("length of content in URL = " + length);
            if (length > -1) {
                byte[] pureContent = new byte[length];
                DataInputStream dis = new DataInputStream(urlConnection.getInputStream());
                dis.readFully(pureContent, 0, length);
                dis.close();
                return pureContent;
            } else {
                throw new IOException("Unable to determine the content-length of the document pointed at " + url.toString());
            }
        } else {
            return readWholeFile(uri).getBytes("UTF-8");
        }
    }

    private static byte[] getBytes(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            System.out.println("File is too large to process");
            return null;
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while ((offset < bytes.length) && ((numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }

    /**
	 * Constructs a new file with the given content
	 * and filePath. If a file with the same name
	 * already exists, simply overwrites the content.
	 * @author Gunes
	 * @param filePath
	 * @param content : expressed as byte[]
	 * @throws IOException : 
	 * 	[approved by gunes]
	 * 	when the file cannot be created. possible causes:
	 * 	1) invalid filePath,
	 * 	2) already existing file cannot be deleted due
	 * 	to read-write locks
	 */
    public static void constructNewFile(String filePath, byte[] content) throws IOException {
        File file = new File(filePath);
        file.mkdirs();
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(content);
        fos.close();
    }

    public static void writeToFile(File file, String content) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter outStreamWriter = new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter bufferedWriter = new BufferedWriter(outStreamWriter);
        bufferedWriter.write(content);
        bufferedWriter.flush();
        bufferedWriter.close();
    }
}
