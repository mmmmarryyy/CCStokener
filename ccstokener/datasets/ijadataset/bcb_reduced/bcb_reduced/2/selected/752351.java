package org.dict.server;

import java.net.*;
import java.io.*;
import java.util.*;
import org.dict.kernel.*;

public class DictHTTPConnection implements Runnable {

    private IDictEngine fEngine;

    private Socket fSocket;

    private ServerSocket fServerSocket;

    private static Hashtable directoryMappings;

    public static File ROOT = new File(System.getProperty("ROOT", "."));

    public static String[] INDEXES = new String[] { "index.html", "index.htm", "default.htm" };

    public DictHTTPConnection(IDictEngine e, Socket s, ServerSocket ss) {
        fEngine = e;
        fSocket = s;
        fServerSocket = ss;
    }

    private static Hashtable getDirectoryMappings() {
        if (directoryMappings == null) {
            directoryMappings = new Hashtable();
            File f = new File(ROOT, "mapping.properties");
            try {
                Properties props = new Properties();
                InputStream is = new FileInputStream(f);
                props.load(is);
                is.close();
                for (Enumeration enumeration = props.keys(); enumeration.hasMoreElements(); ) {
                    Object k = enumeration.nextElement();
                    Object v = props.get(k);
                    File ff = new File((String) v);
                    if (ff.exists()) {
                        directoryMappings.put(k, ff);
                        Logger.getInstance().log("" + k + " mapped to " + ff);
                    }
                }
            } catch (Throwable e) {
                Logger.getInstance().log(e.toString());
            }
        }
        return directoryMappings;
    }

    private void serveAdmin(OutputStream os, IRequest req) throws IOException {
        String cmd = req.getParameter("cmd");
        if ("shutdown".equals(cmd)) {
            String password = req.getParameter("password");
            int k = getAdminPassword();
            if (password != null && password.hashCode() == k) {
                Logger.getInstance().log("Shutdown command received at " + new java.util.Date());
                Logger.getInstance().flush();
                try {
                    String msg = getHead("HTTP/1.0 200 OK", "") + "Server shut down!";
                    os.write(msg.getBytes());
                    os.close();
                } catch (Exception e) {
                }
                stopServer();
            }
        }
    }

