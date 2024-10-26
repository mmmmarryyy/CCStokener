package esa.herschel.randres.xmind.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.ObjectInputStream.GetField;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import esa.herschel.randres.xmind.Activator;
import esa.herschel.randres.xmind.preferences.PreferenceConstants;

public class FileUploader {

    private static final String TAG_ITEM = "items";

    private static final String TAG_START = "<" + TAG_ITEM + ">";

    private static final String TAG_END = "</" + TAG_ITEM + ">";

    private static final String DATE_FORMAT = "yyyyMMddHHmm";

    private static final SimpleDateFormat DATE_FORMATER = new SimpleDateFormat(DATE_FORMAT);

    public static HashMap<String, String> listRepository(ConnectionInfo connection) throws Exception {
        String url = "http://" + connection.getUploadHost() + ":" + connection.getUploadPort() + connection.getUploadAction();
        String content = sendGetRequest(url, "");
        HashMap<String, String> items = processListRepositoryResponse(content);
        DialogHelper.showMessage(url);
        return items;
    }

    private static HashMap<String, String> processListRepositoryResponse(String content) throws ParserConfigurationException, SAXException, IOException {
        HashMap<String, String> list = new HashMap<String, String>();
        String patternStr = TAG_START + ".*?" + TAG_END;
        boolean returnDelims = false;
        Iterator tokenizer = new RETokenizer(content, patternStr, returnDelims);
        for (; tokenizer.hasNext(); ) {
            String token = (String) tokenizer.next();
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(token)));
            NodeList nodes = doc.getElementsByTagName("item");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element item = (Element) nodes.item(i);
                String label = item.getAttribute("label");
                String name = item.getAttribute("name");
                list.put(name, label);
            }
        }
        return list;
    }

    public static String getFile(ConnectionInfo connection, String fileName) throws Exception {
        String url = "http://" + connection.getUploadHost() + ":" + connection.getUploadPort() + connection.getUploadAction();
        String parameter = "id=" + fileName;
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        String downloadDir = prefs.getString(PreferenceConstants.P_DOWNLOAD_DIR);
        checkDownloadDir(downloadDir);
        String downloadedFilename = downloadDir + File.separatorChar + fileName + ".xmind";
        makeBackup(downloadDir, downloadedFilename);
        downloadFile(url, parameter, downloadedFilename);
        return downloadedFilename;
    }

    private static void checkDownloadDir(String downloadDir) {
        File downloadDirFile = new File(downloadDir);
        if (!downloadDirFile.exists()) {
            downloadDirFile.mkdirs();
        }
    }

    private static void makeBackup(String downloadDir, String filename) {
        File file = new File(filename);
        if (file.exists()) {
            String backupDir = downloadDir + File.separatorChar + " backup";
            File backupDirFile = new File(backupDir);
            if (!backupDirFile.exists()) {
                backupDirFile.mkdirs();
            }
            file.renameTo(new File(backupDir + File.separatorChar + file.getName() + System.currentTimeMillis()));
        }
    }

    public static void upload(ConnectionInfo uploader, File file) throws IOException {
        String boundary = "WHATEVERYOURDEARHEARTDESIRES";
        StringBuffer buf = new StringBuffer();
        buf.append("POST ");
        buf.append(uploader.getUploadAction());
        buf.append(" HTTP/1.1\r\n");
        buf.append("Content-Type: multipart/form-data; boundary=");
        buf.append(boundary);
        buf.append("\r\n");
        buf.append("Host: ");
        buf.append(uploader.getUploadHost());
        buf.append(':');
        buf.append(uploader.getUploadPort());
        buf.append("\r\n");
        buf.append("Connection: close\r\n");
        buf.append("Cache-Control: no-cache\r\n");
        List cookies = uploader.getCookies();
        if (!cookies.isEmpty()) {
            buf.append("Cookie: ");
            for (Iterator iterator = cookies.iterator(); iterator.hasNext(); ) {
                Parameter parameter = (Parameter) iterator.next();
                buf.append(parameter.getName());
                buf.append('=');
                buf.append(parameter.getValue());
                if (iterator.hasNext()) buf.append("; ");
            }
            buf.append("\r\n");
        }
        buf.append("Content-Length: ");
        String fileName = buildMyXMindFileName(file.getName());
        System.out.println(fileName);
        StringBuffer body = new StringBuffer();
        List fields = uploader.getFields();
        for (Iterator iterator = fields.iterator(); iterator.hasNext(); ) {
            Parameter parameter = (Parameter) iterator.next();
            body.append("--");
            body.append(boundary);
            body.append("\r\n");
            body.append("Content-Disposition: form-data; name=\"");
            body.append(parameter.getName());
            body.append("\"\r\n\r\n");
            body.append(parameter.getValue());
            body.append("\r\n");
        }
        body.append("--");
        body.append(boundary);
        body.append("\r\n");
        body.append("Content-Disposition: form-data; name=\"");
        body.append("file");
        body.append("\"; filename=\"");
        body.append(fileName);
        body.append("\"\r\n");
        body.append("Content-Type: application/zip\r\n\r\n");
        String lastBoundary = "\r\n--" + boundary + "--\r\n";
        long length = file.length() + (long) lastBoundary.length() + (long) body.length();
        long total = buf.length() + body.length();
        buf.append(length);
        buf.append("\r\n\r\n");
        InetAddress address = InetAddress.getByName(uploader.getUploadHost());
        Socket socket = new Socket(address, uploader.getUploadPort());
        try {
            socket.setSoTimeout(60 * 1000);
            PrintStream out = new PrintStream(new BufferedOutputStream(socket.getOutputStream()));
            out.print(buf);
            out.print(body);
            byte[] bytes = new byte[1024 * 65];
            int size;
            InputStream in = new BufferedInputStream(new FileInputStream(file));
            try {
                while ((size = in.read(bytes)) > 0) {
                    total += size;
                    out.write(bytes, 0, size);
                }
                System.out.println(total);
            } finally {
                in.close();
            }
            out.print(lastBoundary);
            out.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            ;
        } catch (SocketException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    private static String buildMyXMindFileName(String fileName) {
        int extIndx = fileName.lastIndexOf(".");
        String dateSufix = DATE_FORMATER.format(new Date());
        String name = fileName.substring(0, extIndx);
        ;
        String extension = fileName.substring(extIndx);
        String myXMindFileName = name + extension;
        return myXMindFileName;
    }

    /**
	 * Sends an HTTP GET request to a url
	 *
	 * @param endpoint - The URL of the server. (Example: " http://www.yahoo.com/search")
	 * @param requestParameters - all the request parameters (Example: "param1=val1&param2=val2"). Note: This method will add the question mark (?) to the request - DO NOT add it yourself
	 * @return - The response from the end point
	 * @throws Exception 
	 */
    private static String sendGetRequest(String endpoint, String requestParameters) throws Exception {
        String result = null;
        if (endpoint.startsWith("http://")) {
            StringBuffer data = new StringBuffer();
            String urlStr = prepareUrl(endpoint, requestParameters);
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            result = sb.toString();
        }
        return result;
    }

    private static String prepareUrl(String endpoint, String requestParameters) {
        String urlStr = endpoint;
        if (requestParameters != null && requestParameters.length() > 0) {
            if (!urlStr.contains("?")) {
                urlStr += "?";
            }
            if (!urlStr.endsWith("?")) {
                urlStr += "&";
            }
            requestParameters = requestParameters.replace(" ", "%20");
            urlStr += requestParameters;
        }
        System.out.println("Request: " + urlStr);
        return urlStr;
    }

    /**
	 * Sends an HTTP GET request to a url
	 *
	 * @param endpoint - The URL of the server. (Example: " http://www.yahoo.com/search")
	 * @param requestParameters - all the request parameters (Example: "param1=val1&param2=val2"). Note: This method will add the question mark (?) to the request - DO NOT add it yourself
	 * @return - The response from the end point
	 * @throws Exception 
	 */
    private static void downloadFile(String endpoint, String requestParameters, String localFilename) throws Exception {
        if (endpoint.startsWith("http://")) {
            StringBuffer data = new StringBuffer();
            String urlStr = prepareUrl(endpoint, requestParameters);
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            byte[] mybytearray = new byte[1024];
            InputStream is = conn.getInputStream();
            FileOutputStream fos = new FileOutputStream(localFilename);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            int bytesRead;
            while ((bytesRead = is.read(mybytearray, 0, mybytearray.length)) >= 0) {
                bos.write(mybytearray, 0, bytesRead);
            }
            bos.close();
        }
    }

    public static void main(String[] args) {
        ConnectionInfo up = new ConnectionInfo("/vadillo_php/upload.php", "localhost", 80);
        ConnectionInfo connection = new ConnectionInfo("/vadillo_php/list.php", "localhost", 80);
        ConnectionInfo down = new ConnectionInfo("/vadillo_php/download.php", "localhost", 80);
        try {
            HashMap<String, String> list = FileUploader.listRepository(connection);
            for (String it : list.keySet()) {
                System.out.println(it);
            }
            ;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
