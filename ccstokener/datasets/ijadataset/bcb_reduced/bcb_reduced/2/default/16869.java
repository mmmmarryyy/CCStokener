import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * �������ǿͻ��˳�����ߺ�̨����ģ���������upload����ݽ���װ����� . ����Э����ο�w3c RFC1867�淶 ���κ�����뵽�������ҽ���:
 * 
 * http://www.zhanglihai.com/blog/c_359_Socket_Upload.html
 */
public class MultiPartDataSender {

    private String host = "";

    private String url = "";

    private int port = 80;

    private Socket s = null;

    private BufferedReader sin;

    private BufferedOutputStream sout;

    private String responseCharset = "UTF-8";

    private String requestCharset = "UTF-8";

    private ArrayList<String> localFiles = new ArrayList<String>(3);

    private HashMap<String, String> textFields = new HashMap<String, String>(5);

    private static Logger log = Logger.getLogger(MultiPartDataSender.class);

    private HashMap<String, String> cookies = new HashMap<String, String>(3);

    private HashMap<String, String> requestHeaders = new HashMap<String, String>(3);

    private HashMap<String, String> responseHeaders = new HashMap<String, String>(3);

    private int statusCode = -1;

    private String uri;

    private String responseText = null;

    public void setUrl(String url) {
        this.url = url;
        try {
            URL url_ = new URL(url);
            this.host = url_.getHost();
            this.port = url_.getPort();
            this.port = port == -1 ? 80 : port;
            this.uri = url_.getFile();
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public boolean connect() {
        try {
            s = new Socket(host, port);
            this.sin = new BufferedReader(new InputStreamReader(s.getInputStream(), responseCharset));
            this.sout = new BufferedOutputStream(s.getOutputStream());
            return true;
        } catch (Exception ex) {
            log.error("", ex);
            return false;
        }
    }

    public boolean send() {
        try {
            byte[] data = new byte[1024];
            int len = -1;
            String boundId = "-----------------------------" + System.currentTimeMillis() + "ab20d";
            File tmpFile = new File(System.getProperties().getProperty("java.io.tmpdir", "/opt/"), "upload_tmp_" + System.currentTimeMillis());
            FileOutputStream tmpFout = new FileOutputStream(tmpFile);
            StringBuffer content = new StringBuffer();
            String name;
            for (Iterator<String> it = this.textFields.keySet().iterator(); it.hasNext(); ) {
                name = it.next();
                content.append(boundId).append("\r\n");
                content.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n");
                content.append("\r\n");
                content.append(new String(textFields.get(name).getBytes(requestCharset))).append("\r\n");
            }
            tmpFout.write(content.toString().getBytes());
            tmpFout.flush();
            int p = 0;
            for (String file : localFiles) {
                content.setLength(0);
                content.append(boundId).append("\r\n");
                content.append("Content-Disposition: form-data; name=\"file_").append(p++).append("\"; filename=\"").append(file).append("\"\r\n");
                content.append("Content-Type: ").append(getContentType(file)).append("\r\n");
                content.append("\r\n");
                tmpFout.write(content.toString().getBytes());
                tmpFout.flush();
                FileInputStream fin = null;
                try {
                    fin = new FileInputStream(file);
                    while ((len = fin.read(data, 0, data.length)) != -1) {
                        tmpFout.write(data, 0, len);
                    }
                    tmpFout.flush();
                    tmpFout.write("\r\n".getBytes());
                } catch (Exception ex) {
                    log.error("read file .." + file, ex);
                } finally {
                    try {
                        fin.close();
                    } catch (Exception ex) {
                    }
                }
            }
            content.setLength(0);
            content.append(boundId + "\r\n");
            content.append("Content-Disposition: form-data; name=\"submit\"\r\n");
            content.append("\r\n");
            content.append("Submit\r\n");
            content.append(boundId).append("--");
            tmpFout.write(content.toString().getBytes());
            tmpFout.flush();
            tmpFout.close();
            content.setLength(0);
            StringBuffer header = new StringBuffer();
            header.append("POST ").append(uri).append(" HTTP/1.0\r\n");
            header.append("Host:").append(host);
            if (port != 80) header.append(":").append(port);
            header.append("\r\n");
            header.append("User-Agent:Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)\r\n");
            header.append("Accept: text/html, application/xml;q=0.9, application/xhtml+xml, image/png, image/jpeg, image/gif, image/x-xbitmap, */*;q=0.1\r\n");
            header.append("Accept-Charset: gbk, utf-8, utf-16, iso-8859-1;q=0.6, *;q=0.1\r\n");
            header.append("Connection:close\r\n");
            if (requestHeaders.size() > 0) {
                for (Iterator<String> it = requestHeaders.keySet().iterator(); it.hasNext(); ) {
                    name = it.next();
                    header.append(name).append(":").append(requestHeaders.get(name)).append("\r\n");
                }
            }
            header.append("Content-Length:" + (tmpFile.length()) + "\r\n");
            header.append("Content-Type:multipart/form-data; boundary=" + boundId.substring(2) + "\r\n");
            p = 1;
            if (cookies.size() > 0) {
                header.append("Cookie:");
                for (Iterator<String> it = cookies.keySet().iterator(); it.hasNext(); ) {
                    name = it.next();
                    header.append(name).append("=").append(cookies.get(name));
                    if (p++ != cookies.size()) header.append("; ");
                }
            }
            header.append("\r\n\r\n");
            sout.write(header.toString().getBytes());
            FileInputStream tmpFin = new FileInputStream(tmpFile);
            while ((len = tmpFin.read(data, 0, data.length)) != -1) {
                sout.write(data, 0, len);
            }
            sout.flush();
            tmpFin.close();
            String line;
            line = sin.readLine();
            statusCode = Integer.parseInt(line.substring(9, 12));
            content.setLength(0);
            boolean isHeader = true;
            String[] kv;
            while ((line = sin.readLine()) != null) {
                if (line.trim().equals("") && isHeader) {
                    isHeader = false;
                }
                if (!isHeader) {
                    content.append(line).append("\r\n");
                }
                if (isHeader) {
                    kv = line.split(":");
                    if (kv.length == 2) responseHeaders.put(kv[0].trim().toLowerCase(), kv[1].trim());
                }
            }
            tmpFile.delete();
            this.responseText = content.toString();
            return this.statusCode == 200;
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public void close() {
        this.cookies.clear();
        this.localFiles.clear();
        this.textFields.clear();
        this.requestHeaders.clear();
        this.responseHeaders.clear();
        try {
            sin.close();
        } catch (Exception e) {
        }
        try {
            sout.close();
        } catch (Exception e) {
        }
        try {
            s.close();
        } catch (Exception e) {
        }
    }

    public void addTextField(String name, String value) {
        if (name != null && value != null) {
            this.textFields.put(name, value);
        }
    }

    public void addCookie(String name, String value) {
        this.cookies.put(name, value);
    }

    public void addLocalFile(String file) {
        this.localFiles.add(file);
    }

    public String getHeader(String name) {
        if (name == null) return null;
        return responseHeaders.get(name.toLowerCase());
    }

    public Iterator<String> getHeaderNames() {
        return responseHeaders.keySet().iterator();
    }

    public void addHeader(String name, String value) {
        if (name == null) return;
        if (value == null) return;
        String tmpName = name.toLowerCase().trim();
        if (!tmpName.equals("user-agent") && !tmpName.equals("host") && !tmpName.equals("accept-charset") && !tmpName.equals("connection") && !tmpName.equals("content-type") && !tmpName.equals("content-length") && !tmpName.equals("accept")) this.requestHeaders.put(name, value);
    }

    public MultiPartDataSender() {
    }

    public void setRequestCharset(String charset) {
        this.requestCharset = charset;
    }

    public void setResponseCharset(String charset) {
        this.responseCharset = charset;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getResponseText() {
        return this.responseText;
    }

    private static String getContentType(String file) {
        try {
            URL furl = new URL("file:/" + file);
            String mimeType = furl.openConnection().getContentType();
            if (mimeType == null || mimeType.indexOf("unknow") != -1) return "application/octet-stream";
            return mimeType;
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
}