    private int getAdminPassword() {
        String root = System.getProperty("ROOT", ".");
        File f = new File(root, ".admin");
        if (!f.exists()) {
            return -1;
        }
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] b = new byte[fis.available()];
            fis.read(b);
            return Integer.parseInt(new String(b));
        } catch (Throwable t) {
            return -1;
        }
    }

    protected String getServerName() {
        return "Ho Ngoc Duc's JDictd 1.3";
    }

    protected String getHead(String start, String end) {
        StringBuffer sb = new StringBuffer();
        sb.append(start);
        sb.append("\nAllow: GET, HEAD, POST\nMIME-Version: 1.0\n");
        sb.append("Server: ");
        sb.append(getServerName());
        sb.append("\n");
        sb.append(end);
        sb.append("\r\n\r\n");
        return sb.toString();
    }

    protected String getOutputEncoding(IRequest req) {
        return "UTF-8";
    }

    protected String getRemote() {
        return fSocket.getInetAddress().getHostName();
    }

    private void processError(int code, OutputStream os) {
        String message = null;
        switch(code) {
            case 400:
                message = getHead("HTTP/1.0 400 Bad Request", "") + "<HTML><BODY><H1>Bad Request</H1>" + "The server could not understand this request.<P></BODY></HTML>";
                break;
            case 404:
                message = getHead("HTTP/1.0 404 Not Found", "") + "<HTML><BODY><H1>Not Found</H1>" + "The server could not find this file.<P></BODY></HTML>";
                break;
            case 403:
                message = getHead("HTTP/1.0 403 Forbidden", "") + "<HTML><BODY><H1>Forbidden</H1>" + "Access is not allowed.<P></BODY></HTML>";
                break;
        }
        try {
            os.write(message.getBytes("latin1"));
            os.flush();
        } catch (Exception e) {
        }
    }

    protected String getContentType(String name) {
        if (name.endsWith("/")) {
            return "text/html";
        }
        return NetUtils.getMimeType(name);
    }

    private File getInputFile(String name) {
        int k = name.indexOf('/', 1);
        if (k > 0) {
            String first = name.substring(0, k);
            File f = (File) getDirectoryMappings().get(first);
            if (f != null) {
                return new File(f, name.substring(k));
            }
        }
        return new File(ROOT, name);
    }

    protected void processGet(String name, OutputStream os) {
        try {
            if (name.endsWith("/")) {
                name = name + "index.html";
            }
            if (name.indexOf("..") != -1) {
                processError(403, os);
                return;
            }
            BufferedInputStream file = null;
            String header;
            String ct = getContentType(name);
            try {
                File f = getInputFile(name);
                int len = -1;
                String tmp = f.getAbsolutePath();
                if (!f.exists() && name.endsWith("/index.html")) {
                    String list = listDir(new File(tmp.substring(0, tmp.length() - 10)));
                    file = new BufferedInputStream(new StringBufferInputStream(list));
                    len = list.length();
                } else {
                    file = new BufferedInputStream(new FileInputStream(f));
                    len = (int) f.length();
                }
                header = getHead("HTTP/1.0 200 OK", "Content-Type: " + ct + "\nContent-Length: " + len);
            } catch (IOException _ex) {
                Logger.getInstance().log(_ex.toString());
                processError(404, os);
                return;
            }
            os.write(header.getBytes("latin1"));
            byte[] b = new byte[1024];
            int len;
            while ((len = file.read(b)) > 0) {
                os.write(b, 0, len);
            }
            file.close();
            os.flush();
        } catch (Exception e) {
        }
    }

    private String listDir(File f) {
        StringBuffer sb = new StringBuffer();
        String[] a = f.list();
        sb.append("<html><head></head><body>");
        for (int i = 0; i < a.length; i++) {
            if (new File(f, a[i]).isDirectory()) {
                sb.append("[DIR] <a href=\"" + URLEncoder.encode(a[i]) + "/\">" + a[i] + "</a><br>\n");
            }
        }
        for (int i = 0; i < a.length; i++) {
            if (new File(f, a[i]).isFile()) {
                sb.append("<a href=\"" + URLEncoder.encode(a[i]) + "\">" + a[i] + "</a><br>\n");
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    public void run() {
        String method = "";
        String uri = "";
        String params = "";
        String version = "";
        int fContentLength = 0;
        try {
            InputStream input = fSocket.getInputStream();
            BufferedReader is = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            do {
                String request = is.readLine();
                if (request == null || request.length() == 0) break;
                StringTokenizer tokenizer = new StringTokenizer(request, " ");
                if (!tokenizer.hasMoreTokens()) break;
                String first = tokenizer.nextToken();
                if (first.equals("GET")) {
                    method = "GET";
                    String query = tokenizer.nextToken();
                    int idx = query.indexOf('?');
                    if (idx == -1) {
                        processGet(query, fSocket.getOutputStream());
                        fSocket.close();
                        return;
                    } else {
                        uri = query.substring(0, idx);
                        params = query.substring(idx + 1);
                    }
                } else if (first.equals("POST")) {
                    method = "POST";
                    uri = tokenizer.nextToken();
                } else if (request.toUpperCase().startsWith("CONTENT-LENGTH:")) fContentLength = Integer.parseInt(tokenizer.nextToken());
            } while (true);
            if (method.equals("POST") && fContentLength < 65636) {
                char[] b = new char[fContentLength];
                is.read(b, 0, fContentLength);
                params = new String(b);
            }
            OutputStream os = new BufferedOutputStream(fSocket.getOutputStream());
            serve(os, uri, params);
            os.flush();
        } catch (Throwable e) {
        }
        try {
            fSocket.close();
        } catch (IOException e) {
        }
    }

    protected void serve(OutputStream os, IRequest req) throws IOException {
        if (req.getRequestURI().endsWith("admin")) {
            serveAdmin(os, req);
            return;
        }
        if (req.getRequestURI().endsWith("redir")) {
            serveRedir(os, req);
            return;
        }
        String enc = getOutputEncoding(req);
        PrintWriter out = new PrintWriter(new OutputStreamWriter(os, enc));
        try {
            String header = getHead("HTTP/1.0 200 OK", "Content-type: text/html; charset=" + enc);
            out.print(header);
            IAnswer[] arr = fEngine.lookup(req);
            HTMLPrinter.printAnswers(fEngine, req, arr, true, out);
            out.flush();
        } catch (Throwable t) {
            out.println("<pre>");
            t.printStackTrace(out);
            out.println("</pre>");
            out.flush();
        }
    }

    private void serveRedir(OutputStream os, IRequest req) throws IOException {
        String urlString = req.getParameter("url");
        URL url = new URL(urlString);
        URLConnection con = url.openConnection();
        InputStream is = new BufferedInputStream(con.getInputStream());
        String ct = con.getContentType();
        int cl = con.getContentLength();
        String jsLink = "<script language=\"JavaScript1.2\" src=\"/tddt.js\" type='text/javascript'></script>\n";
        int i;
        String head = getHead("HTTP/1.0 200 OK", "Content-Type: " + ct + "\nContent-Length: -1");
        os.write(head.getBytes());
        if (ct != null && ct.indexOf("html") != -1) {
            os.write(jsLink.getBytes());
            NetUtils.saveChangeLink(url, os);
        } else {
            int len;
            byte[] b = new byte[1024];
            while ((len = is.read(b)) >= 0) {
                os.write(b, 0, len);
            }
        }
        os.flush();
    }

    private void serve(OutputStream os, String uri, String params) throws IOException {
        IRequest req = new SimpleRequest(uri, params);
        serve(os, req);
        String msg = params;
        if (msg != null && msg.length() > 100) {
            msg = msg.substring(0, 100) + " ...";
        }
        Logger.getInstance().log(getRemote() + " " + msg);
    }

    protected void stopServer() {
        try {
            fServerSocket.close();
        } catch (Throwable e) {
            Logger.getInstance().log("Error closing server socket: " + e);
        }
        if (Boolean.getBoolean("daemon")) {
            System.exit(0);
        }
    }
}
