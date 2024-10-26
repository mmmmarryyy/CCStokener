package cn.icybear.tools.ipclient4j;

import com.sonalb.net.http.HTTPRedirectHandler;
import com.sonalb.net.http.cookie.Client;
import com.sonalb.net.http.cookie.CookieJar;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An java client of <a href="http://ipserver.guet.edu.cn/ipmanager/">IPManager</a> which is a part of GUET BAS.
 * @author Bearice (Bearice@Gmail.com)
 */
public class IPClient4J {

    public static final String USER_AGENT = "IPClient4J/1.0 (" + System.getProperty("os.name") + "; " + System.getProperty("os.version") + "; " + System.getProperty("os.arch") + ")";

    public static final Logger LOG = Logger.getLogger("IPClient4J");

    private String server;

    private Client client = new Client();

    private CookieJar cookie = new CookieJar();

    /**
     * Creates a Client object with server given.
     * This constructor would connect to server and try to get a cookie from server.
     * An IOException will be thrown if a bad-response (not 200) or a null-cookie recived from server.
     * @param server server's ip address or host name
     * @throws java.io.IOException A bad-response (not 200), a null-cookie, or network problems.
     */
    public IPClient4J(String server) throws IOException {
        this.server = server;
        HttpURLConnection connection = openConnection("/ipmanager/");
        checkHttpResponse(connection, 200);
        if (cookie.isEmpty()) {
            throw new IOException("No cookie returned from server");
        }
        LOG.info("Connected to server: " + server);
    }

    /**
     * Open a HttpURLConnection object with cookie (if exist) and user-agent set.
     * @param path path of this request.
     * @return a connection.
     * @throws java.io.IOException IO Error.
     */
    HttpURLConnection openConnection(String path) throws IOException {
        LOG.finest("OpenConnection: " + path);
        URL url = new URL("http", server, path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        client.setCookies(connection, cookie);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        return connection;
    }

    /**
     * Check if request code euqals code and throw an IOException if not.
     * @param conn connection of this request.
     * @param code code we wanted.
     * @throws java.io.IOException if not euqal or io errors.
     */
    void checkHttpResponse(HttpURLConnection conn, int code) throws IOException {
        LOG.finest("checkHttpResponse: got: [" + conn.getResponseMessage() + "], wants: [" + code + "]");
        cookie.addAll(client.getCookies(conn));
        if (conn.getResponseCode() != code) {
            System.out.println(readString(conn.getErrorStream()));
            throw new IOException(conn.getResponseMessage());
        }
    }

    static String readString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        char[] buf = new char[2048];
        StringBuilder sb = new StringBuilder();
        int len = 0;
        while ((len = reader.read(buf)) != -1) {
            sb.append(buf, 0, len);
        }
        is.close();
        return sb.toString();
    }

    /**
     * POST a string to server.
     * @param path path to post
     * @param val the string to post
     * @return server response.
     * @throws java.io.IOException io error, null args or response code is not 200.
     */
    InputStream postString(String path, String val) throws IOException {
        HttpURLConnection connection = openConnection(path);
        HTTPRedirectHandler handler = new HTTPRedirectHandler(connection);
        connection.setRequestMethod("POST");
        client.setCookies(connection, cookie);
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        os.write(val.getBytes());
        os.close();
        handler.connect();
        LOG.fine("postString() OK!");
        cookie.addAll(handler.getCookieJar());
        return handler.getInputStream();
    }

    /**
     * GET a string to server.
     * @param path path to post
     * @return server response stream.
     * @throws java.io.IOException io error, null args or response code is not 200.
     */
    String getString(String path) throws IOException {
        HttpURLConnection connection = openConnection(path);
        HTTPRedirectHandler handler = new HTTPRedirectHandler(connection);
        connection.setRequestMethod("GET");
        client.setCookies(connection, cookie);
        handler.connect();
        LOG.fine("getString() OK!");
        cookie.addAll(handler.getCookieJar());
        return readString(handler.getInputStream());
    }

    /**
     * Fatch an image from server.an image of JPEG,BMP or GIF is acceptable. throws IOException when response os not an image or unregenesable.
     * @return the image.
     * @throws java.io.IOException response not an image.
     */
    public Image getCodeImage() throws IOException {
        HttpURLConnection connection = openConnection("/ipmanager/servlet/randomnum");
        checkHttpResponse(connection, 200);
        LOG.fine("getCodeImage() OK!");
        Image ret = (Image) connection.getContent(new Class[] { Image.class });
        if (ret == null) {
            throw new IOException("No image returned from server");
        } else {
            return ret;
        }
    }

    private static final Pattern p_msg = Pattern.compile("alert\\(\"(.*)\"\\);");

