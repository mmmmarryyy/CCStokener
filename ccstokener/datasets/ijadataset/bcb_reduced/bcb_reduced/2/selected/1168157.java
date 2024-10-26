package com.handy.socket;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.log4j.Logger;

public class HttpConnection {

    /**
	 * 获取网页内容。
	 * @return
	 */
    public String getHttpText() {
        URL url = null;
        try {
            url = new URL(getUrl());
        } catch (MalformedURLException e) {
            log.error(e.getMessage());
        }
        StringBuffer sb = new StringBuffer();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(getRequestMethod());
            conn.setDoOutput(true);
            if (getRequestProperty() != null && "".equals(getRequestProperty())) {
                conn.setRequestProperty("Accept", getRequestProperty());
            }
            PrintWriter out = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), getCharset()));
            out.println(getParam());
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), getCharset()));
            String inputLine;
            int i = 1;
            while ((inputLine = in.readLine()) != null) {
                if (getStartLine() == 0 && getEndLine() == 0) {
                    sb.append(inputLine).append("\n");
                } else {
                    if (getEndLine() > 0) {
                        if (i >= getStartLine() && i <= getEndLine()) {
                            sb.append(inputLine).append("\n");
                        }
                    } else {
                        if (i >= getStartLine()) {
                            sb.append(inputLine).append("\n");
                        }
                    }
                }
                i++;
            }
            in.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return sb.toString();
    }

    /**
	 * 下载文件
	 * @param srcFile 要下载的文件
	 * @param destFile 保存的文件
	 */
    public void getFile(String srcFile, String destFile) {
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        HttpURLConnection conn = null;
        URL url = null;
        byte[] buf = new byte[8096];
        int size = 0;
        try {
            url = new URL(srcFile);
            conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            bis = new BufferedInputStream(conn.getInputStream());
            fos = new FileOutputStream(destFile);
            while ((size = bis.read(buf)) != -1) {
                fos.write(buf, 0, size);
            }
            fos.close();
            bis.close();
        } catch (MalformedURLException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            conn.disconnect();
        }
    }

    private String url = "";

    private String param = "";

    private String requestMethod = "GET";

    private String charset = "";

    private int startLine = 0;

    private int endLine = 0;

    private String requestProperty = "";

    private static Logger log = Logger.getLogger(HttpConnection.class);

    public String getUrl() {
        return url;
    }

    /**
	 * 设置要访问的页面。
	 * @param url
	 */
    public void setUrl(String url) {
        this.url = url;
    }

    public String getParam() {
        return param;
    }

    /**
	 * 设置要传递的参数。
	 * @param param
	 */
    public void setParam(String param) {
        this.param = param;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    /**
	 * 设置提交方式。
	 * @param requestMethod
	 */
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getCharset() {
        return charset;
    }

    /**
	 * 设置编码方式。
	 * @param charset
	 */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    public int getStartLine() {
        return startLine;
    }

    /**
	 * 设置要获取内容的开始行数。
	 * @param startLine
	 */
    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    /**
	 * 设置要获取内容的结束行数。
	 * @param endLine
	 */
    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public static final String REQUEST_METHOD_GET = "GET";

    public static final String REQUEST_METHOD_POST = "POST";

    public String getRequestProperty() {
        return requestProperty;
    }

    public void setRequestProperty(String requestProperty) {
        this.requestProperty = requestProperty;
    }
}
