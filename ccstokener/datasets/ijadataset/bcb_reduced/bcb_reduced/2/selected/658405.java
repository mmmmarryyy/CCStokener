package com.dukesoftware.utils.net;

import static com.dukesoftware.utils.io.IOUtils._1K_BYTES;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import org.xml.sax.helpers.DefaultHandler;
import com.dukesoftware.utils.common.Closure;
import com.dukesoftware.utils.common.PrintUtils;
import com.dukesoftware.utils.common.StringUtils;
import com.dukesoftware.utils.io.Copier;
import com.dukesoftware.utils.io.IOUtils;
import com.dukesoftware.utils.net.deprecated.PostString;
import com.dukesoftware.utils.xml.XMLUtils;
import com.google.common.io.Closeables;

public class HttpUtils {

    public static final String CRLF = "\r\n";

    /**
	 * @deprecated only implemented html type.
	 * @param fileName
	 * @return
	 */
    public static final String contentType(String fileName) {
        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        }
        return "";
    }

    /**
	 * TODO useful http responce.
	 * @param output
	 * @param s
	 * @throws IOException
	 */
    private static void processForGet(OutputStream output, StringTokenizer s) throws IOException {
        String fileName = s.nextToken();
        fileName = "." + fileName;
        FileInputStream fis = null;
        boolean fileExists = true;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            fileExists = false;
        }
        String serverLine = "Server: fpont simple java httpServer";
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;
        String contentLengthLine = "error";
        if (fileExists) {
            statusLine = "HTTP/1.0 200 OK" + CRLF;
            contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
            contentLengthLine = "Content-Length: " + (Integer.valueOf(fis.available())).toString() + CRLF;
        } else {
            statusLine = "HTTP/1.0 404 Not Found" + CRLF;
            contentTypeLine = "text/html";
            entityBody = "<HTML>" + "<HEAD><TITLE>404 Not Found</TITLE></HEAD>" + "<BODY>404 Not Found" + "<br>usage:http://www.snaip.com:4444/" + "fileName.html</BODY></HTML>";
        }
        output.write(statusLine.getBytes());
        output.write(serverLine.getBytes());
        output.write(contentTypeLine.getBytes());
        output.write(contentLengthLine.getBytes());
        output.write(CRLF.getBytes());
        if (fileExists) {
            Copier.copy(fis, output, 1024);
            fis.close();
        } else {
            output.write(entityBody.getBytes());
        }
    }

    public static HttpURLConnection createHttpGetURLConnection(String url) throws IOException, MalformedURLException, ProtocolException {
        URLConnection uc = new URL(url).openConnection();
        HttpURLConnection connection = (HttpURLConnection) uc;
        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        return connection;
    }

    public static final void parse(String url, ParserCallback cb, HTMLEditorKit.Parser pd) {
        try {
            URL u = new URL(url);
            InputStream is = u.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            pd.parse(br, cb, true);
            br.close();
            is.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void processLine(HttpURLConnection con, Closure<String> body) throws IOException {
        String line;
        InputStream inputStream = null;
        try {
            inputStream = con.getInputStream();
        } catch (IOException e) {
            inputStream = con.getErrorStream();
        }
        BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = rd.readLine()) != null) {
            body.exec(line);
        }
        rd.close();
    }

    private static final Pattern pat = Pattern.compile("/{1,1}+");

    private static final Pattern parentPattern = Pattern.compile("\\.\\./");

    @Deprecated
    public static final String absoluteURL(URL source, String s) {
        if (s.startsWith("http://")) {
            return s;
        }
        int count = 0;
        if (s.charAt(0) == '/') {
            s = s.substring(1);
        } else {
            while (s.startsWith("../")) {
                s = parentPattern.matcher(s).replaceFirst("");
                count++;
            }
        }
        StringBuffer sb = new StringBuffer("http://").append(source.getHost());
        String path = source.getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length());
        } else {
            int index = path.lastIndexOf("/");
            if (index > 0) {
                path = path.substring(0, index);
            }
        }
        String[] parts = pat.split(path);
        for (int i = 0, len = parts.length - count; i < len; i++) {
            if (parts[i].length() > 0) {
                sb.append("/").append(parts[i]);
            }
        }
        sb.append("/");
        sb.append(s);
        return sb.toString();
    }

    public static final String W_CRLF = CRLF + CRLF;

    public static final String CDFormDataName = "Content-Diposition: form-data; name=";

    public static final String ATTACHMENT = "attachment";

    public static final String LOCAL_HOST = "http://localhost";

    public static final String GET = "GET";

    public static final String POST = "POST";

    /**
	 * response to client.
	 * 
	 * @param len 
	 * @param type MIME type
	 */
    public static final void responseSuccess(int len, String type, OutputStream out) throws IOException {
        PrintWriter prn = new PrintWriter(out);
        prn.print("HTTP/1.1 200 OK\r\n");
        prn.print("Connection: close\r\n");
        prn.print("Content-Length: ");
        prn.print(len);
        prn.print("\r\n");
        prn.print("Content-Type: ");
        prn.print(type);
        prn.print("\r\n\r\n");
        prn.flush();
        prn.close();
    }

    public static final void connectExecuteDisconnect(String url, HttpProcess<?> process) throws IOException {
        URL urlObj = null;
        HttpURLConnection urlCon = null;
        try {
            urlObj = new URL(url);
            urlCon = (HttpURLConnection) urlObj.openConnection();
            process.apply(urlCon);
        } finally {
            disconnect(urlCon);
        }
    }

    public static final void connectAndPost(String url, String body) throws IOException {
        connectExecuteDisconnect(url, new PostString(body, HttpUtils.POST));
    }

    public static final void get(String url, final String chrsetName, final String path) throws IOException {
        connectExecuteDisconnect(url, new HttpProcess<Object>() {

            @Override
            public void apply(HttpURLConnection urlCon) throws IOException {
                urlCon.setRequestMethod(GET);
                String content = IOUtils.toString(urlCon.getInputStream(), chrsetName);
                IOUtils.writeStringToFile(new File(path), content, chrsetName);
            }
        });
    }

    public static final void readPrintCloseInputStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String responseData = null;
        while ((responseData = reader.readLine()) != null) {
            System.out.print(responseData);
        }
        is.close();
    }

    public static final void disconnect(HttpURLConnection urlconn) {
        if (urlconn != null) {
            urlconn.disconnect();
        }
    }

    public static void main(String[] args) throws URISyntaxException, IOException {
        System.out.println(getStringContentsFromURL("http://www.youtube.com/watch?gl=JP&hl=ja&v=n0fhz1SGnSE", "utf-8"));
    }

    public static String getStringContentsFromURL(String u, String charset) throws URISyntaxException, IOException {
        URL url = new URL(u);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            return IOUtils.toString(connection.getInputStream(), charset);
        } finally {
            HttpUtils.disconnect(connection);
        }
    }

    public static byte[] getContentsFromURL(String u, int bufsize) throws IOException {
        URL url = new URL(u);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            return IOUtils.toBytes(connection.getInputStream());
        } finally {
            HttpUtils.disconnect(connection);
        }
    }

    @Deprecated
    public static void getAndProcessContents(String videoPageURL, int bufsize, String charset, Closure<String> process) throws IOException {
        URL url = null;
        HttpURLConnection connection = null;
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            url = new URL(videoPageURL);
            connection = (HttpURLConnection) url.openConnection();
            is = connection.getInputStream();
            isr = new InputStreamReader(is, charset);
            br = new BufferedReader(isr);
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                process.exec(line);
            }
        } finally {
            Closeables.closeQuietly(br);
            Closeables.closeQuietly(isr);
            Closeables.closeQuietly(is);
            HttpUtils.disconnect(connection);
        }
    }

    public static final String getStringFromRequest(HttpServletRequest request, String charEncode) throws IOException {
        return IOUtils.toString(request.getInputStream(), charEncode);
    }

    public static final HttpURLConnection connect(String url) {
        URL urlObj = null;
        HttpURLConnection urlCon = null;
        try {
            urlObj = new URL(url);
            urlCon = (HttpURLConnection) urlObj.openConnection();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urlCon;
    }

    /**
	 * HTTP Get Request
	 * @param uri
	 * @return response
	 * @throws Exception
	 */
    public static String sendDoGetRequest(String uri) throws Exception {
        String ret = "";
        BufferedInputStream bis = null;
        try {
            URL u = new URL(uri);
            URLConnection uc = u.openConnection();
            HttpURLConnection connection = (HttpURLConnection) uc;
            connection.setRequestMethod("GET");
            InputStream in = connection.getInputStream();
            ret = new String(IOUtils.toBytes(in));
        } finally {
            Closeables.closeQuietly(bis);
        }
        return ret;
    }

    public static final void sendMultiPartRequest(String url, String text, File file) throws IOException {
        String boundary = StringUtils.generateRandomString("1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_", 40);
        URL urlObj = new URL(url);
        URLConnection conn = urlObj.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        String start = "--" + boundary + CRLF;
        out.writeBytes(start);
        out.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n");
        out.writeBytes("Content-Type: text/plain; charset=utf-8");
        out.writeBytes(W_CRLF);
        out.write(text.getBytes("utf-8"));
        out.writeBytes(CRLF);
        out.writeBytes(start);
        out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"");
        out.write(file.getName().getBytes("utf-8"));
        out.writeBytes("\"");
        out.writeBytes(CRLF);
        out.writeBytes("Content-Type: application/octet-stream");
        out.writeBytes(W_CRLF);
        out.writeBytes(IOUtils.toString(file, "utf-8"));
        out.writeBytes(CRLF);
        out.writeBytes("--" + boundary + "--");
        out.flush();
        out.close();
        PrintUtils.print(conn.getInputStream(), "utf-8");
    }

    /**
	 * @deprecated
	 * @param urlconn
	 * @throws IOException
	 */
    public static void printHeaderInfo(HttpURLConnection urlconn) throws IOException {
        Map<String, List<String>> headers = urlconn.getHeaderFields();
        System.out.println("ResponceHeader:");
        for (Entry<String, List<String>> entry : headers.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("ResponceCode[" + urlconn.getResponseCode() + "] " + "ResponceMessage[" + urlconn.getResponseMessage() + "]");
    }

    public static void parseURL(String urlString, DefaultHandler handler, Map<String, String> properties) throws MalformedURLException {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw e;
        }
        HttpURLConnection urlconn = null;
        try {
            urlconn = (HttpURLConnection) url.openConnection();
            urlconn.setRequestMethod("GET");
            urlconn.setInstanceFollowRedirects(false);
            if (properties != null) {
                for (Entry<String, String> property : properties.entrySet()) {
                    urlconn.setRequestProperty(property.getKey(), property.getValue());
                }
            }
            urlconn.connect();
            XMLUtils.parse(urlconn.getInputStream(), handler);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlconn != null) {
                urlconn.disconnect();
            }
        }
    }

    public static void test(URL url) throws IOException {
        String boundary = StringUtils.generateRandomString("1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_", 40);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        out.writeBytes("--" + boundary + CRLF);
        out.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n");
        out.writeBytes("Content-Type: text/plain; charset=utf-8");
        out.writeBytes(W_CRLF);
        out.write("eLXg".getBytes("utf-8"));
        out.writeBytes(CRLF);
        File file = new File("c:\\temp\\send.txt");
        out.writeBytes("--" + boundary + CRLF);
        out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"");
        out.write(file.getName().getBytes("utf-8"));
        out.writeBytes("\"");
        out.writeBytes(CRLF);
        out.writeBytes("Content-Type: application/octet-stream");
        out.writeBytes(W_CRLF);
        out.writeBytes(IOUtils.toString(file, _1K_BYTES, "utf-8"));
        out.writeBytes(CRLF);
        out.writeBytes("--" + boundary + "--");
        out.flush();
        out.close();
        PrintUtils.print(conn.getInputStream(), "utf-8");
    }
}