    /**
     * Send a login request to server.
     * asserts username ,password ,and code is not null or empty and len(code)==4.
     * @param username
     * @param password password, destory before return (zero filled)
     * @param code
     * @throws SecurityException if server rejected your request.
     * @throws IllegalArgumentException username ,password ,and code is null or empty or len(code)!=4.
     */
    public void sendLogin(String username, char[] password, String code) throws IOException, SecurityException {
        if ((username == null || username.equals("")) || (password == null || password.length == 0) || (code == null || code.length() != 4)) {
            throw new IllegalArgumentException("Please fill all fileds correctly");
        }
        String request = String.format("userid=%s&passwd=%s&validnum=%s", username, new String(password), code);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Request: " + request);
        }
        String response = readString(postString("/ipmanager/", request));
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Response: " + response);
        }
        Arrays.fill(password, '\0');
        Matcher m = p_msg.matcher(response);
        if (m.find()) {
            String msg = m.group(1);
            throw new SecurityException("Server rejected: " + msg);
        }
    }

    public double fee;

    public long used;

    public int spdIn, spdOut, spdInTcp, spdOutTcp, spdInUdp, spdOutUdp, spdInPkt, spdOutPkt;

    public String lastRenew;

    private static final Pattern p_fee = Pattern.compile("<td >剩余金额：(.*)</td>");

    private static final Pattern p_used = Pattern.compile("<td >使用流量：(.*)</td>");

    private static final Pattern p_spdIn = Pattern.compile("<td width=\"50%\">总字节流入速度：(.*)\\(字节/秒\\)</td>");

    private static final Pattern p_spdOut = Pattern.compile("<td width=\"50%\">总字节流出速度：(.*)\\(字节/秒\\)</td>");

    private static final Pattern p_spdInTcp = Pattern.compile("<td>TCP字节流入速度：(.*)\\(字节/秒\\)</td>");

    private static final Pattern p_spdOutTcp = Pattern.compile("<td>TCP字节流出速度：(.*)\\(字节/秒\\)</td>");

    private static final Pattern p_spdInUdp = Pattern.compile("<td>UDP字节流入速度：(.*)\\(字节/秒\\)</td>");

    private static final Pattern p_spdOutUdp = Pattern.compile("<td>UDP字节流出速度：(.*)\\(字节/秒\\)</td>");

    private static final Pattern p_spdInPkt = Pattern.compile("<td>包流入速度：(.*)\\(个/秒\\)</td>");

    private static final Pattern p_spdOutPkt = Pattern.compile("<td>包流出速度：(.*)\\(个/秒\\)</td>");

    private static final Pattern p_lastRenew = Pattern.compile("<td align=\"right\" bgcolor=\"#CCCCCC\">最后刷新时间：(.*)</td>");

    private static String matchPattern(Pattern p, String s) {
        Matcher m = p.matcher(s);
        if (m.find()) {
            return m.group(1);
        } else {
            throw new IllegalStateException("Unreadable server response.");
        }
    }

    /**
     * Send a request to server to tell I'm still here. update fields such like fee,used,and lastRenew if succed. throw an IllegalStateException other wise.
     * Call this method only after a succeful login.
     * @throws IOException when occured an io error
     * @throws IllegalStateException not loged on or server error.
     */
    public synchronized void keepAlive() throws IOException {
        String ret = getString("/ipmanager/main.jsp");
        fee = Double.parseDouble(matchPattern(p_fee, ret));
        used = Long.parseLong(matchPattern(p_used, ret).replace(",", ""));
        spdIn = Integer.parseInt(matchPattern(p_spdIn, ret).replace(",", ""));
        spdOut = Integer.parseInt(matchPattern(p_spdOut, ret).replace(",", ""));
        spdInTcp = Integer.parseInt(matchPattern(p_spdInTcp, ret).replace(",", ""));
        spdOutTcp = Integer.parseInt(matchPattern(p_spdOutTcp, ret).replace(",", ""));
        spdInUdp = Integer.parseInt(matchPattern(p_spdInUdp, ret).replace(",", ""));
        spdOutUdp = Integer.parseInt(matchPattern(p_spdOutUdp, ret).replace(",", ""));
        spdInPkt = Integer.parseInt(matchPattern(p_spdInPkt, ret).replace(",", ""));
        spdOutPkt = Integer.parseInt(matchPattern(p_spdOutPkt, ret).replace(",", ""));
        lastRenew = matchPattern(p_lastRenew, ret);
    }

    /**
     * Send a close request to server. should call reset to clean state data after this method return.
     */
    public void sendClose() throws IOException {
        getString("/ipmanager/close.jsp");
    }

    /**
     * Clear state data.
     */
    public void reset() {
        cookie.clear();
    }
}
