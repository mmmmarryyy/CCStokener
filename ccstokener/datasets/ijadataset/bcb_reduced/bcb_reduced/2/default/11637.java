import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.CookieHandler;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class Connection {

    String[] sSideURLCollection = { "http://www.pennergame.de", "http://berlin.pennergame.de", "http://muenchen.pennergame.de" };

    String sSideURL;

    String sLogoutURL, sLoginURL, sInfoURL, sActivityURL, sCollectURL, sSellURL, sOverviewURL;

    String sLoginLabel;

    String sUID;

    String sPWD;

    int iCTY;

    boolean LogedIn, collectStarted, gettingPage, sellSuccess, useProxy;

    String sUserAgent;

    URLConnection conUrl;

    String encodedProxyAuthorization = null;

    String proxyuser, proxypasswd;

    String oldProxyHost = "";

    Connection() {
        sLogoutURL = "/logout/";
        sLoginURL = "/login/check/";
        sInfoURL = "/stock/bottle/";
        sActivityURL = "/activities/";
        sCollectURL = "/activities/bottle/";
        sSellURL = "/stock/bottle/sell/";
        sOverviewURL = "/overview/";
        useProxy = false;
        LogedIn = false;
        collectStarted = false;
        gettingPage = true;
        conUrl = null;
        sUserAgent = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.5; de; rv:1.9.0.9) Gecko/2009040820 Firefox/3.0.9";
    }

    public void setProxyUID(String UID) {
        proxyuser = UID;
    }

    public void setProxyPWD(char[] PWD) {
        proxypasswd = String.valueOf(PWD);
    }

    public void setProxy(boolean proxy) {
        useProxy = proxy;
        String authorization = proxyuser + ":" + proxypasswd;
        encodedProxyAuthorization = "Basic " + Connection.encodeBase64(authorization);
        Authenticator.setDefault(new HttpAuthenticateProxy(proxyuser, proxypasswd));
    }

    public class HttpAuthenticateProxy extends Authenticator {

        String username = null;

        String password = null;

        public HttpAuthenticateProxy(String username, String password) {
            this.username = username;
            this.password = password;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }

    public static String encodeBase64(String code) {
        sun.misc.BASE64Encoder enCoder = new sun.misc.BASE64Encoder();
        return enCoder.encode(code.getBytes());
    }

    public boolean isServerError() {
        return gettingPage;
    }

    public String getLoginParams() throws UnsupportedEncodingException {
        String data = "&" + URLEncoder.encode((String) sLoginLabel, "UTF-8") + "=" + URLEncoder.encode(sUID, "UTF-8");
        data += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(sPWD, "UTF-8");
        data += "&" + URLEncoder.encode("city", "UTF-8") + "=" + URLEncoder.encode((String) sSideURL + (String) sLoginURL, "UTF-8");
        data += "&" + URLEncoder.encode("submitForm", "UTF-8") + "=" + URLEncoder.encode("Login", "UTF-8");
        return data;
    }

    public String getCollectParams(CollectActivity collectActivity, int x, int y) throws UnsupportedEncodingException {
        String data = URLEncoder.encode("sammeln", "UTF-8") + "=" + URLEncoder.encode(collectActivity.getCollectTime(), "UTF-8");
        data += "&" + URLEncoder.encode("konzentrieren", "UTF-8") + "=" + URLEncoder.encode(collectActivity.getConcentration(), "UTF-8");
        data += "&" + URLEncoder.encode("captchacheck.x", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(x), "UTF-8");
        data += "&" + URLEncoder.encode("captchacheck.y", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(y), "UTF-8");
        return data;
    }

    public String getSellParams(int count, int share, int max) throws UnsupportedEncodingException {
        String data = URLEncoder.encode("chkval", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(share), "UTF-8");
        data += "&" + URLEncoder.encode("max", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(max), "UTF-8");
        data += "&" + URLEncoder.encode("sum", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(count), "UTF-8");
        return data;
    }

    public void setRequestProperties(URLConnection cURL) {
        cURL.setRequestProperty("User-Agent", sUserAgent);
        if (encodedProxyAuthorization != null) {
            cURL.setRequestProperty("Proxy-Authorization", encodedProxyAuthorization);
        }
    }

    public String getPage(String sURL) throws IOException {
        URL pageURL = null;
        InputStream isURL = null;
        String page = "";
        if (sURL != "") {
            try {
                pageURL = new URL(sURL);
            } catch (MalformedURLException e) {
                LogedIn = false;
                return "";
            }
            URLConnection conUrl = (URLConnection) pageURL.openConnection();
            ;
            setRequestProperties(conUrl);
            try {
                isURL = conUrl.getInputStream();
            } catch (ConcurrentModificationException e) {
                return "";
            } catch (UnknownHostException e) {
                gettingPage = false;
                conUrl = (URLConnection) pageURL.openConnection();
                return "";
            } catch (IOException e) {
                gettingPage = false;
                return "";
            }
            BufferedReader rd = new BufferedReader(new InputStreamReader(isURL));
            StringBuilder sb = new StringBuilder();
            String line = rd.readLine();
            while (line != null) {
                sb.append(line + "\n");
                line = rd.readLine();
            }
            rd.close();
            isURL.close();
            page = sb.toString();
        }
        gettingPage = true;
        return page;
    }

    public void relogin() throws Exception {
        login(sUID, sPWD, iCTY);
    }

    public void login(String UID, String PWD, int CTY) throws Exception {
        sSideURL = sSideURLCollection[CTY];
        sUID = UID;
        sPWD = PWD;
        iCTY = CTY;
        sLoginLabel = getLoginLabel(sSideURL);
        String sParams = getLoginParams();
        CookieHandler.setDefault(new ListCookieHandler());
        URL url = new URL(sSideURL + sLoginURL);
        URLConnection conn = url.openConnection();
        setRequestProperties(conn);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(sParams);
        wr.flush();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line = rd.readLine();
        while (line != null) {
            sb.append(line + "\n");
            line = rd.readLine();
        }
        wr.close();
        rd.close();
        String sPage = sb.toString();
        Pattern p = Pattern.compile(">Dein Penner<");
        Matcher matcher = p.matcher(sPage);
        LogedIn = matcher.find();
    }

    public void logout(int CTY) throws Exception {
        sSideURL = sSideURLCollection[CTY];
        String sPage = getPage(sSideURL + sLogoutURL);
        Pattern p = Pattern.compile("Du hast Dich erfolgreich ausgeloggt!");
        Matcher matcher = p.matcher(sPage);
        LogedIn = !matcher.find();
    }

    public String getLoginLabel(String sideURL) throws IOException {
        String result = "";
        String sPage = getPage(sideURL);
        Pattern p = Pattern.compile("<input.*name=\"(.*?)\" id=\"login_username\".*/>");
        Matcher matcher = p.matcher(sPage);
        if (matcher.find()) {
            result = sPage.substring(matcher.start(1), matcher.end(1));
        }
        return result;
    }

    public void getPennerImage(String sURL) throws IOException {
        URL targetURL = new URL(sURL);
        URLConnection url = (URLConnection) targetURL.openConnection();
        setRequestProperties(url);
        InputStream is = url.getInputStream();
        ImageInputStream inStream = ImageIO.createImageInputStream(is);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(inStream);
        ImageReader read = readers.next();
        while (readers.hasNext()) {
            read = readers.next();
        }
        read.setInput(inStream, true, true);
        Image bimage = read.read(0);
        File outfile = new File("gfx/Penner.jpg");
        ImageIO.write((RenderedImage) bimage, "jpg", outfile);
        is.close();
        inStream.close();
    }

    public void getCaptchaImage(String sURL) throws IOException {
        URL targetURL = new URL(sURL);
        URLConnection url = (URLConnection) targetURL.openConnection();
        setRequestProperties(url);
        InputStream is = url.getInputStream();
        ImageInputStream inStream = ImageIO.createImageInputStream(is);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(inStream);
        ImageReader read = readers.next();
        while (readers.hasNext()) {
            read = readers.next();
        }
        read.setInput(inStream, true, true);
        Image bimage = read.read(0);
        File outfile = new File("gfx/captcha.jpg");
        ImageIO.write((RenderedImage) bimage, "jpg", outfile);
        is.close();
        inStream.close();
    }

    public void startCollecting(CollectActivity collectActivity, int[] xy) throws Exception {
        String sParams = getCollectParams(collectActivity, xy[0], xy[1]);
        URL url = new URL(sSideURL + sCollectURL);
        URLConnection conn = (URLConnection) url.openConnection();
        ;
        setRequestProperties(conn);
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(sParams);
        wr.flush();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line = rd.readLine();
        while (line != null) {
            sb.append(line + "\n");
            line = rd.readLine();
        }
        wr.close();
        rd.close();
        String sPage = sb.toString();
        Pattern p = Pattern.compile("Du bist auf Pfandflaschensuche");
        Matcher matcher = p.matcher(sPage);
        collectStarted = matcher.find();
    }

    public void startSelling(int Count, int Share, int Max) throws Exception {
        String sParams = getSellParams(Count, Share, Max);
        URL url = new URL(sSideURL + sSellURL);
        URLConnection conn = (URLConnection) url.openConnection();
        setRequestProperties(conn);
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(sParams);
        wr.flush();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line = rd.readLine();
        while (line != null) {
            sb.append(line + "\n");
            line = rd.readLine();
        }
        wr.close();
        rd.close();
        String sPage = sb.toString();
        Pattern p = Pattern.compile("\'Hinweis\', \'");
        Matcher matcher = p.matcher(sPage);
        sellSuccess = matcher.find();
    }
}
