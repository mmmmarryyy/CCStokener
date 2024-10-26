package org.pointrel.pointrel20090201.test;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.pointrel.pointrel20090201.core.ArchiveUsingCGI;
import org.pointrel.pointrel20090201.core.ArchiveUsingDirectory;
import org.pointrel.pointrel20090201.core.Standards;

public class Test {

    static boolean FALSE = false;

    static boolean TRUE = true;

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        if (FALSE) timestampTest();
        if (FALSE) ftpTest();
        if (FALSE) testConversion();
        if (FALSE) testSimpleURL();
        if (FALSE) listFilesAndContentsOfServer("http://www.pointrel.org");
        if (FALSE) testFileTransfer();
        if (FALSE) testPreferences();
        if (FALSE) printTimestamp();
        if (FALSE) simpleFileNameTest();
        if (FALSE) testApacheVFS();
        if (FALSE) testEncodingAndDecodingByteArray();
        testStaticFieldAccessUsingClassReference();
    }

    private static void testStaticFieldAccessUsingClassReference() {
        System.out.println("first: " + new ArchiveUsingCGI().getArchiveType());
        System.out.println("first: " + new ArchiveUsingDirectory().getArchiveType());
        Class<?> class1 = ArchiveUsingDirectory.class;
        String value = null;
        ;
        try {
            value = (String) class1.getDeclaredField("archiveType").get(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        System.out.println("result: " + value);
    }

    private static void testEncodingAndDecodingByteArray() {
        byte[] bytes = { 0, 1, 2, 3, 4, 5, 127, (byte) 128, (byte) 200, (byte) 201, (byte) 255 };
        for (int theByte : bytes) {
            if (theByte < 0) theByte += 256;
            System.out.println("Original byte: " + theByte);
        }
        System.out.println();
        String hexEncodedBytes = Standards.hexEncodedStringForByteArray(bytes);
        System.out.println("Encoded: " + hexEncodedBytes);
        System.out.println();
        byte[] decodedBytes = Standards.byteArrayForHexEncodedString(hexEncodedBytes);
        for (int theByte : decodedBytes) {
            if (theByte < 0) theByte += 256;
            System.out.println("Decoded byte: " + theByte);
        }
    }

    private static void testApacheVFS() {
        try {
            FileSystemManager vfsManager;
            vfsManager = VFS.getManager();
            String password = null;
            if (true) {
                password = JOptionPane.showInputDialog("Enter password for remote server:");
                if (password == null) return;
                if (!password.equals("")) {
                    password = ":" + password;
                }
            }
            FileObject srcDir = vfsManager.resolveFile("sftp://oscomak" + password + "@qs1895.pair.com/home/oscomak/public_html/oscomak.net/temporary_pointrel_testing/transactions/");
            System.out.println("About to list files");
            FileObject[] children = srcDir.getChildren();
            System.out.println("Children of " + srcDir.getName().getURI());
            for (int i = 0; i < children.length; i++) {
                System.out.println(children[i].getName().getBaseName());
                InputStream inputStream = children[i].getContent().getInputStream();
                System.out.println("Contents (first 4000 bytes): ");
                byte[] buffer = new byte[4000];
                int bytesRead = inputStream.read(buffer);
                System.out.println(new String(buffer, 0, bytesRead));
            }
            System.out.println("Done listing files");
        } catch (FileSystemException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void simpleFileNameTest() {
        File file = new File("test_output.zip!hello.txt");
        System.out.println("File exists: " + file.exists());
        System.out.println("File name: " + file.getName());
        System.out.println("File name: " + file.getAbsolutePath());
        try {
            System.out.println("File name: " + file.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getTimestampThatIsAcceptableForFileNames(String timestamp) {
        String simpleTimestamp = timestamp.replace("T", "-");
        simpleTimestamp = simpleTimestamp.replace(":", "-");
        simpleTimestamp = simpleTimestamp.replace(".", "-");
        return simpleTimestamp;
    }

    public String getTimestampedIdentifierAcceptableForFileNames(String uuid) {
        String timestamp = Standards.getCurrentTimestamp();
        return getTimestampThatIsAcceptableForFileNames(timestamp) + "_" + uuid;
    }

    private static void printTimestamp() {
        long timestamp = System.currentTimeMillis();
        System.out.println(timestamp);
    }

    private static void testPreferences() {
        System.out.println("Max NAME length in preferences: " + Preferences.MAX_NAME_LENGTH);
        System.out.println("Max VALUE length in preferences: " + Preferences.MAX_VALUE_LENGTH);
        Preferences preferences = Preferences.userRoot().node("/org/pointrel/pointrel20090201/test");
        String user = preferences.get("user", "");
        System.out.println("User: " + user);
        if (user.equals("")) {
            user = Standards.newUUID();
            System.out.println("Generated user ID: " + user);
            preferences.put("user", user);
        }
        try {
            if (FALSE) {
                System.out.println("Clearing preferences");
                preferences.clear();
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private static void testFileTransfer() {
        try {
            byte[] bytes = getBytesForWebPageUsingHTTPClient("http://www.oscomak.net/temporary_pointrel_testing/pointrel.php?fileName=PointrelTransaction_output.utf8");
            System.out.println(new String(bytes));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void uploadFile(String url, String fileName) throws Exception {
        DefaultHttpClient client = new DefaultHttpClient();
        client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY), new UsernamePasswordCredentials("test", "test2"));
        HttpPost httpPost = new HttpPost(url);
        File file = new File(fileName);
        System.out.println("File1 Length = " + file.length());
        FileBody bin = new FileBody(new File(fileName));
        StringBody comment = new StringBody("A Pointrel transaction file");
        MultipartEntity multipartEntityForPost = new MultipartEntity();
        multipartEntityForPost.addPart("uploaded", bin);
        multipartEntityForPost.addPart("comment", comment);
        httpPost.setEntity(multipartEntityForPost);
        HttpResponse response = client.execute(httpPost);
        HttpEntity entity = response.getEntity();
        System.out.println("statusLine>>>" + response.getStatusLine());
        if (entity != null) {
            entity.writeTo(System.out);
            entity.consumeContent();
        }
    }

    private static void listFilesAndContentsOfServer(String urlString) {
        try {
            String fileListResult = new String(getBytesForWebPageUsingHTTPClient(urlString));
            System.out.println("splitting");
            String[] splitResult = fileListResult.split("\n");
            for (String fileName : splitResult) {
                System.out.println(":" + fileName + ":");
                byte[] transactionBytes = getBytesForWebPageUsingHTTPClient(urlString + "?fileName=" + fileName);
                System.out.println(transactionBytes.length);
                System.out.println(new String(transactionBytes));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void println(String string) {
        System.out.println(string);
    }

    private static byte[] getBytesForWebPageUsingHTTPClient(String urlString) throws Exception {
        println("Starting test");
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY), new UsernamePasswordCredentials("test", "test2"));
        HttpGet httpget = new HttpGet(urlString);
        System.out.println("executing request" + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        System.out.println("----------------------------------------");
        System.out.println(response.getStatusLine());
        if (entity != null) {
            System.out.println("Response content length: " + entity.getContentLength());
        }
        println("contents");
        byte[] bytes = null;
        if (entity != null) {
            bytes = getBytesFromInputStream(entity.getContent());
            entity.consumeContent();
        }
        return bytes;
    }

    private static void testSimpleURL() {
        byte[] bytes = getBytesForURL("http://www.oscomak.net/temporary_pointrel_testing/list.php");
        String result = new String(bytes);
        System.out.println(result);
        System.out.println("splitting");
        String[] splitResult = result.split("\n");
        for (String fileName : splitResult) {
            System.out.println(":" + fileName + ":");
            byte[] transactionBytes = getBytesForURL("http://www.oscomak.net/temporary_pointrel_testing/transactions/" + fileName);
            System.out.println(transactionBytes.length);
            System.out.println(new String(transactionBytes));
        }
    }

    private static byte[] getBytesForURL(String url) {
        System.out.println("Fetching data at URL: " + url);
        URL theURL = null;
        try {
            theURL = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            InputStream inputStream = theURL.openStream();
            return getBytesFromInputStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int b = inputStream.read();
        while (b != -1) {
            outputStream.write(b);
            b = inputStream.read();
        }
        inputStream.close();
        return outputStream.toByteArray();
    }

    private static void testConversion() {
        Rectangle rectangle = new Rectangle(1, 2, 3, 4);
        System.out.println(rectangle.toString());
        System.out.println("(" + (int) rectangle.getX() + ", " + (int) rectangle.getY() + ")");
        System.out.println((int) rectangle.getX() + " " + (int) rectangle.getY());
        String testInput = "10 20   30";
        String[] splitResult = testInput.split(" ");
        for (String item : splitResult) {
            System.out.println(item);
        }
        System.out.println("trying tokenizer");
        StringTokenizer st = new StringTokenizer(testInput);
        while (st.hasMoreTokens()) {
            System.out.println(st.nextToken());
        }
        byte[] bytes = { 1, 2, 3 };
        System.out.println(bytes);
    }

    private static void timestampTest() {
        Date date = new Date();
        System.out.println(date.toString());
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat ISO8601TimestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        ISO8601TimestampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String formattedTimestamp = ISO8601TimestampFormat.format(timestamp);
        System.out.println(formattedTimestamp);
        System.out.println(System.getProperty("user.name"));
    }

    private static void ftpTest() {
        FTPClient f = new FTPClient();
        try {
            f.connect("oscomak.net");
            System.out.print(f.getReplyString());
            f.setFileType(FTPClient.BINARY_FILE_TYPE);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String password = JOptionPane.showInputDialog("Enter password");
        if (password == null || password.equals("")) {
            System.out.println("No password");
            return;
        }
        try {
            f.login("oscomak_pointrel", password);
            System.out.print(f.getReplyString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            String workingDirectory = f.printWorkingDirectory();
            System.out.println("Working directory: " + workingDirectory);
            System.out.print(f.getReplyString());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            f.enterLocalPassiveMode();
            System.out.print(f.getReplyString());
            System.out.println("Trying to list files");
            String[] fileNames = f.listNames();
            System.out.print(f.getReplyString());
            System.out.println("Got file list fileNames: " + fileNames.length);
            for (String fileName : fileNames) {
                System.out.println("File: " + fileName);
            }
            System.out.println();
            System.out.println("done reading stream");
            System.out.println("trying alterative way to read stream");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            f.retrieveFile(fileNames[0], outputStream);
            System.out.println("size: " + outputStream.size());
            System.out.println(outputStream.toString());
            System.out.println("done with alternative");
            System.out.println("Trying to store file back");
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            boolean storeResult = f.storeFile("test.txt", inputStream);
            System.out.println("Done storing " + storeResult);
            f.disconnect();
            System.out.print(f.getReplyString());
            System.out.println("disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
